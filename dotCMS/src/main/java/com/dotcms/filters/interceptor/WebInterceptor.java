package com.dotcms.filters.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;


/**
 * Encapsulates an Interceptor. An interceptor is a good way to extend a filter
 * behavior, it's usually useful to be added as an OSGI plugin.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 15, 2016
 */
public interface WebInterceptor extends Serializable {

	/**
	 * Get the name of the interceptor by default use the class name
	 * @return String
     */
	default String getName() {

		return this.getClass().getName();
	} // getName.

    /**
     * Called on destroy
     */
	default void destroy() {}

    /**
     * Called on init
     */
    default void init() {}

	/**
	 * In case you want to apply this filter just to some subset of path's
	 * return them here. Null means accept all.
	 *
	 * @return A String array containing the paths.
	 */
    default String [] getFilters() {

        return null;
    }

	/**
	 * Called in any request. Returns true if you want to continue the chain
	 * call, false otherwise.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 * @return Result
	 * @throws IOException
	 */
	Result intercept(HttpServletRequest req, HttpServletResponse res)
            throws IOException;

} // E:O:F:WebInterceptor.
