package com.dotcms.util;

import com.dotcms.repackage.org.apache.commons.configuration.Configuration;
import com.dotcms.repackage.org.apache.commons.configuration.PropertiesConfiguration;
import com.dotmarketing.util.StringUtils;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;

/**
 * If you required to interpolate System properties or Environment variables you can use this implementations.
 * It will return a PropertiesConfiguration, if you need a diff you can override newConfiguration method
 *
 * <table>
 *     <tr>
 *         <th>Prefix</th>
 *         <th>Description</th>
 *     </tr>
 *     <tr>
 *         <td><i>sys</i></td>
 *         <td>This prefix says the interpolation will be done from a system property.
 *         For instance: <code>home={sys:user.home}</code> will be replace by the respective user.home value in the system
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><i>env</i></td>
 *         <td>This prefix says the interpolation will be done from an Operative System environment property.
 *         For instance: <code>home={env:JAVA_HOME}</code> will be replace by the respective java home value in the environment properties
 *         </td>
 *     </tr>
 * </table>
 *
 * @author jsanca
 */
public class SystemEnvironmentConfigurationInterpolator implements ConfigurationInterpolator {

    public static final String ENV_PREFIX = "env:";
    public static final String SYS_PREFIX = "sys:";
    public final static SystemEnvironmentMapDecorator SYSTEM_ENV_LAZY_MAP = new SystemEnvironmentMapDecorator();
    public static final SystemEnvironmentConfigurationInterpolator INSTANCE = new SystemEnvironmentConfigurationInterpolator();

    protected Configuration newConfiguration () {
        return new PropertiesConfiguration();
    }

    @Override
    public Configuration interpolate(final Configuration originalConfiguration) {

        final Iterator      iterator         =  originalConfiguration.getKeys();
        final Configuration newConfiguration = (null != iterator)?
                this.newConfiguration():originalConfiguration;


        Object key   = null;
        Object value = null;

        if (null != iterator) {

            while (iterator.hasNext()) {

                key = iterator.next();
                if (null != key) {

                    value = originalConfiguration.getProperty(key.toString());
                    newConfiguration.addProperty(key.toString(), this.interpolate(value));
                }
            }
        }

        return newConfiguration;
    }

    private Object interpolate(final Object value) {

        return value instanceof String ? this.interpolate(value.toString()) :
                doObjectInterpolation(value);
    } // interpolate.

    private Object doObjectInterpolation(final Object value) {

        if ( value.getClass().isArray() ) {

            return interpolateObjectArray(value);
        }

        // other cases.

        return value;
    } // doObjectInterpolation.

    private Object interpolateObjectArray(final Object array) {

        final int length = Array.getLength(array);
        final Object[] newArray = new Object[length];

        for ( int i = 0; i < length; i++ ) {

            newArray[i] = this.interpolate(Array.get(array, i));
        }

        return newArray;
    } // interpolateObjectArray.

    @Override
    public String interpolate (final String value) {

        return (isInterpolable(value))?
                    StringUtils.interpolate(value, SYSTEM_ENV_LAZY_MAP):value;
    } // interpolate.

    /**
     * Something is interpolable basically if it is a not null string
     * @param value Object
     * @return boolean
     */
    private boolean isInterpolable(final Object value) {

        return (null != value) && (value instanceof String) && String.class.cast(value).contains("{") && String.class.cast(value).contains("}");
    } // isInterpolable.

    /**
     * Just a Decorator of a hashmap to get a lazy resolution of the system and env properties for interpolation
     */
    private static class SystemEnvironmentMapDecorator extends HashMap<String , Object> {

        private final int size = System.getProperties().size() + System.getenv().size();

        @Override
        public int size() {

            return this.size;
        }

        @Override
        public Object get(Object key) {

            Object value = null;

            if (null != key) {

                if (key.toString().startsWith(ENV_PREFIX)) {

                    value = System.getenv(this.envKeyUnWrap(key.toString()));
                } else if (key.toString().startsWith(SYS_PREFIX)) {
                    value = System.getProperty(this.systemKeyUnWrap(key.toString()));
                }
            }

            return value;
        }


        private  String envKeyUnWrap(String key) {
            return com.dotcms.repackage.org.apache.commons.lang.StringUtils.replace(key, ENV_PREFIX, com.dotcms.repackage.org.apache.commons.lang.StringUtils.EMPTY);
        }

        private  String systemKeyUnWrap(String key) {
            return com.dotcms.repackage.org.apache.commons.lang.StringUtils.replace(key, SYS_PREFIX, com.dotcms.repackage.org.apache.commons.lang.StringUtils.EMPTY);
        }
    } // SystemEnvironmentMapDecorator.
} // E:O:F:SystemEnvironmentConfigurationInterpolator.
