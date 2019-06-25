package com.dotcms.cluster.business;

public enum ReplicasMode { AUTOWIRE(-1, "false"), BOUNDED(-1, "0-all"), STATIC(1, "false");
    private int numberOfReplicas;
    private String autoExpandReplicas;

    ReplicasMode(final int numberOfReplicas, final String autoExpandReplicas) {
        this.numberOfReplicas = numberOfReplicas;
        this.autoExpandReplicas = autoExpandReplicas;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public String getAutoExpandReplicas() {
        return autoExpandReplicas;
    }

    public void setNumberOfReplicas(final int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public void setAutoExpandReplicas(final String autoExpandReplicas) {
        this.autoExpandReplicas = autoExpandReplicas;
    }
}
