package com.dotcms.packager;

import java.io.OutputStream;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 9/5/13
 */
public interface Formatter {

    /**
     * Set whether the formatter reports all class instances, or only
     * duplicate class instances.
     *
     * @param value true to report only duplicates, false to report all.
     */
    public void setDuplicatesOnly ( boolean value );

    /**
     * Set the stream the formatter is supposed to write its results to.
     *
     * @param out The stream to write to.
     */
    public void setOutput ( OutputStream out );

    /**
     * Inform the report that it is starting.
     *
     * @param title The title for the report, or null if there is no title.
     */
    public void startReport ( String title );

    /**
     * Inform the report that it is ending.
     */
    public void endReport ();

    /**
     * Add a class file to the report.
     *
     * @param name  the fully-qualified class name
     * @param bases the archives or root paths at which the class is located
     */
    public void reportClass ( String name, List<Inspector.PathInfo> bases );

}