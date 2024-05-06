package com.dotcms.config;

import com.dotcms.business.SystemTable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates the configuration based on the system table
 * This is a dynamic configuration and can be changed at runtime
 * @author jsanca
 */
public class SystemTableConfigSource {

    // there are some keys that can not be configured on the system table, all which includes concurrent are not
    private static final Set<String> SKIP_KEYS_LOWER_SET = Set.of("concurrent", "scheduler");
    private final Lazy<SystemTable> systemTable = Lazy.of(()->APILocator.getSystemAPI().getSystemTable());

    public SystemTableConfigSource() {
        Logger.info(this.getClass(), "Creating SystemTableConfigSource");
    }

    //@Override
    public int getOrdinal() {
        return 900;
    }

    //@Override
    public Set<String> getPropertyNames() {

        return this.systemTable.get().all().keySet();
    }

    // @Override
    public String getValue(final String propertyName) {

        Optional<String> valueOpt = Optional.empty();
        if (null != propertyName && this.isAllowed(propertyName)) {
            valueOpt = this.systemTable.get().get(propertyName);
        }
        return valueOpt.orElse(null);
    }

    private boolean isAllowed(final String propertyName) {
        final String lowerPropertyName = propertyName.toLowerCase();
        return SKIP_KEYS_LOWER_SET.stream().noneMatch(lowerPropertyName::contains);
    }

    public String put(final String propertyName, final String value) {

        this.systemTable.get().set(propertyName, value);
        return value;
    }

    public void remove(final String propertyName) {
        this.systemTable.get().delete(propertyName);
    }

    public void clear() {
        this.systemTable.get().all().keySet()
                .forEach(key -> this.systemTable.get().delete(key));
    }

    //@Override
    public String getName() {
        return SystemTableConfigSource.class.getSimpleName();
    }

}
