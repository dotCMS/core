package com.dotcms.system.event.local.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.services.VanityUrlServices;
import com.dotmarketing.business.APILocator;

/**
 * Initializer class that allow us to register Local System Events subscribers
 *
 * @author Jonathan Gamba 7/28/17
 */
public class LocalSystemEventSubscribersInitializer implements DotInitializer {

    @Override
    public void init() {
        APILocator.getLocalSystemEventsAPI().subscribe(VanityUrlServices.getInstance());
    }

}