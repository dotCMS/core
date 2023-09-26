package com.dotcms.api.client.push;

import java.io.File;

/**
 * This interface represents a push handler, which is responsible for handling push operations for a
 * specific type.
 */
public interface PushHandler<T> {

    /**
     * Returns the type parameter of the class.
     *
     * @return the type parameter
     */
    Class<T> type();

    /**
     * Returns a title we can use for this type on the console.
     *
     * @return the title as a String
     */
    String title();

    /**
     * Generates a simple String representation of a content to use on the console.
     *
     * @param content the content to be displayed
     * @return a string representation of the content
     */
    String contentSimpleDisplay(T content);

    /**
     * Creates a T content in the server.
     *
     * @param localFile       the local file representing the content to be added
     * @param mappedLocalFile the mapped local file as a T
     */
    void add(File localFile, T mappedLocalFile);

    /**
     * Updates the server content with the local T content.
     *
     * @param localFile       the local file representing the content to be updated
     * @param mappedLocalFile the mapped local file as a T
     * @param serverContent   the existing server content to be updated
     */
    void edit(File localFile, T mappedLocalFile, T serverContent);

    /**
     * Removes the given serverContent from the server.
     *
     * @param serverContent the server content to be removed
     */
    void remove(T serverContent);

}