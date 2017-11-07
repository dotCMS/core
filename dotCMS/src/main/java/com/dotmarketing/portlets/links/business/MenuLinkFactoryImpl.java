package com.dotmarketing.portlets.links.business;

import com.dotcms.repackage.org.apache.commons.beanutils.PropertyUtils;
import com.dotmarketing.common.db.DotConnect;
import com.google.common.base.CaseFormat;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.stream.Collectors;

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

	        
		} else {
		    HibernateUtil.save(menuLink);
			APILocator.getIdentifierAPI().createNew(menuLink, destination);
			HibernateUtil.save(menuLink);
			APILocator.getVersionableAPI().setWorking(menuLink);
		}
		CacheLocator.getNavToolCache().removeNav(menuLink.getHostId(),destination.getInode());
		RefreshMenus.deleteMenu(destination);
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
		String condition = !includeArchived?" asset.inode=versioninfo.working_inode and versioninfo.deleted = " +DbConnectionFactory.getDBFalse():"  asset.inode=versioninfo.working_inode ";
		conditionBuffer.append(condition);

		List<Object> paramValues =null;
		if(params!=null && params.size()>0){
			conditionBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				if(counter==0){
					if(entry.getValue() instanceof String){
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(" = '");
							conditionBuffer.append(entry.getValue());
							conditionBuffer.append("'");
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
						if(entry.getKey().equalsIgnoreCase("inode")){
							conditionBuffer.append(" OR asset.");
							conditionBuffer.append(entry.getKey());
							conditionBuffer.append(" = '");
							conditionBuffer.append(entry.getValue());
							conditionBuffer.append("'");
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

		StringBuffer query = new StringBuffer();
		query.append("select asset.* from links asset, inode, identifier,link_version_info versioninfo");
		if(UtilMethods.isSet(parent)){
			query.append(" ,tree where asset.inode=inode.inode ");
			query.append("and asset.identifier = identifier.id and tree.parent = '");
			query.append(parent);
			query.append("' and tree.child=asset.inode");

		}else{
			query.append(" where asset.inode=inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and versioninfo.identifier=identifier.id ");
		if(UtilMethods.isSet(hostId)){
			query.append(" and identifier.host_inode = '");
			query.append(hostId);
			query.append("'");
		}
		if(UtilMethods.isSet(inode)){
			query.append(" and asset.inode = '");
			query.append(inode);
			query.append("'");
		}
		if(UtilMethods.isSet(identifier)){
			query.append(" and asset.identifier = '");
			query.append(identifier);
			query.append("'");
		}
		if(!UtilMethods.isSet(orderBy)){
			orderBy = "mod_date desc";
		}

		List<Link> resultList;
		DotConnect dc = new DotConnect();
		int countLimit = 100;
		int size = 0;
		try {
			query.append(" and ");
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
				resultList = convertDotConnectMapToPOJO(dc.loadResults(),Link.class);
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

	/**
	 *
	 * @param results
	 * @return
	 */
	private static List<Object> convertDotConnectMapToPOJO(List<Map<String,String>> results, Class classToUse)
			throws Exception {

		DateFormat df;
		List<Object> ret;
		Map<String, String> properties;

		ret = new ArrayList<>();

		if(results == null || results.size()==0){
			return ret;
		}

		df = new SimpleDateFormat("yyyy-MM-dd");

		for (Map<String, String> map : results) {
			Constructor<?> ctor = classToUse.getConstructor();
			Object object = ctor.newInstance();

			properties = map.keySet().stream().collect(Collectors
					.toMap(key -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), key ->map.get(key)));

			for (String property: properties.keySet()){
				if (properties.get(property) != null){
					if (isFieldPresent(classToUse, String.class, property)){
						PropertyUtils.setProperty(object, property, properties.get(property));
					}else if (isFieldPresent(classToUse, Integer.TYPE, property)){
						PropertyUtils.setProperty(object, property, Integer.parseInt(properties.get(property)));
					}else if (isFieldPresent(classToUse, Boolean.TYPE, property)){
						PropertyUtils.setProperty(object, property, Boolean.parseBoolean(properties.get(property)));
					}else if (isFieldPresent(classToUse, Date.class, property)){
						PropertyUtils.setProperty(object, property, df.parse(properties.get(property)));
					}else{
						Logger.warn(classToUse, "Property " + property + "not set for " + classToUse.getName());
					}
				}
			}

			ret.add(object);
		}
		return ret;
	}

	private static boolean isFieldPresent(Class classToUse, Class fieldType, String property)
			throws NoSuchFieldException {

		try{
			return classToUse.getDeclaredField(property).getType() == fieldType;
		}catch(NoSuchFieldException e){
			if (classToUse.getSuperclass()!=null) {
				return isFieldPresent(classToUse.getSuperclass(), fieldType, property);
			}
		}
		return false;
	}

}