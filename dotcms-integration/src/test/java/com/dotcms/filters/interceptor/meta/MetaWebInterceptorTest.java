package com.dotcms.filters.interceptor.meta;

import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.StringUtils;
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
import java.util.Collections;

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
