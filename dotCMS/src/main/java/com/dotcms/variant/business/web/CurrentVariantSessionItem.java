package com.dotcms.variant.business.web;

import java.time.Instant;
import java.util.Objects;

public class CurrentVariantSessionItem {

    private final int SECONDS_EXPIRE_TIME  = 10;

    private Instant addedDate;
    private String variantName;

    public CurrentVariantSessionItem(final String variantName) {
        this.addedDate = Instant.now();
        this.variantName = variantName;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(addedDate.plusSeconds(SECONDS_EXPIRE_TIME));
    }

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
