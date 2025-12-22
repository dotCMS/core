package com.dotcms.adminsite;


import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;


public class AdminSiteKeyListener implements EventSubscriber<SystemTableUpdatedKeyEvent> {


    @Override
    public void notify(SystemTableUpdatedKeyEvent event) {
        if (event == null || event.getKey() == null) {
            Logger.info(this, "Missing event key, aborting");
            return;
        }
        if (event.getKey().toLowerCase().startsWith("admin_site") || event.getKey().toLowerCase()
                .startsWith("dot_admin_site")) {
            Logger.info(this, "Admin site key updated: " + event.getKey() + ".  Clearing Admin site cache. ");
            APILocator.getAdminSiteAPI().invalidateCache();

        }
    }
}
