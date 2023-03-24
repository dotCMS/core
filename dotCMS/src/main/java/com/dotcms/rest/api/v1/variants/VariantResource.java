package com.dotcms.rest.api.v1.variants;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import  javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * REST API for {@link com.dotcms.variant.model.Variant}
 */
@Path("/v1/variants")
@Tag(name = "Variant")
public class VariantResource {

    private final WebResource webResource;

    public VariantResource() {
        webResource =  new WebResource();
    }

    @PUT
    @Path("/{variantName}/_promote")
    @JSONP
    @NoCache
    public Response promote(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("variantName") final String variantName) throws DotDataException {

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Variant variant = APILocator.getVariantAPI().get(variantName)
                .orElseThrow(() -> new NotFoundException("Variant not found: " + variantName));

        APILocator.getVariantAPI().promote(variant, user);

        return Response.ok().build();
    }

    private InitDataObject getInitData(@Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
    }
}
