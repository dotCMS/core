package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05205UpdateIndexNameLengthTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private Map<String, String> getColumnPropertiesAfterUpgrade() throws DotDataException {
        String query;

        if (!DbConnectionFactory.getDBType().equals("Oracle")){
            query = "select character_maximum_length as field_length, is_nullable as nullable_value from "
                    + "information_schema.columns where table_name = 'indicies' "
                    + "and column_name='index_name'";
        } else {
            query = "SELECT DATA_LENGTH as field_length, NULLABLE as nullable_value "
                    + "FROM user_tab_columns WHERE table_name = 'INDICIES' "
                    + "AND column_name = 'INDEX_NAME'";
        }
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL(query);
        return (Map<String, String>)dotConnect.loadResults().get(0);

    }

    @Test
    public void testUpgradeTaskShouldPass() throws DotDataException {
        final Task05205UpdateIndexNameLength task = new Task05205UpdateIndexNameLength();

        task.executeUpgrade();

        final Map<String, String> result = getColumnPropertiesAfterUpgrade();
        assertEquals("100", result.get("field_length"));
        assertEquals(DbConnectionFactory.getDBType().equals("Oracle") ? "N" : "NO",
                result.get("nullable_value"));
    }

}
