package com.dotmarketing.portlets.browser.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rendering.velocity.viewtools.BrowserAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
	private ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private BrowserAPI browserAPI = new BrowserAPI();
	private VersionableAPI versionAPI = APILocator.getVersionableAPI();
	private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	String activeHostId = "";
    volatile String activeFolderInode = "";
    private static String SELECTED_BROWSER_PATH_OBJECT = "SELECTED_BROWSER_PATH_OBJECT";
    List<String> openFolders = new ArrayList<String> ();

    String lastSortBy = "name";
    boolean lastSortDirectionDesc = false;


    /**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPI) {
		BrowserAjax.permissionAPI = permissionAPI;
	}

	private String getCurrentHost() {
	    final WebContext ctx = WebContextFactory.get();
	    final String selectedHost = (String) ctx.getHttpServletRequest().getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	    if(selectedHost==null) {
	        // this will fire a host switch host if needed
	        ctx.getHttpServletRequest().getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(ctx.getHttpServletRequest()).getIdentifier());
	    }
	    return selectedHost;
	    
	}

	
    /**
     * This methods is used to load the entire tree by first time.
     * @return The whole folders tree structure.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<Map> getTree(String hostId) throws DotDataException, DotSecurityException {
        hostId = UtilMethods.isSet(hostId) ? hostId : getCurrentHost();
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
     * @param hostId
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
	public List<Map<String, Object>> openFolderContent (String parentInode, String sortBy, boolean showArchived, long languageId) throws DotHibernateException, DotSecurityException, DotDataException {

        activeFolderInode = parentInode;
        this.lastSortBy = sortBy;

    	if (sortBy != null && UtilMethods.isSet(sortBy)) {
    		if (sortBy.equals(lastSortBy)) {
    			this.lastSortDirectionDesc = !this.lastSortDirectionDesc;
    		}
    		this.lastSortBy = sortBy;
    	}

		List<Map<String, Object>> listToReturn;
        try {
			Map<String, Object> resultsMap = getFolderContent(parentInode, 0, -1, "", null, null, showArchived, false, false, this.lastSortBy, this.lastSortDirectionDesc, languageId);
            listToReturn = (List<Map<String, Object>>) resultsMap.get("list");
		} catch ( NotFoundInDbException e ){
            Logger.error( this, "Please refresh the screen you opened this Folder from.", e );
    		listToReturn = new ArrayList<>();
		}

        return listToReturn;
    }

	public Map<String, Object> getFolderContent (String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, boolean excludeLinks, long languageId) throws DotHibernateException, DotSecurityException, DotDataException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User usr = getUser(req);
		HttpSession session = ctx.getSession();
		Map<String, Object> selectedBrowserPathObject = new HashMap<String, Object>();
		if(UtilMethods.isSet(folderId)){
			selectedBrowserPathObject.put("path", getSelectedBrowserPathArray(folderId));
			try {
				selectedBrowserPathObject.put("currentFolder", getFolderMap(folderId));
			} catch (Exception e) {}
			session.setAttribute(SELECTED_BROWSER_PATH_OBJECT, selectedBrowserPathObject);
		}

		req.getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(languageId));

		return browserAPI.getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, excludeLinks, languageId);
	}

	private String[] getSelectedBrowserPathArray(String folderId) {
		List<String> selectedPath = new ArrayList<String>();
		Folder parentFolder = new Folder();
		String[] pathArray = new String[]{"root"};
		try{
			User systemUser = APILocator.getUserAPI().getSystemUser();
			selectedPath.add(folderId);
			String hostId = folderAPI.find(folderId, systemUser, false).getHostId();
			while(parentFolder != null){
				parentFolder = folderAPI.findParentFolder(folderAPI.find(folderId, systemUser, false), systemUser, false);
				if(parentFolder != null){
					selectedPath.add(parentFolder.getInode());
					folderId = parentFolder.getInode();
				}else{
					break;
				}
			}
			pathArray = new String[selectedPath.size()+2];
			int index = 0;
			for(int i = selectedPath.size()+1; i > 1 ; i--){
				pathArray[i] = selectedPath.get(index);
				index++;
			}
			pathArray[0] = "root";
			pathArray[1] = hostId;
		}catch(Exception e){}
		return pathArray;
	}

	public Map<String, Object> getFolderContent (String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, long languageId) throws DotHibernateException, DotSecurityException, DotDataException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User usr = getUser(req);
		
		req.getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(languageId));

		return browserAPI.getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, languageId);
	}

	/**
	 * Retrieves the list of contents under the specified folder. This specific
	 * implementation will only have one identifier per entry. This means that,
	 * for elements such as the new content pages, the list will not contain all
	 * the entries for all the available languages, but only the page in the
	 * default language, or the page in the next available language (one single 
	 * entry per identifier).
	 * 
	 * @param folderId
	 *            - The identifier of the folder whose contents will be
	 *            retrieved.
	 * @param offset
	 *            - The result offset.
	 * @param maxResults
	 *            - The maximum amount of results to return.
	 * @param filter
	 *            - The parameter used to filter the results.
	 * @param mimeTypes
	 *            - The allowed MIME types.
	 * @param extensions
	 *            - The allowed extensions.
	 * @param showArchived
	 *            - If <code>true</code>, retrieve archived elements too.
	 *            Otherwise, set to <code>false</code>.
	 * @param noFolders
	 *            - If <code>true</code>, retrieve everything except for
	 *            folders. Otherwise, set to <code>false</code>.
	 * @param onlyFiles
	 *            - If <code>true</code>, retrieve only file elements.
	 *            Otherwise, set to <code>false</code>.
	 * @param sortBy
	 *            - The sorting parameter.
	 * @param sortByDesc
	 * @param excludeLinks
	 *            - If <code>true</code>, include Links as part of the results.
	 *            Otherwise, set to <code>false</code>.
	 * @return a {@link Map} containing the information of the elements under
	 *         the given folder.
	 * @throws DotHibernateException
	 *             An error occurred during a Hibernate transaction.
	 * @throws DotSecurityException
	 *             The current user does not have permission to perform this
	 *             action.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 */
	public Map<String, Object> getFolderContent(String folderId, int offset,
			int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showArchived, boolean noFolders,
			boolean onlyFiles, String sortBy, boolean sortByDesc,
			boolean excludeLinks) throws DotSecurityException, DotDataException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User usr = getUser(req);
		long getAllLanguages = 0;

		Map<String, Object> results = browserAPI.getFolderContent(usr,
				folderId, offset, maxResults, filter, mimeTypes, extensions,
				showArchived, noFolders, onlyFiles, sortBy, sortByDesc,
				excludeLinks, getAllLanguages);

		listCleanup((List<Map<String, Object>>) results.get("list"), getContentSelectedLanguageId(req));

		return results;
	}

	/**
	 * The list of content under a folder might include the identifier
	 * several times, representing all the available languages for a single
	 * content.
	 * <p>
	 * This method takes that list and <i>leaves only one identifier per
	 * page</i>. This unique record represents either the content with the default
	 * language ID, or the content with the next language ID in the list of system
	 * languages.
	 * </p>
     * * <p>
     * If the fallback properties are false <i>leaves only one identifier per
     * page that match the param languageId</i>.
     * </p>
	 * 
	 * @param results
	 *            - The full list of pages under a given path/directory.
     * @param languageId
     *            - Content Language of the results.
	 */
	private void listCleanup(List<Map<String, Object>> results, long languageId) {
		Map<String, Integer> contentLangCounter = new HashMap<>();

		// Examine only the pages with more than 1 assigned language
		for (Map<String, Object> content : results) {
			if ((boolean) content.get("isContentlet")) {
				String ident = (String) content.get("identifier");
				if (contentLangCounter.containsKey(ident)) {
					int counter = contentLangCounter.get(ident);
					contentLangCounter.put(ident, counter + 1);
				} else {
					contentLangCounter.put(ident, 1);
				}
			}
		}

		Set<String> identifierSet = contentLangCounter.keySet();
		for (String identifier : identifierSet) {
			int counter = contentLangCounter.get(identifier);
			if (counter > 1) {
				// Remove all languages except the default one
				boolean isDeleted = removeAdditionalLanguages(identifier, results, languageId);
				if (!isDeleted) {
					// Otherwise, leave only the next available language
					List<Language> languages = this.languageAPI.getLanguages();
					for (Language language : languages) {
						if (language.getId() != languageId) {
							isDeleted = removeAdditionalLanguages(identifier, results, language.getId());
							if (isDeleted) {
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Removes all other contents from the given list that ARE NOT associated to
	 * the specified language ID. In the end, the list will contain one content per
	 * identifier with either the default language ID or the next available
	 * language.
	 * 
	 * @param identifier
	 *            - The identifier of the page to clean up in the list.
	 * @param resultList
	 *            - The list of all pages that will be displayed.
	 * @param languageId
	 *            - The language ID of the page that will remain in the list.
	 * @return If <code>true</code>, the identifier with the specified language
	 *         ID was successfully cleaned up. If <code>false</code>, the
	 *         identifier is not associated to the specified language ID and was
	 *         not removed from the list.
	 */
	private boolean removeAdditionalLanguages(String identifier,
			List<Map<String, Object>> resultList, long languageId) {

		boolean removeOtherLangs = false;

		for (Map<String, Object> contentInfo : resultList) {
			if ((boolean) contentInfo.get("isContentlet")) {
				String ident = (String) contentInfo.get("identifier");
				if (identifier.equals(ident)) {
					long langId = (long) contentInfo.get("languageId");
					// If specified language is found, remove all others
					if (languageId == langId) {
						removeOtherLangs = true;
						break;
					}
				}
			}
		}

		if (removeOtherLangs) {
			removeLangOtherThan(resultList, identifier, languageId);
		}

		return removeOtherLangs;
	}

	/**
	 * Removes all the pages from the list that are not associated to the
	 * specified language. In the end, the list will contain one page per
	 * identifier.
	 * 
	 * @param resultList
	 *            - The list of pages that will be displayed.
	 * @param identifier
	 *            - The identifier for the page to lookup in the list.
	 * @param languageId
	 *            - The language ID that <b>MUST REMAIN</b> in the list.
	 */
	private void removeLangOtherThan(List<Map<String, Object>> resultList,
			String identifier, long languageId) {
		List<Integer> itemsToRemove = new ArrayList<Integer>();
		for (int i = 0; i < resultList.size(); i++) {
			Map<String, Object> pageInfo = resultList.get(i);
			if ((boolean) pageInfo.get("isContentlet")) {
				String ident = (String) pageInfo.get("identifier");
				if (identifier.equals(ident)) {
					long langId = (long) pageInfo.get("languageId");
					if (languageId != langId) {
						itemsToRemove.add(i);
					}
				}
			}
		}
		int deletionCounter = 0;
		for (int index : itemsToRemove) {
			// Adjust index based on previous deletions
			int indexAfterDeletion = index - deletionCounter;
			resultList.remove(indexAfterDeletion);
			deletionCounter++;
		}
	}

	public Map<String, Object> saveFileAction(String selectedItem,String wfActionAssign,String wfActionId,String wfActionComments, String wfConId, String wfPublishDate,
			String wfPublishTime, String wfExpireDate, String wfExpireTime, String wfNeverExpire, String whereToSend, String forcePush) throws  DotSecurityException, ServletException{
		WebContext ctx = WebContextFactory.get();
        User user = getUser(ctx.getHttpServletRequest());
		Contentlet contentlet = null;
		Map<String, Object> result = new HashMap<String, Object>();
		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		try {
			WorkflowAction action = wapi.findAction(wfActionId, user);
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
			contentlet = APILocator.getContentletAPI().find(wfConId, user, false);
			contentlet.setStringProperty("wfActionId", action.getId());
			contentlet.setStringProperty("wfActionComments", wfActionComments);
			contentlet.setStringProperty("wfActionAssign", wfActionAssign);

			contentlet.setStringProperty("wfPublishDate", wfPublishDate);
			contentlet.setStringProperty("wfPublishTime", wfPublishTime);
			contentlet.setStringProperty("wfExpireDate", wfExpireDate);
			contentlet.setStringProperty("wfExpireTime", wfExpireTime);
			contentlet.setStringProperty("wfNeverExpire", wfNeverExpire);
			contentlet.setStringProperty("whereToSend", whereToSend);
			contentlet.setStringProperty("forcePush", forcePush);
			contentlet.setTags();

			wapi.fireWorkflowNoCheckin(contentlet, user);

			result.put("status", "success");
			result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "Workflow-executed")));

		} catch (Exception e) {
			Logger.error(BrowserAjax.class, e.getMessage(), e);
			result.put("status", "error");
			try {
				result.put("message",
						UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "Workflow-action-execution-error")+" "+ e.getMessage()));
			}catch(LanguageException le){
				Logger.error(BrowserAjax.class, le.getMessage(), le);
			}
		}
		return result;
	}

	public Map<String, Object> getFileInfo(String fileId, long languageId) throws DotDataException, DotSecurityException, PortalException, SystemException {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest req = ctx.getHttpServletRequest();
        ServletContext servletContext = ctx.getServletContext();
        User user = userAPI.getLoggedInUser(req);
        boolean respectFrontendRoles = userAPI.isLoggedToFrontend(req);

        Identifier ident = APILocator.getIdentifierAPI().find(fileId);

		if(languageId==0) {
			languageId = languageAPI.getDefaultLanguage().getId();
		}

		if(ident!=null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals("contentlet")) {
		    ContentletVersionInfo vinfo=versionAPI.getContentletVersionInfo(ident.getId(), languageId);

			if(vinfo==null && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
				languageId = languageAPI.getDefaultLanguage().getId();
				vinfo=versionAPI.getContentletVersionInfo(ident.getId(), languageId);
			}
		    boolean live = respectFrontendRoles || vinfo.getLiveInode()!=null;
			Contentlet cont = contentletAPI.findContentletByIdentifier(ident.getId(),live, languageId , user, respectFrontendRoles);
			if(cont.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
    			FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(cont);
    			java.io.File file = fileAsset.getFileAsset();
    			String mimeType = servletContext.getMimeType(file.getName().toLowerCase());
    			Map<String, Object> fileMap = fileAsset.getMap();
    			fileMap.put("mimeType", mimeType);
    			fileMap.put("path", fileAsset.getPath());
    			fileMap.put("type", "contentlet");
    			return fileMap;
			}
			else if(cont.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_HTMLPAGE) {
			    HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(cont);
			    Map<String, Object> pageMap = page.getMap();
			    pageMap.put("mimeType", "application/dotpage");
	            pageMap.put("pageURI", ident.getURI());
	            return pageMap;
			}
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
    	result.put("assetType", "folder");
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

    /**
     * Copies a given inode folder/host reference into another given folder
     *
     * @param inode     folder inode
     * @param newFolder This could be the inode of a folder or a host
     * @return Confirmation message
     * @throws Exception
     */
    public String copyFolder ( String inode, String newFolder ) throws Exception {

        HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( request );

        UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
        HostAPI hostAPI = APILocator.getHostAPI();

		final Locale requestLocale       = request.getLocale();
		final String successString       = UtilMethods.escapeSingleQuotes(LanguageUtil.get(requestLocale, "Folder-copied"));
		final String errorTryToMoveFolderToItself       = UtilMethods.escapeSingleQuotes(LanguageUtil.get(requestLocale, "Folder-copied-to-itself"));
		final String errorTryToMoveFolderToChild       = UtilMethods.escapeSingleQuotes(LanguageUtil.get(requestLocale, "Folder-copied-to-children"));

        //Searching for the folder to copy
        Folder folder = APILocator.getFolderAPI().find( inode, user, false );

        try {
			if ( !folderAPI.exists( newFolder ) ) {

				Host parentHost = hostAPI.find( newFolder, user, !userWebAPI.isLoggedToBackend( request ) );

				if ( !permissionAPI.doesUserHavePermission( folder, PERMISSION_WRITE, user ) || !permissionAPI.doesUserHavePermission( parentHost, PERMISSION_WRITE, user ) ) {
					throw new DotRuntimeException( "The user doesn't have the required permissions." );
				}

				folderAPI.copy( folder, parentHost, user, false );
				refreshIndex( null, parentHost, folder );
			} else {

				Folder parentFolder = APILocator.getFolderAPI().find( newFolder, user, false );

				if ( !permissionAPI.doesUserHavePermission( folder, PermissionAPI.PERMISSION_WRITE, user ) || !permissionAPI.doesUserHavePermission( parentFolder, PERMISSION_WRITE, user ) ) {
					throw new DotRuntimeException( "The user doesn't have the required permissions." );
				}

				if ( parentFolder.getInode().equalsIgnoreCase( folder.getInode() ) ) {
					//Trying to move a folder over itself
					return errorTryToMoveFolderToItself;
				}
				if ( folderAPI.isChildFolder( parentFolder, folder ) ) {
					//Trying to move a folder over one of its children
					return errorTryToMoveFolderToChild;
				}

				folderAPI.copy( folder, parentFolder, user, false );
				refreshIndex(parentFolder, null, folder );
			}
		} catch(InvalidFolderNameException e ) {
			Logger.error(this, "Error copying folder with id:" + folder.getInode() + " into folder with id:"
					+ newFolder + ". Error: " + e.getMessage());
			return e.getLocalizedMessage();
		}

        return successString;
    }

    /**
     * Moves a given inode folder/host reference into another given folder
     *
     * @param folderId     folder identifier
     * @param newFolderId This could be the inode of a folder or a host
     * @return Confirmation message
     * @throws Exception
     */
    public String moveFolder (final String folderId, final String newFolderId) throws Exception {

    	final HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
        final Locale requestLocale       = request.getLocale();
        final String successString       = UtilMethods.escapeSingleQuotes(LanguageUtil.get(requestLocale, "Folder-moved"));
		final String errorString         = UtilMethods.escapeSingleQuotes(LanguageUtil.get(requestLocale, "Failed-to-move-another-folder-with-the-same-name-already-exists-in-the-destination"));

        try {

            final User user = getUser(request);
            final boolean respectFrontendRoles = !this.userAPI.isLoggedToBackend(request);

            if (!this.folderAPI.move(folderId, newFolderId, user, respectFrontendRoles)) {

            	return errorString;
			}
        } catch(InvalidFolderNameException e ) {
			Logger.error(this, "Error moving folder with id:" + folderId + " into folder with id:"
					+ newFolderId + ". Error: " + e.getMessage());
			return e.getLocalizedMessage();
		}catch (Exception e) {
        	Logger.error(this, "Error moving folder with id:" + folderId + " into folder with id:"
					+ newFolderId + ". Error: " + e.getMessage(), e);
            return e.getLocalizedMessage();
        }

        return successString;
    }

    public Map<String, Object> renameFile (String inode, String newName) throws Exception {

    	HashMap<String, Object> result = new HashMap<String, Object> ();

    	HibernateUtil.startTransaction();

    	try {

    		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
    		User user = null;
    		try {
    			user = com.liferay.portal.util.PortalUtil.getUser(req);
    		} catch (Exception e) {
    			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
    			throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
    		}

    		Identifier id  = APILocator.getIdentifierAPI().findFromInode(inode);
    		if(id!=null && id.getAssetType().equals("contentlet")){
    			Contentlet cont  = APILocator.getContentletAPI().find(inode, user, false);
    			String lName = (String) cont.get(FileAssetAPI.FILE_NAME_FIELD);
    			result.put("lastName", lName.substring(0, lName.lastIndexOf(".")));
    			result.put("extension", UtilMethods.getFileExtension(cont.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)));
    			result.put("newName", newName);
    			result.put("inode", inode);
    			if(!cont.isLocked()){
    				try{
    					if(APILocator.getFileAssetAPI().renameFile(cont, newName, user, false)){
    						result.put("result", 0);
    					}else{
    						result.put("result", 1);
    						result.put("errorReason", "Another file with the same name already exists on this folder");
    					}
    				}catch(Exception e){
    					result.put("result", 1);
    					result.put("errorReason", e.getLocalizedMessage());
    				}
    			}else{
    				result.put("result", 1);
    				result.put("errorReason", "The file is locked");
    			}
    		}

    	} catch ( Exception e ) {
    		HibernateUtil.rollbackTransaction();
    	} finally {
    		HibernateUtil.closeAndCommitTransaction();
    	}

    	return result;

    }

    /**
     * Copies a given inode reference to a given folder
     *
     * @param inode     Contentlet inode
     * @param newFolder This could be the inode of a folder or a host
     * @return Confirmation message
     * @throws Exception
     */
    public Map<String, Object> copyFile ( String inode, String newFolder ) throws Exception {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        Map<String, Object> result = new HashMap<String, Object>();

        try{
			//Contentlet file identifier
			Identifier id = APILocator.getIdentifierAPI().findFromInode( inode );

			// gets folder parent
			Folder parent = null;
			try {
				parent = APILocator.getFolderAPI().find( newFolder, user, false );
			} catch ( Exception ignored ) {
				//Probably what we have here is a host
			}

			Host host = null;
			if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
				host = APILocator.getHostAPI().find( newFolder, user, false );
			}

			// Checking permissions
			if(!hasFileWritePermissions(host, parent, id, user)) {
			    result.put("status", "error");
			    result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "File-failed-to-copy-check-you-have-the-required-permissions")));
			    return result;
			}

			if ( id != null && id.getAssetType().equals( "contentlet" ) ) {

				//Getting the contentlet file
				Contentlet cont = APILocator.getContentletAPI().find( inode, user, false );

				if ( parent != null ) {
					FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet( cont );
					if ( UtilMethods.isSet( fileAsset.getUnderlyingFileName() ) && !folderAPI.matchFilter( parent, fileAsset.getUnderlyingFileName() ) ) {
						result.put("status", "error");
						result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "message.file_asset.error.filename.filters")));
						return result;
					}
				}

				if ( parent != null ) {
					APILocator.getContentletAPI().copyContentlet( cont, parent, user, false );
				} else {
					APILocator.getContentletAPI().copyContentlet( cont, host, user, false );
				}

				// issues/1788
				// issues/1967

				Folder srcFolder = APILocator.getFolderAPI().find(cont.getFolder(),user,false);
				refreshIndex(parent, host, srcFolder );

				result.put("status", "success");
				result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "File-copied")));
				return result;
			}

            return result;
		}
        catch(Exception ex) {
            Logger.error(this, "Error trying to copy the file to folder.", ex);

            // File asset URL already exist
            if(ex instanceof DotDataException && ((DotDataException) ex).getMessage().equalsIgnoreCase("error.copy.url.conflict")) {
                result.put("status", "error");
                result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "message.htmlpage.error.htmlpage.exists.file")));
                return result;
            }

            result.put("status", "error");
            result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "message.file.error.generic.copy")));
            return result;
        }
    }
    
    /**
     * This method verify if the user has write permissions of the file asset
     * @param host
     * @param parent
     * @param id
     * @param user
     * @return true if has permissions otherwise false
     */
    private boolean hasFileWritePermissions(Host host, Folder parent, Identifier id, User user) {
        boolean allowed = false;

        final String permissionsError = "The user doesn't have the required permissions.";
        try {
            // Checking permissions
            if ( !permissionAPI.doesUserHavePermission( id, PERMISSION_WRITE, user ) ) {
                Logger.error(this, permissionsError);
            } else if ( parent != null && !permissionAPI.doesUserHavePermission( parent, PERMISSION_WRITE, user ) ) {
                Logger.error(this, permissionsError);
            } else if ( host != null && !permissionAPI.doesUserHavePermission( host, PERMISSION_WRITE, user ) ) {
                Logger.error(this, permissionsError);
            } else {
                allowed = true;
            }
        } catch(DotDataException e) {
            Logger.error(this, permissionsError, e);
        }

        return allowed;
    }

    /**
     * Moves a given inode reference to a given folder
     *
     * @param inode  Contentlet inode
     * @param folder This could be the inode of a folder or a host
     * @return true if success, false otherwise
     * @throws Exception
     */
    public boolean moveFile ( String inode, String folder ) throws Exception {

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        //Contentlet file identifier
        Identifier id = APILocator.getIdentifierAPI().findFromInode( inode );

        // gets folder parent
        Folder parent = null;
        try {
            parent = APILocator.getFolderAPI().find( folder, user, false );
        } catch ( Exception ignored ) {
            //Probably what we have here is a host
        }

        Host host = null;
        if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
            host = APILocator.getHostAPI().find( folder, user, false );
        }

        // Checking permissions
        if ( !permissionAPI.doesUserHavePermission( id, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( "The user doesn't have the required permissions." );
        } else if ( parent != null && !permissionAPI.doesUserHavePermission( parent, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( "The user doesn't have the required permissions." );
        } else if ( host != null && !permissionAPI.doesUserHavePermission( host, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( "The user doesn't have the required permissions." );
        }

        if ( id != null && id.getAssetType().equals( "contentlet" ) ) {

            //Getting the contentlet file
            final Contentlet contentlet = APILocator.getContentletAPI().find( inode, user, false );
            contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true); // on move we do not want to run a workflow
            Folder srcFolder = APILocator.getFolderAPI().find(contentlet.getFolder(),user,false);

            if(contentlet.getFolder().equals("SYSTEM_FOLDER")) {
            	refreshIndex(null, APILocator.getHostAPI().find(contentlet.getHost(), user, false), srcFolder );
            } else {
            	refreshIndex(parent, host, srcFolder );
            }

            if ( parent != null ) {
                return APILocator.getFileAssetAPI().moveFile( contentlet, parent, user, false );
            } else {
                return APILocator.getFileAssetAPI().moveFile( contentlet, host, user, false );
            }
        }

        return false;
    }

	public Map<String, Object> renameHTMLPage ( String inode, String newName ) throws Exception {

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = getUser( req );

		Identifier ident = APILocator.getIdentifierAPI().findFromInode( inode );
		IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet( APILocator.getContentletAPI().find( inode, user, false ) );

		String pageURL = ident.getAssetName();
		String lastName = (pageURL.lastIndexOf( "." ) > -1) ? pageURL.substring( 0, pageURL.lastIndexOf( "." ) ) : pageURL;

		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put( "lastName", lastName );
		result.put( "newName", newName );
		result.put( "inode", inode );

		if ( APILocator.getHTMLPageAssetAPI().rename( (HTMLPageAsset) page, newName, user ) ) {
			result.put( "result", 0 );
		} else {
			result.put( "result", 1 );
			if ( page.isLocked() )
				result.put( "errorReason", "The page is locked" );
			else
				result.put( "errorReason", "Another page with the same name already exists on this folder" );
		}
		return result;
	}

    /**
     * Copies a given inode HTMLPage to a given folder
     *
     * @param inode     HTMLPage inode
     * @param newFolder This could be the inode of a folder or a host
     * @return true if success, false otherwise
     * @throws Exception
     */
    public Map<String, Object> copyHTMLPage ( String inode, String newFolder ) throws Exception {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        Map<String, Object> result = new HashMap<String, Object>();

        try {
            Identifier ident=APILocator.getIdentifierAPI().findFromInode(inode);
            IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                                     APILocator.getContentletAPI().find(inode, user, false));
    
            // gets folder parent
            Folder parent = null;
            try {
                parent = APILocator.getFolderAPI().find( newFolder, user, false );
            } catch ( Exception ignored ) {
                //Probably what we have here is a host
            }
    
            Host host = null;
            if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
                host = APILocator.getHostAPI().find( newFolder, user, false );
            }
    
            // Checking permissions
            if(!hasHTMLPageWritePermissions(host, parent, page, user)) {
                Logger.error(this, "The user doesn't have the required permissions.");
                result.put("status", "error");
                result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "Failed-to-copy-check-you-have-the-required-permissions")));
                return result;
            }


                Contentlet cont=APILocator.getContentletAPI().find(inode, user, false);
                Contentlet newContentlet=null;
                if(parent!=null) {
                	newContentlet=APILocator.getContentletAPI().copyContentlet(cont, parent, user, false);
                }
                else {
                	newContentlet=APILocator.getContentletAPI().copyContentlet(cont, host, user, false);
                }

            result.put("status", "success");
            result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "Page-copied")));
            return result;
        } catch(Exception e) {
            Logger.error(this, "Error copying the html page.", e);

            // Page URL already exist
            if(e instanceof DotDataException && ((DotDataException) e).getMessage().equalsIgnoreCase("error.copy.url.conflict")) {
                result.put("status", "error");
                result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "message.htmlpage.error.htmlpage.exists")));
                return result;
            }

            result.put("status", "error");
            result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "message.htmlpage.error.generic.copy")));
            return result;
        }
    }
    
    /**
     * This method verify if the user has write permissions of the page
     * @param host
     * @param parent
     * @param page
     * @param user
     * @return true if has permissions otherwise false
     */
    private boolean hasHTMLPageWritePermissions(Host host, Folder parent, IHTMLPage page, User user) {
        boolean allowed = false;

        final String permissionsError = "The user doesn't have the required permissions.";
        try {
            // Checking permissions
            if (!permissionAPI.doesUserHavePermission(page, PERMISSION_WRITE, user)) {
                Logger.error(this, permissionsError);
            } else if (parent != null && !permissionAPI.doesUserHavePermission(parent, PERMISSION_CAN_ADD_CHILDREN, user)) {
                Logger.error(this, permissionsError);
            } else if (host != null && !permissionAPI.doesUserHavePermission(host, PERMISSION_CAN_ADD_CHILDREN, user)) {
                Logger.error(this, permissionsError);
            } else {
                allowed = true;
            }
        } catch(DotDataException e) {
            Logger.error(this, permissionsError, e);
        }

        return allowed;
    }

    /**
     * Moves a given inode HTMLPage to a given folder
     *
     * @param inode     HTMLPage inode
     * @param newFolder This could be the inode of a folder or a host
     * @return true if success, false otherwise
     * @throws Exception
     */
    public boolean moveHTMLPage ( String inode, String newFolder ) throws Exception {

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        Identifier ident=APILocator.getIdentifierAPI().findFromInode(inode);
        IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                                 APILocator.getContentletAPI().find(inode, user, false));

        // gets folder parent
        Folder parent = null;
        try {
            parent = APILocator.getFolderAPI().find( newFolder, user, false );
        } catch ( Exception ignored ) {
            //Probably what we have here is a host
        }

        Host host = null;
        if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
            host = APILocator.getHostAPI().find( newFolder, user, false );
        }

        // Checking permissions
        String permissionsError = "The user doesn't have the required permissions.";
        if ( !permissionAPI.doesUserHavePermission( page, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( parent != null && !permissionAPI.doesUserHavePermission( parent, PERMISSION_CAN_ADD_CHILDREN, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( host != null && !permissionAPI.doesUserHavePermission( host, PERMISSION_CAN_ADD_CHILDREN, user ) ) {
            throw new DotRuntimeException( permissionsError );
        }

            if ( parent != null ) {
                return APILocator.getHTMLPageAssetAPI().move((HTMLPageAsset)page, parent, user);
            }
            else {
                return APILocator.getHTMLPageAssetAPI().move((HTMLPageAsset)page, host, user);
            }

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

    
    public Map<String, Object> getStatus(String inode) throws Exception {

      HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
      User user = null;
      try {
          user = com.liferay.portal.util.PortalUtil.getUser(req);
      } catch (Exception e) {
          Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
          throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
      }
      
      Contentlet c = contentletAPI.find(inode, user, false);
      HashMap<String, Object> result = new HashMap<String, Object> ();
      result.put("LIVE", false);
      result.put("WORKING", false);
      result.put("LOCKED", false);
      result.put("DELETED", false);
      try{
        result.put("LIVE", versionAPI.isLive(c));
        result.put("WORKING", versionAPI.isWorking(c));
        result.put("DELETED", versionAPI.isDeleted(c));
        result.put("LOCKED", versionAPI.isLocked(c));

      }
      catch(Exception e){
        Logger.warn(this.getClass(), "getStatus failed for inode:" + inode + ":" + e.getMessage());
      }
      
      
      
      
      
      
      return result;
  }
    
    
    /**
     * Copies a given inode Link to a given folder
     *
     * @param inode     Link inode
     * @param newFolder This could be the inode of a folder or a host
     * @return true if success, false otherwise
     * @throws Exception
     */
    public boolean copyLink ( String inode, String newFolder ) throws Exception {

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        Link link = (Link) InodeFactory.getInode( inode, Link.class );

        // gets folder parent
        Folder parent = null;
        try {
            parent = APILocator.getFolderAPI().find( newFolder, user, false );
        } catch ( Exception ignored ) {
            //Probably what we have here is a host
        }

        Host host = null;
        if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
            host = APILocator.getHostAPI().find( newFolder, user, false );
        }

        // Checking permissions
        String permissionsError = "The user doesn't have the required permissions.";
        if ( !permissionAPI.doesUserHavePermission( link, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( parent != null && !permissionAPI.doesUserHavePermission( parent, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( host != null && !permissionAPI.doesUserHavePermission( host, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        }

        if ( parent != null ) {
            LinkFactory.copyLink( link, parent );
        } else {
            LinkFactory.copyLink( link, host );
        }

        return true;
    }

    /**
     * Moves a given inode Link to a given folder
     *
     * @param inode     Link inode
     * @param newFolder This could be the inode of a folder or a host
     * @return true if success, false otherwise
     * @throws Exception
     */
    public boolean moveLink ( String inode, String newFolder ) throws Exception {

        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser( req );

        Link link = (Link) InodeFactory.getInode( inode, Link.class );

        // gets folder parent
        Folder parent = null;
        try {
            parent = APILocator.getFolderAPI().find( newFolder, user, false );
        } catch ( Exception ignored ) {
            //Probably what we have here is a host
        }

        Host host = null;
        if ( parent == null ) {//If we didn't find a parent folder lets verify if this is a host
            host = APILocator.getHostAPI().find( newFolder, user, false );
        }

        // Checking permissions
        String permissionsError = "The user doesn't have the required permissions.";
        if ( !permissionAPI.doesUserHavePermission( link, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( parent != null && !permissionAPI.doesUserHavePermission( parent, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        } else if ( host != null && !permissionAPI.doesUserHavePermission( host, PERMISSION_WRITE, user ) ) {
            throw new DotRuntimeException( permissionsError );
        }

        if ( parent != null ) {
            return LinkFactory.moveLink( link, parent );
        } else {
            return LinkFactory.moveLink( link, host );
        }
    }

    /**
     * Publish a given asset, in case of html pages can be published only if there is not related to it
     * unpublished content.
     *
     * @param inode
     * @return
     * @throws Exception
     */
    public boolean publishAsset ( String inode ) throws Exception {


       return _publishAsset(inode); 

    }


	private boolean _publishAsset(String inode) throws Exception {

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = getUser(req);

		Identifier id = APILocator.getIdentifierAPI().findFromInode(inode);
		if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user)) {
			throw new DotRuntimeException("The user doesn't have the required permissions.");
		}

		HTMLPageAsset htmlPageAsset = null;
		if (id != null && id.getAssetType().equals("contentlet")) {
			Contentlet cont = APILocator.getContentletAPI().find(inode, user, false);

			//Verify if it is a HTMLPage, if not let do a normal contentlet publish
			if (cont.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
				htmlPageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(cont);
			} else {
				APILocator.getContentletAPI().publish(cont, user, false);
				return true;
			}
		}

        /*
		Verify if we have unpublish content related to this page
         */
		java.util.List relatedAssets = new java.util.ArrayList();
		if (htmlPageAsset != null && InodeUtils
				.isSet(htmlPageAsset.getInode())) {//Verify for HTMLPages as content
			relatedAssets = PublishFactory
					.getUnpublishedRelatedAssetsForPage(htmlPageAsset, relatedAssets, false, user,
							false);
		}
		/*
		Publishing the HTMLPage
        */
		if (htmlPageAsset != null && InodeUtils
				.isSet(htmlPageAsset.getInode())) {//Publish for the new HTMLPages as content

			if (!permissionAPI.doesUserHavePermission(htmlPageAsset, PERMISSION_PUBLISH, user)) {
				throw new Exception("The user doesn't have the required permissions.");
			}

			//Publish the page
			return PublishFactory.publishHTMLPage(htmlPageAsset, req);
		}

		if(htmlPageAsset == null){
			Link menuLink = APILocator.getMenuLinkAPI().find(inode,user,false);
			return PublishFactory.publishAsset(menuLink,req);

		}

		return false;
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
        		cont.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
        		APILocator.getContentletAPI().unpublish(cont, user, false);
        		ret = true;
        	}else{
        		WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);
        		Folder parent = (Folder)folderAPI.findParentFolder(asset, user, false);
        		ret = WebAssetFactory.unPublishAsset(asset, user.getUserId(), parent);
        	}
        	HibernateUtil.closeAndCommitTransaction();
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
    		return APILocator.getContentletAPI().delete(cont, user, false);
    	}


        WebAsset asset = (WebAsset) InodeFactory.getInode(inode, Inode.class);

        //I verify the permissions in the methods but I could change that
        //if (!PermissionFactory.doesUserHavePermission(asset, PERMISSION_WRITE, user))
		//	throw new DotRuntimeException("The user doesn't have the required permissions.");

        WebAssetFactory.deleteAsset(asset, user);
        return true;
    }

    /**
     * Delete HTML page asset by inode
     * @param inode
     * @return map with status and message
     * @throws Exception
     */
    public Map<String, Object> deleteHTMLPageAsset(String inode) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);

        Identifier id = identifierAPI.findFromInode(inode);
        if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user)) {
            result.put("status", "error");
            result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user,
                    "Failed-to-delete-check-you-have-the-required-permissions")));
            return result;
        }

        if (id != null && id.getAssetType().equals("contentlet")) {
			Contentlet cont = contentletAPI.find(inode, user, false);

            // If delete has errors send a message
            if (!contentletAPI.delete(cont, user, false)) {
                result.put("status", "error");
                result.put("message", UtilMethods.escapeSingleQuotes(LanguageUtil.get(user,
                        "HTML-Page-deleted-error")));
                return result;
            }
        }

        result.put("status", "success");
        result.put("message",
                UtilMethods.escapeSingleQuotes(LanguageUtil.get(user, "HTML-Page-deleted")));
        return result;
    }

	/**
	 * Verifies if a page is being used as a detail page for any content type
	 * @return
	 * @throws DotDataException
	 * @throws LanguageException
	 */
	public Map<String, Object> validateRelatedContentType(String inode)
			throws DotDataException, LanguageException, DotSecurityException {

		Map<String, Object> result = new HashMap<>();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = getUser(req);
		StringBuilder relatedPagesMessage = new StringBuilder();
		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
		Contentlet cont = contentletAPI.find(inode, user, false);
		int relatedContentTypes = contentTypeAPI.count("page_detail='" + cont.getIdentifier() + "'");

		//Verifies if the page is related to any content type
		if (relatedContentTypes > 0){

            relatedPagesMessage.append(UtilMethods.escapeSingleQuotes(LanguageUtil.get(user,
                    "HTML-Page-related-content-type-delete-confirm")));
        }

        result.put("message", relatedPagesMessage.toString());
		result.put("inode", inode);
		return result;
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
	 * This mehod will retrieve the HTML Page LAnguage (WebKeys.CONTENT_SELECTED_LANGUAGE) that is set up in the session.
	 *
	 * @param request
	 * @return language id used in Edit Contentlet Page or default language id if it is not set.
     */
    private long getContentSelectedLanguageId(HttpServletRequest request){
		long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		if ( request != null &&
			request.getSession() != null &&
			request.getSession().getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE) != null ) {

			try{
				languageId = Long.parseLong((String)request.getSession().getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE));
			} catch (Exception e){
				Logger.error(this.getClass(),
					"Language Id from request is not a long value. " +
						"We will use Default Language. " +
						"Value from request: " + languageId, e);
			}
		}

		return languageId;
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
		hostMap.put("hostName",
			host.isSystemHost() ? this.languageAPI.getStringKey(this.languageAPI.getDefaultLanguage(), "tag-system-host")
				: host.getHostname());
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

	public List<Map<String, Object>> getHostsWithThemes() throws PortalException, SystemException, DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();

		// get hosts the user has read permissions on
		List<Host> hosts = hostAPI.getHostsWithPermission(com.dotmarketing.business.PermissionAPI.PERMISSION_READ, false, user, false);

		List<Map<String, Object>> hostsToReturn = new ArrayList<Map<String,Object>>(hosts.size());
		List<Host> filteredHosts = new ArrayList<Host>();

		for (Host h : hosts) {
			Folder folder = APILocator.getFolderAPI().findFolderByPath("/application/themes/", h , user, false);
			// add hosts who have /application/themes/ folder
			// add hosts the user has read permissions to the /application/themes/ folder
			if(UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getName()) &&
					permissionAPI.doesUserHavePermissions(folder,"TEMPLATE_LAYOUTS:"+PermissionAPI.PERMISSION_READ, user)) {
				filteredHosts.add(h);
			}
		}

		Collections.sort(hosts, new HostNameComparator());
		for (Host h: filteredHosts) {
			if(permissionAPI.doesUserHavePermissions(h,"TEMPLATE_LAYOUTS:"+PermissionAPI.PERMISSION_READ, user)){
				hostsToReturn.add(hostMap(h));
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
					Structure defaultFileAssetStructure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
					folderMap.put("defaultFileType", defaultFileAssetStructure.getInode());
					folderMap.put("fullPath", host.getHostname() + ":/");
					folderMap.put("absolutePath", "/");
					return folderMap;
				}

			}

		}
		return null;
	}

	public List<Map<String, Object>> getHostThemes(String hostId) throws PortalException, SystemException, DotDataException, DotSecurityException {
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
		List<Folder> folders = folderAPI.findThemes(host, user, respectFrontendRoles);
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


	public void refreshIndex(final Folder parent,
							 final Host host,
							 final Folder folder ) throws Exception {

        if (folder!=null) {

			this.contentletAPI.refreshContentUnderFolderPath(folder.getHostId(), folder.getPath());
     	}

        if ( parent != null ) {
			this.contentletAPI.refreshContentUnderFolderPath(parent.getHostId(), parent.getPath());
        } else {
			this.contentletAPI.refreshContentUnderHost(host);
        }
	}
	
	public Map<String, Object> getSelectedBrowserPath(){
		Map<String, Object> result = new HashMap<String, Object>();
		HttpSession session = WebContextFactory.get().getSession();
		if(UtilMethods.isSet(session.getAttribute(SELECTED_BROWSER_PATH_OBJECT)))
			return (Map<String, Object>) session.getAttribute(SELECTED_BROWSER_PATH_OBJECT);
		
		result.put("path", new String[]{"root"});
		result.put("currentFolder", null);
		return result;
	}
	
	public boolean deleteHTMLPagePreCheck(String htmlPageInode) throws Exception{
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = getUser(req);


        Identifier id  = APILocator.getIdentifierAPI().findFromInode(htmlPageInode);
    	if (!permissionAPI.doesUserHavePermission(id, PERMISSION_PUBLISH, user))
    		throw new DotRuntimeException("The user doesn't have the required permissions.");

    	if(id!=null && id.getAssetType().equals("contentlet")){
    		for(Contentlet con : APILocator.getContentletAPI().getSiblings(id.getId())){
    			if(!con.getInode().equals(htmlPageInode) && con.isLive())
    				return false;
    		}    		
    		return true;
    	}else{
    		return true;
    	}
	}

	public String getActiveFolderInode(){
		return activeFolderInode;
	}

}
