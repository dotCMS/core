package com.dotcms.publishing.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotcms.content.index.migration.MirrorStatus;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.sitesearch.business.SiteSearchAuditAPI;
import com.dotmarketing.sitesearch.model.SiteSearchAudit;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static org.awaitility.Awaitility.await;

@RunWith(DataProviderRunner.class)
public class SiteSearchJobImplTest extends IntegrationTestBase {

    static long defaultLang;
    static SiteSearchAPI siteSearchAPI;
    static SiteSearchAuditAPI siteSearchAuditAPI;
    static ContentletAPI contentletAPI;
    static HostAPI hostAPI;

    private static String contentGenericId;
    private static Host site;
    private static Template template;
    private static Folder folder;
    private static User systemUser;
    private static Container container;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        systemUser = APILocator.systemUser();

        defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        siteSearchAPI = APILocator.getSiteSearchAPI();
        siteSearchAuditAPI = APILocator.getSiteSearchAuditAPI();
        contentletAPI = APILocator.getContentletAPI();
        hostAPI = APILocator.getHostAPI();

        site = new SiteDataGen().nextPersisted();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
        contentGenericId = contentGenericType.id();

        final String nameTitle = "myTestContainer" + System.currentTimeMillis();
        container = new ContainerDataGen()
                .withContentType(contentGenericType, "$!{body}")
                .friendlyName(nameTitle)
                .title(nameTitle)
                .nextPersisted();

        PublishFactory.publishAsset(container, systemUser, false, false);
        template = new TemplateDataGen().withContainer(container.getIdentifier()).nextPersisted();

        folder = new FolderDataGen().site(site).nextPersisted();

        PublishFactory.publishAsset(template, systemUser, false, false);

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);

        final String pageName = "our-page";

        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template).languageId(1)
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), contentlet1.getIdentifier() ,uuid,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        HTMLPageDataGen.publish(pageEnglishVersion);

    }

    /**
     * Clears every site-search index so each scenario starts from a known-empty state.
     *
     * <p>Deletes through {@link SiteSearchAPI#deleteIndex(String)} rather than the content-index
     * router: {@code listIndices()} aggregates the ES and OpenSearch sets during the dual-write
     * phases, so a name can exist on only one engine, and the content router's delete propagates
     * that engine's {@code index_not_found} instead of skipping it — aborting this loop. The
     * site-search delete is idempotent per engine. The active index is deactivated first, since
     * deleting it is (correctly) rejected.</p>
     */
    private static void deleteAllSiteSearchIndices() throws DotDataException, IOException {
        for (final String index : siteSearchAPI.listIndices()) {
            if (siteSearchAPI.isDefaultIndex(index)) {
                siteSearchAPI.deactivateIndex(index);
            }
            siteSearchAPI.deleteIndex(index);
        }
    }

    @Test
    public void Test_Non_Incremental_Create_Default_Index_Run_Non_Incrementally_Expect_New_Index_Keep_Alias_And_Default()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        deleteAllSiteSearchIndices();
        final String jobId = UUIDUtil.uuid();
        final String alias = "any-alias-"+System.currentTimeMillis();
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        //Even though we are requesting it to be incremental it'll run as non incremental because it is a run now type of job.
        jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, alias);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
        Mockito.when(context.getFireTime()).thenReturn(new Date());
        final SiteSearchJobImpl impl = new SiteSearchJobImpl();
        impl.run(context);

        final List<String> indicesAfterTest = siteSearchAPI.listIndices();
        Assert.assertFalse(indicesAfterTest.isEmpty());
        final String newIndexName = indicesAfterTest.get(0);

        // Resolve the alias through the site-search API, not the content-index router: the latter keys
        // OpenSearch entries by their .os-tagged physical name, so a logical name misses in the phases
        // where OpenSearch serves reads (issue #36360, see #36797).
        Assert.assertEquals("New index is expected have same Alias", newIndexName,
                siteSearchAPI.getAliasToIndexMap().get(alias));
        final SiteSearchResults search = siteSearchAPI.search(newIndexName, "*",0, 10);
        Assert.assertTrue(search.getTotalResults() >= 1);
        final List<SiteSearchAudit> recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);
        Assert.assertEquals(1, siteSearchAudit.getPagesCount());
        Assert.assertFalse(siteSearchAudit.isIncremental()); //did not run incrementally since it was launched as a run-now.
        Assert.assertTrue(siteSearchAPI.isDefaultIndex(newIndexName));
    }


    /**
     * Given a job whose stored {@code indexAlias} is a RAW INDEX NAME instead of an alias — what the
     * Site Search scheduler saved whenever its alias lookup missed on OpenSearch (issue #36983) —
     * when a full crawl runs, then the custom alias of that index must survive on the newly built
     * index, and the dead index's NAME must never become an alias.
     */
    @Test
    public void Test_Non_Incremental_Job_Stored_With_Raw_Index_Name_Expect_Custom_Alias_Preserved()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        deleteAllSiteSearchIndices();

        final long timeMillis = System.currentTimeMillis();
        final String customAlias = IndexType.SITE_SEARCH.getPrefix() + "-alias-" + timeMillis;
        final String originalIndexName = IndexType.SITE_SEARCH.getPrefix() + "_" + timeMillis;
        siteSearchAPI.createSiteSearchIndex(originalIndexName, customAlias, 1);

        final String jobId = UUIDUtil.uuid();
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
        // The defect: the index NAME where an alias is expected.
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, originalIndexName);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
        Mockito.when(context.getFireTime()).thenReturn(new Date());
        new SiteSearchJobImpl().run(context);

        final List<SiteSearchAudit> recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final String newIndexName = recentAudits.get(0).getIndexName();

        final Map<String, String> aliasToIndex = siteSearchAPI.getAliasToIndexMap();
        Assert.assertEquals("The custom alias must follow the crawl onto the new index",
                newIndexName, aliasToIndex.get(customAlias));
        Assert.assertFalse("The name of the replaced index must never become an alias",
                aliasToIndex.containsKey(originalIndexName));
    }

    @Test
    public void Test_Non_Incremental_Create_Default_Index_Create_Second_Index_Run_Non_Incrementally_Expect_Non_Default_New_Index()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        List<SiteSearchAudit> recentAudits;
        final SiteSearchJobImpl impl = new SiteSearchJobImpl();

        deleteAllSiteSearchIndices();

        final long timeMillis1 = System.currentTimeMillis();
        final String defaultAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" + timeMillis1;
        final String defaultIndexName = IndexType.SITE_SEARCH.getPrefix() + "_" + timeMillis1;
        siteSearchAPI.createSiteSearchIndex(defaultIndexName, defaultAlias, 1);
        siteSearchAPI.activateIndex(defaultIndexName); //Make it default.

        final String jobId1 = UUIDUtil.uuid();

        final JobDataMap jobDataMap1 = new JobDataMap();
        jobDataMap1.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap1.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
        jobDataMap1.put(SiteSearchJobImpl.INDEX_ALIAS, defaultAlias);
        jobDataMap1.put(SiteSearchJobImpl.JOB_ID, jobId1);
        jobDataMap1.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap1.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap1.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap1.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail1 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail1.getJobDataMap()).thenReturn(jobDataMap1);
        final JobExecutionContext context1 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context1.getJobDetail()).thenReturn(jobDetail1);
        Mockito.when(context1.getFireTime()).thenReturn(new Date());

        impl.run(context1);


        final List <String> indices = siteSearchAPI.listIndices();
        Assert.assertFalse(indices.contains(defaultIndexName));

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId1, 0, 1);

        //Since non-incremental runs will use its own brand new index
        //This is the only trustworthy way to retrieve the index name that was created.
        Assert.assertFalse(recentAudits.isEmpty());
        final String newDefaultIndexName = recentAudits.get(0).getIndexName();
        Assert.assertTrue( siteSearchAPI.isDefaultIndex(newDefaultIndexName));

        //Second index
        final long timeMillis2 = System.currentTimeMillis();
        final String newIndexAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" +timeMillis2;
        final String newIndexName =  IndexType.SITE_SEARCH.getPrefix() + "_" +timeMillis2;
        siteSearchAPI.createSiteSearchIndex(newIndexName, newIndexAlias, 1);

        final String jobId2 = UUIDUtil.uuid();

        final JobDataMap jobDataMap2 = new JobDataMap();
        jobDataMap2.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap2.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
        jobDataMap2.put(SiteSearchJobImpl.INDEX_ALIAS, newIndexAlias);
        jobDataMap2.put(SiteSearchJobImpl.JOB_ID, jobId2);
        jobDataMap2.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap2.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap2.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap2.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail2 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail2.getJobDataMap()).thenReturn(jobDataMap2);
        final JobExecutionContext context2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(context2.getFireTime()).thenReturn(new Date());

        impl.run(context2);
        //Second run will create the another index. But the first one should remain as default.

        //Check the original index is still the default
        Assert.assertTrue(siteSearchAPI.isDefaultIndex(newDefaultIndexName));

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId2, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);

        final SiteSearchResults search = siteSearchAPI.search(siteSearchAudit.getIndexName(), "*",0, 10);
        Assert.assertTrue(search.getTotalResults() >= 1);

    }

    @Test
    public void Test_Incremental_Job_Test_Pages_Are_Found_Create_And_Publish_New_Page_Test_Changes_Are_Picked_Unpublish_Then_Verify_Page_Is_Gone()
    throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        List<SiteSearchAudit> recentAudits;

        final String jobName = "Any-Scheduled-Job-" + System.currentTimeMillis();

        deleteAllSiteSearchIndices();

        final long timeMillis1 = System.currentTimeMillis();
        final String newIndexAlias = IndexType.SITE_SEARCH.getPrefix() + "_alias_" + timeMillis1;
        final String jobId = UUIDUtil.uuid();
        // As an incremental scheduled type of job.
        // Every single run has to be tied to the same job id.

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.FALSE.toString());
        jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, newIndexAlias);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, jobName);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail1 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail1.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context1 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context1.getJobDetail()).thenReturn(jobDetail1);
        Mockito.when(context1.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl firstRunJob = new SiteSearchJobImpl();
        firstRunJob.run(context1);

        String generatedBundleId1 = firstRunJob.getBundleId();
        Assert.assertNotNull(generatedBundleId1);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    final SiteSearchResults search1 = siteSearchAPI.search(siteSearchAudit.getIndexName(), "our-page*", 0, 10);
                    return search1.getTotalResults() >= 1;
                });

        final File runOnceBundleRoot = BundlerUtil.getBundleRoot(generatedBundleId1, false);
        Assert.assertTrue(runOnceBundleRoot.exists());

        // At this point we just finished dumping our content into a brand new index through a run once type of job.
        // From now on we will have to create an incremental job and continuously feed it.
        // The first time it'll have to create the whole thing since it's the first incremental run.

        final String incrementalJobBundleId = StringUtils.camelCaseLower(jobName);

        final JobDetail jobDetail2 = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail2.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(context2.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl secondRunJob = new SiteSearchJobImpl();
        secondRunJob.run(context2);

        generatedBundleId1 = secondRunJob.getBundleId();
        Assert.assertEquals(incrementalJobBundleId, generatedBundleId1);

        final File incrementalBundleRoot1 = BundlerUtil.getBundleRoot(generatedBundleId1, false);
        Assert.assertTrue(incrementalBundleRoot1.exists());

        final List<File> firstRunFiles = FileUtil.listFilesRecursively(incrementalBundleRoot1);
        Assert.assertFalse(firstRunFiles.isEmpty());

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit2 = recentAudits.get(0);

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    final SiteSearchResults search2 = siteSearchAPI.search(siteSearchAudit2.getIndexName(), "our-page*", 0, 10);
                    return search2.getTotalResults() >= 1;
                });

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);

        final String pageName = RandomStringUtils.randomAlphabetic(20);

        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template).languageId(defaultLang)
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();
        final String uuid = UUIDGenerator.generateUuid();
        final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), contentlet1.getIdentifier() ,uuid,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
        HTMLPageDataGen.publish(pageEnglishVersion);

        //Next Run should run completely incrementally
        final JobExecutionContext incrementalContext2 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(incrementalContext2.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(incrementalContext2.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl thirdRunJob = new SiteSearchJobImpl();
        thirdRunJob.run(incrementalContext2);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit3 = recentAudits.get(0);

        //Now we make sure the page is now part of Site-search search results.
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    final SiteSearchResults search3 = siteSearchAPI.search(siteSearchAudit3.getIndexName(), pageName, 0, 10);
                    return search3.getTotalResults() == 1;
                });

        final String generatedBundleId2 = thirdRunJob.getBundleId();

        Assert.assertEquals(generatedBundleId1, generatedBundleId2);

        final File incrementalBundleRoot2 = BundlerUtil.getBundleRoot(generatedBundleId2, false);
        Assert.assertTrue(incrementalBundleRoot1.exists());

        final List<File> secondRunFiles = FileUtil.listFilesRecursively(incrementalBundleRoot2);

        secondRunFiles.removeAll(firstRunFiles);

        final List<File> secondRunFilteredFiles = secondRunFiles.stream().filter(file -> !file.isDirectory())
                .collect(Collectors.toList());

        //This tests that only the files related to the new-page created by this test are the `new files` listed.
        Assert.assertTrue(secondRunFilteredFiles.stream().allMatch(file -> file.getName().contains(pageName)));

        //And now for my last trick I'm gonna un-publish the page that I just verified appears in the search-results.
        //Then re-run the job and make sure it's gone.

        DateUtil.sleep(6000L); //This sleep is freaking important. It allows a wider time difference between the last start-date. And the file asset last modified timestamp.

        HTMLPageDataGen.unpublish(pageEnglishVersion);
        //Next Run should run completely incrementally
        final JobExecutionContext incrementalContext3 = Mockito.mock(JobExecutionContext.class);
        Mockito.when(incrementalContext3.getJobDetail()).thenReturn(jobDetail2);
        Mockito.when(incrementalContext3.getFireTime()).thenReturn(new Date());

        final SiteSearchJobImpl fourthRunJob = new SiteSearchJobImpl();
        fourthRunJob.run(incrementalContext3);

        recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit4 = recentAudits.get(0);
        //Now we make sure the page is Not part of Site-search search results.
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    final SiteSearchResults search4 = siteSearchAPI.search(siteSearchAudit4.getIndexName(), pageName, 0, 10);
                    return search4.getTotalResults() == 0;
                });
    }

    static class TestCaseSiteSearch {
        boolean defaultPageToDefaultLanguage;
        boolean defaultContentToDefaultLanguage;
        boolean siteSearchDefaultLanguage;
        boolean siteSearchSecondLanguage;
        boolean siteSearchThirdLanguage;
        boolean createContentInDefaultLanguage;
        boolean createContentInSecondLanguage;
        boolean createContentInThirdLanguage;
        boolean createPageInDefaultLanguage;
        boolean createPageInSecondLanguage;
        boolean expectedResultsWhenSearchingContentInDefaultLanguage;
        boolean expectedResultsWhenSearchingContentInSecondLanguage;
        boolean expectedResultsWhenSearchingContentInThirdLanguage;

        public TestCaseSiteSearch(boolean defaultPageToDefaultLanguage,
                boolean defaultContentToDefaultLanguage, boolean siteSearchDefaultLanguage,
                boolean siteSearchSecondLanguage, boolean siteSearchThirdLanguage,
                boolean createContentInDefaultLanguage,
                boolean createContentInSecondLanguage, boolean createContentInThirdLanguage,
                boolean createPageInDefaultLanguage,
                boolean createPageInSecondLanguage,
                boolean expectedResultsWhenSearchingContentInDefaultLanguage,
                boolean expectedResultsWhenSearchingContentInSecondLanguage,
                boolean expectedResultsWhenSearchingContentInThirdLanguage) {
            this.defaultPageToDefaultLanguage = defaultPageToDefaultLanguage;
            this.defaultContentToDefaultLanguage = defaultContentToDefaultLanguage;
            this.siteSearchDefaultLanguage = siteSearchDefaultLanguage;
            this.siteSearchSecondLanguage = siteSearchSecondLanguage;
            this.siteSearchThirdLanguage = siteSearchThirdLanguage;
            this.createContentInDefaultLanguage = createContentInDefaultLanguage;
            this.createContentInSecondLanguage = createContentInSecondLanguage;
            this.createContentInThirdLanguage = createContentInThirdLanguage;
            this.createPageInDefaultLanguage = createPageInDefaultLanguage;
            this.createPageInSecondLanguage = createPageInSecondLanguage;
            this.expectedResultsWhenSearchingContentInDefaultLanguage = expectedResultsWhenSearchingContentInDefaultLanguage;
            this.expectedResultsWhenSearchingContentInSecondLanguage = expectedResultsWhenSearchingContentInSecondLanguage;
            this.expectedResultsWhenSearchingContentInThirdLanguage = expectedResultsWhenSearchingContentInThirdLanguage;
        }
    }

    @DataProvider
    public static Object[] siteSearchTestCases() {

        /*
         * Given sceneario: default-language content referenced from a page in default lang only. DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         * Create a Site-Search run only in default language
         * Expected: searching for the content in default language should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case1 = new TestCaseSiteSearchBuilder()
                .siteSearchDefaultLanguage(true)
                .createContentInDefaultLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInDefaultLanguage(true)
                .createTestCaseSiteSearch();

        /*
         * Given sceneario: default-language content referenced from a page in default lang only.
         * DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE=true
         * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         * Create a Site-Search run only in second language
         * Expected: searching for the content in default language should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case2 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(true)
                .defaultPageToDefaultLanguage(true)
                .siteSearchSecondLanguage(true)
                .createContentInDefaultLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInDefaultLanguage(true)
                .createTestCaseSiteSearch();

        /*

        /*
         * Given sceneario: default-language content referenced from a page in default lang only.
         * DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE=false
         * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         *
         * Create a Site-Search run only in second language
         * Expected: searching for the content in default language should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case3 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(false)
                .defaultPageToDefaultLanguage(true)
                .siteSearchSecondLanguage(true)
                .createContentInDefaultLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInDefaultLanguage(false)
                .createTestCaseSiteSearch();

        /*


         * Given sceneario: Content in second language only referenced from a page in default language.
         * DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE=true.
         * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         * Create a Site-Search run only in second language.
         * Expected: searching for content in second language should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case4 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(true)
                .defaultPageToDefaultLanguage(true)
                .siteSearchSecondLanguage(true)
                .createContentInSecondLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInSecondLanguage(true)
                .createTestCaseSiteSearch();


        /*
         * Same as above but with DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=false
         * Expected: searching for content in second language should give NO results.
         */

        TestCaseSiteSearch case5 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(true)
                .defaultPageToDefaultLanguage(false)
                .siteSearchSecondLanguage(true)
                .createContentInSecondLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInSecondLanguage(false)
                .createTestCaseSiteSearch();

        /*
         * Given sceneario: two-language (default and second) content referenced from a page in default lang only. DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         * Create a Site-Search run including both languages
         * Expected: searching content of either version of the content should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case6 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(true)
                .defaultPageToDefaultLanguage(true)
                .siteSearchDefaultLanguage(true)
                .siteSearchSecondLanguage(true)
                .createContentInDefaultLanguage(true)
                .createContentInSecondLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInSecondLanguage(true)
                .createTestCaseSiteSearch();


        /*
         * Given sceneario: two-language (second and third) content referenced from a page in default lang only. DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true.
         * Create a Site-Search run including both languages
         * Expected: searching content of either version of the content should give results. The result should be the page in the default language
         */

        TestCaseSiteSearch case7 = new TestCaseSiteSearchBuilder()
                .defaultContentToDefaultLanguage(true)
                .defaultPageToDefaultLanguage(true)
                .siteSearchSecondLanguage(true)
                .siteSearchThirdLanguage(true)
                .createContentInSecondLanguage(true)
                .createContentInThirdLanguage(true)
                .createPageInDefaultLanguage(true)
                .expectedResultsWhenSearchingContentInSecondLanguage(true)
                .expectedResultsWhenSearchingContentInThirdLanguage(true)
                .createTestCaseSiteSearch();


        return new TestCaseSiteSearch[] {case1, case2, case3, case4, case5, case6, case7};
    }

    @UseDataProvider("siteSearchTestCases")
    @Test
    public void testSiteSearchDifferentScenarios(final TestCaseSiteSearch testCase)
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException, WebAssetException {

        boolean defaultContentToDefaultLangOriginalValue =
                Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);

        boolean defaultPagetoDefaultLangOriginalValue =
                Config.getBooleanProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true);

        try {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", testCase.defaultContentToDefaultLanguage);
            Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", testCase.defaultPageToDefaultLanguage);

            final Host site = new SiteDataGen().nextPersisted();
            Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
            Language secondLang = new LanguageDataGen().nextPersisted();
            Language thirdLang = new LanguageDataGen().nextPersisted();
            folder = new FolderDataGen().site(site).nextPersisted();

            Contentlet contentletDefaultLang = null;
            Contentlet contentSecondLang = null;

            if(testCase.createContentInDefaultLanguage) {
                contentletDefaultLang = createAndPublishEmployeeContent(site, defaultLang, "catherine");

                if(testCase.createContentInSecondLanguage) {
                    createNewVersionAndPublishExistingEmployeeContent(secondLang, contentletDefaultLang,
                            "catalina");
                }
            } else if(testCase.createContentInSecondLanguage) {
                contentSecondLang = createAndPublishEmployeeContent(site, secondLang, "catalina");

                if(testCase.createContentInThirdLanguage) {
                    createNewVersionAndPublishExistingEmployeeContent(thirdLang, contentSecondLang,
                            "caterina");
                }
            }

            Contentlet contentToPassToPage = contentletDefaultLang!=null
                    ? contentletDefaultLang
                    : contentSecondLang;

            HTMLPageAsset pageDefaultLang = createHtmlPageAsset(defaultLang, contentToPassToPage);

            deleteAllSiteSearchIndices();
            final String jobId = UUIDUtil.uuid();
            final String alias = "any-alias-" + System.currentTimeMillis();
            final JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
            jobDataMap.put(SiteSearchJobImpl.INCREMENTAL, Boolean.FALSE.toString());
            jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, alias);
            jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
            jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME,
                    SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
            jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");


            List<String> langsToIndex = new ArrayList<>();
            // change the order in purpose to test a case of the wrong document making it into the sitesearch index
            if(testCase.siteSearchThirdLanguage) {
                langsToIndex.add(Long.toString(thirdLang.getId()));
            }
            if(testCase.siteSearchSecondLanguage) {
                langsToIndex.add(Long.toString(secondLang.getId()));
            }
            if(testCase.siteSearchDefaultLanguage) {
                langsToIndex.add(Long.toString(defaultLang.getId()));
            }

            jobDataMap
                    .put(SiteSearchJobImpl.LANG_TO_INDEX, langsToIndex.toArray(new String[0]));
            jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

            final JobDetail jobDetail = Mockito.mock(JobDetail.class);
            Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
            final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
            Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
            Mockito.when(context.getFireTime()).thenReturn(new Date());
            final SiteSearchJobImpl impl = new SiteSearchJobImpl();
            impl.run(context);

            final List<String> indicesAfterTest = siteSearchAPI.listIndices();
            Assert.assertFalse(indicesAfterTest.isEmpty());
            final String newIndexName = indicesAfterTest.get(0);


            if(testCase.expectedResultsWhenSearchingContentInDefaultLanguage) {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "catherine", 0, 10);
                Assert.assertTrue("Content in default Language gives results",
                        searchResults.getTotalResults() >= 1);
                Assert.assertEquals(pageDefaultLang.getTitle(),
                        searchResults.getResults().get(0).getTitle());
                Assert.assertEquals(pageDefaultLang.getLanguageId(),
                        searchResults.getResults().get(0).getLanguage());
            } else {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "catherine", 0, 10);
                Assert.assertEquals("Content in default Language gives NO results", 0,
                        searchResults.getTotalResults());
            }

            if(testCase.expectedResultsWhenSearchingContentInSecondLanguage) {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "catalina", 0, 10);
                Assert.assertTrue("Content in second Language gives results",
                        searchResults.getTotalResults() >= 1);
                Assert.assertEquals(pageDefaultLang.getTitle(),
                        searchResults.getResults().get(0).getTitle());
                Assert.assertEquals(pageDefaultLang.getLanguageId(),
                        searchResults.getResults().get(0).getLanguage());
            } else {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "catalina", 0, 10);
                Assert.assertEquals("Content in second Language gives NO results", 0,
                        searchResults.getTotalResults());
            }

            if(testCase.expectedResultsWhenSearchingContentInThirdLanguage) {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "caterina", 0, 10);
                Assert.assertTrue("Content in third Language gives results",
                        searchResults.getTotalResults() >= 1);
                Assert.assertEquals(pageDefaultLang.getTitle(),
                        searchResults.getResults().get(0).getTitle());
                Assert.assertEquals(pageDefaultLang.getLanguageId(),
                        searchResults.getResults().get(0).getLanguage());
            } else {
                SiteSearchResults searchResults = siteSearchAPI.search(newIndexName, "caterina", 0, 10);
                Assert.assertEquals("Content in third Language gives NO results", 0,
                        searchResults.getTotalResults());
            }

        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",
                    defaultContentToDefaultLangOriginalValue);

            Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE",
                    defaultPagetoDefaultLangOriginalValue);
        }
    }

    private void createNewVersionAndPublishExistingEmployeeContent(Language newLang,
            Contentlet contentToCheckout, String firstName) throws DotDataException, DotSecurityException {

        Contentlet contentInNewLang = contentletAPI
                .checkout(contentToCheckout.getInode(), systemUser, false);

        contentInNewLang.setLanguageId(newLang.getId());
        contentInNewLang.setStringProperty("firstName", firstName);
        contentInNewLang = contentletAPI.checkin(contentInNewLang, systemUser, false);
        ContentletDataGen.publish(contentInNewLang);
    }

    private Contentlet createAndPublishEmployeeContent(Host site, Language language,
            String firstName) throws DotDataException, DotSecurityException {
        Contentlet contentletDefaultLang = TestDataUtils
                .getEmployeeContent(true, language.getId(), null, site);

        contentletDefaultLang = contentletAPI
                .find(contentletDefaultLang.getInode(), systemUser, false);
        contentletDefaultLang.setStringProperty("firstName", firstName);
        contentletDefaultLang = contentletAPI.checkin(contentletDefaultLang, systemUser, false);
        ContentletDataGen.publish(contentletDefaultLang);
        return contentletDefaultLang;
    }

    private HTMLPageAsset createHtmlPageAsset(Language lang1, Contentlet contentlet)
            throws DotSecurityException, WebAssetException, DotDataException {
        final Container container = new ContainerDataGen().withContentType(contentlet
                .getContentType(), "$!{firstName}").nextPersisted();

        ContainerDataGen.publish(container);

        final String uuid = UUIDGenerator.generateUuid();

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), uuid)
                .nextPersisted();

        TemplateDataGen.publish(template);

        HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(lang1.getId())
                .nextPersisted();

        HTMLPageDataGen.publish(page);

        final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                container.getIdentifier(),
                contentlet.getIdentifier(), getDotParserContainerUUID(uuid), 0);

        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        return page;
    }

    // ------------------------------------------------------------------------------------------
    // Issue #37321 — the crawl must not hold a pooled connection or an open transaction.
    //
    // The job used to call HibernateUtil.startTransaction() before the crawl and close the session
    // only in the finally, so one pooled connection was leased for the crawl's whole duration —
    // hours on a large site. HikariCP never validates an in-use connection and pgjdbc's
    // tcpKeepAlive is off, so a stateful network device could evict the idle TCP flow and the
    // closing audit INSERT would fail on a dead socket. The failure was then swallowed and the job
    // reported "Job Finished".
    //
    // These tests assert the two properties that fix it, at the two moments that matter. They use
    // the @VisibleForTesting constructor to inject observers rather than driving a real multi-hour
    // crawl: the defect is about *what the thread holds*, which is observable in milliseconds.
    // ------------------------------------------------------------------------------------------

    /**
     * Builds the Quartz context a run-now crawl needs, mirroring the setup the scenarios above use.
     *
     * @param jobId job identifier the audit row is keyed by
     * @param alias index alias the crawl should end up with
     * @return a mock {@link JobExecutionContext} carrying the job detail and fire time
     */
    private JobExecutionContext runNowContext(final String jobId, final String alias) {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, alias);
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());

        final JobDetail jobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
        Mockito.when(context.getFireTime()).thenReturn(new Date());
        return context;
    }

    /**
     * A publisher that does no crawling and instead records what the thread was holding when the
     * crawl would have started. Standing in for the real publisher is the whole point: on a real
     * site this call is where the hours go, and it is precisely then that a held connection goes
     * quiet long enough to be evicted.
     *
     * @param connectionHeld set to whether a pooled connection existed at crawl time
     * @param transactionHeld set to whether a transaction was open at crawl time
     * @return a {@link PublisherAPI} that records and returns the status it was handed
     */
    private PublisherAPI crawlTimeObserver(final AtomicBoolean connectionHeld,
            final AtomicBoolean transactionHeld) throws DotPublishingException {
        final PublisherAPI publisherAPI = Mockito.mock(PublisherAPI.class);
        Mockito.when(publisherAPI.publish(Mockito.any(PublisherConfig.class),
                        Mockito.any(PublishStatus.class)))
                .thenAnswer(invocation -> {
                    connectionHeld.set(DbConnectionFactory.connectionExists());
                    transactionHeld.set(DbConnectionFactory.inTransaction());
                    return invocation.getArgument(1);
                });
        return publisherAPI;
    }

    /**
     * AC-001 — nothing is held while the crawl runs.
     *
     * <p>Before the fix both assertions fail: {@code startTransaction()} at the top of {@code run()}
     * leases a connection and sets {@code autoCommit=false}, and both survive until
     * {@code closeSession()} in the finally. After it, {@code prepareJob()}'s reads each close their
     * own connection through {@code @CloseDBIfOpened}, so the thread holds nothing by the time the
     * publisher is called.</p>
     *
     * <p>The two conditions are asserted separately because they fail independently and mean
     * different things: an open transaction is what stalls autovacuum database-wide, while a held
     * connection is what dies silently behind a firewall.</p>
     */
    @Test
    public void Test_Crawl_Runs_With_No_Connection_And_No_Transaction_Held()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException,
            DotSecurityException {

        deleteAllSiteSearchIndices();

        final AtomicBoolean connectionHeld = new AtomicBoolean(true);
        final AtomicBoolean transactionHeld = new AtomicBoolean(true);

        final SiteSearchJobImpl impl = new SiteSearchJobImpl(
                APILocator.getIndiciesAPI(), siteSearchAPI, hostAPI, APILocator.getUserAPI(),
                siteSearchAuditAPI, crawlTimeObserver(connectionHeld, transactionHeld));

        impl.run(runNowContext(UUIDUtil.uuid(), "no-held-conn-" + System.currentTimeMillis()));

        Assert.assertFalse(
                "The crawl must not run with a pooled connection held: on a long crawl that "
                        + "connection goes quiet and can be evicted by a stateful network device, "
                        + "killing the audit insert that follows (issue #37321)",
                connectionHeld.get());
        Assert.assertFalse(
                "The crawl must not run inside an open transaction: holding one for the duration "
                        + "of the crawl pins the xmin horizon and stalls autovacuum database-wide "
                        + "(issue #37321)",
                transactionHeld.get());
    }

    /**
     * AC-002 — the audit save begins a transaction of its own.
     *
     * <p>This is the property that makes the row survive a connection that died during the crawl.
     * {@code SiteSearchAuditAPIImpl.save()} is {@code @WrapInTransaction}, but that only opens a
     * fresh transaction — on a freshly leased, pool-validated connection — when none is already
     * open. While the job held one, the nested call joined it instead and inherited its dead
     * socket.</p>
     *
     * <p>Killing a real backend mid-crawl is not reproducible in CI, so this asserts the mechanism
     * rather than the symptom: no transaction open on entry to {@code save()} means
     * {@code @WrapInTransaction} is about to start one. The symptom itself is covered by the manual
     * reproduction in the feature's quickstart.</p>
     */
    @Test
    public void Test_Audit_Save_Starts_Its_Own_Transaction()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException,
            DotSecurityException {

        deleteAllSiteSearchIndices();

        final AtomicBoolean inTransactionAtSave = new AtomicBoolean(true);
        final AtomicBoolean saveWasCalled = new AtomicBoolean(false);

        final SiteSearchAuditAPI auditObserver = Mockito.mock(SiteSearchAuditAPI.class);
        Mockito.when(auditObserver.findRecentAudits(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyInt())).thenReturn(new ArrayList<>());
        Mockito.doAnswer(invocation -> {
            saveWasCalled.set(true);
            inTransactionAtSave.set(DbConnectionFactory.inTransaction());
            return null;
        }).when(auditObserver).save(Mockito.any(SiteSearchAudit.class));

        final AtomicBoolean ignoredConnection = new AtomicBoolean();
        final AtomicBoolean ignoredTransaction = new AtomicBoolean();

        final SiteSearchJobImpl impl = new SiteSearchJobImpl(
                APILocator.getIndiciesAPI(), siteSearchAPI, hostAPI, APILocator.getUserAPI(),
                auditObserver, crawlTimeObserver(ignoredConnection, ignoredTransaction));

        impl.run(runNowContext(UUIDUtil.uuid(), "own-tx-" + System.currentTimeMillis()));

        Assert.assertTrue("The job must still attempt to save the audit row", saveWasCalled.get());
        Assert.assertFalse(
                "The audit save must not inherit an already-open transaction: @WrapInTransaction "
                        + "only leases a fresh, pool-validated connection when none is open, and "
                        + "that is what lets the insert survive a socket that died during the "
                        + "crawl (issue #37321)",
                inTransactionAtSave.get());
    }

    /**
     * Builds an audit API that fails the way a dead socket fails it.
     *
     * @param failure the exception {@code save} should throw
     * @return a {@link SiteSearchAuditAPI} with no recent audits and a failing {@code save}
     */
    private SiteSearchAuditAPI failingAuditAPI(final DotDataException failure)
            throws DotDataException {
        final SiteSearchAuditAPI auditAPI = Mockito.mock(SiteSearchAuditAPI.class);
        Mockito.when(auditAPI.findRecentAudits(Mockito.anyString(), Mockito.anyInt(),
                Mockito.anyInt())).thenReturn(new ArrayList<>());
        Mockito.doThrow(failure).when(auditAPI).save(Mockito.any(SiteSearchAudit.class));
        return auditAPI;
    }

    /**
     * AC-003 — a failed audit save fails the job.
     *
     * <p>The audit row is the checkpoint an incremental crawl anchors its delta on. Losing it costs
     * the next run a full rebuild, so a run that lost it has not succeeded. Before the fix the
     * failure was caught, logged as "can't save audit data" and dropped, and the job went on to log
     * "Job Finished" — which is why this went unnoticed in production until someone read the logs
     * closely.</p>
     *
     * <p>The exception must arrive intact rather than wrapped or replaced: whoever reads the job
     * result needs to see which insert failed and why — including, via this same path, the separate
     * {@code path varchar(500)} overflow of issue #36706.</p>
     */
    @Test
    public void Test_Failed_Audit_Save_Propagates_Out_Of_Run()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException,
            DotSecurityException {

        deleteAllSiteSearchIndices();

        final DotDataException auditFailure =
                new DotDataException("simulated audit insert failure (issue #37321)");
        final AtomicBoolean ignoredConnection = new AtomicBoolean();
        final AtomicBoolean ignoredTransaction = new AtomicBoolean();

        final SiteSearchJobImpl impl = new SiteSearchJobImpl(
                APILocator.getIndiciesAPI(), siteSearchAPI, hostAPI, APILocator.getUserAPI(),
                failingAuditAPI(auditFailure),
                crawlTimeObserver(ignoredConnection, ignoredTransaction));

        try {
            impl.run(runNowContext(UUIDUtil.uuid(), "failing-audit-" + System.currentTimeMillis()));
            Assert.fail("A failed audit save must fail the job, not be swallowed and reported as "
                    + "\"Job Finished\" (issue #37321)");
        } catch (final DotDataException expected) {
            Assert.assertSame(
                    "The audit failure must reach the caller intact, so the job result says which "
                            + "insert failed and why", auditFailure, expected);
        }
    }

    /**
     * AC-003, ordering half — the failure is raised only after the crawl has finished.
     *
     * <p>Failing the job must not cost the index work that already succeeded. The crawl writes to
     * the search engine, which no Postgres transaction covers and which nothing here rolls back; a
     * failed audit save leaves the index exactly as a successful one would, and only the checkpoint
     * is missing. This asserts that ordering directly: the publisher ran before the throw.</p>
     *
     * <p>Not asserted here: that {@code SiteSearchJobProxy} turns this into a
     * {@link JobExecutionException}. The proxy builds its own {@code SiteSearchJobImpl} with no
     * injection seam, so a test could only prove it by mocking the very thing it claims to verify.
     * That wrapping is pre-existing behavior this fix does not touch — it catches {@code Exception}
     * and rethrows as {@code JobExecutionException} — and adding a seam to production code purely
     * to assert it would exceed the scope of this fix.</p>
     */
    @Test
    public void Test_Audit_Failure_Is_Raised_After_The_Crawl_Completes()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException,
            DotSecurityException {

        deleteAllSiteSearchIndices();

        final AtomicBoolean crawlCompleted = new AtomicBoolean(false);
        final PublisherAPI publisherAPI = Mockito.mock(PublisherAPI.class);
        Mockito.when(publisherAPI.publish(Mockito.any(PublisherConfig.class),
                        Mockito.any(PublishStatus.class)))
                .thenAnswer(invocation -> {
                    crawlCompleted.set(true);
                    return invocation.getArgument(1);
                });

        final SiteSearchJobImpl impl = new SiteSearchJobImpl(
                APILocator.getIndiciesAPI(), siteSearchAPI, hostAPI, APILocator.getUserAPI(),
                failingAuditAPI(new DotDataException("simulated audit insert failure")),
                publisherAPI);

        try {
            impl.run(runNowContext(UUIDUtil.uuid(), "after-crawl-" + System.currentTimeMillis()));
            Assert.fail("A failed audit save must fail the job (issue #37321)");
        } catch (final DotDataException expected) {
            Assert.assertTrue(
                    "The crawl must have completed before the audit failure is raised, so failing "
                            + "the job never leaves the index in a worse state than a successful "
                            + "run would (issue #37321)",
                    crawlCompleted.get());
        }
    }

    /**
     * AC-001, the branch the other connection test cannot reach — the pre-crawl index-completeness
     * check must not leave a connection bound to the thread.
     *
     * <p>{@code incompleteContentIndexWarning()} is advisory and off by default
     * ({@code SITE_SEARCH_CRAWL_MIN_CONTENT_INDEXED_PERCENT} defaults to 0), so it returns before
     * touching the database and
     * {@link #Test_Crawl_Runs_With_No_Connection_And_No_Transaction_Held()} never exercises it. Turn
     * it on during a migration and it reaches {@code ContentIndexMirrorReconciler.statuses()}, whose
     * last step is an unannotated {@code new DotConnect().setSQL(...).loadObjectResults()} —
     * {@code DotConnect} leases from the pool and never closes, relying on an enclosing
     * {@code @CloseDBIfOpened} that does not exist on that path.</p>
     *
     * <p>That matters here more than anywhere else in the job: the check runs immediately before the
     * publish loop, so a connection left bound is pinned for the whole crawl — the exact shape of
     * issue #37321, a few lines below where it was removed. Worse, it defeats the fix downstream:
     * {@code publish()}'s {@code @CloseDBIfOpened} sees {@code isNewConnection == false} and skips
     * the close, and the audit save's {@code @WrapInTransaction} then works on that same
     * possibly-dead connection instead of leasing a fresh, pool-validated one.</p>
     *
     * <p>The supplier injected here stands in for the reconciler by doing the one thing that
     * matters — an unannotated read — rather than requiring a real migration with live mirrors.</p>
     */
    @Test
    public void Test_Index_Check_Leaves_No_Connection_Bound_For_The_Crawl() {

        // Exactly what ContentIndexMirrorReconciler.loadDatabaseCountsQuietly() does: a bare
        // DotConnect with no transaction or connection boundary of its own.
        final Supplier<List<MirrorStatus>> leakingStatuses = () -> {
            try {
                new DotConnect().setSQL("select 1 as one").loadObjectResults();
            } catch (final DotDataException e) {
                throw new IllegalStateException(e);
            }
            return new ArrayList<>();
        };

        // Both are required to get past the early returns in incompleteContentIndexWarning():
        // a positive threshold, and a migration that has actually started. Only the check itself is
        // invoked — deliberately not a whole crawl, because running one under a non-zero migration
        // phase pulls OpenSearch into a test that has nothing to do with it.
        Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_INDEXED_KEY, "1");
        Config.setProperty(MigrationPhase.FLAG_KEY, "1");
        try {
            DbConnectionFactory.closeSilently();

            final SiteSearchJobImpl impl = new SiteSearchJobImpl(
                    APILocator.getIndiciesAPI(), siteSearchAPI, hostAPI, APILocator.getUserAPI(),
                    siteSearchAuditAPI, Mockito.mock(PublisherAPI.class), leakingStatuses);

            impl.incompleteContentIndexWarning();

            Assert.assertFalse(
                    "The pre-crawl index-completeness check must not leave a connection bound: it "
                            + "runs immediately before the publish loop, so anything it leaves "
                            + "behind is held for the whole crawl, and the audit save then inherits "
                            + "that connection instead of leasing a fresh one (issue #37321)",
                    DbConnectionFactory.connectionExists());
        } finally {
            Config.setProperty(SiteSearchJobImpl.MIN_CONTENT_INDEXED_KEY, null);
            Config.setProperty(MigrationPhase.FLAG_KEY, null);
            HibernateUtil.closeSessionSilently();
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * AC-003, proxy half — a failed audit save surfaces as a Quartz job failure.
     *
     * <p>This is the assertion the rest of the suite could not make without mocking the thing under
     * test: {@link SiteSearchJobProxy} builds its own {@code SiteSearchJobImpl}, so there is nowhere
     * to inject a failing audit API. Instead the failure is induced for real, the same way
     * production hits it — the {@code path} column is {@code varchar(500)} and nothing truncates
     * what the job writes into it, so an over-long path makes the genuine audit insert fail
     * (issue #36706). No mock stands between the crawl and the database here.</p>
     *
     * <p>What it proves end to end: the exception leaves {@code SiteSearchJobImpl.run()}, the proxy
     * turns it into a {@link JobExecutionException} — which is what Quartz records as a failed run —
     * and no audit row exists afterwards. Before the fix this same setup logged "can't save audit
     * data", wrote no row, and reported "Job Finished" (issue #37321).</p>
     *
     * <p>Not asserted: the absence of the "Job Finished" {@code ActivityLogger}/{@code AdminLogger}
     * entries. Those calls sit after the {@code try}/{@code finally} in {@code run()}, so a
     * propagating exception cannot reach them — a structural guarantee that would need log parsing
     * to restate. The missing audit row asserted below is the observable consequence that actually
     * matters.</p>
     */
    @Test
    public void Test_Proxy_Reports_Failed_Audit_Save_As_Job_Execution_Exception()
            throws DotDataException, IOException {

        deleteAllSiteSearchIndices();

        final String jobId = UUIDUtil.uuid();
        // sitesearch_audit.path is varchar(500) and the job does not truncate (issue #36706),
        // so this is a real insert failure, not a simulated one.
        final String overlongPath = "/" + RandomStringUtils.randomAlphabetic(600);

        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(SiteSearchJobImpl.RUN_NOW, Boolean.TRUE.toString());
        jobDataMap.put(SiteSearchJobImpl.INDEX_ALIAS, "proxy-fail-" + System.currentTimeMillis());
        jobDataMap.put(SiteSearchJobImpl.JOB_ID, jobId);
        jobDataMap.put(SiteSearchJobImpl.QUARTZ_JOB_NAME, SiteSearchJobImpl.RUNNING_ONCE_JOB_NAME);
        jobDataMap.put(SiteSearchJobImpl.INCLUDE_EXCLUDE, "all");
        jobDataMap.put(SiteSearchJobImpl.LANG_TO_INDEX, new String[]{Long.toString(defaultLang)});
        jobDataMap.put(SiteSearchJobImpl.INDEX_HOST, site.getIdentifier());
        jobDataMap.put(SiteSearchJobImpl.PATHS, overlongPath);

        final JobDetail jobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        Mockito.when(jobDetail.getName()).thenReturn("site-search-proxy-" + jobId);
        Mockito.when(jobDetail.getGroup()).thenReturn("site-search-proxy-test");
        final JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
        Mockito.when(context.getFireTime()).thenReturn(new Date());

        try {
            new SiteSearchJobProxy().run(context);
            Assert.fail("A crawl whose audit row cannot be written must be reported as a failed "
                    + "Quartz job, not finish silently (issue #37321)");
        } catch (final JobExecutionException expected) {
            // Deliberately specific. A bare non-null check would also pass if the crawl itself had
            // failed for an unrelated reason, since the proxy wraps every exception the same way —
            // the test would go green while proving nothing about the audit path.
            final Throwable cause = expected.getCause();
            Assert.assertTrue(
                    "The JobExecutionException must carry the audit insert failure itself, not "
                            + "some earlier crawl error; got: " + cause,
                    cause instanceof DotDataException
                            && cause.getMessage() != null
                            && cause.getMessage().contains("sitesearch_audit"));
        }

        Assert.assertTrue(
                "No audit row may exist for a run that failed to write one — this is the missing "
                        + "checkpoint that silently forced the next crawl into a full rebuild "
                        + "(issue #37321)",
                siteSearchAuditAPI.findRecentAudits(jobId, 0, 1).isEmpty());
    }

    /**
     * AC-005 — the crawl phase must stay read-only against Postgres.
     *
     * <p>Removing the job's transaction is only safe while the crawl writes nothing. The one write
     * reachable from the bundlers — {@code FileAssetBundler}'s
     * {@code pushedAssetUtil.savePushedAssetForAllEnv(...)} — is gated behind
     * {@link com.dotcms.publishing.PublisherConfig#shouldManageDependencies()}, which returns
     * {@link com.dotcms.publishing.PublisherConfig#isStatic()}. {@link SiteSearchConfig} never sets
     * it, so Site Search takes the non-static branch and that write is unreachable.</p>
     *
     * <p>Nothing in the type system says so, and a later {@code setStatic(true)} — for a
     * static-publishing feature, say — would silently put a per-asset write back inside a crawl that
     * no longer has a transaction around it. This is what that change runs into first. If it fails,
     * do not just update the expectation: the job's transaction boundaries assume a read-only crawl,
     * and that assumption needs revisiting before a write goes back in (see {@code run()}'s
     * Javadoc).</p>
     *
     * <p>Lives here rather than in a pure unit test because {@code PublisherConfig}'s constructor
     * calls {@code APILocator.getLanguageAPI().getDefaultLanguage()}, which needs a live context —
     * a plain unit test fails with {@code DotRuntimeException: No Company!}. Sitting in this class
     * also keeps it inside {@code MainSuite1a}, so it actually runs in CI.</p>
     */
    @Test
    public void Test_SiteSearchConfig_Stays_Non_Static_So_The_Crawl_Writes_Nothing() {

        final SiteSearchConfig config = new SiteSearchConfig();

        Assert.assertFalse(
                "SiteSearchConfig must not be static by default (issue #37321)",
                config.isStatic());
        Assert.assertFalse(
                "SiteSearchConfig must stay non-static: shouldManageDependencies() enables a "
                        + "per-asset DB write inside the crawl, which the job no longer wraps in a "
                        + "transaction (issue #37321)",
                config.shouldManageDependencies());
    }
}
