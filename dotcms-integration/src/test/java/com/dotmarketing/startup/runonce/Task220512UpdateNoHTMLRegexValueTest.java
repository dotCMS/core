package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220512UpdateNoHTMLRegexValueTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws Exception {
        //Create new content type with no html field
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        new FieldDataGen().contentTypeId(contentType.id()).dataType(DataTypes.TEXT)
                .regexCheck(Task220512UpdateNoHTMLRegexValue.OLD_NOHTML_REGEX).nextPersisted();

        //Test upgrade
        final Task220512UpdateNoHTMLRegexValue upgradeTask = new Task220512UpdateNoHTMLRegexValue();
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertFalse(upgradeTask.forceRun());
    }
}
