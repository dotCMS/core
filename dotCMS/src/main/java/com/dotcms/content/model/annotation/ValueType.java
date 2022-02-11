package com.dotcms.content.model.annotation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.jonpeterson.jackson.module.versioning.JsonVersionedModel;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * This basically tells "Immutables" how the naming style of the out class should look like
 * In this case we instruct removal of the "Abstract"  Prefix
 */
@Value.Style(
        // Detect names starting with underscore
        typeAbstract = "Abstract*",
        // Generate without any suffix, just raw detected name
        typeImmutable = "*",
        // Make generated public, leave underscored as package private
        visibility = ImplementationVisibility.PUBLIC,
        passAnnotations = {JsonVersionedModel.class, Hydration.class}
)
@JsonSerialize()
public @interface ValueType {}
