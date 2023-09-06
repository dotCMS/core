package com.dotcms.config;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class PathConfigurationImpl implements PathConfiguration {

    public static final String DYNAMIC_CONTENT_PATH = "DYNAMIC_CONTENT_PATH";
    public static final String DEFAULT_DYNAMIC_CONTENT_PATH = "dotsecure";
    public static final String ASSET_REAL_PATH = "ASSET_REAL_PATH";

    @Inject
    Configuration configuration;
    /**
     * Returns return Path stored in a property, if the property is not found, it will return the default value
     * Optionally try to create if the folder does not exist
     *
     * @param name     The name of the property to locate.
     * @param defValue String value of the path to use if property is not found.
     * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
     * element).
     */
    @Override
    public Path getPathProperty(final String name, final String defValue, boolean create) {

        final String stringVal = configuration.getStringProperty(name, defValue);
        final Path path = Paths.get(stringVal);
        if (create && !Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new DotRuntimeException("Unable to create directory for property: " +name +" with path "+ path,e);
            }
        }
        return path;
    }

    @Override
    public Path getDynamicContentPath() {
        return getPathProperty(DYNAMIC_CONTENT_PATH, DEFAULT_DYNAMIC_CONTENT_PATH, true);
    }

    @Override
    public  Path getVelocityRoot() {
        Path baseRoot = getPathProperty("VELOCITY_ROOT", "${CONTEXT_ROOT}/WEB-INF/velocity", false);
        return baseRoot;
    }

    @Override
    public  Path getAssetRealPath() {
        return Paths.get(ConfigUtils.getAbsoluteAssetsRootPath());
    }
}
