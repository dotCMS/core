package com.dotcms.api.client.push;

import java.io.File;
import java.util.Comparator;
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
     * @param localFile      the local file representing the local content
     * @param localContent   the local content to compare against server contents
     * @param serverContents the list of server contents to search for matches
     * @return an Optional containing the matching server content if found, otherwise an empty
     * Optional.
     */
    Optional<T> findMatchingServerContent(File localFile, T localContent, List<T> serverContents);

    /**
     * Checks if the given server content is contained within the list of local contents.
     *
     * @param serverContent the server content to check for containment
     * @param localFiles    the list of local files representing the local contents
     * @param localContents the list of local contents to search for containment
     * @return true if the server content is contained within the list of local contents, false
     * otherwise
     */
    boolean existMatchingLocalContent(T serverContent, List<File> localFiles,
            List<T> localContents);

    /**
     * Checks if the given local content and server content are equal.
     *
     * @param localContent  the local content to compare
     * @param serverContent the server content to compare
     * @return true if the local content is equal to the server content, false otherwise
     */
    boolean contentEquals(T localContent, T serverContent);

    /**
     * Retrieves the file name without the extension from a given File object.
     *
     * @param file the File object representing the file
     * @return the file name without the extension
     */
    default String getFileName(File file) {
        String fileName = file.getName();
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    /**
     * Retrieves a comparator that can be used to sort a list of contents in the order they should
     * be processed.
     *
     * @return a comparator that can be used to sort a list of contents in the order they should be
     * processed
     */
    default Comparator<T> getProcessingOrderComparator() {
        // By default, don't change the processing order
        return new NullComparator<>();
    }

    /**
     * Default implementation of the ContentComparator interface that does not change the processing
     * order.
     *
     * @param <T> the type of content to be compared
     */
    class NullComparator<T> implements Comparator<T> {

        @Override
        public int compare(T t1, T t2) {
            return 0;
        }
    }
    
}
