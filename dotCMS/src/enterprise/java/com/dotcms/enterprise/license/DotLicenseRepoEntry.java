/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import java.io.Serializable;
import java.util.Date;


public class DotLicenseRepoEntry  implements Serializable {

    
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public final DotLicense dotLicense;
    public final  String license;
    public final  String serverId;
    public final  Date lastPing;
    public final  boolean available;
    public final long startupTime;

    public DotLicenseRepoEntry() {
        this.dotLicense = new DotLicense();
        this.license=null;
        this.serverId=null;
        this.lastPing=null;
        this.available=false;
        this.startupTime=0L;
    }



    public DotLicenseRepoEntry(DotLicense doLicense, String license, String serverId, Date lastPing, boolean available, long startupTime) {
        this.dotLicense = doLicense;
        this.license=license;
        this.serverId=serverId;
        this.lastPing=lastPing;
        this.available = available;
        this.startupTime=startupTime;
    }


    DotLicenseRepoEntry withDotLicense(DotLicense dotLicense) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, this.lastPing, this.available, this.startupTime);
    }

    DotLicenseRepoEntry withLicense(String newlicense) {
        return new DotLicenseRepoEntry(this.dotLicense, newlicense, this.serverId, this.lastPing, this.available, this.startupTime);
    }
    DotLicenseRepoEntry withServerId(String newserverId) {
        return new DotLicenseRepoEntry(dotLicense, this.license, newserverId, this.lastPing, this.available, this.startupTime);
    }

    DotLicenseRepoEntry withLastPing(Date xlastPing) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, xlastPing, this.available, this.startupTime);
    }
    DotLicenseRepoEntry withAvailable(boolean avail) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, this.lastPing, avail, this.startupTime);
    }
    
    DotLicenseRepoEntry withStartupTime(final long startupTime) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, this.lastPing, this.available, startupTime);
    }
    
}
