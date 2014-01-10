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
        
        if(((!UtilMethods.isSet(newHost) || newHost.equals("SYSTEM_HOST")) && !newFolder.equals("SYSTEM_FOLDER"))
                || ((!UtilMethods.isSet(newFolder) || newFolder.equals("SYSTEM_FOLDER")) && !newHost.equals("SYSTEM_HOST"))) {
            
            PreparedStatement smt=conn.prepareStatement("update structure set host='SYSTEM_HOST',folder='SYSTEM_FOLDER' where inode=?");
            smt.setString(1, newSt.getString("inode"));
            smt.executeUpdate();
            smt.close();
            
        }
        
    }
}
