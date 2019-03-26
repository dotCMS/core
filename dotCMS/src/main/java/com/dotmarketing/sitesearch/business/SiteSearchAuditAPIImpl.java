package com.dotmarketing.sitesearch.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import java.util.List;

public class SiteSearchAuditAPIImpl implements SiteSearchAuditAPI {

  final SiteSearchAuditFactory siteSearchAuditFactory = new SiteSearchAuditFactoryImpl();

  @WrapInTransaction
  @Override
  public void save(SiteSearchAudit audit) throws DotDataException {
    siteSearchAuditFactory.save(audit);
  }

  @CloseDBIfOpened
  @Override
  public List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit)
      throws DotDataException {
    return siteSearchAuditFactory.findRecentAudits(jobId, offset, limit);
  }

  @WrapInTransaction
  @Override
  public void removeAudits(String jobId) throws DotDataException {
    siteSearchAuditFactory.removeAudits(jobId);
  }
}
