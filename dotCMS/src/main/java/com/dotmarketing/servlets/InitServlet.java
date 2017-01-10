package com.dotmarketing.servlets;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.org.apache.commons.lang.SystemUtils;

import org.apache.lucene.search.BooleanQuery;

import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotcms.workflow.EscalationThread;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
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
import com.dotmarketing.util.*;
import com.liferay.portal.model.Company;
import com.liferay.portal.util.ReleaseInfo;

import org.quartz.SchedulerException;

import javax.management.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

        if(Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)){

        	Logger.info(this, "   Clustering    : Enabled");

            //Get the current license level
            int licenseLevel = LicenseUtil.getLevel();
            if ( licenseLevel > 100 ) {
                //		Logger.info(this, "   Server        :" + Config.getIntProperty("DIST_INDEXATION_SERVER_ID", 0)  + " of cluster " + Config.getStringProperty("DIST_INDEXATION_SERVERS_IDS", "...unknown"));
                try {
                    /*
                     Without a license this testCluster call will fail as the LicenseManager calls the ClusterFactory.removeNodeFromCluster()
                     if a license is not found.
                     */
                    ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator().getImplementationObject()).testCluster();
                    Logger.info( this, "     Ping Sent" );
                } catch ( Exception e ) {
                    Logger.error( this, "   Ping Error: " + e.getMessage() );
                }
            }
        }
        else{
        	Logger.info(this, "   Clustering    : Disabled");
        }


        Logger.info(this, "");

        //Check and start the ES Content Store
        APILocator.getContentletIndexAPI().checkAndInitialiazeIndex();
        Logger.info(this, "");

		Logger.info(this, "");

        String classPath = config.getServletContext().getRealPath("/WEB-INF/lib");

    	new PluginLoader().loadPlugins(config.getServletContext().getRealPath("/"),classPath);









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

        //Used com.dotmarketing.viewtools.NavigationWebAPI
        String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
        String menuVLTPath = velocityRootPath + "menus" + java.io.File.separator;

        java.io.File fileFolder = new java.io.File(menuVLTPath);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        
        if(Config.getBooleanProperty("CACHE_DISK_SHOULD_DELETE_NAVTOOL", false)){
            // deletes all menues that have been generated
            RefreshMenus.deleteMenus();
        	CacheLocator.getCacheAdministrator().flushGroupLocalOnly("navCache");
        }


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

		// Create the GeoIP2 database reader on startup since it takes around 2
		// seconds to load the file. If the prop is not set, just move on
		if (UtilMethods.isSet(Config.getStringProperty(
				"GEOIP2_CITY_DATABASE_PATH", ""))) {
			try {
				Logger.info(this, "");
				GeoIp2CityDbUtil geoIp2Util = GeoIp2CityDbUtil.getInstance();
				// Validation query to initialize the GeoIP DB
				String state = geoIp2Util
						.getSubdivisionIsoCode("www.google.com");
				Logger.info(this,
						"Local GeoIP2 DB connection established successfully!");
			} catch (IOException | GeoIp2Exception | DotRuntimeException e) {
				Logger.info(this,
						"Could not read from GeoIP2 DB: " + e.getMessage());
			}
			Logger.info(this, "");
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

			// Tell the world we are started up
			System.setProperty(WebKeys.DOTCMS_STARTED_UP, "true");
			
			// Record how long it took to start us up.
			try{

				long startupTime = ManagementFactory.getRuntimeMXBean().getUptime();

				System.setProperty(WebKeys.DOTCMS_STARTUP_TIME, String.valueOf(startupTime));

			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Unable to record startup time :" + e);
			}

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
 * This thread will fire and send host ids to dotcms.com for internal
 * corporate information (we are dying to know who is using dotCMS!).
 * This can be turned off by setting RUN_INIT_THREAD=0 in the config
 *
 */

    private class InitThread extends Thread {

        public void run() {
        	try {
        		long runInitThread = Config.getIntProperty("RUN_INIT_THREAD", 6000);
        		if(runInitThread<1){return;}
        		
                Thread.sleep(runInitThread);
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
                	if(!"System Host".equals(h.getHostname())){
                		sb.append(h.getHostname() + "\n");
                	}
                    if (UtilMethods.isSet(h.getAliases())) {
                    	String[] x = h.getAliases().split("\\n|\\r");
                    	for(String y : x){
                    		if(UtilMethods.isSet(y) && !y.contains("dotcms.com") || !"host".equals(y)){
		                    	sb.append(y + "\\n");
	                    	}
                		}
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
                if(UtilMethods.isSet(System.getProperty(WebKeys.DOTCMS_STARTUP_TIME))){
                	data.append("&");
                	data.append(URLEncoder.encode("startupTime", "UTF-8"));
                	data.append("=");
                	data.append(URLEncoder.encode(System.getProperty(WebKeys.DOTCMS_STARTUP_TIME), "UTF-8"));
                }
                data.append("&");
                data.append(URLEncoder.encode("serverId", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(LicenseUtil.getDisplayServerId()), "UTF-8"));
                data.append("&");
                
                data.append(URLEncoder.encode("licenseId", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(LicenseUtil.getSerial()), "UTF-8"));
                data.append("&");
                
                data.append(URLEncoder.encode("licenseLevel", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(LicenseUtil.getLevel()), "UTF-8"));
                data.append("&");
                
                if(UtilMethods.isSet(LicenseUtil.getValidUntil())){
	                data.append(URLEncoder.encode("licenseValid", "UTF-8"));
	                data.append("=");
	                data.append(URLEncoder.encode(UtilMethods.dateToJDBC(LicenseUtil.getValidUntil())));
	                data.append("&");
                }
                data.append(URLEncoder.encode("perpetual", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(LicenseUtil.isPerpetual()), "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("stName", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode("DotcmsStartup", "UTF-8"));
                data.append("&");
                data.append(URLEncoder.encode("clientName", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode(String.valueOf(LicenseUtil.getClientName()), "UTF-8"));
                data.append("&");
                
                data.append(URLEncoder.encode("hostfolder", "UTF-8"));
                data.append("=");
                data.append(URLEncoder.encode("dotcms.com:/private", "UTF-8"));
                data.append("&");

                
                
                
                String portalUrl = Config.getStringProperty("DOTCMS_PORTAL_URL", "dotcms.com");
                String portalUrlProtocol = Config.getStringProperty("DOTCMS_PORTAL_URL_PROTOCOL", "https");
                String portalUrlUri = Config.getStringProperty("DOTCMS_PORTAL_URL_URI", "/api/content/save/1");
                

                // Send data

                sb = new StringBuilder();
                sb.append(portalUrlProtocol + "://" + portalUrl + portalUrlUri);
                URL url = new URL(sb.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setUseCaches(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                conn.setRequestProperty("DOTAUTH", "bGljZW5zZXJlcXVlc3RAZG90Y21zLmNvbTpKbnM0QHdAOCZM");
                

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(data.toString());
                wr.flush();
                wr.close();
                DataInputStream input = new DataInputStream(conn.getInputStream());
                input.close();


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
				} finally {
	                DbConnectionFactory.closeConnection();
	            }
            }

        }

    }
}
