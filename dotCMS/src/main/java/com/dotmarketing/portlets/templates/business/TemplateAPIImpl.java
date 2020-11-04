package com.dotmarketing.portlets.templates.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.*;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.*;
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
	public boolean publishTemplate(final Template template, final User user, final boolean respectFrontendRoles) {

		Logger.debug(this, ()-> "Publishing the template: " + template.getIdentifier());
		return Try.of(()->PublishFactory.publishAsset(template, user, respectFrontendRoles)).getOrElseThrow(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	public boolean unpublishTemplate(final Template template, final User user, final boolean respectFrontendRoles) {

		Logger.debug(this, ()-> "Unpublishing the template: " + template.getIdentifier());
		final Folder parent = Try.of(()->APILocator.getFolderAPI()
				.findParentFolder(template, user, respectFrontendRoles)).getOrElseThrow(e -> new RuntimeException(e));
		return Try.of(()->WebAssetFactory.unPublishAsset(template, user.getUserId(), parent))
				.getOrElseThrow(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	public void unlock (final Template template, final User user) {

		Try.run(()->this.versionableAPI.get().setLocked(template, false, user))
				.getOrElseThrow(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	public boolean archive (final Template template, final User user, final boolean respectFrontendRoles) {

		Logger.debug(this, ()-> "Doing archive of the template: " + template.getIdentifier());
		if (Try.of(()->template.isLive()).getOrElseThrow(e -> new RuntimeException(e))) {

			if (!this.unpublishTemplate(template, user, respectFrontendRoles)) {

				Logger.debug(this, "the template: " + template.getIdentifier() +
						" could not be archived, b/c it was live and couldn't unpublish");
				return false;
			}
		}

		return Try.of(()-> WebAssetFactory.archiveAsset(template, user.getUserId())).getOrElseThrow(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	public void unarchive (final Template template) {

		Logger.debug(this, ()-> "Doing unarchive of the template: " + template.getIdentifier());
		if (Try.of(()->template.isArchived()).getOrElseThrow(e -> new RuntimeException(e))) {

			Try.run(()->WebAssetFactory.unArchiveAsset(template))
					.getOrElseThrow(e -> new RuntimeException(e));
		}
	}

	@WrapInTransaction
	public boolean deleteTemplate(final Template template, final User user, final boolean respectFrontendRoles) {

		Logger.debug(this, ()-> "Doing delete of the template: " + template.getIdentifier());

		if (Try.of(()->template.isArchived()).getOrElseThrow(e -> new RuntimeException(e))) {

			if(Try.of(()->this.permissionAPI.doesUserHavePermission(
					template, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles))
					.getOrElseThrow(e -> new RuntimeException(e))) {

				return Try.of(() -> WebAssetFactory.deleteAsset(template, user))
						.getOrElseThrow(e -> new RuntimeException(e));
			} else {

				throw new SecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}
		} else {

			throw new DotStateException("The template: " + template.getName() + " must be archived before it can be deleted");
		}
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
		final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(id);
		return (!UtilMethods.isSet(info)) ? null : find(info.getWorkingInode(), user, respectFrontendRoles);

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
