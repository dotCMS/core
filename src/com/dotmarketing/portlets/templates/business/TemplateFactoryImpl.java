package com.dotmarketing.portlets.templates.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.TemplateContainers;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionedWebAssetUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class TemplateFactoryImpl implements TemplateFactory {
	static TemplateCache templateCache = CacheLocator.getTemplateCache();

	private final String subTemplatesSQL =
		"select {template.*} from template,inode template_1_, template_version_info vi where " +
		"template_1_.inode = template.inode and vi.identifier=template.identifier and " +
		"identifier in(select template_id from identifier,htmlpage where " +
		"htmlpage.identifier = identifier.id and parent_path = ?) and " +
		"template.inode=vi.working_inode ";

	private final String templatesUnderHostSQL =
		"select {template.*} from template, inode template_1_, " +
		"identifier template_identifier, template_version_info vi where " +
		"template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
		"template.inode = template_1_.inode and vi.identifier=template.identifier and " +
		"template.inode=vi.working_inode ";

	private final String templateWithNameSQL =
		"select {template.*} from template, inode template_1_, " +
		"identifier template_identifier, template_version_info vi where " +
		"template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
		"vi.identifier=template_identifier.id and template.title = ? and " +
		"template.inode = template_1_.inode and " +
		"template.inode=vi.working_inode ";

	private final String pagesUsingTemplateSQL =
		"select {htmlpage.*} from htmlpage, inode htmlpage_1_, " +
		"identifier htmlpage_identifier, htmlpage_version_info vi where " +
		"htmlpage_identifier.id = htmlpage.identifier and " +
		"htmlpage.template_id = ? and vi.identifier=htmlpage_identifier.id and " +
		"htmlpage.inode = htmlpage_1_.inode and " +
		"htmlpage.inode=vi.working_inode ";


	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesUnder(Folder parentFolder) throws DotStateException, DotDataException {
		HibernateUtil hu = new HibernateUtil(Template.class);
		hu.setSQLQuery(subTemplatesSQL);
		hu.setParam(APILocator.getIdentifierAPI().find(parentFolder).getPath());
		return new ArrayList<Template>(new HashSet<Template>(hu.list()));
	}

	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesAssignedTo(Host parentHost, boolean includeArchived) throws DotHibernateException {
		HibernateUtil hu = new HibernateUtil(Template.class);
		String query = !includeArchived?templatesUnderHostSQL + " and vi.deleted = " + DbConnectionFactory.getDBFalse():templatesUnderHostSQL;
		hu.setSQLQuery(templatesUnderHostSQL);
		hu.setParam(parentHost.getIdentifier());
		return new ArrayList<Template>(new HashSet<Template>(hu.list()));
	}


	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesUserCanUse(User user, String hostName, String query,boolean searchHost ,int offset, int limit) throws DotDataException, DotSecurityException {
		return PermissionedWebAssetUtil.findTemplatesForLimitedUser(query, hostName, searchHost, "title", offset, limit, PermissionAPI.PERMISSION_READ, user, false);
	}

	public void delete(Template template) throws DotDataException {
		HibernateUtil.delete(template);
	}

	public void save(Template template) throws DotDataException {
		HibernateUtil.save(template);
		templateCache.add(template.getInode(), template);
		//WorkingCache.removeAssetFromCache(template);
		//WorkingCache.addToWorkingAssetToCache(template);
		//LiveCache.removeAssetFromCache(template);
		//if (template.isLive()) {
		//	LiveCache.addToLiveAssetToCache(template);
		//}
	}

	public void deleteFromCache(Template template) throws DotDataException {
		templateCache.remove(template.getInode());
		//WorkingCache.removeAssetFromCache(template);
		//if (template.isLive()) {
		//	LiveCache.removeAssetFromCache(template);
		//}
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
	}

	@SuppressWarnings("unchecked")
	public Template findWorkingTemplateByName(String name, Host host) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(Template.class);
		hu.setSQLQuery(templateWithNameSQL);
		hu.setParam(host.getIdentifier());
		hu.setParam(name);
		return (Template) hu.load();

	}


	public List<Template> findTemplates(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {

		PaginatedArrayList<Template> assets = new PaginatedArrayList<Template>();
		List<Permissionable> toReturn = new ArrayList<Permissionable>();
		int internalLimit = 500;
		int internalOffset = 0;
		boolean done = false;

		StringBuffer conditionBuffer = new StringBuffer();
		String condition = !includeArchived?" asset.inode=versioninfo.workingInode and versioninfo.deleted = " +DbConnectionFactory.getDBFalse():" asset.inode=versioninfo.workingInode  ";
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
		query.append("select asset from asset in class " + Template.class.getName() + ", " +
				"inode in class " + Inode.class.getName()+", identifier in class " + Identifier.class.getName());
		query.append(", versioninfo in class ").append(TemplateVersionInfo.class.getName());
		if(UtilMethods.isSet(parent)){
			query.append(" ,tree in class " + Tree.class.getName() + " where asset.inode = inode.inode " +
					"and asset.identifier = identifier.id and tree.parent = '"+parent+"' and tree.child=asset.inode");

		}else{
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and versioninfo.identifier=asset.identifier ");
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

		List<Template> resultList = new ArrayList<Template>();
		HibernateUtil dh = new HibernateUtil(Template.class);
		String type;
		int countLimit = 100;
		int size = 0;
		try {
			type = ((Inode) Template.class.newInstance()).getType();
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
						assets.add((Template) toReturn.get(i));
						pageLimit+=1;
					}else{
						break;
					}

				}
			} else {
				for(int i=0;i<toReturn.size();i++){
						assets.add((Template) toReturn.get(i));
				}
			}
		} catch (Exception e) {

			Logger.error(TemplateFactoryImpl.class, "findTemplates failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

		return assets;


	}

	public List<HTMLPage> getPagesUsingTemplate(Template template) throws DotDataException {
		HibernateUtil hu = new HibernateUtil(HTMLPage.class);
		hu.setSQLQuery(pagesUsingTemplateSQL);
		hu.setParam(template.getIdentifier());
		return new ArrayList<HTMLPage>(new HashSet<HTMLPage>(hu.list()));

	}

	public void associateContainers(List<Container> containerIdentifiers,Template template) throws DotHibernateException{
		HibernateUtil.startTransaction();
			try {
				HibernateUtil.delete("from template_containers in class com.dotmarketing.beans.TemplateContainers where template_id = '" + template.getIdentifier() + "'");
				for(Container container:containerIdentifiers){
					TemplateContainers templateContainer = new  TemplateContainers();
					templateContainer.setTemplateId(template.getIdentifier());
					templateContainer.setContainerId(container.getIdentifier());
					HibernateUtil.save(templateContainer);
				}
			} catch (DotHibernateException e) {
				HibernateUtil.rollbackTransaction();
			}
		HibernateUtil.commitTransaction();
	}

}
