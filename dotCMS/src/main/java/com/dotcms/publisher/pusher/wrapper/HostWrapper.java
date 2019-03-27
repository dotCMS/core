package com.dotcms.publisher.pusher.wrapper;

import java.util.List;
import java.util.Map;

import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.tag.model.Tag;


public class HostWrapper implements ContentWrapper {

	private ContentletVersionInfo info;
	private Contentlet host;
	private Identifier id;
	private List<Map<String,Object>> multiTree;
	private List<Map<String,Object>> tree;
	private List<String> categories;
	private List<Tag> tags;
	private Operation operation;
	private Map<String, List<Tag>> contentTags;

	public ContentletVersionInfo getInfo() {
		return info;
	}
	public void setInfo(ContentletVersionInfo info) {
		this.info = info;
	}

	public Contentlet getContent() {
		return host;
	}
	public void setContent(Contentlet host) {
		this.host = host;
	}

	public Identifier getId() {
		return id;
	}
	public void setId(Identifier id) {
		this.id = id;
	}

	public List<Map<String, Object>> getMultiTree() {
		return multiTree;
	}
	public void setMultiTree(List<Map<String, Object>> multiTree) {
		this.multiTree = multiTree;
	}

	public List<Map<String, Object>> getTree() {
		return tree;
	}
	public void setTree(List<Map<String, Object>> tree) {
		this.tree = tree;
	}

    public List<String> getCategories() {
    	return categories;
    }
	public void setCategories(List<String> categories) {
		this.categories = categories;
	}
    
	public List<Tag> getTags() {
		return tags;
	}
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	public Language getLanguage() {
		return null;
	}

	@Override
	public Map<String, List<Tag>> getContentTags() {
		return contentTags;
	}

	public void setContentTags(Map<String, List<Tag>> contentTags) {
		this.contentTags = contentTags;
	}
}
