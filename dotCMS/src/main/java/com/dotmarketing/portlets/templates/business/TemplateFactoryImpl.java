package com.dotmarketing.portlets.templates.business;

import static com.dotcms.util.FunctionUtils.ifOrElse;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.rendering.velocity.directive.DotCacheDirective;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rendering.velocity.util.VelocityUtil;
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
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.VelocityContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateFactoryImpl implements TemplateFactory {

	public static final String CONTENTLET_INODE_TABLE_FIELD = "inode";
	private static final String FILTER_STRING = "filter";
	private static TemplateCache templateCache = CacheLocator.getTemplateCache();

	@SuppressWarnings("unchecked")

	public Template find(final String inode) throws DotStateException, DotDataException {

		Template template = templateCache.get(inode);

		if(template==null){
			final List<Map<String, Object>> templateResults = new DotConnect()
					.setSQL(TemplateSQL.FIND_BY_INODE)
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

	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesAssignedTo(final Host parentHost, final boolean includeArchived)
			throws DotDataException {
		final DotConnect dc = new DotConnect();
		final String query = !includeArchived ?
				TemplateSQL.FIND_TEMPLATES_BY_HOST_INODE + " and vi.deleted = "
						+ DbConnectionFactory.getDBFalse() : TemplateSQL.FIND_TEMPLATES_BY_HOST_INODE;
		dc.setSQL(query);
		dc.addParam(parentHost.getIdentifier());


		return TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();


	}


	@SuppressWarnings("unchecked")
	public List<Template> findTemplatesUserCanUse(final User user, final String hostId, final String query, final boolean searchHost , final int offset, final int limit) throws DotDataException, DotSecurityException {
		return findTemplates(user, false,
				UtilMethods.isSet(query) ? Map.of(FILTER_STRING, query.toLowerCase())
						: null, hostId, null, null, null, offset, limit, "title");
	}

	public void save(final Template template) throws DotDataException {
		save(template, UUIDGenerator.generateUuid());
	}

	@WrapInTransaction
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
		dc.setSQL(TemplateSQL.INSERT_INODE);
		dc.addParam(template.getInode());
		dc.addParam(template.getiDate());
		dc.addParam(template.getOwner());
		dc.loadResult();
	}

	private void insertTemplateInDB(final Template template) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(TemplateSQL.INSERT_TEMPLATE);
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
		if(null!=template && UtilMethods.isSet(template.getInode())) {
			templateCache.remove(template.getInode());
			new TemplateLoader().invalidate(template);
			CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
		}
	}

	@SuppressWarnings("unchecked")
	public Template findWorkingTemplateByName(String name, Host host) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(TemplateSQL.FIND_WORKING_TEMPLATE_BY_HOST_INODE_AND_TITLE);
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

		PaginatedArrayList<Template> assets = new PaginatedArrayList<>();
		List<Permissionable> toReturn = new ArrayList<>();
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
				final String filter = "%"+params.get(FILTER_STRING).toString().toLowerCase()+"%";
				dc.addParam(filter);
				dc.addParam(filter);
				dc.addParam(filter);
			}

			// adding the templates from the site browser
			toReturn.addAll(this.findFolderAssetTemplate(user, hostId, orderBy,
					includeArchived, params != null ? params.get(FILTER_STRING).toString(): null));

			if(countLimit > 0 && toReturn.size() < countLimit + offset) {//If haven't reach the amount of templates requested
				while (!done) {
					dc.setStartRow(internalOffset);
					dc.setMaxRows(internalLimit);

					resultList = TransformerLocator
							.createTemplateTransformer(dc.loadObjectResults()).asList();

					//Search by inode
					if (resultList.isEmpty()) {
						final Template templateInode =
								params != null ? find(params.get(FILTER_STRING).toString()) : null;
						resultList =
								templateInode != null ? List.of(templateInode)
										: Collections.emptyList();
					}

					PermissionAPI permAPI = APILocator.getPermissionAPI();
					toReturn.addAll(
							permAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ,
									false, user));
					if (countLimit > 0 && toReturn.size() >= countLimit + offset)
						done = true;
					else if (resultList.size() < internalLimit)
						done = true;

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



	@Override
	public List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		List<Container> result = new ArrayList<>();
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
		Set<String> ids = new HashSet<>();
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

	@Override
	public List<ContainerUUID> getContainerUUIDFromHTML(final String templateBody) {

		if(!UtilMethods.isSet(templateBody)){
			return Collections.emptyList();
		}

		final List<ContainerUUID> containerUUIDS = new ArrayList<>();
		try {
			// Get the default host to use for parsing the template
			final Host host = Try.of(()-> APILocator.getHostAPI().findDefaultHost(
					APILocator.systemUser(), false)).getOrElse(APILocator.systemHost());
			final String hostname = UtilMethods.isSet(host) ?
					host.getHostname() : "dotcms.com"; // fake host

			// Get the current request or create a fake request and response to parse the template
			final HttpServletRequest requestProxy = HttpServletRequestThreadLocal.INSTANCE.getRequest() != null ?
					HttpServletRequestThreadLocal.INSTANCE.getRequest() :
					new FakeHttpRequest(hostname, StringPool.FORWARD_SLASH).request();
			final HttpServletResponse responseProxy = HttpServletResponseThreadLocal.INSTANCE.getResponse() != null ?
					HttpServletResponseThreadLocal.INSTANCE.getResponse() : new BaseResponse().response();

			// Create a Velocity context with the fake request and response
			// and parse the template body to extract container UUIDs
			final VelocityContext context = VelocityUtil.getInstance().getContext(requestProxy, responseProxy);
			context.put(DotCacheDirective.DONT_USE_DIRECTIVE_CACHE, Boolean.TRUE); // Disable cache for this parsing
			context.put(ParseContainer.DONT_LOAD_CONTAINERS, Boolean.TRUE); // Disable loading containers
			VelocityUtil.eval(templateBody, context);

			final Object containerIdsObj = context.get(ParseContainer.DOT_TEMPLATE_CONTAINER_IDS);
			if (containerIdsObj instanceof Collection<?>) {
				final Collection<?> containerIds = (Collection<?>) containerIdsObj;
				for (final Object containerIdObj : containerIds) {
					final Pair<?, ?> containerId = (Pair<?, ?>) containerIdObj;
					containerUUIDS.add(new ContainerUUID(
							(String) containerId.getLeft(),
							(String) containerId.getRight()));
				}
			}
		} catch (Exception e) {
			Logger.error(this, "Error parsing template body to get container", e);
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
		dc.setSQL(TemplateSQL.UPDATE_INODE);
		dc.addParam(template.getiDate());
		dc.addParam(template.getOwner());
		dc.addParam(template.getInode());
		dc.loadResult();
	}

	private void updateTemplateInDB(final Template template) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(TemplateSQL.UPDATE_TEMPLATE);
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
			dc.setSQL(TemplateSQL.FIND_TEMPLATES_BY_MOD_USER);
			dc.addParam(userId);
			List<HashMap<String, String>> templates = dc.loadResults();

			dc.setSQL(TemplateSQL.UPDATE_MOD_USER_BY_MOD_USER);
			dc.addParam(replacementUserId);
			dc.addParam(userId);
			dc.loadResult();

			dc.setSQL(TemplateSQL.UPDATE_LOCKED_BY);
			dc.addParam(replacementUserId);
			dc.addParam(userId);
			dc.loadResult();

			for(HashMap<String, String> ident:templates){
				String inode = ident.get(CONTENTLET_INODE_TABLE_FIELD);
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
			query.append(TemplateSQL.FIND_ALL_VERSIONS_BY_IDENTIFIER);

		} else {//This only brings the inode of the working and live version
			query.append(TemplateSQL.FIND_WORKING_LIVE_VERSION_BY_IDENTIFIER);
		}

		dc.setSQL(query.toString());
		dc.addParam(identifier.getId());
		final List<Map<String,Object>> results=dc.loadObjectResults();
		final List<Template> templateAllVersions = new ArrayList<>();
		for(Map<String,Object> result : results) {
			final Template template = find(result.get(CONTENTLET_INODE_TABLE_FIELD).toString());
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
		dc.setSQL(TemplateSQL.DELETE_INODE);
		dc.addParam(inode);
		dc.loadResult();
	}

	private void deleteTemplateInDB(final String inode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(TemplateSQL.DELETE_TEMPLATE_BY_INODE);
		dc.addParam(inode);
		dc.loadResult();
	}

	public List<Template> findTemplatesByContainerInode(final String containerInode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL(TemplateSQL.FIND_TEMPLATES_BY_CONTAINER_INODE);
		dc.addParam(containerInode);
		return TransformerLocator.createTemplateTransformer(dc.loadObjectResults()).asList();
	}

	/**
	 * Get a template based on a folder (non-db) A folder could be considered as a template if:
	 * 1) Is under /application/templates
	 * 2) Has a file called properties.vtl
	 *
	 * And convert it into a Template
	 * @param site site where the folder lives
	 * @param folder folder that should be a template
	 * @return Template
	 */
	@Override
	public Template getTemplateByFolder(final Host site, final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {

		if (!this.isValidTemplateFolderPath(folder)) {

			throw new NotFoundInDbException("On getting the template by folder, the folder: " + (folder != null ? folder.getPath() : "Unknown" ) +
					" is not valid, it must be under: " + Constants.TEMPLATE_FOLDER_PATH + " and must have a child file asset called: " +
					Constants.TEMPLATE_META_INFO_FILE_NAME);
		}

		final Identifier propertiesIdentifer = getTemplatePropertiesIdentifer(site, folder);
		if(propertiesIdentifer==null) {
			throw new NotFoundInDbException("No template found under: " + folder.getPath() );
		}

		final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI().
				getContentletVersionInfo(propertiesIdentifer.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());

		if(contentletVersionInfo.isEmpty()) {
			throw new DotDataException("Can't find ContentletVersionInfo. Identifier:"
					+ propertiesIdentifer.getId() + ". Lang:"
					+ APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		final String inode = showLive  ?
				contentletVersionInfo.get().getLiveInode() : contentletVersionInfo.get().getWorkingInode();
		Template template = templateCache.get(inode);

		if(template==null || !InodeUtils.isSet(template.getInode())) {

			synchronized (propertiesIdentifer) {

				if(template==null || !InodeUtils.isSet(template.getInode())) {

					template = FileAssetTemplateUtil.getInstance().fromAssets (site, folder,
							this.findTemplateAssets(folder, user, showLive));
					if(template != null && InodeUtils.isSet(template.getInode())) {

						templateCache.add(inode, template);
					}
				}
			}
		}

		return template;
	}

	@Override
	public List<HTMLPageVersion> getPages(final String templateId)
			throws DotDataException, DotSecurityException {

		final DotConnect dotConnect = new DotConnect();
		dotConnect.setSQL(TemplateSQL.GET_PAGES_BY_TEMPLATE_ID);
		dotConnect.addParam(templateId);

		return ((List<Map<String, String>>) dotConnect.loadResults()).stream()
				.map(mapEntry -> new HTMLPageVersion.Builder().identifier(mapEntry.get("identifier"))
						.variantName(mapEntry.get("variant"))
						.build()
				)
				.collect(Collectors.toList());
	}

	/**
	 * Determine if the folder lives under /application/templates
	 * @param folder to check
	 * @return true if the folder lives under /application/templates
	 */
	private boolean isValidTemplateFolderPath(final Folder folder) {
		return null != folder && UtilMethods.isSet(folder.getPath()) && folder.getPath().contains(Constants.TEMPLATE_FOLDER_PATH);
	}

	/**
	 * Finds the Identifier of the properties.vtl file of the template folder
	 * @param host    site where the folder lives
	 * @param folder  template folder
	 * @return identifier of the properties.vtl file
	 */
	private Identifier getTemplatePropertiesIdentifer(final Host host, final Folder folder) {
		try {

			final Identifier identifier = APILocator.getIdentifierAPI().find(host, StringUtils.builder(folder.getPath(),
					Constants.TEMPLATE_META_INFO_FILE_NAME).toString());
			return identifier!=null  && UtilMethods.isSet(identifier.getId()) ? identifier : null;
		} catch (Exception  e) {
			Logger.warnAndDebug(this.getClass(),e);
			return null;
		}
	}

	/**
	 * Finds the list of file assets of a template based on a folder.
	 * First search in the index then in the DB(this is because of PP)
	 * @param folder where the files live
	 * @param user
	 * @param showLive get the working or the live assets
	 * @return list of assets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private List<FileAsset> findTemplateAssets(final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {
		List<FileAsset> assetList = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, null, showLive, user, false);
		if(assetList.isEmpty()){
			final List<Contentlet> dbSearch = showLive ? APILocator.getFolderAPI().getLiveContent(folder,user,false).stream()
					.filter(contentlet -> contentlet.getBaseType().get().equals(BaseContentType.FILEASSET))
					.collect(Collectors.toList()):
					 APILocator.getFolderAPI().getWorkingContent(folder,user,false).stream()
							 .filter(contentlet -> contentlet.getBaseType().get().equals(BaseContentType.FILEASSET))
							.collect(Collectors.toList());
			assetList = APILocator.getFileAssetAPI().fromContentlets(dbSearch);
		}
		return assetList;
	}

	/**
	 * Finds all the properties.vtl on a specific host
	 * returns even working versions but not archived
	 * the search is based on the DB.
	 * If exists multiple language version, will consider only the versions based on the default language,
	 * so if only exists a properties.vtl with a non-default language it will be skipped.
	 *
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param includeArchived {@link Boolean} if wants to include archive templates
	 * @return List of Folder where the files lives
	 */
	private List<Folder> findTemplatesAssetsByHost(final Host host, final User user, final boolean includeArchived) {

		List<Contentlet>           templates = null;
		final List<Folder>         folders   = new ArrayList<>();

		final StringBuilder sqlQuery = new StringBuilder("select cvi.working_inode as inode from contentlet_version_info cvi, identifier id where"
				+ " id.parent_path like ? and id.asset_name = ? and cvi.identifier = id.id");
		final List<Object> parameters = new ArrayList<>();
		parameters.add(Constants.TEMPLATE_FOLDER_PATH + StringPool.FORWARD_SLASH + StringPool.PERCENT);
		parameters.add(Constants.TEMPLATE_META_INFO_FILE_NAME);

		if(!includeArchived){
			sqlQuery.append(" and cvi.deleted = " + DbConnectionFactory.getDBFalse());
		}

		if(null != host){
			sqlQuery.append(" and id.host_inode = ?");
			parameters.add(host.getIdentifier());
		}

		final DotConnect dc = new DotConnect().setSQL(sqlQuery.toString());
		parameters.forEach(dc::addParam);

		try {
			final List<Map<String,String>> inodesMapList =  dc.loadResults();

			final List<String> inodes = new ArrayList<>();
			for (final Map<String, String> versionInfoMap : inodesMapList) {
				inodes.add(versionInfoMap.get(CONTENTLET_INODE_TABLE_FIELD));
			}

			final List<Contentlet> contentletList  = APILocator.getContentletAPI().findContentlets(inodes);

			templates =
					this.filterTemplatesAssetsByLanguage (APILocator.getPermissionAPI().filterCollection(contentletList,
							PermissionAPI.PERMISSION_READ, false, user),
							APILocator.getLanguageAPI().getDefaultLanguage().getId());

			for(final Contentlet template : templates) {
				folders.add(APILocator.getFolderAPI().find(template.getFolder(), user, false));
			}
		} catch (Exception e) {

			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return folders;
	}

	/**
	 * Filters a list of contentlets (templates as files) .
	 *
	 * Groups the templates by identifier (could be that the files were created multilanguage) and gets the one in the language specified
	 * if not get any.
	 * @param contentTemplates list of contentlets that are templates
	 * @param languageId
	 * @return list of contentlets (template properties.vtl) one per Identifier
	 */
	private List<Contentlet> filterTemplatesAssetsByLanguage(final List<Contentlet> contentTemplates, final long languageId) {

		final List<Contentlet> uniqueContentTemplates = new ArrayList<>();
		final Map<String, List<Contentlet>> contentletsGroupByIdentifier = CollectionsUtils
				.groupByKey(contentTemplates, Contentlet::getIdentifier);

		for (final Map.Entry<String, List<Contentlet>> entry : contentletsGroupByIdentifier.entrySet()) {

			if (entry.getValue().size() <= 1) {
				uniqueContentTemplates.addAll(entry.getValue());
			} else {
				ifOrElse(entry.getValue().stream()
								.filter(contentlet -> contentlet.getLanguageId() == languageId).findFirst(), // if present the one with the default lang take it
						uniqueContentTemplates::add,
						()->uniqueContentTemplates.add(entry.getValue().get(0))); // otherwise take any one, such as the first one
			}
		}

		return uniqueContentTemplates;
	}

	/**
	 * Converts the folders into templates.
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param subFolders {@link List}
	 * @param includeHostOnPath {@link Boolean} true if you want to  include the host on the template path
	 * @return List of Templates
	 * @throws DotDataException
	 */
	private List<Template> convertFoldersToTemplates(final Host host, final User user,
			final List<Folder> subFolders, final boolean includeHostOnPath) throws DotDataException {

		final List<Template> templates = new ArrayList<>();
		for (final Folder subFolder : subFolders) {

			try {

				final User      userFinal = null != user? user: APILocator.systemUser();
				final Template template = this.getTemplateByFolder(null != host? host :
								APILocator.getHostAPI().find(subFolder.getHostId(), user, includeHostOnPath),
						subFolder, userFinal, false);
				templates.add(template);
			} catch (DotSecurityException e) {
				Logger.debug(this, () -> "Does not have permission to read the folder template: " + subFolder.getPath());
			} catch (NotFoundInDbException e) {
				Logger.debug(this, () -> "The folder: " + subFolder.getPath() + ", is not a template");
			}
		}

		return templates;
	}

	/**
	 * Finds all file based templates for an user. Also order by orderByParam values (title asc, title desc, modDate asc, modDate desc)
	 * @param user {@link User} to check the permissions
	 * @param hostId {@link String} host id to find the containers
	 * @param orderByParam {@link String} order by parameter
	 * @param includeArchived {@link Boolean} if wants to include archive containers
	 **/
	private Collection<? extends Permissionable> findFolderAssetTemplate(final User user, final String hostId,
			final String orderByParam, final boolean includeArchived,
			final String filter) {

		try {

			final Host host     		   = APILocator.getHostAPI().find(hostId, user, false);
			final List<Folder> listOfTemplatesFolders  = this.findTemplatesAssetsByHost(host, user, includeArchived);
			List<Template> templates       = this.convertFoldersToTemplates(host, user, listOfTemplatesFolders, false);

			if (UtilMethods.isSet(filter)) {
				templates = templates.stream().filter(template ->
						template.getTitle().toLowerCase().contains(filter))
						.collect(Collectors.toList());
			}

			if (UtilMethods.isSet(orderByParam)) {
				switch (orderByParam.toLowerCase()) {
					default:
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
}
