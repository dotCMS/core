package com.dotmarketing.sitesearch.business;

import java.util.List;

import com.dotmarketing.sitesearch.model.SiteSearchAudit;

public class SiteSearchAuditAPIImpl implements SiteSearchAuditAPI {
    
    @Override
    public void save(SiteSearchAudit audit) {
        
    }
    
    @Override
    public List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit) {
        return null;
    }
    
    @Override
    public void removeAudits(String jobId) {
        
    }
    
}
