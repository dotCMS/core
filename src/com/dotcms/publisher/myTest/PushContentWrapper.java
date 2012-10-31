package com.dotcms.publisher.myTest;

import java.util.List;
import java.util.Map;

import com.dotcms.publisher.myTest.PushPublisherConfig.Operation;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.tag.model.Tag;


public class PushContentWrapper {
	
	private ContentletVersionInfo info;
	private Contentlet content;
	private Identifier id;
	private List<Map<String,Object>> multiTree;
	private List<Map<String,Object>> tree;
	private List<Tag> tags;
	private Operation operation;
	
	public ContentletVersionInfo getInfo() {
		return info;
	}
	public void setInfo(ContentletVersionInfo info) {
		this.info = info;
	}
	
	public Contentlet getContent() {
		return content;
	}
	public void setContent(Contentlet content) {
		this.content = content;
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
}
