package com.dotcms.datagen;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.liferay.util.Base64;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility class that provides common-use methods for mocking and changing the behavior of the HTTP
 * Request object.
 *
 * @author Jose Castro
 * @since Sep 14th, 2023
 */
public class HttpRequestUtil {

    /**
     * Mocks the {@link HttpServletRequest} object with specific parameters that represent an
     * authenticated request using Basic Auth.
     *
     * @param incomingHostname The Site that the request is going to.
     * @param incomingUri      The URI of the request.
     * @param userEmail        The email of the user that is making the request.
     * @param password         The password of the user that is making the request.
     *
     * @return The mocked {@link HttpServletRequest} object.
     */
    public static HttpServletRequest getHttpRequest(final String incomingHostname, final String incomingUri, final String userEmail, final String password) {
        final String userEmailAndPassword = userEmail + ":" + password;
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        //new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(incomingHostname, incomingUri).request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + Base64.encode(userEmailAndPassword.getBytes()));
        return request;
    }

}
