package com.dotcms.contenttype.model.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Workflow model object
 */
@Value.Immutable
@JsonSerialize(as = ImmutableWorkflow.class)
@JsonDeserialize(as = ImmutableWorkflow.class)
public interface Workflow {

    @JsonIgnore
    @Value.Default
    default Boolean archived() {
        return false;
    }

    /**
     * The creationDate attribute is marked as auxiliary to exclude it from the equals, hashCode,
     * and toString methods. This ensures that two instances of Workflow can be considered equal
     * even if their creationDate values differ. This decision was made because under certain
     * circumstances, the creationDate value is set using the current date.
     */
    @JsonIgnore
    @Value.Auxiliary
    @Nullable
    Date creationDate();

    @JsonIgnore
    @Value.Default
    default Boolean defaultScheme() {
        return false;
    }

    @JsonIgnore
    @Value.Default
    default String description() {
        return "";
    }

    @JsonIgnore
    @Nullable
    String entryActionId();

    @Nullable
    String id();

    @JsonIgnore
    @Value.Default
    default Boolean mandatory() {
        return false;
    }

    @JsonIgnore
    @Nullable
    Date modDate();

    @JsonIgnore
    @Nullable
    String name();

    @Nullable
    String variableName();

    @JsonIgnore
    @Value.Default
    default Boolean system() {
        return false;
    }

}
