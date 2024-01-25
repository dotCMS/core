package com.dotcms.rest.api.v1.announcements;

import com.dotcms.system.announcements.Announcement;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the RemoteAnnouncementsLoader
 */
public class RemoteAnnouncementsLoaderIntegrationTest {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    RemoteAnnouncementsLoaderImpl loader = new RemoteAnnouncementsLoaderImpl();

    /**
     * Given scenario: We have a list of 10 elements
     * Expected result: We test that the sublist is always the same size or smaller than the original list*
     * @throws JsonProcessingException
     */
    @Test
    public void testAnnouncementRemoteURL() throws JsonProcessingException {
        String expected = "https://www.dotcms.com/api/content/render/false/query/+contentType:Announcement +languageId:1 +deleted:false +live:true/orderBy/Announcement.announcementDate desc";
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final String builtURL = loader.buildURL(defaultLanguage);
        Assert.assertEquals(expected, builtURL);
    }

    /**
     * Given scenario: We have a list of 10 elements
     * Expected result: We test that the sublist is always the same size or smaller than the original list
     * @throws JsonProcessingException
     */
    @Test
    public void testAnnouncementConversion() throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonString);
        final List<Announcement> announcements = loader.toAnnouncements(node);
        Assert.assertNotNull(announcements);
        Assert.assertEquals(1, announcements.size());
        final Announcement announcement = announcements.get(0);
        Assert.assertEquals("Title #1", announcement.title());
        Assert.assertEquals("https://demo.dotcms.com", announcement.url());
    }

    String jsonString = "{\n"
            + "   \"contentlets\":[\n"
            + "      {\n"
            + "         \"announcementDate\":\"2024-01-30 00:00:00.0\",\n"
            + "         \"hostName\":\"System Host\",\n"
            + "         \"modDate\":\"2024-01-08 14:23:12.916\",\n"
            + "         \"publishDate\":\"2024-01-08 14:23:12.949\",\n"
            + "         \"title\":\"Title #1\",\n"
            + "         \"baseType\":\"CONTENT\",\n"
            + "         \"inode\":\"2bc1b936-f207-4c7b-bcdd-994201a48faa\",\n"
            + "         \"archived\":false,\n"
            + "         \"host\":\"SYSTEM_HOST\",\n"
            + "         \"working\":true,\n"
            + "         \"locked\":false,\n"
            + "         \"stInode\":\"0d45528d55f0cb09bf7b9af7ef72525c\",\n"
            + "         \"contentType\":\"dotAnnouncement\",\n"
            + "         \"live\":true,\n"
            + "         \"owner\":\"dotcms.org.1\",\n"
            + "         \"identifier\":\"7124dc77192b51ea6c7e9672cd8f564d\",\n"
            + "         \"languageId\":1,\n"
            + "         \"type1\":\"Announcement\",\n"
            + "         \"url\":\"https://demo.dotcms.com\",\n"
            + "         \"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\n"
            + "         \"modUserName\":\"Admin User\",\n"
            + "         \"hasLiveVersion\":true,\n"
            + "         \"folder\":\"SYSTEM_FOLDER\",\n"
            + "         \"hasTitleImage\":false,\n"
            + "         \"sortOrder\":0,\n"
            + "         \"modUser\":\"dotcms.org.1\",\n"
            + "         \"__icon__\":\"contentIcon\",\n"
            + "         \"contentTypeIcon\":\"announcement\",\n"
            + "         \"variant\":\"DEFAULT\"\n"
            + "      }\n"
            + "   ]\n"
            + "}";


    /**
     * Given scenario:  Simply test various configurations of the DateTimeFormatter
     * Expected result: We test that the formatter works as expected
     */
    @Test
    public void testDateTimeFormatter() {

        int maxFractionDigits = 3; // Adjust as needed

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, maxFractionDigits, true)
                .toFormatter();

        // Example usage:
        String dateTimeString1 = "2024-01-08 18:10:27.9";
        String dateTimeString2 = "2024-01-08 18:10:27.99";
        String dateTimeString3 = "2024-01-08 18:10:27.999";
        String dateTimeString4 = "2024-01-08 23:36:00.358";
        String dateTimeString5 = "2024-01-08 00:00:00.0";
        String dateTimeString6 = "2024-01-08 00:00:00";

        // Parse strings to LocalDateTime
         LocalDateTime localDateTime1 = LocalDateTime.parse(dateTimeString1, formatter);
         Assert.assertNotNull(localDateTime1.toInstant(java.time.ZoneOffset.UTC));

         LocalDateTime localDateTime2 = LocalDateTime.parse(dateTimeString2, formatter);
         Assert.assertNotNull(localDateTime2.toInstant(java.time.ZoneOffset.UTC));

         LocalDateTime localDateTime3 = LocalDateTime.parse(dateTimeString3, formatter);
         Assert.assertNotNull(localDateTime3.toInstant(java.time.ZoneOffset.UTC));

         LocalDateTime localDateTime4 = LocalDateTime.parse(dateTimeString4, formatter);
         Assert.assertNotNull(localDateTime4.toInstant(java.time.ZoneOffset.UTC));

         LocalDateTime localDateTime5 = LocalDateTime.parse(dateTimeString5, formatter);
         Assert.assertNotNull(localDateTime5.toInstant(java.time.ZoneOffset.UTC));

         LocalDateTime localDateTime6 = LocalDateTime.parse(dateTimeString6, formatter);
         Assert.assertNotNull(localDateTime6.toInstant(java.time.ZoneOffset.UTC));

    }

}
