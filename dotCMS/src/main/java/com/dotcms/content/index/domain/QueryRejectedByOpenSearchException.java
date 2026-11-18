package com.dotcms.content.index.domain;

import com.dotmarketing.business.DotStateException;

/**
 * OpenSearch parsed a search request and rejected it as malformed (HTTP 400 with a parse error).
 *
 * <p>Not the same as {@link InvalidSearchQueryException}. A query that is not even JSON fails on
 * every engine, so it is the caller's error. A query OpenSearch 3 refuses may still be valid for
 * Elasticsearch 7, which accepts older syntax OpenSearch dropped. At Phase 2 such a query still
 * falls back to Elasticsearch and succeeds; what changes is that the log names it as a query that
 * will stop working at Phase 3, rather than blaming the index (issue #37637).</p>
 *
 * <p>Extends {@link DotStateException}, the type these failures were raised as before, so existing
 * callers and the REST exception mapper see no difference.</p>
 */
public class QueryRejectedByOpenSearchException extends DotStateException {

    private static final long serialVersionUID = 1L;

    public QueryRejectedByOpenSearchException(final String message) {
        super(message);
    }
}
