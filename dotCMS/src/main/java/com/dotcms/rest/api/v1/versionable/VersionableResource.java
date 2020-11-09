package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
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
 * @author jsanca
 */
@Path("/v1/versionables")
public class VersionableResource {

    private final WebResource       webResource;
    private final IdentifierAPI     identifierAPI;
    private final VersionableHelper versionableHelper;

    public VersionableResource() {
        this(new WebResource(), APILocator.getTemplateAPI(),
                APILocator.getContentletAPI(), APILocator.getVersionableAPI(),
                APILocator.getIdentifierAPI(), APILocator.getPermissionAPI(),
                APILocator.getRoleAPI(), APILocator.getContainerAPI());
    }

    @VisibleForTesting
    public VersionableResource(final WebResource     webResource,
                            final TemplateAPI        templateAPI,
                            final ContentletAPI      contentletAPI,
                            final VersionableAPI     versionableAPI,
                            final IdentifierAPI      identifierAPI,
                            final PermissionAPI      permissionAPI,
                            final RoleAPI roleAPI,
                            final ContainerAPI containerAPI) {

        this.webResource    = webResource;
        this.identifierAPI  = identifierAPI;
        this.versionableHelper = new VersionableHelper(templateAPI, contentletAPI, versionableAPI,
                permissionAPI, roleAPI, containerAPI);
    }

    /**
     * Returns the versionable list for contentlets, templates and other versionables
     * @param httpRequest  {@link HttpServletRequest}
     * @param httpResponse {@link HttpServletResponse}
     * @param versionableIdentifier {@link String}
     * @return List of Versionables
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{versionableIdentifier}/versions")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getVersionsByIdentifier(@Context final HttpServletRequest httpRequest,
                                                     @Context final HttpServletResponse httpResponse,
                                                     @PathParam("versionableIdentifier") final String versionableIdentifier) throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Getting the versions by identifier: " + versionableIdentifier);

        final Identifier identifier = this.identifierAPI.find(versionableIdentifier);

        if (null != identifier && InodeUtils.isSet(identifier.getId())) {

            return Response.ok(new ResponseEntityView(
                    this.versionableHelper.getAssetTypeByVersionableFindAllMap().getOrDefault(identifier.getAssetType(),
                            this.versionableHelper.getDefaultVersionableFindAllStrategy())
                            .findAllVersions(identifier, user, mode.respectAnonPerms))).build();
        }

        throw new DoesNotExistException("The versionable, id: " + versionableIdentifier + " does not exists");
    }

    /**
     * Deletes the version inode
     * @param httpRequest  {@link HttpServletRequest}
     * @param httpResponse {@link HttpServletResponse}
     * @param versionableInode {@link String}
     * @return List of Versionables
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
                                  @PathParam("versionableInode") final String versionableInode) {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Deleting the version by inode: " + versionableInode);

        final String type = Try.of(()->InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null == type) {

            throw new DoesNotExistException("The versionable, inode: " + versionableInode + " does not exists");
        }

        this.versionableHelper.getAssetTypeByVersionableDeleteMap().getOrDefault(type,
                this.versionableHelper.getDefaultVersionableDeleteStrategy())
                .deleteVersionByInode(versionableInode, user, mode.respectAnonPerms);

        return Response.ok(new ResponseEntityView(true)).build();
    }

    /**
     * Finds a specific  inode version
     * If the inode for the version does not exists, 404 is returned
     * @param httpRequest  {@link HttpServletRequest}
     * @param httpResponse {@link HttpServletResponse}
     * @param versionableInode {@link String}
     * @return Versionable
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{versionableInode}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response findVersion(@Context final HttpServletRequest httpRequest,
                                     @Context final HttpServletResponse httpResponse,
                                     @PathParam("versionableInode") final String versionableInode) throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Finding the version: " + versionableInode);

        final String type = Try.of(()->InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null != type) {

            final Optional<VersionableView> versionableOpt = this.versionableHelper
                    .getAssetTypeByVersionableFinderMap().getOrDefault(type,
                            this.versionableHelper.getDefaultVersionableFinderStrategy())
                    .findVersion(versionableInode, user, mode.respectAnonPerms);

            if (versionableOpt.isPresent()) {

                return Response.ok(new ResponseEntityView(versionableOpt.get())).build();
            }
        }

        throw new DoesNotExistException("The versionable, inode: " + versionableInode + " does not exists");
    }

    /**
     * Bring back to the top a specific version.
     * If the inode for the version does not exists, 404 is returned
     * @param httpRequest  {@link HttpServletRequest}
     * @param httpResponse {@link HttpServletResponse}
     * @param versionableInode {@link String}
     * @return List of Versionables
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/_bringback/{versionableInode}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response bringBackVersion(@Context final HttpServletRequest httpRequest,
                                            @Context final HttpServletResponse httpResponse,
                                            @PathParam("versionableInode") final String versionableInode) throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Bringing back to the version: " + versionableInode);

        final String type = Try.of(()->InodeUtils.getAssetTypeFromDB(versionableInode)).getOrNull();

        if (null != type) {

            Optional<VersionableView> versionableOpt = this.versionableHelper
                    .getAssetTypeByVersionableFinderMap().getOrDefault(type,
                            this.versionableHelper.getDefaultVersionableFinderStrategy())
                    .findVersion(versionableInode, user, mode.respectAnonPerms);

            if (versionableOpt.isPresent()) {

                versionableOpt = this.versionableHelper.getAssetTypeByVersionableBringBackMap()
                        .getOrDefault(type, this.versionableHelper.getDefaultVersionableBringBackStrategy())
                        .bringBackVersion(versionableOpt.get(), user, mode.respectAnonPerms);
                if (versionableOpt.isPresent()) {

                    return Response.ok(new ResponseEntityView(versionableOpt.get())).build();
                }
            }
        }

        throw new DoesNotExistException("The versionable, id: " + versionableInode + " does not exists");
    }
}
