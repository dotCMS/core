package com.dotmarketing.portlets.structure.factories;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;

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
	public List<Relationship> getRelationshipsByStruct(Structure struct) throws DotCacheException {
		
		return (List<Relationship>) cache.get("STRUCT" + struct.getInode(), primaryGroup);
		
	}
	@Override
	public void putRelationshipsByStruct(Structure struct, List<Relationship> rels)  {
		cache.put("STRUCT" + struct.getInode(), rels, primaryGroup);
	}
	@Override
	public void removeRelationshipsByStruct(Structure struct)  {
		cache.remove("STRUCT" + struct.getInode(), primaryGroup);
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
