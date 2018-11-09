package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;

public class Task05035LanguageTableIdentityOff implements StartupTask  {


    private static final String MY_SQL_ALTER_TABLE_LANGUAGE_DROP_AUTO_INCREMENT =
             "SET foreign_key_checks = 0;\n" +
             "ALTER TABLE language MODIFY id BIGINT, DROP PRIMARY KEY, ADD PRIMARY KEY (id);\n" +
             "SET foreign_key_checks = 1;";

    private static final String MS_SQL_SET_IDENTITY_INSERT_LANGUAGE_OFF = " SET IDENTITY_INSERT language OFF; ";

    private static final String POSTGRES_DROP_SEQUENCE = " DROP SEQUENCE IF EXISTS language_seq CASCADE; ALTER TABLE language ALTER COLUMN id DROP default; ";

    private static final String ORACLE_CHECK_SEQUENCE_EXIST = " SELECT sequence_name FROM all_sequences WHERE lower(sequence_name) = 'language_seq'; ";
    private static final String ORACLE_DROP_SEQUENCE = " DROP SEQUENCE language_seq; ";

    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    private boolean checkSequenceExistsOracle(final DotConnect dotConnect) throws DotDataException, DotRuntimeException {
        dotConnect.setSQL(ORACLE_CHECK_SEQUENCE_EXIST);
        return !dotConnect.loadObjectResults().isEmpty();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.info(this, "Drop Auto-increment/Identity from `Language` Table definition.");
        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();

            conn.setAutoCommit(true);

            final DotConnect dotConnect = new DotConnect();

            if (DbConnectionFactory.isMsSql()) {
                dotConnect.setSQL(MS_SQL_SET_IDENTITY_INSERT_LANGUAGE_OFF);
            } else if (DbConnectionFactory.isMySql()) {
                dotConnect.setSQL(MY_SQL_ALTER_TABLE_LANGUAGE_DROP_AUTO_INCREMENT);
            } else if (DbConnectionFactory.isPostgres()) {
                dotConnect.setSQL(POSTGRES_DROP_SEQUENCE);
            } else if (DbConnectionFactory.isOracle()) {
                 if(!checkSequenceExistsOracle(dotConnect)){
                   return;
                 }
                dotConnect.setSQL(ORACLE_DROP_SEQUENCE);
            }

            dotConnect.loadResult();

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

}
