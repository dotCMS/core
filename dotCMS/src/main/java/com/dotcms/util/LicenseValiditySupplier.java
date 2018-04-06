package com.dotcms.util;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;

public interface LicenseValiditySupplier {

    default boolean hasValidLicense(){
       return (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
    }

}
