package com.dotcms.rendering.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ActionletUtil {

    private ActionletUtil () {}

    /**
     * Returns the current request (if exist) or a mock request
     * @param currentUser {@link User}
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest (final User currentUser) {
        return null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                ActionletUtil.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
    }

    /**
     * Returns a mock request
     * @param currentUser {@link User}
     * @return HttpServletRequest
     */
    public static HttpServletRequest mockRequest (final User currentUser) {

        final Host host = Try.of(()-> APILocator.getHostAPI()
                .findDefaultHost(currentUser, false)).getOrElse(APILocator.systemHost());
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new FakeHttpRequest(host.getHostname(), StringPool.FORWARD_SLASH).request()
                ).request()
        ).request();
    }

    /**
     * Returns the current request (if exist) or a mock request
     * @return HttpServletRequest
     */
    public static HttpServletResponse getResponse () {
        return null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();
    }

    /**
     * Returns a mock response
     * @return HttpServletResponse
     */
    public static HttpServletResponse mockResponse () {

        return new BaseResponse().response();
    }
}
