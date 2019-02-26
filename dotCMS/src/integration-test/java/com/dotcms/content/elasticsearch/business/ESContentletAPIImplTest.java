package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY;
import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nollymar 
 */
@RunWith(DataProviderRunner.class)
public class ESContentletAPIImplTest {

    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;
    private static UserAPI userAPI;

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        fieldAPI        = APILocator.getContentTypeFieldAPI();
        relationshipAPI = APILocator.getRelationshipAPI();

        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentTypeAPI = APILocator.getContentTypeAPI(user);

    }

    public static class DeleteRelatedTestCase {

        boolean legacyRelationship;
        boolean oneSidedRelationship;
        boolean hasParent;
        boolean isDeleteNeeded;

        public DeleteRelatedTestCase(final boolean legacyRelationship,
                final boolean oneSidedRelationship, final boolean hasParent, final boolean isDeleteNeeded) {
            this.legacyRelationship   = legacyRelationship;
            this.oneSidedRelationship = oneSidedRelationship;

            this.hasParent      = hasParent;
            this.isDeleteNeeded = isDeleteNeeded;
        }
    }

    @DataProvider
    public static Object[] deleteRelatedTestCases() {
        return new DeleteRelatedTestCase[]{
                new DeleteRelatedTestCase(true, false, true, true),
                new DeleteRelatedTestCase(true, false, false, true),
                new DeleteRelatedTestCase(false, false, true, true),
                new DeleteRelatedTestCase(false, false, false, true),
                new DeleteRelatedTestCase(false, true, true, true),
                new DeleteRelatedTestCase(false, true, false, false),
        };
    }

    @Test
    @UseDataProvider("deleteRelatedTestCases")
    public void isDeleteRelatedContentNeeded(DeleteRelatedTestCase testCase) throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        final String cardinality = String.valueOf(ONE_TO_MANY.ordinal());

        ContentType childContentType = null;
        ContentType parentContentType = null;

        try {
            parentContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());

            childContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("childContentType" + time)
                            .owner(user.getUserId()).build());

            final Relationship relationship;
            if (testCase.legacyRelationship){
                relationship = new Relationship(
                        new StructureTransformer(parentContentType).asStructure(),
                        new StructureTransformer(childContentType).asStructure(),
                        parentContentType.variable(), childContentType.variable(),
                        MANY_TO_MANY.ordinal(), false, false);
            } else{
                com.dotcms.contenttype.model.field.Field firstField = FieldBuilder
                        .builder(RelationshipField.class)
                        .name("firstField").variable("firstField")
                        .contentTypeId(parentContentType.id()).values(cardinality)
                        .relationType(childContentType.variable()).build();

                firstField = fieldAPI.save(firstField, user);

                if (!testCase.oneSidedRelationship){
                    com.dotcms.contenttype.model.field.Field secondField = FieldBuilder.builder(RelationshipField.class)
                            .name("secondField").variable("secondField")
                            .contentTypeId(childContentType.id()).values(cardinality)
                            .relationType(parentContentType.variable() + StringPool.PERIOD
                                    + firstField.variable()).build();

                    fieldAPI.save(secondField, user);
                }

                relationship = relationshipAPI
                        .getRelationshipFromField(firstField, user);
            }

            //gets content types with their relationship fields
            parentContentType = contentTypeAPI.find(parentContentType.id());
            childContentType  = contentTypeAPI.find(childContentType.id());

            assertEquals(testCase.isDeleteNeeded, new ESContentletAPIImpl()
                    .isDeleteRelatedContentNeeded(testCase.hasParent,
                            testCase.hasParent ? parentContentType : childContentType,
                            relationship));


        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }
}
