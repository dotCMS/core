package com.dotmarketing.osgi;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Interceptor;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.VelocityUtil;
import org.apache.felix.http.proxy.DispatcherTracker;
import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.quartz.SchedulerException;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonathan Gamba
 * Date: 7/23/12
 */
public abstract class GenericBundleActivator implements BundleActivator {

    private PrimitiveToolboxManager toolboxManager;
    private WorkflowAPIOsgiService workflowOsgiService;
    private Collection<ToolInfo> viewTools;
    private Collection<WorkFlowActionlet> actionlets;
    private Map<String, String> jobs;
    private Collection preHooks;
    private Collection postHooks;
    private ClassLoaderUtil classLoaderUtil = new ClassLoaderUtil();

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
    protected void initializeServices ( BundleContext context ) {

        forceHttpServiceLoading( context );
        //Forcing the loading of the ToolboxManager
        forceToolBoxLoading( context );
        //Forcing the loading of the WorkflowService
        forceWorkflowServiceLoading( context );
    }

    /**
     * Allow to this bundle/elements to be visible and accessible from the host classpath
     */
    protected void publishBundleServices ( BundleContext context ) {

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
        String overrideClasses = context.getBundle().getHeaders().get( "Override-Classes" );
        if ( overrideClasses != null ) {
            String[] forceOverride = overrideClasses.split( "," );
            for ( String classToOverride : forceOverride ) {
                try {
                    //Just loading the custom implementation will allows to override the one the classloader already had loaded
                    combinedLoader.loadClass( classToOverride.trim() );
                } catch ( ClassNotFoundException e ) {
                    e.printStackTrace();
                }
            }
        }

        //Use this new "combined" class loader
        Thread.currentThread().setContextClassLoader( combinedLoader );
    }

    /**
     * Register a bundle library, this library must be a bundle inside the felix load folder.
     *
     * @param bundleJarFileName bundle file name
     * @throws Exception
     */
    protected void registerBundleLibrary ( String bundleJarFileName ) throws Exception {

        //Felix directories
        String felixDirectory = Config.CONTEXT.getRealPath( File.separator + "WEB-INF" + File.separator + "felix" );
        String autoLoadDirectory = felixDirectory + File.separator + "load";

        //Adding the library to the application classpath
        addFileToClasspath( autoLoadDirectory + File.separator + bundleJarFileName );
    }

    /**
     * Adds a file to the classpath.
     *
     * @param filePath a String pointing to the file
     * @throws java.io.IOException
     */
    protected void addFileToClasspath ( String filePath ) throws Exception {

        File fileToAdd = new File( filePath );
        addFileToClasspath( fileToAdd );
    }

    /**
     * Adds a file to the classpath
     *
     * @param toAdd the file to be added
     * @throws java.io.IOException
     */
    protected void addFileToClasspath ( File toAdd ) throws Exception {

        addURLToApplicationClassLoader( toAdd.toURI().toURL() );
    }

    private void addURLToApplicationClassLoader ( URL url ) throws IntrospectionException {

        ClassLoader contextClassLoader = getContextClassLoader();

        // Create a ClassLoader using the given url
        URLClassLoader urlClassLoader = new URLClassLoader( new URL[]{url} );

        CombinedLoader combinedLoader;
        if ( contextClassLoader instanceof CombinedLoader ) {
            combinedLoader = (CombinedLoader) contextClassLoader;
            //Chain to the current thread classloader
            combinedLoader.addLoader( urlClassLoader );
        } else {
            combinedLoader = new CombinedLoader( contextClassLoader );
            //Chain to the current thread classloader
            combinedLoader.addLoader( urlClassLoader );
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
    private void forceHttpServiceLoading ( BundleContext context ) {

        try {
            //Working with the http bridge
            if ( OSGIProxyServlet.servletConfig != null ) {//If it is null probably the servlet wasn't even been loaded...

                try {
                    OSGIProxyServlet.bundleContext.getBundle();
                } catch ( IllegalStateException e ) {
                    //If we are here is because we have an invalid bundle context, so we need to provide a new one
                    BundleContext httpBundle = context.getBundle( OSGIUtil.BUNDLE_HTTP_BRIDGE_ID ).getBundleContext();
                    OSGIProxyServlet.tracker = new DispatcherTracker( httpBundle, null, OSGIProxyServlet.servletConfig );
                    OSGIProxyServlet.tracker.open();
                    OSGIProxyServlet.bundleContext = httpBundle;
                }

            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //*******************************************************************
    //*******************************************************************
    //****************REGISTER SERVICES METHODS**************************
    //*******************************************************************
    //*******************************************************************

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

        //Verify if the job class is already in the system class loader
        Class currentJobClass = null;
        try {
            currentJobClass = Class.forName( scheduledTask.getJavaClassName(), true, ClassLoader.getSystemClassLoader() );
        } catch ( ClassNotFoundException e ) {
            //Do nothing, the class is not inside the system classloader
        }

        //Get the job class from this bundle context
        Class jobClass = Class.forName( scheduledTask.getJavaClassName(), true, getFelixClassLoader() );
        URL jobClassURL = jobClass.getProtectionDomain().getCodeSource().getLocation();

        //Verify if we have our UrlOsgiClassLoader on the main class loaders
        UrlOsgiClassLoader urlOsgiClassLoader = classLoaderUtil.findCustomURLLoader( ClassLoader.getSystemClassLoader() );
        if ( urlOsgiClassLoader != null ) {

            if ( urlOsgiClassLoader.contains( jobClassURL ) ) {
                //The classloader and the job content in already in the system classloader, so we need to reload the jar contents
                urlOsgiClassLoader.reload( jobClassURL );
            } else {
                urlOsgiClassLoader.addURL( jobClassURL );
            }
        } else {

            if ( currentJobClass != null ) {
                if ( currentJobClass.getClassLoader() instanceof UrlOsgiClassLoader ) {
                    urlOsgiClassLoader = (UrlOsgiClassLoader) currentJobClass.getClassLoader();
                }
            }

            if ( urlOsgiClassLoader == null ) {
                //Getting the reference of a known class in order to get the base/main class loader
                Class baseJobClass = getContextClassLoader().loadClass( "org.quartz.Job" );
                //Creates our custom class loader in order to use it to inject the job code inside dotcms context
                urlOsgiClassLoader = new UrlOsgiClassLoader( jobClassURL, baseJobClass.getClassLoader() );
            } else {
                //The classloader and the job content in already in the system classloader, so we need to reload the jar contents
                urlOsgiClassLoader.reload( jobClassURL );
            }

            /*
            In order to inject the job code inside dotcms context this is the main part of the process,
            is required to insert our custom class loader inside dotcms class loaders hierarchy.
             */
            ClassLoader loader = classLoaderUtil.findFirstLoader( ClassLoader.getSystemClassLoader() );

            Field parentLoaderField = ClassLoader.class.getDeclaredField( "parent" );
            parentLoaderField.setAccessible( true );
            parentLoaderField.set( loader, urlOsgiClassLoader );
            parentLoaderField.setAccessible( false );
        }

        /*
        Schedules the given job in the quartz system, and depending on the sequentialScheduled
        property it will use the sequential of the standard scheduler.
         */
        QuartzUtils.scheduleTask( scheduledTask );
        jobs.put( jobName, jobGroup );

        Logger.info( this, "Added Quartz Job: " + jobName );
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

        if ( preHooks == null ) {
            preHooks = new ArrayList();
        }

        interceptor.addPreHook( preHook );
        preHooks.add( preHook );
    }

    /**
     * Adds a hook to the end of the chain
     *
     * @param postHook
     * @throws Exception
     */
    protected void addPostHook ( Object postHook ) throws Exception {

        Interceptor interceptor = (Interceptor) APILocator.getContentletAPIntercepter();

        if ( postHooks == null ) {
            postHooks = new ArrayList();
        }

        interceptor.addPostHook( postHook );
        postHooks.add( postHook );
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
    protected void unregisterServices () throws Exception {

        unregisterActionlets();
        unregisterViewToolServices();
        unpublishBundleServices();
        unregisterPreHooks();
        unregisterPostHooks();
        unregisterQuartzJobs();
    }

    /**
     * Unpublish this bundle elements
     */
    protected void unpublishBundleServices () {

        //Get the current classloader
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
            for ( Object postHook : postHooks ) {
                interceptor.delPostHook( postHook );
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
            for ( Object preHook : preHooks ) {
                interceptor.delPreHook( preHook );
            }
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

            /*UrlOsgiClassLoader loader = classLoaderUtil.findCustomURLLoader( ClassLoader.getSystemClassLoader() );
            if ( loader != null ) {
                loader.getInstrumentation().removeTransformer( loader.getTransformer() );
            }*/
        }
    }

    class ClassLoaderUtil {

        public UrlOsgiClassLoader findCustomURLLoader ( ClassLoader loader ) {

            if ( loader == null ) {
                return null;
            } else if ( loader instanceof UrlOsgiClassLoader ) {
                return (UrlOsgiClassLoader) loader;
            } else {
                return findCustomURLLoader( loader.getParent() );
            }
        }

        public ClassLoader findFirstLoader ( ClassLoader loader ) {

            if ( loader.getParent() == null ) {
                return loader;
            } else {
                return findFirstLoader( loader.getParent() );
            }
        }

    }

}