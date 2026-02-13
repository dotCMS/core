package com.dotcms.content.index;

import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.domain.TotalHits;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;

public interface IndexSourceHelper {

    static boolean isOpenSearchReadEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_READ, false);
    }

    static boolean isOpenSearchWriteEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_WRITE, false);
    }

    static boolean isOpenSearchEnabled() {
        return isOpenSearchReadEnabled() || isOpenSearchWriteEnabled();
    }

    /**
     * Converts Elasticsearch SearchHits to dotCMS SearchHits wrapper.
     *
     * @param esSearchHits the Elasticsearch SearchHits
     * @return the dotCMS SearchHits wrapper
     */
    static SearchHits wrapSearchHits(org.elasticsearch.search.SearchHits esSearchHits) {
        return SearchHits.from(esSearchHits);
    }

    /**
     * Converts Elasticsearch SearchHit to dotCMS SearchHit wrapper.
     *
     * @param esSearchHit the Elasticsearch SearchHit
     * @return the dotCMS SearchHit wrapper
     */
    static SearchHit wrapSearchHit(org.elasticsearch.search.SearchHit esSearchHit) {
        return SearchHit.from(esSearchHit);
    }

    /**
     * Converts OpenSearch Hit to dotCMS SearchHit wrapper.
     *
     * @param osHit the OpenSearch Hit
     * @return the dotCMS SearchHit wrapper
     */
    static SearchHit wrapOpenSearchHit(org.opensearch.client.opensearch.core.search.Hit<?> osHit) {
        return SearchHit.from(osHit);
    }

    /**
     * Converts Elasticsearch TotalHits to dotCMS TotalHits wrapper.
     *
     * @param esTotalHits the Elasticsearch TotalHits
     * @return the dotCMS TotalHits wrapper
     */
    static TotalHits wrapTotalHits(org.apache.lucene.search.TotalHits esTotalHits) {
        return TotalHits.from(esTotalHits);
    }

    /**
     * Creates an empty SearchHits instance.
     *
     * @return an empty SearchHits
     */
    static SearchHits emptySearchHits() {
        return SearchHits.empty();
    }

}
