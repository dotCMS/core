package com.dotmarketing.portlets.containers.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

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
		HibernateUtil hu = new HibernateUtil(Container.class);
		String sql = "SELECT {" + Inode.Type.CONTAINERS.getTableName() + ".*} from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_, identifier ident, container_version_info vv " +
				"where vv.working_inode=" + Inode.Type.CONTAINERS.getTableName() + ".inode and " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and " +
				"vv.identifier = ident.id and host_inode = '" + parentPermissionable.getIdentifier() + "'";
		hu.setSQLQuery(sql);
		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<Container> findAllContainers() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Container.class);
		String sql = "SELECT {" + Inode.Type.CONTAINERS.getTableName() + ".*} from " + Inode.Type.CONTAINERS.getTableName() + ", inode dot_containers_1_, container_version_info vv " +
				"where vv.working_inode= " + Inode.Type.CONTAINERS.getTableName() + ".inode and " + Inode.Type.CONTAINERS.getTableName() + ".inode = dot_containers_1_.inode and " +
				"dot_containers_1_.type='containers' order by " + Inode.Type.CONTAINERS.getTableName() + ".title";
		hu.setSQLQuery(sql);
		return hu.list();
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
		/*String condition = !includeArchived?" asset.working = " + DbConnectionFactory.getDBTrue() + " and asset.deleted = " +DbConnectionFactory.getDBFalse():
			" asset.working = " + DbConnectionFactory.getDBTrue();*/
		//conditionBuffer.append(condition);

		List<Object> paramValues =null;
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<Object>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(counter==0){
					if(entry.getValue() instanceof String){
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else if(entry.getKey().equalsIgnoreCase("identifier")){
							conditionBuffer.append(" asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else{
							conditionBuffer.append(" lower(asset." + entry.getKey()+ ") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" asset." + entry.getKey()+ " = " + entry.getValue());
					}
				}else{
					if(entry.getValue() instanceof String){
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" OR asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else if(entry.getKey().equalsIgnoreCase("identifier")){
							conditionBuffer.append(" OR asset." + entry.getKey()+ " = '" + entry.getValue() + "'");
						}else{
							conditionBuffer.append(" OR lower(asset." + entry.getKey()+ ") like ? ");
							paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
						}
					}else{
						conditionBuffer.append(" OR asset." + entry.getKey()+ " = " + entry.getValue());
					}
				}

				counter+=1;
			}
			conditionBuffer.append(" ) ");
		}

		StringBuffer query = new StringBuffer();
		query.append("select asset from asset in class " + Container.class.getName() + ", " +
				"inode in class " + Inode.class.getName()+", identifier in class " + Identifier.class.getName() +", vinfo in class "+ContainerVersionInfo.class.getName());
		if(UtilMethods.isSet(parent)){

			//Search for the given ContentType inode
			ContentType foundContentType = contentTypeAPI.find(parent);

			if ( null != foundContentType && InodeUtils.isSet(foundContentType.inode()) )
				query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id"
						+ " and exists ( from cs in class " + ContainerStructure.class.getName() + " where cs.containerId = asset.identifier and cs.structureId = '" + parent + "' ) ");
			else
				query.append(" ,tree in class " + Tree.class.getName() + " where asset.inode = inode.inode " +
						"and asset.identifier = identifier.id and tree.parent = '" + parent + "' and tree.child=asset.inode");
		}else{
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and vinfo.identifier=identifier.id and vinfo.workingInode=asset.inode ");
		if(!includeArchived)
		    query.append(" and vinfo.deleted="+DbConnectionFactory.getDBFalse());
		if(UtilMethods.isSet(hostId)){
			query.append(" and identifier.hostId = '"+ hostId +"'");
		}
		if(UtilMethods.isSet(inode)){
			query.append(" and asset.inode = '"+ inode +"'");
		}
		if(UtilMethods.isSet(identifier)){
			query.append(" and asset.identifier = '"+ identifier +"'");
		}
		if(!UtilMethods.isSet(orderBy)){
			orderBy = "modDate desc";
		}

		List<Container> resultList = new ArrayList<Container>();
		HibernateUtil dh = new HibernateUtil(Container.class);
		String type;
		int countLimit = 100;
		int size = 0;
		try {
			type = ((Inode) Container.class.newInstance()).getType();
			query.append(" and asset.type='"+type+ "' " + conditionBuffer.toString() + " order by asset." + orderBy);
			dh.setQuery(query.toString());

			if(paramValues!=null && paramValues.size()>0){
				for (Object value : paramValues) {
					dh.setParam((String)value);
				}
			}

			while(!done) {
				dh.setFirstResult(internalOffset);
				dh.setMaxResults(internalLimit);
				resultList = dh.list();
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

    public List<Container> findContainersForStructure(String structureInode) throws DotDataException {
        HibernateUtil dh = new HibernateUtil(Container.class);
        dh.setQuery("FROM c IN CLASS "+Container.class+" WHERE "
        		+ " exists ( from cs in class " + ContainerStructure.class.getName() + " where cs.containerId = c.identifier and cs.structureId = ? ) ");
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
        
        try {
           dc.setSQL("select distinct(identifier) from " + Inode.Type.CONTAINERS.getTableName() + " where mod_user = ?");
           dc.addParam(userId);
           List<HashMap<String, String>> containers = dc.loadResults();
           
           dc.setSQL("UPDATE " + Inode.Type.CONTAINERS.getTableName() + " set mod_user = ? where mod_user = ? ");
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