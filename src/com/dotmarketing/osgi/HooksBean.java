package com.dotmarketing.osgi;

import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHook;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPreHook;

import java.io.Serializable;

/**
 * Encapsulates a hooks bean with pre and post hook
 * @author jsanca
 */
public class HooksBean implements Serializable {

    private final ContentletAPIPreHook apiPreHook;
    private final ContentletAPIPostHook apiPostHook;

    public HooksBean(final ContentletAPIPreHook apiPreHook,
                     final ContentletAPIPostHook apiPostHook) {

        this.apiPreHook = apiPreHook;
        this.apiPostHook = apiPostHook;
    }

    public ContentletAPIPreHook getApiPreHook() {
        return apiPreHook;
    }

    public ContentletAPIPostHook getApiPostHook() {
        return apiPostHook;
    }
} // E:O:F:HooksBean.
