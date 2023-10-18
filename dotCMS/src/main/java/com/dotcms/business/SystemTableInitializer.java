package com.dotcms.business;

import com.dotcms.config.DotInitializer;
import com.dotmarketing.util.Config;

/**
 *
 * @author Jose Castro
 * @since Oct 12th, 2023
 */
public class SystemTableInitializer implements DotInitializer {

    @Override
    public void init() {
        Config.initSystemTableConfigSource();
    }

}
