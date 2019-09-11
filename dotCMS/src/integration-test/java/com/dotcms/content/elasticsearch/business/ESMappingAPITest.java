package com.dotcms.content.elasticsearch.business;

import static com.dotcms.datagen.TestDataUtils.getCommentsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.datagen.TestDataUtils.relateContentTypes;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ESMappingAPITest {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static LanguageAPI languageAPI;
    private static Language language;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();

        contentTypeAPI = APILocator.getContentTypeAPI(user);
        contentletAPI = APILocator.getContentletAPI();
        fieldAPI = APILocator.getContentTypeFieldAPI();
        languageAPI = APILocator.getLanguageAPI();
        language = languageAPI.getDefaultLanguage();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @Test
    public void testLoadRelationshipFields_whenUsingLegacyRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ContentType news = getNewsLikeContentType("News");
        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(news, comments);


        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        final Map<String, Object> esMap = new HashMap<>();

        final ContentType newsContentType = contentTypeAPI.find("News");
        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet newsContentlet = null;
        Contentlet commentsContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(newsContentType.id());
            newsContentlet = dataGen.languageId(language.getId()).setProperty("title", "News Test")
                    .setProperty("urlTitle", "news-test").setProperty("byline", "news-test")
                    .setProperty("sysPublishDate", new Date()).setProperty("story", "news-test")
                    .next();

            //creates child contentlet
            dataGen = new ContentletDataGen(commentsContentType.id());
            commentsContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Comment for News")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Comment for News").nextPersisted();

            final Relationship relationship = relationshipAPI.byTypeValue("News-Comments");

            newsContentlet = contentletAPI.checkin(newsContentlet,
                    map(relationship, list(commentsContentlet)),
                    null, user, false);

            esMappingAPI.loadRelationshipFields(newsContentlet, esMap);

            assertNotNull(esMap);
            assertEquals(commentsContentlet.getIdentifier(),
                    ((List)esMap.get("News-Comments")).get(0));

        } finally {
            if (newsContentlet != null && UtilMethods.isSet(newsContentlet.getIdentifier())) {
                ContentletDataGen.remove(newsContentlet);
            }

            if (commentsContentlet != null && UtilMethods.isSet(commentsContentlet.getIdentifier())) {
                ContentletDataGen.remove(commentsContentlet);
            }

        }
    }

    @Test
    public void testLoadRelationshipFields_whenUsingSelfRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ContentType comments = getCommentsLikeContentType("Comments");
        relateContentTypes(comments, comments);

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        final Map<String, Object> esMap = new HashMap<>();

        final ContentType commentsContentType = contentTypeAPI.find("Comments");

        Contentlet parentContentlet = null;
        Contentlet childContentlet = null;

        try {
            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(commentsContentType.id());
            childContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Child Comment for Test")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Child Comment for Test")
                    .nextPersisted();

            //creates child contentlet
            parentContentlet = dataGen.languageId(language.getId())
                    .setProperty("title", "Parent Comment for Test")
                    .setProperty("email", "testing@dotcms.com")
                    .setProperty("comment", "Parent Comment for Test")
                    .next();

            final Relationship relationship = relationshipAPI.byTypeValue("Comments-Comments");

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    map(relationship, list(childContentlet)),
                    null, user, false);

            esMappingAPI.loadRelationshipFields(parentContentlet, esMap);

            assertNotNull(esMap);
            assertEquals(childContentlet.getIdentifier(), ((List)esMap.get("Comments-Comments")).get(0));

        } finally {
            if (parentContentlet != null && UtilMethods.isSet(parentContentlet.getIdentifier())) {
                ContentletDataGen.remove(parentContentlet);
            }

            if (childContentlet != null && UtilMethods.isSet(childContentlet.getIdentifier())) {
                ContentletDataGen.remove(childContentlet);
            }

        }
    }


    @Test
    public void testLoadRelationshipFields_whenUsingOneSideFieldRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        Map<String, Object> esMap = new HashMap<>();

        String cardinality = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");

            createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), cardinality);

            final Relationship relationship = relationshipAPI.byContentType(parentContentType)
                    .get(0);

            //creates parent contentlet
            ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContentlet = dataGen.languageId(language.getId()).next();

            //creates child contentlet
            dataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet1 = dataGen.languageId(language.getId()).nextPersisted();
            final Contentlet childContentlet2 = dataGen.languageId(language.getId()).nextPersisted();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    map(relationship, list(childContentlet1, childContentlet2)),
                    null, user, false);

            final StringWriter catchAllWriter = new StringWriter();
            esMappingAPI.loadRelationshipFields(parentContentlet, esMap, catchAllWriter);

            assertNotNull(esMap);
            assertNotNull(catchAllWriter);

            final List<String> expectedResults = list(childContentlet1.getIdentifier(), childContentlet2.getIdentifier());

            validateRelationshipIndex(esMap, relationship.getRelationTypeValue(), expectedResults,
                    catchAllWriter.toString());

        } finally {
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }

        }
    }

    @Test
    public void testLoadRelationshipFields_whenUsingTwoSidedFieldRelationships_shouldSuccess()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        Map<String, Object> esMap = new HashMap<>();

        String cardinality = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        ContentType parentContentType = null;
        ContentType childContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType");
            childContentType = createAndSaveSimpleContentType("childContentType");

            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), cardinality);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField
                            .variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, cardinality);

            final Relationship relationship = relationshipAPI.byContentType(parentContentType)
                    .get(0);

            //creates child contentlet
            final ContentletDataGen childDataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet = childDataGen.languageId(language.getId()).nextPersisted();

            //creates parent contentlet
            final ContentletDataGen parentDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContentlet = contentletAPI
                    .checkin(parentDataGen.languageId(language.getId()).next(),
                            CollectionsUtils
                                    .map(relationship, CollectionsUtils.list(childContentlet)),
                            null, user, false);

            esMappingAPI.loadRelationshipFields(childContentlet, esMap, new StringWriter());

            assertNotNull(esMap);

            assertTrue(esMap.isEmpty());

            final StringWriter catchAll = new StringWriter();
            esMappingAPI.loadRelationshipFields(parentContentlet, esMap, catchAll);

            final List<String> expectedResults = CollectionsUtils.list(childContentlet.getIdentifier());

            validateRelationshipIndex(esMap, relationship.getRelationTypeValue(), expectedResults,
                    catchAll.toString());

        } finally {
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }

        }
    }

    private void validateRelationshipIndex(final Map<String, Object> esMap, final String keyName,
            final List<String> identifiers, final String catchAll) {

        final List results = List.class.cast(esMap.get(keyName));
        assertEquals(identifiers.size(), results.size());

        assertFalse(Collections.disjoint(results, identifiers));

        assertTrue(identifiers.stream().allMatch(identifier -> catchAll.contains(identifier)));
    }

    private ContentType createAndSaveSimpleContentType(final String name)
            throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName,
            final String parentTypeId,
            final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return fieldAPI.save(field, user);
    }

    @Test
    public void testLoadCategories_GivenContentWithCats_ShouldLoadESMapWithListOfCatsVarnames()
            throws DotDataException, DotSecurityException {

        final ESMappingAPIImpl esMappingAPI = new ESMappingAPIImpl();
        Map<String, Object> esMap = new HashMap<>();
        ContentType contentType = null;
        List<Category> categoriesToDelete = new ArrayList<>();
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();

        try {
            contentType = createAndSaveSimpleContentType("testContentType");

            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName("CT-Category-Parent");
            parentCategory.setKey("parent");
            parentCategory.setCategoryVelocityVarName("parent");
            parentCategory.setSortOrder((String) null);
            parentCategory.setKeywords(null);

            categoryAPI.save(null, parentCategory, user, false);
            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            Category childCategoryA = new Category();
            childCategoryA.setCategoryName("CT-Category-A");
            childCategoryA.setKey("categoryA");
            childCategoryA.setCategoryVelocityVarName("categoryA");
            childCategoryA.setSortOrder(1);
            childCategoryA.setKeywords(null);

            categoryAPI.save(parentCategory, childCategoryA, user, false);
            categoriesToDelete.add(childCategoryA);

            //Create Second Child Category.
            Category childCategoryB = new Category();
            childCategoryB.setCategoryName("CT-Category-B");
            childCategoryB.setKey("categoryB");
            childCategoryB.setCategoryVelocityVarName("categoryB");
            childCategoryB.setSortOrder(2);
            childCategoryB.setKeywords(null);

            categoryAPI.save(parentCategory, childCategoryB, user, false);
            categoriesToDelete.add(childCategoryB);

            final Field catField = FieldBuilder.builder(CategoryField.class)
                    .name("myCategoryField")
                    .variable("myCategoryField")
                    .values(parentCategory.getInode())
                    .contentTypeId(contentType.id())
                    .build();

            APILocator.getContentTypeFieldAPI().save(catField, user);

            Contentlet content = new ContentletDataGen(contentType.id()).next();

            content = APILocator.getContentletAPI().checkin(content, user, false,
                    list(childCategoryA, childCategoryB));

            esMappingAPI.loadCategories(content, esMap);

            //Categories must be indexed in lower case
            final List<String> expectedCatList = list("categorya", "categoryb");

            assertEquals("All cats present as List of varnames in ES mapping under variable of cat field",
                    expectedCatList, esMap.get(contentType.variable() + "." + catField.variable()));

            assertEquals("All cats present as List of varnames is ES mapping under 'categories'",
                    expectedCatList, esMap.get(ESMappingConstants.CATEGORIES));


        } finally {
            for(final Category category:categoriesToDelete){
                categoryAPI.delete(category, user, false);
            }

            if (contentType != null && contentType.id() != null) {
                contentTypeAPI.delete(contentType);
            }

        }

    }

}
