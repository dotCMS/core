package com.dotmarketing.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.transform.DBTransformer;
import com.dotcms.util.transform.TransformerLocator;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.PermissionAsset;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import java.util.Optional;

/**
 *
 * @author maria, david(2005)
 */
public class WebAssetFactory {

	public enum Direction {
		PREVIOUS,
		NEXT
	}

	public enum AssetType {
		HTMLPAGE("HTMLPAGE"),
		CONTAINER("CONTAINER"),
		TEMPLATE("TEMPLATE"),
		LINK("LINK");

		private String value;

		AssetType (String value) {
			this.value = value;
		}

		public String toString () {
			return value;
		}

		public static AssetType getObject (String value) {
			AssetType[] ojs = AssetType.values();
			for (AssetType oj : ojs) {
				if (oj.value.equals(value))
					return oj;
			}
			return null;
		}
	}


	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private static ContainerAPI containerAPI = APILocator.getContainerAPI();
	private static TemplateAPI templateAPI = APILocator.getTemplateAPI();
	private static MenuLinkAPI linksAPI = APILocator.getMenuLinkAPI();
	private static SystemEventsAPI systemEventsAPI = APILocator.getSystemEventsAPI();

	/**
	 * @param permissionAPIRef the permissionAPI to set
	 */
	public static void setPermissionAPI(PermissionAPI permissionAPIRef) {
		permissionAPI = permissionAPIRef;
	}

	public static void createAsset(WebAsset webasset, String userId, Inode parent) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		   parent.addChild(webasset);

		// create new identifier, with the URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, (Folder) parent);
		id.setOwner(userId);
		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		APILocator.getIdentifierAPI().save(id);
		webasset.setIdentifier(id.getId());
		HibernateUtil.saveOrUpdate(webasset);
        APILocator.getVersionableAPI().setWorking(webasset);

		systemEventsAPI.pushAsync(SystemEventType.SAVE_LINK, new Payload(webasset, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	public static void createAsset(WebAsset webasset, String userId, Host host) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// create new identifier, without URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, host);
		id.setOwner(userId);
		APILocator.getIdentifierAPI().save(id);

		webasset.setIdentifier(id.getId());
		HibernateUtil.saveOrUpdate(webasset);

		APILocator.getVersionableAPI().setWorking(webasset);

		systemEventsAPI.pushAsync(SystemEventType.SAVE_LINK, new Payload(webasset, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	public static void createAsset(WebAsset webasset, String userId, Inode parent, Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);

		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		webasset.setIdentifier(identifier.getInode());


		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		APILocator.getVersionableAPI().setWorking(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		    parent.addChild(webasset);

		// adds asset to the existing identifier
		//identifier.addChild(webasset);

	}

	public static void createAsset(WebAsset webasset, String userId, Identifier identifier) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);

		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		webasset.setIdentifier(identifier.getInode());


		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		APILocator.getVersionableAPI().setWorking(webasset);
		// adds asset to the existing identifier
		//identifier.addChild(webasset);

	}

	public static void createAsset(WebAsset webasset, String userId, Inode parent, Identifier identifier,
			boolean working) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		   parent.addChild(webasset);

		// adds asset to the existing identifier
		//identifier.addChild(webasset);
		//webasset.addParent(identifier);
		webasset.setIdentifier(identifier.getInode());

		HibernateUtil.saveOrUpdate(webasset);

		if(working)
		    APILocator.getVersionableAPI().setWorking(webasset);
	}

	public static void createAsset(WebAsset webasset, String userId, Inode parent, Identifier identifier,
			boolean working, boolean isLive) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		  parent.addChild(webasset);

		// adds asset to the existing identifier
		  //identifier.addChild(webasset);
		  //webasset.addParent(identifier);
		webasset.setIdentifier(identifier.getInode());

		HibernateUtil.saveOrUpdate(webasset);

		if(working)
	        APILocator.getVersionableAPI().setWorking(webasset);
		if(isLive)
	        APILocator.getVersionableAPI().setLive(webasset);
	}

	public static void createAsset(WebAsset webasset, String userId, Identifier identifier, boolean working) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		//HibernateUtil.saveOrUpdate(webasset);

		// adds asset to the existing identifier
		//identifier.addChild(webasset);
		//webasset.addParent(identifier);
		webasset.setIdentifier(identifier.getInode());

		HibernateUtil.saveOrUpdate(webasset);

		if(working)
	        APILocator.getVersionableAPI().setWorking(webasset);

	}

	public static void createAsset(WebAsset webasset, String userId, Inode parent, boolean isLive) throws DotDataException, DotStateException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		  parent.addChild(webasset);

		// create new identifier, with the URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, (Folder) parent);
		id.setOwner(userId);
		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		APILocator.getIdentifierAPI().save(id);

		webasset.setIdentifier(id.getId());
		HibernateUtil.saveOrUpdate(webasset);

        APILocator.getVersionableAPI().setWorking(webasset);
        if(isLive)
            APILocator.getVersionableAPI().setLive(webasset);
	}

	public static WebAsset getParentWebAsset(Inode i) {
		HibernateUtil dh = new HibernateUtil(WebAsset.class);
		WebAsset webAsset = null ;
		try {
			dh.setQuery("from inode in class " + WebAsset.class.getName() + " where ? in inode.children.elements");
			dh.setParam(i.getInode());
			webAsset = (WebAsset) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(WebAssetFactory.class,"getParentWebAsset failed:" + e,e);
		}
		return webAsset;
	}



	public static void renameAsset(WebAsset webasset) throws DotStateException, DotDataException, DotSecurityException {
		List versions = getAssetVersionsandLive(webasset);
		Iterator versIter = versions.iterator();
		while (versIter.hasNext()) {
			WebAsset currWebAsset = (WebAsset) versIter.next();
			currWebAsset.setFriendlyName(webasset.getFriendlyName());
		}
	}

	public static boolean editAsset(WebAsset currWebAsset, String userId) throws DotStateException, DotDataException, DotSecurityException {

		if (!currWebAsset.isLocked()) {
			// sets lock true
		    User proxyuser=new User(userId);
			APILocator.getVersionableAPI().setLocked(currWebAsset, true, proxyuser);
			return true;
		}

		// if it is locked then we compare lockedBy with userId from the user that wants to edit the asset
		Optional<String> lockedByOptional = APILocator.getVersionableAPI().getLockedBy(currWebAsset);
		return lockedByOptional.map(lockedBy -> lockedBy.equals(userId)).orElse(false);
	}

	public static WebAsset getBackAssetVersion(WebAsset versionWebAsset) throws Exception {
		Identifier id = (Identifier) APILocator.getIdentifierAPI().find(versionWebAsset);
		if (!InodeUtils.isSet(id.getInode())) {
			throw new Exception("Web asset Identifier not found!");
		}
		WebAsset working = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);
		if (!InodeUtils.isSet(working.getInode())) {
			throw new Exception("Working copy not found!");
		}
		APILocator.getVersionableAPI().setWorking(versionWebAsset);
		return versionWebAsset;

	}

	/**
	 * This method is odd. You send it an asset, but that may not be the one
	 * that get published. The method will get the identifer of the asset you
	 * send it and find the working version of the asset and make that the live
	 * version.
	 *
	 * @param currWebAsset
	 *            This asset's identifier will be used to find the "working"
	 *            asset.
	 * @return This method returns the OLD live asset or null. Wierd.
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	@SuppressWarnings("unchecked")
	public static WebAsset publishAsset(WebAsset currWebAsset) throws DotStateException, DotDataException, DotSecurityException {

		Logger.debug(WebAssetFactory.class, "Publishing asset!!!!");
		// gets the identifier for this asset
		Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
		// gets the current working asset

		WebAsset workingwebasset = null;

		// gets the current working asset
		workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		if (!InodeUtils.isSet(workingwebasset.getInode())) {
			workingwebasset = currWebAsset;
		}

		Logger.debug(WebAssetFactory.class, "workingwebasset=" + workingwebasset.getInode());

		WebAsset livewebasset = null;


			// gets the current working asset
		livewebasset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		if(workingwebasset.isDeleted()){
			throw new DotStateException("You may not publish deleted assets!!!");
		}

		/*if ((livewebasset != null) && (InodeUtils.isSet(livewebasset.getInode()))
				&& (livewebasset.getInode() != workingwebasset.getInode())) {

			Logger.debug(WebAssetFactory.class, "livewebasset.getInode()=" + livewebasset.getInode());
			// sets previous live to false
			livewebasset.setLive(false);
			livewebasset.setModDate(new java.util.Date());

			// persists it
			HibernateUtil.saveOrUpdate(livewebasset);
		}*/
		// sets new working to live
        APILocator.getVersionableAPI().setLive(workingwebasset);

		workingwebasset.setModDate(new java.util.Date());

		// persists the webasset
		HibernateUtil.saveOrUpdate(workingwebasset);

		Logger.debug(WebAssetFactory.class, "HibernateUtil.saveOrUpdate(workingwebasset)");


		return livewebasset;
	}

	/**
	 * This method is odd. You send it an asset, but that may not be the one
	 * that get published. The method will get the identifer of the asset you
	 * send it and find the working version of the asset and make that the live
	 * version.
	 *
	 * @param currWebAsset
	 *            This asset's identifier will be used to find the "working"
	 *            asset.
	 * @param user
	 * @return This method returns the OLD live asset or null. Wierd.
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	@SuppressWarnings("unchecked")
	public static WebAsset publishAsset(WebAsset currWebAsset, User user) throws WebAssetException, DotStateException, DotDataException, DotSecurityException {

		return publishAsset(currWebAsset,user,true);

	}


	/**
	 * This method is odd. You send it an asset, but that may not be the one
	 * that get published. The method will get the identifer of the asset you
	 * send it and find the working version of the asset and make that the live
	 * version.
	 *
	 * @param currWebAsset
	 *            This asset's identifier will be used to find the "working"
	 *            asset.
	 * @param user
	 * @param isNewVersion - if passed false then the webasset's mod user and mod date will NOT be altered. @see {@link ContentletAPI#checkinWithoutVersioning(Contentlet, java.util.Map, List, List, User, boolean)}checkinWithoutVersioning.
	 * @return This method returns the OLD live asset or null. Wierd.
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	public static WebAsset publishAsset(WebAsset currWebAsset, User user, boolean isNewVersion) throws WebAssetException, DotStateException, DotDataException, DotSecurityException {

		Logger.debug(WebAssetFactory.class, "Publishing asset!!!!");
		// gets the identifier for this asset
		Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
		// gets the current working asset

		WebAsset workingwebasset = null;

		// gets the current working asset
		workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		if (!InodeUtils.isSet(workingwebasset.getInode())) {
			workingwebasset = currWebAsset;
		}

		Logger.debug(WebAssetFactory.class, "workingwebasset=" + workingwebasset.getInode());

		WebAsset livewebasset = null;

		try {
			// gets the current working asset
			livewebasset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		} catch (Exception e) {
		}
		if(workingwebasset.isDeleted()){
			throw new WebAssetException("You may not publish deleted assets!!!");
		}

		boolean localTransaction = false;
		try {

			localTransaction = HibernateUtil.startLocalTransactionIfNeeded();

			// sets new working to live
			APILocator.getVersionableAPI().setLive(workingwebasset);


			if(isNewVersion){

			   workingwebasset.setModDate(new java.util.Date());
			   workingwebasset.setModUser(user.getUserId());
			}

			// persists the webasset
			HibernateUtil.merge(workingwebasset);

			if(localTransaction) {

				HibernateUtil.commitTransaction();
			}
		} catch(Exception e){

			Logger.error(WebAssetFactory.class, e.getMessage(), e);

			if(localTransaction){

				HibernateUtil.rollbackTransaction();
			}
		} finally {

			if(localTransaction) {
				DbConnectionFactory.closeConnection();
			}
		}

		Logger.debug(WebAssetFactory.class, "HibernateUtil.saveOrUpdate(workingwebasset)");


		systemEventsAPI.pushAsync(SystemEventType.PUBLISH_LINK, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

		return livewebasset;
	}


	public static WebAsset getLiveAsset(WebAsset currWebAsset) throws Exception {

		Logger.debug(WebAssetFactory.class, "Publishing asset!!!!");
		// gets the identifier for this asset
		Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);

		WebAsset livewebasset = null;

		// gets the current working asset
		livewebasset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		return livewebasset;
	}

	public static boolean archiveAsset(WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException {
		return archiveAsset(currWebAsset, (String)null);
	}

	public static boolean archiveAsset(WebAsset currWebAsset, User user) throws DotDataException, DotStateException, DotSecurityException {
		return archiveAsset(currWebAsset, user.getUserId());
	}

	public static boolean archiveAsset(WebAsset currWebAsset, String userId) throws DotDataException, DotStateException, DotSecurityException {

		// gets the identifier for this asset
		Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);

		WebAsset workingwebasset = null;

			// gets the current working asset
			workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);


		WebAsset live = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		User userMod = null;
		try{
			userMod = APILocator.getUserAPI().loadUserById(workingwebasset.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		}catch(Exception ex){
			if(ex instanceof NoSuchUserException){
				try {
					userMod = APILocator.getUserAPI().getSystemUser();
				} catch (DotDataException e) {
					Logger.error(WebAssetFactory.class,e.getMessage(),e);
				}
			}
		}
		if(userMod!=null){
		   workingwebasset.setModUser(userMod.getUserId());
		}


		if (userId == null || !workingwebasset.isLocked() || workingwebasset.getModUser().equals(userId)) {

			if (live!=null && InodeUtils.isSet(live.getInode())) {
		        APILocator.getVersionableAPI().removeLive(live.getIdentifier());
			}

			//Reset the mod date
			workingwebasset.setModDate(new Date ());
			// sets deleted to true
	        APILocator.getVersionableAPI().setDeleted(workingwebasset, true);
			// persists the webasset
			HibernateUtil.saveOrUpdate(workingwebasset);

			systemEventsAPI.pushAsync(SystemEventType.ARCHIVE_LINK, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

			return true;
		}
		return false;
	}

	public static boolean deleteAssetVersion(WebAsset currWebAsset) throws DotStateException, DotDataException, DotSecurityException {

		if (!currWebAsset.isLive() && !currWebAsset.isWorking()) {
			// it's a version so delete from database
			InodeFactory.deleteInode(currWebAsset);
			return true;
		}
		return false;

	}

	public static void unLockAsset(WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException {
		// unlocks current asset
		APILocator.getVersionableAPI().setLocked(currWebAsset, false, null);
	}

	public static void unArchiveAsset(WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException {

		RefreshMenus.deleteMenu(currWebAsset);
		Identifier ident=APILocator.getIdentifierAPI().find(currWebAsset);
		CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
		// gets the identifier for this asset
		APILocator.getVersionableAPI().setDeleted(currWebAsset, false);

		systemEventsAPI.pushAsync(SystemEventType.UN_ARCHIVE_SITE, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(currWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
	}

	public static boolean unPublishAsset(WebAsset currWebAsset, String userId, Inode parent) throws DotStateException, DotDataException, DotSecurityException {
		ContentletAPI conAPI = APILocator.getContentletAPI();
		HostAPI hostAPI = APILocator.getHostAPI();

		// gets the identifier for this asset
		Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);

		WebAsset workingwebasset = null;

		// gets the current working asset
		workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		WebAsset livewebasset = null;

		User modUser = null;
		try{
			modUser = APILocator.getUserAPI().loadUserById(workingwebasset.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
		}catch(Exception ex){
			if(ex instanceof NoSuchUserException){
				try {
					modUser = APILocator.getUserAPI().getSystemUser();
				} catch (DotDataException e) {
					Logger.error(WebAssetFactory.class,e.getMessage(),e);
				}
			}
		}
		if(modUser!=null){
		   workingwebasset.setModUser(modUser.getUserId());
		}

		if (!workingwebasset.isLocked() || workingwebasset.getModUser().equals(userId)) {
			try {
				// gets the current working asset
				livewebasset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);

		        APILocator.getVersionableAPI().removeLive(identifier.getId());
				livewebasset.setModDate(new java.util.Date());
				livewebasset.setModUser(userId);
				HibernateUtil.saveOrUpdate(livewebasset);

				if ((livewebasset.getInode() != workingwebasset.getInode())) {
			        APILocator.getVersionableAPI().setLocked(workingwebasset, false, null);
					// removes from folder or parent inode
					if(parent != null)
						parent.deleteChild(workingwebasset);
				}

				if (currWebAsset instanceof Container) {
					//remove container from the live directory
				    new ContainerLoader().invalidate((Container)currWebAsset);
				} else if (currWebAsset instanceof Template) {
					//remove template from the live directory
					new TemplateLoader().invalidate((Template)currWebAsset);
				} else if( currWebAsset instanceof Link ) {
					// Removes static menues to provoke all possible dependencies be generated.
					if( parent instanceof Folder ) {
						Folder parentFolder = (Folder)parent;
						Host host = hostAPI.findParentHost(parentFolder, APILocator.getUserAPI().getSystemUser(), false);
						RefreshMenus.deleteMenu(host);
						CacheLocator.getNavToolCache().removeNav(host.getIdentifier(), parentFolder.getInode());
					}
				}




				systemEventsAPI.pushAsync(SystemEventType.UN_PUBLISH_LINK, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
						new ExcludeOwnerVerifierBean(currWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	// TO-DO
	// Do this one with a language condition...
	public static java.util.List getAssetVersions(WebAsset currWebAsset) throws DotStateException, DotDataException, DotSecurityException {
		// gets the identifier for this asset
		if (currWebAsset.isWorking()) {
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
			return APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
		}
		return new java.util.ArrayList();
	}

	/*
	 * public static java.util.List getWorkingAssetsOfClass(Class c) { return
	 * IdentifierFactory.getLiveOfClass(c); }
	 */

	// TO-DO
	// Do this one with a language condition.
	public static java.util.List getAssetVersionsandLive(WebAsset currWebAsset) throws DotStateException, DotDataException, DotSecurityException {
		// gets the identifier for this asset
		if (currWebAsset.isWorking()) {
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
			return APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
		}
		return new java.util.ArrayList();
	}

	// Do this one with a language condition.
	public static java.util.List getAssetVersionsandLiveandWorking(WebAsset currWebAsset) throws DotStateException, DotDataException, DotSecurityException {
		// gets the identifier for this asset
		if (currWebAsset.isWorking()) {
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
			return APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
		}
		return new java.util.ArrayList();
	}

	/**
	 * This method save the new asset as the new working version and change the
	 * current working as an old version.
	 *
	 * @param newWebAsset
	 *            New webasset version to be converted as the working asset.
	 * @return The current working webasset (The new version), after the method
	 *         execution is must use this class as the working asset instead the
	 *         class you give as parameter.
	 * @throws Exception
	 *             The method throw an exception when the new asset identifier
	 *             or the working folder cannot be found.
	 */
	public static WebAsset saveAsset(WebAsset newWebAsset, Identifier id) throws Exception {
		if (!InodeUtils.isSet(id.getInode())) {
			throw new Exception("Web asset Identifier not found!");
		}
		WebAsset currWebAsset = null;

		// gets the current working asset
		currWebAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);

		//http://jira.dotmarketing.net/browse/DOTCMS-5927
		if (!InodeUtils.isSet(currWebAsset.getInode())) {
			currWebAsset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(), false);
			if(InodeUtils.isSet(currWebAsset.getInode()) && !currWebAsset.isWorking() && currWebAsset.isLive()){
		        APILocator.getVersionableAPI().setWorking(newWebAsset);
			}else if(!InodeUtils.isSet(currWebAsset.getInode()) || !currWebAsset.isLive()){
				throw new Exception("Working copy not found!");
			}
		}

		 APILocator.getVersionableAPI().setWorking(newWebAsset);

		SystemEventType systemEventType = newWebAsset.getInode() == null ? SystemEventType.SAVE_LINK : SystemEventType.UPDATE_LINK;
		systemEventsAPI.pushAsync(systemEventType, new Payload(newWebAsset, Visibility.EXCLUDE_OWNER,
				new ExcludeOwnerVerifierBean(newWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));

		return newWebAsset;
	}

	@SuppressWarnings("unchecked")
	public static List<WebAsset> getAssetsWorkingWithPermission(Class c, int limit,
			int offset, String orderby, String parent, User user) {
		orderby = SQLUtil.sanitizeSortBy(orderby);
		parent = SQLUtil.sanitizeParameter(parent);

		DotConnect dc = new DotConnect();

		StringBuilder sb = new StringBuilder();
		try {

			if(offset < 0) offset = 0;

			String type = ((Inode) c.newInstance()).getType();
			String tableName = Inode.Type.valueOf(type.toUpperCase()).getTableName();
			String versionTable=Inode.Type.valueOf(type.toUpperCase()).getVersionTableName();

			sb.append("select ").append(tableName).append(".*, ").append(tableName)
					.append("_1_.* from ").append(tableName).append(", inode ")
			  		.append(tableName).append("_1_,identifier identifier, ").append(versionTable)
					.append(" vi ").append(" where ").append(tableName).append(".inode = ")
					.append(tableName).append("_1_.inode and ").append(tableName)
					.append(".identifier = identifier.id ").append(" and vi.identifier=")
					.append(tableName).append(".identifier ").append(" and vi.working_inode=")
					.append(tableName).append(".inode ");

			if (!Strings.isNullOrEmpty(parent)) {
				sb.append(" and identifier.host_inode = '" + parent + "'");
			}
			if(orderby != null)
				sb.append(" order by " + orderby);

			List<WebAsset> toReturn = new ArrayList<WebAsset>();
			int internalLimit = 500;
			int internalOffset = 0;
			boolean done = false;

			while(!done) {
				Logger.debug(WebAssetFactory.class, sb.toString());
				dc.setSQL(sb.toString());

				dc.setStartRow(internalOffset);
				dc.setMaxRows(internalLimit);

				PermissionAPI permAPI = APILocator.getPermissionAPI();


				DBTransformer transformer = TransformerLocator
						.createDBTransformer(dc.loadObjectResults(), c);

				List<WebAsset> list;

				if (transformer != null){
					list = (List<WebAsset>) transformer.asList();
				}else{
					return Collections.emptyList();
				}


				toReturn.addAll(permAPI.filterCollection(list, PermissionAPI.PERMISSION_READ, false, user));
				if(limit > 0 && toReturn.size() >= limit + offset)
					done = true;
				else if(list.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}

			if(offset > toReturn.size()) {
				toReturn = new ArrayList<>();
			} else if(limit > 0) {
				int toIndex = offset + limit > toReturn.size()?toReturn.size():offset + limit;
				toReturn = toReturn.subList(offset, toIndex);
			} else if (offset > 0) {
				toReturn = toReturn.subList(offset, toReturn.size());
			}

			return toReturn;

		} catch (Exception e) {
			Logger.warn(WebAssetFactory.class, "getAssetsPerConditionWithPermission failed:" + e, e);
		}

		return Collections.emptyList();

	}

	public static boolean isAbstractAsset(WebAsset asset) {
		if (asset instanceof Container || asset instanceof Template)
			return true;
		return false;
	}

	public static void changeAssetMenuOrder(Inode asset, int newValue, User user) throws ActionException, DotDataException {

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_WRITE, user))
			throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);

		if (asset instanceof Folder) {
			if (newValue == -1) {
				((Folder)asset).setShowOnMenu(false);
			} else {
				((Folder)asset).setShowOnMenu(true);
			}
			((Folder)asset).setSortOrder(newValue);
			RefreshMenus.deleteMenu(((Folder)asset));
		} else if (asset instanceof WebAsset) {
			if (newValue == -1) {
				((WebAsset)asset).setShowOnMenu(false);
			} else {
				((WebAsset)asset).setShowOnMenu(true);
			}
			((WebAsset)asset).setSortOrder(newValue);
			RefreshMenus.deleteMenu(((WebAsset)asset));
		}
		Identifier ident=APILocator.getIdentifierAPI().find(asset);
		CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());

		HibernateUtil.saveOrUpdate(asset);
	}

	/**
	 * This method totally removes an asset from the cms
	 * @param currWebAsset
	 * @return
	 */
	public static boolean deleteAsset(WebAsset currWebAsset) throws Exception
	{
		return deleteAsset(currWebAsset, null);
	}

	/**
	 * This method totally removes an asset from the cms
	 * @param currWebAsset
	 * @param user If the user is passed (not null) the system will check for write permission of the user in the asset
	 * @return true if the asset was successfully removed
	 */
	public static boolean deleteAsset(WebAsset currWebAsset, User user) throws Exception
	{
		boolean returnValue = false;
		if (!UtilMethods.isSet(currWebAsset) || !InodeUtils.isSet(currWebAsset.getInode()))
		{
			return returnValue;
		}
		//Checking permissions
		int permission = PERMISSION_WRITE;

		if(permissionAPI.doesUserHavePermission(currWebAsset, permission, user))
		{
			//### Delete the IDENTIFIER entry from cache ###

			CacheLocator.getIdentifierCache().removeFromCacheByVersionable(currWebAsset);
			//### END Delete the entry from cache ###


			//Get the identifier of the webAsset
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);
			APILocator.getVersionableAPI().deleteVersionInfo(identifier.getId());

			//### Get and delete the webAsset ###
			List<Versionable> webAssetList = new ArrayList<Versionable>();
			if(currWebAsset instanceof Container)
			{
			    new ContainerLoader().invalidate((Container)currWebAsset);
				webAssetList = APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if(currWebAsset instanceof Template)
			{
				new TemplateLoader().invalidate((Template)currWebAsset);
				//webAssetList = APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if(currWebAsset instanceof Link)
			{
				webAssetList = APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
			}
			for(Versionable webAsset : webAssetList)
			{
				//Delete the permission of each version of the asset
				permissionAPI.removePermissions((WebAsset)webAsset);
				InodeFactory.deleteInode(webAsset);
			}
			//### END Get and delete the webAsset and the identifier ###

			//### Get and delete the tree entries ###
			List<Tree> treeList = new ArrayList<Tree>();
			treeList.addAll(TreeFactory.getTreesByChild(identifier.getInode()));
			treeList.addAll(TreeFactory.getTreesByParent(identifier.getInode()));
			for(Tree tree : treeList)
			{
				TreeFactory.deleteTree(tree);
			}
			//### END Get and delete the tree entries ###

			//### Get and delete the multitree entries ###
			List<MultiTree> multiTrees = new ArrayList<MultiTree>();
			if (currWebAsset instanceof Container)
			{
				multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(identifier);
			}
			if(UtilMethods.isSet(multiTrees))
			{
				for(MultiTree multiTree : multiTrees)
				{
					APILocator.getMultiTreeAPI().deleteMultiTree(multiTree);
				}
			}
			//### END Get and delete the multitree entries ###



			//### Delete the Identifier ###
			APILocator.getIdentifierAPI().delete(identifier);
			//### Delete the Identifier ###
			returnValue = true;

			systemEventsAPI.pushAsync(SystemEventType.DELETE_LINK, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION)));
		}
		else
		{
			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return returnValue;
	}

	public PaginatedArrayList<PermissionAsset> getAssetsAndPermissions(String hostId, Role[] roles,
			boolean includeArchived, int limit, int offset, String orderBy, String tableName, String parent, String query, User user) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		PaginatedArrayList<PermissionAsset>  paginatedEntries = new PaginatedArrayList<PermissionAsset> ();
		long totalCount = 0;

		parent = SQLUtil.sanitizeParameter(parent);
		query = SQLUtil.sanitizeParameter(query);
		orderBy = SQLUtil.sanitizeSortBy(orderBy);

		AssetType type = AssetType.getObject(tableName.toUpperCase());
		java.util.List<? extends Permissionable> elements = null;
		Map<String,Object> params = new HashMap<String, Object>();
		if(UtilMethods.isSet(query)){
			params.put("title", query.toLowerCase().replace("\'","\\\'"));
		}
		try {
		if (type.equals(AssetType.CONTAINER)){
			if(APILocator.getIdentifierAPI().isIdentifier(query)){
				params.put("identifier", query);
			}
			elements = containerAPI.findContainers(user, includeArchived, params, hostId, null, null, parent, offset, limit, orderBy);
		}else if (type.equals(AssetType.TEMPLATE)){
			if(APILocator.getIdentifierAPI().isIdentifier(query)){
				params.put("identifier", query);
			}
			elements = templateAPI.findTemplates(user, includeArchived, params, hostId, null, null,  parent, offset, limit, orderBy);
		}else if (type.equals(AssetType.LINK)){
			elements = linksAPI.findLinks(user, includeArchived, params, hostId, null, null, parent, offset, limit, orderBy);
		}
		} catch (DotSecurityException e) {
			Logger.warn(WebAssetFactory.class, "getAssetsAndPermissions failed:" + e, e);
		} catch (DotDataException e) {
			Logger.warn(WebAssetFactory.class, "getAssetsAndPermissions failed:" + e, e);
		}


	    totalCount =  elements!=null?((PaginatedArrayList)elements).getTotalResults():0;
	    java.util.Iterator<? extends Permissionable> elementsIter = elements.iterator();

		while (elementsIter.hasNext()) {
			Permissionable asset = elementsIter.next();
			PermissionAsset permAsset = new PermissionAsset();
			Folder folderParent = null;
			Host host = null;
			if (asset instanceof WebAsset) {
				// For WebAsset objects
				WebAsset webAsset = (WebAsset) asset;
				if (!WebAssetFactory.isAbstractAsset(webAsset)) {
					folderParent = APILocator.getFolderAPI()
							.findParentFolder(webAsset, user, false);
				}
				try {
					host = APILocator.getHostAPI().findParentHost(webAsset,
							user, false);
				} catch (DotDataException e1) {
					Logger.error(WebAssetFactory.class,
							"Could not load host : ", e1);
				} catch (DotSecurityException e1) {
					Logger.error(WebAssetFactory.class,
							"User does not have required permissions : ", e1);
				}
				if (host != null) {
					if (host.isArchived()) {
						continue;
					}
				}
				if (!WebAssetFactory.isAbstractAsset(webAsset)) {
					permAsset.setPathToMe(APILocator.getIdentifierAPI()
							.find(folderParent).getPath());
				} else {
					permAsset.setPathToMe("");
				}
				if (asset instanceof IHTMLPage) {
					permAsset.setPermissionableAsset(asset);
				} else {
					permAsset.setAsset(webAsset);
				}
			} else {
				// For HTMLPage and IHTMLPage objects
				IHTMLPage page = (IHTMLPage) asset;
				String pathToFolderParent = APILocator.getIdentifierAPI().find(page).getParentPath();
				folderParent = APILocator.getFolderAPI()
						.findParentFolder((Treeable) asset, user, false);
				try {
					String pageHostId = APILocator.getIdentifierAPI()
							.find(page.getIdentifier()).getHostId();
					host = APILocator.getHostAPI().find(pageHostId, user, false);
				} catch (DotDataException e1) {
					Logger.error(WebAssetFactory.class,
							"Could not load host : ", e1);
				} catch (DotSecurityException e1) {
					Logger.error(WebAssetFactory.class,
							"User does not have required permissions : ", e1);
				}
				if (host != null) {
					if (host.isArchived()) {
						continue;
					}
				}
				if(folderParent!=null)
					permAsset.setPathToMe(APILocator.getIdentifierAPI()
							.find(folderParent).getPath());
				else
					permAsset.setPathToMe(pathToFolderParent);
				permAsset.setPermissionableAsset(page);
			}
			java.util.List<Integer> permissions = new ArrayList<Integer>();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(asset, roles, user);
			} catch (DotDataException e) {
				Logger.error(WebAssetFactory.class,"Could not load permissions : ",e);
			}
			permAsset.setPermissions(permissions);
			paginatedEntries.add(permAsset);
		}

		paginatedEntries.setTotalResults(totalCount);

		return paginatedEntries;
	}

}
