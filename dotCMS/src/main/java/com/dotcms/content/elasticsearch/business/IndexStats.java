package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.common.unit.ByteSizeValue;

public class IndexStats {

    private String indexName;
    private long documentCount;
    private long size;
    private String prettySize;

    public IndexStats(final String indexName, final long documentCount, final long size) {
        this.indexName = indexName;
        this.documentCount = documentCount;
        this.size = size;
        this.prettySize = new ByteSizeValue(size).toString();
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public String getSize() {
        return prettySize;
    }

    public long getSizeRaw() {
        return size;
    }

    public String getIndexName() {
        return indexName;
    }
}
