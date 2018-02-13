package com.dotcms.contenttype.business;

import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.test.ContentTypeBaseTest;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.structure.model.Relationship;

import java.util.List;

import org.junit.Test;

public class RelationshipFactoryImplTest extends ContentTypeBaseTest{

    @Test
    public void testByTypeValue() throws DotHibernateException {
        RelationshipFactory fac = FactoryLocator.getRelationshipFactory();
        List<Relationship> rels = fac.dbAll();

        for (Relationship ship : rels) {
            Relationship newRel = fac.byTypeValue(ship.getRelationTypeValue().toUpperCase());
            assertTrue("Relationship by type value should be case insensitive:" , ship.equals(newRel));


        }



    }

}
