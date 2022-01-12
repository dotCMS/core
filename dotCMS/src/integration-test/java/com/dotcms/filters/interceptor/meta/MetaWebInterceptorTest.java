package com.dotcms.filters.interceptor.meta;

import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
}
