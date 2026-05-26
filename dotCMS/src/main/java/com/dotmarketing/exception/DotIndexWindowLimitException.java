package com.dotmarketing.exception;

/**
 * Thrown when a search query exceeds the index's configured {@code max_result_window}
 * (default 10 000 in both Elasticsearch and OpenSearch). Callers should stop paginating
 * when they catch this exception — further offsets will not return results.
 */
public class DotIndexWindowLimitException extends DotRuntimeException {

    private static final long serialVersionUID = 1L;

    public DotIndexWindowLimitException(final Throwable cause) {
        super("Result window exceeded: query offset surpasses max_result_window", cause);
    }

    public DotIndexWindowLimitException(final String index, final Throwable cause) {
        super(String.format("Result window exceeded in index '%s': query offset surpasses max_result_window", index), cause);
    }
}
