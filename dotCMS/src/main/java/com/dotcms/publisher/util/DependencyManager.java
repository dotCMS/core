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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
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

	private PublisherFilter publisherFilter;

	private final DependencySet hosts;
	private final DependencySet folders;
	private final DependencySet htmlPages;
	private final DependencySet templates;
	private final DependencySet contentTypes;
	private final DependencySet containers;
	private final DependencySet contents;
	private final DependencySet links;

	private final DependencySet relationships;
	private final DependencySet workflows;
	private final DependencySet languages;
	private final DependencySet rules;
	private final DependencySet categories;

	private Map<AssetTypes, Consumer<String>> consumerDependencies;
	private final DependencyProcessor dependencyProcessor;

	private User user;

	private PushPublisherConfig config;

	private final List<Exception> errors = Collections.synchronizedList(new ArrayList<>());


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

		consumerDependencies = new HashMap<>();
		consumerDependencies.put(AssetTypes.HOST, (hostId) -> proccessHostDependency(hostId));
		consumerDependencies.put(AssetTypes.FOLDER, (folderId) -> processFolderDependency(folderId));
		consumerDependencies.put(AssetTypes.TEMPLATES, (templateId) -> processTemplateDependencies(templateId));
		consumerDependencies.put(AssetTypes.CONTAINERS, (containerId) -> processContainerDependency(containerId));
		consumerDependencies.put(AssetTypes.CONTENT_TYPE, (contentTypeInode) -> processContentTypeDependency(contentTypeInode));
		consumerDependencies.put(AssetTypes.LINKS, (linkId) -> processLinkDependency(linkId));
		consumerDependencies.put(AssetTypes.CONTENTS, (contentId) -> {
			try {
				processContentDependency(contentId);
			} catch (DotBundleException e) {
				Logger.error(DependencyManager.class, e.getMessage());
				errors.add(e);
			}
		});
		consumerDependencies.put(AssetTypes.RULES, (ruleId) -> setRuleDependencies(ruleId));
		consumerDependencies.put(AssetTypes.LANGUAGES, (langId) -> {
			try {
				processLanguage(langId);
			} catch (DotBundleException e) {
				Logger.error(DependencyManager.class, e.getMessage());
				errors.add(e);
			}
		});


		dependencyProcessor = new DependencyProcessor();

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
		this.publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());

		Logger.debug(DependencyManager.class, "publisherFilter.isDependencies() " + publisherFilter.isDependencies());
		if(publisherFilter.isDependencies()){
			dependencyProcessor.start();
		}

		setLanguageVariables();

		List<PublishQueueElement> assets = config.getAssets();

		Logger.debug(this,publisherFilter.toString());
		for (PublishQueueElement asset : assets) {
			//Check if the asset.Type is in the excludeClasses filter, if it is, the asset is not added to the bundle
			if(publisherFilter.doesExcludeClassesContainsType(asset.getType())){
				Logger.debug(getClass(),"Asset Id: " + asset.getAsset() +  " will not be added to the bundle since it's type: " + asset.getType() + " must be excluded according to the filter");
				continue;
			}

			if(asset.getType().equals(PusheableAsset.CONTENT_TYPE.getType())) {
				try {
					Structure st = CacheLocator.getContentTypeCache().getStructureByInode(asset.getAsset());

					if(st == null) {
						Logger.warn(getClass(), "Structure id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						contentTypes.add(asset.getAsset(), st.getModDate(),true);
						dependencyProcessor.put(asset.getAsset(), AssetTypes.CONTENT_TYPE);
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
						templates.add(asset.getAsset(), t.getModDate(),true);
						dependencyProcessor.put(asset.getAsset(), AssetTypes.TEMPLATES);
					}

				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the Template to the Bundle. Bundle ID: " + config.getId() + ", Template ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.CONTAINER.getType())) {
				try {
					Container container = (Container) APILocator.getVersionableAPI().findLiveVersion(asset.getAsset(), user, false);

					if(container == null) {
						container = APILocator.getContainerAPI().getWorkingContainerById(asset.getAsset(), user, false);
					}

					if(container instanceof FileAssetContainer){
						Logger.debug(getClass(), "FileAssetContainer id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" will be ignored");
					} else {
						containers.add(asset.getAsset(), container.getModDate(),true);
					}

					if(container == null) {
						Logger.warn(getClass(), "Container id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					}

					dependencyProcessor.put(asset.getAsset(), AssetTypes.CONTAINERS);

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Container to the Bundle. Bundle ID: " + config.getId() + ", Container ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.FOLDER.getType())) {
				try {
					Folder f = APILocator.getFolderAPI().find(asset.getAsset(), user, false);

					if(f == null){
						Logger.warn(getClass(), "Folder id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
					} else {
						folders.add(asset.getAsset(), f.getModDate(),true);
						dependencyProcessor.put(asset.getAsset(), AssetTypes.FOLDER);
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
						hosts.add(asset.getAsset(), h.getModDate(),true);
						dependencyProcessor.put(asset.getAsset(), AssetTypes.HOST);
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
						links.add(asset.getAsset(),link.getModDate(),true);
						dependencyProcessor.put(asset.getAsset(), AssetTypes.LINKS);
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Host to the Bundle. Bundle ID: " + config.getId() + ", Host ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals(PusheableAsset.WORKFLOW.getType())) {
				WorkflowScheme scheme = APILocator.getWorkflowAPI().findScheme(asset.getAsset());

				if(scheme == null){
					Logger.warn(getClass(), "WorkflowScheme id: "+ (asset.getAsset() != null ? asset.getAsset() : "N/A") +" does NOT have working or live version, not Pushed");
				} else {
					workflows.add(asset.getAsset(),scheme.getModDate(),true);
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
					dependencyProcessor.put(asset.getAsset(), AssetTypes.LANGUAGES);
				}
			} else if (asset.getType().equals(PusheableAsset.RULE.getType())) {
				Rule rule = APILocator.getRulesAPI()
						.getRuleById(asset.getAsset(), user, false);
				if (rule != null && StringUtils.isNotBlank(rule.getId())) {
					this.rules.add(rule.getId());
					dependencyProcessor.put(rule.getId(), AssetTypes.RULES);
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
			contentIds.removeIf(c->publisherFilter.doesExcludeQueryContainsContentletId(c));

			for(String id : contentIds){
				final Identifier ident = APILocator.getIdentifierAPI().find(id);
				final List<Contentlet> contentlets = APILocator.getContentletAPI().findAllVersions(ident, false, user, false);
				for(Contentlet con : contentlets){
					contents.add(con.getIdentifier(), con.getModDate(),true);
					dependencyProcessor.put(con.getIdentifier(), AssetTypes.CONTENTS);
				}
			}
		}

		dependencyProcessor.waitUntilFinish();

		if (!errors.isEmpty()) {
			final String messages =
					errors.stream().map(exception -> exception.getMessage()).collect(Collectors.joining(","));
			throw new DotBundleException(messages);
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
	private void processLinkDependency(final String linkId)  {
		try {
			Identifier ident=APILocator.getIdentifierAPI().find(linkId);

			// Folder Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
				final Folder folder = APILocator.getFolderAPI()
						.findFolderByPath(ident.getParentPath(), ident.getHostId(), user,
								false);
				folders.addOrClean(folder.getInode(), folder.getModDate());
			}

			// Host Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
				final Host host = APILocator.getHostAPI().find(ident.getHostId(), user, false);
				hosts.addOrClean(host.getIdentifier(), host.getModDate());
			}

			// Content Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType())) {
				final Link link = APILocator.getMenuLinkAPI()
						.findWorkingLinkById(linkId, user, false);

				if (link != null) {

					if (link.getLinkType().equals(Link.LinkType.INTERNAL.toString())) {
						final Identifier id = APILocator.getIdentifierAPI()
								.find(link.getInternalLinkIdentifier());

						// add file/content dependencies. will also work with htmlpages as content
						if (InodeUtils.isSet(id.getInode()) && id.getAssetType()
								.equals("contentlet")) {
							final List<Contentlet> contentList = APILocator.getContentletAPI()
									.search("+identifier:" + id.getId(), 0, 0, "moddate", user,
											false);

							for (final Contentlet contentlet : contentList) {
								if(!publisherFilter.doesExcludeDependencyQueryContainsContentletId(contentlet.getIdentifier())) {
									contents.addOrClean(contentlet.getIdentifier(),
											contentlet.getModDate());
									dependencyProcessor.put(contentlet.getIdentifier(), AssetTypes.CONTENTS);
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
	private void proccessHostDependency (final String hostId) {
		try {

			final Host host = APILocator.getHostAPI().find(hostId, user, false);

			// Template dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.TEMPLATE.getType())) {
				final List<Template> templateList = APILocator.getTemplateAPI()
						.findTemplatesAssignedTo(host);
				for (final Template template : templateList) {
					templates.addOrClean(template.getIdentifier(), template.getModDate());
					dependencyProcessor.put(template.getIdentifier(), AssetTypes.TEMPLATES);
				}
			}

			// Container dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTAINER.getType())) {
				final List<Container> containerList = APILocator.getContainerAPI()
						.findContainersUnder(host);
				for (final Container container : containerList) {
					if (!(container instanceof FileAssetContainer)) {
						containers
								.addOrClean(container.getIdentifier(), container.getModDate());
					}

					dependencyProcessor.put(container.getIdentifier(), AssetTypes.CONTAINERS);
				}
			}

			// Content dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType())) {
				final String luceneQuery = "+conHost:" + host.getIdentifier();
				final List<Contentlet> contentList = APILocator.getContentletAPI()
						.search(luceneQuery, 0, 0, null, user, false);
				for (final Contentlet contentlet : contentList) {
					if(!publisherFilter.doesExcludeDependencyQueryContainsContentletId(contentlet.getIdentifier())) {
						contents.addOrClean(contentlet.getIdentifier(),
								contentlet.getModDate());
						dependencyProcessor.put(contentlet.getIdentifier(), AssetTypes.CONTENTS);
					}
				}
			}

			// Structure dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
				final List<Structure> structuresList = StructureFactory
						.getStructuresUnderHost(host, user, false);
				for (final Structure structure : structuresList) {
					contentTypes.addOrClean(structure.getInode(), structure.getModDate());
					dependencyProcessor.put(structure.getInode(), AssetTypes.CONTENT_TYPE);
				}
			}

			// Folder dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
				final List<Folder> folderList = APILocator.getFolderAPI()
						.findFoldersByHost(host, user, false);
				for (final Folder folder : folderList) {
					folders.addOrClean(folder.getInode(), folder.getModDate());
					dependencyProcessor.put(folder.getInode(), AssetTypes.FOLDER);
				}
			}

			// Rule dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.RULE.getType())) {
				final List<Rule> ruleList = APILocator.getRulesAPI()
						.getAllRulesByParent(host, user, false);
				for (final Rule rule : ruleList) {
					this.rules.add(rule.getId());
					dependencyProcessor.put(rule.getId(), AssetTypes.RULES);
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
	private void processFolderDependency(final String folderId) {
		try {
			final Folder folder = APILocator.getFolderAPI().find(folderId, user, false);
			// Parent folder
			final Folder parentFolder = APILocator.getFolderAPI().findParentFolder(folder, user, false);
			if(UtilMethods.isSet(parentFolder)) {
				folders.addOrClean( parentFolder.getInode(), parentFolder.getModDate());
				dependencyProcessor.put(parentFolder.getInode(), AssetTypes.FOLDER);
			}

			setFolderListDependencies(folder);
		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 *

	 * @throws DotIdentifierStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void setFolderListDependencies(final Folder folder)
			throws DotIdentifierStateException, DotDataException, DotSecurityException {

		// Host dependency
		if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
			final Host host = APILocator.getHostAPI().find(folder.getHostId(), user, false);
			hosts.addOrClean(folder.getHostId(), host.getModDate());
		}

		// Content dependencies
		if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType())) {
			final String luceneQuery = "+conFolder:" + folder.getInode();
			final List<Contentlet> contentList = APILocator.getContentletAPI()
					.search(luceneQuery, 0, 0, null, user, false);
			for (final Contentlet contentlet : contentList) {
				if(!publisherFilter.doesExcludeDependencyQueryContainsContentletId(contentlet.getIdentifier())) {
					contents.addOrClean(contentlet.getIdentifier(), contentlet.getModDate());
					dependencyProcessor.put(contentlet.getIdentifier(), AssetTypes.CONTENTS);
				}
			}
		}

		// Menu Link dependencies
		if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.LINK.getType())) {
			final List<Link> linkList = APILocator.getMenuLinkAPI().findFolderMenuLinks(folder);
			for (final Link link : linkList) {
				links.addOrClean(link.getIdentifier(), link.getModDate());
				dependencyProcessor.put(link.getIdentifier(), AssetTypes.LINKS);
			}
		}

		// Structure dependencies
		if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
			final List<Structure> structureList = APILocator.getFolderAPI()
					.getStructures(folder, user, false);
			for (final Structure structure : structureList) {
				contentTypes.addOrClean(structure.getInode(), structure.getModDate());
				dependencyProcessor.put(structure.getInode(), AssetTypes.CONTENT_TYPE);
			}

			//Add the default structure of this folder
			if (folder.getDefaultFileType() != null) {
				final Structure defaultStructure = CacheLocator.getContentTypeCache()
						.getStructureByInode(folder.getDefaultFileType());
				if ((defaultStructure != null && InodeUtils.isSet(defaultStructure.getInode()))
						&& !dependencyProcessor.alreadyProcess(defaultStructure.getInode(), AssetTypes.CONTENT_TYPE)) {
					contentTypes.addOrClean(defaultStructure.getInode(),
							defaultStructure.getModDate());
					dependencyProcessor.put(defaultStructure.getInode(), AssetTypes.CONTENT_TYPE);
				}
			}
		}

		setFolderListDependencies(APILocator.getFolderAPI().findSubFolders(folder, user, false));
	}

	private void setFolderListDependencies(final Collection<Folder> folders)
			throws DotIdentifierStateException, DotDataException, DotSecurityException {

		for (final Folder folder : folders) {
			setFolderListDependencies(folder);
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
	 */
	private void processHTMLPagesDependency(final String pageId) {
		try {

			final IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
			final FolderAPI folderAPI = APILocator.getFolderAPI();
			final Set<Container> containerList = new HashSet<>();


			final Identifier identifier = idenAPI.find(pageId);

			if(identifier==null || UtilMethods.isEmpty(identifier.getId())) {
				Logger.warn(this.getClass(), "Unable to find page for identifier, moving on.  Id: " + identifier );
				return;
			}

			// Host dependency
			if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
				final Host host = APILocator.getHostAPI().find(identifier.getHostId(), user, false);
				hosts.addOrClean(identifier.getHostId(), host.getModDate());
			}

			// Folder dependencies
			if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
				final Folder folder = folderAPI
						.findFolderByPath(identifier.getParentPath(), identifier.getHostId(), user, false);
				folders.addOrClean(folder.getInode(), folder.getModDate());
			}

			// looking for working version (must exists)
			final IHTMLPage workingPage = Try.of(
					()->APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(
							identifier,
							APILocator.getLanguageAPI().getDefaultLanguage().getId(),
							false,
							user,
							false)
			).onFailure(e->Logger.warnAndDebug(DependencyManager.class, e)).getOrNull();

			if(workingPage==null) {
				return;
			}

			final IHTMLPage livePage = workingPage.isLive()
					? workingPage
					: Try.of(()->
					APILocator.getHTMLPageAssetAPI().findByIdLanguageFallback(identifier, APILocator.getLanguageAPI().getDefaultLanguage().getId(), true, user, false))
					.onFailure(e->Logger.warnAndDebug(DependencyManager.class, e)).getOrNull();

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
				if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.TEMPLATE.getType()) && workingTemplateWP!=null) {
					templates.addOrClean(workingPage.getTemplateId(),
							workingTemplateWP.getModDate());
					dependencyProcessor.put(workingPage.getTemplateId(), AssetTypes.TEMPLATES);
				}
			}

			Template liveTemplateLP = null;

			// live template live page
			if (livePage != null) {
				liveTemplateLP = APILocator.getTemplateAPI()
						.findLiveTemplate(livePage.getTemplateId(), user, false);
				// Templates dependencies
				if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.TEMPLATE.getType()) && liveTemplateLP!=null ) {
					templates.addOrClean(livePage.getTemplateId(), livePage.getModDate());
					dependencyProcessor.put(livePage.getTemplateId(), AssetTypes.TEMPLATES);
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

			for (final Container container : containerList) {
				// Containers dependencies
				if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTAINER.getType())) {
					dependencyProcessor.put(container.getIdentifier(), AssetTypes.CONTAINERS);
				}

				// Structure dependencies
				if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
					final List<ContainerStructure> containerStructureList = APILocator.getContainerAPI()
							.getContainerStructures(container);

					for (final ContainerStructure containerStructure : containerStructureList) {
						final Structure structure = CacheLocator.getContentTypeCache()
								.getStructureByInode(containerStructure.getStructureId());
						contentTypes.addOrClean(containerStructure.getStructureId(),
								structure.getModDate());
						dependencyProcessor.put(containerStructure.getStructureId(), AssetTypes.CONTENT_TYPE);
					}
				}

				// Contents dependencies
				if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType())) {

					final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees =
							APILocator.getMultiTreeAPI().getPageMultiTrees(workingPage, false);

					final List<String> contentsId = pageMultiTrees.values().stream()
							.flatMap(personalizedContentlets -> personalizedContentlets.stream())
							.map(personalizedContentlet -> personalizedContentlet.getContentletId())
							.collect(Collectors.toList());

					for (final String contentIdentifier : contentsId) {

						if (contentIdentifier == null) {
							continue;
						}

						final Identifier id = APILocator.getIdentifierAPI().find(contentIdentifier);
						final List<Contentlet> contentList = APILocator.getContentletAPI().findAllVersions(id, false, user, false);


						contentList.removeIf(c->publisherFilter.doesExcludeDependencyQueryContainsContentletId(contentIdentifier));

						for (final Contentlet contentletI : contentList) {
							this.contents.addOrClean(contentletI.getIdentifier(),
									contentletI.getModDate());
							dependencyProcessor.put(contentletI.getIdentifier(), AssetTypes.CONTENTS);
						}
					}
				}
			}

			// Rule dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.RULE.getType())) {
				final List<Rule> ruleList = APILocator.getRulesAPI()
						.getAllRulesByParent(workingPage, user, false);
				for (final Rule rule : ruleList) {
					this.rules.add(rule.getId());
					dependencyProcessor.put(rule.getId(), AssetTypes.RULES);
				}
			}
		} catch (DotSecurityException | DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	private void processTemplateDependencies(final String id) {

		try {
			final FolderAPI folderAPI = APILocator.getFolderAPI();

			final Template workingTemplate = APILocator.getTemplateAPI().findWorkingTemplate(id, user, false);
			final Template liveTemplate = APILocator.getTemplateAPI().findLiveTemplate(id, user, false);

			// Host dependency
			if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
				final Host host = APILocator.getHostAPI()
						.find(APILocator.getTemplateAPI().getTemplateHost(workingTemplate).getIdentifier(),
								user, false);
				hosts.addOrClean(
						APILocator.getTemplateAPI().getTemplateHost(workingTemplate).getIdentifier(),
						host.getModDate());
			}

			final List<Container> containerList = new ArrayList();

			// Container dependencies
			if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTAINER.getType())) {
				containerList.addAll(APILocator.getTemplateAPI()
						.getContainersInTemplate(workingTemplate, user, false));

				if (liveTemplate != null && InodeUtils.isSet(liveTemplate.getInode())) {
					containerList.addAll(APILocator.getTemplateAPI()
							.getContainersInTemplate(liveTemplate, user, false));
				}

				for (final Container container : containerList) {
					dependencyProcessor.put(container.getIdentifier(), AssetTypes.CONTAINERS);

					if (container instanceof FileAssetContainer) {
						continue;
					}

					containers.addOrClean(container.getIdentifier(), container.getModDate());
				}
			}

			//Adding theme
			if (!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
				if (UtilMethods.isSet(workingTemplate.getTheme())) {
					try {
						final Folder themeFolder = folderAPI.find(workingTemplate.getTheme(), user, false);
						if (themeFolder != null && InodeUtils.isSet(themeFolder.getInode())) {
							final Folder parentFolder = APILocator.getFolderAPI()
									.findParentFolder(themeFolder, user, false);
							if (UtilMethods.isSet(parentFolder)) {
								folders.addOrClean(parentFolder.getInode(), parentFolder.getModDate());
								dependencyProcessor.put(parentFolder.getInode(), AssetTypes.FOLDER);
							}
							final List<Folder> folderList = new ArrayList<Folder>();
							folderList.add(themeFolder);
							setFolderListDependencies(folderList);
						}
					} catch (DotDataException e1) {
						Logger.error(DependencyManager.class,
								"Error trying to add theme folder for template Id: " + id
										+ ". Theme folder ignored because: " + e1.getMessage(),
								e1);
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
	private void processContainerDependency(final String containerId)  {

		try {
			final Container containerById = APILocator.getContainerAPI().getWorkingContainerById(containerId, user, false);

			// Host Dependency
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
				final Host host = APILocator.getContainerAPI().getParentHost(containerById, user, false);

				hosts.addOrClean(APILocator.getContainerAPI().getParentHost(containerById, user, false)
						.getIdentifier(), host.getModDate());
			}

			final List<Container> containerList = new ArrayList<>();

			// Content Type Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
				final Container workingContainer = APILocator.getContainerAPI()
						.getWorkingContainerById(containerId, user, false);
				if (workingContainer != null) {
					containerList.add(workingContainer);
				}

				final Container liveContainer = APILocator.getContainerAPI()
						.getLiveContainerById(containerId, user, false);
				if (liveContainer != null) {
					containerList.add(liveContainer);
				}

				for (final Container container : containerList) {
					// Structure dependencies
					final List<ContainerStructure> csList = APILocator.getContainerAPI()
							.getContainerStructures(container);

					for (final ContainerStructure containerStructure : csList) {
						final Structure structure = CacheLocator.getContentTypeCache()
								.getStructureByInode(containerStructure.getStructureId());
						contentTypes.addOrClean(containerStructure.getStructureId(),
								structure.getModDate());
						dependencyProcessor.put(containerStructure.getStructureId(), AssetTypes.CONTENT_TYPE);
					}
				}

			}

			if (containerById instanceof FileAssetContainer ) {
				// Process FileAssetContainer
				final Set<Folder> folders = this.collectFileAssetContainerDependencies((FileAssetContainer) containerById);
				setFolderListDependencies(folders);
			}

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
	 * Given that a FileAssetContainer is defined by a bunch of vtl files we need to collect the folder that enclose'em
	 * @param fileAssetContainer
	 * @return
	 */
	private Set<Folder> collectFileAssetContainerDependencies(final FileAssetContainer fileAssetContainer) {
		try {
			final String path = fileAssetContainer.getPath();
			final Folder rootFolder = APILocator.getFolderAPI()
					.findFolderByPath(path, fileAssetContainer.getHost(), user, false);
			final List<Folder> subFolders = APILocator.getFolderAPI()
					.findSubFolders(rootFolder, user, false);

			final Set<Folder> dependenciesFolders = new HashSet<>();
			dependenciesFolders.add(rootFolder);
			dependenciesFolders.addAll(subFolders);

			return dependenciesFolders;
		}catch (DotSecurityException | DotDataException e) {
			Logger.error(DependencyManager.class, e);
			return Collections.emptySet();
		}
	}

	/**
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void processContentTypeDependency(final String contentTypeInode) {
		final Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(contentTypeInode);

		try{
			// Host Dependency
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
				final Host host = APILocator.getHostAPI().find(structure.getHost(), user, false);
				hosts.addOrClean(structure.getHost(), host.getModDate()); // add the host dependency
			}

			// Folder Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
				final Folder folder = APILocator.getFolderAPI()
						.find(structure.getFolder(), user, false);
				folders.addOrClean(structure.getFolder(),
						folder.getModDate()); // add the folder dependency
			}

			// Workflows Dependencies
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.WORKFLOW.getType())) {
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
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CATEGORY.getType())) {
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

					if (!dependencyProcessor.alreadyProcess(relationship.getChildStructureInode(), AssetTypes.CONTENT_TYPE) &&
							config.getOperation().equals(Operation.PUBLISH)) {
						final Structure childStructure = CacheLocator.getContentTypeCache()
								.getStructureByInode(relationship.getChildStructureInode());
						contentTypes
								.addOrClean(relationship.getChildStructureInode(), childStructure.getModDate());

						dependencyProcessor.put(relationship.getChildStructureInode(), AssetTypes.CONTENT_TYPE);
					}
					if (!dependencyProcessor.alreadyProcess(relationship.getParentStructureInode(), AssetTypes.CONTENT_TYPE) &&
							config.getOperation().equals(Operation.PUBLISH)) {
						Structure struct = CacheLocator.getContentTypeCache()
								.getStructureByInode(relationship.getParentStructureInode());

						contentTypes.addOrClean(relationship.getParentStructureInode(),
								struct.getModDate());

						dependencyProcessor.put(relationship.getParentStructureInode(), AssetTypes.CONTENT_TYPE);
					}
				}
			}
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void processContentDependency(final String contentId)
			throws  DotBundleException {

		try {
			final Identifier ident = APILocator.getIdentifierAPI().find(contentId);
			final List<Contentlet> contentList =
					APILocator.getContentletAPI().findAllVersions(ident, false, user, false);

			final Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
			final Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<Contentlet>();

			for (final Contentlet contentlet : contentList) {

				if (contentlet.isHTMLPage()) {
					processHTMLPagesDependency(contentlet.getIdentifier());
				}

				// Host Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
					final Host host = APILocator.getHostAPI().find(contentlet.getHost(), user, false);
					hosts.addOrClean(contentlet.getHost(), host.getModDate());
				}

				contentsToProcess.add(contentlet);

				// Relationships Dependencies
				if(publisherFilter.isRelationships()) {
					final Map<Relationship, List<Contentlet>> contentRelationshipsMap =
							APILocator.getContentletAPI().findContentRelationships(contentlet, user);

					for (final Relationship relationship : contentRelationshipsMap.keySet()) {
						contentsToProcess.addAll(contentRelationshipsMap.get(relationship));
						/**
						 * ISSUE #2222: https://github.com/dotCMS/dotCMS/issues/2222
						 *
						 * We need the relationships in which the single related content is involved.
						 *
						 */
						if (contentRelationshipsMap.get(relationship).size() > 0)
							relationships.addOrClean(relationship.getInode(), relationship.getModDate());
					}
				}
			}
			// end relationships false

			for (final Contentlet contentletToProcess : contentsToProcess) {
				// Host Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
					final Host host = APILocator.getHostAPI().find(contentletToProcess.getHost(), user, false);
					hosts.addOrClean(contentletToProcess.getHost(), host.getModDate());
				}
				contentsWithDependenciesToProcess.add(contentletToProcess);
				//Copy asset files to bundle folder keeping original folders structure
				final List<Field> fields=FieldsCache.getFieldsByStructureInode(contentletToProcess.getStructureInode());

				for(final Field field : fields) {
					if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
							|| field.getFieldType().equals(Field.FieldType.FILE.toString())) {

						try {
							String value = "";
							if(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(contentletToProcess, field))){
								value = APILocator.getContentletAPI().getFieldValue(contentletToProcess, field).toString();
							}
							final Identifier id = APILocator.getIdentifierAPI().find(value);
							if (InodeUtils.isSet(id.getInode()) && id.getAssetType().equals("contentlet")) {
								contentsWithDependenciesToProcess.addAll(APILocator.getContentletAPI().findAllVersions(id, false, user, false));
							}
						} catch (Exception ex) {
							Logger.debug(this, ex.toString());
							throw new DotStateException("Problem occured while publishing file:" +ex.getMessage(), ex );
						}
					}

				}
			}

			// Adding the Contents (including related) and adding filesAsContent
			for (final Contentlet contentletWithDependenciesToProcess : contentsWithDependenciesToProcess) {
				// Host Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
					final Host host = APILocator.getHostAPI().find(contentletWithDependenciesToProcess.getHost(), user, false);
					hosts.addOrClean(contentletWithDependenciesToProcess.getHost(), host.getModDate());
				}
				// Content Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType()) && !publisherFilter.doesExcludeDependencyQueryContainsContentletId(contentletWithDependenciesToProcess.getIdentifier())) {
					contents.addOrClean(contentletWithDependenciesToProcess.getIdentifier(),
							contentletWithDependenciesToProcess.getModDate());
				}
				// Folder Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.FOLDER.getType())) {
					final Folder folder = APILocator.getFolderAPI().find(contentletWithDependenciesToProcess.getFolder(), user, false);
					folders.addOrClean(contentletWithDependenciesToProcess.getFolder(), folder.getModDate()); // adding content folder
				}
				// Language Dependency
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.LANGUAGE.getType())) {
					languages.addOrClean(Long.toString(contentletWithDependenciesToProcess.getLanguageId()),
							new Date()); // will be included only when hasn't been sent ever
					dependencyProcessor.put(String.valueOf(contentletWithDependenciesToProcess.getLanguageId()),
							AssetTypes.LANGUAGES);
				}
				try {
					if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
							&& contentletWithDependenciesToProcess.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

						final Folder contFolder=APILocator.getFolderAPI().find(contentletWithDependenciesToProcess.getFolder(), user, false);
						final List<IHTMLPage> folderHtmlPages = new ArrayList<IHTMLPage>();
						folderHtmlPages.addAll(APILocator.getHTMLPageAssetAPI().getHTMLPages(contFolder, false, false, user, false));
						folderHtmlPages.addAll(APILocator.getHTMLPageAssetAPI().getHTMLPages(contFolder, true, false, user, false));

						for (final IHTMLPage htmlPage : folderHtmlPages) {

							Boolean mustBeIncluded;

							mustBeIncluded = contents.addOrClean(htmlPage.getIdentifier(), htmlPage.getModDate());


							if (mustBeIncluded) {
								processHTMLPagesDependency(htmlPage.getIdentifier());
							}

						}
					}
				} catch (Exception e) {
					Logger.debug(this, e.toString());
				}

				if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true) && !publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
					final Structure structure = CacheLocator.getContentTypeCache().getStructureByInode(contentletWithDependenciesToProcess.getStructureInode());
					contentTypes.addOrClean( contentletWithDependenciesToProcess.getStructureInode(), structure.getModDate());
					dependencyProcessor.put(contentletWithDependenciesToProcess.getStructureInode(), AssetTypes.CONTENT_TYPE);
				}

				// Evaluate all the categories from this contentlet to include as dependency.
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CATEGORY.getType())) {
					final List<Category> categoriesFromContentlet = APILocator.getCategoryAPI()
							.getParents(contentletWithDependenciesToProcess, APILocator.systemUser(), false);
					for (final Category category : categoriesFromContentlet) {
						categories.addOrClean(category.getCategoryId(), category.getModDate());
					}
				}
			}
		} catch (Exception e) {
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
					+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private void processLanguage(String lang) throws DotBundleException {
		try{
			final String keyValueQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE + " +languageId:" + lang;
			final List<Contentlet> listKeyValueLang = APILocator.getContentletAPI()
					.search(keyValueQuery,0, -1, StringPool.BLANK, user, false);// search for language variables
			// if there is any language variable and we accept to push content type, add the content type
			if (!listKeyValueLang.isEmpty() && !publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType())) {
				final Structure structure = CacheLocator.getContentTypeCache()
						.getStructureByInode(listKeyValueLang.get(0).getContentTypeId());
				contentTypes.addOrClean(structure.getIdentifier(), structure.getModDate());
			}
			if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType())) {
				for (final Contentlet keyValue : listKeyValueLang) {// add the language variable
					if(!publisherFilter.doesExcludeDependencyQueryContainsContentletId(keyValue.getIdentifier())) {
						contents.addOrClean(keyValue.getIdentifier(), keyValue.getModDate());
					}
				}
			}
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
	private void setRuleDependencies(final String ruleId)  {
		String ruleToProcess = "";
		final RulesAPI rulesAPI = APILocator.getRulesAPI();
		final HostAPI hostAPI = APILocator.getHostAPI();
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();


		try {
			final Rule rule = rulesAPI.getRuleById(ruleId, this.user, false);
			ruleToProcess = ruleId;
			final List<Contentlet> contentlets = contentletAPI.searchByIdentifier(
					"+identifier:" + rule.getParent(), 1, 0, null, this.user, false,
					PermissionAPI.PERMISSION_READ, true);

			if (contentlets != null && contentlets.size() > 0) {
				final Contentlet parent = contentlets.get(0);
				// If the parent of the rule is a Site...
				if (parent.isHost()) {
					if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType())) {
						final Host host = hostAPI.find(rule.getParent(), this.user, false);
						this.hosts.addOrClean(host.getIdentifier(), host.getModDate());
					}
				}
				// If the parent of the rule is a Content Page...
				else if (parent.isHTMLPage()) {
					if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType()) && !publisherFilter.doesExcludeDependencyQueryContainsContentletId(parent.getIdentifier())) {
						this.contents.addOrClean(parent.getIdentifier(), parent.getModDate());
					}
				} else {
					throw new DotDataException("The parent ID [" + parent.getIdentifier() + "] is a non-valid parent.");
				}
			} else {
				throw new DotDataException("The parent ID [" + rule.getParent() + "] cannot be found for Rule [" + rule.getId() + "]");
			}
		} catch (DotDataException e) {
			Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Dependencies for rule [" + ruleToProcess + "] could not be set: " + e.getMessage(), e);
		}
	}

	private void setLanguageVariables() {
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();
		final Date date = new Date();
		try{
			//We're no longer filtering by language here..
			//The reason is We're simply collecting all available lang variables so we can infer additional languages used. see #15359
			final String langVarsQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE ;
			final List<Contentlet> langVariables = contentletAPI.search(langVarsQuery, 0, -1, StringPool.BLANK, user, false);
			for(final Contentlet langVar : langVariables){
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType()) && !publisherFilter.doesExcludeDependencyQueryContainsContentletId(langVar.getIdentifier())) {
					contents.addOrClean(langVar.getIdentifier(), langVar.getModDate());
					dependencyProcessor.put(langVar.getIdentifier(), AssetTypes.CONTENTS);
				}
				//Collect the languages
				if(!publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.LANGUAGE.getType())) {
					languages.addOrClean(Long.toString(langVar.getLanguageId()), date);
				}
			}
		}catch (Exception e){
			Logger.error(this, e.getMessage(),e);
		}
	}

	@VisibleForTesting
	Set getContents() {
		return contents;
	}

	@VisibleForTesting
	DependencySet getRelationships() {
		return relationships;
	}

	@VisibleForTesting
	Set getTemplates() {
		return templates;
	}

	@VisibleForTesting
	Set getContainers() {
		return containers;
	}

	@VisibleForTesting
	Set getFolders() {
		return folders;
	}

	private class DependencyProcessorItem {
		String assetKey;
		AssetTypes assetTypes;

		public DependencyProcessorItem(String assetKey, AssetTypes assetTypes) {
			this.assetKey = assetKey;
			this.assetTypes = assetTypes;
		}
	}

	private class DependencyProcessor {
		private BlockingQueue<DependencyProcessorItem> queue;
		private Map<AssetTypes, Set<String>> assetsAlreadyProcessed;
		private boolean finished;
		private boolean started;
		private List<DependencyThread> threads;

		void put(final String assetKey, final AssetTypes assetType) {
			if (!started) {
				return;
			}

			if (assetsAlreadyProcessed != null && !this.alreadyProcess(assetKey, assetType)) {
				Logger.debug(DependencyProcessor.class, () -> String.format("%s: Putting %s in %s",
						Thread.currentThread().getName(), assetKey, assetType));

				queue.add(new DependencyProcessorItem(assetKey, assetType));
				addSet(assetKey, assetType);
			}
		}

		private synchronized void addSet(final String assetKey, final AssetTypes assetTypes) {
			Set<String> set = assetsAlreadyProcessed.get(assetTypes);

			if (set == null) {
				set = new HashSet<>();
				assetsAlreadyProcessed.put(assetTypes, set);
			}

			set.add(assetKey);
		}

		void start(){
			Logger.debug(DependencyProcessor.class, "starting");
			queue = new LinkedBlockingDeque();
			assetsAlreadyProcessed = new HashMap<>();

			int poolSize = Config.getIntProperty("NUMBER_THREAD_TO_EXECUTE_BUNDLER", 10);

			threads = new ArrayList<>();

			for (int i = 0; i < poolSize; i++) {
				final DependencyThread thread = new DependencyThread();
				threads.add(thread);

				Logger.debug(DependencyProcessor.class, () -> String.format("Starting %s", thread.getName()));
				thread.start();
			}

			started = true;
		}

		public void waitUntilFinish(){
			Logger.debug(DependencyProcessor.class, () -> String.format("%s: Wait until all finish",
					Thread.currentThread().getName()));

			while(!isFinish()) {
				Logger.debug(DependencyProcessor.class, () -> String.format("%s: it is not finish yet",
						Thread.currentThread().getName()));
				waitForDependencies();
			}

			Logger.debug(DependencyProcessor.class, () -> String.format("%s: it is finish",
					Thread.currentThread().getName()));
			this.stop();
		}

		private synchronized void waitForDependencies() {
			try {
				wait(TimeUnit.MINUTES.toMillis(1));
			} catch (InterruptedException e) {
				Logger.error(DependencyProcessor.class, e.getMessage());
			}
		}

		private synchronized void notifyMainThread() {
			notify();
		}

		private boolean isFinish(){
			return queue == null || (queue.isEmpty() && allThreadWaiting());
		}

		private boolean allThreadWaiting() {
			for (final DependencyThread thread : threads) {
				if (!thread.getWaitingState()) {
					return false;
				}
			}

			return true;
		}

		void stop(){
			this.finished = true;

			if (threads != null) {
				for (final Thread thread : threads) {
					thread.interrupt();
				}
			}
		}

		public boolean alreadyProcess(final String assetKey, final AssetTypes assetTypes) {
			final Set<String> set = assetsAlreadyProcessed.get(assetTypes);
			return set != null && set.contains(assetKey);
		}

		private class DependencyThread extends Thread {
			boolean waitingForSomethingToProcess = false;

			@Override
			public void run() {
				while (true) {
					try {
						DependencyProcessorItem dependencyProcessorItem = queue.poll();

						if (dependencyProcessorItem == null) {
							setWaitingState(true);
							Logger.debug(DependencyProcessor.class, () -> String.format("%s : Notifying to main thread",
									Thread.currentThread().getName()));

							notifyMainThread();

							Logger.debug(DependencyProcessor.class,
									() -> String.format("%s : Waiting for something to process", Thread.currentThread().getName()));
							dependencyProcessorItem = queue.take();
							setWaitingState(false);
						}

						final AssetTypes assetTypes = dependencyProcessorItem.assetTypes;
						Logger.debug(DependencyProcessor.class,
								String.format("%s : We have something to process - %s %s",
										Thread.currentThread().getName(), dependencyProcessorItem.assetKey, assetTypes));

						consumerDependencies.get(assetTypes).accept(dependencyProcessorItem.assetKey);
					} catch (InterruptedException e) {
						if (!finished) {
							Logger.error(DependencyProcessor.class, e.getMessage());
						}

						//todo: Remove when JAspect is running in test
						DbConnectionFactory.closeSilently();

						setWaitingState(false);
						break;
					} catch (Exception e) {
						Logger.error(DependencyThread.class, e);
					}
				}
			}

			private synchronized void setWaitingState(boolean value) {
				waitingForSomethingToProcess = value;
			}

			private boolean getWaitingState() {
				return waitingForSomethingToProcess;
			}
		}
	}
}
