package com.dotcms.content.business;

import static com.dotcms.content.business.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
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

    @Test
    public void Simple_json_Generation_Test() throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        //disconnect the MD generation on indexing so we can test the generation directly using the API.
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
        try {

            final String hostName = "custom" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .getContentTypeWithAllAvailableFieldTypes();

            new TagDataGen().name("tag1").nextPersisted();

            String categoryName = "myTestCategory" + System.currentTimeMillis();

            final Category category = new CategoryDataGen().setCategoryName(categoryName)
                .setKey(categoryName + "Key").setCategoryVelocityVarName(categoryName)
                .setSortOrder(1).nextPersisted();

            Contentlet in = new ContentletDataGen(contentType).host(site)
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
                    .addCategory(category)
                    .nextPersisted();

            assertNotNull(in);

            final ContentletJsonAPI impl = APILocator.getContentletJsonAPI();
            final String json = impl.toJson(in);
            assertNotNull(json);
            System.out.println(json);


            final Contentlet out = impl.mapContentletFieldsFromJson(json);
            assertNotNull(out);
            assertTrue(areEqual(in.getMap(),out.getMap()));

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    private boolean areEqual(final Map<String, Object> first, final Map<String, Object> second) {
        if (first.size() != second.size()) {
            return false;
        }
        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }

}
