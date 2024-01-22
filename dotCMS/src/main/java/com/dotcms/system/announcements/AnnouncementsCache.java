package com.dotcms.system.announcements;

import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.List;

public interface AnnouncementsCache {

    void clearCache();

    void put(Language language, List<Announcement> announcements);

    List<Announcement> get(Language language);

}
