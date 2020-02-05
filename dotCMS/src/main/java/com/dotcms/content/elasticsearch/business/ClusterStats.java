package com.dotcms.content.elasticsearch.business;

import java.util.ArrayList;
import java.util.List;

public class ClusterStats {

    private String clusterName;
    private List<NodeStats> nodeStats = new ArrayList<>();

    public ClusterStats(final String clusterName) {
        this.clusterName = clusterName;
    }

    public void addNodeStats(final NodeStats nodeStats) {
        this.nodeStats.add(nodeStats);
    }

    public List<NodeStats> getNodeStats() {
        return nodeStats;
    }

    public String getClusterName() {
        return clusterName;
    }
}
