package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task211101AddContentletAsJsonColumnTest {

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

        final DotConnect dotConnect = new DotConnect();
        try {
            dotConnect.setSQL("alter table contentlet drop column contentlet_as_json");
            dotConnect.loadResult();
        }catch (DotDataException e){
            //Nah.
        }
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeConstraintIfAny();
        final Task211101AddContentletAsJsonColumn task = new Task211101AddContentletAsJsonColumn();
        assertTrue(task.forceRun());

        task.executeUpgrade();
        final DotConnect dotConnect = new DotConnect();
        dotConnect
                .setSQL("select * from contentlet")
                .loadObjectResults()
                .forEach(rowMap -> {
                    assertTrue(rowMap.containsKey("contentlet_as_json"));
                    assertNull(rowMap.get("contentlet_as_json"));
                });
    }

}
