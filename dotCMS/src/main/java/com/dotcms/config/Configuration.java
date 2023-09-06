package com.dotcms.config;

import com.dotcms.repackage.com.google.common.base.Supplier;
import com.dotcms.util.transform.StringToEntityTransformer;
import com.google.common.annotations.VisibleForTesting;

import java.util.Iterator;

/**
 * Encapsulates the dotCMS configuration.
 * @author jsanca
 */
public interface Configuration {

    String getStringProperty(final String name, final String defValue);

    <V> V getProperty(final String name, final V defValue, Class<V> clazz);

    @VisibleForTesting
    @Deprecated
     String getStringProperty(final String name, final String defValue, boolean thing);

    String getStringProperty (final String name);

    String getProperty(final String name);

    <V> V getProperty(final String name, final Class<V> clazz);

    String[] getStringArrayProperty (final String name);

    <T> T[] getCustomArrayProperty(final String name,
                                   final StringToEntityTransformer<T> stringToEntityTransformer,
                                   final Class<T> clazz,
                                   final Supplier<T[]> defaultSupplier);

    String[] getStringArrayProperty(final String name, final String[] defaultValue);

    @Deprecated
    int getIntProperty (final String name);

    long getLongProperty (final String name, final long defaultVal);

    int getIntProperty (final String name, final int defaultVal);

    @Deprecated
    float getFloatProperty (final String name);

    float getFloatProperty (final String name, final float defaultVal);

    @Deprecated
    boolean getBooleanProperty (String name);

    boolean getBooleanProperty (String name, boolean defaultVal);

    void setProperty(String key, Object value);

    void removeProperty(String key);

    Iterator<String> getKeys ();

    Iterator<String> subset (final String prefix );

}
