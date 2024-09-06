package com.dotcms.business;

import java.io.Serializable;

/**
 * This is a local event to notify that a system table has been updated.
 * @author jsanca
 */
public class SystemTableUpdatedKeyEvent implements Serializable {

    private final String key;
    public SystemTableUpdatedKeyEvent(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
