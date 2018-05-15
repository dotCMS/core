package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import org.junit.BeforeClass;
import org.junit.Test;

public class RelationshipAPITest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

    }

    /**
     * Test to Attempt to create a Duplicated Relationship (the system should prevent it)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test (expected = DotDataException.class)
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws DotDataException, DotSecurityException {

        Relationship relationship = null;
        Structure structure = null;

        try {
            structure = new Structure();
            structure.setFixed(false);
            structure.setVelocityVarName("IT-Structure-" + System.currentTimeMillis());
            structure.setName("IT-Structure-"+ System.currentTimeMillis());
            StructureFactory.saveStructure(structure);


            final String relationTypeValue = "IT-Parent-Child" + System.currentTimeMillis();

            relationship = new Relationship();
            relationship.setParentRelationName("Parent");
            relationship.setChildRelationName("Child");
            relationship.setRelationTypeValue(relationTypeValue);
            relationship.setParentStructureInode(structure.getInode());
            relationship.setChildStructureInode(structure.getInode());

            APILocator.getRelationshipAPI().save(relationship);
            relationship = APILocator.getRelationshipAPI().byTypeValue(relationTypeValue);

            Relationship duplicated = new Relationship();
            duplicated.setParentRelationName("ParentDuplicated");
            duplicated.setChildRelationName("ChildDuplicated");
            duplicated.setRelationTypeValue(relationTypeValue);
            duplicated.setParentStructureInode(structure.getInode());
            duplicated.setChildStructureInode(structure.getInode());

            //An exception will be thrown because the relationship is duplicated
            //See method declaration, it is expecting this DotDataException
            APILocator.getRelationshipAPI().save(duplicated);

        } finally {
            //The current connection is useless after the Unique violation exception.
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
    }
}
