package com.dotcms.rest.api.v1.folder;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityListMapView;
import com.dotcms.rest.ResponseEntityListStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.BrowserUtil;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jasontesser on 9/28/16.
 */
@SwaggerCompliant(value = "Site architecture and template management APIs", batch = 3)
@Path("/v1/folder")
@Tag(name = "Folders")
public class FolderResource implements Serializable {

    private final WebResource webResource;
    private final FolderHelper folderHelper;

    public FolderResource() {
        this(new WebResource(),
                FolderHelper.getInstance());
    }

    @VisibleForTesting
    public FolderResource(final WebResource webResource,
                          final FolderHelper folderHelper) {

        this.webResource = webResource;
        this.folderHelper = folderHelper;
    }

    /**
     * Delete one or more path for a site
     * @param httpServletRequest
     * @param httpServletResponse
     * @param paths
     * @param siteName
     * @return List of folders deleted
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Operation(
        summary = "Delete folders",
        description = "Deletes one or more folders by their paths within a specified site. Returns a list of successfully deleted folders."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folders deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid paths or site name",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to delete folders",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during folder deletion",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{siteName}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteFolders(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        @RequestBody(description = "List of folder paths to delete", required = true) final List<String> paths,
                                        @Parameter(description = "Site name where folders are located", required = true) @PathParam("siteName") final String siteName)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();

        final User user = initData.getUser();
        final List<String> deletedFolders = new ArrayList<>();

        final Host host = APILocator.getHostAPI().findByName(siteName, user, true);
        if(!UtilMethods.isSet(host)) {

            throw new IllegalArgumentException(String.format(" Couldn't find any host with name `%s` ",siteName));
        }

        Logger.debug(this, ()-> "Deleting the folders: " + paths);

        for (final String path : paths) {

            final Folder folder = folderHelper.loadFolderByURI(host.getIdentifier(), user, path);
            if (null != folder) {

                Logger.debug(this, ()-> "Deleting the folder: " + path);
                folderHelper.deleteFolder (folder, user);
                deletedFolders.add(path);
            } else {

                Logger.error(this, "The folder does not exists: " + path);
                throw new DoesNotExistException("The folder does not exists: " + path);
            }
        }

        return Response.ok(new ResponseEntityListStringView(deletedFolders)).build(); // 200
    }

    @Operation(
        summary = "Create folders",
        description = "Creates one or more folders by their paths within a specified site. Returns a list of successfully created folders with their details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folders created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListMapView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid paths or site name",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to create folders",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", 
                    description = "Conflict - folder already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during folder creation",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/createfolders/{siteName}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response createFolders(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        @RequestBody(description = "List of folder paths to create", required = true) final List<String> paths,
                                        @Parameter(description = "Site name where folders will be created", required = true) @PathParam("siteName") final String siteName)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();

        final User user = initData.getUser();

            final List<Map<String, Object>> createdFolders = folderHelper.createFolders(paths, siteName, user);

            return Response.ok(new ResponseEntityListMapView(createdFolders)).build(); // 200
    }

    @Operation(
        summary = "Select folder in file browser",
        description = "Marks a folder as selected in the file browser interface. This is used for UI state management to track which folder is currently selected by the user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder selected successfully (no body)"),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access folder",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error selecting folder",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{id}/file-browser-selected")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response selectFolder(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @Parameter(description = "Folder ID to select", required = true) @PathParam("id") final String folderId)
            throws DotSecurityException, DotDataException {

        new WebResource.InitBuilder(webResource)
                    .requiredBackendUser(true)
                    .requiredFrontendUser(false)
                    .requestAndResponse(httpServletRequest, httpServletResponse)
                    .init();

        BrowserUtil.setSelectedLastFolder(httpServletRequest, folderId);

        return Response.ok().build(); // 200
    }

    @Operation(
        summary = "Load folder by URI",
        description = "Loads a specific folder by its URI path within a site. The URI should be the folder's path relative to the site root."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder loaded successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFolderView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access folder",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder or site not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error loading folder",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path ("/sitename/{siteName}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response loadFolderByURI(@Context final HttpServletRequest httpServletRequest,
                                          @Context final HttpServletResponse httpServletResponse,
                                          @Parameter(description = "Site name where the folder is located", required = true) @PathParam("siteName") final String siteName,
                                          @Parameter(description = "URI path of the folder to load", required = true) @PathParam("uri") final String uri){
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        try{
            final String uriParam = !uri.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(uri) : uri;
            final Folder folder = folderHelper.loadFolderByURI(siteName,user,uriParam);
            response = Response.ok( new ResponseEntityFolderView(folder) ).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this, "Error getting folder for URI", e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }


    /***
     * This endpoint finds a folder by the given path and
     * returns the requested folder and all the subFolders of it, respecting the user
     * permissions.
     *
     * @param siteId siteId where the folder lives
     * @param path path of the folder to find
     * @return FolderView with the info of the folder requested and the subFolders
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Load folder and subfolders by path",
        description = "Loads a folder and all its subfolders by path within a specific site. Returns the folder hierarchy that the user has permissions to access."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder and subfolders loaded successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFolderWithSubfoldersView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access folder",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Site or folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error loading folder hierarchy",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path ("/siteId/{siteId}/path/{path : .+}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response loadFolderAndSubFoldersByPath(@Context final HttpServletRequest httpServletRequest,
                                                      @Context final HttpServletResponse httpServletResponse,
                                                      @Parameter(description = "Site ID where the folder is located", required = true) @PathParam("siteId") final String siteId,
                                                      @Parameter(description = "Path of the folder to load with subfolders", required = true) @PathParam("path") final String path) throws  DotDataException, DotSecurityException   {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .rejectWhenNoUser(true)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();
        final User user = initData.getUser();

        return Response.ok(new ResponseEntityFolderWithSubfoldersView(folderHelper.loadFolderAndSubFoldersByPath(siteId,path, user))).build(); // 200
    }

    /**
     * This endpoint is to retrieve subfolders of a given path,
     * will also filter these subfolders by the path sent. The subfolders returned will be the ones
     * the user has permissions over.
     *
     * E.g of the behavior of the endpoint:
     *
     * SiteBrowser Tree:
     * default
     * 	folder1
     * 		subfolder1
     * 		subfolder2
     *                  testsubfolder3
     * 	folder2
     * 		subfolder1
     * 	testfolder3
     *
     * default_copy
     * 	folder1_copy
     * 		subfolder1_copy
     * 		subfolder2_copy
     *                  testsubfolder3_copy
     * 	folder2_copy
     * 		subfolder1_copy
     * 	testfolder3_copy
     *
     * 	Value Sent | Expected Result
     * ------------ | -------------
     * //default/ | folder1, folder2, testfolder3
     * //default/fol | folder1, folder2
     * //default/fol/ | Nothing
     * //default/bla | Nothing
     * //default/folder1/ | subfolder1, subfolder2, testsubfolder3
     * //default/folder1/s | subfolder1, subfolder2
     * //default/folder1/b | Nothing
     * / | folder1,folder2,testfolder3,folder1_copy,folder2_copy,testfolder3_copy
     * /f | folder1,folder2,folder1_copy,folder2_copy
     * /folder1/ | subfolder1, subfolder2, testsubfolder3,folder1_copy,folder2_copy,testfolder3_copy
     * /folder1/s | subfolder1, subfolder2,subfolder1_copy, subfolder2_copy
     * f | folder1,folder2,folder1_copy,folder2_copy
     * folder1/ | subfolder1, subfolder2, testsubfolder3,folder1_copy,folder2_copy,testfolder3_copy
     * folder1/s | subfolder1, subfolder2,subfolder1_copy, subfolder2_copy
     *
     *
     * @param searchByPathForm path to look for the sub-folders
     * @return List of {@link FolderSearchResultView}
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Find subfolders by path",
        description = "Searches for subfolders within a specified path that match the search criteria. Returns subfolders that the user has permissions to access, with optional filtering by path pattern."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Subfolders found successfully",
                    content = @Content(mediaType = "application/json", 
                                      schema = @Schema(implementation = ResponseEntityFolderSearchResultView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid search criteria",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access folders",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during folder search",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path ("/byPath")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findSubFoldersByPath(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @RequestBody(description = "Search criteria for finding subfolders by path", required = true) final SearchByPathForm searchByPathForm
            ) throws  DotDataException, DotSecurityException   {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .rejectWhenNoUser(true)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();

        final User user = initData.getUser();

        if(!UtilMethods.isSet(searchByPathForm) ||
                UtilMethods.isNotSet(searchByPathForm.getPath())){
            throw new BadRequestException("Path property must be send");
        }

        String path = searchByPathForm.getPath().toLowerCase();
        String siteId = null;
        String folderPath = path;

        if(path.startsWith(StringPool.DOUBLE_SLASH)){
            //Removes // to search the host
            path = path.startsWith(StringPool.DOUBLE_SLASH) ? path.substring(2) : path;
            final String sitePath = path.split(StringPool.FORWARD_SLASH,2)[0];
            final Host site = APILocator.getHostAPI().findByName(sitePath, user,false);
            if(null == site){
                throw new DoesNotExistException(String.format(" Couldn't find any host with name `%s` ",sitePath));
            }else{
                siteId = site.getIdentifier();
            }
            folderPath = path.split(StringPool.FORWARD_SLASH,2).length == 2  ?
                    path.split(StringPool.FORWARD_SLASH,2)[1] :
                    StringPool.FORWARD_SLASH;
        }

        folderPath = !folderPath.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(folderPath) : folderPath;

        return Response.ok(new ResponseEntityFolderSearchResultView(folderHelper.findSubFoldersPathByParentPath(siteId,folderPath, user))).build(); // 200
    }

    /**
     * This endpoint will try to retrieve folder if exists, otherwise 404
     *
     * @return Folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Find folder by ID",
        description = "Retrieves a specific folder by its unique identifier. Returns the folder details if the user has permission to access it."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Folder found successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFolderView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access folder",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Folder not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error retrieving folder",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path ("/{folderId}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findFolderById(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @Parameter(description = "Unique identifier of the folder to retrieve", required = true) @PathParam("folderId") final String folderId
    ) throws  DotDataException, DotSecurityException   {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .rejectWhenNoUser(true)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();

        final User user = initData.getUser();

        Logger.debug(this, ()-> "Finding the folder: " + folderId);

        final Folder folder = APILocator.getFolderAPI().find(folderId, user,
                PageMode.get(httpServletRequest).respectAnonPerms);

        return null == folder || !UtilMethods.isSet(folder.getIdentifier())?
                Response.status(Response.Status.NOT_FOUND).build():
                Response.ok(new ResponseEntityFolderView(folder)).build(); // 200
    }

}
