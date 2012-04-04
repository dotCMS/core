package com.dotmarketing.portlets.htmlpages.factories;


import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPageVersionInfo;
import com.dotmarketing.portlets.templates.factories.TemplateFactory;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;

/**
 *
 * @author  will
 */
public class HTMLPageFactory {
	
	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI();

	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}
		
	public static HTMLPage getLiveHTMLPageByPath(String path, Host host) throws DotStateException, DotDataException, DotSecurityException{
	    return getLiveHTMLPageByPath (host, path);
	}
	
	public static int findNumOfContent(HTMLPage page, Container container){
		DotConnect dc = new DotConnect();
		StringBuffer buffy = new StringBuffer();
		buffy.append("select count(t.child) as contentletCount ");
		buffy.append("from multi_tree t ");
		buffy.append("where t.parent1 = ? and t.parent2 = ?");
		dc.setSQL(buffy.toString());
		dc.addParam(page.getInode());
		dc.addParam(container.getInode());
		int count = dc.getInt("contentletCount");
		return count;
	}
	
	public static HTMLPage getLiveHTMLPageByPath(Host host, String path) throws DotStateException, DotDataException, DotSecurityException{

        Identifier id = APILocator.getIdentifierAPI().find(host, path);

        Logger.debug(HTMLPageFactory.class, "Looking for page : " + path);
		Logger.debug(HTMLPageFactory.class, "got id " + id.getInode());
        
        //if this page does not exist, create it, add it to the course folder, use the course template, etc...
        if(!InodeUtils.isSet(id.getInode())){
            return  new HTMLPage();
        }
        
	    return (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(), false);
	    
	}

	public static HTMLPage getLiveHTMLPageByIdentifier(Identifier ident) throws DotStateException, DotDataException, DotSecurityException{
	    return (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(ident, APILocator.getUserAPI().getSystemUser(), false);
	    
	}
	
	@SuppressWarnings("unchecked")
	public static java.util.List<HTMLPage> getLiveHTMLPages() {
		HibernateUtil dh = new HibernateUtil(HTMLPage.class);
		List<HTMLPage> list =null;
		try {
		    dh.setQuery(
		            "from inode in class com.dotmarketing.portlets.htmlpages.model.HTMLPage html, "+HTMLPageVersionInfo.class.getName()+" vv " +
		            "where type='htmlpage' and identifier=vv.identifier and vv.live_inode=inode and vv.deleted="+DbConnectionFactory.getDBFalse());
		    list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);	
		}
		return list;
	}

	

	public static Template getHTMLPageTemplate(HTMLPage htmlpage) throws DotStateException, DotDataException, DotSecurityException{
		return APILocator.getTemplateAPI().findWorkingTemplate(htmlpage.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
	}

	public static Template getHTMLPageTemplate(HTMLPage page, boolean previewMode) throws DotStateException, DotDataException, DotSecurityException {
		if (previewMode) 
		    return APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
		else
		    return APILocator.getTemplateAPI().findLiveTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
	}

	public static Template getWorkingNotLiveHTMLPageTemplate(HTMLPage page) throws DotDataException, DotStateException, DotSecurityException{
		Template t = getHTMLPageTemplate(page,true);
		if(t.isLive())
		    return null;
		else
		    return t;
	}
	public static boolean existsPageName(Inode parent, String pageName) throws DotStateException, DotDataException, DotSecurityException {
		List<HTMLPage> pages = APILocator.getFolderAPI().getHTMLPages((Folder)parent, APILocator.getUserAPI().getSystemUser(), false);
		for(HTMLPage htmlpage:pages){
			if(pageName.equalsIgnoreCase(htmlpage.getPageUrl())){
				Logger.debug(HTMLPageFactory.class, "existsFileName=" + htmlpage.getInode());
				return (InodeUtils.isSet(htmlpage.getInode()));
			}
		}
		return false;
	}

	public static HTMLPage getWorkingHTMLPageByPath(String path, Host host) throws DotStateException, DotDataException, DotSecurityException{
	   
        Identifier id = APILocator.getIdentifierAPI().find(host, path);

        Logger.debug(HTMLPageFactory.class, "Looking for page : " + path);
		Logger.debug(HTMLPageFactory.class, "got id " + id.getInode());
        
        //if this page does not exist, create it, add it to the course folder, use the course template, etc...
        if(!InodeUtils.isSet(id.getInode())){
            return  new HTMLPage();
        }
        
	    return (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(),false);
	}
	
	
	public static Template getTemplate(HTMLPage htmlpage) throws DotDataException, DotStateException, DotSecurityException {
		return getHTMLPageTemplate(htmlpage);
	}
	
	/**
	 * Method used to move an htmlpage to a different folder
	 * @param currentHTMLPage
	 * @param parent
	 * @return
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */
	public static boolean moveHTMLPage (HTMLPage currentHTMLPage, Folder parent) throws DotStateException, DotDataException, DotSecurityException {
	

		
		Identifier identifier = APILocator.getIdentifierAPI().find(currentHTMLPage);

		//gets working container
		HTMLPage workingWebAsset = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(),false);
		//gets live container
		HTMLPage liveWebAsset = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(),false);


        if (HTMLPageFactory.existsPageName(parent, workingWebAsset.getPageUrl())) {
        	return false;
        }

        //moving folders
        Folder oldParent = APILocator.getFolderAPI().findParentFolder(workingWebAsset, APILocator.getUserAPI().getSystemUser(),false);
        /*oldParent.deleteChild(workingWebAsset);
        if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
        	oldParent.deleteChild(liveWebAsset);
        }

        //parent.addChild(workingWebAsset);
        if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
        	parent.addChild(liveWebAsset);
        }*/

        //updating caches
        WorkingCache.removeAssetFromCache(workingWebAsset);
        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(workingWebAsset);

        /*
         * This code is commented fix the task DOTCMS-6883
         * if ((liveWebAsset!=null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
        	LiveCache.removeAssetFromCache(liveWebAsset);
        }*/

        //gets identifier for this webasset and changes the uri and
        // persists it
		User systemUser;
		Host newHost;
		try {
			systemUser = APILocator.getUserAPI().getSystemUser();
	        newHost = hostAPI.findParentHost(parent, systemUser, false);
		} catch (DotDataException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
        identifier.setHostId(newHost.getIdentifier());
        identifier.setURI(workingWebAsset.getURI(parent));
        //HibernateUtil.saveOrUpdate(identifier);
        APILocator.getIdentifierAPI().save(identifier);

        //Add to Preview and Live Cache
        if ((liveWebAsset!=null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
        	LiveCache.removeAssetFromCache(liveWebAsset);
        	LiveCache.addToLiveAssetToCache(liveWebAsset);
        }
        WorkingCache.removeAssetFromCache(workingWebAsset);
        WorkingCache.addToWorkingAssetToCache(workingWebAsset);
        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(workingWebAsset);

        //republishes the page to reset the VTL_SERVLETURI variable
        if ((liveWebAsset!=null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
        	PageServices.invalidate(liveWebAsset);
        }

        //Wipe out menues
        //RefreshMenus.deleteMenus();
        RefreshMenus.deleteMenu(oldParent,parent);
        
        return true;
	
	}
	
	@SuppressWarnings("deprecation")
	public static HTMLPage copyHTMLPage (HTMLPage currentHTMLPage, Folder parent) throws DotDataException, DotStateException, DotSecurityException {
		
		if (!currentHTMLPage.isWorking()) {
			Identifier id = APILocator.getIdentifierAPI().find(currentHTMLPage);
			currentHTMLPage = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(id,APILocator.getUserAPI().getSystemUser(),false);
		}
		Folder currentParentFolder = APILocator.getFolderAPI().findParentFolder(currentHTMLPage, APILocator.getUserAPI().getSystemUser(),false);
		
	    Logger.debug(HTMLPageFactory.class, "Copying HTMLPage: " + currentHTMLPage.getURI(currentParentFolder) + " to: " + APILocator.getIdentifierAPI().find(parent).getPath());

	    //gets the new information for the template from the request object
		HTMLPage newHTMLPage = new HTMLPage();

		newHTMLPage.copy(currentHTMLPage);

		//gets page url before extension
		String pageURL = com.dotmarketing.util.UtilMethods
				.getFileName(currentHTMLPage.getPageUrl());
		//gets file extension
		String fileExtension = com.dotmarketing.util.UtilMethods
				.getFileExtension(currentHTMLPage.getPageUrl());
		
		boolean isCopy = false;
		while (HTMLPageFactory.existsPageName(parent, pageURL + "." + fileExtension)) {
			pageURL = pageURL + "_copy";
			isCopy = true;
		}
		
		newHTMLPage.setPageUrl(pageURL + "." + fileExtension);
		
		if (isCopy)
			newHTMLPage.setFriendlyName(currentHTMLPage.getFriendlyName() + " (COPY)");
		
		//gets current template from html page and attach it to the new page
		Template currentTemplate = HTMLPageFactory.getHTMLPageTemplate(currentHTMLPage);
		newHTMLPage.setTemplateId(currentTemplate.getIdentifier());
		
		//persists the webasset
		HibernateUtil.saveOrUpdate(newHTMLPage);

		//Add the new page to the folder
		//parent.addChild(newHTMLPage);

		//creates new identifier for this webasset and persists it
		Identifier newIdent = APILocator.getIdentifierAPI().createNew(newHTMLPage, parent);
		newHTMLPage.setIdentifier(newIdent.getId());
		
		WorkingCache.removeAssetFromCache(newHTMLPage);
		WorkingCache.addToWorkingAssetToCache(newHTMLPage);
		LiveCache.removeAssetFromCache(newHTMLPage);
		LiveCache.addToLiveAssetToCache(newHTMLPage);
		
		APILocator.getVersionableAPI().setWorking(newHTMLPage);

		//Copy permissions
		permissionAPI.copyPermissions(currentHTMLPage, newHTMLPage);
		
		return newHTMLPage;
	}
	
	
    @SuppressWarnings({ "unchecked", "deprecation" })
	public static boolean renameHTMLPage (HTMLPage page, String newName, User user) throws Exception {

    	// Checking permissions
    	if (!permissionAPI.doesUserHavePermission(page, PERMISSION_WRITE, user))
    		throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);

    	//getting old file properties
    	Folder folder = APILocator.getFolderAPI().findParentFolder(page, user, false);
    	
		Host host;
		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
	        host = hostAPI.findParentHost(folder, systemUser, false);
		} catch (DotDataException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HTMLPageFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

    	Identifier ident = APILocator.getIdentifierAPI().find(page);

    	HTMLPage tempPage = new HTMLPage();
    	tempPage.copy(page);
    	// sets filename for this new file
    	
    	String newNamePage = newName + "." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
    	
    	tempPage.setPageUrl(newNamePage);
    	tempPage.setFriendlyName(newNamePage);

    	Identifier testIdentifier = APILocator.getIdentifierAPI().find(host, tempPage.getURI(folder));

    	if(InodeUtils.isSet(testIdentifier.getInode()) || page.isLocked())
    		return false;

    	List<Versionable> versions = APILocator.getVersionableAPI().findAllVersions(ident);
    	
    	boolean islive = false;
    	HTMLPage workingVersion = null;
    	
    	for (Versionable version : versions) {
            HTMLPage htmlpage = (HTMLPage)version;
	    	// sets filename for this new file
            htmlpage.setPageUrl(newNamePage);
            htmlpage.setFriendlyName(newNamePage);

	    		
	    	HibernateUtil.saveOrUpdate(htmlpage);
	    	if (htmlpage.isLive())
	    		islive = true;
	    	if (htmlpage.isWorking())
	    		workingVersion = htmlpage;
    	}
    	
   		LiveCache.removeAssetFromCache(workingVersion);
   		WorkingCache.removeAssetFromCache(workingVersion);
   		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(workingVersion);
   		

   		
    	ident.setURI(page.getURI(folder));
    	//HibernateUtil.saveOrUpdate(ident);
    	APILocator.getIdentifierAPI().save(ident);
    	
    	if (islive){
    		LiveCache.removeAssetFromCache(workingVersion);
    		LiveCache.addToLiveAssetToCache(workingVersion);
    	}
    	WorkingCache.removeAssetFromCache(workingVersion);
   		WorkingCache.addToWorkingAssetToCache(workingVersion);
   		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(workingVersion);
    	
   		if(page.isShowOnMenu())
   		{
   			//RefreshMenus.deleteMenus();
   			RefreshMenus.deleteMenu(page);
   		}
    	return true;
	}
}
