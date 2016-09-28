package com.dotcms.util.marshal;

import java.io.Serializable;

import com.dotcms.repackage.com.google.gson.GsonBuilder;

/**
 * Contract to configure a marshal util object In case you want an specific
 * configuration you can use implement your and overrides it on the
 * "gson.configurator" on dotmarketing-config.properties
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public interface GsonConfigurator extends Serializable {

    /**
     * Configures a gsonBuilder
     * @param gsonBuilder {@link GsonBuilder}n
     */
    void configure (GsonBuilder gsonBuilder);

    /**
     * In case you want to avoid default configuration
     * @return boolean
     */
    boolean excludeDefaultConfiguration();

} // E:O:F:MarshalConfigurator.
