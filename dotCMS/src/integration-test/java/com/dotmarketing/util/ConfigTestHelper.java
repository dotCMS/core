package com.dotmarketing.util;

import com.dotcms.repackage.com.google.common.io.Files;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletContext;

/**
 * Sets configuration and environment details used for Integration Tests
 * Created by nollymar on 9/14/16.
 */
public class ConfigTestHelper extends Config {

    /**
     * This method will set up a dummy ServletContext needed for testing. The main purpose here
     * is to be able to run the integration tests without the web app container i.e. Tomcat.
     */
    public static void _setupFakeTestingContext() throws Exception {
        // if we need a fake ServletContext
        if (CONTEXT == null) {
            ServletContext context = Mockito.mock(ServletContext.class);
            Mockito.when(context.getInitParameter("company_id")).thenReturn("dotcms.org");

            final String topPath = Files.createTempDir().getCanonicalPath();
            Mockito.when(context.getRealPath(Matchers.anyString())).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    String path = (String) invocation.getArguments()[0];
                    path = topPath + path.replaceAll("/", File.separator);

                    return path;
                }
            });
            Config.CONTEXT = context;

        }
        dotmarketingPropertiesUrl = getUrlToTestResource("it-dotmarketing-config.properties");
        clusterPropertiesUrl = getUrlToTestResource("it-dotcms-config-cluster.properties");
    }
    
    /**
     * URL to known resource on the test-resources path, now on "bin"
     * @param resource resource name
     * @return
     */
    public static URL getUrlToTestResource(String resource){
    	return Thread.currentThread().getContextClassLoader().getResource(resource);
    }
    
    /**
     * Path to known resource on the test-resources path, now on "bin"
     * @param resource resource name
     * @return
     */
	public static String getPathToTestResource(String resource){
		return getUrlToTestResource(resource).getPath();	
	}
}
