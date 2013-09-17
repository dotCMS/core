package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01070BundleNameDropUnique extends AbstractJDBCStartupTask {
    

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "delete from publishing_bundle_environment;\n"+
               "alter table publishing_bundle_environment drop constraint FK_bundle_id;\n"+
               "drop table publishing_bundle;\n"+
               "create table publishing_bundle( \n"+
               "  id varchar(36) NOT NULL  primary key,\n"+
               "  name varchar(255) NOT NULL,\n"+
               "  publish_date TIMESTAMP,\n"+
               "  expire_date TIMESTAMP,\n"+
               "  owner varchar(100));\n"+
               "create index idx_pub_bundle_name on publishing_bundle (name);\n"+
               "alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);\n"+
               "alter table publishing_bundle add force_push bool;\n";
    }

    @Override
    public String getMySQLScript() {
        return "drop table publishing_bundle_environment;"+
               "drop table publishing_bundle; \n"+
               "create table publishing_bundle( \n"+
               "  id varchar(36) NOT NULL  primary key,\n"+
               "  name varchar(255) NOT NULL,\n"+
               "  publish_date DATETIME, \n"+
               "  expire_date DATETIME, \n"+
               "  owner varchar(100));\n"+
               "create index idx_pub_bundle_name on publishing_bundle (name);\n"+               
               "alter table publishing_bundle add force_push varchar(1);\n"+
               "create table publishing_bundle_environment(id varchar(36) NOT NULL primary key,bundle_id varchar(36) NOT NULL, environment_id varchar(36) NOT NULL);\n"+
               "alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);\n"+
               "alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id);\n";
    }

    @Override
    public String getOracleScript() {
        return "delete from publishing_bundle_environment;\n"+
               "alter table publishing_bundle_environment drop constraint FK_bundle_id;\n"+
               "drop table publishing_bundle; \n"+
               "create table publishing_bundle(\n"+
               "  id varchar2(36) NOT NULL primary key,\n"+
               "  name varchar2(255) NOT NULL ,\n"+
               "  publish_date TIMESTAMP,\n"+
               "  expire_date TIMESTAMP,\n"+
               "  owner varchar2(100));\n"+
               "create index idx_pub_bundle_name on publishing_bundle (name);\n"+
               "alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);\n"+
               "alter table publishing_bundle add force_push number(1,0);\n";
    }

    @Override
    public String getMSSQLScript() {
        return  "delete from publishing_bundle_environment;\n"+
                "alter table publishing_bundle_environment drop constraint FK_bundle_id;\n"+
                "drop table publishing_bundle; \n"+
                "create table publishing_bundle(\n"+
                "  id varchar(36) NOT NULL  primary key,\n"+
                "  name varchar(255) NOT NULL,\n"+
                "  publish_date DATETIME,\n"+
                "  expire_date DATETIME,\n"+
                "  owner varchar(100));\n"+
                "create index idx_pub_bundle_name on publishing_bundle (name);\n"+
                "alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id);\n"+
                "alter table publishing_bundle add force_push tinyint ;\n";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        List<String> tables=new ArrayList<String>();
        //tables.add("publishing_bundle_environment");
        return tables;
    }

}
