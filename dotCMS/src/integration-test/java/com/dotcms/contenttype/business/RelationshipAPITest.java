package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import javax.management.relation.Relation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RelationshipAPITest extends IntegrationTestBase {

    private static Relationship relationship = null;
    private static Structure structure = null;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        structure = new Structure();
        structure.setFixed(false);
        structure.setVelocityVarName("IT-Structure-" + System.currentTimeMillis());
        structure.setName("IT-Structure-"+ System.currentTimeMillis());
        StructureFactory.saveStructure(structure);

        relationship = new Relationship();
        relationship.setParentRelationName("Parent");
        relationship.setChildRelationName("Child");
        relationship.setRelationTypeValue("IT-Parent-Child" + System.currentTimeMillis());
        relationship.setParentStructureInode(structure.getInode());
        relationship.setChildStructureInode(structure.getInode());
        APILocator.getRelationshipAPI().create(relationship);
    }

    @AfterClass
    public static void cleanUp () throws DotDataException {
        //The current connection is useless after the Test that throws exceptions.
        //Need to close it, so the API opens a new one when cleaning up
        DbConnectionFactory.closeConnection();

        if (relationship != null) {
            APILocator.getRelationshipAPI().delete(relationship);
        }
        //Clean up
        if (structure != null) {
            StructureFactory.deleteStructure(structure);
        }
    }

    /**
     * Test to Attempt to create a Duplicated Relationship (the system should prevent it)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test (expected = DotDataException.class)
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws DotDataException, DotSecurityException {

        Relationship duplicated = new Relationship();
        duplicated.setParentRelationName("ParentDuplicated");
        duplicated.setChildRelationName("ChildDuplicated");
        duplicated.setParentStructureInode(structure.getInode());
        duplicated.setChildStructureInode(structure.getInode());
        duplicated.setRelationTypeValue(relationship.getRelationTypeValue()); //Use existing RelationTypeValue

        //An exception will be thrown because the relationship is duplicated
        //See method declaration, it is expecting this DotDataException
        APILocator.getRelationshipAPI().create(duplicated);

    }

    /**
     * Test to Attempt to Update Relationship but modifying the RelationTypeValue (the system should prevent it)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test (expected = DotValidationException.class)
    public void testSave_RelationshipWithModifiedRelationTypeValue_ShouldThrowException () throws DotDataException, DotSecurityException {

        Relationship modified = new Relationship();
        modified.setParentRelationName("ParentDuplicated");
        modified.setChildRelationName("ChildDuplicated");
        modified.setParentStructureInode(structure.getInode());
        modified.setChildStructureInode(structure.getInode());
        modified.setRelationTypeValue("Modified Relation Type Value");

        //An exception will be thrown because the relationship modified the relationTypeValue
        //See method declaration, it is expecting this DotValidationException
        APILocator.getRelationshipAPI().save(modified, relationship.getInode());

    }
}
