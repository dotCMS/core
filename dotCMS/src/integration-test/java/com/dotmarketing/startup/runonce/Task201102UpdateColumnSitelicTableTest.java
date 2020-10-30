package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Task201102UpdateColumnSitelicTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void removeConstraintIfAny() throws DotDataException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }

        //Certain db engines store unique constraints as indices
        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.setSQL("alter table sitelic drop column startup_time");
            dotConnect.loadResult();
        }catch (DotDataException e){
            //Nah.
        }
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeConstraintIfAny();
        final Task201102UpdateColumnSitelicTable task =  new Task201102UpdateColumnSitelicTable();
        assertTrue(task.forceRun());

        task.executeUpgrade();
        final DotConnect dotConnect = new DotConnect();
        dotConnect
                .setSQL("select * from sitelic")
                .loadObjectResults()
                .forEach(rowMap -> {
                    assertTrue(rowMap.containsKey("startup_time"));
                    assertNull(rowMap.get("startup_time"));
                });
    }

}
