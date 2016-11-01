package com.dotmarketing.sitesearch.business;

import com.dotmarketing.common.db.DotConnect;

class OracleAuditSQL extends SiteSearchAuditSQL {
    public OracleAuditSQL() {
        findrecent="SELECT * FROM ("+
                   " SELECT rownum rnum, a.* "+ 
                   " FROM ( "+
                   "  SELECT * "+ 
                   "  FROM sitesearch_audit " +
                   "  WHERE job_id=? "+ 
                   "  ORDER BY fire_date desc "+ 
                   " ) a "+
                   " WHERE rownum <=? "+
                   ") "+
                   "WHERE rnum >=?";
    }
    
    @Override
    void setRecentParams(DotConnect dc, String jobId, int limit, int offset) {
        dc.addParam(jobId);
        dc.addParam(offset+limit);
        dc.addParam(offset);
    }
}
