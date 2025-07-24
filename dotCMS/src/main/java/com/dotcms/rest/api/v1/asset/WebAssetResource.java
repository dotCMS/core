package com.dotcms.rest.api.v1.asset;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.asset.view.WebAssetEntityView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
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
        summary = "Get asset information",
        description = "Retrieves metadata and information about assets (files and folders) at the specified path. Returns detailed asset information including permissions, modification dates, and size."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = WebAssetEntityView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid asset path or request parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAssetsInfo(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Asset information request form containing the asset path to query",
                       required = true,
                       content = @Content(schema = @Schema(implementation = AssetInfoRequestForm.class)))
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
    @Operation(
        summary = "Download asset content",
        description = "Downloads the binary content of an asset (file) by path, language and version. Returns the file as an octet stream with appropriate content disposition headers for download."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid asset path or request parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_download")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response download(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Asset download request form containing the asset path and optional version information",
                       required = true,
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
     * Push or update an asset by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Operation(
        summary = "Upload or update an asset",
        description = "Uploads a new asset or updates an existing asset at the specified path. Supports file uploads via multipart form data with metadata and binary content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset uploaded/updated successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request data or file upload parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to upload/update asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during upload",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveUpdateAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "File upload data containing asset path, file content, and metadata")
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
    @Operation(
        summary = "Delete an asset",
        description = "Permanently deletes an asset (file) at the specified path. This operation cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset deleted successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid asset path or request parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to delete asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_delete")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Asset deletion request form containing the asset path to delete",
                       required = true,
                       content = @Content(schema = @Schema(implementation = AssetInfoRequestForm.class)))
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
     * Archive an asset by path
     * @param request
     * @param response
     * @param form
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Operation(
        summary = "Archive an asset",
        description = "Archives an asset (file) at the specified path. Archived assets are moved to a non-published state but remain accessible for restoration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Asset archived successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid asset path or request parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to archive asset",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Asset not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/_archive")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response archiveAsset(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Asset archive request form containing the asset path to archive",
                       required = true,
                       content = @Content(schema = @Schema(implementation = AssetInfoRequestForm.class)))
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
    @Operation(
        summary = "Delete a folder",
        description = "Permanently deletes a folder at the specified path. This operation will also delete all assets and subfolders contained within the folder. This operation cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder deleted successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid folder path or request parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to delete folder",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found at the specified path",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/folders/_delete")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFolder(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Folder deletion request form containing the folder path to delete",
                       required = true,
                       content = @Content(schema = @Schema(implementation = AssetInfoRequestForm.class)))
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


}
