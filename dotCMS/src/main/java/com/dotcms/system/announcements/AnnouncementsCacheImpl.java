package com.dotcms.system.announcements;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.List;

public class AnnouncementsCacheImpl implements AnnouncementsCache {

    static final String ANNOUNCEMENTS_CACHE = "AnnouncementsCache";
    public static final String ANNOUNCEMENTS = "announcements::%s";

    private final DotCacheAdministrator cache;

    public AnnouncementsCacheImpl() {
        super();
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public String getPrimaryGroup() {
        return ANNOUNCEMENTS_CACHE;
    }

    @Override
    public String[] getGroups() {
        return new String[]{ANNOUNCEMENTS_CACHE};
    }

    @Override
    public void clearCache() {
        for (final String group : getGroups()) {
            cache.flushGroup(group);
        }
    }

    @Override
    public void put(Language language, List<Announcement> announcements) {
        cache.put(
                String.format(ANNOUNCEMENTS, language.getIsoCode()),
                announcements, ANNOUNCEMENTS_CACHE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Announcement> get(Language language) {
         final Object object = cache.getNoThrow(
                String.format(ANNOUNCEMENTS, language.getIsoCode()),
                ANNOUNCEMENTS_CACHE);
         if(object instanceof List){
             return (List<Announcement>) object;
         }
         return List.of();
    }
}
