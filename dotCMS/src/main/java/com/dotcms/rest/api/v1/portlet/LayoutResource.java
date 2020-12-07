package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This Resource allows you to manage layouts
 */
@Path("/v1/layout")
@SuppressWarnings("serial")
public class LayoutResource implements Serializable {

    private final WebResource webResource;


    /**
     * Default class constructor.
     */
    public LayoutResource() {
        this(new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public LayoutResource(WebResource webResource) {
        this.webResource = webResource;

    }


    @DELETE
    @Path("/gettingStarted")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteGettingStarted(@Context final HttpServletRequest request) {

        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                        .requestAndResponse(request, null).rejectWhenNoUser(true).init().getUser();

        try {
            final Role role = user.getUserRole();
            final Layout layout = APILocator.getLayoutAPI().findGettingStartedLayout();

            if (role == null || layout == null) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                                user.getUserId(), "unable to find user role or layout");
            }

            APILocator.getRoleAPI().removeLayoutFromRole(layout, role);

            return Response.ok(new ResponseEntityView(map("message", layout.getId() + " removed from " + user.getUserId())))
                            .build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }


    /**
     * This method adds the gettingStarted layout to the current user
     * 
     * @param request
     * @param layoutId
     * @return
     */
    @PUT
    @Path("/gettingStarted")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addLayoutForUser(@Context final HttpServletRequest request,
                    @PathParam("layoutId") final String layoutId) {
        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                        .requestAndResponse(request, null).rejectWhenNoUser(true).init().getUser();

        try {
            final Role role = user.getUserRole();
            final Layout layout = APILocator.getLayoutAPI().findGettingStartedLayout();

            if (role == null || layout == null || UtilMethods.isEmpty(role.getId()) || UtilMethods.isEmpty(layout.getId())) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                                user.getUserId(), "unable to find user role or layout");
            }


            APILocator.getLayoutAPI().addLayoutForUser(layout, user);
            return Response.ok(new ResponseEntityView(map("message", layoutId + " added to " + user.getUserId())))
                            .build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }



}
