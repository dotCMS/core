package com.dotcms.rest.api.v1.announcements;

import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.system.announcements.Announcement;
import com.dotcms.system.announcements.AnnouncementsCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class AnnouncementsHelperImpl implements AnnouncementsHelper{

    //The CT varN/ame used to retrieve the Announcements
    static final String DOT_ANNOUNCEMENT = Lazy.of(()-> Config.getStringProperty("DOT_ANNOUNCEMENT", "dotAnnouncement")).get();

    //The query pattern to retrieve the Announcements
    static final String ANNOUNCEMENTS_QUERY_PATTERN = "%s/api/content/render/false/query/+contentType:%s +languageId:%d +deleted:false +live:true/orderBy/modDate desc";

    //This is the url to the dotCMS instance set to provide and feed all consumers with announcements
    private static final Lazy<String> ANNOUNCEMENTS_BASE_URL =
            Lazy.of(() -> Config.getStringProperty("ANNOUNCEMENTS_BASE_URL", "https://www.dotcms.com"));

    private static final Lazy<Integer> ANNOUNCEMENTS_LIMIT =
            Lazy.of(() -> Config.getIntProperty("ANNOUNCEMENTS_LIMIT", 10));

    private final LanguageAPI languageAPI;

    private final AnnouncementsCache announcementsCache;

    public AnnouncementsHelperImpl() {
        this(APILocator.getLanguageAPI(), CacheLocator.getAnnouncementsCache());
    }

    public AnnouncementsHelperImpl(LanguageAPI languageAPI, AnnouncementsCache announcementsCache) {
        this.languageAPI = languageAPI;
        this.announcementsCache = announcementsCache;
    }

    /**
     * Get a rest client
     * @return Client
     */
    private Client restClient() {
        return RestClientBuilder.newClient();
    }

    /**
     * Get the language by id or code, if not found fallback to default language
     * @param languageIdOrCode String
     * @return Language
     */
    private Language getLanguage(final String languageIdOrCode) {
        Language language;
        try {
            language = languageAPI.getLanguage(languageIdOrCode);
        } catch (Exception e) {
            Logger.error(AnnouncementsHelperImpl.class, String.format(" failed to get lang [%s] with message: [%s] fallback to default language", languageIdOrCode, e.getMessage()));
            language = languageAPI.getDefaultLanguage();
        }
        return language;
    }


    @Override
    public List<Announcement> getAnnouncements( final String languageIdOrCode, final boolean refreshCache,
            Integer limit, final User user) {
        final Language language = getLanguage(languageIdOrCode);
        final int limitValue = getLimit(limit);
        if(!refreshCache) {
            Logger.debug(this, "Getting announcements from cache for language: " + language.getId() + " limit: " + limitValue);
            final List<Announcement> announcements = announcementsCache.get(language);
            if (announcements != null && !announcements.isEmpty()) {
                return getSubList(limitValue, announcements);
            }
        }
        final String raw = String.format(ANNOUNCEMENTS_QUERY_PATTERN, ANNOUNCEMENTS_BASE_URL.get(), DOT_ANNOUNCEMENT, language.getId());
        //clean up double slashes in the url
        final String url = raw.replaceAll("(?<!(http:|https:))//", "/");
        try {
            final Client client = restClient();
            final WebTarget webTarget = client.target(url);
            final Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
            if (response.getStatus() == 200) {
                final String jsonString = response.readEntity(String.class);
                final ObjectMapper mapper = ContentletJsonHelper.INSTANCE.get().objectMapper();
                final JsonFactory factory = mapper.getFactory();
                final JsonParser parser = factory.createParser(jsonString);
                final JsonNode root = mapper.readTree(parser);
                final List<Announcement> views = toAnnouncements(root);

                final List<Announcement> subList = getSubList(limitValue, views);
                announcementsCache.put(language, subList);
                return subList;
            } else {
                Logger.error(AnnouncementsHelperImpl.class, String.format(" failed to get announcements from [%s] with status: [%d] and  entity: [%s] ", url, response.getStatus(), response.getEntity()));
                throw new DotRuntimeException(String.format(" failed to get announcements from [%s] with status: [%d]", url, response.getStatus()));
            }

        } catch (Exception e) {
            Logger.error(AnnouncementsHelperImpl.class, String.format(" failed to get announcements from [%s] with message: [%s]", url, e.getMessage()));
            throw new DotRuntimeException(e);
        }
    }

    private int getLimit(final int limit) {
        return limit <= 0 ? ANNOUNCEMENTS_LIMIT.get() : limit;
    }

    public static <T> List<T> getSubList(int limit, List<T> list) {
        if (list.isEmpty()) {
            return List.of(); // Return an empty list for an empty input list
        }

        int endIndex = Math.min(limit, list.size());
        return list.subList(0, endIndex);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    List<Announcement> toAnnouncements(final JsonNode root) {

        final ImmutableList.Builder<Announcement> announcements = ImmutableList.builder();
        final JsonNode nodes = root.get("contentlets");
        nodes.forEach(node -> {
            final String identifier = Try.of(()->node.get("identifier").asText()).getOrElse("unk");
            final String inode = Try.of(()->node.get("inode").asText()).getOrElse("unk");
            final String title = Try.of(()->node.get("title").asText()).getOrElse("unk");
            final String dateString = Try.of(()->node.get("date").asText()).getOrElse("1970-01-01 00:00:00.0");
            final String type = Try.of(()->node.get("type1").asText()).getOrElse("unk");
            final String url = Try.of(()->node.get("url").asText()).getOrElse("unk");
            final String langId = Try.of(() -> node.get("languageId").asText()).getOrElse("unk");

            LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
            final Language language = languageAPI.getLanguage(langId);

            announcements.add(
                    Announcement.builder()
                            .identifier(identifier)
                            .inode(inode)
                            .title(title)
                            .date(localDateTime.toInstant(java.time.ZoneOffset.UTC))
                            .dateAsISO8601(localDateTime.toString())
                            .type(type)
                            .url(url)
                            .languageId(language.getId())
                            .languageCode(language.getIsoCode())
                            .build()
            );
        });
        return announcements.build();

    }

}
