package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Form to encapsulate the copy site
 * @author jsanca
 */
public class CopySiteForm {

    private final String  copyFromSiteId;
    private final boolean copyAll;
    private final boolean copyTemplatesContainers;
    private final boolean copyContentOnPages;
    private final boolean copyFolders;
    private final boolean copyContentOnSite;
    private final boolean copyLinks;
    private final boolean copySiteVariables;
    private final boolean copyContentTypes;
    private final SiteForm site;

    @JsonCreator
    public CopySiteForm(
            @JsonProperty("copyFromSiteId") final String copyFromSiteId,
            @JsonProperty("copyAll") final boolean copyAll,
            @JsonProperty("copyTemplatesContainers") final boolean copyTemplatesContainers,
            @JsonProperty("copyContentOnPages") final boolean copyContentOnPages,
            @JsonProperty("copyFolders") final boolean copyFolders,
            @JsonProperty("copyContentOnSite") final boolean copyContentOnSite,
            @JsonProperty("copyLinks") final boolean copyLinks,
            @JsonProperty("copySiteVariables") final boolean copySiteVariables,
            @JsonProperty("copyContentTypes") final boolean copyContentTypes,
            @JsonProperty("site") final SiteForm site) {

        this.copyFromSiteId = copyFromSiteId;
        this.copyAll = copyAll;
        this.copyTemplatesContainers = copyTemplatesContainers;
        this.copyContentOnPages = copyContentOnPages;
        this.copyFolders = copyFolders;
        this.copyContentOnSite = copyContentOnSite;
        this.copyLinks = copyLinks;
        this.copySiteVariables = copySiteVariables;
        this.copyContentTypes = copyContentTypes;
        this.site = site;
    }

    public SiteForm getSite() {
        return site;
    }

    public String getCopyFromSiteId() {
        return copyFromSiteId;
    }

    public boolean isCopyAll() {
        return copyAll;
    }

    public boolean isCopyTemplatesContainers() {
        return copyTemplatesContainers;
    }

    public boolean isCopyContentOnPages() {
        return copyContentOnPages;
    }

    public boolean isCopyFolders() {
        return copyFolders;
    }

    public boolean isCopyContentOnSite() {
        return copyContentOnSite;
    }

    public boolean isCopyLinks() {
        return copyLinks;
    }

    public boolean isCopySiteVariables() {
        return copySiteVariables;
    }

    public boolean isCopyContentTypes() {
        return copyContentTypes;
    }

    @Override
    public String toString() {
        return "CopySiteForm{" +
                "copyFromSiteId='" + copyFromSiteId + '\'' +
                ", copyAll=" + copyAll +
                ", copyTemplatesContainers=" + copyTemplatesContainers +
                ", copyContentOnPages=" + copyContentOnPages +
                ", copyFolders=" + copyFolders +
                ", copyContentOnSite=" + copyContentOnSite +
                ", copyLinks=" + copyLinks +
                ", copySiteVariables=" + copySiteVariables +
                ", copyContentTypes=" + copyContentTypes +
                ", site=" + site +
                '}';
    }
}
