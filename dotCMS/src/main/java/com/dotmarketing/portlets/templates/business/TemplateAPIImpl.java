package com.dotmarketing.portlets.templates.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.SiteCreatedEvent;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.ApplicationTemplateFolderListener;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.business.TemplateFactory.HTMLPageVersion;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.LayoutChanges;
import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.SystemTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_EDIT;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

/**
 * This implementation of the {@link TemplateAPI} class provides the business logic to manage
 * information related to Templates in dotCMS.
 * <p>Templates are layouts available to users when building new HTML, xHTML or XML pages. Each
 * Template includes one or more Containers, which act as server-side includes. The Containers
 * placed in the Template define the areas on a Page that "permissioned" users will be able to
 * contribute content to, and how that content will be displayed. Templates provide a page layout,
 * and link the Containers to the page.</p>
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class TemplateAPIImpl extends BaseWebAssetAPI implements TemplateAPI, DotInitializer {

	private static final String LAYOUT_FILE_NAME     = "com/dotmarketing/portlets/templates/business/layout.json";
	private static final String SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG = "System template cannot be copied";
	private static final String SYSTEM_TEMPLATE_CANNOT_BE_ARCHIVED_MSG = "System template can not be archived";
	private static final String SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG = "System template can not be deleted";
	private static final String SYSTEM_TEMPLATE_CANNOT_BE_MODIFIED_MSG = "System template can not be modified";

	private final  PermissionAPI    permissionAPI          = APILocator.getPermissionAPI();
	private final  IdentifierAPI    identifierAPI          = APILocator.getIdentifierAPI();
	private final  TemplateFactory  templateFactory        = FactoryLocator.getTemplateFactory();
	private final  ContainerAPI     containerAPI           = APILocator.getContainerAPI();
	private final  Lazy<VersionableAPI> versionableAPI     = Lazy.of(APILocator::getVersionableAPI);
	private final  Lazy<HTMLPageAssetAPI> htmlPageAssetAPI = Lazy.of(APILocator::getHTMLPageAssetAPI);
	private final  HostAPI          hostAPI                = APILocator.getHostAPI();
	private final  Lazy<Template>   systemTemplate         = Lazy.of(SystemTemplate::new);

	private final transient MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();


	@Override
	public Template systemTemplate() {
		return this.systemTemplate.get();
	}

	@Override
	public void init() {
		Logger.debug(this, ()-> "Initializing the System Template");
		this.systemTemplate.get().setDrawedBody(this.readLayout());
	}

	/**
	 * Reads the Velocity code of the System Template from the appropriate {@link #LAYOUT_FILE_NAME} file. This is the
	 * boilerplate that will be used to render the System Container on a page.
	 *
	 * @return The Velocity code for the System Template.
	 */
	private String readLayout () {

		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		final URL resourceURL    = loader.getResource(LAYOUT_FILE_NAME);
		try {

			Logger.debug(this, ()-> "Reading System Template layout");
			return IOUtils.toString(resourceURL, UtilMethods.getCharsetConfiguration());
		} catch (final Exception e) {
			Logger.error(this,
					String.format("An error occurred when reading System Template code: %s", e.getMessage()), e);
			return "{\n" +
					"  \"body\":{\n" +
					"    \"rows\":[\n" +
					"      {\n" +
					"        \"styleClass\":\"\",\n" +
					"        \"columns\":[\n" +
					"          {\n" +
					"            \"styleClass\":\"\",\n" +
					"            \"leftOffset\":1,\n" +
					"            \"width\":12,\n" +
					"            \"containers\":[\n" +
					"              {\n" +
					"                \"identifier\":\"" + Container.SYSTEM_CONTAINER + "\",\n" +
					"                \"uuid\":\"1\"\n" +
					"              }\n" +
					"            ]\n" +
					"          }\n" +
					"        ]\n" +
					"      }\n" +
					"    ]\n" +
					"  },\n" +
					"  \"header\":true,\n" +
					"  \"footer\":true,\n" +
					"  \"sidebar\":null\n" +
					"}\n";
		}
	}

	private List<Template> includeSystemTemplate (final List<Template> inTemplates) {

		Logger.debug(this, ()-> "Including system template on the templates list");

		final PaginatedArrayList<Template> templates = new PaginatedArrayList<>();
		if (inTemplates instanceof PaginatedArrayList) {
			templates.setQuery(PaginatedArrayList.class.cast(inTemplates).getQuery());
			templates.setTotalResults(PaginatedArrayList.class.cast(inTemplates).getTotalResults());
		}
		templates.add(systemTemplate());
		templates.addAll(inTemplates);
		return templates;
	}


	@Override
	@CloseDBIfOpened
	public List<Template> findTemplatesAssignedTo(final Host parentHost) throws DotDataException {

		Logger.debug(this, ()-> "Calling findTemplatesAssignedTo for the host: " + parentHost.getHostname());

		return Host.SYSTEM_HOST.equals(parentHost.getIdentifier())?
				includeSystemTemplate(FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, false)):
				FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, false);
	}

	@Override
	@CloseDBIfOpened
	public List<Template> findTemplatesAssignedTo(final Host parentHost, final boolean includeArchived) throws DotDataException {

		Logger.debug(this, ()-> "Calling findTemplatesAssignedTo for the host: " + parentHost.getHostname()
							+ ", includeArchived: " + includeArchived);

		return Host.SYSTEM_HOST.equals(parentHost.getIdentifier())?
				includeSystemTemplate(FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, includeArchived)):
				FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, includeArchived);
	}

	@Override
	@CloseDBIfOpened
	public List<Template> findTemplatesUserCanUse(final User user, final String hostId, final String query, final boolean searchHost, final int offset, final int limit) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling findTemplatesUserCanUse for the user: " + user.getUserId()
				+ ", hostId: " + hostId + ", query: " + query);

		return 0 == offset? // Host.SYSTEM_HOST.equals(searchHost)?
				includeSystemTemplate(FactoryLocator.getTemplateFactory().findTemplatesUserCanUse(user, hostId, query, searchHost, offset, limit)):
				FactoryLocator.getTemplateFactory().findTemplatesUserCanUse(user, hostId, query, searchHost, offset, limit);
	}

	@WrapInTransaction
	@Override
	public Template copy(final Template sourceTemplate, final User user) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling copy template for the user: " + user.getUserId()
				+ ", sourceTemplate: " + sourceTemplate.getIdentifier());

		if(Template.SYSTEM_TEMPLATE.equals(sourceTemplate.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
		}

		final Identifier identifier = APILocator.getIdentifierAPI().find(sourceTemplate.getIdentifier());
		final Host  host = APILocator.getHostAPI().find(identifier.getHostId(), user, false);

		return copy(sourceTemplate, host, false, false, user, false);
	}

	@WrapInTransaction
	@Override
	public Template copy(final Template sourceTemplate, final Host destination, final boolean forceOverwrite, final List<ContainerRemapTuple> containerMappings, final User user,
			final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling copy template for the user: " + user.getUserId()
				+ ", sourceTemplate: " + sourceTemplate.getIdentifier() + ", destination: " + destination.getHostname());

		if(Template.SYSTEM_TEMPLATE.equals(sourceTemplate.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
		}

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to READ the source template");
			throw new DotSecurityException("You don't have permission to read the source template");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to WRITE in the destination site");
			throw new DotSecurityException("You don't have permission to write in the destination site.");
		}

		boolean isNew = false;
		Template newTemplate;
		if (forceOverwrite) {
			newTemplate = FactoryLocator.getTemplateFactory().findWorkingTemplateByName(sourceTemplate.getTitle(), destination);
			if (newTemplate == null) {
				isNew = true;
				newTemplate  =templateFactory.copyTemplate(sourceTemplate, destination);
			}
		} else {
			isNew = true;
			newTemplate  =templateFactory.copyTemplate(sourceTemplate, destination);
		}


		newTemplate.setModDate(new Date());
		newTemplate.setModUser(user.getUserId());

		if (isNew) {
			// creates new identifier for this webasset and persists it
			Identifier newIdentifier = com.dotmarketing.business.APILocator.getIdentifierAPI().createNew(newTemplate, destination);
			Logger.debug(TemplateFactory.class, "Parent newIdentifier=" + newIdentifier.getId());

			newTemplate.setIdentifier(newIdentifier.getId());
			// persists the webasset
			save(newTemplate);

			//Copy the host again
			newIdentifier.setHostId(destination.getIdentifier());
		} else {
			saveTemplate(newTemplate, destination, user, respectFrontendRoles);
		}

		APILocator.getVersionableAPI().setWorking(newTemplate);
		if(sourceTemplate.isLive()){
		    APILocator.getVersionableAPI().setLive(newTemplate);
		} else if(sourceTemplate.isArchived()) {
			APILocator.getVersionableAPI().setDeleted(newTemplate, true);
		}
		// Copy permissions
		permissionAPI.copyPermissions(sourceTemplate, newTemplate);

		ActivityLogger.logInfo(this.getClass(), "Copied Template", "User " +
				user.getPrimaryKey() + " copied template" + newTemplate.getTitle(), destination.getTitle() != null ? destination.getTitle() : "default");

		return newTemplate;
	}

	@Override
	@WrapInTransaction
	public Template copy(final Template sourceTemplate, final Host destination, final boolean forceOverwrite,
			final boolean copySourceContainers, User user, final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		Logger.debug(this, ()-> "Calling copy template for the user: " + user.getUserId()
				+ ", sourceTemplate: " + sourceTemplate.getIdentifier() + ", destination: " + destination.getHostname());

		if(Template.SYSTEM_TEMPLATE.equals(sourceTemplate.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_COPIED_MSG);
		}

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user,
				respectFrontendRoles)) {
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to READ the source template");
			throw new DotSecurityException("You don't have permission to read the source template");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to WRITE in the destination site");
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		final List<ContainerRemapTuple> remap = new LinkedList<>();
		if (copySourceContainers) {
			final List<Container> sourceContainers = getContainersInTemplate(sourceTemplate, user, respectFrontendRoles);
			Container newContainer;
			for (final Container container : sourceContainers) {
				newContainer = containerAPI.copy(container, destination, user, respectFrontendRoles);
				remap.add(new ContainerRemapTuple(container, newContainer));
			}
		}

		return copy(sourceTemplate, destination, forceOverwrite, remap, user, respectFrontendRoles);
	}

	private void save(final Template template) throws DotDataException {

		Logger.debug(this, ()-> "Saving template: " + template);
		templateFactory.save(template);
	}

	protected void save(final WebAsset webAsset) throws DotDataException {

		Logger.debug(this, ()-> "Saving template, webasset: " + webAsset);
		save((Template) webAsset);
	}

	@Override
	@WrapInTransaction
	public void publishTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, WebAssetException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, "System template can not be published");
			return;
		}

		Logger.debug(this, ()-> "Publishing the template: " + template.getIdentifier());

		//Check Publish Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_PUBLISH, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Publish the Template");
			throw new DotSecurityException("User does not have Permissions to Publish the Template");
		}

		// Check that the template is archived
		if(isArchived(template)){
			Logger.error(this, "The Template: " + template.getName() + " can not be publish. "
					+ "Because it is archived");
			throw new DotStateException("Template can not be published because is archived");
		}

		publishTemplate(template,user);
	}

	/**
	 * This method was extracted from {@link PublishFactory#publishAsset(Inode, User, boolean)},
	 * since in the future template wont inherit from WebAsset
	 * @param template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void publishTemplate(final Template template,final User user)
			throws DotSecurityException, DotDataException, WebAssetException {

		Logger.debug(this, ()-> "Publishing the template: " + template.getIdentifier()
				+ ", user: " + user.getUserId());

		final Template templateWorkingVersion = findWorkingTemplate(template.getIdentifier(),
				APILocator.systemUser(), false);
		//Sets Working as Live
		setLive(template);
		if(template.getIdentifier().contains(Constants.TEMPLATE_FOLDER_PATH)){
			//inode of the template is the inode of the properties.vtl
			//we need to obtain the folder so publish all the files related to template as files
			final Contentlet propertiesVTL = APILocator.getContentletAPI().find(template.getInode(),user,false);
			final Identifier idPropertiesVTL = APILocator.getIdentifierAPI().find(propertiesVTL.getIdentifier());
			final Folder templateFolder = APILocator.getFolderAPI().findFolderByPath(idPropertiesVTL.getParentPath(),
					idPropertiesVTL.getHostId(),user,false);
			PublishFactory.publishAsset(templateFolder,user,false,false);
		} else {
			//Gets all Containers In the Template
			final List<Container> containersInTemplate = APILocator.getTemplateAPI()
					.getContainersInTemplate(template, APILocator.getUserAPI().getSystemUser(),
							false);
			for (final Container container : containersInTemplate) {
				Logger.debug(PublishFactory.class,
						"*****I'm a Template -- Publishing my Container Child= " + container
								.getInode());
				if (!container.isLive()) {
					PublishFactory.publishAsset(container, user, false);
				}
			}
			templateWorkingVersion.setModDate(new java.util.Date());
			templateWorkingVersion.setModUser(user.getUserId());
			templateFactory.save(template);
		}
		//Clean-up the cache for this template
		CacheLocator.getTemplateCache().remove(template.getInode());
		//writes the template to a live directory under velocity folder
		new TemplateLoader().invalidate(template);
	}

	@Override
	@WrapInTransaction
	public void unpublishTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, "System template can not be unpublished");
			throw new IllegalArgumentException("System template can not be unpublished");
		}

		Logger.debug(this, ()-> "Unpublishing the template: " + template.getIdentifier());

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}

		// Check that the template is archived
		if(isArchived(template)){
			Logger.error(this, "The Template: " + template.getName() + " can not be unpublish. "
					+ "Because it is archived");
			throw new DotStateException("Template can not be unpublished because is archived");
		}

		unpublishTemplate(template,user);
	}

	/**
	 * This method was extracted from {@link WebAssetFactory#unPublishAsset(WebAsset, String, Inode)},
	 * since in the future template wont inherit from WebAsset
	 * @param template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void unpublishTemplate(final Template template,final User user)
			throws DotSecurityException, DotDataException {

		Logger.debug(this, ()-> "Unpublishing the template: " + template.getIdentifier()
				+ ", user: " + user.getUserId());

		final Template templateWorkingVersion = findWorkingTemplate(template.getIdentifier(),APILocator.systemUser(),false);
		//Remove live version from version_info
		APILocator.getVersionableAPI().removeLive(template.getIdentifier());
		templateWorkingVersion.setModDate(new java.util.Date());
		templateWorkingVersion.setModUser(user.getUserId());
		templateFactory.save(template);
		//remove template from the live directory
		new TemplateLoader().invalidate(template);
	}

	@Override
	@WrapInTransaction
	public void archive (final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling archive for the template: " + template.getIdentifier()
				+ ", user: " + user.getUserId());

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_ARCHIVED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_ARCHIVED_MSG);
		}

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}

		//Check that the template is Unpublished
		if (template.isLive()) {
			Logger.error(this, "The Template: " + template.getName() + " can not be archive. "
					+ "Because it is live.");
			throw new DotStateException("Template must be unpublished before it can be archived");
		}

		archive(template,user);
	}

	/**
	 * This method was extracted from {@link WebAssetFactory#archiveAsset(WebAsset, String)} }, since in the future
	 * template wont inherit from WebAsset
	 * @param template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void archive(final Template template, final User user) throws DotSecurityException, DotDataException {

		Logger.debug(this, ()-> "Calling archive for the template: " + template.getIdentifier()
				+ ", user: " + user.getUserId());

		final Template templateLiveVersion = findLiveTemplate(template.getIdentifier(),APILocator.systemUser(),false);
		final Template templateWorkingVersion = findWorkingTemplate(template.getIdentifier(),APILocator.systemUser(),false);
		if(templateLiveVersion!=null){
			APILocator.getVersionableAPI().removeLive(template.getIdentifier());
		}
		templateWorkingVersion.setModDate(new java.util.Date());
		templateWorkingVersion.setModUser(user.getUserId());
		// sets deleted to true
		APILocator.getVersionableAPI().setDeleted(templateWorkingVersion, true);
		templateFactory.save(templateWorkingVersion);
	}

	@Override
	@WrapInTransaction
	public void unarchive (final Template template, final User user)
			throws DotDataException, DotSecurityException {
		Logger.debug(this, ()-> "Doing unarchive of the template: " + template.getIdentifier());

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, "System template can not be unarchived");
			throw new IllegalArgumentException("System template can not be unarchived");
		}

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}
		// Check that the template is archived
		if(!isArchived(template)){
			Logger.error(this, "The Template: " + template.getName() + " can not be unarchive. "
					+ "Because it is not archived");
			throw new DotStateException("Template must be archived before it can be unarchived");
		}
		template.setModDate(new java.util.Date());
		template.setModUser(user.getUserId());
		APILocator.getVersionableAPI().setDeleted(template, false);
		templateFactory.save(template);
	}

	@Override
	@WrapInTransaction
	public boolean isArchived(final Template template) throws DotDataException, DotStateException,DotSecurityException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {
			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_ARCHIVED_MSG);
			// System template is never archived
			return false;
		}

		return template instanceof FileAssetTemplate ?
				FileAssetTemplate.class.cast(template).isDeleted() :
				this.versionableAPI.get().isDeleted(template);
	}

	@Override
	@WrapInTransaction
	public void deleteTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG);
		}

		Logger.debug(this, ()-> "Doing delete of the template: " + template.getIdentifier());

		// Check Edit Permissions over Template
		if (!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)) {
			final String errorMsg = String.format("User '%s' does not have permission to delete Template " +
					"'%s' [ %s ]", user.getUserId(), template.getName(), template.getInode());
			Logger.error(this, errorMsg);
			throw new DotSecurityException(errorMsg);
		}

		// Check that the template is archived
		if (!isArchived(template)) {
			final String errorMsg = String.format("Template '%s' [ %s ] must be archived before it can be deleted",
					template.getName(), template.getInode());
			Logger.error(this, errorMsg);
			throw new DotStateException(errorMsg);
		}

		// Check that template does not have pages referencing it
		// use system user b/c the user executing this method might not have access to all pages
		final Map<String,String> checkDependencies = checkPageDependencies(template,APILocator.systemUser(),false);
		if (checkDependencies != null && !checkDependencies.isEmpty()) {
			final String errorMsg = String.format("Template '%s' [ %s ] cannot be deleted because it still has pages referencing it: " +
							"%s", template.getName(), template.getInode(), checkDependencies);
			Logger.error(this, errorMsg);
			throw new DotDataValidationException(errorMsg);
		}

		deleteTemplate(template);
	}

	/**
	 * This method was extracted from {@link WebAssetFactory#deleteAsset(WebAsset, User)}, since in the future
	 * template wont inherit from WebAsset
	 * @param template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void deleteTemplate(final Template template)
			throws DotDataException, DotSecurityException {
		// Delete the IDENTIFIER entry from cache
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable(template);
		//Delete Version Info
		APILocator.getVersionableAPI().deleteVersionInfo(template.getIdentifier());
		//Invalidate Template
		new TemplateLoader().invalidate(template);
		//Find all Versions
		final Identifier identifier = APILocator.getIdentifierAPI().find(template.getIdentifier());
		final List<Template> allVersions = findAllVersions(identifier,APILocator.systemUser(),false,true);
		for(final Template template1 : allVersions) {
			//Delete the permission and the inode of each version of the asset
			permissionAPI.removePermissions(template1);
			InodeFactory.deleteInode(template1);
		}
		//Delete Tree entries
		final List<Tree> treeList = new ArrayList<>();
		treeList.addAll(TreeFactory.getTreesByChild(identifier.getInode()));
		treeList.addAll(TreeFactory.getTreesByParent(identifier.getInode()));
		for(Tree tree : treeList) {
			TreeFactory.deleteTree(tree);
		}
		//Delete Identifier
		APILocator.getIdentifierAPI().delete(identifier);
	}

	@Override
	@WrapInTransaction
	public void deleteVersionByInode(final String inode) {

		Logger.debug(this, ()-> "Deleting template inode: " + inode);

		if(Template.SYSTEM_TEMPLATE.equals(inode)) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG);
		}

		Try.run(()->FactoryLocator.getTemplateFactory().deleteTemplateByInode(inode)).onFailure(e -> new RuntimeException(e));
	}

	/*
	 * if the identifier does not exists, will create a new one.
	 * if does not have an inode, will create a new version
	 * If the latest updated user is not the same of "user" argument, will create a new version
	 */
	@Override
	@WrapInTransaction
	public Template saveDraftTemplate(final Template template, final Host host, final User user,
                                      final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_MODIFIED_MSG);
		}

		if (UtilMethods.isSet(template.getInode()) && UtilMethods.isSet(template.getIdentifier())) {

            final Identifier identifier = APILocator.getIdentifierAPI().find(template.getIdentifier());
            if (identifier != null && UtilMethods.isSet(identifier.getId())) {

                this.checkTemplate(template);

                if (!permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user, respectFrontendRoles)) {
                    throw new DotSecurityException("You don't have permission to edit the template.");
                }

                if (template.isDrawed() && !UtilMethods.isSet(template.getDrawedBody())) {
                    throw new DotStateException("Drawed template MUST have a drawed body:" + template);
                }

                if (UtilMethods.isSet(template.getTheme())) {

                    this.setThemeName(template, user, respectFrontendRoles);
                }

                final Template workingTemplate =
                        this.findWorkingTemplate(template.getIdentifier(), user, respectFrontendRoles);
                // Only draft if there is a working version that is not live
                // and always create a new version if the user is different
                if (null != workingTemplate &&
                        !workingTemplate.isLive() && workingTemplate.getModUser().equals(user.getUserId())) {

                    template.setModDate(new Date());
                    // if we are the latest and greatest and are a draft
                    if (!workingTemplate.getInode().equals(template.getInode())) {

                        template.setInode(workingTemplate.getInode());
                    }

                    this.templateFactory.save(template, template.getInode());
                    templateFactory.deleteFromCache(workingTemplate);

                    return template;
                }
            }
        }

        // if identifier do not exists, save new version
        return this.saveTemplate(template, host, user, respectFrontendRoles);

	}


	public void setThemeName (final Template template, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Theme theme = Try.of(() -> APILocator.getThemeAPI().findThemeById(template.getTheme(),user,respectFrontendRoles)).getOrNull();

		if(null != theme && InodeUtils.isSet(theme.getInode())) {

            template.setThemeName(theme.getName());
        }
    }

	@Override
	@WrapInTransaction
	public Template saveTemplate(final Template template, final Host host, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_MODIFIED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_MODIFIED_MSG);
		}

		boolean existingId=false;

		this.checkTemplate(template);

		if(UtilMethods.isSet(template.getIdentifier())) {
		    final Identifier ident=APILocator.getIdentifierAPI().find(template.getIdentifier());
		    existingId = ident!=null && UtilMethods.isSet(ident.getId());
		}

	    //if is an existing template check EDIT permissions, if is new template you need add_children and edit permissions over the host
	    if(existingId){
			if (!permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user, respectFrontendRoles)) {

				Logger.error(this, "You don't have permission to edit the template.");
				throw new DotSecurityException("You don't have permission to edit the template.");
			}
		} else{
			if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles)) {

				Logger.error(this, "You don't have permission to add_children at the site.");
				throw new DotSecurityException("You don't have permission to add_children at the site.");
			}

			if (!permissionAPI.doesUserHavePermissions(host.getIdentifier(),PermissionableType.TEMPLATES, PermissionAPI.PERMISSION_EDIT, user)) {

				Logger.error(this, "You don't have permission to edit templates at site level.");
				throw new DotSecurityException("You don't have permission to edit templates at site level.");
			}
		}

	    if(template.isDrawed() && !UtilMethods.isSet(template.getDrawedBody())) {

			Logger.error(this, "Drawed template MUST have a drawed body:" + template);
	        throw new DotStateException("Drawed template MUST have a drawed body:" + template);
	    }

		if(UtilMethods.isSet(template.getTheme())) {
            this.setThemeName(template, user, respectFrontendRoles);
		}

		if (existingId) {
			final Template oldTemplate = findWorkingTemplate(template.getIdentifier(), user, respectFrontendRoles);
			templateFactory.deleteFromCache(oldTemplate);
		} else{
			//sets the owner so it can be set at the identifier table
			template.setOwner(user.getUserId());
			final Identifier identifier = UtilMethods.isSet(template.getIdentifier()) ?
					APILocator.getIdentifierAPI().createNew(template, host, template.getIdentifier()) :
					APILocator.getIdentifierAPI().createNew(template, host);
			template.setIdentifier(identifier.getId());
		}
		template.setModDate(new Date());
		template.setModUser(user.getUserId());

		save(template);
		APILocator.getVersionableAPI().setWorking(template);

        return template;
	}

	private void checkTemplate(final Template template) throws DotDataException {

		if (template.isAnonymous() && LicenseManager.getInstance().isCommunity()) {

			Logger.warn(this, String.format("License required to save layout: template -> %s", template));
			throw new InvalidLicenseException();
		}

		if (!UtilMethods.isSet(template.getTitle())) {

			Logger.error(this, "Title is required on templates");
			throw new DotDataException("Title is required on templates");
		}
	}

	@CloseDBIfOpened
    @Override
    public List<Container> getContainersInTemplate(final Template template, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling getContainersInTemplate: template: " + template.getIdentifier());
		if (Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {
			return new ImmutableList.Builder<Container>().add(this.containerAPI.systemContainer()).build();
		}

        final List<Container> containers = new ArrayList<>();
        if(template.isDrawed()) {
            final TemplateLayout layout = DotTemplateTool.themeLayout(template.getInode());
            if (layout != null) {
				final List<String> containersId = this.getContainersId(layout);

				for (final String containerIdOrPath : containersId) {

					final Optional<Container> optionalContainer = APILocator.getContainerAPI().findContainer(containerIdOrPath, user, false, false);

					if (optionalContainer.isEmpty()) {
						continue;
					}

					containers.add(optionalContainer.get());
				}
			}
        } else {//only do this if is not drawed

			final Set<ContainerUUID> containerUUIDSet = APILocator.getTemplateAPI().getContainersUUIDFromDrawTemplateBody(
					template.getBody()).stream().collect(Collectors.toSet());
			for (final ContainerUUID containerUUID : containerUUIDSet) {
				final Optional<Container> container = APILocator.getContainerAPI()
						.findContainer(containerUUID.getIdentifier(), user, false, false);
				if(container.isPresent() && !APILocator.getMultiTreeAPI().getMultiTrees(container.get().getIdentifier()).isEmpty()){
					containers.add(container.get());
				}
			}
		}
        return containers.stream().distinct().collect(Collectors.toList());

    }

	@CloseDBIfOpened
    @Override
	public List<ContainerUUID> getContainersUUID(final TemplateLayout layout) {

		Logger.debug(this, ()-> "Calling getContainersInTemplate: layout: " + layout.getLayout());

		final List<ContainerUUID> containerUUIDS = new ArrayList<>();
		final List<TemplateLayoutRow> rows = layout.getBody().getRows();

		for (final TemplateLayoutRow row : rows) {
			final List<TemplateLayoutColumn> columns = row.getColumns();


			for (final TemplateLayoutColumn column : columns) {
				final List<ContainerUUID> columnContainers = column.getContainers();
				containerUUIDS.addAll(columnContainers);
			}
		}

		final Sidebar sidebar = layout.getSidebar();

		if (sidebar != null && sidebar.getContainers() != null) {
			containerUUIDS.addAll(sidebar.getContainers());
		}

		return containerUUIDS;
	}

	@Override
	public List<ContainerUUID> getContainersUUIDFromDrawTemplateBody(final String drawTemplateBody) {

		Logger.debug(this, ()-> "Calling getContainersUUIDFromDrawTemplateBody: drawTemplateBody: " + drawTemplateBody);

		if (!UtilMethods.isSet(drawTemplateBody)) {

			return Collections.emptyList();
		}

		return templateFactory.getContainerUUIDFromHTML(drawTemplateBody);
	}

	private List<String> getContainersId(final TemplateLayout layout) {

		return this.getContainersUUID(layout).stream()
				.map(ContainerUUID::getIdentifier)
				.collect(Collectors.toList());
	}

	@CloseDBIfOpened
	@Override
	public Host getTemplateHost(final Template template) throws DotDataException {

		try {

			Logger.debug(this, ()-> "Calling getTemplateHost: template: " + template.getIdentifier());

			if (Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

				return APILocator.systemHost();
			}

			if(template instanceof FileAssetTemplate){
				return FileAssetTemplateUtil.getInstance().getHost(template.getIdentifier());
			}
			return APILocator.getHostAPI().findParentHost(template, APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e1) {
			Logger.error(TemplateAPIImpl.class, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

	}

	@Override
	@WrapInTransaction
	public boolean delete(final Template template, final User user, final boolean respectFrontendRoles) throws DotSecurityException,
			Exception {

		Logger.debug(this, ()-> "Calling delete: " + template.getIdentifier());
		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG);
			throw new IllegalArgumentException(SYSTEM_TEMPLATE_CANNOT_BE_DELETED_MSG);
		}

		if(permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(template);
		} else {

			Logger.error(this, WebKeys.USER_PERMISSIONS_EXCEPTION);
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	@Override
	@CloseDBIfOpened
	public Template findWorkingTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling findWorkingTemplate: " + id);

		if(Template.SYSTEM_TEMPLATE.equals(id)) {

			return systemTemplate();
		}

		if (FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(id)) {//Check if the id is a path
			return this.findTemplateByPath(id,null, user, respectFrontendRoles, false);
		}

		final Identifier identifier = this.identifierAPI.find(id);//Finds the Identifier so we can get the path
		if (null != identifier &&
				FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(identifier.getPath())) {
			return this.findTemplateByPath(identifier.getPath(),identifier.getHostId(), user, respectFrontendRoles, false);
		}

		//For non-file based templates
		final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(id);
		return (!UtilMethods.isSet(info)) ? null : find(info.getWorkingInode(), user, respectFrontendRoles);

	}

	@Override
	@CloseDBIfOpened
	public List<Template> findTemplates(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String hostId, final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderBy) throws DotSecurityException,
			DotDataException {

		Logger.debug(this, ()->"Calling findTemplates, params " + params + ", user:" + user.getUserId() +
						", inode = " + inode + ", id: " + identifier + ", parent: " + parent);
		return  offset == 0 && !includeArchived? // if it is the first page and do not include archived, include the system template
				this.includeSystemTemplate(templateFactory.findTemplates(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy)):
				this.templateFactory.findTemplates(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	@CloseDBIfOpened
	@Override
	public Template find(final String inode, final User user, final boolean respectFrontEndRoles) throws DotSecurityException,
			DotDataException {

		Logger.debug(this, ()->"Calling find, user:" + user.getUserId() + ", inode = " + inode);

		if(Template.SYSTEM_TEMPLATE.equals(inode)) {

			return systemTemplate();
		}

		final Template template =  templateFactory.find(inode);

		if(template!=null && InodeUtils.isSet(template.getInode()) &&
		      !permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)) {

			Logger.error(this, "User does not have access to template:" + inode);
			throw new DotSecurityException("User does not have access to template:" + inode);
		}

		return template;

	}

	@CloseDBIfOpened
	@Override
	public Template findLiveTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()->"Calling find, user:" + user.getUserId() + ", id = " + id);

		if(Template.SYSTEM_TEMPLATE.equals(id)) {

			return systemTemplate();
		}

		if (FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(id)) {//Check if the id is a path
			return this.findTemplateByPath(id,null, user, respectFrontendRoles, true);
		}

		final Identifier identifier = this.identifierAPI.find(id);//Finds the Identifier so we can get the path
		if (null != identifier &&
				FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(identifier.getPath())) {
			return this.findTemplateByPath(identifier.getPath(),identifier.getHostId(), user, respectFrontendRoles, true);
		}

		//For non-file based templates
		final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(id);
		return (!UtilMethods.isSet(info)) ? null : find(info.getLiveInode(), user, respectFrontendRoles);
	}

	@CloseDBIfOpened
	@Override
	public String checkDependencies(final String templateInode, final User user, final Boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		// todo: do something for the system template
		Logger.debug(this, ()-> "Calling check Dependencies, templateInode: " + templateInode +
								", user: " + user.getUserId());

		String result = null;
		final Template template = find(templateInode, user, respectFrontendRoles);
		// checking if there are pages using this template
		final List<Contentlet> pages = APILocator.getHTMLPageAssetAPI().findPagesByTemplate(template, user, respectFrontendRoles,
				TemplateConstants.TEMPLATE_DEPENDENCY_SEARCH_LIMIT);

		if(pages != null && !pages.isEmpty()) {

			final StringBuilder builder = new StringBuilder();
			int i = 0;
			for (final Contentlet page : pages) {

				final HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(page);
				final Host host = APILocator.getHostAPI().find(pageAsset.getHost(), user, false);
				builder.append(host.getHostname()).append(":").append(pageAsset.getURI());
				if(i++ != pages.size() - 1){
					builder.append(",");
				}
			}
			result = builder.toString();
		}
		return result;
	}

	@CloseDBIfOpened
	@Override
	public Map<String, String> checkPageDependencies(final Template template, final User user, final boolean respectFrontendRoles) {

		// todo: do something for the system template
		Logger.debug(this, ()-> "Calling check Dependencies, templateInode: " + template.getIdentifier() +
				", user: " + user.getUserId());

		final ImmutableMap.Builder<String, String> resultMapBuilder = new ImmutableMap.Builder<>();

		final List<Contentlet> pages = Try.of(()->this.htmlPageAssetAPI.get().findPagesByTemplate(template, user, respectFrontendRoles,
				TemplateConstants.TEMPLATE_DEPENDENCY_SEARCH_LIMIT)).getOrElseThrow(e -> new RuntimeException(e));

		if (pages!= null && !pages.isEmpty()) {

			for (final Contentlet page : pages) {

				final HTMLPageAsset pageAsset = this.htmlPageAssetAPI.get().fromContentlet(page);
				final Host host               = Try.of(()->this.hostAPI.find(pageAsset.getHost(), user, false))
													.getOrElseThrow(e -> new RuntimeException(e));

				resultMapBuilder.put(template.getName(), host.getHostname() + ":" +
						Try.of(()->pageAsset.getURI()).getOrElseThrow(e -> new RuntimeException(e)));
			}
		}

		return resultMapBuilder.build();
	}

    @Override
    public int deleteOldVersions(final Date assetsOlderThan) throws DotStateException, DotDataException {

		Logger.debug(this, ()-> "Calling deleteOldVersions, assetsOlderThan: " + assetsOlderThan);
        return deleteOldVersions(assetsOlderThan,"template");
    }

    @WrapInTransaction
	@Override
    public void updateThemeWithoutVersioning(final String templateInode, final String theme) throws DotDataException{

		Logger.debug(this, ()-> "Calling updateThemeWithoutVersioning, templateInode: " + templateInode +
									", theme = " + theme);
    	templateFactory.updateThemeWithoutVersioning(templateInode, theme);
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
	@WrapInTransaction
	@Override
	public void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException{

		Logger.debug(this, ()-> "Calling updateUserReferences, userId: " + userId +
				", replacementUserId = " + replacementUserId);
		templateFactory.updateUserReferences(userId, replacementUserId);
	}

	@CloseDBIfOpened
	@Override
	public List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling findAllVersions, identifier: " + identifier +
				", user = " + user.getUserId());

		return Template.SYSTEM_TEMPLATE.equals(identifier.getId())?
				includeSystemTemplate(Collections.emptyList()):
				findAllVersions(identifier,user,respectFrontendRoles,true);
	}

	@CloseDBIfOpened
	@Override
	public List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles, final boolean bringOldVersions)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Calling findAllVersions, identifier: " + identifier +
				", user: " + user.getUserId() + ", bringOldVersions: " + bringOldVersions);

		if (Template.SYSTEM_TEMPLATE.equals(identifier.getId())) {

			return includeSystemTemplate(Collections.emptyList());
		}

		final List<Template> templateAllVersions = templateFactory.findAllVersions(identifier,bringOldVersions);
		if(!templateAllVersions.isEmpty() && !permissionAPI.doesUserHavePermission(templateAllVersions.get(0), PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){

			Logger.error(this, "User does not have READ permissions over the Template, so unable to view Versions");
			throw new DotSecurityException("User does not have READ permissions over the Template, so unable to view Versions");
		}
		return templateAllVersions;
	}

	@Override
	@CloseDBIfOpened
	public List<Template> findTemplatesByContainerInode(final String containerInode)
			throws DotDataException {

		Logger.debug(this, ()-> "Calling findTemplatesByContainerInode, containerInode: " + containerInode);

		return Template.SYSTEM_TEMPLATE.equals(containerInode)?
				includeSystemTemplate(Collections.emptyList()):
				templateFactory.findTemplatesByContainerInode(containerInode);
	}

	@Override
	public boolean isLive(final Template template) throws DotDataException, DotStateException,DotSecurityException {

		Logger.debug(this, ()-> "Calling isLive, template: " + template.getIdentifier());

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {
			// system template is always live
			return true;
		}

		return template instanceof FileAssetTemplate ?
				FileAssetTemplate.class.cast(template).isLive() :
				this.versionableAPI.get().isLive(template);
	}

	@Override
	public void setLive(final Template template) throws DotDataException, DotStateException,DotSecurityException{

		Logger.debug(this, ()-> "Calling setLive, template: " + template.getIdentifier());

		if(Template.SYSTEM_TEMPLATE.equals(template.getIdentifier())) {

			Logger.debug(this, "System template can not be set to live");
			throw new IllegalArgumentException("System template can not be set to live");
		}

		this.versionableAPI.get().setLive(template instanceof FileAssetTemplate ?
				FileAssetTemplate.class.cast(template).toContentlet() :
				template);
	}

	/**
	 * Returns the Template based on the folder and host; this method is mostly used when the
	 * template is file asset based.
	 */
	@Override
	@CloseDBIfOpened
	public Template getTemplateByFolder(final Folder folder, final Host host, final User user,
			final boolean showLive) throws DotSecurityException, DotDataException {

		Logger.debug(this, ()-> "Calling getTemplateByFolder, folder: " + folder.getIdentifier()
							+ ", host: " + host.getHostname());

		return templateFactory.getTemplateByFolder(host,folder,user,showLive);
	}

	/**
	 * Implementation of {@link TemplateAPI#getPages(String)}
	 *
	 * @param templateId Template's ID that we are looking for
	 * @return
	 */
	@Override
	@WrapInTransaction
	public List<HTMLPageVersion> getPages(final String templateId) throws DotDataException, DotSecurityException {
		return FactoryLocator.getTemplateFactory().getPages(templateId);
	}

	/**
	 * Default implementation for {@link TemplateAPI#saveAndUpdateLayout(TemplateSaveParameters, User, boolean)}
	 *
	 * @param templateSaveParameters
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@Override
	public Template saveAndUpdateLayout(final TemplateSaveParameters templateSaveParameters, final User user,
										final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		Optional<TemplateLayout> oldTemplateLayout;

		if (!UtilMethods.isSet(templateSaveParameters.getOldTemplateLayout()) &&
				UtilMethods.isSet(templateSaveParameters.getNewTemplate().getIdentifier())) {

			oldTemplateLayout = getTemplateLayoutFromDatabase(templateSaveParameters.getNewTemplate().getIdentifier(), user);
		} else {
			oldTemplateLayout = Optional.ofNullable(templateSaveParameters.getOldTemplateLayout());
		}

		if (oldTemplateLayout.isPresent()) {
			updateMultiTrees(templateSaveParameters, oldTemplateLayout.get(), user, respectFrontendRoles);
		}

		final TemplateLayout newTemplateLayout = reOrder(templateSaveParameters.getNewLayout());

		if (!templateSaveParameters.isUseHistory()) {
			newTemplateLayout.setVersion(oldTemplateLayout.map(templateLayout -> templateLayout.getVersion() + 1)
					.orElse(1));
		}

		templateSaveParameters.getNewTemplate().setDrawedBody(newTemplateLayout);
		templateSaveParameters.getNewTemplate().setDrawed(true);

		return saveTemplate(templateSaveParameters.getNewTemplate(), templateSaveParameters.getSite(), user, respectFrontendRoles);
	}

	private void updateMultiTrees(final TemplateSaveParameters templateSaveParameters, TemplateLayout oldTemplateLayout,
								  User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		final LayoutChanges changes = templateSaveParameters.isUseHistory() ?
				getChangeFromHistory(oldTemplateLayout, templateSaveParameters.getNewLayout()) :
				getChange(oldTemplateLayout, templateSaveParameters.getNewLayout());

		final List<String> pageIds = UtilMethods.isSet(templateSaveParameters.getPageIds()) ?
				templateSaveParameters.getPageIds() :
				APILocator.getHTMLPageAssetAPI().findPagesByTemplate(templateSaveParameters.getNewTemplate(), user, respectFrontendRoles)
						.stream()
						.map(Contentlet::getIdentifier)
						.collect(Collectors.toList());


		final String currentVariant = WebAPILocator.getVariantWebAPI().currentVariantId();
		multiTreeAPI.updateMultiTrees(changes, pageIds, currentVariant);
	}

	private LayoutChanges getChangeFromHistory(final TemplateLayout oldTemplateLayout, final TemplateLayout newLayout) {

		final LayoutChanges.Builder builder = new LayoutChanges.Builder();
		final List<ContainerUUID> oldContainers = getContainers(oldTemplateLayout);
		final List<ContainerUUID> newContainers = getContainers(newLayout);
		final int deltaVersion = newLayout.getVersion() - oldTemplateLayout.getVersion();

		addMoveAndRemoveChangesWithHistory(oldContainers, newContainers, deltaVersion, builder);

		return builder.build();
	}

	private static void addMoveAndRemoveChangesWithHistory(final List<ContainerUUID> oldContainers,
														final List<ContainerUUID> newContainers,
														final int layoutVersionDelta,
														final LayoutChanges.Builder builder) {
		oldContainers.stream().forEach(oldContainer -> {
			final String uuid = oldContainer.getUUID();

			final Optional<ContainerUUID> newContainerUUIDMatch = newContainers.stream()
					.filter(newContainer -> oldContainer.getIdentifier().equals(newContainer.getIdentifier()))
					.filter(newContainer -> isMatch(oldContainer, newContainer, layoutVersionDelta))
					.findAny();

			if (newContainerUUIDMatch.isPresent() && !uuid.equals(newContainerUUIDMatch.get().getUUID())) {
				builder.change(oldContainer.getIdentifier(), oldContainer.getUUID(),
						newContainerUUIDMatch.get().getUUID());
			} else if (newContainerUUIDMatch.isEmpty()) {
				builder.remove(oldContainer.getIdentifier(), uuid);
			}
		});
	}

	private static boolean isMatch(final ContainerUUID oldContainer, final ContainerUUID newContainer,
								   final int layoutVersionDelta) {

		final List<String> oldHistoryUUIDs = oldContainer.getHistoryUUIDs();
		int oldHistorySize = oldHistoryUUIDs.size();
		int oldHistoryLastIndex = oldHistorySize - 1;
		final String oldLastUUID = oldHistoryUUIDs.get(oldHistoryLastIndex);
		final List<String> newHistoryUUIDs = newContainer.getHistoryUUIDs();

		return newHistoryUUIDs.size() == (oldHistorySize + layoutVersionDelta) &&
				newHistoryUUIDs.get(oldHistoryLastIndex).equals(oldLastUUID);
	}


	private static Optional<TemplateLayout> getTemplateLayoutFromDatabase(final String templateId, final User user)
			throws DotDataException, DotSecurityException {
		final Template templateFromDB = APILocator.getTemplateAPI().findWorkingTemplate(templateId, user, false);

		return Optional.ofNullable(templateFromDB.getDrawedBody()).map(DotTemplateTool::getTemplateLayout);
	}

	/**
	 * Recalculate the UUID for the layout, it means if the layout is something like
	 *
	 * <code>
	 * Row 1:
	 * 	Column 1:
	 * 		Container A UUID = 2
	 * 	Column 2:
	 * 		Container A UUID = -1
	 * Row 2:
	 * 	Column1:
	 * 	   Container A UUID= 1
	 * </code>
	 * The UUID are not in the right sequential order so this method is going to return the follow:
	 *
	 * <code>
	 * Row 1:
	 * 	Column 1:
	 * 		Container A UUID = 1
	 * 	Column 2:
	 * 		Container A UUID = 2
	 * Row 2:
	 * 	Column1:
	 * 	   Container A UUID= 3
	 * </code>
	 *
	 * @param layout
	 * @return
	 */
	private TemplateLayout reOrder(final TemplateLayout layout) {
		final List<ContainerUUID> containers = getContainers(layout);

		final Map<String,Integer> uuidByContainer = new HashMap<>();

		containers.stream().forEach(containerUUID -> {
			Integer maxUUID = uuidByContainer.get(containerUUID.getIdentifier());

			if (maxUUID == null) {
				maxUUID = 1;
			} else {
				maxUUID++;
			}

			uuidByContainer.put(containerUUID.getIdentifier(), maxUUID);
			containerUUID.setUuid(maxUUID.toString());
			containerUUID.addUUIDTOHistory(maxUUID.toString());
		});

		return layout;
	}

	/**
	 * Return all the changes between the OldLayout and the newLayout.
	 *
	 * To match the Container Instance between the two layouts it used the Container's ID and the Container UUID,
	 * so if a Container that had the same ID and UUID are in a different position on the two Layout then it means that
	 * the Container was moved.
	 *
	 * If a new Container instances is add on the newLayout then the UUID is -1, it means this instances does not exist
	 * on the oldLayout.
	 *
	 *
	 * for example if we have
	 *
	 * oldLayout:
	 * <code>
	 * Row 1:
	 * 	Column 1:
	 * 		Container A UUID = 1
	 * 	Column 2:
	 * 		Container A UUID = 2
	 * Row 2:
	 * 	Column1:
	 * 	   Container A UUID= 3
	 * </code>
	 *
	 * And newLayout is
	 *
	 * <code>
	 * Row 1:
	 *   Column 1:
	 *     Container A UUID = -1
	 * Row 2:
	 * 	Column1:
	 * 	   Container A UUID= 3
	 * Row 3:
	 * 	Column 1:
	 * 		Container A UUID = 1
	 * </code>
	 * A -1 UUID means that this is a new Container, with the 2 layouts of the example this method is going to return:
	 *
	 * <code>
	 * Row 1:
	 *   Column 1:
	 *     Container A OLD_UUID = -1 (IS NEW) NEW_UUID = 1
	 * Row 2:
	 * 	Column1:
	 * 	   Container A OLD_UUID = 3  NEW_UUID = 2 (WAS MOVED)
	 * Row 3:
	 * 	Column 1:
	 * 		Container OLD_UUID = 1  NEW_UUID = 3 (WAS MOVED)
	 *
	 * 	finally:
	 *
	 * 	Container OLD_UUID = 2  (WAS DELETED)
	 * </code>
	 * @param oldLayout
	 * @param newLayout
	 * @return
	 */
	private LayoutChanges getChange(final TemplateLayout oldLayout, final TemplateLayout newLayout) {
		final LayoutChanges.Builder builder = new LayoutChanges.Builder();

		final List<ContainerUUID> newContainers = getContainers(newLayout);

		addMoveAndNewChanges(newContainers, builder);
		addRemoveChanges(oldLayout, newContainers, builder);

		return builder.build();
	}

	/**
	 * Add Move/Include changes inside the LayoutChanges.Builder, following these rules:
	 *
	 * - The ContainersUUID must be in sequential order. If not:
	 * If the UUID of the Container is -1, a new included change is added to the LayoutChange.Builder.
	 * The oldUUID is the UUID that should be according to its order in newContainers.
	 *
	 * If the UUID is not in the right order, a new move change is added to the LayoutChange.Builder.
	 * The oldUUID is the UUID that has the ContainerUUID, and the newUUID is the UUID that should be according to its order in newContainers.
	 *
	 * @param newContainers
	 * @param builder
	 */
	private static void addMoveAndNewChanges(List<ContainerUUID> newContainers, LayoutChanges.Builder builder) {
		final Map<String,Integer> uuidByContainer = new HashMap<>();

		newContainers.stream().forEach(newContainer -> {
			Integer maxUUID = uuidByContainer.get(newContainer.getIdentifier());

			if (maxUUID == null) {
				maxUUID = 1;
			} else {
				maxUUID++;
			}

			uuidByContainer.put(newContainer.getIdentifier(), maxUUID);

			if (newContainer.getUUID().equals(ContainerUUID.UUID_DEFAULT_VALUE)) {
				builder.include(newContainer.getIdentifier(), String.valueOf(maxUUID));
			} else if (!newContainer.getUUID().equals(maxUUID.toString())) {
				builder.change(newContainer.getIdentifier(), newContainer.getUUID(), String.valueOf(maxUUID));
			}
		});
	}

	/**
	 * Add Removed changes inside the LayoutChanges.Builder, following these rules:
	 *
	 * - The ContainersUUID must be in sequential order. If not:
	 * If the sequential order has a blank spot then, a new removed change is added to the LayoutChange.Builder.
	 * The oldUUID is the UUID that should be according to its order in newContainers.
	 *
	 * @param newContainers
	 * @param builder
	 */
	private static void addRemoveChanges(TemplateLayout oldLayout, List<ContainerUUID> newContainers,
										 LayoutChanges.Builder builder) {
		final List<ContainerUUID> oldContainers = getContainers(oldLayout);

		oldContainers.stream().forEach(oldContainer -> {
			Optional<ContainerUUID> newContainer = newContainers.stream()
					.filter(containerUUID -> containerUUID.getIdentifier().equals(oldContainer.getIdentifier()))
					.filter(containerUUID -> containerUUID.getUUID().equals(oldContainer.getUUID()))
					.findFirst();

			if (!newContainer.isPresent()) {
				builder.remove(oldContainer.getIdentifier(), oldContainer.getUUID());
			}
		});
	}

	/**
	 * Get all the {@link ContainerUUID} from the {@link TemplateLayout}
	 * @param layout
	 * @return
	 */
	private static List<ContainerUUID> getContainers(TemplateLayout layout) {

		if (!UtilMethods.isSet(layout)) {
			return Collections.emptyList();
		}

		final List<ContainerUUID> bodyContainers = layout.getBody().getRows().stream()
				.flatMap(row -> row.getColumns().stream())
				.flatMap(column -> column.getContainers().stream())
				.collect(Collectors.toList());

		final Sidebar sidebar = layout.getSidebar();

		final List<ContainerUUID> sidebarContainers = sidebar != null ? sidebar.getContainers() : Collections.emptyList();

		return Stream.concat(bodyContainers.stream(), sidebarContainers.stream())
				.collect(Collectors.toList());
	}

	private Template findTemplateByPath (final String path,final String hostId, final User user, final boolean respectFrontendRoles, final boolean showLive) throws DotDataException, DotSecurityException {

		final FileAssetTemplateUtil fileAssetTemplateUtil =
				FileAssetTemplateUtil.getInstance();
		final Set<Host> hostSet = new LinkedHashSet<>();
		String relativePath 	= path;

		if (fileAssetTemplateUtil.isFullPath(path)) {

			final String hostName     = fileAssetTemplateUtil.getHostName(path);
			final Host host           = this.hostAPI.findByName(hostName, user, respectFrontendRoles);

			if (null != host) {
				relativePath = fileAssetTemplateUtil.getPathFromFullPath(hostName, path);
				hostSet.add(host);
			}
		}

		if(UtilMethods.isSet(hostId)){
			hostSet.add(APILocator.getHostAPI().find(hostId,user,respectFrontendRoles));
		}

		final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
		if (null != currentHost) {
			hostSet.add(currentHost);
		}

		hostSet.add(APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles));

		for (final Host host : hostSet) {
			try {

				final Folder folder     = APILocator.getFolderAPI().findFolderByPath(relativePath, host, user, respectFrontendRoles);
				final Template template = this.templateFactory.getTemplateByFolder(host, folder, user, showLive);

				if (template != null) {
					return  template;
				}
			} catch (NotFoundInDbException | DotSecurityException e) {
				Logger.debug(this,String.format("File Template %s not found", relativePath));
				return null;
			}
		}
		return null;
	}

	@Subscriber
	public void onCopySite(final SiteCreatedEvent event)
			throws DotDataException, DotSecurityException {
		final Folder appTemplateFolder = APILocator.getFolderAPI().findFolderByPath(
				Constants.TEMPLATE_FOLDER_PATH,
				APILocator.getHostAPI().find(event.getSiteIdentifier(),APILocator.systemUser(),false),
				APILocator.systemUser(), false);

		APILocator.getFolderAPI().subscribeFolderListener(appTemplateFolder, new ApplicationTemplateFolderListener(),
				childName -> null != childName && (childName.endsWith(Constants.VELOCITY_FILE_EXTENSION) || childName.endsWith(Constants.JSON_FILE_EXTENSION)));
	}

	@CloseDBIfOpened
	@Override
	public Optional<Contentlet> getImageContentlet(final Template template) throws DotDataException, DotSecurityException {

		return Optional.ofNullable(com.dotmarketing.portlets.templates.factories.TemplateFactory.getImageContentlet(template));
	}
}
