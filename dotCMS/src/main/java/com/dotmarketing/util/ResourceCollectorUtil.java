package com.dotmarketing.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

}
