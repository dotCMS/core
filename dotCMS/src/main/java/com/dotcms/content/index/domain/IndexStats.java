package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of index statistics.
 *
 * <p>Replaces both {@code com.dotcms.content.elasticsearch.business.IndexStats} and
 * {@code com.dotcms.content.index.opensearch.IndexStats} with a single domain DTO
 * that is independent of the underlying search engine.</p>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableIndexStats.class)
@JsonDeserialize(as = ImmutableIndexStats.class)
public interface IndexStats {

    /** Returns the index name (without cluster prefix). */
    String indexName();

    /** Returns the total document count for this index. */
    long documentCount();

    /** Returns the raw size of the index in bytes. */
    long sizeRaw();

    /** Returns the human-readable formatted size (e.g., "1.2mb"). */
    String size();

    static ImmutableIndexStats.Builder builder() {
        return ImmutableIndexStats.builder();
    }

    /**
     * Creates an {@code IndexStats} from an Elasticsearch {@code IndexStats} object.
     *
     * @param esStats the Elasticsearch IndexStats
     * @return a vendor-neutral IndexStats instance
     */
    static IndexStats from(com.dotcms.content.elasticsearch.business.IndexStats esStats) {
        return builder()
                .indexName(esStats.getIndexName())
                .documentCount(esStats.getDocumentCount())
                .sizeRaw(esStats.getSizeRaw())
                .size(esStats.getSize())
                .build();
    }

    /**
     * Creates an {@code IndexStats} from an OpenSearch {@code IndexStats} object.
     *
     * @param osStats the OpenSearch IndexStats
     * @return a vendor-neutral IndexStats instance
     */
    static IndexStats from(com.dotcms.content.index.opensearch.IndexStats osStats) {
        return builder()
                .indexName(osStats.getIndexName())
                .documentCount(osStats.getDocumentCount())
                .sizeRaw(osStats.getSizeRaw())
                .size(osStats.getSize())
                .build();
    }
}
