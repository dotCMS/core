package com.dotcms.api.client.push;

import java.util.List;
import java.util.Optional;

/**
 * Interface for comparing content of type T.
 *
 * @param <T> the type of content to be compared
 */
public interface ContentComparator<T> {


    /**
     * Retrieves the type parameter of the class.
     *
     * @return the type parameter of the class
     */
    Class<T> type();

    /**
     * Finds matching server content based on local content and a list of server contents.
     *
     * @param localContent   the local content to compare against server contents
     * @param serverContents the list of server contents to search for matches
     * @return an Optional containing the matching server content if found, otherwise an empty
     * Optional.
     */
    Optional<T> findMatchingServerContent(T localContent, List<T> serverContents);

    /**
     * Checks if the given server content is contained within the list of local contents.
     *
     * @param serverContent the server content to check for containment
     * @param localContents the list of local contents to search for containment
     * @return an Optional containing the matching local content if found, or an empty Optional if
     * not found.
     */
    Optional<T> localContains(T serverContent, List<T> localContents);

    /**
     * Checks if the given local content and server content are equal.
     *
     * @param localContent  the local content to compare
     * @param serverContent the server content to compare
     * @return true if the local content is equal to the server content, false otherwise
     */
    boolean contentEquals(T localContent, T serverContent);
    
}
