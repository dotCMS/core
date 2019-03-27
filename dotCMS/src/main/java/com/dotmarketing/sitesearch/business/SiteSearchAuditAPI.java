package com.dotmarketing.sitesearch.business;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;

public interface SiteSearchAuditAPI {
    /**
     * Saves to db a new audit entry
     * @param audit instance with data to save
     * @throws DotDataException 
     */
    void save(SiteSearchAudit audit) throws DotDataException;
    
    /**
     * Retrieves a list of most recent audits for the specified jobId.
     * It orders by "fireDate desc"
     * @param jobId job identifier
     * @param row skip count
     * @param maximum audit count to return
     * @return list of SiteSearchAudit
     * @throws DotDataException 
     */
    List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit) throws DotDataException;
    
    /**
     * Removes all audits entries for the specified job
     * @param jobId
     * @throws DotDataException 
     */
    void removeAudits(String jobId) throws DotDataException;
}
