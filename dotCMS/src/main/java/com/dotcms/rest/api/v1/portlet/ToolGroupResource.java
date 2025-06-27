package com.dotcms.rest.api.v1.portlet;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.Map;

/**
 * This Resource allows you to manage toolgroups
 */
@Path("/v1/toolgroups")
@Tag(name = "Administration")
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

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        Layout layoutToRemove = getLayout(layoutId);

        APILocator.getRoleAPI().removeLayoutFromRole(layoutToRemove,
                null == userid ? loggedInUser.getUserRole() : user.getUserRole());
        return Response.ok(new ResponseEntityView(Map.of("message",
                        layoutId + " removed from " + (null == userid ? loggedInUser.getUserId()
                                : userid))))
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

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        final Layout layoutToAdd = getLayout(layoutId);

        APILocator.getLayoutAPI()
                .addLayoutForUser(layoutToAdd, null == userid ? loggedInUser : user);
        return Response.ok(new ResponseEntityView(Map.of("message",
                layoutId + " added to " + (null == userid ? loggedInUser.getUserId()
                        : userid))))
                .build();
    }

    /**
     * Given a userId, this method returns a json object with a boolean indicating if the user's role
     * has associated the specified layout.
     * In case the user is not sent, the operation will be performed over the current user
     * @param request
     * @param response
     * @param layoutId
     * @param userid
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{layoutId}/_userHasLayout")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response userHasLayout(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("layoutId") final String layoutId, @QueryParam("userid") final String userid)
            throws DotDataException, DotSecurityException {

        User user = null;

        final User loggedInUser = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        if (null != userid){
            user = APILocator.getUserAPI().loadUserById(userid, loggedInUser, true);
        }

        final Layout layout = getLayout(layoutId);

        return Response.ok(new ResponseEntityView(Map.of("message", APILocator.getRoleAPI()
                .roleHasLayout(layout,
                        null == userid ? loggedInUser.getUserRole() : user.getUserRole()))))
                .build();
    }

    private Layout getLayout(final String layoutId) throws DotDataException {
        final Layout layoutToAdd;
        if (layoutId.equalsIgnoreCase("gettingstarted")) {
            layoutToAdd = APILocator.getLayoutAPI().findGettingStartedLayout();
        } else {
            layoutToAdd = APILocator.getLayoutAPI().findLayout(layoutId);
        }
        return layoutToAdd;
    }

}
