/**
 * 
 */
package com.dotmarketing.util;

import com.dotmarketing.portlets.templates.model.Template;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is so dotCMS can return lists of objects that are paginated and still provide 
 * a total of the results in the list. 
 * @author Jason Tesser
 *
 */
public class PaginatedArrayList<E> extends ArrayList<E> {

	private static final long serialVersionUID = -7345046002562313843L;
	private long totalResults;
	private String query;

	public PaginatedArrayList() {
		super();
	}

	/**
	 * Creates an instance of this class using the elements that are present in the specified collection.
	 *
	 * @param items The collection of items that will be added to this paginated array list.
	 */
	public PaginatedArrayList(final Collection<E> items) {
		final PaginatedArrayList<Template> templates = new PaginatedArrayList<>();
		if (items instanceof PaginatedArrayList) {
			templates.setQuery(PaginatedArrayList.class.cast(items).getQuery());
			templates.setTotalResults(PaginatedArrayList.class.cast(items).getTotalResults());
		}
		addAll(items);
	}

	/**
	 * @return the totalResults
	 */
	public long getTotalResults() {
		return totalResults;
	}

	/**
	 * @param totalResults the totalResults to set
	 */
	public void setTotalResults(long totalResults) {
		this.totalResults = totalResults;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public String toString() {
		return "PaginatedArrayList{" +
				"totalResults=" + totalResults +
				", query='" + query + '\'' +
				'}';
	}

}
