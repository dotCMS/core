package com.dotcms.rest.api.v1.drive;

import com.dotmarketing.business.APILocator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Request form for Content Drive search operations.
 * <p>
 * This immutable form defines the parameters for drive-like content browsing and search functionality.
 * It supports filtering by content types, languages, mime types, and provides pagination and sorting capabilities.
 * The form is designed to work with the BrowserAPI to provide a unified interface for browsing both folders and contentlets.
 * </p>
 *
 * <h3>Core Features:</h3>
 * <ul>
 *   <li><strong>Path-based Navigation:</strong> Navigate through site/folder structures using assetPath</li>
 *   <li><strong>Content Filtering:</strong> Filter by content types, base types, and mime types</li>
 *   <li><strong>Text Search:</strong> Full-text search capabilities via QueryFilters</li>
 *   <li><strong>Pagination:</strong> Configurable offset and maxResults for large datasets</li>
 *   <li><strong>Sorting:</strong> Flexible sorting with field:direction syntax (e.g., "title:asc", "modDate:desc")</li>
 *   <li><strong>Multi-language Support:</strong> Query across multiple languages</li>
 *   <li><strong>Content State Filtering:</strong> Filter by live/working and archived states</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Basic folder browsing
 * DriveRequestForm.builder()
 *     .assetPath("//demo.dotcms.com/documents/")
 *     .build();
 *
 * // Search with filtering and pagination
 * DriveRequestForm.builder()
 *     .assetPath("//demo.dotcms.com/")
 *     .filters(QueryFilters.builder().text("product review").build())
 *     .contentTypes(List.of("Blog", "News"))
 *     .sortBy("title:asc")
 *     .offset(0)
 *     .maxResults(20)
 *     .build();
 *
 * // File-specific browsing
 * DriveRequestForm.builder()
 *     .assetPath("//demo.dotcms.com/images/")
 *     .baseTypes(List.of("FILEASSET"))
 *     .mimeTypes(List.of("image/jpeg", "image/png"))
 *     .build();
 * }</pre>
 *
 * <h3>Sort Field Format:</h3>
 * <p>The sortBy field supports the format: <code>fieldName:direction</code></p>
 * <ul>
 *   <li><code>"title:asc"</code> - Sort by title ascending</li>
 *   <li><code>"modDate:desc"</code> - Sort by modification date descending</li>
 *   <li><code>"name"</code> - Sort by name (defaults to ascending)</li>
 *   <li><code>"modDate"</code> - Default sort field when not specified</li>
 * </ul>
 *
 * @see ContentDriveResource
 * @see ContentDriveHelper
 * @see QueryFilters
 * @author dotCMS
 * @since 24.12
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = DriveRequestForm.class)
@JsonDeserialize(as = DriveRequestForm.class)
public interface AbstractDriveRequestForm {

    /**
     * Default sort field used when no sortBy is specified.
     * Sorts by modification date in ascending order.
     */
    String SORT_BY = "modDate";

    /**
     * The path to the asset/folder to browse.
     * <p>
     * Supports dotCMS path format: <code>//sitename/folder/subfolder/</code>
     * </p>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li><code>"//demo.dotcms.com/"</code> - Browse site root</li>
     *   <li><code>"//demo.dotcms.com/documents/"</code> - Browse documents folder</li>
     *   <li><code>"//demo.dotcms.com/images/products/"</code> - Browse nested folder</li>
     * </ul>
     *
     * @return the asset path to browse, never null
     */
    @JsonProperty("assetPath")
    String assetPath();

    /**
     * Whether to include system host content in the results.
     * <p>
     * When true, content from the system host will be included alongside
     * content from the specified site. This is useful for accessing global
     * assets and templates that are available across all sites.
     * </p>
     *
     * @return true to include system host content (default), false to exclude
     */
    @JsonProperty("includeSystemHost")
    @Value.Default
    default boolean includeSystemHost(){return true;}

    /**
     * List of language identifiers to include in the search.
     * <p>
     * Supports both language codes (e.g., "en", "es") and language IDs.
     * When not specified, defaults to the system's default language.
     * Multiple languages can be specified to search across localized content.
     * </p>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li><code>["en", "es"]</code> - Search English and Spanish content</li>
     *   <li><code>["1", "2"]</code> - Search by language IDs</li>
     * </ul>
     *
     * @return list of language identifiers, defaults to system default language
     */
    @JsonProperty("language")
    @Value.Default
    default List<String> language() { return List.of(APILocator.getLanguageAPI().getDefaultLanguage().toString());  }

    /**
     * List of specific content type identifiers or variable names to include.
     * <p>
     * When specified, only contentlets of these content types will be returned.
     * Can use either content type IDs or variable names (e.g., "Blog", "News", "webPageContent").
     * If not specified, all content types are included (subject to baseTypes filtering).
     * </p>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li><code>["Blog", "News"]</code> - Only blog and news content</li>
     *   <li><code>["webPageContent"]</code> - Only web page content</li>
     *   <li><code>["fileAsset"]</code> - Only file assets</li>
     * </ul>
     *
     * @return list of content type identifiers, null means no filtering by specific content types
     */
    @Nullable
    @JsonProperty("contentTypes")
    List<String> contentTypes();

    /**
     * List of base content types to include in the search.
     * <p>
     * Base types define the fundamental categories of content in dotCMS.
     * When specified, only content matching these base types will be returned.
     * This provides a broader filter than specific content types.
     * </p>
     *
     * <h4>Available Base Types:</h4>
     * <ul>
     *   <li><code>CONTENT</code> - Regular contentlets</li>
     *   <li><code>FILEASSET</code> - File assets (images, documents, etc.)</li>
     *   <li><code>DOTASSET</code> - dotAssets (binary content)</li>
     *   <li><code>HTMLPAGE</code> - HTML pages</li>
     *   <li><code>PERSONA</code> - Persona content</li>
     *   <li><code>FORM</code> - Form content</li>
     * </ul>
     *
     * @return list of base content types, null means no filtering by base type
     */
    @Nullable
    @JsonProperty("baseTypes")
    List<String> baseTypes();

    /**
     * List of MIME types to filter file assets.
     * <p>
     * When specified, only file assets with matching MIME types will be returned.
     * This is particularly useful when browsing file assets and you want to
     * limit results to specific file types (images, documents, etc.).
     * </p>
     *
     * <h4>Common Examples:</h4>
     * <ul>
     *   <li><code>["image/jpeg", "image/png", "image/gif"]</code> - Image files</li>
     *   <li><code>["application/pdf"]</code> - PDF documents</li>
     *   <li><code>["video/mp4", "video/avi"]</code> - Video files</li>
     *   <li><code>["text/plain", "text/csv"]</code> - Text files</li>
     * </ul>
     *
     * @return list of MIME types to include, null means no MIME type filtering
     */
    @Nullable
    @JsonProperty("mimeTypes")
    List<String> mimeTypes();

    /**
     * Search filters for text-based content filtering.
     * <p>
     * Provides Elasticsearch-powered text search capabilities across content fields.
     * When specified, enables full-text search across contentlet titles, bodies,
     * and other searchable fields. The search uses Elasticsearch for enhanced
     * performance and relevance scoring.
     * </p>
     *
     * <h4>Search Features:</h4>
     * <ul>
     *   <li>Full-text search across multiple content fields</li>
     *   <li>Partial word matching and phrase search</li>
     *   <li>Elasticsearch-powered relevance scoring</li>
     *   <li>Case-insensitive search</li>
     * </ul>
     *
     * @return query filters for text search, null means no text filtering
     * @see QueryFilters
     */
    @Nullable
    @JsonProperty("filters")
    QueryFilters filters();

    /**
     * Number of results to skip for pagination.
     * <p>
     * Used in combination with maxResults to implement pagination.
     * For example, to get the second page with 20 items per page,
     * set offset=20 and maxResults=20.
     * </p>
     *
     * <h4>Pagination Examples:</h4>
     * <ul>
     *   <li>Page 1: offset=0, maxResults=20</li>
     *   <li>Page 2: offset=20, maxResults=20</li>
     *   <li>Page 3: offset=40, maxResults=20</li>
     * </ul>
     *
     * @return number of results to skip, defaults to 0
     */
    @JsonProperty("offset")
    @Value.Default
    default int offset(){ return 0; }

    /**
     * Maximum number of results to return.
     * <p>
     * Controls the page size for pagination. The actual number of results
     * may be less than this value if there are fewer matching items available.
     * Combined with offset to implement efficient pagination for large datasets.
     * </p>
     *
     * <h4>Performance Notes:</h4>
     * <ul>
     *   <li>Higher values may impact response time</li>
     *   <li>Consider using smaller page sizes (10-50) for better user experience</li>
     *   <li>Default of 500 provides good balance for most use cases</li>
     * </ul>
     *
     * @return maximum number of results to return, defaults to 2000
     */
    @JsonProperty("maxResults")
    @Value.Default
    default int maxResults() { return 2000; }

    /**
     * Field and direction for sorting results.
     * <p>
     * Supports the format: <code>fieldName:direction</code> where direction
     * can be "asc" or "desc". If no direction is specified, ascending is assumed.
     * The ContentDriveHelper parses this field to extract both the field name
     * and sort direction for the BrowserQuery.
     * </p>
     *
     * <h4>Supported Fields:</h4>
     * <ul>
     *   <li><code>title</code> - Content title/name</li>
     *   <li><code>modDate</code> - Modification date (default)</li>
     *   <li><code>modUser</code> - Modifying user</li>
     *   <li><code>sortOrder</code> - Manual sort order</li>
     *   <li><code>name</code> - Asset/folder name</li>
     * </ul>
     *
     * <h4>Format Examples:</h4>
     * <ul>
     *   <li><code>"title:asc"</code> - Sort by title ascending</li>
     *   <li><code>"modDate:desc"</code> - Sort by modification date descending</li>
     *   <li><code>"name"</code> - Sort by name ascending (no direction specified)</li>
     * </ul>
     *
     * @return sort field with optional direction, defaults to "modDate"
     */
    @JsonProperty("sortBy")
    @Value.Default
    default String sortBy() { return SORT_BY; }

    /**
     * Whether to include only live (published) content.
     * <p>
     * When true, only live/published versions of contentlets are returned.
     * When false (default), working versions are included, which is useful
     * for content management scenarios where editors need to see draft content.
     * </p>
     *
     * <h4>Content States:</h4>
     * <ul>
     *   <li><strong>Live:</strong> Published content visible to end users</li>
     *   <li><strong>Working:</strong> Draft/unpublished content in progress</li>
     * </ul>
     *
     * @return true to include only live content, false to include working content (default)
     */
    @JsonProperty("live")
    @Value.Default
    default boolean live() { return false; }

    /**
     * Whether to include archived content in results.
     * <p>
     * When true, archived/deleted content will be included in the results.
     * When false (default), archived content is excluded. This is typically
     * used in administrative interfaces where users need to access deleted content
     * for recovery or audit purposes.
     * </p>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Content recovery operations</li>
     *   <li>Audit trails and history tracking</li>
     *   <li>Administrative cleanup tasks</li>
     * </ul>
     *
     * @return true to include archived content, false to exclude (default)
     */
    @JsonProperty("archived")
    @Value.Default
    default boolean archived() { return false; }


    @JsonProperty("showFolders")
    @Value.Default
    default boolean showFolders(){return true; }
}
