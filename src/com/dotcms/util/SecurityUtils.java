package com.dotcms.util;

import com.dotmarketing.util.Config;
import com.liferay.util.Xss;

public class SecurityUtils {
    public static String stripReferer(String referer) {
        String ref = referer;
        if(Config.getBooleanProperty("DISABLE_EXTERNAL_REFERERS",true) && ref.contains("://")) {
            ref = "/";
        }
        
        ref = Xss.strip(ref);
        
        if(ref.contains("%0d") || ref.contains("%0a"))
            ref = "/";
        
        return ref;
    }
}
