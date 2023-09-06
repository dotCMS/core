package com.dotcms.config;

import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RuntimeConfigWriter;
import com.google.common.annotations.VisibleForTesting;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.StreamSupport;
/**
 * This class encapsulates the Configuration by demand of the dotcms config
 * @author jsanca
 */
@ApplicationScoped // todo create an interface
public class Configuration {

    @Inject
    private Config config = null;
    @Inject
    private RuntimeConfigWriter memoryConfigWriter;

    public static final String DYNAMIC_CONTENT_PATH = "DYNAMIC_CONTENT_PATH";
    public static final String DEFAULT_DYNAMIC_CONTENT_PATH = "dotsecure";
    public static final String ASSET_REAL_PATH = "ASSET_REAL_PATH";

    //Object Config properties
    public static javax.servlet.ServletContext CONTEXT = null;
    public static String CONTEXT_PATH = null;

    public Configuration() {
    }

    /**
     * Config internal methods
     */
    public void initializeConfig () {
        // todo: not sure if this is necessary
        config = ConfigProvider.getConfig();
    }


    /**
     * Returns a string property
     *
     * @param name     The name of the property to locate.
     * @param defValue Value to return if property is not found.
     * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
     * element).
     */
    public String getStringProperty(final String name, final String defValue) {

        final String[] propsArr = getStringArrayProperty(name,
                defValue == null ? null : new String[]{defValue});

        if (propsArr == null || propsArr.length == 0) {
            return defValue;
        }

        return String.join(",", propsArr);

    }

    public  <V> V getProperty(final String name, final V defValue, Class<V> clazz) {
        return config.getOptionalValue(name, clazz).orElse(defValue);
    }

    /**
     * Returns return Path stored in a property, if the property is not found, it will return the default value
     * Optionally try to create if the folder does not exist
     *
     * @param name     The name of the property to locate.
     * @param defValue String value of the path to use if property is not found.
     * @return The value of the property.  If property is found more than once, all the occurrences will be concatenated (with a comma separating each
     * element).
     */
    public Path getPathProperty(final String name, final String defValue, boolean create) {

        String stringVal = getStringProperty(name, defValue);
        Path path = Paths.get(stringVal);
        if (create && !Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new DotRuntimeException("Unable to create directory for property: " +name +" with path "+ path,e);
            }
        }
        return path;
    }

    // todo: should move to another part of the code
    public Path getDynamicContentPath() {
        return getPathProperty(DYNAMIC_CONTENT_PATH, DEFAULT_DYNAMIC_CONTENT_PATH, true);
    }

    public  Path getVelocityRoot() {
        Path baseRoot = getPathProperty("VELOCITY_ROOT", "${CONTEXT_ROOT}/WEB-INF/velocity", false);
        return baseRoot;
    }

    public  Path getAssetRealPath() {
        return Paths.get(ConfigUtils.getAbsoluteAssetsRootPath());
    }
    //////

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
    public  String getStringProperty(final String name, final String defValue, boolean thing) {

        return config.getOptionalValue(name, String.class).orElse(null);

    }

    public  String getStringProperty (final String name) {
        return config.getOptionalValue(name, String.class).orElse(nullDefault(name));
    }

    public  String getProperty(final String name) {
        return config.getOptionalValue(name, String.class).orElse(nullDefault(name));
    }

    private  String nullDefault(final String name) {
        if (config.getOptionalValue("throwExceptionOnNullConfigValue", Boolean.class).orElse(false)) {
            throw new IllegalArgumentException("Property is required");
        } else if (config.getOptionalValue("warnOnNullConfigValue", Boolean.class).orElse(false)) {
            Logger.warn(Config.class, "Use Optional to set a default value if Config value is not required: " + name);
            return null;
        }
        return null;
    }

    public  <V> V getProperty(final String name, final Class<V> clazz) {
        return config.getOptionalValue(name, clazz).orElseThrow(() -> new IllegalArgumentException("Property " + name + " is required"));
    }

    /**
     *
     * @param name
     * @return
     */
    public  String[] getStringArrayProperty (final String name) {
        return config.getOptionalValue(name, String[].class).orElse(null);
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
    public  <T> T[] getCustomArrayProperty(final String name,
                                                 final StringToEntityTransformer<T> stringToEntityTransformer,
                                                 final Class<T> clazz,
                                                 final Supplier<T[]> defaultSupplier) {

        return config.getOptionalValue(name, String[].class).map( v-> convert(v,clazz,stringToEntityTransformer)).orElseGet(
                defaultSupplier::get);
    }

    private  <T> T[] convert(final String[] values, final Class<T> clazz,
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
    public String[] getStringArrayProperty(final String name, final String[] defaultValue) {
        return config.getOptionalValue(name, String[].class).orElse(defaultValue);
    }
    /**
     * @deprecated  Use getIntProperty(String name, int default) and
     * set an intelligent default
     */
    @Deprecated
    public  int getIntProperty (final String name) {
        return config.getOptionalValue(name, Integer.class).orElse(0);
    }

    public  long getLongProperty (final String name, final long defaultVal) {
        return config.getOptionalValue(name, Long.class).orElse(defaultVal);
    }

    /**
     *
     * @param name
     * @param defaultVal
     * @return
     */
    public  int getIntProperty (final String name, final int defaultVal) {
        return config.getOptionalValue(name, Integer.class).orElse(defaultVal);
    }

    /**
     * @deprecated  Use getFloatProperty(String name, float default) and
     * set an intelligent default
     */
    @Deprecated
    public  float getFloatProperty (final String name) {
        return config.getOptionalValue(name, float.class).orElse(0f);
    }

    /**
     *
     * @param name
     * @param defaultVal
     * @return
     */
    public  float getFloatProperty (final String name, final float defaultVal) {
        return config.getOptionalValue(name, Float.class).orElse(defaultVal);
    }

    /**
     * @deprecated  Use getBooleanProperty(String name, boolean default) and
     * set an intelligent default
     */
    @Deprecated
    public  boolean getBooleanProperty (String name) {
        return config.getOptionalValue(name, Boolean.class).orElse(false);
    }

    /**
     *
     * @param name
     * @param defaultVal
     * @return
     */
    public  boolean getBooleanProperty (String name, boolean defaultVal) {
        return config.getOptionalValue(name, Boolean.class).orElse(defaultVal);
    }

    /**
     *
     * @param key
     * @param value
     */
    public  void setProperty(String key, Object value) {
        memoryConfigWriter.setOverride(key, Objects.toString(value,null));
    }


    public  void removeProperty(String key) {
        memoryConfigWriter.removeOverride(key);
    }


    /**
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public  Iterator<String> getKeys () {
        return config.getPropertyNames().iterator();
    }

    /**
     *
     * @param prefix
     * @return
     */
    @SuppressWarnings ( "unchecked" )
    public  Iterator<String> subset (final String prefix ) {
        return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(key -> key.startsWith(prefix+"."))
                .map(key -> key.substring(prefix.length()+1))
                .iterator();
    }

    public  RuntimeConfigWriter getConfigWriter(){
        return memoryConfigWriter;
    }

}
