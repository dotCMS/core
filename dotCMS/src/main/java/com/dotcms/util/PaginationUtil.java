package com.dotcms.util;

/**
 * Utility class to the pagination elements and filter
 * 
 * @author oswaldogallango
 *
 */
public class PaginationUtil {
	
	public static final String FILTER = "filter";
	public static final String ARCHIVED = "archived";
	public static final String PAGE = "page";
	public static final String COUNT = "count";
	
	/**
	 * Get the minimum pagination element index 
	 * @param currentPage The current page
	 * @param perPage The max amount of element per page
	 * @return minimum pagination element index
	 */
	public static int getMinIndex(int currentPage, int perPage){
		return (currentPage - 1) * perPage;
        
	}
	
	/**
	 * Get the maximum pagination element index
	 * @param currentPage The current page
	 * @param perPage The max amount of element per page
	 * @return maximum pagination element index
	 */
	public static int getMaxIndex(int currentPage, int perPage){
		return perPage * currentPage;
	}

}
