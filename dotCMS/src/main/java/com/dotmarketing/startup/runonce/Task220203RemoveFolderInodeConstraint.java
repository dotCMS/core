package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.ForeignKey;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Upgrade task to remove foreign key `fkb45d1c6e5fb51eb`
 */
public class Task220203RemoveFolderInodeConstraint implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        if (DbConnectionFactory.isMsSql() && !DbConnectionFactory.getAutoCommit()) {
            DbConnectionFactory.setAutoCommit(true);
        }

        final DotDatabaseMetaData metaData = new DotDatabaseMetaData();
        final ForeignKey foreignKey        = metaData.findForeignKeys
                ("folder", "inode",
                        Arrays.asList("inode"), Arrays.asList("inode"));

        if (null != foreignKey) {

            try {
                Logger.info(this, "Droping the FK: " + foreignKey);
                metaData.dropForeignKey(foreignKey);
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        }
    }
}
