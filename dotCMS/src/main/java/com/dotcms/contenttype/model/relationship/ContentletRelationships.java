package com.dotcms.contenttype.model.relationship;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

@Value.Immutable
public abstract class ContentletRelationships implements Serializable {
	

	

	private static final long serialVersionUID = 1L;
	public abstract Contentlet getContentlet();
	public abstract List<ContentletRelationshipRecords> getRelationshipsRecords();
	

	
	
}
