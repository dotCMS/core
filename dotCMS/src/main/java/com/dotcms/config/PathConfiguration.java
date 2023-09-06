package com.dotcms.config;

import java.nio.file.Path;

/**
 * This class encapsulates the Path Configuration for dotcms
 * @author jsanca
 */
public interface PathConfiguration {

    /**
     * Returns return Path stored in a property, if the property is not found, it will return the default value
     * Optionally try to create if the folder does not exist
     *
     * @param name     The name of the property to locate.
     * @param defValue String value of the path to use if property is not found.
     * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
     * element).
     */
    Path getPathProperty(final String name, final String defValue, boolean create);

    Path getDynamicContentPath();

    Path getVelocityRoot();

    Path getAssetRealPath();
}
