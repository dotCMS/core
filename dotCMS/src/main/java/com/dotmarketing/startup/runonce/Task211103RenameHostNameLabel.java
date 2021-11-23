package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import java.sql.Connection;

public class Task211103RenameHostNameLabel implements StartupTask {

    static final String FIND_FIELD_BY_INODE = "select f.inode from structure s join field f on s.inode = f.structure_inode where s.name = 'Host' and f.velocity_var_name = 'hostName'";

    static final String UPDATE_FIELD_NAME = "update field set field_Name = ? where inode = ?";

    static final String SITE_KEY_COLUMN_NAME = "Site Key";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {
                final DotConnect dotConnect = new DotConnect();
                final String fieldInode = findFieldInode(dotConnect);
                updateFieldId(dotConnect,fieldInode);
            } finally {
                conn.setAutoCommit(false);
                conn.close();
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    static String findFieldInode(final DotConnect dotConnect){
       return dotConnect.setSQL(FIND_FIELD_BY_INODE).getString("inode");
    }

    static void updateFieldId(final DotConnect dotConnect, final String inode)
            throws DotDataException {
        dotConnect.setSQL(UPDATE_FIELD_NAME).addParam(SITE_KEY_COLUMN_NAME).addParam(inode).loadObjectResults();
    }

}
