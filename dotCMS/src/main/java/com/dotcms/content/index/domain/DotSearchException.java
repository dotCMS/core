package com.dotcms.content.index.domain;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Vendor-neutral search exception for the index abstraction layer.
 *
 * <p>Replaces {@code org.elasticsearch.ElasticsearchException} on the public surface of the
 * search/site-search APIs so that callers — and the interfaces themselves — no longer couple to
 * Elasticsearch (or any other engine) types. It is the neutral failure signal raised by both the
 * Elasticsearch and OpenSearch providers when a search or index operation cannot be completed.</p>
 *
 * <p>It extends {@link DotRuntimeException} (and therefore is unchecked) to mirror the unchecked
 * nature of {@code ElasticsearchException}: existing callers that never declared a {@code catch}
 * for the vendor exception keep compiling unchanged.</p>
 */
public class DotSearchException extends DotRuntimeException {

    private static final long serialVersionUID = 1L;

    public DotSearchException(final String message) {
        super(message);
    }

    public DotSearchException(final Throwable cause) {
        super(cause);
    }

    public DotSearchException(final String message, final Throwable cause) {
        super(message, cause);
    }
}