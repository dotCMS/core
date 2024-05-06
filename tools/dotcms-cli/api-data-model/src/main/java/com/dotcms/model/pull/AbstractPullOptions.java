package com.dotcms.model.pull;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * This class represents the options for a pull operation.
 *
 * <p>
 * This class is immutable and follows the Value Object pattern. It provides getter methods for
 * accessing the options.
 * </p>
 *
 * <p>
 * Use {@link PullOptions} to create an instance of this class.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * PullOptions options = PullOptions.builder()
 *                      .contentKey("demo.dotcms.com")
 *                      .outputFormat(InputOutputFormat.YAML.toString())
 *                      .isShortOutput(true)
 *                      .build();
 * }</pre>
 * </p>
 */
@ValueType
@Value.Immutable
@JsonDeserialize(as = PullOptions.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractPullOptions {

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
    Optional<String> contentKey();

    /**
     * Retrieves the output format used for displaying the content. If no output format is set, the
     * default output format will be used.
     *
     * @return an Optional containing the output format, or an empty Optional if no output format is
     * set.
     */
    Optional<String> outputFormat();

    /**
     * Retrieves whether the pulled content should be printed to the console or stored to disk.
     */
    boolean isShortOutput();

    /**
     * Retrieves whether the pull operation should fail fast or continue on error.
     */
    boolean failFast();

    /**
     * Number of retry attempts on errors.
     */
    int maxRetryAttempts();

    /**
     * Retrieves the custom options a pull command can use to customize the pull operation.
     *
     * @return an Optional containing the custom options, or an empty Optional if no custom options
     * are set.
     */
    Optional<Map<String, Object>> customOptions();

}
