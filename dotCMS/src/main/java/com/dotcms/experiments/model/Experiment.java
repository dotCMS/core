package com.dotcms.experiments.model;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Experiment implements Serializable, ManifestItem {
    private String name;
    private String description;
    private String id;
    private Status status;
    private TrafficProportion trafficProportion;
    private float trafficAllocation;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime modDate;
    private final String pageId;
    private boolean readyToStart;

    public enum Status {
        RUNNING,
        SCHEDULED,
        ENDED,
        DRAFT
    }

    public Experiment(final String pageId, final String name, final String description) {
        DotPreconditions.checkNotEmpty(pageId, DotStateException.class, "pageId is mandatory");
        DotPreconditions.checkNotEmpty(name, DotStateException.class, "name is mandatory");
        DotPreconditions.checkNotEmpty(description, DotStateException.class, "description is mandatory");
        this.pageId = pageId;
        this.name = name;
        this.description = description;
        this.status = Status.DRAFT;
        this.trafficProportion = TrafficProportion.createSplitEvenlyTraffic();
        this.trafficAllocation = 100;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTrafficProportion(TrafficProportion trafficProportion) {
        this.trafficProportion = trafficProportion;
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

    public void setTrafficAllocation(float trafficAllocation) {
        this.trafficAllocation = trafficAllocation;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getModDate() {
        return modDate;
    }

    public void setModDate(LocalDateTime modDate) {
        this.modDate = modDate;
    }

    public String getPageId() {
        return pageId;
    }

    public boolean isReadyToStart() {
        return readyToStart;
    }

    public void setReadyToStart(boolean readyToStart) {
        this.readyToStart = readyToStart;
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
