package com.dotcms.publisher.pusher;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publisher.util.dependencies.DependencyProcessor;
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
	private DependencyProcessor dependencyProcessor;
	private DependencySet dependencySet = new DependencySet();

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
        final List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>();
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
		return dependencySet.getContainers();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getTemplates() {
		return dependencySet.getTemplates();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getHTMLPages() {
		if(get(AssetTypes.HTMLPAGES.name()) == null){
			Set<String> htmlPagesToBuild =   new HashSet<String>();
			put(AssetTypes.HTMLPAGES.name(), htmlPagesToBuild);
		}
		return (Set<String>) get(AssetTypes.HTMLPAGES.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getContentlets() {
		return dependencySet.getContentlets();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getLinks() {
		return dependencySet.getLinks();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getWorkflows() {
		return dependencySet.getWorkflows();
	}

	/**
	 * Gets the list of {@link Rule} objects that will be pushed to the
	 * destination end-point.
	 *
	 * @return The list of rules.
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getRules() {
		return dependencySet.getRules();
	}

	@SuppressWarnings("unchecked")
	public Set<String> getRelationships() {
		return dependencySet.getRelationships();
	}

    @SuppressWarnings("unchecked")
    public Set<String> getCategories() {
		return dependencySet.getCategories();
    }

    @Override
	public Set<String> getFolders() {
		return dependencySet.getFolders();
	}

	@Override
	public Set<String> getStructures() {
		return dependencySet.getStructures();
	}

	@Override
	public Set<String> getHostSet() {
		return dependencySet.getHosts();
	}

	@Override
	public Set<String> getLanguages() {
		return dependencySet.getLanguages();
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

	public DependencySet dependencySet(){
		return dependencySet;
	}

	public <T> boolean addWithDependencies(final T asset, final PusheableAsset pusheableAsset) {
		final String key = DependencyManager.getKey(asset);
		final boolean isAlreadyAdded = dependencySet.isDependenciesAdded(key, pusheableAsset);

		if(!isAlreadyAdded) {
			dependencySet.addWithDependencies(key, pusheableAsset);
			this.dependencyProcessor.addAsset(asset, pusheableAsset);
		}

		return !isAlreadyAdded;
	}

	public <T> boolean add(final T asset, final PusheableAsset pusheableAsset) {
		final String key = DependencyManager.getKey(asset);

		if(!dependencySet.isAdded(key, pusheableAsset)) {
			dependencySet.add(key, pusheableAsset);
			return true;
		} else {
			return false;
		}
	}

	public void waitUntilResolveAllDependencies() throws ExecutionException {
		this.dependencyProcessor.waitUntilResolveAllDependencies();
	}

}
