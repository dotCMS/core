package com.dotcms.content.index.domain;

import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of the result of an index creation operation.
 *
 * <p>Replaces {@code org.elasticsearch.client.indices.CreateIndexResponse} and
 * {@code org.opensearch.client.opensearch.indices.CreateIndexResponse} in the
 * public {@link com.dotcms.content.elasticsearch.business.IndexAPI} contract.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * CreateIndexStatus status = indexAPI.createIndex("myindex", 1);
 * if (!status.acknowledged()) {
 *     // retry or fail
 * }
 * </pre>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
public interface CreateIndexStatus {

    /**
     * Returns {@code true} if the index creation was acknowledged by the cluster.
     *
     * @return {@code true} if acknowledged
     */
    boolean acknowledged();

    static ImmutableCreateIndexStatus.Builder builder() {
        return ImmutableCreateIndexStatus.builder();
    }

    /**
     * Creates a {@code CreateIndexStatus} from an Elasticsearch {@code CreateIndexResponse}.
     *
     * @param esResponse the Elasticsearch response
     * @return a vendor-neutral status instance
     */
    static CreateIndexStatus from(org.elasticsearch.client.indices.CreateIndexResponse esResponse) {
        return builder().acknowledged(esResponse.isAcknowledged()).build();
    }

    /**
     * Creates a {@code CreateIndexStatus} from an OpenSearch {@code CreateIndexResponse}.
     *
     * @param osResponse the OpenSearch response
     * @return a vendor-neutral status instance
     */
    static CreateIndexStatus from(
            org.opensearch.client.opensearch.indices.CreateIndexResponse osResponse) {
        return builder().acknowledged(osResponse.acknowledged()).build();
    }
}
