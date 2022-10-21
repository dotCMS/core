package com.dotmarketing.startup.runonce;

import static graphql.Assert.assertNull;
import static graphql.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task220824CreateDefaultVariantTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        checkIfVariantDefaultExists();
    }

    private static void checkIfVariantDefaultExists() throws DotDataException {

        final ArrayList results = new DotConnect().setSQL("SELECT * FROM variant WHERE name = 'DEFAULT'")
                .loadResults();

        assertEquals("The DEFAULT Variant should exists", 1, results.size());
    }
    /**
     * Method to test: {@link Task220824CreateDefaultVariant#executeUpgrade()}
     * when: the UT run
     * Should: Create the default variant and add a new field in the contentlet_version_info
     */
    @Test
    public void runningTU() throws DotDataException, SQLException {

        cleanAllBefore();

        final Task220824CreateDefaultVariant upgradeTask = new Task220824CreateDefaultVariant();
        upgradeTask.executeUpgrade();

        checkIfVariantDefaultExists();
    }

    @Test
    public void runningTwice() throws DotDataException {
        cleanAllBefore();

        final Task220824CreateDefaultVariant upgradeTask = new Task220824CreateDefaultVariant();
        upgradeTask.executeUpgrade();
        upgradeTask.executeUpgrade();
    }

    /**
     *  Method to test: {@link Task220824CreateDefaultVariant#forceRun()}
     *  When: the Default variant exists
     *  Should return false
     *  When: the Default variant does not  exists
     *  Should return true
     */
    @Test
    public void forceRun() throws DotDataException {
        cleanAllBefore();

        final Task220824CreateDefaultVariant upgradeTask = new Task220824CreateDefaultVariant();

        assertTrue(upgradeTask.forceRun());
        assertTrue(upgradeTask.forceRun());

        upgradeTask.executeUpgrade();

        assertFalse(upgradeTask.forceRun());
    }

    private void cleanAllBefore() {
        final DotConnect dotConnect = new DotConnect();

        try {
            dotConnect.setSQL("DELETE FROM variant WHERE name = ?")
                    .addParam(VariantAPI.DEFAULT_VARIANT.name())
                    .loadResult();
        } catch (Exception e) {

        }
    }
}
