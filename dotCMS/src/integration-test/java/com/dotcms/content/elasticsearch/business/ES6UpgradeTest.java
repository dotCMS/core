package com.dotcms.content.elasticsearch.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.google.common.base.CaseFormat;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ES6UpgradeTest extends IntegrationTestBase {

    private static final String TO_REPLACE_HOST_ID = "REPLACE_WITH_HOST_ID";
    private static final String TO_REPLACE_NEWS_CONTENT_TYPE_VARNAME = "REPLACE_WITH_NEWS_CONTENT_TYPE_VARNAME";
    private static final String TO_REPLACE_BLOG_CONTENT_TYPE_VARNAME = "REPLACE_WITH_BLOG_CONTENT_TYPE_VARNAME";
    private final static String RESOURCE_DIR = "com/dotcms/content/elasticsearch/business/json";

    private static User systemUser;

    private static ContentType newsContentType = null;

    private static ContentType blogContentType = null;

    private static Host site = null;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        site = new SiteDataGen().nextPersisted();
        newsContentType = TestDataUtils
                .getNewsLikeContentType(site);
        blogContentType = TestDataUtils
                .getBlogLikeContentType(site);
    }

    /**
     * Gets an array of JSon files containing Elastic Search queries to be tested
     * @return list of Files
     */
    @DataProvider
    public static Object[] getJsonFilesDataProvider() {
        final String resource = ConfigTestHelper.getPathToTestResource(RESOURCE_DIR);
        final File directory = new File(resource);
        return directory.listFiles();
    }

    @Test
    @UseDataProvider("getJsonFilesDataProvider")
    public void testElasticSearchJson(final Object objectFile)
            throws DotSecurityException, DotDataException, IOException {

        final File file = (File) objectFile;
        Logger.info(this, "Testing File: " + file.getName());

        final boolean skip = loadQueryData(file.getName());
        if (!skip) {
            String json = FileUtils.readFileToString(file);
            json = json.replaceAll(TO_REPLACE_NEWS_CONTENT_TYPE_VARNAME,
                    newsContentType.variable());
            json = json.replaceAll(TO_REPLACE_BLOG_CONTENT_TYPE_VARNAME,
                    blogContentType.variable());
            json = json.replaceAll(TO_REPLACE_HOST_ID,
                    site.getIdentifier());
            Logger.info(this, json);
            final ESSearchResults results = APILocator.getContentletAPI()
                    .esSearch(json, false, systemUser, false);

            Assert.assertNotNull(results);

            Logger.info(this, "Results size: " + results.getTotalResults());
            Assert.assertTrue(results.getTotalResults() > 0);

            if (json.contains("agg")) {
                //This is an aggregation
                Assert.assertFalse(results.getAggregations().asList().isEmpty());
            } else {
                //Contentlets
                Assert.assertFalse(results.isEmpty());
                for (final Object res : results) {
                    final Contentlet contentlet = (Contentlet) res;
                    Assert.assertTrue(
                            APILocator.getPermissionAPI().doesUserHavePermission(contentlet,
                                    PermissionAPI.PERMISSION_READ, systemUser, false));
                }
            }
            Logger.info(this, "Success Testing File: " + file.getName());
        } else {
            Assume.assumeTrue("File: " + file.getName() +  " was marked to be ignored. ", false);
        }
    }

    /**
     * This method  takes the query file name and maps that to a class name that prepares the query data.
     * So for example something like filter-tags.json will be mapped to the FilterTags class.
     * @param fileName
     * @return
     * @throws DotDataException
     */
    private boolean loadQueryData(final String fileName) throws DotDataException {
        boolean ignore = false;
        final String fileBaseName = FilenameUtils.getBaseName(fileName);
        final String className = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, fileBaseName);
        try {
            final String fullClassName = String.format("%s$%s",getClass().getName(),className);
            Logger.info(this, fullClassName + " maps to file "+ fileName);
            final QueryDataLoader queryData = QueryDataLoader.class.cast(Class.forName(fullClassName).newInstance());
            ignore = queryData.ignore();
            if(!ignore){
                queryData.loadData();
            }
        } catch (Exception e) {
            Logger.error(this,"Unable to load test data for file "+fileName, e);
        }
        return ignore;
    }


    /**
     * This interface is the small entry point to the data generation classes
     */
    interface QueryDataLoader {

        /**
         * override to load the necessary data required by the quey
         * @throws Exception
         */
        default void loadData() throws Exception {};

        /**
         * override if you want to skip test
         * @return
         */
        default boolean ignore() { return false; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: aggregation-tags.json
     */
    static class AggregationTags implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            TestDataUtils.getNewsContent(true, languageId, newsContentType.id());
        }

       // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: aggregation-content-type.json
     */
    static class AggregationContentType implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
           final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
           TestDataUtils.getNewsContent(true, languageId, newsContentType.id());
        }

       // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: filter-tags.json
     */
    static class FilterTags implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final Category category = new CategoryDataGen().setCategoryName("Investment Banking").setKey("investing").setKeywords("investing").setCategoryVelocityVarName("investing").nextPersisted();
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

            //Making sure the tags we are going to use exist
            new TagDataGen().name("gas").nextPersisted();

            new ContentletDataGen(newsContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .addCategory(category)
                    .setProperty("title", "blah")
                    .setProperty("urlTitle", "title")
                    .setProperty("byline", "jean")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("tags", "gas").nextPersisted();

        }

      // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: filter-catchall.json
     */
    static class FilterCatchall implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            new ContentletDataGen(newsContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("title", "The Gas Price Roller coaster")
                    .setProperty("urlTitle", "title")
                    .setProperty("byline", "gas")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("tags", "gas").nextPersisted();

        }

      //  @Override public boolean ignore() { return true; }

    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: fuzzy-search.json
     */
    static class FuzzySearch implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            new ContentletDataGen(blogContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("title", "Tiffany & Co: Still Shining With Substantial Earnings Beat And Guidance Increase")
                    .setProperty("urlTitle", "title")
                    .setProperty("body", "Blah blah Pssa blah blah ")
                    .setProperty("sysPublishDate", new Date()).nextPersisted();
        }

       // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: function-score.json
     */
    static class FunctionScore implements QueryDataLoader {

       // @Override public boolean ignore() { return true; }
    }

    /**
     * Prepares data for the query on: filter-distance.json
     */
    static class FilterDistance implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {

            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            new ContentletDataGen(newsContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("title", "The Gas Price Roller coaster")
                    .setProperty("urlTitle", "title")
                    .setProperty("byline", "gas")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("latlong","37.776,-122.41")
                    .setProperty("tags", "gas").nextPersisted();

        }

        // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: filter-title-dates.json
     */
    static class FilterTitleDates implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Date publishedDate = DateUtil.convertDate("20/10/2015", new String[]{"dd/MM/yyyy"});
            new ContentletDataGen(newsContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("title", "These states are the most financially ready for retirement")
                    .setProperty("urlTitle", "title")
                    .setProperty("sysPublishDate", publishedDate)
                    .setProperty("story", "blah blah. ")
                    .nextPersisted();

        }

       // @Override public boolean ignore() { return true; }
    }

    /**
     * Do not re-name this class
     * Prepares data for the query on: filter-tags-whitespace.json
     */
    static class FilterTagsWhitespace implements QueryDataLoader {

        @Override
        public void loadData() throws Exception {
            final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            new ContentletDataGen(newsContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("title", "Any News article")
                    .setProperty("urlTitle", "title")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "blah blah. ")
                    .setProperty("tags", "home prices").nextPersisted();

        }

        // @Override public boolean ignore() { return true; }
    }

}
