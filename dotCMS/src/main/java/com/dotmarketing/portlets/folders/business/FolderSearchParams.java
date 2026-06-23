package com.dotmarketing.portlets.folders.business;

import com.liferay.portal.model.User;
import java.util.Objects;

/**
 * Encapsulates all parameters for {@link FolderAPI#searchFolders}.
 * Construct via {@link #builder()}.
 */
public record FolderSearchParams(
        String name,
        String path,
        boolean recursive,
        String siteId,
        User user,
        boolean respectFrontendRoles,
        int limit,
        int offset,
        String orderBy,
        String orderDirection) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String path = "/";
        private boolean recursive = false;
        private String siteId;
        private User user;
        private boolean respectFrontendRoles = false;
        private int limit = 40;
        private int offset = 0;
        private String orderBy = "folder.name";
        private String orderDirection = "ASC";

        private Builder() {}

        public Builder name(final String name) { this.name = name; return this; }
        public Builder path(final String path) { this.path = path; return this; }
        public Builder recursive(final boolean recursive) { this.recursive = recursive; return this; }
        public Builder siteId(final String siteId) { this.siteId = siteId; return this; }
        public Builder user(final User user) { this.user = user; return this; }
        public Builder respectFrontendRoles(final boolean respectFrontendRoles) { this.respectFrontendRoles = respectFrontendRoles; return this; }
        public Builder limit(final int limit) { this.limit = limit; return this; }
        public Builder offset(final int offset) { this.offset = offset; return this; }
        public Builder orderBy(final String orderBy) { this.orderBy = orderBy; return this; }
        public Builder orderDirection(final String orderDirection) { this.orderDirection = orderDirection; return this; }

        public FolderSearchParams build() {
            Objects.requireNonNull(siteId, "siteId is required");
            Objects.requireNonNull(user,   "user is required");
            return new FolderSearchParams(name, path, recursive, siteId, user,
                    respectFrontendRoles, limit, offset, orderBy, orderDirection);
        }
    }
}