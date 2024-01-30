package com.dotmarketing.startup.runonce;


import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task230523CreateVariantFieldInContentletIntegrationTest {


    @BeforeClass
    public static  void variantIdExists() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final Connection connection = DbConnectionFactory.getConnection();
        final ResultSet resultSet = DotDatabaseMetaData.getColumnsMetaData(connection, "contentlet");
        boolean existsVariantId = false;

        while(resultSet.next()){
            final String columnName = resultSet.getString("COLUMN_NAME");
            final String columnType = resultSet.getString(6);

            if (columnName.equals("variant_id")) {
                assertEquals(columnType, "varchar");
                existsVariantId = true;
            }
        }

        assertTrue(existsVariantId);
    }

    /**
     * Method to test: {@link Task230523CreateVariantFieldInContentlet#executeUpgrade()} and
     * {@link Task230523CreateVariantFieldInContentlet#forceRun()}
     * when: the variant_id column does not exist in the contentlet table.
     * should: create the variant_id column and set the default value to 'DEFAULT' also the method
     * forceRun should return true.
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void executeTaskUpgrade() throws SQLException, DotDataException, IOException {
        final DotConnect dotConnect = new DotConnect();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();

        final String asJson = APILocator.getContentletJsonAPI().toJson(contentlet_1);
        final Map<String, Object> contentletAsMap = JsonUtil.getJsonFromString(asJson);
        contentletAsMap.put(Contentlet.VARIANT_ID, "1");

        dotConnect.setSQL("UPDATE contentlet SET contentlet_as_json = ? WHERE inode = ?")
                .addJSONParam(contentletAsMap)
                .addParam(contentlet_1.getInode())
                .loadResults();

        dotConnect.setSQL("UPDATE contentlet SET contentlet_as_json = NULL WHERE inode = ?")
                .addParam(contentlet_2.getInode())
                .loadResults();

        final Task230523CreateVariantFieldInContentlet task230523CreateVariantFieldInContentlet =
                new Task230523CreateVariantFieldInContentlet();

        assertFalse(task230523CreateVariantFieldInContentlet.forceRun());


        dotConnect.executeStatement("ALTER TABLE contentlet DROP COLUMN variant_id");

        assertTrue(task230523CreateVariantFieldInContentlet.forceRun());

        task230523CreateVariantFieldInContentlet.executeUpgrade();

        assertFalse(task230523CreateVariantFieldInContentlet.forceRun());

        final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
        assertTrue(databaseMetaData.hasColumn("contentlet", "variant_id"));

        final ArrayList<Map<String, Object>> contentlets_1 = dotConnect.setSQL(
                        "SELECT contentlet_as_json, variant_id FROM contentlet WHERE inode = ?")
                .addParam(contentlet_1.getInode())
                .loadResults();

        assertEquals(1, contentlets_1.size());
        assertEquals("DEFAULT", contentlets_1.get(0).get("variant_id"));
        final Map<String, Object> contentletAsMapFromDB = JsonUtil.getJsonFromString(
                contentlets_1.get(0).get("contentlet_as_json").toString());
        assertNull(contentletAsMapFromDB.get(Contentlet.VARIANT_ID));


        final ArrayList<Map<String, Object>> contentlets_2 = dotConnect.setSQL(
                        "SELECT contentlet_as_json, variant_id FROM contentlet WHERE inode = ?")
                .addParam(contentlet_2.getInode())
                .loadResults();

        assertEquals(1, contentlets_2.size());
        assertEquals("DEFAULT", contentlets_2.get(0).get("variant_id"));

        final Object contentletAsJson = contentlets_2.get(0).get("contentlet_as_json");
        assertFalse(contentletAsJson != null && UtilMethods.isSet(contentletAsJson.toString()));
    }


}
