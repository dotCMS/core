package com.dotmarketing.portlets.categories.business;

import java.util.List;

import com.dotmarketing.portlets.categories.model.Category;

public class PaginatedCategories {
	private List<Category> categories;
	private Integer totalCount;
	
	public PaginatedCategories() {}
	
	public PaginatedCategories(List<Category> categories, Integer totalCount) {
		super();
		this.categories = categories;
		this.totalCount = totalCount;
	}
	public List<Category> getCategories() {
		return categories;
	}
	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}
	public Integer getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(Integer totalCount) {
		this.totalCount = totalCount;
	}
	
	

}
