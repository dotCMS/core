package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.configuration.Configuration;

import java.io.Serializable;

/**
 * Defines an interpolator for a Configuration.
 * A ConfigurationInterpolator do a process over the {@link Configuration} and resolve
 * a dynamic values based on the implementation.
 * @author jsanca
 */
public interface ConfigurationInterpolator extends Serializable {

    /**
     * Process the interpolation over the configuration.
     * @param originalConfiguration {@link Configuration}
     * @return Configuration
     */
    Configuration interpolate (Configuration originalConfiguration);

    /**
     * Do the interpolation for a single string
     * @param value String
     * @return String
     */
    String interpolate (final String value);
} // E:O:F:ConfigurationInterpolator.
