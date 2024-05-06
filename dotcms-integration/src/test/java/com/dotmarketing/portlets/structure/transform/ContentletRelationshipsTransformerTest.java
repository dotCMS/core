package com.dotmarketing.portlets.structure.transform;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ContentletRelationshipsTransformerTest {

    private static FieldAPI fieldAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user      = APILocator.getUserAPI().getSystemUser();
        fieldAPI  = APILocator.getContentTypeFieldAPI();

        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createRelationshipField(final String fieldName, final String parentContentTypeID, final String childContentTypeVariable)
            throws DotDataException, DotSecurityException {
        final Field field = FieldBuilder.builder(RelationshipField.class)
                .name(fieldName)
                .contentTypeId(parentContentTypeID)
                .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .indexed(true)
                .listed(false)
                .relationType(childContentTypeVariable)
                .build();

        return fieldAPI.save(field, user);
    }

    private com.dotcms.contenttype.model.field.Field createTextField(String name, String contentTypeId)
            throws DotSecurityException, DotDataException {
        com.dotcms.contenttype.model.field.Field field =  ImmutableTextField.builder()
                .name(name)
                .contentTypeId(contentTypeId)
                .dataType(DataTypes.TEXT)
                .build();

        return fieldAPI.save(field, user);
    }

    @Test
    public void testHasParentTrue_OneSideRelationship_selfRelatedContentType() throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            parentContentType = createContentType("parentContentType");

            //Create Relationship Field and Text Fields
            createRelationshipField("testRelationship", parentContentType.id(), parentContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString, parentContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet 2").nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet 3").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship, CollectionsUtils.list(contentletChild1, contentletChild2, contentletChild3));

            //Transform contentletRelationships
            ContentletRelationships relationshipsData = new ContentletRelationshipsTransformer(contentletParent, relationshipListMap).findFirst();

            //Check that contentletRelationship is not null and the hasParent is True
            assertNotNull(relationshipsData);
            assertTrue(relationshipsData.getRelationshipsRecords().get(0).isHasParent());

        }finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void testHasParentFalse_BothSidesRelationship_selfRelatedContentType() throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            parentContentType = createContentType("parentContentType");

            //Create Relationship Field and Text Fields
            final Field parentRelationshipField = createRelationshipField("testRelationship", parentContentType.id(), parentContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString, parentContentType.id());

            //Create Relationship Field on Child
            createRelationshipField("testRelationship",parentContentType.id(),parentContentType.variable()+"."+parentRelationshipField.variable());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet 2").nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString, "child Contentlet 3").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship, CollectionsUtils.list(contentletChild1, contentletChild2, contentletChild3));

            //Transform contentletRelationships
            ContentletRelationships relationshipsData = new ContentletRelationshipsTransformer(contentletParent, relationshipListMap).findFirst();

            //Check that contentletRelationship is not null and the hasParent is False
            assertNotNull(relationshipsData);
            assertTrue(relationshipsData.getRelationshipsRecords().get(0).isHasParent());

        }finally {
            try {
                if (parentContentType != null) {
                    contentTypeAPI.delete(parentContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
