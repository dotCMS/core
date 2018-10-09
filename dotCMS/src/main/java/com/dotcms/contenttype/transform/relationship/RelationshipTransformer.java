package com.dotcms.contenttype.transform.relationship;

import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.List;

/**
 * Database transformer to handle objects returned from native sql queries over relationship table
 */
public interface RelationshipTransformer {

	List<Relationship> asList();

	Relationship from();
}
