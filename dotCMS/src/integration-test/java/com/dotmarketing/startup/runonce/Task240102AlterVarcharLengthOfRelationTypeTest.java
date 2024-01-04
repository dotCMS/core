package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotRuntimeException;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Upgrade Task {@link Task240102AlterVarcharLengthOfRelationType} is working as expected.
 *
 * @author Andrey Melendez
 * @since Jan 2nd, 2023
 */
public class Task240102AlterVarcharLengthOfRelationTypeTest {

    private void setOldLengthBeforeTask(){

        final String lengthBeforeTask = "64";
        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.executeStatement("ALTER TABLE tree ALTER COLUMN relation_type type varchar ("+ lengthBeforeTask +")");
        } catch (SQLException e) {
            throw new DotRuntimeException(e);
        }

    }

    @Test
    public void test_executeUpgrade_IncreaseVarcharLengthOfRelationType() throws Exception {
        Map<String, String> result;
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();

        setOldLengthBeforeTask();
        result = dotDatabaseMetaData.getModifiedColumnLength("tree", "relation_type");
        assertEquals("64", result.get("field_length"));

        // Execute the task
        final Task240102AlterVarcharLengthOfRelationType task240102AlterVarcharLengthOfRelationType =
                new Task240102AlterVarcharLengthOfRelationType();
        task240102AlterVarcharLengthOfRelationType.executeUpgrade();

        //Check if the length was increased to 255
        result = dotDatabaseMetaData.getModifiedColumnLength("tree", "relation_type");
        assertEquals("255", result.get("field_length"));
    }




}
