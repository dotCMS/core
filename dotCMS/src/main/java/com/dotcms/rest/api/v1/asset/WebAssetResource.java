package com.dotcms.rest.api.v1.asset;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetEntityView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
@Tag(name = "Web Assets")
public class WebAssetResource {

    private final WebAssetHelper helper = WebAssetHelper.newInstance();

    /**
     * Get Assets and their metadata by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getAssetsInfo(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetInfoRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        Logger.debug(this,
                String.format("User [%s] is requesting assets info for path [%s]",
                        user.getUserId(), form.assetPath()));
        final WebAssetView asset = helper.getAssetInfo(form.assetPath(), user);
        return Response.ok(new WebAssetEntityView(asset)).build();
    }


    /**
     * Get a hold of the asset content for download by path, language and version
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/_download")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response download(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetsRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        Logger.debug(this,
                String.format("User [%s] is requesting asset content for download for path [%s]",
                        user.getUserId(), form.assetPath()));
        final File file = helper.getAsset(form, user).getFileAsset();
        return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .build();
    }


    /**
     * Push or update an asset by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Path("/")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response saveUpdateAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @BeanParam FileUploadData form
    ) throws DotSecurityException, DotDataException, IOException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();

        final WebAssetView webAssetView = helper.saveUpdateAsset(request, form, user);
        Logger.debug(this,
                String.format("User [%s] is uploading asset for path [%s]", user.getUserId(),
                        form.getAssetPath()));
        return Response.ok(new WebAssetEntityView(webAssetView)).build();
    }

    /**
     * Delete an asset by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Path("/_delete")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetInfoRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        helper.deleteAsset(form.assetPath(), user);
        Logger.info(this,
                String.format("User [%s] deleted asset for path [%s] ", user.getUserId(), form.assetPath()));
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }


    /**
     * Delete an asset by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Path("/_archive")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response archiveAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetInfoRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        helper.archiveAsset(form.assetPath(), user);
        Logger.info(this,
                String.format("User [%s] archived asset for path [%s] ", user.getUserId(),
                        form.assetPath()));
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }


    /**
     * Delete a folder by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Path("/folders/_delete")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            AssetInfoRequestForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        helper.deleteFolder(form.assetPath(), user);
        Logger.info(this,
                String.format("User [%s] deleted folder for path [%s]. ",
                        user.getUserId(), form.assetPath()));
        return Response.ok(new ResponseEntityBooleanView(true)).build();
    }

    /**
     * create a new folder
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/folders")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response createFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final NewFolderForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        final FolderView folder = helper.saveNewFolder(form.assetPath(), form.data(), user);
        return Response.ok(new ResponseEntityView<>(folder)).build();
    }

    /**
     * Update a folder by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/folders")
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response updateFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final UpdateFolderForm form
    ) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();
        final FolderView folder = helper.updateFolder(form.assetPath(), form.data(), user);
        return Response.ok(new ResponseEntityView<>(folder)).build();
    }

}
