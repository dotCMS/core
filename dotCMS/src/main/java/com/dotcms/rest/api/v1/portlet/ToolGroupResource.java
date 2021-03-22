package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.liferay.portal.model.User;

/**
 * This Resource allows you to manage toolgroups
 */
@Path("/v1/toolgroups")
@SuppressWarnings("serial")
public class ToolGroupResource implements Serializable {

    private final WebResource webResource;


    /**
     * Default class constructor.
     */
    public ToolGroupResource() {
        this(new WebResource(new ApiProvider()));
    }

    @VisibleForTesting
    public ToolGroupResource(WebResource webResource) {
        this.webResource = webResource;

    }


    /**
     * This method removes a toolgroup to the specified user, using the id as key to find the toolgroup.
     * If the layoutId is gettingStarted it will remove the gettingStartedLayout to the user.
     * In case the user is not sent, the operation will be performed over the current user
     */
    @PUT
    @Path("/{layoutId}/_removefromuser")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteToolGroupFromUser(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("layoutId") final String layoutId, @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        final User user;
        if (null == userid){
            user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();
        }else{
            user = APILocator.getUserAPI().loadUserById(userid);
        }

        Layout layoutToRemove;
        if(layoutId.equalsIgnoreCase("gettingstarted")){
            layoutToRemove = APILocator.getLayoutAPI().findGettingStartedLayout();
        }else{
            layoutToRemove = APILocator.getLayoutAPI().findLayout(layoutId);
        }

        APILocator.getRoleAPI().removeLayoutFromRole(layoutToRemove, user.getUserRole());
        return Response.ok(new ResponseEntityView(map("message", layoutId + " removed from " + user.getUserId())))
                .build();

    }


    /**
     * This method adds a toolgroup to the specified user, using the id as key to find the toolgroup.
     * If the layoutId is gettingStarted it will add the gettingStartedLayout to the user.
     * In case the user is not sent, the operation will be performed over the current user
     */
    @PUT
    @Path("/{layoutId}/_addtouser")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addToolGroupToUser(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("layoutId") final String layoutId, @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        final User user;
        if (null == userid){
            user = new WebResource.InitBuilder(webResource)
                    .requiredBackendUser(true)
                    .requestAndResponse(request, response)
                    .rejectWhenNoUser(true)
                    .init()
                    .getUser();
        }else{
            user = APILocator.getUserAPI().loadUserById(userid);
        }

        final Layout layoutToAdd;
        if(layoutId.equalsIgnoreCase("gettingstarted")){
            layoutToAdd = APILocator.getLayoutAPI().findGettingStartedLayout();
        }else{
            layoutToAdd = APILocator.getLayoutAPI().findLayout(layoutId);
        }

        APILocator.getLayoutAPI().addLayoutForUser(layoutToAdd, user);
            return Response.ok(new ResponseEntityView(map("message", layoutId + " added to " + user.getUserId())))
                            .build();
    }



}
