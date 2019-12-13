package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.IndiciesInfo.CLUSTER_PREFIX;

import com.dotcms.enterprise.cluster.ClusterFactory;

public enum IndexType {
    WORKING(IndexType.ES_WORKING_INDEX_NAME_PREFIX, "working"),
    LIVE(IndexType.ES_LIVE_INDEX_NAME_PREFIX, "live"),
    REINDEX_WORKING(IndexType.ES_WORKING_INDEX_NAME_PREFIX, "reindexWorking"),
    REINDEX_LIVE(IndexType.ES_LIVE_INDEX_NAME_PREFIX, "reindexLive"),
    SITE_SEARCH("sitesearch", "siteSearch");

    private static final String ES_WORKING_INDEX_NAME_PREFIX = "working";
    private static final String ES_LIVE_INDEX_NAME_PREFIX = "live";


    private final String prefix;
    private final String propertyName;

    IndexType(final String prefix, final String propertyName){
        this.prefix = prefix;
        this.propertyName = propertyName;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPropertyName(){
        return propertyName;
    }

    public boolean is(final String indexName) {
        return indexName != null && indexName.startsWith(this.getPrefix());
    }

    public String getPattern() {
        return new StringBuilder(CLUSTER_PREFIX).append(ClusterFactory.getClusterId())
                .append(".").append(this.getPrefix()).append("_*").toString();
    }
}