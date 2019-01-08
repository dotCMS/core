package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import javax.management.relation.Relation;

import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RelationshipAPITest extends IntegrationTestBase {

    private static Relationship relationship = null;
    private static Structure structure = null;
    private static User user = null;

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
        user = APILocator.systemUser();
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

    @Test
    public void testconvertRelationshipToRelationshipField_Success() throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
            childContentType = createContentType("childContentType" + System.currentTimeMillis());

            //Create Text Fields
            final String titleFieldString = "title";
            Field field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(parentContentType.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(field, user);

            field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(childContentType.id())
                    .build();
            APILocator.getContentTypeFieldAPI().save(field, user);

            //Create an old Relationship
            final Structure parentStructure = new StructureTransformer(parentContentType).asStructure();
            final Structure childStructure = new StructureTransformer(childContentType).asStructure();
            APILocator.getRelationshipAPI().save(new Relationship(parentStructure,childStructure,parentContentType.name(),
                    childContentType.name(),0,false,false));
            final Relationship relationship = APILocator.getRelationshipAPI().byParent(parentStructure).get(0);

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id())
                    .setProperty(titleFieldString,"parent Contentlet").next();
            Contentlet contentletParent2 = new ContentletDataGen(parentContentType.id())
                    .setProperty(titleFieldString,"parent Contentlet 2").next();
            final Contentlet contentletChild = new ContentletDataGen(childContentType.id()).setProperty(titleFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(titleFieldString,"child Contentlet 2").nextPersisted();

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship, CollectionsUtils.list(contentletChild,contentletChild2));

            //Checkin of the parent to validate Relationships
            contentletParent = APILocator.getContentletAPI().checkin(contentletParent,relationshipListMap,user,false);

            //List Related Contentlets
            List<Contentlet> relatedContent = APILocator.getContentletAPI().getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());

            //Migrate Relationship
            APILocator.getRelationshipAPI().convertRelationshipToRelationshipField(relationship);

            //Check old relationship does not exists
            assertNull(APILocator.getRelationshipAPI().byInode(relationship.getInode()));

            //Check Content is still related
            relatedContent = APILocator.getContentletAPI().getRelatedContent(contentletParent,relationship,user,false);
            assertNotNull(relatedContent);
            assertEquals(2,relatedContent.size());
            assertEquals(contentletChild.getIdentifier(),relatedContent.get(0).getIdentifier());
            assertEquals(contentletChild2.getIdentifier(),relatedContent.get(1).getIdentifier());

        }finally {
            if(parentContentType != null){
                APILocator.getContentTypeAPI(user).delete(parentContentType);
            }
            if(childContentType != null){
                APILocator.getContentTypeAPI(user).delete(childContentType);
            }
        }
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return APILocator.getContentTypeAPI(user).save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }
}
