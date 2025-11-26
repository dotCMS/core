package com.dotmarketing.portlets.browser.ajax;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.ajax.DwrUtil;
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
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper.ACTIVE_FOLDER_ID;
import static com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper.OPEN_FOLDER_IDS;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

/**
 * This class interacts with the DWR Framework in order to provide the {@code Site Browser} portlet with the expected
 * data in the current dotCMS content repository.
 *
 * @author david
 * @since Mar 22nd, 2012
 */
public class BrowserAjax {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private final UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
	private final HostAPI hostAPI = APILocator.getHostAPI();
	private final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private final FolderAPI folderAPI = APILocator.getFolderAPI();
	private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private final LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private final BrowserAPI browserAPI = APILocator.getBrowserAPI();
	private final VersionableAPI versionAPI = APILocator.getVersionableAPI();
	private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	String activeHostId = "";
	private static final String ALL_SITES_ID = "allHosts";

    private static final String SELECTED_BROWSER_PATH_OBJECT = "SELECTED_BROWSER_PATH_OBJECT";

    String lastSortBy = "modDate";
    boolean lastSortDirectionDesc = true;
	private final String imageMimetype = "image";

	final static private Comparator<Map> nameComparator = new Comparator<Map>() {
		public int compare(Map o1, Map o2) {
			return o1.get("name").toString().compareTo(o2.get("name").toString());
		}
	};


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
	 * Loads the entire folder tree associated to the specified Site ID. Additionally, if one or more folders were
	 * expanded in the current session and/or a specific folder was selected, the returned tree will keep track of
	 * those events and will reflect them in the result.
	 *
	 * @param siteId The ID of the Site whose folder tree will be loaded. If such an ID is invalid or doesn't exist,
	 *               the currently selected Site in the UI will be used instead. And if that's not valid either, then
	 *               all Sites are looked up and returned instead.
	 *
	 * @return The entire Site's folder tree.
	 *
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 * @throws DotSecurityException The currently logged-in user does not have the required permissions to perform
	 *                              this action.
	 * @throws SystemException      An error occurred when retrieving the currently logged-in User.
	 * @throws PortalException      An error occurred when retrieving the currently logged-in User.
	 */
    public List<Map> getTree(String siteId) throws DotDataException, DotSecurityException, SystemException, PortalException {
		final User user = DwrUtil.getLoggedInUser();
        siteId = UtilMethods.isSet(siteId) ? siteId : this.getCurrentHost();
        final Role[] roles = DwrUtil.getUserRoles(user);
		final List<Host> siteList = !UtilMethods.isSet(siteId) || siteId.equals(ALL_SITES_ID)
										 ? this.hostAPI.findAllFromCache(user, false)
										 : List.of(this.hostAPI.find(siteId, user, false));

        final List<Map> folderTree = new ArrayList<>();
        for (final Host site : siteList) {
        	// Exclude System Host ad archived Sites from the final result
			if (site.isSystemHost() || site.isArchived()) {
				continue;
			}
            final Map<String,Object> siteDataMap = site.getMap();
			siteDataMap.put("open", false);
            if (this.activeHostId.equalsIgnoreCase(site.getIdentifier()) || siteList.size() == 1) {
                siteDataMap.put("open", true);
                final List<Map> children = this.getFoldersTree(site, roles);
                siteDataMap.put("childrenFolders", children);
                siteDataMap.put("childrenFoldersCount", children.size());
            }
            final Optional<List<Integer>> permissionsOpt = DwrUtil.getPermissions(site, roles, user);
            siteDataMap.put("permissions", permissionsOpt.isPresent() ? permissionsOpt.get() : List.of());
            folderTree.add(siteDataMap);
        }
        return folderTree;
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
        	return new ArrayList<>();
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
	 * Returns the contents of the Folder that has been selected by the User in the Site Browser.
	 *
	 * @param parentId The ID of the Folder whose contents will be returned.
	 *
	 * @return The contents of the Folder that has been selected by the User.
	 *
	 * @throws DotSecurityException The currently logged-in user does not have the required permissions to perform
	 * this action.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
    public List<Map> openFolderTree(final String parentId) throws DotSecurityException, DotDataException {
        final WebContext ctx = WebContextFactory.get();
        final User user = this.getUser(ctx.getHttpServletRequest());
        final Role[] roles = DwrUtil.getUserRoles(user);
        final Folder folder = APILocator.getFolderAPI().find(parentId, user, false);
		this.getOpenFolderIds().add(parentId);
        return this.getFoldersTree(folder, roles);
    }

	/**
	 * Removes the ID of a given open Folder from the list of open Folders in the Site Browser. Every time a User
	 * collapses a parent Folder, it will be removed from the master list in the current session.
	 *
	 * @param parentInode The ID of the Folder to be removed from the list of open Folders.
	 */
    public void closeFolderTree(final String parentInode) {
		final Set<String> openFolderIds = (Set<String>) DwrUtil.getSession().getAttribute(OPEN_FOLDER_IDS);
		if (null != openFolderIds) {
			openFolderIds.remove(parentInode);
		}
	}

	/**
	 * Retrieves the contents living under a specific Folder so that they can be displayed in the UI.
	 *
	 * @param parentId     The ID of the {@link Folder} whose contents will be displayed.
	 * @param sortBy       The order in which folder contents will be returned.
	 * @param showArchived If archived contents must be included in the result set, set to {@code true}.
	 * @param languageId   The Language ID of the contents that will be returned.
	 *
	 * @return The list of folder contents in the form of a Map.
	 *
	 * @throws DotSecurityException The {@link User} calling this operation does not have the required permissions to do
	 *                              so.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
	public List<Map<String, Object>> openFolderContent(final String parentId, final String sortBy,
													   final boolean showArchived, final long languageId) throws
			DotSecurityException, DotDataException {
		DwrUtil.getSession().setAttribute(ACTIVE_FOLDER_ID, parentId);
		if (UtilMethods.isSet(sortBy)) {
			this.lastSortBy = sortBy;
			if (sortBy.equals(lastSortBy)) {
				this.lastSortDirectionDesc = !this.lastSortDirectionDesc;
			}
			this.lastSortBy = sortBy;
		}

		List<Map<String, Object>> listToReturn;
        try {
        	// Only show folders if the parent is not a Site
			Optional<ShortyId> folderShorty = APILocator.getShortyAPI().getShorty(parentId);
        	final boolean showFolders = folderShorty.isPresent() && folderShorty.get().subType == ShortType.FOLDER;
			// By default, return Shorty IDs for folder items
			final boolean showShorties = Boolean.TRUE;
			final Map<String, Object> resultsMap = getFolderContent(parentId, 0, -1, "", null, null, showArchived,
					!showFolders, false, showShorties, this.lastSortBy, this.lastSortDirectionDesc, languageId);
            listToReturn = (List<Map<String, Object>>) resultsMap.get("list");
		} catch (final NotFoundInDbException e){
			Logger.error(this, String.format(
					"Folder with ID '%s' does not exist. Please refresh the screen you opened the Folder from: %s",
					parentId, e.getMessage()), e);
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
		Map<String, Object> selectedBrowserPathObject = new HashMap<>();
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
		List<String> selectedPath = new ArrayList<>();
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

	/**
	 * Returns the contents of a given Site or Folder based on the specified filtering criteria.
	 *
	 * @param folderId     The ID of the {@link Host} or {@link Folder} whose contents will be listed.
	 * @param offset       The offset value for the result set, for pagination purposes.
	 * @param maxResults   The maximum number of results that will be returned, for pagination purposes.
	 * @param filter       An optional filtering criterion for narrowing results.
	 * @param mimeTypes    The list of allowed MIME Types that the returned contents must match.
	 * @param extensions   The list of allowed file extensions for the returned contents.
	 * @param showArchived If archived contents must be returned, set to {@code true}.
	 * @param noFolders    If no folders must be returned, set to {@code true}.
	 * @param onlyFiles    If only Files must be returned, set to {@code true}.
	 * @param sortBy       The sort criterion for the result set.
	 * @param sortByDesc   If the sorting must be performed in descending order, set to {@code true}.
	 * @param languageId   The ID of the language for the contents being returned.
	 *
	 * @return The filtered list of contents under the specified Site or Folder.
	 *
	 * @throws DotSecurityException The {@link User} calling this operation does not have the required permissions to do
	 *                              so.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
	public Map<String, Object> getFolderContent(final String folderId, final int offset, final int maxResults,
												final String filter, final List<String> mimeTypes,
												final List<String> extensions, final boolean showArchived,
												final boolean noFolders, final boolean onlyFiles,
												final boolean showShorties, final String sortBy,
												final boolean sortByDesc, final long languageId) throws
			DotSecurityException, DotDataException {

		final WebContext ctx = WebContextFactory.get();
		final HttpServletRequest req = ctx.getHttpServletRequest();
		final User user = getUser(req);
		req.getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(languageId));
		final boolean showPages =! onlyFiles;
		final BrowserQuery browserQuery = BrowserQuery.builder()
				.showDotAssets(Boolean.FALSE)
				.showLinks(Boolean.TRUE)
				.showExtensions(extensions)
				.withFilter(filter)
				.withHostOrFolderId(folderId)
				.withLanguageId(languageId)
				.offset(offset)
				.showFiles(true)
				.showPages(showPages)
				.showFolders(!noFolders)
				.showArchived(showArchived)
				.showWorking(Boolean.TRUE)
				.showMimeTypes(mimeTypes)
				.maxResults(maxResults)
				.sortBy(sortBy)
				.sortByDesc(sortByDesc)
				.withUser(user)
				.showShorties(showShorties).build();
		return this.browserAPI.getFolderContent(browserQuery);
	}

	public Map<String, Object> getFolderContent (String folderId, int offset, int maxResults, String filter, List<String> mimeTypes,
			List<String> extensions, boolean showArchived, boolean noFolders, boolean onlyFiles, String sortBy, boolean sortByDesc, long languageId) throws DotHibernateException, DotSecurityException, DotDataException {

		WebContext ctx = WebContextFactory.get();
		HttpServletRequest req = ctx.getHttpServletRequest();
		User usr = getUser(req);
		
		req.getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(languageId));

		return browserAPI.getFolderContent(usr, folderId, offset, maxResults, filter, mimeTypes, extensions, showArchived, noFolders, onlyFiles, sortBy, sortByDesc, languageId);
	}

	   public Map<String, Object> getFolderContentWithDotAssets(final String folderId, final int offset,
																final int maxResults, final String filter, final List<String> mimeTypes,
																final List<String> extensions, final boolean showArchived, final boolean noFolders,
																final boolean onlyFiles, final String sortBy, final boolean sortByDesc,
																final boolean excludeLinks, final boolean dotAssets) throws DotSecurityException, DotDataException {

	        final WebContext webContext  = WebContextFactory.get();
	        final HttpServletRequest req = webContext.getHttpServletRequest();
	        final User user              = getUser(req);
		    final long language   = req.getSession().getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE) != null ?
				   Long.parseLong(req.getSession().getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE).toString()) :
				   APILocator.getLanguageAPI().getDefaultLanguage().getId();

		   final Map<String, Object> results = browserAPI.getFolderContent(
				   BrowserQuery.builder()
						   .withUser(user)
						   .withHostOrFolderId(folderId)
						   .offset(offset)
						   .maxResults(maxResults)
						   .withFilter(filter)
						   .showMimeTypes(mimeTypes)
						   .showImages(mimeTypes.contains(imageMimetype))
						   .showExtensions(extensions)
						   .showWorking(true)
						   .showArchived(showArchived)
						   .showFolders(!noFolders)
						   .showFiles(true)
						   .showPages(!onlyFiles)
						   .sortBy(sortBy)
						   .sortByDesc(sortByDesc)
						   .showLinks(!excludeLinks)
						   .withLanguageId(language)
						   .showDefaultLangItems(true)
						   .showDotAssets(dotAssets)
						   .build());

		   listCleanup((List<Map<String, Object>>) results.get("list"), language);


	        return results;
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
		    
			if(Try.of(()->(boolean) content.get("isContentlet")).getOrElse(false)) {
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
		List<Integer> itemsToRemove = new ArrayList<>();
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
			String wfPublishTime, String wfExpireDate, String wfExpireTime, String wfNeverExpire, String whereToSend, String forcePush, String pathToMove) throws  DotSecurityException, ServletException{
		WebContext ctx = WebContextFactory.get();
        User user = getUser(ctx.getHttpServletRequest());
		Contentlet contentlet = null;
		Map<String, Object> result = new HashMap<>();
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
			if (UtilMethods.isSet(pathToMove)) {
				contentlet.setProperty(Contentlet.PATH_TO_MOVE, pathToMove);
			}
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

	/**
	 * Returns information related to a specific File in the dotCMS repository for a given
	 * language. If an invalid Language ID is passed down, the default Language ID will be used
	 * instead. The requested File must belong to one of three types:
	 * <ol>
	 *     <li>File Asset.</li>
	 *     <li>HTML Page.</li>
	 *     <li>dotAsset.</li>
	 * </ol>
	 *
	 * @param fileId     The Identifier of the File.
	 * @param languageId The language associated to the File.
	 *
	 * @return A Map containing brief information related to the File.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The currently logged-in user does not have the required
	 *                              permissions to perform this action.
	 * @throws PortalException      An error occurred when retrieving the currently logged-in user.
	 * @throws SystemException      An error occurred when retrieving the currently logged-in user.
	 */
	public Map<String, Object> getFileInfo(final String fileId, long languageId) throws DotDataException, DotSecurityException, PortalException, SystemException {
		final User user = DwrUtil.getLoggedInUser();
		final boolean respectFrontendRoles = user.isFrontendUser();
        final Identifier ident = this.identifierAPI.find(fileId);

		if(languageId==0) {
			languageId = languageAPI.getDefaultLanguage().getId();
		}

		if (ident != null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals(Identifier.ASSET_TYPE_CONTENTLET)) {
		    Optional<ContentletVersionInfo> vinfo=versionAPI.getContentletVersionInfo(ident.getId(), languageId);

			if(vinfo.isEmpty() && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
				languageId = languageAPI.getDefaultLanguage().getId();
				vinfo=versionAPI.getContentletVersionInfo(ident.getId(), languageId);
			}

			if(vinfo.isEmpty()) {
				throw new DotDataException("Can't find ContentletVersionInfo. Identifier: "
						+ ident.getId() + ". Lang: " + languageId);
			}

		    boolean live = respectFrontendRoles || vinfo.get().getLiveInode()!=null;
			final Contentlet cont = this.contentletAPI.findContentletByIdentifier(ident.getId(),live, languageId , user, respectFrontendRoles);
			if (cont.getContentType().baseType().getType() == BaseContentType.FILEASSET.getType()) {
    			final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(cont);
    			final java.io.File file = fileAsset.getFileAsset();
    			String mimeType = StringPool.BLANK;
				if (null != file) {
					mimeType = DwrUtil.getServletContext().getMimeType(file.getName().toLowerCase());
				} else {
					Logger.warn(this, String.format("Binary File associated to FileAsset with " +
							"Inode '%s' was not found", fileAsset.getInode()));
				}
				final Map<String, Object> fileMap = fileAsset.getMap();
				fileMap.put("mimeType", mimeType);
    			fileMap.put("path", fileAsset.getPath());
    			fileMap.put("type", Identifier.ASSET_TYPE_CONTENTLET);
				fileMap.put("title", cont.getTitle());
    			return fileMap;
			} else if (cont.getContentType().baseType().getType() == BaseContentType.HTMLPAGE.getType()) {
			    final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(cont);
			    final Map<String, Object> pageMap = page.getMap();
			    pageMap.put("mimeType", "application/dotpage");
	            pageMap.put("pageURI", ident.getURI());
	            return pageMap;
			} else if (cont.getContentType().baseType().getType() == BaseContentType.DOTASSET.getType()) {
				final java.io.File file = Try.of(()->cont.getBinary(DotAssetContentType.ASSET_FIELD_VAR)).getOrNull();
				if (null != file) {
					final String fileName = file.getName();
					final String mimeType = DwrUtil.getServletContext().getMimeType(fileName.toLowerCase());
					final Map<String, Object> fileMap = cont.getMap();
					fileMap.put("mimeType", mimeType);
					fileMap.put("path",          "/dA/" + cont.getIdentifier() + StringPool.SLASH);
					fileMap.put("type", "dotasset");
					fileMap.put("name",     fileName);
					fileMap.put("fileName", fileName);
					fileMap.put("title", cont.getTitle());
					return fileMap;
				} else {
					Logger.warn(this, String.format("Binary File associated to dotAsset with Inode" +
							" '%s' was not found", cont.getInode()));
				}
			}
		}
		return null;
	}

	/**
	 * Returns the list of all sub-folders under the specified Site.
	 *
	 * @param site  The Site to get the sub-folders from.
	 * @param roles The list of Roles to filter the sub-folders by.
	 *
	 * @return The list of all sub-folders under the specified Site.
	 *
	 * @throws DotStateException    The Site object is null.
	 * @throws DotDataException     An error occurred when retrieving the sub-folders.
	 * @throws DotSecurityException An internal User permission error has occurred.
	 */
    @SuppressWarnings("unchecked")
	private List<Map> getFoldersTree(final Host site, final Role[] roles) throws DotStateException, DotDataException, DotSecurityException {
		if (null == site || UtilMethods.isNotSet(site.getIdentifier())) {
			throw new DotStateException("Site object cannot be null");
		}
		List<Folder> subFolders = new ArrayList<>();
		try {
			subFolders = this.folderAPI.findSubFolders(site, this.userAPI.getSystemUser(), false);
		} catch (final Exception e) {
			Logger.error(this, String.format("Failed to get sub-folders for Site '%s' [%s]: %s", site,
					site.getIdentifier(), e.getMessage()), e);
		}
		return this.getFoldersTree(site.getIdentifier(), subFolders, roles);
    }

    @SuppressWarnings("unchecked")
	private List<Map> getFoldersTree (Folder parent, Role[] roles) throws DotStateException, DotDataException, DotSecurityException {
        FolderAPI folderAPI = APILocator.getFolderAPI();
        List<Folder> children = new ArrayList<>();
		try {
			children = folderAPI.findSubFolders(parent,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this, "Could not load subfolders for folder with ID: " + parent.getIdentifier(),e);
		}
        return getFoldersTree(parent.getIdentifier(), children, roles);
    }

	/**
	 * Returns the list of all sub-folders under the specified parent permissionable, which can be either a Site or a
	 * Folder. In case one or more of the sub-folders is marked as open from the UI, this method will be called
	 * recursively in order to display them accordingly.
	 *
	 * @param parentId The Identifier/Inode of the parent permissionable.
	 * @param childFolders The list of sub-folders associated to the specified parent ID.
	 * @param roles    The list of Roles associated to the User that is calling this method.
	 *
	 * @return The list of all sub-folders under the specified parent permissionable, with the appropriate open/closed
	 * status.
	 *
	 * @throws DotDataException     An error occurred when retrieving Folder data.
	 * @throws DotSecurityException An internal User permission problem has occurred.
	 */
	private List<Map> getFoldersTree(final String parentId, final List<Folder> childFolders, final Role[] roles) throws DotDataException, DotSecurityException {
        final WebContext ctx = WebContextFactory.get();
        final User user = this.getUser(ctx.getHttpServletRequest());
        final List<Map> folders = new ArrayList<>();
		final Set<String> openFolderIds = this.getOpenFolderIds();
        for (final Folder folder : childFolders) {
        	final Map<String, Object> folderDataMap = folder.getMap();
			folderDataMap.put("open", false);
			// For backwards compatibility, we need to check the folder-selected status using both folder Inode and
			// Identifier
			if (openFolderIds.contains(folder.getIdentifier()) || openFolderIds.contains(folder.getInode())) {
        		final List<Map> childrenMaps = this.getFoldersTree(folder, roles);
        		folderDataMap.put("open", true);
        		folderDataMap.put("childrenFolders", childrenMaps);
        	}
			// For backwards compatibility, we need to check the folder-selected status using both folder Inode and
			// Identifier
			folderDataMap.put("selected",
					folder.getIdentifier().equalsIgnoreCase(this.getActiveFolderId()) || folder.getInode().equalsIgnoreCase(this.getActiveFolderId()));
        	folderDataMap.put("parent", parentId);
			final Optional<List<Integer>> permissionsOpt = DwrUtil.getPermissions(folder, roles, user);
			folderDataMap.put("permissions", permissionsOpt.isPresent() ? permissionsOpt.get() : List.of());
        	folders.add(folderDataMap);
        }
		return folders.stream().sorted(nameComparator).collect(Collectors.toList());
    }

    public Map<String, Object> renameFolder (String inode, String newName) throws DotDataException, DotSecurityException {
    	WebContext ctx = WebContextFactory.get();
        User usr = getUser(ctx.getHttpServletRequest());
    	HashMap<String, Object> result = new HashMap<> ();
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
					+ newFolderId + ". Error: " + e.getMessage());
            return e.getLocalizedMessage();
        }

        return successString;
    }

    public Map<String, Object> renameFile (String inode, String newName) throws Exception {

    	HashMap<String, Object> result = new HashMap<> ();

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

        Map<String, Object> result = new HashMap<>();

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

		HashMap<String, Object> result = new HashMap<>();
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

        Map<String, Object> result = new HashMap<>();

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

	@CloseDBIfOpened
    public Map<String, Object> renameLink (String inode, String newName) throws Exception {

    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(req);
        } catch (Exception e) {
            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
        }

    	HashMap<String, Object> result = new HashMap<> ();
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
      HashMap<String, Object> result = new HashMap<> ();
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
	@CloseDBIfOpened
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
	@CloseDBIfOpened
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

	@CloseDBIfOpened
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

	@CloseDBIfOpened
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

	@CloseDBIfOpened
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

	@CloseDBIfOpened
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

	@CloseDBIfOpened
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
        Map<String, Object> result = new HashMap<>();
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

	@CloseDBIfOpened
	public Map<String, Object> changeAssetMenuOrder (final String inode, final int newValue) throws ActionException, DotDataException {
    	HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        User user = null;
        try {
        	user = com.liferay.portal.util.PortalUtil.getUser(req);
        } catch (Exception e) {
            Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
            throw new DotRuntimeException ("Error trying to obtain the current liferay user from the request.");
        }

    	final Map<String, Object> result = new HashMap<> ();
		Folder folder = null;
		try {
			folder = APILocator.getFolderAPI().find(inode, user, false);
		} catch (DotSecurityException e) {
			Logger.error(this, "Error trying to get info for folder with inode: " + inode, e);
			throw new DotRuntimeException ("Error changing asset menu order.");
		}

		if (null != folder) {
    		result.put("lastValue", folder.getSortOrder());
    		WebAssetFactory.changeAssetMenuOrder (folder, newValue, user);
    	} else {
			Inode asset = InodeFactory.getInode(inode, Inode.class);
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

        List<Map<String, Object>> toReturn = new ArrayList<>();

        List<Folder> children = new ArrayList<>();
		try {
			children = folderAPI.findSubFolders(parent,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(BrowserAjax.class,e.getMessage(),e);
		}

        for (final Folder folder : children) {
            Map<String, Object> folderMap = new HashMap<>();
            folderMap.put("type", "folder");
            folderMap.put("name", folder.getName());
            folderMap.put("id", folder.getIdentifier());
            String fullPath = currentFullPath + "/" + folder.getName();
            String absolutePath = currentAbsolutePath + "/" + folder.getName();
            folderMap.put("fullPath", fullPath);
            folderMap.put("absolutePath", absolutePath);
            List<Map<String, Object>> childrenMaps = getFolderMinInfoTree (folder, roles, fullPath, absolutePath);
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
        List<Host> hosts = new ArrayList<>();

        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}

		List<Map<String,Object>> toReturn = new ArrayList<>();
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
				Map<String,Object> hostMap = new HashMap<>();
				hostMap.put("type", "host");
				hostMap.put("name", host.getHostname());
				hostMap.put("id", host.getIdentifier());
				hostMap.put("fullPath", currentPath);
				hostMap.put("absolutePath", currentPath);
				List<Map<String, Object>> children = new ArrayList<>();

				List<Folder> subFolders = folderAPI.findSubFolders(host,user,false);
				for (final Folder folder : subFolders) {

						List permissions = new ArrayList();
						try {
							permissions = permissionAPI.getPermissionIdsFromRoles(folder, roles, user);
						} catch (DotDataException e) {
							Logger.error(this, "Could not load permissions for folder with ID: " + folder.getIdentifier(),e);
						}
						if(permissions.contains(PERMISSION_READ)){
							Map<String, Object> folderMap = new HashMap<>();
							folderMap.put("type", "folder");
							folderMap.put("name", folder.getName());
							folderMap.put("id", folder.getIdentifier());
							String fullPath = currentPath + ":/" + folder.getName();
							String absolutePath = "/" + folder.getName();
							folderMap.put("fullPath", fullPath);
							folderMap.put("absolutePath", absolutePath);
							List<Map<String, Object>> childrenMaps = getFolderMinInfoTree(folder, roles, fullPath, absolutePath);
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
        Map<String,Object> hostMap = new HashMap<>();
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

	/**
	 * Generates a data map with specific properties from the specified Folder.
	 *
	 * @param folder The {@link Folder} being retrieved.
	 *
	 * @return The data map with the folder's properties.
	 *
	 * @throws DotDataException     An error occurred when retrieving information from the data source.
	 * @throws DotSecurityException The logged-in User does not have the required permissions to access the
	 * 								sub-folders.
	 */
	private Map<String, Object> folderMap(final Folder folder) throws DotDataException, DotSecurityException {
		if (null == folder) {
			return null;
		}
		final String currentPath = this.hostAPI.findParentHost(folder, WebAPILocator.getUserWebAPI().getSystemUser(),
				false).getHostname();
		final String fullPath = currentPath + ":/" + folder.getName();
		final String absolutePath = "/" + folder.getName();
		final Map<String, Object> folderMap = new HashMap<>();

		folderMap.put("type", "folder");
		folderMap.put("name", folder.getName());
		folderMap.put("id", folder.getIdentifier());
		folderMap.put("inode", folder.getInode());
		folderMap.put("defaultFileType", folder.getDefaultFileType());
		folderMap.put("fullPath", fullPath);
		folderMap.put("absolutePath", absolutePath);
		folderMap.put("folderPath", folder.getPath());

		return folderMap;
	}

	public List<Map<String, Object>> getHosts() throws PortalException, SystemException, DotDataException, DotSecurityException {
    	UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
    	WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		Logger.info(this,"currentLoggedUser: " + user.getFullName()+" - id"+user.getUserId());
        Role[] roles = new Role[]{};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class,e1.getMessage(),e1);
		}
        boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend(ctx.getHttpServletRequest());
		HostAPI hostAPI = APILocator.getHostAPI();
		List<Host> hosts = hostAPI.findAll(user, respectFrontendRoles);
		// Remove invalid hosts, before sorting the list
		hosts.removeIf(host -> Objects.isNull(host) || Strings.isNullOrEmpty(host.getHostname()));
		List<Map<String, Object>> hostsToReturn = new ArrayList<>(hosts.size());
		Collections.sort(hosts, new HostNameComparator());
		for (Host h: hosts) {
			/**
			 * When we created the provided host list, we already validated the user's permission over the system host.
			 * Therefore, there is no need to validate it again.
			 **/
			if (h.isSystemHost()){
				hostsToReturn.add(hostMap(h));
				continue;
			}
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
		List<Map<String, Object>> hostsToReturn = new ArrayList<>(hosts.size());
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

		List<Map<String, Object>> hostsToReturn = new ArrayList<>(hosts.size());
		List<Host> filteredHosts = new ArrayList<>();

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
		List<Map<String, Object>> hostsToReturn = new ArrayList<>(hosts.size());
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
			allHosts.setIdentifier(ALL_SITES_ID);
			hostsToReturn.add(hostMap(allHosts));
		}

		return hostsToReturn;
	}


	public List<Map<String, Object>> getHostSubfolders(String hostId) throws PortalException, SystemException, DotDataException, DotSecurityException {
		if(hostId.equals(ALL_SITES_ID)){
			return  new ArrayList<>();
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
		List<Map<String, Object>> foldersToReturn = new ArrayList<>(folders.size());
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
		List<Map<String, Object>> foldersToReturn = new ArrayList<>(folders.size());
		for (Folder f: folders){
			if(UtilMethods.isSet(requiredPermissions)){
				if(permissionAPI.doesUserHavePermissions(f,requiredPermissions, user)){
					foldersToReturn.add(folderMap(f));
				}
			}
		}
		return foldersToReturn;
	}

	/**
	 * Returns a list with the sub-folders associated to the specified parent Folder.
	 *
	 * @param parentFolderId The ID of the Folder whose sub-folders must be returned.
	 *
	 * @return A list with the data map of all the sub-folders under the specified parent Folder.
	 *
	 * @throws DotDataException     An error occurred when retrieving information from the data source.
	 * @throws DotSecurityException The logged-in User does not have the required permissions to access the
	 * 								sub-folders.
	 */
	@CloseDBIfOpened
	public List<Map<String, Object>> getFolderSubfolders(final String parentFolderId) throws DotDataException, DotSecurityException {
		final User user = this.getLoggedInUser();
		Logger.info(this, "subFoldersUSer" + user.getUserId());
		final Role[] roles = DwrUtil.getUserRoles(user);
		final Folder parentFolder = this.folderAPI.find(parentFolderId, user, false);
		final List<Folder> subFolders = this.folderAPI.findSubFolders(parentFolder, user, false);
		final List<Map<String, Object>> foldersToReturn = new ArrayList<>(subFolders.size());
		subFolders.forEach(folder -> {

			try {
				final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(folder, roles, user);
				if (permissions.contains(PERMISSION_READ)) {
					foldersToReturn.add(this.folderMap(folder));
				}
			} catch (final DotDataException | DotSecurityException e) {
				Logger.error(this, String.format("Could not get permissions from folder '%s': %s", folder.getPath(),
						e.getMessage()), e);
			}

		});
		return foldersToReturn;
	}

	public List<Map<String, Object>> getFolderSubfoldersByPermissions(String parentFolderId, String requiredPermissions) throws PortalException, SystemException, DotDataException, DotSecurityException {
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
        User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		FolderAPI folderAPI = APILocator.getFolderAPI();
		Folder parentFolder = folderAPI.find(parentFolderId,user,false);
		List<Folder> folders = folderAPI.findSubFolders(parentFolder,user,false);
		List<Map<String, Object>> foldersToReturn = new ArrayList<>(folders.size());
		for (Folder f: folders) {
			if(UtilMethods.isSet(requiredPermissions)){
				if(permissionAPI.doesUserHavePermissions(f,requiredPermissions, user)){
					foldersToReturn.add(folderMap(f));
				}
			}
		}
		return foldersToReturn;
	}

	/**
	 * Returns a Map with the information associated to the specified ID. Such an ID may belong to either a Site or a
	 * Folder.
	 *
	 * @param hostFolderId The ID of a Site or a Folder.
	 *
	 * @return A Map with the properties belonging to the specified Site or Folder.
	 */
	@CloseDBIfOpened
	public Map<String, Object> findHostFolder(final String hostFolderId) {
		if (!InodeUtils.isSet(hostFolderId)) {
			return null;
		}
		try {
			final User user = this.getLoggedInUser();
			boolean respectFrontendRoles = this.isFrontEndLogin();
			Host site = this.hostAPI.find(hostFolderId, user, respectFrontendRoles);
			if (site != null) {
				return this.hostMap(site);
			}
			site = this.hostAPI.findByName(hostFolderId, user, respectFrontendRoles);
			if (site != null) {
				return this.hostMap(site);
			}
			final Folder folder = this.folderAPI.find(hostFolderId, user, false);
			if (folder != null) {
				return this.folderMap(folder);
			}
		} catch (final Exception e) {
			Logger.warn(this, String.format("Could not retrieve Site/Folder '%s': %s", hostFolderId, e.getMessage()));
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
					Map<String, Object> folderMap = new HashMap<>();
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
		if(hostId.equals(ALL_SITES_ID)){
			return  new ArrayList<>();
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
		List<Map<String, Object>> foldersToReturn = new ArrayList<>(folders.size());
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
		Map<String, Object> result = new HashMap<>();
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

	/**
	 * Returns the ID of the currently active folder.
	 *
	 * @return The ID of the active folder.
	 */
	public String getActiveFolderId() {
		return (String) DwrUtil.getSession().getAttribute(ACTIVE_FOLDER_ID);
	}

	/**
	 * Returns the set of IDs of Folders that are currently expanded in the Site Browser's UI.
	 *
	 * @return The set of expanded folder IDs.
	 */
	@SuppressWarnings("unchecked")
	private Set<String> getOpenFolderIds() {
		Set<String> openFolderIds = (Set<String>) DwrUtil.getSession().getAttribute(OPEN_FOLDER_IDS);
		if (null == openFolderIds) {
			openFolderIds = new HashSet<>();
			DwrUtil.getSession().setAttribute(OPEN_FOLDER_IDS, openFolderIds);
		}
		return openFolderIds;
	}

	/**
	 * Utility method that returns the currently logged-in User.
	 *
	 * @return The logged-in {@link User}.
	 */
	private User getLoggedInUser() {
		return this.userAPI.getLoggedInUser(WebContextFactory.get().getHttpServletRequest());
	}

	/**
	 * Verifies whether the currently logged-in User is logged into the front-end or not.
	 *
	 * @return If the {@link User} is logged into the front-end, returns {@code true}.
	 *
	 * @throws SystemException An error occurred when performing this check.
	 * @throws PortalException An error occurred when performing this check.
	 */
	private boolean isFrontEndLogin() throws SystemException, PortalException {
		return this.userAPI.isLoggedToFrontend(WebContextFactory.get().getHttpServletRequest());
	}

}
