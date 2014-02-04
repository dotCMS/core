package com.dotcms.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.GET;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.HeaderParam;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.Path;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.PathParam;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.Produces;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.Context;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.Response;
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


@Path("/notification")
public class NotificationResource extends WebResource {

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

        InitDataObject initData = init( params, true, request, true );

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

        JSONArray notificationsJSON = new JSONArray();

        NotificationAPI notificationAPI = APILocator.getNotificationAPI();

        // Let's get the total count
        Long total = notificationAPI.getNotificationsCount();

        // Let's mark the new notifications as read
        notificationAPI.markNotificationsAsRead(user.getUserId());

        List<Notification> notifications = allUsers?notificationAPI.getNotifications(offset, limit):notificationAPI.getNotifications(user.getUserId(), offset, limit);

        for (Notification n : notifications) {
        	JSONObject notificationJSON = new JSONObject();
        	notificationJSON.put("id", n.getId());
        	notificationJSON.put("message", n.getMessage());
        	notificationJSON.put("type", n.getType().name());
        	notificationJSON.put("level", n.getLevel().name());
        	notificationJSON.put("time_sent", DateUtil.prettyDateSince(n.getTimeSent()));
        	notificationsJSON.add(notificationJSON);
		}

        return Response.ok( notificationsJSON.toString()).header("Content-Range", "items " + offset + "-" + limit + "/" + total).build() ;

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

    	InitDataObject initData = init( params, true, request, true );
    	ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

    	User user = initData.getUser();

    	JSONObject newNotificationsCountJSON = new JSONObject();

    	Long newNotificationsCount = APILocator.getNotificationAPI().getNewNotificationsCount(user.getUserId());

    	newNotificationsCountJSON.put("newNotificationsCount", newNotificationsCount);

    	return responseResource.response( newNotificationsCountJSON.toString() );

    }

}
