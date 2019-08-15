package com.dotcms.util;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import javax.servlet.ServletContext;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.dotcms.repackage.com.google.common.io.Files;
import com.dotmarketing.util.Config;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import com.liferay.portal.util.WebAppPool;

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

            final ModuleConfig config = Mockito.mock(ModuleConfig.class);
            Mockito.when(context.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
            
            // Set up language.properties files
            final MultiMessageResources messages = (MultiMessageResources) new MultiMessageResourcesFactory().createResources("messages.Language,messages.Language-ext,messages.cms_language");
            messages.setServletContext(context);
            WebAppPool.put("dotcms.org", Globals.MESSAGES_KEY, messages);
            Mockito.when(context.getAttribute(Globals.MESSAGES_KEY)).thenReturn(messages);

            final String topPath = Files.createTempDir().getCanonicalPath();
            Mockito.when(context.getRealPath(Matchers.anyString())).thenAnswer(new Answer<String>() {
            //Mockito.when(context.getRealPath(Matchers.matches("^(?!/WEB-INF/felix)(?:[\\S\\s](?!/WEB-INF/felix))*+$"))).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    String path = (String) invocation.getArguments()[0];
                    path = topPath + path.replaceAll("/", File.separator);

                    return path;
                }
            });
            Mockito.when(context.getResource(Matchers.anyString())).thenAnswer(new Answer<URL>() {
            //Mockito.when(context.getRealPath(Matchers.matches("^(?!/WEB-INF/felix)(?:[\\S\\s](?!/WEB-INF/felix))*+$"))).thenAnswer(new Answer<String>() {
                @Override
                public URL answer(InvocationOnMock invocation) throws Throwable {
                  final String path = (String) invocation.getArguments()[0];
                  
                  URL url = MultiMessageResources.class.getClassLoader().getResource(path);
                  if(url==null) {
                    String workingDir=new File(".").getAbsolutePath();
                    System.out.println("Working Directory = " + workingDir);

                    
                    String newPath  = workingDir + File.separator + 
                    "src" + File.separator + 
                    "main"  + File.separator +
                    "webapp"  + path;
                    System.out.println("path      :" + path);
                    System.out.println("workingDir:" + workingDir);
                    System.out.println("newPath   :" + newPath);
                    if(new File(newPath).exists()) {
                      return new URL("file://" + newPath);
                    }
                  }
                  return url;
                }
            });
            
           
            Config.CONTEXT = context;
            Config.CONTEXT_PATH = context.getRealPath("/");

        }

        props = null;
        dotmarketingPropertiesUrl = getUrlToTestResource("it-dotmarketing-config.properties");
        clusterPropertiesUrl = getUrlToTestResource("it-dotcms-config-cluster.properties");
        setToolboxPath();
    }

    private static void setToolboxPath() throws IOException {
        String toolboxManagerPath = Config.getStringProperty("TOOLBOX_MANAGER_PATH");
        File toolboxManager= new File(toolboxManagerPath);
        if(toolboxManager.exists()){
          Mockito.when(Config.CONTEXT.getResourceAsStream(toolboxManagerPath)).thenReturn(
                  java.nio.file.Files.newInputStream(Paths.get(toolboxManagerPath)));
        }
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
