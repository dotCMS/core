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
	 * Adds an Interceptor before webInterceptorName, if the there is not any interceptor with that name,
	 * will be added at the end of the collection.
	 *
	 * @param webInterceptor
	 *            - The {@link WebInterceptor} that will be added to the filter.
	 * @param webInterceptorName {@link String}
	 */
	public void addBefore(String webInterceptorName, WebInterceptor webInterceptor);


	/**
	 * Adds an Interceptor after webInterceptorName, if the there is not any interceptor with that name,
	 * will be added at the end of the collection.
	 *
	 * @param webInterceptor
	 *            - The {@link WebInterceptor} that will be added to the filter.
	 * @param webInterceptorName {@link String}
	 */
	public void addAfter(String webInterceptorName, WebInterceptor webInterceptor);

	/**
	 * Adds an Interceptor at the end of the interceptor list.
	 * 
	 * @param webInterceptor
	 *            - The {@link WebInterceptor} that will be added to the filter.
	 */
	public void add(WebInterceptor webInterceptor);

	/**
	 * Adds the {@link WebInterceptor} on a specific order, if it is less than 0, add the element at the begin,
	 * If it's more than collection.size()-1, adds the element at the end.
	 *
	 * @param order {@link Integer}
	 * @param webInterceptor {@link WebInterceptor}
     */
	public void add(int order, WebInterceptor webInterceptor);

	/**
	 * Adds the {@link WebInterceptor} at the begin of the collection
	 * @param webInterceptor {@link WebInterceptor}
     */
	public void addFirst(WebInterceptor webInterceptor);

	/**
	 * Remove all interceptors in the list.
	 * @param destroy {@link Boolean} true if you want to destroy all WebInterceptor's before remove all.
	 */
	public void removeAll(final boolean destroy);

} // E:O:F:WebInterceptorAware.
