package com.dotmarketing.startup.runonce;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nollymar Longa
 */
public class Task03565FixContainerVersionsCheck implements StartupTask {

    public boolean forceRun() {
        return true;
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        DotConnect dc = new DotConnect();
        List<String> statements = new ArrayList<>();
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            if (DbConnectionFactory.isPostgres()) {
                statements = getPostgresStatement();
            } else if (DbConnectionFactory.isMySql()) {
                statements = getMySQLStatement();
            } else if (DbConnectionFactory.isOracle()) {
                statements = getOracleStatement();
            } else if (DbConnectionFactory.isMsSql()) {
                statements = getMSSQLStatement();
            }

            for (String statement : statements) {
                dc.executeStatement(statement);
            }

        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private List<String> getPostgresStatement() {
        String statement = "DROP TRIGGER IF EXISTS container_versions_check_trigger on " +
            Inode.Type.CONTAINERS.getTableName() +
            ";\n " +
            "CREATE OR REPLACE FUNCTION container_versions_check() RETURNS trigger AS '\n" +
            "DECLARE\n" +
            "versionsCount integer;\n" +
            "BEGIN\n" +
            "IF (tg_op = ''DELETE'') THEN\n" +
            "select count(*) into versionsCount from " +
            Inode.Type.CONTAINERS.getTableName() +
            " where identifier = OLD.identifier;\n" +
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
            "ON " +
            Inode.Type.CONTAINERS.getTableName() +
            " FOR EACH ROW\n" +
            "EXECUTE PROCEDURE container_versions_check();\n";
        return SQLUtil.tokenize(statement);
    }

    private List<String> getMySQLStatement() {
        String statement = "DROP PROCEDURE IF EXISTS checkVersions;\n" +
            "CREATE PROCEDURE checkVersions(IN ident varchar(36),IN tableName VARCHAR(20),OUT versionsCount INT)\n" +
            "BEGIN\n" +
            "SET versionsCount := 0;\n" +
            "IF(tableName = 'htmlpage') THEN\n" +
            "select count(inode) into versionsCount from htmlpage where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = 'file_asset') THEN\n" +
            "select count(inode) into versionsCount from file_asset where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = 'links') THEN\n" +
            "select count(inode) into versionsCount from links where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = '" +
            Inode.Type.CONTAINERS.getTableName() +
            "') THEN\n" +
            "select count(inode) into versionsCount from " +
            Inode.Type.CONTAINERS.getTableName() +
            " where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = 'template') THEN\n" +
            "select count(inode) into versionsCount from template where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = 'contentlet') THEN\n" +
            "select count(inode) into versionsCount from contentlet where identifier = ident;\n" +
            "END IF;\n" +
            "IF(tableName = 'folder') THEN\n" +
            "select count(inode) into versionsCount from folder where identifier = ident;\n" +
            "END IF;\n" +
            "END\n" +
            "#" +
            "DROP TRIGGER IF EXISTS check_container_versions;\n" +
            "CREATE TRIGGER check_container_versions BEFORE DELETE\n" +
            "on " +
            Inode.Type.CONTAINERS.getTableName() +
            "\n" +
            "FOR EACH ROW\n" +
            "BEGIN\n" +
            "DECLARE tableName VARCHAR(20);\n" +
            "DECLARE count INT;\n" +
            "SET tableName = '" +
            Inode.Type.CONTAINERS.getTableName() +
            "';\n" +
            "CALL checkVersions(OLD.identifier,tableName,count);\n" +
            "IF(count = 0)THEN\n" +
            "delete from identifier where id = OLD.identifier;\n" +
            "END IF;\n" +
            "END\n" +
            "#\n";

        return SQLUtil.tokenize(statement);
    }

    private List<String> getOracleStatement() {
        String statement = "CREATE OR REPLACE PACKAGE container_pkg as\n" +
            "type array is table of " +
            Inode.Type.CONTAINERS.getTableName() +
            "%rowtype index by binary_integer;\n" +
            "oldvals array;\n" +
            "empty array;\n" +
            "END;\n" +
            "/\n" +
            "CREATE OR REPLACE TRIGGER container_versions_bd\n" +
            "BEFORE DELETE ON " +
            Inode.Type.CONTAINERS.getTableName() +
            "\n" +
            "BEGIN\n" +
            "container_pkg.oldvals := container_pkg.empty;\n" +
            "END;\n" +
            "/\n" +
            "CREATE OR REPLACE TRIGGER container_versions_bdfer\n" +
            "BEFORE DELETE ON " +
            Inode.Type.CONTAINERS.getTableName() +
            "\n" +
            "FOR EACH ROW\n" +
            "BEGIN\n" +
            "container_pkg.oldvals(container_pkg.oldvals.count+1).identifier := :old.identifier;\n" +
            "END;\n" +
            "/\n" +
            "CREATE OR REPLACE TRIGGER container_versions_trigger\n" +
            "AFTER DELETE ON " +
            Inode.Type.CONTAINERS.getTableName() +
            "\n" +
            "DECLARE\n" +
            "versionsCount integer;\n" +
            "BEGIN\n" +
            "for i in 1 .. container_pkg.oldvals.count LOOP\n" +
            "select count(*) into versionsCount from " +
            Inode.Type.CONTAINERS.getTableName() +
            " where identifier = container_pkg.oldvals(i).identifier;\n" +
            "IF (versionsCount = 0)THEN\n" +
            "DELETE from identifier where id = container_pkg.oldvals(i).identifier;\n" +
            "END IF;\n" +
            "END LOOP;\n" +
            "END;\n" +
            "/\n";
        return SQLUtil.tokenize(statement);
    }

    private List<String> getMSSQLStatement() {
        String statement = "drop trigger check_container_versions;\n" +
            "CREATE Trigger check_container_versions\n" +
            "ON " +
            Inode.Type.CONTAINERS.getTableName() +
            "\n" +
            "FOR DELETE AS\n" +
            "DECLARE @totalCount int\n" +
            "DECLARE @identifier varchar(36)\n" +
            "DECLARE container_cur_Deleted cursor LOCAL FAST_FORWARD for\n" +
            "Select identifier\n" +
            "from deleted\n" +
            "for Read Only\n" +
            "open container_cur_Deleted\n" +
            "fetch next from container_cur_Deleted into @identifier\n" +
            "while @@FETCH_STATUS <> -1\n" +
            "BEGIN\n" +
            "select @totalCount = count(*) from " +
            Inode.Type.CONTAINERS.getTableName() +
            " where identifier = @identifier\n" +
            "IF (@totalCount = 0)\n" +
            "BEGIN\n" +
            "DELETE from identifier where id = @identifier\n" +
            "END\n" +
            "fetch next from container_cur_Deleted into @identifier\n" +
            "END;\n";

        return SQLUtil.tokenize(statement);
    }

}
