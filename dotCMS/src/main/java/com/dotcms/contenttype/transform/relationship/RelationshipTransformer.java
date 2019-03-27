package com.dotcms.contenttype.transform.relationship;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.List;

/**
 * Database transformer to handle objects returned from native sql queries over relationship table
 */
public interface RelationshipTransformer extends DBTransformer<Relationship> {

	List<Relationship> asList();

	Relationship from();
}
