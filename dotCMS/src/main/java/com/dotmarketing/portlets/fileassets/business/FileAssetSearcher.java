package com.dotmarketing.portlets.fileassets.business;

import javax.annotation.Nonnull;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.liferay.portal.model.User;
import io.vavr.control.Try;


@JsonDeserialize(builder = FileAssetSearcher.Builder.class)
public class FileAssetSearcher {

    final Host host;
    final Folder folder;
    final long language;
    final int offset, limit;
    final String searchTerm;
    final User user;
    final boolean respectFrontendRoles;
    final boolean live;
    private FileAssetSearcher(Builder builder) {

        
        this.host = builder.host ;
        this.folder = builder.folder;
        this.language = builder.language;
        this.offset = builder.offset;
        this.limit = builder.limit;
        this.searchTerm = builder.searchTerm;
        this.user = builder.user;
        this.respectFrontendRoles = builder.respectFrontendRoles;
        this.live=builder.live;
    }
    /**
     * Creates builder to build {@link FileAssetSearcher}.
     * @return created builder
     */
    
    public static Builder builder() {
        return new Builder();
    }
    /**
     * Creates a builder to build {@link FileAssetSearcher} and initialize it with the given object.
     * @param fileAssetSearcher to initialize the builder with
     * @return created builder
     */
    
    public static Builder from(FileAssetSearcher fileAssetSearcher) {
        return new Builder(fileAssetSearcher);
    }
    /**
     * Builder to build {@link FileAssetSearcher}.
     */
    
    public static final class Builder {
        private Host host;
        private Folder folder;
        private long language;
        private int offset;
        private int limit;
        private String searchTerm;
        private User user;
        private boolean respectFrontendRoles;
        private boolean live;
        private Builder() {}

        private Builder(FileAssetSearcher fileAssetSearcher) {
            this.host = fileAssetSearcher.host;
            this.folder = fileAssetSearcher.folder;
            this.language = fileAssetSearcher.language;
            this.offset = fileAssetSearcher.offset;
            this.limit = fileAssetSearcher.limit;
            this.searchTerm = fileAssetSearcher.searchTerm;
            this.user = fileAssetSearcher.user;
            this.respectFrontendRoles = fileAssetSearcher.respectFrontendRoles;
            this.live=fileAssetSearcher.live;
        }

        public Builder host(@Nonnull Host host) {
            this.host = host;
            // set folder if it is null
            this.folder = (this.folder == null) ? Try.of(()-> APILocator.getFolderAPI().findSystemFolder()).getOrElseThrow(e -> new DotRuntimeException(e)) : folder;
            return this;
        }

        public Builder folder(@Nonnull Folder folder) {
            this.folder = folder;
            // set host if it is null
            this.host = (this.host == null) ? folder.getHost() : this.host;
            return this;
        }

        public Builder language(@Nonnull long language) {
            this.language = language;
            return this;
        }

        public Builder offset(@Nonnull int offset) {
            this.offset = offset;
            return this;
        }

        public Builder limit(@Nonnull int limit) {
            this.limit = limit;
            return this;
        }

        public Builder searchTerm(@Nonnull String searchTerm) {
            this.searchTerm = searchTerm;
            return this;
        }

        public Builder user(@Nonnull User user) {
            this.user = user;
            return this;
        }
        public Builder live(boolean live) {
            this.live = live;
            return this;
        }

        public Builder respectFrontendRoles(@Nonnull boolean respectFrontEndRoles) {
            this.respectFrontendRoles = respectFrontEndRoles;
            return this;
        }

        public FileAssetSearcher build() {
            if(this.folder==null) {
                throw new DotRuntimeException("folder needs to be set");
            }
            return new FileAssetSearcher(this);
        }
    }
    


}
