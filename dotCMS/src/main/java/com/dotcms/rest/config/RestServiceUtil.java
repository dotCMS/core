package com.dotcms.rest.config;

import com.dotcms.rest.servlet.ReloadableServletContainer;



public class RestServiceUtil {

    public static void addResource(Class clazz) {
        DotRestApplication.addClass(clazz);
        reloadRest();
    }

    public static void removeResource(Class clazz) {
        DotRestApplication.removeClass(clazz);
        reloadRest();
    }

    public synchronized static void reloadRest() {
        ReloadableServletContainer.reload(new DotRestApplication());

    }

}
