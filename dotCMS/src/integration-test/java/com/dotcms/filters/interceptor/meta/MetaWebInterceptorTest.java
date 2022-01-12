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
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link MetaWebInterceptor}
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
     * Method to test: {@link MetaWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it adds the header x-dot-server
     * ExpectedResult: the header is added
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server()  {

        final MetaWebInterceptor metaWebInterceptor = new MetaWebInterceptor();
        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
        metaWebInterceptor.intercept(request, mockHeaderResponse);

        final String header = mockHeaderResponse.getHeader(MetaWebInterceptor.X_DOT_SERVER_HEADER);
        Assert.assertNotNull(header);

        final Object nodeName =  Try.of(() -> ClusterUtilProxy.getNodeInfo()).
                getOrElse(Collections.emptyMap()).getOrDefault("friendlyName", "unknown");
        String tokenHeader = nodeName + StringPool.PIPE + StringUtils.shortify(APILocator.getServerAPI().readServerId(), 10);

        Assert.assertEquals(tokenHeader, header);
    }


    /**
     * Method to test: {@link MetaWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it does not adds the header x-dot-server
     * ExpectedResult: the header is not added
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server_config_disable()  {

        Config.setProperty(MetaWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, false);
        final MetaWebInterceptor metaWebInterceptor = new MetaWebInterceptor();
        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
        metaWebInterceptor.intercept(request, mockHeaderResponse);

        final String header = mockHeaderResponse.getHeader(MetaWebInterceptor.X_DOT_SERVER_HEADER);
        Assert.assertNull(header);
        Config.setProperty(MetaWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID, true);
    }


    /**
     * Method to test: {@link MetaWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: calling it adds the header x-dot-server, but do not includes the node name (unknown instead)
     * ExpectedResult: the header is added without node name
     * @throws IOException
     */
    @Test
    public void check_header_x_dot_server_config_disable_node_name()  {

        Config.setProperty(MetaWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, false);
        final MetaWebInterceptor metaWebInterceptor = new MetaWebInterceptor();
        final MockHeaderResponse mockHeaderResponse = new MockHeaderResponse(response);
        metaWebInterceptor.intercept(request, mockHeaderResponse);

        final String header = mockHeaderResponse.getHeader(MetaWebInterceptor.X_DOT_SERVER_HEADER);
        Assert.assertNotNull(header);
        Assert.assertTrue(header.startsWith("unknown|"));
        Config.setProperty(MetaWebInterceptor.RESPONSE_HEADER_ADD_NODE_ID_INCLUDE_NODE_NAME, true);
    }
}
