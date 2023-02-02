package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import java.sql.Connection;
import java.sql.SQLException;

public class Task210719CleanUpTitleField implements StartupTask {
    @Override
    public boolean forceRun() {
        return true;
    }
    
    
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        
        if(DbConnectionFactory.isPostgres()) {
            executePgUpgrade();
        }
        
        if(DbConnectionFactory.isMsSql()) {
            executeSQLServerUpgrade();
        }
        
        
        
        
    }
    

    public void executeSQLServerUpgrade() throws DotDataException, DotRuntimeException {

        try {
             new DotConnect()
                    .executeStatement("update contentlet set title=null");
        } catch (SQLException exception) {
            throw new DotRuntimeException(exception);
        }
    }
    
    
    
    

    public void executePgUpgrade() throws DotDataException, DotRuntimeException {

        
        
        
        int rowsAffecrted=0;
        
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()){
            
            int remaining = (int) new DotConnect().setSQL("select inode from contentlet where title is not null limit 1").loadObjectResults(conn).size();
            while(remaining>0) {
                
                new DotConnect()
                .executeStatement("update contentlet set title=null where contentlet.inode in (select inode from contentlet where title is not null limit 500)", conn);
                rowsAffecrted+=500;
                Logger.info(getClass(), "Task210719CleanUpTitleField Updated: "+ rowsAffecrted );
                remaining = (int) new DotConnect().setSQL("select inode from contentlet where title is not null limit 1").loadObjectResults(conn).size();
            }
            
            

        } catch (SQLException exception) {
            throw new DotRuntimeException(exception);
        }
    }
    
    
    
    
}
