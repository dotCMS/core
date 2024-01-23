package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.dotcms.system.announcements.AnnouncementsCache;
import com.dotcms.system.announcements.AnnouncementsCacheImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * Helper class to get the announcements
 */
public class AnnouncementsHelperImpl implements AnnouncementsHelper{

    private final LanguageAPI languageAPI;

    private final AnnouncementsCache announcementsCache;

    private final AnnouncementsLoader loader;


    public AnnouncementsHelperImpl() {
        this(APILocator.getLanguageAPI(), new RemoteAnnouncementsLoaderImpl(), new AnnouncementsCacheImpl());
    }

    /**
     * Constructor
     * @param languageAPI LanguageAPI
     * @param announcementsCache AnnouncementsCache
     */
    public AnnouncementsHelperImpl(LanguageAPI languageAPI, AnnouncementsLoader loader, AnnouncementsCache announcementsCache) {
        this.languageAPI = languageAPI;
        this.announcementsCache = announcementsCache;
        this.loader = loader;
    }


    /**
     * Get the language by id or code, if not found fallback to default language
     * @param languageIdOrCode String
     * @return Language
     */
    Language getLanguage(final String languageIdOrCode) {
        Language language;
        try {
            final String[] split = languageIdOrCode.split("-");
            if(split.length > 1) {
                language = languageAPI.getLanguage(split[0], split[1]);
            } else {
                language = languageAPI.getLanguage(languageIdOrCode);
            }
        } catch (Exception e) {
            Logger.debug(AnnouncementsHelperImpl.class, String.format(" failed to get lang [%s] with message: [%s] fallback to default language", languageIdOrCode, e.getMessage()));
            language = languageAPI.getDefaultLanguage();
        }
        return language;
    }


    /**
     * Get the announcements from the cache or from the remote server
     * @param languageIdOrCode String
     * @param refreshCache boolean
     * @param limit Integer
     * @param user User
     * @return List<Announcement>
     */
    @Override
    public List<Announcement> getAnnouncements( final String languageIdOrCode, final boolean refreshCache, final Integer limit, final User user) {

        Logger.debug(AnnouncementsHelperImpl.class,String.format("Getting announcements for language: %s refreshCache: %s limit: %d, user: %s ", languageIdOrCode, refreshCache, limit, user.getUserId()));
        final Language language = getLanguage(languageIdOrCode);
        final int limitValue = getLimit(limit);
        if(!refreshCache) {
            Logger.debug(this, "Getting announcements from cache for language: " + language.getId() + " limit: " + limitValue);
            final List<Announcement> announcements = announcementsCache.get(language);
            if (announcements != null && !announcements.isEmpty()) {
                return getSubList(limitValue, announcements);
            }
        }
        final List<Announcement> announcements = loader.loadAnnouncements(language);
        if(!announcements.isEmpty()){
            announcementsCache.put(language, announcements);
            return getSubList(limitValue, announcements);
        }
        return List.of();
    }


    /**
     * Get the limit value
     * @param limit int
     * @return int
     */
    int getLimit(final int limit) {
        return limit <= 0 ? ANNOUNCEMENTS_LIMIT.get() : limit;
    }

    /**
     * Get a sub list of the list based on the limit
     * if the limit is greater than the list size then the list is returned
     * @param limit int
     * @param list List<T>
     * @param <T> T
     * @return List<T>
     */
     <T> List<T> getSubList(final int limit, final List<T> list) {
        if (list.isEmpty()) {
            return List.of(); // Return an empty list for an empty input list
        }

        final int endIndex = Math.min(limit, list.size());
        return list.subList(0, endIndex);
    }

}
