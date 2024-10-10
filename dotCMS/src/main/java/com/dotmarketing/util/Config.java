package com.dotmarketing.util;

import com.dotcms.config.SystemTableConfigSource;
import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.ConfigurationInterpolator;
import com.dotcms.util.FileWatcherAPI;
import com.dotcms.util.ReflectionUtils;
import com.dotcms.util.SystemEnvironmentConfigurationInterpolator;
import com.dotcms.util.ThreadContextUtil;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.dotmarketing.business.APILocator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class provides access to the system configuration parameters that are set through the
 * {@code dotmarketing-config.properties}, and the {@code dotcms-config-cluster.properties} files.
 *
 * The order of the properties resolution is:
 * 1) any property set by environmet variable
 * 2) any property set by the system table (could be on runtime)
 * 3) any property set by the property files
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 */

public class Config {

    //Generated File Indicator
    public static final String GENERATED_FILE = "dotGenerated_";
    public static final AtomicBoolean useWatcherMode = new AtomicBoolean(false);
    public static final AtomicBoolean isWatching = new AtomicBoolean(false);

    public static final Map<String, String> testOverrideTracker = new ConcurrentHashMap<>();

    private static final Set<String> environmentSetKeys = ConcurrentHashMap.newKeySet();

    private static SystemTableConfigSource systemTableConfigSource = null;

    @VisibleForTesting
    public static boolean enableSystemTableConfigSource = "true".equalsIgnoreCase(EnvironmentVariablesService.getInstance().getenv().getOrDefault("DOT_ENABLE_SYSTEM_TABLE_CONFIG_SOURCE", "true"));

    public static boolean isSystemTableConfigSourceInit() {
        return null != systemTableConfigSource;
    }

    public static void initSystemTableConfigSource() {
        systemTableConfigSource = new SystemTableConfigSource();
    }


    /**
     * If this property is set in the dotmarketing-config, will try to use the interpolator for the
     * properties Otherwise will use {@link SystemEnvironmentConfigurationInterpolator}
     */
    public static final String DOTCMS_CUSTOM_INTERPOLATOR = "dotcms.custom.interpolator";

    /**
     * If this property is set, defines the way you want to use to monitoring the changes over the
     * dotmarketing-config.properties file. By default is true and means that the
     * {@link FileWatcherAPI} will be used to monitoring any change over the file and refresh the
     * property based on it. If you set it to false, will use the legacy mode previously used on
     * dotCMS.
     */
    public static final String DOTCMS_USEWATCHERMODE = "dotcms.usewatchermode";
    public static final String USE_CONFIG_TEST_OVERRIDE_TRACKER = "USE_CONFIG_TEST_OVERRIDE_TRACKER";
    public static int DB_VERSION = 0;
    public static int DATA_VERSION = 0;

    //Object Config properties      n
    public static javax.servlet.ServletContext CONTEXT = null;


    //Config internal properties
    protected final static MapConfiguration props = new MapConfiguration(new ConcurrentHashMap<>());
    private static ClassLoader classLoader = null;
    protected static URL dotmarketingPropertiesUrl = null;
    protected static URL clusterPropertiesUrl = null;
    private static int prevInterval = Integer.MIN_VALUE;
    private static FileWatcherAPI fileWatcherAPI = null;


    /**
     * Config internal methods
     */
    public static void initializeConfig() {
        classLoader = Thread.currentThread().getContextClassLoader();
        _loadProperties();
    }

    private static void registerWatcher(final File fileToRead) {

        initWatcherAPI();
        if (null != fileWatcherAPI) {

            // if we are not already watching, so register the waticher
            if (!isWatching.get()) {
                try {

                    Logger.debug(APILocator.class, "Start watching: " + fileToRead);
                    fileWatcherAPI.watchFile(fileToRead, () -> _loadProperties());
                    isWatching.set(true);
                } catch (IOException e) {
                    Logger.error(Config.class, e.getMessage(), e);
                }
            }
        } else {
            // if not fileWatcherAPI could not monitoring, so use the fallback
            useWatcherMode.set(false);
            isWatching.set(false);
        }
    } // registerWatcher.

    private static void initWatcherAPI() {

        // checki if the watcher is already instantiated.
        if (null == fileWatcherAPI) {
            synchronized (Config.class) {

                if (null == fileWatcherAPI) {

                    fileWatcherAPI = APILocator.getFileWatcherAPI();
                }
            }
        }
    }

    private static void unregisterWatcher(final File fileToRead) {

        initWatcherAPI();
        if (null != fileWatcherAPI) {

            Logger.debug(APILocator.class, "Stop watching: " + fileToRead);
            fileWatcherAPI.stopWatchingFile(fileToRead);
        }
    }

    /**
     *
     */
    private static void _loadProperties() {

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            Logger.info(Config.class, "Initializing properties reader.");
        }

        //dotmarketing config file
        String propertyFile = "dotmarketing-config.properties";
        if (dotmarketingPropertiesUrl == null) {
            dotmarketingPropertiesUrl = classLoader.getResource(propertyFile);
        }

        //cluster config file
        propertyFile = "dotcms-config-cluster.properties";
        if (clusterPropertiesUrl == null) {
            clusterPropertiesUrl = classLoader.getResource(propertyFile);
        }

        //Reading both property files
        readProperties(dotmarketingPropertiesUrl, clusterPropertiesUrl);

        // Include ENV variables that start with DOT_

        readEnvironmentVariables();

        reapplyTestOverrides();
    }

    private static void reapplyTestOverrides() {

        if (props.getBoolean(USE_CONFIG_TEST_OVERRIDE_TRACKER, false)) {
            testOverrideTracker.forEach((key, value) -> {
                String currentValue = Try.of(()->props.getString(key)).getOrNull();
                if (value.equals("[remove]"))
                {
                    if (currentValue != null)
                        props.clearProperty(key);
                    else {
                        testOverrideTracker.remove(key);
                    }
                } else if (currentValue == null || !currentValue.equals(value)) {
                    props.setProperty(key, value);
                } else {
                    testOverrideTracker.remove(key);
                }
            });
        }
    }

    /**
     * Reads the properties on the dotmarketing-config.properties and the
     * dotcms-config-cluster.properties properties files.
     *
     * @param dotmarketingURL
     * @param clusterURL
     */
    private static void readProperties(URL dotmarketingURL, URL clusterURL) {
        File dotmarketingFile = new File(dotmarketingURL.getPath());
        File clusterFile = new File(clusterURL.getPath());

        if (props.isEmpty()) {
            synchronized (Config.class) {
                if (props.isEmpty()) {
                    readProperties(dotmarketingFile,
                            "dotmarketing-config.properties");
                    readProperties(clusterFile,
                            "dotcms-config-cluster.properties");
                }
            }
        }
    }

    /**
     * Reads a given property file and appends its content to the current read properties
     *
     * @param fileToRead
     * @param fileName
     */
    private static void readProperties(File fileToRead, String fileName) {

        InputStream propsInputStream = null;

        try {

            Logger.info(Config.class, "Loading dotCMS [" + fileName + "] Properties...");


            propsInputStream = Files.newInputStream(fileToRead.toPath());
            //Using a mapConfiguration to avoid apache reload.
            final PropertiesConfiguration pconfig = new PropertiesConfiguration();
            pconfig.load(new InputStreamReader(propsInputStream));
            pconfig.getKeys().forEachRemaining(k->{
                props.addProperty(k, pconfig.getProperty(k));
            });
            Logger.info(Config.class, "dotCMS Properties [" + fileName + "] Loaded");
            postProperties();
            // check if the configuration for the watcher has changed.
            useWatcherMode.set(getBooleanProperty(DOTCMS_USEWATCHERMODE, false));
            if (useWatcherMode.get()) {

                registerWatcher(fileToRead);
            } else if (isWatching.get()) {
                unregisterWatcher(fileToRead);
                isWatching.set(false);
            }
        } catch (Exception e) {
            Logger.fatal(Config.class, "Exception loading properties for file [" + fileName + "]",
                    e);
            props.clear();
        } finally {

            IOUtils.closeQuietly(propsInputStream);
        }
    }


    /**
     * Does the post process properties based on the interpolator
     */
    protected static void postProperties() {

        final String customConfigurationInterpolator = getStringProperty(DOTCMS_CUSTOM_INTERPOLATOR,
                null);
        final ConfigurationInterpolator customInterpolator =
                UtilMethods.isSet(customConfigurationInterpolator) ?
                        (ConfigurationInterpolator) ReflectionUtils.newInstance(
                                customConfigurationInterpolator) : null;
        final ConfigurationInterpolator interpolator = (null != customInterpolator) ?
                customInterpolator : SystemEnvironmentConfigurationInterpolator.INSTANCE;
        final Configuration configuration = interpolator.interpolate(props);

        if(configuration instanceof PropertiesConfiguration){
            props.copy(configuration);
        }

    }

    /**
     * Force the refresh properties, only for testing
     */
    @VisibleForTesting
    protected static void refreshProperties() {

       _loadProperties();
    }

    /**
     *
     */
    private static void _refreshProperties() {

        if (props.isEmpty()) {
            _loadProperties();
        }
    }


    private final static String ENV_PREFIX = "DOT_";

    private static void readEnvironmentVariables() {
        synchronized (Config.class) {
            EnvironmentVariablesService.getInstance().getenv().entrySet().stream().filter(e -> e.getKey().startsWith(ENV_PREFIX))
                    .forEach(e -> {
                        environmentSetKeys.add(e.getKey());
                        props.setProperty(e.getKey(), e.getValue());
                    });
        }
    }


    public static String envKey(final String theKey) {

        if(!theKey.startsWith(ENV_PREFIX)) {
            String envKey = ENV_PREFIX + theKey.toUpperCase().replace(".", "_").replace("-","_");
            while (envKey.contains("__")) {
                envKey = envKey.replace("__", "_");
            }
            return envKey.endsWith("_") ? envKey.substring(0, envKey.length() - 1) : envKey;
        }
        return theKey;

    }

    /**
     * Returns a list of properties that contains the given String.
     * Also gives priority to the System Env over the ones in the properties file.
     * @param containsString
     * @return list of properties
     */
    public static List<String> subsetContainsAsList(final String containsString){
        final List<String> fullListProps = new ArrayList<String>();
        props.getKeys().forEachRemaining(fullListProps::add);
        if(null != systemTableConfigSource && enableSystemTableConfigSource){
            systemTableConfigSource.getPropertyNames().forEach(fullListProps::add);
        }

        //List with all system env props that contains the pattern
        final String envContainsString = envKey(containsString);
        final List<String> propList = fullListProps.stream().filter(prop -> prop.contains(envContainsString)).collect(Collectors.toList());

        //List with all props with . (dotmarketing.properties) that contains the pattern
        final List<String> configList = fullListProps.stream().filter(prop -> prop.contains(containsString)).collect(Collectors.toList());

        //Final list union of the env list + configList which aren't set by envList
        for(final String prop : configList){
            final String keyRefactor = envKey(prop);
            if(!propList.contains(keyRefactor)){
                propList.add(prop);
            }
        }

        return propList;
    }

    private static String getSystemTableValue(final String ...names) {

        if (null != names && null != systemTableConfigSource && enableSystemTableConfigSource) {

            final String tag = ThreadContextUtil.getOrCreateContext().getTag();
            if (UtilMethods.isSet(tag) && "ConfigSystemTable".equals(tag)) {
                // we are already in the system table, so do not need to check inner system table calls (avoid recursion)
                return null;
            }

            try {

                ThreadContextUtil.getOrCreateContext().setTag("ConfigSystemTable");

                for (final String name : names) {
                    final String value = Try.of(() -> systemTableConfigSource.getValue(name)).getOrNull();
                    if (null != value) {
                        return value;
                    }
                }

            } finally {
                // the result is done, do not need more the barrier tag
                ThreadContextUtil.getOrCreateContext().setTag(null);
            }
        }

        return null;
    }

    /**
     * Given a property name, evaluates if it belongs to the environment variables.
     *
     * @param key property name
     * @return true if key is found when looked by its environment variable name equivalent and if
     * it has properties associated to, otherwise false.
     */
    public static boolean isKeyEnvBased(final String key) {
        final String envKey = envKey(key);

        if (!props.containsKey(envKey)) {
            return false;
        }

        final String[] properties = props.getStringArray(envKey);
        return properties != null && properties.length > 0;
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

        final String[] propsArr = getStringArrayProperty(name,
                defValue == null ? null : new String[]{defValue});

        if (propsArr == null || propsArr.length == 0) {
            return defValue;
        }

        return String.join(",", propsArr);

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
    public static String getStringProperty(String name) {
        return getStringProperty(name, null);
    }

    /**
     * @param name
     * @return
     */
    public static String[] getStringArrayProperty(String name) {
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

        final String[] values = getStringArrayProperty(name);
        return props.containsKey(name) ? convert(values, clazz, stringToEntityTransformer)
                : defaultSupplier.get();
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

        final String envKey = envKey(name);

        final Supplier<String[]> propsSupplier = () -> {

            _refreshProperties();
            return props.containsKey(envKey)
                    ? props.getStringArray(envKey)
                    : props.containsKey(name)
                    ? props.getStringArray(name)
                    : defaultValue;
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return valueString.split(StringPool.COMMA);
        }

        return propsSupplier.get();
    }

    /**
     * @deprecated Use getIntProperty(String name, int default) and set an intelligent default
     */
    @Deprecated
    public static int getIntProperty(final String name) {

        final String envKey = envKey(name);

        final Supplier<Integer> propsSupplier = () -> {

            _refreshProperties();

            Integer value = Try.of(() -> props.getInt(envKey)).getOrNull();
            if (value != null) {
                return value;
            }

            return props.getInt(name);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Integer.parseInt(valueString);
        }

        return propsSupplier.get();
    }

    public static long getLongProperty(final String name, final long defaultVal) {

        final String envKey = envKey(name);

        final Supplier<Long> propsSupplier = () -> {

            _refreshProperties();

            Long value = Try.of(() -> props.getLong(envKey)).getOrNull();
            if (value != null) {
                return value;
            }
            return props.getLong(name, defaultVal);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Long.parseLong(valueString);
        }

        return propsSupplier.get();
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static int getIntProperty(final String name, final int defaultVal) {

        final String envKey = envKey(name);

        final Supplier<Integer> propsSupplier = () -> {

            _refreshProperties();

            Integer value = Try.of(() -> props.getInt(envKey(name))).getOrNull();
            if (value != null) {
                return value;
            }

            return props.getInt(name, defaultVal);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Integer.parseInt(valueString);
        }

        return propsSupplier.get();
    }

    /**
     * @deprecated Use getFloatProperty(String name, float default) and set an intelligent default
     */
    @Deprecated
    public static float getFloatProperty(final String name) {

        final String envKey = envKey(name);

        final Supplier<Float> propsSupplier = () -> {

            _refreshProperties();

            Float value = Try.of(() -> props.getFloat(envKey)).getOrNull();
            if (value != null) {
                return value;
            }

            return props.getFloat(name);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Float.parseFloat(valueString);
        }

        return propsSupplier.get();
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static float getFloatProperty(final String name, final float defaultVal) {

        final String envKey = envKey(name);

        final Supplier<Float> propsSupplier = () -> {

            _refreshProperties();

            Float value = Try.of(() -> props.getFloat(envKey)).getOrNull();
            if (value != null) {
                return value;
            }
            return props.getFloat(name, defaultVal);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Float.parseFloat(valueString);
        }

        return propsSupplier.get();
    }



    /**
     * @deprecated Use getBooleanProperty(String name, boolean default) and set an intelligent
     * default
     */
    @Deprecated
    public static boolean getBooleanProperty(final String name) {

        final String envKey = envKey(name);

        final Supplier<Boolean> propsSupplier = () -> {

            _refreshProperties();

            Boolean value = Try.of(() -> props.getBoolean(envKey)).getOrNull();
            if (value != null) {
                return value;
            }
            return props.getBoolean(name);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Boolean.parseBoolean(valueString);
        }

        return propsSupplier.get();
    }

    /**
     * @param name
     * @param defaultVal
     * @return
     */
    public static boolean getBooleanProperty(String name, boolean defaultVal) {

        final String envKey = envKey(name);

        final Supplier<Boolean> propsSupplier = () -> {

            _refreshProperties();
            
            final Boolean value =
                    props.containsKey(envKey) ? Try.of(() -> props.getBoolean(envKey))
                            .getOrNull() : null;
            if (null != value) {
                return value;
            }
            return props.getBoolean(name, defaultVal);
        };

        if (environmentSetKeys.contains(envKey) || environmentSetKeys.contains(name)) {
            return propsSupplier.get();
        }

        final String valueString = getSystemTableValue(envKey, name);
        if (null != valueString) {
            return Boolean.parseBoolean(valueString);
        }

        return propsSupplier.get();
    }

    /**
     * @param key
     * @param value
     */
    public static void setProperty(String key, Object value) {
        if (props != null) {
            if(props.containsKey(envKey(key))) {
                key = envKey(key);
            }
            trackOverrides(key, value);
            Logger.info(Config.class, "Setting property: " + key + " to " + value);
            props.setProperty(key, value);
        }
    }

    private static void trackOverrides(String key, Object value) {
        if (props.getBoolean(USE_CONFIG_TEST_OVERRIDE_TRACKER, false)) {
            if (value == null) {
                testOverrideTracker.put(key,"[remove]");
            } else {
                testOverrideTracker.put(key, value.toString());
            }
        }
    }

    public static Map<String, String> getOverrides() {
        return Map.copyOf(testOverrideTracker);
    }

    public static Map<String, String> compareOverrides(Map<String, String> before) {
        Map<String, String> after = getOverrides();
        Map<String, String> diff = new HashMap<>();
        for (String key : after.keySet()) {
            if (!before.containsKey(key) || !before.get(key).equals(after.get(key))) {
                diff.put(key, after.get(key));
            }
        }
        return diff;
    }


    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<String> getKeys() {
        _refreshProperties();
        // note: I do not think we need the system table keys here by now
        return ImmutableSet.copyOf(props.getKeys()).iterator();
    }

    /**
     * @param prefix
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<String> subset(String prefix) {
        _refreshProperties();
        // note: I do not think we need the system table keys here by now
        return ImmutableSet.copyOf(props.subset(prefix).getKeys()).iterator();
    }


    /**
     * Spindle Config
     *
     * @param myApp
     */
    public static void setMyApp(javax.servlet.ServletContext myApp) {
        CONTEXT = myApp;
    }

}
