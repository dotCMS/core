package com.dotmarketing.util;

import com.dotmarketing.microprofile.config.MemoryConfigSource;
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
    MemoryConfigSource memoryConfigSource;


    public void setOverride(String key, String value) {
            if (value==null)
                memoryConfigSource.remove(key);
            else
                memoryConfigSource.put(key, value);
    }

    public void removeOverride(String key) {
            memoryConfigSource.remove(key);
    }

    public Map<String,String> getAllOverrides() {
            return memoryConfigSource.getProperties();
    }

    public void logOverrides() {
            memoryConfigSource.getProperties().forEach((k,v) -> Logger.info(this.getClass(), String.format("Config Override %s=%s -> %s",k,config.getOptionalValue(k,String.class).orElse("[not-set]"),v)));
    }

    public void setPropertiesAndRestore(Map<String,String> propsToSet, Runnable runnable) {

        Map<String, String> propsToRestore = saveSetProps(propsToSet);
        try {
            runnable.run();
        } finally {
            restoreProps(propsToSet, propsToRestore);
        }

    }

    public  <U> U setPropertyAndRestore(final Map<String,String> propsToSet, java.util.function.Supplier<U> supplier) {
        U retValue;
        Map<String, String> propsToRestore = saveSetProps(propsToSet);
        try {
            retValue = supplier.get();
        } finally {
            restoreProps(propsToSet,propsToRestore);
        }
        return retValue;
    }


    private Map<String, String> saveSetProps(Map<String, String> propsToSet) {
        Map<String,String> propsToRestore = new HashMap<>();
        for(Entry<String,String> entry : propsToSet.entrySet()) {
            String existing = this.getOverride(entry.getKey());
            if (existing!=null) {
                propsToRestore.put(entry.getKey(), existing);
            }
            this.setOverride(entry.getKey(), entry.getValue());
        }
        return propsToRestore;
    }

    private String getOverride(String key) {
            return memoryConfigSource.getValue(key);
    }

    private void restoreProps( Map<String, String> propsToSet, Map<String, String> propsToRestore) {
        for(Entry<String,String> entry : propsToSet.entrySet()) {
            String restoreVal = propsToRestore.get(entry.getKey());
            if (restoreVal!=null) {
                setOverride(entry.getKey(), restoreVal);
            } else {
                removeOverride(entry.getKey());
            }
        }
    }
    public void clearOverrides() {
        memoryConfigSource.clear();
    }

    public void setPropertyAndRestore(String key, String value, Runnable runnable) {
        setOverride(key, value);
        try {
            runnable.run();
        } finally {
            removeOverride(key);
        }
    }

    public  <U> U setPropertyAndRestore(String key, String value, java.util.function.Supplier<U> supplier) {
        U result = null;

        setOverride(key, value);
        try {
            result=supplier.get();
        } finally {
            removeOverride(key);
        }

        return result;
    }


}
