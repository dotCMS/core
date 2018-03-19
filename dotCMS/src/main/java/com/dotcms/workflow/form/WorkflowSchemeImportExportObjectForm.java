package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;

import java.util.List;


public class WorkflowSchemeImportExportObjectForm extends Validated {

    @NotNull
    private final WorkflowSchemeImportExportObject workflowExportObject;
    private final List<Permission>                 permissions;

    @JsonCreator
    public WorkflowSchemeImportExportObjectForm(
            @JsonProperty("workflowExportObject") final WorkflowSchemeImportExportObject workflowExportObject,
            @JsonProperty("permissions")          final List<Permission> permissions) {

        this.workflowExportObject = workflowExportObject;
        this.permissions = permissions;
        this.checkValid();
    }

    public WorkflowSchemeImportExportObject getWorkflowExportObject() {
        return workflowExportObject;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }
}
