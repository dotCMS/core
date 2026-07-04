package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class RelationshipUtilTest {

    private static User user;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static Host host;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static long languageID;
    private static FieldAPI contentTypeFieldAPI;
    static long defaultLang;
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        hostAPI        = APILocator.getHostAPI();
        languageAPI    = APILocator.getLanguageAPI();
        languageID     = languageAPI.getDefaultLanguage().getId();
        host = hostAPI.findDefaultHost(user, false);
        contentTypeFieldAPI = APILocator.getContentTypeFieldAPI();
        defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

    @Test
    public void testFilterContentletWithLuceneQuery()
            throws DotSecurityException, DotDataException {

        final String filter = "+contentType:Youtube +(conhost:" + host.getIdentifier() + " conhost:SYSTEM_HOST) +catchall:how*";
        final List<Contentlet> testResults = RelationshipUtil
                .filterContentlet(languageID, filter, user, false);

        final List<Contentlet> expectedResults = contentletAPI
                .search(filter + " +languageId:" + languageID, 0, 0, null, user, false);

        assertNotNull(testResults);
        assertNotNull(expectedResults);
        assertEquals(expectedResults.size(), testResults.size());
        assertTrue(testResults.stream().allMatch(contentlet -> expectedResults.contains(contentlet)));
    }

    @Test
    public void testFilterContentletWithIdentifier()
            throws DotSecurityException, DotDataException {

        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();
        final Contentlet contentlet = TestDataUtils
                .getWidgetContent(true, languageID, widgetContentType.id());

        try {
            final String query = "+identifier:" + contentlet.getIdentifier() + " +languageId:" + languageID;
            final List<Contentlet> testResults = RelationshipUtil
                    .filterContentlet(languageID, contentlet.getIdentifier(), user, false);

            final List<Contentlet> expectedResults = contentletAPI
                    .search(query, 0, 0, null, user, false);

            assertNotNull(testResults);
            assertNotNull(expectedResults);
            assertEquals(expectedResults.size(), testResults.size());
            assertTrue(testResults.stream().allMatch(elem -> expectedResults.contains(elem)));
        } finally {
            if(contentlet != null && contentlet.getInode() != null){
                ContentletDataGen.remove(contentlet);
            }
        }
    }

    @Test
    public void testFilterContentletWithIdentifierAndLuceneQuery()
            throws DotSecurityException, DotDataException {

        final ContentType widgetContentType = TestDataUtils.getWidgetLikeContentType();
        final Contentlet contentlet = TestDataUtils
                .getWidgetContent(true, languageID, widgetContentType.id());

        try {
            final String filter = "+contentType:" + widgetContentType.variable();
            final List<Contentlet> testResults = RelationshipUtil
                    .filterContentlet(languageID,  contentlet.getIdentifier() + "," + filter , user, false);

            final List<Contentlet> expectedResults = contentletAPI
                    .search(filter + " +languageId:" + languageID, 0, 0, null, user, false);

            assertNotNull(testResults);
            assertNotNull(expectedResults);
            assertEquals(expectedResults.size(), testResults.size());
            assertTrue(testResults.stream().allMatch(elem -> expectedResults.contains(elem)));
        } finally {
            if (contentlet != null && contentlet.getInode() != null) {
                ContentletDataGen.remove(contentlet);
            }
        }
    }
    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }


    /**
     * Method to test: {@link RelationshipUtil#filterContentlet(long, String, User, boolean)
     * Given Scenario: ordering is consistently incorrect, in that it's not in the order received but always orders them the same way on every call.
     * ExpectedResult: The order of the contentlets should be the same as the order they are received in the filter.
     */
    @Test
    public void test_filterContentlet_shouldFollowTheFilterOrder(){

        try{
            //Create content type
            final ContentType parentContentType = createContentType("ImageSim" + System.currentTimeMillis());

            //Create Text Fields
            final String titleFieldString = "title";
            final Field field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(parentContentType.id())
                    .build();

            contentTypeFieldAPI.save(field, user);

            final ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet child1 = contentletDataGen.languageId(defaultLang).nextPersisted();
            final Contentlet child2 = contentletDataGen.languageId(defaultLang).nextPersisted();
            final Contentlet child3 = contentletDataGen.languageId(defaultLang).nextPersisted();

            final String filter = child1.getIdentifier()+","+child2.getIdentifier()+","+child3.getIdentifier();

            final List<Contentlet> testResults = RelationshipUtil
                    .filterContentlet(defaultLang,  filter, user, false);

            assertNotNull(testResults);
            assertEquals(testResults.get(0).getIdentifier(), child1.getIdentifier());
            assertEquals(testResults.get(testResults.size()-1).getIdentifier(), child3.getIdentifier());

        }catch(Exception e){
            throw new RuntimeException(e);
        }


    }

    /**
     * Method to test: {@link RelationshipUtil#filterContentlet(long, String, User, boolean)}
     * Given Scenario: An identifier is filtered requesting a language for which the contentlet has
     *   no version (it only exists in the default language) — the scenario behind issue #35862 where
     *   an article is translated to Spanish but a related contentlet only exists in English.
     * ExpectedResult: Instead of throwing a NullPointerException, the method falls back to the
     *   working version in any language and returns it (relationships are stored at the identifier
     *   level, so the fallback version is the correct one to relate).
     */
    @Test
    public void test_filterContentlet_fallsBackToAnyLanguage_whenNoVersionInTargetLanguage()
            throws DotSecurityException, DotDataException {

        final Language spanish = TestDataUtils.getSpanishLanguage();
        final ContentType authorType = createContentType("AuthorFallback" + System.currentTimeMillis());

        // Contentlet exists ONLY in the default language
        final Contentlet englishOnly = new ContentletDataGen(authorType.id())
                .languageId(defaultLang).nextPersisted();

        try {
            // Filter requesting the Spanish version of an identifier that only exists in English
            final List<Contentlet> results = RelationshipUtil
                    .filterContentlet(spanish.getId(), englishOnly.getIdentifier(), user, false);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(englishOnly.getIdentifier(), results.get(0).getIdentifier());
            // The fallback returns the working version in the available (default) language
            assertEquals(defaultLang, results.get(0).getLanguageId());
        } finally {
            if (englishOnly.getInode() != null) {
                ContentletDataGen.remove(englishOnly);
            }
        }
    }

    /**
     * Method to test: {@link RelationshipUtil#filterContentlet(long, String, User, boolean)}
     * Given Scenario: The filter contains a syntactically valid UUID that does not correspond to any
     *   existing identifier.
     * ExpectedResult: The element is skipped (no contentlet found in any language) and the method
     *   returns an empty list without throwing a NullPointerException.
     */
    @Test
    public void test_filterContentlet_skipsMissingIdentifier_withoutNPE()
            throws DotSecurityException, DotDataException {

        final String nonExistentId = UUID.randomUUID().toString();

        final List<Contentlet> results = RelationshipUtil
                .filterContentlet(defaultLang, nonExistentId, user, false);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }


}
