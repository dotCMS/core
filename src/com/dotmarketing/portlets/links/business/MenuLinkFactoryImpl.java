package com.dotmarketing.portlets.links.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.LinkVersionInfo;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class MenuLinkFactoryImpl implements MenuLinkFactory {
	static MenuLinkCache menuLinkCache = CacheLocator.getMenuLinkCache();

	
	@Override
	public Link load(String inode) throws DotHibernateException{
		
		HibernateUtil dh = new HibernateUtil(Link.class);

		return (Link) dh.load(inode);
		
		
	}
	
	public void save(Link menuLink) throws DotDataException, DotStateException, DotSecurityException {
	    save(menuLink, APILocator.getFolderAPI().findSystemFolder());
	}
	
	public void save(Link menuLink, Folder destination) throws DotDataException, DotStateException, DotSecurityException {
		
		
		if(UtilMethods.isSet(menuLink.getInode())) {
			Link oldLink = null;
			try{
				oldLink = (Link) HibernateUtil.load(Link.class, menuLink.getInode());
			}catch(DotHibernateException dhe){
				Logger.debug(this.getClass(), dhe.getMessage());
			}
			
			if(oldLink!=null && InodeUtils.isSet(oldLink.getIdentifier())) {
				oldLink.copy(menuLink);
				HibernateUtil.saveOrUpdate(oldLink);
				HibernateUtil.flush();
				menuLink = oldLink;
			} else {
				String existingId=menuLink.getIdentifier();
				menuLink.setIdentifier(null);
				HibernateUtil.saveWithPrimaryKey(menuLink, menuLink.getInode());

				Identifier id=APILocator.getIdentifierAPI().find(existingId);
				if(id==null || !InodeUtils.isSet(id.getId())) {
					APILocator.getIdentifierAPI().createNew(menuLink, destination, existingId);
				}else {
					menuLink.setIdentifier(existingId);
				}
				HibernateUtil.saveOrUpdate(menuLink);
				HibernateUtil.flush();
			}
			APILocator.getIdentifierAPI().updateIdentifierURI(menuLink, destination);
			WorkingCache.removeAssetFromCache(menuLink);
	        LiveCache.removeAssetFromCache(menuLink);
	        
		} else {
		    HibernateUtil.save(menuLink);
			Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(menuLink, destination);
			menuLink.setIdentifier(newIdentifier.getInode());
			HibernateUtil.save(menuLink);
			APILocator.getVersionableAPI().setWorking(menuLink);
		}
	}

	public List<Link> findLinks(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {

		PaginatedArrayList<Link> assets = new PaginatedArrayList<Link>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		StringBuffer conditionBuffer = new StringBuffer();
		String condition = !includeArchived?" asset.inode=versioninfo.workingInode and versioninfo.deleted = " +DbConnectionFactory.getDBFalse():"  asset.inode=versioninfo.workingInode ";
		conditionBuffer.append(condition);

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
		query.append("select asset from asset in class " + Link.class.getName() + ", " +
				"inode in class " + Inode.class.getName()+", identifier in class " + Identifier.class.getName());
		query.append(", versioninfo in class ").append(LinkVersionInfo.class.getName());
		if(UtilMethods.isSet(parent)){
			query.append(" ,tree in class " + Tree.class.getName() + " where asset.inode=inode.inode " +
					"and asset.identifier = identifier.id and tree.parent = '"+parent+"' and tree.child=asset.inode");

		}else{
			query.append(" where asset.inode=inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and versioninfo.identifier=identifier.id ");
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

		List<Link> resultList = new ArrayList<Link>();
		HibernateUtil dh = new HibernateUtil(Link.class);
		String type;
		int countLimit = 100;
		int size = 0;
		try {
			type = ((Inode) Link.class.newInstance()).getType();
			query.append(" and asset.type='"+type+ "' and " + conditionBuffer.toString() + " order by asset." + orderBy);
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
						assets.add((Link) toReturn.get(i));
						pageLimit+=1;
					}else{
						break;
					}

				}
			}
			else {
				for(int i=0;i<toReturn.size();i++){
					assets.add((Link) toReturn.get(i));
				}
			}

		} catch (Exception e) {

			Logger.error(MenuLinkFactoryImpl.class, "findLinks failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return assets;

	}

}