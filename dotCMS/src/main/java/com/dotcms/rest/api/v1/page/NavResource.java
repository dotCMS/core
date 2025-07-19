package com.dotcms.rest.api.v1.page;


import com.dotcms.rendering.velocity.viewtools.navigation.NavResult;
import com.dotcms.rendering.velocity.viewtools.navigation.NavTool;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;

@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Path("/v1/nav")
@Tag(name = "Navigation")
public class NavResource {


    private final WebResource webResource;
    private final int defaultDepth = 1;


    /**
     * Creates an instance of this REST end-point.
     */
    public NavResource() {
        this(new WebResource());
    }

    @VisibleForTesting
    protected NavResource(final WebResource webResource) {

        this.webResource = webResource;

    }

    @Operation(
        summary = "Get navigation hierarchy",
        description = "Returns navigation metadata in JSON format for objects that have been marked to show on menu. " +
                     "Retrieves hierarchical navigation structure starting from the specified URI path."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Navigation retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid depth or languageId parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized access",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Insufficient permissions to access the host",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Navigation path not found",
                    content = @Content(mediaType = "application/json"))
    })
    @NoCache
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{uri: .*}")
    public final Response loadJson(
            @Context final HttpServletRequest request, 
            @Context final HttpServletResponse response,
            @Parameter(description = "Path to the HTML page or folder to retrieve navigation from", required = true)
            @PathParam("uri") final String uri, 
            @Parameter(description = "Number of navigation levels to include (default: 1)", required = false)
            @QueryParam("depth") final String depth, 
            @Parameter(description = "Language ID for navigation items (default: request language)", required = false)
            @QueryParam("languageId") final String languageId) {

        final InitDataObject auth = webResource.init(request, response, true);
        final User user = auth.getUser();

        try {
            int maxDepth;
            long langId;
            try {
                maxDepth = (UtilMethods.isSet(depth)) ? Integer.parseInt(depth) :
                        defaultDepth;
            }catch(NumberFormatException nfe){
                throw new NumberFormatException("depth-not-a-number");
            }

            try {
                langId = (UtilMethods.isSet(languageId)) ? Long.parseLong(languageId) :
                        WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
            }catch (NumberFormatException nfe){
                throw new NumberFormatException("languageId-not-a-number");
            }

            if (!APILocator.getLanguageAPI().getLanguages().stream()
                    .anyMatch(l -> l.getId() == langId)) {
                throw new IllegalArgumentException("languageId-not-exists");
            }

            final Host h = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            APILocator.getPermissionAPI().checkPermission(h, PermissionLevel.READ, user);

            final ViewContext ctx = new ChainedContext(VelocityUtil.getBasicContext(), request,
                    response, Config.CONTEXT);

            final String path = (!uri.startsWith("/")) ? "/" + uri : uri;
            //Force NavTool to behave as Live when rendering items
            PageMode.setPageMode(request, PageMode.LIVE, false);
            final NavTool tool = new NavTool();
            tool.init(ctx);
            final NavResult nav = tool.getNav(path, langId);

            final Map<String, Object> navMap =
                    (nav != null) ? navToMap(nav, maxDepth, 1) : new HashMap<>();

            if (navMap.isEmpty()) {
                throw new DoesNotExistException("dot.common.http.error.404.header");
            }



            return Response.ok(new ResponseEntityMapView(navMap)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "Exception on NavResource exception message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }


    public static Map<String, Object> navToMap(final NavResult nav, final int maxDepth, final int currentDepth) throws Exception {

        final Map<String, Object> navMap = new HashMap<>();
        navMap.put("title", nav.getTitle());
        navMap.put("target", nav.getTarget());
        navMap.put("code", nav.getCodeLink());
        navMap.put("folder", nav.getFolderId());
        navMap.put("host", nav.getHostId());
        navMap.put("href", nav.getHref());
        navMap.put("languageId", nav.getLanguageId());
        navMap.put("order", nav.getOrder());
        navMap.put("type", nav.getType());
        navMap.put("hash", nav.hashCode());

        if (currentDepth < maxDepth) {
            final List<Map<String, Object>> childs = new ArrayList<>();
            for (final NavResult child : nav.getChildren()) {
                int startDepth=currentDepth;
                childs.add(navToMap(child, maxDepth, ++startDepth));
            }
            navMap.put("children", childs);
        }

        return navMap;
    }

}