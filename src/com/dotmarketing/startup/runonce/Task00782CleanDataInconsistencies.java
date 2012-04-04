package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;

/**
 * We try to clean up some orphan data
 * 
 * @author jorgeu
 */
public class Task00782CleanDataInconsistencies implements StartupTask {
    
    public boolean forceRun() {
        return true;
    }
    
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        DotConnect dc=new DotConnect();
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            dc.executeStatement("delete from inode where identifier not in (select inode from identifier)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        
        try {
            dc.executeStatement("delete from file_asset where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        
        try {
            dc.executeStatement("delete from contentlet where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        
        try {
            dc.executeStatement("delete from containers where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        
        try {
            dc.executeStatement("delete from htmlpage where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        try {
            dc.executeStatement("delete from links where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
        try {
            dc.executeStatement("delete from template where inode not in (select inode from inode)");
        } catch (SQLException e) {
            Logger.warn(this, e.getMessage());
        }
    }
    
}
