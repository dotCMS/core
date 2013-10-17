package com.dotmarketing.viewtools;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.ChildrenCondition;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class BrowserAPI {

    private UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
    private FolderAPI folderAPI = APILocator.getFolderAPI();
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    public Map<String, Object> getFolderContent ( User usr, String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
    		List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks ) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, excludeLinks);
    }

    public Map<String, Object> getFolderContent ( User usr, String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
                                                  List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc ) throws DotSecurityException, DotDataException {
    	return getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, true, showArchived, noFolders, onlyFiles, sortBy, sortByDesc);
    }

    public Map<String, Object> getFolderContent(User user, String folderId,
			int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showWorking, boolean showArchived, boolean noFolders,
			boolean onlyFiles, String sortBy, boolean sortByDesc)
			throws DotSecurityException, DotDataException {

    	return getFolderContent(user, folderId, offset, maxResults, filter, mimeTypes, extensions, showWorking, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, false);

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
			boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks)
			throws DotSecurityException, DotDataException {

		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate";
			sortByDesc = true;
		}

		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();

		Role[] roles = new Role[] {};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI()
					.loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class, e1.getMessage(), e1);
		}

		// gets folder parent
		Folder parent = null;
		try {
			parent = APILocator.getFolderAPI().find(folderId, user, false);
		} catch (Exception ignored) {
			// Probably what we have here is a host
		}

		Host host = null;
		if (parent == null) {// If we didn't find a parent folder lets verify if
								// this is a host
			host = APILocator.getHostAPI().find(folderId, user, false);
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
					List<Integer> permissions = new ArrayList<Integer>();
					try {
						permissions = permissionAPI.getPermissionIdsFromRoles(
								folder, roles, user);
					} catch (DotDataException e) {
						Logger.error(this, "Could not load permissions : ", e);
					}
					if (permissions.contains(PERMISSION_READ)) {
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

		if (!onlyFiles) {

			// Getting the html pages directly under the parent folder or host
			List<HTMLPage> pages = new ArrayList<HTMLPage>();
			try {
				if (parent != null) {
					if(!showWorking)
						pages.addAll(folderAPI.getLiveHTMLPages(parent, user, false));
					else
						pages.addAll(folderAPI.getHTMLPages(parent, true, false, user, false));

					if(showArchived)
						pages.addAll(folderAPI.getHTMLPages(parent, true, showArchived, user, false));
				} else {
					pages.addAll(folderAPI.getHTMLPages(host, true,
							showArchived, user, false));
				}
			} catch (Exception e1) {
				Logger.error(this, "Could not load HTMLPages : ", e1);
			}

			for (HTMLPage page : pages) {
				List<Integer> permissions = new ArrayList<Integer>();
				try {
					permissions = permissionAPI.getPermissionIdsFromRoles(page,
							roles, user);
				} catch (DotDataException e) {
					Logger.error(this, "Could not load permissions : ", e);
				}
				if (permissions.contains(PERMISSION_READ)) {
					Map<String, Object> pageMap = page.getMap();
					pageMap.put("mimeType", "application/dotpage");
					pageMap.put("permissions", permissions);
					pageMap.put("name", page.getPageUrl());
					pageMap.put("description", page.getFriendlyName());
					pageMap.put("extension", "page");
					try {
						pageMap.put("pageURI", page.getURI());
					} catch (Exception e) {
						Logger.error(this, "Could not get URI : ", e);
					}
					returnList.add(pageMap);
				}
			}

		}

		List<Versionable> files = new ArrayList<Versionable>();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		try {

			if (parent == null) {

				// Getting the files directly under the host
				files.addAll(APILocator.getFileAssetAPI().findFileAssetsByHost(host, user, !showWorking, showWorking, false, false));
				if(showArchived)
					files.addAll(APILocator.getFileAssetAPI().findFileAssetsByHost(host, user, !showWorking, showWorking, showArchived, false));

			} else {
		        ChildrenCondition cond = new ChildrenCondition();
		        if(showWorking)
		        	cond.working = true;
		        else
		        	cond.live = true;
				files.addAll(folderAPI.getFiles(parent,	userAPI.getSystemUser(), false, cond));
				if (showArchived) {
					cond.deleted = showArchived;
					files.addAll(folderAPI.getFiles(parent, userAPI.getSystemUser(), false, cond));
				}
				files.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(parent, "", !showWorking, showWorking, user, false));
			}

		} catch (Exception e2) {
			Logger.error(this, "Could not load files : ", e2);
		}

		Contentlet contentlet;
		WorkflowStep wfStep;
		WorkflowScheme wfScheme = null;

		for (Versionable file : files) {

			if (file == null)
				continue;

			List<Integer> permissions = new ArrayList<Integer>();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(
						(Permissionable) file, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ", e);
			}

			List<WorkflowAction> wfActions = new ArrayList<WorkflowAction>();

			contentlet = null;
			if (file instanceof com.dotmarketing.portlets.fileassets.business.FileAsset)
				contentlet = (Contentlet) file;
			try {
				if (contentlet != null) {
					wfStep = APILocator.getWorkflowAPI().findStepByContentlet(
							contentlet);
					wfScheme = APILocator.getWorkflowAPI().findScheme(
							wfStep.getSchemeId());
					wfActions = APILocator.getWorkflowAPI()
							.findAvailableActions(contentlet, user);
				}
			} catch (Exception e) {
				Logger.error(this, "Could not load workflow actions : ", e);
				// wfActions = new ArrayList();
			}
			boolean contentEditable = false;
			if (contentlet != null) {
				if (perAPI.doesUserHavePermission(contentlet,
						PermissionAPI.PERMISSION_WRITE, user)
						&& contentlet.isLocked()) {
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
			}

			try {
				if (permissions.contains(PERMISSION_READ)) {
					boolean hasPushPublishActionlet = false;
					if (!showArchived && file.isArchived()) {
						continue;
					}
					IFileAsset fileAsset = (IFileAsset) file;
					List<Map<String, Object>> wfActionMapList = new ArrayList<Map<String, Object>>();
					for (WorkflowAction action : wfActions) {
						List<WorkflowActionClass> actionlets = APILocator
								.getWorkflowAPI().findActionClasses(action);
						if (action.requiresCheckout()) {
							continue;
						}
						Map<String, Object> wfActionMap = new HashMap<String, Object>();
						wfActionMap.put("name", action.getName());
						wfActionMap.put("id", action.getId());
						wfActionMap.put("icon", action.getIcon());
						wfActionMap.put("assignable", action.isAssignable());
						wfActionMap.put("commentable", action.isCommentable()
								|| UtilMethods.isSet(action.getCondition()));
						wfActionMap.put("requiresCheckout",
								action.requiresCheckout());
						wfActionMap.put("wfActionNameStr",
								LanguageUtil.get(user, action.getName()));
						for (WorkflowActionClass actionlet : actionlets) {
							if (actionlet
									.getActionlet()
									.getClass()
									.getCanonicalName()
									.equals(PushPublishActionlet.class
											.getCanonicalName())) {
								hasPushPublishActionlet = true;
							}
						}
						wfActionMap.put("hasPushPublishActionlet",
								hasPushPublishActionlet);
						wfActionMapList.add(wfActionMap);
					}
					if (wfScheme != null && wfScheme.isMandatory()) {
						permissions.remove(new Integer(
								PermissionAPI.PERMISSION_PUBLISH));
					}

					// check for multilingual. we don't need to see the item
					// more than once for every language
					boolean skip = false;
					if (contentlet != null) {
						List<Map<String, Object>> toDelete = new ArrayList<Map<String, Object>>();
						for (Map<String, Object> map : returnList) {
							if (map.get("identifier").equals(
									contentlet.getIdentifier())) {
								if (contentlet.getLanguageId() != APILocator
										.getLanguageAPI().getDefaultLanguage()
										.getId()) {
									// if this is no for the default lang and
									// there is another one in the list skip.
									skip = true;
								} else {
									// if this is for def lang then delete any
									// other we find
									toDelete.add(map);
								}
							}
						}
						returnList.removeAll(toDelete);
					}

					if (skip)
						continue;

					Map<String, Object> fileMap = fileAsset.getMap();

					Identifier ident = APILocator.getIdentifierAPI().find(
							fileAsset.getVersionId());

					fileMap.put("permissions", permissions);
					fileMap.put("mimeType", APILocator.getFileAPI()
							.getMimeType(fileAsset.getFileName()));
					fileMap.put("name", ident.getAssetName());
					fileMap.put("fileName", ident.getAssetName());
					fileMap.put("description", fileAsset.getFriendlyName());
					fileMap.put("extension", UtilMethods
							.getFileExtension(fileAsset.getFileName()));
					fileMap.put("path", fileAsset.getPath());
					fileMap.put("type", fileAsset.getType());
					fileMap.put("wfActionMapList", wfActionMapList);
					fileMap.put("contentEditable", contentEditable);
					fileMap.put("size", fileAsset.getFileSize());
					fileMap.put("publishDate", fileAsset.getIDate());
					// BEGIN GRAZIANO issue-12-dnd-template
					fileMap.put(
							"parent",
							fileAsset.getParent() != null ? fileAsset
									.getParent() : "");
					fileMap.put("isContentlet", false);
					// END GRAZIANO issue-12-dnd-template
					if (contentlet != null) {
						fileMap.put("identifier", contentlet.getIdentifier());
						fileMap.put("inode", contentlet.getInode());
						fileMap.put("languageId", contentlet.getLanguageId());
						fileMap.put("isLocked", contentlet.isLocked());
						fileMap.put("isContentlet", true);
					}

					returnList.add(fileMap);
				}
			} catch (Exception e) {
				Logger.error(this, "Could not load fileAsset : ", e);
			}

		}

		if (!onlyFiles && !excludeLinks) {

			// Getting the links directly under the parent folder or host
			List<Link> links = new ArrayList<Link>();
			try {
				if (parent != null) {
					if(showWorking){
						links.addAll(folderAPI.getLinks(parent, true, false, user,false));
					}else{
						links.addAll(folderAPI.getLiveLinks(parent, user, false));
					}
					if(showArchived)
						links.addAll(folderAPI.getLinks(parent, true, showArchived, user,false));
				} else {
					links = folderAPI.getLinks(host, true, showArchived, user, false);
				}
			} catch (Exception e1) {
				Logger.error(this, "Could not load links : ", e1);
			}

			for (Link link : links) {

				List<Integer> permissions = new ArrayList<Integer>();
				try {
					permissions = permissionAPI.getPermissionIdsFromRoles(link,
							roles, user);
				} catch (DotDataException e) {
					Logger.error(this, "Could not load permissions : ", e);
				}
				if (permissions.contains(PERMISSION_READ)) {
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

		// Filtering
		List<Map<String, Object>> filteredList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> asset : returnList) {

			String name = (String) asset.get("name");
			name = name == null ? "" : name;
			String description = (String) asset.get("description");
			description = description == null ? "" : description;
			String mimeType = (String) asset.get("mimeType");
			mimeType = mimeType == null ? "" : mimeType;

			if (UtilMethods.isSet(filter)
					&& !(name.toLowerCase().contains(filter.toLowerCase()) || description
							.toLowerCase().contains(filter.toLowerCase())))
				continue;
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

}