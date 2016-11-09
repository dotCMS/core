package com.dotcms.rest.api.v1.notification;

import com.dotcms.notifications.NotificationConverter;
import com.dotcms.notifications.bean.*;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.*;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import static com.dotcms.util.ConversionUtils.toLong;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import static com.dotmarketing.util.DateUtil.prettyDateSince;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Resource to handle the notification stuff.
 * - Mark as read
 * - Get notification
 * - Remove notification
 */
@Path("/v1/notification")
public class NotificationResource {

    public static final String ALLUSERS = "allusers";
    public static final String COMMA = ",";
    private final WebResource webResource;
    private final NotificationAPI notificationAPI;
    private final ConversionUtils conversionUtils;
    private final NotificationConverter notificationConverter;


    public NotificationResource() {
        this(APILocator.getNotificationAPI(), new WebResource());
    }

    @VisibleForTesting
    public NotificationResource(final NotificationAPI notificationAPI,
                                final WebResource webResource) {

        this.notificationAPI        = notificationAPI;
        this.webResource            = webResource;
        this.conversionUtils        = ConversionUtils.INSTANCE;
        this.notificationConverter  = new NotificationConverter();
    }

    /**
     * Returns a JSON Array with the notifications for the given User
     *
     * examples:
     *
     * This one get the notification first 6 notifications for the current logged user.
     * http://localhost:8080/api/v1/notification/getNotifications/offset/0/limit/5
     *
     * This get the notifications for all users
     * http://localhost:8080/api/v1/notification/getNotifications/allUsers/true
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
    @InitRequestRequired
    @Path ("/getNotifications/{params:.*}")
    @Produces ("application/json")
    public Response getNotifications (@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @PathParam ("params") final String params,
                                      @HeaderParam("Range") final String range ) throws DotStateException, DotDataException, DotSecurityException, JSONException {


        final InitDataObject initData = webResource.init(params, true, request, true, null);

        try {

            final User user = initData.getUser();
            final String limitStr = initData.getParamsMap().get(RESTParams.LIMIT.getValue());
            final String offsetStr = initData.getParamsMap().get(RESTParams.OFFSET.getValue());
            final boolean allUsers = initData.getParamsMap().get(ALLUSERS) != null ?
                    Boolean.parseBoolean(initData.getParamsMap().get(ALLUSERS)) : false;

            /* Limit and Offset Parameters Handling, if not passed, using default */

            long limit  = toLong(limitStr, -1L);
            long offset = toLong(offsetStr, -1L);

            offset = UtilMethods.isSet(range) ?
                    Long.parseLong(range.split("=")[1].split("-")[0]) : offset;
            limit  = UtilMethods.isSet(range) ?
                    Long.parseLong(range.split("=")[1].split("-")[1]) : limit;
            limit  += 1;

            // Let's mark the new notifications as read: todo: should it work in that way?
            //notificationAPI.markNotificationsAsRead(user.getUserId());

            // Let's get the total count
            final Long total = allUsers ?
                    this.notificationAPI.getNotificationsCount():
                    this.notificationAPI.getNewNotificationsCount(user.getUserId());

            final List<Notification> notifications = allUsers ?
                    this.notificationAPI.getNotifications(offset, limit) :
                    this.notificationAPI.getNotifications(user.getUserId(), offset, limit);

            final List<NotificationView> notificationsResult = list();

            // copy and doing some treatment.
            if (null != notifications) {

                notifications.forEach(notification -> {

                    final NotificationView notificationResult = this.conversionUtils.convert(
                            new UserNotificationPair(user, notification), this.notificationConverter);

                    notificationsResult.add(notificationResult);
                });
            }

            return Response.ok(new ResponseEntityView(map("count", total, "notifications", notificationsResult)))
                    .header("Content-Range", "items " + offset + "-" + limit + "/" + total)
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    } // getNotifications.

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
    @Path ("/getNewNotificationsCount")
    @Produces ("application/json")
    public Response getNewNotificationsCount ( @Context final HttpServletRequest request,
                                               @PathParam ("params") final String params ) throws DotStateException, DotDataException, DotSecurityException, JSONException {

        InitDataObject initData = webResource.init(params, true, request, true, null);
        Long newNotificationsCount = 0l;
        User user = null;
        Response response = null;
        final boolean allUsers = initData.getParamsMap().get(ALLUSERS) != null ?
                Boolean.parseBoolean(initData.getParamsMap().get(ALLUSERS)) : false;

        try {

            user = initData.getUser();

            if (allUsers) {

                newNotificationsCount = notificationAPI.getNotificationsCount();
            } else {

                if (null != user) {

                    newNotificationsCount = notificationAPI.getNewNotificationsCount(user.getUserId());
                }
            }

            response = Response.ok(new ResponseEntityView(newNotificationsCount))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    /**
     * Update the user list of notifications marking them as read
     *
     * @param request
     * @return Response
     */
    @PUT
    @Path ("/markAsRead")
    @Produces ("application/json")
    public Response markAsRead ( @Context final HttpServletRequest request )  {

        InitDataObject initData = webResource.init(null, true, request, true, null);
        Response response = null;
        User user = null;

        try {

            user = initData.getUser();

            if (null != user) {

                this.notificationAPI.markNotificationsAsRead(user.getUserId());
            }

            return Response.ok(new ResponseEntityView(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(), "notification.success.markasread")))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // markAsRead.

    /**
     * Delete one notification
     * @param request
     * @param notificationId
     * @return Response
     */
    @DELETE
    @Path("/id/{id}")
    @Produces ("application/json")
    public Response delete ( @Context HttpServletRequest request, @PathParam("id") String notificationId )  {

        InitDataObject initData = webResource.init(null, true, request, true, null);
        Response response = null;
        User user = null;

        try {

            user = initData.getUser();

            if (null != notificationId && null != user) {

                this.notificationAPI.deleteNotification(user.getUserId(), notificationId); // todo: include the user id, in order to remove by id.
            }

            return Response.ok(new ResponseEntityView(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(),
                            "notification.success.delete", notificationId)))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // delete.



    /**
     * Delete one or more notifications
     *
     * Note: we have use PUT instead of DELETE, just because the ability to have a body/json
     * @param request
     * @param deleteForm
     * @return Response
     */
    @PUT
    @Path("/delete")
    @Produces ("application/json")
    public Response delete ( @Context HttpServletRequest request, final DeleteForm deleteForm )  {

        InitDataObject initData = webResource.init(null, true, request, true, null);
        Response response = null;
        User user = null;

        try {

            user = initData.getUser();

            if (null != deleteForm.getItems() && null != user) {

                this.notificationAPI.deleteNotifications(user.getUserId(),
                        deleteForm.getItems().toArray(new String [] {}));  // todo: include the user id, in order to remove by id.
            }

            response =  Response.ok(new ResponseEntityView(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(),
                            "notifications.success.delete", deleteForm.getItems())))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // delete.



}
