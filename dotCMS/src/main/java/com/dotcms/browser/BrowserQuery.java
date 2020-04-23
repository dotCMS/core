package com.dotcms.browser;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.model.User;

@JsonDeserialize(builder = BrowserQuery.Builder.class)
public class BrowserQuery {

    final User user;
    final String hostFolderId, filter, sortBy;
    final int offset, maxResults;
    final boolean showWorking, showArchived, showFolders, showFiles, showPages,sortByDesc, showLinks, showDotAssets;
    final long languageId;
    final List<String> extensions, mimeTypes;
    
    @Override
    public String toString() {
        return "BrowserQuery {user:" + user + ", hostFolderId:" + hostFolderId + ", filter:" + filter + ", sortBy:" + sortBy + ", offset:" + offset + ", maxResults:" + maxResults + ", showWorking:" + showWorking
                        + ", showArchived:" + showArchived + ", showFolders:" + showFolders + ", onlyFiles:" + showFiles
                        + ", sortByDesc:" + sortByDesc + ", showLinks:" + showLinks + ", showDotAssets:" + showDotAssets + ", showPages:" + showPages+ ", languageId:"
                        + languageId + ", extensions:" + extensions + ", mimeTypes:" + mimeTypes + "}";
    }


    private BrowserQuery(Builder builder) {
        this.user = (builder.user==null) ? APILocator.systemUser() : builder.user;
        this.hostFolderId = builder.hostFolderId;
        this.filter = builder.filter;
        this.mimeTypes = builder.mimeTypes;
        this.extensions = builder.extensions;
        this.sortBy = UtilMethods.isEmpty(builder.sortBy) ? "moddate" : builder.sortBy;
        this.offset = builder.offset;
        this.maxResults = builder.maxResults>500 ? 500 : builder.maxResults;
        this.showWorking = builder.showWorking;
        this.showArchived = builder.showArchived;
        this.showFolders = builder.showFolders;
        this.showFiles = builder.showFiles;
        this.sortByDesc = builder.sortByDesc;
        this.showLinks = builder.showLinks;
        this.showPages = builder.showPages;
        this.showDotAssets = builder.showDotAssets;
        this.languageId = builder.languageId;
  
    }


    /**
     * Creates builder to build {@link BrowserQuery}.
     * 
     * @return created builder
     */
    
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link BrowserQuery} and initialize it with the given object.
     * 
     * @param browserQuery to initialize the builder with
     * @return created builder
     */
    
    public static Builder from(BrowserQuery browserQuery) {
        return new Builder(browserQuery);
    }

    /**
     * Builder to build {@link BrowserQuery}.
     */
    
    public static final class Builder {
        private User user ;
        private String hostFolderId=Folder.SYSTEM_FOLDER;
        private String filter = null;
        private List<String> mimeTypes=new ArrayList<>();
        private List<String>  extensions=new ArrayList<>();
        private String sortBy="moddate";
        private int offset=0;
        private int maxResults=100;
        private boolean showWorking=true;
        private boolean showArchived=false;
        private boolean showFolders=false;
        private boolean showFiles=false;
        private boolean sortByDesc=false;
        private boolean showLinks=false;
        private boolean showPages=false;
        private boolean showDotAssets=false;
        private long languageId=0;

        private Builder() {}

        private Builder(BrowserQuery browserQuery) {
            this.user = browserQuery.user;
            this.hostFolderId = browserQuery.hostFolderId;
            this.filter = browserQuery.filter;
            this.mimeTypes = browserQuery.mimeTypes;
            this.extensions = browserQuery.extensions;
            this.sortBy = browserQuery.sortBy;
            this.offset = browserQuery.offset;
            this.maxResults = browserQuery.maxResults;
            this.showWorking = browserQuery.showWorking;
            this.showArchived = browserQuery.showArchived;
            this.showFolders = browserQuery.showFolders;
            this.showFiles = browserQuery.showFiles;
            this.showPages = browserQuery.showPages;
            this.sortByDesc = browserQuery.sortByDesc;
            this.showLinks = browserQuery.showLinks;
            this.showDotAssets = browserQuery.showDotAssets;
            this.languageId = browserQuery.languageId;
        }

        public Builder withUser(@Nonnull User user) {
            this.user = user;
            return this;
        }
        
        public Builder inHostOrFolder(@Nonnull Folder folder) {
            this.hostFolderId = folder !=null ? folder.getInode() : null;
            return this;
        }
        public Builder inHostOrFolder(@Nonnull Host host) {
            this.hostFolderId = host.getIdentifier();
            return this;
        }
        public Builder withHostId(@Nonnull String hostid) {
            this.hostFolderId = hostid;
            return this;
        }
        public Builder withHostOrFolderId(@Nonnull String hostFolderId) {
            this.hostFolderId = hostFolderId;
            return this;
        }

        public Builder withFilter(@Nonnull String filter) {
            this.filter = filter;
            return this;
        }

        public Builder showMimeTypes(@Nonnull List<String> mimeTypes) {
            this.mimeTypes = mimeTypes;
            return this;
        }

        public Builder showExtensions(@Nonnull List<String>  extensions) {
            this.extensions = extensions;
            return this;
        }

        public Builder sortBy(@Nonnull String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder offset(@Nonnull int offset) {
            this.offset = offset;
            return this;
        }

        public Builder maxResults(@Nonnull int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder showWorking(@Nonnull boolean showWorking) {
            this.showWorking = showWorking;
            return this;
        }

        public Builder showArchived(@Nonnull boolean showArchived) {
            this.showArchived = showArchived;
            return this;
        }

        public Builder showFolders(@Nonnull boolean showFolders) {
            this.showFolders = showFolders;
            return this;
        }

        public Builder showFiles(@Nonnull boolean showFiles) {
            this.showFiles = showFiles;
            return this;
        }

        public Builder sortByDesc(@Nonnull boolean sortByDesc) {
            this.sortByDesc = sortByDesc;
            return this;
        }

        public Builder showLinks(@Nonnull boolean showLinks) {
            this.showLinks = showLinks;
            return this;
        }

        public Builder showPages(@Nonnull boolean showPages) {
            this.showPages = showPages;
            return this;
        }
        
        public Builder showDotAssets(@Nonnull boolean dotAssets) {
            this.showDotAssets = dotAssets;
            return this;
        }

        public Builder withLanguageId(@Nonnull long languageId) {
            this.languageId = languageId;
            return this;
        }

        public BrowserQuery build() {
            return new BrowserQuery(this);
        }
    }



}

