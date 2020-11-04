package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resource to retrieve the versions of different kind of objects.
 * @author jsanca
 */
@Path("/v1/versionables")
public class VersionableResource {

    private final WebResource    webResource;
    private final ContentletAPI  contentletAPI;
    private final TemplateAPI    templateAPI;
    private final IdentifierAPI  identifierAPI;
    private final VersionableAPI versionableAPI;

    private final Map<String, VersionableFinderStrategy> assertTypeByVersionableAPIMap;
    private final VersionableFinderStrategy defaultVersionableFinderStrategy;

    public VersionableResource() {
        this(new WebResource(), APILocator.getTemplateAPI(),
                APILocator.getContentletAPI(), APILocator.getVersionableAPI(),
                APILocator.getIdentifierAPI());
    }

    @VisibleForTesting
    public VersionableResource(final WebResource     webResource,
                            final TemplateAPI        templateAPI,
                            final ContentletAPI      contentletAPI,
                            final VersionableAPI     versionableAPI,
                            final IdentifierAPI      identifierAPI) {

        this.webResource    = webResource;
        this.templateAPI    = templateAPI;
        this.contentletAPI  = contentletAPI;
        this.identifierAPI  = identifierAPI;
        this.versionableAPI = versionableAPI;
        this.assertTypeByVersionableAPIMap =
                new ImmutableMap.Builder<String, VersionableFinderStrategy>()
                        .put(Identifier.ASSET_TYPE_CONTENTLET, this::findAllVersionsByContentletId)
                        .put(Identifier.ASSET_TYPE_TEMPLATE,   this::findAllVersionsByTemplateId)
                        .build();
        this.defaultVersionableFinderStrategy = this::findAllVersionsByVersionableId;
    }

    /**
     * This interface is just a general encapsulation for all kind of types
     */
    @FunctionalInterface
    private interface VersionableFinderStrategy {

        List<VersionableView> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles);
    }

    private List<VersionableView> findAllVersionsByContentletId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.contentletAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByTemplateId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.templateAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByVersionableId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.versionableAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
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
                                                     @PathParam("versionableIdentifier") final String versionableIdentifier) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user     = initData.getUser();
        final PageMode mode = PageMode.get(httpRequest);
        Logger.debug(this, ()-> "Getting the versions by identifier: " + versionableIdentifier);

        final Identifier identifier = this.identifierAPI.find(versionableIdentifier);

        if (null != identifier && InodeUtils.isSet(identifier.getId())) {

            return Response.ok(new ResponseEntityView(
                    this.assertTypeByVersionableAPIMap.getOrDefault(identifier.getAssetType(), this.defaultVersionableFinderStrategy)
                            .findAllVersions(identifier, user, mode.respectAnonPerms))).build();
        }

        throw new DoesNotExistException("The versionable, id: " + versionableIdentifier + " does not exists");
    }
}
