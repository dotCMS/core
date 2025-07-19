package com.dotcms.rest.api.v1.notification;

import com.dotcms.notifications.NotificationConverter;
import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.UserNotificationPair;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.RESTParams;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.SwaggerCompliant;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
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



    @Operation(
        summary = "Get notifications",
        description = "Returns a JSON array with notifications for the given user. Supports pagination with offset/limit and can retrieve notifications for all users with allUsers=true parameter. Supports Range header for pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Notifications retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @InitRequestRequired
    @Path ("/getNotifications/{params:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotifications (@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @Parameter(description = "URL parameters for pagination and filtering (e.g., offset/0/limit/5 or allUsers/true)") @PathParam ("params") final String params,
                                      @Parameter(description = "Range header for pagination (optional)") @HeaderParam("Range") final String range ) throws DotStateException, DotDataException, DotSecurityException, JSONException {


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

            return Response.ok(new ResponseEntityView<>(Map.of("totalUnreadNotifications", totalUnreadNotifications,
                    "notifications", notificationsResult, "total", notificationsCount)))
                    .header("Content-Range", "items " + offset + "-" + limit + "/" + totalUnreadNotifications)
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    } // getNotifications.

    @Operation(
        summary = "Get new notifications count",
        description = "Returns the count of new/unread notifications for the current user or all users if allUsers parameter is true."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Notification count retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityNotificationCountView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path ("/getNewNotificationsCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNewNotificationsCount ( @Context final HttpServletRequest httpServletRequest,
                                               @Context final HttpServletResponse httpServletResponse,
                                               @Parameter(description = "Filter to include all users' notifications") @QueryParam("allUsers") final Boolean allUsers ) throws DotStateException, DotDataException, DotSecurityException, JSONException {
        
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(httpServletRequest, httpServletResponse)
                .rejectWhenNoUser(true).init();


        Long newNotificationsCount = 0L;
        User user;
        Response response;
        final boolean includeAllUsers = allUsers != null && allUsers;

        try {

            user = initData.getUser();

            if (includeAllUsers) {

                newNotificationsCount = notificationAPI.getNotificationsCount();
            } else {

                if (null != user) {

                    newNotificationsCount = notificationAPI.getNewNotificationsCount(user.getUserId());
                }
            }

            response = Response.ok(new ResponseEntityView<>(newNotificationsCount))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Operation(
        summary = "Mark notifications as read",
        description = "Marks all notifications for the current user as read."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Notifications marked as read successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path ("/markAsRead")
    @Produces(MediaType.APPLICATION_JSON)
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

            return Response.ok(new ResponseEntityView<>(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(), "notification.success.markasread")))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // markAsRead.

    @Operation(
        summary = "Delete single notification",
        description = "Deletes a specific notification by its ID for the current user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Notification deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Notification not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/id/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @Parameter(description = "Notification ID to delete", required = true) @PathParam("id") String groupId) {

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

            return Response.ok(new ResponseEntityView<>(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(),
                            "notification.success.delete", groupId)))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // delete.



    @Operation(
        summary = "Delete multiple notifications",
        description = "Deletes multiple notifications for the current user. Uses PUT instead of DELETE to support request body with JSON data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Notifications deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid notification IDs",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete ( @Context HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                 description = "Form data containing list of notification IDs to delete", 
                                 required = true,
                                 content = @Content(schema = @Schema(implementation = DeleteForm.class))
                             ) final DeleteForm deleteForm )  {

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

            response =  Response.ok(new ResponseEntityView<>(Boolean.TRUE,
                    list(new MessageEntity(LanguageUtil.get(user.getLocale(),
                            "notifications.success.delete", deleteForm.getItems())))))
                    .build(); // 200
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // delete.



}
