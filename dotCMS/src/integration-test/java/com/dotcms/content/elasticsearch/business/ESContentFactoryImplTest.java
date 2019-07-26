package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
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
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
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

public class ESContentFactoryImplTest extends IntegrationTestBase {

    private static Host site;

	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        //setDebugMode(true);

        site = new SiteDataGen().nextPersisted();
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
    public void testScore () throws DotDataException, DotSecurityException {

        final ContentType blogContentType = TestDataUtils
                .getBlogLikeContentType("Blog" + System.currentTimeMillis());
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        new ContentletDataGen(blogContentType.id())
                .languageId(languageId)
                .host(site)
                .setProperty("title", "Bullish On America? Get On Board With Southwest Air")
                .setProperty("urlTitle", "title")
                .setProperty("body", "During the 1980s and 1990s Southwest Air (LUV) ")
                .setProperty("sysPublishDate", new Date()).nextPersisted();

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

}