package com.dotcms.publisher.pusher.wrapper;

import java.util.Set;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.categories.model.Category;

/**
 * The Category Wrapper: contains for each category a Set of children inodes.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Mar 6, 2013 - 9:35:53 AM
 */
public class CategoryWrapper {
	
	private boolean topLevel;
	private Category category;
	private Set<String> children;
	private Operation operation;
	
	public boolean isTopLevel() {
		return topLevel;
	}
	public void setTopLevel(boolean topLevel) {
		this.topLevel = topLevel;
	}
	public Category getCategory() {
		return category;
	}
	public void setCategory(Category category) {
		this.category = category;
	}
	public Set<String> getChildren() {
		return children;
	}
	public void setChildren(Set<String> children) {
		this.children = children;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	
	
}
