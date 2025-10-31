package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.BrowserAPI;
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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPIImpl;
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
import java.util.Map;
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
    public Map<String, Object> driveSearch(final DriveRequestForm requestForm, final User user)
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
                .sortBy(requestForm.sortBy());

        // Determine if we're requesting from a specific folder or host root
        if (folder.isSystemFolder()) {
            builder.withHostOrFolderId(host.getIdentifier());
        } else {
            builder.withHostOrFolderId(folder.getInode());
        }

        // Enable Elasticsearch filtering for text search when filter is provided
        if (null != requestForm.filters() && !UtilMethods.isSet(requestForm.filters().text())) {
            builder.withUseElasticsearchFiltering(true) // Rely on ES for enhanced text filtering
                    .withFilter(requestForm.filters().text());
        }

        Logger.debug(this, String.format(
                "Content drive search - User: %s, Path: %s, Languages: %s, ContentTypes: %s, Filter: %s",
                user.getUserId(), assetPath, requestForm.language(), requestForm.contentTypes(), requestForm.filters()));

        return browserAPI.getPaginatedFolderContents(builder.build());
    }

    boolean isShowFile(Set<BaseContentType> baseTypes) {
       return baseTypes.contains(BaseContentType.FILEASSET);
    }

    boolean isShowDotAsset(Set<BaseContentType> baseTypes) {
       return baseTypes.contains(BaseContentType.DOTASSET);
    }

    Set<ContentType> getExcludedContentTypes() {
        try {
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            final ContentType host = contentTypeAPI.find(Host.HOST_VELOCITY_VAR_NAME);
            final ContentType forms = contentTypeAPI.find(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME);
            return Set.of(host, forms);
        } catch (DotDataException | DotSecurityException e) {
            Logger.warn(this, "Unable to retrieve excluded content types: " + e.getMessage(), e);
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