package com.dotmarketing.osgi;

import static com.dotmarketing.osgi.ActivatorUtil.PATH_SEPARATOR;
import static com.dotmarketing.osgi.ActivatorUtil.cleanResources;
import static com.dotmarketing.osgi.ActivatorUtil.findCustomURLLoader;
import static com.dotmarketing.osgi.ActivatorUtil.getBundleFolder;
import static com.dotmarketing.osgi.ActivatorUtil.getManifestHeaderValue;
import static com.dotmarketing.osgi.ActivatorUtil.getModuleConfig;
import static com.dotmarketing.osgi.ActivatorUtil.moveResources;
import static com.dotmarketing.osgi.ActivatorUtil.moveVelocityResources;
import static com.dotmarketing.osgi.ActivatorUtil.unfreeze;
import static com.dotmarketing.osgi.ActivatorUtil.unregisterAll;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import com.dotcms.repackage.commons_lang_2_4.org.apache.commons.lang.Validate;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.struts.org.apache.struts.config.ActionConfig;
import com.dotcms.repackage.struts.org.apache.struts.config.ForwardConfig;
import com.dotcms.repackage.struts.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.urlrewritefilter_4_0_3.org.tuckey.web.filters.urlrewrite.NormalRule;
import com.dotcms.repackage.urlrewritefilter_4_0_3.org.tuckey.web.filters.urlrewrite.Rule;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.DotUrlRewriteFilter;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.ejb.PortletManager;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.PortletPK;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.Http;
import com.liferay.util.SimpleCachePool;

/**
 * Created by Jonathan Gamba
 * Date: 7/23/12
 */
public abstract class GenericBundleActivator implements BundleActivator {

    private static final String MANIFEST_HEADER_BUNDLE_ACTIVATOR = "Bundle-Activator";
    private static final String MANIFEST_HEADER_OVERRIDE_CLASSES = "Override-Classes";

    private static final String INIT_PARAM_VIEW_JSP = "view-jsp";
    private static final String INIT_PARAM_VIEW_TEMPLATE = "view-template";

    private BundleContext context;

    private PrimitiveToolboxManager toolboxManager;
    private WorkflowAPIOsgiService workflowOsgiService;
    private Collection<ToolInfo> viewTools;
    private Collection<WorkFlowActionlet> actionlets;
    private Map<String, String> jobs;
    private Collection<ActionConfig> actions;
    private Collection<Portlet> portlets;
    private Collection<Rule> rules;
    private Collection<String> preHooks;
    private Collection<String> postHooks;
    
	private List<ServiceTracker<ExtHttpService, ExtHttpService>> trackers = new ArrayList<ServiceTracker<ExtHttpService, ExtHttpService>>();
	private boolean languageVariablesNotAdded = true;

    private Scheduler scheduler;
    private Properties schedulerProperties;
    

    /**
     * Verify and initialize if necessary the required OSGI services to create plugins
     *
     * @param context
     */
    protected void initializeServices ( BundleContext context ) throws Exception {

        this.context = context;

        forceHttpServiceLoading( context );
        //Forcing the loading of the ToolboxManager
        forceToolBoxLoading( context );
        //Forcing the loading of the WorkflowService
        forceWorkflowServiceLoading( context );
    }

    /**
     * Allow to this bundle/elements to be visible and accessible from the host classpath
     *
     * @param context
     * @throws Exception
     */
    protected void publishBundleServices ( BundleContext context ) throws Exception {

        if ( this.context == null ) {
            this.context = context;
        }

        //Force the loading of some classes that may be already loaded on the host classpath but we want to override with the ones on this bundle
        String overrideClasses = getManifestHeaderValue( context, MANIFEST_HEADER_OVERRIDE_CLASSES );
        if ( overrideClasses != null && !overrideClasses.isEmpty() ) {

            String[] forceOverride = overrideClasses.split( "," );
            if ( forceOverride.length > 0 ) {

                try {
                    //Get the activator class for this OSGI bundle
                    String activatorClass = getManifestHeaderValue( context, MANIFEST_HEADER_BUNDLE_ACTIVATOR );
                    //Injecting this bundle context code inside the dotCMS context
                    injectContext( activatorClass );
                } catch ( Exception e ) {
                    Logger.error( this, "Error injecting context for overriding", e );
                    throw e;
                }
            }

        }
    }

    /**
     * Is possible on certain scenarios to have our ToolManager without initialization, or most probably a ToolManager without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceToolBoxLoading ( BundleContext context ) {

        ServiceReference serviceRefSelected = context.getServiceReference( PrimitiveToolboxManager.class.getName() );
        if ( serviceRefSelected == null ) {

            //Forcing the loading of the ToolboxManager
            ServletToolboxManager toolboxManager = (ServletToolboxManager) VelocityUtil.getToolboxManager();
            if ( toolboxManager != null ) {

                serviceRefSelected = context.getServiceReference( PrimitiveToolboxManager.class.getName() );
                if ( serviceRefSelected == null ) {
                    toolboxManager.registerService();
                }
            }
        }
    }

    /**
     * Is possible on certain scenarios to have our WorkflowAPI without initialization, or most probably a WorkflowAPI without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceWorkflowServiceLoading ( BundleContext context ) {

        //Getting the service to register our Actionlet
        ServiceReference serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
        if ( serviceRefSelected == null ) {

            //Forcing the loading of the WorkflowService
            WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
            if ( workflowAPI != null ) {

                serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
                if ( serviceRefSelected == null ) {
                    //Forcing the registration of our required services
                    workflowAPI.registerBundleService();
                }
            }
        }
    }

    /**
     * Forcing the registry of the HttpService, usually need it when the felix framework is reloaded and we need to update the
     * bundle context of our already registered services.
     *
     * @param context
     */
    private void forceHttpServiceLoading ( BundleContext context ) throws Exception {

        try {
            //Working with the http bridge
            if ( OSGIProxyServlet.servletConfig != null ) {//If it is null probably the servlet wasn't even been loaded...

                try {
                    OSGIProxyServlet.bundleContext.getBundle();
                } catch ( IllegalStateException e ) {

                    Bundle[] bundles = context.getBundles();
                    for ( Bundle bundle : bundles ) {
                        if ( bundle.getSymbolicName().equals( OSGIUtil.BUNDLE_HTTP_BRIDGE_SYMBOLIC_NAME ) ) {
                            //If we are here is because we have an invalid bundle context, so we need to provide a new one
                            BundleContext httpBundle = bundle.getBundleContext();
                            OSGIProxyServlet.tracker = new DispatcherTracker( httpBundle, null, OSGIProxyServlet.servletConfig );
                            OSGIProxyServlet.tracker.open();
                            OSGIProxyServlet.bundleContext = httpBundle;
                        }

                    }
                }

            }
        } catch ( Exception e ) {
            Logger.error( this, "Error loading HttpService.", e );
            throw e;
        }
    }

    /**
     * Will inject this bundle context code inside the dotCMS context
     *
     * @param name a reference class inside this bundle jar
     * @throws Exception
     */
    private void injectContext ( String name ) throws Exception {
        injectContext( name, true );
    }

    /**
     * Will inject this bundle context code inside the dotCMS context
     *
     * @param className a reference class inside this bundle jar
     * @param reload    if a redefinition should be done or not
     * @throws Exception
     */
    private void injectContext ( String className, Boolean reload ) throws Exception {

        if (this.context == null ) {
            throw new RuntimeException( "No context bundle was set, use the initializeServices method at the top of your Activator class." );
        }

        long bundleId = this.context.getBundle().getBundleId();

        //Get the location of this OSGI bundle jar source code using a known class inside this bundle
        Class clazz = Class.forName( className, false, this.getClass().getClassLoader() );
        URL sourceURL = clazz.getProtectionDomain().getCodeSource().getLocation();

        //Verify if we have our UrlOsgiClassLoader on the main class loaders
        UrlOsgiClassLoader urlOsgiClassLoader = findCustomURLLoader( ClassLoader.getSystemClassLoader(), bundleId );
        if ( urlOsgiClassLoader != null ) {

            if ( !urlOsgiClassLoader.contains( sourceURL ) ) {//Verify if this URL is already in our custom ClassLoader
                urlOsgiClassLoader.addURL( sourceURL );
            }

            //The ClassLoader and the class content is already in the system ClassLoader, so we need to reload the jar contents
            if ( reload ) {
                urlOsgiClassLoader.reload( sourceURL );
            }
        } else {

            //Getting the reference of a known class in order to get the base/main class loader
            Class baseClass = Thread.currentThread().getContextClassLoader().loadClass( "org.quartz.Job" );
            //Creates our custom class loader in order to use it to inject the class code inside dotcms context
            urlOsgiClassLoader = new UrlOsgiClassLoader( sourceURL, baseClass.getClassLoader(), bundleId );

            //We may have classes we want to override from e beginning, for example a custom implementation of a dotCMS class
            if ( reload ) {
                urlOsgiClassLoader.reload( sourceURL );
            } else {
                //Linking our custom class loader with the dotCMS class loaders hierarchy.
                urlOsgiClassLoader.linkClassLoaders();
            }
        }
    }

    //*******************************************************************
    //*******************************************************************
    //****************REGISTER SERVICES METHODS**************************
    //*******************************************************************
    //*******************************************************************

    /**
     * Register the portlets on the given configuration files
     *
     * @param xmls
     * @throws Exception
     */
    @SuppressWarnings ("unchecked")
    protected Collection<Portlet> registerPortlets ( BundleContext context, String[] xmls ) throws Exception {

        String[] confFiles = new String[]{Http.URLtoString( context.getBundle().getResource( xmls[0] ) ),
                Http.URLtoString( context.getBundle().getResource( xmls[1] ) )};

        //Read the portlets xml files and create them
        portlets = PortletManagerUtil.initWAR( null, confFiles );

        for ( Portlet portlet : portlets ) {

            if ( portlet.getPortletClass().equals( "com.liferay.portlet.JSPPortlet" ) ) {

                Map initParams = portlet.getInitParams();
                String jspPath = (String) initParams.get( INIT_PARAM_VIEW_JSP );

                if ( !jspPath.startsWith( PATH_SEPARATOR ) ) {
                    jspPath = PATH_SEPARATOR + jspPath;
                }

                //Copy all the resources inside the folder of the given resource to the corresponding dotCMS folders
                moveResources( context, jspPath );
                portlet.getInitParams().put( INIT_PARAM_VIEW_JSP, getBundleFolder( context ) + jspPath );
            } else if ( portlet.getPortletClass().equals( "com.liferay.portlet.VelocityPortlet" ) ) {

                Map initParams = portlet.getInitParams();
                String templatePath = (String) initParams.get( INIT_PARAM_VIEW_TEMPLATE );

                if ( !templatePath.startsWith( PATH_SEPARATOR ) ) {
                    templatePath = PATH_SEPARATOR + templatePath;
                }

                //Copy all the resources inside the folder of the given resource to the corresponding velocity dotCMS folders
                moveVelocityResources( context, templatePath );
                portlet.getInitParams().put( INIT_PARAM_VIEW_TEMPLATE, getBundleFolder( context ) + templatePath );
            }

            Logger.info( this, "Added Portlet: " + portlet.getPortletId() );
        }

        return portlets;
    }

    /**
     * Method that will create and add an ActionForward to a ActionMapping, this call is mandatory for the creation of ActionForwards
     * because extra logic will be required for jsp forwards to work.
     *
     * @param actionMapping
     * @param name
     * @param path
     * @param redirect
     * @return
     * @throws Exception
     */
    protected ForwardConfig registerActionForward ( BundleContext context, ActionMapping actionMapping, String name, String path, Boolean redirect ) throws Exception {

        if ( !path.startsWith( PATH_SEPARATOR ) ) {
            path = PATH_SEPARATOR + path;
        }

        String forwardMapping = getBundleFolder( context ) + path;

        // Creating an ForwardConfig Instance
        ForwardConfig forwardConfig = new ActionForward( name, forwardMapping, redirect );
        // Adding the ForwardConfig to the ActionConfig
        actionMapping.addForwardConfig( forwardConfig );

        //Copy all the resources inside the folder of the given resource to the corresponding dotCMS folders
        moveResources( context, path );

        return forwardConfig;
    }

    /**
     * Register a given ActionMapping
     *
     * @param actionMapping
     * @throws Exception
     */
    protected void registerActionMapping ( ActionMapping actionMapping ) throws Exception {

        if ( actions == null ) {
            actions = new ArrayList<ActionConfig>();
        }

        String actionClassType = actionMapping.getType();

        //Injects the action classes inside the dotCMS context
        injectContext( actionClassType );

        ModuleConfig moduleConfig = getModuleConfig();
        //We need to unfreeze this module in order to add new action mappings
        unfreeze( moduleConfig );

        //Adding the ActionConfig to the ForwardConfig
        moduleConfig.addActionConfig( actionMapping );
        //moduleConfig.freeze();

        actions.add( actionMapping );
        Logger.info( this, "Added Struts Action Mapping: " + actionClassType );
    }

    /**
     * Register a given Quartz Job scheduled task
     *
     * @param scheduledTask
     * @throws Exception
     */
    protected void scheduleQuartzJob ( ScheduledTask scheduledTask ) throws Exception {

        String jobName = scheduledTask.getJobName();
        String jobGroup = scheduledTask.getJobGroup();

        if ( jobs == null ) {
            jobs = new HashMap<String, String>();
        }

        //Injects the job classes inside the dotCMS context
        injectContext( scheduledTask.getJavaClassName() );

        /*
        Schedules the given job in the quartz system, and depending on the sequentialScheduled
        property it will use the sequential of the standard scheduler.
         */
        QuartzUtils.scheduleTask( scheduledTask );
        jobs.put( jobName, jobGroup );

        Logger.info( this, "Added Quartz Job: " + jobName );
    }

    /**
     * Adds a given tuckey Rule to the url rewrite filter
     *
     * @param rule
     * @throws Exception
     */
    protected void addRewriteRule ( Rule rule ) throws Exception {

        //Get a reference of our url rewrite filter
        DotUrlRewriteFilter urlRewriteFilter = DotUrlRewriteFilter.getUrlRewriteFilter();
        if ( urlRewriteFilter != null ) {

            if ( rules == null ) {
                rules = new ArrayList<Rule>();
            }

            //Adding the Rule to the filter
            urlRewriteFilter.addRule( rule );
            rules.add( rule );
        } else {
            throw new RuntimeException( "Non UrlRewriteFilter found!" );
        }
    }

    /**
     * Creates and add tuckey rules
     *
     * @param from the url to match from
     * @param to   url for redirecting/passing through to
     * @param type Posible values:
     *             <ul>
     *             <li><strong>forward</strong>: Requests matching the "conditions" for this "rule", and the URL in the "from" element will be internally forwarded to the URL specified in the "to" element. Note: In this case the "to" URL must be in the same context as UrlRewriteFilter. This is the same as doing:
     *             <br>RequestDispatcher rq = request.getRequestDispatcher([to value]);
     *             <br>rq.forward(request, response);</li>
     *             <li><strong>passthrough</strong>:Identical to "forward".</li>
     *             <li><strong>redirect</strong>:Requests matching the "conditions" and the "from" for this rule will be HTTP redirected. This is the same a doing:
     *             <br>HttpServletResponse.sendRedirect([to value]))</li>
     *             <li><strong>permanent-redirect</strong>:The same as doing:
     *             <br>response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
     *             <br>response.setHeader("Location", [to value]);
     *             <br>(note, SC_MOVED_PERMANENTLY is HTTP status code 301)</li>
     *             <li><strong>temporary-redirect</strong>:The same as doing:
     *             <br>response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
     *             <br>response.setHeader("Location", [to value]);
     *             <br>(note, SC_MOVED_TEMPORARILY is HTTP status code 302)</li>
     *             <li><strong>pre-include</strong></li>
     *             <li><strong>post-include</strong></li>
     *             <li><strong>proxy</strong>: The request will be proxied to the full url specified. commons-http and commons-codec must both be in the classpath to use this feature.</li>
     *             </ul>
     * @param name rule name
     * @throws Exception
     */
    protected void addRewriteRule ( String from, String to, String type, String name ) throws Exception {

        //Create the tuckey rule
        NormalRule rule = new NormalRule();
        rule.setFrom( from );
        rule.setToType( type );
        rule.setTo( to );
        rule.setName( name );

        //And add the rewrite rule
        addRewriteRule( rule );
    }

    /**
     * Register a WorkFlowActionlet service
     *
     * @param context
     * @param actionlet
     */
    @SuppressWarnings ("unchecked")
    protected void registerActionlet ( BundleContext context, WorkFlowActionlet actionlet ) {

        //Getting the service to register our Actionlet
        ServiceReference serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
        }

        if ( actionlets == null ) {
            actionlets = new ArrayList<WorkFlowActionlet>();
        }

        this.workflowOsgiService = (WorkflowAPIOsgiService) context.getService( serviceRefSelected );
        this.workflowOsgiService.addActionlet( actionlet.getClass() );
        actionlets.add( actionlet );

        Logger.info( this, "Added actionlet: " + actionlet.getName() );
    }

    /**
     * Register a ViewTool service using a ToolInfo object
     *
     * @param context
     * @param info
     */
    @SuppressWarnings ("unchecked")
    protected void registerViewToolService ( BundleContext context, ToolInfo info ) {

        //Getting the service to register our ViewTool
        ServiceReference serviceRefSelected = context.getServiceReference( PrimitiveToolboxManager.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
        }

        if ( viewTools == null ) {
            viewTools = new ArrayList<ToolInfo>();
        }

        this.toolboxManager = (PrimitiveToolboxManager) context.getService( serviceRefSelected );
        this.toolboxManager.addTool( info );
        viewTools.add( info );

        Logger.info( this, "Added View Tool: " + info.getKey() );
    }

    /**
     * Adds a hook to the end of the chain
     *
     * @param preHook
     * @throws Exception
     */
    protected void addPreHook ( Object preHook ) throws Exception {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        //First we need to be sure we are not adding the same hook more than once
        interceptor.delPreHookByClassName( preHook.getClass().getName() );

        if ( preHooks == null ) {
            preHooks = new ArrayList<String>();
        }

        interceptor.addPreHook( preHook );
        preHooks.add( preHook.getClass().getName() );
    }

    /**
     * Adds a hook to the end of the chain
     *
     * @param postHook
     * @throws Exception
     */
    protected void addPostHook ( Object postHook ) throws Exception {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        //First we need to be sure we are not adding the same hook more than once
        interceptor.delPostHookByClassName( postHook.getClass().getName() );

        if ( postHooks == null ) {
            postHooks = new ArrayList<String>();
        }

        interceptor.addPostHook( postHook );
        postHooks.add( postHook.getClass().getName() );
    }
    
	protected void addServlet(BundleContext context, final Class<? extends Servlet> clazz, final String path) {
		Validate.notNull(clazz, "Servlet class may not be null");
		Validate.notEmpty(path, "Servlet path may not be null");
		Validate.isTrue(path.startsWith("/"), "Servlet path must start with a /");

		final Servlet servlet;
		try {
			servlet = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		Logger.info(this, "Registering Servlet " + servlet.getClass().getSimpleName());

		addServlet(context, servlet, path, false);
	}

	/**
	 * @param handleBundleServices is used to add/remove bundleServices, which are needed for the DispatcherServlet
	 */
	private void addServlet(BundleContext context, final Servlet servlet, final String path, final boolean handleBundleServices) {
		ServiceTracker<ExtHttpService, ExtHttpService> tracker = new ServiceTracker<ExtHttpService, ExtHttpService>(context, ExtHttpService.class, null) {
			@Override public ExtHttpService addingService(ServiceReference<ExtHttpService> reference) {
				ExtHttpService extHttpService = super.addingService(reference);

				try {
					if(handleBundleServices) {
						publishBundleServices(context);
					}

					extHttpService.registerServlet(path, servlet, null, null);

				} catch (Exception e) {
					throw new RuntimeException("Failed to register servlet " + servlet.getClass().getSimpleName(), e);
				}

				CMSFilter.addExclude(path);
				CMSFilter.addExclude("/app" + path);

				return extHttpService;
			}
			@Override public void removedService(ServiceReference<ExtHttpService> reference, ExtHttpService extHttpService) {
				extHttpService.unregisterServlet(servlet);
				CMSFilter.removeExclude(path);
				CMSFilter.removeExclude("/app" + path);

				if(handleBundleServices) {
					try {
						unpublishBundleServices();
					} catch (Exception e) {
						//Only a warning since this exception was added later
						Logger.warn(this, "Exception while unpublishing bundle services", e);
					}
				}

				super.removedService(reference, extHttpService);
			}
		};

		this.trackers.add(tracker);
		tracker.open();

	}

	protected void addFilter(BundleContext context, final Class <? extends Filter> clazz, final String regex) {
		Validate.notNull(clazz, "Filter class may not be null");
		Validate.notEmpty(regex, "Filter regex may not be null");
		Validate.isTrue(regex.startsWith("/"), "Filter regex must start with a /");

		final Filter filterToRegister;
		try {
			filterToRegister = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		Logger.info(this, "Registering Filter " + filterToRegister.getClass().getSimpleName());

		ServiceTracker<ExtHttpService, ExtHttpService> tracker = new ServiceTracker<ExtHttpService, ExtHttpService>(context, ExtHttpService.class, null) {
			@Override public ExtHttpService addingService(ServiceReference<ExtHttpService> reference) {
				ExtHttpService extHttpService = super.addingService(reference);

				try {

					extHttpService.registerFilter(filterToRegister, regex, null, trackers.size(), null);

				} catch (ServletException e) {
					throw new RuntimeException("Failed to register filter " + filterToRegister.getClass().getSimpleName(), e);
				}

				CMSFilter.addExclude(regex);
				CMSFilter.addExclude("/app" + regex);

				return extHttpService;
			}
			@Override public void removedService(ServiceReference<ExtHttpService> reference, ExtHttpService extHttpService) {
				extHttpService.unregisterFilter(filterToRegister);
				CMSFilter.removeExclude(regex);
				CMSFilter.removeExclude("/app" + regex);
				super.removedService(reference, extHttpService);
			}
		};

		tracker.open();
		this.trackers.add(tracker);

	}

	/**
	 * This method parses the conf/macros-ext.vm file. This means that the macros in that file will
	 * be available in Dotcms, but can't be removed. They will be stored in memory. 
	 * @param context
	 */
	protected void addMacros(BundleContext context) {
		Logger.info(this, "Registering macros");

		final VelocityEngine engine = VelocityUtil.getEngine();
		URL macrosExtUrl = context.getBundle().getResource("conf/macros-ext.vm");

		InputStream instream = null;
		try {
			instream = macrosExtUrl.openStream();
		    engine.evaluate(VelocityUtil.getBasicContext(), new StringWriter(), context.getBundle().getSymbolicName(), new InputStreamReader(instream, Charset.forName("UTF-8")));
		} catch (IOException e) {
			Logger.warn(this, "Exception while reading macros-ext.vm", e);
		} finally {
			try {
				if(instream != null) {
					instream.close();
				}
			} catch (IOException e) {
				Logger.warn(this, "Exception while closing stream to macros-ext.vm", e);
			}
		}
	}

	private void addLanguageVariables(Map<String, String> languageVariables, Language language) {
		Map<String, String> emptyMap = new HashMap<String, String>();
		Set<String> emptySet = new HashSet<String>();
		try {

			Logger.info(this, "Registering " + languageVariables.keySet().size() + " language variable(s)");
			APILocator.getLanguageAPI().saveLanguageKeys(language, languageVariables, emptyMap, emptySet);

		} catch (DotDataException e) {
			Logger.warn(this, "Unable to register language variables", e);
		}
	}

	/**
	 * Registers the ENGLISH language variables that are saved in the conf/language-ext.properties file
	 * TODO: add multi-language functionality
	 */
	protected void addLanguageVariables(BundleContext context) {
		if(languageVariablesNotAdded) {
			languageVariablesNotAdded = false;
			try {

				// Read all the language variables from the properties file
				URL resourceURL = context.getBundle().getResource("conf/Language-ext.properties");
				PropertyResourceBundle resourceBundle = new PropertyResourceBundle(resourceURL.openStream());

				// Put the properties in a map
				Map<String, String> languageVariables = new HashMap<String, String>();
				for(String key: resourceBundle.keySet()) {
					languageVariables.put(key, resourceBundle.getString(key));
				}

				// Register the variables in locale en_US
				addLanguageVariables(languageVariables, APILocator.getLanguageAPI().getLanguage("en", "US"));

			} catch (IOException e) {
				Logger.warn(this, "Exception while registering language variables", e);
			}
		}
	}
	
	protected void addRestService(BundleContext context, final Class<? extends WebResource> clazz) {
		Logger.info(this, "Registering REST service " + clazz.getSimpleName());
		ServiceTracker<ExtHttpService, ExtHttpService> tracker = new ServiceTracker<ExtHttpService, ExtHttpService>(context, ExtHttpService.class, null) {
			@Override public ExtHttpService addingService(ServiceReference<ExtHttpService> reference) {
				ExtHttpService extHttpService = super.addingService(reference);

				RestServiceUtil.addResource(clazz);
				return extHttpService;
			}
			@Override public void removedService(ServiceReference<ExtHttpService> reference, ExtHttpService extHttpService) {
				RestServiceUtil.removeResource(clazz);
				super.removedService(reference, extHttpService);
			}
		};

		tracker.open();
		this.trackers.add(tracker);

	}

	protected void addPortlets(BundleContext context) {
		if(languageVariablesNotAdded) {
			addLanguageVariables(context);
		}

		Logger.info(this, "Registering portlet(s)");

		ServiceTracker<ExtHttpService, ExtHttpService> tracker = new ServiceTracker<ExtHttpService, ExtHttpService>(context, ExtHttpService.class, null) {
			@Override public ExtHttpService addingService(ServiceReference<ExtHttpService> reference) {
				ExtHttpService extHttpService = super.addingService(reference);

				try {
					registerPortlets(context, new String[] { "conf/portlet.xml", "conf/liferay-portlet.xml"});
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				return extHttpService;
			}
			@Override public void removedService(ServiceReference<ExtHttpService> reference, ExtHttpService extHttpService) {
				try {
					unregisterPortles();
				} catch (Exception e) {
					Logger.warn(this, "Exception while unregistering portlet", e);
				}
				super.removedService(reference, extHttpService);
			}
		};

		tracker.open();
		this.trackers.add(tracker);

		CacheLocator.getVeloctyResourceCache().clearCache();
	}

	protected void addSpringController(BundleContext context, String path, String contextConfigLocation) {
		Logger.info(this, "Registering spring controller " + contextConfigLocation);

		try {
			publishBundleServices(context);
		} catch (Exception e) {
			Logger.warn(this, "Unable to publish bundle services", e);
		}

		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		dispatcherServlet.setContextConfigLocation(contextConfigLocation);

		addServlet(context, dispatcherServlet, path, true);
	}
	
	/**
	 * Set the properties that the org.quartz Scheduler will use. Can only be called once, and only before
	 * a Job is added.
	 */
	protected void initializeSchedulerProperties(Properties properties) {
		if(this.schedulerProperties != null) {
			throw new IllegalStateException("Can't overwrite scheduler properties when they are already set. Set the properties before adding Jobs, and do not change them afterwards.");
		}
		
		this.schedulerProperties = properties;
	}
	
	protected Properties getDefaultSchedulerProperties() {
        Properties properties = new Properties();
        
        //Default properties, retrieved from a quartz.properties file
        //We only changed the threadcount to 1
        properties.setProperty("org.quartz.scheduler.instanceName", "DefaultQuartzScheduler");
        properties.setProperty("org.quartz.scheduler.rmi.export", "false");
        properties.setProperty("org.quartz.scheduler.rmi.proxy", "false");
        properties.setProperty("org.quartz.scheduler.wrapJobExecutionInUserTransaction", "false");
        properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        properties.setProperty("org.quartz.threadPool.threadPriority", "5");
        properties.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
		
        return properties;
	}
	
	/**
	 * Adds a Job, and starts a Scheduler when none was yet started
	 */
	protected void addJob(BundleContext context, Class<? extends Job> clazz, String cronExpression) {
		String jobName = clazz.getName();
		String jobGroup = FrameworkUtil.getBundle(clazz).getSymbolicName();
        JobDetail job = new JobDetail(jobName, jobGroup, clazz);
        job.setDurability(false);
        job.setVolatility(true);
        job.setDescription(jobName);
        
        try {
	        CronTrigger trigger = new CronTrigger(jobName, jobGroup, cronExpression);
	        
	        if(scheduler == null) {
	        	if(schedulerProperties == null) {
	        		schedulerProperties = getDefaultSchedulerProperties();
	        	}
	        	scheduler = new StdSchedulerFactory(schedulerProperties).getScheduler();
				scheduler.start();
	        }
	        
			Date date = scheduler.scheduleJob(job, trigger);
			
			Logger.info(this, "Scheduled job " + jobName + ", next trigger is on " + date);
			
        } catch (ParseException e) {
        	Logger.error(this, "Cron expression '" + cronExpression + "' has an exception. Throwing IllegalArgumentException", e);
        	throw new IllegalArgumentException(e);
        } catch (SchedulerException e) {
        	Logger.error(this, "Unable to schedule job " + jobName, e);
		}
		
	}
	
    //*******************************************************************
    //*******************************************************************
    //****************UNREGISTER SERVICES METHODS************************
    //*******************************************************************
    //*******************************************************************

    /**
     * Utility method to unregister all the possible services and/or tools registered by this activator class.
     * Some how we have to try to clean up anything added on the deploy if this bundle.
     */
    protected void unregisterServices ( BundleContext context ) throws Exception {

        unregisterActionlets();
        unregisterViewToolServices();
        unpublishBundleServices();
        unregisterPreHooks();
        unregisterPostHooks();
        unregisterQuartzJobs();
        unregisterActionMappings();
        unregisterPortles();
        unregisterRewriteRule();
        cleanResources( context );
        unregisterServlets( context );
        
		removeTrackedServices();
		removeScheduler();
    }
    
    /**
     * Unpublish this bundle elements
     */
    protected void unpublishBundleServices () throws Exception {

        //Find the UrlOsgiClassLoader for this bundleId
        UrlOsgiClassLoader urlOsgiClassLoader = findCustomURLLoader( ClassLoader.getSystemClassLoader(), this.context.getBundle().getBundleId() );
        if ( urlOsgiClassLoader != null ) {

            //Get the activator class for this OSGI bundle
            String activatorClass = getManifestHeaderValue( context, MANIFEST_HEADER_BUNDLE_ACTIVATOR );
            //Get the location of this OSGI bundle jar source code using a known class inside this bundle
            Class clazz = Class.forName( activatorClass, false, this.getClass().getClassLoader() );
            URL sourceURL = clazz.getProtectionDomain().getCodeSource().getLocation();

            //Restoring to their original state any override class
            urlOsgiClassLoader.reload( sourceURL, true );

            /*
            Closes this URLClassLoader, so that it can no longer be used to load
            new classes or resources that are defined by this loader.
            Classes and resources defined by any of this loader's parents in the
            delegation hierarchy are still accessible. Also, any classes or resources
            that are already loaded, are still accessible.
             */
            urlOsgiClassLoader.close();
            urlOsgiClassLoader.unlinkClassLoaders();
        }
    }

    /**
     * Unregister the registered WorkFlowActionlet services
     */
    protected void unregisterActionlets () {

        if ( this.workflowOsgiService != null && actionlets != null ) {
            for ( WorkFlowActionlet actionlet : actionlets ) {

                this.workflowOsgiService.removeActionlet( actionlet.getClass().getCanonicalName() );
                Logger.info( this, "Removed actionlet: " + actionlet.getClass().getCanonicalName() );
            }
        }
    }

    /**
     * Unregister the registered ViewTool services
     */
    protected void unregisterViewToolServices () {

        if ( this.toolboxManager != null && viewTools != null ) {

            Iterator<ToolInfo> toolInfoIterator = viewTools.iterator();
            while ( toolInfoIterator.hasNext() ) {

                ToolInfo toolInfo = toolInfoIterator.next();
                this.toolboxManager.removeTool( toolInfo );
                toolInfoIterator.remove();

                Logger.info( this, "Removed View Tool: " + toolInfo.getKey() );
            }
        }
    }

    /**
     * Unregister all the registered post hooks
     *
     * @throws Exception
     */
    protected void unregisterPostHooks () {

        if ( postHooks != null ) {

            Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
            for ( String postHook : postHooks ) {
                interceptor.delPostHookByClassName( postHook );
            }
        }
    }

    /**
     * Unregister all the registered pre hooks
     *
     * @throws Exception
     */
    protected void unregisterPreHooks () {

        if ( preHooks != null ) {

            Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
            for ( String preHook : preHooks ) {
                interceptor.delPreHookByClassName( preHook );
            }
        }
    }

    /**
     * Unregister all the registered ActionMappings
     *
     * @throws Exception
     */
    protected void unregisterActionMappings () throws Exception {

        if ( actions != null ) {

            ModuleConfig moduleConfig = getModuleConfig();
            //We need to unfreeze this module in order to add new action mappings
            unfreeze( moduleConfig );

            for ( ActionConfig actionConfig : actions ) {
                moduleConfig.removeActionConfig( actionConfig );
            }
            moduleConfig.freeze();
        }

    }

    /**
     * Unregister all the registered Quartz Jobs
     *
     * @throws SchedulerException
     */
    protected void unregisterQuartzJobs () throws Exception {

        if ( jobs != null ) {
            for ( String jobName : jobs.keySet() ) {
                QuartzUtils.removeJob( jobName, jobs.get( jobName ) );
            }
        }
    }

    /**
     * Unregister all the registered Quartz Jobs
     *
     * @throws SchedulerException
     */
    protected void unregisterPortles () throws Exception {

        if ( portlets != null ) {

            PortletManager portletManager = PortletManagerFactory.getManager();
            Company company = PublicCompanyFactory.getDefaultCompany();

            for ( Portlet portlet : portlets ) {

                //PK
                PortletPK id = portlet.getPrimaryKey();
                //Cache key
                String scpId = PortalUtil.class.getName() + "." + com.dotcms.repackage.portlet.javax.portlet.Portlet.class.getName();
                if ( !portlet.isWARFile() ) {
                    scpId += "." + company.getCompanyId();
                }

                //Clean-up the caches
                portletManager.removePortletFromPool( company.getCompanyId(), id.getPortletId() );
                //Clean-up the caches
                Map map = (Map) SimpleCachePool.get( scpId );
                if ( map != null ) {
                    map.remove( portlet.getPortletId() );
                }
            }
        }
    }

    /**
     * Unregister all the registered servlets and mappings
     *
     * @throws SchedulerException
     */
    protected void unregisterServlets ( BundleContext context ) throws Exception {
        unregisterAll( context );
    }

    /**
     * Unregister all the registered Rewrite Rules
     *
     * @throws Exception
     */
    protected void unregisterRewriteRule () throws Exception {

        if ( rules != null ) {

            //Get a reference of our url rewrite filter
            DotUrlRewriteFilter urlRewriteFilter = DotUrlRewriteFilter.getUrlRewriteFilter();
            if ( urlRewriteFilter != null ) {
                for ( Rule rule : rules ) {
                    //Remove from the filter this rule
                    urlRewriteFilter.removeRule( rule );
                }
            } else {
                throw new RuntimeException( "Non UrlRewriteFilter found!" );
            }
        }
    }
    
	/**
	 * Removes Dotcms services that are tracked by the GenericBundleActivator. These are
	 * services that require more than just a simple register/unregister. For instance Servlets and Filters.
	 */
	protected void removeTrackedServices() {
		for(ServiceTracker<ExtHttpService, ExtHttpService> tracker: trackers) {
			tracker.close();
		}
	}
	
	/**
	 * Shutdown and remove the scheduler if one was instantiated
	 */
    private void removeScheduler() throws Exception {
		if(scheduler != null) {
			scheduler.shutdown(false);
			scheduler = null;
			schedulerProperties = null;
		}
    }

}