package com.dotcms.content.elasticsearch.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ESContentFactoryImplTest extends IntegrationTestBase {
	
	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        //setDebugMode(true);
    }

    /*@AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {

        cleanupDebug(ESContentFactoryImplTest.class);
    }*/
    
    final ESContentFactoryImpl instance = new ESContentFactoryImpl();

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
    public void testScore () {

        //+++++++++++++++++++++++++++
        //Executing a simple query filtering by score
        SearchHits searchHits = instance.indexSearch("+contenttype:blog", 20, 0, "score");


        //Starting some validations
        assertNotNull(searchHits.getTotalHits());
        assertTrue(searchHits.getTotalHits() > 0);

        SearchHit[] hits = searchHits.getHits();
        float maxScore = hits[0].getScore();
        //With this query all the results must have the same score
        for ( SearchHit searchHit : hits ) {
            Logger.info(this, "Blog - SearchHit Score: " + searchHit.getScore() + " inode: "+ searchHit.getSourceAsMap().get("inode"));
            assertTrue(searchHit.getScore() == maxScore);
        }

        //+++++++++++++++++++++++++++
        //Executing a simple query filtering by score
        searchHits = instance.indexSearch("+contenttype:blog blog.title:bullish*", 20, 0, "score");

        //Starting some validations
        assertNotNull(searchHits.getTotalHits());
        assertTrue(searchHits.getTotalHits() > 0);

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

}