package com.dotcms.variant.business.web;

import java.time.Instant;

public class CurrentVariantSessionItem {

    private final int SECONDS_EXPIRE_TIME  = 10;

    private Instant addedDate;
    private String variantName;

    public CurrentVariantSessionItem(final String variantName) {
        this.addedDate = Instant.now();
        this.variantName = variantName;
    }

    public boolean isExpire() {
        return Instant.now().isAfter(addedDate.plusSeconds(SECONDS_EXPIRE_TIME));
    }

}
