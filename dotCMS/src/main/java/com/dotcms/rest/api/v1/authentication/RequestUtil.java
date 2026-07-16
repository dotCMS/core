package com.dotcms.rest.api.v1.authentication;

import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.liferay.portal.util.WebKeys;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {

    public static RequestUtil INSTANCE =
            new RequestUtil();


    private RequestUtil() {

    }

    /**
     * Create a stateless version of the request
     * pretty useful when you need to use the request out of the request thread (for instance running parallel processes that needs a copy of the request)
     * @param request HttpServletRequest
     * @return HttpServletRequest
     */
    public HttpServletRequest createStatelessRequest(final HttpServletRequest request) {

        final DotCMSMockRequest statelessRequest =
                new DotCMSMockRequestWithSession(request.getSession(false), request.isSecure());

        statelessRequest.setAttribute(WebKeys.USER, request.getAttribute(WebKeys.USER));
        statelessRequest.setAttribute(WebKeys.USER_ID, request.getAttribute(WebKeys.USER_ID));
        statelessRequest.addHeader("User-Agent", request.getHeader("User-Agent"));
        statelessRequest.addHeader("Host", request.getHeader("Host"));
        statelessRequest.addHeader("Accept-Language", request.getHeader("Accept-Language"));
        statelessRequest.addHeader("Accept-Encoding", request.getHeader("Accept-Encoding"));
        statelessRequest.addHeader("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        statelessRequest.addHeader("Origin", request.getHeader("Origin"));
        statelessRequest.addHeader("referer", request.getHeader("referer"));
        statelessRequest.setRemoteAddr(request.getRemoteAddr());
        statelessRequest.setRemoteHost(request.getRemoteHost());
        statelessRequest.setParameterMap(request.getParameterMap());

        return statelessRequest;
    }

    /**
     * Determine if the request is recycled
     * @param request
     * @return
     */
    public boolean isRecycledRequest (final HttpServletRequest request) {

        try {

            request.getHeader("X-Requested-With"); // just testing anything to see if valid
        } catch (IllegalStateException e) {
            // if throws this exception means it is recycled
            return true;
        }

        return false;
    }
}
