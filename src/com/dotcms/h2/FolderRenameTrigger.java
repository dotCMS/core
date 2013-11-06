package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class FolderRenameTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldF, ResultSet newF) throws SQLException {
        
        if(!oldF.getString("name").equals(newF.getString("name"))) {
            
            PreparedStatement smt=conn.prepareStatement("select asset_name,parent_path,host_inode from identifier where id=?");
            smt.setString(1, newF.getString("identifier"));
            ResultSet rs = smt.executeQuery();
            rs.next();
            
            String oldPath = rs.getString("parent_path") + rs.getString("asset_name") + "/";
            String newpath = rs.getString("parent_path") + newF.getString("name") + "/";
            String hostId = rs.getString("host_inode");
            
            rs.close();
            smt.close();
            
            PreparedStatement update = conn.prepareStatement("update identifier set asset_name=? where id=?");
            update.setString(1, newF.getString("name"));
            update.setString(2, newF.getString("identifier"));
            update.executeUpdate();
            
            update.close();
            
            renameChildren(conn,oldPath,newpath,hostId);
        }

    }
    
    protected void renameChildren(Connection conn, String oldPath, String newPath, String hostId) throws SQLException {
        PreparedStatement update=conn.prepareStatement("UPDATE identifier SET  parent_path  = ? "
                + "where parent_path = ? and host_inode = ?");
        update.setString(1, newPath);
        update.setString(2, oldPath);
        update.setString(3, hostId);
        update.executeUpdate();
        update.close();
        
        PreparedStatement childs=conn.prepareStatement("select asset_name from identifier "
                +  " where asset_type='folder' and parent_path = ? and host_inode = ?");
        childs.setString(1, newPath);
        childs.setString(2, hostId);
        ResultSet rs=childs.executeQuery();
        while(rs.next()) {
            String newPath2 = newPath + rs.getString("asset_name") + "/";
            String oldPath2 = oldPath + rs.getString("asset_name") + "/";
            renameChildren(conn,oldPath2,newPath2,hostId);
        }
        rs.close();
        childs.close();
    }

}
