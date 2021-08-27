package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.RelationshipDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210816DeInodeRelationshipTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException {
        final Task210816DeInodeRelationship upgradeTask = new Task210816DeInodeRelationship();

        final DotConnect dotConnect = new DotConnect();
        // re-create the data in inode for already existing rels
        dotConnect.executeStatement("INSERT INTO inode SELECT inode, null, null, 'relationship' FROM relationship");

        // create some rels
        List<String> relInodes = new ArrayList<>();

        for(int i=0; i<10; i++) {
            relInodes.add(new RelationshipDataGen(true).nextPersisted().getInode());
        }

        final Date now = new Date();
        // insert reference in inode
        for (String relInode : relInodes) {
            dotConnect.setSQL("INSERT INTO inode VALUES (?, ?, ?, ?)");
            dotConnect.addParam(relInode)
                    .addParam((String)null)
                    .addParam(now)
                    .addParam(Type.RELATIONSHIP.getValue());
            dotConnect.loadResult();
        }

        //Remove column if exists
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        if (dotDatabaseMetaData.hasColumn("relationship", "mod_date")) {
            dotDatabaseMetaData
                    .dropColumn(DbConnectionFactory.getConnection(), "relationship", "mod_date");
        }

        // Create FK if does not exist
        if (upgradeTask.findRelationshipInodeFK()==null) {
            dotConnect.executeStatement("alter table relationship add constraint "
                    + "fkf06476385fb51eb foreign key (inode) references inode;\n");
        }


        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
        assertTrue(upgradeTask.hasModDateColumn()); // mod_date created
        assertNull(upgradeTask.findRelationshipInodeFK()); // FK dropped
        assertTrue(new DotConnect().setSQL("SELECT * FROM inode where type = 'relationship'")
                .loadObjectResults().isEmpty());
        assertTrue(new DotConnect().setSQL("SELECT * FROM inode WHERE EXISTS(SELECT 1 FROM relationship r WHERE r.inode = inode.inode)")
                .loadObjectResults().isEmpty());

    }


}
