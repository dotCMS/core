package com.dotmarketing.portlets.templates.business;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionedWebAssetUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.Body;
import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.services.TemplateServices;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateFactoryImpl implements TemplateFactory {
	static TemplateCache templateCache = CacheLocator.getTemplateCache();

	private final String templatesUnderHostSQL =
		"select template.*, template_1_.*  from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
		"identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
		"template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
		"template.inode = template_1_.inode and vi.identifier=template.identifier and " +
		"template.inode=vi.working_inode ";

	private final String templateWithNameSQL =
		"select template.*, template_1_.* from " + Type.TEMPLATE.getTableName() + " template, inode template_1_, " +
		"identifier template_identifier, " + Type.TEMPLATE.getVersionTableName() + " vi where " +
		"template_identifier.host_inode = ? and template_identifier.id = template.identifier and " +
		"vi.identifier=template_identifier.id and template.title = ? and " +
		"template.inode = template_1_.inode and " +
		"template.inode=vi.working_inode ";

	
	@SuppressWarnings("unchecked")

	public Template find(String inode) throws DotStateException, DotDataException {
		
		Template template = templateCache.get(inode);
		
		if(template==null){
		
		
			HibernateUtil hu = new HibernateUtil(Template.class);
			template = (Template) hu.load(inode);
			if(template != null && template.getInode() != null)
				templateCache.add(inode, template);
		}
		return template;
	}

	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesAssignedTo(Host parentHost, final boolean includeArchived)
			throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String query = !includeArchived ? templatesUnderHostSQL + " and vi.deleted = "
				+ DbConnectionFactory.getDBFalse() : templatesUnderHostSQL;
		dc.setSQL(query);
		dc.addParam(parentHost.getIdentifier());


		return TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();


	}


	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesUserCanUse(User user, String hostName, String query,boolean searchHost ,int offset, int limit) throws DotDataException, DotSecurityException {
		return PermissionedWebAssetUtil.findTemplatesForLimitedUser(query, hostName, searchHost, "title", offset, limit, PermissionAPI.PERMISSION_READ, user, false);
	}

	public void delete(Template template) throws DotDataException {
		templateCache.remove(template.getInode());
		HibernateUtil.delete(template);
		
	}

	public void save(Template template) throws DotDataException {
		if(!UtilMethods.isSet(template.getIdentifier())){
			throw new DotStateException("Cannot save a template without an Identifier");
		}
		HibernateUtil.save(template);
		
		TemplateServices.invalidate(template, true);

	}
	
	public void save(Template template, String existingId) throws DotDataException {
        if(!UtilMethods.isSet(template.getIdentifier())){
            throw new DotStateException("Cannot save a tempalte without an Identifier");
        }
        HibernateUtil.saveWithPrimaryKey(template, existingId);
        templateCache.add(template.getInode(), template);
        TemplateServices.invalidate(template, true);

    }

	public void deleteFromCache(Template template) throws DotDataException {
		templateCache.remove(template.getInode());
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
	}

	@SuppressWarnings("unchecked")
	public Template findWorkingTemplateByName(String name, Host host) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(templateWithNameSQL);
		dc.addParam(host.getIdentifier());
		dc.addParam(name);
		try{
			final List<Template> result = TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();
			if (result!= null && !result.isEmpty()){
				return result.get(0);
			}
		}catch (Exception e){
			Logger.warn(this, e.getMessage(), e);
		}

		return null;

	}

	@Override
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
		final String condition = !includeArchived ?
				" asset.inode=versioninfo.working_inode and versioninfo.deleted = "
						+ DbConnectionFactory.getDBFalse()
				: " asset.inode=versioninfo.working_inode  ";
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
		query.append("select asset.*, inode.* from ");
		query.append(Type.TEMPLATE.getTableName());
		query.append(" asset, inode, identifier, ");
		query.append(Type.TEMPLATE.getVersionTableName());
		query.append(" versioninfo");
		if(UtilMethods.isSet(parent)){
			query.append(", tree where asset.inode = inode.inode and asset.identifier = identifier.id and tree.parent = '");
			query.append(parent);
			query.append("' and tree.child=asset.inode");
		}else{
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}
		query.append(" and versioninfo.identifier=asset.identifier ");
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

		List<Template> resultList;
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

				resultList = TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();

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


	
	@Override
	public List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		
		List<Container> result = new ArrayList<Container>();
		Collection<String> ids = getContainerIds(template);
		for(String containerId : ids) {
			Container container = APILocator.getContainerAPI().getWorkingContainerById(containerId, user, respectFrontendRoles);
			if(container != null) {
				result.add(container);
			} else {
				Logger.warn(this,"ERROR The Container Id: '" + containerId + "' doesn't exist and its reference by template " + template.getIdentifier());
			}
		}
		return result;
	}

	private Collection<String> getContainerIds(Template template) {
		try {
			return this.getContainerIdsFromJSON(template);
		} catch (Exception e) {
			return this.getContainerIdsFromHTML(template.getBody());
		}
	}

	private Collection<String> getContainerIdsFromJSON(Template template) throws IOException {
		TemplateLayout templateLayout = DotTemplateTool.getTemplateLayoutFromJSON(template.getDrawedBody());

		Collection<String> result = new TreeSet<>(getContainersFromColumn(templateLayout));

		Sidebar sidebar = templateLayout.getSidebar();

		if (sidebar != null && sidebar.getContainers() != null) {
			result.addAll(sidebar.getContainers());
		}

		return result;
	}

	private Collection<String> getContainersFromColumn(TemplateLayout templateLayout) {
		Collection<String> result = new TreeSet<>();

		Body body = templateLayout.getBody();
		List<TemplateLayoutRow> rows = body.getRows();

		for (TemplateLayoutRow row : rows) {
			List<TemplateLayoutColumn> columns = row.getColumns();

			for (TemplateLayoutColumn column : columns) {
				List<String> columnContainers = column.getContainers();
				result.addAll(columnContainers);
			}
		}

		return result;
	}

	private List<String> getContainerIdsFromHTML(String templateBody) {
	    Set<String> ids = new HashSet<String>();
	    if(!UtilMethods.isSet(templateBody)){
	        return new ArrayList<>(ids);
	    }

		Pattern newContainerReferencesRegex = Pattern.compile("#parseContainer\\s*\\(\\s*['\"]*([^'\")]+)['\"]*\\s*\\)");
		Matcher matcher = newContainerReferencesRegex.matcher(templateBody);
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			ids.add(containerId);
		}
        return new ArrayList<>(ids);
	}
	
	
	@Override
	public Template copyTemplate(Template currentTemplate, Host host) throws DotDataException, DotSecurityException {
		if(currentTemplate ==null){
			throw new DotDataException("Template is null");
		}
		Template newTemplate = new Template();

		try {
			BeanUtils.copyProperties(newTemplate, currentTemplate);
		} catch (Exception e1) {
			Logger.error(TemplateFactoryImpl.class,e1.getMessage(),e1);
			throw new DotDataException(e1.getMessage());
		}
		newTemplate.setInode(null);
		newTemplate.setModDate(new Date());
		newTemplate.setDrawed(currentTemplate.isDrawed());
		newTemplate.setDrawedBody(currentTemplate.getDrawedBody());
		newTemplate.setImage(currentTemplate.getImage());
		newTemplate.setIdentifier(null);
		String newTemplateName = currentTemplate.getTitle();
		String testName = currentTemplate.getTitle();

		if(RegEX.contains(newTemplateName, " - [0-9]+$")){
			newTemplateName = newTemplateName.substring(0,newTemplateName.lastIndexOf("-")).trim();
		}
		
		
		
		Template test = null;
		for(int iter=1;iter<100000;iter++){		
			try{
				test = findWorkingTemplateByName(testName, host);
			}
			catch(Exception e){
				Logger.debug(this.getClass(), e.getMessage());
				break;
			}
			if(test != null && UtilMethods.isSet(test.getInode())){
				testName = newTemplateName + " - " + iter ;
			}
			else{
				newTemplateName = testName;
				break;
			}
		}
		
		newTemplate.setFriendlyName(newTemplateName);
		newTemplate.setTitle(newTemplateName);




		return newTemplate;
	}
	
	/**
	 *
	 * Updates the template's theme without creating new version.
	 * @param templateInode
	 * @param theme
	 *
	 */
   public void updateThemeWithoutVersioning(String templateInode, String theme) throws DotDataException{
	   Template templateToUpdate = find(templateInode);
	   templateToUpdate.setTheme(theme);
       HibernateUtil.saveOrUpdate(templateToUpdate);
       templateCache.add(templateToUpdate.getInode(), templateToUpdate);
       TemplateServices.invalidate(templateToUpdate, true);
   };

   /**
	 * Method will replace user references of the given userId in templates
	 * with the replacement user Id  
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();
       
       try {
          dc.setSQL("select inode from " + Type.TEMPLATE.getTableName() + " where mod_user = ?");
          dc.addParam(userId);
          List<HashMap<String, String>> templates = dc.loadResults();
          
          dc.setSQL("UPDATE " + Type.TEMPLATE.getTableName() + " set mod_user = ? where mod_user = ? ");
          dc.addParam(replacementUserId);
          dc.addParam(userId);
          dc.loadResult();
          
          dc.setSQL("update " + Type.TEMPLATE.getVersionTableName() + " set locked_by=? where locked_by  = ?");
          dc.addParam(replacementUserId);
          dc.addParam(userId);
          dc.loadResult();
        
          for(HashMap<String, String> ident:templates){
              String inode = ident.get("inode");
              Template template = find(inode);
              deleteFromCache(template);
              TemplateServices.invalidate(template, true);
          }
       } catch (DotDataException e) {
           Logger.error(TemplateFactory.class,e.getMessage(),e);
           throw new DotDataException(e.getMessage(), e);
       }
	}
}
