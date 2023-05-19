package com.dotcms.rest.api.v1.asset;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.asset.view.WebAssetEntityView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * Web Assets Resource
 * <p> This resource is responsible for handling requests for web assets. </p>
 * <p> An Asset is a File or Folder </p>
 */
@Path("/v1/assets")
public class WebAssetResource {

    private final WebAssetHelper helper = WebAssetHelper.newInstance();

    @Path("/")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getAssetsInfo(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetsRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        Logger.info(this,
                String.format("User [%s] is requesting assets info for path [%s]", user.getUserId(),
                        form.assetPath()));
        final WebAssetView asset = helper.getAsset(form.assetPath(), user);
        return Response.ok(new WebAssetEntityView(asset)).build();
    }
}
