package com.dotcms.content.business;

import static com.dotcms.content.business.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentletJsonAPITest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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
        assertEquals(((Date)in.get("dateField")).toInstant(),((Date)out.get("dateField")).toInstant());
        assertEquals(((Date)in.get("modDate")).toInstant(),((Date)out.get("modDate")).toInstant());
    }


    /**
     * Method to test {@link ContentletJsonAPI#toJson(Contentlet)} && {@link ContentletJsonAPI#mapContentletFieldsFromJson(String)} called within a checkin context
     * Basically we're are testing that the contentlet created via columns and via json looks the same
     * @throws Exception
     */
    @Test
    public void Create_Content_Then_Find_It_Then_Create_Json_Content_Then_Recover_And_Compare() throws Exception {

        //TODO: Once we have json capabilities enabled on all the supported dbs we must remove this condition. Currently we only support them on Postgres
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
    public void Text_Field_Initialize_Test() throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);

        try {
            final String hostName = "custom" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            final ContentletJsonAPI impl = APILocator.getContentletJsonAPI();

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
            final Contentlet outWithZeros = impl.mapContentletFieldsFromJson(json);
            assertEquals(outWithZeros.get("textFieldNumeric"),0L );
            assertEquals(outWithZeros.get("textFieldFloat"),0F );
            assertNull(outWithZeros.get("textField"));

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

}
