package com.dotcms.rest.api.v1.contenttype;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Represents a workflow entry found in the form
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = WorkflowFormEntry.class)
@JsonDeserialize(as = WorkflowFormEntry.class)
public interface AbstractWorkflowFormEntry {

    @Nullable
    String id();

    @Nullable
    String variableName();
}
