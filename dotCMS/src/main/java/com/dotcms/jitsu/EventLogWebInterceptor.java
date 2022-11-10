package com.dotcms.jitsu;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link WebInterceptor} intercepts both "/s/lib.js" and "/api/v1/event" and performs
 * different actions on each.
 * <p>
 * For GET requests to "/s/lib.js", it responds with the lib.js file
 * <p>
 * For POST requests to "/api/v1/event", it sends the event in JSON via {@link EventLogSubmitter}
 */
public class EventLogWebInterceptor implements WebInterceptor {

    private static final long serialVersionUID = 1L;
    private static final String[] PATHS = new String[] {
        "/s/lib.js",
        "/api/v1/event"
    };

    private final EventLogSubmitter submitter;
    private final Lazy<String> jitsuLib;

    public EventLogWebInterceptor() {
        submitter = new EventLogSubmitter();
        jitsuLib = Lazy.of(() -> {
            try (final InputStream in = this.getClass().getResourceAsStream("/jitsu/lib.js")) {
                return new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        });
    }

    @Override
    public String[] getFilters() {
        return PATHS;
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        setHeaders(response);

        if ("GET".equals(request.getMethod())) {
            doGet(request, response);
        }

        if ("POST".equals(request.getMethod())) {
            Try.run(() -> doPost(request, response));
        }

        if ("OPTIONS".equals(request.getMethod())) {
            doOptions(request, response);
        }

        return Result.SKIP_NO_CHAIN;
    }

    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        Logger.info(getClass(), "doGet");
        response.getWriter().append(jitsuLib.get());
    }

    public void doPost(final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, JSONException {
        if (request.getContentType() != null
            && request.getContentType().toLowerCase().contains("multipart/form-data")) {
            return;
        }

        final String payload = IOUtils.toString(request.getReader());
        final String realIp = request.getRemoteAddr();
        final JSONObject json = new JSONObject(payload);
        final String mungedIp = (realIp.lastIndexOf(".") > -1)
                ? realIp.substring(0, realIp.lastIndexOf(".")) + ".1"
                : "0.0.0.0";
        json.put("ip", mungedIp);
        json.put("clusterId", ClusterFactory.getClusterId());

        try {
            final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            this.submitter.logEvent(host, json.toString());
        } catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
            Logger.error(this, "Error resolving current host", e);
        } catch(Exception e) {
            Logger.error(this, "Could not submit event log", e);

        }
    }

    public void doOptions(final HttpServletRequest request, final HttpServletResponse response) {
        response.setHeader(CONTENT_LENGTH, "0");
        response.setStatus(200);
    }

    public void setHeaders(final HttpServletResponse response) {
        response.addHeader(CONTENT_TYPE, "application/javascript; charset=utf-8");
        response.addHeader("access-control-allow-credentials", "true");
        response.addHeader("access-control-allow-headers",
                "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, Host");
        response.addHeader("access-control-allow-methods", "POST, GET, OPTIONS, PUT, DELETE, UPDATE, PATCH");
        response.addHeader("access-control-allow-origin", "*");
        response.addHeader("access-control-max-age", "86400");
    }

}