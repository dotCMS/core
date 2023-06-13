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
    public final  boolean expired;
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
                    int licenseVersion, int level, boolean perpetual) {
        super();
        this.clientName = clientName;
        this.licenseType = licenseType;
        this.serial = serial;
        this.validUntil = validUntil;
        this.licenseVersion = licenseVersion;
        this.level = level;
        this.perpetual = perpetual;
        this.expired = expired();
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


	@Override
	public String toString() {
		return "{clientName:" + clientName + ", licenseType:" + licenseType + ", serial:" + serial
				+ ", validUntil:" + validUntil + ", level:" + level + ", perpetual:" + perpetual + "}";
	}



}
