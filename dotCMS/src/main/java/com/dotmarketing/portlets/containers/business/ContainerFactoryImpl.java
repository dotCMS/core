package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.ConvertToPOJOUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerFactoryImpl implements ContainerFactory {
	static IdentifierCache identifierCache = CacheLocator.getIdentifierCache();
	static ContainerCache containerCache = CacheLocator.getContainerCache();

	public void save(Container container) throws DotDataException {
		HibernateUtil.save(container);
		containerCache.remove(container.getInode());
	}

	public void save(Container container, String existingId) throws DotDataException {
		HibernateUtil.saveWithPrimaryKey(container, existingId);
		containerCache.remove(container.getInode());
	}

	@SuppressWarnings("unchecked")
	public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder sql = new StringBuilder();
		String tableName = Type.CONTAINERS.getTableName();

		sql.append("SELECT ");
		sql.append(tableName);
		sql.append(".* from ");
		sql.append(tableName);
		sql.append(
				", inode dot_containers_1_, identifier ident, container_version_info vv where vv.working_inode=");
		sql.append(tableName);
		sql.append(".inode and ");
		sql.append(tableName);
		sql.append(".inode = dot_containers_1_.inode and vv.identifier = ident.id and host_inode = '");
		sql.append(parentPermissionable.getIdentifier() );
		sql.append('\'');
		dc.setSQL(sql.toString());

		try {
			return ConvertToPOJOUtil.convertDotConnectMapToContainer(dc.loadResults());
		} catch (ParseException e) {
			throw new DotDataException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Container> findAllContainers() throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder sql = new StringBuilder();
		String tableName = Type.CONTAINERS.getTableName();

		sql.append("SELECT ");
		sql.append(tableName);
		sql.append(".* from ");
		sql.append(tableName);
		sql.append(", inode dot_containers_1_, container_version_info vv where vv.working_inode= ");
		sql.append(tableName);
		sql.append(".inode and ");
		sql.append(tableName);
		sql.append(".inode = dot_containers_1_.inode and dot_containers_1_.type='containers' order by ");
		sql.append(tableName);
		sql.append(".title");
		dc.setSQL(sql.toString());

		try {
			return ConvertToPOJOUtil.convertDotConnectMapToContainer(dc.loadResults());
		} catch (ParseException e) {
			throw new DotDataException(e);
		}
	}
    @Override
    @SuppressWarnings("unchecked")
    public Container find(String inode) throws DotDataException, DotSecurityException {
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
	public List<Container> findContainers(User user, boolean includeArchived,
			Map<String, Object> params, String hostId,String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {

		ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

		PaginatedArrayList<Container> assets = new PaginatedArrayList<Container>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		StringBuffer conditionBuffer = new StringBuffer();

		List<Object> paramValues =null;
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(counter==0){
					if(entry.getValue() instanceof String){
						if (entry.getKey().equalsIgnoreCase("inode") || entry.getKey()
								.equalsIgnoreCase("identifier")) {
							conditionBuffer.append(" asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(" = '");
							conditionBuffer.append(entry.getValue());
							conditionBuffer.append('\'');
						}else{
							conditionBuffer.append(" lower(asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" asset.");
						conditionBuffer.append(entry.getKey());
						conditionBuffer.append(" = ");
						conditionBuffer.append(entry.getValue());
					}
				}else{
					if(entry.getValue() instanceof String){
						if (entry.getKey().equalsIgnoreCase("inode") || entry.getKey()
								.equalsIgnoreCase("identifier")) {
							conditionBuffer.append(" OR asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(" = '");
							conditionBuffer.append(entry.getValue());
							conditionBuffer.append('\'');
						}else{
							conditionBuffer.append(" OR lower(asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" OR asset.");
						conditionBuffer.append(entry.getKey());
						conditionBuffer.append(" = ");
						conditionBuffer.append(entry.getValue());
					}
				}

				counter+=1;
			}
			conditionBuffer.append(" ) ");
		}

		StringBuilder query = new StringBuilder();
		query.append("select asset.* from ");
		query.append(Type.CONTAINERS.getTableName());
		query.append(" asset, inode, identifier, ");
		query.append(Type.CONTAINERS.getVersionTableName());
		query.append(" vinfo");

		if(UtilMethods.isSet(parent)){

			//Search for the given ContentType inode
			ContentType foundContentType = contentTypeAPI.find(parent);

			if (null != foundContentType && InodeUtils.isSet(foundContentType.inode())) {
				query.append(
						" where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(
								" and exists ( from container_structures cs where cs.container_id = asset.identifier")
						.append(" and cs.structure_id = '");
				query.append(parent);
				query.append("' ) ");
			}else {
				query.append(
						" ,tree where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(" and tree.parent = '");
				query.append(parent);
				query.append("' and tree.child=asset.inode");
			}
		}else{
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and vinfo.identifier=identifier.id and vinfo.working_inode=asset.inode ");
		if(!includeArchived) {
			query.append(" and vinfo.deleted=");
			query.append(DbConnectionFactory.getDBFalse());
		}
		if(UtilMethods.isSet(hostId)){
			query.append(" and identifier.host_inode = '");
			query.append(hostId).append('\'');
		}
		if(UtilMethods.isSet(inode)){
			query.append(" and asset.inode = '");
			query.append(inode).append('\'');
		}
		if(UtilMethods.isSet(identifier)){
			query.append(" and asset.identifier = '");
			query.append(identifier);
			query.append('\'');
		}
		if(!UtilMethods.isSet(orderBy)){
			orderBy = "mod_date desc";
		}

		List<Container> resultList;
		DotConnect dc = new DotConnect();
		int countLimit = 100;
		int size = 0;
		try {
			query.append(conditionBuffer.toString());
			query.append(" order by asset.");
			query.append(orderBy);
			dc.setSQL(query.toString());

			if(paramValues!=null && paramValues.size()>0){
				for (Object value : paramValues) {
					dc.addParam((String)value);
				}
			}

			while(!done) {
				dc.setStartRow(internalOffset);
				dc.setMaxRows(internalLimit);
				resultList = ConvertToPOJOUtil.convertDotConnectMapToContainer(dc.loadResults());
				PermissionAPI permAPI = APILocator.getPermissionAPI();
				toReturn.addAll(permAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if(countLimit > 0 && toReturn.size() >= countLimit + offset)
					done = true;
				else if(resultList.size() < internalLimit)
					done = true;

				internalOffset += internalLimit;
			}

			if(offset > toReturn.size()) {
				size = 0;
			} else if(countLimit > 0) {
				int toIndex = offset + countLimit > toReturn.size()?toReturn.size():offset + countLimit;
				size = toReturn.subList(offset, toIndex).size();
			} else if (offset > 0) {
				size = toReturn.subList(offset, toReturn.size()).size();
			}
			assets.setTotalResults(size);

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

    public List<Container> findContainersForStructure(String structureInode) throws DotDataException {
        HibernateUtil dh = new HibernateUtil(Container.class);

        StringBuilder query = new StringBuilder();

        query.append("FROM c IN CLASS ");
		query.append(Container.class);
		query.append(" WHERE  exists ( from cs in class ");
		query.append(ContainerStructure.class.getName());
		query.append(" where cs.containerId = c.identifier and cs.structureId = ? ) ");
        dh.setQuery(query.toString());
        dh.setParam(structureInode);
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

				   final VersionInfo info =
						   this.identifierCache.getVersionInfo(identifier);
				   if (null != info && UtilMethods.isSet(info.getLiveInode())) {

					   CacheLocator.getContainerCache().remove(info.getLiveInode());
				   }

				   CacheLocator.getContainerCache().remove(identifier);
			   }
           }
        } catch (DotDataException e) {
            Logger.error(ContainerFactory.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
	}
}