package com.dotcms.cdn.rest;

import com.dotcms.cdn.api.DotCDNAPI;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.Map;
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
public class DotCDNResource implements Serializable {

    private static final long serialVersionUID = 204840922704940654L;

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
    public final Response getStats(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @QueryParam("dateFrom") final String dateFromStr,
            @QueryParam("dateTo") final String dateToStr,
            @QueryParam("hostId") final String hostId) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true)
                .requiredPortlet("dotCDN").init().getUser();

        final Lazy<Host> lazyCurrentHost = Lazy.of(() -> Try.of(
                () -> Host.class.cast(request.getSession().getAttribute(WebKeys.CURRENT_HOST)))
                .getOrNull());
        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(hostId, user, false)).getOrElse(lazyCurrentHost.get());

        return Response.ok(new ResponseEntityView(
                Map.of("stats", DotCDNAPI.api(host).getStats(dateFromStr, dateToStr)))).build();
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
    public final Response purgeCache(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final InvalidationForm invalidationForm) {

        final User user = new WebResource.InitBuilder(request, response)
                .rejectWhenNoUser(true).requiredBackendUser(true)
                .requiredPortlet("dotCDN").init().getUser();

        final Lazy<Host> lazyCurrentHost = Lazy.of(() -> Try.of(
                () -> Host.class.cast(request.getSession().getAttribute(WebKeys.CURRENT_HOST)))
                .getOrNull());
        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(invalidationForm.getHostId(), user, false))
                .getOrElse(lazyCurrentHost.get());

        final DotCDNAPI cdnApi = DotCDNAPI.api(host);

        if (invalidationForm.isInvalidateAll()) {
            Logger.info(this.getClass().getName(),
                    "User:" + user.getUserId() + " purging entire cache");
            return Response.ok(new ResponseEntityView(
                    Map.of("Entire Cache Purged: ", cdnApi.invalidateAll()))).build();
        }

        return Response.ok(new ResponseEntityView(
                Map.of("All Urls Sent Purged: ",
                        cdnApi.invalidate(invalidationForm.getUrls())))).build();
    }
}
