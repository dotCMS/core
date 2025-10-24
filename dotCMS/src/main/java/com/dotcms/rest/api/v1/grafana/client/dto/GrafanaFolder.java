package com.dotcms.rest.api.v1.grafana.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO representing a Grafana folder.
 *
 * This class encapsulates folder information from the Grafana API including
 * permissions and metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrafanaFolder {

    private Long id;
    private String uid;
    private String title;
    private String url;
    private Boolean hasAcl;
    private Boolean canSave;
    private Boolean canEdit;
    private Boolean canAdmin;
    private String createdBy;
    private String updatedBy;
    private String created;
    private String updated;
    private Integer version;

    public GrafanaFolder() {}

    public GrafanaFolder(Long id, String uid, String title, String url, Boolean hasAcl,
                        Boolean canSave, Boolean canEdit, Boolean canAdmin, String createdBy,
                        String updatedBy, String created, String updated, Integer version) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.url = url;
        this.hasAcl = hasAcl;
        this.canSave = canSave;
        this.canEdit = canEdit;
        this.canAdmin = canAdmin;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.created = created;
        this.updated = updated;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getHasAcl() {
        return hasAcl;
    }

    public void setHasAcl(Boolean hasAcl) {
        this.hasAcl = hasAcl;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "GrafanaFolder{" +
                "id=" + id +
                ", uid='" + uid + '\'' +
                ", title='" + title + '\'' +
                ", version=" + version +
                '}';
    }
}