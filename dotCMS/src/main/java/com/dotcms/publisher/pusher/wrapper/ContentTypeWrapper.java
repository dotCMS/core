package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class ContentTypeWrapper {
	private ContentType contentType;
	private List<Field> fields;
	private List<String> workflowSchemaIds;
	private List<String> workflowSchemaNames;
	private Operation operation;
	private List<FieldVariable> fieldVariables;
    private List<SystemActionWorkflowActionMapping> systemActionMappings;

    public ContentTypeWrapper() {}
	
	public ContentTypeWrapper(ContentType contentType, List<Field> fields, List<FieldVariable> variables) {
		this.contentType = contentType;
		this.fields = fields;
		this.fieldVariables = variables;
	}
	
	public List<FieldVariable> getFieldVariables() {
        return fieldVariables;
    }

    public void setFieldVariables(List<FieldVariable> fieldVariables) {
        this.fieldVariables = fieldVariables;
    }

	public List<String> getWorkflowSchemaNames() {
        return workflowSchemaNames;
    }

    public void setWorkflowSchemaNames(List<String> workflowSchemaNames) {
        this.workflowSchemaNames = workflowSchemaNames instanceof ImmutableList?
                workflowSchemaNames:ImmutableList.<String>builder().addAll(workflowSchemaNames).build();
    }

    public ContentType getContentType() {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

    public Operation getOperation() {		
        return operation;		
    }		
 		
    public void setOperation(Operation operation) {		
        this.operation = operation;		
    }

    public List<String> getWorkflowSchemaIds() {
        return workflowSchemaIds;
    }

    public void setWorkflowSchemaIds(List<String> workflowSchemaIds) {
        this.workflowSchemaIds = workflowSchemaIds instanceof ImmutableList?
                workflowSchemaIds:ImmutableList.<String>builder().addAll(workflowSchemaIds).build();
    }

    public List<SystemActionWorkflowActionMapping> getSystemActionMappings() {
        return systemActionMappings;
    }

    public void setSystemActionMappings(final List<SystemActionWorkflowActionMapping> systemActionMappings) {
        this.systemActionMappings = systemActionMappings;
    }
}
