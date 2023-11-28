package com.dotcms.api.client.pull;

import java.util.List;
import java.util.Map;

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
     * Returns a header for the T elements being pulled. Used for console user feedback.
     */
    String startPullingHeader(List<T> contents);

    /**
     * Returns a short format of a given T element. Used for console user feedback.
     */
    String shortFormat(T content, Map<String, Object> customOptions);

}
