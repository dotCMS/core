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
import java.util.Collections;
import java.util.List;

public class Task04225UpdateSystemFolderIdentifier extends AbstractJDBCStartupTask {

    public static final String SYSTEM_FOLDER_IDENTIFIER = FolderAPI.SYSTEM_FOLDER_ID;
    public static final String UPDATE_FOLDER_QUERY = "update folder set identifier = ? where inode = 'SYSTEM_FOLDER'";
    public static final String UPDATE_IDENTIFIER_QUERY = "update identifier set id = ? where asset_name = 'system folder'";
    public static final String DROP_CONSTRAINT_QUERY = "ALTER TABLE Folder drop constraint folder_identifier_fk";
    public static final String MYSQL_DROP_CONSTRAINT_QUERY = "ALTER TABLE Folder DROP FOREIGN KEY folder_identifier_fk";
    public static final String CREATE_CONSTRAINT_QUERY = "ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) " +
            "references identifier(id)";

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {
        DotConnect dc = new DotConnect();

        //Drop folder_identifier_fk
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            if (DbConnectionFactory.isMySql()) {
                dc.setSQL(MYSQL_DROP_CONSTRAINT_QUERY);
            } else {
                dc.setSQL(DROP_CONSTRAINT_QUERY);
            }
            Logger.info(this, "Executing drop constraint query: " + dc.getSQL());
            dc.loadResult();
            DbConnectionFactory.getConnection().setAutoCommit(false);
        } catch (SQLException e) {
            Logger.error(this,"Error dropping constraint: " + e.getMessage());
            throw new DotDataException(e);
        }

        //Update the Ids
        updateFolderIDs();

        //Add folder_identifier_fk
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
        //Update identifier table
        DotConnect dc = new DotConnect();
        dc.setSQL(UPDATE_IDENTIFIER_QUERY);
        dc.addParam(SYSTEM_FOLDER_IDENTIFIER);
        Logger.info(this, "Executing Update identifier query: " + dc.getSQL());
        dc.loadResult();

        //Update folder table
        dc = new DotConnect();
        dc.setSQL(UPDATE_FOLDER_QUERY);
        dc.addParam(SYSTEM_FOLDER_IDENTIFIER);
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
    public String getH2Script() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return Collections.emptyList();
    }
}
