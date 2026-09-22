package com.dotcms.rest.api.v1.portlet;

import com.dotcms.exception.ExceptionUtil;
import com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapStringStringView;
import com.dotcms.rest.ResponseEntityView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.liferay.portal.model.Portlet.DATA_VIEW_MODE_KEY;
import static com.liferay.util.StringPool.BLANK;

/**
 * REST resource for portlets, the tools of the back-end navigation.
 * <p>
 * It manages <b>custom content portlets</b> (tools an admin creates to list the content of
 * given base types or content types): create, update and delete under {@code /custom}, plus a
 * single read of one tool's editable configuration under {@code /custom/{portletId}}. Those
 * operations accept a back-end user who holds the {@code roles}, {@code tools} or
 * {@code tools-beta} portlet, or a CMS Administrator; the read and the catalog require
 * {@code tools} or {@code tools-beta}. The delete refuses anything that is not a custom content
 * tool, so tools shipped with the product cannot be removed through it.
 * <p>
 * It also serves the <b>tools catalog</b> ({@code /_catalog}): every portlet that can be placed
 * in a navigation section, with a localized title and an {@code isCustom} flag, as the Tools
 * portlet's Available Tools panel shows it. The remaining operations (add a tool to a section
 * the caller holds, per-role removal, raw portlet lookup, access check, create-content action
 * URL) keep their original behaviour and gates.
 */
@Path("/v1/portlet")
@Tag(name = "Portlets")
public class PortletResource implements Serializable {

    private final WebResource webResource;
    private final PortletAPI portletApi;
    private final ToolCatalogHelper toolCatalogHelper;

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
        this(webResource, portletApi, new ToolCatalogHelper(portletApi));
    }

    @VisibleForTesting
    public PortletResource(final WebResource webResource, final PortletAPI portletApi,
                           final ToolCatalogHelper toolCatalogHelper) {
        this.webResource = webResource;
        this.portletApi = portletApi;
        this.toolCatalogHelper = toolCatalogHelper;
    }

    /**
     * Lists every tool that can be placed in a navigation section, for the Tools portlet's
     * Available Tools panel. Applies the same inclusion rules as the legacy Roles &amp; Tools
     * picker and sorts by title ignoring letter case. Requires an authenticated back-end user
     * with access to the {@code tools} or {@code tools-beta} portlet, or the CMS Administrator
     * role; anyone else gets 401.
     *
     * @param request  the current request
     * @param response the current response
     * @return the catalog rows wrapped in {@link ResponseEntityToolCatalogView}
     */
    @Operation(
            operationId = "getToolsCatalog",
            summary = "List the tools that can be placed in a navigation section",
            description = "Every portlet that can be added to a section, with the same inclusion rules as the "
                    + "legacy Roles & Tools tool picker: portlets excluded from layouts are omitted, and the old "
                    + "Languages tool is omitted while FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET is on. "
                    + "Sorted by title ignoring letter case. Each row carries the id, the localized title and "
                    + "an isCustom flag that is true only for tools an admin created through New Tool. "
                    + "Requires an authenticated back-end user with access to the tools or tools-beta portlet, "
                    + "or the CMS Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Catalog retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityToolCatalogView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - no authenticated back-end user, or the caller holds neither "
                            + "the tools nor the tools-beta portlet and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/_catalog")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getToolsCatalog(@Context final HttpServletRequest request,
                                          @Context final HttpServletResponse response) {
        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.TOOLS.toString(), PortletID.TOOLS_BETA.toString())
                .init().getUser();

        return Response.ok(new ResponseEntityToolCatalogView(toolCatalogHelper.catalog(user))).build();
    }

    /**
     * Creates a custom dotCMS Portlet for a given Base Type or Content Type. Requires an
     * authenticated back-end user with access to the {@code roles}, {@code tools} or
     * {@code tools-beta} portlet, or the CMS Administrator role.
     *
     * @param request  The current instance of the {@link HttpServletRequest}.
     * @param formData The {@link CustomPortletForm} containing the information for the new
     *                 Portlet.
     *
     * @return A {@link Response} object with the ID of the new portlet.
     */
    @Operation(
            operationId = "saveNew",
            summary = "Create a custom content tool",
            description = "Creates a custom content tool that lists the given base types and/or content types. "
                    + "Requires an authenticated back-end user with access to the roles, tools or tools-beta "
                    + "portlet, or the CMS Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Created; the entity carries the stored portlet id under \"portlet\"",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityMapStringStringView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Validation failure (missing name or view mode, unknown content type, ...)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - no authenticated back-end user, or the caller holds none of the "
                            + "roles, tools or tools-beta portlets and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/custom")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveNew(@Context final HttpServletRequest request,
                                  final CustomPortletForm formData) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.ROLES.toString(), PortletID.TOOLS.toString(), PortletID.TOOLS_BETA.toString())
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

            return Response.ok(new ResponseEntityView<>(Map.of(JSON_RESPONSE_PORTLET_ATTR, savedPortlet.getPortletId()))).build();
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when saving new Portlet with ID " +
                    "'%s': %s", portletId, ExceptionUtil.getErrorMessage(e)), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Reads one custom content tool's editable configuration, for the Tools portlet's Edit
     * dialog. Answers 404 for an unknown id and for a tool shipped with the product, which has no
     * custom configuration to edit. Requires an authenticated back-end user with access to the
     * {@code tools} or {@code tools-beta} portlet, or the CMS Administrator role.
     *
     * @param request   the current request
     * @param response  the current response
     * @param portletId the stored id of the custom tool
     * @return the configuration wrapped in {@link ResponseEntityCustomToolView}, or 404
     */
    @Operation(
            operationId = "getCustomTool",
            summary = "Read one custom content tool's configuration",
            description = "Returns the editable configuration of a custom content tool: id, name, base types, "
                    + "content types and data view mode. Requires an authenticated back-end user with access to "
                    + "the tools or tools-beta portlet, or the CMS Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Custom tool found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityCustomToolView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - no authenticated back-end user, or the caller holds neither "
                            + "the tools nor the tools-beta portlet and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "No portlet has this id, or it is a tool shipped with the product and has no "
                            + "custom configuration",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/custom/{portletId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getCustomTool(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response,
                                        @Parameter(description = "Stored id of the custom tool, c_ prefix included", required = true)
                                        @PathParam("portletId") final String portletId) {
        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.TOOLS.toString(), PortletID.TOOLS_BETA.toString())
                .init().getUser();

        final Portlet portlet = portletApi.findPortlet(portletId);
        if (null == portlet || !portletApi.isCustomContentPortlet(portlet)) {
            return customToolNotFound(request, user, portletId);
        }
        return Response.ok(new ResponseEntityCustomToolView(toolCatalogHelper.toCustomToolView(portlet))).build();
    }

    /**
     * Builds the 404 answered when an id is unknown or does not belong to a custom content tool:
     * the standard dotCMS error envelope with one {@code custom.content.portlet.not.found} entry,
     * exactly as the other not-found answers on this resource are built.
     *
     * @param request   the current request
     * @param user      the caller, whose locale selects the message language
     * @param portletId the id that was requested
     * @return a 404 response
     */
    private Response customToolNotFound(final HttpServletRequest request, final User user, final String portletId) {
        return ResponseUtil.INSTANCE.getErrorResponse(request, Status.NOT_FOUND, user.getLocale(),
                user.getUserId(), "custom.content.portlet.not.found", portletId);
    }

    /**
     * Saves a new working version of an existing custom content tool. The form must carry the
     * identifier of the tool. Requires an authenticated back-end user with access to the
     * {@code roles}, {@code tools} or {@code tools-beta} portlet, or the CMS Administrator role.
     *
     * @param request  the current request
     * @param formData the full definition to store; every field is replaced
     * @return the stored portlet id
     */
    @Operation(
            operationId = "updatePortlet",
            summary = "Update a custom content tool",
            description = "Replaces the name, base types, content types and data view mode of an existing custom "
                    + "content tool. Requires an authenticated back-end user with access to the roles, tools or "
                    + "tools-beta portlet, or the CMS Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Updated; the entity carries the stored portlet id under \"portlet\"",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityMapStringStringView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - no authenticated back-end user, or the caller holds none of the "
                            + "roles, tools or tools-beta portlets and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "No portlet has the given id",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/custom")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updatePortlet(@Context final HttpServletRequest request, final CustomPortletForm formData) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.ROLES.toString(), PortletID.TOOLS.toString(), PortletID.TOOLS_BETA.toString())
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

            return Response.ok(new ResponseEntityView<>(Map.of(JSON_RESPONSE_PORTLET_ATTR, newPortlet.getPortletId()))).build();

        } catch (Exception e) {
            Logger.error(this, String.format("An error occurred when updating Portlet with ID " +
                    "'%s': %s", formData.portletId, ExceptionUtil.getErrorMessage(e)), e);
            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    /**
     * This endpoint links a layout with a portlet Security is considered so the user must have
     * roles on the layout otherwise an unauthorized code is returned.
     * @param request
     * @param portletId
     * @param layoutId
     * @return
     * @throws DotDataException
     */
    @PUT
    @Path("/custom/{portletId}/_addtolayout/{layoutId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response addContentPortletToLayout(@Context final HttpServletRequest request,
                                                    @PathParam("portletId") final String portletId,
                                                    @PathParam("layoutId") final String layoutId)
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

        return Response.ok(new ResponseEntityView<>(
                        Map.of(JSON_RESPONSE_PORTLET_ATTR, portlet.getPortletId(), "layout", layout.getId())))
                .build();

    }

    /**
     * Deletes a custom content tool and removes it from every navigation section that contained
     * it; open admin sessions are notified. Only custom content tools can be deleted here: an id
     * that belongs to a tool shipped with the product (Language Variables included) or to no tool
     * at all is answered with 404 and nothing changes. Requires an authenticated back-end user
     * with access to the {@code roles}, {@code tools} or {@code tools-beta} portlet, or the CMS
     * Administrator role.
     *
     * @param request   the current request
     * @param portletId the stored id of the custom tool
     * @return a confirmation message, or 404
     */
    @Operation(
            operationId = "deleteCustomPortlet",
            summary = "Delete a custom content tool",
            description = "Removes the custom content tool and takes it out of every navigation section that "
                    + "contained it. Refuses, with 404 and no change, any id that is not a custom content tool: "
                    + "tools shipped with the product cannot be removed this way. Requires an authenticated "
                    + "back-end user with access to the roles, tools or tools-beta portlet, or the CMS "
                    + "Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Deleted; the entity carries a confirmation under \"message\"",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityMapStringStringView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - no authenticated back-end user, or the caller holds none of the "
                            + "roles, tools or tools-beta portlets and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "No portlet has this id, or it is a tool shipped with the product",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/custom/{portletId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteCustomPortlet(@Context final HttpServletRequest request,
                                              @Parameter(description = "Stored id of the custom tool, c_ prefix included", required = true)
                                              @PathParam("portletId") final String portletId) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.ROLES.toString(), PortletID.TOOLS.toString(), PortletID.TOOLS_BETA.toString())
                .init();
        final User user = initData.getUser();

        try {
            final Portlet portlet = portletApi.findPortlet(portletId);
            if (null == portlet || !portletApi.isCustomContentPortlet(portlet)) {
                Logger.debug(this, () -> String.format("Refusing to delete '%s': not a custom content tool", portletId));
                return customToolNotFound(request, user, portletId);
            }

            portletApi.deletePortlet(portletId);

            return Response.ok(new ResponseEntityView<>(Map.of("message", portletId + " deleted"))).build();

        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    /**
     * Portlet delete
     * @param request
     * @param portletId
     * @return
     */
    @DELETE
    @Path("/portletId/{portletId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deletePersonalPortlet(@Context final HttpServletRequest request,
                                                @PathParam("portletId") final String portletId) {
        final User user = new WebResource.InitBuilder(webResource).requiredBackendUser(true)
                .requestAndResponse(request, null).rejectWhenNoUser(true).requiredPortlet("roles").init()
                .getUser();

        return deletePortletForRole(request, portletId, user.getUserId());
    }


    /**
     * Delete Portlet For Role
     * @param request
     * @param portletId
     * @param roleId
     * @return
     */
    @DELETE
    @Path("/portletId/{portletId}/roleId/{roleId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deletePortletForRole(@Context final HttpServletRequest request,
                                               @PathParam("portletId") final String portletId, @PathParam("roleId") final String roleId) {

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

            return Response.ok(new ResponseEntityView(Map.of("message", portletId + " deleted"))).build();

        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }

    }

    /**
     * This endpoint returns a portlet's details given its id
     * @param request
     * @param portletId
     * @return
     */
    @GET
    @JSONP
    @Path("/{portletId}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findPortlet(@Context final HttpServletRequest request,
                                      @PathParam("portletId") final String portletId) {

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
        return Response.ok(new ResponseEntityView(
                Map.of("response", portlet))).build();

    }

    /**
     * portlet access permis2sion check
     * @param request
     * @param portletId
     * @return
     */
    @GET
    @JSONP
    @Path("/{portletId}/_doesuserhaveaccess")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response doesUserHaveAccessToPortlet(@Context final HttpServletRequest request,
                                                      @PathParam("portletId") final String portletId) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, null)
                .rejectWhenNoUser(true)
                .init();
        try {
            return Response.ok(new ResponseEntityView(Map.of("response", APILocator.getLayoutAPI()
                    .doesUserHaveAccessToPortlet(portletId, initData.getUser())))).build();
        } catch (Exception e) {
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This endpoint is to get the actionURL to fire the create content modal. The content that
     * will be created is the one pass in the contentTypeVariable param.
     *
     * @param request
     * @param httpResponse
     * @param contentTypeVariable - content type variable name
     * @param languageId - The language to be used for the search. If not set, the user's language Id will be used
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @JSONP
    @Path("/_actionurl/{contentTypeVariable}")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getCreateContentURL(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse httpResponse,
                                              @PathParam("contentTypeVariable") String contentTypeVariable,
                                              @QueryParam("language_id") String languageId)
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
                            new ResponseEntityView((
                                    ContentTypeUtil.getInstance().getActionUrl(request,contentTypeId,user,strutsAction, languageId))))
                    .build();
    }
}
