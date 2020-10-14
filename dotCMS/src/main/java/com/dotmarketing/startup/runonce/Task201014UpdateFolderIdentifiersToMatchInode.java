package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * 
 * This updates the folder inodes to match the folder identifiers
 *
 */
public class Task201014UpdateFolderIdentifiersToMatchInode implements StartupTask {


    
    public boolean forceRun() {
        return true;
    }
    final boolean mysql = DbConnectionFactory.isMySql();
    

    String dropConstraint()  {
        return mysql ?  "alter table folder drop foreign key folder_identifier_fk " 
                     :  "ALTER TABLE folder drop constraint folder_identifier_fk ";
    }

    String addConstraint() {
     
        return mysql ?  "ALTER TABLE folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id)" 
                     :  "ALTER TABLE folder add constraint folder_identifier_fk foreign key (identifier) references identifier(id)";

    }
    
    String updateIdentifiers() {
        return "UPDATE identifier SET id = folder.inode FROM folder where folder.identifier = identifier.id " ;
    }
    
    String updateFolderIdentifiers()  {
        return "update folder set identifier = inode " ;
    }
    
    

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        DotConnect db = new DotConnect();
        
        db.setSQL(dropConstraint()).loadResult();
        db.setSQL(updateIdentifiers()).loadResult();
        db.setSQL(updateFolderIdentifiers()).loadResult();
        db.setSQL(addConstraint()).loadResult();
        
        
    }

}
