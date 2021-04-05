package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.List;

/**
 * Upgrade task used to create tables: `storage_group`, `storage`, `storage_data` and `storage_x_data`
 */
public class Task210319CreateStorageTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        try {
            DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "storage_group")
                    && !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "storage")
                    && !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "storage_data")
                    && !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "storage_x_data");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public String getPostgresScript() {
        return "create table storage_group (\n"
                + "    group_name varchar(255)  not null,\n"
                + "    mod_date   timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,\n"
                + "    PRIMARY KEY (group_name)\n"
                + ");\n"
                + "\n"
                + "create table storage (\n"
                + "    path        varchar(255) not null,\n"
                + "    group_name varchar(255) not null,\n"
                + "    hash       varchar(64) not null,\n"
                + "    metadata   text not null,\n"
                + "    mod_date   timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,\n"
                + "    PRIMARY KEY (path, group_name),\n"
                + "    FOREIGN KEY (group_name) REFERENCES storage_group (group_name)\n"
                + ");\n"
                + "\n"
                + "CREATE INDEX idx_storage_hash ON storage (hash);\n"
                + "\n"
                + "create table storage_data (\n"
                + "    hash_id  varchar(64) not null,\n"
                + "    data     bytea not null,\n"
                + "    mod_date timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,\n"
                + "    PRIMARY KEY (hash_id)\n"
                + ");\n"
                + "\n"
                + "create table storage_x_data (\n"
                + "    storage_hash varchar(64)                 not null,\n"
                + "    data_hash    varchar(64)                 not null,\n"
                + "    data_order   integer                     not null,\n"
                + "    mod_date     timestamp without time zone NOT NULL DEFAULT CURRENT_DATE,\n"
                + "    PRIMARY KEY (storage_hash, data_hash),\n"
                + "    FOREIGN KEY (data_hash) REFERENCES storage_data (hash_id)\n"
                + ");";
    }

    @Override
    public String getMySQLScript() {
        return "create table storage_group (\n"
                + "    group_name varchar(255)  not null,\n"
                + "    mod_date   TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,\n"
                + "    PRIMARY KEY (group_name)\n"
                + ");\n"
                + "\n"
                + "create table storage (\n"
                + "    path       varchar(255) not null,\n"
                + "    group_name varchar(255) not null,\n"
                + "    hash       varchar(64) not null,\n"
                + "    mod_date   TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,\n"
                + "    hash_ref   varchar(64),\n"
                + "    PRIMARY KEY (path, group_name),\n"
                + "    FOREIGN KEY (group_name) REFERENCES storage_group (group_name)\n"
                + ");\n"
                + "\n"
                + "CREATE INDEX idx_storage_hash ON storage (hash);\n"
                + "\n"
                + "create table storage_data (\n"
                + "    hash_id  varchar(64) not null,\n"
                + "    data     MEDIUMBLOB not null,\n"
                + "    mod_date TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,\n"
                + "    PRIMARY KEY (hash_id)\n"
                + ");\n"
                + "\n"
                + "create table storage_x_data (\n"
                + "    storage_hash varchar(64) not null,\n"
                + "    data_hash    varchar(64) not null,\n"
                + "    data_order   integer  not null,\n"
                + "    mod_date     TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,\n"
                + "    PRIMARY KEY (storage_hash, data_hash),\n"
                + "    FOREIGN KEY (data_hash) REFERENCES storage_data (hash_id)\n"
                + ");";
    }

    @Override
    public String getOracleScript() {
        return "CREATE TABLE  storage_group (\n"
                + "    group_name varchar(255)  NOT NULL,\n"
                + "    mod_date  TIMESTAMP  DEFAULT CURRENT_TIMESTAMP NOT NULL,\n"
                + "    PRIMARY KEY (group_name)\n"
                + ");\n"
                + "\n"
                + "CREATE TABLE storage (\n"
                + "    path       varchar(255) NOT NULL,\n"
                + "    group_name varchar(255) NOT NULL,\n"
                + "    hash       varchar(64) NOT NULL,\n"
                + "    mod_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n"
                + "    hash_ref   varchar(64),\n"
                + "    PRIMARY KEY (path, group_name),\n"
                + "    FOREIGN KEY (group_name) REFERENCES storage_group (group_name)\n"
                + ");\n"
                + "\n"
                + "CREATE INDEX idx_storage_hash ON storage (hash);\n"
                + "\n"
                + "CREATE TABLE storage_data (\n"
                + "    hash_id  varchar(64) NOT NULL,\n"
                + "    data     BLOB NOT NULL,\n"
                + "    mod_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ,\n"
                + "    PRIMARY KEY (hash_id)\n"
                + ");\n"
                + "\n"
                + "CREATE TABLE storage_x_data (\n"
                + "    storage_hash varchar(64) NOT NULL,\n"
                + "    data_hash    varchar(64) NOT NULL,\n"
                + "    data_order   INTEGER  NOT NULL,\n"
                + "    mod_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,\n"
                + "    PRIMARY KEY (storage_hash, data_hash),\n"
                + "    FOREIGN KEY (data_hash) REFERENCES storage_data (hash_id)\n"
                + ");";
    }

    @Override
    public String getMSSQLScript() {
        return "create table storage_group (\n"
                + "    group_name varchar(255)  not null,\n"
                + "    mod_date   datetime NOT NULL DEFAULT GETDATE(),\n"
                + "    PRIMARY KEY (group_name)\n"
                + ");\n"
                + "\n"
                + "create table storage (\n"
                + "    path       varchar(255) not null,\n"
                + "    group_name varchar(255) not null,\n"
                + "    hash       varchar(64) not null,\n"
                + "    mod_date   datetime  NOT NULL DEFAULT GETDATE(),\n"
                + "    hash_ref   varchar(64),\n"
                + "    PRIMARY KEY (path, group_name),\n"
                + "    FOREIGN KEY (group_name) REFERENCES storage_group (group_name)\n"
                + ");\n"
                + "\n"
                + "CREATE INDEX idx_storage_hash ON storage (hash);\n"
                + "\n"
                + "create table storage_data (\n"
                + "    hash_id  varchar(64) not null,\n"
                + "    data     varbinary(max) not null,\n"
                + "    mod_date datetime NOT NULL DEFAULT GETDATE(),\n"
                + "    PRIMARY KEY (hash_id)\n"
                + ");\n"
                + "\n"
                + "create table storage_x_data (\n"
                + "    storage_hash varchar(64)                 not null,\n"
                + "    data_hash    varchar(64)                 not null,\n"
                + "    data_order   integer                     not null,\n"
                + "    mod_date     datetime NOT NULL DEFAULT GETDATE(),\n"
                + "    PRIMARY KEY (storage_hash, data_hash),\n"
                + "    FOREIGN KEY (data_hash) REFERENCES storage_data (hash_id)\n"
                + ");\n";
    }

    @Override
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
