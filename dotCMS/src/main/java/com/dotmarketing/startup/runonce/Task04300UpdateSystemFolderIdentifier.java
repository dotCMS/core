package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Task04300UpdateSystemFolderIdentifier extends AbstractJDBCStartupTask {

    public static final String SYSTEM_FOLDER_IDENTIFIER = FolderAPI.OLD_SYSTEM_FOLDER_ID;

    public static final String UPDATE_IDENTIFIER_NAME =
            "update identifier set asset_name = ?, parent_path = ? " +
            "where id = (select identifier from folder where inode = ?)";
    public static final String UPDATE_IDENTIFIER_QUERY =
            "update identifier set id = ? where id = (select identifier from folder where inode = ?)";
    public static final String UPDATE_FOLDER_QUERY = "update folder set identifier = ? where inode = ?";

    public static final String DROP_CONSTRAINT_QUERY = "ALTER TABLE folder drop constraint folder_identifier_fk";
    public static final String MYSQL_DROP_CONSTRAINT_QUERY = "ALTER TABLE folder DROP FOREIGN KEY folder_identifier_fk";
    public static final String CREATE_CONSTRAINT_QUERY = "ALTER TABLE folder add constraint folder_identifier_fk foreign key (identifier) " +
            "references identifier(id)";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    @WrapInTransaction
    public void executeUpgrade() throws DotDataException {
        DotConnect dc = new DotConnect();

        try {
            //Check if FK: folder_identifier_fk exists
            boolean foundFK = false;
            final List<ForeignKey> listForeignKeys = this.getForeingKeys(DbConnectionFactory.getConnection(),
                    Arrays.asList("folder"), false);
            for (ForeignKey key : listForeignKeys) {
                if ("folder_identifier_fk".equalsIgnoreCase(key.fkName())) {
                    foundFK = true;
                    break;
                }
            }

            //Drop FK: folder_identifier_fk
            if (foundFK) {
                DbConnectionFactory.getConnection().setAutoCommit(true);
                if (DbConnectionFactory.isMySql()) {
                    dc.setSQL(MYSQL_DROP_CONSTRAINT_QUERY);
                } else {
                    dc.setSQL(DROP_CONSTRAINT_QUERY);
                }
                Logger.info(this, "Executing drop constraint query: " + dc.getSQL());
                dc.loadResult();
                DbConnectionFactory.getConnection().setAutoCommit(false);
            }
        } catch (SQLException e) {
            Logger.error(this,"Error dropping constraint: " + e.getMessage());
            throw new DotDataException(e);
        }

        //Update the Ids
        updateFolderIDs();

        //Add FK: folder_identifier_fk
        try {
            dc = new DotConnect();
            DbConnectionFactory.getConnection().setAutoCommit(true);
            dc.setSQL(CREATE_CONSTRAINT_QUERY);
            Logger.info(this, "Executing create constraint query: " + dc.getSQL());
            dc.loadResult();
            DbConnectionFactory.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            Logger.error(this,"Error creating constraint: " + e.getMessage());
            throw new DotDataException(e);
        }
    }

    @WrapInTransaction
    private void updateFolderIDs() throws DotDataException {
        //Update identifier asset name and parent path to the default ones
        DotConnect dc = new DotConnect()
            .setSQL(UPDATE_IDENTIFIER_NAME)
            .addParam(FolderAPI.SYSTEM_FOLDER_ASSET_NAME)
            .addParam(FolderAPI.SYSTEM_FOLDER_PARENT_PATH)
            .addParam(FolderAPI.SYSTEM_FOLDER);
        Logger.info(this, "Executing Update identifier name and path query: " + dc.getSQL());
        dc.loadResult();

        //Update identifier table
        dc = new DotConnect()
            .setSQL(UPDATE_IDENTIFIER_QUERY)
            .addParam(SYSTEM_FOLDER_IDENTIFIER)
            .addParam(FolderAPI.SYSTEM_FOLDER);
        Logger.info(this, "Executing Update identifier query: " + dc.getSQL());
        dc.loadResult();

        //Update folder table
        dc = new DotConnect()
            .setSQL(UPDATE_FOLDER_QUERY)
            .addParam(SYSTEM_FOLDER_IDENTIFIER)
            .addParam(FolderAPI.SYSTEM_FOLDER);
        Logger.info(this, "Executing update folder query: " + dc.getSQL());
        dc.loadResult();
    }

    @Override
    public String getPostgresScript() {
        return null;
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return null;
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }
}
