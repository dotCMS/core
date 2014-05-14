package com.dotcms.publisher.pusher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publishing.PublisherConfig;

public class PushPublisherConfig extends PublisherConfig {
	public static enum Operation {
		PUBLISH,
		UNPUBLISH
	}
	public static enum AssetTypes {
		TEMPLATES,
		HTMLPAGES,
		CONTAINERS,
		CONTENTS,
		LINKS,
		RELATIONSHIPS,
		CATEGORIES,
		WORKFLOWS,
		LANGUAGES
	}

	private Operation operation;
	private List<PublishingEndPoint> endpoints;
	private boolean pushing = true;
	private boolean downloading = false;

	public PushPublisherConfig() {
		super();
	}

	boolean switchIndexWhenDone = false;

	public boolean switchIndexWhenDone(){
		return switchIndexWhenDone;
	}


	public void setSwitchIndexWhenDone(boolean switchIndexWhenDone) {
		this.switchIndexWhenDone = switchIndexWhenDone;
	}


	private enum MyConfig {
		RUN_NOW,INDEX_NAME;
	};


	public boolean runNow(){
		return this.get(MyConfig.RUN_NOW.toString()) !=null && new Boolean((String) this.get(MyConfig.RUN_NOW.toString()));

	}

    /**
     * Returns the type of operation we will apply to the bundle (PUBLISH/UNPUBLISH).
     *
     * @return
     */
	public Operation getOperation() {
		return operation;
	}


	public void setOperation(Operation operation) {
		this.operation = operation;
	}


	public List<PublishingEndPoint> getEndpoints() {
		return endpoints;
	}


	public void setEndpoints(List<PublishingEndPoint> endpoints) {
		this.endpoints = endpoints;
	}


	public void setRunNow(boolean once){
		this.put(MyConfig.RUN_NOW.toString(), once);

	}

	public String getIndexName(){
		return (String) this.get(MyConfig.INDEX_NAME.toString());

	}

	public void setIndexName(String name){
		this.put(MyConfig.INDEX_NAME.toString(), name);


	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Class> getPublishers(){
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(PushPublisher.class);
		return clazz;
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

	@SuppressWarnings("unchecked")
	public Set<String> getLanguages() {
		if(get(AssetTypes.LANGUAGES.name()) == null){
			Set<String> languagesToBuild =   new HashSet<String>();
			put(AssetTypes.LANGUAGES.name(), languagesToBuild);
		}

		return (Set<String>) get(AssetTypes.LANGUAGES.name());

	}

//	@SuppressWarnings("unchecked")
//	public Set<String> getCategories() {
//		if(get(AssetTypes.CATEGORIES.name()) == null){
//			Set<String> categoriesToBuild =   new HashSet<String>();
//			put(AssetTypes.CATEGORIES.name(), categoriesToBuild);
//		}
//
//		return (Set<String>) get(AssetTypes.LINKS.name());
//
//	}
	
	@SuppressWarnings("unchecked")
	public Set<String> getRelationships() {
		if(get(AssetTypes.RELATIONSHIPS.name()) == null){
			Set<String> relationshipsToBuild =   new HashSet<String>();
			put(AssetTypes.RELATIONSHIPS.name(), relationshipsToBuild);
		}

		return (Set<String>) get(AssetTypes.RELATIONSHIPS.name());

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

	public void setLanguages(Set<String> languages) {
		put(AssetTypes.LANGUAGES.name(), languages);
	}
//	public void setCategories(Set<String> categories){
//		put(AssetTypes.CATEGORIES.name(),categories);
//	}
//
	public void setRelationships(Set<String> relationships) {
		put(AssetTypes.RELATIONSHIPS.name(), relationships);
	}

	public boolean isPushing() {
		return pushing;
	}

	public void setPushing(boolean pushing) {
		this.pushing = pushing;
	}

    public boolean isDownloading () {
        return downloading;
    }

    public void setDownloading ( boolean downloading ) {
        this.downloading = downloading;
    }

}
