package com.dotcms.publisher.pusher;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publisher.util.dependencies.DependencyProcessor;
import com.dotcms.publisher.util.dependencies.DependencySet;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
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
	private DependencySet dependencySet;

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

    public boolean isDownloading () {
        return downloading;
    }

    public void setDownloading ( boolean downloading ) {
        this.downloading = downloading;
    }

	public void setDependencyProcessor(DependencyProcessor dependencyProcessor) {
		this.dependencyProcessor = dependencyProcessor;
	}

	public <T> void addWithDependencies(final T asset, final PusheableAsset pusheableAsset) {
		final String key = getAssestKey(asset);

		if(!dependencySet.isDependenciesAdded(key, pusheableAsset)) {
			dependencySet.addWithDependencies(key, pusheableAsset);
			this.dependencyProcessor.addAsset(asset, pusheableAsset);
		}
	}

	private <T> String getAssestKey(final T asset) {
		if (Structure.class.isInstance(asset)){
			return ((Structure) asset).getInode();
		} if (Folder.class.isInstance(asset)){
			return ((Folder) asset).getInode();
		} if (Host.class.isInstance(asset)){
			return ((Host) asset).getIdentifier();
		} if (Contentlet.class.isInstance(asset)){
			return ((Contentlet) asset).getIdentifier();
		} if (Template.class.isInstance(asset)){
			return ((Template) asset).getIdentifier();
		} if (Link.class.isInstance(asset)){
			return ((Link) asset).getIdentifier();
		} else {
			throw new IllegalArgumentException("Class no expected");
		}
	}

	public <T> void add(final T asset, final PusheableAsset pusheableAsset) {
		final String key = getAssestKey(asset);

		if(!dependencySet.isAdded(key, pusheableAsset)) {
			dependencySet.add(key, pusheableAsset);
		}
	}

	public void waitUntilResolveAllDependencies() throws ExecutionException {
		this.dependencyProcessor.waitUntilResolveAllDependencies();
	}

}
