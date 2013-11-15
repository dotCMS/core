package com.dotmarketing.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * list resources available from the classpath @ *
 */
public class ResourceCollectorUtil{

    /**
     * for all elements of java.class.path get a Collection of resources Pattern
     * pattern = Pattern.compile(".*"); gets all resources
     * 
     * @param pattern
     *            the pattern to match
     * @return the resources in the order they are found
     */
    public static Collection<String> getResources(){
    	Pattern pattern = Pattern.compile(".*\\.class");
        final Set<String> retval = new HashSet<String>();
        final String classPath = System.getProperty("java.class.path", ".");
        String libPath = ResourceCollectorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(0, ResourceCollectorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().indexOf("WEB-INF" + File.separator)) + "WEB-INF" + File.separator + "lib";
        String classesPath = ResourceCollectorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(0, ResourceCollectorUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().indexOf("WEB-INF" + File.separator)) + "WEB-INF" + File.separator + "classes";
//        String libPath = Config.CONTEXT_PATH + "WEB-INF" + File.separator + "lib";
        List<String> classPathElements = new ArrayList(Arrays.asList(classPath.split(":")));
        classPathElements.add(classesPath);
        File dir = new File(libPath);
        if(dir.exists() && dir.isDirectory()){
        	for(File jar : dir.listFiles()){
        		if(jar.getName().endsWith(".jar")){
        			classPathElements.add(jar.getAbsolutePath());
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
        } else{
            retval.addAll(getResourcesFromJarFile(file, pattern));
        }
        return retval;
    }

    private static Collection<String> getResourcesFromJarFile(
        final File file,
        final Pattern pattern){
        final Set<String> retval = new HashSet<String>();
        ZipFile zf;
        try{
            zf = new ZipFile(file);
        } catch(final ZipException e){
            throw new Error(e);
        } catch(final IOException e){
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while(e.hasMoreElements()){
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if(accept){
            	try{
            		retval.add(fileName.substring(0, fileName.lastIndexOf(File.separator)));
            	}catch (StringIndexOutOfBoundsException e1) {
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