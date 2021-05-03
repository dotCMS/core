package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
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
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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

    /**
     * Deletes the version inode.
     *
     * It checks that the inode provided is not working or live, and the user executing the action
     * needs to have Edit Permissions over the element.
     *
     * @param versionableInode {@link String} Inode of the element to be deleted
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @Path("/{versionableInode}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response deleteVersion(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("versionableInode") final String versionableInode)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Deleting the version by inode: " + versionableInode);

        final String type = Try.of(()->InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null == type) {

            throw new DoesNotExistException("The versionable inode: " + versionableInode + " does not exists");
        }

        this.versionableHelper.getAssetTypeByVersionableDeleteMap().getOrDefault(type,
                this.versionableHelper.getDefaultVersionableDeleteStrategy())
                .deleteVersionByInode(versionableInode, user, mode.respectAnonPerms);

        return Response.ok(new ResponseEntityView("Version " + versionableInode + " deleted successfully")).build();
    }

    /**
     * Finds the versionable for the passed UUID.
     *
     * If the UUID is an inode it will return the versionable for that specific element.
     * If the UUID is an identifier it will return all the versionables for that element.
     *
     * User executing the action needs to have View Permissions over the element.
     *
     * If the UUID does not exists, 404 is returned.
     *
     * @param versionableInodeOrIdentifier {@link String} UUID of the element
     * @return {@link VersionableView} versionable view object
     */
    @GET
    @Path("/{versionableInodeOrIdentifier}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findVersionable(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @PathParam("versionableInodeOrIdentifier") final String versionableInodeOrIdentifier)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, () -> "Finding the version: " + versionableInodeOrIdentifier);

        //Check if is an inode
        final String type = Try
                .of(() -> InodeUtils.getAssetTypeFromDB(versionableInodeOrIdentifier)).getOrNull();

        //Could mean 2 things: it's an identifier or uuid does not exists
        if (null == type) {
            final Identifier identifier = APILocator.getIdentifierAPI()
                    .find(versionableInodeOrIdentifier);

            if (null == identifier || UtilMethods.isNotSet(identifier.getId())) {
                throw new DoesNotExistException(
                        "The versionable with uuid: " + versionableInodeOrIdentifier
                                + " does not exists");
            }

            return Response.ok(new ResponseEntityView(
                    this.versionableHelper.getAssetTypeByVersionableFindAllMap()
                            .getOrDefault(identifier.getAssetType(),
                                    this.versionableHelper.getDefaultVersionableFindAllStrategy())
                            .findAllVersions(identifier, user, mode.respectAnonPerms))).build();
        }

        final VersionableView versionable = this.versionableHelper
                .getAssetTypeByVersionableFindVersionMap().getOrDefault(type,
                        this.versionableHelper.getDefaultVersionableFindVersionStrategy())
                .findVersion(versionableInodeOrIdentifier, user, mode.respectAnonPerms);

        return Response.ok(new ResponseEntityView(versionable)).build();
    }

}
