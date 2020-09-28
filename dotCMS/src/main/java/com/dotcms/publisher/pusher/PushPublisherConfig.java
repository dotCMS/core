package com.dotcms.publisher.pusher;

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
		RULES
	}

	private List<PublishingEndPoint> endpoints;
	private boolean downloading = false;

	public PushPublisherConfig() {
		super();
	}

    /**
     * Convenience constructor for generating a PP config from a bundle
     * 
     * @param bundle
     */
    public PushPublisherConfig(Bundle bundle) {
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
		if(get(AssetTypes.CONTAINERS.name()) == null){
			Set<String> containersToBuild =   new HashSet<String>();
			put(AssetTypes.CONTAINERS.name(), containersToBuild);
		}
		return (Set<String>) get(AssetTypes.CONTAINERS.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getTemplates() {
		if(get(AssetTypes.TEMPLATES.name()) == null){
			Set<String> templatesToBuild =   new HashSet<String>();
			put(AssetTypes.TEMPLATES.name(), templatesToBuild);
		}
		return (Set<String>) get(AssetTypes.TEMPLATES.name());
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
		if(get(AssetTypes.CONTENTS.name()) == null){
			Set<String> contentletsToBuild =   new HashSet<String>();
			put(AssetTypes.CONTENTS.name(), contentletsToBuild);
		}
		return (Set<String>) get(AssetTypes.CONTENTS.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getLinks() {
		if(get(AssetTypes.LINKS.name()) == null){
			Set<String> linksToBuild =   new HashSet<String>();
			put(AssetTypes.LINKS.name(), linksToBuild);
		}
		return (Set<String>) get(AssetTypes.LINKS.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getWorkflows() {
		if(get(AssetTypes.WORKFLOWS.name()) == null){
			Set<String> workflowsToBuild =   new HashSet<String>();
			put(AssetTypes.WORKFLOWS.name(), workflowsToBuild);
		}
		return (Set<String>) get(AssetTypes.WORKFLOWS.name());
	}

	/**
	 * Gets the list of {@link Rule} objects that will be pushed to the
	 * destination end-point.
	 *
	 * @return The list of rules.
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getRules() {
		if (get(AssetTypes.RULES.name()) == null) {
			Set<String> rulesToBuild = new HashSet<String>();
			put(AssetTypes.RULES.name(), rulesToBuild);
		}
		return (Set<String>) get(AssetTypes.RULES.name());
	}

	@SuppressWarnings("unchecked")
	public Set<String> getRelationships() {
		if(get(AssetTypes.RELATIONSHIPS.name()) == null){
			Set<String> relationshipsToBuild =   new HashSet<String>();
			put(AssetTypes.RELATIONSHIPS.name(), relationshipsToBuild);
		}
		return (Set<String>) get(AssetTypes.RELATIONSHIPS.name());
	}

    @SuppressWarnings("unchecked")
    public Set<String> getCategories() {
        if(get(AssetTypes.CATEGORIES.name()) == null){
            Set<String> categoriesToBuild =   new HashSet<>();
            put(AssetTypes.CATEGORIES.name(), categoriesToBuild);
        }
        return (Set<String>) get(AssetTypes.CATEGORIES.name());
    }

	public void setHTMLPages(Set<String> htmlPages) {
		put(AssetTypes.HTMLPAGES.name(), htmlPages);
	}

	public void setContainers(Set<String> containers) {
		put(AssetTypes.CONTAINERS.name(), containers);
	}

	public void setTemplates(Set<String> templates) {
		put(AssetTypes.TEMPLATES.name(), templates);
	}

	public void setContents(Set<String> contents) {
		put(AssetTypes.CONTENTS.name(), contents);
	}

	public void setLinks(Set<String> links) {
		put(AssetTypes.LINKS.name(), links);
	}

	public void setWorkflows(Set<String> workflows) {
		put(AssetTypes.WORKFLOWS.name(), workflows);
	}



	/**
	 * Sets the list of {@link Rule} objects that will be pushed to the
	 * destination end-point.
	 * 
	 * @param rules
	 *            - The list of rules.
	 */
	public void setRules(Set<String> rules) {
		put(AssetTypes.RULES.name(), rules);
	}

	public void setRelationships(Set<String> relationships) {
		put(AssetTypes.RELATIONSHIPS.name(), relationships);
	}

    public void setCategories(Set<String> categories) {
        put(AssetTypes.CATEGORIES.name(), categories);
    }

    public boolean isDownloading () {
        return downloading;
    }

    public void setDownloading ( boolean downloading ) {
        this.downloading = downloading;
    }

}
