package com.dotcms.filters.interceptor;

import java.io.Serializable;

/**
 * This contract is used to add a new web interceptor to a filter.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 15, 2016
 */
public interface WebInterceptorAware extends Serializable {

	/**
	 * Called when the apps wants to add a new interceptor to the filter
	 * 
	 * @param webInterceptor
	 *            - The {@link WebInterceptor} that will be added to the filter.
	 */
	public void add(WebInterceptor webInterceptor);

} // E:O:F:WebInterceptorAware.
