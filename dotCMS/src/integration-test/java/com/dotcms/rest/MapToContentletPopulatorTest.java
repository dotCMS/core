package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.AssertTrue;
import org.immutables.value.Value.Immutable;
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

    private Category parentCategory;
    private ContentType contentTypeWithCategoryField;

    Category createCategories(){

        if(null != parentCategory){
           return parentCategory;
        }
        final String parentCategoryName = "ParentCategory-" + System.currentTimeMillis();
        parentCategory = new CategoryDataGen()
                .setCategoryName(parentCategoryName)
                .setKey(parentCategoryName + "Key")
                .setCategoryVelocityVarName(parentCategoryName)
                .setSortOrder(1)
                .nextPersisted();

        final String childCategoryName1 = "Child-Category-1" + System.currentTimeMillis();

        new CategoryDataGen()
                .setCategoryName(childCategoryName1)
                .setKey(childCategoryName1 + "Key")
                .setCategoryVelocityVarName(childCategoryName1)
                .setSortOrder(1)
                .parent(parentCategory).nextPersisted();

       return parentCategory;
    }

    ContentType contentTypeWithCategoryField(){
        if(null != contentTypeWithCategoryField){
            return contentTypeWithCategoryField;
        }
        final Category categories = createCategories();
        contentTypeWithCategoryField= TestDataUtils.newContentTypeFieldTypesGalore(categories);
        return contentTypeWithCategoryField;
    }

    /**
     * Scenario: We pass any contantlet that lacks a category field  value set
     * Expectation: nothing should blow-up
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Pass_Categoryless_Contentlet() throws DotDataException, DotSecurityException {
        final ContentType contentType = contentTypeWithCategoryField();
        final Contentlet categoryless =  new Contentlet();
        categoryless.setContentTypeId(contentType.id());
        final MapToContentletPopulator populator = new MapToContentletPopulator();
        final List<Category> categories = populator.getCategories(categoryless, APILocator.systemUser(), false);
        assertTrue(categories.isEmpty());
    }

    /**
     * Scenario: We pass any contantlet that has null in a CategoryField
     * Expectation: nothing should blow-up. Nothing should happen.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Pass_Null_As_Category() throws DotDataException, DotSecurityException {
        final Category category = createCategories();
        final ContentType contentType = contentTypeWithCategoryField();
        //Make sure we have a field of type Category
        final Optional<Field> first = contentType.fields(CategoryField.class).stream().findFirst();
        assertTrue(first.isPresent());

        final MapToContentletPopulator populator = new MapToContentletPopulator();
        final String varName = first.get().variable();

        final Contentlet withCategoryField = new Contentlet();
        withCategoryField.setContentTypeId(contentType.id());
        withCategoryField.setProperty(varName, null);
        final List<Category> recovered = populator.getCategories(withCategoryField, APILocator.systemUser(), false);
        assertTrue("Sending null should be ignored.", recovered.isEmpty());
    }

    /**
     * Scenario: We pass an invalid value in a category field
     * Expectation: Illegal Argument Exception
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Pass_Invalid_Category() throws DotDataException, DotSecurityException {

        final ContentType contentType = contentTypeWithCategoryField();
        //Make sure we have a field of type Category
        final Optional<Field> first = contentType.fields(CategoryField.class).stream().findFirst();
        assertTrue(first.isPresent());

        final Contentlet withCategoryField = new Contentlet();
        withCategoryField.setContentTypeId(contentType.id());
        withCategoryField.setProperty(first.get().variable(),"any-invalid-category-id");
        final MapToContentletPopulator populator = new MapToContentletPopulator();
        populator.getCategories(withCategoryField,
                APILocator.systemUser(), false);
    }

    /**
     * Scenario: This time we pass a a set of valid category values
     * Expectation: everytime we must recover a category
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Pass_Valid_Category() throws DotDataException, DotSecurityException {
        final Category category = createCategories();
        final ContentType contentType = contentTypeWithCategoryField();
        //Make sure we have a field of type Category
        final Optional<Field> first = contentType.fields(CategoryField.class).stream().findFirst();
        assertTrue(first.isPresent());

        final MapToContentletPopulator populator = new MapToContentletPopulator();

        final List<String> categoryIdKey = ImmutableList.of(category.getInode(),category.getKey(),
                category.getCategoryId());

        final String varName = first.get().variable();

        for (final String object:categoryIdKey) {
            final Contentlet withCategoryField = new Contentlet();
            withCategoryField.setContentTypeId(contentType.id());
            withCategoryField.setProperty(varName, object);
            final List<Category> recovered = populator.getCategories(withCategoryField, APILocator.systemUser(), false);
            assertFalse(" I couldn't find categories using object "+object, recovered.isEmpty());
        }
    }


    /**
     * Scenario: This time we pass a set of valid category values Expectation: everytime we must
     * recover a category
     */
    @Test
    public void Test_Pass_List_Of_Categories() throws DotDataException, DotSecurityException {
        final Category category = createCategories();
        final ContentType contentType = contentTypeWithCategoryField();
        //Make sure we have a field of type Category
        final Optional<Field> first = contentType.fields(CategoryField.class).stream().findFirst();
        assertTrue(first.isPresent());

        final MapToContentletPopulator populator = new MapToContentletPopulator();

        final List<String> list = ImmutableList.of(category.getInode(), category.getKey(),
                category.getCategoryId());

        final String varName = first.get().variable();

        final Contentlet withCategoryField = new Contentlet();
        withCategoryField.setContentTypeId(contentType.id());
        withCategoryField.setProperty(varName, list);
        final List<Category> recovered = populator.getCategories(withCategoryField,
                APILocator.systemUser(), false);
        assertFalse(" I couldn't find categories using object " + list, recovered.isEmpty());

    }

    /**
     * Scenario: This time we pass an empty collection
     * Expectation: Empty array of categories should be returned
     */
    @Test
    public void Test_Pass_Empty_List_Of_Categories() throws DotDataException, DotSecurityException {

        final ContentType contentType = contentTypeWithCategoryField();
        //Make sure we have a field of type Category
        final Optional<Field> first = contentType.fields(CategoryField.class).stream().findFirst();
        assertTrue(first.isPresent());

        final MapToContentletPopulator populator = new MapToContentletPopulator();

        final String varName = first.get().variable();
        final List<String> list = ImmutableList.of();

        final Contentlet withCategoryField = new Contentlet();
        withCategoryField.setContentTypeId(contentType.id());
        withCategoryField.setProperty(varName, list);
        final List<Category> recovered = populator.getCategories(withCategoryField,
                APILocator.systemUser(), false);
        assertTrue(" Recovered categories should be empty " + list, recovered.isEmpty());

    }


}
