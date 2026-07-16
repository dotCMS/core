package com.dotcms.rest.api.v1.announcements;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.system.announcements.Announcement;
import java.util.List;

/**
 * Entity View for announcements collection responses.
 * Contains list of Announcement entities.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityAnnouncementListView extends ResponseEntityView<List<Announcement>> {
    public ResponseEntityAnnouncementListView(final List<Announcement> entity) {
        super(entity);
    }
}
