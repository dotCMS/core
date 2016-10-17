package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01030AddSiteSearchAuditTable extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        boolean force=false;
        try {
            DotConnect dc=new DotConnect();
            dc.setSQL("select count(*) from sitesearch_audit");
            dc.loadResult();
        }
        catch(Exception ex) {
            force=true;
        }
        return force;
    }
    
    @Override
    public String getPostgresScript() {
        return  "create table sitesearch_audit (\n"+
                "    job_id varchar(36),\n"+
                "    job_name varchar(255) not null,\n"+
                "    fire_date timestamp not null,\n"+
                "    incremental bool not null,\n"+
                "    start_date timestamp,\n"+
                "    end_date timestamp,\n"+
                "    host_list varchar(500) not null,\n"+
                "    all_hosts bool not null,\n"+
                "    lang_list varchar(500) not null,\n"+
                "    path varchar(500) not null,\n"+
                "    path_include bool not null,\n"+
                "    files_count integer not null,\n"+
                "    pages_count integer not null,\n"+
                "    urlmaps_count integer not null,\n"+
                "    index_name varchar(100) not null,\n"+
                "    primary key(job_id,fire_date)\n"+
                ");\n";

    }
    
    @Override
    public String getMySQLScript() {
        return  "create table sitesearch_audit (\n"+
                "    job_id varchar(36),\n"+
                "    job_name varchar(255) not null,\n"+
                "    fire_date datetime not null,\n"+
                "    incremental tinyint not null,\n"+
                "    start_date datetime,\n"+
                "    end_date datetime,\n"+
                "    host_list varchar(500) not null,\n"+
                "    all_hosts tinyint not null,\n"+
                "    lang_list varchar(500) not null,\n"+
                "    path varchar(500) not null,\n"+
                "    path_include tinyint not null,\n"+
                "    files_count integer not null,\n"+
                "    pages_count integer not null,\n"+
                "    urlmaps_count integer not null,\n"+
                "    index_name varchar(100) not null,\n"+
                "    primary key(job_id,fire_date)\n"+
                ");\n";
    }
    
    @Override
    public String getOracleScript() {
        return  "create table sitesearch_audit (\n"+
                "    job_id varchar2(36),\n"+
                "    job_name varchar2(255) not null,\n"+
                "    fire_date timestamp not null,\n"+
                "    incremental number(1,0) not null,\n"+
                "    start_date timestamp,\n"+
                "    end_date timestamp,\n"+
                "    host_list varchar2(500) not null,\n"+
                "    all_hosts number(1,0) not null,\n"+
                "    lang_list varchar2(500) not null,\n"+
                "    path varchar2(500) not null,\n"+
                "    path_include number(1,0) not null,\n"+
                "    files_count number(10,0) not null,\n"+
                "    pages_count number(10,0) not null,\n"+
                "    urlmaps_count number(10,0) not null,\n"+
                "    index_name varchar2(100) not null,\n"+
                "    primary key(job_id,fire_date)\n"+
                ");\n";

    }
    
    @Override
    public String getMSSQLScript() {
        return  "create table sitesearch_audit (\n"+
                "    job_id varchar(36),\n"+
                "    job_name varchar(255) not null,\n"+
                "    fire_date DATETIME not null,\n"+
                "    incremental tinyint not null,\n"+
                "    start_date DATETIME,\n"+
                "    end_date DATETIME,\n"+
                "    host_list varchar(500) not null,\n"+
                "    all_hosts tinyint not null,\n"+
                "    lang_list varchar(500) not null,\n"+
                "    path varchar(500) not null,\n"+
                "    path_include tinyint not null,\n"+
                "    files_count numeric(19,0) not null,\n"+
                "    pages_count numeric(19,0) not null,\n"+
                "    urlmaps_count numeric(19,0) not null,\n"+
                "    index_name varchar(100) not null,\n"+
                "    primary key(job_id,fire_date)\n"+
                ");\n";
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
