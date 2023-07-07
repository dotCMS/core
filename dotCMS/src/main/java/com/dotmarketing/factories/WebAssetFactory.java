package com.dotmarketing.factories;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.transform.DBTransformer;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
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
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.*;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

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

	@WrapInTransaction
	public static void createAsset(WebAsset webasset, String userId, Folder parent) throws DotDataException, DotStateException, DotSecurityException {

		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// create new identifier, with the URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, parent);
		id.setOwner(userId);
		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		APILocator.getIdentifierAPI().save(id);
		webasset.setIdentifier(id.getId());
		HibernateUtil.saveOrUpdate(webasset);
        APILocator.getVersionableAPI().setWorking(webasset);

		final Map webAssetMap = webasset.getMap();
		HibernateUtil.addCommitListener(id.getId(), ()-> {

			Try.run(()->systemEventsAPI.pushAsync(SystemEventType.SAVE_LINK, new Payload(webAssetMap, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
		});
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

		final Map webAssetMap = webasset.getMap();
		HibernateUtil.addCommitListener(id.getId(), ()-> {
			Try.run(()->systemEventsAPI.pushAsync(SystemEventType.SAVE_LINK, new Payload(webAssetMap, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
		});
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
		webasset.setIdentifier(identifier.getInode());

		HibernateUtil.saveOrUpdate(webasset);

		if(working)
	        APILocator.getVersionableAPI().setWorking(webasset);

	}

	public static void createAsset(WebAsset webasset, String userId, Folder parent, boolean isLive) throws DotDataException, DotStateException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		HibernateUtil.saveOrUpdate(webasset);

		// create new identifier, with the URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, parent);
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

		if(currWebAsset instanceof Template){
			// sets new working to live
			APILocator.getVersionableAPI().setLive(workingwebasset);
			//if it's a new version update modDate and modUser
			if (isNewVersion) {
				workingwebasset.setModDate(new java.util.Date());
				workingwebasset.setModUser(user.getUserId());
			}
			//update template
			FactoryLocator.getTemplateFactory().save((Template) workingwebasset);
		} else {

				// sets new working to live
				APILocator.getVersionableAPI().setLive(workingwebasset);

				if (isNewVersion) {
					workingwebasset.setModDate(new java.util.Date());
					workingwebasset.setModUser(user.getUserId());
				}

				// persists the webasset
				HibernateUtil.merge(workingwebasset);


			Logger.debug(WebAssetFactory.class, "HibernateUtil.saveOrUpdate(workingwebasset)");
		}

		final Map webAssetMap = currWebAsset.getMap();
		HibernateUtil.addCommitListener(identifier.getId(), ()-> {
			Try.run(()->systemEventsAPI.pushAsync(SystemEventType.PUBLISH_LINK, new Payload(webAssetMap, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
		});

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

		WebAsset workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);


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
	        if(currWebAsset instanceof Template){
	        	FactoryLocator.getTemplateFactory().save(Template.class.cast(workingwebasset));
			} else {
				// persists the webasset
				HibernateUtil.merge(workingwebasset);
			}

			final Map webAssetMap = currWebAsset.getMap();
			HibernateUtil.addCommitListener(identifier.getId(), ()-> {
				Try.run(()->systemEventsAPI.pushAsync(SystemEventType.ARCHIVE_LINK, new Payload(webAssetMap, Visibility.EXCLUDE_OWNER,
						new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
			});

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

	public static void unArchiveAsset(final WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException {

		RefreshMenus.deleteMenu(currWebAsset);
		final Identifier ident=APILocator.getIdentifierAPI().find(currWebAsset);
		CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
		// gets the identifier for this asset
		APILocator.getVersionableAPI().setDeleted(currWebAsset, false);

		HibernateUtil.addCommitListener(ident.getId(), ()-> {
			Try.run(()->systemEventsAPI.pushAsync(SystemEventType.UN_ARCHIVE_SITE, new Payload(currWebAsset.getMap(), Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(currWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
		});
	}

	public static boolean unPublishAsset(WebAsset currWebAsset, String userId, Treeable parent) throws DotStateException, DotDataException, DotSecurityException {
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
				if(currWebAsset instanceof Template) {
					FactoryLocator.getTemplateFactory().save(Template.class.cast(livewebasset));
				} else {
					HibernateUtil.saveOrUpdate(livewebasset);
				}

				if ((livewebasset.getInode() != workingwebasset.getInode())) {
			        APILocator.getVersionableAPI().setLocked(workingwebasset, false, null);
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
						CacheLocator.getNavToolCache().removeNav(host.getIdentifier(), parentFolder.getInode());
					}
				}

				HibernateUtil.addCommitListener(identifier.getId(), ()-> {
					Try.run(()->systemEventsAPI.pushAsync(SystemEventType.UN_PUBLISH_LINK, new Payload(currWebAsset, Visibility.EXCLUDE_OWNER,
							new ExcludeOwnerVerifierBean(currWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
				});
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
	public static WebAsset saveAsset(final WebAsset newWebAsset, final Identifier id) throws Exception {
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

		HibernateUtil.addCommitListener(id.getId(), ()-> {

			final SystemEventType systemEventType = newWebAsset.getInode() == null ? SystemEventType.SAVE_LINK : SystemEventType.UPDATE_LINK;
			Try.run(()->systemEventsAPI.pushAsync(systemEventType, new Payload(newWebAsset, Visibility.EXCLUDE_OWNER,
					new ExcludeOwnerVerifierBean(newWebAsset.getModUser(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
		});

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

			String type = ((Inode) c.getDeclaredConstructor().newInstance()).getType();
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

			List<WebAsset> toReturn = new ArrayList<>();
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

	public static void changeAssetMenuOrder(Folder folder, int newValue, User user) throws ActionException, DotDataException {

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(folder, PERMISSION_WRITE, user)) {
			throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

		if (newValue == -1) {
			folder.setShowOnMenu(false);
		} else {
			folder.setShowOnMenu(true);
		}
		folder.setSortOrder(newValue);

		final Identifier ident=APILocator.getIdentifierAPI().find(folder.getIdentifier());
		CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());

		try {
			APILocator.getFolderAPI().save(folder, user, false);
		} catch (DotSecurityException e) {
			Logger.warnAndDebug(WebAssetFactory.class, "changeAssetMenuOrder failed for folder :" + folder.getName(), e);
		}
	}

	public static void changeAssetMenuOrder(Inode asset, int newValue, User user) throws ActionException, DotDataException {

		// Checking permissions
		if (!permissionAPI.doesUserHavePermission(asset, PERMISSION_WRITE, user))
			throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);

        if (asset instanceof WebAsset) {
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
			List<Versionable> webAssetList = new ArrayList<>();
			if(currWebAsset instanceof Container)
			{
			    new ContainerLoader().invalidate((Container)currWebAsset);
				webAssetList = APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
			}
			else if(currWebAsset instanceof Template)
			{
				new TemplateLoader().invalidate((Template)currWebAsset);
				webAssetList = APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
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
			List<Tree> treeList = new ArrayList<>();
			treeList.addAll(TreeFactory.getTreesByChild(identifier.getInode()));
			treeList.addAll(TreeFactory.getTreesByParent(identifier.getInode()));
			for(Tree tree : treeList)
			{
				TreeFactory.deleteTree(tree);
			}
			//### END Get and delete the tree entries ###

			//### Get and delete the multitree entries ###
			List<MultiTree> multiTrees = new ArrayList<>();
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

			final Map webAssetMap = currWebAsset.getMap();
			HibernateUtil.addCommitListener(identifier.getId(), ()-> {
				Try.run(()->systemEventsAPI.pushAsync(SystemEventType.DELETE_LINK, new Payload(webAssetMap, Visibility.EXCLUDE_OWNER,
						new ExcludeOwnerVerifierBean(user.getUserId(), PermissionAPI.PERMISSION_READ, Visibility.PERMISSION))));
			});
		} else {

			throw new Exception(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		return returnValue;
	}

	/**
	 * Returns a list of Assets based on the specified filtering parameters. Several legacy portlets in the dotCMS UI
	 * (specially the ones built with JSP files and Dojo) use this method to display a list of different types of
	 * objects, such as: Containers, Menu Links, etc.
	 *
	 * @param siteId          The ID of the Site that the requested assets will be retrieved from.
	 * @param roles           list of {@link Role} objects associated to the User that is calling this method.
	 * @param includeArchived If set to {@code true}, archived Assets will be added to the result set.
	 * @param limit           The maximum number of elements added to the result set, for pagination purposes.
	 * @param offset          The offset value for elements added to the result set, for pagination purposes.
	 * @param orderBy         The criterion used to sort the result set.
	 * @param tableName       The type of Asset that is being requested. For more information: {@link AssetType}.
	 * @param parent          For Containers only: The ID or Velocity Variable Name of the Content Type that must be
	 *                        associated to the Containers that will be returned.
	 * @param query           The filtering criterion for the result set.
	 * @param user            The {@link User} executing this action.
	 *
	 * @return The {@link PaginatedArrayList} of the requested dotCMS Assets.
	 *
	 * @throws DotIdentifierStateException An error occurred when processing Identifier-related information.
	 * @throws DotDataException            An error occurred when interacting with the data source.
	 * @throws DotSecurityException        The specified User does not have the required permissions to perform this
	 *                                     action.
	 */
	public PaginatedArrayList<PermissionAsset> getAssetsAndPermissions(final String siteId, final Role[] roles,
																	   final boolean includeArchived, final int limit,
																	   final int offset, String orderBy,
																	   final String tableName, String parent,
																	   String query, final User user) throws
			DotIdentifierStateException, DotDataException, DotSecurityException {
		final PaginatedArrayList<PermissionAsset> paginatedEntries = new PaginatedArrayList<>();
		long totalCount;

		parent = SQLUtil.sanitizeParameter(parent);
		query = SQLUtil.sanitizeParameter(query);
		orderBy = SQLUtil.sanitizeSortBy(orderBy);

		final AssetType type = AssetType.getObject(tableName.toUpperCase());
		List<? extends Permissionable> elements = null;
		final Map<String,Object> params = new HashMap<>();
		if(UtilMethods.isSet(query) && !type.equals(AssetType.TEMPLATE)){
			params.put("title", query.toLowerCase().replace("\'","\\\'"));
		}
		try {
			if (type.equals(AssetType.CONTAINER)){
				if(APILocator.getIdentifierAPI().isIdentifier(query)){
					params.put("identifier", query);
				}
				final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
						.includeArchived(includeArchived)
						.includeSystemContainer(Boolean.TRUE)
						.filteringCriterion(params)
						.siteId(siteId)
						.contentTypeIdOrVar(parent)
						.offset(offset)
						.limit(limit)
						.orderBy(orderBy).build();
				elements = containerAPI.findContainers(user, searchParams);
			} else if (type.equals(AssetType.TEMPLATE)){
				params.put("filter",query.toLowerCase());
				elements = templateAPI.findTemplates(user, includeArchived, params, siteId, null, null,  parent, offset, limit, orderBy);
			} else if (type.equals(AssetType.LINK)){
				elements = linksAPI.findLinks(user, includeArchived, params, siteId, null, null, parent, offset, limit, orderBy);
			}
		} catch (final DotSecurityException | DotDataException e) {
			Logger.warn(WebAssetFactory.class, String.format(
					"An error occurred when User '%s' tried to retrieve assets and permissions from '%s' in Site '%s': %s",
					user.getUserId(), tableName, siteId, e.getMessage()), e);
		}

		totalCount =  elements!=null?((PaginatedArrayList)elements).getTotalResults():0;
	    final Iterator<? extends Permissionable> elementsIter = elements.iterator();

		while (elementsIter.hasNext()) {
			final Permissionable asset = elementsIter.next();
			final PermissionAsset permAsset = new PermissionAsset();
			Folder folderParent = null;
			Host site = null;
			if (asset instanceof WebAsset) {
				// For WebAsset objects
				final WebAsset webAsset = (WebAsset) asset;
				if (!WebAssetFactory.isAbstractAsset(webAsset)) {
					folderParent = APILocator.getFolderAPI()
							.findParentFolder(webAsset, user, false);
				}
				try {
					site = APILocator.getHostAPI().findParentHost(webAsset,
							user, false);
				} catch (final DotDataException e1) {
					Logger.error(WebAssetFactory.class,
							String.format("An error occurred when finding Site for WebAsset '%s': %s",
									webAsset.getIdentifier(), e1.getMessage()), e1);
				} catch (final DotSecurityException e1) {
					Logger.error(WebAssetFactory.class, String.format(
							"User '%s' does not have the required permissions to get information from WebAsset '%s': %s",
							user.getUserId(), webAsset.getIdentifier(), e1.getMessage()), e1);
				}
				if (site != null) {
					if (site.isArchived()) {
						continue;
					}
				}
				if (!WebAssetFactory.isAbstractAsset(webAsset)) {
					permAsset.setPathToMe(APILocator.getIdentifierAPI()
							.find(folderParent.getIdentifier()).getPath());
				} else {
					permAsset.setPathToMe(StringPool.BLANK);
				}
				if (asset instanceof IHTMLPage) {
					permAsset.setPermissionableAsset(asset);
				} else {
					permAsset.setAsset(webAsset);
				}
			} else {
				// For HTMLPage and IHTMLPage objects
				final IHTMLPage page = (IHTMLPage) asset;
				final String pathToFolderParent = APILocator.getIdentifierAPI().find(page).getParentPath();
				folderParent = APILocator.getFolderAPI()
						.findParentFolder((Treeable) asset, user, false);
				try {
					final String pageHostId = APILocator.getIdentifierAPI()
							.find(page.getIdentifier()).getHostId();
					site = APILocator.getHostAPI().find(pageHostId, user, false);
				} catch (final DotDataException e1) {
					Logger.error(WebAssetFactory.class,
							String.format("An error occurred when finding Site for HTML Page '%s' [%s]: %s",
									page.getPageUrl(), page.getIdentifier(), e1.getMessage()), e1);
				} catch (final DotSecurityException e1) {
					Logger.error(WebAssetFactory.class, String.format(
							"User '%s' does not have the required permissions to get information from HTML Page '%s' [%s]: %s",
							user.getUserId(), page.getPageUrl(), page.getIdentifier(), e1.getMessage()), e1);
				}
				if (site != null) {
					if (site.isArchived()) {
						continue;
					}
				}
				if (folderParent != null) {
					permAsset.setPathToMe(APILocator.getIdentifierAPI()
							.find(folderParent.getIdentifier()).getPath());
				} else {
					permAsset.setPathToMe(pathToFolderParent);
				}
				permAsset.setPermissionableAsset(page);
			}
			List<Integer> permissions = new ArrayList<>();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(asset, roles, user);
			} catch (final DotDataException e) {
				Logger.error(WebAssetFactory.class,
						String.format("Permissions from Permissionable '%s' for User '%s' could not be retrieved: %s",
								asset.getPermissionId(), user.getUserId(), e.getMessage()), e);
			}
			permAsset.setPermissions(permissions);
			paginatedEntries.add(permAsset);
		}

		paginatedEntries.setTotalResults(totalCount);
		return paginatedEntries;
	}

}
