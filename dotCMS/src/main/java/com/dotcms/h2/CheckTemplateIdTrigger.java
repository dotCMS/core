package com.dotcms.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.TriggerAdapter;

public class CheckTemplateIdTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldTemplate, ResultSet newTemplate) throws SQLException {
        PreparedStatement smt=conn.prepareStatement("select id from identifier where asset_type='template' and id=?");
        smt.setString(1, newTemplate.getString("template_id"));
        ResultSet rs=smt.executeQuery();
        boolean found=rs.next();
        rs.close(); smt.close();
        
        if(!found) {
            throw new SQLException("Template Id should be the identifier of a template");
        }
    }

}
