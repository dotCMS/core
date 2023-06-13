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


    public DotLicenseRepoEntry() {
        this.dotLicense = new DotLicense();
        this.license=null;
        this.serverId=null;
        this.lastPing=null;
        this.available=false;
    }



    public DotLicenseRepoEntry(DotLicense doLicense, String license, String serverId, Date lastPing, boolean available) {
        this.dotLicense = doLicense;
        this.license=license;
        this.serverId=serverId;
        this.lastPing=lastPing;
        this.available = available;
    }


    DotLicenseRepoEntry withDotLicense(DotLicense dotLicense) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, this.lastPing, this.available);
    }

    DotLicenseRepoEntry withLicense(String newlicense) {
        return new DotLicenseRepoEntry(this.dotLicense, newlicense, this.serverId, this.lastPing, this.available);
    }
    DotLicenseRepoEntry withServerId(String newserverId) {
        return new DotLicenseRepoEntry(dotLicense, this.license, newserverId, this.lastPing, this.available);
    }

    DotLicenseRepoEntry withLastPing(Date xlastPing) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, xlastPing, this.available);
    }
    DotLicenseRepoEntry withAvailable(boolean avail) {
        return new DotLicenseRepoEntry(dotLicense, this.license, this.serverId, this.lastPing, avail);
    }
}
