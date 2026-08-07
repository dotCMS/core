package com.dotmarketing.portlets.folders.business;

import com.liferay.portal.model.User;
import java.util.Objects;

/**
 * Encapsulates all parameters for {@link FolderAPI#searchFolders}.
 * Construct via {@link #builder()}.
 *
 * <p>{@code includePermissions} opts into per-folder permission computation: when {@code false}
 * (the default) the resulting views carry a {@code null} permission list and no extra permission
 * query is issued.
 *
 * <h2>Design note: this record carries eleven flat components</h2>
 *
 * <p>A record's canonical constructor is positional, and because this record is {@code public} the
 * canonical constructor cannot be declared less accessible than the record itself. So there is always
 * a callable eleven-argument entry point, and {@link #builder()} is a convenience rather than a
 * gate.</p>
 *
 * <p>Three pairs of adjacent components share a type — {@code name} / {@code path},
 * {@code limit} / {@code offset}, {@code orderBy} / {@code orderDirection} — and three more components
 * are {@code boolean} ({@code recursive}, {@code respectFrontendRoles}, {@code includePermissions}). A
 * transposed argument list therefore compiles and silently searches for the wrong thing, or quietly
 * flips a permission behaviour. Validating inside the canonical constructor does not help with this:
 * each transposed value is individually valid, so there is nothing for a check to reject.</p>
 *
 * <p>The components do fall into natural groups, which would remove the hazard by typing instead of by
 * discipline — a {@code PageRequest} cannot be passed where a {@code FolderCriteria} is expected:</p>
 *
 * <pre>{@code
 * public record FolderSearchParams(
 *         FolderCriteria criteria,   // name, path, recursive
 *         Requester requester,       // user, respectFrontendRoles, includePermissions
 *         PageRequest page) {        // limit, offset, sortColumn, sortDirection
 * }
 * }</pre>
 *
 * <p>Not applied here: this type appears in the {@link FolderAPI#searchFolders} signature, so
 * reshaping it is a public API change. Recorded so that keeping the flat shape stays a deliberate
 * choice rather than an oversight — and note that the component count has already grown once.</p>
 *
 * <p>Related, and independent of the shape: the {@code siteId} and {@code user} null checks currently
 * live in {@link Builder#build()}, which a direct call to the canonical constructor bypasses.</p>
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
        String orderDirection,
        boolean includePermissions) {

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
        private boolean includePermissions = false;

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
        public Builder includePermissions(final boolean includePermissions) { this.includePermissions = includePermissions; return this; }

        public FolderSearchParams build() {
            Objects.requireNonNull(siteId, "siteId is required");
            Objects.requireNonNull(user,   "user is required");
            return new FolderSearchParams(name, path, recursive, siteId, user,
                    respectFrontendRoles, limit, offset, orderBy, orderDirection, includePermissions);
        }
    }
}