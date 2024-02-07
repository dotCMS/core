package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.dotcms.system.announcements.AnnouncementsCache;
import com.dotcms.system.announcements.AnnouncementsCacheImpl;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.List;

/**
 * Helper class to get the announcements
 */
public class AnnouncementsHelperImpl implements AnnouncementsHelper{

    private final AnnouncementsCache announcementsCache;

    private final AnnouncementsLoader loader;

    public AnnouncementsHelperImpl() {
        this(new RemoteAnnouncementsLoaderImpl(), new AnnouncementsCacheImpl());
    }

    /**
     * Constructor
     * @param announcementsCache AnnouncementsCache
     */
    public AnnouncementsHelperImpl(AnnouncementsLoader loader, AnnouncementsCache announcementsCache) {
        this.announcementsCache = announcementsCache;
        this.loader = loader;
    }

    /**
     * Get the announcements from the cache or from the remote server
     *
     * @param refreshCache boolean
     * @param limit Integer
     * @param user User
     * @return List<Announcement>
     */
    @Override
    public List<Announcement> getAnnouncements(final boolean refreshCache, final Integer limit, final User user) {

        Logger.debug(AnnouncementsHelperImpl.class,String.format("Getting announcements refreshCache: %s limit: %d, user: %s ", refreshCache, limit, user.getUserId()));
        final int limitValue = getLimit(limit);
        if(!refreshCache) {
            Logger.debug(this, "Getting announcements from cache for limit: " + limitValue);
            final List<Announcement> announcements = announcementsCache.get();
            if (announcements != null && !announcements.isEmpty()) {
                return getSubList(limitValue, announcements);
            }
        }
        final List<Announcement> announcements = loader.loadAnnouncements();
        if(!announcements.isEmpty()){
            announcementsCache.put(announcements);
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
