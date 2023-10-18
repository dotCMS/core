package com.dotcms.contenttype.model.workflow;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Workflow model object
 */
@Value.Immutable
@JsonSerialize(as = ImmutableWorkflow.class)
@JsonDeserialize(as = ImmutableWorkflow.class)
public interface Workflow {

    @Value.Default
    default Boolean archived() {
        return false;
    }

    @Nullable
    Date creationDate();

    @Value.Default
    default Boolean defaultScheme() {
        return false;
    }

    @Value.Default
    default String description() {
        return "";
    }

    @Nullable
    String entryActionId();

    @Nullable
    String id();

    @Value.Default
    default Boolean mandatory() {
        return false;
    }

    @Nullable
    Date modDate();

    @Nullable
    String name();

    @Value.Default
    default Boolean system() {
        return false;
    }

}
