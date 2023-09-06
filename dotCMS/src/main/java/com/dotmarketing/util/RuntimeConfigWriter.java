package com.dotmarketing.util;

import com.dotcms.config.SystemTableConfigSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.eclipse.microprofile.config.Config;

@Dependent
public class RuntimeConfigWriter {

    @Inject
    Config config;

    @Inject
    SystemTableConfigSource systemTableConfigSource;


    public void setOverride(final String key, final String value) {

        if (value==null) {
            systemTableConfigSource.remove(key);
        } else {
            systemTableConfigSource.put(key, value);
        }
    }

    public void removeOverride(final String key) {

        systemTableConfigSource.remove(key);
    }

    public Map<String,String> getAllOverrides() {

            return systemTableConfigSource.getProperties();
    }

    public void logOverrides() {

            systemTableConfigSource.getProperties().forEach((k, v) ->
                    Logger.info(this.getClass(), String.format("Config Override %s=%s -> %s",k,config.getOptionalValue(k,String.class).orElse("[not-set]"),v)));
    }

    public void setPropertiesAndRestore(final Map<String,String> propsToSet, final Runnable runnable) {

        final Map<String, String> propsToRestore = saveSetProps(propsToSet);

        try {
            runnable.run();
        } finally {
            restoreProps(propsToSet, propsToRestore);
        }

    }

    public  <U> U setPropertyAndRestore(final Map<String,String> propsToSet, final java.util.function.Supplier<U> supplier) {

        U retValue;
        Map<String, String> propsToRestore = saveSetProps(propsToSet);

        try {
            retValue = supplier.get();
        } finally {
            restoreProps(propsToSet,propsToRestore);
        }
        return retValue;
    }


    private Map<String, String> saveSetProps(final Map<String, String> propsToSet) {

        final Map<String,String> propsToRestore = new HashMap<>();
        for(final Entry<String,String> entry : propsToSet.entrySet()) {

            final String existing = this.getOverride(entry.getKey());
            if (existing!=null) {

                propsToRestore.put(entry.getKey(), existing);
            }

            this.setOverride(entry.getKey(), entry.getValue());
        }
        return propsToRestore;
    }

    private String getOverride(final String key) {
            return systemTableConfigSource.getValue(key);
    }

    private void restoreProps(final Map<String, String> propsToSet,
                              final Map<String, String> propsToRestore) {

        for(final Entry<String,String> entry : propsToSet.entrySet()) {

            final String restoreVal = propsToRestore.get(entry.getKey());

            if (restoreVal!=null) {
                setOverride(entry.getKey(), restoreVal);
            } else {
                removeOverride(entry.getKey());
            }
        }
    }
    public void clearOverrides() {
        systemTableConfigSource.clear();
    }

    public void setPropertyAndRestore(final String key, final String value, final Runnable runnable) {

        setOverride(key, value);
        try {

            runnable.run();
        } finally {
            removeOverride(key);
        }
    }

    public  <U> U setPropertyAndRestore(final String key,
                                        final String value,
                                        final java.util.function.Supplier<U> supplier) {

        U result = null;

        setOverride(key, value);
        try {

            result = supplier.get();
        } finally {

            removeOverride(key);
        }

        return result;
    }

}
