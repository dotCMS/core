package com.dotcms.content.index.domain;

import com.dotmarketing.business.DotStateException;

/**
 * The caller sent a search the engine cannot run: a query that is not valid JSON, or one the engine
 * rejected as malformed (HTTP 400).
 *
 * <p>Kept apart from availability failures because the right reaction differs. A failure to reach
 * or read an index is what the Phase 2 read fallback exists for; an invalid query fails the same way
 * on every engine, so retrying it elsewhere only doubles the work and, worse, logs it as an index
 * problem (issue #37637).</p>
 *
 * <p>Extends {@link DotStateException}, the type these failures were raised as before, so existing
 * callers and the REST exception mapper see no difference.</p>
 */
public class InvalidSearchQueryException extends DotStateException {

    private static final long serialVersionUID = 1L;

    public InvalidSearchQueryException(final String message) {
        super(message);
    }

    public InvalidSearchQueryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
