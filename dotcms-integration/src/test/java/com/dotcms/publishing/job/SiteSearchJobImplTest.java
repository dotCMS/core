package com.dotcms.publishing.job;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
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
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
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
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static org.awaitility.Awaitility.await;

@RunWith(DataProviderRunner.class)
public class SiteSearchJobImplTest extends IntegrationTestBase {

    static ESIndexAPI esIndexAPI;
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

        esIndexAPI = APILocator.getESIndexAPI();
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

    @Test
    public void Test_Non_Incremental_Create_Default_Index_Run_Non_Incrementally_Expect_New_Index_Keep_Alias_And_Default()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }
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

        Assert.assertEquals("New index is expected have same Alias", alias, esIndexAPI.getIndexAlias(newIndexName));
        final SiteSearchResults search = siteSearchAPI.search(newIndexName, "*",0, 10);
        Assert.assertTrue(search.getTotalResults() >= 1);
        final List<SiteSearchAudit> recentAudits = siteSearchAuditAPI.findRecentAudits(jobId, 0, 1);
        Assert.assertFalse(recentAudits.isEmpty());
        final SiteSearchAudit siteSearchAudit = recentAudits.get(0);
        Assert.assertEquals(1, siteSearchAudit.getPagesCount());
        Assert.assertFalse(siteSearchAudit.isIncremental()); //did not run incrementally since it was launched as a run-now.
        Assert.assertTrue(siteSearchAPI.isDefaultIndex(newIndexName));
    }


    @Test
    public void Test_Non_Incremental_Create_Default_Index_Create_Second_Index_Run_Non_Incrementally_Expect_Non_Default_New_Index()
            throws DotPublishingException, JobExecutionException, DotDataException, IOException, DotSecurityException {

        List<SiteSearchAudit> recentAudits;
        final SiteSearchJobImpl impl = new SiteSearchJobImpl();

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }

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

        final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
        for(final String index:indicesBeforeTest) {
            esIndexAPI.delete(index);
        }

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

            final List<String> indicesBeforeTest = siteSearchAPI.listIndices();
            for (final String index : indicesBeforeTest) {
                esIndexAPI.delete(index);
            }
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
}
