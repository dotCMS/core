package com.dotcms.rest.api.v1.browser;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityMapStringObjectView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper;
import com.dotcms.rest.api.v1.folder.ResponseEntityFolderView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper.ACTIVE_FOLDER_ID;

/**
 * Expose the Browser functionality such as get the contents in a folder
 * @author jsanca
 */
@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Tag(name = "Browser Tree")
@Path("/v1/browser")
public class BrowserResource {

    public  final static String VERSION       = "1.0";
    private final BrowserAPI browserAPI;

    public BrowserResource() {

        this(APILocator.getBrowserAPI());
    }

    @VisibleForTesting
    public BrowserResource(final BrowserAPI browserAPI) {
        this.browserAPI = browserAPI;
    }

    @Operation(
        summary = "Get selected folder",
        description = "Retrieves the currently selected folder from the site browser session. Returns the folder information if one is selected, otherwise returns 404."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Selected folder retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFolderView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "No folder currently selected in session",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @Path("/selectedfolder")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSelectFolder(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();

        final Optional<String> selectedPathOpt = Optional.ofNullable((String) request.getSession().getAttribute(ACTIVE_FOLDER_ID));
        return selectedPathOpt.isPresent()?
                Response.ok(new ResponseEntityView<>(APILocator.getFolderAPI().find(selectedPathOpt.get(), user, false))).build():
                Response.status(Response.Status.NOT_FOUND).build();
    }

    @Operation(
        summary = "Set selected folder",
        description = "Sets the selected folder in the site browser session. Next time the site browser is opened, it will expand to this selected folder."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder selection updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityBooleanView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid folder path",
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
    @Path("/selectedfolder")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response selectFolder(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                         description = "Folder selection form with path", 
                                         required = true,
                                         content = @Content(schema = @Schema(implementation = OpenFolderForm.class))
                                     ) final OpenFolderForm openFolderForm) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initData.getUser();
        final boolean respectFrontendRoles = PageMode.get(request).respectAnonPerms;
        final String folderPath      = openFolderForm.getPath();
        final BrowserTreeHelper browserTreeHelper = BrowserTreeHelper.getInstance();

        Logger.debug(this, ()-> "Selecting the folder on the site browser: " + folderPath);
        browserTreeHelper.selectFolder(request, folderPath, user, respectFrontendRoles);

        return Response.ok(new ResponseEntityView<>(Boolean.TRUE)).build();
    }

    @Operation(
        summary = "Get folder content",
        description = "Retrieves folder contents with extensive filtering options. Can get host or specific folder contents, including archived/working content, folders, pages, files, and dotAssets. Supports filtering by extensions, MIME types, and various content states."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder content retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapStringObjectView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid query parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFolderContent(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                    description = "Browser query form with filtering and pagination options", 
                                                    required = true,
                                                    content = @Content(schema = @Schema(implementation = BrowserQueryForm.class))
                                                ) final BrowserQueryForm browserQueryForm) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final long languageId = browserQueryForm.getLanguageId() > 0?
                browserQueryForm.getLanguageId(): WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        Logger.debug(this, "Getting folder contents, browser query form: " + browserQueryForm);

        return Response.ok(new ResponseEntityView<>(this.browserAPI.getFolderContent(
                BrowserQuery.builder()
                        .showDotAssets(browserQueryForm.isShowDotAssets())
                        .showLinks(browserQueryForm.isShowLinks())
                        .showExtensions(browserQueryForm.getExtensions())
                        .withFilter(browserQueryForm.getFilter())
                        .withHostOrFolderId(browserQueryForm.getHostFolderId())
                        .withLanguageId(languageId)
                        .offset(browserQueryForm.getOffset())
                        .showFiles(browserQueryForm.isShowFiles())
                        .showPages(browserQueryForm.isShowPages())
                        .showFolders(browserQueryForm.isShowFolders())
                        .showArchived(browserQueryForm.isShowArchived())
                        .showWorking(browserQueryForm.isShowWorking())
                        .showMimeTypes(browserQueryForm.getMimeTypes())
                        .maxResults(browserQueryForm.getMaxResults())
                        .sortBy(browserQueryForm.getSortBy())
                        .sortByDesc(browserQueryForm.isSortByDesc())
                        .withUser(initData.getUser())
                        .build()))
        ).build();
    }
}
