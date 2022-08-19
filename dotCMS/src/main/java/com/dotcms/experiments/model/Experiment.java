package com.dotcms.experiments.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;

public final class Experiment implements Serializable, ManifestItem {
    private String name;
    private String description;
    private String id;
    private Status status;
    private TrafficProportion trafficProportion;
    private float trafficAllocation;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modDate;
    private final String pageId;
    private boolean readyToStart;

    public enum Status {
        RUNNING,
        SCHEDULED,
        ENDED,
        DRAFT
    }

    public static class Builder {
        // required parameters
        private final String pageId;
        private String name;
        private String description;

        private String id;
        private Status status = Status.DRAFT;
        private TrafficProportion trafficProportion = TrafficProportion.createSplitEvenlyTraffic();
        private float trafficAllocation = 100;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime modDate;
        private boolean readyToStart;

        public Builder(final String pageId, final String name, final String description) {
            DotPreconditions.checkNotEmpty(pageId, DotStateException.class, "pageId is mandatory");
            DotPreconditions.checkNotEmpty(name, DotStateException.class, "name is mandatory");
            DotPreconditions.checkNotEmpty(description, DotStateException.class, "description is mandatory");
            this.pageId = pageId;
            this.name = name;
            this.description = description;
        }

        public Builder(final Experiment val) {
            this.name = val.name;
            this.description = val.description;
            this.id = val.id;
            this.status = val.status;
            this.trafficProportion = val.trafficProportion;
            this.trafficAllocation = val.trafficAllocation;
            this.startDate = val.startDate;
            this.endDate = val.endDate;
            this.modDate = val.modDate;
            this.pageId = val.pageId;
            this.readyToStart = val.readyToStart;
        }

        public Builder name(final String val) {
            name = val;
            return this;
        }

        public Builder description(final String val) {
            description = val;
            return this;
        }

        public Builder id(final String val) {
            id = val;
            return this;

        }
        public Builder status(final Status val) {
            status = val;
            return this;
        }

        public Builder trafficProportion(final TrafficProportion val) {
            trafficProportion = val;
            return this;
        }

        public Builder trafficAllocation(final float val) {
            trafficAllocation = val;
            return this;
        }

        public Builder startDate(final LocalDateTime val) {
            startDate = val;
            return this;
        }

        public Builder endDate(final LocalDateTime val) {
            endDate = val;
            return this;
        }

        public Builder modDate(final LocalDateTime val) {
            modDate = val;
            return this;
        }

        public Builder readyToStart(final boolean val) {
            readyToStart = val;
            return this;
        }

        public Experiment build() {
            return new Experiment(this);
        }}

    private Experiment(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.id = builder.id;
        this.status = builder.status;
        this.trafficProportion = builder.trafficProportion;
        this.trafficAllocation = builder.trafficAllocation;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.modDate = builder.modDate;
        this.pageId = builder.pageId;
        this.readyToStart = builder.readyToStart;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public TrafficProportion getTrafficProportion() {
        return trafficProportion;
    }
    public float getTrafficAllocation() {
        return trafficAllocation;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public LocalDateTime getModDate() {
        return modDate;
    }

    public String getPageId() {
        return pageId;
    }

    public boolean isReadyToStart() {
        return readyToStart;
    }

    public Experiment.Builder toBuilder() {
        return new Experiment.Builder(this);
    }

    @JsonIgnore
    @Override
    public ManifestInfo getManifestInfo(){
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.EXPERIMENT.getType())
                .id(this.getId())
                .title(this.getName())
                .build();
    }
}
