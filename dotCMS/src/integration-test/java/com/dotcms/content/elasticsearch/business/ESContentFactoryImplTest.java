package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.ESQueryCache;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.API;

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
    public void test_indexCount_not_found_async() throws Exception {

        instance.indexCount("+inode:xxx", 1000,
                (Long count)-> {

                    assertEquals(Long.valueOf(0), count);
                },
                (e)-> {
                    fail(e.getMessage());
                });
    }

    @Test
    public void test_indexCount_found_async() throws Exception {

        final Optional<Contentlet> optionalContentlet = APILocator.getContentletAPI().findAllContent(0, 40)
                .stream().filter(Objects::nonNull).collect(Collectors.toList()).stream().findFirst();

        if (optionalContentlet.isPresent()) {

            final Contentlet contentlet = optionalContentlet.get();
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletIndexAPI().addContentToIndex(optionalContentlet.get());

            instance.indexCount("+inode:"+optionalContentlet.get().getInode(), 1000,
                    (Long count) -> {

                        assertEquals(Long.valueOf(1), count);
                    },
                    (e) -> {
                        fail(e.getMessage());
                    });
        }
    }

    @Test
    public void findContentlets() throws Exception {
        DotConnect dc=new DotConnect();
        dc.setSQL("select inode from contentlet");
        List<String> inodes=new ArrayList<String>();
        for(Map<String,Object> r : dc.loadObjectResults()) {
            inodes.add((String)r.get("inode"));
        }
        
        List<Contentlet> contentlets = instance.findContentlets(inodes);
        
        Assert.assertEquals(inodes.size(), contentlets.size());
        
        Set<String> inodesSet=new HashSet<String>(inodes);
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
        
        List<String> inodesToOrderBy=new ArrayList<String>();
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
                .setProperty("body", "During the 1980s and 1990s Southwest Air (LUV) ")
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
    
        final Language language1 = new LanguageDataGen().nextPersisted();
        final Language language2 = new LanguageDataGen().nextPersisted();
        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);
        

        // create URL-Mapped content
        final Contentlet workingOneLanguage = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", "myBody")
                .nextPersisted();
        
        // create URL-Mapped content
        final Contentlet workingTwoLanguage = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", "myBody")
                .nextPersisted();
        
        // create URL-Mapped content
        final Contentlet publishedTwoLanguage2 = new ContentletDataGen(blogType.id())
                .languageId(language2.getId())
                .setProperty("body", "myBody")
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
        
        final Language language1 = new LanguageDataGen().nextPersisted();

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
                .setProperty("body", "myBody")
                .nextPersisted();
        

        SearchHits hits3 = instance.indexSearch(liveQuery, 10, 0, null);
        
        // Checking in a new piece of content flushed the esQuerycache, we get new results
        assertTrue(hits != hits3);
    }
    
    
    /**
     * This test insures that we are taking all the parameters of the query into account when
     * building our cache key
     * @throws Exception
     */
    @Test
    public void test_cached_es_query_different_responses_for_all_params() throws Exception {
        final Language language1 = new LanguageDataGen().nextPersisted();

        final ContentType blogType = TestDataUtils.getBlogLikeContentType(site);
        
        for(int i=0;i<10;i++) {
        // checkin a new piece of content
        Contentlet con = new ContentletDataGen(blogType.id())
                .languageId(language1.getId())
                .setProperty("body", "myBody")
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
        assertTrue(hits != hits1);
        assertNotEquals(hits , hits1);
        
        assertTrue(hits != hits2);
        assertNotEquals(hits , hits2);
        
        assertTrue(hits != hits3);
        assertNotEquals(hits , hits3);
        
        assertTrue(hits != hits4);
        assertNotEquals(hits , hits4);
        
        assertTrue(hits != hits5);
        assertNotEquals(hits , hits5);
        

    }
}