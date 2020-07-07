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
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;

import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.contenttype.util.KeyValueFieldUtil;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import java.io.File;

import java.sql.SQLException;
import java.util.ArrayList;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ESMappingAPITest {

    private static final String TEMP_FILE = "tempFile";
    public static final String TXT = "txt";
    public static final String DOT_TXT = ".txt";
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static LanguageAPI languageAPI;
    private static Language language;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static FolderAPI folderAPI;
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
        folderAPI = APILocator.getFolderAPI();
        languageAPI = APILocator.getLanguageAPI();
        language = languageAPI.getDefaultLanguage();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @Test
    public void test_toMap_fileasset_txt_shouldSuccess() throws Exception {

        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();
        final Host host = APILocator.getHostAPI().findDefaultHost(user, false);
        final String rootFolderName = String.format("lolFolder-%d", System.currentTimeMillis());
        final Folder root1 = folderAPI.createFolders(rootFolderName, host, user, false);
        final FileAsset fileAsset = new FileAsset();
        final ImmutableFileAssetContentType.Builder builder = ImmutableFileAssetContentType.builder();
        final String contentTypeVariable = "testfa"+System.currentTimeMillis();
        builder.name("Test").variable(contentTypeVariable);
        final ContentType fileAssetContentType = builder.build(); //contentTypeAPI.find("FileAsset");
        final String fileName1 = TEMP_FILE + System.currentTimeMillis();
        final File tempFile1 = File.createTempFile(fileName1, TXT);
        final String anyContent = "LOL!";
        FileUtil.write(tempFile1, anyContent);
        final String fileNameField1 = fileName1 + DOT_TXT;
        final String title1 = "Contentlet-1";

        fileAsset.setContentType(contentTypeAPI.save(fileAssetContentType));
        fileAsset.setFolder(root1.getInode());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tempFile1);
        fileAsset.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, root1.getInode());
        fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, title1);
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileNameField1);
        fileAsset.setIndexPolicy(IndexPolicy.FORCE);

        // Create a piece of content for the default host
        final Map<String,Object>  contentletMap = esMappingAPI.toMap(APILocator.getContentletAPI().checkin(fileAsset, user, false));

        assertNotNull(contentletMap);
        assertEquals(fileNameField1.toLowerCase(), contentletMap.get(contentTypeVariable + ".filename"));
        assertEquals(contentTypeVariable, contentletMap.get("structurename"));
        assertEquals("text/plain; charset=iso-8859-1", contentletMap.get("metadata.contenttype").toString().toLowerCase());
        assertEquals(4, contentletMap.get("metadata.filesize"));
        assertTrue( contentletMap.get("metadata.content").toString().contains("lol!"));

    }

    /**
     * Method to Test: {@link ESMappingAPIImpl#toMap(Contentlet)}
     * When: The contentlet has a invalid host
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void whenContentletHasInvalidHostId(){
        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();

        contentlet.setHost("not_exist_host");
        ContentletDataGen.checkin(contentlet);

        esMappingAPI.toMap(contentlet);
    }

    /**
     * Method to Test: {@link ESMappingAPIImpl#toMap(Contentlet)}
     * When: The contentlet has a invalid folder
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void whenContentletHasInvalidFolderId(){
        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();

        contentlet.setFolder("not_exist_folder");
        ContentletDataGen.checkin(contentlet);

        esMappingAPI.toMap(contentlet);
    }

    /**
     * Method to Test: {@link ESMappingAPIImpl#toMap(Contentlet)}
     * When: The contentlet has a invalid content type
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void whenContentletHasInvalidContentTypeId(){
        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).next();

        contentlet.setContentTypeId("not_exist_content_type");
        ContentletDataGen.checkin(contentlet);

        esMappingAPI.toMap(contentlet);
    }

    /**
     * Method to Test: {@link ESMappingAPIImpl#toMap(Contentlet)}
     * When: The contentlet has not {@link com.dotmarketing.beans.Identifier}
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void whenContentletHasNotIdentifier() throws DotHibernateException, SQLException {
        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        HibernateUtil.startTransaction();
        DotConnect dc = new DotConnect();
        dc.executeStatement(String.format("DELETE from identifier where id = %s", contentType.id()));
        HibernateUtil.commitTransaction();

        esMappingAPI.toMap(contentlet);
    }

    /**
     * Method to Test: {@link ESMappingAPIImpl#toMap(Contentlet)}
     * When: The contentlet has not {@link ContentletVersionInfo}
     * Should: Throw a {@link DotDataException}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void whenContentletHasNotContentletVersionInfo() throws DotDataException, SQLException {
        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final ContentletVersionInfo versionInfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

        HibernateUtil.delete(versionInfo);

        esMappingAPI.toMap(contentlet);
    }

    @Test
    public void test_toMap_binary_field_shouldSuccess() throws Exception {

        final ESMappingAPIImpl esMappingAPI    = new ESMappingAPIImpl();
        final FieldAPI         fieldAPI        = APILocator.getContentTypeFieldAPI();
        final Host host = APILocator.getHostAPI().findDefaultHost(user, false);
        final String varname = "testcontenttypetwobinaryfields" + System.currentTimeMillis();

        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Test ContentType Binary Fields")
                .host(host.getIdentifier())
                .name("Test ContentType Binary Fields")
                .owner("owner")
                .variable(varname)
                .build();

        contentType = contentTypeAPI.save(contentType);

        Field textField = ImmutableTextField.builder()
                .name("Title")
                .variable("title")
                .contentTypeId(contentType.id())
                .dataType(DataTypes.TEXT)
                .build();

        textField = fieldAPI.save(textField, user);

        //Creating First Binary Field.
        Field binaryField1 = ImmutableBinaryField.builder()
                .name("Binary 1")
                .variable("binary1")
                .contentTypeId(contentType.id())
                .build();

        binaryField1 = fieldAPI.save(binaryField1, user);

        //Creating Second Binary Field.
        Field binaryField2 = ImmutableBinaryField.builder()
                .name("Binary 2")
                .variable("binary2")
                .indexed(true)
                .searchable(true)
                .contentTypeId(contentType.id())
                .build();

        binaryField2 = fieldAPI.save(binaryField2, user);

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);
        contentlet.setProperty("title", "binary1");

        final String fileName1 = TEMP_FILE + System.currentTimeMillis();
        final File binary1 = File.createTempFile(fileName1, TXT);
        final String anyContent = "LOL!";
        FileUtil.write(binary1, anyContent);
        final File binary2 = new File(ESMappingAPITest.class.getClassLoader().getResource("images/test.jpg").getFile());

        Assert.assertTrue(binary2.exists());

        contentlet.setBinary(binaryField1, binary1);
        contentlet.setBinary(binaryField2, binary2);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);

        final Contentlet contentletSaved = APILocator.getContentletAPI().checkin(contentlet, user, false);
        // Create a piece of content for the default host
        final Map<String,Object>  contentletMap = esMappingAPI.toMap(contentletSaved);

        assertNotNull(contentletMap);
        assertEquals(varname, contentletMap.get("structurename"));
        assertEquals("image/jpeg", contentletMap.get("metadata.contenttype"));
        assertEquals("320", contentletMap.get("metadata.width"));
        assertEquals("235", contentletMap.get("metadata.height"));
        assertTrue( contentletMap.get("metadata.content").toString().trim().isEmpty());

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

    /**
     * General purpose is testing that KeyValue fields are indexed correctly.
     * Given scenario: We create a Content type that holds a Key value field.
     * Expected Results: Then we feed data into such field and use an ES query over the KeyValue to verify the results are coming back.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Create_ContentType_With_KeyValue_Field_Test_Query_Expect_Success()
            throws DotDataException, DotSecurityException {

        final List<Field> fields = new ArrayList<>();
        final String myKeyValueField = "myKeyValueField";
        fields.add(
                new FieldDataGen()
                        .type(KeyValueField.class)
                        .name(myKeyValueField)
                        .velocityVarName(myKeyValueField).indexed(true)
                        .next()
        );
        final String contentTypeName = "myContentTypeWithKeyValues" + System.currentTimeMillis();
        final ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeName)
                .fields(fields)
                .nextPersisted();

        new ContentletDataGen(contentType.id()).setProperty(myKeyValueField,
                "{\"key\":\"key1\", value:\"val\" }").nextPersisted();
        final String queryString =  String.format("+%s.%s.key:%s*",contentTypeName, myKeyValueField, "val");
        Logger.info(ESMappingAPITest.class, () -> String.format(" Query: %s ",queryString));
        final String wrappedQuery = String.format("{"
                + "query: {"
                + "   query_string: {"
                + "        query: \"%s\""
                + "     }"
                + "  }"
                + "}", queryString);

        final ESSearchResults searchResults = contentletAPI.esSearch(wrappedQuery, false,  user, false);
        Assert.assertFalse(searchResults.isEmpty());
        for (final Object searchResult : searchResults) {
           final Contentlet contentlet = (Contentlet) searchResult;
            final String json = (String)contentlet.getMap().get("myKeyValueField");
            assertNotNull(json);
            final Map<String, Object> map = KeyValueFieldUtil.JSONValueToHashMap(json);
            assertTrue(map.containsKey("key"));
        }
    }

    /**
     * General purpose is testing that Metadata fields are indexed correctly can be used as query parameters.
     * Given scenario: Known that saving a file assets generates metadata into the index. We create a file asset.
     * Expected Results: Once the file is saved we use an ES query to verify that queries by metadata are functional.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Create_FileAsset_Query_by_MetaData_Expect_Success()
            throws DotDataException, DotSecurityException {

        final File binary = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();
        final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(site, binary);
        fileAssetDataGen.nextPersisted();

        final String queryString =  String.format("+fileasset.filename:%s +metadata.contenttype:image* +metadata.filesize:%d ",binary.getName(), binary.length());
        Logger.info(ESMappingAPITest.class, () -> String.format(" Query: %s ",queryString));
        final String wrappedQuery = String.format("{"
                + "query: {"
                + "   query_string: {"
                + "        query: \"%s\""
                + "     }"
                + "  }"
                + "}", queryString);

        final ESSearchResults searchResults = contentletAPI.esSearch(wrappedQuery, false,  user, false);
        assertFalse(searchResults.isEmpty());
    }
}
