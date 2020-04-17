package com.dotcms.rendering.velocity.viewtools;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

public class BrowserAPI {

    private LanguageAPI langAPI = APILocator.getLanguageAPI();
    private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
    private FolderAPI folderAPI = APILocator.getFolderAPI();
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    public Map<String, Object> getFolderContent ( User usr, String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
    		List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks, long languageId ) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, excludeLinks, languageId);
    }

    public Map<String, Object> getFolderContent ( User usr, String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
                                                  List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, long languageId ) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, languageId);
    }
    
    public Map<String, Object> getFolderContent(User user, String folderId,
			int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showWorking, boolean showArchived, boolean noFolders,
			boolean onlyFiles, String sortBy, boolean sortByDesc)
			throws DotSecurityException, DotDataException {

    	return getFolderContent(user, folderId, offset, maxResults, filter, mimeTypes, extensions, showWorking, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, false, 0);

    }

    public Map<String, Object> getFolderContent(User user, String folderId,
			int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showWorking, boolean showArchived, boolean noFolders,
			boolean onlyFiles, String sortBy, boolean sortByDesc, long languageId)
			throws DotSecurityException, DotDataException {

    	return getFolderContent(user, folderId, offset, maxResults, filter, mimeTypes, extensions, showWorking, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, false, languageId);

    }
    
    protected static class WfData {
        
        List<WorkflowAction> wfActions = new ArrayList<WorkflowAction>();
        boolean contentEditable = false;
        List<Map<String, Object>> wfActionMapList = new ArrayList<Map<String, Object>>();
        
        boolean skip=false;
        
        public WfData(Contentlet contentlet, List<Integer> permissions, User user, boolean showArchived) throws DotStateException, DotDataException, DotSecurityException {
            
            
            if(null==contentlet) {
                return;
            }

            wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user, WorkflowAPI.RenderMode.LISTING);
            
            if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user) && contentlet.isLocked()) {
                String lockedUserId = APILocator.getVersionableAPI()
                        .getLockedBy(contentlet);
                if (user.getUserId().equals(lockedUserId)) {
                    contentEditable = true;
                } else {
                    contentEditable = false;
                }
            } else {
                contentEditable = false;
            }
            



            if (permissions.contains(PERMISSION_READ)) {
                if (!showArchived && contentlet.isArchived()) {
                    skip=true;
                    return;
                }
                final boolean showScheme = (wfActions!=null) ?  wfActions.stream().collect(Collectors.groupingBy(WorkflowAction::getSchemeId)).size()>1 : false;
                
                for (final WorkflowAction action : wfActions) {

					WorkflowScheme wfScheme = APILocator.getWorkflowAPI().findScheme(action.getSchemeId());
                    Map<String, Object> wfActionMap = new HashMap<String, Object>();
                    wfActionMap.put("name", action.getName());
                    wfActionMap.put("id", action.getId());
                    wfActionMap.put("icon", action.getIcon());
                    wfActionMap.put("assignable", action.isAssignable());
                    wfActionMap.put("commentable", action.isCommentable()
                            || UtilMethods.isSet(action.getCondition()));
                    wfActionMap.put("requiresCheckout",
                            action.requiresCheckout());
                    final String actionName = Try.of(() ->LanguageUtil.get(user, action.getName())).getOrElse(action.getName());
                    final String schemeName = Try.of(() ->LanguageUtil.get(user,wfScheme.getName())).getOrElse(wfScheme.getName());
                    
                    final String actionNameStr = (showScheme) ? actionName +" ( "+schemeName+" )" : actionName;

                    wfActionMap.put("wfActionNameStr",actionNameStr);
                    wfActionMap.put("hasPushPublishActionlet", action.hasPushPublishActionlet());
                    wfActionMapList.add(wfActionMap);
                }

            }


        }
    }
    
    public Map<String, Object> getFolderContent(User user, String folderId,
                    int offset, int maxResults, String filter, List<String> mimeTypes,
                    List<String> extensions, boolean showWorking, boolean showArchived, boolean noFolders,
                    boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks, long languageId)
                    throws DotSecurityException, DotDataException {
        
        
        return getFolderContent( user,  folderId,
                         offset,  maxResults,  filter,  mimeTypes,
                        extensions,  showWorking,  showArchived,  noFolders,
                         onlyFiles,  sortBy,  sortByDesc,  excludeLinks,  languageId, false);
        
    }
    
	/**
	 * Gets the Folders, HTMLPages, Links, FileAssets under the specified folderId.
	 *
	 * @param user
	 * @param folderId
	 * @param offset
	 * @param maxResults
	 * @param filter
	 * @param mimeTypes
	 * @param extensions
	 *
	 * @param showWorking   If true, returns the working version of HTMLPages, Links and FileAssets in the folder.
	 * 						If false, returns the live version of HTMLPages, Links and FileAssets in the folder.
	 *
	 * @param showArchived  If true, includes the archived version of HTMLPages, Links and FileAssets in the folder.
	 *
	 * @param noFolders
	 * @param onlyFiles
	 * @param sortBy
	 * @param sortByDesc
	 * @param excludeLinks
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Map<String, Object> getFolderContent(User user, String folderId,
			int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showWorking, boolean showArchived, boolean noFolders,
			boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks, long languageId, boolean dotAssets)
			throws DotSecurityException, DotDataException {

		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate";
			sortByDesc = true;
		}

		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();

		Role[] roles = com.dotmarketing.business.APILocator.getRoleAPI()
					.loadRolesForUser(user.getUserId()).toArray(new Role[0]);


		// gets folder parent
		Folder parent = APILocator.getFolderAPI().find(folderId, user, false);


		Host host = null;
		if (parent == null) {// If we didn't find a parent folder lets verify if
								// this is a host
			host = APILocator.getHostAPI().find(folderId, user, false);

            if ( host == null ){
                Logger.error( this, "Folder ID doesn't belong to a Folder nor a Host, id: " + folderId +
                                                    ", maybe the Folder was modified in the background." );
                throw new NotFoundInDbException( "Folder ID doesn't belong to a Folder nor a Host, id: " + folderId );
            }
		}

		if (!noFolders) {
			if (parent != null) {

				List<Folder> folders = new ArrayList<Folder>();
				try {
					folders = folderAPI.findSubFolders(parent,
							userAPI.getSystemUser(), false);
				} catch (Exception e1) {
					Logger.error(this, "Could not load folders : ", e1);
				}
				for (Folder folder : folders) {
					List<Integer> permissions = permissions = permissionAPI.getPermissionIdsFromRoles(
								folder, roles, user);
					if (permissions.contains(PERMISSION_READ)) {
						Map<String, Object> folderMap = folder.getMap();
						folderMap.put("permissions", permissions);
						folderMap.put("parent", folder.getInode());
						folderMap.put("mimeType", "");
						folderMap.put("name", folder.getName());
						folderMap.put("description", folder.getTitle());
						folderMap.put("extension", "folder");
						folderMap.put("hasTitleImage","");
						folderMap.put("__icon__","folderIcon");
						returnList.add(folderMap);
					}
				}
			}
		}

		if (!onlyFiles) {

	        // Getting the links directly under the parent folder or host
	        List<Link> links = new ArrayList<Link>();

            if (parent != null) {
                if (showWorking) {
                    links.addAll(folderAPI.getLinks(parent, true, false, user, false));
                } else {
                    links.addAll(folderAPI.getLiveLinks(parent, user, false));
                }
                if (showArchived)
                    links.addAll(folderAPI.getLinks(parent, true, showArchived, user, false));
            } else {
                links = folderAPI.getLinks(host, true, showArchived, user, false);
            }


	        for (Link link : links) {

	            List<Integer> permissions2 = permissionAPI.getPermissionIdsFromRoles(link, roles, user);

	            if (permissions2.contains(PERMISSION_READ)) {
	                Map<String, Object> linkMap = link.getMap();
	                linkMap.put("permissions", permissions2);
	                linkMap.put("mimeType", "application/dotlink");
	                linkMap.put("name", link.getTitle());
	                linkMap.put("description", link.getFriendlyName());
	                linkMap.put("extension", "link");
	                linkMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(link));
	                linkMap.put("statusIcons", UtilHTML.getStatusIcons(link));
	                linkMap.put("hasTitleImage", "");
	                linkMap.put("__icon__", "linkIcon");
	                returnList.add(linkMap);
	            }
	        }

            // Getting the html pages directly under the parent folder or host
            List<IHTMLPage> pages = new ArrayList<IHTMLPage>();
            
            if (parent != null) {//For folders
                if(!showWorking) {
                    pages.addAll(APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(parent, user, false));
                }
                else {
                    pages.addAll(APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(parent, user, false));
                }

                if(showArchived) {
                    pages.addAll(APILocator.getHTMLPageAssetAPI().getDeletedHTMLPages(parent, user, false));
                }
            } else {//For hosts
                if ( !showWorking ) {
                    pages.addAll( APILocator.getHTMLPageAssetAPI().getLiveHTMLPages( host, user, false ) );
                } else {
                    pages.addAll( APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages( host, user, false ) );
                }

                if ( showArchived ) {
                    pages.addAll( APILocator.getHTMLPageAssetAPI().getDeletedHTMLPages( host, user, false ) );
                }
            }

            for (IHTMLPage page : pages) {
                // include only pages for the given language
                if(languageId > 0 && ((Contentlet)page).getLanguageId()!= languageId) {
                    continue;
                }
                returnList.add(htmlPageMap((HTMLPageAsset) page, user, showArchived, languageId));
            }
		}
		


		
		String luceneQuery=(dotAssets)  ? "+basetype:(4 OR 9) " : "+basetype:4 ";
		luceneQuery+=(languageId>0) ? " +languageid:" +  languageId : "";
		luceneQuery+=(host!=null)   ? " +conhost:" + host.getIdentifier() + " +confolder:" + Folder.SYSTEM_FOLDER : "";
		luceneQuery+=(parent!=null) ? " +confolder:" + parent.getInode() :"";
		

		if(UtilMethods.isSet(filter)) {
		    String[] spliter = filter.split(" ");
		    for(String tok : spliter) {
		        luceneQuery+=" +title:" + tok + "*";
		    }
		}
		
        luceneQuery+=showWorking ?  " +working:true +deleted:" + showArchived  : " +live:true";

        List<Contentlet> files = APILocator.getContentletAPI().search(luceneQuery, maxResults, offset, sortBy, user, true);
        


		for (Contentlet file : files) {

            if (file.getBaseType().get() == BaseContentType.FILEASSET) {
                FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(file);
                returnList.add(fileAssetMap(fileAsset, user, showArchived));
            }

            if (file.getBaseType().get() == BaseContentType.DOTASSET) {
                returnList.add(dotAssetMap(file, user, showArchived));
            }
		}



		// Filtering
		List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> asset : returnList) {

			String name = (String) asset.get("name");
			name = name == null ? "" : name;
			String description = (String) asset.get("description");
			description = description == null ? "" : description;
			String mimeType = (String) asset.get("mimeType");
			mimeType = mimeType == null ? "" : mimeType;


			if (mimeTypes != null && mimeTypes.size() > 0) {
				boolean match = false;
				for (String mType : mimeTypes)
					if (mimeType.contains(mType))
						match = true;
				if (!match)
					continue;
			}
			if (extensions != null && extensions.size() > 0) {
				boolean match = false;
				for (String ext : extensions)
					if (((String) asset.get("extension")).contains(ext))
						match = true;
				if (!match)
					continue;
			}
			filteredList.add(asset);
		}
		returnList = filteredList;

		// Sorting
		WebAssetMapComparator comparator = new WebAssetMapComparator(sortBy,
				sortByDesc);
		Collections.sort(returnList, comparator);

		// Offsetting
		if (offset < 0)
			offset = 0;
		if (maxResults <= 0)
			maxResults = returnList.size() - offset;
		if (maxResults + offset > returnList.size())
			maxResults = returnList.size() - offset;

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("total", returnList.size());
		returnMap.put("list", returnList.subList(offset, offset + maxResults));
		return returnMap;
	}

	
	   private Map<String,Object> htmlPageMap(HTMLPageAsset page, User user, boolean showArchived, long languageId) throws DotDataException, DotStateException, DotSecurityException{
	        Map<String, Object> pageMap = new HashMap<>(page.getMap());


            
            Identifier ident = APILocator.getIdentifierAPI().find(
                            page.getVersionId());
                    Role[] roles = com.dotmarketing.business.APILocator.getRoleAPI()
                                    .loadRolesForUser(user.getUserId()).toArray(new Role[0]);

            List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(page, roles, user);

            if (permissions.contains(PERMISSION_READ)) {
                WfData wfdata = page instanceof Contentlet ? new WfData((Contentlet)page, permissions, user, showArchived) : null;
                
    
                pageMap.put("mimeType", "application/dotpage");
                pageMap.put("permissions", permissions);
                pageMap.put("name", page.getPageUrl());
                pageMap.put("description", page.getFriendlyName());
                pageMap.put("extension", "page");
                pageMap.put("isContentlet", true);
                
                if(wfdata!=null ) {
                    pageMap.put("wfActionMapList", wfdata.wfActionMapList);
                    pageMap.put("contentEditable", wfdata.contentEditable);
                }
                

                    pageMap.put("identifier", page.getIdentifier());
                    pageMap.put("inode", page.getInode());
                    pageMap.put("languageId", ((Contentlet)page).getLanguageId());
                    
                    Language lang = APILocator.getLanguageAPI().getLanguage(((Contentlet)page).getLanguageId());
                    
                    pageMap.put("languageCode", lang.getLanguageCode());
                    pageMap.put("countryCode", lang.getCountryCode());
                    pageMap.put("isLocked", page.isLocked());
                    pageMap.put("languageFlag", LanguageUtil.getLiteralLocale(lang.getLanguageCode(), lang.getCountryCode()));
                


                pageMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(page));
                pageMap.put("statusIcons", UtilHTML.getStatusIcons(page));
                pageMap.put("hasTitleImage",String.valueOf(((Contentlet)page).getTitleImage().isPresent()));
                pageMap.put("__icon__", "pageIcon");

            }
	        
	        
	        
	
	        return pageMap;
	   }
	
	
	private Map<String,Object> fileAssetMap(FileAsset fileAsset, User user, boolean showArchived) throws DotDataException, DotStateException, DotSecurityException{
	    Map<String, Object> fileMap = new HashMap<>(fileAsset.getMap());

        Identifier ident = APILocator.getIdentifierAPI().find(
                fileAsset.getVersionId());
        Role[] roles = com.dotmarketing.business.APILocator.getRoleAPI()
                        .loadRolesForUser(user.getUserId()).toArray(new Role[0]);
        
        List<Integer> permissions =permissionAPI.getPermissionIdsFromRoles(fileAsset, roles, user);
        
        WfData wfdata=new WfData(fileAsset, permissions, user, showArchived);
        fileMap.put("permissions", permissions);
        fileMap.put("mimeType", APILocator.getFileAssetAPI()
                .getMimeType(fileAsset.getUnderlyingFileName()));
        fileMap.put("name", ident.getAssetName());
        fileMap.put("fileName", ident.getAssetName());
        fileMap.put("title", fileAsset.getFriendlyName());
        fileMap.put("description", fileAsset instanceof Contentlet ?
                                   ((Contentlet)fileAsset).getStringProperty(FileAssetAPI.DESCRIPTION)
                                   : "");
        fileMap.put("extension", UtilMethods
                .getFileExtension(fileAsset.getUnderlyingFileName()));
        fileMap.put("path", fileAsset.getPath());
        fileMap.put("type", "file_asset");
        Host hoster = APILocator.getHostAPI().find(ident.getHostId(), APILocator.systemUser(), false);
        fileMap.put("hostName", hoster.getHostname());
        
        
        
        
        if(wfdata!=null) {
            fileMap.put("wfActionMapList", wfdata.wfActionMapList);
            fileMap.put("contentEditable", wfdata.contentEditable);
        }
        fileMap.put("size", fileAsset.getFileSize());
        fileMap.put("publishDate", fileAsset.getIDate());
        // BEGIN GRAZIANO issue-12-dnd-template
        fileMap.put(
                "parent",
                fileAsset.getParent() != null ? fileAsset
                        .getParent() : "");

        // END GRAZIANO issue-12-dnd-template
        Language lang = null;

        fileMap.put("identifier", fileAsset.getIdentifier());
        fileMap.put("inode", fileAsset.getInode());
        fileMap.put("isLocked", fileAsset.isLocked());
        fileMap.put("isContentlet", true);
        lang = langAPI.getLanguage(fileAsset.getLanguageId());

        fileMap.put("languageId", lang.getId());
        fileMap.put("languageCode", lang.getLanguageCode());
        fileMap.put("countryCode", lang.getCountryCode());
        fileMap.put("languageFlag", LanguageUtil.getLiteralLocale(lang.getLanguageCode(), lang.getCountryCode()));

        fileMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(fileAsset));
        fileMap.put("statusIcons", UtilHTML.getStatusIcons(fileAsset));
        fileMap.put("hasTitleImage",String.valueOf(fileAsset.getTitleImage().isPresent()));
        fileMap.put("__icon__", UtilMethods.getFileExtension( ident.getURI()) + "Icon");
        return fileMap;
	    
	    
	}
	
	
	
	
	
	   private Map<String,Object> dotAssetMap(Contentlet dotAsset, User user, boolean showArchived) throws DotDataException, DotStateException, DotSecurityException{
	        Map<String, Object> fileMap = new  ContentletToMapTransformer(dotAsset).toMaps().get(0);
	       
	        Identifier ident = APILocator.getIdentifierAPI().find(
	                dotAsset.getVersionId());
	        Role[] roles = com.dotmarketing.business.APILocator.getRoleAPI()
	                        .loadRolesForUser(user.getUserId()).toArray(new Role[0]);
	        
	        List<Integer> permissions =permissionAPI.getPermissionIdsFromRoles(dotAsset, roles, user);
	        final String fileName=Try.of(()->dotAsset.getBinary("asset").getName()).getOrElse("unknown");
	        WfData wfdata=new WfData(dotAsset, permissions, user, showArchived);
	        fileMap.put("permissions", permissions);
	        fileMap.put("mimeType", APILocator.getFileAssetAPI()
	                .getMimeType(fileName));
	        fileMap.put("name", fileName);
	        fileMap.put("fileName", fileName);
	        fileMap.put("title", fileName);
	        fileMap.put("friendyName", "");
	       
	        
	        fileMap.put("extension", UtilMethods.getFileExtension(fileName));
	        fileMap.put("path", "/dA/" + ident.getId() + "/" + fileName);
	        fileMap.put("type", "dotasset");
	        Host hoster = APILocator.getHostAPI().find(ident.getHostId(), APILocator.systemUser(), false);
	        fileMap.put("hostName", hoster.getHostname());
	        
	        
	        
	        if(wfdata!=null) {
	            fileMap.put("wfActionMapList", wfdata.wfActionMapList);
	            fileMap.put("contentEditable", wfdata.contentEditable);
	        }
	        fileMap.put("size", Try.of(()->dotAsset.getBinary("asset").length()).getOrElse(0l));
	        fileMap.put("publishDate", dotAsset.getModDate());
	        // BEGIN GRAZIANO issue-12-dnd-template

	        fileMap.put("isContentlet", true);
	        // END GRAZIANO issue-12-dnd-template
	        Language lang = null;

	        fileMap.put("identifier", dotAsset.getIdentifier());
	        fileMap.put("inode", dotAsset.getInode());
	        fileMap.put("isLocked", dotAsset.isLocked());
	        fileMap.put("isContentlet", true);
	        lang = langAPI.getLanguage(dotAsset.getLanguageId());

	        fileMap.put("languageId", lang.getId());
	        fileMap.put("languageCode", lang.getLanguageCode());
	        fileMap.put("countryCode", lang.getCountryCode());
	        fileMap.put("languageFlag", LanguageUtil.getLiteralLocale(lang.getLanguageCode(), lang.getCountryCode()));

	        fileMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(dotAsset));
	        fileMap.put("statusIcons", UtilHTML.getStatusIcons(dotAsset));
	        fileMap.put("hasTitleImage",String.valueOf(dotAsset.getTitleImage().isPresent()));
	        fileMap.put("__icon__", UtilMethods.getFileExtension( ident.getURI()) + "Icon");
	        return fileMap;
	        
	        
	    }
	    
	
	
	
	
	
	
}

