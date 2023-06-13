package com.dotcms.enterprise.license;

import com.dotmarketing.business.DotStateException;

public enum LicenseType {

	COMMUNITY("community"),
    TRIAL("trial"), 
    DEV("dev"), 
    PROD("prod");


    public final String type;

    
    LicenseType(String type) {
        this.type = type;

    }
    
    public static LicenseType fromString(String typeStr ){
        for(LicenseType type : LicenseType.values()){
            if(type.type.equals(typeStr)){
            	return type;
            }
        }
        throw new DotStateException("Invalid license type");
        
    }


}
