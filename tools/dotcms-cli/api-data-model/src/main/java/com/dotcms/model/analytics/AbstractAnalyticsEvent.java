package com.dotcms.model.analytics;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

/**
 * Represents an abstract analytics event with details about the event type, related command, user,
 * and associated site information.
 * <p>
 * The implementing class is expected to provide the immutable structure for storing and accessing
 * these attributes consistently, making it suitable for serialization and analytics processing.
 * <p>
 * This interface is annotated for compatibility with JSON serialization frameworks and enforces
 * immutability for ensuring thread-safety and data integrity.
 */
@ValueType
@Value.Immutable
@JsonDeserialize(as = AnalyticsEvent.class)
public interface AbstractAnalyticsEvent {

    /**
     * Retrieves the type of the event. This method returns the value of the {@code event_type}
     * attribute, which identifies the category or nature of the analytics event.
     *
     * @return A non-null String representing the event type.
     */
    @JsonProperty("event_type")
    String eventType();

    /**
     * Retrieves the command associated with a specific analytics event.
     *
     * @return The value of the {@code command} attribute, which typically represents the operation
     * or action tied to the event.
     */
    String command();

    /**
     * Retrieves the list of command arguments associated with the executed command.
     *
     * @return An immutable list of strings representing the arguments used for the command.
     */
    List<String> arguments();

}