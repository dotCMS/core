package com.dotcms.publisher.pusher.wrapper;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.publishing.PublisherConfig.Operation;

public class ContentTypeWrapper {
	private ContentType contentType;
	private List<Field> fields;
	private String workflowSchemaId;
	private String workflowSchemaName;
	private Operation operation;
	private List<FieldVariable> fieldVariables;
	
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

	public String getWorkflowSchemaName() {
        return workflowSchemaName;
    }

    public void setWorkflowSchemaName(String workflowSchemaName) {
        this.workflowSchemaName = workflowSchemaName;
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

    public String getWorkflowSchemaId() {
        return workflowSchemaId;
    }

    public void setWorkflowSchemaId(String workflowSchemaId) {
        this.workflowSchemaId = workflowSchemaId;
    }
    
}
