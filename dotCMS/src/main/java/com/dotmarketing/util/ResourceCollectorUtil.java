package com.dotmarketing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import com.google.common.collect.ImmutableList;

/**
 * list resources available from the classpath @ *
 */
public class ResourceCollectorUtil {

    /**
     * Returns true if the file is a fragment
     * @param file {@link File}
     * @return boolean if it is a fragment
     */
    public static boolean isFragmentJar (final File file) {

        boolean isFragment = false;
        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String fragmentHost  = manifest.getMainAttributes().getValue("Fragment-Host");
            isFragment =  UtilMethods.isSet(fragmentHost) &&
                    "system.bundle; extension:=framework".equals(fragmentHost.trim());
        }  catch (Exception e) {

            Logger.error(ResourceCollectorUtil.class, e.getMessage(), e);
        }

        return isFragment;
    }

    /**
     * Get the packages for a jar file, if the file is a fragment will return the Export packages
     * If the file is a bundle will return the Import packages
     * @param file File
     * @return Collection string
     */
    
    public static Collection<String> getPackages(final File file) {
        String importPackage = null;
        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String fragmentHost  = manifest.getMainAttributes().getValue("Fragment-Host");
            importPackage =  UtilMethods.isSet(fragmentHost) &&
                    "system.bundle; extension:=framework".equals(fragmentHost.trim())?
                    manifest.getMainAttributes().getValue("Export-Package"):
                    manifest.getMainAttributes().getValue("Import-Package");
        }  catch (Exception e) {

            Logger.error(ResourceCollectorUtil.class, e.getMessage(), e);
        }

        return getPackages(importPackage);
        
        
    }

    public static Collection<String> getPackages(final String importPackage) {

        
        if(UtilMethods.isEmpty(importPackage)) {
            return  ImmutableList.of();
        }

        return removeVersionRange(importPackage);
        
    }

    private static Collection<String> removeVersionRange(String packages){
        
        final Set<String> finalSet = new LinkedHashSet<>();
        
        for(final String pkg :StringUtils.splitOnCommasWithQuotes(packages)
                .stream().map(String::trim).filter(ResourceCollectorUtil::isValidPackage).collect(Collectors.toSet())) {
            int brace = pkg.indexOf("\"[");
            int comma = pkg.indexOf(",");
            int parans = pkg.indexOf(")\"");
            
            String version = pkg;
            
            if(brace> 0 && brace < comma && comma < parans){
                version = pkg.substring(0,brace) +  pkg.substring(brace+2, parans).split(",")[0] ;
            }
            finalSet.add(version);
            
            
        }
        
        return finalSet;
    }

    private static boolean isValidPackage (final String packageName) {

        return Objects.nonNull(packageName) && packageName.length() > 0 && !".".equals(packageName); // more?
    }
    
    
    
    
    
    /**
     * Get the Import-Package from the jar file
     * @param file File
     * @return Collection String
     */
    public static Collection<String> getImports(final File file) {

        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String importPackage = manifest.getMainAttributes().getValue("Import-Package");
            
            return StringUtils.splitOnCommasWithQuotes(importPackage);

        }  catch (Exception e) {

            Logger.error(ResourceCollectorUtil.class, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * Get the Export-Package from the jar file
     * @param file File
     * @return Collection String
     */
    public static Collection<String> getExports(final File file) {

        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String exportPackage = manifest.getMainAttributes().getValue("Export-Package");
            return StringUtils.splitOnCommasWithQuotes(exportPackage);
        }  catch (Exception e) {

            Logger.error(ResourceCollectorUtil.class, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     *
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources() {
        return getResources(null);
    }

    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     *
     * @param jarPrefixesFilter List of jar prefixes of the jars we want to get the resources from
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources(List<String> jarPrefixesFilter) {

    	Pattern pattern = Pattern.compile(".*\\.class");
        final Set<String> retval = new HashSet<>();
        final String classPath = System.getProperty("java.class.path", ".");
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        //Getting the context path
        String contextPath = Config.CONTEXT.getRealPath("/");
        if ( !contextPath.endsWith( File.separator ) ) {
            contextPath += File.separator;
        }
        String codeSourcePath = contextPath + "WEB-INF" + File.separator;

        if(isWindows){
        	try {
				codeSourcePath = new File(URLDecoder.decode(codeSourcePath, "UTF-8")).getPath();
			} catch (UnsupportedEncodingException e) {
				Logger.error(ResourceCollectorUtil.class, e.getMessage());
			}
        }
        String libPath = codeSourcePath.substring(0, codeSourcePath.indexOf("WEB-INF")) + File.separator + "WEB-INF" + File.separator + "lib";
        String classesPath = codeSourcePath.substring(0, codeSourcePath.indexOf("WEB-INF"))  + File.separator + "WEB-INF" + File.separator + "classes";

        List<String> classPathElements;
        if (isWindows) {
            classPathElements = new ArrayList(Arrays.asList(classPath.split(";")));
        } else {
            classPathElements = new ArrayList(Arrays.asList(classPath.split(":")));
        }

        classPathElements.add(classesPath);
        File dir = new File(libPath);
        if(dir.exists() && dir.isDirectory()){
        	for(File jar : dir.listFiles()){
        		if(jar.getName().endsWith(".jar")){

                    Boolean useJar = true;

                    //Filtering the jars if a filter is available
                    if (null != jarPrefixesFilter && !jarPrefixesFilter.isEmpty()) {
                        for (String prefix : jarPrefixesFilter) {
                            useJar = jar.getName().startsWith(prefix);
                            if (useJar) {
                                break;
                            }
                        }
                    }
                    if (useJar) {
                        classPathElements.add(jar.getAbsolutePath());
                    }
                }
        	}
        }
        for(final String element : classPathElements){
            retval.addAll(getResources(element, pattern));
        }
        return retval;
    }

    private static Collection<String> getResources(
        final String element,
        final Pattern pattern){
        final ArrayList<String> retval = new ArrayList<>();
        final File file = new File(element);
        if(file.isDirectory()){
            retval.addAll(getResourcesFromDirectory(file, pattern));
        } else if(file.getName().endsWith(".jar")){
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(
        final File file,
        final Pattern pattern){
        final Set<String> retval = new HashSet<>();

        if(file==null) return retval;

        ZipFile zf;
        try{
            zf = new ZipFile(file);
        } catch(final ZipException e){
        	Logger.error(ResourceCollectorUtil.class, "Problem while creating ZipFile for file: " + file.getName());
            throw new Error(e);
        }  catch(final FileNotFoundException e){
        	Logger.error(ResourceCollectorUtil.class, e.getMessage());
        	return retval;
        } catch(final IOException e){
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while(e.hasMoreElements()){
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if(accept){
                try {
                    //Zip entries have '/' as separator on all platforms
                    retval.add( fileName.substring( 0, fileName.lastIndexOf( "/" ) ) );
                } catch ( StringIndexOutOfBoundsException e1 ) {
                    //do nothing
                }
            }
        }
        try{
            zf.close();
        } catch(final IOException e1){
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(
        final File directory,
        final Pattern pattern){
        final Set<String> retval = new HashSet<>();
        final File[] fileList = directory.listFiles();
        for(final File file : fileList){
            if(file.isDirectory()){
                retval.addAll(getResourcesFromDirectory(file, pattern));
            } else{
                try{
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if(accept){
                    	try{
                    		retval.add(fileName.substring(0, fileName.lastIndexOf(File.separator)));
                    	}catch (StringIndexOutOfBoundsException e1) {
                    		//do nothing
        				}
                    }
                } catch(final IOException e){
                    throw new Error(e);
                }
            }
        }
        return retval;
    }

    /**
     * Represents Java compilation version information extracted from a JAR file
     */
    public static class JavaVersionInfo {
        private final String manifestVersion;
        private final Integer classMajorVersion;
        private final boolean isMultiRelease;

        public JavaVersionInfo(final String manifestVersion, final Integer classMajorVersion, final boolean isMultiRelease) {
            this.manifestVersion = manifestVersion;
            this.classMajorVersion = classMajorVersion;
            this.isMultiRelease = isMultiRelease;
        }

        /**
         * Returns the Java version string from the manifest (Created-By or Build-Jdk)
         * @return String manifest version or null
         */
        public String getManifestVersion() {
            return manifestVersion;
        }

        /**
         * Returns the class file major version number
         * @return Integer major version (e.g., 55 for Java 11, 61 for Java 17, 65 for Java 21) or null
         */
        public Integer getClassMajorVersion() {
            return classMajorVersion;
        }

        /**
         * Returns whether this is a multi-release JAR (Java 9+)
         * @return boolean true if multi-release JAR
         */
        public boolean isMultiRelease() {
            return isMultiRelease;
        }

        /**
         * Returns a human-readable Java version string
         * @return String like "Java 11", "Java 17", "Java 21", or the manifest version if class version unavailable
         */
        public String getJavaVersion() {
            if (classMajorVersion != null) {
                return majorVersionToJavaVersion(classMajorVersion);
            }
            return manifestVersion != null ? manifestVersion : "Unknown";
        }

        /**
         * Checks if the JAR is compatible with Java 21 runtime (class version <= 65)
         * @return boolean true if compatible
         */
        public boolean isCompatibleWithJava21() {
            return classMajorVersion == null || classMajorVersion <= 65;
        }

        /**
         * Checks if the JAR requires Java 11 or higher (class version >= 55)
         * @return boolean true if requires Java 11+
         */
        public boolean requiresJava11OrHigher() {
            return classMajorVersion != null && classMajorVersion >= 55;
        }

        /**
         * Converts class file major version to Java version string
         * @param majorVersion class file major version
         * @return String Java version
         */
        private static String majorVersionToJavaVersion(final int majorVersion) {
            // Handle legacy Java 1.4 naming convention
            if (majorVersion == 48) {
                return "Java 1.4";
            }
            // Formula works for Java 5+ (version 49+)
            // Java 21 = 65, Java 22 = 66, etc.
            // majorVersion - 44 = Java version number
            if (majorVersion >= 45) {
                return "Java " + (majorVersion - 44);
            }
            return "Unknown (version " + majorVersion + ")";
        }
    }

    /**
     * Extracts Java compilation version information from a JAR file.
     * Uses two methods: reading MANIFEST.MF attributes (Created-By, Build-Jdk) and
     * reading class file bytecode major version (most reliable).
     *
     * @param file JAR file to analyze
     * @return JavaVersionInfo containing version details, never null
     */
    public static JavaVersionInfo getJavaVersion(final File file) {
        String manifestVersion = null;
        Integer classMajorVersion = null;
        boolean isMultiRelease = false;

        if (file == null || !file.exists() || !file.canRead()) {
            Logger.debug(ResourceCollectorUtil.class, "Cannot read JAR file: " +
                (file != null ? file.getName() : "null"));
            return new JavaVersionInfo(null, null, false);
        }

        try (final JarFile jarFile = new JarFile(file)) {
            // Method 1: Read from MANIFEST.MF
            final Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                // Try different manifest attributes in order of preference
                manifestVersion = manifest.getMainAttributes().getValue("Created-By");
                if (!UtilMethods.isSet(manifestVersion)) {
                    manifestVersion = manifest.getMainAttributes().getValue("Build-Jdk");
                }
                if (!UtilMethods.isSet(manifestVersion)) {
                    manifestVersion = manifest.getMainAttributes().getValue("Build-Jdk-Spec");
                }

                // Check if multi-release JAR
                final String multiRelease = manifest.getMainAttributes().getValue("Multi-Release");
                isMultiRelease = "true".equalsIgnoreCase(multiRelease);
            }

            // Method 2: Read class file major version (most reliable)
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String name = entry.getName();

                // Find first .class file (skip module-info and package-info)
                if (name.endsWith(".class") &&
                    !name.endsWith("module-info.class") &&
                    !name.endsWith("package-info.class")) {

                    try (final InputStream is = jarFile.getInputStream(entry)) {
                        // Read class file header
                        // Bytes 0-3: magic number (0xCAFEBABE)
                        // Bytes 4-5: minor version
                        // Bytes 6-7: major version
                        final byte[] header = new byte[8];
                        final int bytesRead = is.read(header);

                        if (bytesRead == 8) {
                            // Verify magic number
                            final int magic = ((header[0] & 0xFF) << 24) |
                                            ((header[1] & 0xFF) << 16) |
                                            ((header[2] & 0xFF) << 8) |
                                            (header[3] & 0xFF);

                            if (magic == 0xCAFEBABE) {
                                // Extract major version (big-endian)
                                classMajorVersion = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                                final Integer detectedVersion = classMajorVersion;
                                Logger.debug(ResourceCollectorUtil.class,
                                    () -> String.format("Detected class major version %d for %s from %s",
                                        detectedVersion, entry.getName(), file.getName()));
                            } else {
                                Logger.debug(ResourceCollectorUtil.class,
                                    "Invalid class file magic number in: " + entry.getName());
                            }
                        }
                    } catch (IOException e) {
                        Logger.debug(ResourceCollectorUtil.class,
                            "Error reading class file: " + entry.getName() + " - " + e.getMessage());
                    }
                    break; // Only need to check one class file
                }
            }

        } catch (Exception e) {
            Logger.error(ResourceCollectorUtil.class,
                "Error extracting Java version from: " + file.getName(), e);
        }

        return new JavaVersionInfo(manifestVersion, classMajorVersion, isMultiRelease);
    }

    /**
     * Represents Maven build information extracted from a JAR file.
     * Only contains the dotcms-core dependency version if the plugin was built with Maven.
     */
    public static class MavenInfo {
        private final boolean isBuiltWithMaven;
        private final String dotcmsCoreDependencyVersion;

        public MavenInfo(final boolean isBuiltWithMaven, final String dotcmsCoreDependencyVersion) {
            this.isBuiltWithMaven = isBuiltWithMaven;
            this.dotcmsCoreDependencyVersion = dotcmsCoreDependencyVersion;
        }

        /**
         * Returns whether this JAR was built with Maven
         * @return boolean true if Maven metadata exists
         */
        public boolean isBuiltWithMaven() {
            return isBuiltWithMaven;
        }

        /**
         * Returns the version of com.dotcms:dotcms-core dependency from pom.xml
         * @return String version or null if not found
         */
        public String getDotcmsCoreDependencyVersion() {
            return dotcmsCoreDependencyVersion;
        }
    }

    /**
     * Extracts Maven build information from a JAR file.
     * Reads META-INF/maven/{groupId}/{artifactId}/pom.xml
     * to find the dotcms-core dependency version if the plugin was built with Maven.
     *
     * @param file JAR file to analyze
     * @return MavenInfo containing Maven metadata, never null
     */
    public static MavenInfo getMavenInfo(final File file) {
        boolean isBuiltWithMaven = false;
        String dotcmsCoreDependencyVersion = null;

        if (file == null || !file.exists() || !file.canRead()) {
            Logger.debug(ResourceCollectorUtil.class, "Cannot read JAR file: " +
                (file != null ? file.getName() : "null"));
            return new MavenInfo(false, null);
        }

        try (final JarFile jarFile = new JarFile(file)) {
            // Find any pom.xml in META-INF/maven directory
            final Enumeration<JarEntry> entries = jarFile.entries();
            String pomXmlPath = null;

            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String name = entry.getName();

                // Look for pom.xml: META-INF/maven/{groupId}/{artifactId}/pom.xml
                if (name.startsWith("META-INF/maven/") && name.endsWith("/pom.xml")) {
                    pomXmlPath = name;
                    isBuiltWithMaven = true;
                    break;
                }
            }

            // Read pom.xml to find com.dotcms:dotcms-core dependency version
            if (pomXmlPath != null) {
                final JarEntry pomXmlEntry = jarFile.getJarEntry(pomXmlPath);
                if (pomXmlEntry != null) {
                    try (final InputStream is = jarFile.getInputStream(pomXmlEntry)) {
                        // Parse XML to find dotcms-core dependency
                        // Look for pattern: <groupId>com.dotcms</groupId><artifactId>dotcms-core</artifactId><version>X.X.X</version>
                        final String pomContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        dotcmsCoreDependencyVersion = extractDotcmsCoreDependencyVersion(pomContent);

                        if (dotcmsCoreDependencyVersion != null) {
                            final String foundVersion = dotcmsCoreDependencyVersion;
                            Logger.debug(ResourceCollectorUtil.class, () -> String.format(
                                "Found dotcms-core dependency version %s in %s",
                                foundVersion, file.getName()
                            ));
                        }
                    }
                }
            }

        } catch (Exception e) {
            Logger.debug(ResourceCollectorUtil.class,
                "Error extracting Maven info from: " + file.getName() + " - " + e.getMessage());
        }

        return new MavenInfo(isBuiltWithMaven, dotcmsCoreDependencyVersion);
    }

    /**
     * Extracts the dotcms-core dependency version from a pom.xml file content.
     * Looks for the specific dependency block with groupId=com.dotcms and artifactId=dotcms-core.
     *
     * @param pomContent The XML content of the pom.xml file
     * @return The version string or null if not found
     */
    private static String extractDotcmsCoreDependencyVersion(final String pomContent) {
        if (pomContent == null || pomContent.isEmpty()) {
            return null;
        }

        try {
            // Simple XML parsing using regex to find the dotcms-core dependency
            // Pattern: <dependency>...<groupId>com.dotcms</groupId>...<artifactId>dotcms-core</artifactId>...<version>X.X.X</version>...</dependency>

            // Split by <dependency> tags
            final String[] dependencies = pomContent.split("<dependency>");

            for (final String dependency : dependencies) {
                if (!dependency.contains("</dependency>")) {
                    continue;
                }

                final String depBlock = dependency.substring(0, dependency.indexOf("</dependency>"));

                // Check if this is the com.dotcms:dotcms-core dependency
                if (depBlock.contains("<groupId>com.dotcms</groupId>") &&
                    depBlock.contains("<artifactId>dotcms-core</artifactId>")) {

                    // Extract version
                    final int versionStart = depBlock.indexOf("<version>");
                    if (versionStart != -1) {
                        final int versionEnd = depBlock.indexOf("</version>", versionStart);
                        if (versionEnd != -1) {
                            return depBlock.substring(versionStart + 9, versionEnd).trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ResourceCollectorUtil.class,
                "Error parsing pom.xml for dotcms-core dependency: " + e.getMessage());
        }

        return null;
    }

}
