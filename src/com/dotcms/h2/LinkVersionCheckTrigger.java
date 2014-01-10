package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class LinkVersionCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldLink, ResultSet newLink) throws SQLException {
        PreparedStatement smt=conn.prepareStatement("select count(*) from links where identifier = ?");
        smt.setString(1, oldLink.getString("identifier"));
        ResultSet rs=smt.executeQuery();
        rs.next();
        int versions=rs.getInt(1);
        rs.close(); smt.close();
        
        if(versions==0) {
            smt=conn.prepareStatement("delete from identifier where id =?");
            smt.setString(1, oldLink.getString("identifier"));
            smt.executeUpdate();
            smt.close();
        }
    }

}
