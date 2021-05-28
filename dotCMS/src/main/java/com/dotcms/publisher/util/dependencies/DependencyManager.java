package com.dotcms.publisher.util.dependencies;

import static com.dotcms.util.CollectionsUtils.set;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
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
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

	private DependencyModDateUtil dependencyModDateUtil;
	private PushedAssetUtil pushedAssetUtil;
	private User user;

	private PushPublisherConfig config;
	private ConcurrentDependencyProcessor dependencyProcessor;

	@FunctionalInterface
	public interface SupplierWithException<T> {
		T get() throws DotDataException, DotSecurityException;
	}

	/**
	 * Initializes the list of dependencies that this manager needs to satisfy, based on the {@link
	 * PushPublisherConfig} specified for the bundle.
	 *
	 * @param user   - The user requesting the generation of the bundle.
	 * @param config - The main configuration values for the bundle we are trying to create.
	 */
	public DependencyManager(User user, PushPublisherConfig config) {
		this.config = config;
		dependencyModDateUtil = new DependencyModDateUtil(this.config);
		pushedAssetUtil = new PushedAssetUtil(config);

		// these ones store the assets that will be sent in the bundle
		dependencyModDateUtil = new DependencyModDateUtil(config);
		dependencyProcessor = new ConcurrentDependencyProcessor();
		this.config.setDependencyProcessor(dependencyProcessor);
		this.user = user;

		dependencyProcessor.addProcessor(PusheableAsset.SITE, (host) ->
				proccessHostDependency((Host) host));

		dependencyProcessor.addProcessor(PusheableAsset.FOLDER, (folder) ->
				processFolderDependency((Folder) folder));

		dependencyProcessor.addProcessor(PusheableAsset.TEMPLATE,
				(template) -> processTemplateDependencies((Template) template));

		dependencyProcessor.addProcessor(PusheableAsset.CONTAINER,
				(container) -> processContainerDependency((Container) container));

		dependencyProcessor.addProcessor(PusheableAsset.CONTENT_TYPE,
				(contentType) -> processContentTypeDependency((Structure) contentType));

		dependencyProcessor.addProcessor(PusheableAsset.LINK,
				(link) -> processLinkDependency((Link) link));

		dependencyProcessor.addProcessor(PusheableAsset.CONTENTLET, (content) -> {
			try {
				processContentDependency((Contentlet) content);
			} catch (DotBundleException e) {
				Logger.error(DependencyManager.class, e.getMessage());
				throw new DotRuntimeException(e);
			}
		});
		dependencyProcessor.addProcessor(PusheableAsset.RULE, (rule) -> setRuleDependencies((Rule) rule));
		dependencyProcessor.addProcessor(PusheableAsset.LANGUAGE, (lang) -> {
			try {
				processLanguage((Language) lang);
			} catch (DotBundleException e) {
				Logger.error(DependencyManager.class, e.getMessage());
				throw new DotRuntimeException(e);
			}
		});
		dependencyProcessor.addProcessor(PusheableAsset.RELATIONSHIP,
				(relationship) -> processRelationshipDependencies((Relationship) relationship));

	}

	/**
	 * Initializes the main mechanism to search for dependencies. It starts with the identification
	 * of the type of assets in the queue, and their dependencies will be searched and found base on
	 * those types.
	 *
	 * @throws DotSecurityException The specified user does not have the required permissions to
	 *                              perform the action.
	 * @throws DotDataException     An error occurred when retrieving information from the
	 *                              database.
	 * @throws DotBundleException   An error occurred when generating the bundle data.
	 */
	public void setDependencies()
			throws DotSecurityException, DotDataException, DotBundleException {
		this.publisherFilter = APILocator.getPublisherAPI().createPublisherFilter(config.getId());

		Logger.debug(DependencyManager.class,
				"publisherFilter.isDependencies() " + publisherFilter.isDependencies());

		if (publisherFilter.isDependencies()) {
			setLanguageVariables();
		}

		List<PublishQueueElement> assets = config.getAssets();

		Logger.debug(this, publisherFilter.toString());
		for (PublishQueueElement asset : assets) {
			//Check if the asset.Type is in the excludeClasses filter, if it is, the asset is not added to the bundle
			if (publisherFilter.doesExcludeClassesContainsType(asset.getType())) {
				Logger.debug(getClass(), "Asset Id: " + asset.getAsset()
						+ " will not be added to the bundle since it's type: " + asset.getType()
						+ " must be excluded according to the filter");
				continue;
			}

			if (asset.getType().equals(PusheableAsset.CONTENT_TYPE.getType())) {
				try {
					Structure st = CacheLocator.getContentTypeCache()
							.getStructureByInode(asset.getAsset());

					if (st == null) {
						Logger.warn(getClass(),
								"Structure id: " + (asset.getAsset() != null ? asset.getAsset()
										: "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						config.addWithDependencies(st, PusheableAsset.CONTENT_TYPE);
					}

				} catch (Exception e) {
					Logger.error(getClass(),
							"Couldn't add the Structure to the Bundle. Bundle ID: " + config.getId()
									+ ", Structure ID: " + asset.getAsset(), e);
				}

			} else if (asset.getType().equals(PusheableAsset.TEMPLATE.getType())) {
				try {
					Template template = APILocator.getTemplateAPI()
							.findLiveTemplate(asset.getAsset(), user, false);

					if (template == null || !UtilMethods.isSet(template.getIdentifier())) {
						template = APILocator.getTemplateAPI()
								.findWorkingTemplate(asset.getAsset(), user, false);
					}
					if (template == null || !UtilMethods.isSet(template.getIdentifier())) {
						Logger.warn(getClass(),
								"Template id: " + (asset.getAsset() != null ? asset.getAsset()
										: "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						if (template instanceof FileAssetTemplate) {
							Logger.debug(getClass(),
									"FileAssetTemplate id: " + (asset.getAsset() != null ? asset
											.getAsset() : "N/A") + " will be ignored");
						} else {
							config.addWithDependencies(template, PusheableAsset.TEMPLATE);
						}
					}

				} catch (Exception e) {
					Logger.error(getClass(),
							"Couldn't add the Template to the Bundle. Bundle ID: " + config.getId()
									+ ", Template ID: " + asset.getAsset(), e);
				}
			} else if (asset.getType().equals(PusheableAsset.CONTAINER.getType())) {
				try {
					Container container = (Container) APILocator.getVersionableAPI()
							.findLiveVersion(asset.getAsset(), user, false);

					if (container == null) {
						container = APILocator.getContainerAPI()
								.getWorkingContainerById(asset.getAsset(), user, false);
					}

					if (container == null) {
						Logger.warn(getClass(),
								"Container id: " + (asset.getAsset() != null ? asset.getAsset()
										: "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						config.addWithDependencies(container, PusheableAsset.FOLDER);
					}
				} catch (DotSecurityException e) {
					Logger.error(getClass(),
							"Couldn't add the Container to the Bundle. Bundle ID: " + config.getId()
									+ ", Container ID: " + asset.getAsset(), e);
				}
			} else if (asset.getType().equals(PusheableAsset.FOLDER.getType())) {
				try {
					Folder folder = APILocator.getFolderAPI().find(asset.getAsset(), user, false);

					if (folder == null) {
						Logger.warn(getClass(),
								"Folder id: " + (asset.getAsset() != null ? asset.getAsset()
										: "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						config.addWithDependencies(folder, PusheableAsset.FOLDER);
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(),
							"Couldn't add the Folder to the Bundle. Bundle ID: " + config.getId()
									+ ", Folder ID: " + asset.getAsset(), e);
				}
			} else if (asset.getType().equals(PusheableAsset.SITE.getType())) {
				try {
					Host host = APILocator.getHostAPI().find(asset.getAsset(), user, false);

					if (host == null) {
						Logger.warn(getClass(),
								"Host id: " + (asset.getAsset() != null ? asset.getAsset() : "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						config.addWithDependencies(host, PusheableAsset.SITE);
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(),
							"Couldn't add the Host to the Bundle. Bundle ID: " + config.getId()
									+ ", Host ID: " + asset.getAsset(), e);
				}
			} else if (asset.getType().equals(PusheableAsset.LINK.getType())) {
				try {
					Link link = (Link) APILocator.getVersionableAPI()
							.findLiveVersion(asset.getAsset(), user, false);

					if (link == null || !InodeUtils.isSet(link.getInode())) {
						link = APILocator.getMenuLinkAPI()
								.findWorkingLinkById(asset.getAsset(), user, false);
					}

					if (link == null || !InodeUtils.isSet(link.getInode())) {
						Logger.warn(getClass(),
								"Link id: " + (asset.getAsset() != null ? asset.getAsset() : "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						config.addWithDependencies(link, PusheableAsset.LINK);
					}

				} catch (DotSecurityException e) {
					Logger.error(getClass(),
							"Couldn't add the Host to the Bundle. Bundle ID: " + config.getId()
									+ ", Host ID: " + asset.getAsset(), e);
				}
			} else if (asset.getType().equals(PusheableAsset.WORKFLOW.getType())) {
				WorkflowScheme scheme = APILocator.getWorkflowAPI().findScheme(asset.getAsset());

				if (scheme == null) {
					Logger.warn(getClass(),
							"WorkflowScheme id: " + (asset.getAsset() != null ? asset.getAsset()
									: "N/A")
									+ " does NOT have working or live version, not Pushed");
				} else {
					config.addWithDependencies(scheme, PusheableAsset.WORKFLOW);
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
					config.addWithDependencies(language, PusheableAsset.LANGUAGE);
				}
			} else if (asset.getType().equals(PusheableAsset.RULE.getType())) {
				Rule rule = APILocator.getRulesAPI()
						.getRuleById(asset.getAsset(), user, false);
				if (rule != null && StringUtils.isNotBlank(rule.getId())) {
					config.addWithDependencies(rule, PusheableAsset.RULE);
				} else {
					Logger.warn(getClass(), "Rule id: "
							+ (asset.getAsset() != null ? asset.getAsset()
							: "N/A")
							+ " is not present in the database, not Pushed.");
				}
			}
		}

		if (UtilMethods.isSet(config.getLuceneQueries())) {
			List<String> contentIds = PublisherUtil.getContentIds(config.getLuceneQueries());
			contentIds.removeIf(c -> publisherFilter.doesExcludeQueryContainsContentletId(c));

			for (String id : contentIds) {
				final Identifier ident = APILocator.getIdentifierAPI().find(id);
				final List<Contentlet> contentlets = APILocator.getContentletAPI()
						.findAllVersions(ident, false, user, false);
				for (Contentlet con : contentlets) {
					config.addWithDependencies(con, PusheableAsset.CONTENTLET);
				}
			}
		}

		try {
			if (publisherFilter.isDependencies()) {
				config.waitUntilResolveAllDependencies();
			}
		} catch (ExecutionException e) {
			final Optional<Throwable> causeOptional = ExceptionUtil
					.getCause(e, set(DotBundleException.class));

			if (causeOptional.isPresent()) {
				throw (DotBundleException) causeOptional.get();
			} else {
				final Throwable rootCause = ExceptionUtil.getRootCause(e);
				throw new DotBundleException(rootCause.getMessage(), (Exception) rootCause);
			}
		}

		System.out.println("assets = " + assets);
	}

	private Folder getFolderByParentIdentifier(final Identifier identifier)
			throws DotDataException, DotSecurityException {

		return APILocator.getFolderAPI()
				.findFolderByPath(identifier.getParentPath(), identifier.getHostId(), user,
						false);
	}

	private Host getHostById(final String hostId)
			throws DotDataException, DotSecurityException {

		return APILocator.getHostAPI().find(hostId, user, false);
	}

	private List<Contentlet> getContentletsByLink(final String linkId)
			throws DotDataException, DotSecurityException {

		final Link link = APILocator.getMenuLinkAPI()
				.findWorkingLinkById(linkId, user, false);

		if (link != null) {

			if (link.getLinkType().equals(Link.LinkType.INTERNAL.toString())) {
				final Identifier id = APILocator.getIdentifierAPI()
						.find(link.getInternalLinkIdentifier());

				// add file/content dependencies. will also work with htmlpages as content
				if (InodeUtils.isSet(id.getInode()) && id.getAssetType()
						.equals("contentlet")) {
					return APILocator.getContentletAPI()
									.search("+identifier:" + id.getId(), 0, 0, "moddate", user,
											false);
				}
			}
		}

		return null;
	}

	private List<Contentlet> getContentletByLuceneQuery(final String luceneQuery)
			throws DotDataException, DotSecurityException {
		return APILocator.getContentletAPI()
				.search(luceneQuery, 0, 0, null, user, false);
	}

	private List<Template> getTemplatesByHost(final Host host)
			throws DotDataException, DotSecurityException {
		return APILocator.getTemplateAPI().findTemplatesAssignedTo(host);
	}

	private List<Rule> getRulesByHost(final Host host)
			throws DotDataException, DotSecurityException {
		return APILocator.getRulesAPI().getAllRulesByParent(host, user, false);
	}

	private List<Folder> getFoldersByHost(final Host host)
			throws DotDataException, DotSecurityException {
		return APILocator.getFolderAPI()
				.findFoldersByHost(host, user, false);
	}

	private List<Structure> getContentTypeByHost(final Host host)
			throws DotDataException, DotSecurityException {
		return StructureFactory
				.getStructuresUnderHost(host, user, false);
	}

	private List<Structure> getContentTypeByFolder(final Folder folder)
			throws DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().getStructures(folder, user, false);
	}

	private List<Container> getContainersByHost(final Host host) throws DotDataException {
		return APILocator.getContainerAPI().findContainersUnder(host);
	}

	private List<FileAssetContainer> getFileContainersByHost(final Host host)
			throws DotDataException {
		return getContainersByHost(host).stream()
				.filter(FileAssetContainer.class::isInstance)
				.map(FileAssetContainer.class::cast)
				.collect(Collectors.toList());
	}

	private Folder getParentFolder(Folder folder) throws DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().findParentFolder(folder, user, false);
	}

	private List<Link> getLinksByFolder(Folder folder)
			throws DotDataException, DotSecurityException {
		return APILocator.getMenuLinkAPI().findFolderMenuLinks(folder);
	}

	private Identifier findIdentifier(final String id) {
		try {
			return APILocator.getIdentifierAPI().find(id);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	private List<Contentlet> findContentletsByIdentifier(final Identifier identifier) {
		try {
			return APILocator.getContentletAPI()
					.findAllVersions(identifier, false, user, false);
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	private List<Contentlet> getContentletsByPage(final IHTMLPage page)
			throws DotDataException, DotSecurityException {

		final Table<String, String, Set<PersonalizedContentlet>> pageMultiTrees =
				APILocator.getMultiTreeAPI().getPageMultiTrees(page, false);

		return pageMultiTrees.values().stream()
				.flatMap(personalizedContentlets -> personalizedContentlets.stream())
				.map(personalizedContentlet -> personalizedContentlet.getContentletId())
				.filter(Objects::isNull)
				.map(contentletId -> findIdentifier(contentletId))
				.filter(Objects::isNull)
				.flatMap(identifier -> findContentletsByIdentifier(identifier).stream())
				.collect(Collectors.toList());
	}

	private List<Rule> getRuleByPage(final IHTMLPage page)
			throws DotDataException, DotSecurityException {
		return APILocator.getRulesAPI().getAllRulesByParent(page, user, false);
	}

	private Host getHostByTemplate(final Template template)
			throws DotDataException, DotSecurityException {
		return APILocator.getHostAPI()
				.find(APILocator.getTemplateAPI().getTemplateHost(template)
						.getIdentifier(), user, false);
	}

	private List<Container> getContainerByTemplate(final Template template)
			throws DotDataException, DotSecurityException {
		return APILocator.getTemplateAPI()
				.getContainersInTemplate(template, user, false);
	}

	private Optional<Folder> getThemeByTemplate(final Template template)
			throws DotDataException, DotSecurityException {

			final Folder themeFolder = APILocator.getFolderAPI()
					.find(template.getTheme(), user, false);

			return themeFolder != null && InodeUtils.isSet(themeFolder.getInode()) ?
					Optional.of(themeFolder) : Optional.empty();
	}

	private Host getHostByContainer(final Container container)
			throws DotDataException, DotSecurityException {
		return APILocator.getContainerAPI().getParentHost(container, user, false);
	}

	private Collection<Structure> getContentTypeByLiveContainer(final String containerId)
			throws DotDataException, DotSecurityException {
		return getContentTypeByContainer(containerId, true);
	}

	private Collection<Structure> getContentTypeByWorkingContainer(final String containerId)
			throws DotDataException, DotSecurityException {
		return getContentTypeByContainer(containerId, false);
	}

	private Collection<Structure> getContentTypeByContainer(final String containerId, final boolean live)
			throws DotDataException, DotSecurityException {

		final Container container = live ? APILocator.getContainerAPI()
				.getLiveContainerById(containerId, user, false)
				: APILocator.getContainerAPI()
						.getWorkingContainerById(containerId, user, false);

		if (container == null) {
			return Collections.emptyList();
		}

		return APILocator.getContainerAPI().getContainerStructures(container).stream()
				.map(containerStructure ->
						CacheLocator.getContentTypeCache()
								.getStructureByInode(containerStructure.getStructureId()))
				.collect(Collectors.toSet());
	}

	private Folder getFolderById(final String id) throws DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().find(id, user, false);
	}

	/**
	 * Given that a FileAssetTemplate is defined by a bunch of files we need to collect the folder that enclose'em
	 */
	private Optional<Folder> getFileAssetTemplateRootFolder(final FileAssetTemplate fileAssetTemplate)
			throws DotDataException, DotSecurityException {
		try {
			final String path = fileAssetTemplate.getPath();
			return Optional.of(
					APILocator.getFolderAPI()
						.findFolderByPath(path, fileAssetTemplate.getHost(), user, false)
			);
		}catch (DotSecurityException | DotDataException e) {
			Logger.error(DependencyManager.class, "Error Collecting the Folder of the File Asset Template: " + fileAssetTemplate.getIdentifier(), e);
			return Optional.empty();
		}
	}

	private Collection<WorkflowScheme> getWorkflowSchemasByContentType(final Structure structure){
		try{
			return APILocator.getWorkflowAPI().findSchemesForStruct(structure);
		} catch (DotDataException e) {
			Logger.debug(getClass(),
					() -> "Could not get the Workflow Scheme Dependency for Structure ID: "
							+ structure.getInode());
			return null;
		}
	}

	public <T> boolean isExcludeByFilter(
			final PusheableAsset pusheableAsset) {

		return (PusheableAsset.RELATIONSHIP == pusheableAsset && !publisherFilter.isRelationships()) ||
				publisherFilter.doesExcludeDependencyClassesContainsType(pusheableAsset.getType());
	}

	public <T> void tryToAddAllAndProcessDependencies(
			final PusheableAsset pusheableAsset, final SupplierWithException<Collection<T>> getter)
			throws DotDataException, DotSecurityException {

		tryToAddAll(pusheableAsset, getter).stream()
				.forEach(asset -> config.addWithDependencies(asset, pusheableAsset));
	}

	public <T> Collection<T> tryToAddAll(
			final PusheableAsset pusheableAsset, final SupplierWithException<Collection<T>> getter)
			throws DotDataException, DotSecurityException {

		if (!isExcludeByFilter(pusheableAsset)) {
			final Collection<T> assets = getter.get();

			if (assets != null) {
				assets.stream()
					.filter(asset -> add(pusheableAsset, asset))
					.collect(Collectors.toSet());
			}
		}

		return Collections.emptySet();
	}

	public <T> void tryToAddAndProcessDependencies(
			final PusheableAsset pusheableAsset, final SupplierWithException<T> getter)
			throws DotDataException, DotSecurityException {
		tryToAdd(pusheableAsset, getter).ifPresent((asset) ->
				config.addWithDependencies(asset, pusheableAsset));
	}

	public <T> Optional<T> tryToAdd(
			final PusheableAsset pusheableAsset, final SupplierWithException<T> getter)
			throws DotDataException, DotSecurityException {

		if (!isExcludeByFilter(pusheableAsset)) {
			final T asset = getter.get();

			if (asset != null) {
				return add(pusheableAsset, asset) ? Optional.of(asset) : Optional.empty();
			}
		}

		return Optional.empty();
	}

	private <T> boolean add(final PusheableAsset pusheableAsset, final T asset) {
		if (Contentlet.class.isInstance(asset) && !Contentlet.class.cast(asset).isHost() &&
				!publisherFilter.doesExcludeDependencyQueryContainsContentletId(
						((Contentlet) asset).getIdentifier())) {
			return false;
		}

		if (!dependencyModDateUtil.excludeByModDate(asset, pusheableAsset)) {
			config.add(asset, pusheableAsset);
			pushedAssetUtil.savePushedAssetForAllEnv(asset, pusheableAsset);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * For given Links adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Folders</li>
	 * </ul>
	 */
	private void processLinkDependency(final Link link)  {
		final String linkId = link.getIdentifier();

		try {
			Identifier ident = APILocator.getIdentifierAPI().find(linkId);

			// Folder Dependencies
			tryToAdd(PusheableAsset.FOLDER, () -> Optional.of(getFolderByParentIdentifier(ident)));

			// Host Dependencies
			tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(ident.getHostId())));

			// Content Dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET, () -> getContentletsByLink(linkId));
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
	private void proccessHostDependency (final Host host) {
		try {
			// Template dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.TEMPLATE,
					() -> getTemplatesByHost(host));

			// Container dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER,
					() -> getContainersByHost(host));
			getFileContainersByHost(host).stream()
					.forEach(fileContainer -> dependencyProcessor.addAsset(fileContainer.getIdentifier(),
							PusheableAsset.CONTAINER));

			// Content dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
					() -> getContentletByLuceneQuery("+conHost:" + host.getIdentifier()));

			// Structure dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> getContentTypeByHost(host));

			// Folder dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> getFoldersByHost(host));

			// Rule dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.RULE,
					() -> getRulesByHost(host));

		} catch (DotSecurityException | DotDataException e) {
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
	private void processFolderDependency(final Folder folder) {
		try {
			tryToAdd(PusheableAsset.FOLDER, () -> Optional.of(getParentFolder(folder)));
			setFolderListDependencies(folder);
		} catch (DotSecurityException | DotDataException e) {
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

		tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(folder.getHostId())));

		// Content dependencies
		tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
				() -> getContentletByLuceneQuery("+conFolder:" + folder.getInode()));

		// Menu Link dependencies
		tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET,
				() -> getLinksByFolder(folder));

		// Structure dependencies
		tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
				() -> getContentTypeByFolder(folder));

		//Add the default structure of this folder
		tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
				() -> getContentTypeByFolder(folder));

		// SubFolders
		APILocator.getFolderAPI().findSubFolders(folder, user, false).stream()
				.forEach(subFolder -> dependencyProcessor.addAsset(subFolder, PusheableAsset.FOLDER));
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
			tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(identifier.getHostId())));

			// Folder dependencies
			tryToAdd(PusheableAsset.FOLDER, () -> Optional.of(getFolderByParentIdentifier(identifier)));

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
			final Template workingTemplateWP = workingPage != null ?
					APILocator.getTemplateAPI()
							.findWorkingTemplate(workingPage.getTemplateId(), user, false) : null;

			if (workingTemplateWP != null) {

				// Templates dependencies
				if(!(workingTemplateWP instanceof FileAssetTemplate)) {
					tryToAdd(PusheableAsset.TEMPLATE, () -> Optional.of(workingTemplateWP));
				}
				dependencyProcessor.addAsset(workingPage.getTemplateId(), PusheableAsset.TEMPLATE);
			}

			final Template liveTemplateLP = livePage != null ?
					APILocator.getTemplateAPI()
							.findLiveTemplate(livePage.getTemplateId(), user, false) : null;

			// Templates dependencies
			if (liveTemplateLP != null ) {
				if(!(liveTemplateLP instanceof FileAssetTemplate)) {
					tryToAdd(PusheableAsset.TEMPLATE, () -> Optional.of(liveTemplateLP));
				}
				dependencyProcessor.addAsset(livePage.getTemplateId(), PusheableAsset.TEMPLATE);
			}

			// Contents dependencies
			tryToAddAndProcessDependencies(PusheableAsset.CONTENTLET, () -> Optional.of(getContentletsByPage(workingPage)));


			// Rule dependencies
			tryToAddAndProcessDependencies(PusheableAsset.RULE, () -> Optional.of(getRuleByPage(workingPage)));
		} catch (DotSecurityException | DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	private void processTemplateDependencies(final Template template) {

		try {

			final Template workingTemplate = template.isWorking() ? template
			: APILocator.getTemplateAPI().findWorkingTemplate(template.getIdentifier(), user, false);
			final Template liveTemplate = template.isLive() ? template :
					APILocator.getTemplateAPI().findLiveTemplate(template.getIdentifier(), user, false);

			// Host dependency
			tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostByTemplate(workingTemplate)));

			addContainerByTemplate(workingTemplate);
			addContainerByTemplate(liveTemplate);

			tryToAddAndProcessDependencies(PusheableAsset.FOLDER, () -> getThemeByTemplate(workingTemplate));

			if(workingTemplate instanceof FileAssetTemplate){
				//Process FileAssetTemplate
				tryToAddAndProcessDependencies(PusheableAsset.FOLDER, () ->
						getFileAssetTemplateRootFolder(FileAssetTemplate.class.cast(workingTemplate)));
			}
		} catch (DotSecurityException | DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	private void addContainerByTemplate(Template workingTemplate) throws DotDataException, DotSecurityException {
		final List<Container> containerByTemplate = getContainerByTemplate(workingTemplate);

		tryToAddAllAndProcessDependencies(PusheableAsset.CONTAINER, () ->
					containerByTemplate.stream()
						.filter(FileAssetContainer.class::isInstance)
						.collect(Collectors.toList())
		);

		containerByTemplate.stream()
			.filter(container -> !FileAssetContainer.class.isInstance(container))
			.forEach(container -> dependencyProcessor.addAsset(container.getIdentifier(),
					PusheableAsset.CONTAINER));
	}



	/**
	 * For given Containers adds its dependencies:
	 * <ul>
	 * <li>Hosts</li>
	 * <li>Structures</li>
	 * </ul>
	 */
	private void processContainerDependency(final Container container)  {

		try {
			final String containerId = container.getInode();
			final Container containerById = APILocator.getContainerAPI()
					.getWorkingContainerById(containerId, user, false);

			// Host Dependency
			tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostByContainer(containerById)));

			// Content Type Dependencies
			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> getContentTypeByWorkingContainer(containerId));

			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> getContentTypeByLiveContainer(containerId));

			if (containerById instanceof FileAssetContainer ) {
				// Process FileAssetContainer
				tryToAddAndProcessDependencies(PusheableAsset.FOLDER, () ->
						getFileAssetContainerRootFolder(FileAssetContainer.class.cast(containerById)));
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
	private Optional<Folder> getFileAssetContainerRootFolder(final FileAssetContainer fileAssetContainer) {
		try {
			final String path = fileAssetContainer.getPath();
			return Optional.of(
					APILocator.getFolderAPI()
						.findFolderByPath(path, fileAssetContainer.getHost(), user, false)
			);
		}catch (DotSecurityException | DotDataException e) {
			Logger.error(DependencyManager.class, e);
			return Optional.empty();
		}
	}


	public void processRelationshipDependencies(final Relationship relationship) {

		try {
			tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> Optional.of(CacheLocator.getContentTypeCache()
							.getStructureByInode(relationship.getChildStructureInode())));

			tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE,
					() -> Optional.of(CacheLocator.getContentTypeCache()
							.getStructureByInode(relationship.getParentStructureInode())));
		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void processContentTypeDependency(final Structure structure) {
		try{
			// Host Dependency
			tryToAdd(PusheableAsset.SITE, () -> getHostById(structure.getHost()));

			// Folder Dependencies
			tryToAdd(PusheableAsset.FOLDER, () -> getFolderById(structure.getFolder()));

			// Workflows Dependencies
			tryToAddAll(PusheableAsset.WORKFLOW, () -> getWorkflowSchemasByContentType(structure));

			// Categories Dependencies
			tryToAddAll(PusheableAsset.CATEGORY, () -> APILocator.getCategoryAPI()
							.findCategories(new StructureTransformer(structure).from(), user));

			// Related structures
			tryToAddAllAndProcessDependencies(PusheableAsset.RELATIONSHIP, () ->
					APILocator.getRelationshipAPI().byContentType(structure));

		} catch (DotDataException | DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		}
	}

	/**
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void processContentDependency(final Contentlet contentlet)
			throws  DotBundleException {

		try {
			final String contentId = contentlet.getIdentifier();
			final Identifier ident = APILocator.getIdentifierAPI().find(contentId);
			final List<Contentlet> contentList =
					APILocator.getContentletAPI().findAllVersions(ident, false, user, false);

			final Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
			final Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<Contentlet>();

			for (final Contentlet contentletVersion : contentList) {

				if (contentletVersion.isHTMLPage()) {
					processHTMLPagesDependency(contentletVersion.getIdentifier());
				}

				// Host Dependency
				tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(contentletVersion.getHost())));

				contentsToProcess.add(contentletVersion);

				// Relationships Dependencies
				final Map<Relationship, List<Contentlet>> contentRelationships = APILocator
						.getContentletAPI().findContentRelationships(contentletVersion, user);

				tryToAddAndProcessDependencies(PusheableAsset.RELATIONSHIP, () ->
						Optional.of(contentRelationships.keySet()));

				contentRelationships.values().stream()
						.forEach(contentlets -> contentsToProcess.addAll(contentlets));

			}

			for (final Contentlet contentletToProcess : contentsToProcess) {
				// Host Dependency
				tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(contentletToProcess.getHost())));

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
				tryToAdd(PusheableAsset.SITE, () -> Optional.of(getHostById(contentletWithDependenciesToProcess.getHost())));

				// Content Dependency
				tryToAdd(PusheableAsset.CONTENTLET, () -> Optional.of(contentletWithDependenciesToProcess));

				// Folder Dependency
				tryToAdd(PusheableAsset.FOLDER, () -> Optional.of(getFolderById(contentletWithDependenciesToProcess.getFolder())));

				// Language Dependency
				tryToAddAndProcessDependencies(PusheableAsset.LANGUAGE, () -> Optional.of(
						APILocator.getLanguageAPI().getLanguage(contentletWithDependenciesToProcess.getLanguageId())
				));

				try {
					if (Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES", false)
							&& contentletWithDependenciesToProcess.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {

						final Folder contFolder=APILocator.getFolderAPI()
								.find(contentletWithDependenciesToProcess.getFolder(), user, false);

						tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET, () -> {
							final List<IHTMLPage> liveHtmlPages = APILocator.getHTMLPageAssetAPI()
									.getHTMLPages(contFolder, false, false, user, false);

							final List<IHTMLPage> workingHtmlPages = APILocator.getHTMLPageAssetAPI()
									.getHTMLPages(contFolder, true, false, user, false);

							return Stream.concat(liveHtmlPages.stream(), workingHtmlPages.stream())
										.collect(Collectors.toSet());
						});
					}
				} catch (Exception e) {
					Logger.debug(this, e.toString());
				}

				if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true)) {
					tryToAddAndProcessDependencies(PusheableAsset.CONTENT_TYPE, () ->
							Optional.of(
								CacheLocator.getContentTypeCache()
										.getStructureByInode(contentletWithDependenciesToProcess.getStructureInode())
							));

				}

				// Evaluate all the categories from this contentlet to include as dependency.
				tryToAddAll(PusheableAsset.CATEGORY, () ->
						APILocator.getCategoryAPI()
								.getParents(contentletWithDependenciesToProcess, APILocator.systemUser(), false)
				);

			}
		} catch (Exception e) {
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
					+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	private void processLanguage(final Language language) throws DotBundleException {
		try{
			final long lang = language.getId();
			final String keyValueQuery = "+contentType:" + LanguageVariableAPI.LANGUAGEVARIABLE + " +languageId:" + lang;
			final List<Contentlet> listKeyValueLang = APILocator.getContentletAPI()
					.search(keyValueQuery,0, -1, StringPool.BLANK, user, false);
			tryToAddAll(PusheableAsset.CONTENTLET, () -> listKeyValueLang);

			final String contentTypeId = listKeyValueLang.get(0).getContentTypeId();
			tryToAdd(PusheableAsset.CONTENT_TYPE, () ->
					Optional.of(CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId)));

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
	private void setRuleDependencies(final Rule rule)  {
		String ruleToProcess = "";
		final HostAPI hostAPI = APILocator.getHostAPI();
		final ContentletAPI contentletAPI = APILocator.getContentletAPI();

		try {
			ruleToProcess = rule.getId();
			final List<Contentlet> contentlets = contentletAPI.searchByIdentifier(
					"+identifier:" + rule.getParent(), 1, 0, null, this.user, false,
					PermissionAPI.PERMISSION_READ, true);

			tryToAddAll(PusheableAsset.CONTENTLET, () -> contentlets);

			if (contentlets != null && contentlets.size() > 0) {
				final Contentlet parent = contentlets.get(0);

				if (parent.isHost()) {
					tryToAdd(PusheableAsset.SITE, () -> Optional.of(hostAPI.find(rule.getParent(), this.user, false)));
				} else if (parent.isHTMLPage()) {
					tryToAdd(PusheableAsset.CONTENTLET, () -> Optional.of(parent));
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

			tryToAddAllAndProcessDependencies(PusheableAsset.CONTENTLET, () -> langVariables);
		}catch (Exception e){
			Logger.error(this, e.getMessage(),e);
		}
	}

	@VisibleForTesting
	public Set getContents() {
		return config.getContentlets();
	}

	@VisibleForTesting
	public Set getRelationships() {
		return config.getRelationships();
	}

	@VisibleForTesting
	public Set getTemplates() {
		return config.getTemplates();
	}

	@VisibleForTesting
	public Set getContainers() {
		return config.getContainers();
	}

	@VisibleForTesting
	public Set getFolders() {
		return config.getFolders();
	}

	public static <T> String getKey ( final T asset) {
		if (Contentlet.class.isInstance(asset)) {
			final Contentlet contentlet = Contentlet.class.cast(asset);
			return contentlet.getIdentifier();
		} else if (Folder.class.isInstance(asset)) {
			final Folder folder = Folder.class.cast(asset);
			return folder.getInode();
		} else if (Template.class.isInstance(asset)) {
			final Template template = Template.class.cast(asset);
			return template.getIdentifier();
		} else if (Container.class.isInstance(asset)) {
			final Container container = Container.class.cast(asset);
			return container.getIdentifier();
		} else if (Structure.class.isInstance(asset)) {
			final Structure structure = Structure.class.cast(asset);
			return structure.getInode();
		}  else if (Link.class.isInstance(asset)) {
			final Link link = Link.class.cast(asset);
			return link.getIdentifier();
		}  else if (Rule.class.isInstance(asset)) {
			final Rule rule = Rule.class.cast(asset);
			return rule.getId();
		} else if (Relationship.class.isInstance(asset)) {
			final Relationship relationship = Relationship.class.cast(asset);
			return relationship.getInode();
		} else if (WorkflowScheme.class.isInstance(asset)) {
			final WorkflowScheme workflowScheme = WorkflowScheme.class.cast(asset);
			return workflowScheme.getId();
		} else if (Category.class.isInstance(asset)) {
			final Category category = Category.class.cast(asset);
			return category.getCategoryId();
		} else {
			throw new IllegalArgumentException();
		}
	}
}
