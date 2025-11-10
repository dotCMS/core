package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.asset.AssetPathResolver;
import com.dotcms.rest.api.v1.asset.ResolvedAssetAndPath;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Content Drive Helper
 * <p>Helper class responsible for handling the business logic for ContentDriveResource.</p>
 * <p>Provides drive-like functionality for browsing and searching content with advanced filtering capabilities.</p>
 */
public class ContentDriveHelper {

    private final BrowserAPI browserAPI;

    /**
     * Constructor with injected API dependencies
     * @param browserAPI browser API for content browsing operations
     */
    @VisibleForTesting
    ContentDriveHelper(final BrowserAPI browserAPI) {
        this.browserAPI = browserAPI;
    }

    /**
     * Default constructor with API locator dependencies
     */
    ContentDriveHelper() {
        this(APILocator.getBrowserAPI());
    }

    /**
     * Drive search functionality for content browsing with advanced filtering.
     * This endpoint is intended to be used to feed content-drive functionality.
     * It behaves similarly to BrowserResource but with enhanced capabilities:
     * - Can take a site/folder path expressed in the form //site/folder/subfolder/
     * - Supports multiple languages and content-types for more flexible filtering
     * - Uses Elasticsearch for text filtering while maintaining database reliability
     *
     * @param requestForm JSON body request with search parameters
     * @param user Current logged in user
     * @return a Map with all requested content and metadata
     * @throws DotDataException any data-related exception
     * @throws DotSecurityException any security violation exception
     */
    public PaginatedContents driveSearch(final DriveRequestForm requestForm, final User user)
            throws DotDataException, DotSecurityException {

        final List<Long> langIds = requestForm.language().stream()
                .map(LanguageUtil::getLanguageId)
                .collect(Collectors.toList());

        List<BaseContentType> baseContentTypes = BaseContentType.allBaseTypes();
        if (null != requestForm.baseTypes()) {
            baseContentTypes = requestForm.baseTypes().stream()
                    .map(s -> BaseContentType.getBaseContentType(s.toUpperCase()))
                    .collect(Collectors.toList());
        }

        final ContentTypeAPI myContentTypeAPI = APILocator.getContentTypeAPI(user);
        List<ContentType> contentTypes = List.of();
        if (null != requestForm.contentTypes()) {
            contentTypes = requestForm.contentTypes().stream()
                    .map(s -> Try.of(() -> myContentTypeAPI.find(s)).getOrNull())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        //These are types we always need to remove from our query
        final Set<ContentType> excludedContentTypes = getExcludedContentTypes();
        //Combine the list of base types using the ones directly provided plus the ones extracted from concrete the content-types passed
        final Set<BaseContentType> types = new HashSet<>(baseContentTypes);
        types.addAll(contentTypes.stream().map(ContentType::baseType).collect(Collectors.toList()));

        final AssetPathResolver resolver = AssetPathResolver.newInstance();
        final String assetPath = requestForm.assetPath();

        final ResolvedAssetAndPath assetAndPath = resolver.resolve(assetPath, user, false);
        final Host host = assetAndPath.resolvedHost();
        final Folder folder = assetAndPath.resolvedFolder();

        final Builder builder = BrowserQuery.builder();
        final boolean live = requestForm.live();
        final boolean showArchived = requestForm.archived();
        final boolean showFiles = isShowFile(types);
        final boolean showDotAssets = isShowDotAsset(types);
        if (null != requestForm.mimeTypes()){
            builder.showMimeTypes(requestForm.mimeTypes());
        }
        final String sortBy = sortBy(requestForm.sortBy());
        final boolean sortDesc = sortDesc(requestForm.sortBy());
        builder.withUser(user)
        //These are not always present
        .withContentTypes(
            contentTypes.stream().map(ContentType::id).collect(Collectors.toSet())
        )
        //However, these are
        .withExcludedContentTypes(
            excludedContentTypes.stream().map(ContentType::id).collect(Collectors.toSet())
        )
        .withBaseTypes(new ArrayList<>(types))
        .showDotAssets(showDotAssets)
        .showFiles(showFiles)
        .showImages(showFiles)
        .showArchived(showArchived)
        .showWorking(!live)
        .showFolders(true)
        .showLinks(false)
        .showContent(!baseContentTypes.isEmpty())
        .withLanguageIds(langIds)
        .offset(requestForm.offset())
        .maxResults(requestForm.maxResults())
        .overrideMaxResults(true)
        .sortBy(sortBy)
        .sortByDesc(sortDesc);

        // Determine if we're requesting from a specific folder or host root
        if (folder.isSystemFolder()) {
            builder.withHostOrFolderId(host.getIdentifier())
             /// if we're setting a site-name directly, we care fore all subfolders
             /// Therefore, we should skip setting a folder path
            .skipFolder(true);
        } else {
            builder.withHostOrFolderId(folder.getInode());
        }
        //This ensures that despite the site passed systemHost will be included too
        builder.withForceSystemHost(requestForm.includeSystemHost());

        // Enable Elasticsearch filtering for text search when filter is provided
        if (null != requestForm.filters() && UtilMethods.isSet(requestForm.filters().text())) {
             builder.useElasticsearchFiltering(true) // Rely on ES for enhanced text filtering
             .filterFolderNames(requestForm.filters().filterFolders())
             .withFilter(requestForm.filters().text());
        }

        Logger.debug(this, String.format(
                "Content drive search - User: %s, Path: %s, Languages: %s, ContentTypes: %s, Filter: %s",
                user.getUserId(), assetPath, requestForm.language(), requestForm.contentTypes(), requestForm.filters()));

        return browserAPI.getPaginatedContents(builder.build());
    }

    /**
     * if base types include FILEASSET then we pass the respective parameter as true
     * @param baseTypes base types
     * @return true if FILE_ASSET is present in the list
     */
    static boolean isShowFile(final Set<BaseContentType> baseTypes) {
       return baseTypes.contains(BaseContentType.FILEASSET);
    }

    /**
     * if base types include DOTASSET then we pass the respective parameter as true
     * @param baseTypes
     * @return true if DOT_ASSET is present in the list
     */
    static boolean isShowDotAsset(final Set<BaseContentType> baseTypes) {
       return baseTypes.contains(BaseContentType.DOTASSET);
    }

    /**
     * Processes a field string with an optional ":asc" or ":desc" suffix and returns the field name
     * without the sorting directive. If no valid suffix is present, the original string is returned.
     * If the input is null or empty, a default sort field is returned.
     *
     * @param fieldWithOrder a string representing a field name with an optional ":asc" or ":desc"
     *                       suffix indicating sorting order. Can be null or empty.
     * @return the field name without the sorting suffix, or the default sort field if the input
     *         is null or empty.
     */
    static String sortBy(final String fieldWithOrder) {
        if (fieldWithOrder == null || fieldWithOrder.trim().isEmpty()) {
            return AbstractDriveRequestForm.SORT_BY;
        }

        final String trimmed = fieldWithOrder.trim();
        final int lastColonIndex = trimmed.lastIndexOf(':');

        if (lastColonIndex == -1) {
            // No colon found, return as-is
            return trimmed;
        }

        final String suffix = trimmed.substring(lastColonIndex + 1).toLowerCase();

        if ("desc".equals(suffix) || "asc".equals(suffix)) {
            // Remove the :desc or :asc suffix
            return trimmed.substring(0, lastColonIndex);
        }

        // Colon found but not followed by desc/asc, return as-is
        return trimmed;
    }

    /**
     * Determines whether the provided sort key specifies descending order.
     * The function checks if the input string ends with "desc" (case-insensitive)
     * after trimming leading and trailing whitespace.
     *
     * @param sortBy the input sort key as a string. Typically, includes the field name
     *               followed by an optional ":asc" or ":desc" suffix.
     * @return true if the sort key specifies descending order ("desc"), false otherwise.
     */
    static boolean sortDesc(final String sortBy) {
        return sortBy.trim().toLowerCase().endsWith("desc");
    }

    /**
     * We never take into account Host nor Forms Content-types
     * @return
     */
    static Set<ContentType> getExcludedContentTypes() {
        try {
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            final ContentType host = contentTypeAPI.find(Host.HOST_VELOCITY_VAR_NAME);
            final ContentType forms = contentTypeAPI.find(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
            return Set.of(host, forms);
        } catch (DotDataException | DotSecurityException e) {
            Logger.warn(ContentDriveHelper.class, "Unable to retrieve excluded content types: " + e.getMessage(), e);
            return Set.of(); // Return empty set as fallback
        }
    }

    /**
     * Creates a new instance of ContentDriveHelper
     * @return ContentDriveHelper instance
     */
    public static ContentDriveHelper newInstance() {
        return new ContentDriveHelper();
    }
}