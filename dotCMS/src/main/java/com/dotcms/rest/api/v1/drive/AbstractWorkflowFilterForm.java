package com.dotcms.rest.api.v1.drive;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * A single workflow filter entry for {@link AbstractDriveRequestForm#workflow()}: one workflow
 * scheme, optionally pinned to a single step.
 * <ul>
 *   <li>{@code scheme} only — content governed by that scheme (matched by content-type
 *   assignment, so never-actioned content still appears).</li>
 *   <li>{@code scheme} + {@code step} — content whose current workflow task is at that step.</li>
 * </ul>
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = WorkflowFilterForm.class)
@JsonDeserialize(as = WorkflowFilterForm.class)
public interface AbstractWorkflowFilterForm {

    /**
     * Workflow scheme id (required per entry).
     *
     * @return the workflow scheme identifier
     */
    @JsonProperty("scheme")
    String scheme();

    /**
     * Optional workflow step id. When set, filters to content whose current task is at this
     * step; when null, the whole scheme is matched.
     *
     * @return the workflow step identifier, or null for "all steps of the scheme"
     */
    @Nullable
    @JsonProperty("step")
    String step();
}
