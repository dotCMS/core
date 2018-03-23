package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.test.ContentTypeBaseTest;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.structure.model.Relationship;

import com.dotmarketing.util.Logger;
import java.util.List;

import org.junit.Test;

public class RelationshipFactoryImplTest extends ContentTypeBaseTest{

    @Test
    public void testByTypeValue_RelationshipsShouldBeSameRegardlessCase_SameRelationship() throws DotHibernateException {
        RelationshipFactory relationshipFactory = FactoryLocator.getRelationshipFactory();
        List<Relationship> relationshipList = relationshipFactory.dbAll();
        for (Relationship relationship : relationshipList) {
            Relationship relationshipWithUpperCase = relationshipFactory.byTypeValue(relationship.getRelationTypeValue().toUpperCase());
            assertEquals(relationship,relationshipWithUpperCase);

        }
    }

}
