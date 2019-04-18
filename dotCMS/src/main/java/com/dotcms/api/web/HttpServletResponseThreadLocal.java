package com.dotcms.api.web;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * This thread local service is useful get the {@link HttpServletResponse} set in the current thread execution
 *
 * Note: keep in mind that the scope of the response is just in the current thread, any asynchronous task you raise inside the request main thread
 * won't have access to the response. You have to send explicitly the context to the new thread if you want to share the response.
 *
 * @author jsanca
 */
public class HttpServletResponseThreadLocal implements Serializable {

    private static ThreadLocal<HttpServletResponse> responseLocal =
            new ThreadLocal<>();

    public static final HttpServletResponseThreadLocal INSTANCE =
            new HttpServletResponseThreadLocal();
    /**
     * Get the response from the current thread
     * @return {@link HttpServletResponse}
     */
    public HttpServletResponse getResponse() {

        return responseLocal.get();
    }


    /**
     * Set the current {@link HttpServletResponse} to the thread pool
     * @param response  {@link HttpServletResponse}
     */
    public void setResponse(final HttpServletResponse  response) {

        responseLocal.set(response);
    }

} // E:O:F:HttpServletRequestThreadLocal.
