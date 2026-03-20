package com.dotcms.content.index.domain;

import com.dotcms.annotations.Nullable;
import org.immutables.value.Value;

/**
 * Vendor-neutral result for a single item in a completed bulk operation.
 *
 * <p>Replaces direct use of {@code org.elasticsearch.action.bulk.BulkItemResponse}
 * (and its OpenSearch equivalent) in {@link IndexBulkListener} callbacks.
 * Vendor implementations map their library-specific item types to this DTO
 * inside the {@link com.dotcms.content.index.ContentletIndexOperations} adapter.</p>
 *
 * @author Fabrizzio Araya
 */
@Value.Immutable
public interface IndexBulkItemResult {

    /**
     * Raw document ID as returned by the vendor client, before any
     * caller-side trimming (e.g. stripping the {@code _languageId_variantId} suffix).
     * ES: {@code BulkItemResponse.getFailure().getId()} / {@code DocWriteResponse.getId()}.
     * OS: equivalent field on the OpenSearch bulk item.
     */
    String id();

    /** Whether this item failed to be indexed or deleted. */
    boolean failed();

    /**
     * Raw failure message when {@link #failed()} is {@code true}; {@code null} otherwise.
     * ES: {@code BulkItemResponse.getFailure().getMessage()}.
     */
    @Nullable
    String failureMessage();

    static ImmutableIndexBulkItemResult.Builder builder() {
        return ImmutableIndexBulkItemResult.builder();
    }
}
