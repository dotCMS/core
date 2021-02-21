package com.dotcms.config;

import com.dotcms.util.Cleanable;
import com.dotcms.util.ServletContextAware;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dotcms.util.CollectionsUtils.treeSet;

/**
 * Class in charge of init, reload or merge the dotcms config
 * @author jsanca
 */
public class DotCMSConfigInitializer {

    public static final  String DOT_MARKETING_CONFIG_PROPERTY_FILE  = "dotmarketing-config.properties";
    public static final  String DOTCMS_CONFIG_CLUSTER_PROPERTY_FILE = "dotcms-config-cluster.properties";
    private static final File [] dotCMSConfigFiles = FileUtil.getFilesFromClassLoader(DOT_MARKETING_CONFIG_PROPERTY_FILE, DOTCMS_CONFIG_CLUSTER_PROPERTY_FILE);
    private static final AtomicBoolean isInit = new AtomicBoolean(false);

    // todo: a set of Initializers that can be add programatically by OSGI.

    /**
     * Init not only loads the configuration, but also an additional services
     * So call init only at the begin of the app, them call merge or reload
     * Note: this method can be called just once, after need to call to reload or merge.
     * @param context
     */
    public static void init (final ServletContext context) {

        if (!isInit.get()) {
            internalReload(context);
            // todo: add here the watchers for the dotcms config properties
            isInit.set(true);
        }
    }

    /**
     * Clear all Configura
     * @param context
     */
    public static void reload (final ServletContext context) {

        if (isInit.get()) {
            internalReload(context);
        } else {

            throw new RuntimeException("Could not reload dotCMS configuration, before has to be init");
        }
    }

    private static void internalReload (final ServletContext context) {

        Cleanable.class.cast(APILocator.getConfigAPI()).clear();
        internalMerge(context);
    }

    public static void merge (final ServletContext context) {

        if (isInit.get()) {
            internalMerge(context);
        } else {

            throw new RuntimeException("Could not merge dotCMS configuration, before has to be init");
        }
    }
    public static void internalMerge (final ServletContext context) {

        ServletContextAware.class.cast(APILocator.getConfigAPI()).setServletContext(context);
        final MutableInt suggestedOrder = new MutableInt(0);
        final Set<ConfigurationProvider> internalInitializers      = getInternalInitializers(suggestedOrder);
        final Set<ConfigurationProvider> serviceLoaderInitializers = Try.of(()->getServiceLoaderInitializers(suggestedOrder)).getOrNull();
        final Set<ConfigurationProvider> initializerSet            = new TreeSet<>();

        initializerSet.addAll(internalInitializers);
        if (null != serviceLoaderInitializers) {

            initializerSet.addAll(serviceLoaderInitializers);
        }

        for (final ConfigurationProvider internalInitializer : initializerSet) {

            try {
                APILocator.getConfigAPI().setProperties(internalInitializer.getConfig());
            } catch (Exception e) {

                Logger.fatal(DotCMSConfigInitializer.class, "Can not add the configuration from : " + internalInitializer.getName());
            }
        }
    }

    private static Set<ConfigurationProvider> getInternalInitializers(final MutableInt suggestedOrder) {

        return treeSet(
                new FilePropertiesConfigurationProvider(dotCMSConfigFiles).suggestOrder(suggestedOrder.getAndIncrement()),
                new EnvConfigurationProvider().suggestOrder(suggestedOrder.getAndIncrement())
                );
    } // getInternalInitializers.

    private static Set<ConfigurationProvider> getServiceLoaderInitializers(final MutableInt suggestedOrder) {

        Set<ConfigurationProvider> initializerSet = null;
        final ServiceLoader<ConfigurationProvider> serviceLoader =
                ServiceLoader.load(ConfigurationProvider.class);

        final Iterator<ConfigurationProvider> iterator =
                serviceLoader.iterator();

        if (iterator.hasNext()) {

            initializerSet = new TreeSet<>();
            initializerSet.add(iterator.next().suggestOrder(suggestedOrder.getAndIncrement()));
        }

        while (iterator.hasNext()) {

            initializerSet.add(iterator.next().suggestOrder(suggestedOrder.getAndIncrement()));
        }

        return initializerSet;
    }
}
