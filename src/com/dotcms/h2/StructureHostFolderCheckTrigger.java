package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

import com.dotmarketing.util.UtilMethods;

public class StructureHostFolderCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldSt, ResultSet newSt) throws SQLException {
        String newHost=newSt.getString("host");
        String newFolder=newSt.getString("folder");
        if(UtilMethods.isSet(newHost) && !newHost.equals("SYSTEM_HOST") && UtilMethods.isSet(newFolder) && !newFolder.equals("SYSTEM_FOLDER")) {
            PreparedStatement smt=conn.prepareStatement("select host_inode from folder join identifier "
                    + " on folder.identifier = identifier.id where folder.inode=?");
            smt.setString(1, newSt.getString("folder"));
            ResultSet rs=smt.executeQuery();
            boolean found=rs.next();
            boolean samehost=found && rs.getString("host_inode").equals(newSt.getString("host"));
            rs.close(); smt.close();
            if(!samehost) {
                throw new SQLException("Cannot assign host/folder to structure, folder does not belong to given host");
            }
        }
    }
}
