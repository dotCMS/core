package com.dotcms.experiments.model;

public class Experiment {
    private String name;
    private String description;
    private String key;
    private Status status;
    private TrafficProportion trafficProportion;

    public enum Status {
        RUNNING,
        ENDED,
        DRAFT
    }

    public Experiment(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = Status.DRAFT;
        this.trafficProportion = TrafficProportion.createSplitEvenlyTraffic();
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
}
