package com.dotcms.enterprise.license;

import com.dotmarketing.business.DotStateException;

public enum LicenseLevel {


    COMMUNITY("COMMUNITY EDITION", 100), 
    STANDARD("STANDARD EDITION", 200), 
    PROFESSIONAL("PROFESSIONAL EDITION", 300), 
    PRIME("PRIME EDITION", 400), 
    PLATFORM("PLATFORM EDITION", 500);

    public final String name;
    public final int level;
    
    LicenseLevel(String name, int level) {
        this.name = name;
        this.level = level;
    }
    
    static LicenseLevel fromInt(int level){
        for(LicenseLevel ll : LicenseLevel.values()){
            if(level == ll.level){
                return ll;
            }
        }
        throw new DotStateException("Invalid license level");
        
    }


}
