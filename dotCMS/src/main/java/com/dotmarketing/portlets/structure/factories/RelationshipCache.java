package com.dotmarketing.portlets.structure.factories;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.Map;
import java.util.function.BiFunction;


public abstract class RelationshipCache implements Cachable {
	
	// ### READ ###
	public abstract Relationship getRelationshipByInode(String inode) throws DotCacheException;

	public abstract Relationship getRelationshipByName(String name) throws DotCacheException;

	public abstract void putRelationshipByInode(Relationship rel);

	public abstract void removeRelationshipByInode(Relationship rel);
	
	abstract public void clearCache();

	public List<Relationship> getRelationshipsByStruct(ContentTypeIf struct) throws DotCacheException {
		// TODO Auto-generated method stub
		return null;
	}

	public void putRelationshipsByStruct(ContentTypeIf struct, List<Relationship> rels) throws DotCacheException {
		// TODO Auto-generated method stub
		
	}

	public void removeRelationshipsByStruct(ContentTypeIf struct) throws DotCacheException {
		// TODO Auto-generated method stub
		
	}

	public void putRelationshipsByType(ContentTypeIf type, List<Relationship> rels) {
		// TODO Auto-generated method stub
		
	}

	public void removeRelationshipsByType(ContentTypeIf type) {
		// TODO Auto-generated method stub
		
	}

	public List<Relationship> getRelationshipsByType(ContentTypeIf type) throws DotCacheException {
		// TODO Auto-generated method stub
		return null;
	}



    /**
     * Invalidates relationship cache for a given contentlet
     * @param contentletIdentifier
     */
    public abstract void removeRelatedContentMap(Contentlet contentlet);

    /**
     * Removes related content from cache given a contentlet identifier and the velocity var name of
     * the relationship field
     * @param contentletIdentifier
     * @param relationshipFieldVar
     * @throws DotCacheException
     */
    public abstract void putRelatedContent(Contentlet contentlet,
            String relationshipFieldVar, List<String> values);


    /**
     * Gets the list of related content from cache or returns null
     * @param contentletIdentifier
     * @param fieldOrType
     * @return
     */
    public abstract List<String> getRelatedContent(final Contentlet contentlet, final String fieldOrType);
}
