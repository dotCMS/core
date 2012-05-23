package com.dotmarketing.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;

/**
 *
 * @author  maria, david (2005)
 */
public class PublishFactory {	

	private static PermissionAPI permissionAPI  = APILocator.getPermissionAPI();

	/**
	 * @param permissionAPI the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	public static boolean publishAsset(Inode webAsset,HttpServletRequest req) throws WebAssetException, DotSecurityException, DotDataException
	{
		User user = null;
		try {
			user = PortalUtil.getUser(req);
		} catch (Exception e1) {
			Logger.error(PublishFactory.class, "publishAsset: Cannot obtain the user from the request.", e1);
			return false;
		}
		
		return publishAsset(webAsset, user, false);
	}

	
	/**
	 * This method publish a given and asset and its related assets 
	 * if a user is passed the method will check permissions to publish
	 * if the user doesn't have permission to publish one of the related assets
	 * then that one will be skipped
	 * @param webAsset
	 * @param user
	 * @param respectFrontendRoles TODO
	 * @return
	 * @throws WebAssetException 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	@SuppressWarnings("unchecked")
	public static boolean publishAsset(Inode webAsset, User user, boolean respectFrontendRoles) throws WebAssetException, DotSecurityException, DotDataException 
	{
  
		return publishAsset(webAsset,user,respectFrontendRoles, true);

	}
	
	/**
	 * This method publish a given and asset and its related assets 
	 * if a user is passed the method will check permissions to publish
	 * if the user doesn't have permission to publish one of the related assets
	 * then that one will be skipped
	 * @param webAsset
	 * @param user
	 * @param respectFrontendRoles
	 * @param isNewVersion  - if passed false then the webasset's mod user and mod date will NOT be altered. @see {@link ContentletAPI#checkinWithoutVersioning(Contentlet, java.util.Map, List, List, User, boolean)}checkinWithoutVersioning. 
	 * @return
	 * @throws WebAssetException 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	@SuppressWarnings("unchecked")
	public static boolean publishAsset(Inode webAsset, User user, boolean respectFrontendRoles, boolean isNewVersion) throws WebAssetException, DotSecurityException, DotDataException 
	{
		ContentletAPI conAPI = APILocator.getContentletAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		
		//http://jira.dotmarketing.net/browse/DOTCMS-6325
		if (user != null && 
				((webAsset instanceof Folder)?
				!permissionAPI.doesUserHavePermission(webAsset, PermissionAPI.PERMISSION_EDIT, user):
				!permissionAPI.doesUserHavePermission(webAsset, PERMISSION_PUBLISH, user))) {
			Logger.debug(PublishFactory.class, "publishAsset: user = " + user.getEmailAddress() + ", don't have permissions to publish: " + webAsset);
			return false;
		}
		
		if (webAsset instanceof WebAsset)
		{
			try {
				WebAssetFactory.publishAsset((WebAsset) webAsset, user, isNewVersion);
			} catch (Exception e) {
				Logger.error(PublishFactory.class, "publishAsset: Failed to publish the asset.", e);
			}
		}

		if (webAsset instanceof com.dotmarketing.portlets.files.model.File) 
		{
			// publishing a file
			LiveCache.removeAssetFromCache((WebAsset)webAsset);
			LiveCache.addToLiveAssetToCache((WebAsset) webAsset);
			WorkingCache.removeAssetFromCache((WebAsset)webAsset);
			WorkingCache.addToWorkingAssetToCache((WebAsset) webAsset);
			if(RefreshMenus.shouldRefreshMenus((com.dotmarketing.portlets.files.model.File)webAsset)){
				com.dotmarketing.menubuilders.RefreshMenus.deleteMenu((WebAsset)webAsset);
			}

		}

		if (webAsset instanceof Container) {

			//saves to live folder under velocity
			ContainerServices.invalidate((Container)webAsset);
		}


		if (webAsset instanceof Template) {

		    Logger.debug(PublishFactory.class, "*****I'm a Template -- Publishing");

			//gets all identifier children
			java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate((Template)webAsset, APILocator.getUserAPI().getSystemUser(), false);
			java.util.Iterator<Container> identifiersIter = identifiers.iterator();
			while (identifiersIter.hasNext()) {

				Container container =(Container) identifiersIter.next();

			    Logger.debug(PublishFactory.class, "*****I'm a Template -- Publishing my Container Child=" + container.getInode());
			    if(!container.isLive()){
			    	publishAsset(container,user, respectFrontendRoles, isNewVersion);
			    }
				
				
			}
			//writes the template to a live directory under velocity folder
			TemplateServices.invalidate((Template)webAsset);

		}

		if (webAsset instanceof HTMLPage) 
		{

		    Logger.debug(PublishFactory.class, "*****I'm an HTML Page -- Publishing");

		    List relatedNotPublished = new ArrayList();
		    relatedNotPublished = getUnpublishedRelatedAssets(webAsset, relatedNotPublished, user, respectFrontendRoles);
		    
		    //Publishing related pieces of content
		    for(Object asset : relatedNotPublished) {
		    	if(asset instanceof Contentlet) {
					Logger.debug(PublishFactory.class, "*****I'm an HTML Page -- Publishing my Contentlet Child=" + ((Contentlet)asset).getInode());
					try {
						Contentlet c = (Contentlet)asset;
						if(!APILocator.getWorkflowAPI().findSchemeForStruct(c.getStructure()).isMandatory()){
							conAPI.publish((Contentlet)asset, user, false);
						}
					} catch (DotSecurityException e) {
						//User has no permission to publish the content in the page so we just skip it
						Logger.debug(PublishFactory.class, "publish html page: User has no permission to publish the content inode = " + ((Contentlet)asset).getInode() + " in the page, skipping it.");
					}		    		
		    	}else if(asset instanceof Template){
		    		Logger.debug(PublishFactory.class, "*****I'm an HTML Page -- Publishing Template =" + ((Template)asset).getInode());
		    		publishAsset((Template)asset,user, respectFrontendRoles,false);
		    	}
		    }

		    LiveCache.removeAssetFromCache((WebAsset) webAsset);
			LiveCache.addToLiveAssetToCache((WebAsset) webAsset);
			WorkingCache.removeAssetFromCache((WebAsset) webAsset);
			WorkingCache.addToWorkingAssetToCache((WebAsset) webAsset);
			//writes the htmlpage to a live directory under velocity folder
			PageServices.invalidate((HTMLPage)webAsset);

            //Refreshing the menues
			
			
			if(RefreshMenus.shouldRefreshMenus((HTMLPage)webAsset)){
				Folder folder = (Folder) APILocator.getFolderAPI().findParentFolder((Treeable)webAsset,user,false);
				if(folder != null){
					RefreshMenus.deleteMenu(folder);
				}
			}
            CacheLocator.getHTMLPageCache().remove((HTMLPage) webAsset);

		}

		if (webAsset instanceof Folder) {

			Folder parentFolder = (Folder) webAsset;

		    Logger.debug(PublishFactory.class, "*****I'm a Folder -- Publishing" + parentFolder.getName());

			//gets all links for this folder
			java.util.List foldersListSubChildren = APILocator.getFolderAPI().findSubFolders(parentFolder,APILocator.getUserAPI().getSystemUser(),false);
			//gets all links for this folder
			java.util.List linksListSubChildren = APILocator.getFolderAPI().getWorkingLinks(parentFolder, user, false);
			//gets all html pages for this folder
			java.util.List htmlPagesSubListChildren = APILocator.getFolderAPI().getWorkingHTMLPages(parentFolder,user,false);
			//gets all files for this folder
			java.util.List filesListSubChildren = APILocator.getFolderAPI().getWorkingFiles(parentFolder,user,false);
			//gets all templates for this folder
			//java.util.List templatesListSubChildren = APILocator.getFolderAPI().getWorkingChildren(parentFolder,Template.class);
			//gets all containers for this folder
			//java.util.List containersListSubChildren = APILocator.getFolderAPI().getWorkingChildren(parentFolder,Container.class);

			//gets all subitems
			java.util.List elements = new java.util.ArrayList();
			elements.addAll(foldersListSubChildren);
			elements.addAll(linksListSubChildren);
			elements.addAll(htmlPagesSubListChildren);
			elements.addAll(filesListSubChildren);
			//elements.addAll(templatesListSubChildren);
			//elements.addAll(containersListSubChildren);

			java.util.Iterator elementsIter = elements.iterator();
			while (elementsIter.hasNext()) {
				Inode inode = (Inode) elementsIter.next();
			    Logger.debug(PublishFactory.class, "*****I'm a Folder -- Publishing my Inode Child=" + inode.getInode());
				publishAsset(inode,user, respectFrontendRoles, isNewVersion);
			}
			
			java.util.List<Contentlet> contentlets = conAPI.findContentletsByFolder(parentFolder, user, false);
			java.util.Iterator<Contentlet> contentletsIter = contentlets.iterator();
			while (contentletsIter.hasNext()) {
				//publishes each one
				Contentlet contentlet = (Contentlet)contentletsIter.next();
				Logger.debug(PublishFactory.class, "*****I'm a Folder -- Publishing my Inode Child=" + contentlet.getInode());
				if(!contentlet.isLive() && (permissionAPI.doesUserHavePermission(contentlet, PERMISSION_PUBLISH, user, respectFrontendRoles))) {
					APILocator.getContentletAPI().publish(contentlet, user, false);
				}
			}
		}

		if (webAsset instanceof Link) {
			List contentlets = InodeFactory.getParentsOfClass(webAsset, com.dotmarketing.portlets.contentlet.business.Contentlet.class);
			Iterator it = contentlets.iterator();
			while (it.hasNext()) {
				com.dotmarketing.portlets.contentlet.business.Contentlet cont = (com.dotmarketing.portlets.contentlet.business.Contentlet) it.next();
			    if (cont.isLive()) {
			    	try {
			    		com.dotmarketing.portlets.contentlet.model.Contentlet newFormatContentlet = 
							conAPI.convertFatContentletToContentlet(cont);
						ContentletServices.invalidate(newFormatContentlet,  false);
				    	ContentletMapServices.invalidate(newFormatContentlet, false);
					} catch (DotDataException e) {
						throw new WebAssetException(e.getMessage(), e);
					}
			    }
			}
			// Removes static menues to provoke all possible dependencies be generated.
			Folder parentFolder = (Folder)APILocator.getFolderAPI().findParentFolder((Treeable) webAsset,user,false);
			Host host = (Host) hostAPI.findParentHost(parentFolder, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);
			RefreshMenus.deleteMenu(host);
		}		
		
		return true;

	}
	
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getUnpublishedRelatedAssets(webAsset, relatedAssets, true, false, user, respectFrontendRoles);
	}
	
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, boolean checkPublishPermissions, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return getUnpublishedRelatedAssets(webAsset, relatedAssets, true,checkPublishPermissions, user, respectFrontendRoles);
	}
	
	/**
	 * Retrieves a list of dependent object (dependent of object of the given webAsset param) 
	 * that the given user has permissions to publish
	 * @param webAsset
	 * @param relatedAssets
	 * @param returnOnlyWebAssets
	 * @param checkPublishPermissions
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException 
	 * @throws DotSecurityException 
	 */
	@SuppressWarnings("unchecked")
	public static List getUnpublishedRelatedAssets(Inode webAsset, List relatedAssets, boolean returnOnlyWebAssets, boolean checkPublishPermissions, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		ContentletAPI conAPI = APILocator.getContentletAPI();

		if (webAsset instanceof Template) {

		    Logger.debug(PublishFactory.class, "*****I'm a Template -- PrePublishing");

			//gets all identifier children

			java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate((Template)webAsset, APILocator.getUserAPI().getSystemUser(), false);
			Iterator<Container> identifiersIter = identifiers.iterator();
			while (identifiersIter.hasNext()) {

				Container container = (Container)identifiersIter.next();
				if(!container.isLive() && (permissionAPI.doesUserHavePermission(container, PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
					relatedAssets.add(container);
				}
			}

		}

		if (webAsset instanceof HTMLPage) {

		    Logger.debug(PublishFactory.class, "*****I'm an HTML Page -- PrePublishing");

			//gets working (not published) template parent for this html page
			Template templateParent = HTMLPageFactory.getHTMLPageTemplate((HTMLPage)webAsset,true);

			if (InodeUtils.isSet(templateParent.getInode())) {
				
				if(!templateParent.isLive() && (permissionAPI.doesUserHavePermission(templateParent, PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
					relatedAssets.add(templateParent);
				}


				//gets all live container children				
				java.util.List<Container> identifiers = APILocator.getTemplateAPI().getContainersInTemplate(templateParent, APILocator.getUserAPI().getSystemUser(), false);
				java.util.Iterator<Container> identifiersIter = identifiers.iterator();
				while (identifiersIter.hasNext()) {

					Container container = (Container)identifiersIter.next();

                    List categories = InodeFactory.getParentsOfClass(container, Category.class);
					List contentlets = null;

					if (categories.size() == 0) {
					    Logger.debug(PublishFactory.class, "*******HTML Page PrePublishing Static Container");
					    Identifier idenHtmlPage = APILocator.getIdentifierAPI().find(webAsset);
					    Identifier idenContainer = APILocator.getIdentifierAPI().find(container);
					    try{
					    	contentlets = conAPI.findPageContentlets(idenHtmlPage.getInode(),idenContainer.getInode(),null, true, -1, APILocator.getUserAPI().getSystemUser(), false);
					    }catch (Exception e) {
							 Logger.error(PublishFactory.class,"Unable to get contentlets on page",e);
							 contentlets = new ArrayList<Contentlet>();
						}
                    }
                    else {

            		    Logger.debug(PublishFactory.class, "*******HTML Page PrePublishing Dynamic Container");
                        Iterator catsIter = categories.iterator();
                        Set contentletSet = new HashSet();

                        String condition = "working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " and deleted=" + com.dotmarketing.db.DbConnectionFactory.getDBFalse();
                        String sort = (container.getSortContentletsBy() == null) ? "sort_order" : container.getSortContentletsBy();

                        while (catsIter.hasNext()) {
                            Category category = (Category) catsIter.next();
                            List contentletsChildren = InodeFactory.getChildrenClassByConditionAndOrderBy(category,
                                    Contentlet.class, condition, sort);
                            if (contentletsChildren != null && contentletsChildren.size() > 0) {
                                contentletSet.addAll(contentletsChildren);
                            }
                        }
                        contentlets = new ArrayList();
                        contentlets.addAll(contentletSet);
                    }
					java.util.Iterator contentletsIter = contentlets.iterator();
					while (contentletsIter.hasNext()) {
						//publishes each one
						Contentlet contentlet = (Contentlet)contentletsIter.next();
						if(!contentlet.isLive() && (permissionAPI.doesUserHavePermission(contentlet, PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
							relatedAssets.add(contentlet);
						}
					}
				}

			}

		}

		if (webAsset instanceof Folder) {

			Folder parentFolder = (Folder) webAsset;

		    Logger.debug(PublishFactory.class, "*****I'm a Folder -- PrePublishing" + parentFolder.getName());
		    
			//gets all links for this folder
			java.util.List foldersListSubChildren = APILocator.getFolderAPI().findSubFolders(parentFolder,APILocator.getUserAPI().getSystemUser(),false);
			//gets all links for this folder
			java.util.List linksListSubChildren = APILocator.getFolderAPI().getWorkingLinks(parentFolder,user,false);
			//gets all html pages for this folder
			java.util.List htmlPagesSubListChildren = APILocator.getFolderAPI().getWorkingHTMLPages(parentFolder,user,false);
			//gets all files for this folder
			java.util.List filesListSubChildren = APILocator.getFolderAPI().getWorkingFiles(parentFolder,user,false);
			//gets all templates for this folder
			//java.util.List templatesListSubChildren = APILocator.getFolderAPI().getWorkingChildren(parentFolder,Template.class);
			//gets all containers for this folder
			//java.util.List containersListSubChildren = APILocator.getFolderAPI().getWorkingChildren(parentFolder,Container.class);

			//gets all subitems
			java.util.List elements = new java.util.ArrayList();
			elements.addAll(foldersListSubChildren);
			elements.addAll(linksListSubChildren);
			elements.addAll(htmlPagesSubListChildren);
			elements.addAll(filesListSubChildren);
			//elements.addAll(templatesListSubChildren);
			//elements.addAll(containersListSubChildren);



			java.util.Iterator elementsIter = elements.iterator();
			while (elementsIter.hasNext()) {
				Inode asset = (Inode) elementsIter.next();
				if (asset instanceof WebAsset) {
					if(!((WebAsset)asset).isLive() && (permissionAPI.doesUserHavePermission(((WebAsset)asset), PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
						relatedAssets.add(asset);
					}
				}else if(!returnOnlyWebAssets){
					relatedAssets.add(asset);
				}
				//if it exists it prepublishes it
				relatedAssets = getUnpublishedRelatedAssets(asset,relatedAssets, returnOnlyWebAssets, checkPublishPermissions, user, respectFrontendRoles);
			}
			
			java.util.List<Contentlet> contentlets = conAPI.findContentletsByFolder(parentFolder, user, false);
			java.util.Iterator<Contentlet> contentletsIter = contentlets.iterator();
			while (contentletsIter.hasNext()) {
				Contentlet contentlet = (Contentlet)contentletsIter.next();
				if(!contentlet.isLive() && (permissionAPI.doesUserHavePermission(contentlet, PERMISSION_PUBLISH, user, respectFrontendRoles) || !checkPublishPermissions)) {
					relatedAssets.add(contentlet);
				}
			}

		}

		return relatedAssets;
	}

}
