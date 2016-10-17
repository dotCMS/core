package com.dotmarketing.sitesearch.model;

import java.util.Date;

public class SiteSearchAudit {
    private String jobId;
    private String jobName;
    
    private Date fireDate;
    
    private boolean incremental;
    private Date startDate;
    private Date endDate;
    
    private String hostList;
    private boolean allHosts;
    private String langList;
    private String path;
    private boolean pathInclude;
    
    private int filesCount;
    private int pagesCount;
    private int urlmapsCount;
    
    private String indexName;
    
    public String getIndexName() {
        return indexName;
    }
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    public String getJobId() {
        return jobId;
    }
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    public String getJobName() {
        return jobName;
    }
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    public Date getFireDate() {
        return fireDate;
    }
    public void setFireDate(Date fireDate) {
        this.fireDate = fireDate;
    }
    public boolean isIncremental() {
        return incremental;
    }
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }
    public Date getStartDate() {
        return startDate;
    }
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    public Date getEndDate() {
        return endDate;
    }
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    public String getHostList() {
        return hostList;
    }
    public void setHostList(String hostList) {
        this.hostList = hostList;
    }
    public boolean isAllHosts() {
        return allHosts;
    }
    public void setAllHosts(boolean allHosts) {
        this.allHosts = allHosts;
    }
    public String getLangList() {
        return langList;
    }
    public void setLangList(String langList) {
        this.langList = langList;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public boolean isPathInclude() {
        return pathInclude;
    }
    public void setPathInclude(boolean pathInclude) {
        this.pathInclude = pathInclude;
    }
    public int getFilesCount() {
        return filesCount;
    }
    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }
    public int getPagesCount() {
        return pagesCount;
    }
    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }
    public int getUrlmapsCount() {
        return urlmapsCount;
    }
    public void setUrlmapsCount(int urlmapsCount) {
        this.urlmapsCount = urlmapsCount;
    }
}
