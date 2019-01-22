package com.dotcms.rest.api.v1.asset;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
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
import javax.servlet.http.HttpServletRequest;

@Path("/v1/content/asset")
public class AssetsResource {

    private final WebResource webResource;
    private final ContentletAPI contentletAPI;

    @VisibleForTesting
    public AssetsResource(ContentletAPI contentletAPI, WebResource webResource) {
        this.contentletAPI = contentletAPI;
        this.webResource = webResource;
    }

    public AssetsResource() {
        this(APILocator.getContentletAPI(), new WebResource());
    }

    @GET
    @JSONP
    @NoCache
    @Path("/resourcelink")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response findResourceLink(@Context final HttpServletRequest request,
            @QueryParam("inode") final String inode)
            throws DotDataException, DotStateException, DotSecurityException {
        try {
            final InitDataObject auth = webResource.init(true, request, true);
            final User user = auth.getUser();
            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("Missing required inode");
            }
            final Contentlet contentlet = contentletAPI.find(inode, user, false);
            final ResourceLink link = new ResourceLinkBuilder().build(request, user, contentlet);
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
