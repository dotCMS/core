package com.dotcms.h2;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.tools.TriggerAdapter;

public class IdentifierParentPathCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldIdent, ResultSet newIdent) throws SQLException {
        String newParentPath=newIdent.getString("parent_path");
        if(!newParentPath.equals("/") && !newParentPath.equals(SYSTEM_FOLDER_PARENT_PATH)) {
            PreparedStatement smt=conn.prepareStatement("select id from identifier where asset_type='folder' and host_inode = ? and "
                    + " parent_path||asset_name||'/' = ? and id <> ?");
            smt.setString(1, newIdent.getString("host_inode"));
            smt.setString(2, newParentPath);
            smt.setString(3, newIdent.getString("id"));
            ResultSet rs=smt.executeQuery();
            boolean found=rs.next();
            rs.close(); smt.close();
            if(!found) {
                throw new SQLException("Cannot insert/update for this path does not exist for the given host");
            }
        }
    }

}
