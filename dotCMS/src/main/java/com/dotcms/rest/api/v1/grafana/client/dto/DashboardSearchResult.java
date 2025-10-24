package com.dotcms.rest.api.v1.grafana.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTO representing a dashboard search result from Grafana API.
 *
 * This class is used for dashboard search operations and folder dashboard listings.
 * It provides basic dashboard information and metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardSearchResult {

    private Long id;
    private String uid;
    private String title;
    private String uri;
    private String url;
    private String slug;
    private String type;
    private List<String> tags;
    private Boolean isStarred;
    private String folderId;
    private String folderUid;
    private String folderTitle;
    private String folderUrl;

    public DashboardSearchResult() {}

    public DashboardSearchResult(Long id, String uid, String title, String uri, String url,
                               String slug, String type, List<String> tags, Boolean isStarred,
                               String folderId, String folderUid, String folderTitle, String folderUrl) {
        this.id = id;
        this.uid = uid;
        this.title = title;
        this.uri = uri;
        this.url = url;
        this.slug = slug;
        this.type = type;
        this.tags = tags;
        this.isStarred = isStarred;
        this.folderId = folderId;
        this.folderUid = folderUid;
        this.folderTitle = folderTitle;
        this.folderUrl = folderUrl;
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getIsStarred() {
        return isStarred;
    }

    public void setIsStarred(Boolean isStarred) {
        this.isStarred = isStarred;
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

    @Override
    public String toString() {
        return "DashboardSearchResult{" +
                "id=" + id +
                ", uid='" + uid + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", folderTitle='" + folderTitle + '\'' +
                '}';
    }
}