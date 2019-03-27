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
	 * <p><strong>NOTE:</strong> All paths should be lowercase as we lower case the requested URI when the filters
	 * are evaluated.</p>
	 *
	 * @return A String array containing the paths.
	 */
    default String [] getFilters() {

        return null;
    }

	/**
	 * @return true if the {@link WebInterceptor} is activated.
	 */
	default boolean isActive() {
		return true;
	}

	/**
	 * Optional method called after the doFilter in case you need to do something with the request/response at the end of the request
	 * @param req
	 *   - The {@link HttpServletRequest} object.
	 * @param res
	 *  - The {@link HttpServletResponse} object.
	 * @return boolean true means continue with the next interceptor, false stop the chain.
	 * @throws IOException
	 */
	default boolean afterIntercept(final HttpServletRequest req, final HttpServletResponse res)  {
		return true;
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
