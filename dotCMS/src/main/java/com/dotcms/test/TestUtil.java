package com.dotcms.test;

public class TestUtil {



    public static final String DOTCMS_INTEGRATION_TEST = "DOTCMS_INTEGRATION_TEST";

    public static boolean isUnitTest() {
        if (isIntegrationTest()) {
            return false;
        }


        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;

    }

    public static boolean isIntegrationTest() {
        return System.getProperty(DOTCMS_INTEGRATION_TEST) != null;
    }


}
