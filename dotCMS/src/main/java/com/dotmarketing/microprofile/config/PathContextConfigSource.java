package com.dotmarketing.microprofile.config;

import com.dotmarketing.util.Logger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.spi.ConfigSource;

@ApplicationScoped
public class PathContextConfigSource implements ConfigSource {

    // private static final ThreadLocal<HashMap<String, String>> configuration =
    //        new ThreadLocal<>();

    private static final AtomicReference<HashMap<String, String>> configuration =
            new AtomicReference<>();

    public PathContextConfigSource() {
        Logger.info(this.getClass(), "Creating PathContextConfigSource");
    }

    @Override
    public int getOrdinal() {
        return 800;
    }

    @Override
    public Set<String> getPropertyNames() {
        HashMap<String, String> map = configuration.get();
        if (map == null) {
            return Collections.emptySet();
        }
        return map.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        HashMap<String, String> map = configuration.get();
        if (map == null) {
            return null;
        }
        return map.get(propertyName);
    }

    public String put(final String propertyName, String value) {
        Logger.info(this.getClass(), "Setting " + propertyName + " to " + value);
        HashMap<String, String> map = configuration.get();
        if (map == null) {
            map = new HashMap<>();
            configuration.set(map);
        }
        return map.put(propertyName, value);
    }

    public void remove(final String propertyName) {
        HashMap<String, String> map = configuration.get();
        if (map == null) {
            return;
        }
        map.remove(propertyName);
    }

    public void clear() {
        HashMap<String, String> map = configuration.get();
        if (map == null) {
            return;
        }
        map.clear();
    }

    @Override
    public String getName() {
        return PathContextConfigSource.class.getSimpleName();
    }

}
