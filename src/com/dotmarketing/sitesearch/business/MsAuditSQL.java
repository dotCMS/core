package com.dotmarketing.sitesearch.business;

import com.dotmarketing.common.db.DotConnect;

class MsAuditSQL extends SiteSearchAuditSQL {
    public MsAuditSQL() {
        findrecent="select top ? * from ( "+
                   " select *, ROW_NUMBER() over (order by fire_date desc) as rnn " +
                   " from sitesearch_audit where job_id=? "+ 
                   ") xx where rnn >=?";
    }
    
    @Override
    void setRecentParams(DotConnect dc, String jobId, int limit, int offset) {
        dc.addParam(limit);
        dc.addParam(jobId);
        dc.addParam(offset+1);
    }
}
