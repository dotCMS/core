package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * This upgrade task adds "left" indexes to on the inode.inode and the identifier.id columns.  This
 * GREATLY speeds up queries that use `like 'param%'` clauses in their queries, where we only wildcard
 * on the right hand side.
 */
public class Task240111AddInodeAndIdentifierLeftIndexes implements StartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL("CREATE INDEX if not exists inode_inode_leading_idx ON inode(inode  COLLATE \"C\");").loadResult();

        new DotConnect().setSQL("CREATE INDEX if not exists identifier_id_leading_idx ON identifier(id  COLLATE \"C\");").loadResult();

    }

}
