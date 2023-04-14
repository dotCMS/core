package com.dotcms.enterprise.priv;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.enterprise.ParentProxy;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.SiteCreatedEvent;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
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
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.beanutils.BeanUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This Quartz Job allows dotCMS users to copy sites. The Sites portlet also
 * allows users to select what specific pieces of the site (i.e., templates and
 * containers, folders, files, pages, content on pages, etc.) will be copied
 * over to the new site.
 * <p>
 * Keep in mind that depending on the number of elements in a site, this
 * process can take an important amount of time. Therefore, any improvement on 
 * the performance of SQL statements is very important.
 * </p>
 * 
 * @author root
 * @version 3.5
 * @since Jun 11, 2012
 *
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
	private final HostAssetsJobProxy siteCopyStatus;
	private final User SYSTEM_USER;
	private final Host SYSTEM_HOST;
	private final Folder SYSTEM_FOLDER;

	private static final boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;
	private static final boolean RESPECT_FRONTEND_ROLES = Boolean.TRUE;

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
		this.SYSTEM_USER = Try.of(() -> userAPI.getSystemUser()).getOrNull();
		this.SYSTEM_HOST = Try.of(() -> siteAPI.findSystemHost(this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES)).getOrNull();
		this.SYSTEM_FOLDER = Try.of(() -> folderAPI.findSystemFolder()).getOrNull();
	}

	/**
	 * This is the initial execution point of the Site Copy Job. The Quartz Framework calls this method once the Job is
	 * triggered.
	 *
	 * @param jobContext Provides useful information about the Job and allows you to pass down specific properties to
	 *                   the code.
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
				Logger.warn(this, "Site " + sourceSite.getHostname() + " [" + sourceSite.getIdentifier() + "], is already being copied. This second request will be ignored.");
				return;
			}

			Logger.info(HostAssetsJobImpl.class, () -> (String.format("====> Starting Site Copy Job: '%s'", jobName)));
			SystemMessageEventUtil.getInstance().pushSimpleTextEvent(
					"Starting the copy of: " + sourceSite.getHostname() +
					" to: " + destinationSite.getHostname(), userId);
			validateSiteInfo(sourceSite,destinationSite,sourceSiteId,destinationSiteId,jobName);
			copySiteAssets(sourceSite, destinationSite, copyOptions);
		} catch (final DotDataException | DotSecurityException e) {
			final String errorMsg = String
					.format("An error occurred when copying source Site '%s' to destination Site" +
									" '%s': %s", sourceSite.getHostname(), destinationSite.getHostname(),
							e.getMessage());
			Logger.error(HostAssetsJobImpl.class, errorMsg, e);
			throw new JobExecutionException(errorMsg, e);
		}
		Logger.info(HostAssetsJobImpl.class, () -> (String.format("====> Site Copy for Job '%s' has finished " +
				"correctly!", jobName)));
		SystemMessageEventUtil.getInstance().pushSimpleTextEvent(
				"The copy of: " + sourceSite.getHostname() +
						" to: " + destinationSite.getHostname() + " has finished", userId);
	}
	
	private HTMLPageAssetAPI.TemplateContainersReMap getMirrorTemplateContainersReMap(Template sourceTemplate)
			throws DotDataException, DotSecurityException {

		User user = userAPI.getSystemUser();
		boolean respectFrontendRoles = false;

		List<Container> sourceContainers = templateAPI.getContainersInTemplate(sourceTemplate, user,
				respectFrontendRoles);
		List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple>();
		for (Container sourceContainer : sourceContainers) {
			HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple containerMapping = new HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple(
					sourceContainer, sourceContainer);
			containerMappings.add(containerMapping);
		}
		return new HTMLPageAssetAPI.TemplateContainersReMap(sourceTemplate,
				sourceTemplate, containerMappings);
	}

	/**
	 * Performs the copy process of the user-specified elements from the source
	 * site to the destination site.
	 * 
	 * @param sourceSite
	 *            - The {@link Host} object containing the information that will
	 *            be copied.
	 * @param destinationSite
	 *            - The new {@link Host} object that will contain the
	 *            information from the source site.
	 * @param copyOptions
	 *            - The preferences selected by the user regarding what elements
	 *            of the source site will be copied over to the new site.
	 * @throws DotDataException
	 *             An error occurred when interacting with the database.
	 * @throws DotSecurityException
	 *             The current user does not have the permissions to perform
	 *             this action.
	 */
	private void copySiteAssets(final Host sourceSite, final Host destinationSite, final HostCopyOptions copyOptions) throws DotDataException, DotSecurityException {
		try {
			HibernateUtil.startTransaction();
			// Global Vars
			double progressIncrement = 0;
			double currentProgress = 0;

			this.siteCopyStatus.addMessage("copying-templates");
			// ======================================================================
			// Copying templates and containers
			// ======================================================================
			final Map<String, HTMLPageAssetAPI.TemplateContainersReMap> templatesMappingBySourceId = new HashMap<>();
			final Map<String, Container> copiedContainersBySourceId = new HashMap<>();
			final Map<String, FolderMapping> folderMappingsBySourceId = new HashMap<>();
			final Map<String, ContentMapping> contentMappingsBySourceId = new HashMap<>();
			final List<Contentlet> contentsToCopyDependencies =  new ArrayList<>();
			
			if (copyOptions.isCopyTemplatesAndContainers()) {
				Logger.info(HostAssetsJobImpl.class,()->(":::: Copying Templates and containers. "));
				final List<Template> sourceTemplates = this.templateAPI.findTemplatesAssignedTo(sourceSite);
				for (final Template sourceTemplate : sourceTemplates) {
					try {
						final List<Container> sourceContainers = this.templateAPI.getContainersInTemplate(sourceTemplate, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
						final List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<>();
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
										Logger.debug(HostAssetsJobImpl.class,
												() -> (":::: Copying file-asset container "));
										final FileAssetContainer sourceFileAssetContainer = FileAssetContainer.class.cast(sourceContainer);
										final Contentlet sourceContent = this.contentAPI.findContentletByIdentifier(
														sourceFileAssetContainer.getIdentifier(),
														sourceFileAssetContainer.isLive(),
														sourceFileAssetContainer.getLanguageId(),
														this.SYSTEM_USER,
														DONT_RESPECT_FRONTEND_ROLES);
										final Folder sourceFolder = this.folderAPI.find(sourceContent.getFolder(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
										// This should store the new container destination folder into the map.
										final Folder destinationFolder = copyFolder(sourceFolder, destinationSite, folderMappingsBySourceId);
										Logger.debug(HostAssetsJobImpl.class, () -> String
												.format(":::: Container-As-File destination folder path is `%s`",
														destinationFolder.getPath()));
										// now create the Copy of the container as file and all its assets
										final List<Contentlet> processedContentletsList = new ArrayList<>();
										final List<FileAsset> sourceContainerAssets = this.fileAssetAPI
												.findFileAssetsByFolder(sourceFolder, null, false,
														this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
										for (final Contentlet asset : sourceContainerAssets) {
											processedContentletsList.add(
													processCopyOfContentlet(asset, copyOptions,
															destinationSite,
															contentMappingsBySourceId,
															folderMappingsBySourceId,
															copiedContainersBySourceId,
															templatesMappingBySourceId,
															contentsToCopyDependencies
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
													FileAssetContainer.class.isInstance(sourceContainer) ?
															getFullPath(sourceContainer) :
															sourceContainer.getIdentifier();

											copiedContainersBySourceId.put(identifierOrPath, newFileAssetContainer);
											Logger.debug(HostAssetsJobImpl.class, () -> String
													.format(":::: Source Container-As-File `%s` mapped to `%s` ",
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
						templatesMappingBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
					} catch (final Exception e) {
						Logger.error(this, String.format("An error occurred when copying data from Template '%s' [%s] " +
								"from Site '%s' to Site '%s'. The process will continue...", sourceTemplate.getTitle()
								, sourceTemplate.getIdentifier(), sourceSite.getHostname(), destinationSite
										.getHostname()), e);
					}
				}
				// make sure we include any other container previously skipped or ignored in case templates were not copied.
				final List<Container> containersUnderSourceSite = this.containerAPI.findContainersUnder(sourceSite);
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
				Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying Templates."));
				final List<Template> sourceTemplates = this.templateAPI.findTemplatesAssignedTo(sourceSite);
				for (final Template sourceTemplate : sourceTemplates) {
					try {
						final List<Container> sourceContainers = this.templateAPI.getContainersInTemplate(sourceTemplate, this.SYSTEM_USER,
								DONT_RESPECT_FRONTEND_ROLES);
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
						templatesMappingBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
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
				final List<Folder> allSourceFolders = this.folderAPI.findSubFoldersRecursively(sourceSite, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
				Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying Folders."));
				for (final Folder sourceFolder : allSourceFolders) {
					try {
					    copyFolder(sourceFolder, destinationSite, folderMappingsBySourceId);
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
					final Collection<FolderMapping> folders = folderMappingsBySourceId.values();
					// ======================================================================
					// Copying Menu Links
					// ======================================================================
					this.siteCopyStatus.addMessage("copying-menu-links");
					Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying Menu Links."));
					for (final FolderMapping folderMapping : folders) {
						final List<Link> sourceLinks = this.menuLinkAPI.findFolderMenuLinks(folderMapping.sourceFolder);
						for (final Link sourceLink : sourceLinks) {
							try {
								Logger.debug(this, "Copying menu with inode : " + sourceLink.getInode());
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
					Logger.info(HostAssetsJobImpl.class, () -> (":::: Pointing Templates to copied themes."));
					for (final String sourceTemplateId : templatesMappingBySourceId.keySet()) {
						final Template srcTemplate = templatesMappingBySourceId.get(sourceTemplateId).getSourceTemplate();
						if(UtilMethods.isSet(srcTemplate.getTheme()) && folderMappingsBySourceId.containsKey(srcTemplate.getTheme())){
							final String destTemplateInode = templatesMappingBySourceId.get(sourceTemplateId).getDestinationTemplate().getInode();
							final String destTheme = folderMappingsBySourceId.get(srcTemplate.getTheme()).destinationFolder.getInode();
							this.templateAPI.updateThemeWithoutVersioning(destTemplateInode, destTheme);
						}
					}
				}
				
			}
			HibernateUtil.closeAndCommitTransaction();
			HibernateUtil.startTransaction();
			this.siteCopyStatus.updateProgress(70);
			
			// ======================================================================
			// Copying content on site
			// ======================================================================
			this.siteCopyStatus.addMessage("copying-content-on-host");
			int contentCount = 0;

            // Option 1: Copy ONLY content pages WITHOUT other contents, if 
			// copyOptions.isCopyContentOnHost() == true should be handle by option 2.
            if (!copyOptions.isCopyContentOnHost() && copyOptions.isCopyLinks()) {
				Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying Content Pages only."));
                final PaginatedContentlets sourceContentlets = this.contentAPI.findContentletsPaginatedByHost(sourceSite,
                        Arrays.asList(BaseContentType.HTMLPAGE.getType()), null, this.SYSTEM_USER,
						DONT_RESPECT_FRONTEND_ROLES);

                currentProgress = 70;
                progressIncrement = (95 - 70) / (double) sourceContentlets.size();

                for (final Contentlet sourceContent : sourceContentlets) {
                    processCopyOfContentlet(sourceContent, copyOptions,
                            destinationSite, contentMappingsBySourceId, folderMappingsBySourceId,
                            copiedContainersBySourceId, templatesMappingBySourceId,
                            contentsToCopyDependencies);

                    currentProgress += progressIncrement;

					this.siteCopyStatus.updateProgress((int) currentProgress);
                    if (contentCount % 100 == 0) {
                        HibernateUtil.closeAndCommitTransaction();
                        HibernateUtil.startTransaction();
                    }
                    contentCount++;
                }

                // Copy contentlet dependencies
                processToCopyContentletDependencies(contentsToCopyDependencies,
                        contentMappingsBySourceId);
            }

            // Option 2: Copy all content on site
            if (copyOptions.isCopyContentOnHost()) {
				Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying All Content."));
                final PaginatedContentlets sourceContentlets = this.contentAPI.findContentletsPaginatedByHost(sourceSite,
						this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
                currentProgress = 70;
                progressIncrement = (95 - 70) / (double) sourceContentlets.size();

                // Process simple Contents first. This makes it easier to later 
                // associate page contents when updating the multi-tree
                Iterator<Contentlet> ite = sourceContentlets.iterator();
                while (ite.hasNext()) {
                	final Contentlet sourceContent = ite.next();
                	if (!sourceContent.isHTMLPage()) {
	                    processCopyOfContentlet(sourceContent, copyOptions,
	                            destinationSite, contentMappingsBySourceId, folderMappingsBySourceId,
	                            copiedContainersBySourceId, templatesMappingBySourceId,
	                            contentsToCopyDependencies);
	                    // Update progress ONLY if the record is processed
	                    currentProgress += progressIncrement;
						this.siteCopyStatus.updateProgress((int) currentProgress);
	                    if (contentCount % 100 == 0) {
	                        HibernateUtil.closeAndCommitTransaction();
	                        HibernateUtil.startTransaction();
	                    }
	                    contentCount++;
	                    ite.remove();
                	}
                }
                
                // Now process Content Pages. Updating the multi-tree will be 
                // easier since the content is already in
                ite = sourceContentlets.iterator();
                while (ite.hasNext()) {
                	final Contentlet sourceContent = ite.next();
                    processCopyOfContentlet(sourceContent, copyOptions,
                            destinationSite, contentMappingsBySourceId, folderMappingsBySourceId,
                            copiedContainersBySourceId, templatesMappingBySourceId,
                            contentsToCopyDependencies);
                    currentProgress += progressIncrement;
                    siteCopyStatus.updateProgress((int) currentProgress);
                    if (contentCount % 100 == 0) {
                        HibernateUtil.closeAndCommitTransaction();
                        HibernateUtil.startTransaction();
                    }
                    contentCount++;
                }

                // Copy contentlet dependencies
                processToCopyContentletDependencies(contentsToCopyDependencies,
                        contentMappingsBySourceId);
            }

			this.siteCopyStatus.updateProgress(95);

			// Copying HostVariables
			this.siteCopyStatus.addMessage("copying-host-variables");
			if (copyOptions.isCopyHostVariables()) {
				Logger.info(HostAssetsJobImpl.class, () -> (":::: Copying Site Variables."));
				String variableKey = "";
				try {
					final HostVariableAPI hostVariablesAPI = APILocator.getHostVariableAPI();
					final List<HostVariable> sourceVariables = hostVariablesAPI.getVariablesForHost(sourceSite
							.getIdentifier(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
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
		} finally {
			HibernateUtil.closeSessionSilently();
		}
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

	private String getFullPath(Container sourceContainer) {
		return FileAssetContainerUtil.getInstance().getFullPath(FileAssetContainer.class.cast(sourceContainer));
	}

	/**
	 * This method will take a template and make a copy of it
	 * but it'll also update all the references in that template to point to the new mapped containers passed in the copiedContainersBySourceId
	 * @param sourceTemplate origin template
	 * @param destinationSite target site
	 * @param copiedContainersBySourceId Copy Containers reference map
	 * @return resulting Copy Template
	 * @throws DotDataException
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
	 * Internal method that encloses the copy folder logic re-used across the copy site method
	 * The method will update the Map folderMappingsBySourceId with the new id of the generated folder
	 * If the folder already exist this method will skip copying it and will still return the folder object that was copied
	 * @param sourceFolder Source original site folder
	 * @param destinationSite Target site folder
	 * @param folderMappingsBySourceId Map that gets updated once the folder has been copied
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private Folder copyFolder(final Folder sourceFolder, final Host destinationSite, final Map<String, FolderMapping> folderMappingsBySourceId)
			throws DotDataException, DotSecurityException {
		if (folderMappingsBySourceId.containsKey(sourceFolder.getInode())) {
			Logger.debug(HostAssetsJobImpl.class, () -> String
					.format(":::: folder `%s` has been copied already. ", sourceFolder.getPath()));
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
	 * Copies a content from one site to another. At this point, it's important
	 * to keep track of the elements that have already been copied in order to
	 * avoid duplicating information unnecessarily.
	 * <p>
	 * If the content to copy is a Content Page, then its multi-tree structure
	 * needs to be updated, which involves updating the child references to
	 * point to the recently copied contentlets.
	 * </p>
	 * 
	 * @param sourceContent
	 *            - The {@link Contentlet} whose data will be copied.
	 * @param copyOptions
	 *            - The preferences selected by the user regarding what elements
	 *            of the source site will be copied over to the new site.
	 * @param destinationSite
	 *            - The new {@link Host} object that will contain the
	 *            information from the source site.
	 * @param contentMappingsBySourceId
	 *            - A {@link Map} containing the references between the
	 *            contentlet's Identifier from the source site with the
	 *            contentlet's Identifier from the destination site. This map
	 *            keeps both the source and the destination {@link Contentlet}
	 *            objects.
	 * @param folderMappingsBySourceId
	 *            - A {@link Map} containing the references between the folder's
	 *            Identifier from the source site with the the folder's
	 *            Identifier from the destination site. This map keeps both the
	 *            source and the destination {@link Folder} objects.
	 * @param copiedContainersBySourceId
	 *            - A {@link Map} that says what containerId from the source site has become what in the new copy-site
	 * @param templatesMappingBySourceId
	 *            - A {@link Map} that says what templateId from the source site has become what in the new copy-site
	 * @param contentsToCopyDependencies
	 *            - The dependencies of the contentlet to copy.
	 */
	private Contentlet processCopyOfContentlet(final Contentlet sourceContent,
			final HostCopyOptions copyOptions, final Host destinationSite,
			final Map<String, ContentMapping> contentMappingsBySourceId,
			final Map<String, FolderMapping> folderMappingsBySourceId,
			final Map<String, Container> copiedContainersBySourceId,
			final Map<String, HTMLPageAssetAPI.TemplateContainersReMap> templatesMappingBySourceId,
			final List<Contentlet> contentsToCopyDependencies) {

		//Since certain properties are modified here we're gonna use a defensive copy to avoid cache issue.
		final Contentlet sourceCopy = new Contentlet(sourceContent);
        Contentlet newContent = null;
        try {
            if (contentMappingsBySourceId.containsKey(sourceCopy.getIdentifier())) {
                // The content has already been copied
				Logger.debug(HostAssetsJobImpl.class,()->String.format(":::: Content identified by `%s` has been copied already.", sourceCopy.getIdentifier()));
                return contentMappingsBySourceId.get(sourceCopy.getIdentifier()).destinationContent;
            }
			sourceCopy.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
			sourceCopy.getMap().put(Contentlet.DISABLE_WORKFLOW, true);
			sourceCopy.setLowIndexPriority(true);

			if (copyOptions.isCopyTemplatesAndContainers() && sourceCopy.isHTMLPage()) {
				//If we're dealing with pages, need pass template mappings to the copyContentlet
				//such method deals with all versions of the contentlet and it needs to know about the mapping info internally.
				sourceCopy.getMap().put(Contentlet.TEMPLATE_MAPPINGS, templatesMappingBySourceId);
			}

            if (InodeUtils.isSet(sourceCopy.getFolder())
                    && !sourceCopy.getFolder().equals(this.SYSTEM_FOLDER.getInode())) {
                // The source content has a folder assigned in the
                // source Site we copy it to the same destination
                // folder
                final Folder sourceFolder = this.folderAPI.find(sourceCopy.getFolder(), this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
                final Folder destinationFolder = folderMappingsBySourceId.get(sourceFolder.getInode()) != null ? folderMappingsBySourceId
                        .get(sourceFolder.getInode()).destinationFolder : null;
                if (!copyOptions.isCopyFolders()) {
                    return null;
                }

                if (destinationFolder != null) {
                    // We have already mapped the source folder of
                    // the content to a destination folder because
                    // the user requested to also
                    // copy folders
                    newContent = this.contentAPI.copyContentlet(sourceCopy, destinationFolder, this.SYSTEM_USER,
							DONT_RESPECT_FRONTEND_ROLES);
                } else {
                    // We don't have a destination folder to set the
                    // content to so we are going to set it to the
                    // Site
                    newContent = this.contentAPI.copyContentlet(sourceCopy, destinationSite, this.SYSTEM_USER,
							DONT_RESPECT_FRONTEND_ROLES);
                }
            } else if (InodeUtils.isSet(sourceCopy.getHost())
                    && !sourceCopy.getHost().equals(this.SYSTEM_HOST.getInode())) {
                // The content is assigned to the source Site we
                // assign the new content to the new Site
                newContent = this.contentAPI.copyContentlet(sourceCopy, destinationSite, this.SYSTEM_USER,
						DONT_RESPECT_FRONTEND_ROLES);
            } else {
                // The content has no folder or Site association so
                // we create a global copy as well
                newContent = this.contentAPI.copyContentlet(sourceCopy, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
            }

            if (copyOptions.isCopyContentOnPages() && newContent.isHTMLPage()) {
				Logger.debug(HostAssetsJobImpl.class,()->String.format(":::: Copying contents from page with title `%s` and id `%s`", sourceCopy.getTitle(), sourceCopy.getIdentifier()));
                // Copy page-associated contentlets
                final List<MultiTree> pageContents = APILocator.getMultiTreeAPI().getMultiTrees(sourceCopy.getIdentifier());
				for (final MultiTree sourceMultiTree : pageContents) {
					String newChild = sourceMultiTree.getChild();
					// Update the child reference to point to the previously
					// copied content
					if (contentMappingsBySourceId.containsKey(sourceMultiTree.getChild())) {
						newChild = contentMappingsBySourceId.get(sourceMultiTree.getChild()).destinationContent.getIdentifier();
					}

                    String newContainer = sourceMultiTree.getParent2();
					if(copiedContainersBySourceId.containsKey(sourceMultiTree.getParent2())){
						newContainer = copiedContainersBySourceId.get(sourceMultiTree.getParent2()).getIdentifier();
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

            contentMappingsBySourceId.put(sourceCopy.getIdentifier(), new ContentMapping(sourceCopy, newContent));
			final Contentlet finalNewContent = newContent;
			Logger.debug(HostAssetsJobImpl.class,()->String.format(":::: Re-Mapping content: Identifier `%s` now points to `%s`.", sourceCopy.getIdentifier(), finalNewContent
					.getIdentifier()));

            if (doesRelatedContentExists(sourceCopy)) {
                contentsToCopyDependencies.add(sourceCopy);
            }
        } catch (final Exception e) {
			Logger.error(this, String.format("An error occurred when copying content '%s' from Site '%s' to Site '%s'." +
					" The process will continue...", sourceCopy.getIdentifier(), sourceCopy.getHost(),
					destinationSite.getHostname()), e);
		}
        return newContent;
    }

	/**
	 *
	 * @param contentsToCopyDependencies
	 * @param contentMappingsBySourceId
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
    private void processToCopyContentletDependencies(final List<Contentlet> contentsToCopyDependencies,
            final Map<String, ContentMapping> contentMappingsBySourceId) throws DotDataException, DotSecurityException {
        for (final Contentlet sourceContent : contentsToCopyDependencies) {
            final Contentlet destinationContent = contentMappingsBySourceId.get(sourceContent.getIdentifier()).destinationContent;
            final Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<>();
            final List<Relationship> rels = APILocator.getRelationshipAPI().byContentType(sourceContent.getContentType());
            for (final Relationship r : rels) {
                if (!contentRelationships.containsKey(r)) {
                    contentRelationships.put(r, new ArrayList<>());
                }
                final List<Contentlet> cons = this.contentAPI.getRelatedContent(sourceContent, r, this.SYSTEM_USER, RESPECT_FRONTEND_ROLES);
                List<Contentlet> records = new ArrayList<>();
                for (final Contentlet c : cons) {
                    records = contentRelationships.get(r);
                    if (UtilMethods.isSet(contentMappingsBySourceId.get(c.getIdentifier()))) {
                        records.add(contentMappingsBySourceId.get(c.getIdentifier()).destinationContent);
                    } else {
                        records.add(c);
                    }
                }
                final ContentletRelationshipRecords related = new ContentletRelationships(
                        destinationContent).new ContentletRelationshipRecords(r, true);
                related.setRecords(records);
				this.contentAPI.relateContent(destinationContent, related, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
            }
        }
    }

	/**
	 * Verifies if the specified {@link Contentlet} object has one or more valid related contentlets.
	 *
	 * @param contentlet The Contentlet whose relationships will be verified.
	 *
	 * @return If the Contentlet is the parent of one or more Contentlets, returns {@code true}. Otherwise, returns
	 * {@code false}.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified user does not have the required permissions to perform this action.
	 */
	private boolean doesRelatedContentExists(final Contentlet contentlet) throws DotDataException, DotSecurityException{
        if(contentlet == null) {
			return false;
		}

        final List<Relationship> rels = APILocator.getRelationshipAPI().byContentType(contentlet.getContentType());
        for (final Relationship r : rels) {
            final List<Contentlet> cons = this.contentAPI.getRelatedContent(contentlet, r, this.SYSTEM_USER, DONT_RESPECT_FRONTEND_ROLES);
            if(cons.size() > 0 && APILocator.getRelationshipAPI().isParent(r, contentlet.getContentType())){
            	return true;
            }
        }
        return false;
	}

    @Override
    protected int[] getAllowedVersions() {
        return new int[] { LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level };
    }

    private class ContentMapping {
        @SuppressWarnings("unused")
        Contentlet sourceContent;
        Contentlet destinationContent;

        public ContentMapping(Contentlet sourceContent, Contentlet destinationContent) {
            super();
            this.sourceContent = sourceContent;
            this.destinationContent = destinationContent;
        }
    }

    private class FolderMapping {
        Folder sourceFolder;
        Folder destinationFolder;

        public FolderMapping(Folder sourceFolder, Folder destinationFolder) {
            super();
            this.sourceFolder = sourceFolder;
            this.destinationFolder = destinationFolder;
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
		Logger.info(HostAssetsJobImpl.class, () -> ("Validating Sites Info at DB"));
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
		Logger.info(HostAssetsJobImpl.class, String.format("-> Source Site: %s (%s)", sourceSite.getHostname(),
				sourceSiteId));
		Logger.info(HostAssetsJobImpl.class, String.format("-> Destination Site: %s (%s)", destinationSite
				.getHostname(), destinationSiteId));
	}

}
