package com.dotcms.model.push;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.File;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * The AbstractPushAnalysisResult interface represents the base structure for the result of a push
 * analysis. This analysis provides information about the action to take, the server content, local
 * content, and local file.
 * <p>
 * This interface is used with PushAnalysisResult and it's value is immutable.
 *
 * @param <T> the generic type of the server content and local content objects.
 */
@ValueType
@Value.Immutable
@JsonDeserialize(as = PushAnalysisResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractPushAnalysisResult<T> {

    /**
     * Returns the action to take during the push.
     *
     * @return a PushAction object.
     */
    PushAction action();

    /**
     * Returns the server content information if available.
     *
     * @return an Optional of the generic type T.
     */
    Optional<T> serverContent();

    /**
     * Returns the local content information if available.
     *
     * @return an Optional of the generic type T.
     */
    Optional<T> localContent();

    /**
     * Returns the local file information if available.
     *
     * @return an Optional with a File object.
     */
    Optional<File> localFile();

}
