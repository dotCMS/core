package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
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
        } catch (SQLException e) {
            Logger.warn(this.getClass(), e.getMessage(),e);
        }
        
        if(Config.getBooleanProperty("upgrade-cleanup-bad-data",true)) {
            final String[] smts=new String[] {
            "delete from inode where type='folder' and not exists (select * from folder where folder.inode=inode.inode)",
            "delete from inode where type='contentlet' and not exists (select * from contentlet where contentlet.inode=inode.inode)",
            "delete from inode where type='file_asset' and not exists (select * from file_asset where file_asset.inode=inode.inode)",
            "delete from inode where type='containers' and not exists (select * from containers where containers.inode=inode.inode)",
            "delete from inode where type='template' and not exists (select * from template where template.inode=inode.inode)",
            "delete from inode where type='htmlpage' and not exists (select * from htmlpage where htmlpage.inode=inode.inode)",
            "delete from inode where type='containers' and not exists (select * from containers where containers.inode=inode.inode)",
            "delete from inode where type='links' and not exists (select * from links where links.inode=inode.inode)",
            "delete from identifier where not exists (select * from inode where inode.identifier=identifier.inode)"
            };
            
            for(String smt : smts) {
                try {
                    dc.executeStatement(smt);
                } catch (SQLException e) {
                    Logger.warn(this.getClass(), e.getMessage());
                }
            }
        }
        
        try {
            dc.executeStatement("delete from dist_reindex_journal");
        } catch(SQLException e) {
            Logger.warn(this, "can't clean dist_reindex_journal");
        }
    }
    
}
