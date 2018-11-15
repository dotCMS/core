package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.*;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;

import java.util.*;

import static com.dotmarketing.util.StringUtils.builder;

public class ContainerFactoryImpl implements ContainerFactory {

	static IdentifierCache identifierCache    = CacheLocator.getIdentifierCache();
	static ContainerCache  containerCache     = CacheLocator.getContainerCache();
	private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private final FolderAPI     folderAPI     = APILocator.getFolderAPI();
    private final FileAssetAPI  fileAssetAPI  = APILocator.getFileAssetAPI();
    private final HostAPI       hostAPI       = APILocator.getHostAPI();
	private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();


	public void save(final Container container) throws DotDataException {
		HibernateUtil.save(container);
		containerCache.remove(container);
	}

	public void save(final Container container, final String existingId) throws DotDataException {
		HibernateUtil.saveWithPrimaryKey(container, existingId);
		containerCache.remove(container);
	}

	@SuppressWarnings("unchecked")
	public List<Container> findContainersUnder(final Host parentPermissionable) throws DotDataException {

		final List<Container> containers = new ArrayList<>();
		final String tableName 			 = Type.CONTAINERS.getTableName();
		final StringBuilder sql = new StringBuilder();

		sql.append("SELECT ")
			.append(tableName)
			.append(".*, dot_containers_1_.* from ")
			.append(tableName)
			.append(", inode dot_containers_1_, identifier ident, container_version_info vv where vv.working_inode=")
			.append(tableName)
			.append(".inode and ")
			.append(tableName)
			.append(".inode = dot_containers_1_.inode and vv.identifier = ident.id and host_inode = '")
			.append(parentPermissionable.getIdentifier() )
			.append('\'');

		try {

			containers.addAll(this.findHostContainers(parentPermissionable, APILocator.systemUser()));
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}

		containers.addAll(TransformerLocator.createContainerTransformer
				(new DotConnect().setSQL(sql.toString()).loadObjectResults()).asList());

		return containers;
	}

	@SuppressWarnings("unchecked")
	public List<Container> findAllContainers() throws DotDataException {

		final List<Container> containers = new ArrayList<>();

		try {
			containers.addAll(this.findAllHostFolderAssetContainers());
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}

		containers.addAll(this.findAllHostDataBaseContainers());

		return containers;
	}

	private List<Container> findAllHostDataBaseContainers() throws DotDataException {

		final String tableName 			 = Type.CONTAINERS.getTableName();
		final StringBuilder sql = new StringBuilder()
				.append("SELECT ")
				.append(tableName)
				.append(".*, dot_containers_1_.* from ")
				.append(tableName)
				.append(", inode dot_containers_1_, container_version_info vv where vv.working_inode= ")
				.append(tableName)
				.append(".inode and ")
				.append(tableName)
				.append(".inode = dot_containers_1_.inode and dot_containers_1_.type='containers' order by ")
				.append(tableName)
				.append(".title");

		return TransformerLocator.createContainerTransformer
				(new DotConnect().setSQL(sql.toString()).loadObjectResults()).asList();
	}

	@Override
    @SuppressWarnings("unchecked")
    public Container find(final String inode) throws DotDataException, DotSecurityException {
      Container container = CacheLocator.getContainerCache().get(inode);
      //If it is not in cache.
      if(container == null){
          
          //Get container from DB.
          HibernateUtil dh = new HibernateUtil(Container.class);
          
          Container containerAux= (Container) dh.load(inode);

          if(InodeUtils.isSet(containerAux.getInode())){
              //container is the one we are going to return.
              container = containerAux;
              //Add to cache.
              CacheLocator.getContainerCache().add(container);
          }
          
      }
      
      return container;
    }

	@Override
	public Container getLiveContainerByFolderPath(final String path, final Host host, final User user,
												  final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		return this.getContainerByFolder(host, this.folderAPI.findFolderByPath(path, host, user, respectFrontEndPermissions), user,true);
	}

    @Override
    public Container getWorkingContainerByFolderPath(final String path, final Host host, final User user,
                                                     final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

        return this.getContainerByFolder(host, this.folderAPI.findFolderByPath(path, host, user, respectFrontEndPermissions), user,false);
    }


    @Override
    public Container getContainerByFolder(final Host host, final Folder folder, final User user, final boolean showLive) throws DotSecurityException, DotDataException {

        if (!this.isValidContainerPath (folder) ||
				!hasContainerAsset(host, folder)) {

        	throw new NotFoundInDbException("On getting the container by folder, the folder: " + folder.getPath() +
					" is not valid, it must be under: " + Constants.CONTAINER_FOLDER_PATH + " and must have a child file asset called: " +
					Constants.CONTAINER_META_INFO_FILE_NAME);
		}

        return ContainerByFolderAssetsUtil.getInstance().fromAssets (folder, this.findContainerAssets(folder, user, showLive), showLive);
    }

	private List<FileAsset> findContainerAssets(final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {
		return this.fileAssetAPI.findFileAssetsByFolder(folder, null, showLive, user, false);
	}

	private boolean hasContainerAsset(final Host host, final Folder folder) {
		try {

			final Identifier identifier = this.identifierAPI.find(host, builder(folder.getPath(),
					 Constants.CONTAINER_META_INFO_FILE_NAME).toString());
			return null != identifier && UtilMethods.isSet(identifier.getId());
		} catch (Exception  e) {
			return false;
		}
	}

	private boolean isValidContainerPath(final Folder folder) {
		return null != folder && UtilMethods.isSet(folder.getPath()) && folder.getPath().contains(Constants.CONTAINER_FOLDER_PATH);
	}

	@Override
	public List<Container> findContainers(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String hostId,
			final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderByParam) throws DotSecurityException,
			DotDataException {

		final ContentTypeAPI contentTypeAPI        = APILocator.getContentTypeAPI(user);
		final StringBuffer conditionBuffer         = new StringBuffer();
		final List<Object> paramValues 			   = this.getConditionParametersAndBuildConditionQuery(params, conditionBuffer);
		final PaginatedArrayList<Container> assets = new PaginatedArrayList<Container>();
		final List<Permissionable> toReturn        = new ArrayList<>();
		int     internalLimit                      = 500;
		int     internalOffset                     = 0;
		boolean done                               = false;
		String  orderBy                            = orderByParam;
		final StringBuilder query 				   = new StringBuilder().append("select asset.*, inode.* from ")
				.append(Type.CONTAINERS.getTableName()).append(" asset, inode, identifier, ")
				.append(Type.CONTAINERS.getVersionTableName()).append(" vinfo");

		this.buildFindContainersQuery(includeArchived, hostId, inode,
				identifier, parent, contentTypeAPI, query);

		if(!UtilMethods.isSet(orderBy)) {
			orderBy = "mod_date desc";
		}

		List<Container> resultList;
		final DotConnect dc  = new DotConnect();
		int countLimit 		 = 100;

		try {

			query.append(conditionBuffer.toString());
			query.append(" order by asset.");
			query.append(orderBy);
			dc.setSQL(query.toString());

			if(paramValues!=null && paramValues.size()>0) {
				for (final Object value : paramValues) {
					dc.addParam((String)value);
				}
			}

			// adding the container from the site browser
			toReturn.addAll(this.findFolderAssetContainers(user, includeArchived,
					params, hostId, inode, identifier, parent, orderByParam));

			while(!done) {

				dc.setStartRow(internalOffset).setMaxRows(internalLimit);
				resultList = TransformerLocator.createContainerTransformer(dc.loadObjectResults()).asList();
				toReturn.addAll(this.permissionAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if(countLimit > 0 && toReturn.size() >= countLimit + offset) {
					done = true;
				} else if(resultList.size() < internalLimit) {
					done = true;
				}

				internalOffset += internalLimit;
			}

			assets.setTotalResults(toReturn.size());

			if(limit!=-1) {
				int from = offset<toReturn.size()?offset:0;
				int pageLimit = 0;
				for(int i=from;i<toReturn.size();i++){
					if(pageLimit<limit){
						assets.add((Container) toReturn.get(i));
						pageLimit+=1;
					}else{
						break;
					}
				}
			} else {
				for(int i=0;i<toReturn.size();i++){
					assets.add((Container) toReturn.get(i));
				}
			}
		} catch (Exception e) {
			Logger.error(ContainerFactoryImpl.class, "findContainers failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return assets;
	}

	private Collection<? extends Permissionable> findFolderAssetContainers(final User user, final boolean includeArchived,
																		   final Map<String, Object> params, final String hostId,
																		   final String inode,  final String identifier,
																		   final String parent, final String orderByParam) throws DotSecurityException, DotDataException {

		// todo: when something happen cache and return an empty collection
	    // todo: we are passing all the parameters but not sure if we need everything
		try {
			final Host host     			 = this.hostAPI.find(hostId, user, false);
			final Folder folder 			 = this.folderAPI.findFolderByPath(Constants.CONTAINER_FOLDER_PATH, host, user, false);
			final List<Folder> subFolders    = this.folderAPI.findSubFolders(folder, user, false); // todo: change this in order to get the information from the index
			final List<Container> containers = this.getFolderContainers(host, user, subFolders);

			// todo: order by???

			return containers;
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	private Collection<Container> findAllHostFolderAssetContainers() throws DotSecurityException, DotDataException {

		final User       user  = APILocator.systemUser();
		// todo: find another strategy in order to be less items
		final List<Host> hosts = this.hostAPI.findAll(user, false);
		final ImmutableList.Builder<Container> containers = new ImmutableList.Builder<>();

		for (final Host host : hosts) {

			containers.addAll(this.findHostContainers(host, user));
		}

		return containers.build();
	}

	private List<Container> findHostContainers(final Host host, final User user) throws DotDataException, DotSecurityException {

		// todo: replace this for ES call, not matter the host by sort by host
		final List<Container> containers = new ArrayList<>();
		final Optional<Folder> folder = this.findContainerFolderByHost(host, user);

		if (folder.isPresent() && UtilMethods.isSet(folder.get().getIdentifier())) {

			final List<Folder> subFolders = this.folderAPI.findSubFolders(folder.get(), user, false);
			containers.addAll(this.getFolderContainers(host, user, subFolders));
		}

		return containers;
	}

	private List<Container> getFolderContainers(final Host host, final User user, final List<Folder> subFolders) throws DotDataException {

		final List<Container> containers = new ArrayList<>();
		for (final Folder subFolder : subFolders) {

			try {
				final Container container = this.getContainerByFolder(host, subFolder, null != user? user: APILocator.systemUser(), false); // todo: check eventually the false.
				containers.add(container);
			} catch (DotSecurityException e) {

				Logger.debug(this, () -> "Does not have permission to read the folder container: " + subFolder.getPath());
			} catch (NotFoundInDbException e) {

                Logger.debug(this, () -> "The folder: " + subFolder.getPath() + ", is not a container");
            }
		}

		return containers;
	}

	private Optional<Folder> findContainerFolderByHost (final Host host, final User user) {

		try {

			return Optional.of(this.folderAPI.findFolderByPath(Constants.CONTAINER_FOLDER_PATH,
					host, user, false));
		} catch (Exception e) {

			return Optional.empty();
		}
	}

	private void buildFindContainersQuery(final boolean includeArchived, final String hostId, final String inode,
										  final String identifier, final String parent, final ContentTypeAPI contentTypeAPI,
										  final StringBuilder query) throws DotSecurityException, DotDataException {

		if(UtilMethods.isSet(parent)) {

			//Search for the given ContentType inode
			final ContentType foundContentType = contentTypeAPI.find(parent);

			if (null != foundContentType && InodeUtils.isSet(foundContentType.inode())) {
				query.append(
						" where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(
								" and exists (select * from container_structures cs where cs.container_id = asset.identifier")
						.append(" and cs.structure_id = '")
						.append(parent)
						.append("' ) ");
			}else {
				query.append(
						" ,tree where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(" and tree.parent = '")
						.append(parent)
						.append("' and tree.child=asset.inode");
			}
		} else {
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}

		query.append(" and vinfo.identifier=identifier.id and vinfo.working_inode=asset.inode ");

		if(!includeArchived) {
			query.append(" and vinfo.deleted=");
			query.append(DbConnectionFactory.getDBFalse());
		}

		if(UtilMethods.isSet(hostId)) {
			query.append(" and identifier.host_inode = '");
			query.append(hostId).append('\'');
		}

		if(UtilMethods.isSet(inode)) {
			query.append(" and asset.inode = '");
			query.append(inode).append('\'');
		}

		if(UtilMethods.isSet(identifier)) {
			query.append(" and asset.identifier = '");
			query.append(identifier);
			query.append('\'');
		}
	}

	private List<Object> getConditionParametersAndBuildConditionQuery(final Map<String, Object> params, final StringBuffer conditionQueryBuffer) {

		List<Object> paramValues = null;

		if(params!=null && params.size()>0) {

			conditionQueryBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;

			for (final Map.Entry<String, Object> entry : params.entrySet()) {

				this.buildConditionParameterAndBuildConditionQuery(entry, paramValues, conditionQueryBuffer,
						(counter==0)?
								Optional.empty():
								Optional.of(" OR"));
				counter+=1;
			}

			conditionQueryBuffer.append(" ) ");
		}
		return paramValues;
	}

	private void buildConditionParameterAndBuildConditionQuery (final Map.Entry<String, Object> entry,
																final List<Object> paramValues,
																final StringBuffer conditionQueryBuffer,
																final Optional<String> prefix) {

		if(entry.getValue() instanceof String){
			if (entry.getKey().equalsIgnoreCase("inode") || entry.getKey()
					.equalsIgnoreCase("identifier")) {

				if (prefix.isPresent()) {
					conditionQueryBuffer.append(prefix.get());
				}
				conditionQueryBuffer.append(" asset.");
				conditionQueryBuffer.append(entry.getKey());
				conditionQueryBuffer.append(" = '");
				conditionQueryBuffer.append(entry.getValue());
				conditionQueryBuffer.append('\'');
			} else {

				if (prefix.isPresent()) {
					conditionQueryBuffer.append(prefix.get());
				}
				conditionQueryBuffer.append(" lower(asset.");
				conditionQueryBuffer.append(entry.getKey());
				conditionQueryBuffer.append(") like ? ");
				paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
			}
		} else {

			if (prefix.isPresent()) {
				conditionQueryBuffer.append(prefix.get());
			}
			conditionQueryBuffer.append(" asset.");
			conditionQueryBuffer.append(entry.getKey());
			conditionQueryBuffer.append(" = ");
			conditionQueryBuffer.append(entry.getValue());
		}
	}

	@Override
	public List<Container> findContainersForStructure(final String structureIdentifier) throws DotDataException {
        return findContainersForStructure(structureIdentifier, false);
    }

	@Override
	public List<Container> findContainersForStructure(final String structureIdentifier,
			final boolean workingOrLiveOnly) throws DotDataException {
		HibernateUtil dh = new HibernateUtil(Container.class);

		StringBuilder query = new StringBuilder();

		query.append("FROM c IN CLASS ");
		query.append(Container.class);
		query.append(" WHERE  exists ( from cs in class ");
		query.append(ContainerStructure.class.getName());
		query.append(" where cs.containerId = c.identifier and cs.structureId = ? ");
		if (workingOrLiveOnly) {
			query.append(" AND EXISTS ( FROM vi IN CLASS ");
			query.append(ContainerVersionInfo.class.getName());
			query.append(" WHERE vi.identifier = c.identifier AND ");
			query.append(" (cs.containerInode = vi.workingInode OR cs.containerInode = vi.liveInode ) ) ");
		}
		query.append(") ");
		dh.setQuery(query.toString());
		dh.setParam(structureIdentifier);
		return dh.list();
	}

	/**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();
        StringBuilder query = new StringBuilder();

        query.append("select distinct(identifier) from ");
		query.append(Inode.Type.CONTAINERS.getTableName());
		query.append(" where mod_user = ?");
        try {
           dc.setSQL(query.toString());
           dc.addParam(userId);
           List<HashMap<String, String>> containers = dc.loadResults();

           query = new StringBuilder();
           query.append("UPDATE ");
           query.append(Inode.Type.CONTAINERS.getTableName());
           query.append(" set mod_user = ? where mod_user = ? ");
           dc.setSQL(query.toString());
           dc.addParam(replacementUserId);
           dc.addParam(userId);
           dc.loadResult();

           dc.setSQL("update container_version_info set locked_by=? where locked_by  = ?");
           dc.addParam(replacementUserId);
           dc.addParam(userId);
           dc.loadResult();
         
           for(HashMap<String, String> ident:containers){
               String identifier = ident.get("identifier");
               if (UtilMethods.isSet(identifier)) {

        			   final VersionInfo info =APILocator.getVersionableAPI().getVersionInfo(identifier);
        			   CacheLocator.getContainerCache().remove(info);

			   }
           }
        } catch (DotDataException e) {
            Logger.error(ContainerFactory.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
	}
}