package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Task201016UpdatePrimaryKeyLengthIdentifierTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        final Connection connection = DbConnectionFactory.getConnection();
        try {
            final ResultSet resultSet = DotDatabaseMetaData.getColumnsMetaData(connection, "identifier");

            while (resultSet.next()) {
                final String columnName = resultSet.getString("COLUMN_NAME");

                if (columnName.equals("id")) {
                    final int columnSize;
                    columnSize = resultSet.getInt("COLUMN_SIZE");
                    return columnSize == 750;
                }
            }
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
        }
        return false;
    }


    @Override
    public String getPostgresScript() {
        return "ALTER TABLE identifier ALTER COLUMN id TYPE varchar(750);";
    }

    @Override
    public String getMySQLScript() {
        return "ALTER TABLE identifier MODIFY id varchar(750);";

    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE identifier MODIFY id varchar2(750);";
    }

    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE identifier ALTER COLUMN id TYPE NVARCHAR(750);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }
}