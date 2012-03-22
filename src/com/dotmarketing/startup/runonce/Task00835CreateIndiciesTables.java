package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00835CreateIndiciesTables extends AbstractJDBCStartupTask {
    
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return  "create table indicies (\n"+
                "  index_name varchar(30) primary key,\n"+
                "  index_type varchar(16) not null unique\n"+
                ");\n";
    }
    
    @Override
    public String getMySQLScript() {
        return  "create table indicies (\n"+
                "  index_name varchar(30) primary key,\n"+
                "  index_type varchar(16) not null unique\n"+
                ");\n";
    }
    
    @Override
    public String getOracleScript() {
        return  "create table indicies (\n"+
                "  index_name varchar2(30) primary key,\n"+
                "  index_type varchar2(16) not null unique\n"+
                ");\n";
    }
    
    @Override
    public String getMSSQLScript() {
        return  "create table indicies (\n"+
                "  index_name varchar(30) primary key,\n"+
                "  index_type varchar(16) not null unique\n"+
                ");\n";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
