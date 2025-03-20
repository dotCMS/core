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

public enum LicenseType {

	COMMUNITY("community"),
    TRIAL("trial"), 
    DEV("dev"), 
    PROD("prod");

    public static LicenseType DEFAULT_TYPE = PROD;

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
