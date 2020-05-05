package com.dotcms.publisher.util;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.AssetTypes;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

/**
 * The main purpose of this class is to determine all possible content
 * dependencies associated to the asset(s) the user wants to push via the Push
 * Publish feature. Including the dependencies makes sure that pushing an asset
 * will not fail when saved in the destination server(s).
 * <p>
 * Most of the assets in dotCMS <b>MUST</b> be associated to another parent
 * structure. For example, pushing a Content Page requires the bundle to include
 * information regarding the Site (host) in was created in, its folder,
 * template, containers, contentlets, and so on. Whereas pushing a Language only
 * needs the language information and the push will work correctly.
 * <p>
 * The Dependency Manager analyzes the type of each asset to push and includes
 * dependent information in the bundle. This way, it can be seen by users
 * exactly the same in both the sender and receiver servers.
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since Jan 11, 2013
 *
 */
public class DependencyManager {

	private DependencySet hosts;
	private DependencySet folders;
	private DependencySet htmlPages;
	private DependencySet templates;
	private DependencySet contentTypes;
	private DependencySet containers;
	private DependencySet contents;
	private DependencySet links;
	private DependencySet relationships;
	private DependencySet workflows;
	private DependencySet languages;
	private DependencySet rules;
	private DependencySet categories;

	private Set<String> hostsSet;
	private Set<String> foldersSet;
	private Set<String> htmlPagesSet;
	private Set<String> templatesSet;
	private Set<String> contentTypesSet;
	private Set<String> containersSet;
	private Set<String> fileAssetContainersSet;
	private Set<String> contentsSet;
	private Set<String> linksSet;
	private Set<String> ruleSet;
	private Set<String> solvedContentTypes;

	private User user;

	private PushPublisherConfig config;

	/**
	 * Initializes the list of dependencies that this manager needs to satisfy,
	 * based on the {@link PushPublisherConfig} specified for the bundle.
	 *
	 * @param user
	 *            - The user requesting the generation of the bundle.
	 * @param config
	 *            - The main configuration values for the bundle we are trying
	 *            to create.
	 */
	public DependencyManager(User user, PushPublisherConfig config) {
		this.config = config;
		// these ones store the assets that will be sent in the bundle
		boolean isPublish=config.getOperation().equals(Operation.PUBLISH);
		hosts = new DependencySet(config.getId(), "host", config.isDownloading(), isPublish, config.isStatic());
		folders = new DependencySet(config.getId(), "folder", config.isDownloading(), isPublish, config.isStatic());
		htmlPages = new DependencySet(config.getId(), "htmlpage", config.isDownloading(), isPublish, config.isStatic());
		templates = new DependencySet(config.getId(), "template", config.isDownloading(), isPublish, config.isStatic());
		contentTypes = new DependencySet(config.getId(), "contenttype", config.isDownloading(), isPublish, config.isStatic());
		containers = new DependencySet(config.getId(), "container", config.isDownloading(), isPublish, config.isStatic());
		contents = new DependencySet(config.getId(), "content", config.isDownloading(), isPublish, config.isStatic());
		relationships = new DependencySet(config.getId(), "relationship", config.isDownloading(), isPublish, config.isStatic());
		links = new DependencySet(config.getId(),"links",config.isDownloading(), isPublish, config.isStatic());
		workflows = new DependencySet(config.getId(),"workflows",config.isDownloading(), isPublish, config.isStatic());
		languages = new DependencySet(config.getId(),"languages",config.isDownloading(), isPublish, config.isStatic());
		this.rules = new DependencySet(config.getId(), PushPublisherConfig.AssetTypes.RULES.toString(), config.isDownloading(), isPublish, config.isStatic());
		categories = new DependencySet(config.getId(), AssetTypes.CATEGORIES.toString(), config.isDownloading(), isPublish, config.isStatic());

		// these ones are for being iterated over to solve the asset's dependencies
		hostsSet = new HashSet<>();
		foldersSet = new HashSet<>();
		htmlPagesSet = new HashSet<>();
		templatesSet = new HashSet<>();
		contentTypesSet = new HashSet<>();
		containersSet = new HashSet<>();
		contentsSet = new HashSet<>();
		fileAssetContainersSet = new HashSet<>();
		linksSet = new HashSet<>();
		this.ruleSet = new HashSet<>();
		solvedContentTypes = new HashSet<>();

		this.user = user;

	}

	/**
	 * Initializes the main mechanism to search for dependencies. It starts with
	 * the identification of the type of assets in the queue, and their
	 * dependencies will be searched and found base on those types.
	 * 
	 * @throws DotSecurityException
	 *             The specified user does not have the required permissions to
	 *             perform the action.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 * @throws DotBundleException
	 *             An error occurred when generating the bundle data.
	 */
	public void setDependencies() throws DotSecurityException, DotDataException, DotBundleException {
		List<PublishQueueElement> assets = config.getAssets();


		final PublisherFilter publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());
		Logger.info(this,publisherFilter.toString());
		for (PublishQueueElement asset : assets) {
			//Check if the asset.Type is in the excludeClasses filter, if it is the asset is not added to the bundle
			if(!publisherFilter.acceptExcludeClasses(asset.getType())){
				continue;
			}

			if(asset.getType().equals(PusheableAsset.CONTENT_TYPE.getType())) {
				try {
					Structure st = CacheLocator.getContentTypeCache().getStructureByInode(asset.getAsset());

					if(st == null) {
						Logger.warn(getClass(), "Structure id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						contentTypes.add(asset.getAsset(), st.getModDate());
						contentTypesSet.add(asset.getAsset());
					}

				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the Structure to the Bundle. Bundle ID: " + config.getId() + ", Structure ID: " + asset.getAsset(), e);
				}

			} else if(asset.getType().equals(PusheableAsset.TEMPLATE.getType())) {
				try {
					Template t = APILocator.getTemplateAPI().findLiveTemplate(asset.getAsset(), user, false);

					if(t == null || !UtilMethods.isSet(t.getIdentifier())) {
						t = APILocator.getTemplateAPI().findWorkingTemplate(asset.getAsset(), user, false);
					}
					if(t == null || !UtilMethods.isSet(t.getIdentifier())) {
						Logger.warn(getClass(), "Template id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						templates.add(asset.getAsset(), t.getModDate());
						templatesSet.add(asset.getAsset());
					}

				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the Template to the Bundle. Bundle ID: " + config.getId() + ", Template ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.CONTAINER.getType())) {
				try {
					Container c = (Container) APILocator.getVersionableAPI().findLiveVersion(asset.getAsset(), user, false);

					if(c == null) {
						c = APILocator.getContainerAPI().getWorkingContainerById(asset.getAsset(), user, false);
					}

                    if(c instanceof FileAssetContainer){
					   Logger.debug(getClass(), "FileAssetContainer id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" will be ignored");
					   fileAssetContainersSet.add(asset.getAsset());
                       continue;
                    }

					if(c == null) {
						Logger.warn(getClass(), "Container id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						containers.add(asset.getAsset(), c.getModDate());
						containersSet.add(asset.getAsset());
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Container to the Bundle. Bundle ID: " + config.getId() + ", Container ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.FOLDER.getType())) {
				try {
					Folder f = APILocator.getFolderAPI().find(asset.getAsset(), user, false);

					if(f == null){
						Logger.warn(getClass(), "Folder id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						folders.add(asset.getAsset(), f.getModDate());
						foldersSet.add(asset.getAsset());
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Folder to the Bundle. Bundle ID: " + config.getId() + ", Folder ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.SITE.getType())) {
				try {
					Host h = APILocator.getHostAPI().find(asset.getAsset(), user, false);

					if(h == null){
						Logger.warn(getClass(), "Host id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						hosts.add(asset.getAsset(), h.getModDate());
						hostsSet.add(asset.getAsset());
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Host to the Bundle. Bundle ID: " + config.getId() + ", Host ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.LINK.getType())) {
				try {
					Link link = (Link) APILocator.getVersionableAPI().findLiveVersion(asset.getAsset(), user, false);

					if(link == null || !InodeUtils.isSet(link.getInode())) {
						link = APILocator.getMenuLinkAPI().findWorkingLinkById(asset.getAsset(), user, false);
					}

					if(link == null || !InodeUtils.isSet(link.getInode())) {
						Logger.warn(getClass(), "Link id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						links.add(asset.getAsset(),link.getModDate());
						linksSet.add(asset.getAsset());
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Host to the Bundle. Bundle ID: " + config.getId() + ", Host ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.WORKFLOW.getType())) {
				WorkflowScheme scheme = APILocator.getWorkflowAPI().findScheme(asset.getAsset());

				if(scheme == null){
					Logger.warn(getClass(), "WorkflowScheme id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
				} else {
					workflows.add(asset.getAsset(),scheme.getModDate());
				}
			} else if (asset.getType().equals(PusheableAsset.LANGUAGE.getType())) {
				Language language = APILocator.getLanguageAPI()
						.getLanguage(asset.getAsset());
				if (language == null || !UtilMethods.isSet(language.getLanguage())) {
					Logger.warn(getClass(), "Language id: "
							+ (asset.getAsset() != null ? asset.getAsset()
									: "N/A")
							+ " is not present in the database, not Pushed");
				} else {
					languages.add(asset.getAsset());
				}
			} else if (asset.getType().equals(PusheableAsset.RULE.getType())) {
				Rule rule = APILocator.getRulesAPI()
						.getRuleById(asset.getAsset(), user, false);
				if (rule != null && StringUtils.isNotBlank(rule.getId())) {
					this.rules.add(rule.getId());
					this.ruleSet.add(rule.getId());
				} else {
					Logger.warn(getClass(), "Rule id: "
							+ (asset.getAsset() != null ? asset.getAsset()
									: "N/A")
							+ " is not present in the database, not Pushed.");
				}
			}
		}

		if(UtilMethods.isSet(config.getLuceneQueries())){
			List<String> contentIds = PublisherUtil.getContentIds( config.getLuceneQueries());
			for(String id : contentIds){
				List<Contentlet> contentlets = APILocator.getContentletAPI().search("+identifier:"+id, 0, 0, "moddate", user, false);
				for(Contentlet con : contentlets){
					if(publisherFilter.acceptExcludeQuery(con.getIdentifier())) {
						contents.add(con.getIdentifier(), con.getModDate());
						contentsSet.add(con.getIdentifier());
					}
				}
			}
		}

		Logger.info(this,"ContentTypes: " + this.contentTypes.size());

		if(publisherFilter.isDependencies()){
			setHostDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setFolderDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setHTMLPagesDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setTemplateDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setContainerDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setStructureDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setLinkDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setLanguageDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setContentDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
			setRuleDependencies(publisherFilter);
			Logger.info(this,"ContentTypes: " + this.contentTypes.size());
		}

		config.setHostSet(hosts);
		config.setFolders(folders);
		config.setHTMLPages(htmlPages);
		config.setTemplates(templates);
		config.setContainers(containers);
		config.setStructures(contentTypes);
		config.setContents(contents);
		config.setLinks(links);
		config.setRelationships(relationships);
		config.setWorkflows(workflows);
		config.setLanguages(languages);
		config.setRules(this.rules);
		config.setCategories(categories);
	}

	/**
	 * For given Links adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Folders</li>
	 * </ul>
	 */
	private void setLinkDependencies(final PublisherFilter publisherFilter)  {
		for(String linkId : linksSet) {
			try {
				Identifier ident=APILocator.getIdentifierAPI().find(linkId);

				// Folder Dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
					Folder ff = APILocator.getFolderAPI()
							.findFolderByPath(ident.getParentPath(), ident.getHostId(), user,
									false);
					folders.addOrClean(ff.getInode(), ff.getModDate());
					foldersSet.add(ff.getInode());
				}

				// Host Dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
					Host hh = APILocator.getHostAPI().find(ident.getHostId(), user, false);
					hosts.addOrClean(hh.getIdentifier(), hh.getModDate());
					hostsSet.add(hh.getIdentifier());
				}

				// Content Dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType())) {
					Link link = APILocator.getMenuLinkAPI()
							.findWorkingLinkById(linkId, user, false);

					if (link != null) {

						if (link.getLinkType().equals(Link.LinkType.INTERNAL.toString())) {
							Identifier id = APILocator.getIdentifierAPI()
									.find(link.getInternalLinkIdentifier());

							// add file/content dependencies. will also work with htmlpages as content
							if (InodeUtils.isSet(id.getInode()) && id.getAssetType()
									.equals("contentlet")) {
								List<Contentlet> contentList = APILocator.getContentletAPI()
										.search("+identifier:" + id.getId(), 0, 0, "moddate", user,
												false);

								for (Contentlet contentlet : contentList) {
									if(publisherFilter.acceptExcludeDependencyQuery(contentlet.getIdentifier())) {
										contents.addOrClean(contentlet.getIdentifier(),
												contentlet.getModDate());
										contentsSet.add(contentlet.getIdentifier());
									}
								}


							}
						}
					}
				}

			} catch (Exception e) {
				Logger.error(this, "can't load menuLink deps "+linkId,e);
			}
		}
	}

	/**
	 * Collects the different dependent objects that are required for pushing
	 * {@link Host} objects. The required dependencies of a site are:
	 * <ul>
	 * <li>Templates.</li>
	 * <li>Containers.</li>
	 * <li>Contentlets.</li>
	 * <li>Content Types.</li>
	 * <li>Folders.</li>
	 * <li>Rules.</li>
	 * </ul>
	 */
	private void setHostDependencies (final PublisherFilter publisherFilter) {
		try {
			for (String id : hostsSet) {
				final Host h = APILocator.getHostAPI().find(id, user, false);

				// Template dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.TEMPLATE.getType())) {
					final List<Template> templateList = APILocator.getTemplateAPI()
							.findTemplatesAssignedTo(h);
					for (Template template : templateList) {
						templates.addOrClean(template.getIdentifier(), template.getModDate());
						templatesSet.add(template.getIdentifier());
					}
				}

				// Container dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTAINER.getType())) {
					final List<Container> containerList = APILocator.getContainerAPI()
							.findContainersUnder(h);
					for (Container container : containerList) {

						if (container instanceof FileAssetContainer) {
							fileAssetContainersSet.add(container.getIdentifier());
						} else {
							containers
									.addOrClean(container.getIdentifier(), container.getModDate());
							containersSet.add(container.getIdentifier());
						}
					}
				}

				// Content dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType())) {
					String luceneQuery = "+conHost:" + h.getIdentifier();
					final List<Contentlet> contentList = APILocator.getContentletAPI()
							.search(luceneQuery, 0, 0, null, user, false);
					for (Contentlet contentlet : contentList) {
						if(publisherFilter.acceptExcludeDependencyQuery(contentlet.getIdentifier())) {
							contents.addOrClean(contentlet.getIdentifier(),
									contentlet.getModDate());
							contentsSet.add(contentlet.getIdentifier());
						}
					}
				}

				// Structure dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType())) {
					final List<Structure> structuresList = StructureFactory
							.getStructuresUnderHost(h, user, false);
					for (Structure structure : structuresList) {
						contentTypes.addOrClean(structure.getInode(), structure.getModDate());
						contentTypesSet.add(structure.getInode());
					}
				}

				// Folder dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
					final List<Folder> folderList = APILocator.getFolderAPI()
							.findFoldersByHost(h, user, false);
					for (Folder folder : folderList) {
						folders.addOrClean(folder.getInode(), folder.getModDate());
						foldersSet.add(folder.getInode());
					}
				}

				// Rule dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.RULE.getType())) {
					final List<Rule> ruleList = APILocator.getRulesAPI()
							.getAllRulesByParent(h, user, false);
					for (final Rule rule : ruleList) {
						this.rules.add(rule.getId());
						this.ruleSet.add(rule.getId());
					}
				}
			}
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 * For given Folders adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Contentlets</li>
	 * <li>Links</li>
	 * <li>Structures</li>
	 * <li>HTMLPages</li>
	 * </ul>
	 */
	private void setFolderDependencies(final PublisherFilter publisherFilter) {
		try {
			List<Folder> folderList = new ArrayList<Folder>();

			HashSet<String> parentFolders = new HashSet<String>();

			for (String id : foldersSet) {
				Folder f = APILocator.getFolderAPI().find(id, user, false);
				// Parent folder
				Folder parent = APILocator.getFolderAPI().findParentFolder(f, user, false);
				if(UtilMethods.isSet(parent)) {
					folders.addOrClean( parent.getInode(), parent.getModDate());
					parentFolders.add(parent.getInode());
				}

				folderList.add(f);
			}
			foldersSet.addAll(parentFolders);
			setFolderListDependencies(folderList, publisherFilter);
		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 * 
	 * @param folderList
	 * @throws DotIdentifierStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void setFolderListDependencies(final List<Folder> folderList, final PublisherFilter publisherFilter) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		for (Folder f : folderList) {

			// Add folder even if empty
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
				folders.addOrClean(f.getInode(), f.getModDate());
				foldersSet.add(f.getInode());
			}

			// Host dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
				Host h = APILocator.getHostAPI().find(f.getHostId(), user, false);
				hosts.addOrClean(f.getHostId(), h.getModDate());
				hostsSet.add(f.getHostId());
			}

			// Content dependencies
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType())) {
				String luceneQuery = "+conFolder:" + f.getInode();
				List<Contentlet> contentList = APILocator.getContentletAPI()
						.search(luceneQuery, 0, 0, null, user, false);
				for (Contentlet contentlet : contentList) {
					if(publisherFilter.acceptExcludeDependencyQuery(contentlet.getIdentifier())) {
						contents.addOrClean(contentlet.getIdentifier(), contentlet.getModDate());
						contentsSet.add(contentlet.getIdentifier());
					}
				}
			}

			// Menu Link dependencies
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.LINK.getType())) {
				List<Link> linkList = APILocator.getMenuLinkAPI().findFolderMenuLinks(f);
				for (Link link : linkList) {
					links.addOrClean(link.getIdentifier(), link.getModDate());
					linksSet.add(link.getIdentifier());
				}
			}

			// Structure dependencies
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType())) {
				List<Structure> structureList = APILocator.getFolderAPI()
						.getStructures(f, user, false);
				for (Structure structure : structureList) {
					contentTypes.addOrClean(structure.getInode(), structure.getModDate());
					contentTypesSet.add(structure.getInode());
				}

				//Add the default structure of this folder
				if (f.getDefaultFileType() != null) {
					Structure defaultStructure = CacheLocator.getContentTypeCache()
							.getStructureByInode(f.getDefaultFileType());
					if ((defaultStructure != null && InodeUtils.isSet(defaultStructure.getInode()))
							&& !contentTypesSet.contains(defaultStructure.getInode())) {
						contentTypes.addOrClean(defaultStructure.getInode(),
								defaultStructure.getModDate());
						contentTypesSet.add(defaultStructure.getInode());
					}
				}
			}

			setFolderListDependencies(APILocator.getFolderAPI().findSubFolders(f, user, false),publisherFilter);
		}

	}

	/**
	 * Traverses the list of selected contentlets that are being pushed and
	 * determines which of them are actually {@link IHTMLPage} objects. After
	 * that, the respective dependencies will be retrieved and added acordingly.
	 */
	private void setHTMLPagesDependencies(final PublisherFilter publisherFilter)  {
		try {

			Set<String> idsToWork = new HashSet<>();
			idsToWork.addAll(htmlPagesSet);
			for (String contId : contentsSet) {

				List<Contentlet> c = APILocator.getContentletAPI().search("+identifier:" + contId, 0, 0, "moddate", user, false);

				if (c != null && !c.isEmpty() && c.get(0).getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
					idsToWork.add(contId);
				}
			}

			//Process the pages we found
			setHTMLPagesDependencies(idsToWork,publisherFilter);

		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	/**
	 * Collects the different dependent objects that are required for pushing
	 * {@link IHTMLPage} objects. The required dependencies of a page are:
	 * <ul>
	 * <li>Host.</li>
	 * <li>Template.</li>
	 * <li>Containers.</li>
	 * <li>Content Types.</li>
	 * <li>Contentlets.</li>
	 * <li>Rules.</li>
	 * </ul>
	 * 
	 * @param idsToWork
	 */
	private void setHTMLPagesDependencies(final Set<String> idsToWork,final PublisherFilter publisherFilter) {

		try {

			IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
			FolderAPI folderAPI = APILocator.getFolderAPI();
			Set<Container> containerList = new HashSet<>();

			for (String pageId : idsToWork) {
				Identifier iden = idenAPI.find(pageId);

				// Host dependency
				if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
					Host h = APILocator.getHostAPI().find(iden.getHostId(), user, false);
					hosts.addOrClean(iden.getHostId(), h.getModDate());
					hostsSet.add(iden.getHostId());
				}

				// Folder dependencies
				if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
					Folder folder = folderAPI
							.findFolderByPath(iden.getParentPath(), iden.getHostId(), user, false);
					folders.addOrClean(folder.getInode(), folder.getModDate());
					foldersSet.add(folder.getInode());
				}

				// looking for working version (must exists)
				IHTMLPage workingPage = null;

				Contentlet contentlet = null;
				try {
					contentlet = APILocator.getContentletAPI()
							.search("+identifier:" + pageId + " +working:true", 0, 0, "moddate",
									user, false).get(0);
				} catch (DotContentletStateException e) {
					// content not found message is already displayed on console
					Logger.debug(this, e.getMessage(), e);
				}
				if (contentlet != null)
					workingPage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

				// looking for live version (might not exists)
				IHTMLPage livePage = null;

				contentlet = null;
				try {
					List<Contentlet> result = APILocator.getContentletAPI()
							.search("+identifier:" + pageId + " +live:true", 0, 0, "moddate", user,
									false);
					if (!result.isEmpty()) {
						contentlet = result.get(0);
					}

				} catch (DotContentletStateException e) {
					// content not found message is already displayed on console
					Logger.debug(this, e.getMessage(), e);
				}
				if (contentlet != null)
					livePage = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);

				// working template working page
				Template workingTemplateWP = null;
				// live template working page
				Template liveTemplateWP = null;

				if (workingPage != null) {
					workingTemplateWP = APILocator.getTemplateAPI()
							.findWorkingTemplate(workingPage.getTemplateId(), user, false);
					liveTemplateWP = APILocator.getTemplateAPI()
							.findLiveTemplate(workingPage.getTemplateId(), user, false);
					// Templates dependencies
					if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.TEMPLATE.getType())) {
						templates.addOrClean(workingPage.getTemplateId(),
								workingTemplateWP.getModDate());
						templatesSet.add(workingPage.getTemplateId());
					}
				}

				Template liveTemplateLP = null;

				// live template live page
				if (livePage != null) {
					liveTemplateLP = APILocator.getTemplateAPI()
							.findLiveTemplate(livePage.getTemplateId(), user, false);
					// Templates dependencies
					if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.TEMPLATE.getType())) {
						templates.addOrClean(livePage.getTemplateId(), livePage.getModDate());
						templatesSet.add(livePage.getTemplateId());
					}
				}

				containerList.clear();

				if (workingTemplateWP != null && InodeUtils.isSet(workingTemplateWP.getInode())) {
					containerList.addAll(APILocator.getTemplateAPI()
							.getContainersInTemplate(workingTemplateWP, user, false));
				}
				if (liveTemplateWP != null && InodeUtils.isSet(liveTemplateWP.getInode())) {
					containerList.addAll(APILocator.getTemplateAPI()
							.getContainersInTemplate(liveTemplateWP, user, false));
				}
				if (liveTemplateLP != null && InodeUtils.isSet(liveTemplateLP.getInode())) {
					containerList.addAll(APILocator.getTemplateAPI()
							.getContainersInTemplate(liveTemplateLP, user, false));
				}

				for (Container container : containerList) {
					// Containers dependencies
					if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTAINER.getType())) {
						if (container instanceof FileAssetContainer) {
							fileAssetContainersSet.add(container.getIdentifier());
						} else {
							containers
									.addOrClean(container.getIdentifier(), container.getModDate());
							containersSet.add(container.getIdentifier());
						}
					}

					// Structure dependencies
					if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType())) {
						List<ContainerStructure> csList = APILocator.getContainerAPI()
								.getContainerStructures(container);

						for (ContainerStructure containerStructure : csList) {
							Structure st = CacheLocator.getContentTypeCache()
									.getStructureByInode(containerStructure.getStructureId());
							contentTypes.addOrClean(containerStructure.getStructureId(),
									st.getModDate());
							contentTypesSet.add(containerStructure.getStructureId());
						}
					}

					// Contents dependencies
					if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType())) {
						List<MultiTree> treeList = APILocator.getMultiTreeAPI()
								.getMultiTrees(workingPage, container);

						for (MultiTree mt : treeList) {
							String contentIdentifier = mt.getChild();
							List<Contentlet> contentList = APILocator.getContentletAPI()
									.search("+identifier:" + contentIdentifier, 0, 0, "moddate",
											user, false);
							for (Contentlet contentletI : contentList) {
								if(publisherFilter.acceptExcludeDependencyQuery(contentlet.getIdentifier())) {
									contents.addOrClean(contentletI.getIdentifier(),
											contentletI.getModDate());
									contentsSet.add(contentletI.getIdentifier());
								}
							}
						}
					}
				}

				// Rule dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.RULE.getType())) {
					final List<Rule> ruleList = APILocator.getRulesAPI()
							.getAllRulesByParent(workingPage, user, false);
					for (final Rule rule : ruleList) {
						this.rules.add(rule.getId());
						this.ruleSet.add(rule.getId());
					}
				}
			}
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 * For given Templates adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Containers</li>
	 * </ul>
	 */
	private void setTemplateDependencies(final PublisherFilter publisherFilter)  {
		try {
			List<Container> containerList = new ArrayList<>();
			FolderAPI folderAPI = APILocator.getFolderAPI();

			for (String id : templatesSet) {
				Template wkT = APILocator.getTemplateAPI().findWorkingTemplate(id, user, false);
				Template lvT = APILocator.getTemplateAPI().findLiveTemplate(id, user, false);

				// Host dependency
				if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
					Host h = APILocator.getHostAPI()
							.find(APILocator.getTemplateAPI().getTemplateHost(wkT).getIdentifier(),
									user, false);
					hosts.addOrClean(
							APILocator.getTemplateAPI().getTemplateHost(wkT).getIdentifier(),
							h.getModDate());
				}

				containerList.clear();
				// Container dependencies
				if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTAINER.getType())) {
					containerList.addAll(APILocator.getTemplateAPI()
							.getContainersInTemplate(wkT, user, false));

					if (lvT != null && InodeUtils.isSet(lvT.getInode())) {
						containerList.addAll(APILocator.getTemplateAPI()
								.getContainersInTemplate(lvT, user, false));
					}

					for (Container container : containerList) {

						if (container instanceof FileAssetContainer) {
							fileAssetContainersSet.add(container.getIdentifier());
							continue;
						}

						containers.addOrClean(container.getIdentifier(), container.getModDate());
						containersSet.add(container.getIdentifier());
					}
				}

				//Adding theme
				if (publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
					if (UtilMethods.isSet(wkT.getTheme())) {
						try {
							Folder themeFolder = folderAPI.find(wkT.getTheme(), user, false);
							if (themeFolder != null && InodeUtils.isSet(themeFolder.getInode())) {
								Folder parent = APILocator.getFolderAPI()
										.findParentFolder(themeFolder, user, false);
								if (UtilMethods.isSet(parent)) {
									folders.addOrClean(parent.getInode(), parent.getModDate());
									foldersSet.add(parent.getInode());
								}
								List<Folder> folderList = new ArrayList<Folder>();
								folderList.add(themeFolder);
								setFolderListDependencies(folderList, publisherFilter);
							}
						} catch (DotDataException e1) {
							Logger.error(DependencyManager.class,
									"Error trying to add theme folder for template Id: " + id
											+ ". Theme folder ignored because: " + e1.getMessage(),
									e1);
						}
					}
				}
			}

		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}

	}

	/**
	 * For given Containers adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Structures</li>
	 * </ul>
	 */
	private void setContainerDependencies(final PublisherFilter publisherFilter)  {

		try {

			List<Container> containerList = new ArrayList<>();

			for (String id : containersSet) {
				Container c = APILocator.getContainerAPI().getWorkingContainerById(id, user, false);

				// Host Dependency
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
					Host h = APILocator.getContainerAPI().getParentHost(c, user, false);
					hosts.addOrClean(APILocator.getContainerAPI().getParentHost(c, user, false)
							.getIdentifier(), h.getModDate());
				}

				containerList.clear();

				// Content Type Dependencies
				if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType())) {
					Container workingContainer = APILocator.getContainerAPI()
							.getWorkingContainerById(id, user, false);
					if (workingContainer != null) {
						containerList.add(workingContainer);
					}

					Container liveContainer = APILocator.getContainerAPI()
							.getLiveContainerById(id, user, false);
					if (liveContainer != null) {
						containerList.add(liveContainer);
					}

					for (Container container : containerList) {
						// Structure dependencies
						List<ContainerStructure> csList = APILocator.getContainerAPI()
								.getContainerStructures(container);

						for (ContainerStructure containerStructure : csList) {
							Structure st = CacheLocator.getContentTypeCache()
									.getStructureByInode(containerStructure.getStructureId());
							contentTypes.addOrClean(containerStructure.getStructureId(),
									st.getModDate());
							contentTypesSet.add(containerStructure.getStructureId());
						}
					}

				}
			}//end for containersSet

            // Process FileAssetContainer
			 final List<Folder> folders = collectFileAssetContainer().stream()
			        .map(this::collectFileAssetContainerDependencies).flatMap(Collection::stream)
					.collect(Collectors.toList());

             setFolderListDependencies(folders, publisherFilter);

		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}

	}

	/**
	 * Function that wraps calling getWorkingContainerById
	 */
	private Function<String, Container> workingContainerById = id -> {
		try {
			return APILocator.getContainerAPI().getWorkingContainerById(id, user, false);
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		}
		return null;
	};

	/**
	 * Aux Predicate to simplify filtering FileAssetContainer
	 */
	private Predicate<Container> fileAssetContainer = container -> container instanceof FileAssetContainer;

	/**
	 * Utility method that takes a Container source function as param and returns FileAssetContainer
	 * @return
	 */
	private Set<FileAssetContainer> collectFileAssetContainer() {
		return fileAssetContainersSet.stream().map(workingContainerById).filter(Objects::nonNull)
				.filter(fileAssetContainer).map(FileAssetContainer.class::cast)
				.collect(Collectors.toSet());
	}

	/**
	 * Given that a FileAssetContainer is defined by a bunch of vtl files we need to collect the folder that enclose'em
 	 * @param fileAssetContainer
	 * @return
	 */
	private Set<Folder> collectFileAssetContainerDependencies(final FileAssetContainer fileAssetContainer){
       final Set<Folder> collectedFolders = new HashSet<>();
       final List<FileAsset> fileAssets = fileAssetContainer.getContainerStructuresAssets();
       for(final FileAsset fileAsset:fileAssets){
		   try {
			   final Folder folder = APILocator.getFolderAPI().findFolderByPath(fileAsset.getPath(), fileAsset.getHost(),user, false);
			   if(UtilMethods.isSet(folder)) {
				  collectedFolders.add(folder);
			   }
		   } catch (DotSecurityException | DotDataException e) {
			   Logger.error(this, "Error collecting folders for FileAssetContainer " + fileAsset.getFileName() ,e);
		   }
       }
       return collectedFolders;
    }

	/**
	 * For given Structures adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Folders</li>
	 * <li>Relationships</li>
	 * </ul>
	 */
	private void setStructureDependencies(final PublisherFilter publisherFilter)  throws DotDataException, DotSecurityException {
		try {

			for (String inode : contentTypesSet) {
				structureDependencyHelper(inode,publisherFilter);
			}

		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 * 
	 * @param stInode
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void structureDependencyHelper(final String stInode, final PublisherFilter publisherFilter) throws DotDataException, DotSecurityException{
		final Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(stInode);

		// Host Dependency
		if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
			final Host host = APILocator.getHostAPI().find(structure.getHost(), user, false);
			hosts.addOrClean(structure.getHost(), host.getModDate()); // add the host dependency
		}

		// Folder Dependencies
		if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
			final Folder folder = APILocator.getFolderAPI()
					.find(structure.getFolder(), user, false);
			folders.addOrClean(structure.getFolder(),
					folder.getModDate()); // add the folder dependency
		}

		// Workflows Dependencies
		if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.WORKFLOW.getType())) {
			try {
				final List<WorkflowScheme> schemes = APILocator.getWorkflowAPI()
						.findSchemesForStruct(structure);
				for (final WorkflowScheme scheme : schemes) {
					workflows.addOrClean(scheme.getId(), scheme.getModDate());
				}
			} catch (DotDataException e) {
				Logger.debug(getClass(),
						() -> "Could not get the Workflow Scheme Dependency for Structure ID: "
								+ structure.getInode());
			}
		}

		// Categories Dependencies
		if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CATEGORY.getType())) {
			APILocator.getCategoryAPI()
					.findCategories(new StructureTransformer(structure).from(), user)
					.forEach(category -> {
						this.categories.addOrClean(category.getCategoryId(), category.getModDate());
					});
		}

		// Related structures
		if(publisherFilter.isRelationships()) {
			final List<Relationship> relations = FactoryLocator.getRelationshipFactory()
					.byContentType(structure);

			for (final Relationship relationship : relations) {
				relationships.addOrClean(relationship.getInode(), relationship.getModDate());

				if (!contentTypes.contains(relationship.getChildStructureInode()) && config
						.getOperation().equals(Operation.PUBLISH)) {
					Structure struct = CacheLocator.getContentTypeCache()
							.getStructureByInode(relationship.getChildStructureInode());
					solvedContentTypes.add(stInode);
					contentTypes
							.addOrClean(relationship.getChildStructureInode(), struct.getModDate());

					if (!solvedContentTypes.contains(relationship.getChildStructureInode()))
						structureDependencyHelper(relationship.getChildStructureInode(),
								publisherFilter);
				}
				if (!contentTypes.contains(relationship.getParentStructureInode()) && config
						.getOperation().equals(Operation.PUBLISH)) {
					Structure struct = CacheLocator.getContentTypeCache()
							.getStructureByInode(relationship.getParentStructureInode());
					solvedContentTypes.add(stInode);
					contentTypes.addOrClean(relationship.getParentStructureInode(),
							struct.getModDate());

					if (!solvedContentTypes.contains(relationship.getParentStructureInode()))
						structureDependencyHelper(relationship.getParentStructureInode(),
								publisherFilter);
				}
			}
		}
	}

	/**
	 * 
	 * @param cons
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void processList(final Set<Contentlet> cons, final PublisherFilter publisherFilter) throws DotDataException, DotSecurityException {
		Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
		Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<Contentlet>();

		for (Contentlet con : cons) {
			// Host Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
				Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
				hosts.addOrClean(con.getHost(), h.getModDate());
			}

			contentsToProcess.add(con);

			// Relationships Dependencies
			if(publisherFilter.isRelationships()) {
				Map<Relationship, List<Contentlet>> contentRel =
						APILocator.getContentletAPI().findContentRelationships(con, user);

				for (Relationship rel : contentRel.keySet()) {
					contentsToProcess.addAll(contentRel.get(rel));
					/**
					 * ISSUE #2222: https://github.com/dotCMS/dotCMS/issues/2222
					 *
					 * We need the relationships in which the single related content is involved.
					 *
					 */
					if (contentRel.get(rel).size() > 0)
						relationships.addOrClean(rel.getInode(), rel.getModDate());
				}
			}
		}
		// end relationships false

		for (Contentlet con : contentsToProcess) {
			// Host Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
				Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
				hosts.addOrClean(con.getHost(), h.getModDate());
			}
			contentsWithDependenciesToProcess.add(con);
			//Copy asset files to bundle folder keeping original folders structure
			List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());

			for(Field ff : fields) {
				if (ff.getFieldType().equals(Field.FieldType.IMAGE.toString())
						|| ff.getFieldType().equals(Field.FieldType.FILE.toString())) {

					try {
						String value = "";
						if(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(con, ff))){
							value = APILocator.getContentletAPI().getFieldValue(con, ff).toString();
						}
						Identifier id = APILocator.getIdentifierAPI().find(value);
						if (InodeUtils.isSet(id.getInode()) && id.getAssetType().equals("contentlet")) {
							contentsWithDependenciesToProcess.addAll(
									APILocator.getContentletAPI()
									.search("+identifier:"+id.getId(), 0, 0, "moddate", user, false));
						}
					} catch (Exception ex) {
						Logger.debug(this, ex.toString());
						throw new DotStateException("Problem occured while publishing file");
					}
				}

			}
		}

		// Adding the Contents (including related) and adding filesAsContent
		for (Contentlet con : contentsWithDependenciesToProcess) {
			// Host Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
				Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
				hosts.addOrClean(con.getHost(), h.getModDate());
			}
			// Content Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType()) && publisherFilter.acceptExcludeDependencyQuery(con.getIdentifier())) {
				contents.addOrClean(con.getIdentifier(),
						con.getModDate());
			}
			// Folder Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.FOLDER.getType())) {
				Folder f = APILocator.getFolderAPI().find(con.getFolder(), user, false);
				folders.addOrClean(con.getFolder(), f.getModDate()); // adding content folder
			}
			// Language Dependency
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.LANGUAGE.getType())) {
				languages.addOrClean(Long.toString(con.getLanguageId()),
						new Date()); // will be included only when hasn't been sent ever
			}

			try {
				if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
						&& con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

					Folder contFolder=APILocator.getFolderAPI().find(con.getFolder(), user, false);
					List<IHTMLPage> folderHtmlPages = new ArrayList<IHTMLPage>(); 
					folderHtmlPages.addAll(APILocator.getHTMLPageAssetAPI().getHTMLPages(contFolder, false, false, user, false));
					folderHtmlPages.addAll(APILocator.getHTMLPageAssetAPI().getHTMLPages(contFolder, true, false, user, false));

					Set<String> pagesToProcess = new HashSet<>();
					for (IHTMLPage htmlPage : folderHtmlPages) {

						Boolean mustBeIncluded;

						mustBeIncluded = contents.addOrClean(htmlPage.getIdentifier(), htmlPage.getModDate());


						if (mustBeIncluded) {
							pagesToProcess.add(htmlPage.getIdentifier());
						}

					}
					//Process the pages we found
					setHTMLPagesDependencies(pagesToProcess,publisherFilter);
				}
			} catch (Exception e) {
				Logger.debug(this, e.toString());
			}

			if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true) && publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType())) {
				Structure struct = CacheLocator.getContentTypeCache().getStructureByInode(con.getStructureInode());
				contentTypes.addOrClean( con.getStructureInode(), struct.getModDate());
				structureDependencyHelper(con.getStructureInode(),publisherFilter);
			}

			// Evaluate all the categories from this contentlet to include as dependency.
			if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CATEGORY.getType())) {
				final List<Category> categoriesFromContentlet = APILocator.getCategoryAPI()
						.getParents(con, APILocator.systemUser(), false);
				for (Category category : categoriesFromContentlet) {
					categories.addOrClean(category.getCategoryId(), category.getModDate());
				}
			}
        }
		
		//This is for adding the new language variables (as content)
        for (String lang : languages) {
            String keyValueQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE + " +languageId:" + lang;
            List<Contentlet> listKeyValueLang = APILocator.getContentletAPI()
                            .search(keyValueQuery,0, -1, StringPool.BLANK, user, false);// search for language variables
            if (!listKeyValueLang.isEmpty()) {// if there is any language variable add the content type
                Structure struct = CacheLocator.getContentTypeCache()
                                .getStructureByInode(listKeyValueLang.get(0).getContentTypeId());
                contentTypes.addOrClean(struct.getIdentifier(), struct.getModDate());
                structureDependencyHelper(struct.getIdentifier(),publisherFilter);
            }
            for (Contentlet keyValue : listKeyValueLang) {// add the language variable
                contents.addOrClean(keyValue.getIdentifier(), keyValue.getModDate());
            }
        }

	}

	/**
	 * For given Contentles adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Folders</li>
	 * <li>Structures</li>
	 * <li>Relationships</li>
	 * </ul>
	 *
	 * @throws DotBundleException If fails executing the Lucene queries
	 */
	private void setContentDependencies(final PublisherFilter publisherFilter)  throws DotBundleException {
		try {
			// we need to process contents already taken as dependency
			Set<String> cons = new HashSet<String>(contentsSet);

			Set<Contentlet> allContents = new HashSet<Contentlet>(); // we will put here those already added and the ones from lucene queries

			for(String id : cons){
				allContents.addAll(APILocator.getContentletAPI().search("+identifier:"+id, 0, 0, "moddate", user, false));
			}

			processList(allContents,publisherFilter);

		} catch (Exception e) {
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
					+ e.getMessage() + ": Unable to pull content", e);
		}

	}

	/**
	 * Collects the different dependent objects that are required for pushing
	 * {@link Rule} objects. The required dependency of a rule is either:
	 * <ol>
	 * <li>The Site (host) they were created in.</li>
	 * <li>Or the Content Page they were created in.</li>
	 * </ol>
	 */
	private void setRuleDependencies(final PublisherFilter publisherFilter)  {
		String ruleToProcess = "";
		final RulesAPI rulesAPI = APILocator.getRulesAPI();
		final HostAPI hostAPI = APILocator.getHostAPI();
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();
		try {
			for (String ruleId : this.rules) {
				final Rule rule = rulesAPI.getRuleById(ruleId, this.user, false);
				ruleToProcess = ruleId;
				final List<Contentlet> contentlets = contentletAPI.searchByIdentifier(
						"+identifier:" + rule.getParent(), 1, 0, null, this.user, false,
						PermissionAPI.PERMISSION_READ, true);

				if (contentlets != null && contentlets.size() > 0) {
					final Contentlet parent = contentlets.get(0);
					// If the parent of the rule is a Site...
					if (parent.isHost()) {
						if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType())) {
							final Host host = hostAPI.find(rule.getParent(), this.user, false);
							this.hosts.addOrClean(host.getIdentifier(), host.getModDate());
							this.hostsSet.add(host.getIdentifier());
						}
					}
					// If the parent of the rule is a Content Page...
					else if (parent.isHTMLPage()) {
						if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType()) && publisherFilter.acceptExcludeDependencyQuery(parent.getIdentifier())) {
							this.contents.addOrClean(parent.getIdentifier(), parent.getModDate());
							this.contentsSet.add(parent.getIdentifier());
						}
					} else {
						throw new DotDataException("The parent ID [" + parent.getIdentifier() + "] is a non-valid parent.");
					}
				} else {
					throw new DotDataException("The parent ID [" + rule.getParent() + "] cannot be found for Rule [" + rule.getId() + "]");
				}
			}
		} catch (DotDataException e) {
			Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
		}
	}
	
	private void setLanguageDependencies(final PublisherFilter publisherFilter) {
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();
		final Date date = new Date();
		try{
		//We're no longer filtering by language here..
		//The reason is We're simply collecting all available lang variables so we can infer additional languages used. see #15359
				final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE ;
				final List<Contentlet> langVariables = contentletAPI.search(langVarsQuery, 0, -1, StringPool.BLANK, user, false);
				for(final Contentlet langVar : langVariables){
					if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType()) && publisherFilter.acceptExcludeDependencyQuery(langVar.getIdentifier())) {
						contents.addOrClean(langVar.getIdentifier(), langVar.getModDate());
						contentsSet.add(langVar.getIdentifier());
					}
					//Collect the languages
					if(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.LANGUAGE.getType())) {
						languages.addOrClean(Long.toString(langVar.getLanguageId()), date);
					}
				}

		}catch (Exception e){
			Logger.error(this, e.getMessage(),e);
		}
	}

}
