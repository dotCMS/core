package com.dotcms.api.web;

import java.util.List;

import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.filters.interceptor.WebInterceptorAware;

/**
 * Provides a way for developers to add functionality or more features to
 * existing dotCMS filters. Interceptor classes can be read and executed from
 * any existing filters without having to worry about performance issues caused
 * by adding more filters to system requests, and their specific order
 * (position) in the {@code web.xml} file.
 * 
 * Existing or new filters must implement the {@link WebInterceptorAware}
 * interface in order to take advantage of this mechanism. Interceptors can be
 * added through a Dynamic Plugin (OSGi) so that new featues can be added
 * removed at runtime.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jun 23, 2016
 *
 */
public interface WebInterceptorAPI {

	/**
	 * Adds a new {@link WebInterceptor} to an existing Web Filter.
	 * 
	 * @param filter
	 *            - An existing Web Filter that implements the
	 *            {@link WebInterceptorAware} interface.
	 * @param interceptor
	 *            - The {@link WebInterceptor} that will add more features to
	 *            the specified filter.
	 */
	public void add(Class<WebInterceptorAware> filter, WebInterceptor interceptor);

	/**
	 * Adds a the list of new {@link WebInterceptor} objects to an existing Web
	 * Filter.
	 * 
	 * @param filter
	 *            - An existing Web Filter that implements the
	 *            {@link WebInterceptorAware} interface.
	 * @param interceptors
	 *            - The list of {@link WebInterceptor} objects that will add
	 *            more features to the specified filter.
	 */
	public void addAll(Class<WebInterceptorAware> filter, List<WebInterceptor> interceptors);

	/**
	 * Returns the specified {@link WebInterceptor} from a
	 * {@link WebInterceptorAware} filter.
	 * 
	 * @param filter
	 *            - The filter containing the instance of the Interceptor.
	 * @param interceptor
	 *            - The class name of the {@link WebInterceptor} to retrieve.
	 * @return The specified {@link WebInterceptor}.
	 */
	public WebInterceptor get(Class<WebInterceptorAware> filter, String interceptor);

	/**
	 * Returns the list of all the {@link WebInterceptor} objects that belong to
	 * a specific {@link WebInterceptorAware} filter.
	 * 
	 * @param filter
	 *            - The filter containing the instances of the Interceptors.
	 * @return The list of WebInterceptor objects associated to the specified
	 *         filter.
	 */
	public WebInterceptor getAll(Class<WebInterceptorAware> filter);

	/**
	 * Removes the specified {@link WebInterceptor} from the existing
	 * {@link WebInterceptorAware} filter.
	 * 
	 * @param filter
	 *            - An existing Web Filter whose interceptor will be removed.
	 * @param interceptor
	 *            - The class name of the {@link WebInterceptor} to be removed.
	 */
	public void remove(Class<WebInterceptorAware> filter, String interceptor);

	/**
	 * Removes the specified list of {@link WebInterceptor} objects from the
	 * existing {@link WebInterceptorAware} filter.
	 * 
	 * @param filter
	 *            - An existing Web Filter whose interceptor will be removed.
	 * @param interceptor
	 *            - The list of {@link WebInterceptor} objects to be removed.
	 */
	public void removeAll(Class<WebInterceptorAware> filter);

}
