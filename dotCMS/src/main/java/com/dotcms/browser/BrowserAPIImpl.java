package com.dotcms.browser;

import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.INCLUDE_DOTRAW_METADATA_FIELDS;
import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.WRITE_METADATA_ON_REINDEX;
import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.getDotRawMetadataFields;
import static com.dotcms.content.elasticsearch.business.ESMappingAPIImpl.isWriteMetadataOnReindex;
import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.comparators.GenericMapFieldComparator;
import com.dotmarketing.comparators.WebAssetMapComparator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotFolderTransformerBuilder;
import com.dotmarketing.portlets.contentlet.transform.DotMapViewTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation for the {@link BrowserAPI} class.
 *
 * @author Jonathan Sanchez
 * @since Apr 28th, 2020
 */
public class BrowserAPIImpl implements BrowserAPI {

    private final UserWebAPI userAPI = WebAPILocator.getUserWebAPI();
    private final FolderAPI folderAPI = APILocator.getFolderAPI();
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
    private static final StringBuilder POSTGRES_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("-> 'fields' -> ").append("'fileName' ->> 'value' ");
    private static final StringBuilder POSTGRES_BINARY_ASSETNAME_COLUMN = new StringBuilder(ContentletJsonAPI
            .CONTENTLET_AS_JSON).append("-> 'fields' -> ").append("'asset' ->> 'value' ");

    private static final StringBuilder MSSQL_ASSETNAME_COLUMN = new StringBuilder("JSON_VALUE(c.").append
            (ContentletJsonAPI.CONTENTLET_AS_JSON).append(", '$.fields.").append("fileName.").append("value')" +
            " ");

    private static final StringBuilder ASSET_NAME_LIKE = new StringBuilder().append("LOWER(%s) LIKE ? ");

    private static final StringBuilder ASSET_NAME_EQ = new StringBuilder().append("LOWER(%s) = ? ");

    private static final String ES_QUERY_TEMPLATE =
            "{\n" +
                    "    \"query\": {\n" +
                    "        \"query_string\": {\n" +
                    "            \"query\": \"%s\"\n" +
                    "        }\n" +
                    "    }\n" +
            "}";


    /**
     * Returns a collection of contentlets based on specific filtering criteria specified via the
     * {@link BrowserQuery} class, such as: Parent folder, Site, archived/non-archived status, base Content Types,
     * language, among many others. After that, the resulting list is filtered based on {@code READ} permissions.
     *
     * @param browserQuery The {@link BrowserQuery} object specifying the filtering criteria.
     *
     * @return The list of filtered contentlets.
     */
    @Override
    public List<Contentlet> getContentUnderParentFromDB(final BrowserQuery browserQuery) {
        return getContentUnderParentFromDB(browserQuery, -1, -1).contentlets;
    }

    /**
     * Returns a collection of contentlets based on specific filtering criteria specified via the
     * {@link BrowserQuery} class, such as: Parent folder, Site, archived/non-archived status, base Content Types,
     * language, among many others. After that, the resulting list is filtered based on {@code READ} permissions.
     * This version of the method applies database pagination through a startRow and maxRow param
     * @param browserQuery The {@link BrowserQuery} object specifying the filtering criteria.
     * @param startRow
     * @param maxRows
     * @return The list of filtered contentlets.
     */
    @CloseDBIfOpened
    ContentUnderParent getContentUnderParentFromDB(final BrowserQuery browserQuery, final int startRow, final int maxRows) {

        final SelectAndCountQueries sqlQuery = this.selectAndCountQueries(browserQuery);
        final DotConnect dcCount = new DotConnect().setSQL(sqlQuery.countQuery);
        sqlQuery.params.forEach(dcCount::addParam);
        final int count = dcCount.getInt("count");

        final boolean useElasticSearchForTextFiltering = isUseElasticSearchForTextFiltering(browserQuery);
        try {
            final Set<String> collectedInodes = new LinkedHashSet<>();
            if(useElasticSearchForTextFiltering){
               //If set to "ON" we use ES to filter when text is passed
               collectedInodes.addAll(doElasticSearchTextFiltering(browserQuery, startRow, maxRows, sqlQuery));
            } else {
                final DotConnect dcSelect = new DotConnect().setSQL(sqlQuery.selectQuery);
                sqlQuery.params.forEach(dcSelect::addParam);

                //Set Pagination params only if they make sense, this also allows me to keep the original behavior available
                if(startRow >= 0 && maxRows > 0) {
                    dcSelect.setStartRow(startRow)
                            .setMaxRows(maxRows);
                }
                @SuppressWarnings("unchecked")
                final List<Map<String, String>> inodesMapList = dcSelect.loadResults();
                inodesMapList.forEach(inode -> collectedInodes.add(inode.get("inode")));
            }

            //Now this should load the good contentlets using parallel processing
            final List<Contentlet> contentlets = findContentletsInParallel(collectedInodes);

            final List<Contentlet> filtered = permissionAPI.filterCollection(contentlets,
                    PERMISSION_READ, true, browserQuery.user);
            return new ContentUnderParent(filtered, count);
        } catch (final Exception e) {
            final String folderPath = UtilMethods.isSet(browserQuery.folder) ? browserQuery.folder.getPath() : "N/A";
            final String siteName = UtilMethods.isSet(browserQuery.site) ? browserQuery.site.getHostname() : "N/A";
            final String errorMsg = String.format("Failed to load contents from folder '%s' in Site '%s': %s",
                    folderPath, siteName, e.getMessage());
            Logger.warnAndDebug(this.getClass(), errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Enum defining different heuristic strategies for hybrid database/Elasticsearch search
     */
    public enum SearchHeuristicType {
        // Single query chunked - fetches all inodes in one DB query, then processes in ES chunks
        HYBRID_SINGLE_CHUNKED_QUERY_ES,
        // Pure ES - uses Elasticsearch exclusively without any database queries */
        PURE_ES
    }

    private Set<String> doElasticSearchTextFiltering(BrowserQuery browserQuery, int startRow, int maxRows,
            SelectAndCountQueries sqlQuery) throws DotDataException {

        // Get the heuristic strategy from lazy configuration
        final SearchHeuristicType heuristicType = HEURISTIC_TYPE.get();

        // Track execution time for heuristic performance analysis using modern time APIs
        final long startTimeNanos = System.nanoTime();
        try {
            switch (heuristicType) {
                case HYBRID_SINGLE_CHUNKED_QUERY_ES:
                    return doHybridSingleChunkedQueryES(browserQuery, startRow, maxRows, sqlQuery);
                case PURE_ES:
                default:
                    return doPureESQuery(browserQuery, startRow, maxRows);
            }
        } finally {
            final boolean debugEnabled = Logger.isDebugEnabled(BrowserAPIImpl.class);
            if(debugEnabled) {
                final long endTimeNanos = System.nanoTime();
                final long executionTimeNanos = endTimeNanos - startTimeNanos;

                // Convert to milliseconds and seconds for performance analysis
                final long executionTimeMillis = TimeUnit.NANOSECONDS.toMillis(executionTimeNanos);
                final double executionTimeSeconds = executionTimeNanos / 1_000_000_000.0;

                // Log with seconds and milliseconds only
                if (executionTimeMillis > 1000) {
                    // For longer operations, show seconds with high precision
                    Logger.debug(this, String.format(
                            "===== Heuristic %s execution completed in %.3f seconds (%d ms) =====",
                            heuristicType.name(), executionTimeSeconds, executionTimeMillis));
                } else {
                    // For shorter operations, show milliseconds
                    Logger.debug(this, String.format(
                            "===== Heuristic %s execution completed in %d ms =====",
                            heuristicType.name(), executionTimeMillis));
                }
            }
        }
    }

    /**
     * Single Query Chunked: Fetches all inodes in a single database query without pagination,
     * then processes them in optimally-sized ES chunks based on total count percentage.
     */
    Set<String> doHybridSingleChunkedQueryES(BrowserQuery browserQuery, int startRow, int maxRows,
            SelectAndCountQueries sqlQuery) throws DotDataException {
        final Set<String> collectedInodes = new LinkedHashSet<>();

        Logger.debug(this, "::::: Using Single Query Chunked for text filtering ::::");

        // Execute single DB query to get ALL candidate inodes without pagination
        final DotConnect dcSelect = new DotConnect().setSQL(sqlQuery.selectQuery);
        sqlQuery.params.forEach(dcSelect::addParam);

        @SuppressWarnings("unchecked")
        final List<Map<String, String>> inodesMapList = dcSelect.loadResults();
        final List<String> allCandidateInodes = inodesMapList.stream()
                .map(data -> data.get("inode"))
                .collect(Collectors.toList());

        if (allCandidateInodes.isEmpty()) {
            Logger.debug(this, "Single Query Chunked: No candidate inodes found");
            return collectedInodes;
        }

        final int totalCandidates = allCandidateInodes.size();

        // Calculate the optimal ES chunk size based on total count
        final int esChunkSize = calculateESChunkSizeFromTotalCount(totalCandidates);

        // Process chunks in parallel for better throughput
        final LinkedList<String> list = new LinkedList<>(
                parallelChunksInES(browserQuery, allCandidateInodes, totalCandidates, esChunkSize));

        // Apply safe slicing with startRow and maxRows
        final int listSize = list.size();
        final int safeStartRow = Math.max(0, Math.min(startRow, listSize));
        final int safeEndRow = Math.min(listSize, safeStartRow + Math.max(0, maxRows));

        // Create a LinkedHashSet from the sliced sublist to preserve order
        return new LinkedHashSet<>(list.subList(safeStartRow, safeEndRow));
    }

    /**
     * Processes chunks of candidate inodes in parallel for improved performance.
     * Each chunk is processed directly through ES without further internal partitioning.
     *
     * @param browserQuery The browser query containing search criteria
     * @param allCandidateInodes All candidate inodes to process
     * @param totalCandidates Total number of candidates
     * @param esChunkSize Size of each chunk for ES processing
     * @return Set of filtered inodes that match the search criteria
     */
    private Set<String> parallelChunksInES(BrowserQuery browserQuery, List<String> allCandidateInodes,
                                               int totalCandidates, int esChunkSize) {
        final Set<String> collectedInodes = Collections.synchronizedSet(new LinkedHashSet<>());
        final long startTime = System.currentTimeMillis();

        // Create chunks for parallel processing
        final List<List<String>> chunks = Lists.partition(allCandidateInodes, esChunkSize);

        final int actualChunks = chunks.size();
        Logger.debug(this, String.format("Processing %d chunks in parallel", actualChunks));

        // Process chunks in parallel using CompletableFuture
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final CompletableFuture[] futures = new CompletableFuture[actualChunks];

        for (int i = 0; i < actualChunks; i++) {
            final List<String> chunk = chunks.get(i);
            final int chunkIndex = i + 1;
            futures[i] = CompletableFuture
                .supplyAsync(() -> {
                    // Process chunk directly without internal partitioning
                    final Set<String> chunkMatches = processESDirectly(browserQuery, new LinkedHashSet<>(chunk));
                    Logger.debug(BrowserAPIImpl.this, String.format("Processed chunk %d/%d: %d inodes, found %d matches.",
                            chunkIndex, actualChunks, chunk.size(), chunkMatches.size()));
                    return chunkMatches;
                }, submitter)
                .orTimeout(60, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    Logger.error(BrowserAPIImpl.this, String.format("Chunk %d failed: %s", chunkIndex, throwable.getMessage()), throwable);
                    return new LinkedHashSet<>();
                });
        }

        // Wait for all chunks to complete and collect results
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
            allFutures.get(120, TimeUnit.SECONDS); // Global timeout
            for (CompletableFuture<Set<String>> future : futures) {
                try {
                    Set<String> chunkMatches = future.get();
                    collectedInodes.addAll(chunkMatches);
                } catch (Exception e) {
                    Logger.warn(this, "Failed to get result from chunk future: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            final long totalDuration = System.currentTimeMillis() - startTime;
            Logger.debug(this, String.format(
                "Single Query Chunked parallel processing completed: %d candidates in %d chunks → %d total matches in %d ms",
                totalCandidates, actualChunks, collectedInodes.size(), totalDuration));

        } catch (InterruptedException e) {
            Logger.error(this, "Parallel chunk processing interrupted: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Logger.error(this, "Parallel chunk processing execution error: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            Logger.error(this, "Parallel chunk processing timed out: " + e.getMessage(), e);
        }

        return collectedInodes;
    }


    /**
     * Pure ES: Uses Elasticsearch exclusively without any database queries.
     * Constructs a comprehensive ES query from browserQuery and uses the appropriate search API
     * to return contentlets directly, bypassing all database operations.
     */
    private Set<String> doPureESQuery(BrowserQuery browserQuery, int startRow, int maxRows) throws DotDataException {
        final Set<String> collectedInodes = new LinkedHashSet<>();

        Logger.debug(this, "::::: Using Pure ES for text filtering (no database queries) ::::");

        try {
            // Build comprehensive ES query without inode restrictions
            final String esQuery = buildPureESQuery(browserQuery);

            Logger.debug(this, String.format("::: Pure ES query: %s", esQuery));

            // Use ContentletAPI to search directly in ES
            final com.dotmarketing.portlets.contentlet.business.ContentletAPI contentletAPI = APILocator.getContentletAPI();

            // Execute the search using ContentletAPI with proper parameters
            final List<Contentlet> contentlets = contentletAPI.search(
                esQuery,
                browserQuery.maxResults > 0 ? browserQuery.maxResults : maxRows,
                browserQuery.offset >= 0 ? browserQuery.offset : startRow,
                browserQuery.sortBy,
                browserQuery.user,
                false // respectFrontendRoles - use false for backend searches
            );

            // Extract inodes from the results
            contentlets.forEach(contentlet -> collectedInodes.add(contentlet.getInode()));

            Logger.debug(this, String.format("Pure ES completed: found %d contentlets, collected %d inodes",
                contentlets.size(), collectedInodes.size()));

        } catch (final Exception e) {
            Logger.error(this, "Error in Pure ES search: " + e.getMessage(), e);
            throw new DotDataException("Pure ES search failed: " + e.getMessage(), e);
        }

        return collectedInodes;
    }

    /**
     * Builds a comprehensive Elasticsearch query for Pure ES search without inode filtering.
     * Constructs all necessary filters including system filters, content type filters,
     * host/folder filters, language filters, and text search filters.
     *
     * @param browserQuery The browser query parameters
     * @return Complete Elasticsearch query string ready for direct ES execution
     */
    private String buildPureESQuery(final BrowserQuery browserQuery) {
        final StringBuilder query = new StringBuilder();

        // Essential system filters (always required)
        query.append("+systemType:false ");
        query.append("-contentType:forms ");
        query.append("-contentType:Host ");
        query.append("+deleted:false ");

        // Working/live content filter
        if (browserQuery.showWorking) {
            query.append("+working:true ");
        } else {
            query.append("+live:true ");
        }

        // Variant filter (always default unless specified)
        query.append("+variant:default ");

        // Host/folder filter - always include system host option
        String hostId = browserQuery.folder.isSystemFolder()
            ? browserQuery.site.getIdentifier()
            : browserQuery.folder.getHostId();

        if (browserQuery.forceSystemHost || browserQuery.folder.isSystemFolder()) {
            query.append("+(conhost:").append(hostId).append(" OR conhost:SYSTEM_HOST) ");
        } else {
            query.append("+conhost:").append(hostId).append(" ");
        }

        // Content type filters - include specific types if provided
        if (UtilMethods.isSet(browserQuery.contentTypeIds) && !browserQuery.contentTypeIds.isEmpty()) {
            query.append("+contentType:(")
                 .append(String.join(" OR ", browserQuery.contentTypeIds))
                 .append(") ");
        }

        // Excluded content types
        if (UtilMethods.isSet(browserQuery.excludedContentTypeIds) && !browserQuery.excludedContentTypeIds.isEmpty()) {
            for (String excludedType : browserQuery.excludedContentTypeIds) {
                query.append("-contentType:").append(excludedType).append(" ");
            }
        }

        // Language filter
        if (UtilMethods.isSet(browserQuery.languageIds) && !browserQuery.languageIds.isEmpty()) {
            query.append("+languageId:(")
                 .append(browserQuery.languageIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(" OR ")))
                 .append(") ");
        }

        // Text search filter (the main search criteria)
        if (UtilMethods.isSet(browserQuery.filter)) {
            query.append("+(title:'*").append(browserQuery.filter)
                 .append("*'^5 OR catchall:*").append(browserQuery.filter)
                 .append("*^3 OR fileName:*").append(browserQuery.filter)
                 .append("*^2) ");
        }

        // Base type filters
        if (UtilMethods.isSet(browserQuery.baseTypes) && !browserQuery.baseTypes.isEmpty()) {
            boolean hasFileAsset = browserQuery.baseTypes.contains(BaseContentType.FILEASSET);
            boolean hasContent = browserQuery.baseTypes.contains(BaseContentType.CONTENT);
            boolean hasDotAsset = browserQuery.baseTypes.contains(BaseContentType.DOTASSET);

            if (hasFileAsset || hasContent || hasDotAsset) {
                query.append("+baseType:(");
                List<String> baseTypeStrings = new ArrayList<>();
                if (hasContent) baseTypeStrings.add("" + BaseContentType.CONTENT.getType());
                if (hasFileAsset) baseTypeStrings.add("" + BaseContentType.FILEASSET.getType());
                if (hasDotAsset) baseTypeStrings.add("" + BaseContentType.DOTASSET.getType());
                query.append(String.join(" OR ", baseTypeStrings));
                query.append(") ");
            }
        }

        // MIME type filter for file assets
        if (UtilMethods.isSet(browserQuery.mimeTypes) && !browserQuery.mimeTypes.isEmpty()) {
            query.append("+mimeType:(")
                 .append(String.join(" OR ", browserQuery.mimeTypes))
                 .append(") ");
        }

        String finalQuery = query.toString().trim();
        Logger.debug(this, String.format("Pure ES query built: %s", finalQuery));
        return finalQuery;
    }

    // Lazy initialization for heuristic type configuration
    private final Lazy<SearchHeuristicType> HEURISTIC_TYPE = Lazy.of(() -> {
        final String heuristicConfigValue = Config.getStringProperty("BROWSE_API_HEURISTIC_TYPE", "HYBRID_SINGLE_CHUNKED_QUERY_ES");
        try {
            return SearchHeuristicType.valueOf(heuristicConfigValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            Logger.warn(this.getClass(), "Invalid heuristic type: " + heuristicConfigValue + ", falling back to SINGLE_QUERY_CHUNKED");
            return SearchHeuristicType.HYBRID_SINGLE_CHUNKED_QUERY_ES;
        }
    });

    // ===========================================
    // CONFIGURATION PROPERTIES FOR ES CHUNK SIZING
    // ===========================================

    //Configuration for ES chunk percentage calculation
    final Lazy<Double> SINGLE_QUERY_ES_CHUNK_PERCENTAGE = Lazy.of(
            () -> {
                final String value = Config.getStringProperty("BROWSE_API_SINGLE_QUERY_ES_CHUNK_PERCENTAGE", "30.0");
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    Logger.warn(this.getClass(), "Invalid ES chunk percentage value: " + value + ", using default 30.0");
                    return 30.0;
                }
            });

    //Minimum ES chunk size to ensure reasonable performance
    final Lazy<Integer> SINGLE_QUERY_ES_CHUNK_MIN_SIZE = Lazy.of(
            () -> Config.getIntProperty("BROWSE_API_SINGLE_QUERY_ES_CHUNK_MIN_SIZE", 100));

    /**
     * Calculates optimal ES chunk size based on percentage of total count for processing inodes.
     * The chunk size is calculated as a percentage of the total count, with minimum and maximum limits
     * to ensure reasonable ES performance and memory usage.
     *
     * @param totalCount Total number of inodes to process
     * @return Calculated ES chunk size within configured bounds
     */
    private int calculateESChunkSizeFromTotalCount(int totalCount) {
        if (totalCount <= 0) {
            return SINGLE_QUERY_ES_CHUNK_MIN_SIZE.get();
        }

        // Calculate chunk size as percentage of total count
        final double percentage = SINGLE_QUERY_ES_CHUNK_PERCENTAGE.get() / 100.0;
        int calculatedSize = (int) Math.ceil(totalCount * percentage);

        // Calculate estimated number of ES chunks
        final int estimatedChunks = (int) Math.ceil((double) totalCount / calculatedSize);

        Logger.debug(this, String.format("ES chunk size calculation: total=%d, percentage=%.1f%%, calculated=%d, final=%d, estimated ES chunks=%d",
            totalCount, SINGLE_QUERY_ES_CHUNK_PERCENTAGE.get(), (int) Math.ceil(totalCount * percentage), calculatedSize, estimatedChunks));

        return calculatedSize;
    }

    /**
     * Represents content items under a specific parent along with the total count.
     * This class is immutable and holds a list of content items and their total results count.
     */
    static class ContentUnderParent {
        final List<Contentlet> contentlets;
        final int totalResults;

        ContentUnderParent(List<Contentlet> contentlets, int totalResults) {
            this.contentlets = contentlets;
            this.totalResults = totalResults;
        }
    }


    /**
     * Processes inodes directly through Elasticsearch with ES clause limit awareness.
     * This method is designed to be called from external chunking loops and handles
     * the ES boolean clause limit (1024) by subdividing large inode sets when necessary.
     *
     * @param browserQuery The {@link BrowserQuery} containing search criteria (filter, fileName)
     * @param inodes       The set of inodes to filter through Elasticsearch text search
     * @return A filtered set of inodes that match the text search criteria
     */
    Set<String> processESDirectly(BrowserQuery browserQuery, Set<String> inodes) {
        if (inodes == null || inodes.isEmpty()) {
            return new LinkedHashSet<>();
        }

        final int totalInodes = inodes.size();
        final long startTime = System.currentTimeMillis();

        // Calculate the maximum inodes we can handle in a single ES query
        // considering the ES boolean clause limit and other query conditions
        final int maxInodesPerESQuery = calculateMaxInodesPerESQuery(browserQuery);

        Logger.debug(this, String.format("Direct ES processing: %d inodes, max per query: %d",
            totalInodes, maxInodesPerESQuery));

        // If we're under the limit, process directly
        if (totalInodes <= maxInodesPerESQuery) {
            return processSingleESQuery(browserQuery, inodes, startTime);
        } else {
            // Split into multiple ES queries to respect the clause limit
            return processMultipleESQueries(browserQuery, inodes, maxInodesPerESQuery, startTime);
        }
    }

    /**
     * Calculates the maximum number of inodes we can include in a single ES query
     * while staying under the boolean clause limit (1024).
     */
    private int calculateMaxInodesPerESQuery(BrowserQuery browserQuery) {
        // ES has a limit of 1024 boolean clauses per query
        final int ES_MAX_BOOLEAN_CLAUSES = 1024;

        // Build base query to count how many clauses it uses
        final String baseQuery = buildBaseESQuery(browserQuery);

        // Count approximate clauses in base query (conservative estimate)
        int baseQueryClauses = countApproximateClausesInQuery(baseQuery);

        // Reserve some clauses for the base query (minimum 50, or actual count + buffer)
        int reservedClauses = Math.max(50, baseQueryClauses + 20);

        // Each inode adds one OR clause, so max inodes = remaining clauses
        int maxInodes = ES_MAX_BOOLEAN_CLAUSES - reservedClauses;

        // Safety margin - use 90% of the calculated limit
        maxInodes = (int) (maxInodes * 0.9);

        // Ensure we have at least 100 inodes minimum for efficiency
        maxInodes = Math.max(100, maxInodes);

        Logger.debug(this, String.format("ES clause calculation: base clauses≈%d, reserved=%d, max inodes=%d",
            baseQueryClauses, reservedClauses, maxInodes));

        return maxInodes;
    }

    /**
     * Counts approximate number of boolean clauses in a query string.
     * This is a heuristic estimation for safety.
     */
    private int countApproximateClausesInQuery(String query) {
        if (query == null || query.isEmpty()) {
            return 0;
        }

        // Count OR and AND operators as indicators of clauses
        int orCount = (query.split(" OR ", -1).length - 1);
        int andCount = (query.split(" AND ", -1).length - 1);
        int plusCount = (query.split("\\+", -1).length - 1);
        int minusCount = (query.split("\\-", -1).length - 1);

        // Each field:value pair is approximately one clause
        int colonCount = (query.split(":", -1).length - 1);

        // Conservative estimate: use the highest count as base
        int estimatedClauses = Math.max(orCount, Math.max(andCount, Math.max(plusCount, colonCount)));

        // If no clear operators, estimate based on length (very conservative)
        if (estimatedClauses == 0) {
            estimatedClauses = Math.max(1, query.length() / 50); // ~1 clause per 50 chars
        }

        return estimatedClauses;
    }

    /**
     * Processes a single ES query when inode count is under the limit.
     */
    private Set<String> processSingleESQuery(BrowserQuery browserQuery, Set<String> inodes, long startTime) {
        final boolean live = !browserQuery.showWorking;
        final ESSeachAPI esSearchAPI = APILocator.getEsSearchAPI();
        final List<String> collectedInodes = new ArrayList<>();

        try {
            final String baseQuery = buildBaseESQuery(browserQuery);
            final List<String> inodesList = new ArrayList<>(inodes);
            final String inodeFilter = String.format(" +inode:(%s) ", String.join(" OR ", inodesList));
            final String luceneQuery = inodeFilter + baseQuery;
            final String esQuery = String.format(ES_QUERY_TEMPLATE, luceneQuery);

            Logger.debug(this, String.format("Single ES query: %d inodes", inodes.size()));

            esSearchAPI.esSearch(esQuery, live, browserQuery.user, false).forEach(result -> {
                final Contentlet contentlet = (Contentlet) result;
                collectedInodes.add(contentlet.getInode());
            });

            final long duration = System.currentTimeMillis() - startTime;
            Logger.debug(this, String.format("Single ES query completed: %d inodes → %d matches in %d ms",
                inodes.size(), collectedInodes.size(), duration));

        } catch (Exception e) {
            Logger.error(this, String.format("Single ES query failed for %d inodes: %s", inodes.size(), e.getMessage()), e);
        }

        return new LinkedHashSet<>(collectedInodes);
    }

    /**
     * Processes multiple ES queries when inode count exceeds the limit.
     * Uses parallel processing for better performance.
     */
    private Set<String> processMultipleESQueries(BrowserQuery browserQuery, Set<String> inodes,
                                                 int maxInodesPerQuery, long startTime) {
        final Set<String> allResults = Collections.synchronizedSet(new LinkedHashSet<>());
        final List<String> inodesList = new ArrayList<>(inodes);
        final int totalInodes = inodesList.size();

        // Create sub-batches that respect the ES clause limit
        final List<List<String>> subBatches = new ArrayList<>();
        for (int i = 0; i < totalInodes; i += maxInodesPerQuery) {
            final int endIndex = Math.min(i + maxInodesPerQuery, totalInodes);
            subBatches.add(inodesList.subList(i, endIndex));
        }

        final int batchCount = subBatches.size();
        Logger.info(this, String.format("ES clause limit handling: splitting %d inodes into %d sub-queries (max %d inodes per query)",
            totalInodes, batchCount, maxInodesPerQuery));

        // Process sub-batches in parallel
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final CompletableFuture<Set<String>>[] futures = new CompletableFuture[batchCount];

        for (int i = 0; i < batchCount; i++) {
            final List<String> batch = subBatches.get(i);
            final int batchIndex = i + 1;

            futures[i] = CompletableFuture
                .supplyAsync(() -> {
                    Logger.debug(BrowserAPIImpl.this, String.format("Processing ES sub-query %d/%d: %d inodes",
                        batchIndex, batchCount, batch.size()));
                    return processSingleESQuery(browserQuery, new LinkedHashSet<>(batch), System.currentTimeMillis());
                }, submitter)
                .orTimeout(60, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    Logger.error(BrowserAPIImpl.this, String.format("ES sub-query %d failed: %s",
                        batchIndex, throwable.getMessage()), throwable);
                    return new LinkedHashSet<>();
                });
        }

        // Collect results from all sub-queries
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
            allFutures.get(120, TimeUnit.SECONDS);

            for (CompletableFuture<Set<String>> future : futures) {
                try {
                    Set<String> batchResults = future.get();
                    allResults.addAll(batchResults);
                } catch (Exception e) {
                    Logger.warn(this, "Failed to get result from ES sub-query future: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            final long totalDuration = System.currentTimeMillis() - startTime;
            Logger.info(this, String.format("Multiple ES queries completed: %d inodes in %d sub-queries → %d matches in %d ms",
                totalInodes, batchCount, allResults.size(), totalDuration));

        } catch (InterruptedException e) {
            Logger.error(this, "Multiple ES queries interrupted: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Logger.error(this, "Multiple ES queries execution error: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            Logger.error(this, "Multiple ES queries timed out: " + e.getMessage(), e);
        }

        return allResults;
    }

    /**
     * Loads contentlets in parallel using the same 30% heuristic for chunking.
     * This reduces database load by processing multiple smaller requests concurrently
     * instead of one large request that could cause timeouts or memory issues.
     *
     * @param inodes Set of inode strings to load contentlets for
     * @return List of loaded contentlets
     */
    private List<Contentlet> findContentletsInParallel(Set<String> inodes) {
        if (inodes == null || inodes.isEmpty()) {
            return new ArrayList<>();
        }

        final int totalInodes = inodes.size();
        final long startTime = System.currentTimeMillis();

        // Use the same 30% heuristic for contentlet loading chunks
        final int chunkSize = calculateContentletChunkSize(totalInodes);

        Logger.debug(this, String.format("Loading contentlets in parallel: %d inodes, chunk size: %d",
            totalInodes, chunkSize));

        // If small enough, process directly
        if (totalInodes <= chunkSize) {
            return loadContentletsSingle(new ArrayList<>(inodes), startTime);
        } else {
            return loadContentletsParallel(inodes, chunkSize, startTime);
        }
    }

    /**
     * Calculates optimal chunk size for contentlet loading using 30% heuristic.
     * Similar to ES chunk calculation but optimized for database operations.
     */
    private int calculateContentletChunkSize(int totalInodes) {
        // Use the same percentage logic as ES chunks
        final double percentage = SINGLE_QUERY_ES_CHUNK_PERCENTAGE.get() / 100.0;
        int calculatedSize = (int) Math.ceil(totalInodes * percentage);

        // Apply bounds - smaller max than ES since DB operations are typically more expensive
        final int minSize = 50;   // Minimum chunk size for efficiency
        final int maxSize = 1000; // Maximum to avoid memory/timeout issues

        calculatedSize = Math.max(calculatedSize, minSize);
        calculatedSize = Math.min(calculatedSize, maxSize);

        Logger.debug(this, String.format("Contentlet chunk calculation: %d inodes → %d per chunk (%.1f%% of total)",
            totalInodes, calculatedSize, percentage * 100));

        return calculatedSize;
    }

    /**
     * Loads contentlets in a single request (for small sets).
     */
    private List<Contentlet> loadContentletsSingle(List<String> inodes, long startTime) {
        try {
            final List<Contentlet> contentlets = APILocator.getContentletAPI().findContentlets(inodes);
            final long duration = System.currentTimeMillis() - startTime;
            Logger.debug(this, String.format("Single contentlet load: %d inodes → %d contentlets in %d ms",
                inodes.size(), contentlets.size(), duration));
            return contentlets;
        } catch (Exception e) {
            Logger.error(this, String.format("Failed to load %d contentlets: %s", inodes.size(), e.getMessage()), e);
            return new ArrayList<>();
        }
    }

    /**
     * Loads contentlets in parallel chunks for better performance and reliability.
     */
    private List<Contentlet> loadContentletsParallel(Set<String> inodes, int chunkSize, long startTime) {
        final List<String> inodesList = new ArrayList<>(inodes);
        final int totalInodes = inodesList.size();

        // Create chunks for parallel processing
        final List<List<String>> chunks = createChunks(inodesList, chunkSize);
        final int chunkCount = chunks.size();
        Logger.debug(this, String.format("Loading contentlets in parallel: %d inodes in %d chunks (chunk size: %d)",
            totalInodes, chunkCount, chunkSize));

        // Process chunks in parallel using CompletableFuture
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final CompletableFuture<List<Contentlet>>[] futures = new CompletableFuture[chunkCount];
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        for (int i = 0; i < chunkCount; i++) {
            final List<String> chunk = chunks.get(i);
            final int chunkIndex = i + 1;

            futures[i] = CompletableFuture
                .supplyAsync(() -> {
                    final long chunkStartTime = System.currentTimeMillis();
                    Logger.debug(BrowserAPIImpl.this, String.format("Loading contentlet chunk %d/%d: %d inodes",
                        chunkIndex, chunkCount, chunk.size()));

                    try {
                        final List<Contentlet> chunkContentlets = contentletAPI.findContentlets(chunk);
                        final long chunkDuration = System.currentTimeMillis() - chunkStartTime;
                        Logger.debug(BrowserAPIImpl.this, String.format(
                            "Contentlet chunk %d/%d completed: %d inodes → %d contentlets in %d ms",
                            chunkIndex, chunkCount, chunk.size(), chunkContentlets.size(), chunkDuration));
                        return chunkContentlets;
                    } catch (Exception e) {
                        Logger.error(BrowserAPIImpl.this, String.format("Contentlet chunk %d failed: %s",
                            chunkIndex, e.getMessage()), e);
                        return new ArrayList<Contentlet>();
                    }
                }, submitter)
                .orTimeout(90, TimeUnit.SECONDS) // Longer timeout for DB operations
                .exceptionally(throwable -> {
                    Logger.error(BrowserAPIImpl.this, String.format("Contentlet chunk %d timed out or failed: %s",
                        chunkIndex, throwable.getMessage()), throwable);
                    return new ArrayList<>();
                });
        }

        // Collect results from all chunks
        final List<Contentlet> allContentlets = new ArrayList<>();
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
            allFutures.get(180, TimeUnit.SECONDS); // Global timeout for all chunks

            for (CompletableFuture<List<Contentlet>> future : futures) {
                try {
                    List<Contentlet> chunkContentlets = future.get();
                    allContentlets.addAll(chunkContentlets);
                } catch (Exception e) {
                    Logger.warn(this, "Failed to get result from contentlet chunk future: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            final long totalDuration = System.currentTimeMillis() - startTime;
            Logger.debug(this, String.format(
                "Parallel contentlet loading completed: %d inodes in %d chunks → %d contentlets in %d ms",
                totalInodes, chunkCount, allContentlets.size(), totalDuration));

        } catch (InterruptedException e) {
            Logger.error(this, "Parallel contentlet loading interrupted: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Logger.error(this, "Parallel contentlet loading execution error: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            Logger.error(this, "Parallel contentlet loading timed out: " + e.getMessage(), e);
        }

        return allContentlets;
    }

    /**
     * Hydrates contentlets in parallel using chunks for improved performance.
     *
     * @param contentlets List of contentlets to hydrate
     * @param browserQuery Browser query parameters for hydration
     * @param roles User roles for permission checking
     * @param resultList Output list to add hydrated results to
     */
    private void hydrateContentletsInParallel(final List<Contentlet> contentlets,
                                              final BrowserQuery browserQuery,
                                              final Role[] roles,
                                              final List<Map<String, Object>> resultList) {
        final int totalContentlets = contentlets.size();
        final int chunkSize = Math.max(1, Math.min(10, totalContentlets / 4));
        final List<List<Contentlet>> chunks = createChunks(contentlets, chunkSize);

        final List<CompletableFuture<List<Map<String, Object>>>> futures = chunks.stream()
            .map(chunk -> CompletableFuture.supplyAsync(() -> {
                final List<Map<String, Object>> chunkResults = new ArrayList<>(chunk.size());
                for (final Contentlet contentlet : chunk) {
                    try {
                        final Map<String, Object> contentMap = hydrate(browserQuery, contentlet, roles);
                        chunkResults.add(contentMap);
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.error(this, "Error hydrating contentlet " + contentlet.getInode() + ": " + e.getMessage(), e);
                        throw new DotRuntimeException("Failed to hydrate contentlet: " + contentlet.getInode(), e);
                    }
                }
                return chunkResults;
            }, DotConcurrentFactory.getInstance().getSubmitter()))
            .collect(Collectors.toList());

        // Collect results maintaining order
        for (final CompletableFuture<List<Map<String, Object>>> future : futures) {
            try {
                resultList.addAll(future.get(30, TimeUnit.SECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Logger.error(this, "Error in parallel hydration: " + e.getMessage(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new DotRuntimeException("Failed to hydrate contentlets in parallel", e);
            }
        }
    }

    /**
     * Creates chunks from a list with the specified chunk size.
     *
     * @param list The list to split into chunks
     * @param chunkSize The size of each chunk
     * @return List of chunks, each containing at most chunkSize elements
     */
    private <T> List<List<T>> createChunks(List<T> list, int chunkSize) {
        /*
        if (list == null || list.isEmpty() || chunkSize <= 0) {
            return new ArrayList<>();
        }

        final List<List<T>> chunks = new ArrayList<>();
        final int totalSize = list.size();

        for (int i = 0; i < totalSize; i += chunkSize) {
            final int endIndex = Math.min(i + chunkSize, totalSize);
            chunks.add(list.subList(i, endIndex));
        }*/

        return Lists.partition(list, chunkSize);
    }

    /**
     * Builds the base Elasticsearch query with filter and fileName criteria,
     * excluding the inode list which will be added later.
     *
     * @param browserQuery The {@link BrowserQuery} containing search criteria
     * @return The base Elasticsearch query string without inode filtering
     */
    String buildBaseESQuery(final BrowserQuery browserQuery) {
        final StringBuilder baseQuery = new StringBuilder();

        if (UtilMethods.isSet(browserQuery.filter)) {
            final String titleFilters = String.format(
                    "title:%s* OR title:'%s'^15 OR title_dotraw:*%s*^5 OR +catchall:*%s*^10",
                    browserQuery.filter,
                    browserQuery.filter,
                    browserQuery.filter,
                    browserQuery.filter);
            baseQuery.append(titleFilters);
        }

        if (UtilMethods.isSet(browserQuery.fileName)) {
            if (isWriteMetadataOnReindex() && getDotRawMetadataFields().contains("name")) {
                final String metadataFilters = String.format(
                        "metadata.name:%s* OR metadata.name:'%s'^15 OR metadata.name_dotraw:*%s*^5",
                        browserQuery.fileName,
                        browserQuery.fileName,
                        browserQuery.fileName);
                if (baseQuery.length() > 0) {
                    baseQuery.append(" AND ");
                }
                baseQuery.append(metadataFilters);
            } else {
                Logger.warn(BrowserAPIImpl.class,
                        String.format(
                                "Unable to search fileAssets by fileName in Elasticsearch: " +
                                        "metadata indexing is disabled by property '%s'. " +
                                        "Additionally, ensure that property (if overwritten) '%s' contains the 'name' field. " +
                                        "Current metadata fields: %s",
                                WRITE_METADATA_ON_REINDEX,
                                INCLUDE_DOTRAW_METADATA_FIELDS,
                                getDotRawMetadataFields()));
            }
        }

        // Early return if no query was built
        if (baseQuery.length() == 0) {
            return BLANK;
        }

        // Wrap in mandatory group for ES query_string syntax
        return " +(" + baseQuery + ')';
    }

    /**
     * Determines whether Elasticsearch should be used for text-based filtering instead of SQL ILIKE queries.
     * This optimization is triggered when Elasticsearch filtering is enabled AND there are text search criteria.
     *
     * @param browserQuery The {@link BrowserQuery} containing filtering preferences and search criteria
     * @return {@code true} if ES should be used for text filtering, {@code false} to use SQL filtering
     */
    boolean isUseElasticSearchForTextFiltering(final BrowserQuery browserQuery) {
        final boolean hasTextFilter = UtilMethods.isSet(browserQuery.filter) ||
                UtilMethods.isSet(browserQuery.fileName);
        return browserQuery.useElasticsearchFiltering && hasTextFilter;
    }

    /**
     * Returns a collection of contentlets, folders, links based on diff attributes of the BrowserQuery
     * object. The collection is filtered based on the user's permissions respecting front-end roles
     * @param browserQuery {@link BrowserQuery}
     * @return list of treeable (folders, content, links)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    public List<Treeable> getFolderContentList(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {
      return getFolderContentList(browserQuery, true);
    }

    /**
     * Returns a collection of contentlets, folders, links based on diff attributes of the BrowserQuery
     * @param browserQuery {@link BrowserQuery}
     * @param respectFrontEndRoles if true, the method will respect the front end roles
     * @return list of treeable (folders, content, links)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Override
    @CloseDBIfOpened
    public List<Treeable> getFolderContentList(final BrowserQuery browserQuery, final boolean respectFrontEndRoles) throws DotSecurityException, DotDataException{

        final List<Contentlet> contentlets = browserQuery.showContent ? getContentUnderParentFromDB(browserQuery)
                : Collections.emptyList();
        final List<Treeable> returnList = new ArrayList<>(contentlets);

        if (browserQuery.showFolders) {
            List<Folder> folders = folderAPI.findSubFoldersByParent(browserQuery.directParent, userAPI.getSystemUser(), false);
            if (browserQuery.showMenuItemsOnly) {
                folders.removeIf(folder -> !folder.isShowOnMenu());
            }
            returnList.addAll(folders);
        }

        if (browserQuery.showLinks) {
            List<Link> links = this.getLinks(browserQuery);
            if(browserQuery.showMenuItemsOnly){
                links.removeIf(link -> !link.isShowOnMenu());
            }
            returnList.addAll(links);
        }

        return permissionAPI.filterCollection(returnList, PERMISSION_READ, respectFrontEndRoles, browserQuery.user);
    }

    @Override
    @CloseDBIfOpened
    public Map<String, Object> getFolderContent(final BrowserQuery browserQuery) throws DotSecurityException, DotDataException {
        List<Map<String, Object>> returnList = new ArrayList<>();
        final Role[] roles = APILocator.getRoleAPI().loadRolesForUser(browserQuery.user.getUserId()).toArray(new Role[0]);

        if (browserQuery.showFolders) {
            returnList.addAll(this.getFolders(browserQuery,  roles));
        }

        if (browserQuery.showLinks) {
            returnList.addAll(this.includeLinks(browserQuery));
        }

        //Get Content
        final List<Contentlet> contentlets = browserQuery.showContent ? getContentUnderParentFromDB(browserQuery)
                : Collections.emptyList();

        for (final Contentlet contentlet : contentlets) {
            final Map<String, Object> contentMap = hydrate(browserQuery, contentlet, roles);
            returnList.add(contentMap);
        }

        // Filtering
        returnList = this.filterReturnList(browserQuery,returnList);

        // Sorting
        Collections.sort(returnList, new WebAssetMapComparator(browserQuery.sortBy, browserQuery.sortByDesc));

        int offset     = browserQuery.offset;
        int maxResults = browserQuery.maxResults;
        // Offsetting
        if (offset < 0) {
            offset = 0;
        }

        if (maxResults <= 0) {
            maxResults = returnList.size() - offset;
        }

        if (maxResults + offset > returnList.size()) {
            maxResults = returnList.size() - offset;
        }

        final Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("total", returnList.size());
        returnMap.put("list",
                offset > returnList.size() ? Collections.emptyList() : returnList.subList(offset, offset + maxResults));
        return returnMap;
    }

    @Override
    @CloseDBIfOpened
    public PaginatedContents getPaginatedContents(final BrowserQuery browserQuery)
            throws DotSecurityException, DotDataException {

        final Role[] roles = APILocator.getRoleAPI()
                .loadRolesForUser(browserQuery.user.getUserId())
                .toArray(new Role[0]);

        final List<Map<String, Object>> list = new LinkedList<>();

        int offset = browserQuery.offset;
        int maxResults = browserQuery.maxResults;

        int folderCount = 0;
        int contentTotalCount = 0;
        int contentCount = 0;

        // 1. Folders
        if (browserQuery.showFolders) {
            final List<Map<String, Object>> folders = getFolders(browserQuery, roles);
            folderCount = folders.size();

            // Calculate if the offset still falls within folders
            if (offset < folderCount) {
                int toIndex = Math.min(folderCount, offset + maxResults);
                list.addAll(folders.subList(offset, toIndex));
                maxResults -= (toIndex - offset);
                offset = 0;
            } else {
                offset -= folderCount;
            }
        }

        // 2. Contentlets
        if (browserQuery.showContent && maxResults > 0) {
            // Now the offset is adjusted (subtracting folders already seen)
            final ContentUnderParent fromDB = getContentUnderParentFromDB(browserQuery, offset, maxResults);
            contentTotalCount = fromDB.totalResults;
            contentCount = fromDB.contentlets.size();

            // Parallelize hydration with chunks
            hydrateContentletsInParallel(fromDB.contentlets, browserQuery, roles, list);
        }

        // Final sorting (optional: maybe you only need to sort within each block before slicing)
        list.sort(new GenericMapFieldComparator(browserQuery.sortBy, browserQuery.sortByDesc));

        return new PaginatedContents(list, folderCount, contentTotalCount, contentCount);
    }

    /**
     * Paginated result
     */
    public static class PaginatedContents {
        public final List<Map<String, Object>> list;
        public final int folderCount;
        public final int contentTotalCount;
        public final int contentCount;

        public PaginatedContents(final List<Map<String, Object>> list, final int folderCount,
                final int contentTotalCount, final int contentCount) {
            this.list = list;
            this.folderCount = folderCount;
            this.contentTotalCount = contentTotalCount;
            this.contentCount = contentCount;
        }
    }

    /**
     * hydrate and transform fetched contentlets
     * @param browserQuery QueryParams
     * @param contentlet incoming
     * @param roles precalculated roles
     * @return Map
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private @NotNull Map<String, Object> hydrate(BrowserQuery browserQuery,
            Contentlet contentlet, Role[] roles) throws DotDataException, DotSecurityException {

        final long hydrationStartTime = System.nanoTime();
        final String contentletId = contentlet.getInode();
        final String contentType = contentlet.getContentType().variable();

        try {
            // Step 1: Content mapping based on type
            final long mappingStartTime = System.nanoTime();
            Map<String, Object> contentMap = createContentMap(contentlet);
            final long mappingDuration = System.nanoTime() - mappingStartTime;

            // Step 2: Shorty identifiers (if requested)
            final long shortiesStartTime = System.nanoTime();
            if (browserQuery.showShorties) {
                contentMap.put("shortyIdentifier", this.shortyIdAPI.shortify(contentlet.getIdentifier()));
                contentMap.put("shortyInode", this.shortyIdAPI.shortify(contentlet.getInode()));
            }
            final long shortiesDuration = System.nanoTime() - shortiesStartTime;

            // Step 3: Permissions calculation
            final long permissionsStartTime = System.nanoTime();
            final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(contentlet, roles, browserQuery.user);
            final long permissionsDuration = System.nanoTime() - permissionsStartTime;

            // Step 4: Workflow data
            final long workflowStartTime = System.nanoTime();
            final WfData wfdata = new WfData(contentlet, permissions, browserQuery.user, browserQuery.showArchived);
            contentMap.put("wfActionMapList", wfdata.wfActionMapList);
            contentMap.put("contentEditable", wfdata.contentEditable);
            contentMap.put("permissions", permissions);
            final long workflowDuration = System.nanoTime() - workflowStartTime;

            // Calculate total duration and log performance details
            final long totalDuration = System.nanoTime() - hydrationStartTime;
            final long totalMillis = TimeUnit.NANOSECONDS.toMillis(totalDuration);


            // Log slow hydrations for performance analysis
            if (totalMillis > 100) {
                Logger.warn(this, String.format(
                    "SLOW HYDRATION: contentlet=%s, type=%s, total=%dms [mapping=%dms, shorties=%dms, permissions=%dms, workflow=%dms]",
                    contentletId, contentType, totalMillis,
                    TimeUnit.NANOSECONDS.toMillis(mappingDuration),
                    TimeUnit.NANOSECONDS.toMillis(shortiesDuration),
                    TimeUnit.NANOSECONDS.toMillis(permissionsDuration),
                    TimeUnit.NANOSECONDS.toMillis(workflowDuration)
                ));
            }

            return contentMap;

        } catch (DotDataException | DotSecurityException e) {
            final long totalDuration = System.nanoTime() - hydrationStartTime;
            final long totalMillis = TimeUnit.NANOSECONDS.toMillis(totalDuration);
            Logger.error(this, String.format(
                "HYDRATION ERROR: contentlet=%s, type=%s, duration=%dms, error=%s",
                contentletId, contentType, totalMillis, e.getMessage()
            ), e);
            throw e;
        } catch (Exception e) {
            final long totalDuration = System.nanoTime() - hydrationStartTime;
            final long totalMillis = TimeUnit.NANOSECONDS.toMillis(totalDuration);
            Logger.error(this, String.format(
                "HYDRATION UNEXPECTED ERROR: contentlet=%s, type=%s, duration=%dms, error=%s",
                contentletId, contentType, totalMillis, e.getMessage()
            ), e);
            throw new DotRuntimeException("Failed to hydrate contentlet: " + contentletId, e);
        }
    }

    /**
     * Creates the appropriate content map based on contentlet type.
     * Extracted for better performance tracking and maintainability.
     */
    private Map<String, Object> createContentMap(Contentlet contentlet)  {
        final Optional<BaseContentType> baseType = contentlet.getBaseType();
        if (baseType.isPresent() && baseType.get() == BaseContentType.FILEASSET) {
            final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
            return fileAssetMap(fileAsset);
        } else if (baseType.isPresent() && baseType.get() == BaseContentType.DOTASSET) {
            return dotAssetMap(contentlet);
        } else if (baseType.isPresent() &&  baseType.get() == BaseContentType.HTMLPAGE) {
            final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
            return htmlPageMap(page);
        } else {
            return dotContentMap(contentlet);
        }
    }

    private List<Map<String, Object>> filterReturnList(final BrowserQuery browserQuery, final List<Map<String, Object>> returnList) {

        final List<Map<String, Object>> filteredList = new ArrayList<>();
        for (final Map<String, Object> asset : returnList) {

            String mimeType = (String) asset.get("mimeType");
            mimeType = mimeType == null ? BLANK : mimeType;

            if (browserQuery.mimeTypes != null && !browserQuery.mimeTypes.isEmpty()) {

                boolean match = false;
                for (final String mType : browserQuery.mimeTypes) {
                    if (mimeType.contains(mType)) {
                        match = true;
                    }
                }

                if (!match) {
                    continue;
                }
            }

            if (browserQuery.extensions != null && !browserQuery.extensions.isEmpty()) {

                boolean match = false;
                for (final String extension : browserQuery.extensions) {
                    if (((String) asset.get("extension")).contains(extension)) {
                        match = true;
                    }
                }

                if (!match) {
                    continue;
                }
            }

            filteredList.add(asset);
        }

        return filteredList;
    }

    /**
     * Generates both the select and count SQL queries with all filtering criteria applied.
     * This method ensures that both queries use identical filtering logic for consistency.
     *
     * @param browserQuery The filtering criteria set via the {@link BrowserQuery}.
     * @return The {@link SelectAndCountQueries} object containing both select and count queries with parameters.
     */
    private SelectAndCountQueries selectAndCountQueries(final BrowserQuery browserQuery) {

        final String workingLiveInode = browserQuery.showWorking || browserQuery.showArchived ?
                "working_inode" : "live_inode";

        final BaseQuery baseQueries = buildBaseQuery(browserQuery, workingLiveInode);
        final StringBuilder selectQuery = new StringBuilder(baseQueries.selectQuery);
        final StringBuilder countQuery = new StringBuilder(baseQueries.countQuery);

        final List<Object> parameters = new ArrayList<>();
        final List<Object> dump = new ArrayList<>();

        if (!browserQuery.languageIds.isEmpty()) {
            appendLanguageQuery(selectQuery, browserQuery.languageIds,
                    browserQuery.showDefaultLangItems);
            appendLanguageQuery(countQuery, browserQuery.languageIds,
                    browserQuery.showDefaultLangItems);
        }
        if (browserQuery.site != null) {
            appendSiteQuery(selectQuery, browserQuery.site.getIdentifier(), browserQuery.forceSystemHost, parameters);
            appendSiteQuery(countQuery, browserQuery.site.getIdentifier(), browserQuery.forceSystemHost, dump);
        } else {
            if (browserQuery.forceSystemHost) {
                appendSystemHostQuery(selectQuery);
                appendSystemHostQuery(countQuery);
            }
        }
        //This property allows the exclusion of the folder in the base query
        if (browserQuery.folder != null && !browserQuery.skipFolder) {
            appendFolderQuery(selectQuery, browserQuery.folder.getPath(), parameters);
            appendFolderQuery(countQuery, browserQuery.folder.getPath(), dump);
        }
        //We only build the filtering bits of the SQL Query if we're not using ES
        if (!browserQuery.useElasticsearchFiltering) {
            if (UtilMethods.isSet(browserQuery.filter)) {
                appendFilterQuery(selectQuery, browserQuery.filter, parameters);
                appendFilterQuery(countQuery, browserQuery.filter, dump);
            }
            if (UtilMethods.isSet(browserQuery.fileName)) {
                appendFileNameQuery(selectQuery, browserQuery.fileName, parameters);
                appendFileNameQuery(countQuery, browserQuery.fileName, dump);
            }
        }
        if (browserQuery.showMenuItemsOnly) {
            appendShowOnMenuQuery(selectQuery);
            appendShowOnMenuQuery(countQuery);
        }
        if (!browserQuery.showArchived) {
            appendExcludeArchivedQuery(selectQuery);
            appendExcludeArchivedQuery(countQuery);
        }

        if (null != browserQuery.sortBy) {
            appendOrderByQuery(selectQuery, browserQuery.sortByDesc);
        }

        Logger.debug(this, "Select Query: " + selectQuery);
        Logger.debug(this, "Count Query: " + countQuery);

        return new SelectAndCountQueries(selectQuery.toString(), countQuery.toString(), parameters);
    }

    /**
     * Simple inner class to pass around data and make things a bit easier to understand
     */
    static class SelectAndCountQueries {
        final String selectQuery;
        final String countQuery;
        final List<Object> params;
        SelectAndCountQueries(String selectQuery, String countQuery, List<Object> params) {
            this.selectQuery = selectQuery;
            this.countQuery = countQuery;
            this.params = params;
        }
    }

    /**
     * Builds the base SQL queries (both regular and count) for content retrieval based on specific filtering criteria.
     *
     * @param browserQuery     The {@link BrowserQuery} object specifying the filtering criteria.
     * @param workingLiveInode The identifier of the working live inode.
     * @return The {@link BaseQuery} object containing both regular and count SQL queries.
     */
    private BaseQuery buildBaseQuery(final BrowserQuery browserQuery, final String workingLiveInode) {

        // Common base clause shared between select and count queries
        final String baseClause = " from contentlet_version_info cvi, identifier id, structure struc, contentlet c "
                + " where cvi.identifier = id.id and struc.velocity_var_name = id.asset_subtype and  "
                + " c.inode = cvi." + workingLiveInode + " and cvi.variant_id='"
                + DEFAULT_VARIANT.name() + "' ";

        // Build the main query
        final StringBuilder baseQuery = new StringBuilder(
                "select cvi." + workingLiveInode + " as inode " + baseClause);

        // Build the count query
        final StringBuilder countQuery = new StringBuilder(
                "select count(cvi." + workingLiveInode + ") as count " + baseClause);

        final boolean showAllBaseTypes = browserQuery.baseTypes.contains(BaseContentType.ANY);
        if (!showAllBaseTypes) {
            final List<String> baseTypes =
                    browserQuery.baseTypes.stream().map(t -> String.valueOf(t.getType()))
                            .collect(Collectors.toList());
            String baseTypeFilter = " and struc.structuretype in (" + String.join(" , ", baseTypes) + ") ";
            baseQuery.append(baseTypeFilter);
            countQuery.append(baseTypeFilter);
        }

        if(!browserQuery.contentTypeIds.isEmpty()){
            String contentTypeFilter = " and struc.inode in (" +
                    browserQuery.contentTypeIds.stream()
                            .map(id -> "'" + id + "'")
                            .collect(Collectors.joining(" , ")) + ") ";
            baseQuery.append(contentTypeFilter);
            countQuery.append(contentTypeFilter);
        }

        if (!browserQuery.excludedContentTypeIds.isEmpty()) {
            String excludeTypesFilter = " and struc.inode not in (" +
                    browserQuery.excludedContentTypeIds.stream()
                            .map(id -> "'" + id + "'")
                            .collect(Collectors.joining(" , ")) + ") ";
            baseQuery.append(excludeTypesFilter);
            countQuery.append(excludeTypesFilter);
        }

        return new BaseQuery(baseQuery.toString(), countQuery.toString());
    }

    static class BaseQuery {
        final String selectQuery;
        final String countQuery;
        BaseQuery(String selectQuery, String countQuery) {
            this.selectQuery = selectQuery;
            this.countQuery = countQuery;
        }
    }

    /**
     * Appends the language query to the given SQL query to filter content by language.
     *
     * @param sqlQuery             The StringBuilder object representing the SQL query.
     * @param languageIds          The Set of language IDs to filter by.
     * @param showDefaultLangItems Whether to include default language items in the filter.
     */
    private void appendLanguageQuery(StringBuilder sqlQuery, Set<Long> languageIds,
            boolean showDefaultLangItems) {

        final Set<Long> filteredLanguageIds = languageIds.stream()
                .filter(langId -> langId != null && langId > 0)
                .collect(Collectors.toSet());

        if (filteredLanguageIds.isEmpty()) {
            return;
        }

        sqlQuery.append(" and cvi.lang in (");
        sqlQuery.append(filteredLanguageIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));

        final long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        if (showDefaultLangItems && !filteredLanguageIds.contains(defaultLang)) {
            sqlQuery.append(",").append(defaultLang);
        }
        sqlQuery.append(")");
    }

    /**
     * Appends the query to filter by site identifier to the given SQL query and adds the site
     * identifier to the parameter list.
     *
     * @param sqlQuery       The StringBuilder object representing the SQL query to be appended.
     * @param siteIdentifier The site identifier to filter by.
     * @param parameters     The list of parameters to add the site identifier to.
     */
    private void appendSiteQuery(StringBuilder sqlQuery, String siteIdentifier, boolean forceSystemHost,
            List<Object> parameters) {
        if(forceSystemHost){
            sqlQuery.append(" and (id.host_inode = ? or id.host_inode = 'SYSTEM_HOST') ");
        } else {
            sqlQuery.append(" and (id.host_inode = ?) ");
        }
        parameters.add(siteIdentifier);
    }

    private void appendSystemHostQuery(StringBuilder sqlQuery) {
            sqlQuery.append(" and (id.host_inode = 'SYSTEM_HOST') ");
    }
    /**
     * Appends the query to filter by a specific folder path to the given SQL query and adds the
     * folder path to the list of parameters.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param folderPath The folder path to filter by.
     * @param parameters The list of parameters to add the folder path to.
     */
    private void appendFolderQuery(StringBuilder sqlQuery, String folderPath,
            List<Object> parameters) {

        sqlQuery.append(" and id.parent_path=? ");
        parameters.add(folderPath);
    }

    /**
     * Appends the query to filter by a specific text filter to the given SQL query and adds the
     * necessary parameters for the filter.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param filter     The filter string to match against. Case-insensitive.
     * @param parameters The list of parameters to add the filter values to.
     */
    private void appendFilterQuery(StringBuilder sqlQuery, String filter,
            List<Object> parameters) {

        final String filterText = filter.toLowerCase().trim();
        final String[] splitter = filterText.split(" ");

        sqlQuery.append(" and (");
        for (int indx = 0; indx < splitter.length; indx++) {
            final String token = splitter[indx];
            if (token.equals(BLANK)) {
                continue;
            }


            // I am not sure if this is faster of if we should just hand it off to ES in the case of a user trying to filter
            // I can see this grinding in large directories.
            sqlQuery.append(" contentlet_as_json::text ILIKE ? ");
            parameters.add("%" + token + "%");
            if (indx + 1 < splitter.length) {
                sqlQuery.append(" and");
            }
        }
        sqlQuery.append(" OR ");
        sqlQuery.append(getAssetNameColumn(ASSET_NAME_LIKE.toString()));
        sqlQuery.append(" OR ");
        sqlQuery.append(getBinaryAssetNameColumn(ASSET_NAME_LIKE.toString()));
        sqlQuery.append(" OR ");
        sqlQuery.append(" working_inode in( select inode from tag, tag_inode where tag.tag_id=tag_inode.tag_id and tagname ILIKE ? ) ");

        sqlQuery.append(" ) ");




        parameters.add("%" + filterText + "%");
        parameters.add("%" + filterText + "%");
        parameters.add("%" + filterText + "%");


    }

    /**
     * Appends the query to filter by filename to the given SQL query and adds the filename to the
     * parameters list.
     *
     * @param sqlQuery   The StringBuilder object representing the SQL query to be appended.
     * @param fileName   The filename to filter by.
     * @param parameters The list of parameters to add the filename to.
     */
    private void appendFileNameQuery(StringBuilder sqlQuery, String fileName,
            List<Object> parameters) {

        final String matchText = fileName.toLowerCase().trim();
        sqlQuery.append(" and (");
        sqlQuery.append(" LOWER(id.asset_name) = ?");
        sqlQuery.append(" ) ");
        parameters.add(matchText);
    }

    /**
     * Appends the query to filter by show_on_menu flag to the given SQL query. Adds the necessary
     * conditions to the query based on the show_on_menu property
     *
     * @param sqlQuery The StringBuilder object representing the SQL query to be appended.
     */
    private void appendShowOnMenuQuery(StringBuilder sqlQuery) {
        sqlQuery.append(" and c.show_on_menu = ").append(DbConnectionFactory.getDBTrue());
    }

    /**
     * Appends the query to exclude archived content to the given SQL query.
     *
     * @param sqlQuery The StringBuilder object representing the SQL query to be appended.
     */
    private void appendExcludeArchivedQuery(StringBuilder sqlQuery) {
        sqlQuery.append(" and cvi.deleted = ").append(DbConnectionFactory.getDBFalse());
    }

    /**
     * Appends an order by condition to the main query
     * @param sqlQuery
     * @param orderByDesc
     */
    private void appendOrderByQuery(StringBuilder sqlQuery, boolean orderByDesc) {
        sqlQuery.append(" order by ");
        if (orderByDesc) {
            sqlQuery.append(" c.mod_date desc");
        } else  {
            sqlQuery.append(" c.mod_date asc");
        }
    }

    /**
     * Returns the appropriate column for the {@code Asset Name} field depending on the database that dotCMS is running
     * on. That is, if the value is inside the "Content as JSON" column, or the legacy "text" column.
     *
     * @param baseQuery The base SQL query whose column name will be replaced.
     *
     * @return The appropriate database column for the Asset Name field.
     */
    public static String getAssetNameColumn(final String baseQuery) {
        String sql = baseQuery;
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            if (DbConnectionFactory.isPostgres()) {
                sql = String.format(sql, POSTGRES_ASSETNAME_COLUMN);
            } else {
                sql = String.format(sql, MSSQL_ASSETNAME_COLUMN);
            }
        }
        return sql;
    }
    /**
     * Retrieve the column that corresponds to the {@code Asset Name} of a binary field.
     * The value that is inside the "Content as JSON" column called "asset".
     *
     * @param baseQuery The base SQL query whose column name will be replaced.
     *
     * @return The appropriate database column for the Asset Name field.
     */
    private String getBinaryAssetNameColumn(final String baseQuery){
        String sql = baseQuery;
        if (APILocator.getContentletJsonAPI().isJsonSupportedDatabase()) {
            sql = String.format(sql, POSTGRES_BINARY_ASSETNAME_COLUMN);
        }
        return sql;
    }

    private List<Map<String, Object>> includeLinks(final BrowserQuery browserQuery)
            throws DotDataException, DotSecurityException {

        List<Map<String, Object>> returnList = new ArrayList<>();

        for (final Link link : getLinks(browserQuery)) {

            final List<Integer> permissions2 =
                    permissionAPI.getPermissionIdsFromRoles(link, browserQuery.roles, browserQuery.user);

            if (permissions2.contains(PERMISSION_READ)) {

                final Map<String, Object> linkMap = link.getMap();
                linkMap.put("permissions", permissions2);
                linkMap.put("mimeType", "application/dotlink");
                linkMap.put("name", link.getTitle());
                linkMap.put("title", link.getName());
                linkMap.put("description", link.getFriendlyName());
                linkMap.put("extension", "link");
                linkMap.put("hasLiveVersion", APILocator.getVersionableAPI().hasLiveVersion(link));
                linkMap.put("statusIcons", UtilHTML.getStatusIcons(link));
                linkMap.put("hasTitleImage", "");
                linkMap.put("__icon__", "linkIcon");
                returnList.add(linkMap);

            }
        }
        return returnList;
    } // includeLinks.


    private List<Link> getLinks(final BrowserQuery browserQuery) throws DotDataException, DotSecurityException {
        if (browserQuery.directParent instanceof Host) {
            return folderAPI.getLinks((Host) browserQuery.directParent,
                    browserQuery.showWorking, browserQuery.showArchived, browserQuery.user,
                    false);
        }

        if (browserQuery.directParent instanceof Folder) {
            return folderAPI
                    .getLinks((Folder) browserQuery.directParent, browserQuery.showWorking, browserQuery.showArchived,
                            browserQuery.user,
                            false);
        }
        return Collections.emptyList();
    }



    private  List<Map<String, Object>> getFolders(final BrowserQuery browserQuery, final Role[] roles) throws DotDataException, DotSecurityException {

        if (browserQuery.directParent != null) {

            List<Folder> folders = Collections.emptyList();
            try {

                folders = folderAPI.findSubFoldersByParent(browserQuery.directParent, userAPI.getSystemUser(),false).stream()
                        .sorted(Comparator.comparing(Folder::getName)).collect(Collectors.toList());

            } catch (Exception e1) {

                Logger.error(this, "Could not load folders : ", e1);
            }


            if(browserQuery.showMenuItemsOnly) {
                folders.removeIf(f->!f.isShowOnMenu());
            }

            if(browserQuery.filterFolderNames){
                folders.removeIf(f->!f.getName().toLowerCase().contains(browserQuery.filter.toLowerCase()));
            }

            final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(folders)
                    .withUserAndRoles(browserQuery.user, roles).build();
            return transformer.toMaps();

        }
        return List.of();
    } // getFolders.

    private Map<String,Object> htmlPageMap(final HTMLPageAsset page) throws DotStateException {
        return new DotTransformerBuilder().webAssetOptions().content(page).build().toMaps().get(0);
    } // htmlPageMap.

    private Map<String,Object> fileAssetMap(final FileAsset fileAsset) throws DotStateException {
        return new DotTransformerBuilder().webAssetOptions().content(fileAsset).build().toMaps().get(0);
    } // fileAssetMap.

    private Map<String,Object> dotAssetMap(final Contentlet dotAsset) throws DotStateException {
        return new DotTransformerBuilder().dotAssetOptions().content(dotAsset).build().toMaps().get(0);
    } // dotAssetMap.

    private Map<String, Object> dotContentMap(final Contentlet dotAsset) throws DotStateException {
        return new DotTransformerBuilder().defaultOptions().content(dotAsset).build().toMaps().get(0);
    } // dotAssetMap.


    protected class WfData {

        List<WorkflowAction> wfActions = new ArrayList<>();
        boolean contentEditable = false;
        List<Map<String, Object>> wfActionMapList = new ArrayList<>();
        boolean skip=false;

        public WfData(final Contentlet contentlet, final List<Integer> permissions, final User user, final boolean showArchived)
                throws DotStateException, DotDataException, DotSecurityException {

            if(null==contentlet) {
                return;
            }

            wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user, WorkflowAPI.RenderMode.LISTING);

            if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user) && contentlet.isLocked()) {

                final Optional<String> lockedUserId = APILocator.getVersionableAPI().getLockedBy(contentlet);
                contentEditable = lockedUserId.isPresent() && user.getUserId().equals(lockedUserId.get());
            } else {

                contentEditable = false;
            }

            if (permissions.contains(PERMISSION_READ)) {

                if (!showArchived && contentlet.isArchived()) {
                    skip=true;
                    return;
                }

                final boolean showScheme = wfActions!=null?
                        wfActions.stream().collect(Collectors.groupingBy(WorkflowAction::getSchemeId)).size()>1 : false;

                for (final WorkflowAction action : wfActions) {

                    final WorkflowScheme wfScheme         = APILocator.getWorkflowAPI().findScheme(action.getSchemeId());
                    final Map<String, Object> wfActionMap = new HashMap<>();
                    wfActionMap.put("name", action.getName());
                    wfActionMap.put("id",   action.getId());
                    wfActionMap.put("icon", action.getIcon());
                    wfActionMap.put("assignable",  action.isAssignable());
                    wfActionMap.put("commentable", action.isCommentable() || UtilMethods.isSet(action.getCondition()));
                    if (action.hasMoveActionletActionlet() && !action.hasMoveActionletHasPathActionlet()) {

                        wfActionMap.put("moveable", "true");
                    }

                    final String actionName = Try.of(() -> LanguageUtil.get(user, action.getName())).getOrElse(action.getName());
                    final String schemeName = Try.of(() ->LanguageUtil.get(user,wfScheme.getName())).getOrElse(wfScheme.getName());
                    final String actionNameStr = showScheme? actionName +" ( "+schemeName+" )" : actionName;

                    wfActionMap.put("wfActionNameStr",actionNameStr);
                    wfActionMap.put("hasPushPublishActionlet", action.hasPushPublishActionlet());
                    wfActionMapList.add(wfActionMap);
                }
            }
        }
    } // WfData

}
