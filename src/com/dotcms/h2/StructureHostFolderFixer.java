package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

import com.dotmarketing.util.UtilMethods;

public class StructureHostFolderFixer extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldSt, ResultSet newSt) throws SQLException {
        String newHost=newSt.getString("host");
        String newFolder=newSt.getString("folder");
        boolean updateToDefault = false;
        boolean updateToSystemFolder = false;
        
        if(!UtilMethods.isSet(newHost) || ( newHost.equals("SYSTEM_HOST")  && !newFolder.equals("SYSTEM_FOLDER"))){
        	updateToDefault = true;
        }else if(!UtilMethods.isSet(newFolder)){
        	updateToSystemFolder = true;
        }

        if(updateToDefault){
            PreparedStatement smt=conn.prepareStatement("update structure set host='SYSTEM_HOST',folder='SYSTEM_FOLDER' where inode=?");
            smt.setString(1, newSt.getString("inode"));
            smt.executeUpdate();
            smt.close();
        }else if(updateToSystemFolder){
        	PreparedStatement smt=conn.prepareStatement("update structure set folder='SYSTEM_FOLDER' where inode=?");
            smt.setString(1, newSt.getString("inode"));
            smt.executeUpdate();
            smt.close();
        }
        
    }
}
