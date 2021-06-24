package com.dotcms.publisher.util.dependencies;

import static com.dotcms.util.CollectionsUtils.set;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.PublisherFilter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
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
	private User user;
	private PushPublisherConfig config;
	private PushedAssetUtil pushedAssetUtil;

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
		this.user = user;
		pushedAssetUtil = new PushedAssetUtil(config);
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

		this.config.setDependencyProcessor(
				new PushPublishigDependencyProcesor(user, config, publisherFilter));

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
					final Structure structure = CacheLocator.getContentTypeCache()
							.getStructureByInode(asset.getAsset());

					if (structure == null) {
						Logger.warn(getClass(),
								"Structure id: " + (asset.getAsset() != null ? asset.getAsset()
										: "N/A")
										+ " does NOT have working or live version, not Pushed");
					} else {
						add(structure, PusheableAsset.CONTENT_TYPE);
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
							add(template, PusheableAsset.TEMPLATE);
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
						add(container, PusheableAsset.CONTAINER);
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
						add(folder, PusheableAsset.FOLDER);
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
						add(host, PusheableAsset.SITE);
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
						add(link, PusheableAsset.LINK);
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
					add(scheme, PusheableAsset.WORKFLOW);
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
					add(language, PusheableAsset.LANGUAGE);
				}
			} else if (asset.getType().equals(PusheableAsset.RULE.getType())) {
				Rule rule = APILocator.getRulesAPI()
						.getRuleById(asset.getAsset(), user, false);
				if (rule != null && StringUtils.isNotBlank(rule.getId())) {
					add(rule, PusheableAsset.RULE);
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
					add(con, PusheableAsset.CONTENTLET);
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
	}

	private <T> void add(final T asset, final PusheableAsset pusheableAsset) {
		final boolean added = config.addWithDependencies(asset, pusheableAsset);

		if (added) {
			pushedAssetUtil.savePushedAssetForAllEnv(asset, pusheableAsset);
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

	/**
	 * Return the key to add a asset into a Bundle
	 *
	 * @param asset
	 * @param <T>
	 * @return
	 */
	public static <T> String getBundleKey( final T asset) {
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
		} else if (ContentType.class.isInstance(asset)) {
			final ContentType contentType = ContentType.class.cast(asset);
			return contentType.inode();
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
		} else if (Language.class.isInstance(asset)) {
			final Language languege = Language.class.cast(asset);
			return String.valueOf(languege.getId());
		} else {
			throw new IllegalArgumentException("Not allowed: " + asset.getClass().getName());
		}
	}
}
