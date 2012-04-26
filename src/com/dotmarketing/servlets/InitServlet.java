package com.dotmarketing.servlets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.SystemUtils;
import org.apache.lucene.search.BooleanQuery;
import org.quartz.SchedulerException;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.loggers.mbeans.Log4jConfig;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.plugin.PluginLoader;
import com.dotmarketing.portlets.campaigns.factories.CampaignFactory;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.quartz.job.ShutdownHookThread;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.ReleaseInfo;

public class InitServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    PermissionAPI             permissionAPI    = APILocator.getPermissionAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

    //	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Config
    //			.getIntProperty("EXEC_NUM_OF_THREAD"));

    /**
     * @param permissionAPI
     *            the permissionAPI to set
     */
    public void setPermissionAPI(PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
    }

    public void destroy() {
    	new ESClient().shutDownNode();
        Logger.info(this, "dotCMS shutting down");

        
    }

    public static Date startupDate;

    /**
     * Description of the Method
     *
     * @throws DotDataException
     */
    public void init(ServletConfig config) throws ServletException {

        startupDate = new java.util.Date();
        // Config class Initialization
//        Config.initializeConfig();
//        com.dotmarketing.util.Config.setMyApp(config.getServletContext());
        
       
		
        Company company = PublicCompanyFactory.getDefaultCompany();
        TimeZone companyTimeZone = company.getTimeZone();
        TimeZone.setDefault(companyTimeZone);
        Logger.info(this, "InitServlet: Setting Default Timezone: " + companyTimeZone.getDisplayName());

        String _dbType = DbConnectionFactory.getDBType();
        String _dailect = "";
		try {
			_dailect = HibernateUtil.getDialect();
		} catch (DotHibernateException e3) {
			Logger.error(InitServlet.class, e3.getMessage(), e3);
		}
        String _companyId = PublicCompanyFactory.getDefaultCompanyId();
        Logger.info(this, "");
        Logger.info(this, "   Initializing dotCMS");
        Logger.info(this, "   Using database: " + _dbType);
        Logger.info(this, "   Using dialect : " + _dailect);
        Logger.info(this, "   Company Name  : " + _companyId);
        Logger.info(this, "");
        
        String classPath = null;
        	classPath = config.getServletContext().getRealPath("WEB-INF/lib");
        
        new PluginLoader().loadPlugins(config.getServletContext().getRealPath("."),classPath);        
        
        //Check and start the ES Content Store
        APILocator.getContentletIndexAPI().checkAndInitialiazeIndex();
        
        int mc = Config.getIntProperty("lucene_max_clause_count", 4096);
        BooleanQuery.setMaxClauseCount(mc); 
        
        ImportAuditUtil.voidValidateAuditTableOnStartup();
       
        // Set up the database
//        try {
//            DotCMSInitDb.InitializeDb();
//        } catch (DotDataException e1) {
//            throw new ServletException(e1);
//        }

      

        // set the application context for use all over the site
        Logger.debug(this, "");
        Logger.debug(this, "InitServlet: Setting Application Context!!!!!!");

        // creates the velocity folders to make sure they are there
        new java.io.File(ConfigUtils.getDynamicVelocityPath() + File.separator + "live").mkdirs();
        new java.io.File(ConfigUtils.getDynamicVelocityPath() + File.separator + "working").mkdirs();

        // deletes all menues that have been generated
        RefreshMenus.deleteMenus();


        // maps all virtual links in memory
        VirtualLinksCache.mapAllVirtualLinks();

        Language language = langAPI.getDefaultLanguage();

        if (language.getId() == 0) {
            Logger.debug(this, "Creating Default Language");
            langAPI.createDefaultLanguage();
        }

        try {
			DotInitScheduler.start();
		} catch (SchedulerException e2) {
			Logger.fatal(InitServlet.class, e2.getMessage(), e2);
			throw new ServletException(e2.getMessage(), e2);
		}

        //Adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());

        /*
         * Delete the files out of the temp dir (this gets huge)
         */

        deleteFiles(new File(SystemUtils.JAVA_IO_TMPDIR));

        /*
         * unlocking campaigns
         */
        try {
			CampaignFactory.unlockAllCampaigns();
		} catch (DotHibernateException e2) {
			Logger.error(InitServlet.class, e2.getMessage(), e2);
			throw new ServletException("Unable to Unlock Campaigns", e2);
		}

        // runs the InitThread

        InitThread it = new InitThread();
        it.start();

        //Ensure the system host is in the system
        try {
			APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotDataException e1) {
			Logger.fatal(InitServlet.class, e1.getMessage(), e1);
			throw new ServletException("Unable to initialize system host", e1);
		} catch (DotSecurityException e) {
			Logger.fatal(InitServlet.class, e.getMessage(), e);
			throw new ServletException("Unable to initialize system host", e);
		}
		
		try {
			APILocator.getFolderAPI().findSystemFolder();
		} catch (DotDataException e1) {
			Logger.error(InitServlet.class, e1.getMessage(), e1);
			throw new ServletException("Unable to initialize system folder", e1);
		}
		
		

        /*
         * SHOULD BE LAST THING THAT HAPPENS
         */
        try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e1) {
			Logger.error(InitServlet.class, e1.getMessage(), e1);
		}
        
                
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
				ObjectName name = new ObjectName("org.dotcms:type=Log4J"); 
				Log4jConfig mbean = new Log4jConfig(); 
				mbs.registerMBean(mbean, name);
			} catch (MalformedObjectNameException e) {
				Logger.debug(InitServlet.class,"MalformedObjectNameException: " + e.getMessage(),e);
			} catch (InstanceAlreadyExistsException e) {
				Logger.debug(InitServlet.class,"InstanceAlreadyExistsException: " + e.getMessage(),e);
			} catch (MBeanRegistrationException e) {
				Logger.debug(InitServlet.class,"MBeanRegistrationException: " + e.getMessage(),e);
			} catch (NotCompliantMBeanException e) {
				Logger.debug(InitServlet.class,"NotCompliantMBeanException: " + e.getMessage(),e);
			} catch (NullPointerException e) {
				Logger.debug(InitServlet.class,"NullPointerException: " + e.getMessage(),e);
			}		
			
			
			//Just get the Engine to make sure it gets inited on time before the first request
			VelocityUtil.getEngine();



    }

    protected void deleteFiles(java.io.File directory) {
        if (directory.isDirectory()) {
            // get all files for this directory
            java.io.File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                // deletes all files on the directory
                ((java.io.File) files[i]).delete();
            }
        }
    }

    public static Date getStartupDate() {
        return startupDate;
    }
/**
 *
 * @author will
 * This thread will fire and send all the configured host names to dotcms.org for internal
 * corporate information (we are dying to know who is using dotCMS!).
 * To turn this off, set the dotmarketing-config.properties
 * INIT_THREAD_DOTCMS = false
 *
 */

    public class InitThread extends Thread {

        public void run() {
        	try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                Logger.debug(this,e.getMessage(),e);
            }
            String address = null;
            String hostname = "unknown";
            try {
                InetAddress addr = InetAddress.getLocalHost();
                // Get IP Address
                byte[] ipAddr = addr.getAddress();
                addr = InetAddress.getByAddress(ipAddr);
                address = addr.getHostAddress();
                // Get hostname
                hostname = addr.getHostName();
            } catch (Exception e) {
                Logger.debug(this, "InitThread broke:", e);
            }
            try{

            	HostAPI hostAPI = APILocator.getHostAPI();
                String defaultHost = hostAPI.findDefaultHost(APILocator.getUserAPI().getSystemUser(), false).getHostname();
                StringBuilder sb = new StringBuilder();
                List<Host> hosts = hostAPI.findAll(APILocator.getUserAPI().getSystemUser(), false);
                for (Host h : hosts) {
                    sb.append(h.getHostname() + "\n");
                    if (UtilMethods.isSet(h.getAliases())) {
                        sb.append(h.getAliases());
                    }
                }

                // Construct data
                StringBuilder data = new StringBuilder();
                data.append(URLEncoder.encode("ipAddr", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(address, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("hostname", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(hostname, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("defaultHost", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(defaultHost, "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("allHosts", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(sb.toString(), "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("version", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(ReleaseInfo.getReleaseInfo(), "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("build", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(ReleaseInfo.getBuildNumber()), "UTF-8"));
                // Send data

                sb.delete(0, sb.length());
                sb.append("h");
                sb.append("tt");
                sb.append("p");
                sb.append(":");
                sb.append("//");
                sb.append("p");
                sb.append("i");
                sb.append("n");
                sb.append("g");
                sb.append(".");
                sb.append("d");
                sb.append("ot");
                sb.append("cms");
                sb.append(".");
                sb.append("or");
                sb.append("g/");
                sb.append("servlets/TB");
                sb.append("Information");
                URL url = new URL(sb.toString());
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//                conn.connect();

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(data.toString());
                wr.flush();
                wr.close();
                DataInputStream input = new DataInputStream(conn.getInputStream());
                input.close();
//                conn.getContent();

            } catch (UnknownHostException e) {
                Logger.debug(this, "Unable to get Hostname", e);
            } catch (Exception e) {
                Logger.debug(this, "InitThread broke:", e);
            }
            finally{
                try {
					HibernateUtil.closeSession();
				} catch (DotHibernateException e) {
					Logger.error(InitServlet.class, e.getMessage(), e);
				}
            }

        }

    }
}
