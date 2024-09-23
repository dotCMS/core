package com.dotcms.publisher.pusher;


import static com.dotcms.publisher.ajax.RemotePublishAjaxAction.ADD_ALL_CATEGORIES_TO_BUNDLE_KEY;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publisher.util.dependencies.DependencyProcessor;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem;

import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.concurrent.ExecutionException;

/**
 * This class provides the main configuration values for the bundle that is
 * being published. It is possible to retrieve the list of objects that make up
 * the bundle, organized by their types, the destination end-points where the
 * bundle will be sent, among other data.
 * 
 * @author Alberto
 * @version 1.0
 * @since Oct 11, 2012
 *
 */
public class PushPublisherConfig extends PublisherConfig {

    public enum AssetTypes {
		TEMPLATES,
		HTMLPAGES,
		CONTAINERS,
		CONTENTS,
		LINKS,
		RELATIONSHIPS,
		CATEGORIES,
		WORKFLOWS,
		LANGUAGES,
		RULES,
		HOST,
		FOLDER,
		CONTENT_TYPE
	}

	private List<PublishingEndPoint> endpoints;
	private boolean downloading = false;
	@XStreamOmitField
	private DependencyProcessor dependencyProcessor;
	private BundleAssets bundleAssets = new BundleAssets();
	private Set<String> excludes = new HashSet<>();

	public PushPublisherConfig() {
		super();
	}

    /**
     * Convenience constructor for generating a PP config from a bundle
     * 
     * @param bundle
     */
    public PushPublisherConfig(final Bundle bundle) {
        super();

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();

        final List<PublishQueueElement> tempBundleContents =
                        Try.of(() -> publisherAPI.getQueueElementsByBundleId(bundle.getId()))
                                        .onFailure(e -> Logger.warnAndDebug(PushPublisherConfig.class, e))
                                        .getOrElse(ImmutableList.of());
        final List<PublishQueueElement> assetsToPublish = new ArrayList<>();
        assetsToPublish.addAll(tempBundleContents);


        this.setDownloading(true);
        this.setOperation(PushPublisherConfig.Operation.PUBLISH.ordinal() == bundle.getOperation()
                        ? PushPublisherConfig.Operation.PUBLISH
                        : PushPublisherConfig.Operation.UNPUBLISH);

        setAssets(assetsToPublish);
        // Queries creation
        setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
        setId(bundle.getId());
        // get the bundle user or system user if failure
        final User user = Try.of(()->APILocator.getUserAPI().loadUserById(bundle.getOwner())).getOrElse(APILocator.systemUser());
                        
        setUser(user);


    }
	
	
	

	boolean switchIndexWhenDone = false;

	public boolean switchIndexWhenDone(){
		return switchIndexWhenDone;
	}

	public void setSwitchIndexWhenDone(boolean switchIndexWhenDone) {
		this.switchIndexWhenDone = switchIndexWhenDone;
	}

	/**
	 * Return true if the {@link PushPublisherConfig#getAssets()} list is not empty and just
	 * contains {@link User}, otherwise return false.
	 *
	 * @return
	 */
	public boolean justIncludesUsers() {

		if (!UtilMethods.isSet(this.getAssets())) {
			return false;
		}

		for (PublishQueueElement asset : this.getAssets()) {
			if (!PusheableAsset.USER.name().equalsIgnoreCase(asset.getType())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Return true if the {@link PushPublisherConfig#getAssets()} list is not empty and just
	 * contains {@link com.dotmarketing.portlets.categories.model.Category}, otherwise return false.
	 *
	 * @return
	 */
	public boolean justIncludesCategories() {

		if (!UtilMethods.isSet(this.getAssets())) {
			return false;
		}

		return !this.getAssets().stream().anyMatch (asset -> !ADD_ALL_CATEGORIES_TO_BUNDLE_KEY.equalsIgnoreCase(asset.getAsset()));
	}

	public List<PublishingEndPoint> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<PublishingEndPoint> endpoints) {
		this.endpoints = endpoints;
	}

	public String getIndexName(){
		return (String) this.get(MyConfig.INDEX_NAME.toString());
	}

	public void setIndexName(String name){
		this.put(MyConfig.INDEX_NAME.toString(), name);
	}

	@SuppressWarnings("unchecked")
	public Set<String> getContainers() {
		return bundleAssets.getContainers();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getTemplates() {
		return bundleAssets.getTemplates();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getHTMLPages() {
		if(get(AssetTypes.HTMLPAGES.name()) == null){
			Set<String> htmlPagesToBuild =   new HashSet<>();
			put(AssetTypes.HTMLPAGES.name(), htmlPagesToBuild);
		}
		return (Set<String>) get(AssetTypes.HTMLPAGES.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getContentlets() {
		return bundleAssets.getContentlets();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getLinks() {
		return bundleAssets.getLinks();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getWorkflows() {
		return bundleAssets.getWorkflows();
	}

	/**
	 * Gets the list of {@link Rule} objects that will be pushed to the
	 * destination end-point.
	 *
	 * @return The list of rules.
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getRules() {
		return bundleAssets.getRules();
	}

	public Set<String> getExperiments() {
		return bundleAssets.getExperiments();
	}

	public Set<String> getVariants() {
		return bundleAssets.getVariants();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getRelationships() {
		return bundleAssets.getRelationships();
	}

    @SuppressWarnings("unchecked")
    public Set<String> getCategories() {
		return bundleAssets.getCategories();
    }

    @Override
	public Set<String> getFolders() {
		return bundleAssets.getFolders();
	}

	@Override
	public Set<String> getStructures() {
		return bundleAssets.getStructures();
	}

	@Override
	public Set<String> getHostSet() {
		return bundleAssets.getHosts();
	}

	public Set<String> getIncludedLanguages() {
		return bundleAssets.getLanguages();
	}

    public boolean isDownloading () {
        return downloading;
    }

    public void setDownloading ( boolean downloading ) {
        this.downloading = downloading;
    }

	public void setDependencyProcessor(DependencyProcessor dependencyProcessor) {
		this.dependencyProcessor = dependencyProcessor;
	}

	public BundleAssets dependencySet(){
		return bundleAssets;
	}

	/**
	 * Add an asset to the bundle. If the asset is already added, it will not be added again.
	 * It will be included in the dependency processor, so its dependencies are included too.
	 * @param asset The asset to be added
	 * @param pusheableAsset The object type of the asset
	 * @param evaluateReason The reason why the asset is being added
	 * @return true if the asset was added, false otherwise
	 */
	public <T> boolean addWithDependencies(final T asset, final PusheableAsset pusheableAsset,
			final String evaluateReason) {
		final String key = DependencyManager.getBundleKey(asset);
		final boolean added = bundleAssets.isAdded(key, pusheableAsset);
		final boolean isAlreadyAdded = bundleAssets.isDependenciesAdded(key, pusheableAsset);

		if(!isAlreadyAdded) {
			bundleAssets.addWithDependencies(key, pusheableAsset);
			this.dependencyProcessor.addAsset(asset, pusheableAsset);

			if (!added) {
				writeIncludeManifestItem(asset, evaluateReason);
			}
		}

		return !isAlreadyAdded;
	}

	/**
	 * Add an asset to the bundle. If the asset is already added, it will not be added again.
	 * @param asset The asset to be added
	 * @param pusheableAsset The object type of the asset
	 * @param evaluateReason The reason why the asset is being added
	 * @return true if the asset was added, false otherwise
	 */
	public <T> boolean add(final T asset, final PusheableAsset pusheableAsset, final String evaluateReason) {
		final String key = DependencyManager.getBundleKey(asset);

		if(!bundleAssets.isAdded(key, pusheableAsset)) {
			bundleAssets.add(key, pusheableAsset);
			writeIncludeManifestItem(asset, evaluateReason);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns true if the asset is already added to the bundle, false otherwise.
	 * @param asset The asset to be checked
	 * @param pusheableAsset The object type of the asset
	 * @return true if the asset is already added, false otherwise
	 */
	public <T> boolean contains(final T asset, final PusheableAsset pusheableAsset) {
		final String key = DependencyManager.getBundleKey(asset);
		return bundleAssets.isAdded(key, pusheableAsset);
	}

	/**
	 * Add an asset to the list of assets that will be excluded from the bundle.
	 * If the asset is already excluded, it will not be excluded again.
	 * @param asset The asset to be excluded
	 * @param pusheableAsset The object type of the asset
	 * @param evaluateReason The reason why the asset is being evaluated
	 * @param excludeReason The reason why the asset is being excluded
	 * @return true if the asset was excluded, false otherwise
	 */
	public <T> boolean exclude(final T asset, final PusheableAsset pusheableAsset, final String evaluateReason, final String excludeReason) {
		final String key = DependencyManager.getBundleKey(asset);

		if(!excludes.contains(key)) {
			excludes.add(key);
			writeExcludeManifestItem(asset, evaluateReason, excludeReason);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Write a line to the manifest file as an included item.
	 * @param asset The asset to be included
	 * @param evaluateReason The reason why the asset is being included
	 */
	public <T> void writeIncludeManifestItem(final T asset, final String evaluateReason) {
		if (ManifestItem.class.isAssignableFrom(asset.getClass())) {
			if (UtilMethods.isSet(manifestBuilder)) {
				manifestBuilder.include((ManifestItem) asset, evaluateReason);
			}
		} else {
			Logger.warn(PushPublisherConfig.class,
					String.format("It is not possible add %s into the manifest", asset));
		}
	}

	/**
	 * Write a line to the manifest file as an excluded item.
	 * @param asset The asset to be excluded
	 * @param evaluateReason The reason why the asset is being evaluated
	 * @param excludeReason The reason why the asset is being excluded
	 */
	private <T> void writeExcludeManifestItem(final T asset, final String evaluateReason, final String excludeReason) {
		if (ManifestItem.class.isAssignableFrom(asset.getClass())) {
			if (UtilMethods.isSet(manifestBuilder)) {
				manifestBuilder.exclude((ManifestItem) asset, evaluateReason, excludeReason);
			}
		} else {
			Logger.warn(PushPublisherConfig.class,
					String.format("It is not possible add %s into the manifest", asset));
		}
	}

	public void waitUntilResolveAllDependencies() throws ExecutionException {
		this.dependencyProcessor.waitUntilResolveAllDependencies();
	}

	public ManifestBuilder getManifestBuilder() {
		return manifestBuilder;
	}
}
