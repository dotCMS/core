package com.dotcms.enterprise.priv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.license.LicenseLevel;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


import com.dotcms.enterprise.HostAssetsJobProxy;
import com.dotcms.enterprise.ParentProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariableAPI;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;

import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Arrays;

/**
 * This Quartz job allow dotCMS users to copy sites. The Sites portal also
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
public class HostAssetsJobImpl extends  ParentProxy{

	private HostAPI hostAPI;
	private UserAPI userAPI;
	private FolderAPI folderAPI;
	private TemplateAPI templateAPI;
	private ContainerAPI containerAPI;
	private ContentletAPI contentAPI;
	private MenuLinkAPI menuLinkAPI;
	private HostAssetsJobProxy hp;
	
	
	public HostAssetsJobImpl (HostAssetsJobProxy hp) {
		this.hp = hp;
		hostAPI = APILocator.getHostAPI();
		userAPI = APILocator.getUserAPI();
		folderAPI = APILocator.getFolderAPI();
		templateAPI = APILocator.getTemplateAPI();
		userAPI = APILocator.getUserAPI();
		contentAPI = APILocator.getContentletAPI();
		menuLinkAPI = APILocator.getMenuLinkAPI();
		containerAPI = APILocator.getContainerAPI();
	}
	
	public void run(JobExecutionContext jobContext) throws JobExecutionException {
		if(!allowExecution()){
			return;
		}
		JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
		String sourceHostId = dataMap.getString("sourceHostId");
		String destinationHostId = dataMap.getString("destinationHostId");
		HostCopyOptions copyOptions = (HostCopyOptions) dataMap.get("copyOptions");
		try {
			Host sourceHost = hostAPI.DBSearch(sourceHostId, userAPI.getSystemUser(), false);
			Host destinationHost = hostAPI.DBSearch(destinationHostId, userAPI.getSystemUser(), false);
			if(QuartzUtils.getTaskProgress("setup-host-" + sourceHost.getIdentifier(), "setup-host-group") >= 0 && QuartzUtils.getTaskProgress("setup-host-" + sourceHost.getIdentifier(), "setup-host-group") < 100) {
				Logger.warn(this, "Host " + sourceHost.getHostname() + " - " + sourceHost.getIdentifier() + ", is already being copied, this second request will be ignored.");
				return;
			}
			copyHostAssets(sourceHost, destinationHost, copyOptions);
		} catch (DotDataException e) {
			Logger.error(HostAssetsJobImpl.class, e.getMessage(), e);
			throw new JobExecutionException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(HostAssetsJobImpl.class, e.getMessage(), e);
			throw new JobExecutionException(e.getMessage(), e);
		}

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
		HTMLPageAssetAPI.TemplateContainersReMap templateMapping = new HTMLPageAssetAPI.TemplateContainersReMap(sourceTemplate,
				sourceTemplate, containerMappings);
		return templateMapping;
	}

	/**
	 * Performs the copy process of the user-specified elements from the source
	 * site to the destination site.
	 * 
	 * @param sourceHost
	 *            - The {@link Host} object containing the information that will
	 *            be copied.
	 * @param destinationHost
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
	public void copyHostAssets(Host sourceHost, Host destinationHost, HostCopyOptions copyOptions) throws DotDataException, DotSecurityException {
		
		try {
			
			HibernateUtil.startTransaction();
			
			// Global Vars
			User user = userAPI.getSystemUser();
			boolean respectFrontendRoles = false;
			Host systemHost = hostAPI.findSystemHost(user, false);
			Folder systemFolder = folderAPI.findSystemFolder();
			double progressIncrement = 0;
			double currentProgress = 0;

			hp.addMessage("copying-templates");
			// ======================================================================
			// Copying templates and containers
			// ======================================================================
			Map<String, HTMLPageAssetAPI.TemplateContainersReMap> templatesMappingBySourceId = new HashMap<String, HTMLPageAssetAPI.TemplateContainersReMap>();
			Map<String, Container> copiedContainersBySourceId = new HashMap<String, Container>();
			
			if (copyOptions.isCopyTemplatesAndContainers()) {
				List<Template> sourceTemplates = templateAPI.findTemplatesAssignedTo(sourceHost);
				for (Template sourceTemplate : sourceTemplates) {
					try {

						List<Container> sourceContainers = templateAPI.getContainersInTemplate(sourceTemplate, user,
								respectFrontendRoles);
						List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple>();
						for(Container sourceContainer : sourceContainers) {
							Container destinationContainer = copiedContainersBySourceId.get(sourceContainer.getIdentifier());
							if(destinationContainer == null) {
								Host containerHost = containerAPI.getParentHost(sourceContainer, user, respectFrontendRoles);
								if(containerHost.getIdentifier().equals(sourceHost.getIdentifier())) {
									destinationContainer = containerAPI.copy(sourceContainer, destinationHost, user, respectFrontendRoles);
									copiedContainersBySourceId.put(sourceContainer.getIdentifier(), destinationContainer);
								} else {
									destinationContainer = sourceContainer;
									copiedContainersBySourceId.put(sourceContainer.getIdentifier(), sourceContainer);
								}
							}
							
							HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple containerMapping = new HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple(
									sourceContainer, destinationContainer);
							containerMappings.add(containerMapping);
						}
						Template newTemplate = templateAPI.copy(sourceTemplate, destinationHost, false, containerMappings, user, respectFrontendRoles);
						
						HTMLPageAssetAPI.TemplateContainersReMap templateMapping = new HTMLPageAssetAPI.TemplateContainersReMap(
								sourceTemplate, newTemplate, containerMappings);
						templatesMappingBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
					} catch (Exception e) {
						Logger.error(this,
								"Error ocurred on copy of templates and containers, the process will continue", e);
					}

				}
				List<Container> containersUnderSourceHost = containerAPI.findContainersUnder(sourceHost);
				for(Container cntr : containersUnderSourceHost){
					if(copiedContainersBySourceId.get(cntr.getIdentifier()) == null){
						Container destinationContainer = containerAPI.copy(cntr, destinationHost, user, respectFrontendRoles);
						copiedContainersBySourceId.put(cntr.getIdentifier(), destinationContainer);
					}
				}
			} else {
				List<Template> sourceTemplates = templateAPI.findTemplatesAssignedTo(sourceHost);
				for (Template sourceTemplate : sourceTemplates) {
					try {
						List<Container> sourceContainers = templateAPI.getContainersInTemplate(sourceTemplate, user,
								respectFrontendRoles);
						List<Container> destinationContainers = sourceContainers;
						List<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple> containerMappings = new LinkedList<HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple>();
						for (int i = 0; i < sourceContainers.size(); i++) {
							Container sourceContainer = sourceContainers.get(i);
							Container destinationContainer = destinationContainers.get(i);
							HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple containerMapping = new HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple(
									sourceContainer, destinationContainer);
							containerMappings.add(containerMapping);
						}
						HTMLPageAssetAPI.TemplateContainersReMap templateMapping = new HTMLPageAssetAPI.TemplateContainersReMap(
								sourceTemplate, sourceTemplate, containerMappings);
						templatesMappingBySourceId.put(sourceTemplate.getIdentifier(), templateMapping);
					} catch (Exception e) {
						Logger.error(this,
								"Error ocurred on copy of templates and containers, the process will continue", e);
					}
				}
			}
			HibernateUtil.closeAndCommitTransaction();
			HibernateUtil.startTransaction();
			hp.updateProgress(5);

			// ======================================================================
			// Copying folders
			// ======================================================================
			Map<String, FolderMapping> folderMappingsBySourceId = new HashMap<String, FolderMapping>();
			Map<String, ContentMapping> contentMappingsBySourceId = new HashMap<String, ContentMapping>();
			List<Contentlet> contentsToCopyDependencies =  new ArrayList<Contentlet>();

			if (copyOptions.isCopyFolders()) {

			    // ======================================================================
				// copying folders
			    // ======================================================================
				hp.addMessage("copying-folders");
				List<Folder> allSourceFolders = folderAPI.findSubFoldersRecursively(sourceHost,user,false);
				for (Folder sourceFolder : allSourceFolders) {
					try {
						Folder newFolder = folderAPI.createFolders(APILocator.getIdentifierAPI().find(sourceFolder).getPath(), destinationHost,user,false);
						newFolder.setTitle(sourceFolder.getTitle());
						if(sourceFolder.isShowOnMenu()) {
							newFolder.setSortOrder(sourceFolder.getSortOrder());
							newFolder.setShowOnMenu(true);
						}
						folderAPI.save(newFolder,user,false);
						folderMappingsBySourceId.put(sourceFolder.getInode(),
								new FolderMapping(sourceFolder, newFolder));
					} catch (Exception e) {
						Logger.error(this, "Error ocurred on copy of folder, the process will continue", e);
					}
				}
				HibernateUtil.closeAndCommitTransaction();
				HibernateUtil.startTransaction();
				hp.updateProgress(10);

				// ======================================================================
				// Copying legacy pages
				// ======================================================================
				hp.addMessage("copying-pages-and-content-on-pages");
				if (copyOptions.isCopyLinks()) {
					Collection<FolderMapping> folders = folderMappingsBySourceId.values();
					// ======================================================================
					// Copying Menu Links
					// ======================================================================
					hp.addMessage("copying-menu-links");
					for (FolderMapping folderMapping : folders) {
						List<Link> sourceLinks = menuLinkAPI.findFolderMenuLinks(folderMapping.sourceFolder);
						for (Link sourceLink : sourceLinks) {
							try {
								Logger.debug(this, "Coping menu with inode : " + sourceLink.getInode());
								menuLinkAPI.copy(sourceLink, folderMapping.destinationFolder, user, respectFrontendRoles);
							} catch (Exception e) {
								Logger.error(this, "Error ocurred on copy of menu links, the process will continue", e);
							}
						}
					}
				}

				// Point templates to copied themes (if themes belong to the copied site)
				if(copyOptions.isCopyTemplatesAndContainers()){
					for(String sourceTemplateId : templatesMappingBySourceId.keySet()){
						Template srcTemplate = templatesMappingBySourceId.get(sourceTemplateId).getSourceTemplate();						
						if(UtilMethods.isSet(srcTemplate.getTheme()) && folderMappingsBySourceId.containsKey(srcTemplate.getTheme())){
							String destTemplateInode = templatesMappingBySourceId.get(sourceTemplateId).getDestinationTemplate().getInode();
							String destTheme = folderMappingsBySourceId.get(srcTemplate.getTheme()).destinationFolder.getInode();
							templateAPI.updateThemeWithoutVersioning(destTemplateInode, destTheme);
						}
					}
				}
				
			}
			HibernateUtil.closeAndCommitTransaction();
			HibernateUtil.startTransaction();
			hp.updateProgress(70);
			
			// ======================================================================
			// Copying content on site
			// ======================================================================
			hp.addMessage("copying-content-on-host");
			int contentCount = 0;

            // Option 1: Copy ONLY content pages WITHOUT other contents, if 
			// copyOptions.isCopyContentOnHost() == true should be handle by option 2.
            if (!copyOptions.isCopyContentOnHost() && copyOptions.isCopyLinks()) {
                List<Contentlet> sourceContentlets = contentAPI.findContentletsByHost(sourceHost,
                        Arrays.asList(Structure.STRUCTURE_TYPE_HTMLPAGE), null, user,
                        respectFrontendRoles);
                currentProgress = 70;
                progressIncrement = (95 - 70) / (double) sourceContentlets.size();

                for (Contentlet sourceContent : sourceContentlets) {
                    processCopyOfContentlet(sourceContent, copyOptions, systemHost, systemFolder,
                            destinationHost, contentMappingsBySourceId, folderMappingsBySourceId,
                            contentsToCopyDependencies, user, respectFrontendRoles);

                    currentProgress += progressIncrement;

                    hp.updateProgress((int) currentProgress);
                    if (contentCount % 100 == 0) {
                        HibernateUtil.closeAndCommitTransaction();
                        HibernateUtil.startTransaction();
                    }
                    contentCount++;
                }

                // Copy contentlet dependencies
                processToCopyContentletDependencies(contentsToCopyDependencies,
                        contentMappingsBySourceId, user, respectFrontendRoles);
            }

            // Option 2: Copy all content on site
            if (copyOptions.isCopyContentOnHost()) {
                List<Contentlet> sourceContentlets = contentAPI.findContentletsByHost(sourceHost,
                        user, respectFrontendRoles);
                currentProgress = 70;
                progressIncrement = (95 - 70) / (double) sourceContentlets.size();

                // Process simple Contents first. This makes it easier to later 
                // associate page contents when updating the multi-tree
                Iterator<Contentlet> ite = sourceContentlets.iterator();
                while (ite.hasNext()) {
                	Contentlet sourceContent = ite.next();
                	if (!sourceContent.isHTMLPage()) {
	                    processCopyOfContentlet(sourceContent, copyOptions, systemHost, systemFolder,
	                            destinationHost, contentMappingsBySourceId, folderMappingsBySourceId,
	                            contentsToCopyDependencies, user, respectFrontendRoles);
	                    // Update progress ONLY if the record is processed
	                    currentProgress += progressIncrement;
	                    hp.updateProgress((int) currentProgress);
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
                	Contentlet sourceContent = ite.next();
                    processCopyOfContentlet(sourceContent, copyOptions, systemHost, systemFolder,
                            destinationHost, contentMappingsBySourceId, folderMappingsBySourceId,
                            contentsToCopyDependencies, user, respectFrontendRoles);
                    currentProgress += progressIncrement;
                    hp.updateProgress((int) currentProgress);
                    if (contentCount % 100 == 0) {
                        HibernateUtil.closeAndCommitTransaction();
                        HibernateUtil.startTransaction();
                    }
                    contentCount++;
                }

                // Copy contentlet dependencies
                processToCopyContentletDependencies(contentsToCopyDependencies,
                        contentMappingsBySourceId, user, respectFrontendRoles);
            }

			hp.updateProgress(95);

			// Copying HostVariables
			hp.addMessage("copying-host-variables");
			if (copyOptions.isCopyHostVariables()) {
				try {
					HostVariableAPI hostVariablesAPI = APILocator.getHostVariableAPI();
					List<HostVariable> sourceVariables = hostVariablesAPI.getVariablesForHost(sourceHost
							.getIdentifier(), user, respectFrontendRoles);
					for (HostVariable variable : sourceVariables) {
						hostVariablesAPI.copy(variable, destinationHost, user, respectFrontendRoles);
					}
				} catch (Exception e) {
					Logger.error(this, "Error ocurred on copy of host variables, the process will continue", e);
				}
			}
			hp.updateProgress(100);

			HibernateUtil.closeAndCommitTransaction();
		} catch (Exception e) {
			Logger.error(this, "A general error as ocurred on the copy host process, the whole process will stop", e);
			HibernateUtil.rollbackTransaction();
		} finally {
			HibernateUtil.closeSessionSilently();
		}
		
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
	 * @param systemHost
	 *            - The main system {@link Host}.
	 * @param systemFolder
	 *            - The main system {@link Folder}.
	 * @param destinationHost
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
	 * @param contentsToCopyDependencies
	 *            - The dependencies of the contentlet to copy.
	 * @param user
	 *            - The user performing this action
	 * @param respectFrontendRoles
	 *            -
	 */
    private void processCopyOfContentlet(Contentlet sourceContent, HostCopyOptions copyOptions, Host systemHost,
            Folder systemFolder, Host destinationHost,
            Map<String, ContentMapping> contentMappingsBySourceId, Map<String, FolderMapping> folderMappingsBySourceId,
            List<Contentlet> contentsToCopyDependencies, User user, boolean respectFrontendRoles) {

        Contentlet newContent;

        try {
            if (contentMappingsBySourceId.containsKey(sourceContent.getIdentifier())) {
                // The content has already been copied
                return;
            }

            sourceContent.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
            sourceContent.getMap().put(Contentlet.DISABLE_WORKFLOW, true);
            sourceContent.setLowIndexPriority(true);
            if (InodeUtils.isSet(sourceContent.getFolder())
                    && !sourceContent.getFolder().equals(systemFolder.getInode())) {
                // The source content has a folder assigned in the
                // source host we copy it to the same destination
                // folder
                Folder sourceFolder = folderAPI.find(sourceContent.getFolder(), user, false);
                Folder destinationFolder = folderMappingsBySourceId.get(sourceFolder.getInode()) != null ? folderMappingsBySourceId
                        .get(sourceFolder.getInode()).destinationFolder : null;

                if (!copyOptions.isCopyFolders() && sourceFolder != null) {
                    return;
                }

                if (destinationFolder != null) {
                    // We have already mapped the source folder of
                    // the content to a destination folder because
                    // the user requested to also
                    // copy folders
                    newContent = contentAPI.copyContentlet(sourceContent, destinationFolder, user,
                            respectFrontendRoles);
                } else {
                    // We don't have a destination folder to set the
                    // content to so we are going to set it to the
                    // host
                    newContent = contentAPI.copyContentlet(sourceContent, destinationHost, user,
                            respectFrontendRoles);
                }
            } else if (InodeUtils.isSet(sourceContent.getHost())
                    && !sourceContent.getHost().equals(systemHost.getInode())) {
                // The content is assigned to the source host we
                // assign the new content to the new host
                newContent = contentAPI.copyContentlet(sourceContent, destinationHost, user,
                        respectFrontendRoles);
            } else {
                // The content has no folder or host association so
                // we create a global copy as well
                newContent = contentAPI.copyContentlet(sourceContent, user, respectFrontendRoles);
            }

            if (copyOptions.isCopyContentOnPages() && newContent.isHTMLPage()) {
                // Copy page-associated contentlets
                List<MultiTree> pageContents = APILocator.getMultiTreeAPI().getMultiTrees(sourceContent
                        .getIdentifier());
				for (MultiTree m : pageContents) {
					String newChild = m.getChild();
					// Update the child reference to point to the previously 
					// copied content
					if (contentMappingsBySourceId.containsKey(m.getChild())) {
						newChild = contentMappingsBySourceId.get(m.getChild()).destinationContent.getIdentifier();
					}
					MultiTree mt = new MultiTree(newContent.getIdentifier(), m.getParent2(), newChild);
					mt.setTreeOrder(m.getTreeOrder());
					APILocator.getMultiTreeAPI().saveMultiTree(mt);
				}
            }

            contentMappingsBySourceId.put(sourceContent.getIdentifier(), new ContentMapping(
                    sourceContent, newContent));
            if (doesRelatedContentExists(sourceContent))
                contentsToCopyDependencies.add(sourceContent);
        } catch (Exception e) {
            Logger.error(this,
                    "Error ocurred on copy of content on host, the process will continue", e);
        }
    }

    private void processToCopyContentletDependencies(List<Contentlet> contentsToCopyDependencies,
            Map<String, ContentMapping> contentMappingsBySourceId, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        for (Contentlet sourceContent : contentsToCopyDependencies) {
            Contentlet destinationContent = contentMappingsBySourceId.get(sourceContent
                    .getIdentifier()).destinationContent;
            Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
            List<Relationship> rels = FactoryLocator.getRelationshipFactory()
                    .byContentType(sourceContent.getStructure());
            for (Relationship r : rels) {
                if (!contentRelationships.containsKey(r)) {
                    contentRelationships.put(r, new ArrayList<Contentlet>());
                }
                List<Contentlet> cons = contentAPI.getRelatedContent(sourceContent, r, APILocator.getUserAPI().getSystemUser(), true);
                List<Contentlet> records = new ArrayList<Contentlet>();
                for (Contentlet c : cons) {
                    records = contentRelationships.get(r);
                    if (UtilMethods.isSet(contentMappingsBySourceId.get(c.getIdentifier()))) {
                        records.add(contentMappingsBySourceId.get(c.getIdentifier()).destinationContent);
                    } else {
                        records.add(c);
                    }
                }
                ContentletRelationshipRecords related = new ContentletRelationships(
                        destinationContent).new ContentletRelationshipRecords(r, true);
                related.setRecords(records);
                contentAPI.relateContent(destinationContent, related, user, respectFrontendRoles);
            }
        }
    }
	
	private boolean doesRelatedContentExists(Contentlet contentlet) throws DotDataException, DotSecurityException{
		
        if(contentlet == null)
            return false;

        List<Relationship> rels = FactoryLocator.getRelationshipFactory().byContentType(contentlet.getStructure());
        for (Relationship r : rels) {
            List<Contentlet> cons = contentAPI.getRelatedContent(contentlet, r, APILocator.getUserAPI().getSystemUser(), false);            
            if(cons.size() > 0 && FactoryLocator.getRelationshipFactory().isParent(r, contentlet.getStructure())){
            	return true;
            }
        }
        return false;
	}

	private String findDestinationContainerFromMapping(String containerId, TemplateContainersReMap tempContReMapping) {
		List<ContainerRemapTuple> containerMapping = tempContReMapping.getContainersRemap();
		for(ContainerRemapTuple tuple: containerMapping) {
			if(tuple.getSourceContainer().getIdentifier().equals(containerId))
				return tuple.getDestinationContainer().getIdentifier();
		}
		return null;
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
}
