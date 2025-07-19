package com.dotcms.rest.api.v1.portlet;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;
import javax.ws.rs.QueryParam;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.liferay.portal.model.Portlet.DATA_VIEW_MODE_KEY;
import static com.liferay.util.StringPool.BLANK;

/**
 * This Resource is for create custom portlets. These kind of custom portlets are to show diff types
 * or content (content types or base types).
 */
@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Path("/v1/portlet")
@Tag(name = "Portlets")
public class PortletResource implements Serializable {

    private final WebResource webResource;
    private final PortletAPI portletApi;

    private static final String JSON_RESPONSE_PORTLET_ATTR = "portlet";

    /**
     * Default class constructor.
     */
    @SuppressWarnings("unused")
    public PortletResource() {
        this(new WebResource(new ApiProvider()), APILocator.getPortletAPI());
    }

    @VisibleForTesting
    public PortletResource(WebResource webResource, PortletAPI portletApi) {
        this.webResource = webResource;
        this.portletApi = portletApi;
    }

    @Operation(
        summary = "Create custom portlet",
        description = "Creates a custom dotCMS Portlet for a given Base Type or Content Type. Requires roles permission."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Custom portlet created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid portlet data or portlet already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", 
                    description = "Conflict - portlet with this ID already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/custom")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response saveNew(@Context final HttpServletRequest request,
                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                      description = "Custom portlet form data", 
                                      required = true,
                                      content = @Content(schema = @Schema(implementation = CustomPortletForm.class))
                                  ) final CustomPortletForm formData) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.ROLES.toString())
                .init();
        String portletId = BLANK;
        try {
            portletId = portletApi.portletIdPrefixCleaner(formData.portletId);
            if (UtilMethods.isSet(portletApi.findPortlet(portletId))) {
                throw new DoesNotExistException(String.format("Portlet with ID '%s' already exist",
                        formData.portletId));
            }

            final Portlet contentPortlet = portletApi.findPortlet("content");

            final DotPortlet newPortlet = DotPortlet.builder()
                    .portletId(portletId)
                    .portletClass(contentPortlet.getPortletClass())
                    .putAllInitParams(contentPortlet.getInitParams()) // add view-action from base content portlet
                    .putInitParam("name", formData.portletName)
                    .putInitParam("baseTypes", formData.baseTypes)
                    .putInitParam("contentTypes", formData.contentTypes)
                    .putInitParam(DATA_VIEW_MODE_KEY, formData.dataViewMode)
                    .build();


            final Portlet savedPortlet = APILocator.getPortletAPI()
                    .savePortlet(newPortlet.toPortlet(), initData.getUser());

            return Response.ok(new ResponseEntityMapView(Map.of(JSON_RESPONSE_PORTLET_ATTR, savedPortlet.getPortletId()))).build();
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when saving new Portlet with ID " +
                    "'%s': %s", portletId, ExceptionUtil.getErrorMessage(e)), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @Operation(
        summary = "Update custom portlet",
        description = "Saves a new working version of an existing custom portlet. The formData must contain the identifier of the portlet. Requires roles permission."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Custom portlet updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid portlet data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/custom")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response updatePortlet(@Context final HttpServletRequest request, 
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                           description = "Custom portlet form data with identifier", 
                                           required = true,
                                           content = @Content(schema = @Schema(implementation = CustomPortletForm.class))
                                       ) final CustomPortletForm formData) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet("roles")
                .init();

        Response response = null;

        try {
            final String portletId = portletApi.portletIdPrefixCleaner(formData.portletId);
            if (!UtilMethods.isSet(portletApi.findPortlet(portletId))) {
                throw new DoesNotExistException("Portlet with Id: " + formData.portletId + " does not exist");
            }
            final Portlet contentPortlet = portletApi.findPortlet("content");

            final DotPortlet updatedPortlet =  DotPortlet.builder()
                    .portletId(portletId)
                    .portletClass(contentPortlet.getPortletClass())
                    .putAllInitParams(contentPortlet.getInitParams()) // add view-action from base content portlet
                    .putInitParam("name", formData.portletName)
                    .putInitParam("baseTypes", formData.baseTypes)
                    .putInitParam("contentTypes", formData.contentTypes)
                    .putInitParam(DATA_VIEW_MODE_KEY, formData.dataViewMode)
                    .build();


            final Portlet newPortlet = APILocator.getPortletAPI()
                    .savePortlet(updatedPortlet.toPortlet(), initData.getUser());

            return Response.ok(new ResponseEntityMapView(Map.of(JSON_RESPONSE_PORTLET_ATTR, newPortlet.getPortletId()))).build();

        } catch (Exception e) {
            Logger.error(this, String.format("An error occurred when updating Portlet with ID " +
                    "'%s': %s", formData.portletId, ExceptionUtil.getErrorMessage(e)), e);
            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @Operation(
        summary = "Add portlet to layout",
        description = "Links a layout with a portlet. Security is enforced - the user must have roles on the layout, otherwise an unauthorized response is returned."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Portlet added to layout successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - layout already contains portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - insufficient permissions or restricted portlet",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - user lacks layout permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet or layout not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/custom/{portletId}/_addtolayout/{layoutId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response addContentPortletToLayout(@Context final HttpServletRequest request,
                                                    @Parameter(description = "Portlet ID to add to layout", required = true) @PathParam("portletId") final String portletId,
                                                    @Parameter(description = "Layout ID to add portlet to", required = true) @PathParam("layoutId") final String layoutId)
            throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet("roles")
                .init();

        final User user = initData.getUser();

        final PortletAPI portletAPI = APILocator.getPortletAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        if (!portletAPI.canAddPortletToLayout(portletId)) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                            user.getUserId(), "custom.content.portlet.add.restricted", portletId);
        }

        final Portlet portlet = portletAPI.findPortlet(portletId);
        if (null == portlet || UtilMethods.isNotSet(portlet.getPortletId())) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.not.found",user.getUserId(), portletId);
        }

        final Layout layout = layoutAPI.loadLayout(layoutId);
        if (null == layout || UtilMethods.isNotSet(layout.getId())) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.layout.not.found",user.getUserId(), layoutId);
        }

        final List<Layout> userLayouts = layoutAPI.loadLayoutsForUser(user);
        if (!userLayouts.contains(layout)) {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.user.layout.permission",user.getUserId(), layout.getId());
        }

        final List<String> portletIds = new ArrayList<>(layout.getPortletIds());

        if(!portletIds.contains(portletId)){
            portletIds.add(portletId);
        } else {
            return ResponseUtil.INSTANCE
                    .getErrorResponse(request, Status.BAD_REQUEST, user.getLocale(),
                            user.getUserId(),
                            "custom.content.portlet.layout.contains.portletId",layout.getId(),portletId);
        }

        layoutAPI.setPortletIdsToLayout(layout, portletIds);

        return Response.ok(new ResponseEntityMapView(
                        Map.of(JSON_RESPONSE_PORTLET_ATTR, portlet.getPortletId(), "layout", layout.getId())))
                .build();

    }

    @Operation(
        summary = "Delete custom portlet",
        description = "Deletes a custom portlet by its ID. Requires roles permission."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Custom portlet deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/custom/{portletId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteCustomPortlet(@Context final HttpServletRequest request, 
                                             @Parameter(description = "Custom portlet ID to delete", required = true) @PathParam("portletId") final String portletId) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet("roles")
                .init();

        try {


            APILocator.getPortletAPI().deletePortlet(portletId);

            return Response.ok(new ResponseEntityMapView(Map.of("message", portletId + " deleted"))).build();

        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    @Operation(
        summary = "Delete personal portlet",
        description = "Deletes a personal portlet for the current user. Requires roles permission."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Personal portlet deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/portletId/{portletId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deletePersonalPortlet(@Context final HttpServletRequest request,
                                                @Parameter(description = "Personal portlet ID to delete", required = true) @PathParam("portletId") final String portletId) {
        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                .requestAndResponse(request, null).rejectWhenNoUser(true).requiredPortlet("roles").init()
                .getUser();

        return deletePortletForRole(request, portletId, user.getUserId());
    }


    @Operation(
        summary = "Delete portlet for role",
        description = "Removes a portlet from a specific role. Automatically removes layouts if they become empty. Requires roles permission and user must be admin or own the role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Portlet removed from role successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required or insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet or role not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/portletId/{portletId}/roleId/{roleId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deletePortletForRole(@Context final HttpServletRequest request,
                                               @Parameter(description = "Portlet ID to remove from role", required = true) @PathParam("portletId") final String portletId, 
                                               @Parameter(description = "Role ID to remove portlet from", required = true) @PathParam("roleId") final String roleId) {

        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                .requestAndResponse(request, null).rejectWhenNoUser(true).requiredPortlet("roles").init()
                .getUser();

        try {


            final Role role = APILocator.getRoleAPI().loadRoleById(roleId);
            final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);

            if (role == null || portlet == null) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                        user.getUserId(), "unable to remove role from portlet");
            }

            if(!user.isAdmin() && !user.getUserId().equals(role.getRoleKey())) {
                return ResponseUtil.INSTANCE.getErrorResponse(request, Response.Status.UNAUTHORIZED, user.getLocale(),
                        user.getUserId(),
                        "Unable to remove portlet for role");
            }




            List<Layout> layouts = APILocator.getLayoutAPI().loadLayoutsForRole(role);
            for (Layout layout : layouts) {
                if (layout.getPortletIds().contains(portletId)) {
                    List<Portlet> portlets = layout.getPortletIds().stream().filter(p -> !p.equals(portletId))
                            .map(p -> APILocator.getPortletAPI().findPortlet(p)).collect(Collectors.toList());

                    if (portlets.isEmpty()) {
                        Logger.info(this.getClass(), "removing layout " + layout.getName() + " from role " + role.getName());
                        APILocator.getRoleAPI().removeLayoutFromRole(layout, role);
                    } else {
                        APILocator.getLayoutAPI().setPortletsToLayout(layout, portlets);
                    }


                }
            }

            return Response.ok(new ResponseEntityMapView(Map.of("message", portletId + " deleted"))).build();

        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    @Operation(
        summary = "Get portlet details",
        description = "Returns detailed information about a specific portlet by its ID. Requires appropriate permissions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Portlet details retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Portlet not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/{portletId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findPortlet(@Context final HttpServletRequest request,
                                      @Parameter(description = "Portlet ID to retrieve", required = true) @PathParam("portletId") final String portletId) {

        final User user = new InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(portletId)
                .init().getUser();

        final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        if(null == portlet){
            return ResponseUtil.INSTANCE.getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                    user.getUserId(),
                    "Unable to find portlet");
        }
        return Response.ok(new ResponseEntityMapView(
                Map.of("response", portlet))).build();

    }

    @Operation(
        summary = "Check portlet access",
        description = "Checks if the current user has access to the specified portlet. Returns a boolean result."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Access check completed successfully",
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
    @JSONP
    @Path("/{portletId}/_doesuserhaveaccess")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response doesUserHaveAccessToPortlet(@Context final HttpServletRequest request,
                                                      @Parameter(description = "Portlet ID to check access for", required = true) @PathParam("portletId") final String portletId) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        try {
            return Response.ok(new ResponseEntityMapView(Map.of("response", APILocator.getLayoutAPI()
                    .doesUserHaveAccessToPortlet(portletId, initData.getUser())))).build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @Operation(
        summary = "Get content creation URL",
        description = "Returns the action URL to fire the create content modal for the specified content type. Handles special cases like calendar events."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Content creation URL retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid content type variable",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Content type not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @Path("/_actionurl/{contentTypeVariable}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getCreateContentURL(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse httpResponse,
                                              @Parameter(description = "Content type variable name", required = true) @PathParam("contentTypeVariable") String contentTypeVariable,
                                              @Parameter(description = "Language ID (optional, defaults to user's language)") @QueryParam("language_id") String languageId)
            throws DotDataException, DotSecurityException {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        final User user = initData.getUser();
        final String contentTypeId = APILocator.getContentTypeAPI(user).find(contentTypeVariable).id();
        final String strutsAction = "calendarEvent".equals(contentTypeVariable) ?
                "/ext/calendar/edit_event" :
                "/ext/contentlet/edit_contentlet";

        return Response.ok(
                            new ResponseEntityStringView(
                                    ContentTypeUtil.getInstance().getActionUrl(request,contentTypeId,user,strutsAction, languageId)))
                    .build();
    }
}
