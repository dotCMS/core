package com.dotmarketing.sitesearch.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;

public class SiteSearchAuditAPIImpl implements SiteSearchAuditAPI {

    SiteSearchAuditFactory ssFac=new SiteSearchAuditFactoryImpl();
    
    @Override
    public void save(SiteSearchAudit audit) throws DotDataException {
        ssFac.save(audit);
    }

    @Override
    public List<SiteSearchAudit> findRecentAudits(String jobId, int offset,  int limit) throws DotDataException {
        return ssFac.findRecentAudits(jobId, offset, limit);
    }

    @Override
    public void removeAudits(String jobId) throws DotDataException {
        ssFac.removeAudits(jobId);
    }
    
}
