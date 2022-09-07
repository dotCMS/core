package com.dotcms.experiments.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Immutable implementation of an Experiment.
 * <p>
 * Experiments are a way to test changes to HTML Pages by creating new versions of a Page
 * and then get a report on which page performed better according to the decided goals.
 * <p>
 * The Experiment can be started now or scheduled to start and finish according to given dates
 * <p>
 *
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractExperiment extends Serializable, ManifestItem {
    @JsonProperty("name")
    String name();

    @JsonProperty("description")
    String description();

    @JsonProperty("id")
    Optional<String> id();

    @JsonProperty("status")
    @Value.Default
    default Status status() {
        return Status.DRAFT;
    }

    @JsonProperty("trafficProportion")
    @Value.Default
    default TrafficProportion trafficProportion() {
        return TrafficProportion.builder().build();
    }

    @JsonProperty("scheduling")
    Optional<Scheduling> scheduling();

    @JsonProperty("trafficAllocation")
    @Value.Default
    default float trafficAllocation() {
        return 100;
    }

    @JsonProperty("creationDate")
    @Value.Default
    default Instant creationDate() {
        return Instant.now();
    }

    @JsonProperty("modDate")
    @Value.Default
    default Instant modDate() {
        return Instant.now();
    }

    @JsonProperty("pageId")
    String pageId();

    @JsonProperty("archived")
    @Value.Default
    default boolean archived() {
        return false;
    }

    @JsonProperty("createdBy")
    String createdBy();

    @JsonProperty("lastModifiedBy")
    String lastModifiedBy();

    @JsonProperty("goals")
    Optional<Goals> goals();

    @Value.Derived
    @Override
    default ManifestInfo getManifestInfo() {
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.EXPERIMENT.getType())
                .id(this.id().orElse(null))
                .title(this.name())
                .build();
    }

    enum Status {
        RUNNING,
        SCHEDULED,
        ENDED,
        DRAFT
    }
}
