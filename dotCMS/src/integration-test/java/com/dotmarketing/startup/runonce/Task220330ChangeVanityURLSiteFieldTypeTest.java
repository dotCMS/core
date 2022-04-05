package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220330ChangeVanityURLSiteFieldTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_upgradeTask_success() throws DotDataException, DotSecurityException {
        final ContentType contentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.VANITY_URL)
                .nextPersisted();

        final DotConnect dotConnect = new DotConnect();
        final String updateSQL =  "UPDATE field SET field_type = 'com.dotcms.contenttype.model.field.CustomField', field_contentlet = 'text' "
                + "WHERE velocity_var_name = 'site' and structure_inode = ?";

        dotConnect.setSQL(updateSQL);
        dotConnect.addParam(contentType.inode());
        dotConnect.loadResult();

        CacheLocator.getContentTypeCache2().remove(contentType);
        final ContentType contentTypeFromDB = FactoryLocator.getContentTypeFactory().find(contentType.inode());

        final Optional<Field> siteField = contentTypeFromDB.fields().stream()
                .filter(field -> field.variable().equals("site")).findFirst();

        assertTrue(siteField.isPresent());
        assertEquals(siteField.get().type(), CustomField.class);
        assertEquals(siteField.get().dataType(), DataTypes.TEXT);

        final Task220330ChangeVanityURLSiteFieldType task = new Task220330ChangeVanityURLSiteFieldType();
        task.executeUpgrade();

        CacheLocator.getContentTypeCache2().remove(contentType);
        final ContentType contentTypeFromDBAfterTU = FactoryLocator.getContentTypeFactory().find(contentType.inode());

        final Optional<Field> siteFieldAfterTU = contentTypeFromDBAfterTU.fields().stream()
                .filter(field -> field.variable().equals("site")).findFirst();

        assertTrue(siteFieldAfterTU.isPresent());
        assertEquals(siteFieldAfterTU.get().type(), HostFolderField.class);
        assertEquals(siteFieldAfterTU.get().dataType(), DataTypes.SYSTEM);
    }
}
