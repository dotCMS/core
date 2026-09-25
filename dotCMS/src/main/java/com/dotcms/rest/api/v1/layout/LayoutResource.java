package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import java.io.Serializable;
import java.util.List;

/**
 * REST resource for navigation sections (layouts) as the Tools portlet manages them: the ordered
 * list of sections in the back-end left menu, each with its icon, position and ordered tools.
 * <p>
 * Reads require an authenticated back-end user who holds the {@code tools} or {@code tools-beta}
 * portlet in a granted section, or the CMS Administrator role; a missing grant answers 401, as
 * every portlet-gated resource does. Writes additionally require the CMS Administrator role,
 * following the role-layout operations on {@code /v1/roles/layouts}; a portlet holder who is not
 * an administrator answers 403. Every successful write and every administrator refusal is
 * recorded in the security log.
 *
 * @author hassandotcms
 */
@Path("/v1/layouts")
@Tag(name = "Administration")
@SwaggerCompliant(value = "Navigation section management for the Tools portlet", batch = 1)
public class LayoutResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Portlet ids that open this resource during the Tools Beta. Replace with
     * {@code PortletID.TOOLS} / {@code PortletID.TOOLS_BETA} once #37574 (PR #37678) merges.
     */
    static final String[] TOOLS_PORTLET_IDS = {"tools", "tools-beta"};

    private static final String OP_CREATE = "create";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";
    private static final String OP_REORDER = "reorder";
    private static final String OP_SET_TOOLS = "set-tools";

    private final WebResource webResource;
    private final LayoutAPI layoutApi;
    private final RoleAPI roleApi;
    private final LayoutHelper helper;

    /**
     * Default constructor used by the JAX-RS runtime.
     */
    public LayoutResource() {
        this(new WebResource(new ApiProvider()), APILocator.getLayoutAPI(), APILocator.getPortletAPI(),
                APILocator.getRoleAPI());
    }

    @VisibleForTesting
    LayoutResource(final WebResource webResource, final LayoutAPI layoutApi, final PortletAPI portletApi,
                   final RoleAPI roleApi) {
        this.webResource = webResource;
        this.layoutApi = layoutApi;
        this.roleApi = roleApi;
        this.helper = new LayoutHelper(layoutApi, portletApi);
    }

    // ==================== read ====================

    /**
     * Lists every navigation section in the order the back-end menu shows them, each with its
     * id, name, icon, position, ordered tool ids and one localized tool title per id.
     *
     * @param request  the current request
     * @param response the current response
     * @return the sections in navigation order
     * @throws DotDataException if the sections cannot be read
     */
    @Operation(
            operationId = "listNavigationSections",
            summary = "List every navigation section",
            description = "Returns every navigation section (layout) in the order the back-end left menu shows "
                    + "them. Each section carries its id, name, icon, position, the ordered ids of its tools and "
                    + "one localized title per tool id, aligned with portletIds. Sections holding tools the tools "
                    + "catalog hides are still returned as stored. Requires an authenticated back-end user with "
                    + "access to the tools or tools-beta portlet, or the CMS Administrator role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Sections retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionListView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or the caller holds neither the "
                            + "tools nor the tools-beta portlet and is not a CMS Administrator",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@Context final HttpServletRequest request,
                         @Context final HttpServletResponse response) throws DotDataException {

        final User user = initRead(request, response);
        final List<Layout> layouts = layoutApi.findAllLayouts();
        Logger.debug(this, () -> "Listing " + layouts.size() + " navigation sections for user " + user.getUserId());
        return Response.ok(new ResponseEntitySectionListView(helper.toViews(layouts, user))).build();
    }

    // ==================== create / update / delete ====================

    /**
     * Creates a navigation section from a name and an icon. The section is placed after every
     * existing one and holds no tools.
     *
     * @param request  the current request
     * @param response the current response
     * @param form     the name and icon
     * @return the saved section
     * @throws DotDataException     if the write fails
     * @throws DotSecurityException if the caller is not a CMS Administrator
     */
    @Operation(
            operationId = "createNavigationSection",
            summary = "Create a navigation section",
            description = "Creates a section from a name and an icon. The new section is placed after every "
                    + "existing section and holds no tools. The name is trimmed, must be present, at most 255 "
                    + "characters and unique among sections; the icon is at most 255 characters and may be empty. "
                    + "Requires a CMS Administrator who also holds the tools or tools-beta portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Section created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - blank or over-long name, over-long icon, or duplicate name",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or no tools / tools-beta portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @RequestBody(description = "Name and icon of the new section", required = true,
                                   content = @Content(schema = @Schema(implementation = SectionForm.class)))
                           final SectionForm form) throws DotDataException, DotSecurityException {

        final User user = initWrite(request, response, OP_CREATE);
        requireBody(form);
        final Layout layout = new Layout();
        layout.setName(helper.validateName(form.getName()));
        layout.setDescription(helper.validateIcon(form.getIcon()));
        layout.setTabOrder(helper.nextTabOrder(layoutApi.findAllLayouts()));
        helper.saveOrDuplicate(layout);
        logWrite(user, OP_CREATE, layout.getId());
        return Response.ok(new ResponseEntitySectionView(helper.toView(layoutApi.loadLayout(layout.getId()), user))).build();
    }

    /**
     * Renames and re-icons a navigation section. Position and tools are left as they are.
     *
     * @param request  the current request
     * @param response the current response
     * @param layoutId the section id
     * @param form     the new name and icon
     * @return the saved section
     * @throws DotDataException     if the write fails
     * @throws DotSecurityException if the caller is not a CMS Administrator
     */
    @Operation(
            operationId = "updateNavigationSection",
            summary = "Rename and re-icon a navigation section",
            description = "Changes a section's name and icon together; its position and its tools are untouched. "
                    + "Same name and icon rules as create. Getting Started may be renamed and re-iconed. "
                    + "Requires a CMS Administrator who also holds the tools or tools-beta portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Section updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - blank or over-long name, over-long icon, or duplicate name",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or no tools / tools-beta portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "Not found - no section has that id",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{layoutId}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @Parameter(description = "Navigation section id", required = true)
                           @PathParam("layoutId") final String layoutId,
                           @RequestBody(description = "New name and icon", required = true,
                                   content = @Content(schema = @Schema(implementation = SectionForm.class)))
                           final SectionForm form) throws DotDataException, DotSecurityException {

        final User user = initWrite(request, response, OP_UPDATE);
        requireBody(form);
        final String name = helper.validateName(form.getName());
        final String icon = helper.validateIcon(form.getIcon());
        final Layout layout = helper.findSectionOrThrow(layoutId);
        layout.setName(name);
        layout.setDescription(icon);
        helper.saveOrDuplicate(layout);
        logWrite(user, OP_UPDATE, layoutId);
        return Response.ok(new ResponseEntitySectionView(helper.toView(layoutApi.loadLayout(layoutId), user))).build();
    }

    /**
     * Deletes a navigation section together with its tool list and every grant of it to a role
     * or a user. The product's Getting Started section cannot be deleted.
     *
     * @param request  the current request
     * @param response the current response
     * @param layoutId the section id
     * @return the remaining sections in navigation order
     * @throws DotDataException     if the write fails
     * @throws DotSecurityException if the caller is not a CMS Administrator
     */
    @Operation(
            operationId = "deleteNavigationSection",
            summary = "Delete a navigation section",
            description = "Removes the section, its tool list and every grant of it to a role or a user, so "
                    + "users who reached a tool only through this section lose it from their menu. The Getting "
                    + "Started section cannot be deleted. Returns the remaining sections in navigation order. "
                    + "Requires a CMS Administrator who also holds the tools or tools-beta portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Section deleted; the remaining sections are returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionListView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - the Getting Started section cannot be deleted",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or no tools / tools-beta portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "Not found - no section has that id",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{layoutId}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@Context final HttpServletRequest request,
                           @Context final HttpServletResponse response,
                           @Parameter(description = "Navigation section id", required = true)
                           @PathParam("layoutId") final String layoutId) throws DotDataException, DotSecurityException {

        final User user = initWrite(request, response, OP_DELETE);
        final Layout layout = helper.findSectionOrThrow(layoutId);
        if (helper.isGettingStarted(layout)) {
            throw new BadRequestException("The Getting Started section cannot be deleted");
        }
        layoutApi.removeLayout(layout);
        logWrite(user, OP_DELETE, layoutId);
        return Response.ok(new ResponseEntitySectionListView(helper.toViews(layoutApi.findAllLayouts(), user))).build();
    }

    // ==================== reorder / set tools ====================

    /**
     * Rewrites the navigation position of every section from one full ordered list of ids, as a
     * single step: either every position changes or none does, and open sessions receive one
     * refresh.
     *
     * @param request  the current request
     * @param response the current response
     * @param form     every section id exactly once, in the new order
     * @return the sections in the new order
     * @throws DotDataException     if the write fails
     * @throws DotSecurityException if the caller is not a CMS Administrator
     */
    @Operation(
            operationId = "reorderNavigationSections",
            summary = "Reorder every navigation section",
            description = "Rewrites the position of every section from one full ordered list of section ids. The "
                    + "list must contain every existing section id exactly once; a missing, unknown or repeated "
                    + "id is rejected and no position changes. Positions are written strictly increasing in the "
                    + "order sent, in one transaction, and open sessions receive a single refresh. Requires a CMS "
                    + "Administrator who also holds the tools or tools-beta portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Sections reordered; the full list is returned in the new order",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionListView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - an id is missing, unknown or repeated; nothing was written",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or no tools / tools-beta portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/_reorder")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reorder(@Context final HttpServletRequest request,
                            @Context final HttpServletResponse response,
                            @RequestBody(description = "Every section id exactly once, in the new navigation order",
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = SectionOrderForm.class)))
                            final SectionOrderForm form) throws DotDataException, DotSecurityException {

        final User user = initWrite(request, response, OP_REORDER);
        requireBody(form);
        final List<String> sent = null == form.getLayoutIds() ? List.of() : form.getLayoutIds();
        // Check completeness and write in one transaction, so a section created by another admin
        // between the check and the write cannot be left out of the order.
        LocalTransaction.wrap(() -> {
            helper.validateOrder(sent, layoutApi.findAllLayouts());
            layoutApi.setTabOrders(helper.positions(sent));
        });
        logWrite(user, OP_REORDER, null);
        return Response.ok(new ResponseEntitySectionListView(helper.toViews(layoutApi.findAllLayouts(), user))).build();
    }

    /**
     * Replaces the ordered tools of one section from a full ordered list of tool ids. This covers
     * adding, removing and reordering tools in one call.
     *
     * @param request  the current request
     * @param response the current response
     * @param layoutId the section id
     * @param form     the full ordered list of tool ids
     * @return the sections in navigation order, the target carrying the new list
     * @throws DotDataException     if the write fails
     * @throws DotSecurityException if the caller is not a CMS Administrator
     */
    @Operation(
            operationId = "setNavigationSectionTools",
            summary = "Replace the ordered tools of a navigation section",
            description = "Replaces the section's tools with the full ordered list sent, which covers adding, "
                    + "removing and reordering in one call. Every id must name a registered portlet the product "
                    + "allows in a section and may appear once; otherwise the request is rejected naming the id and "
                    + "the section is unchanged. Registered tools the tools catalog hides are accepted. An empty "
                    + "list is accepted for every section except Getting Started. Requires a CMS Administrator who "
                    + "also holds the tools or tools-beta portlet."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Tools replaced; the full section list is returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySectionListView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - an id is unregistered, not placeable or repeated, or the list "
                            + "would empty the Getting Started section",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required, or no tools / tools-beta portlet",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404",
                    description = "Not found - no section has that id",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{layoutId}/portlets")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setTools(@Context final HttpServletRequest request,
                             @Context final HttpServletResponse response,
                             @Parameter(description = "Navigation section id", required = true)
                           @PathParam("layoutId") final String layoutId,
                             @RequestBody(description = "Full ordered list of tool ids; may be empty",
                                     required = true,
                                     content = @Content(schema = @Schema(implementation = SectionToolsForm.class)))
                             final SectionToolsForm form) throws DotDataException, DotSecurityException {

        final User user = initWrite(request, response, OP_SET_TOOLS);
        requireBody(form);
        final Layout layout = helper.findSectionOrThrow(layoutId);
        final List<String> portletIds = null == form.getPortletIds() ? List.of() : form.getPortletIds();
        if (portletIds.isEmpty() && helper.isGettingStarted(layout)) {
            throw new BadRequestException("The Getting Started section must keep at least one tool");
        }
        helper.validateToolIds(portletIds);
        layoutApi.setPortletIdsToLayout(layout, portletIds);
        logWrite(user, OP_SET_TOOLS, layoutId);
        return Response.ok(new ResponseEntitySectionListView(helper.toViews(layoutApi.findAllLayouts(), user))).build();
    }

    // ==================== gates and audit ====================

    /**
     * Authenticates the caller for a read: a back-end user holding {@code tools} or
     * {@code tools-beta}, or a CMS Administrator. A missing grant throws the REST
     * {@code SecurityException} that answers 401.
     */
    private User initRead(final HttpServletRequest request, final HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .rejectWhenNoUser(true)
                .requiredPortlet(TOOLS_PORTLET_IDS)
                .requestAndResponse(request, response)
                .init().getUser();
    }

    /**
     * Authenticates the caller for a write: the read gate plus the CMS Administrator role, as the
     * role-layout operations on {@code /v1/roles/layouts} require. A refusal is written to the
     * security log and answers 403.
     */
    private User initWrite(final HttpServletRequest request, final HttpServletResponse response,
                           final String operation) throws DotDataException, DotSecurityException {
        final User user = initRead(request, response);
        if (!roleApi.doesUserHaveRole(user, roleApi.loadCMSAdminRole())) {
            SecurityLogger.logInfo(LayoutResource.class, "unauthorized attempt to " + operation
                    + " navigation section by user " + user.getUserId() + " from " + request.getRemoteHost());
            throw new DotSecurityException("User: '" + user.getUserId() + "' not authorized to " + operation
                    + " navigation sections");
        }
        return user;
    }

    /** Records a successful write in the security log: who, what, and which section if any. */
    private static void logWrite(final User user, final String operation, final String layoutId) {
        SecurityLogger.logInfo(LayoutResource.class, "navigation section " + operation
                + (UtilMethods.isSet(layoutId) ? " " + layoutId : "") + " by user " + user.getUserId());
    }

    private static void requireBody(final Object form) {
        if (null == form) {
            throw new BadRequestException("Request body is required");
        }
    }
}
