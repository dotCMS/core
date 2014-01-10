package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class CheckChildAssetTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldIdent, ResultSet newIdent) throws SQLException {
        int childs=0;
        
        if(oldIdent.getString("asset_type").equals("folder")) {
            PreparedStatement smt=conn.prepareStatement("select count(*) from identifier where parent_path=? and host_inode=?");
            smt.setString(1, oldIdent.getString("parent_path")+oldIdent.getString("asset_name")+"/");
            smt.setString(2, oldIdent.getString("host_inode"));
            ResultSet rs=smt.executeQuery();
            rs.next();
            childs += rs.getInt(1);
            
            rs.close(); smt.close();
        }
        else if(oldIdent.getString("asset_type").equals("contentlet")) {
            PreparedStatement smt=conn.prepareStatement("select count(*) from identifier where host_inode=?");
            smt.setString(1, oldIdent.getString("id"));
            ResultSet rs=smt.executeQuery();
            rs.next();
            childs += rs.getInt(1);
            
            rs.close(); smt.close();
        }
        
        if(childs>0) {
            throw new SQLException("Cannot delete as this path has children");
        }
    }

}
