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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
        summary = "Get asset information by path",
        description = "Retrieve detailed information and metadata for an asset (file or folder) using its path"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = WebAssetEntityView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getAssetsInfo(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Asset path information", required = true,
                       content = @Content(schema = @Schema(implementation = AssetLookupRequestForm.class)))
            AssetLookupRequestForm form
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
     * Download asset content by path, language and version
     */
    @Operation(
        summary = "Download asset file",
        description = "Download the binary content of an asset file by path, language and version"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset file download started successfully",
                    content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_download")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response download(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Asset download request information", required = true,
                       content = @Content(schema = @Schema(implementation = AssetsRequestForm.class)))
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
     * Upload or update an asset file
     */
    @Operation(
        summary = "Upload or update asset file",
        description = "Upload a new asset file or update an existing one at the specified path using multipart form data"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset uploaded/updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = WebAssetEntityView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid file or path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json"))
    })
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
     * Delete an asset permanently
     */
    @Operation(
        summary = "Delete asset permanently",
        description = "Permanently delete an asset (file) from the system using its path"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Insufficient permissions to delete asset",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_delete")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Asset deletion request information", required = true,
                       content = @Content(schema = @Schema(implementation = AssetDeletionRequestForm.class)))
            AssetDeletionRequestForm form
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
     * Archive an asset
     */
    @Operation(
        summary = "Archive asset",
        description = "Archive an asset (file) to make it inactive while preserving it in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset archived successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Insufficient permissions to archive asset",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_archive")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response archiveAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Asset archive request information", required = true,
                       content = @Content(schema = @Schema(implementation = AssetArchiveRequestForm.class)))
            AssetArchiveRequestForm form
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
     * Delete a folder permanently
     */
    @Operation(
        summary = "Delete folder permanently",
        description = "Permanently delete a folder and all its contents from the system using its path"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Insufficient permissions to delete folder",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/folders/_delete")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Folder deletion request information", required = true,
                       content = @Content(schema = @Schema(implementation = FolderDeletionRequestForm.class)))
            FolderDeletionRequestForm form
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
     * Create a new folder
     */
    @Operation(
        summary = "Create new folder",
        description = "Create a new folder at the specified path with the provided configuration details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request data or folder path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", 
                    description = "Folder already exists at the specified path",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/folders")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response createFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "New folder creation request with path and configuration", required = true,
                       content = @Content(schema = @Schema(implementation = NewFolderForm.class)))
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
     */
    @Operation(
        summary = "Update existing folder",
        description = "Update an existing folder's configuration details and optionally rename it"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request data or folder path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Insufficient permissions to update folder",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/folders")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response updateFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Folder update request with path and new configuration details", required = true,
                       content = @Content(schema = @Schema(implementation = UpdateFolderForm.class)))
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


    @Path("/drive")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response drive(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            ContentDriveSearchRequest form
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
        return Response.ok(helper.driveSearch(form, user)).build();
    }

}
