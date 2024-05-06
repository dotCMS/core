package com.dotcms.contenttype.model.workflow;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Workflow action mapping
 */
@Value.Immutable
@JsonSerialize(as = ImmutableActionMapping.class)
@JsonDeserialize(as = ImmutableActionMapping.class)
public interface ActionMapping {

    String identifier();

    @Nullable
    String systemAction();

    @Nullable
    WorkflowAction workflowAction();

    // the owner could be a ContentType or WorkflowScheme
    @Nullable
    Object owner();

    @Nullable
    Boolean ownerContentType();

    @Nullable
    Boolean ownerScheme();

}
