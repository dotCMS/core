package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
 * following the role-layout operations on {@code /v1/roles/layouts}.
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
}
