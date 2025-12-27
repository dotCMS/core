package com.dotcms.content.elasticsearch.business;

import java.util.Map;

public interface IndicesInfo {

    String OPEN_SEARCH_VERSION = "3.x";

    String getLive();

    String getWorking();

    String getReindexLive();

    String getReindexWorking();

    String getSiteSearch();

    String version();

    default boolean isLegacy(){ return !OPEN_SEARCH_VERSION.equals(version()); }

    long getIndexTimeStamp(IndexType indexType);

    Map<IndexType, String> asMap();
}
