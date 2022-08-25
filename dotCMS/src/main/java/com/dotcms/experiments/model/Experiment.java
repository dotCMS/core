//package com.dotcms.experiments.model;
//
//import com.dotcms.publisher.util.PusheableAsset;
//import com.dotcms.publishing.manifest.ManifestItem;
//import com.dotcms.util.DotPreconditions;
//import com.dotmarketing.business.DotStateException;
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
//import java.io.Serializable;
//import java.time.LocalDateTime;
//
//public class Experiment implements Serializable, ManifestItem {
//    private String name;
//    private String description;
//    private String id;
//    private Status status;
//    private TrafficProportion trafficProportion;
//    private Scheduling scheduling;
//    private float trafficAllocation;
//    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
//    @JsonSerialize(using = LocalDateTimeSerializer.class)
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime modDate;
//    private final String pageId;
//    private boolean readyToStart;
//    private boolean archived;
//
//    public enum Status {
//        RUNNING,
//        SCHEDULED,
//        ENDED,
//        DRAFT
//    }
//
//    public static class Builder {
//        // required parameters
//        private final String pageId;
//        private String name;
//        private String description;
//
//        private String id;
//        private Status status = Status.DRAFT;
//        private TrafficProportion trafficProportion = TrafficProportion.createSplitEvenlyTraffic();
//        private Scheduling scheduling;
//        private float trafficAllocation = 100;
//        private LocalDateTime modDate = LocalDateTime.now();
//        private boolean readyToStart;
//        private boolean archived;
//
//        public Builder(final String pageId, final String name, final String description) {
//            this.pageId = pageId;
//            this.name = name;
//            this.description = description;
//        }
//
//        public Builder(final Experiment val) {
//            this.name = val.name;
//            this.description = val.description;
//            this.id = val.id;
//            this.status = val.status;
//            this.trafficProportion = val.trafficProportion;
//            this.trafficAllocation = val.trafficAllocation;
//            this.scheduling = val.scheduling;
//            this.modDate = val.modDate;
//            this.pageId = val.pageId;
//            this.readyToStart = val.readyToStart;
//        }
//
//        public Builder name(final String val) {
//            name = val;
//            return this;
//        }
//
//        public Builder description(final String val) {
//            description = val;
//            return this;
//        }
//
//        public Builder id(final String val) {
//            id = val;
//            return this;
//
//        }
//        public Builder status(final Status val) {
//            status = val;
//            return this;
//        }
//
//        public Builder trafficProportion(final TrafficProportion val) {
//            trafficProportion = val;
//            return this;
//        }
//
//        public Builder trafficAllocation(final float val) {
//            trafficAllocation = val;
//            return this;
//        }
//
//        public Builder scheduling(final Scheduling val) {
//            scheduling = val;
//            return this;
//        }
//
//        public Builder modDate(final LocalDateTime val) {
//            modDate = val;
//            return this;
//        }
//
//        public Builder readyToStart(final boolean val) {
//            readyToStart = val;
//            return this;
//        }
//
//        public Builder archived(final boolean val) {
//            archived = val;
//            return this;
//        }
//
//        public Experiment build() {
//            return new Experiment(this);
//        }}
//
//    private Experiment(final Builder builder) {
//        DotPreconditions.checkNotEmpty(builder.pageId, DotStateException.class, "pageId is mandatory");
//        DotPreconditions.checkNotEmpty(builder.name, DotStateException.class, "name is mandatory");
//        DotPreconditions.checkNotEmpty(builder.description, DotStateException.class, "description is mandatory");
//        DotPreconditions.checkNotNull(builder.status, DotStateException.class, "status is mandatory");
//        DotPreconditions.checkNotNull(builder.trafficProportion, DotStateException.class, "trafficProportion is mandatory");
//        DotPreconditions.checkArgument(builder.trafficAllocation>=0
//                && builder.trafficAllocation<=100, "trafficAllocation must be between 0 and 100");
//        DotPreconditions.checkNotNull(builder.modDate, DotStateException.class, "modDate is mandatory");
//
////        if(UtilMethods.isSet(builder.startDate) && UtilMethods.isSet(builder.endDate)) {
////           DotPreconditions.checkArgument(builder.endDate.isAfter(builder.startDate),
////                    "endDate must be after startDate");
////        }
//
//        this.name = builder.name;
//        this.description = builder.description;
//        this.id = builder.id;
//        this.status = builder.status;
//        this.trafficProportion = builder.trafficProportion;
//        this.trafficAllocation = builder.trafficAllocation;
//        this.scheduling = builder.scheduling;
//        this.modDate = builder.modDate;
//        this.pageId = builder.pageId;
//        this.readyToStart = builder.readyToStart;
//        this.archived = builder.archived;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public Status getStatus() {
//        return status;
//    }
//
//    public TrafficProportion getTrafficProportion() {
//        return trafficProportion;
//    }
//    public float getTrafficAllocation() {
//        return trafficAllocation;
//    }
//
//    public Scheduling getScheduling() {
//        return scheduling;
//    }
//
//    public LocalDateTime getModDate() {
//        return modDate;
//    }
//
//    public String getPageId() {
//        return pageId;
//    }
//
//    public boolean isReadyToStart() {
//        return readyToStart;
//    }
//
//    public boolean isArchived() {
//        return archived;
//    }
//
//    public Experiment.Builder toBuilder() {
//        return new Experiment.Builder(this);
//    }
//
//    @JsonIgnore
//    @Override
//    public ManifestInfo getManifestInfo(){
//        return new ManifestInfoBuilder()
//                .objectType(PusheableAsset.EXPERIMENT.getType())
//                .id(this.getId())
//                .title(this.getName())
//                .build();
//    }
//}
