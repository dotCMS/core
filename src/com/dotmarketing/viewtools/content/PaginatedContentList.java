package com.dotmarketing.viewtools.content;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.util.PaginatedArrayList;

public class PaginatedContentList<E> extends PaginatedArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3041937930751284374L;
	private boolean previousPage;
	private boolean nextPage;
	private long totalPages;
		
	/**
	 * @return the previousPage
	 */
	public boolean isPreviousPage() {
		return previousPage;
	}
	/**
	 * @param previousPage the previousPage to set
	 */
	public void setPreviousPage(boolean previousPage) {
		this.previousPage = previousPage;
	}
	/**
	 * @return the nextPage
	 */
	public boolean isNextPage() {
		return nextPage;
	}
	/**
	 * @param nextPage the nextPage to set
	 */
	public void setNextPage(boolean nextPage) {
		this.nextPage = nextPage;
	}
	/**
	 * @return the totalPages
	 */
	public long getTotalPages() {
		return totalPages;
	}
	/**
	 * @param totalPages the totalPages to set
	 */
	public void setTotalPages(long totalPages) {
		this.totalPages = totalPages;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this) + "\r\n" + super.toString();
	}
	
}
