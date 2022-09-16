package com.dotcms.variant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Variants represent branches or workspaces for {@link com.dotmarketing.portlets.contentlet.model.Contentlet}.
 *
 * In the future it should include also: {@link com.dotmarketing.portlets.templates.model.Template}
 * and {@link com.dotmarketing.portlets.containers.model.Container}.
 *
 * They are a new dimension in addition to languages which means you can have a content version in
 * a language and in a variation.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractVariant extends Serializable {
    @JsonProperty("name")
    String name();

    @Value.Default
    @JsonProperty("description")
    default Optional<String> description() {
        return Optional.of("");
    }

    @Value.Default
    @JsonProperty("archived")
    default boolean archived() {
        return false;
    }

}
