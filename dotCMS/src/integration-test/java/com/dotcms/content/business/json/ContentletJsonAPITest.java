package com.dotcms.content.business.json;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.ImmutableContentlet;
import com.dotcms.content.model.type.ImageFieldType;
import com.dotcms.content.model.type.system.AbstractCategoryFieldType;
import com.dotcms.content.model.type.system.AbstractTagFieldType;
import com.dotcms.content.model.type.system.BinaryFieldType;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentletJsonAPITest extends IntegrationTestBase {

    static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final String hostName = "yet.another.site" + System.currentTimeMillis() + ".dotcms.com";
        site = new SiteDataGen().name(hostName).nextPersisted(true);
    }

    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)}
     * Basic scenario we turn off the  SAVE_CONTENTLET_AS_JSON flag then we create some entries
     * then we serialize it and compare it against the original map build out of the columns
     * @throws Exception
     */
    @Test
    public void Simple_Json_Serialize_Test() throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            final String hostName = "custom" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            new TagDataGen().name("tag1").nextPersisted();

            final Contentlet in = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("textFieldNumeric",0)
                    .setProperty("textFieldFloat",0.0F)
                    .setProperty("textField","text")
                    .setProperty("binaryField", TestDataUtils.nextBinaryFile(TestFile.JPG))
                    .setProperty("textAreaField", "Desc")
                    .setProperty("dateField",new Date())
                    .setProperty("dateTimeField",new Date())
                    .setProperty("tagField","tag1") //System field isn't expected to get saved in the json
                    .setProperty("keyValueField", "{\"key1\":\"val1\"}")
                    .nextPersisted();

            assertNotNull(in);
            final ContentletJsonAPI impl = APILocator.getContentletJsonAPI();
            final String json = impl.toJson(in);
            assertNotNull(json);
            final Contentlet out = impl.mapContentletFieldsFromJson(json);
            mapsAreEqual(in.getMap(),out.getMap());

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    private void mapsAreEqual(final Map<String, Object> in, final Map<String, Object> out) {
        assertEquals(in.get("title"),out.get("title"));
        assertEquals(in.get("hostFolder"),out.get("hostFolder"));
        assertEquals(in.get("folder"),out.get("folder"));
        assertEquals(in.get("textFieldNumeric"),out.get("textFieldNumeric"));
        assertEquals(in.get("textFieldFloat"),out.get("textFieldFloat"));
        assertEquals(in.get("textField"),out.get("textField"));
        assertEquals(in.get("binaryField"),out.get("binaryField"));
        assertEquals(in.get("textAreaField"),out.get("textAreaField"));
        assertEquals(in.get("modUser"),out.get("modUser"));
        assertEquals(in.get("inode"),out.get("inode"));
        assertEquals(in.get("identifier"),out.get("identifier"));
        assertEquals(in.get("stInode"),out.get("stInode"));
        assertEquals(in.get("host"),out.get("host"));
        assertEquals(in.get("languageId"),out.get("languageId"));
        assertEquals(in.get("owner"),out.get("owner"));
        assertEquals(in.get("tagField"),out.get("tagField"));
        assertEquals(in.get("keyValueField"),out.get("keyValueField"));

        if (null != in.get("dateField") && null != out.get("dateField")) {
            assertEquals(((Date) in.get("dateField")).toInstant(),
                    ((Date) out.get("dateField")).toInstant());
        }
        if(null != in.get("modDate") &&  null != out.get("modDate") ) {
            assertEquals(((Date) in.get("modDate")).toInstant(),
                    ((Date) out.get("modDate")).toInstant());
        }
    }


    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)} called within a checkin context
     * Basically we're are testing that the contentlet created via columns and via json looks the same
     * @throws Exception
     */
    @Test
    public void Create_Content_Then_Find_It_Then_Create_Json_Content_Then_Recover_And_Compare() throws Exception {

        if(!APILocator.getContentletJsonAPI().isPersistContentAsJson()){
            Logger.info(ContentletJsonAPITest.class, ()->"Test Should only run on databases with enabled json capabilities.");
            return;
        }

        final String hostName = "my.custom" + System.currentTimeMillis() + ".dotcms.com";
        final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final ContentType contentType = TestDataUtils
                .newContentTypeFieldTypesGalore();

        final File file = TestDataUtils.nextBinaryFile(TestFile.JPG);

        new TagDataGen().name("tag1").nextPersisted();

        String categoryName = "myTestCategory" + System.currentTimeMillis();

        final Category category = new CategoryDataGen().setCategoryName(categoryName)
                .setKey(categoryName + "Key").setCategoryVelocityVarName(categoryName)
                .setSortOrder(1).nextPersisted();

        Contentlet columnSaved = null;
        Contentlet jsonSaved = null;

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            columnSaved = getContentlet(site, folder, contentType, category, file);

            assertNotNull(columnSaved);

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }

        Config.setProperty(SAVE_CONTENTLET_AS_JSON, true);
        try {
            jsonSaved = getContentlet(site, folder, contentType, category, file);

            assertNotNull(jsonSaved);

        }finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }


        final DotConnect dotConnect = new DotConnect();
        //These lines should corroborate the json is not written when the API was instructed to use the columns
        final int columnSavedCount = dotConnect.setSQL(
                "select count(*) as x from contentlet c where c.inode = ? and c.contentlet_as_json is null"
        ).addParam(columnSaved.getInode()).getInt("x");

        //These lines should corroborate the json was actually written when the API was instructed to use json
        final int jsonSavedCount = dotConnect.setSQL(
                "select count(*) as x from contentlet c where c.inode = ? and c.contentlet_as_json is not null"
        ).addParam(jsonSaved.getInode()).getInt("x");

        assertEquals(columnSavedCount,1);
        assertEquals(jsonSavedCount,1);

        compareMapVersionsAreEquivalent(columnSaved.getMap(), jsonSaved.getMap());

    }

    private Contentlet getContentlet(final Host site, final Folder folder, final ContentType contentType,
            final Category category, final File file ) {
        return new ContentletDataGen(contentType).host(site)
                .languageId(1)
                .setProperty("title", "lol")
                .setProperty("hostFolder", folder.getIdentifier())
                .setProperty("textFieldNumeric", 0L)
                .setProperty("textFieldFloat", 0.0F)
                .setProperty("textField", "text")
                .setProperty("binaryField", file)
                .setProperty("textAreaField", "Desc")
                .setProperty("dateField", new Date())
                .setProperty("dateTimeField", new Date())
                .setProperty("tagField", "tag1")
                .setProperty("keyValueField", "{\"key1\":\"val1\"}")
                .addCategory(category)
                .nextPersisted();
    }


    private void compareMapVersionsAreEquivalent(final Map<String, Object> in, final Map<String, Object> out) {
        assertEquals(in.get("title"),out.get("title"));
        assertEquals(in.get("hostFolder"),out.get("hostFolder"));
        assertEquals(in.get("folder"),out.get("folder"));
        assertEquals(in.get("textFieldNumeric"),out.get("textFieldNumeric"));
        assertEquals(in.get("textFieldFloat"),out.get("textFieldFloat"));
        assertEquals(in.get("textField"),out.get("textField"));

        assertEquals(
                Paths.get(in.get("binaryField").toString()).getFileName().toString(),
                Paths.get(out.get("binaryField").toString()).getFileName().toString()
        );

        assertEquals(in.get("textAreaField"),out.get("textAreaField"));
        assertEquals(in.get("modUser"),out.get("modUser"));
        assertNotEquals(in.get("inode"),out.get("inode"));
        assertNotEquals(in.get("identifier"),out.get("identifier"));
        assertEquals(in.get("stInode"),out.get("stInode"));
        assertEquals(in.get("host"),out.get("host"));
        assertEquals(in.get("languageId"),out.get("languageId"));
        assertEquals(in.get("owner"),out.get("owner"));
        assertEquals(in.get("tagField"),out.get("tagField"));
        assertEquals(in.get("keyValueField"),out.get("keyValueField"));


    }

    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)}
     * Called over two version of a contentlet (with text fields) one with preset numeric value the other with nulls
     * Basically we're are testing here is that the even if call toJson on contentlet that has nulls on their text fields we get back 0s as the defaults.
     * @throws Exception
     */
    @Test
    public void Initialize_Fields_With_Default_Value_Test() throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);

        try {
            final String hostName = "custom" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            final ContentletJsonAPIImpl impl = (ContentletJsonAPIImpl)APILocator.getContentletJsonAPI();

            final Contentlet filledWithZeros = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("textFieldNumeric",null)
                    .setProperty("textFieldFloat",null)
                    .setProperty("textField",null)
                    .nextPersisted();

            assertNotNull(filledWithZeros);

            final String json = impl.toJson(filledWithZeros);
            assertNotNull(json);
            final Contentlet out = impl.mapContentletFieldsFromJson(json);
            assertEquals(0L, out.get("textFieldNumeric"));
            assertEquals(0F, out.get("textFieldFloat"));
            assertEquals(false, out.get("hiddenBool"));
            assertNull(out.get("textField"));

            final ImmutableContentlet immutableContentlet = impl.toImmutable(filledWithZeros);
            final ImmutableMap<String, FieldValue<?>> fields = immutableContentlet.fields();
            assertEquals(0L, fields.get("textFieldNumeric").value());
            assertEquals(0F,fields.get("textFieldFloat").value());
            assertEquals(false, fields.get("hiddenBool").value());

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)} called within a checkin context
     * This is a much simpler test. What changes here is the set of fields we're passing
     * @throws Exception
     */
    @Test
    public void Simple_Serializer_Test()
            throws DotDataException, JsonProcessingException, DotSecurityException {
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            new TagDataGen().name("tag1").nextPersisted();
            final Contentlet imageFileAsset = TestDataUtils.getFileAssetContent(true, 1, TestFile.JPG);

            final Contentlet in = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("imageField",imageFileAsset.getIdentifier())
                    .nextPersisted();

            assertNotNull(in);
            final ContentletJsonAPI impl = APILocator.getContentletJsonAPI();
            final String json = impl.toJson(in);
            assertNotNull(json);
            final Contentlet out = impl.mapContentletFieldsFromJson(json);
            mapsAreEqual(in.getMap(),out.getMap());

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)}
     * Here we're testing that what is annotated to be properly hydrated comes back with the injected attributes
     * @throws Exception
     */
    @Test
    public void Test_Hydration_Test()
            throws DotDataException, IOException, DotSecurityException, URISyntaxException {
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            new TagDataGen().name("tag1").nextPersisted();
            final Contentlet imageFileAsset = TestDataUtils.getFileAssetContent(true, 1, TestFile.JPG);

            final Metadata metadataNoCache = APILocator.getFileMetadataAPI()
                    .getFullMetadataNoCache((File) imageFileAsset.get("fileAsset"), null);
            assertNotNull(metadataNoCache);

            final Metadata metadata = APILocator.getFileMetadataAPI()
                    .getOrGenerateMetadata(imageFileAsset, "fileAsset");

            System.out.println(metadata);

            final Contentlet in = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("imageField",imageFileAsset.getIdentifier())
                    .setProperty("binaryField", TestDataUtils.nextBinaryFile(TestFile.JPG))
                    .nextPersisted();

            final ContentletJsonAPIImpl impl = (ContentletJsonAPIImpl)APILocator.getContentletJsonAPI();
            final String json = impl.toJson(in);
            final com.dotcms.content.model.Contentlet immutableFromJson = impl.immutableFromJson(json);
            assertNotNull(immutableFromJson);
            final Map<String, FieldValue<?>> fieldValueMap = immutableFromJson.fields();
            final FieldValue<?> imageField = fieldValueMap.get("imageField");
            final FieldValue<?> binaryField = fieldValueMap.get("binaryField");
            final ImageFieldType imageType = (ImageFieldType)imageField;
            assertNotNull(imageType.metadata());
            final BinaryFieldType binaryType = (BinaryFieldType)binaryField;
            assertNotNull(binaryType.metadata());
        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)}
     * This test is intended to create a content with tags and categories then serialize it to json and do the inverse process read and compare
     * @throws DotDataException
     * @throws JsonProcessingException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Category_And_Tags_Serialization_And_Recovery()
            throws DotDataException, JsonProcessingException, DotSecurityException {
        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            final Folder folder = new FolderDataGen().site(site).nextPersisted();

            final String parentCategoryName = "ParentCategory" + System.currentTimeMillis();

            final Category parentCategory = new CategoryDataGen()
                    .setCategoryName(parentCategoryName)
                    .setKey(parentCategoryName + "Key")
                    .setCategoryVelocityVarName(parentCategoryName)
                    .setSortOrder(1)
                    .nextPersisted();

            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore(parentCategory);

            final Optional<Field> tag = contentType.fields(TagField.class).stream().findFirst();
            final Optional<Field> category = contentType.fields(CategoryField.class).stream()
                    .findFirst();

            assertTrue(tag.isPresent());
            assertTrue(category.isPresent());

            final Category parent = APILocator.getCategoryAPI()
                    .find(category.get().values(), APILocator.systemUser(), false);

            assertNotNull(parent);

            new TagDataGen().name("mtb").nextPersisted();
            new TagDataGen().name("road").nextPersisted();

            final String bikeCategoryName = "Enduro" + System.currentTimeMillis();

            final Category childCategory = new CategoryDataGen()
                    .setCategoryName(bikeCategoryName)
                    .setKey(bikeCategoryName + "Key")
                    .setCategoryVelocityVarName(bikeCategoryName)
                    .setSortOrder(1)
                    .parent(parent).nextPersisted();

            final Contentlet persisted = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("tagField", "mtb,road")
                    .addCategory(childCategory) //These are associated to the category type field
                    .nextPersisted();

            final String tagsAsString = (String)persisted.get("tagField");
            final List<String> tags = Stream.of(tagsAsString.split(",", -1))
                    .collect(Collectors.toList());
            assertTrue(tags.contains("mtb"));
            assertTrue(tags.contains("road"));

            final ContentletJsonAPIImpl impl = (ContentletJsonAPIImpl)APILocator.getContentletJsonAPI();
            final ImmutableContentlet immutableContentlet = impl.toImmutable(persisted);

            final ImmutableMap<String, FieldValue<?>> fields = immutableContentlet.fields();
            final AbstractTagFieldType tagsFieldValue =  (AbstractTagFieldType)fields.get("tagField");
            final AbstractCategoryFieldType categoryFieldValue =  (AbstractCategoryFieldType)fields.get("categoryField");
            assertNotNull(tagsFieldValue);
            assertEquals(Arrays.asList("mtb","road"),tagsFieldValue.value());
            assertNotNull(categoryFieldValue);
            final List<String> categories = categoryFieldValue.value();
            assertNotNull(categories);
            assertTrue(categories.contains(childCategory.getCategoryVelocityVarName()));
            final String json = impl.toJson(persisted);
            assertNotNull(json);
            final Contentlet out = impl.mapContentletFieldsFromJson(json);
            final List<?> recoveredCategories =  (List<?>)out.get("categoryField");
            assertTrue(recoveredCategories.contains(childCategory.getCategoryVelocityVarName()));

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }


}
