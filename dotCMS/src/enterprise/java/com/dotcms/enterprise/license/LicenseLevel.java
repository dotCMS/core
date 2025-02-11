/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import com.dotmarketing.business.DotStateException;

public enum LicenseLevel {


    COMMUNITY("COMMUNITY EDITION", 100), 
    STANDARD("STANDARD EDITION", 200), 
    PROFESSIONAL("PROFESSIONAL EDITION", 300), 
    PRIME("PRIME EDITION", 400), 
    PLATFORM("PLATFORM EDITION", 500);

    public static LicenseLevel DEFAULT_LEVEL = PLATFORM;



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
