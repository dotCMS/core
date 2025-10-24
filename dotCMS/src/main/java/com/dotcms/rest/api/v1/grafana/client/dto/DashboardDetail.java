package com.dotcms.rest.api.v1.grafana.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.Map;

/**
 * DTO representing detailed dashboard information from Grafana API.
 *
 * This class encapsulates the full dashboard response including the dashboard
 * definition and metadata information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardDetail {

    private Map<String, Object> dashboard;
    private DashboardMeta meta;

    public DashboardDetail() {}

    public DashboardDetail(Map<String, Object> dashboard, DashboardMeta meta) {
        this.dashboard = dashboard;
        this.meta = meta;
    }

    public Map<String, Object> getDashboard() {
        return dashboard;
    }

    public void setDashboard(Map<String, Object> dashboard) {
        this.dashboard = dashboard;
    }

    public DashboardMeta getMeta() {
        return meta;
    }

    public void setMeta(DashboardMeta meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "DashboardDetail{" +
                "dashboard=" + (dashboard != null ? dashboard.get("title") : "null") +
                ", meta=" + meta +
                '}';
    }

    /**
     * DTO representing dashboard metadata.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DashboardMeta {

        private String type;
        private Boolean canSave;
        private Boolean canEdit;
        private Boolean canAdmin;
        private Boolean canStar;
        private String slug;
        private String url;
        private Long expires;
        private Date created;
        private Date updated;
        private String updatedBy;
        private String createdBy;
        private Integer version;
        private Boolean hasAcl;
        private Boolean isFolder;
        private String folderId;
        private String folderUid;
        private String folderTitle;
        private String folderUrl;
        private Boolean provisioned;

        public DashboardMeta() {}

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getCanSave() {
            return canSave;
        }

        public void setCanSave(Boolean canSave) {
            this.canSave = canSave;
        }

        public Boolean getCanEdit() {
            return canEdit;
        }

        public void setCanEdit(Boolean canEdit) {
            this.canEdit = canEdit;
        }

        public Boolean getCanAdmin() {
            return canAdmin;
        }

        public void setCanAdmin(Boolean canAdmin) {
            this.canAdmin = canAdmin;
        }

        public Boolean getCanStar() {
            return canStar;
        }

        public void setCanStar(Boolean canStar) {
            this.canStar = canStar;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Long getExpires() {
            return expires;
        }

        public void setExpires(Long expires) {
            this.expires = expires;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public Date getUpdated() {
            return updated;
        }

        public void setUpdated(Date updated) {
            this.updated = updated;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public Boolean getHasAcl() {
            return hasAcl;
        }

        public void setHasAcl(Boolean hasAcl) {
            this.hasAcl = hasAcl;
        }

        public Boolean getIsFolder() {
            return isFolder;
        }

        public void setIsFolder(Boolean isFolder) {
            this.isFolder = isFolder;
        }

        public String getFolderId() {
            return folderId;
        }

        public void setFolderId(String folderId) {
            this.folderId = folderId;
        }

        public String getFolderUid() {
            return folderUid;
        }

        public void setFolderUid(String folderUid) {
            this.folderUid = folderUid;
        }

        public String getFolderTitle() {
            return folderTitle;
        }

        public void setFolderTitle(String folderTitle) {
            this.folderTitle = folderTitle;
        }

        public String getFolderUrl() {
            return folderUrl;
        }

        public void setFolderUrl(String folderUrl) {
            this.folderUrl = folderUrl;
        }

        public Boolean getProvisioned() {
            return provisioned;
        }

        public void setProvisioned(Boolean provisioned) {
            this.provisioned = provisioned;
        }

        @Override
        public String toString() {
            return "DashboardMeta{" +
                    "type='" + type + '\'' +
                    ", slug='" + slug + '\'' +
                    ", version=" + version +
                    '}';
        }
    }
}