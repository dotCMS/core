package com.dotcms.config;

import java.io.Serializable;

/**
 * Responsible for initializing service, library or any state
 *
 * The implementation of these classes will be look up from the dotmarketing-config.properties or
 * {@link java.util.ServiceLoader} approaches in this way you could extends dotCMS initialization with your own stuff.
 *
 *
 */
public interface DotInitializer extends Serializable {

    /**
     * Do the initialization
     *
     */
    public void init();

    /**
     * This is just for debugging proposes.
     * @return String
     */
    public default String getName () {

        return this.getClass().getName();
    }
} // E:O:F:DotInitializer.
