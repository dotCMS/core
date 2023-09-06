package com.dotmarketing.util;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.config.Configuration;
import com.dotcms.config.PathConfiguration;
import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.FileWatcherAPI;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides access to the system configuration parameters that are set through the
 * {@code dotmarketing-config.properties}, and the {@code dotcms-config-cluster.properties} files.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 */

public class Config {

    //Generated File Indicator
    public static final String GENERATED_FILE = "dotGenerated_";
    public static final String RENDITION_FILE = "dotRendition_";

    public static int DB_VERSION = 0;
    public static int DATA_VERSION = 0;

    //Object Config properties      n
    public static javax.servlet.ServletContext CONTEXT = null;
    public static String CONTEXT_PATH = null;

    private static Configuration configuration = null;
    private static PathConfiguration pathConfiguration = null;

    /**
     * Config internal methods
     */
    public static void initializeConfig() {

        configuration     = CDIUtils.getBean(Configuration.class);
        pathConfiguration = CDIUtils.getBean(PathConfiguration.class);
    }

    /**
     * Returns a string property
     *
     * @param name     The name of the property to locate.
     * @param defValue Value to return if property is not found.
     * @return The value of the property.  If property is found more than once, all the occurrences
     * will be concatenated (with a comma separating each element).
     */
    public static String getStringProperty(final String name, final String defValue) {

        return configuration.getStringProperty(name, defValue);
    }

    /**
     * this is only here so the old tests pass
     *
     * @param name
     * @param defValue
     * @param thing
     * @return
     */
    @VisibleForTesting
    @Deprecated
    public static String getStringProperty(final String name, final String defValue,
            boolean thing) {

        return getStringProperty(name, defValue);
    }


    /**
     * @deprecated Use getStringProperty(String name, String default) and set an intelligent default
     */
    @Deprecated
    public static String getStringProperty(final String name) {
        return getStringProperty(name, null);
    }

    /**
     * @param name
     * @return
     */
    public static String[] getStringArrayProperty(final String name) {

        return getStringArrayProperty(name, null);
    }

    /**
     * Transform an array into an array of entity, needs a transformer to convert the string from
     * the config to object and the class. In addition if the name does not exists, the supplier
     * will be invoke
     *
     * @param name                      {@link String} name of the array property
     * @param stringToEntityTransformer {@link StringToEntityTransformer} transformer to string to
     *                                  T
     * @param clazz                     {@link Class}
     * @param defaultSupplier           {@link Supplier}
     * @param <T>
     * @return Array of T
     */
    public static <T> T[] getCustomArrayProperty(final String name,
            final StringToEntityTransformer<T> stringToEntityTransformer,
            final Class<T> clazz,
            final Supplier<T[]> defaultSupplier) {

        return configuration.getCustomArrayProperty(name, stringToEntityTransformer, clazz,
                defaultSupplier);
    }

    private static <T> T[] convert(final String[] values, final Class<T> clazz,
            final StringToEntityTransformer<T> stringToEntityTransformer) {

        final T[] entities = (T[]) Array.newInstance(clazz, values.length);

        for (int i = 0; i < values.length; ++i) {

            entities[i] = stringToEntityTransformer.from(values[i]);
        }

        return entities;
    }

    /**
     * If config value == null, returns the default
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public static String[] getStringArrayProperty(final String name, final String[] defaultValue) {

        return configuration.getStringArrayProperty(name, defaultValue);
    }

    /**
     * @deprecated Use getIntProperty(String name, int default) and set an intelligent default
     */
    @Deprecated
    public static int getIntProperty(final String name) {

        return configuration.getIntProperty(name);
    }

    public static long getLongProperty(final String name, final long defaultVal) {

        return configuration.getLongProperty(name, defaultVal);
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static int getIntProperty(final String name, final int defaultVal) {

        return configuration.getIntProperty(name, defaultVal);
    }

    /**
     * @deprecated Use getFloatProperty(String name, float default) and set an intelligent default
     */
    @Deprecated
    public static float getFloatProperty(final String name) {

        return configuration.getFloatProperty(name);
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static float getFloatProperty(final String name, final float defaultVal) {

        return configuration.getFloatProperty(name, defaultVal);
    }



    /**
     * @deprecated Use getBooleanProperty(String name, boolean default) and set an intelligent
     * default
     */
    @Deprecated
    public static boolean getBooleanProperty(final String name) {

        return configuration.getBooleanProperty(name);
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static boolean getBooleanProperty(final String name, final boolean defaultVal) {

        return configuration.getBooleanProperty(name, defaultVal);
    }

    /**
     * @param key
     * @param value
     */
    public static void setProperty(final String key, final Object value) {

        configuration.setProperty(key, value);
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<String> getKeys() {
        return configuration.getKeys();
    }

    /**
     * @param prefix
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<String> subset(String prefix) {
        return configuration.subset(prefix);
    }


    /**
     * Spindle Config
     *
     * @param myApp
     */
    public static void setMyApp(final javax.servlet.ServletContext myApp) {
        CONTEXT = myApp;
        CONTEXT_PATH = myApp.getRealPath("/");
    }

}
