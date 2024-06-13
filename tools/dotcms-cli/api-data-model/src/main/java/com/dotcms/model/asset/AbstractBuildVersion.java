package com.dotcms.model.asset;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Represents the version of the build
 */
@ValueType
@Value.Immutable
@JsonDeserialize(as = BuildVersion.class)
public interface AbstractBuildVersion {

    /**
     * The name of the build
     * @return the name of the build
     */
    String name();

    /**
     * The version of the build
     * @return the version of the build
     */
    String version();

    /**
     * The timestamp of the build
     * @return the timestamp of the build
     */
    long timestamp();

    /**
     * The revision of the build
     * @return the revision of the build
     */
    String revision();

}
