package com.dotcms.rest.api.v1.announcements;

import static org.junit.Assert.fail;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.system.announcements.Announcement;
import com.dotcms.system.announcements.AnnouncementsCacheImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liferay.portal.model.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the AnnouncementsHelper
 */
public class AnnouncementsHelperIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given scenario: We have a list of 10 elements
     * Expected result: We test that the sublist is always the same size or smaller than the original list
     */
    @Test
    public void subListTest() {
        AnnouncementsHelperImpl announcementsHelper = new AnnouncementsHelperImpl();
        final List<Integer> integers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        final List<Integer> smallerSubList = announcementsHelper.getSubList(5, integers);
        Assert.assertEquals(5, smallerSubList.size());

        final List<Integer> eqSubList = announcementsHelper.getSubList(10, integers);
        Assert.assertEquals(10, eqSubList.size());

        final List<Integer> biggerSubList = announcementsHelper.getSubList(15, integers);
        Assert.assertEquals(10, biggerSubList.size());

    }


    /**
     *  Given scenario: We mock the loader to return a JsonNode with 100 contentlets for a given language
     *  Expected result: we test that the cache is populated and that the loader is called only once except when we force a reload
     */
    @Test
    public void testGetAnnouncements() {

        final int seed = 100;
        final AtomicInteger loadCount = new AtomicInteger(0);

        final Language language = new LanguageDataGen().nextPersisted();

        //Since we don't have a running dotCMS instance during Integration Testing, we need to mock the loader
        final AnnouncementsLoader loader = new RemoteAnnouncementsLoaderImpl(){
            @Override
            public List<Announcement> loadAnnouncements() {
               try {
                   loadCount.incrementAndGet();
                   final JsonNode jsonNode = generateJson(seed);
                   return toAnnouncements(jsonNode);
               }catch (JsonProcessingException e) {
                   fail(e.getMessage());
               }
               return List.of();
            }
        };
        final User user = APILocator.systemUser();
        final AnnouncementsCacheImpl cache = new AnnouncementsCacheImpl();
        AnnouncementsHelperImpl announcementsHelper = new AnnouncementsHelperImpl(loader, cache);
        final List<Announcement> announcements = announcementsHelper.getAnnouncements(false, -1, user);
        Assert.assertNotNull(announcements);
        Assert.assertEquals((int)AnnouncementsHelper.ANNOUNCEMENTS_LIMIT.get(), announcements.size());
        final List<Announcement> cached = cache.get();
        Assert.assertNotNull(cached);
        Assert.assertEquals(seed, cached.size());
        Assert.assertEquals(1, loadCount.get());

        //Try another call but this time loadCount should be 1 again since we are using the cache
        final List<Announcement> announcements2 = announcementsHelper.getAnnouncements(false, -1, user);
        Assert.assertNotNull(announcements2);
        Assert.assertEquals((int)AnnouncementsHelper.ANNOUNCEMENTS_LIMIT.get(), announcements2.size());
        Assert.assertEquals(1, loadCount.get());

        final List<Announcement> announcements3 = announcementsHelper.getAnnouncements(true, -1, user);
        Assert.assertNotNull(announcements3);
        Assert.assertEquals(2, loadCount.get());

        final List<Announcement> announcements4 = announcementsHelper.getAnnouncements(false, 5, user);
        Assert.assertNotNull(announcements4);
        Assert.assertEquals(5, announcements4.size());
    }

     SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");

    /**
     * Generate a JsonNode with n contentlets
     * @param n
     * @param lang
     * @return JsonNode
     * @throws JsonProcessingException
     */
    public  JsonNode generateJson(final int n) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode contentletsArray = objectMapper.createArrayNode();

        for (int i = 0; i < n; i++) {
            String title = "Title " + (i + 1);
            String desc = "Desc " + (i + 1);
            String url = "https://example.com/contentlet" + (i + 1);
            String date = simpleDateFormat.format(new Date());

            ObjectNode contentletObject = objectMapper.createObjectNode();
            contentletObject.put("announcementDate", date);
            contentletObject.put("hostName", "System Host");
            contentletObject.put("modDate", date); // Using the same date for modDate and publishDate for simplicity
            contentletObject.put("publishDate", date);
            contentletObject.put("title", title);
            contentletObject.put("description", desc);
            contentletObject.put("baseType", "CONTENT");
            contentletObject.put("inode", java.util.UUID.randomUUID().toString());
            contentletObject.put("archived", false);
            contentletObject.put("host", "SYSTEM_HOST");
            contentletObject.put("working", true);
            contentletObject.put("locked", false);
            contentletObject.put("stInode", java.util.UUID.randomUUID().toString());
            contentletObject.put("contentType", "dotAnnouncement");
            contentletObject.put("live", true);
            contentletObject.put("owner", "dotcms.org.1");
            contentletObject.put("identifier", java.util.UUID.randomUUID().toString());
            contentletObject.put("languageId",1);
            contentletObject.put("type1", "Announcement");
            contentletObject.put("url", url);
            contentletObject.put("titleImage", "TITLE_IMAGE_NOT_FOUND");
            contentletObject.put("modUserName", "Admin User");
            contentletObject.put("hasLiveVersion", true);
            contentletObject.put("folder", "SYSTEM_FOLDER");
            contentletObject.put("hasTitleImage", false);
            contentletObject.put("sortOrder", 0);
            contentletObject.put("modUser", "dotcms.org.1");
            contentletObject.put("__icon__", "contentIcon");
            contentletObject.put("contentTypeIcon", "announcement");
            contentletObject.put("variant", "DEFAULT");

            contentletsArray.add(contentletObject);
        }

        ObjectNode mainObject = objectMapper.createObjectNode();
        mainObject.set("contentlets", contentletsArray);

        return objectMapper.convertValue(mainObject, JsonNode.class);
    }

    /**
     * Given scenario: We're testing the cache using a TTL of 10 seconds
     * Expected result: We test that the cache has expired after 20 seconds and no longer contains any elements
     */
    @Test
    public void testAnnouncementCache() throws JsonProcessingException, InterruptedException {
        //Create a cache
        AnnouncementsCacheImpl cache = new AnnouncementsCacheImpl();
        //Seed the cache
        RemoteAnnouncementsLoaderImpl loader = new RemoteAnnouncementsLoaderImpl();
        final JsonNode jsonNode = generateJson(100);
        final List<Announcement> announcements = loader.toAnnouncements(jsonNode);
        //Here we are using a TTL of 10 seconds
        cache.put( announcements, 10);
        //Get the cache and verify that it contains 100 elements
        final List<Announcement> cached = cache.get();
        Assert.assertNotNull(cached);
        Assert.assertEquals(100, cached.size());
        //Wait for the cache to expire sleeping for 11 seconds
        Thread.sleep(TimeUnit.SECONDS.toMillis(11));
        //Get the cache again
        final List<Announcement> cached2 = cache.get();
        Assert.assertNotNull(cached2);
        //Verify that the cache is empty
        Assert.assertEquals(0, cached2.size());
    }

}
