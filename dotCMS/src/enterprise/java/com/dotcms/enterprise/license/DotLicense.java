/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import com.dotcms.cluster.business.ServerAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;

import com.dotmarketing.util.UUIDGenerator;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.Date;


public class DotLicense implements Serializable {


    private static final long serialVersionUID = 1L;
    public final String clientName, licenseType, serial;
    public final Date validUntil;
    public final int licenseVersion, level;
    public final boolean perpetual;
    public final boolean expired;
    public final String raw;

    public static final Lazy<String> DEFAULT_CLIENT_NAME= Lazy.of(()-> Config.getStringProperty("CUSTOMER_LICENSE_NAME","dotCMS BSL License"));
    public static final Lazy<String>  DEFAULT_SERIAL= ServerAPI.SERVER_ID;


    public DotLicense() {
        this.licenseType = LicenseType.DEFAULT_TYPE.type;
        this.serial = DEFAULT_SERIAL.get();
        this.clientName = DEFAULT_CLIENT_NAME.get();
        this.validUntil = new Date(System.currentTimeMillis() + (100 * 356 * DateUtil.daysToMillis(24)));
        this.licenseVersion = 400;
        this.level = LicenseLevel.DEFAULT_LEVEL.level;
        this.perpetual = true;
        this.expired = false;
        this.raw = "BSL-"+serial;

    }


    private boolean expired(){
        return  (!this.perpetual && this.validUntil.before(new Date()));
    }


    public DotLicense(String clientName, LicenseType licenseType, String serial, Date validUntil,
                    int licenseVersion, int level, boolean perpetual, String raw) {
        super();
        this.clientName = clientName;
        this.licenseType = licenseType.type;
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
        this(clientName, LicenseType.valueOf(licenseType), serial, validUntil, licenseVersion, level, perpetual, null);
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
        return new DotLicense(this.clientName, LicenseType.valueOf(licenseType), this.serial, this.validUntil,
                this.licenseVersion, this.level, this.perpetual, value);
    }


	@Override
	public String toString() {
		return "{clientName:" + clientName + ", licenseType:" + licenseType + ", serial:" + serial
				+ ", validUntil:" + validUntil + ", level:" + level + ", perpetual:" + perpetual + "}";
	}



}
