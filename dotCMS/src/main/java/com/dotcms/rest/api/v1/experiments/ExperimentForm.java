package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment.Status;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;

public class ExperimentForm extends Validated {
    private String name;
    private String description;
    private Status status;
    private String pageId;
    private float trafficAllocation=-1;
    private TrafficProportion trafficProportion;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public Status getStatus() {
        return status;
    }

    public float getTrafficAllocation() {
        return trafficAllocation;
    }

    public void setTrafficAllocation(float trafficAllocation) {
        this.trafficAllocation = trafficAllocation;
    }

    public TrafficProportion getTrafficProportion() {
        return trafficProportion;
    }

    public void setTrafficProportion(TrafficProportion trafficProportion) {
        this.trafficProportion = trafficProportion;
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
}
