package com.dotmarketing.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

import com.dotmarketing.osgi.HostActivator;

public class OsgiFelixListener implements ServletContextListener {

    private Framework m_fwk;

    /**
     * Default constructor. 
     */
    public OsgiFelixListener() {
    }
    
	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent context) {
    	
    	String confDir = context.getServletContext().getRealPath("/WEB-INF");
    	String felixDir = context.getServletContext().getRealPath("/WEB-INF/felix");
    	    	
    	String url = confDir + "/felix.properties";
    	
    	System.setProperty("felix.config.properties", "file://" + url);
    	
    	System.out.println(System.getProperty("felix.config.properties"));
    	
    	String bundleDir = felixDir + "/bundle";
    	String cacheDir = felixDir + "/felix-cache";
    	String autoLoadDir = felixDir + "/load";
    	
    	// (2) Load system properties.
        Main.loadSystemProperties();
        
        // (3) Read configuration properties.
        Properties configProps = Main.loadConfigProperties();
        if (configProps == null)
        {
            System.err.println("No " + Main.CONFIG_PROPERTIES_FILE_VALUE + " found.");
            configProps = new Properties();
        }

        // (4) Copy framework properties from the system properties.
        Main.copySystemProperties(configProps);
            
        // (5) Use the specified auto-deploy directory over default.
        if (bundleDir != null) {
            configProps.setProperty(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, bundleDir);
        }

        // (6) Use the specified bundle cache directory over default.
        if (cacheDir != null) {
            configProps.setProperty(Constants.FRAMEWORK_STORAGE, cacheDir);
        }

        // (7) Add a shutdown hook to clean stop the framework.
        String enableHook = configProps.getProperty(Main.SHUTDOWN_HOOK_PROP);
        if ((enableHook == null) || !enableHook.equalsIgnoreCase("false"))
        {
            Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
                public void run()
                {
                    try {
                        if (m_fwk != null) {
                            m_fwk.stop();
                            m_fwk.waitForStop(0);
                        }
                    }
                    catch (Exception ex) {
                        System.err.println("Error stopping framework: " + ex);
                    }
                }
            });
        }

        // Create host activator;
        List<BundleActivator> list = new ArrayList<BundleActivator>();
        list.add(HostActivator.instance());
        configProps.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, list);
        
        configProps.put("felix.fileinstall.dir", autoLoadDir);
        
        try
        {
            // (8) Create an instance and initialize the framework.
            FrameworkFactory factory = getFrameworkFactory();
            m_fwk = factory.newFramework(configProps);
            m_fwk.init();

            // (9) Use the system bundle context to process the auto-deploy
            // and auto-install/auto-start properties.
            AutoProcessor.process(configProps, m_fwk.getBundleContext());
            
            // (10) Start the framework.
            m_fwk.start();
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(0);
        }
    }
    
    private static FrameworkFactory getFrameworkFactory() throws Exception {
        URL url = Main.class.getClassLoader().getResource(
        	"META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try {
                for (String s = br.readLine(); s != null; s = br.readLine()) {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#')) {
                        return (FrameworkFactory) Class.forName(s).newInstance();
                    }
                }
            }
            finally {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent context) {
    	// Stop felix
    	try {
			m_fwk.stop();
	        // (11) Wait for framework to stop to exit the VM.
	        m_fwk.waitForStop(0);
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
	
}
