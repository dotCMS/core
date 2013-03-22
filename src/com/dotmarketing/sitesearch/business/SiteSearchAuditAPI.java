package com.dotmarketing.sitesearch.business;

import java.util.List;

import com.dotmarketing.sitesearch.model.SiteSearchAudit;

public interface SiteSearchAuditAPI {
    /**
     * Saves to db a new audit entry
     * @param audit instance with data to save
     */
    void save(SiteSearchAudit audit);
    
    /**
     * Retrieves a list of most recent audits for the specified jobId.
     * It orders by "fireDate desc"
     * @param jobId job identifier
     * @param row skip count
     * @param maximum audit count to return
     * @return list of SiteSearchAudit
     */
    List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit);
    
    /**
     * Removes all audits entries for the specified job
     * @param jobId
     */
    void removeAudits(String jobId);
}
