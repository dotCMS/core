
package com.dotcms.variant.business.web;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a session item for the current variant.
 * This is a Flash Session Item that will be available for 10 seconds.
 */
public class CurrentVariantSessionItem {

    private final int SECONDS_EXPIRE_TIME  = 10;

    private Instant addedDate;
    private String variantName;

    public CurrentVariantSessionItem(final String variantName) {
        this.addedDate = Instant.now();
        this.variantName = variantName;
    }

    /**
     * Return true if the instance is expired.
     * @return
     */
    public boolean isExpired() {
        return Instant.now().isAfter(addedDate.plusSeconds(SECONDS_EXPIRE_TIME));
    }

    /**
     * Return the current variant name.
     * @return
     */
    public String getVariantName() {
        return variantName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CurrentVariantSessionItem that = (CurrentVariantSessionItem) o;
        return Objects.equals(variantName, that.variantName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantName);
    }
}
