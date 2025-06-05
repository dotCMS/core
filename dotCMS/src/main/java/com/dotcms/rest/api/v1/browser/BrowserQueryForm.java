package com.dotcms.rest.api.v1.browser;

import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = BrowserQueryForm.Builder.class)
public class BrowserQueryForm {

    private final String hostFolderId, filter, sortBy;
    private final int offset, maxResults;
    private final boolean showWorking, showArchived, showFolders, showFiles, showPages,sortByDesc, showLinks, showDotAssets;
    private final long languageId;
    private final List<String> extensions, mimeTypes;

    private BrowserQueryForm(final BrowserQueryForm.Builder builder) {

        this.hostFolderId  = builder.hostFolderId;
        this.filter        = builder.filter;
        this.mimeTypes     = builder.mimeTypes;
        this.extensions    = builder.extensions;
        this.sortBy        = UtilMethods.isEmpty(builder.sortBy) ? "moddate" : builder.sortBy;
        this.offset        = builder.offset;
        this.maxResults    = builder.maxResults>500 ? 500 : builder.maxResults;
        this.showWorking   = builder.showWorking;
        this.showArchived  = builder.showArchived;
        this.showFolders   = builder.showFolders;
        this.showFiles     = builder.showFiles;
        this.sortByDesc    = builder.sortByDesc;
        this.showLinks     = builder.showLinks;
        this.showPages     = builder.showPages;
        this.showDotAssets = builder.showDotAssets;
        this.languageId    = builder.languageId;
    }

    public String getHostFolderId() {
        return hostFolderId;
    }

    public String getFilter() {
        return filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    public int getOffset() {
        return offset;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public boolean isShowWorking() {
        return showWorking;
    }

    public boolean isShowArchived() {
        return showArchived;
    }

    public boolean isShowFolders() {
        return showFolders;
    }

    public boolean isShowFiles() {
        return showFiles;
    }

    public boolean isShowPages() {
        return showPages;
    }

    public boolean isSortByDesc() {
        return sortByDesc;
    }

    public boolean isShowLinks() {
        return showLinks;
    }

    public boolean isShowDotAssets() {
        return showDotAssets;
    }

    public long getLanguageId() {
        return languageId;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public String toString() {
        return "BrowserQueryForm{" +
                "hostFolderId='" + hostFolderId + '\'' +
                ", filter='" + filter + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", offset=" + offset +
                ", maxResults=" + maxResults +
                ", showWorking=" + showWorking +
                ", showArchived=" + showArchived +
                ", showFolders=" + showFolders +
                ", showFiles=" + showFiles +
                ", showPages=" + showPages +
                ", sortByDesc=" + sortByDesc +
                ", showLinks=" + showLinks +
                ", showDotAssets=" + showDotAssets +
                ", languageId=" + languageId +
                ", extensions=" + extensions +
                ", mimeTypes=" + mimeTypes +
                '}';
    }

    public static final class Builder {

        @JsonProperty
        private String hostFolderId   = FolderAPI.SYSTEM_FOLDER;

        @JsonProperty
        private String filter         = null;

        @JsonProperty
        private List<String> mimeTypes  = new ArrayList<>();

        @JsonProperty
        private List<String> extensions = new ArrayList<>();

        @JsonProperty
        private String sortBy         = "moddate";

        @JsonProperty
        private int offset            = 0;

        @JsonProperty
        private int maxResults        = 100;

        @JsonProperty
        private boolean showWorking   = true;

        @JsonProperty
        private boolean showArchived  = false;

        @JsonProperty
        private boolean showFolders   = false;

        @JsonProperty
        private boolean showFiles     = false;

        @JsonProperty
        private boolean sortByDesc    = false;

        @JsonProperty
        private boolean showLinks     = false;

        @JsonProperty
        private boolean showPages     = false;

        @JsonProperty
        private boolean showDotAssets = false;

        @JsonProperty
        private long languageId       = 0;

        private Builder() {}

        public BrowserQueryForm.Builder hostFolderId(final String hostFolderId) {
            this.hostFolderId = hostFolderId;
            return this;
        }

        public BrowserQueryForm.Builder filter(final String filter) {
            this.filter = filter;
            return this;
        }

        public BrowserQueryForm.Builder mimeTypes(final List<String> mimeTypes) {
            this.mimeTypes = mimeTypes;
            return this;
        }

        public BrowserQueryForm.Builder extensions(final List<String>  extensions) {
            this.extensions = extensions;
            return this;
        }

        public BrowserQueryForm.Builder sortBy(final String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public BrowserQueryForm.Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        public BrowserQueryForm.Builder maxResults(final int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public BrowserQueryForm.Builder showWorking(final boolean showWorking) {
            this.showWorking = showWorking;
            return this;
        }

        public BrowserQueryForm.Builder showArchived(final boolean showArchived) {
            this.showArchived = showArchived;
            return this;
        }

        public BrowserQueryForm.Builder showFolders(final boolean showFolders) {
            this.showFolders = showFolders;
            return this;
        }

        public BrowserQueryForm.Builder showFiles(final boolean showFiles) {
            this.showFiles = showFiles;
            return this;
        }

        public BrowserQueryForm.Builder sortByDesc(final boolean sortByDesc) {
            this.sortByDesc = sortByDesc;
            return this;
        }

        public BrowserQueryForm.Builder showLinks(final boolean showLinks) {
            this.showLinks = showLinks;
            return this;
        }

        public BrowserQueryForm.Builder showPages(final boolean showPages) {
            this.showPages = showPages;
            return this;
        }

        public BrowserQueryForm.Builder showDotAssets(final boolean showDotAssets) {
            this.showDotAssets = showDotAssets;
            return this;
        }

        public BrowserQueryForm.Builder languageId(final long languageId) {
            this.languageId = languageId;
            return this;
        }

        public BrowserQueryForm build() {
            return new BrowserQueryForm(this);
        }
    }
}
