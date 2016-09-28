package com.dotmarketing.osgi;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.org.apache.felix.http.proxy.DispatcherTracker;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.config.ActionConfig;
import com.dotcms.repackage.org.apache.struts.config.ForwardConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.osgi.framework.*;
import com.dotcms.repackage.org.tuckey.web.filters.urlrewrite.NormalRule;
import com.dotcms.repackage.org.tuckey.web.filters.urlrewrite.Rule;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.business.cache.CacheOSGIService;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.filters.DotUrlRewriteFilter;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.actionlet.RuleActionletOSGIService;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import com.dotmarketing.portlets.rules.conditionlet.ConditionletOSGIService;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.ejb.PortletManager;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.ejb.PortletPK;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.Http;
import com.liferay.util.SimpleCachePool;

import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.quartz.SchedulerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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
    private CacheOSGIService cacheOSGIService;
    private ConditionletOSGIService conditionletOSGIService;
    private RuleActionletOSGIService actionletOSGIService;
    private Collection<ToolInfo> viewTools;
    private Collection<WorkFlowActionlet> actionlets;
    private Collection<Conditionlet> conditionlets;
    private Collection<RuleActionlet> ruleActionlets;
    private Collection<Class<CacheProvider>> cacheProviders;
    private Map<String, String> jobs;
    private Collection<ActionConfig> actions;
    private Collection<Portlet> portlets;
    private Collection<Rule> rules;
    private Collection<String> preHooks;
    private Collection<String> postHooks;

    private ClassLoader getFelixClassLoader () {
        return this.getClass().getClassLoader();
    }

    private ClassLoader getContextClassLoader () {
        return Thread.currentThread().getContextClassLoader();
    }

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
        //Forcing the loading of the Rule Engine Conditionlet
        forceRuleConditionletServiceLoading(context);
        //Forcing the loading of the CacheOSGIService
        forceCacheProviderServiceLoading(context);
    }

    /**
     * Allow to this bundle elements to be visible and accessible from the host classpath (Current thread class loader)
     *
     * @param context
     * @throws Exception
     */
    protected void publishBundleServices ( BundleContext context ) throws Exception {

        if ( this.context == null ) {
            this.context = context;
        }

        //ClassLoaders
        ClassLoader felixClassLoader = getFelixClassLoader();
        ClassLoader contextClassLoader = getContextClassLoader();

        //Create a new class loader where we can "combine" our classLoaders
        CombinedLoader combinedLoader;
        if ( contextClassLoader instanceof CombinedLoader ) {
            combinedLoader = (CombinedLoader) contextClassLoader;
            combinedLoader.addLoader( felixClassLoader );
        } else {
            combinedLoader = new CombinedLoader( contextClassLoader );
            combinedLoader.addLoader( felixClassLoader );
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
     * Is possible on certain scenarios to have our Rule Conditionlet without initialization, or most probably a Rule Conditionlet without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceRuleConditionletServiceLoading ( BundleContext context ) {

        //Getting the service to register our Actionlet.
        ServiceReference serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
        if ( serviceRefSelected == null ) {

            //Forcing the loading of the Rule Conditionlet Service.
            RulesAPI rulesAPI = APILocator.getRulesAPI();
            if ( rulesAPI != null ) {

                serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
                if ( serviceRefSelected == null ) {
                    //Forcing the registration of our required services
                    rulesAPI.registerBundleService();
                }
            }
        }
    }

    /**
     * Is possible on certain scenarios to have our Rule Conditionlet without initialization, or most probably a Rule Conditionlet without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceRuleActionletServiceLoading ( BundleContext context ) {

        //Getting the service to register our Actionlet.
        ServiceReference serviceRefSelected = context.getServiceReference( RuleActionletOSGIService.class.getName() );
        if ( serviceRefSelected == null ) {

            //Forcing the loading of the Rule Conditionlet Service.
            RulesAPI rulesAPI = APILocator.getRulesAPI();
            if ( rulesAPI != null ) {

                serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
                if ( serviceRefSelected == null ) {
                    //Forcing the registration of our required services
                    rulesAPI.registerBundleService();
                }
            }
        }
    }

    /**
     * Is possible on certain scenarios to have our CacheProviderAPI without initialization, or most probably a CacheProviderAPI without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceCacheProviderServiceLoading ( BundleContext context ) {

        //Getting the service to register our CacheProvider
        ServiceReference serviceRefSelected = context.getServiceReference(CacheOSGIService.class.getName());
        if ( serviceRefSelected == null ) {

            //Forcing the loading of the CacheOSGIService
            CacheProviderAPI cacheProviderAPI = APILocator.getCacheProviderAPI();
            if ( cacheProviderAPI != null ) {

                serviceRefSelected = context.getServiceReference(CacheOSGIService.class.getName());
                if ( serviceRefSelected == null ) {
                    //Forcing the registration of our required services
                    cacheProviderAPI.registerBundleService();
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
            if ( System.getProperty(WebKeys.OSGI_ENABLED)!=null ) {//If it is null probably the servlet wasn't even been loaded...

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
        Class clazz = Class.forName( className, false, getFelixClassLoader() );
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
            Class baseClass = getContextClassLoader().loadClass( "com.dotcms.rest.CMSConfigResource" );
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
                portlet.getInitParams().put( INIT_PARAM_VIEW_JSP, getBundleFolder( context, File.separator ) + jspPath );
            } else if ( portlet.getPortletClass().equals( "com.liferay.portlet.VelocityPortlet" ) ) {

                Map initParams = portlet.getInitParams();
                String templatePath = (String) initParams.get( INIT_PARAM_VIEW_TEMPLATE );

                if ( !templatePath.startsWith( PATH_SEPARATOR ) ) {
                    templatePath = PATH_SEPARATOR + templatePath;
                }

                //Copy all the resources inside the folder of the given resource to the corresponding velocity dotCMS folders
                moveVelocityResources( context, templatePath );
                portlet.getInitParams().put( INIT_PARAM_VIEW_TEMPLATE, getBundleFolder( context, File.separator ) + templatePath );
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

        String forwardMapping = getBundleFolder( context, File.separator ) + path;

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
     * Register a Rules Engine RuleActionlet service
     */
    @SuppressWarnings("unchecked")
    protected void registerRuleActionlet(BundleContext context, RuleActionlet actionlet) {

        //Getting the service to register our Actionlet
        ServiceReference serviceRefSelected = context.getServiceReference(RuleActionletOSGIService.class.getName());
        if(serviceRefSelected == null) {
            return;
        }

        if(ruleActionlets == null) {
            ruleActionlets = new ArrayList<>();
        }

        this.actionletOSGIService = (RuleActionletOSGIService)context.getService(serviceRefSelected);
        this.actionletOSGIService.addRuleActionlet(actionlet.getClass());
        ruleActionlets.add(actionlet);
        registerBundleResourceMessages(context);
    }

    /**
     * Register a Rules Engine Conditionlet service
     *
     * @param context
     * @param conditionlet
     */
    @SuppressWarnings ("unchecked")
    protected void registerRuleConditionlet ( BundleContext context, Conditionlet conditionlet) {

        //Getting the service to register our Conditionlet
        ServiceReference serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
        }

        if ( conditionlets == null ) {
            conditionlets = new ArrayList<>();
        }

        this.conditionletOSGIService = (ConditionletOSGIService) context.getService( serviceRefSelected );
        this.conditionletOSGIService.addConditionlet(conditionlet.getClass());
        conditionlets.add( conditionlet );

        Logger.info( this, "Added Rule Conditionlet: " + conditionlet.getId() );
        registerBundleResourceMessages(context);
    }

    private void registerBundleResourceMessages(BundleContext context) {
        //Register Language under /resources/messages folder.
        Enumeration<String> langFiles = context.getBundle().getEntryPaths("messages");
        while(langFiles != null && langFiles.hasMoreElements() ){

            String langFile = langFiles.nextElement();

            //We need to verify file is a language file.
            String languageFilePrefix = "messages/Language_";
            String languageFileSuffix = ".properties";
            String languageFileDelimiter = "-";

            //Validating Language file name: For example: Language_en-US.properties
            if(langFile.startsWith(languageFilePrefix)
                && langFile.contains(languageFileDelimiter)
                && langFile.endsWith(languageFileSuffix)){

                //Get the Language and Country Codes.
                String languageCountry = langFile.replace(languageFilePrefix,"").replace(languageFileSuffix, "");
                String languageCode = languageCountry.split(languageFileDelimiter)[0];
                String countryCode = languageCountry.split(languageFileDelimiter)[1];

                URL file = context.getBundle().getEntry(langFile);
                Map<String, String> generalKeysToAdd = new HashMap<>();

                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(file.openStream()));
                    String line;
                    while ((line = in.readLine()) != null){
                        //Make sure line contains = sign.
                        String delimiter = "=";
                        if(line.contains(delimiter)){
                            String[] keyValue = line.split(delimiter);
                            String key = keyValue[0];
                            String value = keyValue[1];

                            generalKeysToAdd.put(key,value);
                        }
                    }
                } catch (IOException e) {
                    Logger.error(this.getClass(), "Error opening Language File: " + langFile, e);
                }

                try {
                    Language languageObject = APILocator.getLanguageAPI().getLanguage(languageCode, countryCode);
                    if(UtilMethods.isSet(languageObject.getLanguageCode())){

                        APILocator.getLanguageAPI().saveLanguageKeys(languageObject,
                                                                     generalKeysToAdd, new HashMap<>(), new HashSet<>());
                    } else {
                        Logger.warn(this.getClass(), "Country and Language do not exist: " + languageCountry);
                    }

                } catch (DotDataException e) {
                    Logger.error(this.getClass(), "Error inserting language properties for: " + languageCountry, e);
                }
            }
        }
    }


    /**
     * Register a given CacheProvider implementation for a given region
     *
     * @param context
     * @param cacheRegion
     * @param provider
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings ( "unchecked" )
    protected void registerCacheProvider ( BundleContext context, String cacheRegion, Class<CacheProvider> provider ) throws Exception {

        //Getting the service to register our Cache provider implementation
        ServiceReference serviceRefSelected = context.getServiceReference(CacheOSGIService.class.getName());
        if ( serviceRefSelected == null ) {
            return;
        }

        if ( cacheProviders == null ) {
            cacheProviders = new ArrayList<>();
        }

        this.cacheOSGIService = (CacheOSGIService) context.getService(serviceRefSelected);
        this.cacheOSGIService.addCacheProvider(cacheRegion, provider);
        cacheProviders.add(provider);

        Logger.info(this, "Added Cache Provider: " + provider.getName());
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
        unregisterCacheProviders();
        unregisterConditionlets();
        unregisterRuleActionlets();
        unregisterViewToolServices();
        unregisterPreHooks();
        unregisterPostHooks();
        unregisterQuartzJobs();
        unregisterActionMappings();
        unregisterPortlets();
        unregisterRewriteRule();
        cleanResources( context );
        unregisterServlets( context );
        unpublishBundleServices();
    }

    /**
     * Unpublish this bundle elements
     */
    protected void unpublishBundleServices () throws Exception {

        //Get the current ClassLoader
        ClassLoader contextClassLoader = getContextClassLoader();
        if ( contextClassLoader instanceof CombinedLoader ) {
            //Try to remove this class loader
            ClassLoader felixClassLoader = getFelixClassLoader();
            ((CombinedLoader) contextClassLoader).removeLoader( felixClassLoader );
        }

        //Find the UrlOsgiClassLoader for this bundleId
        UrlOsgiClassLoader urlOsgiClassLoader = findCustomURLLoader( ClassLoader.getSystemClassLoader(), this.context.getBundle().getBundleId() );
        if ( urlOsgiClassLoader != null ) {

            //Get the activator class for this OSGI bundle
            String activatorClass = getManifestHeaderValue( context, MANIFEST_HEADER_BUNDLE_ACTIVATOR );
            //Get the location of this OSGI bundle jar source code using a known class inside this bundle
            Class clazz = Class.forName( activatorClass, false, this.getClass().getClassLoader() );
            URL sourceURL = clazz.getProtectionDomain().getCodeSource().getLocation();

            //First verify if the source url exist
            File source = new File( sourceURL.getPath() );
            if ( source.exists() ) {
                //Restoring to their original state any override class
                urlOsgiClassLoader.reload( sourceURL, true );
            }

            /*
             Closes this URLClassLoader, so that it can no longer be used to load new classes or resources that are defined by this loader.
             Classes and resources defined by any of this loader's parents in the delegation hierarchy are still accessible.
             Also, any classes or resources that are already loaded, are still accessible.
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
                Logger.info( this, "Removed actionlet: " + actionlet.getClass().getCanonicalName());
            }
        }
    }

    /**
     * Unregister the registered Rules Conditionlet services
     */
    protected void unregisterConditionlets() {

        if ( this.conditionletOSGIService != null && conditionletOSGIService != null ) {
            for ( Conditionlet conditionlet : conditionlets ) {

                this.conditionletOSGIService.removeConditionlet(conditionlet.getClass().getSimpleName());
                Logger.info( this, "Removed Rules Conditionlet: " + conditionlet.getClass().getSimpleName());
            }
        }
    }

    /**
     * Unregister the registered Rules Actionlets services
     */
    protected void unregisterRuleActionlets() {

        if ( this.actionletOSGIService != null && actionletOSGIService != null ) {
            for ( RuleActionlet actionlet : ruleActionlets ) {

                this.actionletOSGIService.removeRuleActionlet(actionlet.getClass().getSimpleName());
                Logger.info( this, "Removed Rules Actionlet: " + actionlet.getClass().getSimpleName());
            }
        }
    }

    /**
     * Unregister the registered CacheProviders services
     */
    protected void unregisterCacheProviders () {

        if ( this.cacheOSGIService != null && cacheProviders != null ) {
            for ( Class<CacheProvider> provider : cacheProviders ) {

                this.cacheOSGIService.removeCacheProvider(provider);
                Logger.info(this, "Removed Cache Provider: " + provider.getCanonicalName());
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
     * Unregister all the registered Portlets
     *
     * @throws SchedulerException
     */
    protected void unregisterPortlets () throws Exception {

        if ( portlets != null ) {

            PortletManager portletManager = PortletManagerFactory.getManager();
            Company company = PublicCompanyFactory.getDefaultCompany();

            for ( Portlet portlet : portlets ) {

                //PK
                PortletPK id = portlet.getPrimaryKey();
                //Cache key
                String scpId = PortalUtil.class.getName() + "." + com.dotcms.repackage.javax.portlet.Portlet.class.getName();
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

}
