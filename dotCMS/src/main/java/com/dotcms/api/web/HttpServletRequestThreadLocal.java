package com.dotcms.api.web;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * This thread local service is useful get the {@link javax.servlet.http.HttpServletRequest} set in the current thread execution
 *
 * Note: keep in mind that the scope of the request is just in the current thread, any asynchronous task you raise inside the request main thread
 * won't have access to the request. You have to send explicitly the context to the new thread if you want to share the request.
 *
 * @author jsanca
 */
public class HttpServletRequestThreadLocal implements Serializable {

    private static ThreadLocal<HttpServletRequest> requestLocal =
            new ThreadLocal<>();

    public static final HttpServletRequestThreadLocal INSTANCE =
            new HttpServletRequestThreadLocal();
    /**
     * Get the annotations for the method resources added in the current thread
     * @return {@link HttpServletRequest}
     */
    public HttpServletRequest getRequest () {

        return requestLocal.get();
    }


    /**
     * Set the current {@link HttpServletRequest} to the thread pool
     * @param request  {@link HttpServletRequest}
     */
    public void setRequest (final HttpServletRequest  request) {

        requestLocal.set(request);
    }

} // E:O:F:HttpServletRequestThreadLocal.
