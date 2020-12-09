package com.dotmarketing.portlets.templates.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_EDIT;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerAPIImpl;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.*;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;



public class TemplateAPIImpl extends BaseWebAssetAPI implements TemplateAPI {

	private final  PermissionAPI    permissionAPI          = APILocator.getPermissionAPI();
	private final  IdentifierAPI    identifierAPI          = APILocator.getIdentifierAPI();
	private final  TemplateFactory  templateFactory        = FactoryLocator.getTemplateFactory();
	private final  ContainerAPI     containerAPI           = APILocator.getContainerAPI();
	private final  Lazy<VersionableAPI> versionableAPI     = Lazy.of(()->APILocator.getVersionableAPI());
	private final  Lazy<HTMLPageAssetAPI> htmlPageAssetAPI = Lazy.of(()->APILocator.getHTMLPageAssetAPI());
	private final  HostAPI          hostAPI                = APILocator.getHostAPI();


	@CloseDBIfOpened
	public List<Template> findTemplatesAssignedTo(final Host parentHost) throws DotDataException {
		return FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, false);
	}

	@CloseDBIfOpened
	public List<Template> findTemplatesAssignedTo(final Host parentHost, final boolean includeArchived) throws DotDataException {
		return FactoryLocator.getTemplateFactory().findTemplatesAssignedTo(parentHost, includeArchived);
	}

	@CloseDBIfOpened
	public List<Template> findTemplatesUserCanUse(final User user, final String hostId, final String query, final boolean searchHost, final int offset, final int limit) throws DotDataException, DotSecurityException {
		return FactoryLocator.getTemplateFactory().findTemplatesUserCanUse(user, hostId, query, searchHost, offset, limit);
	}

	@WrapInTransaction
	@Override
	public Template copy(final Template sourceTemplate, final User user) throws DotDataException, DotSecurityException {

		final Identifier identifier = APILocator.getIdentifierAPI().find(sourceTemplate.getIdentifier());
		final Host  host = APILocator.getHostAPI().find(identifier.getHostId(), user, false);

		return copy(sourceTemplate, host, false, false, user, false);
	}

	@WrapInTransaction
	@Override
	public Template copy(final Template sourceTemplate, final Host destination, final boolean forceOverwrite, final List<ContainerRemapTuple> containerMappings, final User user,
			final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
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

		return newTemplate;
	}

	@WrapInTransaction
	public Template copy(final Template sourceTemplate, final Host destination, final boolean forceOverwrite,
			final boolean copySourceContainers, User user, final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(sourceTemplate, PermissionAPI.PERMISSION_READ, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		List<ContainerRemapTuple> remap = new LinkedList<ContainerRemapTuple>();
		if (copySourceContainers) {
			List<Container> sourceContainers = getContainersInTemplate(sourceTemplate, user, respectFrontendRoles);
			Container newContainer;
			for (Container container : sourceContainers) {
				newContainer = containerAPI.copy(container, destination, user, respectFrontendRoles);
				remap.add(new ContainerRemapTuple(container, newContainer));
			}
		}

		return copy(sourceTemplate, destination, forceOverwrite, remap, user, respectFrontendRoles);
	}

	private void save(final Template template) throws DotDataException {
		templateFactory.save(template);
	}

	private void save(final Template template, final String existingId) throws DotDataException {
        templateFactory.save(template,existingId);
    }

	protected void save(final WebAsset webAsset) throws DotDataException {
		save((Template) webAsset);
	}

	@WrapInTransaction
	public void publishTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException, WebAssetException {

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
		final Template templateWorkingVersion = findWorkingTemplate(template.getIdentifier(),APILocator.systemUser(),false);
		//Sets Working as Live
		APILocator.getVersionableAPI().setLive(templateWorkingVersion);
		//Gets all Containers In the Template
		final List<Container> containersInTemplate = APILocator.getTemplateAPI().getContainersInTemplate(template, APILocator.getUserAPI().getSystemUser(), false);
		for(final Container container : containersInTemplate){
			Logger.debug(PublishFactory.class, "*****I'm a Template -- Publishing my Container Child= " + container.getInode());
			if(!container.isLive()){
				PublishFactory.publishAsset(container,user, false);
			}
		}
		templateWorkingVersion.setModDate(new java.util.Date());
		templateWorkingVersion.setModUser(user.getUserId());
		templateFactory.save(template);
		//Clean-up the cache for this template
		CacheLocator.getTemplateCache().remove(template.getInode());
		//writes the template to a live directory under velocity folder
		new TemplateLoader().invalidate(template);
	}

	@WrapInTransaction
	public void unpublishTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotSecurityException, DotDataException {

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
		final Template templateWorkingVersion = findWorkingTemplate(template.getIdentifier(),APILocator.systemUser(),false);
		//Remove live version from version_info
		APILocator.getVersionableAPI().removeLive(template.getIdentifier());
		templateWorkingVersion.setModDate(new java.util.Date());
		templateWorkingVersion.setModUser(user.getUserId());
		templateFactory.save(template);
		//remove template from the live directory
		new TemplateLoader().invalidate(template);
	}

	@WrapInTransaction
	public void unlock (final Template template, final User user)
			throws DotSecurityException, DotDataException {

		Logger.debug(this, ()->"Unlocking the Template: " + template.getIdentifier());

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}

		this.versionableAPI.get().setLocked(template, false, user);
	}

	@WrapInTransaction
	public void lock (final Template template, final User user)
			throws DotSecurityException, DotDataException {

		Logger.debug(this, ()->"Locking the Template: " + template.getIdentifier());

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}

		this.versionableAPI.get().setLocked(template, true, user);
	}

	@WrapInTransaction
	public void archive (final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing archive of the template: " + template.getIdentifier());

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


	@WrapInTransaction
	public void unarchive (final Template template, final User user)
			throws DotDataException, DotSecurityException {
		Logger.debug(this, ()-> "Doing unarchive of the template: " + template.getIdentifier());
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
	public boolean isArchived(final Template template) throws DotDataException {
		return APILocator.getVersionableAPI().isDeleted(template);
	}

	@Override
	public boolean isLive(final Template template) throws DotDataException, DotStateException,DotSecurityException {

		return template instanceof FileAssetTemplate ?
				FileAssetTemplate.class.cast(template).isLive():
				this.versionableAPI.get().isLive(template);
	}

	@Override
	public void setLive(final Template template) throws DotDataException, DotStateException,DotSecurityException {

		this.versionableAPI.get().setLive(template instanceof FileAssetTemplate?
				FileAssetTemplate.class.cast(template).toContentlet(): template);
	}

	@WrapInTransaction
	public void deleteTemplate(final Template template, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing delete of the template: " + template.getIdentifier());

		//Check Edit Permissions over Template
		if(!this.permissionAPI.doesUserHavePermission(template, PERMISSION_EDIT, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit the Template");
			throw new DotSecurityException("User does not have Permissions to Edit the Template");
		}

		//Check that the template is archived
		if(!isArchived(template)) {
			Logger.error(this,"The template: " + template.getIdentifier() + " must be archived before it can be deleted");
			throw new DotStateException("Template must be archived before it can be deleted");
		}

		//Check that template do not have dependencies (pages referencing the template),
		// use system user b/c user executing the delete could no have access to all pages
		final Map<String,String> checkDependencies = checkPageDependencies(template,APILocator.systemUser(),false);
		if(checkDependencies!= null && !checkDependencies.isEmpty()){
			Logger.error(this, "The Template: " + template.getName() + " can not be deleted. "
					+ "Because it has pages referencing to it: " + checkDependencies);
			throw new DotDataValidationException("Template still has pages referencing to it: " + checkDependencies);
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
	public void deleteByInode(final String inode) {

		Logger.debug(this, ()-> "Deleting template inode: " + inode);
		Try.run(()->FactoryLocator.getTemplateFactory().deleteTemplateByInode(inode)).onFailure(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	public Template saveTemplate(final Template template, final Host destination, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		boolean existingId=false, existingInode=false;

		if (template.isAnonymous() && LicenseManager.getInstance().isCommunity()) {

			Logger.warn(this, String.format("License required to save layout: template -> %s", template));
			throw new InvalidLicenseException();
		}

	    if(UtilMethods.isSet(template.getIdentifier())) {
		    final Identifier ident=APILocator.getIdentifierAPI().find(template.getIdentifier());
		    existingId = ident==null || !UtilMethods.isSet(ident.getId());
		}

	    if(template.isDrawed() && !UtilMethods.isSet(template.getDrawedBody())) {
	        throw new DotStateException("Drawed template MUST have a drawed body:" + template);

	    }

	    if(UtilMethods.isSet(template.getInode())) {
    	    try {
    	        final Template existing= templateFactory.find(template.getInode());
    	        existingInode = existing==null || !UtilMethods.isSet(existing.getInode());
    	    }
    	    catch(Exception ex) {
    	        existingInode=true;
    	    }
	    }

	    final Template oldTemplate = !existingId && UtilMethods.isSet(template.getIdentifier())
				?findWorkingTemplate(template.getIdentifier(), user, respectFrontendRoles)
						:null;


		if ((oldTemplate != null) && InodeUtils.isSet(oldTemplate.getInode())) {
			if (!permissionAPI.doesUserHavePermission(oldTemplate, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to read the source file.");
			}
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write in the destination folder.");
		}

		if (!permissionAPI.doesUserHavePermissions(PermissionableType.TEMPLATES, PermissionAPI.PERMISSION_EDIT, user)) {
			throw new DotSecurityException("You don't have permission to edit templates.");
		}


		//gets identifier from the current asset
		Identifier identifier = null;
		if (oldTemplate != null) {
			templateFactory.deleteFromCache(oldTemplate);

			identifier = identifierAPI.findFromInode(oldTemplate.getIdentifier());
		}
		else{
			//sets the owner so it can be set at the identifier table
			template.setOwner(user.getUserId());
			identifier = (!existingId) ? APILocator.getIdentifierAPI().createNew(template, destination) :
			                             APILocator.getIdentifierAPI().createNew(template, destination, template.getIdentifier());
			template.setIdentifier(identifier.getId());
		}
		template.setModDate(new Date());
		template.setModUser(user.getUserId());



		//it saves or updates the asset
		if(existingInode) {
		    // support for existing inode
		    save(template,template.getInode());
		}else {
		    save(template);
		}
		APILocator.getVersionableAPI().setWorking(template);

        return template;
	}

    @CloseDBIfOpened
    @Override
    public List<Container> getContainersInTemplate(final Template template, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {


        final List<Container> containers = new ArrayList<>();
        if(template.isDrawed()) {
            final TemplateLayout layout = DotTemplateTool.themeLayout(template.getInode());
            if (layout != null) {
				final List<String> containersId = this.getContainersId(layout);

				for (final String containerIdOrPath : containersId) {

					final Optional<Container> optionalContainer = APILocator.getContainerAPI().findContainer(containerIdOrPath, user, false, false);

					if (!optionalContainer.isPresent()) {
						continue;
					}

					containers.add(optionalContainer.get());
				}
			}
        }
        // this is a light weight search for pages that use this template
        final List<ContentletSearch> pages =
				APILocator.getContentletAPIImpl().searchIndex("+catchall:" + template.getIdentifier() + " +baseType:"
                + BaseContentType.HTMLPAGE.getType(), 100, 0, null, user,respectFrontendRoles);

        for (final ContentletSearch page : pages) {
            final Set<String> containerIdSet =
                    APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier())
                    .stream()
                    .map(MultiTree::getContainer)
                    .collect(Collectors.toSet());


            for (final String containerId : containerIdSet) {
                final Container container = APILocator.getContainerAPI().getWorkingContainerById(containerId, user, false);
                if(container==null) {
                    continue;
                }
                containers.add(container);
            }
        }
        return new ArrayList<Container>(containers);

    }

	public List<ContainerUUID> getContainersUUID(final TemplateLayout layout) {
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

	public Host getTemplateHost(final Template template) throws DotDataException {

		try {
			final Host host = APILocator.getHostAPI().findParentHost(template, APILocator.getUserAPI().getSystemUser(), false);
			return host;
		} catch (DotSecurityException e1) {
			Logger.error(TemplateAPIImpl.class, e1.getMessage(), e1);
			throw new DotRuntimeException(e1.getMessage(), e1);
		}

	}

	@WrapInTransaction
	public boolean delete(final Template template, final User user, final boolean respectFrontendRoles) throws DotSecurityException,
			Exception {
		if(permissionAPI.doesUserHavePermission(template, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(template);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}


	public Template findWorkingTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if (FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(id)) {

			return this.findTemplateByPath(id, user, respectFrontendRoles, false);
		}

		final Identifier identifier = this.identifierAPI.find(id);
		if (null != identifier && UtilMethods.isSet(identifier.getPath()) &&
				FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(identifier.getPath())) {

			return this.findTemplateByPath(identifier.getPath(), user, respectFrontendRoles, false);
		}

		final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(id);
		return (!UtilMethods.isSet(info)) ? null : find(info.getWorkingInode(), user, respectFrontendRoles);

	}

	private Template findTemplateByPath (final String path, final User user, final boolean respectFrontendRoles, final boolean showLive) throws DotDataException, DotSecurityException {

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

		try {

			final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
			if (request != null) {
				final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(request, user);
				if (null != currentHost) {
					hostSet.add(currentHost);
				}
			}
		} catch (DotSecurityException e) {

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
				continue;
			}
		}

		throw new NotFoundInDbException(String.format("File Template %s not found", relativePath));
	}



	@CloseDBIfOpened
	public List<Template> findTemplates(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String hostId, final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderBy) throws DotSecurityException,
			DotDataException {
		return templateFactory.findTemplates(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

	@CloseDBIfOpened
	@Override
	public Template find(final String inode, final User user, final boolean respectFrontEndRoles) throws DotSecurityException,
			DotDataException {
		Template t =  templateFactory.find(inode);
		if(t!=null && InodeUtils.isSet(t.getInode()) &&
		      !permissionAPI.doesUserHavePermission(t, PermissionAPI.PERMISSION_READ, user, respectFrontEndRoles)){
			throw new DotSecurityException("User does not have access to template:" + inode);
		}
		return t;

	}

	public Template findLiveTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		if (FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(id)) {

			return this.findTemplateByPath(id, user, respectFrontendRoles, true);
		}

		final Identifier identifier = this.identifierAPI.find(id);
		if (null != identifier && UtilMethods.isSet(identifier.getPath()) &&
				FileAssetTemplateUtil.getInstance().isFolderAssetTemplateId(identifier.getPath())) {

			return this.findTemplateByPath(identifier.getPath(), user, respectFrontendRoles, true);
		}

		VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(id);
		return (!UtilMethods.isSet(info)) ? null : find(info.getLiveInode(), user, respectFrontendRoles);
	}

	@CloseDBIfOpened
	@Override
	public String checkDependencies(final String templateInode, final User user, final Boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		String result = null;
		Template template = find(templateInode, user, respectFrontendRoles);
		// checking if there are pages using this template
		List<Contentlet> pages=APILocator.getHTMLPageAssetAPI().findPagesByTemplate(template, user, respectFrontendRoles,
				TemplateConstants.TEMPLATE_DEPENDENCY_SEARCH_LIMIT);

		if(pages != null && !pages.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			int i = 0;
			for (Contentlet page : pages) {
				HTMLPageAsset pageAsset = APILocator.getHTMLPageAssetAPI().fromContentlet(page);
				Host host = APILocator.getHostAPI().find(pageAsset.getHost(), user, false);
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
        return deleteOldVersions(assetsOlderThan,"template");
    }

    @WrapInTransaction
    public void updateThemeWithoutVersioning(final String templateInode, final String theme) throws DotDataException{
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
	public void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException{
		templateFactory.updateUserReferences(userId, replacementUserId);
	}

	@CloseDBIfOpened
	@Override
	public List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		return findAllVersions(identifier,user,respectFrontendRoles,true);
	}

	@CloseDBIfOpened
	@Override
	public List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles, final boolean bringOldVersions)
			throws DotDataException, DotSecurityException {
		final List<Template> templateAllVersions = templateFactory.findAllVersions(identifier,bringOldVersions);
		if(!templateAllVersions.isEmpty() && !permissionAPI.doesUserHavePermission(templateAllVersions.get(0), PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)){
			throw new DotSecurityException("User cannot read Contentlet So Unable to View Versions");
		}
		return templateAllVersions;
	}

	@Override
	@CloseDBIfOpened
	public List<Template> findTemplatesByContainerInode(final String containerInode)
			throws DotDataException {
		return templateFactory.findTemplatesByContainerInode(containerInode);
	}
}
