package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.liferay.portal.model.User;
import java.util.List;

public interface AnnouncementsHelper {

    List<Announcement> getAnnouncements(String languageIdOrCode, boolean refreshCache,
            Integer limit, User user);

}
