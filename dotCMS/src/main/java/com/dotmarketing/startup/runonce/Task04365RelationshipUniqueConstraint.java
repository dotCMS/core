package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class Task04365RelationshipUniqueConstraint extends AbstractJDBCStartupTask {


    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }

    private static final String ORACLE_ADD_UNIQUE_CONSTRAINT = "ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value)";

    @Override
    public String getPostgresScript() {
        final StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE FROM relationship R1 ")
            .append("USING relationship R2 ")
            .append("WHERE R1.inode < R2.inode ")
            .append("AND R1.relation_type_value = R2.relation_type_value; ")

            //Delete orphaned inodes
            .append("DELETE FROM inode WHERE inode.type = 'relationship' AND NOT EXISTS (")
            .append("  SELECT 1 FROM relationship WHERE inode.inode = relationship.inode); ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getMySQLScript() {
        final StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE R1 ")
            .append("FROM relationship AS R1, relationship AS R2 ")
            .append("WHERE R1.inode < R2.inode ")
            .append("AND R1.relation_type_value = R2.relation_type_value; ")

            //Delete orphaned inodes
            .append("DELETE FROM inode WHERE inode.type = 'relationship' AND NOT EXISTS (")
            .append("  SELECT 1 FROM relationship WHERE inode.inode = relationship.inode); ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getMSSQLScript() {
        final StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE R1 FROM ( ")
            .append("  Select *, ROW_NUMBER() over (Partition by relation_type_value order by relation_type_value) as rowNumber ")
            .append("  From relationship ) R1  ")
            .append("WHERE R1.rowNumber > 1; ")

            //Delete orphaned inodes
            .append("DELETE FROM inode WHERE inode.type = 'relationship' AND NOT EXISTS (")
            .append("  SELECT 1 FROM relationship WHERE inode.inode = relationship.inode); ")

            //Create Unique Key
            .append("ALTER TABLE relationship ADD CONSTRAINT unique_relation_type_value UNIQUE (relation_type_value); ");
        return builder.toString();
    }

    @Override
    public String getOracleScript() {
        final StringBuilder builder = new StringBuilder()
            //Delete duplicates
            .append("DELETE FROM relationship R1 ")
            .append("WHERE R1.ROWID > ANY ( ")
            .append("  SELECT R2.ROWID ")
            .append("  FROM relationship R2 ")
            .append("  WHERE R1.relation_type_value = R2.relation_type_value ); ")

            //Delete orphaned inodes
            .append("DELETE FROM inode WHERE inode.type = 'relationship' AND NOT EXISTS (")
            .append("  SELECT 1 FROM relationship WHERE inode.inode = relationship.inode); ");

            //Create Unique Key deferred to method executeUpgrade below, because DML and DDL require different connections
        return builder.toString();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        //Run upgrade as usual
        super.executeUpgrade();

        //For Oracle the Alter (DDL) needs to be executed on a different connection
        Connection conn = null;
        try {
            if (DbConnectionFactory.isOracle()) {

                conn = DbConnectionFactory.getDataSource().getConnection();
                conn.setAutoCommit(true);

                Logger.info(this, "Executing: "+ ORACLE_ADD_UNIQUE_CONSTRAINT);
                final DotConnect db = new DotConnect();
                db.setSQL(ORACLE_ADD_UNIQUE_CONSTRAINT);
                db.loadResult(conn);
                Logger.info(this, "Finished Executing: "+ ORACLE_ADD_UNIQUE_CONSTRAINT);
            }
        } catch(Exception e) {
            throw new DotDataException(e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        }
    }
}
