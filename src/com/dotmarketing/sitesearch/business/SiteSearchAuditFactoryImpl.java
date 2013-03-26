package com.dotmarketing.sitesearch.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;

class SiteSearchAuditFactoryImpl implements SiteSearchAuditFactory {
    
    SiteSearchAuditSQL auditSQL;
    
    public SiteSearchAuditFactoryImpl() {
        auditSQL=SiteSearchAuditSQL.getInstance();
    }
    
    @Override
    public void save(SiteSearchAudit audit) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL(auditSQL.insert);
        dc.addParam(audit.getJobId());
        dc.addParam(audit.getJobName());
        dc.addParam(audit.getFireDate());
        dc.addParam(audit.isIncremental());
        dc.addParam(audit.getStartDate());
        dc.addParam(audit.getEndDate());
        dc.addParam(audit.getHostList());
        dc.addParam(audit.isAllHosts());
        dc.addParam(audit.getLangList());
        dc.addParam(audit.getPath());
        dc.addParam(audit.isPathInclude());
        dc.addParam(audit.getFilesCount());
        dc.addParam(audit.getPagesCount());
        dc.addParam(audit.getUrlmapsCount());
        dc.addParam(audit.getIndexName());
        dc.loadResult();
    }
    
    @Override
    public List<SiteSearchAudit> findRecentAudits(String jobId, int offset, int limit) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL(auditSQL.findrecent);
        auditSQL.setRecentParams(dc, jobId, limit, offset);
        List<Map<String,Object>> results = dc.loadObjectResults();
        List<SiteSearchAudit> recents=new ArrayList<SiteSearchAudit>();
        for (Map<String, Object> map : results) {
            SiteSearchAudit audit=new SiteSearchAudit();
            audit.setAllHosts(DbConnectionFactory.isDBTrue(map.get("all_hosts").toString()));
            audit.setIncremental(DbConnectionFactory.isDBTrue(map.get("incremental").toString()));
            audit.setEndDate((Date)map.get("end_date"));
            audit.setStartDate((Date)map.get("start_date"));
            audit.setFireDate((Date)map.get("fire_date"));
            audit.setHostList((String)map.get("host_list"));
            audit.setIndexName((String)map.get("index_name"));
            audit.setJobId(jobId);
            audit.setJobName((String)map.get("job_name"));
            audit.setLangList((String)map.get("lang_list"));
            audit.setFilesCount(((Number)map.get("files_count")).intValue());
            audit.setPagesCount(((Number)map.get("pages_count")).intValue());
            audit.setUrlmapsCount(((Number)map.get("urlmaps_count")).intValue());
            audit.setPath((String)map.get("path"));
            audit.setPathInclude(DbConnectionFactory.isDBTrue(map.get("path_include").toString()));
            recents.add(audit);
        }
        return recents;
    }
    
    @Override
    public void removeAudits(String jobId) throws DotDataException {
        DotConnect dc=new DotConnect();
        dc.setSQL(auditSQL.remove);
        dc.addParam(jobId);
        dc.loadResult();
    }
    
}
