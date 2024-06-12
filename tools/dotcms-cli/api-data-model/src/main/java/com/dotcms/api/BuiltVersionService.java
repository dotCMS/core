package com.dotcms.api;

import com.dotcms.model.asset.BuildVersion;
import java.util.Optional;

public interface BuiltVersionService {

    boolean isBuildPropertiesFileFound();

    Optional<BuildVersion> version();

}
