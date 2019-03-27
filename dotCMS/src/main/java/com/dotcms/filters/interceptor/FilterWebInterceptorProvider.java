package com.dotcms.filters.interceptor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This provider is in charge of keep the instances of the {@link WebInterceptorDelegate}'s
 * associated to the {@link javax.servlet.Filter}'s
 * @author jsanca
 */
// todo: make this a mbean to monitored the list of interceptor
public class FilterWebInterceptorProvider implements Serializable {

    private final Map<String, WebInterceptorDelegate> delegateMap =
            new ConcurrentHashMap<>();

    private FilterWebInterceptorProvider () {}

    /**
     * Get's the instance of the Provider
     * @param request {@link HttpServletRequest}
     * @return FilterWebInterceptorProvider
     */
    public static FilterWebInterceptorProvider getInstance (final HttpServletRequest request) {

        final ServletContext context =
                request.getServletContext();

        return getInstance(context);
    } // getInstance.


    /**
     * Get's the instance of the Provider
     * @param context {@link ServletContext}
     * @return FilterWebInterceptorProvider
     */
    public static FilterWebInterceptorProvider getInstance (final ServletContext context) {

        if (null == context.getAttribute(FilterWebInterceptorProvider.class.getName())) {

            synchronized (FilterWebInterceptorProvider.class) {

                if (null == context.getAttribute(FilterWebInterceptorProvider.class.getName())) {

                    context.setAttribute(FilterWebInterceptorProvider.class.getName(),
                            new FilterWebInterceptorProvider());
                }
            }
        }

        return (FilterWebInterceptorProvider) context.getAttribute
                (FilterWebInterceptorProvider.class.getName());
    } // getInstance.

    /**
     * Get's the delegate associated to the filter class.
     * @param filterClass WebInterceptorDelegate
     * @return WebInterceptorDelegate
     */
    public WebInterceptorDelegate getDelegate (final Class<? extends AbstractWebInterceptorSupportFilter> filterClass) {

        return this.getDelegate(filterClass.getName());
    } // getDelegate.

    /**
     * Get's the delegate associated to the filter name .
     * @param filterName {@link String}
     * @return WebInterceptorDelegate
     */
    public WebInterceptorDelegate getDelegate (final String filterName) {

        if (!this.delegateMap.containsKey(filterName)) {

            this.delegateMap.put(filterName, this.createDelegate ());
        }

        return this.delegateMap.get(filterName);
    } // getDelegate.

    // override if you want to use a diff implementation
    protected WebInterceptorDelegate createDelegate() {

        return new SimpleWebInterceptorDelegateImpl();
    } // createDelegate.

} // E:OF:FilterWebInterceptorProvider.
