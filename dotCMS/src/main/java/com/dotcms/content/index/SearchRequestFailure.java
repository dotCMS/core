package com.dotcms.content.index;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Keeps a swallowed template search failure from poisoning page or fragment caches. */
public final class SearchRequestFailure {
    private static final String ATTRIBUTE = SearchRequestFailure.class.getName();

    /** Utility class. */
    private SearchRequestFailure() { }

    /** Marks only this request; no additional thread-local state survives the request. */
    public static SearchUnavailableException record(final SearchUnavailableException failure) {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (request != null) {
            request.setAttribute(ATTRIBUTE, failure);
        }
        return failure;
    }

    /** Stops rendering before emitting or caching output when a helper swallowed the failure. */
    public static void check() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (request != null && request.getAttribute(ATTRIBUTE) instanceof SearchUnavailableException) {
            throw (SearchUnavailableException) request.getAttribute(ATTRIBUTE);
        }
    }

    /** Sends a direct 503, or aborts already-started output rather than completing a partial page. */
    public static void respond(final HttpServletResponse response) throws IOException {
        Logger.warnEvery(SearchRequestFailure.class, "search-unavailable",
                "Page rendering temporarily unavailable; inspect search admission metrics", 5000);
        if (response.isCommitted()) {
            throw new IOException("Search unavailable after the response was committed");
        }
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Retry-After", "1");
        response.setContentType("text/plain;charset=UTF-8");
        response.getOutputStream().write("Search temporarily unavailable. Please retry later."
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
