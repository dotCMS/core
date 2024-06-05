package com.dotcms.filters.interceptor.meta;

import com.dotcms.enterprise.ClusterUtilProxy;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
