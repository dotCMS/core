package com.dotmarketing.portlets.htmlpages.business;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.dotcms.enterprise.cmis.QueryResult;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotRequestProxy;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.filters.ClickstreamFilter;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class HTMLPageAPIImpl extends BaseWebAssetAPI implements HTMLPageAPI {

	static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	static HTMLPageFactory htmlPageFactory = FactoryLocator.getHTMLPageFactory();
	static HostAPI hostAPI = APILocator.getHostAPI();
	static TemplateAPI templateAPI = APILocator.getTemplateAPI();
	static IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	static ContentletAPI contentletAPI = APILocator.getContentletAPI();

	/**
	 * Will copy the HTMLPage set on the HTMLPageAPI to the passed in folder.
	 * Currently this method will copy permissions but will not bring the
	 * content associated with HTMLPage being copied.
	 *
	 * @param folderToCopyTo
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public HTMLPage copy(HTMLPage htmlPage, Folder destination, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return copy(htmlPage, destination, true, false, HTMLPageAPI.CopyMode.BLANK_HTMLPAGE, user, respectFrontendRoles);
	}


	public HTMLPage copy(HTMLPage source, Folder destination, boolean forceOverwrite, boolean copyTemplateContainers,
			HTMLPageAPI.CopyMode copyMode, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		Template sourceTemplate = getHTMLPageTemplate(source);
		Template template;

		if (copyTemplateContainers) {
			Host hostDestination = hostAPI.find(destination.getHostId(), user, respectFrontendRoles);
			template = templateAPI.copy(sourceTemplate, hostDestination, false, true, user, respectFrontendRoles);
		} else {
			template = sourceTemplate;
		}

		TemplateAPI templateAPI = APILocator.getTemplateAPI();

        List<Container> sourceContainers = templateAPI.getContainersInTemplate(sourceTemplate, user, respectFrontendRoles);
        List<Container> copyContainers = templateAPI.getContainersInTemplate(template, user, respectFrontendRoles);

        List<ContainerRemapTuple> containersRemap = new LinkedList<ContainerRemapTuple>();
        for (int i = 0; i < sourceContainers.size(); i++) {
			Container sourceContainer = sourceContainers.get(i);
			Container destinationContainer = copyContainers.get(i);
        	containersRemap.add(new ContainerRemapTuple(sourceContainer, destinationContainer));
        }
        TemplateContainersReMap remap = new TemplateContainersReMap(sourceTemplate, template, containersRemap);

		return copy(source, destination, forceOverwrite, copyMode, remap, user, respectFrontendRoles);
	}

	public HTMLPage copy(HTMLPage source, Folder destination, boolean forceOverwrite, CopyMode copyMode,
			TemplateContainersReMap reMapping, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		boolean isNew = false;
		HTMLPage newHTMLPage;
		if (forceOverwrite) {
			newHTMLPage = getWorkingHTMLPageByPageURL(source.getPageUrl(), destination);
			if (newHTMLPage == null) {
				isNew = true;
			}
		} else {
			isNew = true;
		}

		newHTMLPage = new HTMLPage();
		newHTMLPage.copy(source);

		// gets page url before extension
		String pageURL = UtilMethods.getFileName(source.getPageUrl());

		// gets file extension
		String fileExtension = UtilMethods.getFileExtension(source.getPageUrl());

		if (!forceOverwrite) {
			newHTMLPage.setPageUrl(getCopyHTMLPageName(pageURL, fileExtension, destination));

			if (!UtilMethods.getFileName(newHTMLPage.getPageUrl()).equals(pageURL))
				newHTMLPage.setFriendlyName(source.getFriendlyName() + " (COPY)");
		}

		Template destinationTemplate = reMapping.getDestinationTemplate();

		List<MultiTree> associatedSourceContentlets = null;

		//Checking if contentlets just need to be remapped or need to be copied on destination
		if (copyMode == HTMLPageAPI.CopyMode.USE_SOURCE_CONTENT) {
			associatedSourceContentlets = getHTMLPageMultiTree(source);
		} else if (copyMode == HTMLPageAPI.CopyMode.COPY_SOURCE_CONTENT) {
			associatedSourceContentlets = getHTMLPageMultiTree(source);

			Contentlet contentlet;
			FolderAPI folderAPI = APILocator.getFolderAPI();
			Host systemHost = hostAPI.findSystemHost(user, respectFrontendRoles);
			Folder systemFolder = folderAPI.findSystemFolder();
			Host destinationHost = hostAPI.find(destination.getHostId(), user, respectFrontendRoles);

			for (MultiTree multiTree : associatedSourceContentlets) {
				contentlet = contentletAPI.findContentletByIdentifier(multiTree.getChild(), false, 0, user,
						respectFrontendRoles);

				Host contentletHost = null;
				if(!UtilMethods.isSet(contentlet.getHost()) && !contentlet.getHost().equals(systemHost.getInode())) {
					contentletHost = hostAPI.find(contentlet.getHost(), user, respectFrontendRoles);
				}
				Folder contentletFolder = null;
				if(!UtilMethods.isSet(contentlet.getFolder()) && !contentlet.getFolder().equals(systemFolder.getInode())) {
					contentletFolder = folderAPI.find(contentlet.getFolder(),user,false);
				}

				if (contentletFolder != null) {
					Folder contentletDestFolder = folderAPI.createFolders(APILocator.getIdentifierAPI().find(contentletFolder).getPath(), destinationHost,user,false);
					contentlet = contentletAPI.copyContentlet(contentlet, contentletDestFolder, user, respectFrontendRoles);
				} else if (contentletHost != null) {
					contentlet = contentletAPI.copyContentlet(contentlet, destinationHost, user, respectFrontendRoles);
				} else {
					contentlet = contentletAPI.copyContentlet(contentlet, user, respectFrontendRoles);
				}

				multiTree.setChild(contentlet.getIdentifier());
			}
		}

		//Creating the new pages associations mapping with containers
		List<MultiTree> newContentletAssociation = new LinkedList<MultiTree>();
		if (copyMode != HTMLPageAPI.CopyMode.BLANK_HTMLPAGE) {
			for (MultiTree multiTree : associatedSourceContentlets) {
				String sourceContainerId = multiTree.getParent2();
				String destinationContainerId = null;
				for (int i = 0; i < reMapping.getContainersRemap().size(); i++) {
					ContainerRemapTuple tuple = reMapping.getContainersRemap().get(i);
					if(tuple.getSourceContainer().getIdentifier().equals(sourceContainerId)) {
						destinationContainerId = tuple.getDestinationContainer().getIdentifier();
					}
				}
				if(destinationContainerId != null) {
					newContentletAssociation.add(new MultiTree("", destinationContainerId, multiTree.getChild()));
				}
			}
		}
        newHTMLPage.setTemplateId(destinationTemplate.getIdentifier());
		//Persisting the new page
		if (isNew) {
			// creates new identifier for this webasset and persists it
			Identifier newIdentifier = com.dotmarketing.business.APILocator.getIdentifierAPI().createNew(newHTMLPage, destination);
			
			newHTMLPage.setIdentifier(newIdentifier.getInode());
			
			// persists the webasset
			save(newHTMLPage);
		} else {
			saveHTMLPage(newHTMLPage, destinationTemplate, destination, user, respectFrontendRoles);
		}
		
		if(source.isLive()){
			APILocator.getVersionableAPI().setWorking(newHTMLPage);
			APILocator.getVersionableAPI().setLive(newHTMLPage);
		}
		    

		//Saving the new content mapping
		if (copyMode != HTMLPageAPI.CopyMode.BLANK_HTMLPAGE) {
			MultiTree newMultiTree;
			for (MultiTree multiTree : newContentletAssociation) {
				newMultiTree = new MultiTree(newHTMLPage.getIdentifier(), multiTree.getParent2(), multiTree.getChild());
				MultiTreeFactory.saveMultiTree(newMultiTree);
			}
		}

		// Copy permissions
		permissionAPI.copyPermissions(source, newHTMLPage);

		return newHTMLPage;
	}

	private List<MultiTree> getHTMLPageMultiTree(HTMLPage htmlPage) throws DotDataException {
		return MultiTreeFactory.getMultiTree(identifierAPI.findFromInode(htmlPage.getIdentifier()));
	}

	@SuppressWarnings("unchecked")
	public HTMLPage getWorkingHTMLPageByPageURL(String htmlPageURL, Folder folder) throws DotStateException, DotDataException, DotSecurityException {
		List<HTMLPage> htmlPages = APILocator.getFolderAPI().getWorkingHTMLPages(folder, APILocator.getUserAPI().getSystemUser(),false);
		for(HTMLPage page:htmlPages){
			if(htmlPageURL.equalsIgnoreCase(page.getPageUrl())){
				return page;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected String getCopyHTMLPageName(String htmlPageName, String fileExtension, Folder folder) throws DotStateException, DotDataException, DotSecurityException {
		String result = new String(htmlPageName);

		List<HTMLPage> htmlPages = APILocator.getFolderAPI().getHTMLPages(folder, APILocator.getUserAPI().getSystemUser(), false);

		boolean isValidHTMLPageName = false;
		String temp1, temp2;

		for (; !isValidHTMLPageName;) {
			isValidHTMLPageName = true;
			temp1 = result + "." + fileExtension;

			for (HTMLPage htmlPage : htmlPages) {
				temp2 = UtilMethods.getFileName(htmlPage.getPageUrl()) + "."
						+ UtilMethods.getFileExtension(htmlPage.getPageUrl());
				if (temp2.equals(temp1)) {
					isValidHTMLPageName = false;
					break;
				}
			}

			if (!isValidHTMLPageName)
				result += "_copy";
			else
				result = temp1;
		}

		return result;
	}

	private void save(HTMLPage htmlPage) throws DotDataException, DotStateException, DotSecurityException {
		htmlPageFactory.save(htmlPage);
	}

	protected void save(WebAsset webAsset) throws DotDataException, DotStateException, DotSecurityException {
		save((HTMLPage) webAsset);
	}

	protected static Template getHTMLPageTemplate(HTMLPage page) throws DotDataException, DotSecurityException {
		return APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
	}


	public HTMLPage saveHTMLPage(HTMLPage newHtmlPage, Template template, Folder parentFolder, User user,
			boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		HTMLPage existingHTMLPage = getWorkingHTMLPageByPageURL(newHtmlPage.getPageUrl(), parentFolder);

		boolean pageExists = (existingHTMLPage != null) && InodeUtils.isSet(existingHTMLPage.getInode());

		if (pageExists) {
			if (!permissionAPI.doesUserHavePermission(existingHTMLPage, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}

		if (!permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		if (!permissionAPI.doesUserHavePermission(parentFolder, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		try {
			boolean previousShowMenu = false;

			// parent identifier for this file
			Identifier identifier = null;

			if (pageExists) {
				previousShowMenu = existingHTMLPage.isShowOnMenu();
				identifier = (Identifier) APILocator.getIdentifierAPI().find(existingHTMLPage);
			}

			// Some checks

			// Get asset host based on the parentFolder of the asset
			Host host = hostAPI.findParentHost(parentFolder, user, false);

			// get an identifier based on this new uri
			Identifier testIdentifier = (Identifier) APILocator.getIdentifierAPI().find(host, newHtmlPage.getURI(parentFolder));

			// if this is a new htmlpage and there is already an identifier with
			// this uri, return
			if ((existingHTMLPage != null) && !InodeUtils.isSet(existingHTMLPage.getInode()) && InodeUtils.isSet(testIdentifier.getInode())) {
				existingHTMLPage.setParent(parentFolder.getInode());
				throw new DotDataException("Another page with the same page url exists in this folder");
			}
			// if this is an existing htmlpage and there is already an
			// identifier
			// with this uri, return
			else if (pageExists
					&& (!testIdentifier.getInode().equalsIgnoreCase(identifier.getInode()))
					&& InodeUtils.isSet(testIdentifier.getInode())) {
				// when there is an error saving should unlock working asset
				unLockAsset(existingHTMLPage);
				throw new DotDataException("Another page with the same page url exists in this folder");
			}
			if (template != null) {
				// Adds template children from selected box
				Identifier templateIdentifier = APILocator.getIdentifierAPI().find(template);

				newHtmlPage.setTemplateId(templateIdentifier.getInode());
			}
			// Versioning
			if (pageExists) {
				// Creation the version asset
				createAsset(newHtmlPage, user.getUserId(), parentFolder, identifier, false);
				HibernateUtil.flush();

				LiveCache.removeAssetFromCache(existingHTMLPage);
				newHtmlPage = (HTMLPage) saveAsset(newHtmlPage, identifier, user, false);

				// if we need to update the identifier
				if (InodeUtils.isSet(parentFolder.getInode())
						&& !newHtmlPage.getURI(parentFolder).equals(identifier.getURI())) {

					// assets cache
					LiveCache.removeAssetFromCache(newHtmlPage);
					LiveCache.removeAssetFromCache(existingHTMLPage);
					LiveCache.clearCache(host.getIdentifier());
					WorkingCache.removeAssetFromCache(newHtmlPage);

					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(newHtmlPage);

					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(existingHTMLPage);


					APILocator.getIdentifierAPI().updateIdentifierURI(newHtmlPage, parentFolder);

				}

			} // Creating the new page
			else {
				createAsset(newHtmlPage, user.getUserId(), parentFolder);
			}



			HibernateUtil.flush();
			HibernateUtil.getSession().refresh(newHtmlPage);

			// Refreshing the menues
			if (previousShowMenu != newHtmlPage.isShowOnMenu()) {
				// existing folder with different show on menu ... need to
				// regenerate menu
				//RefreshMenus.deleteMenus();
				RefreshMenus.deleteMenu(newHtmlPage);
			}
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return newHtmlPage;
	}

	/**
	 *
	 * @param path
	 * @param host
	 * @return HTMLPage from a path on a given host
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public HTMLPage loadPageByPath(String path, Host host) throws DotDataException, DotSecurityException {
		return loadPageByPath(path, host.getIdentifier());
	}

	/**
	 *
	 * @param path
	 * @param host
	 * @return HTMLPage from a path on a given hostId
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public HTMLPage loadPageByPath(String path, String hostId) throws DotDataException, DotSecurityException {
		return htmlPageFactory.getLiveHTMLPageByPath(path, hostId);
	}

	/**
	 *
	 * @param page
	 * @param container
	 * @return true/false on whether or not a Page has content with a specificed
	 *         container
	 */
	public boolean hasContent(HTMLPage page, Container container) {
		return htmlPageFactory.findNumOfContent(page, container) > 0 ? true : false;
	}

	/**
	 * Use to method to get the template for the HTMLPage set on the API. This
	 * method will hit the database.
	 *
	 * @return Template for the working version of a HTMLPage
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	 */
	public Template getTemplateForWorkingHTMLPage(HTMLPage htmlpage) throws DotStateException, DotDataException, DotSecurityException {
		return (Template) APILocator.getVersionableAPI().findWorkingVersion(htmlpage.getTemplateId(),APILocator.getUserAPI().getSystemUser(), false);

	}

	/**
	 *
	 * @param folder
	 *            to get HTMLPages for
	 * @return a List of all live HTMLPages
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public List<HTMLPage> findLiveHTMLPages(Folder folder) throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().getLiveHTMLPages(folder, APILocator.getUserAPI().getSystemUser(),false);
	}

	/**
	 *
	 * @param folder
	 *            to get HTMLPages for
	 * @return a List of all live HTMLPages
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	public List<HTMLPage> findWorkingHTMLPages(Folder folder) throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().getWorkingHTMLPages(folder,APILocator.getUserAPI().getSystemUser(),false);
	}

	public Folder getParentFolder(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		return htmlPageFactory.getParentFolder(object);
	}

	public Host getParentHost(HTMLPage object) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		return htmlPageFactory.getParentHost(object);
	}

	public boolean delete(HTMLPage htmlPage, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception {
		if(permissionAPI.doesUserHavePermission(htmlPage, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(htmlPage);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	public List<Map<String, Serializable>> DBSearch(Query query, User user,boolean respectFrontendRoles) throws ValidationException,DotDataException {

		Map<String, String> dbColToObjectAttribute = new HashMap<String, String>();

		if(UtilMethods.isSet(query.getSelectAttributes())){

			if(!query.getSelectAttributes().contains("title")){
				query.getSelectAttributes().add("title" + " as " + QueryResult.CMIS_TITLE);
			}
		}else{
			List<String> atts = new ArrayList<String>();
			atts.add("*");
			atts.add("title" + " as " + QueryResult.CMIS_TITLE);
			query.setSelectAttributes(atts);
		}

		return QueryUtil.DBSearch(query, dbColToObjectAttribute, null, user, true, respectFrontendRoles);
	}

	public String getHTML(HTMLPage htmlPage) throws DotStateException, DotDataException, DotSecurityException {

		return getHTML(htmlPage, true, null);
	}
	public String getHTML(HTMLPage htmlPage, boolean liveMode) throws DotStateException, DotDataException, DotSecurityException {

		return getHTML(htmlPage, liveMode, null);
	}
	public String getHTML(HTMLPage htmlPage, boolean liveMode, String contentId) throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(htmlPage, liveMode, contentId, null);
	}
	
	@Override
	public String getHTML(String uri, Host host,boolean liveMode, String contentId,User user) throws DotStateException, DotDataException, DotSecurityException {
	    return getHTML(uri,host,liveMode,contentId,user,null);
	}
	
	@Override
	public String getHTML(String uri, Host host,boolean liveMode, String contentId,User user, String langId) throws DotStateException, DotDataException, DotSecurityException {
		/*
		 * The below code is copied from VelocityServlet.doLiveMode() and modified to parse a HTMLPage.
		 * Replaced the request and response objects with DotRequestProxy and DotResponseProxyObjects.
		 *
		 * TODO Code clean-up.
		 *
		 * TODO: I don't think it will work - jorge.urdaneta
		 */

		InvocationHandler dotInvocationHandler = new DotInvocationHandler();

		DotRequestProxy requestProxy = (DotRequestProxy) Proxy
				.newProxyInstance(DotRequestProxy.class.getClassLoader(),
						new Class[] { DotRequestProxy.class },
						dotInvocationHandler);

		DotResponseProxy responseProxy = (DotResponseProxy) Proxy
				.newProxyInstance(DotResponseProxy.class.getClassLoader(),
						new Class[] { DotResponseProxy.class },
						dotInvocationHandler);

		StringWriter out = new StringWriter();
		Context context = null;
		

		uri = UtilMethods.cleanURI(uri);

		// Map with all identifier inodes for a given uri.
		String idInode = APILocator.getIdentifierAPI().find(host, uri)
				.getInode();

		// Checking the path is really live using the livecache
		String cachedUri = (liveMode) ? LiveCache.getPathFromCache(uri, host) : WorkingCache.getPathFromCache(uri, host);

		// if we still have nothing.
		if (!InodeUtils.isSet(idInode) || cachedUri == null) {
			throw new ResourceNotFoundException(String.format(
					"Resource %s not found in Live mode!", uri));
		}

		responseProxy.setContentType("text/html");
		requestProxy.setAttribute("idInode", String.valueOf(idInode));

		Logger.debug(HTMLPageAPIImpl.class, "VELOCITY HTML INODE=" + idInode);

		/* Set long lived cookie regardless of who this is */
		String _dotCMSID = UtilMethods.getCookieValue(
				requestProxy.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);


		if (!UtilMethods.isSet(_dotCMSID)) {
			/* create unique generator engine */
			Cookie idCookie = CookieUtil.createCookie();
			responseProxy.addCookie(idCookie);
		}
		
		requestProxy.put("host", host);
		requestProxy.put("host_id", host.getIdentifier());
		requestProxy.put("uri", uri);
		requestProxy.put("user", user);
		if(!liveMode){
			requestProxy.setAttribute(WebKeys.PREVIEW_MODE_SESSION, "true");
		}
		boolean signedIn = false;

		if (user != null) {
			signedIn = true;
		}
		Identifier ident = APILocator.getIdentifierAPI().find(host, uri);


		Logger.debug(HTMLPageAPIImpl.class, "Page Permissions for URI=" + uri);

		HTMLPage pageProxy = new HTMLPage();
		pageProxy.setIdentifier(ident.getInode());

		// Check if the page is visible by a CMS Anonymous role
		try {
			if (!permissionAPI.doesUserHavePermission(pageProxy,
					PermissionAPI.PERMISSION_READ, user, true)) {
				// this page is protected. not anonymous access

				/*******************************************************************
				 * If we need to redirect someone somewhere to login before
				 * seeing a page, we need to edit the /portal/401.jsp page to
				 * sendRedirect the user to the proper login page. We are not
				 * using the REDIRECT_TO_LOGIN variable in the config any
				 * longer.
				 ******************************************************************/
				if (!signedIn) {
					// No need for the below LAST_PATH attribute on the front
					// end http://jira.dotmarketing.net/browse/DOTCMS-2675
					// request.getSession().setAttribute(WebKeys.LAST_PATH,
					// new ObjectValuePair(uri, request.getParameterMap()));
					requestProxy.getSession().setAttribute(
							com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN,
							uri);

					Logger.debug(HTMLPageAPIImpl.class,
							"VELOCITY CHECKING PERMISSION: Page doesn't have anonymous access"
									+ uri);

					Logger.debug(HTMLPageAPIImpl.class, "401 URI = " + uri);

					Logger.debug(HTMLPageAPIImpl.class, "Unauthorized URI = "
							+ uri);
					responseProxy.sendError(401,
							"The requested page/file is unauthorized");
					return "An SYSTEM ERROR OCCURED !";

				} else if (!permissionAPI.getReadRoles(ident).contains(
						APILocator.getRoleAPI().loadLoggedinSiteRole())) {
					// user is logged in need to check user permissions
					Logger.debug(HTMLPageAPIImpl.class,
							"VELOCITY CHECKING PERMISSION: User signed in");

					// check user permissions on this asset
					if (!permissionAPI.doesUserHavePermission(ident,
							PermissionAPI.PERMISSION_READ, user, true)) {
						// the user doesn't have permissions to see this page
						// go to unauthorized page
						Logger
								.warn(HTMLPageAPIImpl.class,
										"VELOCITY CHECKING PERMISSION: Page doesn't have any access for this user");
						responseProxy.sendError(403,
								"The requested page/file is forbidden");
						return "PAGE NOT FOUND!";
					}
				}
			}

			if(UtilMethods.isSet(contentId)){
				requestProxy.setAttribute(WebKeys.WIKI_CONTENTLET, contentId);
			}
			
			if(UtilMethods.isSet(langId)) {
			    requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, langId);
			    requestProxy.getSession().setAttribute(WebKeys.HTMLPAGE_LANGUAGE, langId);
			}
			
			context = VelocityUtil.getWebContext(requestProxy, responseProxy);
			if(! liveMode ){
				context.put("PREVIEW_MODE", new Boolean(true));
			}else{
				context.put("PREVIEW_MODE", new Boolean(false));
			}
			
			if(UtilMethods.isSet(langId)) {
                context.put("language", langId);
            }

			context.put("host", host);
			VelocityEngine ve = VelocityUtil.getEngine();

			Logger.debug(HTMLPageAPIImpl.class, "Got the template!!!!"
					+ idInode);

			requestProxy.setAttribute("velocityContext", context);

			String VELOCITY_HTMLPAGE_EXTENSION = Config
					.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");
			String vTempalate = (liveMode) ?
					"/live/" + idInode + "." + VELOCITY_HTMLPAGE_EXTENSION :
						"/working/" + idInode + "." + VELOCITY_HTMLPAGE_EXTENSION ;

			ve.getTemplate(vTempalate)
					.merge(context, out);

		} catch (PortalException e1) {
			Logger.error(this, e1.getMessage(), e1);
		} catch (SystemException e1) {
			Logger.error(this, e1.getMessage(), e1);
		} catch (DotDataException e1) {
			Logger.error(this, e1.getMessage(), e1);
		} catch (DotSecurityException e1) {
			Logger.error(this, e1.getMessage(), e1);
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (ResourceNotFoundException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (ParseErrorException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (MethodInvocationException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}finally{
			context = null;
			VelocityServlet.velocityCtx.remove();
		}


		if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
			Logger.debug(HTMLPageAPIImpl.class, "Into the ClickstreamFilter");
			// Ensure that clickstream is recorded only once per request.
			if (requestProxy.getAttribute(ClickstreamFilter.FILTER_APPLIED) == null) {
				requestProxy.setAttribute(ClickstreamFilter.FILTER_APPLIED,
						Boolean.TRUE);

				if (user != null) {
					UserProxy userProxy = null;
					try {
						userProxy = com.dotmarketing.business.APILocator
								.getUserProxyAPI()
								.getUserProxy(
										user,
										APILocator.getUserAPI().getSystemUser(),
										false);
					} catch (DotRuntimeException e) {
						e.printStackTrace();
					} catch (DotSecurityException e) {
						e.printStackTrace();
					} catch (DotDataException e) {
						e.printStackTrace();
					}

				}
			}
		}

		return out.toString();
	}
	
	//http://jira.dotmarketing.net/browse/DOTCMS-3392
	public String getHTML(HTMLPage htmlPage, boolean liveMode, String contentId, User user) throws DotStateException, DotDataException, DotSecurityException {
		String uri = htmlPage.getURI();
		Host host = getParentHost(htmlPage);
		return getHTML(uri, host, liveMode, contentId, user);
	}


	public HTMLPage loadWorkingPageById(String pageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		HTMLPage page = htmlPageFactory.loadWorkingPageById(pageId);
		if(page == null)
			return page;
		if(!permissionAPI.doesUserHavePermission(page, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User " + user.getUserId() + "has no permissions to read page id " + pageId);

		return page;
	}

	public HTMLPage loadLivePageById(String pageId, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		HTMLPage page = htmlPageFactory.loadLivePageById(pageId);
		if(page == null)
			return page;
		if(!permissionAPI.doesUserHavePermission(page, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles))
			throw new DotSecurityException("User " + user.getUserId() + "has no permissions to read page id " + pageId);

		return page;
	}


	public List<HTMLPage> findHtmlPages(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,int offset, int limit, String orderBy)
			throws DotSecurityException, DotDataException {
		return FactoryLocator.getHTMLPageFactory().findHtmlPages(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}
	
	public boolean movePage(HTMLPage page, Folder parent, User user,boolean respectFrontendRoles) throws DotStateException,
			DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(page,
				PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		if (!permissionAPI.doesUserHavePermission(parent,
				PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,
				respectFrontendRoles)) {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return htmlPageFactory.movePage(page, parent);

	}
}
