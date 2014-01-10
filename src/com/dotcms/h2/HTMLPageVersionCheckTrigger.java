package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class HTMLPageVersionCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldPage, ResultSet newPage) throws SQLException {
        PreparedStatement smt=conn.prepareStatement("select count(*) from htmlpage where identifier = ?");
        smt.setString(1, oldPage.getString("identifier"));
        ResultSet rs=smt.executeQuery();
        rs.next();
        int versions=rs.getInt(1);
        rs.close(); smt.close();
        
        if(versions==0) {
            smt=conn.prepareStatement("delete from identifier where id =?");
            smt.setString(1, oldPage.getString("identifier"));
            smt.executeUpdate();
            smt.close();
        }
    }

}
