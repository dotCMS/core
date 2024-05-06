package com.dotcms.api.client.push.task;

import com.dotcms.api.client.push.PushHandler;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.push.PushAnalysisResult;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Interface representing the parameters for the PushTask.
 *
 * @param <T> the type of content.
 */
@ValueType
@Value.Immutable
public interface AbstractPushTaskParams<T> extends Serializable {

    /**
     * Retrieves the list of push analysis to be processed.
     *
     * @return the list of PushAnalysisResult objects.
     */
    List<PushAnalysisResult<T>> results();

    /**
     * Retrieves the push handler to be used to push the content.
     *
     * @return the push handler.
     */
    PushHandler<T> pushHandler();

    /**
     * Retrieves the custom push options.
     *
     * @return the custom options.
     */
    Map<String, Object> customOptions();

    /**
     * Retrieves whether the remove operation is allowed or not.
     *
     * @return true if remove operation is allowed, false otherwise.
     */
    boolean allowRemove();

    /**
     * Retrieves whether the push operation should fail fast or continue on error.
     */
    boolean failFast();

    /**
     * Disables the auto update feature.
     *
     * @return true if auto update is disabled, false otherwise.
     */
    boolean disableAutoUpdate();

    /**
     * Retrieves the progress bar for the push operation.
     *
     * @return the progress bar.
     */
    ConsoleProgressBar progressBar();

}
