package com.dotmarketing.util;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import com.liferay.util.StringPool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * list resources available from the classpath @ *
 */
public class ResourceCollectorUtil {

    public static Collection<String> getImports(final File file) {

        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String importPackage = manifest.getMainAttributes().getValue("Import-Package");
            return UtilMethods.isSet(importPackage)?
                    Stream.of(importPackage.split(StringPool.COMMA)).collect(Collectors.toList()):
                    Collections.emptyList();
        }  catch (Exception e) {

            Logger.error(ResourceCollectorUtil.class, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public static Collection<String> getExports(final File file) {

        try (final JarFile jarFile     = new JarFile(file)){

            final Manifest manifest    = jarFile.getManifest();
            final String exportPackage = manifest.getMainAttributes().getValue("Export-Package");
            return UtilMethods.isSet(exportPackage)?
                    Stream.of(exportPackage.split(StringPool.COMMA)).collect(Collectors.toList()):
                    Collections.emptyList();
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
        final Set<String> retval = new HashSet<String>();
        final String classPath = System.getProperty("java.class.path", ".");
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        //Getting the context path
        String contextPath = Config.CONTEXT_PATH;
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
        final ArrayList<String> retval = new ArrayList<String>();
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
        final Set<String> retval = new HashSet<String>();

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
        final Set<String> retval = new HashSet<String>();
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