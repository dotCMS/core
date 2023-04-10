package com.dotcms.browser;

import com.dotcms.api.tree.Parentable;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Theme;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class abstracts all the different querying and filtering criteria used to return results to the
 * {@code Site Browser} portlet in dotCMS. This operation can get increasingly complex as more filtering parameters are
 * used to return information, so a Query Builder approach is a lot simpler and more scalable.
 *
 * @author Jonathan Sanchez
 * @since Apr 28th, 2020
 */
@JsonDeserialize(builder = BrowserQuery.Builder.class)
public class BrowserQuery {

    private static final int MAX_FETCH_PER_REQUEST = Config.getIntProperty("BROWSER_MAX_FETCH_PER_REQUEST", 300);
    final User user;
    final String  filter, sortBy;
    final int offset, maxResults;
    final boolean showWorking, showArchived, showFolders, sortByDesc, showLinks,showMenuItemsOnly,showContent, showShorties;
    final long languageId;
    final String luceneQuery;
    final Set<BaseContentType> baseTypes;
    final Host site;
    final Folder folder;
    final Parentable directParent;
    final Role[] roles;
    final List<String> extensions, mimeTypes;

    @Override
    public String toString() {
        return "BrowserQuery {user:" + user + ", site:" + site + ", folder:" + folder + ", filter:" + filter + ", sortBy:" + sortBy
                + ", offset:" + offset + ", maxResults:" + maxResults + ", showWorking:" + showWorking + ", showArchived:"
                + showArchived + ", showFolders:" + showFolders + ", sortByDesc:" + sortByDesc + ", showLinks:"
                + showLinks + ", showContent:" + showContent + ", showShorties:" + showShorties + ", languageId:" + languageId + ", luceneQuery:" + luceneQuery
                + ", baseTypes:" + baseTypes + "}";
    }

    private BrowserQuery(final Builder builder) {
        this.user = builder.user == null ? APILocator.systemUser() : builder.user;
        final Tuple2<Host, Folder> siteAndFolder = getParents(builder.hostFolderId,this.user, builder.hostIdSystemFolder);
        this.filter = builder.filter;
        this.luceneQuery = builder.luceneQuery.toString();
        this.sortBy = UtilMethods.isEmpty(builder.sortBy) ? "moddate" : builder.sortBy;
        this.offset = builder.offset;
        this.maxResults = builder.maxResults > MAX_FETCH_PER_REQUEST ? MAX_FETCH_PER_REQUEST : builder.maxResults;
        this.showWorking = builder.showWorking || builder.showArchived;
        this.showArchived = builder.showArchived;
        this.showFolders = builder.showFolders;
        this.showContent = builder.showContent;
        this.showShorties = builder.showShorties;
        this.mimeTypes     = builder.mimeTypes;
        this.extensions    = builder.extensions;
        this.sortByDesc = UtilMethods.isEmpty(builder.sortBy) ? true : builder.sortByDesc;
        this.showLinks = builder.showLinks;

        this.baseTypes = builder.baseTypes.isEmpty()
                ? ImmutableSet.of(BaseContentType.ANY)
                : ImmutableSet.copyOf(builder.baseTypes);
        this.languageId = builder.languageId;
        this.showMenuItemsOnly = builder.showMenuItemsOnly;
        this.site = siteAndFolder._1;
        this.folder= siteAndFolder._2;
        this.directParent = this.folder.isSystemFolder() ? site : folder;
        this.roles= Try.of(()->APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0])).getOrElse(new Role[0]);
    }

    @CloseDBIfOpened
    private Tuple2<Host, Folder> getParents(final String parentId, final User user, final String hostIdSystemFolder) {
        boolean respectFrontEndPermissions = PageMode.get().respectAnonPerms;
        //check if the parentId exists
        final Identifier identifier = Try.of(() -> APILocator.getIdentifierAPI().findFromInode(parentId)).getOrNull();
        if ((identifier == null || UtilMethods.isEmpty(identifier.getId())) && (!parentId.equalsIgnoreCase(
                Theme.SYSTEM_THEME))) {
            Logger.error(this, "parentId doesn't belong to a Folder or a Host, id: " + parentId
                    + ", maybe the Folder was modified in the background. If using SystemFolder must send hostIdSystemFolder.");
            throw new DotRuntimeException("parentId doesn't belong to a Folder or a Host, id: " + parentId);
        }

        // gets folder parent
        final Folder folder = Try.of(() -> APILocator.getFolderAPI().find(parentId, user, respectFrontEndPermissions)).toJavaOptional()
                .orElse(APILocator.getFolderAPI().findSystemFolder());
        final Host site = folder.isSystemFolder()
                ? null != hostIdSystemFolder ? Try.of(() -> APILocator.getHostAPI().find(hostIdSystemFolder, user, respectFrontEndPermissions)).getOrNull()
                :  Try.of(() -> WebAPILocator.getHostWebAPI().getCurrentHost()).getOrNull()
                : Try.of(() -> APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions)).getOrNull();

        return Tuple.of(site, folder);
    }

    /**
     *
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

        private User user;
        private String filter = null;
        private String sortBy = "moddate";
        private int offset = 0;
        private int maxResults = MAX_FETCH_PER_REQUEST;
        private boolean showWorking = true;
        private boolean showArchived = false;
        private boolean showContent = true;
        private boolean showShorties = false;
        private boolean showFolders = false;
        private boolean sortByDesc = false;
        private boolean showLinks = false;
        private boolean showMenuItemsOnly = false;
        private long languageId = 0;
        private final StringBuilder luceneQuery = new StringBuilder();
        private Set<BaseContentType> baseTypes = new HashSet<>();
        private String hostFolderId = FolderAPI.SYSTEM_FOLDER_ID;
        private String hostIdSystemFolder = null;
        private List<String> mimeTypes = new ArrayList<>();
        private List<String> extensions = new ArrayList<>();

        private Builder() {
        }

        private Builder(BrowserQuery browserQuery) {
            this.user = browserQuery.user;
            this.hostFolderId = browserQuery.folder.isSystemFolder()
                    ? browserQuery.site.getIdentifier()
                    : browserQuery.folder.getInode();
            this.filter = browserQuery.filter;
            if (browserQuery.luceneQuery != null) {
                this.luceneQuery.append(browserQuery.luceneQuery);
            }
            this.sortBy = browserQuery.sortBy;
            this.offset = browserQuery.offset;
            this.maxResults = browserQuery.maxResults;
            this.showWorking = browserQuery.showWorking;
            this.showArchived = browserQuery.showArchived;
            this.showFolders = browserQuery.showFolders;
            this.sortByDesc = browserQuery.sortByDesc;
            this.showLinks = browserQuery.showLinks;
            this.languageId = browserQuery.languageId;
            this.showMenuItemsOnly = browserQuery.showMenuItemsOnly;
            this.mimeTypes = browserQuery.mimeTypes;
            this.extensions = browserQuery.extensions;
            this.showContent = browserQuery.showContent;
            this.showShorties = browserQuery.showShorties;
        }

        public Builder withUser(@Nonnull User user) {
            this.user = user;
            return this;
        }

        public Builder withHostOrFolderId(@Nonnull String hostFolderId) {
            this.hostFolderId = hostFolderId;
            return this;
        }

        public Builder withFilter(@Nonnull String filter) {
            if (UtilMethods.isSet(filter)) {
                luceneQuery.append(StringPool.SPACE).append(filter);
                this.filter = filter;
            }
            return this;
        }

        public Builder showOnlyMenuItems(boolean menuItems) {
            this.showMenuItemsOnly = menuItems;
            return this;
        }

        public Builder showMimeTypes(@Nonnull List<String> mimeTypes) {
            this.mimeTypes = mimeTypes;
            return this;
        }

        public Builder showExtensions(@Nonnull List<String> extensions) {
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
            this.maxResults = maxResults < 0 ? this.maxResults : maxResults;
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

        public Builder showContent(@Nonnull boolean showContent) {
            this.showContent = showContent;
            return this;
        }

        /**
         * Determines if the Shorty IDs of a given dotCMS object must be included in the result set.
         *
         * @param showShorties If Shorty IDs are required, set to {@code true}.
         *
         * @return The current Builder instance.
         */
        public Builder showShorties(@Nonnull boolean showShorties) {
            this.showShorties = showShorties;
            return this;
        }

        public Builder showFolders(@Nonnull boolean showFolders) {
            this.showFolders = showFolders;
            return this;
        }

        public Builder showFiles(@Nonnull boolean showFiles) {
            if (showFiles) {
                baseTypes.add(BaseContentType.FILEASSET);
            }
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
            if (showPages) {
                baseTypes.add(BaseContentType.HTMLPAGE);
            }
            return this;
        }

        public Builder showDotAssets(@Nonnull boolean dotAssets) {
            if (dotAssets) {
                baseTypes.add(BaseContentType.DOTASSET);
            }
            return this;
        }

        public Builder withLanguageId(@Nonnull long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder withBaseTypes(@Nonnull List<BaseContentType> types) {
            baseTypes.addAll(types);
            return this;
        }

        public Builder hostIdSystemFolder(@Nonnull String hostIdSystemFolder) {
            this.hostIdSystemFolder = hostIdSystemFolder;
            return this;
        }

        public BrowserQuery build() {
            return new BrowserQuery(this);
        }

    }

}
