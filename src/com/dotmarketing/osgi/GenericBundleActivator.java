package com.dotmarketing.osgi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.quartz.SchedulerException;
import org.tuckey.web.filters.urlrewrite.NormalRule;
import org.tuckey.web.filters.urlrewrite.Rule;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.DotUrlRewriteFilter;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.ejb.PortletManager;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.PortletPK;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.FileUtil;
import com.liferay.util.Http;
import com.liferay.util.SimpleCachePool;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Created by Jonathan Gamba
 * Date: 7/23/12
 */
public abstract class GenericBundleActivator implements BundleActivator {

    private static final String MANIFEST_HEADER_BUNDLE_ACTIVATOR = "Bundle-Activator";
    private static final String MANIFEST_HEADER_OVERRIDE_CLASSES = "Override-Classes";

    private static final String INIT_PARAM_VIEW_JSP = "view-jsp";
    private static final String INIT_PARAM_VIEW_TEMPLATE = "view-template";
    private static final String PATH_SEPARATOR = "/";
    private static final String OSGI_FOLDER = "/osgi";
    private static final String VELOCITY_FOLDER = "/WEB-INF/velocity";

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
    private ActivatorUtil activatorUtil = new ActivatorUtil();

    private ClassLoader getFelixClassLoader () {
        return this.getClass().getClassLoader();
    }

    private ClassLoader getContextClassLoader () {
        //return ClassLoader.getSystemClassLoader();
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Verify and initialize if necessary the required OSGI services to create plugins
     *
     * @param context
     */
    protected void initializeServices ( BundleContext context ) throws Exception {

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

        //Classloaders
        ClassLoader felixClassLoader = getFelixClassLoader();
        ClassLoader contextClassLoader = getContextClassLoader();

        //Create a new class loader where we can "combine" our classloaders
        CombinedLoader combinedLoader;
        if ( contextClassLoader instanceof CombinedLoader ) {
            combinedLoader = (CombinedLoader) contextClassLoader;
            combinedLoader.addLoader( felixClassLoader );
        } else {
            combinedLoader = new CombinedLoader( contextClassLoader );
            combinedLoader.addLoader( felixClassLoader );
        }

        //Force the loading of some classes that may be already loaded on the host classpath but we want to override with the ones on this bundle and we specified
        String overrideClasses = activatorUtil.getManifestHeaderValue( context, MANIFEST_HEADER_OVERRIDE_CLASSES );
        if ( overrideClasses != null && !overrideClasses.isEmpty() ) {

            String[] forceOverride = overrideClasses.split( "," );
            if ( forceOverride.length > 0 ) {

                try {
                    //Get the activator class for this OSGI bundle
                    String activatorClass = activatorUtil.getManifestHeaderValue( context, MANIFEST_HEADER_BUNDLE_ACTIVATOR );
                    //Injecting this bundle context code inside the dotCMS context
                    injectContext( activatorClass );
                } catch ( Exception e ) {
                    Logger.error( this, "Error injecting context for overriding", e );
                    throw e;
                }

                for ( String classToOverride : forceOverride ) {
                    try {
                        /*
                         Loading the custom implementation will allows to override the one the ClassLoader
                         already had loaded and use it on this OSGI bundle context
                         */
                        combinedLoader.loadClass( classToOverride.trim() );
                    } catch ( Exception e ) {
                        Logger.error( this, "Error overriding class: " + classToOverride, e );
                        throw e;
                    }
                }
            }

        }

        //Use this new "combined" class loader
        Thread.currentThread().setContextClassLoader( combinedLoader );
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

        //Get the location of this OSGI bundle jar source code using a known class inside this bundle
        Class clazz = Class.forName( className, false, getFelixClassLoader() );
        URL classURL = clazz.getProtectionDomain().getCodeSource().getLocation();

        //Verify if we have our UrlOsgiClassLoader on the main class loaders
        UrlOsgiClassLoader urlOsgiClassLoader = activatorUtil.findCustomURLLoader( ClassLoader.getSystemClassLoader() );
        if ( urlOsgiClassLoader != null ) {

            if ( !urlOsgiClassLoader.contains( classURL ) ) {//Verify if this URL is already in our custom ClassLoader
                urlOsgiClassLoader.addURL( classURL );
            }

            //The ClassLoader and the class content is already in the system ClassLoader, so we need to reload the jar contents
            if ( reload ) {
                urlOsgiClassLoader.reload( classURL );
            }
        } else {

            //Getting the reference of a known class in order to get the base/main class loader
            Class baseClass = getContextClassLoader().loadClass( "org.quartz.Job" );
            //Creates our custom class loader in order to use it to inject the class code inside dotcms context
            urlOsgiClassLoader = new UrlOsgiClassLoader( classURL, baseClass.getClassLoader() );

            //We may have classes we want to override from e beginning, for example a custom implementation of a dotCMS class
            if ( reload ) {
                urlOsgiClassLoader.reload( classURL );
            }

            //Linking our custom class loader with the dotCMS class loaders hierarchy.
            urlOsgiClassLoader.linkClassLoaders();
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
                activatorUtil.moveResources( context, jspPath );
                portlet.getInitParams().put( INIT_PARAM_VIEW_JSP, activatorUtil.getBundleFolder( context ) + jspPath );
            } else if ( portlet.getPortletClass().equals( "com.liferay.portlet.VelocityPortlet" ) ) {

                Map initParams = portlet.getInitParams();
                String templatePath = (String) initParams.get( INIT_PARAM_VIEW_TEMPLATE );

                if ( !templatePath.startsWith( PATH_SEPARATOR ) ) {
                    templatePath = PATH_SEPARATOR + templatePath;
                }

                //Copy all the resources inside the folder of the given resource to the corresponding velocity dotCMS folders
                activatorUtil.moveVelocityResources( context, templatePath );
                portlet.getInitParams().put( INIT_PARAM_VIEW_TEMPLATE, activatorUtil.getBundleFolder( context ) + templatePath );
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

        String forwardMapping = activatorUtil.getBundleFolder( context ) + path;

        // Creating an ForwardConfig Instance
        ForwardConfig forwardConfig = new ActionForward( name, forwardMapping, redirect );
        // Adding the ForwardConfig to the ActionConfig
        actionMapping.addForwardConfig( forwardConfig );

        //Copy all the resources inside the folder of the given resource to the corresponding dotCMS folders
        activatorUtil.moveResources( context, path );

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

        //Will inject the action classes inside the dotCMS context
        injectContext( actionClassType );

        ModuleConfig moduleConfig = activatorUtil.getModuleConfig();
        //We need to unfreeze this module in order to add new action mappings
        activatorUtil.unfreeze( moduleConfig );

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

        //Will inject the job classes inside the dotCMS context
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
	
	/**
	 * Adds REST services<br/>
	 * <br/>
	 * <strong>Example:</strong><br/>
	 * addRestServices(context, extHttpService, "org.example.rest", "/example-rest")<br/>
	 * <br/>
	 * This will bind all REST resources in package org.example.rest to /app/example-rest/...
	 * 
	 * @param context The bundle context
	 * @param extHttpService The service(s) will be added to this ExtHttpService
	 * @param basePackage Package that will be scanned for REST resources. All classes with @Path or @Provider annotations 
	 * 		  will be loaded. This package must live in the same dynamic plugin as the Activator that calls this function
	 * @param baseUri The base URI that the REST services will be bound on. The paths defined in @Path annotations are relative to
	 * this path.
	 */
	protected void addRestServices(BundleContext context, ExtHttpService extHttpService, String basePackage, final String baseUri) {
		
		BundleWiring bundleWiring = context.getBundle().adapt(BundleWiring.class);
		Application restApplication = new RestApplication(bundleWiring, basePackage);
		
		ServletContainer restServletContainer = new ServletContainer(restApplication);
		
		try {
			extHttpService.registerServlet(baseUri, restServletContainer, null, null);
		} catch (Exception e) {
			Logger.error(this, "Failed to add REST service", e);
			throw new RuntimeException(e);
		}

		CMSFilter.addExclude(baseUri + "/.*");
	}
	
	/**
	 * Adds a servlet to dotCMS
	 * 
	 * @param context The bundle context
	 * @param extHttpService The service(s) will be added to this ExtHttpService
	 * @param servlet The servlet that must be added
	 * @param servletUri The URI that the servlet will be bound on
	 * @param initParams The init params that will be passed to the servlet
	 */
	protected void addServlet(BundleContext context, ExtHttpService extHttpService, HttpServlet servlet, String servletUri, Dictionary<?, ?> initParams) {
		try {
			extHttpService.registerServlet(servletUri, servlet, initParams, null);
		} catch (Exception e) {
			Logger.error(this, "Failed to add servlet", e);
			throw new RuntimeException(e);
		}
		
		CMSFilter.addExclude(servletUri);
		
		Logger.info(this, "Added servlet: " + servlet.getClass().getName());
	}

	/**
	 * Adds a filter
	 * 
	 * @param context The bundle context
	 * @param extHttpService The service(s) will be added to this ExtHttpService
	 * @param filter The filter that must be added
	 * @param path The path that the filter will be bound on
	 * @param initParams The init params that will be passed to the filter
	 */
	protected void addFilter(BundleContext context, ExtHttpService extHttpService, Filter filter, String path, Dictionary<?, ?> initParams) {
		try {
			extHttpService.registerFilter(filter, path, initParams, 0, null);
		} catch (Exception e) {
			Logger.error(this, "Failed to add servlet", e);
			throw new RuntimeException(e);
		}
		
		Logger.info(this, "Added filter: " + filter.getClass().getName());
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
        activatorUtil.cleanResources( context );
        unregisterServlets( context );
    }

    /**
     * Unpublish this bundle elements
     */
    protected void unpublishBundleServices () {

        //Get the current ClassLoader
        ClassLoader contextClassLoader = getContextClassLoader();
        if ( contextClassLoader instanceof CombinedLoader ) {
            //Try to remove this class loader
            ClassLoader felixClassLoader = getFelixClassLoader();
            ((CombinedLoader) contextClassLoader).removeLoader( felixClassLoader );
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
            for ( ToolInfo toolInfo : viewTools ) {

                this.toolboxManager.removeTool( toolInfo );
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

            ModuleConfig moduleConfig = activatorUtil.getModuleConfig();
            //We need to unfreeze this module in order to add new action mappings
            activatorUtil.unfreeze( moduleConfig );

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
                String scpId = PortalUtil.class.getName() + "." + javax.portlet.Portlet.class.getName();
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
        activatorUtil.unregisterAll( context );
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

    class ActivatorUtil {

        private UrlOsgiClassLoader findCustomURLLoader ( ClassLoader loader ) {

            if ( loader == null ) {
                return null;
            } else if ( loader instanceof UrlOsgiClassLoader ) {
                return (UrlOsgiClassLoader) loader;
            } else {
                return findCustomURLLoader( loader.getParent() );
            }
        }

        private String getBundleFolder ( BundleContext context ) {

            //We will use the bundle jar name as the folder name for the osgi resources we move inside dotCMS
            String bundleLocation = context.getBundle().getLocation();
            String jarFileName = FilenameUtils.getName( bundleLocation );
            jarFileName = jarFileName.replace( ".jar", "" );

            //return OSGI_FOLDER + File.separator + context.getBundle().getBundleId();
            return OSGI_FOLDER + File.separator + jarFileName;
        }

        private ModuleConfig getModuleConfig () {
            ServletContext servletContext = Config.CONTEXT;
            return (ModuleConfig) servletContext.getAttribute( Globals.MODULE_KEY );
        }

        /**
         * In order to be able to add ActionMappings we need to unfreeze the module config, that freeze status don't allow modifications.
         *
         * @param moduleConfig
         * @throws NoSuchFieldException
         * @throws IllegalAccessException
         */
        private void unfreeze ( ModuleConfig moduleConfig ) throws NoSuchFieldException, IllegalAccessException {

            Field configuredField = ModuleConfigImpl.class.getDeclaredField( "configured" );//We need to access it using reflection
            configuredField.setAccessible( true );
            configuredField.set( moduleConfig, false );
            configuredField.setAccessible( false );
        }

        /**
         * Deletes the generated folders and copied files inside dotCMS for this bundle
         *
         * @param context
         */
        private void cleanResources ( BundleContext context ) {

            ServletContext servletContext = Config.CONTEXT;

            //Cleaning the resources under the html folder
            String resourcesPath = servletContext.getRealPath( Constants.TEXT_HTML_DIR + getBundleFolder( context ) );
            File resources = new File( resourcesPath );
            if ( resources.exists() ) {
                FileUtil.deltree( resources );
            }

            //Now cleaning the resources under the velocity folder
            resourcesPath = servletContext.getRealPath( VELOCITY_FOLDER + getBundleFolder( context ) );
            resources = new File( resourcesPath );
            if ( resources.exists() ) {
                FileUtil.deltree( resources );
            }
        }

        /**
         * Util method to copy all the resources inside the folder of the given resource to the corresponding velocity dotCMS folders
         *
         * @param context
         * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
         * @throws Exception
         */
        private void moveVelocityResources ( BundleContext context, String referenceResourcePath ) throws Exception {

            ServletContext servletContext = Config.CONTEXT;
            String destinationPath = servletContext.getRealPath( VELOCITY_FOLDER + getBundleFolder( context ) );

            moveResources( context, referenceResourcePath, destinationPath );
        }

        /**
         * Util method to copy all the resources inside the folder of the given resource to the corresponding dotCMS folders
         *
         * @param context
         * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
         * @throws Exception
         */
        private void moveResources ( BundleContext context, String referenceResourcePath ) throws Exception {

            ServletContext servletContext = Config.CONTEXT;
            String destinationPath = servletContext.getRealPath( Constants.TEXT_HTML_DIR + getBundleFolder( context ) );

            moveResources( context, referenceResourcePath, destinationPath );
        }

        /**
         * Util method to copy all the resources inside a folder to a given destination
         *
         * @param context
         * @param referenceResourcePath reference resource to get its container folder and move the resources inside that folder
         * @param destinationPath
         * @throws Exception
         */
        private void moveResources ( BundleContext context, String referenceResourcePath, String destinationPath ) throws Exception {

            //Get the container folder of the given resource
            String containerFolder = getContainerFolder( referenceResourcePath );

            //Find all the resources under that folder
            Enumeration<URL> entries = context.getBundle().findEntries( containerFolder, "*.*", true );
            while ( entries.hasMoreElements() ) {

                URL entryUrl = entries.nextElement();
                String entryPath = entryUrl.getPath();

                String resourceFilePath = destinationPath + entryPath;
                File resourceFile = new File( resourceFilePath );
                if ( !resourceFile.exists() ) {

                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        if ( !resourceFile.getParentFile().exists() ) {
                            resourceFile.getParentFile().mkdirs();
                        }
                        resourceFile.createNewFile();

                        in = entryUrl.openStream();
                        out = new FileOutputStream( resourceFile );

                        byte[] buffer = new byte[1024];
                        int length;
                        while ( (length = in.read( buffer )) > 0 ) {
                            out.write( buffer, 0, length );
                        }

                    } finally {
                        if ( in != null ) {
                            in.close();
                        }
                        if ( out != null ) {
                            out.flush();
                            out.close();
                        }
                    }
                }

            }

        }

        /**
         * Unregister a servlet using a given mapping servlet
         *
         * @param context
         * @throws Exception
         */
        @SuppressWarnings ("unchecked")
        private void unregisterAll ( BundleContext context ) throws Exception {

            ServiceReference sRef = context.getServiceReference( ExtHttpService.class.getName() );
            if ( sRef != null ) {

                /*
                 Why don't use it in the same way as the activators???, classpaths :)

                 On the felix framework initialization dotCMS loads this class (ExtHttpService) using its own ClassLoader.
                 So I can't use directly this class and its implementation because on this class felix can't use its
                 instance (Created with its own ClassLoader because the dotCMS ClassLoader already loaded the same class,
                 meaning we have in memory a definition that is not the one provided by felix and for that reason they are different, nice... :) ),
                 That will cause runtime errors and that's why we use reflection.
                 */

                //ExtHttpService httpService = (ExtHttpService) context.getService( sRef );
                Object httpService = context.getService( sRef );

                //Now invoke the method that will unregister all the registered Servlets and Filters
                Method unregisterAllMethod = httpService.getClass().getMethod( "unregisterAll" );
                unregisterAllMethod.invoke( httpService );
            }
        }

        /**
         * Util method to get the container folder of a given resource inside this bundle
         *
         * @param path
         * @return
         */
        private String getContainerFolder ( String path ) {

            if ( !path.startsWith( PATH_SEPARATOR ) ) {
                path = PATH_SEPARATOR + path;
            }

            int index = path.indexOf( PATH_SEPARATOR );
            if ( path.startsWith( PATH_SEPARATOR ) ) {
                index = path.indexOf( PATH_SEPARATOR, 1 );
            }

            return path.substring( 1, index + 1 );
        }

        private String getManifestHeaderValue ( BundleContext context, String key ) {
            return context.getBundle().getHeaders().get( key );
        }

    }

}