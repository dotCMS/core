package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertTrue;

public class Task05210CreateDefaultDotAssetTest {

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
        final String [] fieldInodes = Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_FIELD_INODES;
        for (final String fieldInode : fieldInodes) {
            try {
                new DotConnect().setSQL("delete from field where inode = ?")
                        .addParam(fieldInode).loadResult();
            } catch (DotDataException e) {
                //Nah.
            }
        }

        for (final String fieldInode : fieldInodes) {
            try {
                new DotConnect().setSQL("delete from inode where inode = ?")
                        .addParam(fieldInode).loadResult();
            } catch (DotDataException e) {
                //Nah.
            }
        }

        new DotConnect().setSQL("delete from structure where inode = ?")
                .addParam(Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_INODE).loadResult();

        new DotConnect().setSQL("delete from inode where inode = ?")
                .addParam(Task05210CreateDefaultDotAsset.DOTASSET_VARIABLE_INODE).loadResult();
    }

    @Test
    public void Test_Upgrade_Task() throws DotDataException {
        removeConstraintIfAny();
        final Task05210CreateDefaultDotAsset task =  new Task05210CreateDefaultDotAsset();
        assertTrue(task.forceRun());
        task.executeUpgrade();
    }



}
