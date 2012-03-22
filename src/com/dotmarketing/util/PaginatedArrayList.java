/**
 * 
 */
package com.dotmarketing.util;

import java.util.ArrayList;

/**
 * This class is so dotCMS can return lists of objects that are paginated and still provide 
 * a total of the results in the list. 
 * @author Jason Tesser
 *
 */
public class PaginatedArrayList<E> extends ArrayList<E>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7345046002562313843L;
	private long totalResults;
	
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

}
