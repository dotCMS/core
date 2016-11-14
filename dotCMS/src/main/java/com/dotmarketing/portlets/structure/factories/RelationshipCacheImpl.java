package com.dotmarketing.portlets.structure.factories;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Relationship;


public class RelationshipCacheImpl extends RelationshipCache {

	private DotCacheAdministrator cache;
	
	private String primaryGroup = "RelationshipCacheByInode";
	private String secondaryGroup = "RelationshipCacheByName";

	// region's name for the cache
    private String[] groupNames = {primaryGroup, secondaryGroup};

	public RelationshipCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}
	
	public Relationship getRelationshipByInode(String inode) throws DotCacheException {
		return (Relationship) cache.get(String.valueOf(inode), primaryGroup);
	}

	public Relationship getRelationshipByName(String name) throws DotCacheException {
		return (Relationship) cache.get(name, secondaryGroup);
	}

	public void putRelationshipByInode(Relationship rel) {
		cache.put(String.valueOf(rel.getInode()), rel, primaryGroup);
		cache.put(String.valueOf(rel.getRelationTypeValue()), rel, secondaryGroup);
	}
	
	public void removeRelationshipByInode(Relationship rel){
		cache.remove(String.valueOf(rel.getInode()), primaryGroup);
		cache.remove(String.valueOf(rel.getRelationTypeValue()), secondaryGroup);
	}
	@Override
	public List<Relationship> getRelationshipsByStruct(ContentTypeIf struct) throws DotCacheException {
		
		return (List<Relationship>) cache.get("STRUCT" + struct.id(), primaryGroup);
		
	}
	@Override
	public List<Relationship> getRelationshipsByType(ContentTypeIf type) throws DotCacheException {
		
		return (List<Relationship>) cache.get("STRUCT" + type.id(), primaryGroup);
		
	}
	@Override
	public void putRelationshipsByStruct(ContentTypeIf struct, List<Relationship> rels)  {
		
		cache.put("STRUCT" + struct.id(), ImmutableList.copyOf(rels), primaryGroup);
	}
	@Override
	public void putRelationshipsByType(ContentTypeIf type, List<Relationship> rels)  {
		
		cache.put("STRUCT" + type.id(), ImmutableList.copyOf(rels), primaryGroup);
	}
	@Override
	public void removeRelationshipsByStruct(ContentTypeIf struct)  {
		cache.remove("STRUCT" + struct.id(), primaryGroup);
	}
	@Override
	public void removeRelationshipsByType(ContentTypeIf type)  {
		cache.remove("STRUCT" + type.id(), primaryGroup);
	}
	
	@Override
	public void clearCache() {
		for(String g : groupNames)
			cache.flushGroup(g);
	}

	public String[] getGroups() {
		return groupNames;
	}

	public String getPrimaryGroup() {
		return primaryGroup;
	}

}
