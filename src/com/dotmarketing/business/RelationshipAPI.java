package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;

public interface RelationshipAPI {

	public void addRelationship(String parent,String child, String relationType) throws DotDataException;
}
