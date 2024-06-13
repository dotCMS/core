package com.dotcms.api;

import com.dotcms.model.asset.BuildVersion;
import java.util.Optional;

/**
 * Service to get the version of the build
 */
public interface BuiltVersionService {

    /**
     * Check if the build.properties file was found
     * @return true if the build.properties file was found
     */
    Optional<BuildVersion> version();

}
