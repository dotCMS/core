package com.dotcms.graphql;

import static org.junit.Assert.*;
import java.util.HashMap;
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
        
        HashMap<String,String> headers = new DotGraphQLHttpServlet().corsHeaders.apply();
        
        assertEquals(headers.get("access-control-allow-origin"), "*");
        assertEquals(headers.get("access-control-allow-credentials"), "true");
        assertEquals(headers.get("access-control-allow-headers"), "*");
        assertEquals(headers.get("access-control-allow-methods"), "GET,PUT,POST,DELETE,HEAD,OPTIONS,PATCH");
        
        // this property is specifically overriden for graphql
        assertEquals(headers.get("access-control-expose-headers"), "Content-Type,Cache-Control");

        
    }

}
