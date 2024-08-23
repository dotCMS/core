package com.dotcms.cdi;


import javax.enterprise.inject.spi.CDI;

public class CDIUtils {

    private CDIUtils() {
    }

    public static <T> T getBean(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }


}
