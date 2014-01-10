package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;

public class H2Procedure {
    public static ResultSet loadRecordsToIndex(Connection conn, String serverId, int records) throws SQLException {
        
        SimpleResultSet ret=new SimpleResultSet();
        ret.addColumn("id", Types.INTEGER, 10, 0);
        ret.addColumn("inode_to_index", Types.VARCHAR, 36, 0);
        ret.addColumn("ident_to_index", Types.VARCHAR, 36, 0);
        ret.addColumn("priority", Types.INTEGER, 10, 0);
        
        PreparedStatement update=conn.prepareStatement("UPDATE dist_reindex_journal SET serverid=? WHERE id=?");
        
        PreparedStatement smt=conn.prepareStatement("SELECT * FROM dist_reindex_journal "
                + "WHERE serverid IS NULL ORDER BY priority ASC LIMIT ? FOR UPDATE");
        smt.setInt(1, records);
        ResultSet rs=smt.executeQuery();
        
        while(rs.next()) {
            int id = rs.getInt("id");
            ret.addRow(id, rs.getString("inode_to_index"), rs.getString("ident_to_index"), rs.getInt("priority"));
            
            update.setString(1, serverId);
            update.setInt(2, id);
            update.executeUpdate();
        }
        
        rs.close();
        smt.close();
        update.close();
        
        return ret;
    }
    
    
    public static String dotFolderPath(String parentPath, String assetName) throws SQLException {
        if(parentPath.equals("/System folder")) {
            return "/";
        }
        else {
            return parentPath + assetName + "/";
        }
    }
}
