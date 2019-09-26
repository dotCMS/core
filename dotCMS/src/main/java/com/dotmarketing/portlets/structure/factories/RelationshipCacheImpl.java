package com.dotmarketing.portlets.structure.factories;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;


public class RelationshipCacheImpl extends RelationshipCache {

	private DotCacheAdministrator cache;
	
	private final String primaryGroup = "RelationshipCacheByInode";
	private final String secondaryGroup = "RelationshipCacheByName";
    private final String tertiaryGroup = "RelatedContentCache";

	// region's name for the cache
    private final String[] groupNames = {primaryGroup, secondaryGroup, tertiaryGroup};

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

		return UtilMethods.isSet(struct) ? (List<Relationship>) cache
				.get("STRUCT" + struct.id(), primaryGroup) : Collections.emptyList();
		
	}
	@Override
	public List<Relationship> getRelationshipsByType(ContentTypeIf type) throws DotCacheException {

		return UtilMethods.isSet(type) ? (List<Relationship>) cache
				.get("STRUCT" + type.id(), primaryGroup) : Collections.emptyList();
		
	}
	@Override
	public void putRelationshipsByStruct(ContentTypeIf struct, List<Relationship> rels)  {
		if (UtilMethods.isSet(struct)) {
			cache.put("STRUCT" + struct.id(), ImmutableList.copyOf(rels), primaryGroup);
		}
	}
	@Override
	public void putRelationshipsByType(ContentTypeIf type, List<Relationship> rels)  {
		if (UtilMethods.isSet(type)) {
			cache.put("STRUCT" + type.id(), ImmutableList.copyOf(rels), primaryGroup);
		}
	}
	@Override
	public void removeRelationshipsByStruct(ContentTypeIf struct)  {
		if (UtilMethods.isSet(struct)) {
			cache.remove("STRUCT" + struct.id(), primaryGroup);
		}
	}
	@Override
	public void removeRelationshipsByType(ContentTypeIf type)  {
		if (UtilMethods.isSet(type)) {
			cache.remove("STRUCT" + type.id(), primaryGroup);
		}
	}
	
	@Override
	public void clearCache() {
		for(String g : groupNames)
			cache.flushGroup(g);
	}

    @Override
    public List<String> getRelatedContent(final Contentlet contentlet, final String fieldOrType) {
        if (contentlet==null || UtilMethods.isEmpty(contentlet.getIdentifier()) || UtilMethods.isEmpty(fieldOrType)) {
            return null;
        }
        Map<String, List<String>> map = (Map<String, List<String>>) cache.getNoThrow(contentlet.getIdentifier(), tertiaryGroup);
        if (map == null) {
            return null;
        }
        return map.get(fieldOrType);
    }
    


    @Override
    public void removeRelatedContentMap(final Contentlet contentlet)  {
        if (contentlet!=null && UtilMethods.isSet(contentlet.getIdentifier())) {
            cache.remove(contentlet.getIdentifier(), tertiaryGroup);
        }
    }

    @Override
    public void putRelatedContent(final Contentlet contentlet, final String relationshipFieldVar, List<String> values) {
        if (contentlet==null && UtilMethods.isEmpty(contentlet.getIdentifier())) {
            return;
        }
        Map<String,List<String>> map = (Map<String, List<String>>) cache
                        .getNoThrow(contentlet.getIdentifier(), tertiaryGroup);
        map = (map==null) ? new ConcurrentHashMap<>() : map;

        if(map.containsKey(relationshipFieldVar)) {
            throw new DotStateException("Content Relationship Map " + contentlet.getIdentifier() + "  already has values for:" + relationshipFieldVar + " and should be invalidated before adding");
        }
        map.put(relationshipFieldVar, values);
        cache.put(contentlet.getIdentifier(), map, tertiaryGroup);
    }

	public String[] getGroups() {
		return groupNames;
	}

	public String getPrimaryGroup() {
		return primaryGroup;
	}


	

	
	
	
	

}
