package com.dotcms.publisher.pusher.wrapper;

import java.util.List;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;

public class StructureWrapper {
	private Structure structure;
	private List<Field> fields;
	private String workflowSchemaId;
	private String workflowSchemaName;
	private Operation operation;
	
	public StructureWrapper() {}
	
	public StructureWrapper(Structure structure, List<Field> fields) {
		this.structure = structure;
		this.fields = fields;
	}

	public String getWorkflowSchemaName() {
        return workflowSchemaName;
    }

    public void setWorkflowSchemaName(String workflowSchemaName) {
        this.workflowSchemaName = workflowSchemaName;
    }

    public Structure getStructure() {
		return structure;
	}

	public void setStructure(Structure structure) {
		this.structure = structure;
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
