package com.dotcms.rest.config;

public class RestServiceUtil {

    public static void addResource(Class clazz) {
        DotRestApplication.addClass(clazz);
    }

    public static void removeResource(Class clazz) {
        DotRestApplication.removeClass(clazz);

    }

}
