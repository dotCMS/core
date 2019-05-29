package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.model.type.ContentType;

/**
 * @author nollymar
 */
public interface RelationshipConstructionStrategy {
    void apply(final Relationship relationship);
}
