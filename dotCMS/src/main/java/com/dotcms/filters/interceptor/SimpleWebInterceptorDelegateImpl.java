package com.dotcms.filters.interceptor;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.config.DotRestApplication;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import io.vavr.Lazy;
import org.apache.commons.collections.iterators.ReverseListIterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Delegate encapsulates the interceptor logic, keep the collection of interceptors and usually will be
 * called by the Web Filters.
 *
 * @author jsanca
 */
public class SimpleWebInterceptorDelegateImpl implements WebInterceptorDelegate {

    private final AtomicBoolean alreadyStarted      =
            new AtomicBoolean(false);
    private final List<WebInterceptor> interceptors =
            new CopyOnWriteArrayList<>();
    private OrderMode orderMode = OrderMode.FILO;
    private final AtomicBoolean reverseOrderForPostInvoke =
            new AtomicBoolean(false);

    private static final Lazy<Boolean> ENABLE_TELEMETRY_FROM_CORE = Lazy.of(() ->
            Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_TELEMETRY_CORE_ENABLED, true));

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
        if (Boolean.TRUE.equals(ENABLE_TELEMETRY_FROM_CORE.get()) &&
                webInterceptor.getClass().getName().equalsIgnoreCase("com.dotcms.experience.collectors.api.ApiMetricWebInterceptor")) {
            Logger.warn(DotRestApplication.class, "Bypassing addition of API Metric Web Interceptor from OSGi");
            return;
        }
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

    @Override
    public void remove(final String webInterceptorName, final boolean destroy) {

        WebInterceptor interceptor = null;
        final int index = this.indexOf(webInterceptorName);

        if (-1 != index) {

            interceptor =
                    this.interceptors.get(index);

            if (destroy) {

                interceptor.destroy();
            }

            this.interceptors.remove(index);
        }
    }

    public void move(final String webInterceptorName, int index){
        if (index >= 0 && index <= this.interceptors.size()) {
            final int currentIndex = this.indexOf(webInterceptorName);

            if (-1 != currentIndex) {
                WebInterceptor webInterceptorRemoved = this.interceptors.remove(currentIndex);
                this.add(index, webInterceptorRemoved);
            }
        }else{
            throw new IndexOutOfBoundsException();
        }
    }

    public void moveToFirst(final String webInterceptorName){
        move(webInterceptorName, 0);
    }

    public void moveToLast(final String webInterceptorName){
        move(webInterceptorName, this.interceptors.size());
    }

    public void orderMode(final OrderMode orderMode) {

        this.orderMode = orderMode;
    }

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
    public DelegateResult intercept(final HttpServletRequest request,
                             final HttpServletResponse response)
            throws IOException {

        Result  result                     = null;
        boolean shouldContinue             = true;
        HttpServletRequest  requestResult  = request;
        HttpServletResponse responseResult = response;


        if (!this.interceptors.isEmpty()) {

            for (final WebInterceptor webInterceptor : this.interceptors) {

                // if the filter applies just for some filter patterns.
                if (webInterceptor.isActive() &&
                        this.anyMatchFilter(webInterceptor, request.getRequestURI())) {

                    result          = webInterceptor.intercept(requestResult, responseResult);
                    shouldContinue &= (Result.Type.SKIP_NO_CHAIN != result.getType());

                    if (null != result.getRequest()) {

                        requestResult  = result.getRequest();
                    }

                    if (null != result.getResponse()) {

                        responseResult  = result.getResponse();
                    }

                    if (Result.Type.NEXT != result.getType()) {
                        // if just one interceptor failed; we stopped the loop and do not continue the chain call
                        break;
                    }
                }
            }
        }

        return new DelegateResult(shouldContinue, requestResult, responseResult);
    } // intercept.

    @Override
    public void after(final HttpServletRequest request,
                                    final HttpServletResponse response)
            throws IOException {

        final HttpServletRequest  requestResult  = request;
        final HttpServletResponse responseResult = response;


        if (!this.interceptors.isEmpty()) {

            final ListIterator<WebInterceptor> iterator = this.orderMode == OrderMode.FILO?
                    new ReverseListIterator(this.interceptors):this.interceptors.listIterator();

            while(iterator.hasNext()) {

                final WebInterceptor webInterceptor = iterator.next();
                // if the filter applies just for some filter patterns.
                if (webInterceptor.isActive() &&
                        this.anyMatchFilter(webInterceptor, request.getRequestURI())) {

                    if (!webInterceptor.afterIntercept(requestResult, responseResult)) {

                        return;
                    }
                }
            }
        }
    } // after.

    @VisibleForTesting
    public boolean anyMatchFilter(final WebInterceptor webInterceptor,
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

        return RegEX.containsCaseInsensitive(uftUri, filterPattern.trim());
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
