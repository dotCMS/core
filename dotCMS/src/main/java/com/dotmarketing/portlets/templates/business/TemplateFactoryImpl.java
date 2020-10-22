package com.dotmarketing.portlets.templates.business;

import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.*;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.google.common.io.LineReader;
import com.liferay.portal.model.User;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateFactoryImpl implements TemplateFactory {
	private static TemplateCache templateCache = CacheLocator.getTemplateCache();
	private static TemplateSQL templateSQL = TemplateSQL.getInstance();

	@SuppressWarnings("unchecked")

	public Template find(final String inode) throws DotStateException, DotDataException {
		
		Template template = templateCache.get(inode);

		if(template==null){
			final List<Map<String, Object>> templateResults = new DotConnect()
					.setSQL(templateSQL.FIND_BY_INODE)
					.addParam(inode)
					.loadObjectResults();
			if (templateResults.isEmpty()) {
				Logger.debug(this, "Template with inode: " + inode + " not found");
				return null;
			}
			//This probably can be deleted when we add the iDate and Owner to the template table
			final List<Map<String, Object>> inodeResults = new DotConnect()
					.setSQL(templateSQL.FIND_IDATE_OWNER_FROM_INODE_BY_INODE)
					.addParam(inode)
					.loadObjectResults();
			templateResults.get(0).putAll(inodeResults.get(0));
			//
			template = (Template) TransformerLocator.createTemplateTransformer(templateResults).findFirst();

			if(template != null && template.getInode() != null) {
				templateCache.add(inode, template);
			}
		}

		return template;
	}

	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesAssignedTo(final Host parentHost, final boolean includeArchived)
			throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String query = !includeArchived ?
				templateSQL.FIND_TEMPLATES_BY_HOST_INODE + " and vi.deleted = "
				+ DbConnectionFactory.getDBFalse() : templateSQL.FIND_TEMPLATES_BY_HOST_INODE;
		dc.setSQL(query);
		dc.addParam(parentHost.getIdentifier());


		return TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();


	}


	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesUserCanUse(final User user, final String hostId, final String query, final boolean searchHost , final int offset, final int limit) throws DotDataException, DotSecurityException {
		return findTemplates(user, false,
				UtilMethods.isSet(query) ? Collections.singletonMap("filter", query.toLowerCase())
						: null, hostId, null, null, null, offset, limit, "title");
	}

	public void save(final Template template) throws DotDataException {
		save(template, UUIDGenerator.generateUuid());
	}
	
	public void save(final Template template, final String inode) throws DotDataException {
        if(!UtilMethods.isSet(template.getIdentifier())){
            throw new DotStateException("Cannot save a template without an Identifier");
        }

		if (UtilMethods.isSet(template.getTitle()) && !template.isAnonymous()) {
			template.setIsTemplate(true);
		}
        
        if(UtilMethods.isSet(template.getDrawedBody())) {
            template.setDrawed(true);
        }else {
            template.setDrawedBody((String)null);
            template.setDrawed(false);
        }

        if(!UtilMethods.isSet(template.getInode())) {
			template.setInode(UUIDGenerator.generateUuid());
		}

        if(!UtilMethods.isSet(find(template.getInode()))) {
			insertInodeInDB(template);
			insertTemplateInDB(template);
		} else {
        	updateInodeInDB(template);
        	updateTemplateInDB(template);
		}

        templateCache.add(template.getInode(), template);
        new TemplateLoader().invalidate(template);

    }

	private void insertInodeInDB(final Template template) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.INSERT_INODE);
		dc.addParam(template.getInode());
		dc.addParam(template.getiDate());
		dc.addParam(template.getOwner());
		dc.loadResult();
	}

	private void insertTemplateInDB(final Template template) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.INSERT_TEMPLATE);
		dc.addParam(template.getInode());
		dc.addParam(template.isShowOnMenu());
		dc.addParam(template.getTitle());
		dc.addParam(template.getModDate());
		dc.addParam(template.getModUser());
		dc.addParam(template.getSortOrder());
		dc.addParam(template.getFriendlyName());
		dc.addParam(template.getBody());
		dc.addParam(template.getHeader());
		dc.addParam(template.getFooter());
		dc.addParam(template.getImage());
		dc.addParam(template.getIdentifier());
		dc.addParam(template.isDrawed());
		dc.addParam(template.getDrawedBody());
		dc.addParam(template.getCountAddContainer());
		dc.addParam(template.getCountContainers());
		dc.addParam(template.getHeadCode());
		dc.addParam(template.getTheme());
		dc.loadResult();
	}

	public void deleteFromCache(final Template template) throws DotDataException {
		templateCache.remove(template.getInode());
		new TemplateLoader().invalidate(template);
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
	}

	@SuppressWarnings("unchecked")
	public Template findWorkingTemplateByName(String name, Host host) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.FIND_WORKING_TEMPLATE_BY_HOST_INODE_AND_TITLE);
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

		if(params!=null && params.size()>0){
					conditionBuffer
							.append(" and ( asset.inode like ? or asset.identifier like ? or lower(asset.title) like ? )");
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
		query.append(" and versioninfo.identifier=asset.identifier ")
			.append(" and show_on_menu = ").append(DbConnectionFactory.getDBTrue());

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

			if(params!=null && params.size()>0){
				final String filter = "%"+params.get("filter").toString().toLowerCase()+"%";
				dc.addParam(filter);
				dc.addParam(filter);
				dc.addParam(filter);
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
			result.addAll(sidebar.getContainers().stream()
					.map(ContainerUUID::getIdentifier)
					.collect(Collectors.toList()));
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
				List<String> columnContainers = column.getContainers().stream()
						.map(ContainerUUID::getIdentifier)
						.collect(Collectors.toList());
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

		Pattern newContainerReferencesRegex = Pattern.compile(PARSE_CONTAINER_ID_PATTERN);
		Matcher matcher = newContainerReferencesRegex.matcher(templateBody);
		while(matcher.find()) {
			String containerId = matcher.group(1).trim();
			ids.add(containerId);
		}
        return new ArrayList<>(ids);
	}

    private static final String PARSE_CONTAINER_ID_PATTERN =
            "#parseContainer\\s*\\(\\s*['\"]*([^'\")]+)['\"]*\\s*\\)";
	private static final String PARSE_CONTAINER_ID_UUDI_PATTERN =
			"\\s*#parseContainer\\s*\\(\\s*['\"]{1}([^'\")]+)['\"]{1}\\s*,\\s*['\"]{1}([^'\")]+)['\"]{1}\\s*\\)\\s*";

	@Override
	public List<ContainerUUID> getContainerUUIDFromHTML(final String templateBody) {

		final LineReader lineReader = new LineReader(new StringReader(templateBody));
		final List<ContainerUUID> containerUUIDS = new ArrayList<>();
		String line  = null;

		try {

			line = lineReader.readLine();
			final Pattern newContainerUUIDReferencesRegex =
					Pattern.compile(PARSE_CONTAINER_ID_UUDI_PATTERN);
            final Pattern newContainerReferencesRegex =
                    Pattern.compile(PARSE_CONTAINER_ID_PATTERN);

			while (null != line) {

				Matcher matcher = newContainerUUIDReferencesRegex.matcher(line);

				if (matcher.find() && matcher.groupCount() == 2) {

					final String containerId = matcher.group(1).trim();
					final String uuid        = matcher.group(2).trim();
					containerUUIDS.add(new ContainerUUID(containerId, uuid));
				} else {

                    matcher = newContainerReferencesRegex.matcher(line);
                    if (matcher.find() && matcher.groupCount() == 1) {

                        final String containerId = matcher.group(1).trim();
                        final String uuid        = ContainerUUID.UUID_LEGACY_VALUE;
                        containerUUIDS.add(new ContainerUUID(containerId, uuid));
                    }
                }

				line = lineReader.readLine();
			}
		} catch (IOException e) {

			Logger.error(this, e.getMessage(), e);
		}

		return containerUUIDS;
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
   public void updateThemeWithoutVersioning(final String templateInode, final String theme) throws DotDataException{
	   final Template templateToUpdate = find(templateInode);
	   templateToUpdate.setTheme(theme);

	   updateInodeInDB(templateToUpdate);
	   updateTemplateInDB(templateToUpdate);

       templateCache.add(templateToUpdate.getInode(), templateToUpdate);
       new TemplateLoader().invalidate(templateToUpdate);
   }

	private void updateInodeInDB(final Template template) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.UPDATE_INODE);
		dc.addParam(template.getiDate());
		dc.addParam(template.getOwner());
		dc.addParam(template.getInode());
		dc.loadResult();
	}

	private void updateTemplateInDB(final Template template) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.UPDATE_TEMPLATE);
		dc.addParam(template.isShowOnMenu());
		dc.addParam(template.getTitle());
		dc.addParam(template.getModDate());
		dc.addParam(template.getModUser());
		dc.addParam(template.getSortOrder());
		dc.addParam(template.getFriendlyName());
		dc.addParam(template.getBody());
		dc.addParam(template.getHeader());
		dc.addParam(template.getFooter());
		dc.addParam(template.getImage());
		dc.addParam(template.getIdentifier());
		dc.addParam(template.isDrawed());
		dc.addParam(template.getDrawedBody());
		dc.addParam(template.getCountAddContainer());
		dc.addParam(template.getCountContainers());
		dc.addParam(template.getHeadCode());
		dc.addParam(template.getTheme());
		dc.addParam(template.getInode());
		dc.loadResult();
	}

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
          dc.setSQL(templateSQL.FIND_TEMPLATES_BY_MOD_USER);
          dc.addParam(userId);
          List<HashMap<String, String>> templates = dc.loadResults();
          
          dc.setSQL(templateSQL.UPDATE_MOD_USER_BY_MOD_USER);
          dc.addParam(replacementUserId);
          dc.addParam(userId);
          dc.loadResult();
          
          dc.setSQL(templateSQL.UPDATE_LOCKED_BY);
          dc.addParam(replacementUserId);
          dc.addParam(userId);
          dc.loadResult();
        
          for(HashMap<String, String> ident:templates){
              String inode = ident.get("inode");
              Template template = find(inode);
              deleteFromCache(template);
              new TemplateLoader().invalidate(template);
          }
       } catch (DotDataException e) {
           Logger.error(TemplateFactory.class,e.getMessage(),e);
           throw new DotDataException(e.getMessage(), e);
       }
	}

	public List<Template> findAllVersions(final Identifier identifier, final boolean bringOldVersions)
			throws DotDataException {
		if(!UtilMethods.isSet(identifier) || !UtilMethods.isSet(identifier.getId())) {
			return new ArrayList<>();
		}

		final DotConnect dc = new DotConnect();
		final StringBuffer query = new StringBuffer();

		if(bringOldVersions) {
			query.append(templateSQL.FIND_ALL_VERSIONS_BY_IDENTIFIER);

		} else {//This only brings the inode of the working and live version
			query.append(templateSQL.FIND_WORKING_LIVE_VERSION_BY_IDENTIFIER);
		}

		dc.setSQL(query.toString());
		dc.addParam(identifier.getId());
		final List<Map<String,Object>> results=dc.loadObjectResults();
		final List<Template> templateAllVersions = new ArrayList<>();
		for(Map<String,Object> result : results) {
			final Template template = find(result.get("inode").toString());
			templateAllVersions.add(template);
		}
		return templateAllVersions;
	}

	public void deleteTemplateByInode(final String templateInode) throws DotDataException {
		deleteTemplateInDB(templateInode);
		deleteInodeInDB(templateInode);
	}

	private void deleteInodeInDB(final String inode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.DELETE_INODE);
		dc.addParam(inode);
		dc.loadResult();
	}

	private void deleteTemplateInDB(final String inode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.DELETE_TEMPLATE_BY_INODE);
		dc.addParam(inode);
		dc.loadResult();
	}

	public List<Template> findTemplatesByContainerInode(final String containerInode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(templateSQL.FIND_TEMPLATES_BY_CONTAINER_INODE);
		dc.addParam(containerInode);
		return TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();
	}
}
