package com.dotcms.filters.interceptor.meta;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.StringUtils;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link ResponseMetaDataWebInterceptor}
 * @author jsanca
 */
public class MetaWebInterceptorTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * The tests below call the interceptor with a bare mock request that has no credentials, which
     * only gets the x-dot-server header when the anonymous flag is on.
     */
    @Before
    public void enableHeadersForAnonymous() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, true);
    }

    @After
    public void restoreAnonymousFlag() {
        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, null);
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it adds the header x-dot-server
     * ExpectedResult: the header is added
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server()  {

        final ResponseMetaDataWebInterceptor metaWebInterceptor = new ResponseMetaDataWebInterceptor();
        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
        metaWebInterceptor.intercept(request, mockHeaderResponse);

        final String header = mockHeaderResponse.getHeader(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER);
        Assert.assertNotNull(header);

        final String tokenHeader = "unknown" + StringPool.PIPE + StringUtils.shortify(APILocator.getServerAPI().readServerId(), 10);

        Assert.assertEquals(tokenHeader, header);
    }


    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: a request with no credentials asks for the header with nodeid=true while the
     * anonymous flag is off
     * ExpectedResult: the header is not added
     */
    @Test
    public void check_header_x_dot_server_nodeid_param_without_credentials()  {

        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, false);
        Mockito.when(request.getParameter("nodeid")).thenReturn("true");

        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
        new ResponseMetaDataWebInterceptor().intercept(request, mockHeaderResponse);

        Assert.assertNull(mockHeaderResponse.getHeader(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER));
    }

    /**
     * Method to test: {@link SdkVersionWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: a real API token that dotCMS issued is sent as a bearer token, first while it is
     * valid and then after it has been revoked, with the anonymous flag off
     * ExpectedResult: both version headers are added while the token is valid and none once it is
     * revoked, which shows the token is validated and not only checked for presence
     */
    @Test
    public void testSdkVersionHeaders_whenApiTokenIsRevoked_shouldNotAddVersionHeaders() throws Exception {

        Config.setProperty(FeatureFlagName.FEATURE_FLAG_RESPONSE_META_HEADERS_FOR_ANONYMOUS, false);
        final User user = new UserDataGen().nextPersisted();
        final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(
                ApiToken.builder().withIssuer(ClusterFactory.getClusterId())
                        .withExpires(Date.from(Instant.now().plus(Duration.ofDays(1))))
                        .withUserId(user.getUserId())
                        .withRequestingUserId(APILocator.systemUser().getUserId())
                        .withRequestingIp("127.0.0.1").build(),
                APILocator.systemUser());
        final String jwt = APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());

        Assert.assertTrue("The version headers are sent for a valid token, so the token path is live",
                versionHeadersAreSent(jwt));

        APILocator.getApiTokenAPI().revokeToken(token, APILocator.systemUser());
        // Revocation takes effect once its timestamp has passed, as in ApiTokenAPITest
        DateUtil.sleep(1000);

        Assert.assertFalse("A revoked token gets no version headers", versionHeadersAreSent(jwt));
    }

    /**
     * Sends the token through {@link SdkVersionWebInterceptor} as a bearer token on a fresh request
     * and tells whether the version header was added to the response.
     */
    private static boolean versionHeadersAreSent(final String jwt) {
        final HttpServletRequest tokenRequest = mock(HttpServletRequest.class);
        Mockito.when(tokenRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(mock(HttpServletResponse.class));

        new SdkVersionWebInterceptor().intercept(tokenRequest, mockHeaderResponse);

        return mockHeaderResponse.getHeader(SdkVersionWebInterceptor.DOTCMS_VERSION_HEADER) != null;
    }

    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it does not adds the header x-dot-server
     * ExpectedResult: the header is not added
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server_config_disable()  {

        try {
            Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, false);
            final ResponseMetaDataWebInterceptor metaWebInterceptor = new ResponseMetaDataWebInterceptor();
            final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
            metaWebInterceptor.intercept(request, mockHeaderResponse);

            final String header = mockHeaderResponse.getHeader(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER);
            Assert.assertNull(header);
        } finally {

            Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, true);
        }
    }


    /**
     * Method to test: {@link ResponseMetaDataWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it adds the header x-dot-server, but do not includes the node name (unknown instead)
     * ExpectedResult: the header is added without node name
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server_config_disable_node_name()  {

        try {
            Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, false);
            final ResponseMetaDataWebInterceptor metaWebInterceptor = new ResponseMetaDataWebInterceptor();
            final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
            metaWebInterceptor.intercept(request, mockHeaderResponse);

            final String header = mockHeaderResponse.getHeader(ResponseMetaDataWebInterceptor.X_DOT_SERVER_HEADER);
            Assert.assertNotNull(header);
            Assert.assertTrue(header.startsWith("unknown|"));
        } finally {

            Config.setProperty(ResponseMetaDataWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, true);
        }
    }
}
