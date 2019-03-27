package com.dotmarketing.sitesearch.business;

import com.dotmarketing.common.db.DotConnect;

class MsAuditSQL extends SiteSearchAuditSQL {
    public MsAuditSQL() {
        // see the "top (?)"?
        // keep an eye here http://www.mirthcorp.com/community/forums/showthread.php?t=2190
        findrecent="select top (?) * from ( "+
                   " select satt.*, ROW_NUMBER() over (order by fire_date desc) as r_n_n " +
                   " from sitesearch_audit satt where job_id=? "+ 
                   " ) subqueryname where r_n_n >=?";
    }
    
    @Override
    void setRecentParams(DotConnect dc, String jobId, int limit, int offset) {
        dc.addParam(limit);
        dc.addParam(jobId);
        dc.addParam(offset+1);
    }
}
