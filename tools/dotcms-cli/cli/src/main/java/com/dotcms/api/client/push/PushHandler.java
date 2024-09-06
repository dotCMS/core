package com.dotcms.api.client.push;

import java.io.File;
import java.util.Map;

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
     * Returns the file name for a given T elements used to save the content to a file.
     *
     * @param content the content to be saved to a file.
     */
    String fileName(T content);

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
     * @param customOptions   custom options for the push operation
     * @return the created content
     */
    T add(File localFile, T mappedLocalFile, Map<String, Object> customOptions);

    /**
     * Updates the server content with the local T content.
     *
     * @param localFile       the local file representing the content to be updated
     * @param mappedLocalFile the mapped local file as a T
     * @param serverContent   the existing server content to be updated
     * @param customOptions   custom options for the push operation
     * @return the updated content
     */
    T edit(File localFile, T mappedLocalFile, T serverContent,
            Map<String, Object> customOptions);

    /**
     * Removes the given serverContent from the server.
     *
     * @param serverContent the server content to be removed
     * @param customOptions custom options for the push operation
     */
    void remove(T serverContent, Map<String, Object> customOptions);

}