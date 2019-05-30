package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.workflow.WorkflowSchemeImportExportObjectView;
import com.dotmarketing.beans.Permission;

import java.util.List;


public class WorkflowSchemeImportObjectForm extends Validated {

    @NotNull
    private final WorkflowSchemeImportExportObjectView workflowImportObject;
    private final List<Permission>                 permissions;

    @JsonCreator
    public WorkflowSchemeImportObjectForm(
            @JsonProperty("workflowObject") final WorkflowSchemeImportExportObjectView workflowImportObject,
            @JsonProperty("permissions")          final List<Permission> permissions) {

        this.workflowImportObject = workflowImportObject;
        this.permissions          = permissions;
        this.checkValid();
    }

    public WorkflowSchemeImportExportObjectView getWorkflowImportObject() {
        return workflowImportObject;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }
}
