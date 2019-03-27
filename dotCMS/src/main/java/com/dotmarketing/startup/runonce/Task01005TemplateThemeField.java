package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.util.Logger;

public class Task01005TemplateThemeField extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        Connection conn=null;
        Statement smt=null;
        ResultSet rs=null;
        try {
            conn=DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            smt=conn.createStatement();
            rs=smt.executeQuery("select theme from template");
            rs.next();
            return false;
        }
        catch(Exception ex) {
            return true;
        }
        finally {
            if(conn!=null) {
                try {
                    if(rs!=null) rs.close();
                    if(smt!=null) smt.close();
                    conn.close();
                } catch (SQLException e) {
                    Logger.error(this, e.getMessage(),e);
                }
            }
        }
    }
    
    @Override
    public String getPostgresScript() {
        return "alter table template add theme varchar(255);";
    }
    
    @Override
    public String getMySQLScript() {
        return "alter table template add theme varchar(255);";
    }
    
    @Override
    public String getOracleScript() {
        return "alter table template add theme varchar2(255);";
    }
    
    @Override
    public String getMSSQLScript() {
        return "alter table template add theme varchar(255);";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

    @Override
    public String getH2Script() {
        return null;
    }
    
}
