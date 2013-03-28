package com.dotmarketing.sitesearch.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;

abstract class SiteSearchAuditSQL {
    
    static SiteSearchAuditSQL getInstance() {
        if(DbConnectionFactory.isPostgres())
            return new PgAuditSQL();
        else if(DbConnectionFactory.isMySql())
            return new MyAuditSQL();
        else if(DbConnectionFactory.isMsSql())
            return new MsAuditSQL();
        else
            return new OracleAuditSQL();
    }
    
    String insert="insert into sitesearch_audit " +
            " (job_id,job_name,fire_date,incremental,start_date,end_date,host_list,all_hosts," +
            "  lang_list,path,path_include,files_count,pages_count,urlmaps_count,index_name) " +
            " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    String findrecent="select * from sitesearch_audit where job_id=? order by fire_date desc limit ? offset ?";
    void setRecentParams(DotConnect dc, String jobId, int limit, int offset) {
        dc.addParam(jobId);
        dc.addParam(limit);
        dc.addParam(offset);
    }
    
    String remove="delete from sitesearch_audit where job_id=?";
}
