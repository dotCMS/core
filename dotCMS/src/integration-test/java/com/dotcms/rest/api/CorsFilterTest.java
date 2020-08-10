package com.dotcms.rest.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;

public class CorsFilterTest {
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

    }
    
    @Test
    public void test_cors_filter_header_capitalizer() {
        
        final CorsFilter corsFilter = new CorsFilter();
        final String[] possibleHeaders = new String[] {"does-this-work", "DoEs-tHIS-WORK", "DOES-THIS-WORK", " does-this-Work "};
        final String correctHeader = "Does-This-Work";

        for(String head : possibleHeaders) {
            final String fixed = corsFilter.fixHeaderCase(head);
            assertEquals(fixed, correctHeader);
        }

    }
    
    
    @Test
    public void test_cors_filter_headers_returned_by_resource() {
        

        
        final String[] contentResourceKeyValue = new String[] {"api.cors.contentresource.Access-Control-Allow-Origin", "https://demo.dotcms.com"};
        final String[] defaultKeyValue1 = new String[] {"api.cors.default.Access-Control-Allow-Origin", "https://test2"};
        final String[] defaultKeyValue2 = new String[] {"api.cors.default.Access-Control-Allow-Methods", "GET,HEAD,POST,PUT,DELETE,OPTIONS"};
        

        
        Config.setProperty(contentResourceKeyValue[0], contentResourceKeyValue[1]);
        Config.setProperty(defaultKeyValue1[0], defaultKeyValue1[1]);
        Config.setProperty(defaultKeyValue2[0], defaultKeyValue2[1]);
        
        final CorsFilter corsFilter = new CorsFilter();
        
        // Make sure we get specific headers
        List<String[]> results = corsFilter.getHeaders("contentresource");
        assertTrue(results.size()==1);
        assertEquals(results.get(0)[0], "Access-Control-Allow-Origin");
        assertEquals(results.get(0)[1], "https://demo.dotcms.com");

        // Make sure we get the defaults if resource does not exist
        results = corsFilter.getHeaders("this-does-not-exist");
        assertTrue(results.size()>=2);
        assertEquals(results.get(3)[0], "Access-Control-Allow-Origin");
        assertEquals(results.get(3)[1], "https://test2");
        assertEquals(results.get(4)[0], "Access-Control-Allow-Methods");
        assertEquals(results.get(4)[1], "GET,HEAD,POST,PUT,DELETE,OPTIONS");
        
        
    }
}
