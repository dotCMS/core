package com.dotcms.api.client.pull.task;

import com.dotcms.api.client.pull.GeneralPullHandler;
import com.dotcms.cli.common.ConsoleProgressBar;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.model.annotation.ValueType;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;

/**
 * The AbstractPullTaskParams class is used to compile all the parameters shared by various Pull
 * APIs.
 */
@ValueType
@Value.Immutable
public interface AbstractPullTaskParams<T> extends Serializable {

    /**
     * Retrieves the destination for the pulled content.
     */
    File destination();

    /**
     * Retrieves a content key used to pull a specific content. If no content key is set, then all
     * the contents are pulled.
     *
     * @return an Optional containing the content key, or an empty Optional if no content key is
     * set.
     */
    List<T> contents();

    /**
     * Retrieves the PullHandler object associated with this instance.
     *
     * @return the PullHandler object.
     */
    GeneralPullHandler<T> pullHandler();

    /**
     * Retrieves the output format used for displaying the content. If no output format is set, the
     * default output format will be used.
     *
     * @return an Optional containing the output format, or an empty Optional if no output format is
     * set.
     */
    InputOutputFormat format();

    /**
     * Retrieves the output options for the pull operation.
     *
     * @return the output options.
     */
    OutputOptionMixin output();

    /**
     * Retrieves the progress bar for the pull operation.
     *
     * @return the progress bar.
     */
    ConsoleProgressBar progressBar();

    /**
     * Retrieves whether the pull operation should fail fast or continue on error.
     */
    boolean failFast();

}
