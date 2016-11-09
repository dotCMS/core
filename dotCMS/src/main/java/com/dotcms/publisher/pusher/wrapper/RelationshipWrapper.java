package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.structure.model.Relationship;

public class RelationshipWrapper {
	
	private Relationship relationship;
	private Operation operation;
	
	public RelationshipWrapper() {}
	
	public RelationshipWrapper(Relationship relationship, Operation operation) {
		this.relationship = relationship;
		this.operation = operation;
	}
	
	public Relationship getRelationship() {
		return relationship;
	}
	public void setRelationship(Relationship relationship) {
		this.relationship = relationship;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	
	
}
