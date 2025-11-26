package com.dotmarketing.util;

import java.util.Objects;
import org.apache.commons.lang.builder.ToStringBuilder;

public class PaginatedContentList<E> extends PaginatedArrayList<E> {

	private static final long serialVersionUID = -3041937930751284374L;
	private boolean previousPage;
	private boolean nextPage;
	private int totalPages;
	private int offset;
	private int limit;
	private int currentPage;

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
	 * @param offset the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param limit the limit to set
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 * @return the limit
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @param currentPage the page to set
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * @return the current page
	 */
	public int getCurrentPage() {
		return currentPage;
	}

	/**
	 * @return the totalPages
	 */
	public int getTotalPages() {
		return totalPages;
	}
	/**
	 * @param totalPages the totalPages to set
	 */
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (!(o instanceof PaginatedContentList)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		PaginatedContentList<?> that = (PaginatedContentList<?>) o;
		return previousPage == that.previousPage &&
				nextPage == that.nextPage &&
				totalPages == that.totalPages &&
				offset == that.offset &&
				limit == that.limit &&
				currentPage == that.currentPage;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				super.hashCode(), previousPage, nextPage, totalPages, offset, limit, currentPage
		);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this) + "\r\n" + super.toString();
	}
	
}
