package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * This Resource is for the push publishing filters
 */
@Path("/v1/pushpublish/filters")
public class PushPublishFilterResource {

    private final WebResource webResource;

    public PushPublishFilterResource(){
        this(new WebResource());
    }

    @VisibleForTesting
    public PushPublishFilterResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Lists all filters descriptors that the user role has access to.
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getFilters(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws DotDataException {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();

            final List<FilterDescriptor> list = APILocator.getPublisherAPI().getFiltersDescriptorsByRole(user);

        return Response.ok(new ResponseEntityView(list)).build();
    }

}
