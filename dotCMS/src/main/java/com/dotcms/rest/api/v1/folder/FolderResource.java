package com.dotcms.rest.api.v1.folder;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
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
@Path("/v1/folder")
@Tag(name = "Folders", description = "Endpoints for managing folder structure and organization")
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
    @DELETE
    @Path("/{siteName}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteFolders(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        final List<String> paths,
                                        @PathParam("siteName") final String siteName)
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

        return Response.ok(new ResponseEntityView<>(deletedFolders)).build(); // 200
    }

    @POST
    @Path("/createfolders/{siteName}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createFolders(@Context final HttpServletRequest httpServletRequest,
                                        @Context final HttpServletResponse httpServletResponse,
                                        final List<String> paths,
                                        @PathParam("siteName") final String siteName)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();

        final User user = initData.getUser();

            final List<Map<String, Object>> createdFolders = folderHelper.createFolders(paths, siteName, user);

            return Response.ok(new ResponseEntityView<>(createdFolders)).build(); // 200
    }

    @PUT
    @Path("/{id}/file-browser-selected")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response selectFolder(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            @PathParam("id") final String folderId)
            throws DotSecurityException, DotDataException {

        new WebResource.InitBuilder(webResource)
                    .requiredBackendUser(true)
                    .requiredFrontendUser(false)
                    .requestAndResponse(httpServletRequest, httpServletResponse)
                    .init();

        BrowserUtil.setSelectedLastFolder(httpServletRequest, folderId);

        return Response.ok().build(); // 200
    }

    @GET
    @Path ("/sitename/{siteName}/uri/{uri : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadFolderByURI(@Context final HttpServletRequest httpServletRequest,
                                          @Context final HttpServletResponse httpServletResponse,
                                          @PathParam("siteName") final String siteName,
                                          @PathParam("uri") final String uri){
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        try{
            final String uriParam = !uri.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(uri) : uri;
            final Folder folder = folderHelper.loadFolderByURI(siteName,user,uriParam);
            response = Response.ok( new ResponseEntityView(folder) ).build();
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
    @GET
    @Path ("/siteId/{siteId}/path/{path : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response loadFolderAndSubFoldersByPath(@Context final HttpServletRequest httpServletRequest,
                                                      @Context final HttpServletResponse httpServletResponse,
                                                      @PathParam("siteId") final String siteId,
                                                      @PathParam("path") final String path) throws  DotDataException, DotSecurityException   {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .rejectWhenNoUser(true)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(httpServletRequest, httpServletResponse)
                        .init();
        final User user = initData.getUser();

        return Response.ok(new ResponseEntityView(folderHelper.loadFolderAndSubFoldersByPath(siteId,path, user))).build(); // 200
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
    @POST
    @Path ("/byPath")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response findSubFoldersByPath(@Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse,
            final SearchByPathForm searchByPathForm
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

        return Response.ok(new ResponseEntityView<>(folderHelper.findSubFoldersPathByParentPath(siteId,folderPath, user))).build(); // 200
    }

    /**
     * This endpoint will try to retrieve folder if exists, otherwise 404
     *
     * @return Folder
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path ("/{folderId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response findFolderById(@Context final HttpServletRequest httpServletRequest,
                                         @Context final HttpServletResponse httpServletResponse,
                                         @PathParam("folderId") final String folderId
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
                Response.ok(new ResponseEntityView(folder)).build(); // 200
    }

}
