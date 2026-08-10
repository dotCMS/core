package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;

/** Seals further down: the branch stays closed. */
public sealed class Contentlet implements Permissionable permits Host {

    public String getPermissionType() {
        return "Contentlet";
    }
}
