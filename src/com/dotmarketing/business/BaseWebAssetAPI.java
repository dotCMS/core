/**
 * 
 */
package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author jtesser
 * All methods in the BaseWebAssetAPI should be protected or private. The BaseWebAssetAPI is intended to be extended by other APIs for WebAsset Objects.
 * This api will eventually fade out when all web assets move to content
 *  
 */
public abstract class BaseWebAssetAPI extends BaseInodeAPI {


	/**
	 * Save the asset.
	 * 
	 * @param currWebAsset
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 * @throws DotStateException 
	 */
	protected abstract void save(WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException; 
	
	protected void unLockAsset(WebAsset currWebAsset) throws DotDataException, DotStateException, DotSecurityException {
		// unlocks current working asseted 
		APILocator.getVersionableAPI().setLocked(currWebAsset, false, null);
	}
	
	protected void createAsset(WebAsset webasset, String userId) throws DotDataException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		save(webasset);

		// create new identifier, without URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, (Host)null);
		id.setOwner(userId);
		//HibernateUtil.saveOrUpdate(id);
		APILocator.getIdentifierAPI().save(id);
		
		webasset.setIdentifier(id.getId());
		APILocator.getVersionableAPI().setWorking(webasset);
	}
	
	protected void createAsset(WebAsset webasset, String userId, Inode parent) throws DotDataException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, (Folder) parent);
		id.setOwner(userId);
		// set the identifier on the inode for future reference.
		// and for when we get rid of identifiers all together
		//HibernateUtil.saveOrUpdate(id);
		APILocator.getIdentifierAPI().save(id);
		
		webasset.setIdentifier(id.getId());
		// persists the webasset
		save(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		  parent.addChild(webasset);

		// create new identifier, with the URI
		APILocator.getVersionableAPI().setWorking(webasset);
	}
	
	protected void createAsset(WebAsset webasset, String userId, Identifier identifier, boolean working) throws DotDataException, DotStateException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		save(webasset);

		// adds asset to the existing identifier
		//identifier.addChild(webasset);
		//webasset.addParent(identifier);
		webasset.setIdentifier(identifier.getInode());

		save(webasset);

		if(working)
		    APILocator.getVersionableAPI().setWorking(webasset);
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
	 * @throws DotDataException
	 *             The method throw an exception when the new asset identifier
	 *             or the working folder cannot be found.
	 * @throws DotSecurityException 
	 * @throws DotStateException 
	 */
	protected WebAsset saveAsset(WebAsset newWebAsset, Identifier id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException, DotSecurityException {
		
		if (!InodeUtils.isSet(id.getInode())) {
			throw new DotDataException("Identifier not found!");
		}
		/*WebAsset oldWebAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(id, user, respectAnonPermissions);
		
		if (!InodeUtils.isSet(oldWebAsset.getInode())) {
			throw new DotDataException("Working copy of id: " + id.getAssetType() + ":" + id.getInode() + " not found!");
		}*/
		//oldWebAsset.setWorking(false);
		//newWebAsset.setWorking(true);
		
		//save(oldWebAsset);
		save(newWebAsset);
		newWebAsset.setIdentifier(id.getId());
		APILocator.getVersionableAPI().setWorking(newWebAsset);

		return newWebAsset;
	}
	
	protected void createAsset(WebAsset webasset, String userId, Inode parent, Identifier identifier, boolean working) throws DotDataException, DotStateException, DotSecurityException {
		webasset.setInode(UUID.randomUUID().toString());
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);
		// persists the webasset
		save(webasset);

		// adds the webasset as child of the folder or parent inode
		if(!parent.getType().equalsIgnoreCase("folder"))
		   parent.addChild(webasset);

		// adds asset to the existing identifier
		//identifier.addChild(webasset);
		//webasset.addParent(identifier);
		webasset.setIdentifier(identifier.getInode());

		save(webasset);
		
		if(working)
		    APILocator.getVersionableAPI().setWorking(webasset);
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
	 * @throws DotDataException
	 *             The method throw an exception when the new asset identifier
	 *             or the working folder cannot be found.
	 */
	
	/*
	protected WebAsset saveAsset(WebAsset newWebAsset, Identifier id) throws DotDataException {
		if (!InodeUtils.isSet(id.getInode())) {
			throw new DotDataException("Web asset Identifier not found!");
		}
		WebAsset currWebAsset = null;
		
		// gets the current working asset
		currWebAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(id, newWebAsset.getClass());
		
		if (!InodeUtils.isSet(currWebAsset.getInode())) {
			throw new DotDataException("Working copy not found!");
		}
		
		WebAsset workingAsset = null;
		
		try {
			workingAsset = swapAssets(currWebAsset, newWebAsset);
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}

		// Check
		List workingAssets = null;
		// gets the current working asset
			workingAssets = APILocator.getVersionableAPI().findWorkingVersion(id, workingAsset.getClass());

		// if there is more than one working asset
		if (workingAssets.size() > 1) {
			Iterator iter = workingAssets.iterator();
			while (iter.hasNext()) {
				WebAsset webAsset = (WebAsset) iter.next();
				if (webAsset.getInode() != workingAsset.getInode()) {
					webAsset.setWorking(false);
					save(webAsset);
				}
			}
		}

		return workingAsset;
	}
	*/

	protected boolean isAbstractAsset(WebAsset asset) {
		if (asset instanceof Container || asset instanceof Template)
			return true;
		return false;
	}
	
	/**
	 * This method totally removes an asset from the cms
	 * @param currWebAsset
	 * @param user If the user is passed (not null) the system will check for write permission of the user in the asset
	 * @param respectFrontendRoles
	 * @return true if the asset was sucessfully removed
	 * @exception Exception
	 */
	public static boolean deleteAsset(WebAsset currWebAsset) {
		boolean returnValue = false;
		try
		{
			if (!UtilMethods.isSet(currWebAsset) || !InodeUtils.isSet(currWebAsset.getInode()))
			{
				return returnValue;
			}
			
			PermissionAPI permissionAPI = APILocator.getPermissionAPI();
			
			//### Delete the IDENTIFIER entry from cache ###
			LiveCache.removeAssetFromCache(currWebAsset);
			WorkingCache.removeAssetFromCache(currWebAsset);
			CacheLocator.getIdentifierCache().removeFromCacheByVersionable(currWebAsset);

			//### END Delete the entry from cache ###


			//Get the identifier of the webAsset
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);

			//### Get and delete the webAsset ###
			List<Versionable> webAssetList = new ArrayList<Versionable>();
			webAssetList.addAll(APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false));
			if(currWebAsset instanceof Container)
			{
				ContainerServices.unpublishContainerFile((Container)currWebAsset);
				CacheLocator.getContainerCache().remove(currWebAsset.getInode());
			}
			else if(currWebAsset instanceof HTMLPage)
			{
				PageServices.unpublishPageFile((HTMLPage)currWebAsset);
				if(RefreshMenus.shouldRefreshMenus((HTMLPage)currWebAsset)){
					RefreshMenus.deleteMenu(currWebAsset);
				}
				
				CacheLocator.getHTMLPageCache().remove((HTMLPage)currWebAsset);
			}
			else if(currWebAsset instanceof Template)
			{
				TemplateServices.unpublishTemplateFile((Template)currWebAsset);

				CacheLocator.getTemplateCache().remove(currWebAsset.getInode());
			}
			else if(currWebAsset instanceof Link)
			{

				VirtualLinksCache.removePathFromCache(((Link)currWebAsset).getUrl());
			}
			else if(currWebAsset instanceof File)
			{

				APILocator.getFileAPI().invalidateCache((File)currWebAsset);
				if(RefreshMenus.shouldRefreshMenus((File)currWebAsset)){
					RefreshMenus.deleteMenu(currWebAsset);
				}
			}
			APILocator.getVersionableAPI().deleteVersionInfo(currWebAsset.getVersionId());
			for(Versionable webAsset : webAssetList)
			{
				//Delete the permission of each version of the asset
				permissionAPI.removePermissions((WebAsset) webAsset);
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
			if (currWebAsset instanceof Container || currWebAsset instanceof HTMLPage)
			{
				multiTrees = MultiTreeFactory.getMultiTree(identifier);
			}
			if(UtilMethods.isSet(multiTrees))
			{
				for(MultiTree multiTree : multiTrees)
				{
					MultiTreeFactory.deleteMultiTree(multiTree);
				}
			}
			//### END Get and delete the multitree entries ###



			//### Delete the Identifier ###
			if(identifier!=null && UtilMethods.isSet(identifier.getId()))
			    APILocator.getIdentifierAPI().delete(identifier);
			
			//### Delete the Identifier ###
			returnValue = true;
		}
		catch(Exception ex)
		{
			Logger.warn(BaseWebAssetAPI.class, ex.getMessage(),ex);
			throw ex;
		}
		finally
		{
			return returnValue;
		}
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsAndPermissionsPerRoleAndConditionWithParent(String condition, Class assetsClass, String parentId, boolean showDeleted, User user) {
		return WebAssetFactory.getAssetsCountPerConditionWithPermissionWithParent(condition, assetsClass, 100000, 0, parentId, showDeleted, user);
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(String condition, Class c, User user) {
		return getCountAssetsPerConditionWithPermission(condition, c, null, user);
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(String condition, Class c, String parent, User user) {
		return WebAssetFactory.getAssetsCountPerConditionWithPermission(condition, c, -1, 0, parent, user);
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsAndPermissionsPerRoleAndConditionWithParent(String hostId, String condition, Class assetsClass, String parentId, boolean showDeleted, User user) {		
		return WebAssetFactory.getAssetsCountPerConditionWithPermissionWithParent(hostId, condition, assetsClass, 100000, 0, parentId, showDeleted, user);
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(Host host, String condition, Class c, User user) {
		return getCountAssetsPerConditionWithPermission(host.getIdentifier(), condition, c, user);
	}

	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(String hostId, String condition, Class c, User user) {
		return getCountAssetsPerConditionWithPermission(hostId, condition, c, null, user);
	}

	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(Host host, String condition, Class c, String parent, User user) {
		return getCountAssetsPerConditionWithPermission(host.getIdentifier(), condition, c, parent, user);
	}
	
	@SuppressWarnings("unchecked")
	public int getCountAssetsPerConditionWithPermission(String hostId, String condition, Class c, String parent, User user) {
		return WebAssetFactory.getAssetsCountPerConditionWithPermission(hostId, condition, c, -1, 0, parent, user);
	}
}
