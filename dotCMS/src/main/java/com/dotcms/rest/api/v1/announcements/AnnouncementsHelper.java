package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import java.util.List;

/**
 * Helper class to get the announcements
 */
public interface AnnouncementsHelper {

    Lazy<Integer> ANNOUNCEMENTS_LIMIT =
            Lazy.of(() -> Config.getIntProperty("ANNOUNCEMENTS_LIMIT", 5));


    List<Announcement> getAnnouncements(boolean refreshCache,
            Integer limit, User user);

}
