package com.dotcms.graphql;

import static org.junit.Assert.*;

import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHeaderResponse;
import com.dotcms.mock.response.MockHttpResponse;
import java.io.IOException;
import java.util.HashMap;
import javax.servlet.ServletException;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;

public class DotGraphQLHttpServletTest {

    
    @BeforeClass
    public static void prepare() throws Exception{

        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testing_cors_headers() {
        
        HashMap<String,String> headers = new DotGraphQLHttpServlet().corsHeaders.get();
        
        assertEquals(headers.get("access-control-allow-origin"), "*");
        assertEquals(headers.get("access-control-allow-credentials"), "true");
        assertEquals(headers.get("access-control-allow-headers"), "*");
        assertEquals(headers.get("access-control-allow-methods"), "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");
        
        // this property is specifically overriden for graphql
        assertEquals(headers.get("access-control-expose-headers"), "Content-Type,Cache-Control");
    }

    @Test
    public void testing_GETRequestToGraphQLServer_returnResponseWithExpectedHeaders()
            throws ServletException, IOException {

        MockHttpRequestIntegrationTest request = new MockHttpRequestIntegrationTest("localhost", "/?qid=123abc");
        MockHeaderResponse response = new MockHeaderResponse(new MockHttpResponse());
        DotGraphQLHttpServlet graphQLHttpServlet = new DotGraphQLHttpServlet();
        graphQLHttpServlet.init(null);
        graphQLHttpServlet.doGet(request.request(), response);

        assertEquals(response.getHeader("access-control-allow-origin"), "*");
        assertEquals(response.getHeader("access-control-allow-credentials"), "true");
        assertEquals(response.getHeader("access-control-allow-headers"), "*");
        assertEquals(response.getHeader("access-control-allow-methods"), "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");

        // this property is specifically overriden for graphql
        assertEquals(response.getHeader("access-control-expose-headers"), "Content-Type,Cache-Control");
    }

    @Test
    public void testing_POSTRequestToGraphQLServer_returnResponseWithExpectedHeaders()
            throws ServletException, IOException {

        MockHttpRequestIntegrationTest request = new MockHttpRequestIntegrationTest("localhost", "/");
        MockHeaderResponse response = new MockHeaderResponse(new MockHttpResponse());
        DotGraphQLHttpServlet graphQLHttpServlet = new DotGraphQLHttpServlet();
        graphQLHttpServlet.init(null);
        graphQLHttpServlet.doPost(request.request(), response);

        assertEquals(response.getHeader("access-control-allow-origin"), "*");
        assertEquals(response.getHeader("access-control-allow-credentials"), "true");
        assertEquals(response.getHeader("access-control-allow-headers"), "*");
        assertEquals(response.getHeader("access-control-allow-methods"), "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");

        // this property is specifically overriden for graphql
        assertEquals(response.getHeader("access-control-expose-headers"), "Content-Type,Cache-Control");
    }

    @Test
    public void testing_OPTIONSRequestToGraphQLServer_returnResponseWithExpectedHeaders()
            throws ServletException, IOException {

        MockHttpRequestIntegrationTest request = new MockHttpRequestIntegrationTest("localhost", "/");
        MockHeaderResponse response = new MockHeaderResponse(new MockHttpResponse());
        DotGraphQLHttpServlet graphQLHttpServlet = new DotGraphQLHttpServlet();
        graphQLHttpServlet.init(null);
        graphQLHttpServlet.doOptions(request.request(), response);

        assertEquals(response.getHeader("access-control-allow-origin"), "*");
        assertEquals(response.getHeader("access-control-allow-credentials"), "true");
        assertEquals(response.getHeader("access-control-allow-headers"), "*");
        assertEquals(response.getHeader("access-control-allow-methods"), "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");

        // this property is specifically overriden for graphql
        assertEquals(response.getHeader("access-control-expose-headers"), "Content-Type,Cache-Control");
    }

}
