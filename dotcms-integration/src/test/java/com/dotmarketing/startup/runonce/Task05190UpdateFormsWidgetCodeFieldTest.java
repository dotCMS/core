package com.dotmarketing.startup.runonce;

import static junit.framework.TestCase.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;

public class Task05190UpdateFormsWidgetCodeFieldTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Upgrade()
            throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final Task05190UpdateFormsWidgetCodeField task =
            new Task05190UpdateFormsWidgetCodeField();

        String inode = dotConnect
            .setSQL(Task05190UpdateFormsWidgetCodeField.SELECT_FORM_WIDGET_FIELD_INODE).getString("contentTypeId");
        



          assertTrue(task.forceRun());
          task.executeUpgrade();

          Contentlet con = new ContentletDataGen(inode).setProperty("formId", "testing").setProperty("widgetTitle", "testing").nextPersisted();
          assertTrue(con.getStringProperty(FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME).equals(task.REPLACEMENT_VELOCITY_CODE));
        




    }



}
