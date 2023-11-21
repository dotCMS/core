package com.dotcms.model.pull;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
     * Retrieves a content key used to pull a specific content. If no content key is set, then all
     * the contents are pulled.
     *
     * @return an Optional containing the content key, or an empty Optional if no content key is
     * set.
     */
    Optional<String> contentKey();

    /**
     * Retrieves the output format for the pulled content (JSON/YAML).
     */
    String outputFormat();

    /**
     * Retrieves whether the pulled content should be printed to the console or stored to disk.
     */
    boolean isShortOutput();

}
