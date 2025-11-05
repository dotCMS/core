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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
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

            //Now this should load the good contentlets
            final List<Contentlet> contentlets = APILocator.getContentletAPI().findContentlets(new ArrayList<>(collectedInodes));

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

    private Set<String> doElasticSearchTextFiltering(BrowserQuery browserQuery, int startRow, int maxRows,
            SelectAndCountQueries sqlQuery) throws DotDataException {
        final Set<String> collectedInodes = new LinkedHashSet<>();
        boolean refetch = false;
        int startAt = startRow;
        //This is the minimum number of rows to fetch from ES.
        final int minCollectedToFeelHappy = MIN_COLLECTED_MATCHES.get();
        int attempts = 0;
        do {
            attempts++;
            final DotConnect dcSelect = new DotConnect().setSQL(sqlQuery.selectQuery);
            sqlQuery.params.forEach(dcSelect::addParam);

            //Set Pagination params only if they make sense, this also allows me to keep the original behavior available
            dcSelect.setStartRow(startAt).setMaxRows(maxRows);

            @SuppressWarnings("unchecked")
            final List<Map<String, String>> inodesMapList = dcSelect.loadResults();
            Set<String> inodes = inodesMapList.stream().map(data -> data.get("inode"))
                    .collect(Collectors.toSet());
            /// the contents retuned from ES are less than the expected fo fit the page size
            if (!inodes.isEmpty()) {
                //After having applied the text filters, these are the good inodes we need to return
                final Set<String> matchingInodes = new HashSet<>(inodesByTextFromES(browserQuery, inodes));
                collectedInodes.addAll(matchingInodes);
                if (collectedInodes.size() < minCollectedToFeelHappy) {
                    refetch = true;
                    //We didn't get back a lot we still have room for another attempt
                    startAt += maxRows;
                } else {
                   break;
                }
            }
        } while (refetch && attempts < MAX_DB_ATTEMPT.get());
        return collectedInodes;
    }

    //We'll stop digging when we feel happy with the number of items we have collected to show
    final Lazy<Integer> MIN_COLLECTED_MATCHES = Lazy.of(
            () -> Config.getIntProperty("BROWSE_API_MIN_COLLECTED_MATCHES", 30));

    //We'll stop trying to fetch from the DB if we don't get back enough results
    final Lazy<Integer> MAX_DB_ATTEMPT = Lazy.of(
            () -> Config.getIntProperty("BROWSE_API_MAX_DB_ATTEMPT", 100));

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
     * Filters the provided set of inodes by performing text-based searches in Elasticsearch.
     * This method partitions the inodes and executes parallel searches to improve performance.
     *
     * @param browserQuery The {@link BrowserQuery} containing search criteria (filter, fileName)
     * @param inodes       The set of inodes to filter through Elasticsearch text search
     * @return A filtered set of inodes that match the text search criteria
     */
    private Set<String> inodesByTextFromES(BrowserQuery browserQuery, Set<String> inodes) {
        final Set<String> collectedInodes = new HashSet<>();
        //Collect the results returned by the futures and extract the matching inodes
        getFutures(browserQuery, inodes).forEach(future -> {
            try {
                collectedInodes.addAll(future.get());
            } catch (Exception e) {
                Logger.error(this, "Error while getting content from lucene", e);
                Thread.currentThread().interrupt();
            }
        });
        return collectedInodes;
    }

    /**
     * Creates asynchronous tasks for parallel Elasticsearch searches across partitioned inode sets.
     * This method optimizes performance by building the base query once and reusing it across all partitions,
     * avoiding redundant query reconstruction for each parallel search operation.
     *
     * @param browserQuery The {@link BrowserQuery} containing search criteria for ES query construction
     * @param inodes       The complete set of inodes to be partitioned and searched
     * @return A list of {@link Future} objects, each representing an async ES search for a partition of inodes
     */
    private List<Future<List<String>>> getFutures(final BrowserQuery browserQuery,
            final Set<String> inodes) {
        final List<Future<List<String>>> futures = new ArrayList<>();
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        Logger.info(BrowserAPIImpl.class,"inodes returned from the db : "+inodes);
        final List<List<String>> partitions = Lists.partition(new ArrayList<>(inodes), 10);

        // Build the base query once outside the loop - this is the key optimization
        final String baseQuery = buildBaseESQuery(browserQuery);

        //Take the seed inodes returned from the DB. Partition them into smaller sets
        //Execute them in parallel queries. Sending the subset of nodes and the text params,
        //We're searching for this next line crete future that will bring the matching inodes returned by ES
        partitions.forEach(partition -> futures.add(
                      submitter.submit(() -> performSearchES(baseQuery, partition, browserQuery))
                )
        );
        return futures;
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
                    "title:%s* OR title:'%s'^15 OR title_dotraw:*%s*^5 OR +catchall:%s*^10",
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
     * Performs an Elasticsearch search by combining a pre-built base query with a specific partition of inodes.
     * This method represents the optimized approach where the base query (containing filter and fileName criteria)
     * is constructed once and reused, with only the inode filter being dynamically added for each partition.
     *
     * @param baseQuery    The pre-constructed Lucene query containing search filters (title, metadata, etc.)
     * @param partition    A subset of inodes to search within this specific Elasticsearch query
     * @param browserQuery The original {@link BrowserQuery} containing user context and display preferences
     * @return A list of inode strings that match both the base query criteria and the partition constraint
     */
    List<String> performSearchES(String baseQuery, List<String> partition, BrowserQuery browserQuery) {
        final boolean live = !browserQuery.showWorking;
        final ESSeachAPI esSearchAPI = APILocator.getEsSearchAPI();
        final List<String> collectedInodes = new ArrayList<>();
        // Build the complete query by combining the precompiled base query with the inode filter
        final String inodeFilter = String.format(" +inode:(%s) ", String.join(" OR ", partition));
        final String luceneQuery = inodeFilter + baseQuery;
        Logger.info(BrowserAPIImpl.class," Content-Drive Request Lucene Query: "+luceneQuery);
        final String esQuery = String.format(ES_QUERY_TEMPLATE, luceneQuery);
        try {
            esSearchAPI.esSearch(esQuery, live, browserQuery.user, false).forEach(result -> {
                final Contentlet contentlet = (Contentlet)result;
                 collectedInodes.add(contentlet.getInode());
            });

        } catch (Exception e) {
            Logger.error(this, String.format("Error while getting content from lucene with query: %s",luceneQuery), e);
        }
        return collectedInodes;
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

            for (final Contentlet contentlet : fromDB.contentlets) {
                final Map<String, Object> contentMap = hydrate(browserQuery, contentlet, roles);
                list.add(contentMap);
            }
        }

        // Final sorting (optional: maybe you only need to sort within each block before slicing)
        list.sort(new GenericMapFieldComparator(browserQuery.sortBy, browserQuery.sortByDesc));

        return new PaginatedContents(list, folderCount, contentTotalCount, contentCount);
    }

    /**
     *
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
        Map<String, Object> contentMap;
        final Optional<BaseContentType> baseType = contentlet.getBaseType();
        if (baseType.isPresent() && baseType.get() == BaseContentType.FILEASSET) {
            final FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
            contentMap = fileAssetMap(fileAsset);
        } else if (baseType.isPresent() && baseType.get() == BaseContentType.DOTASSET) {
            contentMap = dotAssetMap(contentlet);
        } else if (baseType.isPresent() &&  baseType.get() == BaseContentType.HTMLPAGE) {
            final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
            contentMap = htmlPageMap(page);
        } else {
            contentMap = dotContentMap(contentlet);
        }
        if (browserQuery.showShorties) {
            contentMap.put("shortyIdentifier", this.shortyIdAPI.shortify(contentlet.getIdentifier()));
            contentMap.put("shortyInode", this.shortyIdAPI.shortify(contentlet.getInode()));
        }
        final List<Integer> permissions = permissionAPI.getPermissionIdsFromRoles(contentlet, roles, browserQuery.user);
        final WfData wfdata = new WfData(contentlet, permissions, browserQuery.user, browserQuery.showArchived);
        contentMap.put("wfActionMapList", wfdata.wfActionMapList);
        contentMap.put("contentEditable", wfdata.contentEditable);
        contentMap.put("permissions", permissions);
        return contentMap;
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

        if (!browserQuery.getLanguageIds().isEmpty()) {
            appendLanguageQuery(selectQuery, browserQuery.getLanguageIds(),
                    browserQuery.showDefaultLangItems);
            appendLanguageQuery(countQuery, browserQuery.getLanguageIds(),
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

        // Im almost certain that Folder can't be null.
        // It is always calculated with some obscure logic that I don't want to break.
        // Therefore, I'm introducing this skipFolder flag
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

        Logger.info(this, "Select Query: " + selectQuery);
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
