package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nollymar
 */

@RunWith(DataProviderRunner.class)
public class MapToContentletPopulatorTest extends IntegrationTestBase {

    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        fieldAPI = APILocator.getContentTypeFieldAPI();
    }

    public static class TestCase {
        String query;
        int relationshipsCount;
        int relatedContentCount;

        public TestCase(final String query, final int relationshipsCount, final int relatedContentCount) {
            this.query = query;

            this.relationshipsCount  = relationshipsCount;
            this.relatedContentCount = relatedContentCount;
        }
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                new TestCase("identifier", 1,1),
                new TestCase("", 1,0),
                new TestCase("null", 0,0),
                new TestCase(null, 0,0),
        };
    }

    @Test
    public void testPopulateLegacyRelationshipWithLuceneQueryAndIdentifier() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        final ContentType product = TestDataUtils.getProductLikeContentType();
        final ContentType youtube = TestDataUtils.getYoutubeLikeContentType();

        final Relationship relationship = TestDataUtils.relateContentTypes(product,youtube);
        final String name = relationship.getRelationTypeValue();

        Contentlet contentlet = createContentlet(youtube);

        try {
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, youtube.id());
            final String productYoutube = String.format("+%s.url:new-youtube-content, ", youtube.name()) + contentlet.getIdentifier();
            properties.put(name, productYoutube );

            contentlet = populator.populate(contentlet, properties);

            assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

            ContentletRelationships  resultMap = (ContentletRelationships) contentlet
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(1, resultMap.getRelationshipsRecords().size());

            ContentletRelationships.ContentletRelationshipRecords result = resultMap.getRelationshipsRecords().get(0);

            //validates the relationship
            assertEquals(name, result.getRelationship().getRelationTypeValue());

            //validates the contentlet
            assertEquals(1, result.getRecords().size());
            assertEquals(contentlet.getInode(), result.getRecords().get(0).getInode());
        } finally {
            if(contentlet != null && contentlet.getInode() != null){
                ContentletDataGen.remove(contentlet);
            }
        }
    }

    @UseDataProvider("testCases")
    @Test
    public void testPopulateOneSidedRelationship(final TestCase testCase) throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");
            Contentlet contentlet = createContentlet(parentContentType);

            Field field = createAndSaveRelationshipField("newRel", parentContentType.id(), childContentType.variable());
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, parentContentType.id());

            if (testCase.query != null && testCase.query.equals("identifier")){
                properties.put(field.variable(), contentlet.getIdentifier());
            }else{
                properties.put(field.variable(), testCase.query);
            }


            contentlet = populator.populate(contentlet, properties);

            if(testCase.relationshipsCount > 0) {

                assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

                final ContentletRelationships resultMap = (ContentletRelationships) contentlet
                        .get(Contentlet.RELATIONSHIP_KEY);

                assertEquals(testCase.relationshipsCount, resultMap.getRelationshipsRecords().size());
                final ContentletRelationships.ContentletRelationshipRecords result = resultMap.getRelationshipsRecords().get(0);

                //validates the relationship
                assertEquals(parentContentType.inode(), result.getRelationship().getParentStructureInode());

                //validates the contentlet
                assertEquals(testCase.relatedContentCount, result.getRecords().size());

                if(testCase.relatedContentCount > 0){
                    assertEquals(contentlet.getInode(), result.getRecords().get(0).getInode());
                }
            }else{
                assertNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));
            }
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testPopulateTwoSidedRelationshipWithLuceneQuery() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");
            Contentlet contentlet = createContentlet(childContentType);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar);
            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, childContentType.id());
            properties.put(childTypeRelationshipField.variable(), contentlet.getIdentifier());

            contentlet = populator.populate(contentlet, properties);

            assertNotNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));

            ContentletRelationships  resultMap = (ContentletRelationships) contentlet
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(1, resultMap.getRelationshipsRecords().size());

            ContentletRelationships.ContentletRelationshipRecords result = resultMap.getRelationshipsRecords().get(0);

            //validates the relationship
            assertEquals(childContentType.inode(), result.getRelationship().getChildStructureInode());

            //validates the contentlet
            assertEquals(1, result.getRecords().size());
            assertEquals(contentlet.getInode(), result.getRecords().get(0).getInode());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testPopulateSelfJoinedRelationshipWithLuceneQuery() throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");

            Contentlet parentContentlet = createContentlet(parentContentType);
            Contentlet childContentlet = createContentlet(parentContentType);
            Contentlet contentletToPopulate = createContentlet(parentContentType);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), parentContentType.variable());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    parentContentType.id(), fullFieldVar);


            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, parentContentType.id());
            properties.put(parentTypeRelationshipField.variable(), childContentlet.getIdentifier());
            properties.put(childTypeRelationshipField.variable(), parentContentlet.getIdentifier());

            contentletToPopulate = populator.populate(contentletToPopulate, properties);

            assertNotNull(contentletToPopulate.get(Contentlet.RELATIONSHIP_KEY));

            ContentletRelationships  resultMap = (ContentletRelationships) contentletToPopulate
                    .get(Contentlet.RELATIONSHIP_KEY);

            assertEquals(2, resultMap.getRelationshipsRecords().size());

            for(ContentletRelationships.ContentletRelationshipRecords result:resultMap.getRelationshipsRecords()){

                //validates the relationship
                assertEquals(parentContentType.id(), result.getRelationship().getParentStructureInode());
                assertEquals(parentContentType.id(), result.getRelationship().getChildStructureInode());

                //validates the contentlet
                assertEquals(1, result.getRecords().size());

                if (result.isHasParent()){
                    assertEquals(childContentlet.getInode(), result.getRecords().get(0).getInode());
                } else{
                    assertEquals(parentContentlet.getInode(), result.getRecords().get(0).getInode());
                }
            }
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void testPopulateContentletWithTwoSidedRelationshipAndParentRelationNameEqualsToAnotherFieldVarInChild()
            throws DotDataException, DotSecurityException {
        final MapToContentletPopulator populator = new MapToContentletPopulator();

        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");
            Contentlet contentlet = createContentlet(childContentType);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable());

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField
                            .variable();

            //Adding a RelationshipField to the child
            createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar);

            //Creating text field in the child with relationship field variable set in the parent
            Field textField = FieldBuilder.builder(TextField.class)
                    .name(parentTypeRelationshipField.variable())
                    .variable(parentTypeRelationshipField.variable())
                    .contentTypeId(childContentType.id()).build();

            textField = fieldAPI.save(textField, user);

            final Map<String, Object> properties = new HashMap<>();
            properties.put(Contentlet.STRUCTURE_INODE_KEY, childContentType.id());
            properties.put(textField.variable(), contentlet.getIdentifier());

            contentlet = populator.populate(contentlet, properties);

            assertNull(contentlet.get(Contentlet.RELATIONSHIP_KEY));
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName, final String parentTypeId,
            final String childTypeVar)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values( String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(childTypeVar).build();

        return fieldAPI.save(field, user);
    }

    private Contentlet createContentlet(final ContentType contentType) {
        return new ContentletDataGen(contentType.id())
                .setProperty("widgetTitle", "New YouTube Content")
                .setProperty("url", "new-youtube-content").nextPersisted();
    }

    /**
     * We're testing a few improvements that come handy when dealing with language
     * The old impl relied on the given param passed on the map to establish the language Id
     * If not lang id was passed the old impl would fall back directly to the default lang
     * But that can cause an issue if the inode that is passed is bound to a different lang
     * therefore the new impl will try a few other things like extracting the lang from the given inode
     * keep the lang passed on the original contentlet (if any)
     * And at least attempt to use the default lang
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Set_Or_Resolve_Language() throws DotDataException, DotSecurityException {
        final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final long spanish =  TestDataUtils.getSpanishLanguage().getId();

        final Contentlet contentInSpanish = TestDataUtils.getGenericContentContent(true, spanish);

        final MapToContentletPopulator populator = new MapToContentletPopulator();
        //We're sending no Language here but since the incoming contentlet has a lang of its own
        Contentlet populatedContentlet = populator.populate(contentInSpanish, Map.of(
                Contentlet.STRUCTURE_INODE_KEY, contentInSpanish.getContentTypeId())
        );
        //We should expect it to come back with one
        Assert.assertEquals(spanish, populatedContentlet.getLanguageId());

        //Now set any arbitrary lang and corroborate it takes precedence over the existing one on the contentlet
        populatedContentlet = populator.populate(contentInSpanish, Map.of(
                        Contentlet.STRUCTURE_INODE_KEY, contentInSpanish.getContentTypeId(),
                        Contentlet.LANGUAGEID_KEY, defaultLanguage
                )
        );
        Assert.assertEquals(defaultLanguage, populatedContentlet.getLanguageId());

        //Here we send no lang but since we have an inode we'll use it to derive the lang
        contentInSpanish.setLanguageId(0);
        Assert.assertNotNull(contentInSpanish.getInode());
        populatedContentlet = populator.populate(contentInSpanish, Map.of(
                        Contentlet.STRUCTURE_INODE_KEY, contentInSpanish.getContentTypeId()
                )
        );

        Assert.assertEquals(spanish, populatedContentlet.getLanguageId());

    }

}
