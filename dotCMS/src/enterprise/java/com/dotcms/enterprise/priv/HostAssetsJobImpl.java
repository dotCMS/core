/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.priv;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.CopyContentTypeBean;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.enterprise.ParentProxy;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.SiteCreatedEvent;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This Quartz Job allows dotCMS users to copy sites. The Sites portlet also allows users to select
 * what specific pieces of the site (i.e., templates and containers, folders, files, pages, content
 * on pages, etc.) will be copied over to the new site.
 * <p>
 * Keep in mind that depending on the number of elements in a site, this process can take an
 * important amount of time. Therefore, any improvement on the performance of SQL statements is very
 * important.
 * </p>
 * 
 * @author root
 * @version 3.5
 * @since Jun 11, 2012
 */
public class HostAssetsJobImpl extends ParentProxy{

	private final HostAPI siteAPI;
	private final UserAPI userAPI;
	private final FolderAPI folderAPI;
	private final FileAssetAPI fileAssetAPI;
	private final TemplateAPI templateAPI;
	private final ContainerAPI containerAPI;
	private final ContentletAPI contentAPI;
	private final MenuLinkAPI menuLinkAPI;
	private final ContentTypeAPI contentTypeAPI;
	private final RelationshipAPI relationshipAPI;
	private final FieldAPI contentTypeFieldAPI;
	private final HostAssetsJobProxy siteCopyStatus;
	private final User SYSTEM_USER;
	private final Host SYSTEM_HOST;
	private final Folder SYSTEM_FOLDER;

	/**
	 * This feature flag allows you to enable/disable copying Content Types when you copy a Site.
	 */
	public static final String ENABLE_CONTENT_TYPE_COPY = "FEATURE_FLAG_ENABLE_CONTENT_TYPE_COPY";
	/**
	 * This feature flag allows you to fall back to NOT copy related content whose parent is a
	 * Contentlet living in System Host or a Site that is neither the source Site nor the
	 * destination Site. Such types of relationship have always been ignored, so this flag allows
	 * you to bring that behavior back.
	 */
	public static final String KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES =
			"KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES";

	private static final Lazy<Boolean> CONTENT_TYPE_COPY_FLAG =
			Lazy.of(() -> Config.getBooleanProperty(ENABLE_CONTENT_TYPE_COPY, false));
	/**
	 * The current value of the {@link #KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES} property.
	 */
	private static final Lazy<Boolean> KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES_FLAG = Lazy.of(() ->
			Config.getBooleanProperty(KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES, true));

	private static final boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;
	private static final boolean RESPECT_FRONTEND_ROLES = Boolean.TRUE;

	/**
	 * Configuration property to control how many contentlets are processed before committing
	 * the transaction and clearing Hibernate session. Lower values reduce memory usage but
	 * increase transaction overhead. Default: 500
	 */
	private static final Lazy<Integer> CONTENT_BATCH_SIZE = Lazy.of(() ->
			Config.getIntProperty("SITE_COPY_CONTENT_BATCH_SIZE", 500));

	/**
	 * Configuration property to control how many contentlets with relationships are accumulated
	 * before processing them. This prevents unbounded list growth. Default: 500
	 */
	private static final Lazy<Integer> RELATIONSHIP_BATCH_SIZE = Lazy.of(() ->
			Config.getIntProperty("SITE_COPY_RELATIONSHIP_BATCH_SIZE", 500));

	/**
	 * Configuration property to control how long to pause (in milliseconds) between batches
	 * of content processing. This gives the system time to reclaim memory and reduces CPU
	 * pressure. Even if set to 0, a minimum pause will still be enforced. Default: 100ms
	 */
	private static final Lazy<Integer> BATCH_PAUSE_MS = Lazy.of(() ->
			Config.getIntProperty("SITE_COPY_BATCH_PAUSE_MS", 100));

	/**
	 * Minimum pause time (in milliseconds) between batches, enforced even if configured value
	 * is 0. This ensures the system always has time to breathe, allowing garbage collection,
	 * thread scheduling, and preventing CPU thrashing. Default: 10ms
	 */
	private static final int MIN_BATCH_PAUSE_MS = 10;

	/**
	 * Configuration property to control the maximum initial capacity for the HTML pages collection
	 * during site copy. This prevents excessive memory allocation for sites with very large numbers
	 * of contentlets but relatively few HTML pages. Default: 100000
	 */
	private static final Lazy<Integer> HTML_PAGES_MAX_INITIAL_CAPACITY = Lazy.of(() ->
			Config.getIntProperty("SITE_COPY_HTML_PAGES_MAX_CAPACITY", 100000));

	/**
	 * Creates an instance of the Site Copy Job.
	 *
	 * @param siteCopyStatus Status object used to keep track of the Site Copy process.
	 */
	public HostAssetsJobImpl(final HostAssetsJobProxy siteCopyStatus) {
		this.siteCopyStatus = siteCopyStatus;
		this.siteAPI = APILocator.getHostAPI();
		this.userAPI = APILocator.getUserAPI();
		this.folderAPI = APILocator.getFolderAPI();
		this.fileAssetAPI = APILocator.getFileAssetAPI();
		this.templateAPI = APILocator.getTemplateAPI();
		this.contentAPI = APILocator.getContentletAPI();
		this.menuLinkAPI = APILocator.getMenuLinkAPI();
		this.containerAPI = APILocator.getContainerAPI();
		this.SYSTEM_USER = Try.of(userAPI::getSystemUser).getOrNull();
		this.contentTypeAPI = APILocator.getContentTypeAPI(this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
		this.relationshipAPI = APILocator.getRelationshipAPI();
		this.contentTypeFieldAPI = APILocator.getContentTypeFieldAPI();
		this.SYSTEM_HOST = Try.of(() -> siteAPI.findSystemHost(this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES)).getOrNull();
		this.SYSTEM_FOLDER = Try.of(folderAPI::findSystemFolder).getOrNull();
	}

	/**
	 * This is the initial execution point of the Site Copy Job. The Quartz Framework calls this
	 * method once the Job is triggered.
	 *
	 * @param jobContext Provides useful information about the Job and allows you to pass down
	 *                   specific properties to the code.
	 *
	 * @throws JobExecutionException An error occurred when copying a Site.
	 */
	public void run(final JobExecutionContext jobContext) throws JobExecutionException {

		if(!allowExecution()) {

			return;
		}

		final Map<String, Serializable> map = this.siteCopyStatus.getExecutionData(jobContext.getTrigger());
		final String userId = (String) map.get(HostAssetsJobProxy.USER_ID);
		final String sourceSiteId = (String) map.get(HostAssetsJobProxy.SOURCE_HOST_ID);
		final String destinationSiteId = (String) map.get(HostAssetsJobProxy.DESTINATION_HOST_ID);
		final HostCopyOptions copyOptions = (HostCopyOptions) map.get(HostAssetsJobProxy.COPY_OPTIONS);

		Host sourceSite = new Host();
		Host destinationSite = new Host();
		final String jobName = "setup-host-" + sourceSiteId;
		final String jobGroup = "setup-host-group";

		try {
			sourceSite = this.siteAPI.DBSearch(sourceSiteId, this.userAPI.getSystemUser(), DONT_RESPECT_FRONTEND_ROLES);
			destinationSite = this.siteAPI.DBSearch(destinationSiteId, this.userAPI.getSystemUser(), DONT_RESPECT_FRONTEND_ROLES);
			if(QuartzUtils.getTaskProgress(jobName,jobGroup) >= 0 && QuartzUtils.getTaskProgress(jobName, jobGroup) < 100) {
				final String infoMsg = String.format("Site '%s' is already being copied. This new request will be ignored.",
						sourceSite.getHostname());
				Logger.warn(this, infoMsg);
				sendNotification(infoMsg, userId, NotificationLevel.WARNING);
				return;
			}

			Logger.info(this, "======================================================================");
			Logger.info(this, String.format("  Starting Site Copy Job: '%s'", jobName));
			Logger.info(this, "======================================================================");
			this.sendNotification(String.format("Starting the copy of Site '%s' based off of Site '%s'.",
					destinationSite.getHostname(), sourceSite.getHostname()), userId, NotificationLevel.INFO);
			validateSiteInfo(sourceSite,destinationSite,sourceSiteId,destinationSiteId,jobName);
			copySiteAssets(sourceSite, destinationSite, copyOptions);
		} catch (final Throwable e) {
			final String errorMsg = String
					.format("An error occurred when copying source Site '%s' to destination Site '%s': %s",
							sourceSite.getHostname(), destinationSite.getHostname(), ExceptionUtil.getErrorMessage(e));
			Logger.error(HostAssetsJobImpl.class, errorMsg, e);
			this.sendNotification(String.format("The copy of Site '%s' has failed: %s. Please check the logs for more information",
					destinationSite.getHostname(), ExceptionUtil.getErrorMessage(e)), userId, NotificationLevel.ERROR);
			throw new JobExecutionException(errorMsg, e);
		}
		Logger.info(this, "======================================================================");
		Logger.info(this, String.format("  Site Copy Job '%s' for Site '%s' has finished correctly!", jobName, destinationSite.getHostname()));
		String successMsg = "The copy of Site '%s' has finished correctly. ";
		if (copyOptions.isCopyContentOnHost() || copyOptions.isCopyContentOnPages()) {
			Logger.info(this, "");
			final String contentIndexationMsg = "Now, Contentlets will be added to the ES Index. Please take this into consideration and wait for that process to finish, if required.";
			Logger.info(this, "  " + contentIndexationMsg);
			successMsg += contentIndexationMsg;
		}
		Logger.info(this, "======================================================================");
		this.sendNotification(String.format(successMsg, destinationSite.getHostname()), userId, NotificationLevel.INFO);
	}

	/**
	 * Performs the copy process of the user-specified elements from the source site to the
	 * destination site.
	 * 
	 * @param sourceSite      The {@link Host} object containing the information that will be
	 *                        copied.
	 * @param destinationSite The new {@link Host} object that will contain the information from
	 *                        the source site.
	 * @param copyOptions     The preferences selected by the user regarding what elements of the
	 *                        source site will be copied over to the new site.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The current user does not have the permissions to perform this
	 *                              action.
	 */
	private void copySiteAssets(final Host sourceSite, final Host destinationSite, final HostCopyOptions copyOptions) throws DotDataException, DotContentletStateException, DotSecurityException {
		try {
			HibernateUtil.startTransaction();
			// Global Vars
			double progressIncrement;
			double currentProgress;

			this.siteCopyStatus.addMessage("copying-templates");
			// ======================================================================
			// Copying templates and containers
			// ======================================================================
			final Map<String, HTMLPageAssetAPI.TemplateContainersReMap> copiedTemplatesBySourceId = new HashMap<>();
			final Map<String, Container> copiedContainersBySourceId = new HashMap<>();
			final Map<String, FolderMapping> copiedFoldersBySourceId = new HashMap<>();
			final Map<String, ContentMapping> copiedContentsBySourceId = new HashMap<>();
			final List<Contentlet> contentsWithRelationships =  new ArrayList<>();
			final Map<String, ContentTypeMapping> copiedContentTypesBySourceId = new HashMap<>();
			final Map<String, RelationshipMapping> copiedRelationshipsBySourceId = new HashMap<>();
			if (copyOptions.isCopyTemplatesAndContainers()) {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying Templates and Containers to new Site '%s'", destinationSite.getHostname()));
				final List<Template> sourceTemplates = this.templateAPI.findTemplatesAssignedTo(sourceSite);
				Logger.info(this, String.format("-> Copying %d Templates", sourceTemplates.size()));
				for (final Template sourceTemplate : sourceTemplates) {
					try {
						final List<Container> sourceContainers = this.templateAPI.getContainersInTemplate(sourceTemplate, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
						final List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<>();
						Logger.debug(this, () -> String.format("---> Copying %d Containers in Template '%s [ %s ]", sourceContainers.size(),
								sourceTemplate.getName(), sourceTemplate.getIdentifier()));
						for (final Container sourceContainer : sourceContainers) {
							Container destinationContainer = copiedContainersBySourceId.get(sourceContainer.getIdentifier());
							if(destinationContainer == null) {
								final Host containerSite = this.containerAPI.getParentHost(sourceContainer, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
								if(containerSite.getIdentifier().equals(sourceSite.getIdentifier())) {
									// Same Site
									if ((sourceContainer instanceof FileAssetContainer)
											&& copyOptions.isCopyFolders()) {
										// Containers as files are file assets that depend upon an existing folder
										/// so we need to copy the containers folder and then copy the contentlet in it.
										final FileAssetContainer sourceFileAssetContainer = (FileAssetContainer) sourceContainer;
										Logger.debug(HostAssetsJobImpl.class, () -> String.format("---> Copying file-asset container '%s' [ %s ]",
												sourceFileAssetContainer.getPath(), sourceFileAssetContainer.getIdentifier()));
										final Contentlet sourceContent = this.contentAPI.findContentletByIdentifier(
														sourceFileAssetContainer.getIdentifier(),
														sourceFileAssetContainer.isLive(),
														sourceFileAssetContainer.getLanguageId(),
														this.SYSTEM_USER,
														DONT_RESPECT_FRONTEND_ROLES);
										final Folder sourceFolder = this.folderAPI.find(sourceContent.getFolder(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
										// This should store the new container destination folder into the map.
										final Folder destinationFolder = copyFolder(sourceFolder, destinationSite, copiedFoldersBySourceId);
										Logger.debug(HostAssetsJobImpl.class, () -> String.format("---> Container-As-File destination folder path is '%s'",
												destinationFolder.getPath()));
										// now create the Copy of the container as file and all its assets
										final List<Contentlet> processedContentletsList = new ArrayList<>();
										final List<FileAsset> sourceContainerAssets = this.fileAssetAPI
												.findFileAssetsByFolder(sourceFolder, null, false,
														this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
										for (final Contentlet asset : sourceContainerAssets) {
											processedContentletsList.add(
													processCopyOfContentlet(asset, copyOptions,
															sourceSite, destinationSite,
															copiedContentsBySourceId,
															copiedFoldersBySourceId,
															copiedContainersBySourceId,
															copiedTemplatesBySourceId,
															contentsWithRelationships,
															copiedContentTypesBySourceId
													)
											);
										}
										final Optional<Contentlet> fileAssetOptional = processedContentletsList
												.stream().filter(
														Contentlet::isFileAssetContainer)
												.findFirst();
										if (fileAssetOptional.isPresent()) {
											final Contentlet contentlet = fileAssetOptional.get();
											final FileAssetContainer newFileAssetContainer = new FileAssetContainer();
											newFileAssetContainer.setLanguage(contentlet.getLanguageId());
											newFileAssetContainer.setInode(contentlet.getInode());
											newFileAssetContainer.setIdentifier(contentlet.getIdentifier());
											final String identifierOrPath =
													FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) sourceContainer);

											copiedContainersBySourceId.put(identifierOrPath, newFileAssetContainer);
											Logger.debug(HostAssetsJobImpl.class, () -> String
													.format("---> Source Container-As-File ID '%s' mapped to new ID '%s'",
															sourceContainer.getIdentifier(),
															newFileAssetContainer.getIdentifier()));
										}
									} else {
									 destinationContainer = this.containerAPI.copy(sourceContainer, destinationSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
									 copiedContainersBySourceId.put(sourceContainer.getIdentifier(), destinationContainer);
								  }
								} else {
									// Probably belongs to system host
									destinationContainer = sourceContainer;
									copiedContainersBySourceId.put(sourceContainer.getIdentifier(), sourceContainer);
								}
							}

							HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple containerMapping = new HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple(
									sourceContainer, destinationContainer);
							containerMappings.add(containerMapping);
						}
						//Copy template with updated references
						final Template newTemplate = copyTemplate(sourceTemplate, destinationSite, copiedContainersBySourceId);
						HTMLPageAssetAPI.TemplateContainersReMap templateMapping = new HTMLPageAssetAPI.TemplateContainersReMap(
								sourceTemplate, newTemplate, containerMappings);
						copiedTemplatesBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
					} catch (final Exception e) {
						Logger.error(this, String.format("An error occurred when copying data from Template '%s' [%s] " +
								"from Site '%s' to Site '%s'. The process will continue...", sourceTemplate.getTitle()
								, sourceTemplate.getIdentifier(), sourceSite.getHostname(), destinationSite
										.getHostname()), e);
					}
				}
				// make sure we include any other container previously skipped or ignored in case templates were not copied.
				final List<Container> containersUnderSourceSite = this.containerAPI.findContainersUnder(sourceSite);
				Logger.debug(HostAssetsJobImpl.class, () -> String.format("---> Copying %d Containers under Site '%s'",
						containersUnderSourceSite.size(), sourceSite.getHostname()));
				for (final Container cntr : containersUnderSourceSite) {
					final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
					final String containerIdOrPath = fileAssetContainerUtil.isFileAssetContainer(cntr) ?
							fileAssetContainerUtil.getFullPath(sourceSite.getHostname(), ((FileAssetContainer) cntr).getPath()) :
							cntr.getIdentifier();

					if(copiedContainersBySourceId.get(containerIdOrPath) == null){
						Container destinationContainer = this.containerAPI.copy(cntr, destinationSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);copiedContainersBySourceId.put(cntr.getIdentifier(), destinationContainer);
					} else {
						copiedContainersBySourceId.put(cntr.getIdentifier(), copiedContainersBySourceId.get(containerIdOrPath));
					}
				}
			} else {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying Templates to new Site '%s'", destinationSite.getHostname()));
				final List<Template> sourceTemplates = this.templateAPI.findTemplatesAssignedTo(sourceSite);
				Logger.info(this, String.format("-> Copying %d Templates", sourceTemplates.size()));
				for (final Template sourceTemplate : sourceTemplates) {
					try {
						final List<Container> sourceContainers = this.templateAPI.getContainersInTemplate(sourceTemplate, this.SYSTEM_USER,
								DONT_RESPECT_FRONTEND_ROLES);
						Logger.debug(this, () -> String.format("---> Copying %d Containers in Template '%s [ %s ]", sourceContainers.size(),
								sourceTemplate.getName(), sourceTemplate.getIdentifier()));
						final List<Container> destinationContainers = sourceContainers;
						final List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<>();
						for (int i = 0; i < sourceContainers.size(); i++) {
							final Container sourceContainer = sourceContainers.get(i);
							final Container destinationContainer = destinationContainers.get(i);
							final HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple containerMapping = new HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple(
									sourceContainer, destinationContainer);
							containerMappings.add(containerMapping);
						}
						final HTMLPageAssetAPI.TemplateContainersReMap templateMapping = new HTMLPageAssetAPI.TemplateContainersReMap(
								sourceTemplate, sourceTemplate, containerMappings);
						copiedTemplatesBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
					} catch (final Exception e) {
						Logger.error(this, String.format("An error occurred when copying data from Template '%s' [%s] " +
										"from Site '%s' to Site '%s'. The process will continue...", sourceTemplate.getTitle()
								, sourceTemplate.getIdentifier(), sourceSite.getHostname(), destinationSite
										.getHostname()), e);
					}
				}
			}
			HibernateUtil.closeAndCommitTransaction();
			HibernateUtil.startTransaction();
			this.siteCopyStatus.updateProgress(5);

			// ======================================================================
			// Copying folders
			// ======================================================================
			if (copyOptions.isCopyFolders()) {
				this.siteCopyStatus.addMessage("copying-folders");
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying Folders to new Site '%s'", destinationSite.getHostname()));
				final List<Folder> allSourceFolders = this.folderAPI.findSubFoldersRecursively(sourceSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
				Logger.info(this, String.format("-> Copying %d Folders", allSourceFolders.size()));
				for (final Folder sourceFolder : allSourceFolders) {
					try {
					    copyFolder(sourceFolder, destinationSite, copiedFoldersBySourceId);
					} catch (final Exception e) {
						Logger.error(this, String.format("An error occurred when copying folder '%s' from Site '%s' to" +
								" Site '%s'. The process will continue...", sourceFolder.getPath(), sourceSite
								.getHostname(), destinationSite.getHostname()), e);
					}
				}
				HibernateUtil.closeAndCommitTransaction();
				HibernateUtil.startTransaction();
				this.siteCopyStatus.updateProgress(10);

				if (copyOptions.isCopyLinks()) {
					final Collection<FolderMapping> folders = copiedFoldersBySourceId.values();
					// ======================================================================
					// Copying Menu Links
					// ======================================================================
					this.siteCopyStatus.addMessage("copying-menu-links");
					Logger.info(this, "----------------------------------------------------------------------");
					Logger.info(this, String.format(":::: Copying Menu Links to new Site '%s'", destinationSite.getHostname()));
					for (final FolderMapping folderMapping : folders) {
						final List<Link> sourceLinks = this.menuLinkAPI.findFolderMenuLinks(folderMapping.sourceFolder);
						Logger.debug(this, () -> String.format("-> Copying %d Menu Links from Folder '%s'", sourceLinks.size(), folderMapping.sourceFolder.getPath()));
						for (final Link sourceLink : sourceLinks) {
							try {
								Logger.debug(this, () -> "---> Copying menu with inode : " + sourceLink.getInode());
								this.menuLinkAPI.copy(sourceLink, folderMapping.destinationFolder, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
							} catch (final Exception e) {
								Logger.error(this, String.format("An error occurred when copying menu link '%s' from " +
										"'%s' in Site '%s' to Site '%s'. The process will continue...", sourceLink
										.getTitle(), folderMapping.sourceFolder.getPath(), sourceSite.getHostname(),
										destinationSite.getHostname()), e);
							}
						}
					}
				}

				// Point templates to copied themes (if themes belong to the copied site)
				if(copyOptions.isCopyTemplatesAndContainers()){
					Logger.info(this, "----------------------------------------------------------------------");
					Logger.info(this, String.format(":::: Pointing %d Templates to copied themes for new Site '%s'",
							copiedTemplatesBySourceId.size(), destinationSite.getHostname()));
					for (final String sourceTemplateId : copiedTemplatesBySourceId.keySet()) {
						final Template srcTemplate = copiedTemplatesBySourceId.get(sourceTemplateId).getSourceTemplate();
						if(UtilMethods.isSet(srcTemplate.getTheme()) && copiedFoldersBySourceId.containsKey(srcTemplate.getTheme())){
							final String destTemplateInode = copiedTemplatesBySourceId.get(sourceTemplateId).getDestinationTemplate().getInode();
							final String destTheme = copiedFoldersBySourceId.get(srcTemplate.getTheme()).destinationFolder.getInode();
							this.templateAPI.updateThemeWithoutVersioning(destTemplateInode, destTheme);
						}
					}
				}
				
			}
			HibernateUtil.closeAndCommitTransaction();
			HibernateUtil.startTransaction();
			this.siteCopyStatus.updateProgress(70);

			if (CONTENT_TYPE_COPY_FLAG.get() && copyOptions.isCopyContentTypes()) {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying Content Types to new Site '%s'", destinationSite.getHostname()));
				final List<ContentType> sourceContentTypes = this.contentTypeAPI.search("",
						BaseContentType.ANY, "upper(name)", -1, 0, sourceSite.getIdentifier());
				Logger.info(this, String.format("-> Copying %d Content Types", sourceContentTypes.size()));
				for (final ContentType sourceContentType : sourceContentTypes) {
					final CopyContentTypeBean.Builder builder = new CopyContentTypeBean.Builder()
							.sourceContentType(sourceContentType)
							.icon(sourceContentType.icon())
							.name(sourceContentType.name())
							.folder(sourceContentType.folder())
							.host(destinationSite.getIdentifier());
					// Copy the Content Type objects with NONE of their relationship fields
					final ContentType copiedContentType =
							this.contentTypeAPI.copyFromAndDependencies(builder.build(), destinationSite, false);
					copiedContentTypesBySourceId.put(sourceContentType.id(), new ContentTypeMapping(sourceContentType, copiedContentType));
				}
				final List<RelationshipMapping> childRelationships = new ArrayList<>();
				// Now, copy the relationship fields back, but EXCLUDE all relationship fields that are pointing
				// to Content Types living under System Host
				for (final ContentType sourceContentType : sourceContentTypes) {
					final List<Field> sourceRelationshipFields = sourceContentType.fields(RelationshipField.class);
					for (final Field sourceField : sourceRelationshipFields) {
						final RelationshipField sourceRelationshipField = (RelationshipField) sourceField;
						final Relationship sourceRelationship = this.relationshipAPI.getRelationshipFromField(sourceRelationshipField, this.SYSTEM_USER);
						if (this.hasSystemHostTypes(sourceRelationship)) {
							Logger.warn(this, String.format("Skipping Relationship Field '%s' from Content Type '%s'" +
											" because it points to at least one Content Type living under System Host",
									sourceRelationshipField.name(), sourceContentType.name()));
							continue;
						}
						final ContentTypeMapping parentContentTypeMapping = copiedContentTypesBySourceId.get(sourceRelationship.getParentStructure().id());
						final ContentTypeMapping childContentTypeMapping = copiedContentTypesBySourceId.get(sourceRelationship.getChildStructure().id());
						checkNotNull(parentContentTypeMapping, "Parent Content Type ID in Relationship Field " +
								"'%s' in Content Type '%s' is null", sourceRelationshipField.name(), sourceContentType.name());
						checkNotNull(childContentTypeMapping, "Child Content Type ID in Relationship Field " +
								"'%s' in Content Type '%s' is null", sourceRelationshipField.name(), sourceContentType.name());
						// If this Relationship Field represents the parent of the relationship, or if the relationship is between
						// the same Content Types, just copy the field with the new IDs and data
						if (this.relationshipAPI.isChildField(sourceRelationship, sourceRelationshipField) || this.relationshipAPI.sameParentAndChild(sourceRelationship)) {
							final String copiedContentTypeId = copiedContentTypesBySourceId.get(sourceContentType.id()).destinationContentType.id();
							final String copiedContentTypeVarName = childContentTypeMapping.destinationContentType.variable();
							this.createRelationshipField(copiedContentTypeId,
									copiedContentTypeVarName, sourceRelationshipField,
									sourceRelationship, copiedRelationshipsBySourceId);
						} else {
							// Here, the Relationship Field is the child of the current Relationship and its parent Relationship has
							// already been copied. Therefore, we can create the child Relationship Field now and reference the
							// existing relationship
							if (copiedRelationshipsBySourceId.containsKey(sourceRelationship.getInode())) {
								final Relationship copiedRelationship = copiedRelationshipsBySourceId.get(sourceRelationship.getInode()).destinationRelationship;
								final String copiedContentTypeId = copiedContentTypesBySourceId.get(sourceContentType.id()).destinationContentType.id();
								this.createRelationshipField(copiedContentTypeId,
										copiedRelationship.getRelationTypeValue(), sourceRelationshipField,
										sourceRelationship, copiedRelationshipsBySourceId);
							} else {
								// If the Relationship Field points to a relationship that hasn't been copied yet, we'll wait until
								// the parent Relationship is created and store its data in a list
								final RelationshipMapping childRelationshipMapping =
										new RelationshipMapping(sourceRelationship, sourceContentType, sourceRelationshipField);
								childRelationships.add(childRelationshipMapping);
							}
						}

					}
				}
				if (!childRelationships.isEmpty()) {
					// Copy all child Relationships that don't have a parent Relationship, if any
					for (final RelationshipMapping childRelationshipMapping : childRelationships) {
						this.copyChildRelationship(childRelationshipMapping.sourceRelationship,
								childRelationshipMapping.sourceContentType,
								childRelationshipMapping.sourceField, copiedRelationshipsBySourceId,
								copiedContentTypesBySourceId);
					}
				}
				HibernateUtil.closeAndCommitTransaction();
				HibernateUtil.startTransaction();
			}

			// ======================================================================
			// Copying content on site
			// ======================================================================
			this.siteCopyStatus.addMessage("copying-content-on-host");
			int contentCount = 0;

            // Option 1: Copy ONLY content pages WITHOUT other contents, if
			// copyOptions.isCopyContentOnHost() == true should be handled by option 2.
            if (!copyOptions.isCopyContentOnHost() && copyOptions.isCopyLinks()) {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying HTML Pages - but NOT their contents - to new Site '%s'", destinationSite.getHostname()));

				final int batchSize = CONTENT_BATCH_SIZE.get();
				final int relationshipBatchSize = RELATIONSHIP_BATCH_SIZE.get();
				final int batchPauseMs = BATCH_PAUSE_MS.get();

                try (final PaginatedContentlets sourceContentlets = this.contentAPI.findContentletsPaginatedByHost(sourceSite,
                        List.of(BaseContentType.HTMLPAGE.getType()), null, this.SYSTEM_USER,
						DONT_RESPECT_FRONTEND_ROLES)) {

					if (sourceContentlets.isUsingScrollApi()) {
						Logger.debug(this, "-> Using Scroll API for large result set");
					}

					currentProgress = 70;
					progressIncrement = (95 - 70) / (double) sourceContentlets.size();
					Logger.info(this, String.format("-> Copying %d HTML Pages (batch size: %d, pause: %dms)",
							sourceContentlets.size(), batchSize, batchPauseMs));

					for (final Contentlet sourceContent : sourceContentlets) {
                        if (null != sourceContent) {
                            this.processCopyOfContentlet(sourceContent, copyOptions,
                                    sourceSite, destinationSite, copiedContentsBySourceId, copiedFoldersBySourceId,
                                    copiedContainersBySourceId, copiedTemplatesBySourceId,
                                    contentsWithRelationships, copiedContentTypesBySourceId);

                            currentProgress += progressIncrement;
                            contentCount++;

                            this.siteCopyStatus.updateProgress((int) currentProgress);

                            // Commit more frequently and clear Hibernate session to prevent memory buildup
                            if (contentCount % batchSize == 0) {
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }

                            // Process relationships in batches to prevent unbounded list growth
                            if (contentsWithRelationships.size() >= relationshipBatchSize) {
                                Logger.debug(this, () -> String.format("Processing batch of %d relationships", contentsWithRelationships.size()));
                                this.copyRelatedContentlets(contentsWithRelationships, copiedContentsBySourceId,
                                        copiedRelationshipsBySourceId, copyOptions, sourceSite, destinationSite);
                                contentsWithRelationships.clear();
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }
                        }
					} // end for loop

					// Copy remaining contentlet dependencies
					if (!contentsWithRelationships.isEmpty()) {
						Logger.debug(this, String.format("-> Processing final batch of %d relationships", contentsWithRelationships.size()));
						this.copyRelatedContentlets(contentsWithRelationships, copiedContentsBySourceId,
								copiedRelationshipsBySourceId, copyOptions, sourceSite, destinationSite);
						contentsWithRelationships.clear();
					}
				} // end try-with-resources (PaginatedContentlets will auto-close and clear scroll)
            }

            // Option 2: Copy all content on site
            if (copyOptions.isCopyContentOnHost()) {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying ALL contents to new Site '%s'", destinationSite.getHostname()));

				final int batchSize = CONTENT_BATCH_SIZE.get();
				final int relationshipBatchSize = RELATIONSHIP_BATCH_SIZE.get();
				final int batchPauseMs = BATCH_PAUSE_MS.get();

				// Use try-with-resources to ensure Scroll context is cleaned up
                try (final PaginatedContentlets sourceContentlets = this.contentAPI.findContentletsPaginatedByHost(sourceSite,
						this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES)) {

					if (sourceContentlets.isUsingScrollApi()) {
						Logger.debug(this, "-> Using Scroll API for large result set");
					}

					currentProgress = 70;
					progressIncrement = (95 - 70) / (double) sourceContentlets.size();
					Logger.info(this, String.format("-> Total contentlets to copy: %d (batch size: %d, relationship batch: %d, pause: %dms)",
							sourceContentlets.size(), batchSize, relationshipBatchSize, batchPauseMs));

					// Strategy: Process simple content immediately, collect HTML pages for later processing
					final ArrayList<String> htmlPageInodes = new ArrayList<>(
                            Math.min(HTML_PAGES_MAX_INITIAL_CAPACITY.get(), (int) sourceContentlets.size()));
					int simpleContentCount = 0;

                    Logger.info(this, "-> Processing contentlets (simple content first, then HTML pages)");
                    for (final Contentlet sourceContent : sourceContentlets) {
                        if (null == sourceContent) {
                            continue;
                        }

                        if (sourceContent.isHTMLPage()) {
                            // Collect HTML pages for second pass (inodes only to minimize memory)
                            htmlPageInodes.add(sourceContent.getInode());
                        } else {
                            // Process simple content immediately
                            this.processCopyOfContentlet(sourceContent, copyOptions,
                                    sourceSite, destinationSite, copiedContentsBySourceId, copiedFoldersBySourceId,
                                    copiedContainersBySourceId, copiedTemplatesBySourceId,
                                    contentsWithRelationships, copiedContentTypesBySourceId);
                            currentProgress += progressIncrement;
                            contentCount++;
                            simpleContentCount++;

                            this.siteCopyStatus.updateProgress((int) currentProgress);

                            // Commit more frequently and clear Hibernate session to prevent memory buildup
                            if (contentCount % batchSize == 0) {
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }

                            // Process relationships in batches to prevent unbounded list growth
                            final int contentCountFinal = contentCount;
                            if (contentsWithRelationships.size() >= relationshipBatchSize) {
                                Logger.debug(this, () -> String.format("Processing batch of %d relationships at content %d",
                                        contentsWithRelationships.size(), contentCountFinal));
                                this.copyRelatedContentlets(contentsWithRelationships, copiedContentsBySourceId,
                                        copiedRelationshipsBySourceId, copyOptions, sourceSite, destinationSite);
                                contentsWithRelationships.clear();
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }
                        }
                    }
                    Logger.info(this, String.format("-> %d simple contents have been copied", simpleContentCount));

                    // Trim the ArrayList to actual size to free unused capacity
                    htmlPageInodes.trimToSize();

                    // Now process HTML Pages that were collected during first pass
                    // Processing them second makes it easier to associate page contents when updating the multi-tree
                    Logger.info(this, String.format("-> Now copying %d HTML Pages", htmlPageInodes.size()));
                    final int htmlPagesStart = contentCount;
                    for (final String htmlPageInode : htmlPageInodes) {
                        // Load the contentlet by inode
                        final Contentlet sourceContent = Try.of(() ->
                                this.contentAPI.find(htmlPageInode, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES))
                                .getOrElseThrow(e -> new DotDataException(
                                        "Error loading HTML page: " + htmlPageInode, e));

                        if (null != sourceContent) {
                            this.processCopyOfContentlet(sourceContent, copyOptions,
                                    sourceSite, destinationSite, copiedContentsBySourceId, copiedFoldersBySourceId,
                                    copiedContainersBySourceId, copiedTemplatesBySourceId,
                                    contentsWithRelationships, copiedContentTypesBySourceId);
                            currentProgress += progressIncrement;
                            contentCount++;

                            siteCopyStatus.updateProgress((int) currentProgress);

                            // Commit more frequently and clear Hibernate session
                            if (contentCount % batchSize == 0) {
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }

                            // Process relationships in batches
                            final int contentCountFinal = contentCount;
                            if (contentsWithRelationships.size() >= relationshipBatchSize) {
                                Logger.debug(this, () -> String.format("Processing batch of %d relationships at content %d",
                                        contentsWithRelationships.size(), contentCountFinal));
                                this.copyRelatedContentlets(contentsWithRelationships, copiedContentsBySourceId,
                                        copiedRelationshipsBySourceId, copyOptions, sourceSite, destinationSite);
                                contentsWithRelationships.clear();
                                this.commitAndClearSession();
                                this.pauseBetweenBatches(batchPauseMs);
                            }
                        }
                    }
                    Logger.info(this, String.format("-> %d HTML pages have been copied", contentCount - htmlPagesStart));
                    Logger.info(this, String.format("-> A total of %d contents have been copied", contentCount));

                    // Copy remaining contentlet dependencies
                    if (!contentsWithRelationships.isEmpty()) {
                        Logger.info(this, String.format("-> Processing final batch of %d relationships", contentsWithRelationships.size()));
                        this.copyRelatedContentlets(contentsWithRelationships, copiedContentsBySourceId,
                                copiedRelationshipsBySourceId, copyOptions, sourceSite, destinationSite);
                        contentsWithRelationships.clear();
                    }
				} // end try-with-resources (PaginatedContentlets will auto-close and clear scroll)
            }

			this.siteCopyStatus.updateProgress(95);

			// Copying HostVariables
			this.siteCopyStatus.addMessage("copying-host-variables");
			if (copyOptions.isCopyHostVariables()) {
				Logger.info(this, "----------------------------------------------------------------------");
				Logger.info(this, String.format(":::: Copying Site Variables to new Site '%s'", destinationSite.getHostname()));
				String variableKey = "";
				try {
					final HostVariableAPI hostVariablesAPI = APILocator.getHostVariableAPI();
					final List<HostVariable> sourceVariables = hostVariablesAPI.getVariablesForHost(sourceSite
							.getIdentifier(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
					Logger.info(this, String.format("-> Copying %d Site Variables", sourceVariables.size()));
					for (final HostVariable variable : sourceVariables) {
						variableKey = variable.getKey();
						hostVariablesAPI.copy(variable, destinationSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
					}
				} catch (final Exception e) {
					Logger.error(this, String.format("An error occurred when copying Site Variable '%s' from Site '%s'" +
							". The process will continue...", variableKey, sourceSite.getHostname()), e);
				}
			}
			siteCopyStatus.updateProgress(100);

			final String destinationSiteIdentifier = destinationSite.getIdentifier();
			HibernateUtil.addCommitListener("Host"+destinationSiteIdentifier, ()-> triggerEvents(destinationSiteIdentifier));
			HibernateUtil.closeAndCommitTransaction();
		} catch (final Exception e) {
			Logger.error(this, String.format("A general error has occurred when copying Site '%s' to Site '%s'. The " +
					"Site Copy process will stop now!", sourceSite.getHostname(), destinationSite.getHostname()), e);
			HibernateUtil.rollbackTransaction();
			throw e;
		} finally {
			HibernateUtil.closeSessionSilently();
		}
	}

	/**
	 * Creates a Relationship Field in a given Content Type with a list of specific parameters, and
	 * keeps track of the generated Relationship record. This method is very useful when Content
	 * Types are being copied over to the new Site and Relationships are present.
	 * <p>Relationship Fields must be carefully handled as the Relationship they generate must
	 * reflect the new IDs and Velocity Variable Names from the copied Content Types while keeping
	 * the exact same structure as the original one. Keep in mind that the API will create both the
	 * Relationship Field and the Relationship record. It's very important that the new
	 * Relationship be added to the Relationship tracking map.</p>
	 *
	 * @param contentTypeId       The ID of the Content Type where the new Relationship Field
	 *                            will be added.
	 * @param relationTypeValue   The Velocity Variable Name of the Content Type that the
	 *                            Relationship Field will point to, or the existing Relation Type
	 *                            Value in case the Relationship already exists.
	 * @param relationshipField   The source {@link RelationshipField} that will be used to build
	 *                            the new field, as it contain data that can be simply reused.
	 * @param sourceRelationship  The source {@link Relationship} that will be added to the map of
	 *                            copied Relationships.
	 * @param copiedRelationships The map containing the copied Relationships from the source Site.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The current User does not have the permissions to perform this
	 *                              action.
	 */
	private void createRelationshipField(final String contentTypeId,
										 final String relationTypeValue,
										 final RelationshipField relationshipField,
										 final Relationship sourceRelationship,
										 final Map<String, RelationshipMapping> copiedRelationships) throws DotDataException, DotSecurityException {
		final Field newField = RelationshipFieldBuilder.builder(relationshipField)
				.sortOrder(relationshipField.sortOrder())
				.contentTypeId(contentTypeId)
				.id(null)
				.relationType(relationTypeValue)
				.build();
		this.contentTypeFieldAPI.save(newField, this.SYSTEM_USER);
		final Relationship copiedRelationship = this.relationshipAPI.getRelationshipFromField(newField, this.SYSTEM_USER);
		copiedRelationships.put(sourceRelationship.getInode(), new RelationshipMapping(sourceRelationship, copiedRelationship));
	}

	/**
	 * Depending on the order in which Content Types are copied, there might be a situation where
	 * a child Relationship is retrieved before its parent Relationship has been saved. This method
	 * takes all the child Relationships that could not be processed yet, and copies them to the new
	 * Content Type. If their parent Relationship was added, then it will be associated to the
	 * existing Relationship; if there's no parent Relationship, a new one will be created.
	 *
	 * @param sourceRelationship      The source child {@link Relationship} used to retrieve useful
	 *                                information from the source data. Also, it will be added to
	 *                                the map of copied Relationships.
	 * @param sourceContentType       The source {@link ContentType} that contains the child
	 *                                Relationship.
	 * @param sourceRelationshipField The source {@link RelationshipField} that represents the
	 *                                child Relationship and will be used to create the new field.
	 * @param copiedRelationships     The map containing the copied Relationships from the source
	 *                                Content Types.
	 * @param copiedContentTypes      The map containing the copied Content Types from the source
	 *                                Site.
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The current User does not have the permissions to perform this
	 *                              action.
	 */
	private void copyChildRelationship(final Relationship sourceRelationship,
									   final ContentType sourceContentType,
									   final RelationshipField sourceRelationshipField,
									   final Map<String, RelationshipMapping> copiedRelationships,
									   final Map<String, ContentTypeMapping> copiedContentTypes) throws DotDataException, DotSecurityException {
		String contentTypeId;
		String relationTypeValue;
		if (copiedRelationships.containsKey(sourceRelationship.getInode())) {
			// Copy the Relationship Field using an existing Relationship
			final Relationship copiedRelationship = copiedRelationships.get(sourceRelationship.getInode()).destinationRelationship;
			contentTypeId = copiedContentTypes.get(sourceContentType.id()).destinationContentType.id();
			relationTypeValue = copiedRelationship.getRelationTypeValue();
		} else {
			// Copy the Relationship Field and create a new Relationship as it doesn't exist yet
			final ContentTypeMapping parentContentTypeMapping = copiedContentTypes.get(sourceRelationship.getParentStructureInode());
			final ContentTypeMapping childContentTypeMapping = copiedContentTypes.get(sourceRelationship.getChildStructureInode());
			contentTypeId = childContentTypeMapping.destinationContentType.id();
			relationTypeValue = parentContentTypeMapping.destinationContentType.variable();
		}
		this.createRelationshipField(contentTypeId, relationTypeValue, sourceRelationshipField, sourceRelationship, copiedRelationships);
	}

	/**
	 * Indicates whether the given {@link Relationship} references at least one Content Type that
	 * lives under System Host.
	 *
	 * @param relationship The {@link Relationship} to be analyzed.
	 *
	 * @return If the Relationship contains at least one Content Type that lives under System Host,
	 * returns {@code true}.
	 */
	private boolean hasSystemHostTypes(final Relationship relationship) {
		final String parentContentTypeSiteId = relationship.getParentStructure().getHost();
		final String childContentTypeSiteId = relationship.getChildStructure().getHost();
		return Host.SYSTEM_HOST.equals(parentContentTypeSiteId) || Host.SYSTEM_HOST.equals(childContentTypeSiteId);
	}

	private void triggerEvents (final String destinationSiteIdentifier) {

		final SiteCreatedEvent siteCreatedEvent = new SiteCreatedEvent();
		siteCreatedEvent.setSiteIdentifier(destinationSiteIdentifier);

		APILocator.getLocalSystemEventsAPI().notify(siteCreatedEvent);  // LOCAL

		Try.run(()->APILocator.getSystemEventsAPI()					    // CLUSTER WIDE
				.push(SystemEventType.CLUSTER_WIDE_EVENT, new Payload(siteCreatedEvent)))
				.onFailure(e -> Logger.error(HostAssetsJobImpl.this, e.getMessage()));

		Try.run(()->APILocator.getSystemEventsAPI()					    // for the socket
				.push(SystemEventType.CREATED_SITE, new Payload(destinationSiteIdentifier)))
				.onFailure(e -> Logger.error(HostAssetsJobImpl.this, e.getMessage()));
	}

	/**
	 * This method will take a template and make a copy of it, but it'll also update all the
	 * references in that template to point to the new mapped containers passed in the
	 * copiedContainersBySourceId
	 *
	 * @param sourceTemplate             origin template
	 * @param destinationSite            target site
	 * @param copiedContainersBySourceId Copy Containers reference map
	 *
	 * @return resulting Copy Template
	 *
	 * @throws DotDataException An error occurred when interacting with the database.
	 */
	private Template copyTemplate(final Template sourceTemplate, final Host destinationSite,
			final Map<String, Container> copiedContainersBySourceId) throws DotDataException {
		try {
			final Template sourceTemplateCopy = new Template();
			BeanUtils.copyProperties(sourceTemplateCopy, sourceTemplate);
			String drawnBody = sourceTemplateCopy.getDrawedBody();

			if (UtilMethods.isSet(drawnBody)) {
				final Set<String> containersIdOrPath = getContainersIdentifierOrPath(sourceTemplateCopy);

				drawnBody = replaceContainers(destinationSite, copiedContainersBySourceId, drawnBody, containersIdOrPath);
				sourceTemplateCopy.setDrawedBody(drawnBody);
			}

			String body = sourceTemplateCopy.getBody();
			if (UtilMethods.isSet(body)) {
			  final Set<String> containersIdOrPath = StringUtils.quotedLiteral(body)
					  .stream()
					  .filter(literalValue -> UUIDUtil.isUUID(literalValue) ||
							  FileAssetContainerUtil.getInstance().isFullPath(literalValue))
					  .collect(Collectors.toSet());

			  body = replaceContainers(destinationSite, copiedContainersBySourceId, body, containersIdOrPath);
			  sourceTemplateCopy.setBody(body);
			}

			return this.templateAPI.copy(sourceTemplateCopy, destinationSite, false, null, this.SYSTEM_USER,
					DONT_RESPECT_FRONTEND_ROLES);
		} catch (final Exception e) {
			throw new DotDataException(String.format("An error occurred when copying Template '%s' [%s] to Site '%s'",
					sourceTemplate.getTitle(), sourceTemplate.getIdentifier(), destinationSite.getHostname()), e);
		}
	}

	private String replaceContainers(
			final Host destinationSite,
			final Map<String, Container> copiedContainersBySourceId,
			final String drawnBody,
			final Set<String> containersIdOrPath) {

		String newDrawedBody = drawnBody;

		for (final String idOrPath : containersIdOrPath) {
			final Container copyContainer = copiedContainersBySourceId.get(idOrPath);

			if (null != copyContainer) {
				String copyContainerIdOrPath = null;

				if (FileAssetContainerUtil.getInstance().isFileAssetContainer(copyContainer)) {
					final String hostName = FileAssetContainerUtil.getInstance().getHostName(idOrPath);
					newDrawedBody = newDrawedBody.replaceAll(hostName, destinationSite.getHostname());
				} else {
					copyContainerIdOrPath = copyContainer.getIdentifier();
					newDrawedBody = newDrawedBody.replaceAll(idOrPath, copyContainerIdOrPath);
				}

			}
		}
		return newDrawedBody;
	}
	
	private Set<String> getContainersIdentifierOrPath(final Template sourceTemplateCopy)
			throws DotDataException, DotSecurityException {

		final TemplateLayout layout = DotTemplateTool
				.themeLayout(sourceTemplateCopy.getInode());

		return layout.getContainersIdentifierOrPath();
	}

	/**
	 * Internal method that encloses the copy folder logic re-used across the copy site method The
	 * method will update the Map folderMappingsBySourceId with the new id of the generated folder
	 * If the folder already exist this method will skip copying it and will still return the
	 * folder object that was copied.
	 *
	 * @param sourceFolder             Source original site folder
	 * @param destinationSite          Target site folder
	 * @param folderMappingsBySourceId Map that gets updated once the folder has been copied
	 *
	 * @throws DotDataException     An error occurred when interacting with the database.
	 * @throws DotSecurityException The current User does not have the permissions to perform this
	 *                              action.
	 */
	private Folder copyFolder(final Folder sourceFolder, final Host destinationSite, final Map<String, FolderMapping> folderMappingsBySourceId)
			throws DotDataException, DotSecurityException {
		if (folderMappingsBySourceId.containsKey(sourceFolder.getInode())) {
			Logger.debug(HostAssetsJobImpl.class, () -> String
					.format("---> Folder `%s` has been copied already. ", sourceFolder.getPath()));
			return folderMappingsBySourceId.get(sourceFolder.getInode()).destinationFolder;
		}
		final Folder newFolder = this.folderAPI.createFolders(APILocator.getIdentifierAPI().find(sourceFolder.getIdentifier()).getPath(),
						destinationSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
		newFolder.setTitle(sourceFolder.getTitle());
		if (sourceFolder.isShowOnMenu()) {
			newFolder.setSortOrder(sourceFolder.getSortOrder());
			newFolder.setShowOnMenu(true);
		}
		this.folderAPI.save(newFolder, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
		folderMappingsBySourceId.put(sourceFolder.getInode(), new FolderMapping(sourceFolder, newFolder));
		return newFolder;
	}

	/**
	 * Copies a Contentlet from one site to another. At this point, it's important to keep track of
	 * the elements that have already been copied in order to avoid duplicating information
	 * unnecessarily.
	 * <p>
	 * If the content to copy is a Content Page, then its multi-tree structure needs to be updated,
	 * which involves updating the child references to point to the recently copied contentlets.
	 * </p>
	 *
	 * @param sourceContent                The {@link Contentlet} whose data will be copied.
	 * @param copyOptions                  The preferences selected by the user regarding what
	 *                                     elements of the source site will be copied over to the
	 *                                     new site.
	 * @param destinationSite              The new {@link Host} object that will contain the
	 *                                     information from the source site.
	 * @param copiedContentletsBySourceId  A {@link Map} containing the association between the
	 *                                     Contentlet's Identifier in the source site and the
	 *                                     Contentlet's Identifier in the destination site. This
	 *                                     map keeps both the source and the destination
	 *                                     {@link Contentlet} objects.
	 * @param copiedFoldersBySourceId      A {@link Map} containing the association between the
	 *                                     folder's Identifier from the source site with the
	 *                                     folder's Identifier from the destination site. This map
	 *                                     keeps both the source and the destination {@link Folder}
	 *                                     objects.
	 * @param copiedContainersBySourceId   A {@link Map} that says what containerId from the source
	 *                                     site has become what in the new copy-site.
	 * @param copiedTemplatesBySourceId    A {@link Map} that says what templateId from the source
	 *                                     site has become what in the new copy-site.
	 * @param contentsWithRelationships    The list of Contentlets that have other content related
	 *                                     to them.
	 * @param copiedContentTypesBySourceId A {@link Map} containing the association between the
	 *                                     Content Type's Identifier in the source site and the
	 *                                     Content Type's Identifier in the destination site. This
	 *                                     is relevant ONLY if the
	 *                                     {@code FEATURE_FLAG_ENABLE_CONTENT_TYPE_COPY} property
	 *                                     is enabled.
	 * @param sourceSite               The {@link Host} object from which content is being copied.
	 */
	private Contentlet processCopyOfContentlet(final Contentlet sourceContent,
			final HostCopyOptions copyOptions, final Host sourceSite, final Host destinationSite,
			final Map<String, ContentMapping> copiedContentletsBySourceId,
			final Map<String, FolderMapping> copiedFoldersBySourceId,
			final Map<String, Container> copiedContainersBySourceId,
			final Map<String, HTMLPageAssetAPI.TemplateContainersReMap> copiedTemplatesBySourceId,
			final List<Contentlet> contentsWithRelationships,
		    final Map<String, ContentTypeMapping> copiedContentTypesBySourceId) {

		//Since certain properties are modified here we're going to use a defensive copy to avoid cache issue.
		final Contentlet sourceCopy = new Contentlet(sourceContent);
        Contentlet newContent = null;
        try {
            if (copiedContentletsBySourceId.containsKey(sourceCopy.getIdentifier())) {
                // The content has already been copied
				Logger.debug(HostAssetsJobImpl.class,()->String.format("---> Content identified by `%s` has been copied already.", sourceCopy.getIdentifier()));
                return copiedContentletsBySourceId.get(sourceCopy.getIdentifier()).destinationContent;
            }
			sourceCopy.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
			sourceCopy.getMap().put(Contentlet.DISABLE_WORKFLOW, true);
			sourceCopy.setLowIndexPriority(true);

			if (copyOptions.isCopyTemplatesAndContainers() && sourceCopy.isHTMLPage()) {
				//If we're dealing with pages, we need to pass template mappings to the copyContentlet
				//such a method deals with all versions of the contentlet, and it needs to know about the mapping info internally.
				sourceCopy.getMap().put(Contentlet.TEMPLATE_MAPPINGS, copiedTemplatesBySourceId);
			}
			final ContentType destinationContentType = Try.of(() -> copiedContentTypesBySourceId.get(sourceCopy.getContentTypeId()).destinationContentType).getOrNull();
            if (InodeUtils.isSet(sourceCopy.getFolder())
                    && !sourceCopy.getFolder().equals(this.SYSTEM_FOLDER.getInode())) {
                // The source content has a folder assigned in the source Site we copy it to the
				// same destination folder
                final Folder sourceFolder = this.folderAPI.find(sourceCopy.getFolder(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
                final Folder destinationFolder = copiedFoldersBySourceId.get(sourceFolder.getInode()) != null ? copiedFoldersBySourceId
                        .get(sourceFolder.getInode()).destinationFolder : null;
                if (!copyOptions.isCopyFolders()) {
                    return null;
                }

                if (destinationFolder != null) {
                    // We have already mapped the source folder of the content to a destination
					// folder because the user requested to also copy folders
                    newContent = this.contentAPI.copyContentlet(sourceCopy, destinationContentType, destinationFolder, this.SYSTEM_USER,
							DONT_RESPECT_FRONTEND_ROLES);
                } else {
                    // We don't have a destination folder to set the content to, so we are going to
					// set it to the Site
                    newContent = this.contentAPI.copyContentlet(sourceCopy, destinationContentType, destinationSite, this.SYSTEM_USER,
							DONT_RESPECT_FRONTEND_ROLES);
                }
            } else if (InodeUtils.isSet(sourceCopy.getHost())
                    && !sourceCopy.getHost().equals(this.SYSTEM_HOST.getInode())) {
                // The content is assigned to the source Site, we assign the new content to the new Site
                newContent = this.contentAPI.copyContentlet(sourceCopy, destinationContentType, destinationSite, this.SYSTEM_USER,
						DONT_RESPECT_FRONTEND_ROLES);
            } else {
                // The content has no folder or Site association, so we create a global copy as well
                newContent = this.contentAPI.copyContentlet(sourceCopy, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
            }

            if (copyOptions.isCopyContentOnPages() && newContent.isHTMLPage()) {
				Logger.debug(HostAssetsJobImpl.class,()->String.format("---> Copying contents from page with title `%s` and id `%s`", sourceCopy.getTitle(), sourceCopy.getIdentifier()));
                // Copy page-associated contentlets
                final List<MultiTree> pageContents = APILocator.getMultiTreeAPI().getMultiTrees(sourceCopy.getIdentifier());
				for (final MultiTree sourceMultiTree : pageContents) {
					String newChild = sourceMultiTree.getContentlet();

					// Update the child reference to point to the previously copied content
					if (copiedContentletsBySourceId.containsKey(sourceMultiTree.getContentlet())) {
						newChild = copiedContentletsBySourceId.get(sourceMultiTree.getContentlet()).destinationContent.getIdentifier();
					} else {
						// Contentlet was not copied - validate if it exists and is accessible
						if (shouldSkipMultiTreeEntry(sourceMultiTree.getContentlet(), sourceSite,
								destinationSite, newContent.getIdentifier())) {
							continue;
						}
					}

                    String newContainer = sourceMultiTree.getContainer();
					if(copiedContainersBySourceId.containsKey(sourceMultiTree.getContainer())){
						newContainer = copiedContainersBySourceId.get(sourceMultiTree.getContainer()).getIdentifier();
					}

					final MultiTree multiTree = new MultiTree(newContent.getIdentifier(),
							newContainer,
							newChild,
							sourceMultiTree.getRelationType(),
							sourceMultiTree.getTreeOrder(),
							sourceMultiTree.getPersonalization());

					APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
				}

            }// Pages are a big deal.

            copiedContentletsBySourceId.put(sourceCopy.getIdentifier(), new ContentMapping(sourceCopy, newContent));
			final Contentlet finalNewContent = newContent;
			Logger.debug(HostAssetsJobImpl.class,()->String.format("---> Re-Mapping content: Identifier `%s` now points to `%s`.", sourceCopy.getIdentifier(), finalNewContent
					.getIdentifier()));

			this.checkRelatedContentToCopy(sourceCopy, contentsWithRelationships, destinationSite);
        } catch (final Exception e) {
			Logger.error(this, String.format("An error occurred when copying content '%s' from Site '%s' to Site '%s'." +
					" The process will continue...", sourceCopy.getIdentifier(), sourceCopy.getHost(),
					destinationSite.getHostname()), e);
		}
        return newContent;
    }

	/**
	 * Copies the Relationship data of Contentlets that have child relationships based on the newly
	 * assigned Identifiers after they have been copied to the new Site; i.e., parent Contentlets.
	 * <p>If no Content Types are being copied to the new Site, the relationship data update will
	 * only include adding the new related Contentlet copies that now live in the new Site. But, if
	 * Content Types ARE being copied, the copied Relationships must be taken into account to save
	 * the copied Contentlets to the new Relationship records as they reflect the new IDs for the
	 * copied Content Types as well.</p>
	 * <p>Now, depending on the value of the
	 * {@link #KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES} property -- defaults to {@code
	 * true} -- this method can simply save relationships to Contentlets that live in other Sites
	 * as well. For instance, if the related content lives under System Host or a Site that is
	 * neither the source Site nor the copied Site, we just need the copied record to point to them
	 * so the relationship data won't get lost.</p>
	 *
	 * @param contentsWithRelationships     The list of {@link Contentlet} objects that are the
	 *                                      parents of a given relationship.
	 * @param copiedContentsBySourceId      The {@link Map} containing the association between the
	 *                                      contentlet's Identifier from the source site and the
	 *                                      contentlet's Identifier from the destination site. This
	 *                                      map keeps both the source and the destination
	 *                                      {@link Contentlet} objects.
	 * @param copiedRelationshipsBySourceId The {@link Map} containing the copied Relationships
	 *                                         from
	 *                                      the source Site. This way, the copied Contentlets will
	 *                                      be saved just like the original ones.
	 * @param copyOptions                   The {@link HostCopyOptions} object containing what
	 *                                      objects from the source Site must be copied to the
	 *                                      destination Site.
	 * @param sourceSite                    The original Site where the data is being copied from.
	 * @param destinationSite               The new Site where the copied data will be stored.
	 *
	 * @throws DotDataException     An error occurred when updating records in the database.
	 * @throws DotSecurityException The {@link User} accessing the APIs doesn't have the required
	 *                              permissions to perform this action.
	 */
	private void copyRelatedContentlets(final List<Contentlet> contentsWithRelationships,
										final Map<String, ContentMapping> copiedContentsBySourceId,
										final Map<String, RelationshipMapping> copiedRelationshipsBySourceId,
										final HostCopyOptions copyOptions, final Host sourceSite,
										final Host destinationSite) throws DotDataException, DotSecurityException {
        for (final Contentlet sourceContent : contentsWithRelationships) {
            boolean isDestinationContentInOtherSite = false;
			Contentlet destinationContent;
			if (!sourceSite.getIdentifier().equals(sourceContent.getHost())
					&& !destinationSite.getIdentifier().equals(sourceContent.getHost())) {
				// The related content lives in a whole different Site, so we just need to save the relationship
				// as is and not consider copied ID vs new ID, because that comparison doesn't exist. This happens
				// when the KEEP_RELATED_CONTENT_FROM_DIFFERENT_COPIED_SITE property is enabled only.
				destinationContent = sourceContent;
				isDestinationContentInOtherSite = true;
			} else {
				destinationContent = copiedContentsBySourceId.get(sourceContent.getIdentifier()).destinationContent;
			}
            final Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<>();
            final List<Relationship> relationshipsInContentType = this.relationshipAPI.byContentType(sourceContent.getContentType());
            for (final Relationship relationship : relationshipsInContentType) {
                if (!contentRelationships.containsKey(relationship)) {
                    contentRelationships.put(relationship, new ArrayList<>());
                }
				final List<Contentlet> relatedContentlets =
						this.contentAPI.getRelatedContent(sourceContent, relationship, this.SYSTEM_USER, RESPECT_FRONTEND_ROLES);
                List<Contentlet> records = new ArrayList<>();
                for (final Contentlet relatedContentlet : relatedContentlets) {
                    records = contentRelationships.get(relationship);
                    if (UtilMethods.isSet(copiedContentsBySourceId.get(relatedContentlet.getIdentifier()))) {
						final Tree relationshipData =
								TreeFactory.getTree(relatedContentlet.getIdentifier(),
										sourceContent.getIdentifier(), relationship.getRelationTypeValue());
						// In self-related Relationships, we need to make sure that the related Contentlet is NOT the
						// parent in the relationship in order to NOT create a duplicate relationship
						if (this.relationshipAPI.sameParentAndChild(relationship) && UtilMethods.isSet(relationshipData.getParent())) {
							continue;
						}
                        records.add(copiedContentsBySourceId.get(relatedContentlet.getIdentifier()).destinationContent);
						if (isDestinationContentInOtherSite) {
							records.add(relatedContentlet);
						}
                    } else {
                        records.add(relatedContentlet);
                    }
                }
				if (!records.isEmpty()) {
					ContentletRelationshipRecords related;
					if (!CONTENT_TYPE_COPY_FLAG.get() || !copyOptions.isCopyContentTypes()) {
						related = new ContentletRelationships(
								destinationContent).new ContentletRelationshipRecords(relationship, true);
					} else {
						if (!copiedRelationshipsBySourceId.containsKey(relationship.getInode())) {
							continue;
						}
						final Relationship copiedRelationship = copiedRelationshipsBySourceId.get(relationship.getInode()).destinationRelationship;
						related = new ContentletRelationships(destinationContent)
								.new ContentletRelationshipRecords(copiedRelationship, true);
					}
					related.setRecords(records);
					this.contentAPI.relateContent(destinationContent, related, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
				}
            }
        }
    }

	/**
	 * Verifies if the specified {@link Contentlet} object has one or more valid related
	 * contentlets. That is, whether the Contentlet is the parent of the relationship or not. Keep
	 * in mind that, in the case of self-related Content Types, the parent and child point to each
	 * other, so the Tree data must be accessed and read accordingly.
	 *
	 * @param contentlet                   The Contentlet whose relationships will be verified.
	 * @param contentletsWithRelationships The list of Contentlets that have relationships, and
	 *                                     need to be processed later.
	 * @param destinationSite              The new Site where the copied data will be stored.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified user does not have the required permissions to
	 *                              perform this action.
	 */
	private void checkRelatedContentToCopy(final Contentlet contentlet,
										   final List<Contentlet> contentletsWithRelationships,
										   final Host destinationSite) throws DotDataException, DotSecurityException {
		if (contentlet == null) {
			return;
		}
		final List<Relationship> relationshipsByCT =
				this.relationshipAPI.byContentType(contentlet.getContentType());
		for (final Relationship relationship : relationshipsByCT) {
			final List<Contentlet> relatedContents = this.contentAPI.getRelatedContent(contentlet,
					relationship, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
			if (KEEP_RELATED_CONTENT_FROM_DIFFERENT_SITES_FLAG.get()) {
				// If the related content belongs to System Host or a Site that is NOT the source
				// or destination Site, just add it to the list so the relationship record gets
				// copied as it is and the data doesn't get lost.
				for (final Contentlet relatedContent : relatedContents) {
					if (!contentlet.getHost().equals(relatedContent.getHost())
							&& !destinationSite.getIdentifier().equals(relatedContent.getHost())) {
						contentletsWithRelationships.add(relatedContent);
					}
				}
			}
			if (!relatedContents.isEmpty() && this.relationshipAPI.isParent(relationship,
					contentlet.getContentType())) {
				contentletsWithRelationships.add(contentlet);
			}
		}
	}

    @Override
    protected int[] getAllowedVersions() {
        return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };
    }

    private static class ContentMapping {
        @SuppressWarnings("unused")
        Contentlet sourceContent;
        Contentlet destinationContent;

        public ContentMapping(Contentlet sourceContent, Contentlet destinationContent) {
            super();
            this.sourceContent = sourceContent;
            this.destinationContent = destinationContent;
        }
    }

    private static class FolderMapping {
        Folder sourceFolder;
        Folder destinationFolder;

        public FolderMapping(Folder sourceFolder, Folder destinationFolder) {
            super();
            this.sourceFolder = sourceFolder;
            this.destinationFolder = destinationFolder;
        }

    }

	/**
	 * This mapping class stores the relationship between source Content Types, and copied Content
	 * Types. This is very useful when Content Types are copied.
	 */
	private static class ContentTypeMapping {

		ContentType sourceContentType;
		ContentType destinationContentType;

		/**
		 * Default class constructor.
		 *
		 * @param sourceContentType      The {@link ContentType} from the source/original Site.
		 * @param destinationContentType The {@link ContentType} from the new Site that is taking
		 *                               the copied data.
		 */
		public ContentTypeMapping(final ContentType sourceContentType,
								  final ContentType destinationContentType) {
			this.sourceContentType = sourceContentType;
			this.destinationContentType = destinationContentType;
		}

    }

	/**
	 * This mapping class stores the relationship between source Relationships and copied
	 * Relationships. This is very useful when Content Types are copied as it helps re-create the
	 * appropriate data Relationship structures in the copied Content Types.
	 */
	private static class RelationshipMapping {

		final ContentType sourceContentType;
		final RelationshipField sourceField;
		final Relationship sourceRelationship;
		final Relationship destinationRelationship;

		/**
		 * Creates a Relationship Mapping for a copied Content Type.
		 *
		 * @param sourceRelationship      The {@link Relationship} record from the source/original
		 *                                Site.
		 * @param destinationRelationship The {@link Relationship} record for the new Site that is
		 *                                taking the copied data.
		 */
		public RelationshipMapping(final Relationship sourceRelationship,
								   final Relationship destinationRelationship) {
			this(sourceRelationship, destinationRelationship, null, null);
		}

		/**
		 * Creates a Relationship Mapping for a copied Content Type.
		 *
		 * @param sourceRelationship The {@link Relationship} record from the source/original Site.
		 * @param sourceContentType  The {@link ContentType} from the source/original Site.
		 * @param sourceField        The {@link RelationshipField} from the source/original Content
		 *                           Type.
		 */
		public RelationshipMapping(final Relationship sourceRelationship,
								   final ContentType sourceContentType,
								   final RelationshipField sourceField) {
			this(sourceRelationship, null, sourceContentType, sourceField);
		}

		/**
		 * Creates a Relationship Mapping for a copied Content Type.
		 *
		 * @param sourceRelationship      The {@link Relationship} record from the source/original
		 *                                Site.
		 * @param destinationRelationship The {@link Relationship} record for the new Site that is
		 *                                taking the copied data.
		 * @param sourceContentType       The {@link ContentType} from the source/original Site.
		 * @param sourceField             The {@link RelationshipField} from the source/original
		 *                                Content Type.
		 */
		public RelationshipMapping(final Relationship sourceRelationship,
								   final Relationship destinationRelationship,
								   final ContentType sourceContentType,
								   final RelationshipField sourceField) {
			this.sourceRelationship = sourceRelationship;
			this.destinationRelationship = destinationRelationship;
			this.sourceContentType = sourceContentType;
			this.sourceField = sourceField;
		}

	}

	/**
	 * Validates whether a contentlet referenced in a MultiTree entry can be used in the
	 * destination site. This method checks if the contentlet exists and is accessible, and
	 * determines if the MultiTree entry should be skipped.
	 *
	 * @param contentletId    The identifier of the contentlet to validate.
	 * @param sourceSite      The source {@link Host} from which content is being copied.
	 * @param destinationSite The destination {@link Host} to which content is being copied.
	 * @param pageId          The identifier of the HTML page that references this contentlet.
	 *
	 * @return {@code true} if the MultiTree entry should be skipped (contentlet is not
	 *         accessible), {@code false} if the entry should be kept.
	 */
	private boolean shouldSkipMultiTreeEntry(final String contentletId, final Host sourceSite,
											  final Host destinationSite, final String pageId) {
		try {
			final Contentlet originalContentlet = this.contentAPI.findContentletByIdentifierAnyLanguage(
					contentletId, false);

			// Check if the contentlet is from System Host or is accessible from the destination site
			if (originalContentlet.getHost().equals(this.SYSTEM_HOST.getIdentifier())) {
				// Content from System Host can be referenced as-is
				Logger.debug(HostAssetsJobImpl.class, () -> String.format(
						"---> MultiTree references System Host contentlet '%s', keeping original reference",
						contentletId));
				return false;
			} else if (!originalContentlet.getHost().equals(sourceSite.getIdentifier())
					&& !originalContentlet.getHost().equals(destinationSite.getIdentifier())) {
				// Content from a different site - keep reference if accessible
				Logger.debug(HostAssetsJobImpl.class, () -> String.format(
						"---> MultiTree references contentlet '%s' from different site, keeping original reference",
						contentletId));
				return false;
			} else {
				// Contentlet should have been copied but wasn't - skip this MultiTree entry
				Logger.warn(this, String.format(
						"Skipping MultiTree entry for page '%s': contentlet '%s' was not copied and is not accessible from destination site '%s'",
						pageId, contentletId, destinationSite.getHostname()));
				return true;
			}
		} catch (final Exception e) {
			// Contentlet doesn't exist or is not accessible
			Logger.warn(this, String.format(
					"Skipping MultiTree entry for page '%s': contentlet '%s' could not be found or accessed: %s",
					pageId, contentletId, e.getMessage()));
			return true;
		}
	}

	/**
	 * Utility method which validates that the source and destination Sites can be pulled from the database. That is,
	 * it makes sure that both Sites have been already persisted to the database before starting the Site Copy process.
	 *
	 * @param sourceSite        The source Site coming from the database.
	 * @param destinationSite   The destination Site coming from the database.
	 * @param sourceSiteId      The source Site Identifier that was passed down to the Quartz Job.
	 * @param destinationSiteId The destination Site Identifier that was passed down to the Quartz Job.
	 * @param jobName           The name of this specific Site Copy Job.
	 *
	 * @throws JobExecutionException Either the source or destination Site could not be retrieved from the database.
	 */
	private void validateSiteInfo(final Host sourceSite, final Host destinationSite, final String sourceSiteId, final String destinationSiteId,
			final String jobName) throws JobExecutionException {
		final List<String> nullSites = new ArrayList<>();
		if (null == sourceSite || !UtilMethods.isSet(sourceSite.getIdentifier())) {
			nullSites.add(sourceSiteId);
		}
		if (null == destinationSite || !UtilMethods.isSet(destinationSite.getIdentifier())) {
			nullSites.add(destinationSiteId);
		}
		if (!nullSites.isEmpty()) {
			final String errorMsg = String.format("Sites: [%s] came in as null from DB search. Site Copy for Job" +
							" '%s' has been cancelled!", org.apache.commons.lang3.StringUtils.join(nullSites,
					StringPool.COMMA),
					jobName);
			Logger.error(HostAssetsJobImpl.class, errorMsg);
			throw new JobExecutionException(errorMsg);
		}
		Logger.info(HostAssetsJobImpl.class, String.format("-> Source Site: %s [ %s ]", sourceSite.getHostname(),
				sourceSiteId));
		Logger.info(HostAssetsJobImpl.class, String.format("-> Destination Site: %s [ %s ]", destinationSite
				.getHostname(), destinationSiteId));
	}

	/**
	 * Sends a notification to both the System Events API and the Notification API.
	 *
	 * @param message The message to be sent to the logged-in user.
	 * @param userId  The User Identifier that will receive the notification.
	 * @param level   The Notification Level of the message: {@link NotificationLevel#INFO},
	 *                {@link NotificationLevel#WARNING}, or {@link NotificationLevel#ERROR}.
	 */
	private void sendNotification(final String message, final String userId, final NotificationLevel level) {
		final SystemMessageEventUtil messageEventUtil = SystemMessageEventUtil.getInstance();
		if (NotificationLevel.ERROR.equals(level)) {
			messageEventUtil.pushSimpleErrorEvent(new ErrorEntity(StringPool.BLANK, message));
		} else {
			messageEventUtil.pushSimpleTextEvent(message, userId);
		}
		try {
			final User user = this.userAPI.loadUserById(userId);
			APILocator.getNotificationAPI().generateNotification(
					new I18NMessage("Site Copy"),
					new I18NMessage(message),
					null,
					level,
					NotificationType.GENERIC,
					user.getUserId(),
					user.getLocale()
			);
		} catch (final DotDataException | DotSecurityException e) {
			// Notification could not be sent. Just move on
		}
	}

	/**
	 * Commits the current transaction and starts a new one, while also flushing and clearing
	 * the Hibernate session to release memory. This is critical for processing large datasets
	 * to prevent OutOfMemoryErrors.
	 */
	private void commitAndClearSession() {
		try {
			HibernateUtil.flush();
			HibernateUtil.closeAndCommitTransaction();
		} catch (final Exception e) {
			Logger.error(this, "Error flushing/committing transaction", e);
		}
		try {
			HibernateUtil.startTransaction();
		} catch (final Exception e) {
			Logger.error(this, "Error starting new transaction", e);
		}
	}

    /**
	 * Pauses the current thread for the configured duration to reduce system pressure during
	 * large batch operations. This gives the system time to reclaim memory, reduce CPU load,
	 * and prevent resource exhaustion. A minimum pause is always enforced to ensure system health.
	 *
	 * @param pauseMs The requested pause duration in milliseconds. The actual pause will be
	 *                at least MIN_BATCH_PAUSE_MS (10ms), even if this value is 0 or negative.
	 */
	private void pauseBetweenBatches(final int pauseMs) {
		// Always enforce a minimum pause to allow garbage collection, thread scheduling,
		// and prevent CPU thrashing, even if config is set to 0
		final int effectivePauseMs = Math.max(pauseMs, MIN_BATCH_PAUSE_MS);

		try {
			Thread.sleep(effectivePauseMs);
		} catch (final InterruptedException e) {
			Logger.warn(this, "Batch pause was interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

}
