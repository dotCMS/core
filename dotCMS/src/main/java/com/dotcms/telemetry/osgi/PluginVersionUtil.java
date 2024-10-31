package com.dotcms.telemetry.osgi;

import org.osgi.framework.BundleContext;

public class PluginVersionUtil {

    private static String version;

    private PluginVersionUtil(){}

    public static void init(BundleContext context) {
        version = context.getBundle().getHeaders().get("Bundle-Version");
    }

    public static String getVersion(){
        return version;
    }
}
