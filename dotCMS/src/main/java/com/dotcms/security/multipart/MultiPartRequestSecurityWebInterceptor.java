package com.dotcms.security.multipart;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

/**
 * This web interceptor checks if the current request is a POST or PUT and multipart.
 * If it is, the request is wrapped so that every part file name is checked before any endpoint
 * reads it. The media type is matched without regard to letter case, as HTTP defines it.
 * @author jsanca
 */
public class MultiPartRequestSecurityWebInterceptor implements WebInterceptor {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final String[] BLOCK_REQUESTS = {
            Config.getStringProperty("MULTI_PART_INTERCEPTOR_BLOCK_REQUESTS","/*")
    };

    public MultiPartRequestSecurityWebInterceptor() {

    }

    @Override
    public String[] getFilters() {

        return BLOCK_REQUESTS;
    }

    /**
     * Wraps multipart POST and PUT requests in a {@link MultiPartSecurityRequestWrapper}, which
     * validates the file names in the body. Any other request continues untouched.
     *
     * @param request  the current request
     * @param response the current response
     * @return a result that continues the chain, carrying the wrapped request when it is multipart
     * @throws IOException if the multipart body cannot be read for validation
     */
    @Override
    public Result intercept(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        final String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {

            final String contentTypeHeader = request.getHeader("content-type");
            if (isMultipartFormData(contentTypeHeader)) {

                final MultiPartSecurityRequestWrapper requestWrapper = new MultiPartSecurityRequestWrapper(request);

                return new Result.Builder().wrap(requestWrapper).next().build();
            }
        }

        return Result.NEXT;
    }

    /**
     * Tells whether a Content-Type header announces a multipart form upload. Media types are
     * case-insensitive, so {@code Multipart/Form-Data} counts the same as the lower-case spelling.
     *
     * @param contentTypeHeader the raw Content-Type header value, possibly null
     * @return {@code true} if the header names the multipart/form-data media type
     */
    static boolean isMultipartFormData(final String contentTypeHeader) {
        return UtilMethods.isSet(contentTypeHeader)
                && contentTypeHeader.toLowerCase(Locale.ROOT).contains(MULTIPART_FORM_DATA);
    }
}
