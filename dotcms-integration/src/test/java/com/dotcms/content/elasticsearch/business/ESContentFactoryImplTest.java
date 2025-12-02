package com.dotcms.content.elasticsearch.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.TranslatedQuery;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;
import static com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.ES_TRACK_TOTAL_HITS;
import static com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.ES_TRACK_TOTAL_HITS_DEFAULT;
import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.MAX_LIMIT;
import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class ESContentFactoryImplTest extends IntegrationTestBase {

    private static Host site;

	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        //setDebugMode(true);

        site = new SiteDataGen().nextPersisted();
    }

    final ESContentFactoryImpl instance = new ESContentFactoryImpl();

    public static class TestCase {

        String formatPattern;

        public TestCase(final String formatPattern) {
            this.formatPattern = formatPattern;
        }
    }

    @DataProvider
    public static Object[] dateTimeTestCases() {
        return new TestCase[]{
                new TestCase("MM/dd/yyyy hh:mm:ssa"),
                new TestCase("MM/dd/yyyy hh:mm:ss a"),
                new TestCase("MM/dd/yyyy hh:mm a"),
                new TestCase("MM/dd/yyyy hh:mma"),
                new TestCase("MM/dd/yyyy HH:mm:ss"),
                new TestCase("MM/dd/yyyy HH:mm"),
                new TestCase("yyyyMMddHHmmss"),
                new TestCase("yyyyMMdd"),
                new TestCase("MM/dd/yyyy")
        };
    }

    @DataProvider
    public static Object[] timeTestCases() {
        return new TestCase[]{
                new TestCase("hh:mm:ssa"),
                new TestCase("hh:mm:ss a"),
                new TestCase("HH:mm:ss"),
                new TestCase("hh:mma"),
                new TestCase("hh:mm a"),
                new TestCase("HH:mm")
        };
    }

    @Test
    public void findContentlets() throws Exception {
        DotConnect dc=new DotConnect();
        dc.setSQL("select inode from contentlet");
        List<String> inodes=new ArrayList<>();
        for(Map<String,Object> r : dc.loadObjectResults()) {
            inodes.add((String)r.get("inode"));
        }

        List<Contentlet> contentlets = instance.findContentlets(inodes);

        Assert.assertEquals(inodes.size(), contentlets.size());

        Set<String> inodesSet=new HashSet<>(inodes);
        for(Contentlet cc : contentlets) {
            Assert.assertTrue(inodesSet.remove(cc.getInode()));
        }
        Assert.assertEquals(0, inodesSet.size());
    }

    /**
     * When calling the findContentlets(List<String> inodes), it should return the
     * contentlets in the same order as they were asked for, meaning, if the list
     * of contentlets was sent in ordered by mod_date, then they should come out ordered
     * by mod_date.  If you order them by random, then you get them out in that same
     * random order
     * @throws Exception
     */
    @Test
    public void findContentletsReturnsInExpectedOrder() throws Exception {
      String name = "contentType" + System.currentTimeMillis();
      ContentType type = new ContentTypeDataGen().velocityVarName(name).name(name).nextPersisted();
      for(int i=0;i<10;i++) {
        new ContentletDataGen(type.inode()).nextPersisted();
      }
        DotConnect dc=new DotConnect();
        dc.setSQL("select inode from contentlet order by mod_date desc");
        dc.setMaxRows(10);

        List<String> inodesToOrderBy=new ArrayList<>();
        for(Map<String,Object> r : dc.loadObjectResults()) {
          inodesToOrderBy.add((String)r.get("inode"));
        }

        // ten inodes
        assertTrue("We've added 10 contentlets", inodesToOrderBy.size()==10);

        //load the cache with the 9th one and the 7th one
        instance.find(inodesToOrderBy.get(9));
        instance.find(inodesToOrderBy.get(7));


        List<Contentlet> contentlets = instance.findContentlets(inodesToOrderBy);

        // this list should mirror the db query above
        Assert.assertEquals("our inodes and contentlet lists match",inodesToOrderBy.size(), contentlets.size());

        // make sure the contentlets are in the same order
        for(int i=0;i<inodesToOrderBy.size();i++) {
          assertEquals(inodesToOrderBy.get(i),contentlets.get(i).getInode());
        }


        //randomize the order sent in
        Collections.shuffle(inodesToOrderBy);
        contentlets = instance.findContentlets(inodesToOrderBy);

        // this list should mirror the shuffle above
        Assert.assertEquals("our inodes and contentlet lists match",inodesToOrderBy.size(), contentlets.size());

        // make sure the contentlets are in the same order
        for(int i=0;i<inodesToOrderBy.size();i++) {
          assertEquals("content is in the right order",inodesToOrderBy.get(i),contentlets.get(i).getInode());
        }



    }

    @Test
    public void saveContentlets() throws Exception {
        try {
            // Insert without language id
            Host systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("dummy-system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            instance.save(systemHost);

            Assert.fail("Saving a contentlet without language must throw an exception.");
        } catch (Exception e) {
        }

        try {
            // Insert with an invalid language id
            Host systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("dummy-system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            systemHost.setLanguageId(9999);
            instance.save(systemHost);

            Assert.fail("Saving a contentlet with unexisting language must throw an exception.");
        } catch (Exception e) {
        }
    }

    @Test
    public void testScore () throws DotDataException, DotSecurityException {
        String blogId="Blog" + System.currentTimeMillis();
        final ContentType blogContentType = TestDataUtils
                .getBlogLikeContentType(blogId);
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        new ContentletDataGen(blogContentType.id())
                .languageId(languageId)
                .host(site)
                .setProperty("title", "Bullish On America? Get On Board With Southwest Air")
                .setProperty("urlTitle", "title")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .setProperty("sysPublishDate", new Date()).nextPersisted();

        new ContentletDataGen(blogContentType.id())
        .languageId(languageId)
        .host(site)
        .setProperty("title", "Mulish On America? Get On Board With Southwest Air")
        .setProperty("urlTitle", "title")
        .setProperty("body", "During the 1980s and 1990s Southwest Air (LUV) ")
        .setProperty("sysPublishDate", new Date()).nextPersisted();

        //+++++++++++++++++++++++++++
        //Executing a simple query filtering by score
        SearchHits searchHits = instance.indexSearch("+contenttype:"+blogId, 20, 0, "score");


        //Starting some validations
        assertNotNull(searchHits.getTotalHits());
        assertTrue(searchHits.getTotalHits().value > 0);

        SearchHit[] hits = searchHits.getHits();
        float maxScore = hits[0].getScore();
        //With this query all the results must have the same score
        for ( SearchHit searchHit : hits ) {
            Logger.info(this, "Blog - SearchHit Score: " + searchHit.getScore() + " inode: "+ searchHit.getSourceAsMap().get("inode"));
            assertTrue(searchHit.getScore() == maxScore);
        }

        //+++++++++++++++++++++++++++
        //Executing a simple query filtering by score
        searchHits = instance.indexSearch("+contenttype:"+blogId + " " + blogId + ".title:bullish*", 20, 0, "score");

        //Starting some validations
        assertNotNull(searchHits.getTotalHits());
        assertTrue(searchHits.getTotalHits().value > 0);

        hits = searchHits.getHits();
        maxScore = getMaxScore(hits);


        //With this query the first result must have a higher score than the others
        assertTrue(maxScore == searchHits.getHits()[0].getScore());
        //The second record should have a lower score
        assertTrue(maxScore != searchHits.getHits()[1].getScore());
        assertTrue(searchHits.getHits()[0].getScore() > searchHits.getHits()[1].getScore());
    }

    @Test
    public void testModDateDotRawFormatIsValid(){
        final SearchHits searchHits = instance.indexSearch("+moddate_dotraw: *t*", 20, 0, "modDate desc");
        assertFalse(UtilMethods.isSet(searchHits.getHits()));
    }




    /**
     * this method tests that if we are passing in asc or desc as a sort order (both invalid without a
     * specified field) then we do not blow up.
     */

    @Test
    public void test_trying_with_bad_sorts() {
        final SearchHits descendingHits = instance.indexSearch("*", 20, 0, "desc");
        // we should have hits, as we are ignoring the invalid sort
        assert (descendingHits.getHits().length > 0);


        final SearchHits ascendingHits = instance.indexSearch("*", 20, 0, "asc");
     // we should have hits, as we are ignoring the invalid sort
        assert (ascendingHits.getHits().length > 0);

        final SearchHits descendingHitsUpper = instance.indexSearch("*", 20, 0, "DESC");
        // we should have hits, as we are ignoring the invalid sort
        assert (descendingHitsUpper.getHits().length > 0);

        final SearchHits ascHitsUpper = instance.indexSearch("*", 20, 0, "DESC");
        // we should have hits, as we are ignoring the invalid sort
        assert (ascHitsUpper.getHits().length > 0);


    }







    private float getMaxScore(SearchHit[] hits) {
        float maxScore = java.lang.Float.MIN_VALUE;

        for (SearchHit hit : hits) {
            float score = hit.getScore();

            if (maxScore < score){
                maxScore = score;
            }
        }

        return maxScore;
    }

    /**
     * Tests that after removing a particular version of a content, previously assigned permissions
     * are maintained.
     */

    @Test
    public void testDeleteVersion_KeepPermissions() throws DotSecurityException, DotDataException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User systemUser  = APILocator.systemUser();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        Contentlet firstVersion;
        Contentlet secondVersion = null;
        Role reviewerRole;
        try {
            // create test contentlet
            firstVersion = TestDataUtils.getBlogContent(true, 1);

            final String contentletIdentifier = firstVersion.getIdentifier();

            // create reviewer role
            reviewerRole = TestUserUtils.getOrCreateReviewerRole();

            // asign publish permissions on contentlet for reviewer role
            List<Permission> newSetOfPermissions = new ArrayList<>();

            newSetOfPermissions.add(
                    new Permission(contentletIdentifier, reviewerRole.getId(),
                            3, true));


            permissionAPI.assignPermissions(newSetOfPermissions, firstVersion, systemUser,
                            false);

            // create new version of contentlet
            secondVersion = contentletAPI.checkout(firstVersion.getInode(),
                            APILocator.systemUser(), false);

            secondVersion = contentletAPI.checkin(secondVersion, systemUser,
                    false );

            contentletAPI.publish(secondVersion, systemUser, false);

            // delete old version of contentlet
            contentletAPI.deleteVersion(firstVersion,
                    APILocator.systemUser(), false);


            // verify peremissions are ok
            final String reviewerRoleId = reviewerRole.getId();
            List<Permission> permissions = permissionAPI.getPermissionsByRole(reviewerRole, false);
            final boolean doesPermissionStillExist = permissions.stream()
                    .anyMatch(permission -> permission.getInode().equals(
                            contentletIdentifier) && permission.getRoleId()
                            .equals(reviewerRoleId));

            assertTrue(doesPermissionStillExist);


        } finally {
            // clean up
            if(secondVersion!=null) {
                APILocator.getContentTypeAPI(APILocator.systemUser())
                        .delete(secondVersion.getContentType());
            }
        }
    }

    @Test
    @UseDataProvider("dateTimeTestCases")
    public void testIndexSearchFilteringByDateTime(final TestCase testCase){

        LocalDateTime today, yesterday, tomorrow;

        final LocalDateTime now = LocalDateTime.now();

        today = now;
        yesterday = today.minusDays(1).withHour(0).withMinute(0).withSecond(0);
        tomorrow = today.plusDays(1).withHour(23).withMinute(59).withSecond(59);

        buildLuceneQueryAndTest(testCase, today, yesterday, tomorrow);
    }

    @Test
    @UseDataProvider("timeTestCases")
    public void testIndexSearchFilteringByTime(final TestCase testCase){

        LocalDateTime today, yesterday, tomorrow;

        final LocalDateTime now = LocalDateTime.now();
        today = LocalDateTime.of(1970, 1, 1, now.getHour(), now.getMinute(), now.getSecond());
        yesterday = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        tomorrow = LocalDateTime.of(1970, 1, 1, 23, 59, 59);

        buildLuceneQueryAndTest(testCase, today, yesterday, tomorrow);
    }

    /**
     *
     * @param testCase
     * @param today
     * @param yesterday
     * @param tomorrow
     */
    private void buildLuceneQueryAndTest(final TestCase testCase, final LocalDateTime today,
            final LocalDateTime yesterday, final LocalDateTime tomorrow) {
        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet contentlet = createNewsContent(contentType, today);
        final DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern(testCase.formatPattern);

        //Test time ranges with TO (upper case)
        String query = String.format(
                "+contentType:%1$s +%1$s.sysPublishDate:[%2$s  TO  %3$s]", contentType.variable(),
                yesterday.format(myFormatObj), tomorrow.format(myFormatObj));

        runLuceneQueryAndValidateResults(query, contentlet);

        //Test time ranges with to (lower case)
        query = String.format(
                "+contentType:%1$s +%1$s.sysPublishDate:[%2$s  to  %3$s]", contentType.variable(),
                yesterday.format(myFormatObj), tomorrow.format(myFormatObj));

        runLuceneQueryAndValidateResults(query, contentlet);
    }

    /**
     * Creates a content with a specific publish date
     * @param contentType
     * @param today
     * @return
     */
    private Contentlet createNewsContent(final ContentType contentType, final LocalDateTime today) {
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        Contentlet contentlet = TestDataUtils.getNewsContent(false, languageId, contentType.id());
        contentlet.setDateProperty("sysPublishDate", Date.from(today.atZone(ZoneId.systemDefault()).toInstant()));

        contentlet = ContentletDataGen.checkin(contentlet, IndexPolicy.WAIT_FOR);
        return contentlet;
    }

    /**
     *
     * @param query
     * @param contentlet
     */
    private void runLuceneQueryAndValidateResults(final String query, final Contentlet contentlet) {
        final SearchHits searchHits = instance.indexSearch(query,-1, -1, null);

        //Validate results
        assertNotNull(searchHits.getTotalHits());
        assertTrue(searchHits.getTotalHits().value > 0);

        final SearchHit[] hits = searchHits.getHits();
        assertEquals(contentlet.getInode(), hits[0].getSourceAsMap().get("inode"));
    }

    @Test
    public void test_findContentletByIdentifier() throws Exception {

        final Language language1 = new UniqueLanguageDataGen().nextPersisted();
        final Language language2 = new UniqueLanguageDataGen().nextPersisted();
        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);


        // create URL-Mapped content
        final Contentlet workingOneLanguage = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        // create URL-Mapped content
        final Contentlet workingTwoLanguage = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        // create URL-Mapped content
        final Contentlet publishedTwoLanguage2 = new ContentletDataGen(blogType.id())
                .languageId(language2.getId())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .setProperty("identifier", workingTwoLanguage.getIdentifier())
                .nextPersisted();


        APILocator.getContentletAPI().publish(publishedTwoLanguage2, APILocator.systemUser(), false);



        assertEquals("workingOneLanguage exists and is working", workingOneLanguage, instance.findContentletByIdentifier(workingOneLanguage.getIdentifier(), false, language1.getId()));
        assertNull("workingOneLanguage does not exist in 2nd language", instance.findContentletByIdentifier(workingOneLanguage.getIdentifier(), false, language2.getId()));
        assertNull("workingOneLanguage does not exist in live", instance.findContentletByIdentifier(workingOneLanguage.getIdentifier(), true, language1.getId()));

        assertNull("workingTwoLanguage in language1 is not live", instance.findContentletByIdentifier(workingTwoLanguage.getIdentifier(), true, language1.getId()));
        assertEquals("workingTwoLanguage exists in langauge1 and is working", workingTwoLanguage, instance.findContentletByIdentifier(workingTwoLanguage.getIdentifier(), false, language1.getId()));
        assertEquals("workingTwoLanguage exists in langauge2 and is working", publishedTwoLanguage2, instance.findContentletByIdentifier(workingTwoLanguage.getIdentifier(), false, language2.getId()));
        assertEquals("workingTwoLanguage exists in langauge2 and is live", publishedTwoLanguage2, instance.findContentletByIdentifier(workingTwoLanguage.getIdentifier(), true, language2.getId()));

    }

    /**
     * This tests whether we are getting cached results from queries to elasticsearch and that these
     * results are invalidated when a new piece of content is checked in
     *
     * @throws Exception
     */
    @Test
    public void test_cached_es_query_response() throws Exception {

        final Language language1 = new UniqueLanguageDataGen().nextPersisted();

        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);


        assert(CacheLocator.getESQueryCache() !=null);
        final String liveQuery = "+baseType:1 +live:true" ;
        final String workingQuery = "+baseType:1 +live:false" ;


        SearchHits hits = instance.indexSearch(liveQuery, 10, 0, null);

        //assert we have results
        assertTrue(hits.getTotalHits().value > 0);

        SearchHits hits2 = instance.indexSearch(liveQuery, 10, 0, null);

        // hits and hits2 are the same object in memory (meaning, it came from cache)
        assertTrue(hits == hits2);


        // checkin a new piece of content
        new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();


        SearchHits hits3 = instance.indexSearch(liveQuery, 10, 0, null);

        // Checking in a new piece of content flushed the esQuerycache, we get new results
        assertTrue(hits != hits3);
        assertTrue(hits3.getTotalHits().value > 0);
    }


    /**
     * This test insures that we are taking all the parameters of the query into account when
     * building our cache key
     * @throws Exception
     */
    @Test
    public void test_cached_es_query_different_responses_for_all_params() throws Exception {
        final Language language1 = new UniqueLanguageDataGen().nextPersisted();

        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);

        for(int i=0;i<10;i++) {
        // checkin a new piece of content
        Contentlet con = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();
            if(i % 2==0) {
                APILocator.getContentletAPI().publish(con, APILocator.systemUser(), false);
            }
        }
        final String liveQuery = "+baseType:1 +live:true" ;
        final String workingQuery = "+baseType:1 +live:false" ;


        //default
        SearchHits hits = instance.indexSearch(liveQuery, 5, 0, null);

        // working index
        SearchHits hits1 = instance.indexSearch(workingQuery, 5, 0, null);

        // different limit
        SearchHits hits2 = instance.indexSearch(liveQuery, 4, 0, null);

        // different offset
        SearchHits hits3 = instance.indexSearch(liveQuery, 5, 1, null);

        // different sort
        SearchHits hits4 = instance.indexSearch(liveQuery, 5, 0, "title desc");

        // different sort direction
        SearchHits hits5 = instance.indexSearch(liveQuery, 5, 0, "title asc");



        //assert we have results
        assertTrue(hits.getTotalHits().value > 0);

        // all parameters are being taken into account when building the cache key
        assertNotSame(hits, hits1);
        assertNotEquals(hits , hits1);
        assertTrue(hits1.getTotalHits().value > 0);

        assertNotSame(hits, hits2);
        assertNotEquals(hits , hits2);
        assertTrue(hits2.getTotalHits().value > 0);

        assertNotSame(hits, hits3);
        assertNotEquals(hits , hits3);
        assertTrue(hits3.getTotalHits().value > 0);

        assertNotSame(hits, hits4);
        assertNotEquals(hits , hits4);
        assertTrue(hits4.getTotalHits().value > 0);

        assertNotSame(hits, hits5);
        assertNotEquals(hits , hits5);
        assertTrue(hits5.getTotalHits().value > 0);


    }



    /**
     * Method to test: {@link ESContentFactoryImpl#translateQuery(String, String)}
     * Given Scenario: Perform a query with an enterprise license
     * ExpectedResult: The query should not contain a filter by {@link BaseContentType#PERSONA} nor {@link BaseContentType#FORM}
     */
    @Test
    public void test_translateQueryWithLicense(){
        final TranslatedQuery translatedQuery = ESContentFactoryImpl
                .translateQuery("+contentType:Host", null);
        assertFalse(translatedQuery.getQuery()
                .contains("-basetype:" + BaseContentType.PERSONA.getType()));
        assertFalse(
                translatedQuery.getQuery().contains("-basetype:" + BaseContentType.FORM.getType()));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findContentletByIdentifierAnyLanguage(String)}
     * Given Scenario: Happy path to get a contentlet given its identifier regardless of the language
     * ExpectedResult: The method should return a contentlet
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_findContentletByIdentifierAnyLanguage()
            throws DotSecurityException, DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User user = APILocator.systemUser();
        final Language language1 = new UniqueLanguageDataGen().nextPersisted();
        final Language language2 = new UniqueLanguageDataGen().nextPersisted();

        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();

        final Contentlet banner1 = TestDataUtils
                .getBannerLikeContent(true, language1.getId(), bannerLikeContentType.id(), null);

        Contentlet banner2 = contentletAPI.checkout(banner1.getInode(), APILocator.systemUser(), false);

        banner2.setLanguageId(language2.getId());

        banner2 = contentletAPI.checkin(banner2, user, false);

        final Contentlet result = instance.findContentletByIdentifierAnyLanguage(banner1.getIdentifier());

        assertNotNull(result);
        assertEquals(banner2.getIdentifier(), result.getIdentifier());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findContentletByIdentifierAnyLanguage(String)}
     * Given Scenario: Find a contentlet given its identifier regardless of the language and all versions
     * of the contentlet are archived
     * ExpectedResult: The method shouldn't return any contentlet
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_findContentletByIdentifierAnyLanguageNoArchived()
            throws DotSecurityException, DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User user = APILocator.systemUser();
        final Language language1 = new UniqueLanguageDataGen().nextPersisted();
        final Language language2 = new UniqueLanguageDataGen().nextPersisted();

        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();

        final Contentlet banner1 = TestDataUtils
                .getBannerLikeContent(true, language1.getId(), bannerLikeContentType.id(), null);

        Contentlet banner2 = contentletAPI.checkout(banner1.getInode(), APILocator.systemUser(), false);

        banner2.setLanguageId(language2.getId());

        banner2 = contentletAPI.checkin(banner2, user, false);

        contentletAPI.archive(banner1, user, false);
        contentletAPI.archive(banner2, user, false);

        CacheLocator.getContentletCache().remove(banner1);
        CacheLocator.getContentletCache().remove(banner2);

        final Contentlet result = instance.findContentletByIdentifierAnyLanguage(banner1.getIdentifier());

        assertNull(result);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findContentletByIdentifierAnyLanguage(String, boolean)}
     * Given Scenario: Get a contentlet given its identifier regardless of the language and its archived status
     * ExpectedResult: The method should return a contentlet
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void test_findContentletByIdentifierAnyLanguageIncludeDeleted()
            throws DotSecurityException, DotDataException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final User user = APILocator.systemUser();
        final Language language1 = new UniqueLanguageDataGen().nextPersisted();
        final Language language2 = new UniqueLanguageDataGen().nextPersisted();

        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();

        final Contentlet banner1 = TestDataUtils
                .getBannerLikeContent(true, language1.getId(), bannerLikeContentType.id(), null);

        Contentlet banner2 = contentletAPI.checkout(banner1.getInode(), APILocator.systemUser(), false);

        banner2.setLanguageId(language2.getId());

        banner2 = contentletAPI.checkin(banner2, user, false);

        contentletAPI.archive(banner1, user, false);
        contentletAPI.archive(banner2, user, false);

        CacheLocator.getContentletCache().remove(banner1);
        CacheLocator.getContentletCache().remove(banner2);

        final Contentlet result = instance.findContentletByIdentifierAnyLanguage(banner1.getIdentifier(), true);

        assertNotNull(result);
        assertEquals(banner2.getIdentifier(), result.getIdentifier());
    }

    /**
     * Test the different values we might have for the property ES_TRACK_TOTAL_HITS and how they affect the search options
     * Given scenario: We set different values into the Config and verify the expected value adopted by the SearchBuilder
     * Expected Result: if the property is to false the feature is disabled.
     * if we set it to true then the tracking is done accurately automatically
     * if the property gets excluded we default to the default limit of 10K
     */
    @Test
    public void Test_Setting_Track_Hits() {
       final String savedValue = Config.getStringProperty(ES_TRACK_TOTAL_HITS);
       try {
           final SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource();

           Random ran = new Random();
           final int limit = ran.nextInt(100);

           Config.setProperty(ES_TRACK_TOTAL_HITS, Integer.toString(limit));
           instance.setTrackHits(searchSourceBuilder);
           assertEquals((long)searchSourceBuilder.trackTotalHitsUpTo(),limit);

           Config.setProperty(ES_TRACK_TOTAL_HITS, null);
           instance.setTrackHits(searchSourceBuilder);
           assertEquals((long)searchSourceBuilder.trackTotalHitsUpTo(),ES_TRACK_TOTAL_HITS_DEFAULT);

       }finally {
           Config.setProperty(ES_TRACK_TOTAL_HITS, savedValue);
       }
    }

    /***
     * Here we're testing how the property ES_TRACK_TOTAL_HITS affects the count retrieved by the search.
     * When we vary the limit allowed the count through the property ES_TRACK_TOTAL_HITS using an integer the limit is set to a number
     * Given Scenario: We create a number of contentlets. Then we set a limit via  ES_TRACK_TOTAL_HITS
     * Expected Result: We verify that the number of items returned by the query matches the total, but the total count respects the tracking_total_hits flag.
     */
    @Test
    public void Test_TrackHits_SearchCount(){

        final ESQueryCache esQueryCache = CacheLocator.getESQueryCache();
        final int newContentTypeItems = 60; // We're adding 60 items of our CT
        final int trackHitsLimit = 30; //But we're setting up a track count limit

        final String contentTypeName = "TContentType" + System.currentTimeMillis();
        final ContentType type = new ContentTypeDataGen().velocityVarName(contentTypeName)
                .name(contentTypeName)
                .nextPersisted();

        for (int i = 0; i < newContentTypeItems; i++) {
            new ContentletDataGen(type.inode()).nextPersisted();
        }

        final String queryString = String
                .format("+contenttype:%s +languageid:1 +deleted:false +working:true",
                        contentTypeName);

        //There are 60 items but ES can only retrieve a max of 40.
        SearchHits searchHits = instance
                .indexSearch(queryString, MAX_LIMIT, 0, null);
        assertEquals(searchHits.getHits().length, newContentTypeItems);
        assertEquals(searchHits.getTotalHits().value, newContentTypeItems);
        assertEquals(newContentTypeItems, instance.indexCount(queryString));

        final String savedValue = Config.getStringProperty(ES_TRACK_TOTAL_HITS);
        try {

            final int max = trackHitsLimit + 5;
            for(int i = trackHitsLimit; i <= max; i++) {
                Config.setProperty(ES_TRACK_TOTAL_HITS, Integer.toString(i));
                //We're always removing cache otherwise we would get the same number of pre-cached items
                esQueryCache.clearCache();
                searchHits = instance.indexSearch(queryString, MAX_LIMIT, 0, null);
                assertEquals(searchHits.getHits().length, newContentTypeItems);
                assertEquals(searchHits.getTotalHits().value, i);
                //Regardless of the track_hits count flag. index count should always get you the accurate number.
                // as it works independently from that flag.
                assertEquals(newContentTypeItems, instance.indexCount(queryString));
            }

        } finally {
            Config.setProperty(ES_TRACK_TOTAL_HITS, savedValue);
        }
    }

    /**
     * Method to test: {@link Contentlet#getTitle()} <br>
     * Given scenario: A piece of content without an explicit title field should persist with title=null <br>
     * Expected Result: Title is not persisted in db but {@link Contentlet#getTitle()} returns a value
     */
    @Test
    public void test_dotAssets_persist_title_as_null() {
        Contentlet contentlet = TestDataUtils.getDotAssetLikeContentlet();

        assertNull(contentlet.getMap().get(Contentlet.TITTLE_KEY));
        assertNotNull(contentlet.getTitle());

        contentlet = instance.findInDb(contentlet.getInode()).get();

        assertNull(contentlet.getMap().get(Contentlet.TITTLE_KEY));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#loadJsonField(String, Field)} ()} <br>
     * Lets create a few random Contentlets and test that the properties we know for sure they have
     * can be fetched via json
     */
    @Test
    public void Test_load_Json_Field() throws DotDataException {
        final Contentlet fileAssetContent = TestDataUtils.getFileAssetContent(true, 1L);
        final List<String> fileAssetExpectedFields = ImmutableList
                .of("fileName", "fileAsset", "title", "sortOrder");
        validateLoadJsonField(fileAssetContent, fileAssetExpectedFields);

        final List<String> blogExpectedFields = ImmutableList
                .of("title", "urlTitle", "author", "sysPublishDate", "body", "hostFolder");
        final Contentlet blogContent = TestDataUtils.getBlogContent(true, 1L);
        validateLoadJsonField(blogContent, blogExpectedFields);

        final List<String> employeeExpectedFields = ImmutableList
                .of("firstName", "lastName", "photo", "mobile", "fax", "email");
        final Contentlet employeeContent = TestDataUtils.getEmployeeContent(true, 1L, null);
        validateLoadJsonField(employeeContent, employeeExpectedFields);
    }

    /**
     * given the list of fields and a contentlet we validate the fields can be retrieved
     * @param contentlet
     * @param expectedFields
     * @throws DotDataException
     */
    private void validateLoadJsonField(final Contentlet contentlet,
            final List<String> expectedFields)
            throws DotDataException {
        for (final Field field : contentlet.getContentType().fields().stream()
                .filter(field -> expectedFields.contains(field.variable()))
                .collect(Collectors.toList())) {
            final Object fieldValue = instance.loadJsonField(contentlet.getInode(), field);
            if (null != contentlet.get(field.variable())) {
                assertNotNull("field: " + field.variable() + " shouldn't be null", fieldValue);
            }
        }
    }

    /**
     * This test how {@link com.dotcms.content.elasticsearch.business.ESContentFactoryImpl#save(Contentlet)} persists contentlet when instructed to use the contentlet-as-json field
     * The other dynamic field columns shouldn't be used to store anything
     * @throws Exception
     */
    @Test
    public void Test_Nulled_Out_Columns() throws Exception {

        final boolean defaultValue = Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
        Config.setProperty(SAVE_CONTENTLET_AS_JSON, true);
        try {

            final String hostName = "custom" + System.currentTimeMillis() + ".dotcms.com";
            final Host site = new SiteDataGen().name(hostName).nextPersisted(true);
            final Folder folder = new FolderDataGen().site(site).nextPersisted();
            final ContentType contentType = TestDataUtils
                    .newContentTypeFieldTypesGalore();

            final Contentlet in = new ContentletDataGen(contentType).host(site)
                    .languageId(1)
                    .setProperty("title", "lol")
                    .setProperty("hostFolder", folder.getIdentifier())
                    .setProperty("textFieldNumeric",0)
                    .setProperty("textFieldFloat",0.0F)
                    .setProperty("textField","text")
                    .setProperty("textAreaField", "Desc")
                    .setProperty("dateField",new Date())
                    .setProperty("dateTimeField",new Date())
                    .nextPersisted();

            assertNotNull(in);
            final List<Map<String, Object>> maps = new DotConnect()
                    .setSQL("select * from contentlet where inode = ?").addParam(in.getInode())
                    .loadObjectResults();
            final Map<String, Object> values = maps.get(0);
            assertNotNull(values.get("contentlet_as_json"));

            validateNulledOutField(values, "date");
            validateNulledOutField(values, "text");
            validateNulledOutField(values, "text_area");
            validateNulledOutField(values, "integer");
            validateNulledOutField(values, "float");
            validateNulledOutField(values, "bool");

        } finally {
            Config.setProperty(SAVE_CONTENTLET_AS_JSON, defaultValue);
        }
    }


    private void validateNulledOutField(final Map<String, Object> values, final String prefix) {
        for (int i = 1; i <= 25; i++) {
            final String key = prefix + i;
            final Object object = values.get(key);
            if ("integer".equals(prefix) || "float".equals(prefix)) {
                final Number number = (Number) object;
                assertEquals("field : " + key + " should has value 0. ", 0, number.intValue());
            }
            if ("text_area".equals(prefix) || "text".equals(prefix)) {
                junit.framework.TestCase.assertNull("field: " + key + " should be null. ", object);
            }
            if ("date".equals(prefix)) {
                junit.framework.TestCase.assertNull("field : " + key + " should be null. ", object);
            }
            if ("bool".equals(prefix)) {
                if (object instanceof Number) {
                    final Number number = (Number) object;
                    assertFalse(BooleanUtils.toBooleanObject(number.intValue()));
                }
                if (object instanceof Boolean) {
                    assertFalse(" field: " + key + " should be false ", (Boolean) object);
                }
            }
        }
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findContentletByIdentifierAnyLanguage(String, boolean)}
     * When: The contentlet had just one version not in the DEFAULT variant
     * Should: return that version anyway
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findContentletByIdentifierAnyLanguageNoDefaultVersion()
            throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();


        final Contentlet contentletByIdentifierAnyLanguage = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findContentletByIdentifierAnyLanguage(contentlet.getIdentifier(), variant.name(),false);

        assertNotNull(contentletByIdentifierAnyLanguage);
        assertEquals(contentlet.getIdentifier(), contentletByIdentifierAnyLanguage.getIdentifier());
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findContentletByIdentifierAnyLanguage(String, boolean)}
     * When: The contentlet had just one version not in the DEFAULT variant but it was archived
     * Should: return {@link Optional#empty()}
     *
     * @throws WebAssetException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void findDeletedContentletByIdentifierAnyLanguage()
            throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(language.getId())
                .host(host)
                .variant(variant)
                .nextPersisted();

        final String identifier = contentlet.getIdentifier();
        APILocator.getContentletAPI().archive(contentlet, APILocator.systemUser(), false);

        Contentlet contentletByIdentifierAnyLanguage = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findContentletByIdentifierAnyLanguage(identifier, variant.name(),true);

        assertNotNull(contentletByIdentifierAnyLanguage);
        assertEquals(contentlet.getIdentifier(), contentletByIdentifierAnyLanguage.getIdentifier());


        contentletByIdentifierAnyLanguage = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findContentletByIdentifierAnyLanguage(identifier, variant.name(), false);

        assertNull(contentletByIdentifierAnyLanguage);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language}
     * Should: return all the versions
     */
    @Test
    public void findAllVersions() throws DotDataException, DotSecurityException {
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1 = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersisted();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1);
        contentlet1Checkout.setLanguageId(language_2.getId());
        final Contentlet contentletLanguage2 = ContentletDataGen.checkin(contentlet1Checkout);

        contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1);
        contentlet1Checkout.setLanguageId(language_3.getId());
        final Contentlet contentletLanguage3 = ContentletDataGen.checkin(contentlet1Checkout);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1.getIdentifier());

        final List<Contentlet> contentlets = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findAllVersions(identifier, false);

        assertNotNull(contentlets);
        assertEquals(3, contentlets.size());

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage1.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage2.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage3.getIdentifier())));
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} and some then are old versions
     * Should: return all the versions even the old ones if the flag is true
     */
    @Test
    public void findAllVersionsWithOldVersions() throws DotDataException, DotSecurityException {
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1Live = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang1 = createNewlyWorkingAndLiveVersion(
                contentletLanguage1Live);

        final Contentlet contentletLanguage2Live = createNewLangVersion(language_2, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang2 = createNewlyWorkingAndLiveVersion(
                contentletLanguage2Live);

        final Contentlet contentletLanguage3Live = createNewLangVersion(language_3, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang3 = createNewlyWorkingAndLiveVersion(
                contentletLanguage3Live);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1Live.getIdentifier());

        final List<Contentlet> contentlets = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findAllVersions(identifier, true);

        assertNotNull(contentlets);
        assertEquals(9, contentlets.size());

        final List<String> expectedInodes = list(
                contentletLanguage1Live,
                newlyWorkingAndLiveVersionLang1.get("WORKING"),
                newlyWorkingAndLiveVersionLang1.get("LIVE"),
                contentletLanguage2Live,
                newlyWorkingAndLiveVersionLang2.get("WORKING"),
                newlyWorkingAndLiveVersionLang2.get("LIVE"),
                contentletLanguage3Live,
                newlyWorkingAndLiveVersionLang3.get("WORKING"),
                newlyWorkingAndLiveVersionLang3.get("LIVE")
        ).stream().map(Contentlet::getInode).collect(Collectors.toList());

        expectedInodes.forEach(inode -> assertTrue(contentlets.stream()
                .anyMatch(contentlet -> contentlet.getInode().equals(inode))));
    }

    private static Contentlet createNewLangVersion(final Language language,
            final Contentlet contentlet) {
        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentlet);
        contentlet1Checkout.setLanguageId(language.getId());
        return ContentletDataGen.checkin(contentlet1Checkout);
    }

    private static Map<String, Contentlet> createNewlyWorkingAndLiveVersion(final Contentlet contentletLanguage1Live) {
        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1Live);
        final Contentlet contentletWorking = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet newlyContentleLive = ContentletDataGen.publish(contentletWorking);
        contentlet1Checkout = ContentletDataGen.checkout(newlyContentleLive);
        final Contentlet contentletWorking2 = ContentletDataGen.checkin(contentlet1Checkout);

        return Map.of("LIVE", newlyContentleLive, "WORKING", contentletWorking2);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} and some then are old versions
     * Should: return all the versions even the old ones if the flag is false
     */
    @Test
    public void findAllVersionsWithNotOldVersions() throws DotDataException, DotSecurityException {
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1Live = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1Live);
        final Contentlet contentletLanguage1Working = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet contentletLanguage2Live = createNewLangVersionAndPublish(language_2, contentletLanguage1Live);

        final Contentlet contentlet2Checkout = ContentletDataGen.checkout(contentletLanguage2Live);
        final Contentlet contentletLanguage2Working = ContentletDataGen.checkin(contentlet2Checkout);

        final Contentlet contentletLanguage3Live = createNewLangVersionAndPublish(language_3, contentletLanguage1Live);
        final Contentlet contentlet3Checkout = ContentletDataGen.checkout(contentletLanguage3Live);
        final Contentlet contentletLanguage3Working = ContentletDataGen.checkin(contentlet3Checkout);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1Live.getIdentifier());

        final List<Contentlet> contentlets = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findAllVersions(identifier, false);

        assertNotNull(contentlets);
        assertEquals(6, contentlets.size());

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage1Live.getIdentifier())));

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage1Working.getIdentifier())));

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage2Live.getIdentifier())));

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage2Working.getIdentifier())));

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage3Live.getIdentifier())));

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage3Working.getIdentifier())));
    }

    private Contentlet createNewLangVersionAndPublish(final Language language,
            final Contentlet contentlet) {
        final Contentlet newLangVersion = createNewLangVersion(language, contentlet);
        return ContentletDataGen.publish(newLangVersion);
    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} into the
     * DEFAULT {@link Variant} and a specific {@link Variant}.
     * Should: return all the versions for the DEFAULT {@link Variant} and the specific {@link Variant}
     */
    @Test
    public void findAllVersionsByVariant() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1DefaultVariant = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersisted();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_2.getId());
        final Contentlet contentletLanguage2DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1DefaultVariant);
        contentlet1Checkout.setLanguageId(language_3.getId());
        final Contentlet contentletLanguage3DefaultVariant = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet contentletLang1SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage1DefaultVariant,
                variant, new HashMap<>());

        final Contentlet contentletLang2SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage2DefaultVariant,
                variant, new HashMap<>());

        final Contentlet contentletLang3SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage3DefaultVariant,
                variant, new HashMap<>());

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1DefaultVariant.getIdentifier());

        final List<Contentlet> contentlets = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT);

        assertNotNull(contentlets);
        assertEquals(3, contentlets.size());

        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage1DefaultVariant.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage2DefaultVariant.getIdentifier())));
        assertTrue(contentlets.stream().anyMatch(contentlet -> contentlet.getIdentifier()
                .equals(contentletLanguage3DefaultVariant.getIdentifier())));

      //Now test findAllVersions is returning the versions in descending order
       final List<Contentlet>  copy = new ArrayList<>(contentlets);
       copy.sort((o1, o2) -> o2.getModDate().compareTo(o1.getModDate()));
       assertEquals(copy,contentlets);

    }

    /**
     * Method to test: {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * When: The contentlet had several versions in different {@link Language} and {@link Variant}
     * Also they have  old versions
     * Should: return all the versions even the old ones into the DEFAULT {@link Variant}
     */
    @Test
    public void findAllVersionsWithOldVersionsByVariant() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1Live = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang1 = createNewlyWorkingAndLiveVersion(
                contentletLanguage1Live);

        final Contentlet contentletLanguage2Live = createNewLangVersion(language_2, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang2 = createNewlyWorkingAndLiveVersion(
                contentletLanguage2Live);

        final Contentlet contentletLanguage3Live = createNewLangVersion(language_3, contentletLanguage1Live);
        final Map<String, Contentlet> newlyWorkingAndLiveVersionLang3 = createNewlyWorkingAndLiveVersion(
                contentletLanguage3Live);

        final Contentlet contentletLang1SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage1Live,
                variant, new HashMap<>());

        createNewlyWorkingAndLiveVersion(contentletLang1SpecificVariant);

        final Contentlet contentletLang2SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage2Live,
                variant, new HashMap<>());

        createNewlyWorkingAndLiveVersion(contentletLang2SpecificVariant);

        final Contentlet contentletLang3SpecificVariant = ContentletDataGen.createNewVersion(contentletLanguage3Live,
                variant, new HashMap<>());

        createNewlyWorkingAndLiveVersion(contentletLang3SpecificVariant);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(contentletLanguage1Live.getIdentifier());

        final List<Contentlet> contentlets = ((ESContentFactoryImpl) FactoryLocator.getContentletFactory())
                .findAllVersions(identifier, VariantAPI.DEFAULT_VARIANT);

        assertNotNull(contentlets);
        assertEquals(9, contentlets.size());

        final List<String> expectedInodes = list(
                contentletLanguage1Live,
                newlyWorkingAndLiveVersionLang1.get("WORKING"),
                newlyWorkingAndLiveVersionLang1.get("LIVE"),
                contentletLanguage2Live,
                newlyWorkingAndLiveVersionLang2.get("WORKING"),
                newlyWorkingAndLiveVersionLang2.get("LIVE"),
                contentletLanguage3Live,
                newlyWorkingAndLiveVersionLang3.get("WORKING"),
                newlyWorkingAndLiveVersionLang3.get("LIVE")
        ).stream().map(Contentlet::getInode).collect(Collectors.toList());

        expectedInodes.forEach(inode -> assertTrue(contentlets.stream()
                .anyMatch(contentlet -> contentlet.getInode().equals(inode))));

        //Now test findAllVersions is returning the versions in descending order
        final List<Contentlet>  copy = new ArrayList<>(contentlets);
        copy.sort((o1, o2) -> o2.getModDate().compareTo(o1.getModDate()));
        assertEquals(copy,contentlets);

    }

    /**
     * Testing the method {@link ESContentFactoryImpl#findAllVersions(Identifier, boolean)}
     * This version of the method takes a collection of identifiers and returns all the versions of the contentlets
     * Given scenario: The contentlet had several versions in different {@link Language} `
     * Expected result: The method should return all the versions of the contentlets
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void TestFindAllVersions() throws DotDataException, DotSecurityException {

        final Language language_1 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_2 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();
        final Language language_3 = new com.dotcms.datagen.LanguageDataGen().nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentletLanguage1Live = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        Contentlet contentlet1Checkout = ContentletDataGen.checkout(contentletLanguage1Live);
        final Contentlet contentletLanguage1Working = ContentletDataGen.checkin(contentlet1Checkout);

        final Contentlet contentletLanguage2Live = createNewLangVersionAndPublish(language_2, contentletLanguage1Live);

        final Contentlet contentlet2Checkout = ContentletDataGen.checkout(contentletLanguage2Live);
        final Contentlet contentletLanguage2Working = ContentletDataGen.checkin(contentlet2Checkout);

        final Contentlet contentletLanguage3Live = createNewLangVersionAndPublish(language_3, contentletLanguage1Live);
        final Contentlet contentlet3Checkout = ContentletDataGen.checkout(contentletLanguage3Live);
        final Contentlet contentletLanguage3Working = ContentletDataGen.checkin(contentlet3Checkout);

        final Contentlet contentletLanguage1Live2 = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final Contentlet contentletLanguage1Live3 = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final Contentlet contentletLanguage1Live4 = new ContentletDataGen(contentType)
                .languageId(language_1.getId())
                .host(host)
                .nextPersistedAndPublish();

        final List<Contentlet> contentlets = List.of(contentletLanguage1Live,
                contentletLanguage1Working, contentletLanguage2Live, contentletLanguage2Working,
                contentletLanguage3Live, contentletLanguage3Working, contentletLanguage1Live2,
                contentletLanguage1Live3,
                contentletLanguage1Live4);

        final Set<String> identifiers = contentlets.stream().map(Contentlet::getIdentifier)
                .collect(Collectors.toSet());

        ESContentFactoryImpl impl = (ESContentFactoryImpl) FactoryLocator.getContentletFactory();
        final List<Contentlet> allVersions = impl.findLiveOrWorkingVersions(identifiers);

        Assert.assertEquals(9, allVersions.size());

        for (Contentlet c:contentlets) {
            Assert.assertTrue(allVersions.stream().anyMatch(contentlet -> contentlet.getIdentifier().equals(c.getIdentifier())));
            Assert.assertTrue(allVersions.stream().anyMatch(contentlet -> contentlet.getInode().equals(c.getInode())));
        }

    }

}
