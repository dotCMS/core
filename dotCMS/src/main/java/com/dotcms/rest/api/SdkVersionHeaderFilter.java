package com.dotcms.rest.api;

import com.dotcms.rest.config.MinSdkVersion;
import com.dotmarketing.util.Logger;
import com.liferay.portal.util.ReleaseInfo;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Advertises the running dotCMS version and the oldest SDK version it still supports on
 * every REST response, via two static headers:
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
 * <p>Unrelated to CORS request handling — see {@link CorsFilter} for
 * {@code Access-Control-Expose-Headers}, which already defaults to {@code *} and therefore
 * already exposes these two headers cross-origin without any further change.
 */
@Provider
public class SdkVersionHeaderFilter implements ContainerResponseFilter {

    public static final String DOTCMS_VERSION_HEADER = "X-DotCMS-Version";
    public static final String DOTCMS_MIN_SDK_HEADER = "X-DotCMS-Min-SDK";

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws IOException {
        try {
            final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            addHeaderIfAbsent(headers, DOTCMS_VERSION_HEADER, ReleaseInfo.getVersion());
            addHeaderIfAbsent(headers, DOTCMS_MIN_SDK_HEADER, MinSdkVersion.VALUE);
        } catch (Exception e) {
            // Never let a header-advertisement failure break the actual response.
            Logger.debug(this.getClass(), () -> "Unable to set SDK compatibility headers: " + e.getMessage());
        }
    }

    private void addHeaderIfAbsent(final MultivaluedMap<String, Object> headers, final String name, final String value) {
        final List<Object> values = new ArrayList<>();
        values.add(value);
        headers.putIfAbsent(name, values);
    }

}
