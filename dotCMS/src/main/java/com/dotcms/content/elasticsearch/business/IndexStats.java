package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.common.unit.ByteSizeValue;

public class IndexStats {

    private String indexName;
    private int documentCount;
    private int size;
    private String prettySize;

    public IndexStats(String indexName, int documentCount, int size) {
        this.indexName = indexName;
        this.documentCount = documentCount;
        this.size = size;
        this.prettySize = new ByteSizeValue(size).toString();
    }

    public int getDocumentCount() {
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
