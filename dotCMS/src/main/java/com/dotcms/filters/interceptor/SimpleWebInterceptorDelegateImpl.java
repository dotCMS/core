package com.dotcms.filters.interceptor;

import com.dotmarketing.util.RegEX;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Delegate encapsulates the interceptor logic, keep the collection of interceptors and usually will be
 * called by the Web Filters.
 *
 * @author jsanca
 */
public class SimpleWebInterceptorDelegateImpl implements WebInterceptorDelegate {

    private final AtomicBoolean alreadyStarted = new AtomicBoolean(false);
    private final List<WebInterceptor> interceptors =
            new CopyOnWriteArrayList<>();

    @Override
    public void addBefore(final String webInterceptorName, final WebInterceptor webInterceptor) {

        final int indexOf = this.indexOf(webInterceptorName);

        if (-1 == indexOf) {
            // if the name does not exists, add at the end.
            this.add(webInterceptor);
        } else {
            // if the name exists insert before it
            this.add(indexOf, webInterceptor);
        }
    } // addBefore.

    @Override
    public void addAfter(final String webInterceptorName, final WebInterceptor webInterceptor) {

        final int indexOf = this.indexOf(webInterceptorName);

        if (-1 == indexOf) {
            // if the name does not exists, add at the end.
            this.add(webInterceptor);
        } else {
            // if the name exists insert after it
            this.add(indexOf + 1, webInterceptor);
        }
    } // addAfter.

    @Override
    public void add(final WebInterceptor webInterceptor) {

        this.interceptors.add(webInterceptor);
        this.init(webInterceptor);
    } // add.

    @Override
    public void add(final int order, final WebInterceptor webInterceptor) {

        if (order < 0) {

            this.addFirst(webInterceptor);
        } else if (order >= this.interceptors.size()) {

            this.add(webInterceptor); // adds at the end.
        } else {
            // adds in the order desire
            this.interceptors.add(order, webInterceptor);
            this.init(webInterceptor);
        }
    } // add

    @Override
    public void addFirst(final WebInterceptor webInterceptor) {

        this.interceptors.add(0, webInterceptor);
        this.init(webInterceptor);
    } // addFirst.

    @Override
    public void removeAll(final boolean destroy) {

        if (destroy) {

            this.destroy();
        }

        this.interceptors.clear();
    } // removeAll.

    /**
     * Call me on destroy
     */
    @Override
    public void destroy() {

        if (!this.interceptors.isEmpty()) {

            this.interceptors.forEach(interceptor -> interceptor.destroy());
        }
    } // destroy.

    /**
     * Call me on init
     */
    @Override
    public void init() {

        if (!this.interceptors.isEmpty()) {

            this.interceptors.forEach(interceptor -> interceptor.init());
        }

        this.alreadyStarted.set(true);
    } // init.

    @Override
    public boolean intercept(final HttpServletRequest request,
                             final HttpServletResponse response)
            throws IOException {

        boolean shouldContinue = true;
        Result result         = null;

        if (!this.interceptors.isEmpty()) {

            for (WebInterceptor webInterceptor : this.interceptors) {

                // if the filter applies just for some filter patterns.
                if (webInterceptor.isActive() &&
                        this.anyMatchFilter(webInterceptor, request.getRequestURI())) {

                    result          = webInterceptor.intercept(request, response);
                    shouldContinue &= (Result.SKIP_NO_CHAIN != result);

                    if (Result.NEXT != result) {
                        // if just one interceptor failed; we stopped the loop and do not continue the chain call
                        break;
                    }
                }
            }
        }

        return shouldContinue;
    } // intercept.

    private boolean anyMatchFilter(final WebInterceptor webInterceptor,
                                   final String uri) {

        final String [] filters = webInterceptor.getFilters();
        // if the interceptor does not have any filter, means apply to everything.
        boolean isOk = (null == filters);

        // If there is any filter specified, should check against each of them
        // and see if some of them match with the request path
        if (!isOk) {

            for (String filter : filters) {

                if (match(uri, filter)) {  // if some of the filter match, is enough.

                    isOk = true;
                    break;
                }
            }
        }

        return isOk;
    } // checkInterceptorFilter.

    private boolean match (final String uri, final String filterPattern) {

        String uftUri = null;

        try {

            uftUri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            uftUri = uri;
        }

        return RegEX.contains(uftUri, filterPattern);
    } // match.

    private void init(final WebInterceptor webInterceptor) {

        if (this.alreadyStarted.get()) {

            webInterceptor.init();
        }
    } // init.

    private int indexOf (final String webInterceptorName) {

        for (int i = 0; i < this.interceptors.size(); ++i) {

            if (this.interceptors.get(i).getName().equals(webInterceptorName)) {

                return i;
            }
        }

        return -1;
    } // indexOf.

    @Override
    public String toString() {
        return "SimpleWebInterceptorDelegateImpl{" +
                "alreadyStarted=" + alreadyStarted +
                ", interceptors=" + interceptors +
                '}';
    }
} // E:O:F:SimpleWebInterceptorDelegateImpl.
