package com.dotcms.packager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 9/5/13
 */
public class PlainFormatter implements Formatter {

    // Whether or not to report all instances, or only duplicates
    private boolean reportAll = true;

    // The stream we write our report to.
    private PrintStream out = System.out;

    /**
     * Create a new formatter.
     */
    public PlainFormatter () {
        // no activity
    }

    /**
     * Set whether the formatter reports all class instances, or only
     * duplicate class instances.
     *
     * @param duplicatesOnly true to report only duplicates, false to report all.
     */
    public void setDuplicatesOnly ( boolean duplicatesOnly ) {
        reportAll = !duplicatesOnly;
    }

    /**
     * Set the stream the formatter is supposed to write its results to.
     *
     * @param out
     */
    public void setOutput ( OutputStream out ) {
        this.out = new PrintStream( out );
    }

    /**
     * Inform the report that it is starting.
     *
     * @param title
     */
    public void startReport ( String title ) {
        if ( title != null ) {
            out.println( title );
        }
    }

    /**
     * Inform the report that it is ending.
     */
    public void endReport () {
        //No need for plain text
    }

    /**
     * Add a class file to the report.
     *
     * @param name  the fully-qualified class name
     * @param bases the archives or root paths at which the class is located
     */
    public void reportClass ( String name, List<Inspector.PathInfo> bases ) {

        if ( reportAll || bases.size() > 1 ) {

            out.println( name );
            for ( Inspector.PathInfo base : bases ) {
                out.println( "    " + base.size + "  " + getCompletePath( base.base ) );
            }
        }
    }

    /**
     * Return the best available absolute path for the given file. This will
     * be the canonical path if it is available; otherwise it will be the
     * absolute path.
     *
     * @param file the file whose path we want
     * @return an absolute path for the file
     */
    private String getCompletePath ( File file ) {
        try {
            return file.getCanonicalPath();
        } catch ( IOException e ) {
            return file.getAbsolutePath();
        }
    }

}