package com.dotcms.content.elasticsearch.business;

public enum IndexType {
    WORKING(IndexType.ES_WORKING_INDEX_NAME_PREFIX),
    LIVE(IndexType.ES_LIVE_INDEX_NAME_PREFIX),
    REINDEX_WORKING(IndexType.ES_WORKING_INDEX_NAME_PREFIX),
    REINDEX_LIVE(IndexType.ES_LIVE_INDEX_NAME_PREFIX),
    SITE_SEARCH("sitesearch");

    private static final String ES_WORKING_INDEX_NAME_PREFIX = "working";
    private static final String ES_LIVE_INDEX_NAME_PREFIX = "live";


    private final String prefix;

    IndexType(final String prefix){
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean is(final String indexName) {
        return indexName != null && indexName.startsWith(this.getPrefix());
    }

    public String getPattern() {
        return this.getPrefix() +  "_*";
    }
}