/**
 *
 */
package com.dotmarketing.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
		if(!UtilMethods.isSet(webasset.getInode()))
		    HibernateUtil.save(webasset);

		// create new identifier, without URI
		Identifier id = APILocator.getIdentifierAPI().createNew(webasset, (Host)null);
		id.setOwner(userId);
		//HibernateUtil.saveOrUpdate(id);
		APILocator.getIdentifierAPI().save(id);

		webasset.setIdentifier(id.getId());
		save(webasset);
		APILocator.getVersionableAPI().setWorking(webasset);
	}

	protected void createAsset(WebAsset webasset, String userId, Inode parent) throws DotDataException, DotSecurityException {
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);

		// persists the webasset
		if(!UtilMethods.isSet(webasset.getInode()))
            HibernateUtil.save(webasset);

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
		//save(webasset);

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
		newWebAsset.setIdentifier(id.getId());

		save(newWebAsset);

		APILocator.getVersionableAPI().setWorking(newWebAsset);

		return newWebAsset;
	}

	protected void createAsset(WebAsset webasset, String userId, Inode parent, Identifier identifier, boolean working) throws DotDataException, DotStateException, DotSecurityException {
		if(!UtilMethods.isSet(webasset.getInode()))
		    webasset.setInode(UUID.randomUUID().toString());
		webasset.setModDate(new java.util.Date());
		webasset.setModUser(userId);

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
     * This method totally removes an asset from the cms.
     *
     * @param currWebAsset
     * @return true if the asset was sucessfully removed.
     * @throws DotSecurityException
     * @throws DotDataException
     */
	public static boolean deleteAsset(WebAsset currWebAsset) throws DotSecurityException, DotDataException{
		boolean returnValue = false;
		try
		{
			if (!UtilMethods.isSet(currWebAsset) || !InodeUtils.isSet(currWebAsset.getInode()))
			{
				return returnValue;
			}

			PermissionAPI permissionAPI = APILocator.getPermissionAPI();


			CacheLocator.getIdentifierCache().removeFromCacheByVersionable(currWebAsset);

			//### END Delete the entry from cache ###


			//Get the identifier of the webAsset
			Identifier identifier = APILocator.getIdentifierAPI().find(currWebAsset);

			//### Get and delete the webAsset ###
			List<Versionable> webAssetList = new ArrayList<Versionable>();
			webAssetList.addAll(APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false));
			if(currWebAsset instanceof Container)
			{
                //We need to delete also the references to template_containers
                DotConnect dc = new DotConnect();
                dc.setSQL("DELETE FROM template_containers WHERE container_id = ?");
                dc.addParam(currWebAsset.getIdentifier());
                dc.loadResult();

				ContainerServices.unpublishContainerFile((Container)currWebAsset);
				CacheLocator.getContainerCache().remove(currWebAsset.getInode());
			}
			else if(currWebAsset instanceof Template)
			{
				TemplateServices.unpublishTemplateFile((Template)currWebAsset);
				APILocator.getTemplateAPI().associateContainers(new ArrayList<Container>(), (Template)currWebAsset);
				CacheLocator.getTemplateCache().remove(currWebAsset.getInode());
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
			if (currWebAsset instanceof Container)
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
		catch(DotHibernateException ex){
			Logger.warn(BaseWebAssetAPI.class, ex.getMessage(),ex);
			throw ex;
		}
		catch(DotDataException ex){
			Logger.warn(BaseWebAssetAPI.class, ex.getMessage(),ex);
			throw ex;
		}
		catch(DotSecurityException ex){
			Logger.warn(BaseWebAssetAPI.class, ex.getMessage(),ex);
			throw ex;
		}
		return returnValue;
	}

	private static VersionInfo getVersionInfo(WebAsset currWebAsset,
			Identifier identifier, List<Versionable> webAssetList, String type)
			throws DotHibernateException {
		VersionInfo auxVersionInfo;
		Class clazz = UtilMethods.getVersionInfoType(type);
		HibernateUtil dh = new HibernateUtil(clazz);
		dh.setQuery("from "+clazz.getName()+" where identifier=?");
		dh.setParam(identifier);
		Logger.debug(BaseWebAssetAPI.class, "getVersionInfo query: "+dh.getQuery());
		auxVersionInfo=(VersionInfo)dh.load();

		if(UtilMethods.isSet(auxVersionInfo) && UtilMethods.isSet(auxVersionInfo.getIdentifier())) {
		    clazz = InodeUtils.getClassByDBType(type);
		    dh = new HibernateUtil(clazz);
			dh.setQuery("from inode in class " + clazz.getName() + " where inode.identifier = ? and inode.type='"+type+"' order by mod_date desc");
			dh.setParam(currWebAsset.getIdentifier());
			Logger.debug(BaseWebAssetAPI.class, "findAllVersions query: " + dh.getQuery());
			webAssetList.addAll( (List<Versionable>) dh.list() );
		}
		return auxVersionInfo;
	}

	@WrapInTransaction
	public int deleteOldVersions(final Date olderThan, final String type) throws DotDataException {
        DotConnect dc = new DotConnect();

        //Setting the date to olderThan 00:00:00.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(olderThan);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();

		String tableName = Inode.Type.valueOf(type.toUpperCase()).getTableName();
		String versionInfoTable = Inode.Type.valueOf(type.toUpperCase()).getVersionTableName();

        //Get the count of the tableName before deleting.
        String countSQL = "select count(*) as count from " + tableName;
        dc.setSQL(countSQL);
        List<Map<String, String>> result = dc.loadResults();
        int before = Integer.parseInt(result.get(0).get("count"));
        int after = before;

        String condition =  " mod_date < ? " +
                            " and not exists (select 1 from " + versionInfoTable +
                                                " where working_inode = " + tableName + ".inode" +
                                                " or live_inode = " + tableName + ".inode)";

        String inodesToDeleteSQL = "select inode from " + tableName + " where " + condition;

        //Get the list of inodes to delete.
        dc.setSQL(inodesToDeleteSQL);
        dc.addParam(date);
        List<Map<String, Object>> resultInodes = dc.loadObjectResults();

        //No need to run all the SQL is we don't get any inodes from the condition.
        if(!resultInodes.isEmpty()){
            List<String> inodesToDelete = new ArrayList<>();
            int truncateAt = 100;

            //Fill inodesToDelete where each inode from the result.
            for (Map<String, Object> row : resultInodes) {
                inodesToDelete.add(row.get("inode").toString());
            }

            //We want to create lists of 100 inodes.
			List<List<String>> inodesToDeleteMatrix = Lists.partition(inodesToDelete, truncateAt);

            //These are all the queries we want to run involving the inodes.
            List<String> queries = Lists.newArrayList("delete from tree where child in (?)",
                    "delete from tree where parent in (?)",
                    "delete from container_structures where container_inode in (?)",
                    "delete from " + tableName + " where inode in (?)");

            //For each query we will run sets of 100 inodes at a time.
            for (String query : queries) {
                for (List<String> inodes : inodesToDeleteMatrix) {
                    //Create (?,?,?...) string depending of the number of inodes.
                    String parameterPlaceholders = DotConnect.createParametersPlaceholder(inodes.size());
                    //Replace '?' in the query, with the correct number of '?'s.
                    dc.setSQL(query.replace("?", parameterPlaceholders));

                    for (String inode : inodes) {
                        dc.addParam(inode);
                    }

                    dc.loadResult();
                }
            }

            //Clean inode table.
            String deleteInodesSQL = "delete from inode " +
                    "where type = ? " +
                    "and idate < ? " +
                    "and inode not in (select inode from " + tableName + ")";
            dc.setSQL(deleteInodesSQL);
            dc.addParam(type);
            dc.addParam(date);
            dc.loadResult();

            //Get the count of the tableName after deleting.
            dc.setSQL(countSQL);
            result = dc.loadResults();
            after = Integer.parseInt(result.get(0).get("count"));
        }

	    return before - after;
	}
}
