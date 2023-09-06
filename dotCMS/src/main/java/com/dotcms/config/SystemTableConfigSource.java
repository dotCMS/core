package com.dotcms.config;

import com.dotcms.business.SystemAPI;
import com.dotcms.business.SystemTable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import java.util.Optional;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Encapsulates the configuration based on the system table
 * This is a dynamic configuration and can be changed at runtime
 * @author jsanca
 */
// todo: we have to handle the recursive call here
@ApplicationScoped
public class SystemTableConfigSource implements ConfigSource {

    @Inject
    private SystemAPI systemAPI;
    private final SystemTable systemTable;

    public SystemTableConfigSource() {
        Logger.info(this.getClass(), "Creating SystemTableConfigSource");
        systemTable = systemAPI.getSystemTable();
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @Override
    public Set<String> getPropertyNames() {

        return this.systemTable.all().keySet();
    }

    @Override
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

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

}
