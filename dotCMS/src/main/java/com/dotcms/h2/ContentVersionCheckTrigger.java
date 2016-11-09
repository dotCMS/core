package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class ContentVersionCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldContent, ResultSet newContent) throws SQLException {
        PreparedStatement smt=conn.prepareStatement("select count(*) from contentlet where identifier = ?");
        smt.setString(1, oldContent.getString("identifier"));
        ResultSet rs=smt.executeQuery();
        rs.next();
        int versions=rs.getInt(1);
        rs.close(); smt.close();
        
        if(versions==0) {
            smt=conn.prepareStatement("delete from identifier where id =?");
            smt.setString(1, oldContent.getString("identifier"));
            smt.executeUpdate();
            smt.close();
        }
    }

}
