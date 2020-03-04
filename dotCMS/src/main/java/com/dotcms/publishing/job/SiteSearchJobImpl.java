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
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.ElasticsearchException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
    public void run(final JobExecutionContext jobContext) throws JobExecutionException, DotPublishingException, DotDataException, DotSecurityException, ElasticsearchException, IOException {
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

        final JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();

        String jobId = (String) dataMap.get(JOB_ID);
        if (jobId == null) {
            jobId = dataMap.getString(QUARTZ_JOB_NAME);
        }

        final boolean indexAll = UtilMethods.isSet((String) dataMap.get(INDEX_ALL));
        final String[] indexHosts;
        final Object obj = (dataMap.get(INDEX_HOST) != null) ? dataMap.get(INDEX_HOST) : new String[0];
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
            for (String x : path.split("\r")) {
                if (UtilMethods.isSet(x)) {
                    paths.add(x);
                }
            }
        }
        final boolean isRunNowJob = dataMap.getBooleanFromString(RUN_NOW);
        // Run now jobs can not get the incremental treatment.
        final IndexMetaData indexMetaData = getIndexMetaData(dataMap.getString(INDEX_ALIAS));
        final String newIndexName;
        final String indexName;

        final String jobName = dataMap.getString(QUARTZ_JOB_NAME);
        final Date startDate, endDate;
        final List<SiteSearchAudit> recentAudits = isRunNowJob ? Collections.emptyList()
                : siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);

        final boolean incremental = (incrementalParam && !isRunNowJob && !indexMetaData.isNewIndex() && !indexMetaData.isEmpty() && !recentAudits.isEmpty());
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
            // Otherwise it is safe to create a unique time-stamp like folder name.
                       UtilMethods.dateToJDBC(new Date()).replace(':', '-').replace(' ', '_');
            // We use a new index name only on non-incremental
            newIndexName = newIndexName();
            final String newAlias = indexMetaData.isNewIndex() ? indexMetaData.getAlias() : null ;
            siteSearchAPI.createSiteSearchIndex(newIndexName, newAlias, 1);
            // This is the old index we will swap from.
            // if it doesnt exist. It doesnt matter here since we will end up with the new one.
            indexName = indexMetaData.getIndexName();
        }

        Logger.info(SiteSearchJobImpl.class, () -> String
                .format(" Incremental mode [%s]. current index is `%s`. new index is `%s`. bundle id is `%s` ",
                        BooleanUtils.toStringYesNo(incremental), indexName ,
                        UtilMethods.isSet(newIndexName) ? newIndexName : "N/A",
                        bundleId));

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

        final List<String> languageToIndex = Arrays.asList((String[])dataMap.get(LANG_TO_INDEX));
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
            config.setId(bundleId);
            config.setStartDate(startDate);
            config.setEndDate(endDate);
            config.setIncremental(incremental);
            config.setUser(userToRun);

            if(include) {
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
            publisherAPI.publish(config, status);
        }

        int filesCount = 0, pagesCount = 0, urlmapCount = 0;
        for (final BundlerStatus bs : status.getBundlerStatuses()) {
            if (bs.getBundlerClass().equals(FileAssetBundler.class.getName())) {
                filesCount += bs.getTotal();
            } else if (bs.getBundlerClass().equals(URLMapBundler.class.getName())) {
                urlmapCount += bs.getTotal();
            } else if (bs.getBundlerClass().equals(HTMLPageAsContentBundler.class.getName())) {
                pagesCount += bs.getTotal();
            }
        }

        try {
            final SiteSearchAudit audit = new SiteSearchAudit();
            audit.setPagesCount(pagesCount);
            audit.setFilesCount(filesCount);
            audit.setUrlmapsCount(urlmapCount);
            audit.setAllHosts(indexAll);
            audit.setFireDate(jobContext.getFireTime());
            audit.setHostList(UtilMethods.join(indexHosts,",",true));
            audit.setIncremental(incremental);
            audit.setStartDate(startDate);
            audit.setEndDate(endDate);
            audit.setIndexName( UtilMethods.isSet(newIndexName) ? newIndexName :  indexName );
            audit.setJobId(jobId);
            audit.setJobName(dataMap.getString(QUARTZ_JOB_NAME));
            audit.setLangList(UtilMethods.join(languageToIndex,","));
            audit.setPath(paths.size() > 0 ? UtilMethods.join(paths,",") : "/*");
            audit.setPathInclude(include);
            siteSearchAuditAPI.save(audit);
        }
        catch(DotDataException ex) {
            Logger.error(this, "can't save audit data",ex);
        }
        finally {
            HibernateUtil.closeSession();
        }

        date = DateUtil.getCurrentDate();
        ActivityLogger.logInfo(getClass(), "Job Finished", "User: " +userAPI.getSystemUser().getUserId()+ "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME  );
        AdminLogger.log(getClass(), "Job Finished", "User: " +userAPI.getSystemUser().getUserId()+ "; Date: " + date + "; Job Identifier: " + SiteSearchAPI.ES_SITE_SEARCH_NAME );
    }

     private String newIndexName(){
        return SiteSearchAPI.ES_SITE_SEARCH_NAME + StringPool.UNDERLINE
                + ESMappingAPIImpl.datetimeFormat.format(new Date());
     }

     private IndexMetaData getIndexMetaData(String indexAlias) throws DotDataException {
        String indexName = null;
        boolean defaultIndex = false;
        long recordCount = 0;
        if (UtilMethods.isSet(indexAlias)) {
            indexAlias = indexAlias.split("\\s+")[0];
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

}