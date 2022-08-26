package com.dotcms.experiments.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
public interface AbstractExperiment extends Serializable, ManifestItem {
    @JsonGetter("name")
    String name();

    @JsonGetter("description")
    String description();

    @JsonGetter("id")
    Optional<String> id();

    @JsonGetter("status")
    @Value.Default
    default Status status() {
        return Status.DRAFT;
    }

    @JsonGetter("trafficProportion")
    @Value.Default
    default TrafficProportion trafficProportion() {
        return TrafficProportion.createSplitEvenlyTraffic();
    }

    @JsonGetter("scheduling")
    Optional<Scheduling> scheduling();

    @JsonGetter("trafficAllocation")
    @Value.Default
    default float trafficAllocation() {
        return 100;
    }
    @JsonDeserialize(using = LocalDateTimeDeserializer.class, converter = OptionalConverter.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class, converter = OptionalConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonGetter("modDate")
    Optional<LocalDateTime> modDate();

    @JsonGetter("pageId")
    String pageId();

    @JsonGetter("readyToStart")
    @Value.Default
    default boolean readyToStart() {
        return false;
    }

    @JsonGetter("archived")
    @Value.Default
    default boolean archived() {
        return false;
    }

    @Value.Default
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
