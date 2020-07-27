package com.dotcms.rest.api.v1.folder;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.liferay.util.StringPool;
import org.glassfish.jersey.server.JSONP;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.springframework.beans.BeanUtils;
import java.util.Date;
import java.util.LinkedList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;


/**
 * Created by jasontesser on 9/28/16.
 */
@Path("/v1/folder")
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

            return Response.ok(new ResponseEntityView(createdFolders)).build(); // 200
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

    @GET
    @Path ("/sitename/{siteName}/folder/{folder : .+}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response loadFolderChildrenByURIPath(@Context final HttpServletRequest httpServletRequest,
                                                      @Context final HttpServletResponse httpServletResponse,
                                                      @PathParam("siteName") final String siteName,
                                                      @PathParam("folder") final String uri){
        Response response = null;
        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        try{
            final String uriParam = !uri.startsWith(StringPool.FORWARD_SLASH) ? StringPool.FORWARD_SLASH.concat(uri) : uri;
            Host host = APILocator.getHostAPI().findByName(siteName, user, false);
            //final Folder folder = folderHelper.loadFolderByURI(siteName,user,uriParam);
            Folder folder = APILocator.getFolderAPI().findFolderByPath(uriParam, host, user, false);
            if(host==null || folder==null) {
                throw new Exception("No folder found for "+uri+" on site "+siteName);
            }
            CustomFolder root = getFolderStructure(folder, user);
            response = Response.ok( new ResponseEntityView(root) ).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.
            Logger.error(this, "Error gettign folder for URI", e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private CustomFolder getFolderStructure(Folder folder, User user){

        CustomFolder customFolder = convertFrom(folder);

        List<CustomFolder> foldersChildCustoms = new LinkedList<>();
        List<Folder> children = null;
        try {
            children = APILocator.getFolderAPI().findSubFolders(folder, user, false);
        } catch (Exception e) {
            Logger.error(this, "Error getting findSubFolders for folder "+folder.getPath(), e);
        }

        if(children != null && children.size() != 0){
            for(Folder child : children){
                CustomFolder recursiveFolder = getFolderStructure(child, user);
                foldersChildCustoms.add(recursiveFolder);
            }
        }

        customFolder.setCustomFolders(foldersChildCustoms);

        return customFolder;
    }

    private CustomFolder convertFrom(Folder folder){
        CustomFolder customFolder = new CustomFolder();
        try {
            BeanUtils.copyProperties(folder, customFolder);
            //TODO Research why for some reason path is not being copied on jostens dev
            customFolder.setPath(folder.getPath());
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return customFolder;
    }

    @JsonPropertyOrder({ "path", "customFolders" })
    @JsonRootName("folder")
    class CustomFolder {

        private String path;

        private List<CustomFolder> customFolders;

        private String defaultFileType;
        private String filesMasks;
        private Date iDate;
        private String hostId;
        private String identifier;
        private String inode;
        private Date modDate;
        private String name;
        private Boolean showOnMenu;
        private Integer sortOrder;
        private String title;
        private String type;



        public CustomFolder(){}


        public String getDefaultFileType() {
            return defaultFileType;
        }

        public void setDefaultFileType(String defaultFileType) {
            this.defaultFileType = defaultFileType;
        }

        public String getFilesMasks() {
            return filesMasks;
        }

        public void setFilesMasks(String filesMasks) {
            this.filesMasks = filesMasks;
        }

        public Date getiDate() {
            return iDate;
        }

        public void setiDate(Date iDate) {
            this.iDate = iDate;
        }

        public String getHostId() {
            return hostId;
        }

        public void setHostId(String hostId) {
            this.hostId = hostId;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getInode() {
            return inode;
        }

        public void setInode(String inode) {
            this.inode = inode;
        }

        public Date getModDate() {
            return modDate;
        }

        public void setModDate(Date modDate) {
            this.modDate = modDate;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getShowOnMenu() {
            return showOnMenu;
        }

        public void setShowOnMenu(Boolean showOnMenu) {
            this.showOnMenu = showOnMenu;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public CustomFolder(String path){
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @JsonProperty("children")
        public List<CustomFolder> getCustomFolders() {
            return customFolders;
        }

        public void setCustomFolders(List<CustomFolder> customFolders) {
            this.customFolders = customFolders;
        }
    }

}
