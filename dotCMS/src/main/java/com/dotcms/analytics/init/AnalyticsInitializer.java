package com.dotcms.analytics.init;

import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;

/**
 * Does the initialization of the analytics
 * Basically subscribes to the system table update events
 * @author jsanca
 */
public class AnalyticsInitializer implements DotInitializer {

    @Override
    public void init() {

        Logger.debug(this, ()-> "Initializing AnalyticsInitializer");
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                WebAPILocator.getAnalyticsWebAPI());
    }
}
