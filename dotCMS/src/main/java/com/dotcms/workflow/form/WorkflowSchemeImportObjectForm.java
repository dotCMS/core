package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.workflow.WorkflowSchemeImportExportObjectView;
import com.dotmarketing.beans.Permission;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;


public class WorkflowSchemeImportObjectForm extends Validated {

    @NotNull
    private final WorkflowSchemeImportExportObjectView workflowObject;
    private final List<Permission>                 permissions;

    @JsonCreator
    public WorkflowSchemeImportObjectForm(
            @JsonProperty("workflowObject") final WorkflowSchemeImportExportObjectView workflowObject,
            @JsonProperty("permissions")          final List<Permission> permissions) {

        this.workflowObject = workflowObject;
        this.permissions          = permissions;
        this.checkValid();
    }

    @Hidden // hides method from Swagger UI on the API Playground, to avoid cluttering its schema
    public WorkflowSchemeImportExportObjectView getWorkflowImportObject() {
        return workflowObject;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }
}
