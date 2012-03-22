package com.dotmarketing.portlets.structure.factories;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Relationship;

public abstract class RelationshipCache implements Cachable {
	
	// ### READ ###
	public abstract Relationship getRelationshipByInode(String inode) throws DotCacheException;

	public abstract Relationship getRelationshipByName(String name) throws DotCacheException;

	public abstract void putRelationshipByInode(Relationship rel);

	public abstract void removeRelationshipByInode(Relationship rel);
	
	abstract public void clearCache();
	
}
