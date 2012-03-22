package com.dotmarketing.portlets.browser.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.ChildrenCondition;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

/**
 * 
 * @author david
 *
 */
public class BrowserAjax {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private FileAPI fileAPI = APILocator.getFileAPI();
	private HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
	private ContentletAPI contAPI = APILocator.getContentletAPI();
	private WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

	String activeHostId = "";
    String activeFolderInode = ""; 
    List<String> openFolders = new ArrayList<String> ();

    String lastSortBy = "name";
    boolean lastSortDirectionDesc = false;
    
    
    /**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPI) {
		BrowserAjax.permissionAPI = permissionAPI;
	}
    
    /**
     * This methods is used to load the entire tree by first time.
     * @return The whole folders tree structure.
     * @throws DotDataException 
     * @throws DotSecurityException 
     */
    public List<Map> getTree(String hostId) throws DotDataException, DotSecurityException {
    	
        WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
        User systemUser = userAPI.getSystemUser();
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(usr.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
		
		List<Host> hosts=new ArrayList<Host>();
		if(!UtilMethods.isSet(hostId) || hostId.equals("allHosts")){
			hostId = "allHosts";
			hosts.addAll(hostAPI.findAll(usr, false));	
		}
		else{
			hosts.add(hostAPI.find(hostId, usr, false));
        }
        List<Map> retList = new ArrayList<Map>();
         for (Host host : hosts) {
        	
        	//Ignore system host
        	if(host.isSystemHost()||host.isArchived())
        	continue;

        	//Obtain maps from hosts to be returned by ajax
            Map<String,Object> hostMap = (Map<String,Object>)host.getMap();
            if (activeHostId.equalsIgnoreCase(host.getIdentifier())|| hosts.size()==1 )  {
                hostMap.put("open", true);
                List<Map> children = getFoldersTree (host, roles);
                hostMap.put("childrenFolders", children);
                hostMap.put("childrenFoldersCount", children.size());
            } else {
                hostMap.put("open", false);
//                hostMap.put("childrenFoldersCount", getSubFoldersCount(host));
            }
            
            java.util.List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(host, roles, usr);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ",e);
			}
            
            hostMap.put("permissions", permissions);
            retList.add(hostMap);
        	
        }     
           
        return retList;
    }
    
    /**
     * Action called every time a user opens a folder using the + (left hand side)
     * @param parentInode Parent folder to be opened
     * @return The subtree structure of folders
     * @throws SystemException 
     * @throws PortalException 
     * @throws DotSecurityException 
     * @throws DotDataException 
     */
    public List<Map> openHostTree (String hostId) throws PortalException, SystemException, DotDataException, DotSecurityException {
    	
        WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
        boolean respectFrontend = !userAPI.isLoggedToBackend(ctx.getHttpServletRequest());

        Host host = hostAPI.find(hostId, usr, respectFrontend); 
        
        if(! UtilMethods.isSet(hostId) || host == null){
        	Host browseHost = hostAPI.find(hostId, APILocator.getUserAPI().getSystemUser(), respectFrontend); 
        	if(browseHost != null){
        		Logger.warn(this, "User " + usr.getUserId() + " cannot browse host id " + hostId + " aka "+ browseHost.getHostname());
        	}
        	else{
        		Logger.warn(this, "User " + usr.getUserId() + " cannot browse host id " + hostId );
        	}
        	return new ArrayList<Map>();
        }
        
        activeHostId = hostId;
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(usr.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e) {
			Logger.error(BrowserAjax.class,e.getMessage(),e);
		}
        
        return getFoldersTree (host, roles);
    }

    /**
     * Action called every time a user opens a folder using the + (left hand side)
     * @param parentInode Parent folder to be opened
     * @return The subtree structure of folders
     * @throws DotDataException 
     * @throws DotSecurityException 
     * @throws DotHibernateException 
     */
    public List<Map> openFolderTree (String parentInode) throws DotHibernateException, DotSecurityException, DotDataException {
        WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
        Role[] roles = new Role[]{};
        if(usr != null){
			try {
				roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(usr.getUserId()).toArray(new Role[0]);
			}catch (NullPointerException e) {
				Logger.debug(this, "array was null");
			} catch (DotDataException e) {
				Logger.error(BrowserAjax.class,e.getMessage(),e);
			}
        }
        
        
        Folder f = (Folder) APILocator.getFolderAPI().find(parentInode, usr, false);
        openFolders.add(parentInode);
        return getFoldersTree (f, roles);
    }

    /**
     * Action called everytime a user closes a folder using the - (left hand side)
     * @param parentInode Parent folder to be opened
     * @return The subtree structure of folders
     */
    public void closeFolderTree (String parentInode) {
        openFolders.remove(parentInode);
    }


    @SuppressWarnings("unchecked")
	public List<Map<String, Object>> openFolderContent (String parentInode, String sortBy, boolean showArchived) throws DotHibernateException, DotSecurityException, DotDataException {
    	
        activeFolderInode = parentInode;
        this.lastSortBy = sortBy;
        
    	if (sortBy != null && UtilMethods.isSet(sortBy)) {
    		if (sortBy.equals(lastSortBy)) {
    			this.lastSortDirectionDesc = !this.lastSortDirectionDesc;
    		}
    		this.lastSortBy = sortBy;
    	}
        
        Map<String, Object> resultsMap = getFolderContent(parentInode, 0, -1, "", null, null, showArchived, false, false, this.lastSortBy, this.lastSortDirectionDesc);
        return (List<Map<String, Object>>) resultsMap.get("list");
    }

	public Map<String, Object> getFolderContent (String folderId, int offset, int maxResults, String filter, List<String> mimeTypes, 
			List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc) throws DotHibernateException, DotSecurityException, DotDataException {
		
		if(!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate";
			sortByDesc = true;
		}
		
		ChildrenCondition cond = new ChildrenCondition();
		cond.working=true;
		cond.deleted=false;
		
        List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>> ();
        
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest req = ctx.getHttpServletRequest();
        ServletContext servletContext = ctx.getServletContext();
        User usr = getUser(req);
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(usr.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}

		Folder f = null;
		try{
			f = (Folder) APILocator.getFolderAPI().find(folderId, usr, false);
		}catch(Exception e){

		}
        if(!noFolders) {
        	if(f!=null){
        		List<Folder> folders = new ArrayList<Folder>();
        		try {
        			folders = folderAPI.findSubFolders(f,userAPI.getSystemUser(),false);
        		} catch (Exception e1) {
        			Logger.error(this, "Could not load folders : ",e1);
        		} 
        		for (Folder folder : folders) {
        			List<Integer> permissions = new ArrayList<Integer>();
        			try {
        				permissions = permissionAPI.getPermissionIdsFromRoles(folder, roles, usr);
        			} catch (DotDataException e) {
        				Logger.error(this, "Could not load permissions : ",e);
        			}
        			if(permissions.contains(PERMISSION_READ)){
        				Map<String, Object> folderMap = folder.getMap();
        				folderMap.put("permissions", permissions);
        				folderMap.put("parent", folder.getInode());
        				folderMap.put("mimeType", "");
        				folderMap.put("name", folder.getName());
        				folderMap.put("description", folder.getTitle());
        				folderMap.put("extension", "folder");
        				returnList.add(folderMap);
        			}
        		}
        	}
        }
        if(!onlyFiles) {
        	if(f!=null){
        		List<HTMLPage> pages = new ArrayList<HTMLPage>();
        		try {
        			pages.addAll(folderAPI.getHTMLPages(f,true,false,userAPI.getSystemUser(),false));
        			if(showArchived)
        				pages.addAll(folderAPI.getHTMLPages(f,true,showArchived,userAPI.getSystemUser(),false));
        		} catch (Exception e1) {
        			Logger.error(this, "Could not load HTMLPages : ",e1);
        		} 
        		for (HTMLPage page : pages) {
        			List<Integer> permissions = new ArrayList<Integer>();
        			try {
        				permissions = permissionAPI.getPermissionIdsFromRoles(page, roles, usr);
        			} catch (DotDataException e) {
        				Logger.error(this, "Could not load permissions : ",e);
        			}
        			if(permissions.contains(PERMISSION_READ)){
        				Map<String, Object> pageMap = page.getMap();
        				pageMap.put("mimeType", "application/dotpage");
        				pageMap.put("permissions", permissions);
        				pageMap.put("name", page.getPageUrl());
        				pageMap.put("description", page.getFriendlyName());
        				pageMap.put("extension", "page");
        				try {
        					pageMap.put("pageURI", page.getURI());
        				} catch (Exception e) {
        					Logger.error(this, "Could not get URI : ",e);
        				} 
        				returnList.add(pageMap);
        			}

        		}
        	}
        }
        List<Versionable> files = new ArrayList<Versionable>();
        PermissionAPI  perAPI = APILocator.getPermissionAPI();
		try {
			if(f==null){
				files.addAll(APILocator.getFileAssetAPI().findFileAssetsByHost(APILocator.getHostAPI().find(folderId, userAPI.getSystemUser(),false), userAPI.getSystemUser(),false));
			}else{
				files.addAll(folderAPI.getFiles(f,userAPI.getSystemUser(),false, cond));
				if(showArchived){
					cond.deleted=showArchived;
					files.addAll(folderAPI.getFiles(f,userAPI.getSystemUser(),false, cond));
				}
				files.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(f, userAPI.getSystemUser(),false));
			}

		} catch (Exception e2) {
			Logger.error(this, "Could not load files : ",e2);
		} 
		ContentletAPI conAPI = APILocator.getContentletAPI();
    	Contentlet contentlet = new Contentlet();    	
    	WorkflowStep wfStep = null;
    	WorkflowScheme wfScheme = null;
        for (Versionable file : files) {
        	if(file ==null) continue;
        	List<Integer> permissions = new ArrayList<Integer>();
        	try {
        		
        		permissions = permissionAPI.getPermissionIdsFromRoles((Permissionable)file, roles, usr);
        	} catch (DotDataException e) {
        		Logger.error(this, "Could not load permissions : ",e);
        	}
        	
    		List<WorkflowAction> wfActions = new ArrayList<WorkflowAction>();
    		
    		contentlet=null;
    		if(file instanceof com.dotmarketing.portlets.fileassets.business.FileAsset)
    		    contentlet = conAPI.find(file.getInode(),usr,false);
    		try{
    			if(contentlet != null){
    				wfStep = APILocator.getWorkflowAPI().findStepByContentlet(contentlet); 
    				wfScheme = APILocator.getWorkflowAPI().findScheme(wfStep.getSchemeId());
    				wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, usr); 
    			}
    		}
    		catch(Exception e){
    			Logger.error(this, "Could not load workflow actions : ",e);
    			//wfActions = new ArrayList();
    		}
    		boolean contentEditable = false;
    		if(contentlet != null){
    			if(perAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, usr) && contentlet.isLocked()){    			
    				String lockedUserId = APILocator.getVersionableAPI().getLockedBy(contentlet);
    				if(usr.getUserId().equals(lockedUserId)){
    					contentEditable = true;
    				}else{
    					contentEditable =  false;
    				}
    			}else{
    				contentEditable =  false;
    			}
        	}

        	try{
        	if(permissions.contains(PERMISSION_READ)){
        		if(!showArchived && file.isArchived()){
        			continue;
        		}
        		IFileAsset fileAsset = (IFileAsset)file;
        		
        		List<Map<String, Object>> wfActionMapList = new ArrayList<Map<String, Object>> ();
        		for(WorkflowAction action : wfActions){  
        			
        			if(action.requiresCheckout()){
        				continue;
        			}
        			Map<String, Object> wfActionMap = new HashMap<String, Object>();
        			wfActionMap.put("name", action.getName());
        			wfActionMap.put("id", action.getId());
        			wfActionMap.put("icon", action.getIcon());
        			wfActionMap.put("assignable", action.isAssignable());
        			wfActionMap.put("commentable", action.isCommentable() || UtilMethods.isSet(action.getCondition()));
        			wfActionMap.put("requiresCheckout", action.requiresCheckout());
        			wfActionMap.put("wfActionNameStr", LanguageUtil.get(usr,action.getName()));
        			wfActionMapList.add(wfActionMap);
        		}
        		if(wfScheme != null && wfScheme.isMandatory()){
        			permissions.remove(new Integer(PermissionAPI.PERMISSION_PUBLISH));
        		}
        		
        		
        		
        		
        		Map<String, Object> fileMap = fileAsset.getMap();
        		fileMap.put("permissions", permissions);        		
        		fileMap.put("mimeType", APILocator.getFileAPI().getMimeType(fileAsset.getFileName()));
        		fileMap.put("name", fileAsset.getFileName());
        		fileMap.put("description", fileAsset.getFriendlyName());
        		fileMap.put("extension", FileUtil.getIconExtension(fileAsset.getFileName()));
        		fileMap.put("path", fileAsset.getPath());
        		fileMap.put("type", fileAsset.getType());
        		fileMap.put("wfActionMapList", wfActionMapList);        		
        		fileMap.put("contentEditable", contentEditable);
        		if(contentlet != null){
        			fileMap.put("identifier", contentlet.getIdentifier());
        			fileMap.put("inode", contentlet.getInode());
        			fileMap.put("languageId", contentlet.getLanguageId());
        			fileMap.put("isLocked", contentlet.isLocked());
        		}
        		returnList.add(fileMap);
        	}
        	}
        	catch(Exception e){
        		Logger.error(this, "Could not load fileAsset : ",e);
        	}

        }

        if(!onlyFiles && f!=null) {
        	List<Link> links = new ArrayList<Link>();
			try {
				links = folderAPI.getLinks(f, true, false, userAPI.getSystemUser(), false);
				if(showArchived)
					links.addAll(folderAPI.getLinks(f, true, showArchived, userAPI.getSystemUser(), false));
			} catch (Exception e1) {
				Logger.error(this, "Could not load links : ",e1);
			} 
        	for (Link link : links) {
        		java.util.List<Integer> permissions = new ArrayList<Integer>();
        		try {
        			permissions = permissionAPI.getPermissionIdsFromRoles(link, roles, usr);
        		} catch (DotDataException e) {
        			Logger.error(this, "Could not load permissions : ",e);
        		}
        		if(permissions.contains(PERMISSION_READ)){
        			Map<String, Object> linkMap = link.getMap(); 				
        			linkMap.put("permissions", permissions);
        			linkMap.put("mimeType", "application/dotlink");
        			linkMap.put("name", link.getTitle());
        			linkMap.put("description", link.getFriendlyName());
        			linkMap.put("extension", "link");
        			returnList.add(linkMap);

        		}

        	}
        }

        //Filtering
        List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>> ();
        for(Map<String, Object> asset : returnList) {

        	String name = (String)asset.get("name");
        	name = name == null?"":name;
        	String description = (String)asset.get("description");
        	description = description == null?"":description;
        	String mimeType = (String)asset.get("mimeType");
        	mimeType = mimeType == null?"":mimeType;
        	
        	if(UtilMethods.isSet(filter) &&
        		!(name.toLowerCase().contains(filter.toLowerCase()) || 
        			description.toLowerCase().contains(filter.toLowerCase()))) 
        		continue;
        	if(mimeTypes != null && mimeTypes.size() > 0) {
        		boolean match = false;
        		for(String mType : mimeTypes)
        			if(mimeType.contains(mType))
        				match = true;
        		if(!match) continue;
        	}
        	if(extensions != null && extensions.size() > 0) {
        		boolean match = false;
        		for(String ext : extensions)
        			if(((String)asset.get("extension")).contains(ext))
        				match = true;
        		if(!match) continue;
        	}
        	filteredList.add(asset);
        }
        returnList = filteredList;
        
        //Sorting        
    	WebAssetMapComparator comparator = new WebAssetMapComparator (sortBy, sortByDesc);
    	Collections.sort(returnList, comparator);
    	
        //Offsetting
    	if(offset < 0) offset = 0;
    	if(maxResults <= 0) maxResults = returnList.size() - offset;
    	if(maxResults + offset > returnList.size()) maxResults = returnList.size() - offset;
    	
    	Map<String, Object> returnMap = new HashMap<String, Object>();
    	returnMap.put("total", returnList.size());
    	returnMap.put("list", returnList.subList(offset, offset + maxResults));
        return returnMap;
	}
	public void saveFileAction(String selectedItem,String wfActionAssign,String wfActionId,String wfActionComments, String wfConId) throws  DotSecurityException, ServletException{
		WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());		
		Contentlet c = null;
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		try {
			WorkflowAction action = wapi.findAction(wfActionId, usr);
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
			c = APILocator.getContentletAPI().find(wfConId, usr, false);		
			c.setStringProperty("wfActionId", action.getId());
			c.setStringProperty("wfActionComments", wfActionComments);
			c.setStringProperty("wfActionAssign", wfActionAssign);	
			
			wapi.fireWorkflowNoCheckin(c);
			
		} catch (Exception e) {
			Logger.error(BrowserAjax.class, e.getMessage(), e);			
			throw new ServletException(e.getMessage());
		}
	}
	
	public Map<String, Object> getFileInfo(String fileId) throws DotDataException, DotSecurityException, PortalException, SystemException {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest req = ctx.getHttpServletRequest();
        ServletContext servletContext = ctx.getServletContext();
        User user = userAPI.getLoggedInUser(req);
        boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);
		
        Identifier ident = APILocator.getIdentifierAPI().find(fileId);
        
		if(ident!=null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals("file_asset")) {
			File file = fileAPI.getWorkingFileById(fileId, user, respectFrontendRoles);
			String mimeType = servletContext.getMimeType(file.getFileName().toLowerCase());
			Map<String, Object> fileMap = file.getMap();
			fileMap.put("mimeType", mimeType);
			fileMap.put("path", file.getPath());
			return fileMap;
		}
		
		if(ident!=null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals("htmlpage")) {
			HTMLPage page = pageAPI.loadWorkingPageById(fileId, user, respectFrontendRoles);
			Map<String, Object> pageMap = page.getMap();
			pageMap.put("mimeType", "application/dotpage");
			pageMap.put("pageURI", page.getURI());
			return pageMap;
		}
		
		if(ident!=null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals("contentlet")) {
			
			Contentlet cont = contAPI.findContentletByIdentifier(ident.getId(),true, APILocator.getLanguageAPI().getDefaultLanguage().getId() , user, respectFrontendRoles);
			FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(cont);
			java.io.File file = fileAsset.getFileAsset();
			String mimeType = servletContext.getMimeType(file.getName().toLowerCase());
			Map<String, Object> fileMap = fileAsset.getMap();
			fileMap.put("mimeType", mimeType);
			fileMap.put("path", fileAsset.getPath());
			fileMap.put("type", "contentlet");
			return fileMap;			
		}		

		return null;
	}
    
    @SuppressWarnings("unchecked")
	private List<Map> getFoldersTree (Host host, Role[] roles) throws DotStateException, DotDataException, DotSecurityException {
        FolderAPI folderAPI = APILocator.getFolderAPI();
        List<Folder> children = new ArrayList<Folder>();
		try {
			children = folderAPI.findSubFolders(host,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, "Could not load folders : ",e);
		}
        return getFoldersTree(host.getIdentifier(), children, roles);
    }
    
    @SuppressWarnings("unchecked")
	private List<Map> getFoldersTree (Folder parent, Role[] roles) throws DotStateException, DotDataException, DotSecurityException {
        FolderAPI folderAPI = APILocator.getFolderAPI();
        List<Folder> children = new ArrayList<Folder>();
		try {
			children = folderAPI.findSubFolders(parent,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, "Could not load folders : ",e);
		} 
        return getFoldersTree(parent.getInode(), children, roles);
    }

	private List<Map> getFoldersTree (String parentInode, List<Folder> children, Role[] roles) throws DotStateException, DotDataException, DotSecurityException {

        WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
        ArrayList<Map> folders = new ArrayList<Map> ();

        for (Folder f : children) {
        	Map<String, Object> folderMap = f.getMap();
        	if (openFolders.contains(f.getInode())) {
        		List<Map> childrenMaps = getFoldersTree (f, roles);
        		folderMap.put("open", true);
        		folderMap.put("childrenFolders", childrenMaps);
        	} else {
        		folderMap.put("open", false);
        	}
        	if(f.getInode().equalsIgnoreCase(activeFolderInode))
        		folderMap.put("selected", true);
        	else
        		folderMap.put("selected", false);
        	folderMap.put("parent", parentInode);

        	List permissions = new ArrayList();
        	try {
        		permissions = permissionAPI.getPermissionIdsFromRoles(f, roles, usr);
        	} catch (DotDataException e) {
        		Logger.error(this, "Could not load permissions : ",e);
        	}

        	folderMap.put("permissions", permissions);

        	folders.add(folderMap);

        }

        	return folders;
    }
    
    public Map<String, Object> renameFolder (String inode, String newName) throws DotDataException, DotSecurityException {
    	WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
    	HashMap<String, Object> result = new HashMap<String, Object> ();
    	Folder folder = APILocator.getFolderAPI().find(inode, usr, false);
    	result.put("lastName", folder.getName());
    	result.put("extension", "");
    	result.put("newName", newName);
    	result.put("inode", folder.getInode());
    	try {
			if (folderAPI.renameFolder(folder, newName,usr,false)) {
				result.put("result", 0);
			} else {
				result.put("result", 1);
				result.put("errorReason", "There is another folder that has the same name");
			}
		} catch (Exception e) {
			Logger.error(this, "Problem occured in the method  renameFolder: ",e);
		} 
    	return result;
    }
    
	// Copy action
	public boolean copyFolder(String inode, String newFolder) throws Exception {
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        

    	Folder folder = APILocator.getFolderAPI().find(inode, user, false);

		// Checking permissions

		if (!folderAPI.exists(newFolder)) {
			HostAPI hostAPI = APILocator.getHostAPI();
			
			Host parentHost = (Host) hostAPI.find(newFolder, user, !userWebAPI.isLoggedToBackend(req));
			
			if (!permissionAPI.doesUserHavePermission(folder, PERMISSION_WRITE, user) ||
					!permissionAPI.doesUserHavePermission(parentHost, PERMISSION_WRITE, user))
				throw new DotRuntimeException("The user doesn't have the required permissions.");
			folderAPI.copy(folder, parentHost, user, false);
		} else {
			Folder parentFolder = APILocator.getFolderAPI().find(newFolder, user, false);
			
			if (!permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_WRITE, user) ||
					!permissionAPI.doesUserHavePermission(parentFolder, PERMISSION_WRITE, user))
				throw new DotRuntimeException("The user doesn't have the required permissions.");

			if (parentFolder.getInode().equalsIgnoreCase(folder.getInode())) {
				//Trying to move a folder over itself
				return false;
			}
			if (folderAPI.isChildFolder(parentFolder, folder)) {
				//Trying to move a folder over one of its children
				return false;
			}
            folderAPI.copy(folder, parentFolder, user, false);
		}
		return true;
	}

	// Move action
	public boolean moveFolder(String inode, String newFolder) throws Exception {

		HibernateUtil.startTransaction();
		
		try {
			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
	        User user = getUser(req);
	        boolean respectFrontendRoles = !userAPI.isLoggedToBackend(req);
	
	    	Folder folder = APILocator.getFolderAPI().find(inode, user, false);
	
			if (!folderAPI.exists(newFolder)) {
				Host parentHost = hostAPI.find(newFolder, user, respectFrontendRoles); 
				
				if (!permissionAPI.doesUserHavePermission(folder, PERMISSION_WRITE, user) ||
						!permissionAPI.doesUserHavePermission(parentHost, PERMISSION_WRITE, user))
					throw new DotRuntimeException("The user doesn't have the required permissions.");
				
				if (!folderAPI.move(folder, parentHost,user,respectFrontendRoles)){
					//A folder with the same name already exists on the destination
					return false;
				}
			} else {
				Folder parentFolder = APILocator.getFolderAPI().find(newFolder, user, false);
	
				if (!permissionAPI.doesUserHavePermission(folder, PERMISSION_WRITE, user) ||
						!permissionAPI.doesUserHavePermission(parentFolder, PERMISSION_WRITE, user))
					throw new DotRuntimeException("The user doesn't have the required permissions.");
				
				if (parentFolder.getInode().equalsIgnoreCase(folder.getInode())) {
					//Trying to move a folder over itself
					return false;
				}
				if (folderAPI.isChildFolder(parentFolder, folder)) {
					//Trying to move a folder over one of its children
					return false;
				}
				if (!folderAPI.move(folder, parentFolder,user,respectFrontendRoles)) {
					//A folder with the same name already exists on the destination
					return false;
				}
			}
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
		} finally {
			HibernateUtil.commitTransaction();
		}
		return true;
	}
    
    public Map<String, Object> renameFile (String inode, String newName) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
    	User user = null;
    	try {
    		user = com.liferay.portal.util.PortalUtil.getUser(req);
    	} catch (Exception e) {
    		Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
    		throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
    	}

    	HashMap<String, Object> result = new HashMap<String, Object> ();
    	Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		result.put("lastName", cont.get(FileAssetAPI.FILE_NAME_FIELD));
    		result.put("extension", UtilMethods.getFileExtension(cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)));
    		result.put("newName", newName);
    		result.put("inode", inode);
    		if(!cont.isLocked()){
    			if(APILocator.getFileAssetAPI().renameFile(cont, newName, user, false)){
    				result.put("result", 0);
    			}else{
    				result.put("result", 1);
    				result.put("errorReason", "Another file with the same name already exists on this folder");
    			}
    		}else{
    			result.put("result", 1);
    			result.put("errorReason", "The file is locked");
    		}
    	}else{
    		File file = (File) InodeFactory.getInode(inode, File.class);
    		result.put("lastName", file.getNameOnly());
    		result.put("extension", file.getExtension());
    		result.put("newName", newName);
    		result.put("inode", inode);
    		if (APILocator.getFileAPI().renameFile(file, newName, user, false)) {
    			result.put("result", 0);
    		} else {
    			result.put("result", 1);
    			if (file.isLocked())
    				result.put("errorReason", "The file is locked");
    			else
    				result.put("errorReason", "Another file with the same name already exists on this folder");
    		}
    	}

    	return result;
    }
    
    public String copyFile (String inode, String newFolder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        // gets folder parent
    	Folder parent = APILocator.getFolderAPI().find(newFolder, user, false);
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(id, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			return "File-failed-to-copy-check-you-have-the-required-permissions";

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);
    		if (UtilMethods.isSet(fa.getFileName()) && !folderAPI.matchFilter(parent, fa.getFileName())) {			
    			return "message.file_asset.error.filename.filters";
    		}
    		APILocator.getContentletAPI().copyContentlet(cont, parent, user, false);
    		return "File-copied";
    	}

    	File file = (File) InodeFactory.getInode(inode, File.class);
		// CHECK THE FOLDER PATTERN		//DOTCMS-6017
		if (UtilMethods.isSet(file.getFileName()) && !folderAPI.matchFilter(parent, file.getFileName())) {			
			return "message.file_asset.error.filename.filters";
		}

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(file, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			return "File-failed-to-copy-check-you-have-the-required-permissions";

		APILocator.getFileAPI().copyFile(file, parent, user, false);	
		return "File-copied";
		
    }

    public boolean moveFile (String inode, String folder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
        // gets folder parent
        Folder parent =APILocator.getFolderAPI().find(folder, user, false);
        if (!permissionAPI.doesUserHavePermission(id, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");
    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		return APILocator.getFileAssetAPI().moveFile(cont, parent, user, false);
    	}
    	File file = (File) InodeFactory.getInode(inode, File.class);

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(file, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");

		boolean ret = APILocator.getFileAPI().moveFile(file, parent, user, false);

		return ret;
		
    }

    
    public Map<String, Object> renameHTMLPage (String inode, String newName) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
    	
    	HashMap<String, Object> result = new HashMap<String, Object> ();
    	HTMLPage page = (HTMLPage) InodeFactory.getInode(inode, HTMLPage.class);
    	String pageURL = page.getPageUrl();
    	result.put("lastName", pageURL.substring(0, pageURL.lastIndexOf(".")));
    	result.put("extension", Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
    	result.put("newName", newName);
    	result.put("inode", inode);
    	if (HTMLPageFactory.renameHTMLPage(page, newName, user)) {
        	result.put("result", 0);
    	} else {
        	result.put("result", 1);
        	if (page.isLocked())
        		result.put("errorReason", "The page is locked");
        	else
        		result.put("errorReason", "Another page with the same name already exists on this folder");
    	}
    	return result;
    }
    
    public boolean copyHTMLPage (String inode, String newFolder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);

    	HTMLPage page = (HTMLPage) InodeFactory.getInode(inode, HTMLPage.class);

        // gets folder parent
		Folder parent = APILocator.getFolderAPI().find(newFolder, user, false);

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(page, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");

		HTMLPageFactory.copyHTMLPage(page, parent);
		
		return true;
		
    }

    public boolean moveHTMLPage (String inode, String folder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);

    	HTMLPage page = (HTMLPage) InodeFactory.getInode(inode, HTMLPage.class);

        // gets folder parent
		Folder parent = APILocator.getFolderAPI().find(folder, user, false);
		
		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(page, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");

		return HTMLPageFactory.moveHTMLPage(page, parent);
		
    }

    public Map<String, Object> renameLink (String inode, String newName) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(req);
        } catch (Exception e) {
            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
        }
    	
    	HashMap<String, Object> result = new HashMap<String, Object> ();
    	Link link = (Link) InodeFactory.getInode(inode, Link.class);
    	String oldName = link.getTitle();
    	result.put("lastName", oldName);
    	result.put("extension", "");
    	result.put("newName", newName);
    	result.put("inode", inode);
    	if (LinkFactory.renameLink(link, newName, user)) {
        	result.put("result", 0);
    	} else {
        	result.put("result", 1);
        	if (link.isLocked())
        		result.put("errorReason", "The link is locked");
        	else
        		result.put("errorReason", "Another link with the same name already exists on this folder");
    	}
    	return result;
    }
    
    public boolean copyLink (String inode, String newFolder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);

    	Link link = (Link) InodeFactory.getInode(inode, Link.class);

        // gets folder parent
		Folder parent = APILocator.getFolderAPI().find(newFolder, user, false);

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(link, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");

		LinkFactory.copyLink(link, parent);
		
		return true;
		
    }

    public boolean moveLink (String inode, String folder) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);

    	Link link = (Link) InodeFactory.getInode(inode, Link.class);

        // gets folder parent
		Folder parent = APILocator.getFolderAPI().find(folder, user, false);
		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(link, PERMISSION_WRITE, user) ||
				!permissionAPI.doesUserHavePermission(parent, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");
		
		return LinkFactory.moveLink(link, parent);
		
    }

    public boolean publishAsset (String inode) throws WebAssetException, Exception {
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		APILocator.getContentletAPI().publish(cont, user, false);
    		return true;
    	}
        
		java.util.List relatedAssets = new java.util.ArrayList();
        Inode inodeObj = InodeFactory.getInode(inode,Inode.class);
                
        HTMLPage htmlPage = null;
        
        if( inodeObj instanceof HTMLPage ) {
        	htmlPage =(HTMLPage)inodeObj;
        }
		
        if (htmlPage != null && InodeUtils.isSet(htmlPage.getInode())) {
			relatedAssets = PublishFactory.getUnpublishedRelatedAssets(htmlPage,relatedAssets, user, false);
        }
        
        if ((relatedAssets == null) || (relatedAssets.size() == 0)) { 
	    	Inode asset = inodeObj;
	
	        if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_PUBLISH, user))
				throw new Exception("The user doesn't have the required permissions.");
	        
	        return PublishFactory.publishAsset(asset, req);
        } else {
        	throw new Exception("Related assets needs to be published");
        }
    }
    
    public boolean unPublishAsset (String inode) throws Exception {
    	HibernateUtil.startTransaction();
    	boolean ret = false;
    	try{
        	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        	User user = getUser(req);

        	Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
        	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
        		throw new DotRuntimeException("The user doesn't have the required permissions.");

        	if(id!=null && id.getAssetType().equals("contentlet")){
        		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
        		APILocator.getContentletAPI().unpublish(cont, user, false);
        		ret = true;
        	}else{
        		WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);
        		Folder parent = (Folder)folderAPI.findParentFolder(asset, user, false);
        		ret = WebAssetFactory.unPublishAsset(asset, user.getUserId(), parent);
        	}
        	HibernateUtil.commitTransaction();
    	}catch(Exception e){
    		ret = false;
    		HibernateUtil.rollbackTransaction();
    	}

    	return ret;
    }

    public boolean archiveAsset (String inode) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		APILocator.getContentletAPI().archive(cont, user, false);
    		return true;
    	}

        WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);

        if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");
        
        return WebAssetFactory.archiveAsset(asset, user.getUserId());
    }
    
    public boolean unArchiveAsset (String inode) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		APILocator.getContentletAPI().unarchive(cont, user, false);
    		return true;
    	}

        WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);

        
        if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");
        
        WebAssetFactory.unArchiveAsset(asset);
        
        return true;
    }

    
    public boolean unlockAsset (String inode) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		APILocator.getContentletAPI().unlock(cont, user, false);
    		return true;
    	}

        WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);

        if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_WRITE, user))
			throw new DotRuntimeException("The user doesn't have the required permissions.");
        
        WebAssetFactory.unLockAsset(asset);
        return true;
    }
    
    public boolean deleteAsset(String inode) throws Exception 
    {
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);
        
        
        Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    		APILocator.getContentletAPI().delete(cont, user, false);
    		return true;
    	}


        WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);

        //I verify the permissions in the methods but I could change that
        //if (!PermissionFactory.doesUserHavePermission(asset, PERMISSION_WRITE, user))
		//	throw new DotRuntimeException("The user doesn't have the required permissions.");
        
        WebAssetFactory.deleteAsset(asset, user);
        return true;
    }
    
    

    public Map<String, Object> changeAssetMenuOrder (String inode, int newValue) throws ActionException, DotDataException {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(req);
        } catch (Exception e) {
            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
        }
        
    	HashMap<String, Object> result = new HashMap<String, Object> ();
    	Inode asset = (Inode) InodeFactory.getInode(inode, Inode.class);
    	if (asset instanceof Folder) {
    		Folder folder = (Folder) asset;
    		result.put("lastValue", folder.getSortOrder());
    		WebAssetFactory.changeAssetMenuOrder (asset, newValue, user);
    	} else {
    		result.put("lastValue", ((WebAsset)asset).getSortOrder());
    		WebAssetFactory.changeAssetMenuOrder (asset, newValue, user);
    	}
       	result.put("result", 0);
    	return result;
    }

    
    private User getUser(HttpServletRequest req) {

        // get the user
        User user = null;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser(req);
        } catch (Exception e) {
            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
        }
        return user;

    }
    

	/**
	 * This method returns the basic info of the full tree of hosts and folders
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws DotRuntimeException 
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
    public List<Map<String,Object>> getTreeMinInfo () throws DotRuntimeException, PortalException, SystemException, DotDataException, DotSecurityException {
         return getTreeMinInfoByHostId("fullTree");
    }
    
	private List<Map<String,Object>> getFolderMinInfoTree (Folder parent, Role[] roles, String currentFullPath, String currentAbsolutePath) {
    	
        FolderAPI folderAPI = APILocator.getFolderAPI();
        
        List<Map<String, Object>> toReturn = new ArrayList<Map<String,Object>>();
        
        List<Folder> children = new ArrayList<Folder>();
		try {
			children = folderAPI.findSubFolders(parent,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(BrowserAjax.class,e.getMessage(),e);
		}	
		
        for (Folder f : children) {
            Map<String, Object> folderMap = new HashMap<String, Object>();
            folderMap.put("type", "folder");
            folderMap.put("name", f.getName());
            folderMap.put("id", f.getInode());
            String fullPath = currentFullPath + "/" + f.getName();
            String absolutePath = currentAbsolutePath + "/" + f.getName();
            folderMap.put("fullPath", fullPath);
            folderMap.put("absolutePath", absolutePath);
            List<Map<String, Object>> childrenMaps = getFolderMinInfoTree (f, roles, fullPath, absolutePath);
            folderMap.put("children", childrenMaps);
            toReturn.add(folderMap);
        }
        
        return toReturn;
    }
	//http://jira.dotmarketing.net/browse/DOTCMS-3232
	/**
	 * This method returns the basic info of the tree of host and folders for the given hostId
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws DotRuntimeException 
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 * 
	 */
	public List<Map<String,Object>> getTreeMinInfoByHostId (String hostId) throws DotRuntimeException, PortalException, SystemException, DotDataException, DotSecurityException {
        
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        FolderAPI folderAPI = APILocator.getFolderAPI();
        Host newHost = new Host();
        List<Host> hosts = new ArrayList<Host>();

        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}

		List<Map<String,Object>> toReturn = new ArrayList<Map<String,Object>>();
		if(UtilMethods.isSet(hostId)&& hostId.equalsIgnoreCase("fullTree")) {
			   hosts = hostAPI.findAll(user, false);
		} else if(InodeUtils.isSet(hostId)) {
		   newHost = hostAPI.find(hostId, user, false);
		   hosts.add(newHost);
		} else {
		   newHost = hostWebAPI.getCurrentHost(ctx.getHttpServletRequest());
		   hosts.add(newHost);
		}
		Collections.sort(hosts, new HostNameComparator());  // DOTCMS JIRA - 4354
		
		for (Host host : hosts) {
			if(host.isSystemHost())
				continue;
			if(host.isArchived()==false){
				String currentPath = host.getHostname();
				Map<String,Object> hostMap = new HashMap<String, Object>();
				hostMap.put("type", "host");
				hostMap.put("name", host.getHostname());
				hostMap.put("id", host.getIdentifier());
				hostMap.put("fullPath", currentPath);
				hostMap.put("absolutePath", currentPath);
				List<Map<String, Object>> children = new ArrayList<Map<String,Object>>();

				List<Folder> subFolders = folderAPI.findSubFolders(host,user,false);
				for (Folder f : subFolders) {

						List permissions = new ArrayList();
						try {
							permissions = permissionAPI.getPermissionIdsFromRoles(f, roles, user);
						} catch (DotDataException e) {
							Logger.error(this, "Could not load permissions : ",e);
						}
						if(permissions.contains(PERMISSION_READ)){
							Map<String, Object> folderMap = new HashMap<String, Object>();
							folderMap.put("type", "folder");
							folderMap.put("name", f.getName());
							folderMap.put("id", f.getInode());
							String fullPath = currentPath + ":/" + f.getName();
							String absolutePath = "/" + f.getName();
							folderMap.put("fullPath", fullPath);
							folderMap.put("absolutePath", absolutePath);
							List<Map<String, Object>> childrenMaps = getFolderMinInfoTree(f, roles, fullPath, absolutePath);
							folderMap.put("children", childrenMaps);
							children.add(folderMap);
						}
					
				}
				hostMap.put("children", children);
				toReturn.add(hostMap);
			}
		}
		return toReturn;
	}
	
	private Map<String, Object> hostMap(Host host) {
    	String currentPath = host.getHostname();
        Map<String,Object> hostMap = new HashMap<String, Object>();
        hostMap.put("type", "host");
        hostMap.put("hostName", host.getHostname());
        hostMap.put("name", host.getHostname());
        hostMap.put("id", host.getIdentifier());
        hostMap.put("identifier", host.getIdentifier());
        hostMap.put("fullPath", currentPath);
        hostMap.put("absolutePath", currentPath);
        return hostMap;
	}
	
	private Map<String, Object> folderMap(Folder f) throws DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		Map<String, Object> folderMap = new HashMap<String, Object>();
		folderMap.put("type", "folder");
		folderMap.put("name", f.getName());
		folderMap.put("id", f.getInode());
		folderMap.put("inode", f.getInode());
		folderMap.put("defaultFileType", f.getDefaultFileType());
		String currentPath = hostAPI.findParentHost(f, userWebAPI.getSystemUser(), false).getHostname();
		String fullPath = currentPath + ":/" + f.getName();
		String absolutePath = "/" + f.getName();
		folderMap.put("fullPath", fullPath);
		folderMap.put("absolutePath", absolutePath);
        return folderMap;
	}
	
	public List<Map<String, Object>> getHosts() throws PortalException, SystemException, DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts = hostAPI.findAll(user, respectFrontendRoles);
		List<Map<String, Object>> hostsToReturn = new ArrayList<Map<String,Object>>(hosts.size());
		Collections.sort(hosts, new HostNameComparator());  
		for (Host h: hosts) {
			List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(h, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ",e);
			}
			if(permissions.contains(PERMISSION_READ)){
			    hostsToReturn.add(hostMap(h));
			}
		}
		return hostsToReturn;
	}
	
	
	public List<Map<String, Object>> getHostsByPermissions(String requiredPermissions) throws PortalException, SystemException, DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts = hostAPI.findAll(user, respectFrontendRoles);
		List<Map<String, Object>> hostsToReturn = new ArrayList<Map<String,Object>>(hosts.size());
		Collections.sort(hosts, new HostNameComparator());  
		for (Host h: hosts) {
			if(UtilMethods.isSet(requiredPermissions)){	
				if(permissionAPI.doesUserHavePermissions(h,requiredPermissions, user)){
					hostsToReturn.add(hostMap(h));
				}	
			}
		}
		return hostsToReturn;
	}
	
	public List<Map<String, Object>> getHostsIncludeAll() throws PortalException, SystemException, DotDataException, DotSecurityException {
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
		boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts = hostAPI.findAll(user, respectFrontendRoles);
		List<Map<String, Object>> hostsToReturn = new ArrayList<Map<String,Object>>(hosts.size());
		Collections.sort(hosts, new HostNameComparator());  
		for (Host h: hosts) {
			List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(h, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ",e);
			}
			if(permissions.contains(PERMISSION_READ)){
				hostsToReturn.add(hostMap(h));
			}
		}
		Host system = hostAPI.findSystemHost();
		List permissions = new ArrayList();
		try {
			permissions = permissionAPI.getPermissionIdsFromRoles(system, roles, user);
		} catch (DotDataException e) {
			Logger.error(this, "Could not load permissions : ",e);
		}
		if(permissions.contains(PERMISSION_READ)){
			Host allHosts = new Host();
			allHosts.setHostname("All Hosts");
			allHosts.setIdentifier("allHosts");
			hostsToReturn.add(hostMap(allHosts));
		}

		return hostsToReturn;
	}

    
	public List<Map<String, Object>> getHostSubfolders(String hostId) throws PortalException, SystemException, DotDataException, DotSecurityException {
		if(hostId.equals("allHosts")){
			return  new ArrayList<Map<String,Object>>();
		}
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		FolderAPI folderAPI = APILocator.getFolderAPI();
		List<Folder> folders = folderAPI.findSubFolders(host,user,false);
		List<Map<String, Object>> foldersToReturn = new ArrayList<Map<String,Object>>(folders.size());
		for (Folder f: folders){
			List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(f, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ",e);
			}
			if(permissions.contains(PERMISSION_READ)){
			     foldersToReturn.add(folderMap(f));
			}
		}
		return foldersToReturn;
	}	
	
	public List<Map<String, Object>> getHostSubfoldersByPermissions(String hostId, String requiredPermissions) throws PortalException, SystemException, DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		Host host = hostAPI.find(hostId, user, respectFrontendRoles);
		FolderAPI folderAPI = APILocator.getFolderAPI();
		List<Folder> folders = folderAPI.findSubFolders(host,user,false);
		List<Map<String, Object>> foldersToReturn = new ArrayList<Map<String,Object>>(folders.size());
		for (Folder f: folders){
			if(UtilMethods.isSet(requiredPermissions)){	
				if(permissionAPI.doesUserHavePermissions(f,requiredPermissions, user)){
					foldersToReturn.add(folderMap(f));			
				}
			}
		}
		return foldersToReturn;
	}
    
	public List<Map<String, Object>> getFolderSubfolders(String parentFolderId) throws PortalException, SystemException, DotDataException, DotSecurityException {
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
		FolderAPI folderAPI = APILocator.getFolderAPI();
		Folder parentFolder = folderAPI.find(parentFolderId,user,false);
		List<Folder> folders = folderAPI.findSubFolders(parentFolder,user,false);
		List<Map<String, Object>> foldersToReturn = new ArrayList<Map<String,Object>>(folders.size());
		for (Folder f: folders) {
			List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(f, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ",e);
			}
			if(permissions.contains(PERMISSION_READ)){
			   foldersToReturn.add(folderMap(f));
			}
		}
		return foldersToReturn;
	}	
	
	
	public List<Map<String, Object>> getFolderSubfoldersByPermissions(String parentFolderId, String requiredPermissions) throws PortalException, SystemException, DotDataException, DotSecurityException {
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		FolderAPI folderAPI = APILocator.getFolderAPI();
		Folder parentFolder = folderAPI.find(parentFolderId,user,false);
		List<Folder> folders = folderAPI.findSubFolders(parentFolder,user,false);
		List<Map<String, Object>> foldersToReturn = new ArrayList<Map<String,Object>>(folders.size());
		for (Folder f: folders) {
			if(UtilMethods.isSet(requiredPermissions)){	
				if(permissionAPI.doesUserHavePermissions(f,requiredPermissions, user)){
					foldersToReturn.add(folderMap(f));			
				}
			}
		}
		return foldersToReturn;
	}	
		
	public Map<String, Object> findHostFolder(String hostFolderId) throws PortalException, SystemException, DotDataException, DotSecurityException {
		try {
			if (InodeUtils.isSet(hostFolderId)) {
				UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
				WebContext ctx = WebContextFactory.get();
				User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
				boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
				HostAPI hostAPI = APILocator.getHostAPI();
				Host host = hostAPI.find(hostFolderId, user, respectFrontendRoles);
				if(host != null) {
					return hostMap(host);
				}
				
				host = hostAPI.findByName(hostFolderId, user, respectFrontendRoles);
				if(host != null) {
					return hostMap(host);
				}
				
				FolderAPI folderAPI = APILocator.getFolderAPI();
				Folder folder = folderAPI.find(hostFolderId,user,false);
				if(folder != null) {
					return folderMap(folder);
				}
			}
		} catch (Exception e) {
		}
		
		return null;
	}
	
	public Map<String, Object> getFolderMap(String folderId) throws PortalException, SystemException, DotSecurityException, DotDataException{
		if (InodeUtils.isSet(folderId)) {
			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			WebContext ctx = WebContextFactory.get();
			User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
			boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
			FolderAPI folderAPI = APILocator.getFolderAPI();
			Folder folder = null;
			try{
				folder = folderAPI.find(folderId,user,respectFrontendRoles);
			}catch(Exception e){
				Logger.warn(this, "Unable to find folder with the given id, looking for host");
			}
			if(folder != null) {
				return folderMap(folder);
			}else{
				Host host = APILocator.getHostAPI().find(folderId, user, respectFrontendRoles);
				if(host!=null && InodeUtils.isSet(host.getIdentifier())){
					Map<String, Object> folderMap = new HashMap<String, Object>();
					folderMap.put("type", "folder");
					folderMap.put("name", FolderAPI.SYSTEM_FOLDER);
					folderMap.put("id", FolderAPI.SYSTEM_FOLDER);
					folderMap.put("inode", FolderAPI.SYSTEM_FOLDER);
					Structure defaultFileAssetStructure = StructureCache.getStructureByName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
					folderMap.put("defaultFileType", defaultFileAssetStructure.getInode());
					folderMap.put("fullPath", host.getHostname() + ":/");
					folderMap.put("absolutePath", "/");
					return folderMap;
				}

			}
			
		}
		return null;
	}

}
