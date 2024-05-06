package com.dotcms.system.announcements;

import java.util.List;

public interface AnnouncementsCache {

    void clearCache();

    void put(List<Announcement> announcements);

    List<Announcement> get();

}
