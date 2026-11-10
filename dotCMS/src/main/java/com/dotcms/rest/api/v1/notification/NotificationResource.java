package com.dotcms.rest.api.v1.notification;

import com.dotcms.notifications.NotificationConverter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.UserNotificationPair;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.view.NotificationView;
import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.RESTParams;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.ConversionUtils.toLong;



/**
 * Resource to handle the notification stuff.
 * - Mark as read
 * - Get notification
 * - Remove notification
 */
@Path("/v1/notification")
@Tag(name = "Notifications")
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


        final InitDataObject initData = new WebResource.InitBuilder(webResource)
           .requiredBackendUser(true)
           .requiredFrontendUser(false)
           .params(params)
           .requestAndResponse(request, response)
           .rejectWhenNoUser(true).init();


        try {

            final User user = initData.getUser();
            final String limitStr = initData.getParamsMap().get(RESTParams.LIMIT.getValue());
            final String offsetStr = initData.getParamsMap().get(RESTParams.OFFSET.getValue());
            final boolean allUsers = isAllUsers(initData);

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
            Long notificationsCount = allUsers ?
                    this.notificationAPI.getNotificationsCount() :
                    this.notificationAPI.getNotificationsCount(user.getUserId());

            final Long totalUnreadNotifications = allUsers ?
                    this.notificationAPI.getNotificationsCount() :
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

            return Response.ok(new ResponseEntityView(Map.of("totalUnreadNotifications", totalUnreadNotifications,
                    "notifications", notificationsResult, "total", notificationsCount)))
                    .header("Content-Range", "items " + offset + "-" + limit + "/" + totalUnreadNotifications)
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    } // getNotifications.

    /**
     * Reads the {@code allusers} parameter. It is honored only for CMS Administrators; for anyone
     * else the listing stays limited to their own notifications.
     *
     * @param initData the request data holding the parameters and the current user
     * @return {@code true} when the parameter is {@code true} and the current user is a CMS Administrator
     */
    private boolean isAllUsers(final InitDataObject initData) {
        return Boolean.parseBoolean(initData.getParamsMap().get(ALLUSERS))
                && null != initData.getUser() && initData.getUser().isAdmin();
    }

    /**
     * Returns the number of unread notifications of the current user. The count is always the
     * user's own, for administrators too.
     *
     * @param httpServletRequest the current request
     * @param httpServletResponse the current response
     * @return a response whose entity is the user's unread notifications count
     * @throws DotStateException if the request cannot be initialized
     * @throws DotDataException if the count cannot be read
     * @throws DotSecurityException if the user cannot be resolved
     * @throws JSONException kept for compatibility with existing callers
     */
    @GET
    @Path ("/getNewNotificationsCount")
    @Produces ("application/json")
    public Response getNewNotificationsCount ( @Context final HttpServletRequest httpServletRequest,
                                               @Context final HttpServletResponse httpServletResponse ) throws DotStateException, DotDataException, DotSecurityException, JSONException {
        
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();


        Long newNotificationsCount = 0l;
        User user;
        Response response;

        try {

            user = initData.getUser();

            if (null != user) {

                newNotificationsCount = notificationAPI.getNewNotificationsCount(user.getUserId());
            }

            response = Response.ok(new ResponseEntityView(newNotificationsCount))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    /**
     * Returns the number of unread notifications of the current user.
     *
     * @param httpServletRequest the current request
     * @param httpServletResponse the current response
     * @param params ignored; the endpoint takes no path parameters
     * @return a response whose entity is the user's unread notifications count
     * @throws DotStateException if the request cannot be initialized
     * @throws DotDataException if the count cannot be read
     * @throws DotSecurityException if the user cannot be resolved
     * @throws JSONException kept for compatibility with existing callers
     * @deprecated use {@link #getNewNotificationsCount(HttpServletRequest, HttpServletResponse)}
     */
    @Deprecated
    public Response getNewNotificationsCount (final HttpServletRequest httpServletRequest,
                                              final HttpServletResponse httpServletResponse,
                                              final String params) throws DotStateException, DotDataException, DotSecurityException, JSONException {
        return getNewNotificationsCount(httpServletRequest, httpServletResponse);
    }

    /**
     * Update the user list of notifications marking them as read
     *
     * @param httpServletRequest
     * @return Response
     */
    @PUT
    @Path ("/markAsRead")
    @Produces ("application/json")
    public Response markAsRead ( @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse )  {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        Response response;
        User user;

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
     * @param httpServletRequest
     * @param groupId
     * @return Response
     */
    @DELETE
    @Path("/id/{id}")
    @Produces ("application/json")
    public Response delete(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam("id") String groupId) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        Response response;
        User user;

        try {

            user = initData.getUser();

            if ( null != groupId && null != user ) {

                this.notificationAPI.deleteNotification(user.getUserId(), groupId); // todo: include the user id, in order to remove by id.
            }

            return Response.ok(new ResponseEntityView(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(),
                            "notification.success.delete", groupId)))))
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
     * @param httpServletRequest
     * @param deleteForm
     * @return Response
     */
    @PUT
    @Path("/delete")
    @Produces ("application/json")
    public Response delete ( @Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, final DeleteForm deleteForm )  {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();

        Response response;
        User user;

        try {

            user = initData.getUser();

            if (null != deleteForm.getItems() && null != user) {

                this.notificationAPI.deleteNotifications(user.getUserId(), deleteForm.getItems().toArray(new String[] {}));
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
