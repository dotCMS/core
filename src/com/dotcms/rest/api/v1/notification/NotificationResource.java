package com.dotcms.rest.api.v1.notification;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.HeaderParam;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Resource to handle the notification stuff.
 * - Mark as read
 * - Get notification
 * - Remove notification
 */
@Path("/v1/notification")
public class NotificationResource {

    private final WebResource webResource = new WebResource();

    /**
     * Returns a JSON Array with the notifications for the given User
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws JSONException
     */
    @GET
    @Path ("/getNotifications/{params:.*}")
    @Produces ("application/json")
    public Response getNotifications ( @Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam ("params") String params, @HeaderParam("Range") String range ) throws DotStateException, DotDataException, DotSecurityException, JSONException {


        InitDataObject initData = webResource.init(params, true, request, true, null);

        User user = initData.getUser();
        String limitStr = initData.getParamsMap().get(RESTParams.LIMIT.getValue());
        String offsetStr = initData.getParamsMap().get(RESTParams.OFFSET.getValue());
        boolean allUsers = initData.getParamsMap().get("allusers")!=null?Boolean.parseBoolean(initData.getParamsMap().get("allusers")):false;

        /* Limit and Offset Parameters Handling, if not passed, using default */

        long limit = -1;
        long offset = -1;

        try {
            if(UtilMethods.isSet(limitStr)) {
                limit = Long.parseLong(limitStr);
            }
        } catch(NumberFormatException e) {
        	// we DON'T want to do anything here, just use default limit
        }

        try {
            if(UtilMethods.isSet(offsetStr)) {
                offset = Long.parseLong(offsetStr);
            }
        } catch(NumberFormatException e) {
        	// we DON'T want to do anything here, just use default offset
        }

        offset = UtilMethods.isSet(range)?Long.parseLong(range.split("=")[1].split("-")[0]):offset;
        limit = UtilMethods.isSet(range)?Long.parseLong(range.split("=")[1].split("-")[1]):limit;
        limit += 1;

        NotificationAPI notificationAPI = APILocator.getNotificationAPI();

        // Let's get the total count
        Long total = notificationAPI.getNotificationsCount();

        // Let's mark the new notifications as read
        notificationAPI.markNotificationsAsRead(user.getUserId());

        List<Notification> notifications = allUsers?notificationAPI.getNotifications(offset, limit):notificationAPI.getNotifications(user.getUserId(), offset, limit);

        return Response.ok(new ResponseEntityView(notifications))
                .header("Content-Range", "items " + offset + "-" + limit + "/" + total)
                .build(); // 200
    }

    /**
     * Returns whether there are new Notifications or not for the given User
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws JSONException
     */
    @GET
    @Path ("/getNewNotificationsCount/{params:.*}")
    @Produces ("application/json")
    public Response getNewNotificationsCount ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, true, null);
    	ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

    	User user = initData.getUser();

    	JSONObject newNotificationsCountJSON = new JSONObject();

    	Long newNotificationsCount = APILocator.getNotificationAPI().getNewNotificationsCount(user.getUserId());

    	newNotificationsCountJSON.put("newNotificationsCount", newNotificationsCount);

    	return responseResource.response( newNotificationsCountJSON.toString() );

    }

}
