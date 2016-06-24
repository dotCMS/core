package com.dotmarketing.util.marshal;

import com.dotcms.repackage.com.google.gson.GsonBuilder;
import com.dotmarketing.util.jwt.SigningKeyFactory;

import java.io.Serializable;

/**
 * Contract to configure a marshal util object
 * In case you want an specific configuration you can use implement your and overrides it on the
 * "gson.configurator" on dotmarketing-config.properties
 * @author jsanca
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
