package com.dotmarketing.sitesearch.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;

interface SiteSearchAuditFactory {
    
    void save(SiteSearchAudit audit) throws DotDataException;
    
    List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit) throws DotDataException;
    
    void removeAudits(String jobId) throws DotDataException;
}
