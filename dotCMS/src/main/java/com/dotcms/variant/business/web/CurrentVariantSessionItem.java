
package com.dotcms.variant.business.web;

import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a session item for the current variant.
 * This is a Flash Session Item that will be available for 10 seconds.
 */
public class CurrentVariantSessionItem implements Serializable {

    private final Lazy<Integer> SECONDS_EXPIRE_TIME  = Lazy.of(() ->
            Config.getIntProperty("CURRENT_VARIANT_SECONDS_EXPIRE_TIME", 10));


    private final Instant addedDate;
    private final String variantName;

    public CurrentVariantSessionItem(final String variantName) {
        this.addedDate = Instant.now();
        this.variantName = variantName;
    }

    /**
     * Return true if the instance is expired.
     * @return
     */
    public boolean isExpired() {
        return Instant.now().isAfter(addedDate.plusSeconds(SECONDS_EXPIRE_TIME.get()));
    }

    /**
     * Return the current variant name.
     * @return
     */
    public String getVariantName() {
        return variantName;
    }

    @Override
    public String toString() {
        return  variantName;
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
