package com.dotcms.publishing.job;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.ElasticsearchException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


/**
 * Even though this is expected to be an implementation of a Quartz Stateful Job
 * Which by default are guaranteed the run only one at the time (cluster wide).
 * Such warranty is based on the job name and as these jobs can be created dynamically and named by the user
 * It turnout that we can have sever instances of this same job type running concurrently for that reason this class needs to be thread-safe
 * And guaranteed that several different instances can co-exist and run concurrently without stepping on each others toes.
 * @See SiteSearchJobProxy (Quartz Stateful Job)
 */
public class SiteSearchJobImpl {

    static final String INCREMENTAL = "incremental";
    static final String LANG_TO_INDEX = "langToIndex";
    static final String INDEX_ALIAS = "indexAlias";
    static final String INDEX_ALL = "indexAll";
    static final String INDEX_HOST = "indexhost";
    static final String QUARTZ_JOB_NAME = "QUARTZ_JOB_NAME";
    static final String RUNNING_ONCE_JOB_NAME = "runningOnce";
    static final String JOB_ID = "JOB_ID";
    static final String RUN_NOW = "RUN_NOW";
    static final String INCLUDE_EXCLUDE = "includeExclude";
    static final String INCLUDE = "include";
    static final String PATHS = "paths";

    private final ESIndexAPI esIndexAPI;
    private final IndiciesAPI indicesAPI;
    private final SiteSearchAPI siteSearchAPI;
    private final HostAPI hostAPI;
    private final UserAPI userAPI;
    private final SiteSearchAuditAPI siteSearchAuditAPI;
    private final PublisherAPI publisherAPI;

    private String bundleId;

    @VisibleForTesting
    SiteSearchJobImpl(
            final ESIndexAPI esIndexAPI,
            final IndiciesAPI indicesAPI,
            final SiteSearchAPI siteSearchAPI,
            final HostAPI hostAPI,
            final UserAPI userAPI,
            final SiteSearchAuditAPI siteSearchAuditAPI,
            final PublisherAPI publisherAPI
            ) {
        this.esIndexAPI = esIndexAPI;
        this.indicesAPI = indicesAPI;
        this.siteSearchAPI = siteSearchAPI;
        this.hostAPI = hostAPI;
        this.userAPI = userAPI;
        this.siteSearchAuditAPI = siteSearchAuditAPI;
        this.publisherAPI = publisherAPI;
    }

    public SiteSearchJobImpl() {
        this(APILocator.getESIndexAPI(), APILocator.getIndiciesAPI(), APILocator.getSiteSearchAPI(),
                APILocator.getHostAPI(), APILocator.getUserAPI(),
                APILocator.getSiteSearchAuditAPI(), APILocator.getPublisherAPI());
    }

    private PublishStatus status = new PublishStatus();
    public PublishStatus getStatus() {
        return status;
    }
    public void setStatus(PublishStatus status) {
        this.status = status;
    }

    @VisibleForTesting
    public String getBundleId() {
        return bundleId;
    }

    @SuppressWarnings("unchecked")
    public void run(final JobExecutionContext jobContext)
            throws JobExecutionException, DotPublishingException, DotDataException, DotSecurityException, ElasticsearchException, IOException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            Logger.warn(this, "Invalid attempt to run SiteSearch job without a license.");
            return;
        }
        String date = DateUtil.getCurrentDate();
        ActivityLogger.logInfo(getClass(), "Job Started",
                "User:" + userAPI.getSystemUser().getUserId() + "; Date: " + date
                        + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME);
        AdminLogger.log(getClass(), "Job Started",
                "User:" + userAPI.getSystemUser().getUserId() + "; Date: " + date
                        + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME);

        HibernateUtil.startTransaction();
        try {
            final PreparedJobContext preparedJobContext = prepareJob(jobContext);
            synchronized (preparedJobContext.lockKey()) {
                for (final SiteSearchConfig config : preparedJobContext.getConfigs()) {
                    publisherAPI.publish(config, status);
                }

                try {

                    int filesCount = 0, pagesCount = 0, urlmapCount = 0;
                    for (final BundlerStatus bundlerStatus : status.getBundlerStatuses()) {
                        if (bundlerStatus.getBundlerClass()
                                .equals(FileAssetBundler.class.getName())) {
                            filesCount += bundlerStatus.getTotal();
                        } else if (bundlerStatus.getBundlerClass()
                                .equals(URLMapBundler.class.getName())) {
                            urlmapCount += bundlerStatus.getTotal();
                        } else if (bundlerStatus.getBundlerClass()
                                .equals(HTMLPageAsContentBundler.class.getName())) {
                            pagesCount += bundlerStatus.getTotal();
                        }
                    }

                    final SiteSearchAudit audit = new SiteSearchAudit();
                    audit.setPagesCount(pagesCount);
                    audit.setFilesCount(filesCount);
                    audit.setUrlmapsCount(urlmapCount);
                    audit.setAllHosts(preparedJobContext.isIndexAll());
                    audit.setFireDate(jobContext.getFireTime());
                    audit.setHostList(preparedJobContext.getJoinedHosts());
                    audit.setIncremental(preparedJobContext.isIncremental());
                    audit.setStartDate(preparedJobContext.getStartDate());
                    audit.setEndDate(preparedJobContext.getEndDate());
                    audit.setIndexName(
                            UtilMethods.isSet(preparedJobContext.getNewIndexName())
                                    ? preparedJobContext
                                    .getNewIndexName() : preparedJobContext.getIndexName());
                    audit.setJobId(preparedJobContext.getJobId());
                    audit.setJobName(preparedJobContext.getJobName());
                    audit.setLangList(preparedJobContext.getLangList());
                    audit.setPath(preparedJobContext.getPaths());
                    audit.setPathInclude(preparedJobContext.isPathInclude());
                    siteSearchAuditAPI.save(audit);

                } catch (DotDataException ex) {
                    Logger.error(this, "can't save audit data", ex);
                }
            }
        } finally {
            HibernateUtil.closeSession();
        }
        date = DateUtil.getCurrentDate();
        ActivityLogger.logInfo(getClass(), "Job Finished",
                "User: " + userAPI.getSystemUser().getUserId() + "; Date: " + date
                        + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME);
        AdminLogger.log(getClass(), "Job Finished",
                "User: " + userAPI.getSystemUser().getUserId() + "; Date: " + date
                        + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME);
    }

    private PreparedJobContext prepareJob(final JobExecutionContext jobContext)
            throws DotDataException, IOException, DotSecurityException {
        synchronized (SiteSearchJobImpl.class) {
            final JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
            String jobId = (String) dataMap.get(JOB_ID);
            if (jobId == null) {
                jobId = dataMap.getString(QUARTZ_JOB_NAME);
            }

            final boolean indexAll = UtilMethods.isSet((String) dataMap.get(INDEX_ALL));
            final String[] indexHosts;
            final Object obj =
                    (dataMap.get(INDEX_HOST) != null) ? dataMap.get(INDEX_HOST) : new String[0];
            if (obj instanceof String) {
                indexHosts = new String[]{(String) obj};
            } else {
                indexHosts = (String[]) obj;
            }

            final boolean incrementalParam = dataMap.getBooleanFromString(INCREMENTAL);

            final User userToRun = userAPI.getSystemUser();

            final boolean include = ("all".equals(dataMap.getString(INCLUDE_EXCLUDE)) || INCLUDE
                    .equals(dataMap.getString(INCLUDE_EXCLUDE)));

            String path = dataMap.getString(PATHS);
            final List<String> paths = new ArrayList<>();
            if (path != null) {
                path = path.replace(',', '\r');
                path = path.replace('\n', '\r');
                for (String pathSplit : path.split("\r")) {
                    if (UtilMethods.isSet(pathSplit)) {
                        paths.add(pathSplit.trim());
                    }
                }
            }
            final boolean isRunNowJob = dataMap.getBooleanFromString(RUN_NOW);
            // Run now jobs can not get the incremental treatment.
            final String indexAlias = getAliasName(dataMap.getString(INDEX_ALIAS));
            final IndexMetaData indexMetaData = getIndexMetaData(indexAlias);
            final String newIndexName;
            final String indexName;

            final String jobName = dataMap.getString(QUARTZ_JOB_NAME);
            final Date startDate, endDate;
            final List<SiteSearchAudit> recentAudits = isRunNowJob ? Collections.emptyList()
                    : siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);

            final boolean incremental = (incrementalParam && !isRunNowJob && !indexMetaData
                    .isNewIndex() && !indexMetaData.isEmpty() && !recentAudits.isEmpty());
            //We can only run incrementally if all the above pre-requisites are met.
            if (incremental) {
                //Incremental mode is useful only if there's already an index previously built.
                //Incremental mode also implies that we have to have a date range to work on.
                //So if we have an empty index or we lack of audit data we can not run incrementally.
                //Even if the user wants to.
                newIndexName = null;
                endDate = jobContext.getFireTime();
                startDate = recentAudits.get(0).getFireDate();
                //For incremental jobs, we write the bundle to the same folder every time.
                bundleId = StringUtils.camelCaseLower(jobName);
                //We'll be working directly into the final index.
                indexName = indexMetaData.getIndexName();
            } else {
                //Set null explicitly just in case
                startDate = endDate = null;
                // For non-incremental jobs. We create a new folder using a date stamp.
                // But even if this run was executed non-incrementally for not having met any of the pre-requisits
                // The job originally was meant to run incrementally therefore the results must be stored in the job specific folder.
                // So they will still be available in the next round.
                bundleId = incrementalParam ? StringUtils.camelCaseLower(jobName) :
                        // Otherwise it is safe to create a unique  folder name.
                        uniqueFolderName();
                // We use a new index name only on non-incremental
                newIndexName = newIndexName();
                final String newAlias =
                        indexMetaData.isNewIndex() ? indexMetaData.getAlias() : null;
                siteSearchAPI.createSiteSearchIndex(newIndexName, newAlias, 1);
                // This is the old index we will swap from.
                // if it doesnt exist. It doesnt matter here since we will end up with the new one.
                indexName = indexMetaData.getIndexName();
            }

            Logger.info(SiteSearchJobImpl.class, () -> String
                    .format("Incremental mode [%s]. current index is `%s`. new index is `%s`. alias is `%s`  bundle id is `%s` ",
                            BooleanUtils.toStringYesNo(incremental), indexName,
                            UtilMethods.isSet(newIndexName) ? newIndexName : "N/A",
                            indexAlias,
                            bundleId)
            );

            final List<Host> hosts;
            if (indexAll) {
                hosts = hostAPI.findAll(userToRun, true);
            } else {
                hosts = Stream.of(indexHosts).map(h -> {
                    try {
                        return hostAPI.find(h, userToRun, true);
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.error(SiteSearchJobImpl.class, e);
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
            }

            final Builder<SiteSearchConfig> builder = ImmutableList.builder();

            final List<String> languageToIndex = Arrays
                    .asList((String[]) dataMap.get(LANG_TO_INDEX));
            final ListIterator<String> listIterator = languageToIndex.listIterator();
            while (listIterator.hasNext()) {
                final String lang = listIterator.next();
                final SiteSearchConfig config = new SiteSearchConfig();
                config.setJobId(jobId);
                config.setLanguage(Long.parseLong(lang));
                config.setJobName(jobName);
                config.setHosts(hosts);
                config.setNewIndexName(newIndexName);
                config.setIndexName(indexName);
                config.setIndexAlias(indexAlias);
                config.setId(bundleId);
                config.setStartDate(startDate);
                config.setEndDate(endDate);
                config.setIncremental(incremental);
                config.setUser(userToRun);

                if (include) {
                    config.setIncludePatterns(paths);
                } else {
                    config.setExcludePatterns(paths);
                }

                //We should always replace the index when performing on non-incremental mode.
                //That means we drop the old one and re-use the alias.
                //But we only activate the new index when the old one was the default.
                //Or there wasn't any previous index.
                //it must be done on the last round of our loop.
                final boolean switchIndex = !incremental && !listIterator.hasNext();
                config.setSwitchIndexWhenDone(switchIndex);
                builder.add(config);
            }
            final String joinedHosts = UtilMethods.join(indexHosts, ",", true);
            final String langList = UtilMethods.join(languageToIndex, ",");
            final String pathsAsString = paths.size() > 0 ? UtilMethods.join(paths, ",") : "/*";
            return new PreparedJobContext(indexName, newIndexName, indexAll, joinedHosts,
                    incremental,
                    startDate, endDate, jobId, jobName, langList, pathsAsString, include,
                    builder.build()
            );
        }
    }

    /**
     * Unique thread safe site-search index name
     * @return
     */
    private String newIndexName() {
        return SiteSearchAPI.ES_SITE_SEARCH_NAME
                + StringPool.UNDERLINE
                + ESMappingAPIImpl.datetimeFormat.format(Instant.now())
                + StringPool.UNDERLINE
                + UUIDUtil.uuidTimeBased();
    }

    /**
     * unique threadsafe bundle folder name
     * @return
     */
    private String uniqueFolderName(){
        return  UUIDUtil.uuid() + "_" + UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
    }

    /***
     * Given an alias this tells you all you need to know about an index.
     * @param indexAlias
     * @return @see IndexMetaData
     * @throws DotDataException
     */
    private IndexMetaData getIndexMetaData(String indexAlias) throws DotDataException {
        String indexName = null;
        boolean defaultIndex = false;
        long recordCount = 0;
        if (UtilMethods.isSet(indexAlias)) {
            final List<String> indices = siteSearchAPI.listIndices();
            final Map<String, String> aliasMap = esIndexAPI.getAliasToIndexMap(indices);
            indexName = aliasMap.get(indexAlias);
            if (UtilMethods.isSet(indexName)) {
                if (siteSearchAPI.isDefaultIndex(indexAlias)) {
                    Logger.info(SiteSearchJobImpl.class, String.format("Index `%s` is currently Site-Search DEFAULT.",indexAlias));
                    defaultIndex = true;
                }
            } else {
                // the alias comes with an index name that is already in use.
                if(indices.contains(indexAlias)){
                   indexName = indexAlias;
                   indexAlias = null;
                }
            }
            if(UtilMethods.isSet(indexName)){
                final SiteSearchResults search = siteSearchAPI.search(indexName, "*",0, 10);
                recordCount = search.getTotalResults();
            }
        }//if indexName is null. Then the result is interpreted as a new index.
        return new IndexMetaData(indexName, defaultIndex, indexAlias, recordCount == 0);
    }

    private static final Pattern invalidAliasNamePattern = Pattern.compile("[^a-zA-Z0-9-_]");

    /**
     * This basically gets rid of the "(Default)" postfix and white spaces.
     * And applies a validation to make sure the job can run with the saved alias name.
     * The alias name is applied at the end of the execution so it is a good idea validating it ahead of time.
     * @param aliasName the alias stored in the quartz-job detail.
     * @return cleaned up alias string.
     */
    private String getAliasName(final String aliasName) throws DotDataException {
       if(UtilMethods.isSet(aliasName)){
          final String cleanedUpAlias = aliasName.split("\\s+")[0].trim();
          //This should grab only the first part of the alias name and drop the `(Default)` piece.
          if(invalidAliasNamePattern.matcher(cleanedUpAlias).matches()){
          //Since we're saving the alias in the quartz job detail we need to perform this cleanup before it runs.
             throw new DotDataException(String.format("Invalid Alias name `%s` ",aliasName));
          }
          return cleanedUpAlias;
       }
       return null;
    }

    static class IndexMetaData {

        private final String indexName;
        private final boolean defaultIndex;
        private final String alias;
        private final boolean empty;

        IndexMetaData(final String indexName,final boolean defaultIndex,final String alias,final boolean empty) {
            this.indexName = indexName;
            this.defaultIndex = defaultIndex;
            this.alias = alias;
            this.empty = empty;
        }

        String getIndexName() {
            return indexName;
        }

        boolean isDefaultIndex() {
            return defaultIndex && !isNewIndex();
        }

        String getAlias() {
            return alias;
        }

        boolean isNewIndex() {
            return UtilMethods.isNotSet(indexName);
        }

        public boolean isEmpty() {
            return empty;
        }
    }

    static class PreparedJobContext{

        private final String indexName;
        private final String newIndexName;
        private final boolean indexAll;
        private final String joinedHosts;
        private final boolean incremental;
        private final Date startDate;
        private final Date endDate;
        private final String jobId;
        private final String jobName;
        private final String langList;
        private final String paths;
        private final boolean pathInclude;
        private final List<SiteSearchConfig> configs;

        PreparedJobContext(
                final String indexName,
                final String newIndexName,
                final boolean indexAll,
                final String joinedHosts,
                final boolean incremental,
                final Date startDate,
                final Date endDate,
                final String jobId,
                final String jobName,
                final String langList,
                final String paths,
                final boolean pathInclude,
                final List<SiteSearchConfig> configs) {
            this.indexName = indexName;
            this.newIndexName = newIndexName;
            this.indexAll = indexAll;
            this.joinedHosts = joinedHosts;
            this.incremental = incremental;
            this.startDate = startDate;
            this.endDate = endDate;
            this.jobId = jobId;
            this.jobName = jobName;
            this.langList = langList;
            this.configs = configs;
            this.pathInclude = pathInclude;
            this.paths = paths;
        }

        String getIndexName() {
            return indexName;
        }

        List<SiteSearchConfig> getConfigs() {
            return configs;
        }

        String getNewIndexName() {
            return newIndexName;
        }

        boolean isIndexAll() {
            return indexAll;
        }

        String getJoinedHosts() {
            return joinedHosts;
        }

        boolean isIncremental() {
            return incremental;
        }

        Date getStartDate() {
            return startDate;
        }

        Date getEndDate() {
            return endDate;
        }

        String getJobId() {
            return jobId;
        }

        String getJobName() {
            return jobName;
        }

        String getLangList() {
            return langList;
        }

        public String getPaths() {
            return paths;
        }

        boolean isPathInclude() {
            return pathInclude;
        }

        String lockKey(){
           return ( UtilMethods.isSet(indexName) ? indexName  : newIndexName );
        }
    }

}
