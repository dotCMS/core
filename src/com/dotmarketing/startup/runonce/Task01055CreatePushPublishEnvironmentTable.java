package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01055CreatePushPublishEnvironmentTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all bool NOT NULL);";
    }

    @Override
    public String getMySQLScript() {
        return "create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all bool NOT NULL);";
    }

    @Override
    public String getOracleScript() {
        return "create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all tinyint NOT NULL);";
    }

    @Override
    public String getMSSQLScript() {
        return "create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all tinyint NOT NULL);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
