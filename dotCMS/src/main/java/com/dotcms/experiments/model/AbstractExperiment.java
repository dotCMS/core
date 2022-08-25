package com.dotcms.experiments.model;

import com.dotcms.experiments.model.Experiment.Status;
import com.dotcms.publishing.manifest.ManifestItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.immutables.value.Value;

@Value.Style(typeImmutable="*Test", typeAbstract="Abstract*")
@Value.Immutable
interface AbstractExperiment extends Serializable, ManifestItem {
    String name();
    String description();
    String id();
    Status status();
    TrafficProportion trafficProportion();
    Scheduling scheduling();
    float trafficAllocation();
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime modDate();
    String pageId();
    boolean readyToStart();
    boolean archived();

    enum Status {
        RUNNING,
        SCHEDULED,
        ENDED,
        DRAFT
    }
}
