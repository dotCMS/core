package com.dotcms.browser;

import com.dotcms.api.tree.Parentable;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Theme;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

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
    final String  filter, fileName, sortBy;
    final int offset, maxResults;
    final boolean showWorking, showArchived, showFolders, sortByDesc, showLinks,showMenuItemsOnly,showContent, showShorties, showDefaultLangItems, useElasticsearchFiltering;
    final Set<Long> languageIds;
    final String luceneQuery;
    final Set<BaseContentType> baseTypes;
    final Set<String> contentTypeIds;
    final Set<String> excludedContentTypeIds;
    final Host site;
    final boolean forceSystemHost;
    final Folder folder;
    final Parentable directParent;
    final Role[] roles;
    final List<String> extensions, mimeTypes;

    /**
     * Returns the primary language ID for backward compatibility.
     * This returns the first language ID from the set, or 0 if no languages are set.
     */
    public long getLanguageId() {
        return languageIds.isEmpty() ? 0 : languageIds.iterator().next();
    }

    /**
     * Returns all language IDs as a Set.
     */
    public Set<Long> getLanguageIds() {
        return languageIds;
    }

    /**
     * Returns all content types as a Set.
     */
    public Set<String> getContentTypeIds() {
        return contentTypeIds;
    }

    @Override
    public String toString() {
        return "BrowserQuery {user:" + user + ", site:" + site + ", folder:" + folder + ", filter:"
                + filter + ", sortBy:" + sortBy
                + ", offset:" + offset + ", maxResults:" + maxResults + ", showWorking:"
                + showWorking + ", showArchived:"
                + showArchived + ", showFolders:" + showFolders + ", showDefaultLangItems:"
                + showDefaultLangItems + ", sortByDesc:" + sortByDesc + ", showLinks:"
                + showLinks + ", showContent:" + showContent + ", showShorties:" + showShorties
                + ", luceneQuery:" + luceneQuery
                + ", languageIds:" + StringUtils.join(languageIds)
                + ", baseTypes:" + StringUtils.join(baseTypes)
                + ", contentTypes:" + StringUtils.join(contentTypeIds)
                + "}";
    }

    private BrowserQuery(final Builder builder) {
        this.user = builder.user == null ? APILocator.systemUser() : builder.user;
        final Tuple2<Host, Folder> siteAndFolder = getParents(builder.hostFolderId,this.user, builder.hostIdSystemFolder);
        this.filter = builder.filter;
        this.useElasticsearchFiltering = builder.useElasticsearchFiltering;
        this.fileName = builder.fileName;
        this.luceneQuery = builder.luceneQuery.toString();
        this.sortBy = UtilMethods.isEmpty(builder.sortBy) ? "moddate" : builder.sortBy;
        this.offset = builder.offset;
        this.maxResults = Math.min(builder.maxResults, MAX_FETCH_PER_REQUEST);
        this.showWorking = builder.showWorking || builder.showArchived;
        this.showArchived = builder.showArchived;
        this.showFolders = builder.showFolders;
        this.showContent = builder.showContent;
        this.showShorties = builder.showShorties;
        this.mimeTypes     = builder.mimeTypes;
        this.extensions    = builder.extensions;
        this.sortByDesc = UtilMethods.isEmpty(builder.sortBy) || builder.sortByDesc;
        this.showLinks = builder.showLinks;
        this.showDefaultLangItems = builder.showDefaultLangItems;

        this.baseTypes = builder.baseTypes.isEmpty()
                ? ImmutableSet.of(BaseContentType.ANY)
                : ImmutableSet.copyOf(builder.baseTypes);
        this.languageIds = Set.copyOf(builder.languageIds);
        this.contentTypeIds = Set.copyOf(builder.contentTypes);
        this.excludedContentTypeIds = Set.copyOf(builder.excludedContentTypes);
        this.showMenuItemsOnly = builder.showMenuItemsOnly;
        this.site = siteAndFolder._1;
        this.folder= siteAndFolder._2;
        //Despite the site and folder passed, forceSystemHost makes the inclusion of SYSTEM_HOME in the query
        this.forceSystemHost = builder.forceSystemHost;
        this.directParent = this.folder.isSystemFolder() ? site : folder;
        this.roles= Try.of(()->APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0])).getOrElse(new Role[0]);
    }

    /**
     * Returns the appropriate Site and Folder based on the input from the User or system configuration. It's worth
     * noting that if the value of {@code parentId} points to the System Folder, then the value for
     * {@code hostIdSystemFolder} must be set as well.
     *
     * @param parentId           The ID of the Folder or Site that has been selected or specified.
     * @param user               The {@link User} executing this action.
     * @param hostIdSystemFolder The ID of a specific Site, if required.
     *
     * @return A Tuple2 containing the Site and its Folder.
     */
    @CloseDBIfOpened
    private Tuple2<Host, Folder> getParents(final String parentId, final User user, final String hostIdSystemFolder) {
        boolean respectFrontEndPermissions = PageMode.get().respectAnonPerms;
        Folder folderObj;
        boolean isSite = false;
        final Identifier identifier = Try.of(() -> APILocator.getIdentifierAPI().findFromInode(parentId)).getOrNull();
        if (null != identifier && UtilMethods.isSet(identifier.getId()) && !parentId.equalsIgnoreCase(
                Theme.SYSTEM_THEME) && Host.HOST_VELOCITY_VAR_NAME.equals(identifier.getAssetSubType())) {
            // The ID belongs to a Site
            folderObj = APILocator.getFolderAPI().findSystemFolder();
            isSite = true;
        } else {
            // The ID belongs to a Folder
            folderObj = parentId.equalsIgnoreCase(Theme.SYSTEM_THEME)
                                ? APILocator.getFolderAPI().findSystemFolder()
                                : Try.of(() -> APILocator.getFolderAPI().find(parentId, user, respectFrontEndPermissions)).getOrElse(new Folder());
        }
        String calculatedSiteId = null;
        // Determine the appropriate Site ID based on provided data
        if (isSite) {
            calculatedSiteId = UtilMethods.isSet(hostIdSystemFolder) ? hostIdSystemFolder : parentId;
        } else {
            if (null != folderObj) {
                calculatedSiteId = Folder.SYSTEM_FOLDER.equals(folderObj.getInode()) && UtilMethods.isSet(hostIdSystemFolder) ? hostIdSystemFolder :
                                           folderObj.getHostId();
            }
        }
        final String siteId = calculatedSiteId;
        final Host siteObj = Try.of(() -> APILocator.getHostAPI().find(siteId, user, respectFrontEndPermissions)).getOrNull();
        if (null == folderObj || UtilMethods.isEmpty(folderObj.getIdentifier()) || null == siteObj || UtilMethods.isEmpty(siteObj.getIdentifier())) {
            final String errorMsg = String.format("Parent ID '%s' [ %s ] does not match any existing Folder or Site.",
                    parentId, siteId);
            Logger.error(this, errorMsg + ". Maybe the Site/Folder was modified or deleted in the background. If " +
                                       "System Folder is specified, then set a value for hostIdSystemFolder as well.");
            throw new DotRuntimeException(errorMsg);
        }
        return Tuple.of(siteObj, folderObj);
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
        private boolean useElasticsearchFiltering = false;
        private String filter = null;
        private String fileName = null;
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
        private boolean showDefaultLangItems = false;
        private Set<Long> languageIds = new LinkedHashSet<>();
        private Set<String> contentTypes = new LinkedHashSet<>();
        private Set<String> excludedContentTypes = new LinkedHashSet<>();
        private final StringBuilder luceneQuery = new StringBuilder();
        private Set<BaseContentType> baseTypes = new HashSet<>();
        private String hostFolderId = FolderAPI.SYSTEM_FOLDER;
        private boolean forceSystemHost = false;
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
            this.forceSystemHost = browserQuery.forceSystemHost;
            this.filter = browserQuery.filter;
            this.fileName = browserQuery.fileName;
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
            this.languageIds = new LinkedHashSet<>(browserQuery.languageIds);
            this.contentTypes = new LinkedHashSet<>(browserQuery.contentTypeIds);
            this.excludedContentTypes = new LinkedHashSet<>(browserQuery.excludedContentTypeIds);
            this.showMenuItemsOnly = browserQuery.showMenuItemsOnly;
            this.mimeTypes = browserQuery.mimeTypes;
            this.extensions = browserQuery.extensions;
            this.showContent = browserQuery.showContent;
            this.showShorties = browserQuery.showShorties;
            this.showDefaultLangItems = browserQuery.showDefaultLangItems;
        }

        public Builder withUser(@Nonnull User user) {
            this.user = user;
            return this;
        }

        public Builder withHostOrFolderId(@Nonnull String hostFolderId) {
            this.hostFolderId = hostFolderId;
            return this;
        }

        public Builder withForceSystemHost(boolean forceSystemHost) {
            this.forceSystemHost = forceSystemHost;
            return this;
        }

        /**
         * This activates seatch text using ElasticSearch
         * @param useElasticsearchFiltering
         * @return
         */
        public Builder withUseElasticsearchFiltering(boolean useElasticsearchFiltering) {
            this.useElasticsearchFiltering = useElasticsearchFiltering;
            return this;
        }

        public Builder withFilter(@Nonnull String filter) {
            if (UtilMethods.isSet(filter)) {
                luceneQuery.append(StringPool.SPACE).append(filter);
                this.filter = filter;
            }
            return this;
        }

        public Builder withFileName(@Nonnull String fileName) {
            if (UtilMethods.isSet(fileName)) {
                // for exact file-name match we need to relay exclusively on the database
                // we can not trust on the use of the title field indexed in lucene
                // As different files can share the same title, and we need an exact match on identifier.asset_name
                // Therefore we need to make it fail on purpose by adding a non-existing value to the query
                // If we include this fileNAme here BrowserAPI will try to match the title in lucene bringing back false positives
                luceneQuery.append(StringPool.SPACE).append("___").append(fileName).append("___");
                this.fileName = fileName;
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

        public Builder showImages(@Nonnull boolean showImages) {
            if(showImages){
                baseTypes.add(BaseContentType.FILEASSET);
            }
            return this;
        }

        public Builder withLanguageId(@Nonnull long languageId) {
            this.languageIds.clear();
            if (languageId > 0) {
                this.languageIds.add(languageId);
            }
            return this;
        }

        public Builder withLanguageIds(@Nonnull Set<Long> languageIds) {
            this.languageIds.clear();
            this.languageIds.addAll(languageIds);
            return this;
        }

        public Builder withLanguageIds(@Nonnull List<Long> languageIds) {
            this.languageIds.clear();
            this.languageIds.addAll(languageIds);
            return this;
        }

        public Builder addLanguageId(@Nonnull long languageId) {
            if (languageId > 0) {
                this.languageIds.add(languageId);
            }
            return this;
        }

        public Builder withContentTypes(@Nonnull Set<String> contentTypes) {
            this.contentTypes.clear();
            this.contentTypes.addAll(contentTypes);
            return this;
        }

        public Builder withContentTypes(@Nonnull List<String> contentTypes) {
            this.contentTypes.clear();
            this.contentTypes.addAll(contentTypes);
            return this;
        }

        public Builder addContentType(@Nonnull String contentType) {
            if (UtilMethods.isSet(contentType)) {
                this.contentTypes.add(contentType);
            }
            return this;
        }

        /**
         * This option is useful if we want to build a query that implicitly takes into account every content-type except for the ones specified.
         * @param excludedContentTypes
         * @return
         */
        public Builder withExcludedContentTypes(@Nonnull Set<String> excludedContentTypes) {
            this.excludedContentTypes.clear();
            this.excludedContentTypes.addAll(excludedContentTypes);
            this.contentTypes.removeAll(excludedContentTypes);
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

        public Builder showDefaultLangItems(@Nonnull boolean showDefaultLangItems) {
            this.showDefaultLangItems = showDefaultLangItems;
            return this;
        }

        public BrowserQuery build() {
            return new BrowserQuery(this);
        }

    }

}
