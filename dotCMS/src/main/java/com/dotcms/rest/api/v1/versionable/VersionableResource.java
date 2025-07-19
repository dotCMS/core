package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Resource to interact with versions for Contentlet, Templates, Containers, Links, etc
 * @author erickgonzalez
 */
@SwaggerCompliant(value = "Publishing and content distribution APIs", batch = 5)
@Tag(name = "Versionables")
@Path("/v1/versionables")
public class VersionableResource {

    private final WebResource       webResource;
    private final VersionableHelper versionableHelper;

    public VersionableResource() {
        this(new WebResource(), APILocator.getTemplateAPI(),
                APILocator.getContentletAPI(), APILocator.getPermissionAPI(),
                APILocator.getContainerAPI());
    }

    @VisibleForTesting
    public VersionableResource(final WebResource     webResource,
            final TemplateAPI        templateAPI,
            final ContentletAPI      contentletAPI,
            final PermissionAPI      permissionAPI,
            final ContainerAPI containerAPI) {

        this.webResource    = webResource;
        this.versionableHelper = new VersionableHelper(templateAPI, contentletAPI,
                permissionAPI, containerAPI);
    }

    @Operation(
        summary = "Delete version",
        description = "Deletes a specific version of a versionable asset. The version cannot be working or live, and user needs edit permissions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Version deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityVersionableOperationView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - version is working or live",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Version not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{versionableInode}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteVersion(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Versionable asset inode", required = true) @PathParam("versionableInode") final String versionableInode)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Deleting the version by inode: " + versionableInode);

        final String type = Try.of(()->InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null == type) {

            throw new DoesNotExistException("The versionable inode: " + versionableInode + " does not exist");
        }

        this.versionableHelper.getAssetTypeByVersionableDeleteMap().getOrDefault(type,
                this.versionableHelper.getDefaultVersionableDeleteStrategy())
                .deleteVersionByInode(versionableInode, user, mode.respectAnonPerms);

        return Response.ok(new ResponseEntityVersionableOperationView("Version " + versionableInode + " deleted successfully")).build();
    }

    @Operation(
        summary = "Find versionable",
        description = "Finds versionable asset by UUID. If UUID is an inode, returns specific version; if identifier, returns all versions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Versionable found successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityVersionableView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Versionable not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{versionableInodeOrIdentifier}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response findVersionable(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Versionable asset inode or identifier", required = true) @PathParam("versionableInodeOrIdentifier") final String versionableInodeOrIdentifier)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, () -> "Finding the version: " + versionableInodeOrIdentifier);

        //Check if is an inode
        final String type = Try
                .of(() -> InodeUtils.getAssetTypeFromDB(versionableInodeOrIdentifier)).getOrNull();

        //Could mean 2 things: it's an identifier or uuid does not exist
        if (null == type) {
            final Identifier identifier = APILocator.getIdentifierAPI()
                    .find(versionableInodeOrIdentifier);

            if (null == identifier || UtilMethods.isNotSet(identifier.getId())) {
                throw new DoesNotExistException(
                        "The versionable with uuid: " + versionableInodeOrIdentifier
                                + " does not exist");
            }

            return Response.ok(new ResponseEntityVersionableListView(
                    this.versionableHelper.getAssetTypeByVersionableFindAllMap()
                            .getOrDefault(identifier.getAssetType(),
                                    this.versionableHelper.getDefaultVersionableFindAllStrategy())
                            .findAllVersions(identifier, user, mode.respectAnonPerms))).build();
        }

        final VersionableView versionable = this.versionableHelper
                .getAssetTypeByVersionableFindVersionMap().getOrDefault(type,
                        this.versionableHelper.getDefaultVersionableFindVersionStrategy())
                .findVersion(versionableInodeOrIdentifier, user, mode.respectAnonPerms);

        return Response.ok(new ResponseEntityVersionableView(versionable)).build();
    }

    @Operation(
        summary = "Bring back version",
        description = "Restores a specific version as the working version. User needs edit permissions"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Version restored successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityVersionableView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Version not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{versionableInode}/_bringback")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response bringBack(@Context final HttpServletRequest httpRequest,
                              @Context final HttpServletResponse httpResponse,
                              @Parameter(description = "Versionable asset inode to restore", required = true) @PathParam("versionableInode") final String versionableInode)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true)
                .requiredBackendUser(true).init();

        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, () -> "Finding the version: " + versionableInode);

        //Check if is an inode
        final String type = Try
                .of(() -> InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null == type) {

            throw new DoesNotExistException(
                    "The versionable with uuid: " + versionableInode + " does not exist");
        }

        final VersionableView versionable = this.versionableHelper
                .getAssetTypeByVersionableFindVersionMap().getOrDefault(type,
                        this.versionableHelper.getDefaultVersionableFindVersionStrategy())
                .findVersion(versionableInode, user, mode.respectAnonPerms);

        if (versionable.getVersionable() instanceof Permissionable) {

            this.versionableHelper.checkWritePermissions((Permissionable)versionable.getVersionable(), user);
        } else {

            throw new DotSecurityException(
                    "Can not use versionable with uuid: " + versionableInode);
        }

        Logger.debug(this, () -> "Restoring to the version: " + versionableInode);

        final VersionableView newVersionable = this.versionableHelper
                .getAssetTypeByVersionableRestoreVersionMap().getOrDefault(type,
                    this.versionableHelper.getDefaultVersionableRestoreVersionStrategy())
                .restoreVersion(versionable.getVersionable(), user, false);

        return Response.ok(new ResponseEntityVersionableView(newVersionable)).build();
    }
}
