package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

public class Task201013AddNewColumnsToIdentifierTable extends AbstractJDBCStartupTask {
    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData dbMetadata = new DotDatabaseMetaData();
            return !dbMetadata.hasColumn("identifier", "owner") &&
                    !dbMetadata.hasColumn("identifier", "create_date") &&
                    !dbMetadata.hasColumn("identifier", "asset_subtype");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }


    @Override
    public String getPostgresScript() {
        return "alter table identifier add owner varchar(255);" +
                "alter table identifier add create_date timestamp;" +
                "alter table identifier add asset_subtype varchar(255);" +
                "create index idx_identifier_asset_subtype on identifier (asset_subtype);";

    }

    @Override
    public String getMySQLScript() {
        return "alter table identifier add owner varchar(255);" +
                "alter table identifier add create_date datetime;" +
                "alter table identifier add asset_subtype varchar(255);" +
                "create index idx_identifier_asset_subtype on identifier (asset_subtype);";

    }

    @Override
    public String getOracleScript() {
        return "alter table identifier add owner varchar2(255);" +
                "alter table identifier add create_date date;" +
                "alter table identifier add asset_subtype varchar2(255);" +
                "create index idx_identifier_asset_subtype on identifier (asset_subtype);";
    }

    @Override
    public String getMSSQLScript() {
        return  "alter table identifier add owner NVARCHAR(255);" +
                "alter table identifier add create_date datetime;" +
                "alter table identifier add asset_subtype NVARCHAR(255);" +
                "create index idx_identifier_asset_subtype on identifier (asset_subtype);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
