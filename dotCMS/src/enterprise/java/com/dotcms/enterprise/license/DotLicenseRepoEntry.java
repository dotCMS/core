/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
