package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import java.util.List;

/**
 * Loader for the announcements Contract
 */
public interface AnnouncementsLoader {

    /**
     * Load the announcements for the given language
     * @return List<Announcement>
     */
    List<Announcement> loadAnnouncements();

}
