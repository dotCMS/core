package com.dotcms.config;

import com.dotcms.business.SystemTable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates the configuration based on the system table
 * This is a dynamic configuration and can be changed at runtime
 * @author jsanca
 */
@ApplicationScoped
public class SystemTableConfigSourceOld implements ConfigSource {

    // todo: use CDI here instead of APILocator
    @Inject
    private final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();

    public SystemTableConfigSourceOld() {
        Logger.info(this.getClass(), "Creating SystemTableConfigSource");
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
        return SystemTableConfigSourceOld.class.getSimpleName();
    }
}
