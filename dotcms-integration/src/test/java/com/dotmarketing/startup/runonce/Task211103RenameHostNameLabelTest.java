package com.dotmarketing.startup.runonce;

import static com.dotmarketing.startup.runonce.Task211103RenameHostNameLabel.UPDATE_FIELD_NAME;
import static com.dotmarketing.startup.runonce.Task211103RenameHostNameLabel.findFieldInode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.Connection;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task211103RenameHostNameLabelTest {

    static final String FIND_FIELD_NAME_BY_INODE = "select f.field_Name from structure s join field f on s.inode = f.structure_inode where s.name = 'Host' and f.velocity_var_name = 'hostName'";

    static final String NAME = "Site Name Blah Blah.";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    static void updateField(final DotConnect dotConnect, final String inode)
            throws DotDataException {
        dotConnect.setSQL(UPDATE_FIELD_NAME).addParam(NAME).addParam(inode).loadObjectResults();
    }

    @Test
    public void testUpgradeTask() throws DotDataException {

        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);

            try {
                final DotConnect dotConnect = new DotConnect();

                final String fieldInode = findFieldInode(dotConnect);
                updateField(dotConnect, fieldInode);

                final Task211103RenameHostNameLabel task = new Task211103RenameHostNameLabel();
                assertTrue(task.forceRun());
                task.executeUpgrade();

                final String fieldKey =  dotConnect.setSQL(FIND_FIELD_NAME_BY_INODE).getString("field_Name");
                assertEquals(Task211103RenameHostNameLabel.SITE_KEY_COLUMN_NAME,fieldKey);

            }finally {
                conn.setAutoCommit(false);
                conn.close();
            }

        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }

    }

}
