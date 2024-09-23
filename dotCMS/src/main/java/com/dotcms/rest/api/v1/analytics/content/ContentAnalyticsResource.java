package com.dotcms.rest.api.v1.analytics.content;

import com.dotcms.analytics.content.ContentAnalyticsAPI;
import com.dotcms.analytics.content.ReportResponse;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Resource class that exposes endpoints to query content analytics data.
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
@Path("/v1/analytics/content")
@Tag(name = "Content Analytics",
        description = "Endpoints that exposes information related to how content is accessed and interacted with by users.")
public class ContentAnalyticsResource {

    private final WebResource webResource;
    private final ContentAnalyticsAPI contentAnalyticsAPI;

    public ContentAnalyticsResource() {
        this(CDIUtils.getBean(ContentAnalyticsAPI.class).orElseGet(APILocator::getContentAnalyticsAPI));
    }

    //@Inject
    public ContentAnalyticsResource(final ContentAnalyticsAPI contentAnalyticsAPI) {
        this(new WebResource(), contentAnalyticsAPI);
    }

    @VisibleForTesting
    public ContentAnalyticsResource(final WebResource webResource,
                                    final ContentAnalyticsAPI contentAnalyticsAPI) {
        this.webResource = webResource;
        this.contentAnalyticsAPI = contentAnalyticsAPI;
    }

    @POST
    @Path("/_query")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ReportResponseEntityView query(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response,
                                          final QueryForm queryForm) {

        final InitDataObject initDataObject = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        Logger.debug(this, () -> "Querying content analytics data with the form: " + queryForm);
        final User user = initDataObject.getUser();
        final ReportResponse reportResponse = this.contentAnalyticsAPI.runReport(queryForm.getQuery(), user);
        return new ReportResponseEntityView(reportResponse);
    }

}
