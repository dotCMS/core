package com.dotmarketing.portlets.structure.factories;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;

public abstract class RelationshipCache implements Cachable {
	
	// ### READ ###
	public abstract Relationship getRelationshipByInode(String inode) throws DotCacheException;

	public abstract Relationship getRelationshipByName(String name) throws DotCacheException;

	public abstract void putRelationshipByInode(Relationship rel);

	public abstract void removeRelationshipByInode(Relationship rel);
	
	abstract public void clearCache();

	public List<Relationship> getRelationshipsByStruct(Structure struct) throws DotCacheException {
		// TODO Auto-generated method stub
		return null;
	}

	public void putRelationshipsByStruct(Structure struct, List<Relationship> rels) throws DotCacheException {
		// TODO Auto-generated method stub
		
	}

	public void removeRelationshipsByStruct(Structure struct) throws DotCacheException {
		// TODO Auto-generated method stub
		
	}

	public void putRelationshipsByType(ContentType type, List<Relationship> rels) {
		// TODO Auto-generated method stub
		
	}

	public void removeRelationshipsByType(ContentType type) {
		// TODO Auto-generated method stub
		
	}

	public List<Relationship> getRelationshipsByType(ContentType type) throws DotCacheException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
