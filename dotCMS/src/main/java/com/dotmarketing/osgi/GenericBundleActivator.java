package com.dotmarketing.osgi;

import static com.dotmarketing.osgi.ActivatorUtil.PATH_SEPARATOR;
import static com.dotmarketing.osgi.ActivatorUtil.cleanResources;
import static com.dotmarketing.osgi.ActivatorUtil.getBundleFolder;
import static com.dotmarketing.osgi.ActivatorUtil.getManifestHeaderValue;
import static com.dotmarketing.osgi.ActivatorUtil.getModuleConfig;
import static com.dotmarketing.osgi.ActivatorUtil.moveResources;
import static com.dotmarketing.osgi.ActivatorUtil.moveVelocityResources;
import static com.dotmarketing.osgi.ActivatorUtil.unfreeze;
import static com.dotmarketing.osgi.ActivatorUtil.unregisterAll;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.felix.framework.OSGIUtil;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.quartz.SchedulerException;
import org.tuckey.web.filters.urlrewrite.NormalRule;
import org.tuckey.web.filters.urlrewrite.Rule;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.config.ActionConfig;
import com.dotcms.repackage.org.apache.struts.config.ForwardConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.business.cache.CacheOSGIService;
import com.dotmarketing.business.cache.provider.CacheProvider;
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
import com.dotmarketing.servlets.InitServlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.util.Http;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;

/**
 * Created by Jonathan Gamba
 * Date: 7/23/12
 */
public abstract class GenericBundleActivator implements BundleActivator {

    private static final String MANIFEST_HEADER_OVERRIDE_CLASSES = "Override-Classes";

    private static final String INIT_PARAM_VIEW_JSP = "view-jsp";
    private static final String INIT_PARAM_VIEW_TEMPLATE = "view-template";
    public static final String BYTEBUDDY_CLASS_RELOADING_STRATEGY_NOT_SET_JAVA_AGENT_NOT_SET = "bytebuddy ClassReloadingStrategy not set [java agent not set?]";
    public static final String COM_LIFERAY_PORTLET_JSPPORTLET = "com.liferay.portlet.JSPPortlet";
    public static final String COM_LIFERAY_PORTLET_VELOCITY_PORTLET = "com.liferay.portlet.VelocityPortlet";

    private BundleContext context;


    private PrimitiveToolboxManager toolboxManager;
    private CacheOSGIService cacheOSGIService;
    private ConditionletOSGIService conditionletOSGIService;
    private RuleActionletOSGIService actionletOSGIService;
    private final Collection<ToolInfo> viewTools = new ArrayList<>();
    private final Collection<WorkFlowActionlet> actionlets = new ArrayList<>();
    private final Collection<Conditionlet<?>> conditionlets = new ArrayList<>();
    private final Collection<RuleActionlet<?>> ruleActionlets = new ArrayList<>();
    private final Collection<Class<CacheProvider>> cacheProviders = new ArrayList<>();
    private final Map<String, String> jobs = new HashMap<>();
    private final Collection<ActionConfig> actions = new ArrayList<>();
    private final Map<String,Portlet> portlets = new ConcurrentHashMap<>();
    private final Collection<Rule> rules = new ArrayList<>();
    private final Collection<String> preHooks = new ArrayList<>();
    private final Collection<String> postHooks = new ArrayList<>();
    private final Collection<String> overriddenClasses = new HashSet<>();


    protected ClassLoader getBundleClassloader () {
        return this.getClass().getClassLoader();
    }

    protected ClassLoader getWebAppClassloader () {
        return InitServlet.class.getClassLoader();
    }

    protected ClassReloadingStrategy getClassReloadingStrategy () {
        try {
            return ClassReloadingStrategy.fromInstalledAgent();
        } catch (Exception e) {
            //Even if there is not a java agent set we should continue with the plugin processing
            Logger.warnAndDebug(this.getClass(),
                    "Error reading ClassReloadingStrategy from agent [javaagent not set?].  Classpath overrides might not work: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Verify and initialize if necessary the required OSGI services to create plugins
     *
     * @param context
     */
    protected void initializeServices ( BundleContext context ) throws ClassNotFoundException {

        this.context = context;


        //Override the classes found in the Override-Classes attribute
        overrideClasses(context);

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
     * Reads in the bundle MANIFEST file the <strong>Override-Classes</strong> attribute in order to
     * inject or override each found class in that attribute into the dotCMS class loader.
     * <br/>
     * New classes will be injected, existing ones will be overridden
     */
    protected void overrideClasses(final BundleContext context) throws ClassNotFoundException {

        if (null == this.context) {
            this.context = context;
        }

        //Force the loading of some classes that may be already loaded on the host classpath but we want to override with the ones on this bundle
        String overrideClasses = getManifestHeaderValue( context, MANIFEST_HEADER_OVERRIDE_CLASSES );
        if ( overrideClasses != null && !overrideClasses.isEmpty() ) {

            String[] forceOverride = overrideClasses.split( "," );
            for ( String classToOverride : forceOverride ) {
                //Injecting this bundle context code inside the dotCMS context
                overrideClass( classToOverride );
            }

        }

    }

    /**
     * @deprecated Use {@link #overrideClasses(BundleContext)} instead
     */
    @Deprecated
    protected void publishBundleServices ( BundleContext context ) throws ClassNotFoundException {
        overrideClasses(context);
    }

    /**
     * Is possible on certain scenarios to have our ToolManager without initialization, or most probably a ToolManager without
     * set our required services, so we need to force things a little bit here, and register those services if it is necessary.
     *
     * @param context
     */
    private void forceToolBoxLoading (BundleContext context) {
        ServiceReference<?> serviceRefSelected = context.getServiceReference(PrimitiveToolboxManager.class.getName());
        if (serviceRefSelected == null) {
            // Forcing the loading of the ToolboxManager
            ServletToolboxManager toolboxManagerLocal = (ServletToolboxManager) VelocityUtil.getToolboxManager();
            if (toolboxManagerLocal != null) {
                serviceRefSelected = context.getServiceReference(PrimitiveToolboxManager.class.getName());
                if (serviceRefSelected == null) {
                    toolboxManagerLocal.registerService();
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
        ServiceReference<?> serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
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
        ServiceReference<?> serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
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
        ServiceReference<?> serviceRefSelected = context.getServiceReference(CacheOSGIService.class.getName());
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






    protected void overrideClass(String className) throws ClassNotFoundException {

        if (null == getClassReloadingStrategy()) {
            Logger.error(this, BYTEBUDDY_CLASS_RELOADING_STRATEGY_NOT_SET_JAVA_AGENT_NOT_SET);
            return;
        }

        className = className.trim();

        // Search for the class we want to inject using the felix plugin class loader
        Class<?> clazz = Class.forName(className.trim(), false, getBundleClassloader());

        overrideClass(clazz);
    }





    /**
     * Will inject this bundle context code inside the dotCMS context
     *
     * @param clazz a reference class inside this bundle jar
     */
    protected void overrideClass ( Class<?>  clazz) {

        if (null == getClassReloadingStrategy()) {
            Logger.error(this, BYTEBUDDY_CLASS_RELOADING_STRATEGY_NOT_SET_JAVA_AGENT_NOT_SET);
            return;
        }


        if(!clazz.getClassLoader().equals(getBundleClassloader())) {
            Logger.error(this, "Class:" + clazz.getName() + " not loaded from bundle classloader, cannot override/inject into dotCMS");

        }

        Logger.info(this.getClass().getName(), "Injecting: " + clazz.getName() + " into classloader: " + getWebAppClassloader());
        Logger.debug(this.getClass().getName(),"bundle classloader :" +getBundleClassloader() );
        Logger.debug(this.getClass().getName(),"context classloader:" +getWebAppClassloader() );

        ByteBuddyAgent.install();
        new ByteBuddy()
                .rebase(clazz, ClassFileLocator.ForClassLoader.of(getBundleClassloader()))
                .name(clazz.getName())
                .make()
                .load(getWebAppClassloader(), this.getClassReloadingStrategy());

        this.overriddenClasses.add(clazz.getName());
    }

    //*******************************************************************
    //*******************************************************************
    //****************REGISTER SERVICES METHODS**************************
    //*******************************************************************
    //*******************************************************************


    /**
     * Registers portlets based on the provided XML configurations.
     *
     * @param context the BundleContext
     * @param xmls an array of XML file paths
     * @return a collection of registered portlets
     * @throws Exception if an error occurs during portlet registration
     */
    protected Collection<Portlet> registerPortlets(BundleContext context, String[] xmls) throws Exception {
        for (String xml : xmls) {
            try (InputStream input = new ByteArrayInputStream(Http.URLtoString(context.getBundle().getResource(xml)).getBytes(StandardCharsets.UTF_8))) {
                portlets.putAll(PortletManagerUtil.addPortlets(new InputStream[]{input}));
            }
        }

        for (Map.Entry<String, Portlet> entry : portlets.entrySet()) {
            Portlet portlet = entry.getValue();
            String portletClass = portlet.getPortletClass();

            if (portletClass.equals(COM_LIFERAY_PORTLET_JSPPORTLET) || portletClass.equals(
                    COM_LIFERAY_PORTLET_VELOCITY_PORTLET)) {
                handlePortlet(context, portlet, portletClass);
            }

            Logger.info(this, "Added Portlet: " + portlet.getPortletId());
            OSGIUtil.getInstance().getPortletIDsStopped().remove(portlet.getPortletId());
        }

        // Forcing a refresh of the portlets cache
        APILocator.getPortletAPI().findAllPortlets();

        return portlets.values();
    }

    /**
     * Handles the processing and updating of a specific portlet type.
     *
     * @param context the BundleContext
     * @param portlet the Portlet object
     * @param portletClass the class type of the portlet
     * @throws Exception if an error occurs during portlet processing
     */
    private void handlePortlet(BundleContext context, Portlet portlet, String portletClass) throws Exception {
        Map<String, String> initParams = portlet.getInitParams();
        String pathParam = portletClass.equals(COM_LIFERAY_PORTLET_JSPPORTLET) ? INIT_PARAM_VIEW_JSP : INIT_PARAM_VIEW_TEMPLATE;
        String path = initParams.get(pathParam);

        if (!path.startsWith(PATH_SEPARATOR)) {
            path = PATH_SEPARATOR + path;
        }

        if (portletClass.equals(COM_LIFERAY_PORTLET_JSPPORTLET)) {
            moveResources(context, path);
        } else if (portletClass.equals(COM_LIFERAY_PORTLET_VELOCITY_PORTLET)) {
            moveVelocityResources(context, path);
        }

        Map<String, String> mutableInitParams = new HashMap<>(portlet.getInitParams());
        mutableInitParams.put(pathParam, getBundleFolder(context, File.separator) + path);

        DotPortlet updatedDotPortlet = DotPortlet.builder()
                .from(portlet)
                .initParams(mutableInitParams)
                .build();

        Portlet updatedPortlet = updatedDotPortlet.toPortlet();
        PortletAPI api = APILocator.getPortletAPI();
        api.updatePortlet(updatedPortlet);
        portlets.put(updatedPortlet.getPortletId(), updatedPortlet);
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
    protected void registerActionMapping ( ActionMapping actionMapping )
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        String actionClassType = actionMapping.getType();

        //Injects the action classes inside the dotCMS context
        overrideClass( actionClassType );

        ModuleConfig moduleConfig = getModuleConfig();
        //We need to unfreeze this module in order to add new action mappings
        unfreeze( moduleConfig );

        //Adding the ActionConfig to the ForwardConfig
        moduleConfig.addActionConfig( actionMapping );

        actions.add( actionMapping );
        Logger.info( this, "Added Struts Action Mapping: " + actionClassType );
    }

    /**
     * Register a given Quartz Job scheduled task
     *
     * @param scheduledTask
     * @throws Exception
     */
    protected void scheduleQuartzJob ( ScheduledTask scheduledTask ) throws ClassNotFoundException, SchedulerException, ParseException {

        String jobName = scheduledTask.getJobName();
        String jobGroup = scheduledTask.getJobGroup();

        //Injects the job classes inside the dotCMS context
        overrideClass( scheduledTask.getJavaClassName() );

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

            //Adding the Rule to the filter
            urlRewriteFilter.addRule( rule );
            rules.add( rule );
        } else {
            throw new DotRuntimeException( "Non UrlRewriteFilter found!" );
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
    protected void registerActionlet ( BundleContext context, WorkFlowActionlet actionlet ) {

        //Getting the service to register our Actionlet
        ServiceReference<?> serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
        }


        OSGIUtil.getInstance().workflowOsgiService = (WorkflowAPIOsgiService) context.getService( serviceRefSelected );
        OSGIUtil.getInstance().workflowOsgiService.addActionlet( actionlet.getClass() );
        actionlets.add( actionlet );

        Logger.info( this, "Added actionlet: " + actionlet.getName() );
        OSGIUtil.getInstance().actionletsStopped.remove(actionlet.getClass().getCanonicalName());
    }

    /**
     * Register a Rules Engine RuleActionlet service
     */
    protected void registerRuleActionlet(BundleContext context, RuleActionlet<?> actionlet) {

        //Getting the service to register our Actionlet
        ServiceReference<?> serviceRefSelected = context.getServiceReference(RuleActionletOSGIService.class.getName());
        if(serviceRefSelected == null) {
            return;
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
    protected void registerRuleConditionlet ( BundleContext context, Conditionlet<?> conditionlet) {

        //Getting the service to register our Conditionlet
        ServiceReference<?> serviceRefSelected = context.getServiceReference( ConditionletOSGIService.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
        }


        this.conditionletOSGIService = (ConditionletOSGIService) context.getService( serviceRefSelected );
        this.conditionletOSGIService.addConditionlet(conditionlet.getClass());
        conditionlets.add( conditionlet );

        Logger.info( this, "Added Rule Conditionlet: " + conditionlet.getId() );
        registerBundleResourceMessages(context);
    }

    private void registerBundleResourceMessages(BundleContext context) {
        Enumeration<String> langFiles = context.getBundle().getEntryPaths("messages");
        while (langFiles != null && langFiles.hasMoreElements()) {
            String langFile = langFiles.nextElement();
            if (isValidLanguageFile(langFile)) {
                processLanguageFile(context, langFile);
            }
        }
    }

    private boolean isValidLanguageFile(String langFile) {
        String languageFilePrefix = "messages/Language_";
        String languageFileSuffix = ".properties";
        String languageFileDelimiter = "-";

        return langFile.startsWith(languageFilePrefix)
                && langFile.contains(languageFileDelimiter)
                && langFile.endsWith(languageFileSuffix);
    }

    private void processLanguageFile(BundleContext context, String langFile) {
        String languageFilePrefix = "messages/Language_";
        String languageFileSuffix = ".properties";
        String languageFileDelimiter = "-";

        String languageCountry = langFile.replace(languageFilePrefix, "").replace(languageFileSuffix, "");
        String[] languageCountryParts = languageCountry.split(languageFileDelimiter);
        String languageCode = languageCountryParts[0];
        String countryCode = languageCountryParts[1];

        URL file = context.getBundle().getEntry(langFile);
        Map<String, String> generalKeysToAdd = readLanguageFile(file);

        if (generalKeysToAdd != null) {
            saveLanguageKeys(languageCode, countryCode, generalKeysToAdd, languageCountry);
        }
    }

    private Map<String, String> readLanguageFile(URL file) {
        Map<String, String> generalKeysToAdd = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(file.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("=")) {
                    String[] keyValue = line.split("=", 2);
                    generalKeysToAdd.put(keyValue[0], keyValue[1]);
                }
            }
        } catch (IOException e) {
            Logger.error(this.getClass(), "Error opening Language File: " + file, e);
            return Map.of();
        }
        return generalKeysToAdd;
    }

    private void saveLanguageKeys(String languageCode, String countryCode, Map<String, String> generalKeysToAdd, String languageCountry) {
        try {
            Language languageObject = APILocator.getLanguageAPI().getLanguage(languageCode, countryCode);
            if (languageObject != null && UtilMethods.isSet(languageObject.getLanguageCode())) {
                //noinspection removal
                APILocator.getLanguageAPI().saveLanguageKeys(languageObject, generalKeysToAdd, new HashMap<>(), new HashSet<>());
            } else {
                Logger.warn(this.getClass(), "Country and Language do not exist: " + languageCountry);
            }
        } catch (DotDataException e) {
            Logger.error(this.getClass(), "Error inserting language properties for: " + languageCountry, e);
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
    protected void registerCacheProvider ( BundleContext context, String cacheRegion, Class<CacheProvider> provider ) throws Exception {

        //Getting the service to register our Cache provider implementation
        ServiceReference<?> serviceRefSelected = context.getServiceReference(CacheOSGIService.class.getName());
        if ( serviceRefSelected == null ) {
            return;
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
    protected void registerViewToolService ( BundleContext context, ToolInfo info ) {

        //Getting the service to register our ViewTool
        ServiceReference<?> serviceRefSelected = context.getServiceReference( PrimitiveToolboxManager.class.getName() );
        if ( serviceRefSelected == null ) {
            return;
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
    protected void addPreHook ( Object preHook )
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        //First we need to be sure we are not adding the same hook more than once
        interceptor.delPreHookByClassName( preHook.getClass().getName() );

        interceptor.addPreHook( preHook );
        preHooks.add( preHook.getClass().getName() );
    }

    /**
     * Adds a hook to the end of the chain
     *
     * @param postHook
     * @throws Exception
     */
    protected void addPostHook ( Object postHook )
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        //First we need to be sure we are not adding the same hook more than once
        interceptor.delPostHookByClassName( postHook.getClass().getName() );


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
    protected void unpublishBundleServices () {

        if (!this.overriddenClasses.isEmpty()) {

            if (null == this.getClassReloadingStrategy()) {
                Logger.error(this,
                        BYTEBUDDY_CLASS_RELOADING_STRATEGY_NOT_SET_JAVA_AGENT_NOT_SET);
                return;
            }

            for (String overridenClass : this.overriddenClasses) {
                try {
                    this.getClassReloadingStrategy()
                            .reset(ClassFileLocator.ForClassLoader.of(getWebAppClassloader()),
                                    Class.forName(overridenClass));
                } catch (Exception e) {
                    Logger.debug(this.getClass(),
                            String.format("Error resetting [%s] class in dotCMS classloader",
                                    overridenClass));
                }
            }
        }
    }

    /**
     * Unregister the registered WorkFlowActionlet services
     */
    protected void unregisterActionlets () {

        if (OSGIUtil.getInstance().workflowOsgiService != null) {
            for ( WorkFlowActionlet actionlet : actionlets ) {
                if(!OSGIUtil.getInstance().actionletsStopped.contains(actionlet.getClass().getCanonicalName())){
                    OSGIUtil.getInstance().actionletsStopped.add(actionlet.getClass().getCanonicalName());
                }
            }
        }
    }

    /**
     * Unregister the registered Rules Conditionlet services
     */
    protected void unregisterConditionlets() {

        if (this.conditionletOSGIService != null) {
            for ( Conditionlet<?> conditionlet : conditionlets ) {

                this.conditionletOSGIService.removeConditionlet(conditionlet.getClass().getSimpleName());
                Logger.info( this, "Removed Rules Conditionlet: " + conditionlet.getClass().getSimpleName());
            }
        }
    }

    /**
     * Unregister the registered Rules Actionlets services
     */
    protected void unregisterRuleActionlets() {

        if (this.actionletOSGIService != null) {
            for ( RuleActionlet<?> actionlet : ruleActionlets ) {

                this.actionletOSGIService.removeRuleActionlet(actionlet.getClass().getSimpleName());
                Logger.info( this, "Removed Rules Actionlet: " + actionlet.getClass().getSimpleName());
            }
        }
    }

    /**
     * Unregister the registered CacheProviders services
     */
    protected void unregisterCacheProviders () {

        if (this.cacheOSGIService != null) {
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

        if (this.toolboxManager != null) {

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
        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        for ( String postHook : postHooks ) {
            interceptor.delPostHookByClassName( postHook );
        }
    }

    /**
     * Unregister all the registered pre hooks
     *
     * @throws Exception
     */
    protected void unregisterPreHooks () {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();
        for ( String preHook : preHooks ) {
            interceptor.delPreHookByClassName( preHook );
        }
    }

    /**
     * Unregister all the registered ActionMappings
     *
     * @throws Exception
     */
    protected void unregisterActionMappings () throws NoSuchFieldException, IllegalAccessException {

        ModuleConfig moduleConfig = getModuleConfig();
        //We need to unfreeze this module in order to add new action mappings
        unfreeze( moduleConfig );

        for ( ActionConfig actionConfig : actions ) {
            moduleConfig.removeActionConfig( actionConfig );
        }
        moduleConfig.freeze();

    }

    /**
     * Unregister all the registered Quartz Jobs
     *
     * @throws SchedulerException
     */
    protected void unregisterQuartzJobs () throws SchedulerException {

        for ( Map.Entry<String,String> entry : jobs.entrySet() ) {
            QuartzUtils.removeJob( entry.getKey(), entry.getValue() );
        }
    }

    /**
     * Unregister all the registered Portlets
     *
     * @throws SchedulerException
     */
    protected void unregisterPortlets () {
        for ( Portlet portlet : portlets.values() ) {
            if(!OSGIUtil.getInstance().portletIDsStopped.contains(portlet.getPortletId())){
                OSGIUtil.getInstance().portletIDsStopped.add(portlet.getPortletId());
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

        //Get a reference of our url rewrite filter
        DotUrlRewriteFilter urlRewriteFilter = DotUrlRewriteFilter.getUrlRewriteFilter();
        if ( urlRewriteFilter != null ) {
            for ( Rule rule : rules ) {
                //Remove from the filter this rule
                urlRewriteFilter.removeRule( rule );
            }
        } else {
            throw new DotRuntimeException( "Non UrlRewriteFilter found!" );
        }
    }

}
