package com.dotmarketing.startup.runonce;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ACTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.FORWARD_TO_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.ORDER_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.TITLE_FIELD_VAR;
import static com.dotcms.contenttype.model.type.VanityUrlContentType.URI_FIELD_VAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220330ChangeVanityURLSiteFieldTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task220330ChangeVanityURLSiteFieldType#executeUpgrade}
     * When: you have a Vanity URl Content Type with the site field as a {@link CustomField}
     * Should: Change the field type to {@link HostFolderField}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void test_upgradeTask_success() throws DotDataException, DotSecurityException {
        final boolean defaultValue = APILocator.getContentletJsonAPI().isPersistContentAsJson();

        try {
            final ContentType contentType = new ContentTypeDataGen()
                    .baseContentType(BaseContentType.VANITY_URL)
                    .nextPersisted();

            final DotConnect dotConnect = new DotConnect();
            final String updateSQL =
                    "UPDATE field SET field_type = 'com.dotcms.contenttype.model.field.CustomField', field_contentlet = 'text6' "
                            + "WHERE velocity_var_name = 'site' and structure_inode = ?";

            dotConnect.setSQL(updateSQL);
            dotConnect.addParam(contentType.inode());
            dotConnect.loadResult();

            CacheLocator.getContentTypeCache2().remove(contentType);
            final ContentType contentTypeFromDB = FactoryLocator.getContentTypeFactory()
                    .find(contentType.inode());

            final Host host = new SiteDataGen().nextPersisted();
            
            final Optional<Field> siteField = contentTypeFromDB.fields().stream()
                    .filter(field -> field.variable().equals("site")).findFirst();

            assertTrue(siteField.isPresent());
            assertEquals(siteField.get().type(), CustomField.class);
            assertEquals(siteField.get().dataType(), DataTypes.TEXT);

            Config.setProperty(SAVE_CONTENTLET_AS_JSON, false);
            final Contentlet vanityURL_1 = new ContentletDataGen(contentTypeFromDB)
                    .setProperty(TITLE_FIELD_VAR, "title_1")
                    .setProperty(URI_FIELD_VAR, "/uri/test_1")
                    .setProperty(FORWARD_TO_FIELD_VAR, "/foward_1")
                    .setProperty(ACTION_FIELD_VAR, 301)
                    .setProperty(ORDER_FIELD_VAR, 0)
                    .setProperty("site", host.getIdentifier())
                    .host(host)
                    .nextPersistedAndPublish();

            Config.setProperty(SAVE_CONTENTLET_AS_JSON, true);
            final Contentlet vanityURL_2 = new ContentletDataGen(contentTypeFromDB)
                    .setProperty(TITLE_FIELD_VAR, "title_2")
                    .setProperty(URI_FIELD_VAR, "/uri/test_2")
                    .setProperty(FORWARD_TO_FIELD_VAR, "/foward_2")
                    .setProperty(ACTION_FIELD_VAR, 301)
                    .setProperty(ORDER_FIELD_VAR, 0)
                    .setProperty("site", host.getIdentifier())
                    .host(host)
                    .nextPersistedAndPublish();

            final Task220330ChangeVanityURLSiteFieldType task = new Task220330ChangeVanityURLSiteFieldType();
            task.executeUpgrade();

            CacheLocator.getContentTypeCache2().remove(contentType);
            final ContentType contentTypeFromDBAfterTU = FactoryLocator.getContentTypeFactory()
                    .find(contentType.inode());

            final Optional<Field> siteFieldAfterTU = contentTypeFromDBAfterTU.fields().stream()
                    .filter(field -> field.variable().equals("site")).findFirst();

            assertTrue(siteFieldAfterTU.isPresent());
            assertEquals(siteFieldAfterTU.get().type(), HostFolderField.class);
            assertEquals(siteFieldAfterTU.get().dataType(), DataTypes.SYSTEM);

            assertContentlet(host, vanityURL_1);
            assertContentlet(host, vanityURL_2);

            final Contentlet vanityURL_3 = new ContentletDataGen(contentTypeFromDB)
                    .setProperty(TITLE_FIELD_VAR, "title_3")
                    .setProperty(URI_FIELD_VAR, "/uri/test_3")
                    .setProperty(FORWARD_TO_FIELD_VAR, "/foward_3")
                    .setProperty(ACTION_FIELD_VAR, 301)
                    .setProperty(ORDER_FIELD_VAR, 0)
                    .host(host)
                    .nextPersistedAndPublish();

            assertContentlet(host, vanityURL_3);
        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }

    /**
     * Method to test: {@link Task220330ChangeVanityURLSiteFieldType#executeUpgrade()}
     * When: The {@link Task220330ChangeVanityURLSiteFieldType} is ru twice the second time is fail because the
     * {@link Task220330ChangeVanityURLSiteFieldType#GET_CONTENTLET_NOT_JSON} has a wrong sintax
     * Should: Should not fail and not execute the SQL query
     *
     * @throws DotDataException
     */
    @Test
    public void runTUTwice() throws DotDataException {
        final Task220330ChangeVanityURLSiteFieldType task = new Task220330ChangeVanityURLSiteFieldType();
        task.executeUpgrade();
        task.executeUpgrade();
    }

    private void assertContentlet(Host host, Contentlet vanityURL_1) {
        final Optional<Contentlet> inDb = APILocator.getContentletAPI()
                .findInDb(vanityURL_1.getInode());

        assertTrue(inDb.isPresent());
        assertEquals(inDb.get().getHost(), host.getIdentifier());
    }
}
