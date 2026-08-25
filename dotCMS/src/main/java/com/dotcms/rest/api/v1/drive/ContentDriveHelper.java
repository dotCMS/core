package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.BrowserQuery.Builder;
import com.dotcms.browser.ContentStatus;
import com.dotcms.browser.FieldSearchCriteria;
import com.dotcms.rest.exception.BadRequestException;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final ContentDriveFieldFilterResolver fieldFilterResolver =
            new ContentDriveFieldFilterResolver();

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
     * Drive search functionality for content browsing with advanced filtering. This endpoint is
     * intended to be used to feed content-drive functionality. It behaves similarly to the
     * {@link com.dotcms.rest.api.v1.browser.BrowserResource} but with enhanced capabilities:
     * <ul>
     *     <li>Can take a site/folder path expressed in the form of
     *     {@code //site/folder/subfolder/}.</li>
     *     <li>Supports multiple languages and content-types for more flexible filtering.</li>
     *     <li>Uses Elasticsearch for text filtering while maintaining database reliability.</li>
     * </ul>
     *
     * @param requestForm The {@link DriveRequestForm} as the JSON body request with search
     *                    parameters
     * @param user        Current logged in {@link User}.
     *
     * @return a Map with all requested content and metadata
     *
     * @throws DotDataException     any data-related exception
     * @throws DotSecurityException any security violation exception
     */
    public PaginatedContents driveSearch(final DriveRequestForm requestForm, final User user)
            throws DotDataException, DotSecurityException {

        final List<Long> langIds = requestForm.language().stream()
                .map(LanguageUtil::getLanguageId)
                .collect(Collectors.toList());

        List<BaseContentType> baseContentTypes = BaseContentType.allBaseTypes();
        if (UtilMethods.isSet(requestForm.mimeTypes())) {
            baseContentTypes = List.of(BaseContentType.FILEASSET, BaseContentType.DOTASSET);
        } else if (null != requestForm.baseTypes()) {
            baseContentTypes = requestForm.baseTypes().stream()
                    .map(s -> BaseContentType.getBaseContentType(s.toUpperCase()))
                    .collect(Collectors.toList());
        }

        final ContentTypeAPI myContentTypeAPI = APILocator.getContentTypeAPI(user);
        List<ContentType> contentTypes = List.of();
        if (null != requestForm.contentTypes()) {
            contentTypes = requestForm.contentTypes().stream()
                    .map(inodeOrVar -> Try.of(() -> myContentTypeAPI.find(inodeOrVar)).getOrNull())
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
        final boolean showFolders = requestForm.showFolders();
        // A Link carries no file MIME type, and the paginated path does not run the mimeType
        // filter, so links would otherwise leak through a mimeType-narrowed search unfiltered.
        final boolean showLinks = requestForm.showLinks()
                && !UtilMethods.isSet(requestForm.mimeTypes());
        if (null != requestForm.mimeTypes()){
            builder.showMimeTypes(requestForm.mimeTypes());
        }
        final String sortBy = sortBy(requestForm.sortBy());
        final boolean sortDesc = sortDesc(requestForm.sortBy());
        builder.withUser(user)
            .respectFrontEndRoles(false)
            .contentCursor(requestForm.contentCursor())
            .folderCursor(requestForm.folderCursor())
            .linkCursor(requestForm.linkCursor())
            //These are not always present
            .withContentTypes(
                contentTypes.stream().map(ContentType::id).collect(Collectors.toSet())
            )
            //However, these are
            .excludedContentTypes(
                excludedContentTypes.stream().map(ContentType::id).collect(Collectors.toSet())
            )
            .withBaseTypes(List.copyOf(types))
            .showDotAssets(showDotAssets)
            .showFiles(showFiles)
            .showImages(showFiles)
            .showArchived(showArchived)
            .showWorking(!live)
            .showFolders(showFolders)
            .showLinks(showLinks)
            .showContent(!baseContentTypes.isEmpty())
            .withLanguageIds(langIds)
            .offset(requestForm.offset())
            .maxResults(requestForm.maxResults())
            .sortBy(sortBy)
            .sortByDesc(sortDesc);

        // Determine if we're requesting from a specific folder or host root
        if (folder.isSystemFolder()) {
            builder.withHostOrFolderId(host.getIdentifier())
                 /// if we're setting a site-name directly, we care fore all subfolders
                 /// Therefore, we should skip setting a folder path
                .skipFolder(true);
        } else {
            builder.withHostOrFolderId(folder.getInode())
                // When a specific folder is selected, enable ignoreSiteForFolders to allow
                // folder selection without being limited by site filtering
                .ignoreSiteForFolders(true);
        }
        //This ensures that despite the site passed systemHost will be included too
        builder.forceSystemHost(requestForm.includeSystemHost());

        // Enable Elasticsearch filtering for text search when filter is provided
        if (null != requestForm.filters() && UtilMethods.isSet(requestForm.filters().text())) {
             builder.useElasticsearchFiltering(true) // Rely on ES for enhanced text filtering
                 .filterFolderNames(requestForm.filters().filterFolders())
                 .withFilter(requestForm.filters().text());
        }

        // Per-field value filters (Content Drive). Field types are resolved against a single
        // content type; index-routed criteria also flip on ES filtering, while DB-routed criteria
        // (Tag) are resolved in the DB path.
        if (UtilMethods.isSet(requestForm.userSearchable())) {
            if (contentTypes.size() != 1) {
                throw new BadRequestException(
                        "Exactly one content type is required when using 'userSearchable' field filters.");
            }
            final List<FieldSearchCriteria> fieldCriteria =
                    fieldFilterResolver.parse(requestForm.userSearchable(), contentTypes.get(0));
            // Field filters are resolved against a single content type; links have no fields, so
            // they could never satisfy one — drop them as folders already are elsewhere.
            builder.withFieldCriteria(fieldCriteria).showLinks(false);
            final boolean hasIndexCriteria = fieldCriteria.stream()
                    .anyMatch(criteria ->
                            criteria.getBucket() == FieldSearchCriteria.RoutingBucket.INDEX);
            if (hasIndexCriteria) {
                builder.useElasticsearchFiltering(true);
            }
        }

        // Workflow filter — split entries into scheme-only (match by content-type assignment,
        // so never-actioned content still appears) and step-pinned (match the current task).
        if (UtilMethods.isSet(requestForm.workflow())) {
            final Set<String> workflowSchemeIds = requestForm.workflow().stream()
                    .filter(entry -> !UtilMethods.isSet(entry.step()))
                    .map(WorkflowFilterForm::scheme)
                    .filter(UtilMethods::isSet)
                    .collect(Collectors.toSet());
            final Set<String> workflowStepIds = requestForm.workflow().stream()
                    .filter(entry -> UtilMethods.isSet(entry.step()))
                    .map(WorkflowFilterForm::step)
                    .collect(Collectors.toSet());

            if (!workflowSchemeIds.isEmpty() || !workflowStepIds.isEmpty()) {
                builder.withWorkflowSchemeIds(workflowSchemeIds)
                        .withWorkflowStepIds(workflowStepIds)
                        // Folders and links carry no workflow state — drop them when filtering
                        // by workflow.
                        .showFolders(false)
                        .showLinks(false);
            }
        }

        // Status filter — the selected states are OR'd together server-side (see
        // BrowserAPIImpl#appendContentStatusQuery). Empty means no status filtering at all, which
        // is the path every pre-existing caller takes.
        final Set<ContentStatus> contentStatuses = parseStatuses(requestForm.status());
        if (!contentStatuses.isEmpty()) {
            builder.withContentStatuses(contentStatuses)
                    // Folders carry no status — drop them when filtering by status.
                    .showFolders(false);
        }

        // Build once and log the query itself: flags such as showLinks and showFolders can be
        // overridden by the workflow, status and userSearchable branches above, so logging the
        // locals would misreport what actually ran. BrowserQuery.toString() carries the effective
        // flags, all three cursors and the filters.
        //
        // ⚠ EVERY builder mutation MUST go ABOVE this line. build() snapshots the builder, so a
        // `builder.withX(...)` placed after it is silently discarded — the request succeeds and the
        // filter simply does nothing. That is exactly how the status filter shipped broken once:
        // the block was anchored on the Logger.debug below, and a refactor that hoisted build()
        // above the log moved it to the wrong side without any compile or test failure.
        final BrowserQuery browserQuery = builder.build();

        Logger.debug(this, () -> String.format("Content drive search - User: %s, Path: %s, %s",
                user.getUserId(), assetPath, browserQuery));

        return browserAPI.getPaginatedContents(browserQuery);
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
     * Parses the raw {@code status} values of a drive-search request into {@link ContentStatus}.
     * <p>
     * Declared on the form as strings rather than the enum so this rejection stays an explicit
     * {@link BadRequestException} — the deterministic 400 the contract asks for — rather than a
     * Jackson deserialization failure whose status mapping is not under our control. Mirrors how
     * {@code userSearchable} rejects unknown keys.
     * <p>
     * Matching is case-insensitive and duplicates collapse (the result is a Set), so a repeated
     * value cannot widen the generated OR group. An unrecognized or blank value is rejected rather
     * than skipped: silently dropping it would return a <b>wider</b> result set than the caller
     * asked for, which is worse than failing.
     *
     * @param statuses raw status values from the request; may be empty, never null
     * @return the parsed statuses; empty means no status filtering
     * @throws BadRequestException if any value does not name a {@link ContentStatus}
     */
    static Set<ContentStatus> parseStatuses(final List<String> statuses) {
        if (!UtilMethods.isSet(statuses)) {
            return Set.of();
        }

        final Set<ContentStatus> parsed = new LinkedHashSet<>();
        for (final String status : statuses) {
            if (!UtilMethods.isSet(status)) {
                throw invalidStatus(status);
            }
            try {
                parsed.add(ContentStatus.valueOf(status.trim().toUpperCase()));
            } catch (final IllegalArgumentException e) {
                throw invalidStatus(status);
            }
        }

        return parsed;
    }

    /**
     * Builds the 400 for an unusable status value, naming both the offending value and every
     * accepted one.
     * <p>
     * The offending value is passed as a format <b>argument</b>, never concatenated into the format
     * string: {@code HttpStatusCodeException} runs {@link String#format} over the message it is
     * given, so a pre-formatted string carrying user input would blow up on a stray {@code %} — a
     * request for {@code "50%"} would raise UnknownFormatConversionException and surface as a 500
     * instead of the 400 this method exists to produce.
     */
    private static BadRequestException invalidStatus(final String status) {
        return new BadRequestException("Invalid status '%s'. Accepted values are: %s.",
                String.valueOf(status),
                Arrays.stream(ContentStatus.values()).map(Enum::name)
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Creates a new instance of ContentDriveHelper
     * @return ContentDriveHelper instance
     */
    public static ContentDriveHelper newInstance() {
        return new ContentDriveHelper();
    }
}