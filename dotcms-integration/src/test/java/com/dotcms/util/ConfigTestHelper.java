package com.dotcms.util;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import com.liferay.portal.util.WebAppPool;
import com.liferay.util.FileUtil;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

            // make sure the tmp dir is there before running
            new File(System.getProperty("java.io.tmpdir")).mkdirs();
            final String topPath = Files.createTempDirectory("config_test_helper").toAbsolutePath().toString();

            final String velocityPath = VelocityUtil.getVelocityRootPath();
            copyVelocityFolder(topPath, velocityPath);

            Path testRoot = Paths.get(Config.getStringProperty("TEST_WEBAPP_ROOT","src/main/webapp")).normalize().toAbsolutePath();

            String[] contextRoots = new String[]{topPath,testRoot.toString()};

            Mockito.when(context.getRealPath(ArgumentMatchers.anyString())).thenAnswer(new Answer<String>() {
            //Mockito.when(context.getRealPath(Matchers.matches("^(?!/WEB-INF/felix)(?:[\\S\\s](?!/WEB-INF/felix))*+$"))).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    String path = (String) invocation.getArguments()[0];
                    Path fullPath = getTestFilePath(path, contextRoots);
                    return (fullPath!=null) ? fullPath.toString() : null;
                }
            });
            Mockito.when(context.getResource(ArgumentMatchers.anyString())).thenAnswer(new Answer<URL>() {
            //Mockito.when(context.getRealPath(Matchers.matches("^(?!/WEB-INF/felix)(?:[\\S\\s](?!/WEB-INF/felix))*+$"))).thenAnswer(new Answer<String>() {
                @Override
                public URL answer(InvocationOnMock invocation) throws Throwable {
                    String path = (String) invocation.getArguments()[0];
                    Path fullPath = getTestFilePath(path, contextRoots);
                    if (fullPath!=null)
                        return fullPath.toUri().toURL();
                    else
                        return null;
                }
            });


            Config.CONTEXT = context;

        }

        props.clear();
        dotmarketingPropertiesUrl = getUrlToTestResource("it-dotmarketing-config.properties");
        clusterPropertiesUrl = getUrlToTestResource("it-dotcms-config-cluster.properties");
        setToolboxPath();
    }


    private static Path getTestFilePath(String contextPath,String[] contextRoots){
        if (!contextPath.startsWith("/"))
            throw new IllegalArgumentException("Resource path must start with a / and is relative to context root");
        else
            contextPath = contextPath.substring(1);

        for (String root : contextRoots) {
            Path path = Paths.get(root,contextPath);
            if(Files.exists(path)) {
               return path.normalize().toAbsolutePath();
            }
        }

        return null;
    }
    /*
    * Copy the velocity code to the temporal directory
     */
    private static void copyVelocityFolder(final String topPath, final String velocityPath) throws IOException {

        final File topPathDirectory = new File(topPath);
        final File velocityDirectory = new File(velocityPath);
        if (velocityDirectory.exists() && velocityDirectory.canRead()) {

            FileUtil.copyDirectory(velocityDirectory, new File(topPathDirectory, "/WEB-INF/velocity"));
        } else {

            final String userDirPath = System.getProperty("user.dir");
            final String userDirPathComplete = userDirPath + "/src/main/webapp/WEB-INF/velocity";
            final File velocityDirectory2 = new File(userDirPathComplete);
            if (velocityDirectory2.exists() && velocityDirectory2.canRead()) {

                FileUtil.copyDirectory(velocityDirectory2, new File(topPathDirectory, "/WEB-INF/velocity"));
            }
        }

    }

    private static void setToolboxPath() throws IOException {
        String toolboxManagerPath = Config.getStringProperty("TOOLBOX_MANAGER_PATH","/WEB-INF/toolbox.xml");
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
