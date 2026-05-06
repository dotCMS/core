package com.dotcms.cdn.rest;

import com.dotcms.cdn.api.DotCDNAPI;
import com.dotcms.cdn.api.DotCDNStats;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/dotcdn")
public class DotCDNResource {

    private static final int DEFAULT_MAX_PURGE_URLS = 1000;
    private static final int MAX_PURGE_URL_LENGTH = 2048;

    /**
     * This endpoint is to get the statistics from the CDN.
     * We get the stats of a range of dates.
     *
     * The hostId property is to get the AppSecret Configuration. If the hostId is not send will
     * try to get it from the session.
     *
     * @param dateFromStr date from we should get the stats, format yyyy-MM-dd
     * @param dateToStr date until we should get the stats, format: yyyy-MM-dd
     * @param hostId  Id of the host which App config we should get.
     * @return stats response
     */
    @GET
    @Path("/stats")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get CDN statistics",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "CDN statistics for the requested date range",
                    content = @Content(
                            schema = @Schema(implementation = ResponseEntityDotCDNStatsView.class))))
    public final Response getStats(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("dateFrom") final String dateFromStr,
            @QueryParam("dateTo") final String dateToStr,
            @QueryParam("hostId") final String hostId,
            @QueryParam("hourly") final boolean hourly) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true)
                .requiredPortlet("dotCDN").init().getUser();

        final Lazy<Host> lazyCurrentHost = Lazy.of(() -> Try.of(
                () -> Host.class.cast(request.getSession().getAttribute(WebKeys.CURRENT_HOST)))
                .getOrNull());
        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(hostId, user, false)).getOrElse(lazyCurrentHost.get());

        return Response.ok(new ResponseEntityDotCDNStatsView(
                new DotCDNStatsResponse(DotCDNAPI.api(host).getStats(dateFromStr, dateToStr, hourly))))
                .build();
    }

    /**
     * This endpoint is to purgeCache of the CDN.
     * You can purge the entire cache by setting the invalidateAll to true
     * Or you can purge specific urls by sending them in a Array.
     *
     * The hostId property is to get the AppSecret Configuration. If the hostId is not send will
     * try to get it from the session.
     *
     * @param invalidationForm request body
     * @return a message if the purged was successful.
     */
    @DELETE
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Purge CDN cache",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "CDN purge result",
                    content = @Content(
                            schema = @Schema(implementation = ResponseEntityDotCDNPurgeView.class))))
    public final Response purgeCache(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final InvalidationForm invalidationForm) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true)
                .requiredPortlet("dotCDN").init().getUser();

        final Lazy<Host> lazyCurrentHost = Lazy.of(() -> Try.of(
                () -> Host.class.cast(request.getSession().getAttribute(WebKeys.CURRENT_HOST)))
                .getOrNull());
        if (invalidationForm == null) {
            throw new BadRequestException("Invalidation form is required");
        }

        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(invalidationForm.getHostId(), user, false))
                .getOrElse(lazyCurrentHost.get());

        final DotCDNAPI cdnApi = DotCDNAPI.api(host);

        if (invalidationForm.isInvalidateAll()) {
            Logger.info(this.getClass().getName(),
                    "User:" + user.getUserId() + " purging entire cache");
            return Response.ok(new ResponseEntityDotCDNPurgeView(
                    new DotCDNPurgeResponse(cdnApi.invalidateAll(), true))).build();
        }

        final List<String> urls = validateUrls(invalidationForm.getUrls());
        return Response.ok(new ResponseEntityDotCDNPurgeView(
                new DotCDNPurgeResponse(cdnApi.invalidate(urls), false))).build();
    }

    private List<String> validateUrls(final List<String> urls) {
        if (!UtilMethods.isSet(urls)) {
            throw new BadRequestException("At least one URL is required");
        }

        final int maxPurgeUrls = Config.getIntProperty(
                "DOT_CDN_MAX_PURGE_URLS", DEFAULT_MAX_PURGE_URLS);
        if (urls.size() > maxPurgeUrls) {
            throw new BadRequestException("A maximum of " + maxPurgeUrls + " URLs can be purged");
        }

        final List<String> normalizedUrls = urls.stream()
                .filter(UtilMethods::isSet)
                .map(String::trim)
                .collect(Collectors.toList());

        if (normalizedUrls.size() != urls.size()) {
            throw new BadRequestException("Purge URLs cannot be blank");
        }

        for (final String url : normalizedUrls) {
            if (!isValidPurgeUrl(url)) {
                throw new BadRequestException("Invalid purge URL: " + url);
            }
        }

        return normalizedUrls;
    }

    private boolean isValidPurgeUrl(final String url) {
        if (url.length() > MAX_PURGE_URL_LENGTH || containsControlCharacter(url)) {
            return false;
        }

        if (url.startsWith("/")) {
            return true;
        }

        return Try.of(() -> URI.create(url))
                .map(uri -> UtilMethods.isSet(uri.getHost())
                        && ("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme())))
                .getOrElse(false);
    }

    private boolean containsControlCharacter(final String url) {
        for (int i = 0; i < url.length(); i++) {
            if (Character.isISOControl(url.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    @Schema(description = "CDN statistics response")
    public static class DotCDNStatsResponse {

        private final DotCDNStats stats;

        public DotCDNStatsResponse(final DotCDNStats stats) {
            this.stats = stats;
        }

        public DotCDNStats getStats() {
            return stats;
        }
    }

    @Schema(description = "CDN purge response")
    public static class DotCDNPurgeResponse {

        private final boolean success;
        private final boolean invalidateAll;

        public DotCDNPurgeResponse(final boolean success, final boolean invalidateAll) {
            this.success = success;
            this.invalidateAll = invalidateAll;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isInvalidateAll() {
            return invalidateAll;
        }
    }

    public static class ResponseEntityDotCDNStatsView
            extends ResponseEntityView<DotCDNStatsResponse> {

        public ResponseEntityDotCDNStatsView(final DotCDNStatsResponse entity) {
            super(entity);
        }
    }

    public static class ResponseEntityDotCDNPurgeView
            extends ResponseEntityView<DotCDNPurgeResponse> {

        public ResponseEntityDotCDNPurgeView(final DotCDNPurgeResponse entity) {
            super(entity);
        }
    }
}
