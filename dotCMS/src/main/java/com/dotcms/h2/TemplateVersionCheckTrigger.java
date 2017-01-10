package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class TemplateVersionCheckTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldT, ResultSet newT) throws SQLException {
        PreparedStatement smt=conn.prepareStatement("select count(*) from template where identifier = ?");
        smt.setString(1, oldT.getString("identifier"));
        ResultSet rs=smt.executeQuery();
        rs.next();
        int versions=rs.getInt(1);
        rs.close(); smt.close();
        
        if(versions==0) {
            smt=conn.prepareStatement("delete from identifier where id =?");
            smt.setString(1, oldT.getString("identifier"));
            smt.executeUpdate();
            smt.close();
        }
    }

}
