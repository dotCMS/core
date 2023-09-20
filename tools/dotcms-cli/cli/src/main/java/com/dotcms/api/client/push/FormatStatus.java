package com.dotcms.api.client.push;

import com.dotcms.api.client.push.exception.PushException;
import com.dotcms.model.push.PushAnalysisResult;
import java.util.Iterator;
import java.util.List;
import javax.enterprise.context.Dependent;

/**
 * The {@code FormatStatus} class is responsible for formatting the status of a push operation. It
 * provides methods for formatting the results of a push analysis into a user-friendly format.
 *
 * <p>This class is meant to be used in conjunction with a {@link PushHandler} which provides
 * additional functionality for handling the push operation.
 *
 * @see PushAnalysisResult
 * @see PushHandler
 */
@Dependent
public class FormatStatus {

    private final String COLOR_NEW = "green";
    private final String COLOR_MODIFIED = "cyan";
    private final String COLOR_DELETED = "red";

    private final String REGULAR_FORMAT = "%s";
    private final String PUSH_NEW_FORMAT = "@|bold," + COLOR_NEW + " %s \u2795|@";
    private final String PUSH_MODIFIED_FORMAT = "@|bold," + COLOR_MODIFIED + " %s \u270E|@";
    private final String PUSH_DELETE_FORMAT = "@|bold," + COLOR_DELETED + " %s \u2716|@";

    /**
     * Formats the push analysis results using the specified push handler.
     *
     * @param results     the list of push analysis results
     * @param pushHandler the push handler to use for formatting
     * @return a StringBuilder containing the formatted push analysis results
     */
    public <T> StringBuilder format(final List<PushAnalysisResult<T>> results,
            PushHandler<T> pushHandler) {

        var outputBuilder = new StringBuilder();

        outputBuilder.append(String.format(" %s:", pushHandler.title())).append("\n");

        Iterator<PushAnalysisResult<T>> iterator = results.iterator();
        while (iterator.hasNext()) {
            PushAnalysisResult<T> result = iterator.next();
            boolean isLast = !iterator.hasNext();
            outputBuilder.append(formatResult("     ", result, pushHandler, isLast));
        }

        return outputBuilder;
    }

    /**
     * Formats a single push analysis result using the specified prefix, result, push handler, and
     * lastElement indicator.
     *
     * @param prefix      the prefix to use for indentation
     * @param result      the push analysis result to format
     * @param pushHandler the push handler to use for formatting
     * @param lastElement indicates whether the result is the last element in the list
     * @param <T>         z        the type of the push analysis result
     * @return a StringBuilder containing the formatted push analysis result
     */
    private <T> StringBuilder formatResult(final String prefix,
            final PushAnalysisResult<T> result, final PushHandler<T> pushHandler,
            final boolean lastElement) {

        var outputBuilder = new StringBuilder();

        String contentFormat;
        String contentName;
        switch (result.action()) {
            case ADD:
                contentFormat = PUSH_NEW_FORMAT;
                contentName = result.localFile().get().getName();
                break;
            case UPDATE:
                contentFormat = PUSH_MODIFIED_FORMAT;
                contentName = result.localFile().get().getName();
                break;
            case REMOVE:
                contentFormat = PUSH_DELETE_FORMAT;
                contentName = pushHandler.contentSimpleDisplay(result.serverContent().get());
                break;
            case NO_ACTION:
                contentFormat = REGULAR_FORMAT;
                contentName = result.localFile().get().getName();
                break;
            default:
                throw new PushException("Unknown action: " + result.action());
        }

        outputBuilder.append(prefix).
                append(lastElement ? "└── " : "├── ").
                append(String.format(contentFormat, contentName)).
                append("\n");

        return outputBuilder;
    }

}
