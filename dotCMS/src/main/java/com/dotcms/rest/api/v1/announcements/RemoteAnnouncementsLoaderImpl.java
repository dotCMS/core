package com.dotcms.rest.api.v1.announcements;

import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.system.announcements.Announcement;
import com.dotmarketing.business.APILocator;
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
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * `AnnouncementsLoader` implementation that loads the announcements from a remote dotCMS instance
 */
public class RemoteAnnouncementsLoaderImpl implements AnnouncementsLoader{

    //The CT varN/ame used to retrieve the Announcements
    static final Lazy<String>DOT_ANNOUNCEMENT_CT =
            Lazy.of(()-> Config.getStringProperty("DOT_ANNOUNCEMENT_CT", "Announcement"));

    //This is the url to the dotCMS instance set to provide and feed all consumers with announcements
    static final  Lazy<String> ANNOUNCEMENTS_BASE_URL =
            Lazy.of(() -> Config.getStringProperty("ANNOUNCEMENTS_BASE_URL", "https://www.dotcms.com"));

    //The query pattern to retrieve the Announcements
    static final String ANNOUNCEMENTS_QUERY_PATTERN = "%s/api/content/render/false/query/+contentType:%s +languageId:%d +deleted:false +live:true/orderBy/%s.announcementDate desc";

    /**
     * Load the announcements from the remote dotCMS instance
     * @param language Language
     * @return List<Announcement>
     */
    @Override
    public List<Announcement> loadAnnouncements(final Language language) {
        final JsonNode jsonNode = loadRemoteAnnouncements(language);
        return toAnnouncements(jsonNode);
    }

    /**
     * Load the announcements from the remote dotCMS instance
     * @param language Language
     * @return JsonNode
     */
    JsonNode loadRemoteAnnouncements(final Language language) {
        final String url = buildURL(language);
        try {
            final Client client = restClient();
            final WebTarget webTarget = client.target(url);
            final Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
            if (response.getStatus() == 200) {
                final String jsonString = response.readEntity(String.class);
                final ObjectMapper mapper = ContentletJsonHelper.INSTANCE.get().objectMapper();
                final JsonFactory factory = mapper.getFactory();
                final JsonParser parser = factory.createParser(jsonString);
                return mapper.readTree(parser);
            } else {
                Logger.debug(AnnouncementsHelperImpl.class, String.format(" failed to get announcements from [%s] with status: [%d] and  entity: [%s] ", url, response.getStatus(), response.getEntity()));
                throw new DotRuntimeException(String.format(" failed to get announcements from [%s] with status: [%d]", url, response.getStatus()));
            }

        } catch (Exception e) {
            Logger.debug(AnnouncementsHelperImpl.class, String.format(" failed to get announcements from [%s] with message: [%s]", url, e.getMessage()));
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Build the url to retrieve the announcements
     * @param language Language
     * @return String
     */
     String buildURL(Language language) {
        final String raw = String.format(ANNOUNCEMENTS_QUERY_PATTERN, ANNOUNCEMENTS_BASE_URL.get(),
                DOT_ANNOUNCEMENT_CT.get(), language.getId(), DOT_ANNOUNCEMENT_CT.get());
        //clean up double slashes in the url
        return raw.replaceAll("(?<!(http:|https:))//", "/");
    }

    /**
     * Get a rest client
     * @return Client
     */
    static Client restClient() {
        return RestClientBuilder.newClient();
    }

    /**
     * Get the language by id or code, if not found fallback to default language
     * @param languageIdOrCode String
     * @return Language
     */
    Language getLanguage(final String languageIdOrCode) {
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        Language language;
        try {
            language = languageAPI.getLanguage(languageIdOrCode);
        } catch (Exception e) {
            Logger.debug(AnnouncementsHelperImpl.class, String.format(" failed to get lang [%s] with message: [%s] fallback to default language", languageIdOrCode, e.getMessage()));
            language = languageAPI.getDefaultLanguage();
        }
        return language;
    }

    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 3, true)
            .toFormatter();


    /**
     * Convert the json to a list of announcements
     * @param root JsonNode
     * @return List<Announcement>
     */
     List<Announcement> toAnnouncements(final JsonNode root) {

        final ImmutableList.Builder<Announcement> announcements = ImmutableList.builder();
        final JsonNode nodes = root.get("contentlets");
        nodes.forEach(node -> {
            final String identifier = Try.of(()->node.get("identifier").asText()).getOrElse("unk");
            final String inode = Try.of(()->node.get("inode").asText()).getOrElse("unk");
            final String title = Try.of(()->node.get("title").asText()).getOrElse("unk");
            final String dateString = Try.of(()->node.get("announcementDate").asText()).getOrElse("1970-01-01 00:00:00.0");
            final String type = Try.of(()->node.get("type1").asText()).getOrElse("Announcement");
            final String url = Try.of(()->node.get("url").asText()).getOrElse("unk");
            final String langId = Try.of(() -> node.get("languageId").asText()).getOrElse("1");
            final String modDateString = Try.of(()->node.get("modDate").asText()).getOrElse("1970-01-01 00:00:00.0");
            final String description = Try.of(()->node.get("description").asText()).getOrElse("unk");

            final LocalDateTime date = LocalDateTime.parse(dateString, formatter);
            final LocalDateTime modDate = LocalDateTime.parse(modDateString, formatter);

            final Language language = getLanguage(langId);


            announcements.add(
                    Announcement.builder()
                            .identifier(identifier)
                            .inode(inode)
                            .title(title)
                            .announcementDate(date.toInstant(java.time.ZoneOffset.UTC))
                            .announcementDateAsISO8601(date.toString())
                            .type(type)
                            .url(url)
                            .languageId(language.getId())
                            .languageCode(language.getIsoCode())
                            .modDate(modDate.toInstant(java.time.ZoneOffset.UTC))
                            .modDateAsISO8601(modDate.toString())
                            .description(description)
                            .build()
            );
        });
        return announcements.build();

    }


}
