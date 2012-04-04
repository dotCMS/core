package com.dotmarketing.portlets.containers.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
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
	static ContainerCache containerCache = CacheLocator.getContainerCache();

	public void save(Container container) throws DotDataException {
		HibernateUtil.save(container);
		containerCache.add(container.getInode(), container);
	}

	@SuppressWarnings("unchecked")
	public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Container.class);
		String sql = "SELECT {containers.*} from containers, inode containers_1_, identifier ident, container_version_info vv " +
				"where vv.working_inode=containers.inode and containers.inode = containers_1_.inode and " +
				"vv.identifier = ident.id and host_inode = '" + parentPermissionable.getIdentifier() + "'";
		hu.setSQLQuery(sql);
		return hu.list();
	}

	@SuppressWarnings("unchecked")
	public List<Container> findAllContainers() throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Container.class);
		String sql = "SELECT {containers.*} from containers, inode containers_1_, container_version_info vv " +
				"where vv.working_inode= containers.inode and containers.inode = containers_1_.inode and " +
				"containers_1_.type='containers' order by containers.title";
		hu.setSQLQuery(sql);
		return hu.list();
	}

	public List<Container> findContainers(User user, boolean includeArchived,
			Map<String, Object> params, String hostId,String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {

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
			if(InodeUtils.isSet(InodeFactory.getInode(parent, Structure.class).getInode()))
			  query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id and asset.structureInode = '"+parent+"' ");
		   else
			   query.append(" ,tree in class " + Tree.class.getName() + " where asset.inode = inode.inode " +
						    "and asset.identifier = identifier.id and tree.parent = '"+parent+"' and tree.child=asset.inode");
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
        dh.setQuery("FROM c IN CLASS "+Container.class+" WHERE c.structureInode=?");
        dh.setParam(structureInode);
        return dh.list();
    }

}