package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;
import java.util.List;

public class Task04225UpdateSystemFolderIdentifier extends AbstractJDBCStartupTask {

    public static final String SYSTEM_FOLDER_IDENTIFIER = "bc9a1d37-dd2d-4d49-a29d-0c9be740bfaf";
    public static final String UPDATE_FOLDER_QUERY = "update folder set identifier = ? where inode = 'SYSTEM_FOLDER'";
    public static final String UPDATE_IDENTIFIER_QUERY = "update identifier set id = ? where asset_name = 'system folder'";
    public static final String DROP_CONSTRAINT_QUERY = "ALTER TABLE Folder drop constraint folder_identifier_fk";
    public static final String CREATE_CONSTRAINT_QUERY = "ALTER TABLE Folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id)";



    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public void executeUpgrade() throws DotDataException {
        DotConnect dc = new DotConnect();

        //Drop folder_identifier_fk
        dc.setSQL(DROP_CONSTRAINT_QUERY);
        Logger.info(this, "Executing Query: " + dc.getSQL());
        dc.loadResult();

        //Update identifier table
        dc = new DotConnect();
        dc.setSQL(UPDATE_IDENTIFIER_QUERY);
        dc.addParam(SYSTEM_FOLDER_IDENTIFIER);
        Logger.info(this, "Executing Query: " + dc.getSQL());
        dc.loadResult();

        //Update folder table
        dc = new DotConnect();
        dc.setSQL(UPDATE_FOLDER_QUERY);
        dc.addParam(SYSTEM_FOLDER_IDENTIFIER);
        Logger.info(this, "Executing Query: " + dc.getSQL());
        dc.loadResult();

        //Add folder_identifier_fk
        dc = new DotConnect();
        dc.setSQL(CREATE_CONSTRAINT_QUERY);
        Logger.info(this, "Executing Query: " + dc.getSQL());
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
        return null;
    }
}
