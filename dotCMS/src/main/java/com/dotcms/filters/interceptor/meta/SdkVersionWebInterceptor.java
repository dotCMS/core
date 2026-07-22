package com.dotcms.filters.interceptor.meta;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.config.MinSdkVersion;
import com.liferay.portal.util.ReleaseInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Advertises the running dotCMS version and the oldest SDK version it still supports on
 * every response, via two static headers:
 *
 * <ul>
 *   <li>{@code X-DotCMS-Version} — this server's version ({@link ReleaseInfo#getVersion()}).</li>
 *   <li>{@code X-DotCMS-Min-SDK} — the oldest {@code @dotcms/*} SDK version this server
 *       still supports ({@link MinSdkVersion#VALUE}).</li>
 * </ul>
 *
 * <p>The {@code @dotcms/client} SDK reads these headers off responses it already makes to
 * warn when the installed SDK is older than what the server requires, or newer than the
 * server itself.
 *
 * <p>Implemented as a {@link WebInterceptor} (registered in
 * {@link com.dotmarketing.filters.InterceptorFilter}) rather than a JAX-RS
 * {@code ContainerResponseFilter}: a JAX-RS filter only covers requests routed through
 * Jersey, but SDK traffic also hits plain-servlet endpoints outside JAX-RS entirely — e.g.
 * GraphQL ({@code /api/v1/graphql}, served by {@code DotGraphQLHttpServlet}, a raw
 * {@code HttpServlet}, not a JAX-RS resource). {@code InterceptorFilter} is the first filter
 * in the pipeline and maps every request, so this covers both uniformly — the same reason
 * {@link ResponseMetaDataWebInterceptor} (the {@code x-dot-server} header) uses this same
 * mechanism instead of a JAX-RS filter.
 *
 * <p>Unrelated to CORS request handling — {@code Access-Control-Expose-Headers} already
 * defaults to {@code *} (see {@code dotmarketing-config.properties}), so these two headers
 * are already exposed cross-origin without any further change.
 */
public class SdkVersionWebInterceptor implements WebInterceptor {

    public static final String DOTCMS_VERSION_HEADER = "X-DotCMS-Version";
    public static final String DOTCMS_MIN_SDK_HEADER = "X-DotCMS-Min-SDK";

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            response.setHeader(DOTCMS_VERSION_HEADER, ReleaseInfo.getVersion());
            response.setHeader(DOTCMS_MIN_SDK_HEADER, MinSdkVersion.VALUE);
        } catch (Exception e) {
            // Never let a header-advertisement failure break the actual request.
        }

        return Result.NEXT;
    }

}
