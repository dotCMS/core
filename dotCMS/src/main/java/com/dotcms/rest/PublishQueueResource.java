package com.dotcms.rest;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.json.JSONException;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Tag(name = "Publishing")
@Path("/v1/publishqueue")
public class PublishQueueResource {

    private final WebResource            webResource            = new WebResource();
    private final BundleAPI              bundleAPI              = APILocator.getBundleAPI();
    private final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();


    /**
     * Deletes elements {@link com.dotcms.publisher.business.PublishQueueElement} from the Push Publish Queue
     *
     * @param request
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     */
    @DELETE
    @Produces("application/json")
    public Response deleteAssetsByIdentifiers(@Context   final HttpServletRequest request,
            @Context   final HttpServletResponse response,
            final DeletePPQueueElementsByIdentifierForm  deletePPQueueElementsByIdentifierForm)
            throws DotPublisherException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet("publishing-queue")
                .init();

        PublisherAPI.getInstance().deleteElementsFromPublishQueueTable(
                deletePPQueueElementsByIdentifierForm.getIdentifiers(),
                0);

        return Response.ok(new ResponseEntityView(
                "Requested elements were removed from the Push-Publish Queue")).build();
    }
}
