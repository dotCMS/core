package com.dotcms.rest.api.v1.fileasset;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Tag(name = "File Assets")
@Path("/v1/content/fileassets")
public class FileAssetsResource {

    private final WebResource webResource;
    private final ContentletAPI contentletAPI;

    @VisibleForTesting
    public FileAssetsResource(final ContentletAPI contentletAPI, final WebResource webResource) {
        this.contentletAPI = contentletAPI;
        this.webResource = webResource;
    }

    public FileAssetsResource() {
        this(APILocator.getContentletAPI(), new WebResource());
    }

    /**
     * Given an inode this will build get you a Resource Link
     * The inode is expected to be File Asset other wise you'll get exception
     * @param httpServletRequest http request
     * @param inode file asset inode
     * @return
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @NoCache
    @Path("/{inode}/resourcelink")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findResourceLink(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @PathParam("inode") final String inode) throws DotStateException {
        try {
            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("Missing required inode param");
            }
            final InitDataObject auth = webResource.init(httpServletRequest, httpServletResponse, true);
            final User user = auth.getUser();
            final Contentlet contentlet = contentletAPI.find(inode, user, false);
            final ResourceLink link = new ResourceLinkBuilder().build(httpServletRequest, user, contentlet);
            if(link.isDownloadRestricted()){
               throw new DotSecurityException("The Resource link to the contentlet is restricted.");
            }
            return Response.ok(new ResponseEntityView(ImmutableMap.of("resourceLink",
                    ImmutableMap.of(
                    "href", link.getResourceLinkAsString(),
                    "text", link.getResourceLinkUriAsString(),
                     "mimeType", link.getMimeType()
                    )
            ))).build();

        } catch (Exception ex) {
            Logger.error(this.getClass(),
                    "Exception on method findResourceLink with exception message: " + ex
                            .getMessage(), ex);
            return ResponseUtil.mapExceptionResponse(ex);
        }
    }


}
