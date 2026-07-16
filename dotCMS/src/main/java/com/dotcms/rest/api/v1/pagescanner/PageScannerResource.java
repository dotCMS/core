package com.dotcms.rest.api.v1.pagescanner;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * REST resource that proxies requests to the remote Page Scanner service for
 * accessibility (a11y) and geo checks. Both endpoints require an authenticated
 * backend user and inject a short-lived JWT so the upstream service can
 * call back into dotCMS on behalf of the requesting user.
 */
@Path("/v1/page-scanner")
@Tag(name = "Accessibility Checker", description = "Web accessibility checking and compliance")
public class PageScannerResource {

    static final String APP_KEY = "dotPageScanner-config";

    /**
     * Backward-compatible property name retained so existing references
     * (for example, configuration whitelists in other resources) continue
     * to compile while the effective configuration is stored in App secrets.
     */
    @Deprecated
    public static final String API_URL_PROPERTY = "PAGE_SCANNER_API_URL";
    static final String DEFAULT_API_URL =
            "https://a11y.api.dotcms.site";

    private static final String NOT_CONFIGURED_MSG =
            "Page Scanner service is not available.";

    private final WebResource webResource;
    private final HttpClient httpClient;

    public PageScannerResource() {
        this.webResource = new WebResource();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /** Package-private constructor for unit tests. */
    PageScannerResource(final WebResource webResource, final HttpClient httpClient) {
        this.webResource = webResource;
        this.httpClient = httpClient;
    }

    /**
     * Proxies a POST request to the upstream {@code /a11y/check} endpoint.
     *
     * @param request  the HTTP servlet request
     * @param response the HTTP servlet response
     * @param body     JSON object containing at minimum {@code { "url": "..." }}
     * @return the upstream response, status code preserved
     */
    @POST
    @Path("/a11y/check")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response a11yCheck(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final PageScanCheckForm body) {

        return proxyCheck(request, response, body, CheckType.a11y);
    }

    /**
     * Proxies a POST request to the upstream {@code /geo/check} endpoint.
     *
     * @param request  the HTTP servlet request
     * @param response the HTTP servlet response
     * @param body     JSON object containing {@code { "url": "..." }}
     * @return the upstream response, status code preserved
     */
    @POST
    @Path("/geo/check")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response geoCheck(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final PageScanCheckForm body) {

        return proxyCheck(request, response, body, CheckType.geo);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Response proxyCheck(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final PageScanCheckForm body,
            final CheckType checkType) {

        // Require authenticated backend user
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Host currentHost = Try.<Host>of(
                () -> com.dotmarketing.business.web.WebAPILocator.getHostWebAPI().getCurrentHost(request))
                .getOrElse(APILocator.systemHost());

        final Optional<AppSecrets> appSecretsOpt = Try.of(
                () -> APILocator.getAppsAPI().getSecrets(APP_KEY, true,
                        currentHost, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (appSecretsOpt.isEmpty()) {
            Logger.warn(PageScannerResource.class,
                    "Page Scanner App is not configured in the Apps portlet.");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ResponseEntityView<>(new ErrorEntity("PAGE_SCANNER_NOT_CONFIGURED", NOT_CONFIGURED_MSG)))
                    .build();
        }

        final Map<String, Secret> secrets = appSecretsOpt.get().getSecrets();
        final String apiUrl = sanitizeSecret(Try.of(() -> secrets.get("apiUrl").getString())
                .getOrElse(DEFAULT_API_URL));
        final String apiAuthToken = sanitizeSecret(Try.of(() -> secrets.get("apiAuthToken").getString())
                .getOrElse((String) null));

        if (!UtilMethods.isSet(apiUrl) || !UtilMethods.isSet(apiAuthToken)) {
            Logger.warn(PageScannerResource.class,
                    "Page Scanner App is missing required configuration: apiUrl and apiAuthToken must be set.");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ResponseEntityView<>(new ErrorEntity("PAGE_SCANNER_NOT_CONFIGURED", NOT_CONFIGURED_MSG)))
                    .build();
        }

        final User user = initData.getUser();
        final String pageUrl = body != null && body.getUrl() != null ? body.getUrl() : "";

        // Generate a short-lived JWT for the current user
        final String shortLivedToken = generateShortLivedToken(user, request);

        if (!UtilMethods.isSet(shortLivedToken)) {
            Logger.error(PageScannerResource.class,
                    "Failed to generate short-lived token for user: " + user.getUserId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(new ErrorEntity("TOKEN_GENERATION_FAILED", "Unable to generate authentication token.")))
                    .build();
        }

        // Build the JSON payload for the upstream service
        final String upstreamPayload = buildPayload(pageUrl, shortLivedToken);

        // Build the upstream URL
        final String upstreamUrl = buildUpstreamUrl(apiUrl, checkType);

        Logger.debug(PageScannerResource.class,
                "Forwarding " + checkType.pathSegment() + " check to: " + upstreamUrl);

        return forwardRequest(upstreamUrl, upstreamPayload, apiAuthToken);
    }

    private String generateShortLivedToken(final User user, final HttpServletRequest request) {
        try {
            final long ttlMs = Config.getLongProperty("DOT_PAGE_SCANNER_TOKEN_TTL_MS", 5L * 60L * 1000L);
            final Date expiry = new Date(System.currentTimeMillis() + ttlMs);
            final String ipAddress = request.getRemoteAddr();

            final ApiToken apiToken = APILocator.getApiTokenAPI()
                    .persistApiToken(user.getUserId(), expiry, user.getUserId(), ipAddress, "page-scanner-short-lived");

            return APILocator.getApiTokenAPI().getJWT(apiToken, user);
        } catch (Exception e) {
            Logger.error(PageScannerResource.class,
                    "Error generating short-lived token: " + e.getMessage(), e);
            return null;
        }
    }

    private String buildPayload(final String url, final String shortLivedToken) {
        // Simple JSON construction — avoids pulling in a full mapper dependency
        final String safeUrl   = escapeJson(url);
        final String safeToken = escapeJson(shortLivedToken);
        return "{\"url\":\"" + safeUrl + "\",\"shortLivedToken\":\"" + safeToken + "\"}";
    }

    private String buildUpstreamUrl(final String apiUrl, final CheckType checkType) {
        final String base = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        return base + "/" + checkType.pathSegment() + "/check";
    }

    private Response forwardRequest(
            final String upstreamUrl,
            final String payload,
            final String authToken) {

        try {
            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(upstreamUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("auth-token", authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            final HttpResponse<String> upstreamResponse =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            final int upstreamStatus = upstreamResponse.statusCode();
            Logger.debug(PageScannerResource.class,
                    "Upstream responded with status: " + upstreamStatus);

            // Never forward upstream auth errors — a 401/403 from the external
            // service would trigger client-side logout since the browser cannot
            // distinguish it from a dotCMS session error.
            if (upstreamStatus == 401 || upstreamStatus == 403) {
                Logger.warn(PageScannerResource.class,
                        "Upstream Page Scanner returned " + upstreamStatus + " — check apiAuthToken in the Page Scanner App configuration");
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(new ResponseEntityView<>(new ErrorEntity("PAGE_SCANNER_AUTH_FAILED", "Page Scanner service authentication failed.")))
                        .build();
            }

            return Response.status(upstreamStatus)
                    .entity(upstreamResponse.body())
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            Logger.error(PageScannerResource.class,
                    "Network error forwarding to upstream Page Scanner: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ResponseEntityView<>(new ErrorEntity("PAGE_SCANNER_UNREACHABLE", "Unable to reach the Page Scanner service.")))
                    .build();
        }
    }

    /**
     * Cleans a secret value read from the Apps portfolio before it is used in an
     * outbound HTTP header or URL. Hidden secret fields are pasted blind, so a
     * line-wrapped or trailing newline easily slips in. Java's {@link HttpRequest}
     * rejects any header value char that is not a valid RFC 7230 field-vchar
     * (see {@code jdk.internal.net.http.common.Utils#isValidValue}): control
     * chars below {@code 0x20}, DEL ({@code 0x7F}), and anything above
     * {@code 0xFF} (smart quotes, em-dashes, zero-width spaces, BOM, etc.). A
     * blind paste into a hidden secret field can carry any of these, so we strip
     * every disallowed char before the value reaches the header, then trim
     * surrounding whitespace.
     */
    private String sanitizeSecret(final String value) {
        if (value == null) {
            return null;
        }
        // Keep only chars Java's HttpRequest accepts in a header value: the
        // printable range 0x20-0xFF minus DEL (0x7F). Everything else — control
        // chars (0x00-0x1F), DEL, and any char above 0xFF — is dropped. Then trim
        // surrounding whitespace (0x20 / tab).
        return value.replaceAll("[^\\u0020-\\u007E\\u0080-\\u00FF]", "").trim();
    }

    /**
     * Minimal JSON string escaping to prevent injection in the payload JSON.
     */
    private String escapeJson(final String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
