package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;

/**
 * @author Nollymar Longa
 */
public class Task03565FixContainerVersionsCheck implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        DotConnect dc = new DotConnect();
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            if (DbConnectionFactory.isPostgres()) {
                String statement = "DROP TRIGGER IF EXISTS container_versions_check_trigger on dot_containers;\n " +
                    "CREATE OR REPLACE FUNCTION container_versions_check() RETURNS trigger AS '\n" +
                    "DECLARE\n" +
                    "versionsCount integer;\n" +
                    "BEGIN\n" +
                    "IF (tg_op = ''DELETE'') THEN\n" +
                    "select count(*) into versionsCount from dot_containers where identifier = OLD.identifier;\n" +
                    "IF (versionsCount = 0)THEN\n" +
                    "DELETE from identifier where id = OLD.identifier;\n" +
                    "ELSE\n" +
                    "RETURN OLD;\n" +
                    "END IF;\n" +
                    "END IF;\n" +
                    "RETURN NULL;\n" +
                    "END\n" +
                    "' LANGUAGE plpgsql;\n" +
                    "CREATE TRIGGER container_versions_check_trigger AFTER DELETE\n" +
                    "ON dot_containers FOR EACH ROW\n" +
                    "EXECUTE PROCEDURE container_versions_check();\n";

                dc.executeStatement(statement);
            }
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

}
