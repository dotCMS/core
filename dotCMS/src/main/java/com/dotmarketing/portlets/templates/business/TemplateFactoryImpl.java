package com.dotmarketing.portlets.templates.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.design.bean.*;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.google.common.io.LineReader;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.beanutils.BeanUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dotcms.util.FunctionUtils.ifOrElse;
import static com.dotmarketing.util.StringUtils.builder;

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

			template = (Template) TransformerLocator.createTemplateTransformer(templateResults).findFirst();

			if(template != null && template.getInode() != null) {
				templateCache.add(inode, template);
			}
		}

		return template;
	}

	/*
	 * Determine if the template is a file based.
	 */
	private boolean isValidTemplatePath(final Folder folder) {
		return null != folder && UtilMethods.isSet(folder.getPath()) && folder.getPath().contains(Constants.TEMPLATE_FOLDER_PATH);
	}

	/**
	 * If a folder under application/templates/ has a vtl with the name: template.vtl, means it is a template file based.
	 * @param host    {@link Host}
	 * @param folder  folder
	 * @return boolean
	 */
	private Identifier getTemplate(final Host host, final Folder folder) {
		try {

			final Identifier identifier = APILocator.getIdentifierAPI().find(host, builder(folder.getPath(),
					Constants.TEMPLATE_META_INFO_FILE_NAME).toString());
			return identifier!=null  && UtilMethods.isSet(identifier.getId()) ? identifier : null;
		} catch (Exception  e) {
			Logger.warnAndDebug(this.getClass(),e);
			return null;
		}
	}

	/*
	 * Finds the template on the file system, base on a folder
	 * showLive in true means get just template published.
	 */
	private List<FileAsset> findTemplateAssets(final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {
		return APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, null, showLive, user, false);
	}

	@Override
	public Template getTemplateByFolder(final Host host, final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {

		if (!this.isValidTemplatePath(folder)) {

			throw new NotFoundInDbException("On getting the template by folder, the folder: " + (folder != null ? folder.getPath() : "Unknown" ) +
					" is not valid, it must be under: " + Constants.TEMPLATE_FOLDER_PATH + " and must have a child file asset called: " +
					Constants.TEMPLATE_META_INFO_FILE_NAME);
		}

		final Identifier identifier = getTemplate(host, folder);
		if(identifier==null) {

			throw new NotFoundInDbException("no template found under: " + folder.getPath() );
		}

		final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI().
				getContentletVersionInfo(identifier.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());

		if(!contentletVersionInfo.isPresent()) {
			throw new DotDataException("Can't find ContentletVersionInfo. Identifier:"
					+ identifier.getId() + ". Lang:"
					+ APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		final String inode = showLive && UtilMethods.isSet(contentletVersionInfo.get().getLiveInode()) ?
				contentletVersionInfo.get().getLiveInode() : contentletVersionInfo.get().getWorkingInode();
		Template template = templateCache.get(inode);

		if(template==null || !InodeUtils.isSet(template.getInode())) {

			synchronized (identifier) {

				if(template==null || !InodeUtils.isSet(template.getInode())) {

					template = FileAssetTemplateUtil.getInstance().fromAssets (host, folder,
							this.findTemplateAssets(folder, user, showLive), showLive);
					if(template != null && InodeUtils.isSet(template.getInode())) {

						templateCache.add(inode, template);
					}
				}
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

		final PaginatedArrayList<Template> assets = new PaginatedArrayList<>();
		final List<Permissionable> toReturn       = new ArrayList<>();
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
		query.append("select asset.*, identifier.* from ");
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

		orderBy = SQLUtil.sanitizeSortBy(orderBy);
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

			// adding the template from the site browser
			toReturn.addAll(this.findFolderAssetTemplate(user, hostId, orderBy,
					includeArchived, params != null? params.values(): Collections.emptyList()));

			if(countLimit > 0 && toReturn.size() < countLimit + offset) {

				while (!done) {
					dc.setStartRow(internalOffset);
					dc.setMaxRows(internalLimit);

					resultList = TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();

					toReturn.addAll(APILocator.getPermissionAPI().filterCollection(
							resultList, PermissionAPI.PERMISSION_READ, false, user));
					if (countLimit > 0 && toReturn.size() >= countLimit + offset) {
						done = true;
					} else if (resultList.size() < internalLimit) {
						done = true;
					}

					internalOffset += internalLimit;
				}
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

	private List<Contentlet> filterContainersAssetsByLanguage(final List<Contentlet> contentTemplates, final long defaultLanguageId) {

		final List<Contentlet> uniqueContentTemplates   				 = new ArrayList<>();
		final Map<String, List<Contentlet>> contentletsGroupByIdentifier = CollectionsUtils.groupByKey(contentTemplates, Contentlet::getIdentifier);

		for (final Map.Entry<String, List<Contentlet>> entry : contentletsGroupByIdentifier.entrySet()) {

			if (entry.getValue().size() <= 1) {

				uniqueContentTemplates.addAll(entry.getValue());
			} else {

				ifOrElse(entry.getValue().stream()
								.filter(contentlet -> contentlet.getLanguageId() == defaultLanguageId).findFirst(), // if present the one with the default lang take it
						uniqueContentTemplates::add,
						()->uniqueContentTemplates.add(entry.getValue().get(0))); // otherwise take any one, such as the first one
			}
		}

		return uniqueContentTemplates;
	}

	/**
	 * Finds the container.vtl on a specific host
	 * returns even working versions but not archived
	 * the search is based on the ES Index.
	 * If exists multiple language version, will consider only the versions based on the default language, so if only exists a container.vtl with a non-default language it will be skipped.
	 *
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param includeArchived {@link Boolean} if wants to include archive containers
	 * @return List of Folder
	 */
	private List<Folder> findTemplatesAssetsByHost(final Host host, final User user, final boolean includeArchived) {

		List<Contentlet>           templates = null;
		final List<Folder>         folders   = new ArrayList<>();

		try {

			final StringBuilder queryBuilder = builder("+structureType:", BaseContentType.FILEASSET.getType(),
					" +path:",    Constants.TEMPLATE_FOLDER_PATH, "/*",
					" +path:*/" + Constants.TEMPLATE_META_INFO_FILE_NAME,
					" +working:true",
					includeArchived? StringPool.BLANK : " +deleted:false");

			if (null != host) {

				queryBuilder.append(" +conhost:" + host.getIdentifier());
			}

			final String query = queryBuilder.toString();

			templates =
					this.filterContainersAssetsByLanguage (APILocator.getPermissionAPI().filterCollection(
							APILocator.getContentletAPI().search(query,-1, 0, null , user, false),
							PermissionAPI.PERMISSION_READ, false, user),
							APILocator.getLanguageAPI().getDefaultLanguage().getId());



			for(final Contentlet container : templates) {

				folders.add(APILocator.getFolderAPI().find(container.getFolder(), user, false));
			}
		} catch (Exception e) {

			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return folders;
	}

	/**
	 * Gets the folder templates based on the host and sub folders.
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param subFolders {@link List}
	 * @param includeHostOnPath {@link Boolean} true if you want to  include the host on the template path
	 * @return List of Templates
	 * @throws DotDataException
	 */
	private List<Template> getFolderTemplates(final Host host, final User user,
											   final List<Folder> subFolders, final boolean includeHostOnPath) throws DotDataException {

		final List<Template> templates = new ArrayList<>();
		for (final Folder subFolder : subFolders) {

			try {

				final User      userFinal = null != user? user: APILocator.systemUser();
				final Template template = this.getTemplateByFolder(null != host? host:APILocator.getHostAPI().find(subFolder.getHostId(), user, includeHostOnPath),
						subFolder, userFinal, false);
				templates.add(template);
			} catch (DotSecurityException e) {

				Logger.debug(this, () -> "Does not have permission to read the folder container: " + subFolder.getPath());
			} catch (NotFoundInDbException e) {

				Logger.debug(this, () -> "The folder: " + subFolder.getPath() + ", is not a container");
			}
		}

		return templates;
	}

	/**
	 * Finds all file base template for an user. Also order by orderByParam values (title asc, title desc, modDate asc, modDate desc)
	 * @param user {@link User} to check the permissions
	 * @param hostId {@link String} host id to find the containers
	 * @param orderByParam {@link String} order by parameter
	 * @param includeArchived {@link Boolean} if wants to include archive containers
	 **/
	private Collection<? extends Permissionable> findFolderAssetTemplate(final User user, final String hostId,
																		 final String orderByParam, final boolean includeArchived,
																		 final Collection<Object> filterByNameCollection) {

		try {

			final Host host     		   = APILocator.getHostAPI().find(hostId, user, false);
			final List<Folder> subFolders  = this.findTemplatesAssetsByHost(host, user, includeArchived);
			List<Template> templates       = this.getFolderTemplates(host, user, subFolders, false);

			if (UtilMethods.isSet(filterByNameCollection)) {

				templates = templates.stream().filter(container -> {

					for (final Object name : filterByNameCollection) {

						if (container.getName().toLowerCase().contains(name.toString())) {
							return true;
						}
					}
					return false;
				}).collect(Collectors.toList());
			}

			if (UtilMethods.isSet(orderByParam)) {
				switch (orderByParam.toLowerCase()) {
					case "title asc":
						templates.sort(Comparator.comparing(Template::getTitle));
						break;

					case "title desc":
						templates.sort(Comparator.comparing(Template::getTitle).reversed());
						break;

					case "moddate asc":
						templates.sort(Comparator.comparing(Template::getModDate));
						break;

					case "moddate desc":
						templates.sort(Comparator.comparing(Template::getModDate).reversed());
						break;
				}
			}

			return templates;
		} catch (Exception e) {

			Logger.error(this, e.getMessage(), e);
			return Collections.emptyList();
		}
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
