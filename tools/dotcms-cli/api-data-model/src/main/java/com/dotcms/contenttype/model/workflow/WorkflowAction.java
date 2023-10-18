package com.dotcms.contenttype.model.workflow;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Workflow action
 */
@Value.Immutable
@JsonSerialize(as = ImmutableWorkflowAction.class)
@JsonDeserialize(as = ImmutableWorkflowAction.class)
public interface WorkflowAction {

    @Nullable
    Boolean assignable();

    @Nullable
    Boolean commentable();

    @Nullable
    String condition();

    @Nullable
    String icon();

    String name();

    String id();

    String nextAssign();
    String nextStep();

    Boolean nextStepCurrentStep();

    Integer order();

    @Nullable
    Boolean roleHierarchyForAssign();

    @Nullable
    String schemeId();

    @Nullable
    Object owner();

    @Nullable
    List<String> showOn();

}
