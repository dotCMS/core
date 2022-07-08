package com.dotcms.publishing;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;

/**
 * Path of the yaml files: /assets/server/publishing-filters/
 *
 * @author Erick Gonzalez
 * @since Jun 3rd, 2020
 */
public class PushPublishFiltersInitializer implements DotInitializer {

    @Override
    public void init() {
        APILocator.getPublisherAPI().initializeFilterDescriptors();
    }

}
