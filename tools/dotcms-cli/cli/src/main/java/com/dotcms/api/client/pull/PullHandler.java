package com.dotcms.api.client.pull;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.pull.PullOptions;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This abstract class represents a PullHandler, which is responsible for pulling elements of type T.
 *
 * @param <T> the type of elements to be pulled
 */
public abstract class PullHandler<T> {

    /**
     * Returns the title for the T elements being pulled. Used for logging purposes and for console
     * user feedback.
     */
    public abstract String title();

    /**
     * Returns a header for the T elements being pulled. Used for console user feedback.
     */
    public abstract String startPullingHeader(List<T> contents);

    /**
     * Returns a short format of a given T element. Used for console user feedback.
     */
    public abstract String shortFormat(T content, PullOptions pullOptions);

    /**
     * Pulls a list of T elements with the provided options and output settings.
     *
     * @param contents    the list of T elements to pull
     * @param pullOptions the options for the pull operation
     * @param output      the output settings for the pulled elements
     * @return the exit code for the pull operation
     * @throws ExecutionException   if an error occurs during the execution of the pull operation
     * @throws InterruptedException if the pull operation is interrupted
     */
    public abstract int pull(List<T> contents,
            PullOptions pullOptions,
            OutputOptionMixin output) throws ExecutionException, InterruptedException;

}
