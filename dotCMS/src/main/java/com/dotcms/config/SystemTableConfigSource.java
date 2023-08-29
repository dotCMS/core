package com.dotcms.config;

import com.dotcms.business.SystemTable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates the configuration based on the system table
 * This is a dynamic configuration and can be changed at runtime
 * @author jsanca
 */
public class SystemTableConfigSource {

    // private static final ThreadLocal<HashMap<String, String>> configuration =
    //        new ThreadLocal<>();
    private final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();

    public SystemTableConfigSource() {
        Logger.info(this.getClass(), "Creating SystemTableConfigSource");
    }

    //@Override
    public int getOrdinal() {
        return 900;
    }

    //@Override
    public Set<String> getPropertyNames() {

        return this.systemTable.all().keySet();
    }

    // @Override
    public String getValue(final String propertyName) {

        final Optional<String> valueOpt = this.systemTable.get(propertyName);
        return valueOpt.isPresent() ? valueOpt.get() : null;
    }

    public String put(final String propertyName, final String value) {

        this.systemTable.set(propertyName, value);
        return value;
    }

    public void remove(final String propertyName) {
        this.systemTable.delete(propertyName);
    }

    public void clear() {
        this.systemTable.all().keySet().stream()
                .forEach(key -> this.systemTable.delete(key));
    }

    //@Override
    public String getName() {
        return SystemTableConfigSource.class.getSimpleName();
    }
}
