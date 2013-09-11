package com.dotcms.packager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * @author Jonathan Gamba
 *         Date: 9/4/13
 */
public class Inspector {

    private HashMap<String, List<PathInfo>> classes = new HashMap<String, List<PathInfo>>();
    private HashSet<Formatter> formatters = new HashSet<Formatter>();

    /**
     * Add a Formatter to this inspector; results will be sent to
     * the formatter.
     *
     * @param fmt
     */
    public void addFormatter ( Formatter fmt ) {
        formatters.add( fmt );
    }

    /**
     * Generate the report of duplicate classes.
     *
     * @param title
     */
    public void report ( String title ) {

        for ( Formatter fmt : formatters ) {
            fmt.startReport( title );
            for ( String name : classes.keySet() ) {
                fmt.reportClass( name, classes.get( name ) );
            }
            fmt.endReport();
        }
    }

    /**
     * Inspect the given archive or directory tree for class instances.
     * If the pathElement is an archive (JAR), then we look for
     * class files in the archive. If the pathElement is a directory, then
     * we look for classes or archives with this directory as the root.
     *
     * @param pathElement the archive or directory to inspect.
     * @throws IllegalArgumentException if pathElement is null.
     */
    public void inspect ( File pathElement ) {

        // Validate input
        if ( pathElement == null ) {
            throw new IllegalArgumentException( "No path element to inspect!" );
        }

        // Inspect archives and subdirectories
        if ( pathElement.isFile() && isJarName( pathElement.getName() ) ) {
            inspectJar( pathElement );
        } else if ( pathElement.isDirectory() ) {
            inspectDir( pathElement, pathElement, "" );
        }
    }

    /**
     * Traverse the directory, looking for class files using this directory as
     * a root.
     *
     * @param base   the base directory we are inspecting from.
     * @param dir    the directory to inspect.
     * @param prefix the fully-qualified name of the package that corresponds
     *               to this directory
     */
    private void inspectDir ( File base, File dir, String prefix ) {

        File[] children = dir.listFiles();

        if ( children != null ) {

            // Inspect class files and subdirectories
            for ( File child : children ) {
                String name = prefix + "." + child.getName();
                if ( child.isFile() && isMatchingClass( child.getName() ) ) {
                    addClass( name, child.length(), base );
                }
                if ( child.isDirectory() ) {
                    inspectDir( base, child, name );
                }
                if ( isJarName( child.getName() ) ) {
                    inspectJar( child );
                }
            }
        }
    }

    /**
     * Inspect an archive for class files.
     *
     * @param jarfile the archive to inspect.
     */
    private void inspectJar ( File jarfile ) {

        // Open the file as a jar
        JarFile jar;
        try {
            jar = new JarFile( jarfile );
        } catch ( IOException e ) {
            e.printStackTrace();
            return;
        }

        // Look for each class file in the JAR
        Enumeration<JarEntry> enumeration = jar.entries();
        while ( enumeration.hasMoreElements() ) {
            JarEntry entry = enumeration.nextElement();
            if ( (!entry.isDirectory()) && isMatchingClass( entry.getName() ) ) {
                addClass( entry.getName(), entry.getSize(), jarfile );
            }
        }
    }

    /**
     * Check the name to see if it is a class file
     *
     * @param name the class name to check.
     * @return true if the name matches the pattern, false otherwise.
     */
    private boolean isMatchingClass ( String name ) {
        return name.endsWith( ".class" );
    }

    /**
     * Add the given class, found from the given base, to our list of classes
     * that we've found so far.
     *
     * @param className the fully-qualified class name for this class.
     * @param classSize the size of the class file, in bytes.
     * @param base      the base directory or archive in which this class was found.
     */
    private void addClass ( String className, long classSize, File base ) {

        // Convert class name from possible file or jar entry format
        className = className.replace( '/', '.' );
        className = className.replace( '\\', '.' );
        if ( className.endsWith( ".class" ) ) {
            className = className.substring( 0, className.length() - 6 );
        }

        // Add to the list
        List<PathInfo> files = classes.get( className );
        if ( files == null ) {
            files = new ArrayList<PathInfo>();
            files.add( new PathInfo( base, classSize ) );
            classes.put( className, files );
        } else {
            files.add( new PathInfo( base, classSize ) );
        }
    }

    /**
     * Return true if the name corresponds to a jar file
     *
     * @param name The file name
     * @return true if the name is a jar file, false otherwise.
     */
    private boolean isJarName ( String name ) {
        name = name.toLowerCase();
        return name.endsWith( ".jar" );
    }

    /**
     * Return the number of unique classes found.
     */
    public int getClassCount () {
        return classes.size();
    }

    /**
     * Return the number of unique classes which have two or more instances
     * present on the classpath.
     */
    public int getDuplicateCount () {
        int count = 0;
        for ( String name : classes.keySet() ) {
            List<PathInfo> paths = classes.get( name );
            if ( paths.size() > 1 ) {
                count++;
            }
        }
        return count;
    }

    public HashMap<String, List<PathInfo>> getClasses () {
        return classes;
    }

    /**
     * For objects that hold information about an instance of a class.
     */
    public static class PathInfo {

        /**
         * The base of the classpath that holds this instance
         */
        public File base;

        /**
         * The size in bytes of this instance
         */
        public long size;

        /**
         * Create an object holding the information about an instance of
         * a class.
         *
         * @param base      The base of the classpath that holds this instance
         * @param classSize The size in bytes of this instance
         */
        public PathInfo ( File base, long classSize ) {
            this.base = base;
            this.size = classSize;
        }
    }

}