package com.dotcms.rest.api.v1.job;

public class JobStatusResponse {
    private String jobId;
    private String statusUrl;


    public JobStatusResponse(String jobId, String statusUrl) {
        this.jobId = jobId;
        this.statusUrl = statusUrl;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    @Override
    public String toString() {
        return "JobStatusResponse{" +
                "jobId='" + jobId + '\'' +
                ", statusUrl='" + statusUrl + '\'' +
                '}';
    }
}
