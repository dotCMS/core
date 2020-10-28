package com.dotcms.system.event.local.type.security;

import java.io.Serializable;
import java.security.Key;


/**
 * This event is used to broadcast the info related with ResetKey event
 */
public class CompanyKeyResetEvent implements Serializable {

    private final String companyId;

    private final Key originalKey;

    private final Key resetKey;

    /**
     * Constructor
     * @param companyId
     * @param originalKey
     * @param resetKey
     */
    public CompanyKeyResetEvent(final String companyId, final Key originalKey, final Key resetKey) {
        this.companyId = companyId;
        this.originalKey = originalKey;
        this.resetKey = resetKey;
    }

    /**
     * Company id on which the event took place
     * @return
     */
    public String getCompanyId() {
        return companyId;
    }

    /**
     * Original Key before the Key reset action.
     * @return
     */
    public Key getOriginalKey() {
        return originalKey;
    }

    /**
     * The key after the reset action takes place.
     * @return
     */
    public Key getResetKey() {
        return resetKey;
    }
}
