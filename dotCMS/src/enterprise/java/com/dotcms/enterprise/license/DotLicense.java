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

import com.dotmarketing.util.DateUtil;

import java.io.Serializable;
import java.util.Date;


public class DotLicense implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public final String clientName, licenseType, serial;
    public final Date validUntil;
    public final int licenseVersion, level;
    public final boolean perpetual;
    public final boolean expired;
    public final String raw;
    public static final String DEFAULT_CLIENT_NAME= "dotCMS Community License";
    public static final String DEFAULT_SERIAL= "dotCMS Community License";
    public static final LicenseLevel DEFAULT_LEVEL= LicenseLevel.COMMUNITY;
    
    
    public DotLicense() {
        this(DEFAULT_CLIENT_NAME,  
                        DEFAULT_LEVEL.COMMUNITY.name,
                        DEFAULT_LEVEL.COMMUNITY.name,
                        new Date(System.currentTimeMillis() + (100 * 356 * DateUtil.daysToMillis(24))),
                        400,
                        DEFAULT_LEVEL.level,
                        true);

        
    }


    private boolean expired(){
        return  (!this.perpetual && this.validUntil.before(new Date()));
    }
        

    public DotLicense(String clientName, String licenseType, String serial, Date validUntil,
                    int licenseVersion, int level, boolean perpetual, String raw) {
        super();
        this.clientName = clientName;
        this.licenseType = licenseType;
        this.serial = serial;
        this.validUntil = validUntil;
        this.licenseVersion = licenseVersion;
        this.level = level;
        this.perpetual = perpetual;
        this.expired = expired();
        this.raw = raw;
    }

    public DotLicense(String clientName, String licenseType, String serial, Date validUntil,
                      int licenseVersion, int level, boolean perpetual) {
        this(clientName, licenseType, serial, validUntil, licenseVersion, level, perpetual, null);
    }



    DotLicense licenseVersion(int value) {
        return new DotLicense(this.clientName, this.licenseType, this.serial, this.validUntil,
                        value, this.level, this.perpetual);
    }

    DotLicense validUntil(Date value) {
        return new DotLicense(this.clientName, this.licenseType, this.serial, value,
                        this.licenseVersion, this.level, this.perpetual);
    }

    DotLicense level(int value) {
        return new DotLicense(this.clientName, this.licenseType, this.serial, this.validUntil,
                        this.licenseVersion, value, this.perpetual);
    }

    DotLicense perpetual(boolean value) {
        return new DotLicense(this.clientName, this.licenseType, this.serial, this.validUntil,
                        this.licenseVersion, this.level, value);
    }

    DotLicense licenseType(String value) {
        return new DotLicense(this.clientName, value, this.serial, this.validUntil,
                        this.licenseVersion, this.level, this.perpetual);
    }
    DotLicense clientName(String value) {
        return new DotLicense(value, this.licenseType, this.serial, this.validUntil,
                        this.licenseVersion, this.level, this.perpetual);
    }
    DotLicense serial(String value) {
        return new DotLicense(this.clientName, this.licenseType, value, this.validUntil,
                        this.licenseVersion, this.level, this.perpetual);
    }
    DotLicense raw(String value) {
        return new DotLicense(this.clientName, this.licenseType, this.serial, this.validUntil,
                this.licenseVersion, this.level, this.perpetual, value);
    }


	@Override
	public String toString() {
		return "{clientName:" + clientName + ", licenseType:" + licenseType + ", serial:" + serial
				+ ", validUntil:" + validUntil + ", level:" + level + ", perpetual:" + perpetual + "}";
	}



}
