package org.elasticsearch.bootstrap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

public class JarHell {
    private JarHell() {}
    public static void checkJarHell() throws Exception {}
    public static void checkJarHell(URL urls[]) throws Exception {}
    public static void checkJarHell(Set<URL> urls) throws URISyntaxException, IOException{}
    public static void checkVersionFormat(String targetVersion) {}
    public static void checkJavaVersion(String resource, String targetVersion) {}
    public static Set<URL> parseClassPath() {return Set.of();}
}
