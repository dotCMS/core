package com.dotcms.api.client.pull;

/**
 * This interface provides utility methods to handle the pulled content.
 *
 * @param <T> the type of pulled content.
 */
public interface PullHandler<T> {

    /**
     * Returns the title for the T elements being pulled. Used for logging purposes and for console
     * user feedback.
     */
    String title();

    /**
     * Returns a display name of a given T element. Used for logging purposes.
     */
    String displayName(T content);

    /**
     * Returns the file name for a given T elements used to save the content to a file.
     *
     * @param content the content to be saved to a file.
     */
    String fileName(T content);

    /**
     * Returns a short format of a given T element. Used for console user feedback.
     */
    String shortFormat(T content);

}
