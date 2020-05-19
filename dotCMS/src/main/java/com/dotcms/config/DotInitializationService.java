package com.dotcms.config;

import com.dotcms.api.system.event.PayloadVerifierFactoryInitializer;
import com.dotcms.api.system.event.SystemEventProcessorFactoryInitializer;
import com.dotcms.rendering.velocity.events.ExceptionHandlersInitializer;
import com.dotcms.storage.StorageProviderInitializer;
import com.dotcms.system.event.local.business.LocalSystemEventSubscribersInitializer;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.*;
import org.apache.commons.lang.time.StopWatch;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

import static com.dotcms.util.CollectionsUtils.linkSet;

/**
 * This class is in charge of the Services, Factories or any eager Component to be initialized
 *
 * It use three approaches:
 *
 * 1) {@link java.util.ServiceLoader} will use the standard java extension mechanism, the class to extends will be {@link DotInitializer}
 * 2) List of class names comma separated on the  dotmarketing-config.properties by using the property: dotcms.initializationservice.services
 * 3) You can extends this class and override on the  dotmarketing-config.properties by using the property: dotcms.initializationservice.classname
 *
 * @author jsanca
 */
public class DotInitializationService implements Serializable {

    /**
     * Key for dotmarketing-config.properties in order to get the comma separated list of {@link DotInitializer} classes.
     */
    private static final String SERVICES_KEY = "dotcms.initializationservice.services";
    private final StopWatch stopWatch = new StopWatch();

    /**
     * Key for dotmarketing-config.properties in order to get a subclass of {@link DotInitializationService} in order you want a specific init.
     * If you use a custom implementation, you might want to override getInitializers() (by default returns a null set)
     */
    private static final String INITIALIZATION_SERVICE_CLASSNAME_KEY = "dotcms.initializationservice.classname";

    protected DotInitializationService () {

    }

    private void initService (final DotInitializer initializer) {

        try {

            this.stopWatch.reset();
            this.stopWatch.start();

            Logger.info(this, "Initializing :" + initializer.getName());

            initializer.init();

            this.stopWatch.stop();

            Logger.info(this, "Initializing Done:" + initializer.getName() + ", duration:" +
                    DateUtil.millisToSeconds(this.stopWatch.getTime()) + " seconds");
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
     * Runs the initialization
     */
    public synchronized void initialize() {

        final Set<DotInitializer> initializers              = this.getInternalInitializers();
        final Set<DotInitializer> subClassInitializers      = this.getInitializers();
        final Set<DotInitializer> dotMarketingInitializers  = this.getDotMarketingInitializers();
        final Set<DotInitializer> serviceLoaderInitializers = this.getServiceLoaderInitializers();


        if (null != subClassInitializers) {

            initializers.addAll(subClassInitializers);
        }

        if (null != dotMarketingInitializers) {

            initializers.addAll(dotMarketingInitializers);
        }

        if (null != serviceLoaderInitializers) {

            initializers.addAll(serviceLoaderInitializers);
        }

        Logger.info(this, "Initializing DotCMS services");

        initializers.forEach(this::initService);
    } // initialize.

    /**
     * Returns a list of internal components to initialize
     *
     * @return List with the {@link DotInitializer} elements to initialize
     */
    private Set<DotInitializer> getInternalInitializers() {

        return linkSet(
                new SystemEventProcessorFactoryInitializer(),
                new PayloadVerifierFactoryInitializer(),
                new LocalSystemEventSubscribersInitializer(),
                (DotInitializer)APILocator.getPersonaAPI(),
                new ExceptionHandlersInitializer(),
                new StorageProviderInitializer()
                );
    } // getInternalInitializers.

    private Set<DotInitializer> getServiceLoaderInitializers() {

        Set<DotInitializer> initializerSet = null;
        final ServiceLoader<DotInitializer> serviceLoader =
                ServiceLoader.load(DotInitializer.class);

        final Iterator<DotInitializer> iterator =
                serviceLoader.iterator();

        if (iterator.hasNext()) {

            initializerSet = new LinkedHashSet<>();
            initializerSet.add(iterator.next());
        }

        while (iterator.hasNext()) {

            initializerSet.add(iterator.next());
        }

        return initializerSet;
    }

    /**
     * By default returns a null set, but if you override it, you can return your custom set of {@link DotInitializer}
     * @return Set of {@link DotInitializer}
     */
    protected Set<DotInitializer> getInitializers() {
        return null;
    } // getInitializers.


    private Set<DotInitializer> getDotMarketingInitializers() {

        Set<DotInitializer> initializerSet = null;
        String [] implementationArray = null;
        final String implementations  = Config.getStringProperty(SERVICES_KEY, null);

        if (UtilMethods.isSet(implementations)) {

            implementationArray =
                    StringUtils.splitByCommas(implementations);

            if (UtilMethods.isSet(implementationArray)) {

                initializerSet =  new LinkedHashSet<>();

                for (String implementation : implementationArray) {

                    this.addInitializer(initializerSet, implementation);
                }
            }
        }

        return initializerSet;
    } // getDotMarketingInitializers.

    private void addInitializer(final Set<DotInitializer> initializerSet,
                                final String implementation) {

        final Object initializerObject;
        if (UtilMethods.isSet(implementation)) {

            initializerObject =
                    ReflectionUtils.newInstance(implementation);

            if (UtilMethods.isSet(initializerObject)
                    && initializerObject instanceof DotInitializer) {

                initializerSet.add((DotInitializer) initializerObject);
            } else {

                Logger.error(this, "The object " + implementation
                        + "is not a DotInitializer, can not be initialized");
            }
        }
    } // addInitializer.

    /**
     * Get the instance, custom or default one.
     * @return DotInitializationService
     */
    public static DotInitializationService getInstance () {

        DotInitializationService service = null;
        final String customImpl = Config.getStringProperty
                (INITIALIZATION_SERVICE_CLASSNAME_KEY, null);

        // if has a custom implemention use it.
        if (UtilMethods.isSet(customImpl)) {

            service = (DotInitializationService)
                    ReflectionUtils.newInstance(customImpl);
        }

        // otherwise use this one as a default on.
        if (null == service) {

            service = new DotInitializationService();
        }

        return service;
    } // getInstance.

} // E:O:F:DotInitializationService.
