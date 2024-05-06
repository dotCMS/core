package com.dotcms.variant.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@JsonDeserialize(as = Variant.class)
@JsonSerialize(as = Variant.class)
@Value.Immutable
public interface AbstractVariant extends Serializable, ManifestItem {
    @JsonProperty("name")
    String name();

    @JsonProperty("description")
    Optional<String> description();

    @Value.Default
    @JsonProperty("archived")
    default boolean archived() {
        return false;
    }

    @Value.Derived
    @Override
    @JsonIgnore
    default ManifestInfo getManifestInfo() {
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.VARIANT.getType())
                .id(this.name())
                .title(this.name())
                .build();
    }

}
