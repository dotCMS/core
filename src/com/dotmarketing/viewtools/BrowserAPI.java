package com.dotmarketing.viewtools;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.ChildrenCondition;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class BrowserAPI {

	private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

	public Map<String, Object> getFolderContent (User usr, String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc) throws DotHibernateException, DotSecurityException, DotDataException {

		if(!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate";
			sortByDesc = true;
		}

		ChildrenCondition cond = new ChildrenCondition();
		cond.working=true;
		cond.deleted=false;

        List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>> ();

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
    	Contentlet contentlet = null;
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
    		    contentlet = (Contentlet)file;
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
        		fileMap.put("size", fileAsset.getFileSize());
        		fileMap.put("publishDate", fileAsset.getIDate());
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


}
