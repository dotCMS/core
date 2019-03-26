package com.dotmarketing.sitesearch.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import java.util.List;

interface SiteSearchAuditFactory {

  void save(SiteSearchAudit audit) throws DotDataException;

  List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit)
      throws DotDataException;

  void removeAudits(String jobId) throws DotDataException;
}
