package com.dotcms.system.announcements;

import static java.time.temporal.ChronoUnit.SECONDS;

import com.dotcms.business.SystemCache;
import com.dotcms.cache.Expirable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class AnnouncementsCacheImpl implements AnnouncementsCache {

    static final Lazy<Integer> ANNOUNCEMENTS_TTL =
            Lazy.of(() -> Config.getIntProperty("ANNOUNCEMENTS_TTL", 3600 ));

    static final String ANNOUNCEMENTS = "announcements::%s";
    private final SystemCache systemCache;

    public AnnouncementsCacheImpl() {
        super();
        this.systemCache = CacheLocator.getSystemCache();
    }

    @Override
    public void clearCache() {
        systemCache.clearCache();
    }

    private String hashKey(Language language) {
        return String.format(ANNOUNCEMENTS, language.getIsoCode());
    }

    @Override
    public void put(final Language language, final List<Announcement> announcements) {
        this.put(language, announcements, ANNOUNCEMENTS_TTL.get());
    }

    @VisibleForTesting
    public void put(final Language language, final List<Announcement> announcements , final long ttl) {
        systemCache.put(hashKey(language), new CacheEntryImpl(announcements,ttl));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Announcement> get(final Language language) {
        final String key = String.format(ANNOUNCEMENTS, language.getIsoCode());
        final Object object = systemCache.get(key);
        if (object instanceof Expirable) {
            final CacheEntryImpl entry = (CacheEntryImpl) object;
            if (entry.isExpired()) {
                systemCache.remove(key);
                return List.of();
            }
            return entry.getAnnouncements();
        }
        return List.of();
    }

    private static class CacheEntryImpl implements Expirable, Serializable {
        private static final long serialVersionUID = 1L;

        private final long ttl;

        private final LocalDateTime since;
        private final List<Announcement> announcements;


        CacheEntryImpl(final List<Announcement> announcements, final long ttl) {
            this.announcements = announcements;
            this.ttl = ttl;
            this.since = LocalDateTime.now();
        }

        public List<Announcement> getAnnouncements() {
            return  announcements == null ? List.of() : announcements;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(since.plus(ttl, SECONDS));
        }
    }
}
