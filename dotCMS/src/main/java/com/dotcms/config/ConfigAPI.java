package com.dotcms.config;

import com.dotcms.util.transform.StringToEntityTransformer;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Encapsulates all the configuration for dotcms
 * @author jsanca
 */
public interface ConfigAPI {

    /**
     * Gets a boolean property
     * @param key         {@link String} key of the property
     * @param defaultVal  {@link Boolean} default value
     * @return return the configuration value for name otherwise defaultVal
     */
    boolean getBooleanProperty (String key, boolean defaultVal);

    /**
     * Gets a boolean property
     * @param key        {@link String} key of the property
     * @param defaultVal {@link String} {@link Supplier} to retrieve the default value
     * @return the configuration value for name otherwise defaultVal
     */
    boolean getBooleanProperty (String key, Supplier<Boolean> defaultVal);

    /**
     *
     * @param key
     * @return
     */
    String[] getStringArrayProperty (String key);

    String[] getStringArrayProperty(final String name, final String[] defaultValue);

    String[] getStringArrayProperty(final String name, Supplier<String[]>  defaultValue);


    /**
     * Transform an array into an array of entity, needs a transformer to convert the string from the config to object
     * and the class.
     * In addition if the name does not exists, the supplier will be invoke
     * @param name {@link String} name of the array property
     * @param stringToEntityTransformer {@link StringToEntityTransformer} transformer to string to T
     * @param clazz {@link Class}
     * @param defaultSupplier {@link Supplier}
     * @param <T>
     * @return Array of T
     */
    <T>  T[] getCustomArrayProperty(final String name,
                                                  final StringToEntityTransformer<T> stringToEntityTransformer,
                                                  final Class<T> clazz,
                                                  final Supplier<T[]> defaultSupplier);

    long getLongProperty (final String name, final long defaultVal);

    long getLongProperty (final String name, final Supplier<Long> defaultVal);

    int getIntProperty (final String name, final int defaultVal);

    int getIntProperty (final String name, final Supplier<Integer> defaultVal);

    float getFloatProperty (final String name, final float defaultVal);

    float getFloatProperty (final String name, final Supplier<Float> defaultVal);

    /**
     * Returns all Set of the keys
     * @return Set of keys
     */
    Set<String> keys ();

    /**
     * Returns subset of keys based on the prefix
     *
     * @param prefix {@link String}
     * @return subset of prefix keys
     */
    Set<String> keysSubset (String prefix);

    /**
     * Returns subset of keys based on the {@link Predicate} filter
     * @param filterKey {@link Predicate} predicate to filter the key
     * @return filtered subset key
     */
    Set<String> keysSubset (Predicate<String> filterKey);

    /**
     * Sets a property
     * @param key    {@link String}
     * @param value
     */
    void setProperty(String key, Object value);

    void setProperties (Map<String, Object> values);

    ServletContext getContext();

    String getContextPath();

}
