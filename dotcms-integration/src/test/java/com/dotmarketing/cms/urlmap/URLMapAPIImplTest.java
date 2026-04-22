package com.dotmarketing.cms.urlmap;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class URLMapAPIImplTest {
    public static final String TEST_PATTERN = "/testpattern";
    private static User systemUser;
    private static Host host;

    private URLMapAPIImpl urlMapAPI;
    private static HTMLPageAsset detailPage1;
    private static HTMLPageAsset detailPage2;
    private static HttpSession session;

    @Before
    public void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        host = new SiteDataGen().nextPersisted();

        final String parent1Name = "news-events";
        final Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(host)
                .nextPersisted();
        final String parent2Name = "news";
        final Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name).parent(parent1)
                .nextPersisted();

        final String parent3Name = "news-events2";
        final Folder parent3 = new FolderDataGen().name(parent3Name).title(parent3Name).site(host)
                .nextPersisted();
        final String parent4Name = "news2";
        final Folder parent4 = new FolderDataGen().name(parent4Name).title(parent4Name).parent(parent3)
                .nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        detailPage1 = new HTMLPageDataGen(parent2, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();

        detailPage2 = new HTMLPageDataGen(parent4, template)
                .pageURL("news-detail2")
                .title("news-detail2")
                .nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
    }

    @Before
    public void prepareTest() {
        urlMapAPI = new URLMapAPIImpl();
    }

    /**
     * UrlMapped Content that lives on the system host should be available on every host where the map
     * detail page exists (by path). If content does not live on the system host, it should only be
     * available on the host it is currently one
     * 
     * @throws Exception
     */


    //@Test
    public void systemHost_UrlMaps_Should_Work_Across_Hosts() throws Exception {

        final Host host1 = new SiteDataGen().nextPersisted();
        final Host host2 = new SiteDataGen().nextPersisted();
        final Host host3 = new SiteDataGen().nextPersisted();
        
        final Folder folder1 = new FolderDataGen().name("folder").title("folder").site(host1).nextPersisted();
        final Folder folder2 = new FolderDataGen().name("folder").title("folder").site(host2).nextPersisted();



        final Template template1 = new TemplateDataGen().site(host1).nextPersisted();
        final Template template2 = new TemplateDataGen().site(host2).nextPersisted();

        final HTMLPageAsset pageOnHost1 = new HTMLPageDataGen(folder1, template1).pageURL("my-detail")
                        .title("my-detail").nextPersisted();

        final HTMLPageAsset pageOnHost2 = new HTMLPageDataGen(folder2, template2).pageURL("my-detail")
                        .title("my-detail").nextPersisted();

        final String urlPattern = "/" + UUIDGenerator.shorty() + "/{urlTitle}";
        
        // set the detail page to page on host1
        final ContentType contentType1 = getNewsLikeContentType("News" + System.currentTimeMillis(),
                        APILocator.systemHost(), pageOnHost1.getIdentifier(), urlPattern);


        final Contentlet newsOnSystemHost = TestDataUtils.getNewsContent(true,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), contentType1.id(),
                        APILocator.systemHost(), null, UUIDGenerator.generateUuid());


        final Contentlet newsOnHost1 = TestDataUtils.getNewsContent(true,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), contentType1.id(), host1,
                        null, UUIDGenerator.generateUuid());

        final Contentlet newsOnHost2 = TestDataUtils.getNewsContent(true,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(), contentType1.id(), host2,
                        null, UUIDGenerator.generateUuid());


        
        // WORKS : trying where contentlet on the system host and request is on host1 
        UrlMapContext context = getUrlMapContext(systemUser, host1,
                        urlPattern.replace("{urlTitle}", newsOnSystemHost.getStringProperty("urlTitle")));

        Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);
        URLMapInfo urlMapInfo = urlMapInfoOptional.get();

        assertEquals(newsOnSystemHost.getStringProperty("title"), urlMapInfo.getContentlet().getName());
        assertEquals(pageOnHost1.getURI(), urlMapInfo.getIdentifier().getURI());
        

        // WORKS :  trying where contentlet on the system host and request is on host2, which also has a detail page in the right place
        context = getUrlMapContext(systemUser, host2,
                        urlPattern.replace("{urlTitle}", newsOnSystemHost.getStringProperty("urlTitle")));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);
        urlMapInfo = urlMapInfoOptional.get();

        assertEquals(newsOnSystemHost.getStringProperty("title"), urlMapInfo.getContentlet().getName());
        assertEquals(pageOnHost2.getURI(), urlMapInfo.getIdentifier().getURI());
        
        
        
        // FAIL : trying where contentlet on the system host and request is on host3, which does not have a detail page
        context = getUrlMapContext(systemUser, host3,
                        urlPattern.replace("{urlTitle}", newsOnSystemHost.getStringProperty("urlTitle")));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);
        assert(urlMapInfoOptional.isEmpty());

        
        
        // WORKS :  trying where contentlet on host1 and request is on host1 
        context = getUrlMapContext(systemUser, host1,
                        urlPattern.replace("{urlTitle}", newsOnHost1.getStringProperty("urlTitle")));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);
        urlMapInfo = urlMapInfoOptional.get();

        assertEquals(newsOnHost1.getStringProperty("title"), urlMapInfo.getContentlet().getName());
        assertEquals(pageOnHost1.getURI(), urlMapInfo.getIdentifier().getURI());
        
        
        
        // FAIL : trying where contentlet on the host1 and request ison host2 
        context = getUrlMapContext(systemUser, host2,
                        urlPattern.replace("{urlTitle}", newsOnHost1.getStringProperty("urlTitle")));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assert(urlMapInfoOptional.isEmpty());
        
        
        
        // WORKS : where contentlet on the host2 and request is on host2 
        context = getUrlMapContext(systemUser, host2,
                        urlPattern.replace("{urlTitle}", newsOnHost2.getStringProperty("urlTitle")));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);
        urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsOnHost2.getStringProperty("title"), urlMapInfo.getContentlet().getName());
        assertEquals(pageOnHost2.getURI(), urlMapInfo.getIdentifier().getURI());
        
        
        


    }
    
    
    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists
     * ExpectedResult: Should return a {@link URLMapInfo} with the right content and detail page
     */
    @Test
    public void shouldReturnContentletWhenTheContentExists()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix);
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals(newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"),
                urlMapInfo.getContentlet().getStringProperty("urlMap"));
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }



    /**
     * MethodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url with a field that doesn't exist
     * ExpectedResult: Should return an empty {@link URLMapInfo}
     */
    @Test
    public void processURLMapMethodShouldNotFailWithInvalidFields()
            throws DotDataException, DotSecurityException {
        final String patternPrefix = TEST_PATTERN + System.currentTimeMillis() + "/";


        final Field field = new FieldDataGen().next();
        final String urlMapper = patternPrefix  + "{nonValidField}";
        new ContentTypeDataGen()
                .field(field)
                .detailPage(detailPage1.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                patternPrefix + "anyFieldValue");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);
        
        assertNotNull(urlMapInfoOptional);
        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * Testing {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Multiple Content Types could have the same URLMap pattern,
     * the {@link URLMapAPIImpl#processURLMap(UrlMapContext)} should be able to handle that case.
     * On this test both the Content Type and Content exists.
     * ExpectedResult: Should return a {@link URLMapInfo} with the right content and detail pages
     */
    @Test
    public void test_multiple_content_types_using_same_pattern()
            throws DotDataException, DotSecurityException {

        // Create two content types using the same URL patterns
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        final Contentlet newsTestContent1 = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage1);
        final Contentlet newsTestContent2 = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage2);

        //*************
        // Looking for the second content
        UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent2.getStringProperty("urlTitle"));

        Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent2.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events2/news2/news-detail2", urlMapInfo.getIdentifier().getURI());

        //*************
        // Looking for the first content
        context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent1.getStringProperty("urlTitle"));

        urlMapInfoOptional = urlMapAPI.processURLMap(context);

        urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent1.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * Testing {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists when
     * the URLMap pattern is at the root of the URI: /{urlTitle}
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content and detail page
     */
    @Test
    public void test_url_map_pattern_at_root()
            throws DotDataException, DotSecurityException {

        // Create a Content Type with a URLMap pattern at the root of the URI -> /{urlTitle}
        final String newsPatternPrefix = "/";
        final Contentlet newsTestContent1 = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage1);

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent1.getStringProperty("urlTitle"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent1.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * Testing {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists when
     * the URLMap pattern is at the root of the URI: /{urlTitle} and the url title has slashes on it.
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content and detail page
     */
    @Test
    public void test_url_map_pattern_at_root_with_slash_in_url_title()
            throws DotDataException, DotSecurityException {

        // Create a Content Type with a URLMap pattern at the root of the URI -> /{urlTitle}
        final String newsPatternPrefix = "/";
        final Contentlet newsTestContent1 = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage1, "test/title/" + System.currentTimeMillis());

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent1.getStringProperty("urlTitle"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent1.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * Testing {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists and
     * the url title has slashes on it.
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content and detail page
     */
    @Test
    public void test_url_map_pattern_with_slash_in_url_title()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage1, "test/" + System.currentTimeMillis());

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * Testing {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists and
     * the url title has slashes on it.
     * This test uses a more complex pattern /something/{urlTitle-with-slashes}/{title-with-spaces-on-it}
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content and detail page
     */
    @Test
    public void test_url_map_pattern_with_slash_in_url_title_complex_rul_map_pattern()
            throws DotDataException, DotSecurityException {

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix,
                new Date(), detailPage1, "test/" + System.currentTimeMillis(),
                newsPatternPrefix + "{urlTitle}" + "/" + "{title}");

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle")
                        + "/" + newsTestContent.getStringProperty("title"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when the Content Type exists but the content nos exists
     * ExpectedResult: Should return a {@link Optional#empty()}
     */
    @Test
    public void shouldReturnNullWhenTheContentNotExists()
            throws DotDataException, DotSecurityException {

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";
        createURLMapperContentType(newsPatternPrefix);

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + "not-exists-content");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#isUrlPattern(UrlMapContext)}
     * Given Scenario: Request a URL with the api prefix
     * ExpectedResult: Should return false, even when the Content Type and the Content exists
     */
    @Test
    public void shouldNotMatchUrlStaringWithAPI()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                "/testpattern" + System.currentTimeMillis() + "/";
        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix);

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                    "/api" + newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        assertFalse(urlMapAPI.isUrlPattern(context));
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Request a not detail page's URL
     * ExpectedResult: Should return false
     */
    @Test
    public void shouldReturnNullWhenThePageUriIsNotDetailPage()
            throws DotDataException, DotSecurityException {

        final Folder folder = new FolderDataGen().nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .nextPersisted();

        final UrlMapContext context = getUrlMapContext(systemUser, host, page.getURI());

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Request a detail page's URL, but with limited user who not have READ permission to the content
     * ExpectedResult: Should throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserDontHavePermission()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                "/testpattern" + System.currentTimeMillis() + "/";
        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix);

        final User newUser = new UserDataGen().nextPersisted();

        final UrlMapContext context = getUrlMapContext(newUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        urlMapAPI.processURLMap(context);
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process dotAdmin URL when not any URL Mapper exists
     * ExpectedResult: Should return a {@link Optional#empty()}
     */
    @Test
    @Ignore
    public void processURLMapWithoutUrlMap() throws DotDataException, DotSecurityException {
        deleteAllUrlMapperContentType();
        final UrlMapContext context = getUrlMapContext(systemUser, host, "/dotAdmin");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#isUrlPattern(UrlMapContext)}}
     * Given Scenario: Call  isUrlPattern with dotAdmin URL when not any URL Mapper exists
     * ExpectedResult: Should return false
     */
    @Test
    public void isUrlPatternWithoutUrlMap() throws DotDataException, DotSecurityException {
        final UrlMapContext context = getUrlMapContext(systemUser, host, "/dotAdmin");
        assertFalse(urlMapAPI.isUrlPattern(context));
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists, but the public date for
     * the content is in the future and the mode requested is {@link PageMode#LIVE}
     * ExpectedResult: Should return a {@link Optional#empty()}
     */
    @Test
    public void shouldReturnEmptyOptionalWhenContentExistsInFuture()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final Date tomorrow = calendar.getTime();

        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix, tomorrow);
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"), PageMode.LIVE);

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists, and the public date for
     * the content is in the future, but the time machine is set to a day after the publish date
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content ans detail page
     */
    @Test
    public void shouldReturnURLInfoWhenContentExistsInFutureButTimeMachineIsSet()
            throws DotDataException, DotSecurityException {

        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final Date tomorrow = calendar.getTime();

        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix, tomorrow);
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"), PageMode.LIVE);

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final Date afterTomorrow = calendar.getTime();

        when(session.getAttribute("tm_date")).thenReturn(String.valueOf(afterTomorrow.getTime()));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists, and the public date for
     * the content is in the future, and the mode requested is {@link PageMode#PREVIEW_MODE}
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content ans detail page
     */
    @Test
    public void shouldReturnInPREVIEW_MODEWhenContentExistsInFuture()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        final Date tomorrow = calendar.getTime();

        final Contentlet newsTestContent = createURLMapperContentType(newsPatternPrefix, tomorrow);
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"), PageMode.PREVIEW_MODE);

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    private static void deleteAllUrlMapperContentType() throws DotDataException {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final List<SimpleStructureURLMap> structureURLMapPatterns =
                contentTypeAPI.findStructureURLMapPatterns();

        structureURLMapPatterns
                .stream()
                .map((SimpleStructureURLMap simpleStructureURLMap) -> simpleStructureURLMap.getInode())
                .forEach(contenTypeInode -> {
                    try {
                        contentTypeAPI.delete(contentTypeAPI.find(contenTypeInode));
                    } catch (DotSecurityException | DotDataException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix) {
        return createURLMapperContentType(newsPatternPrefix, new Date());
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix,
            final Date sysPublishDate) {
        return createURLMapperContentType(newsPatternPrefix, sysPublishDate, null);
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix,
            final Date sysPublishDate, final HTMLPageAsset detailPageToUse) {
        return createURLMapperContentType(newsPatternPrefix, sysPublishDate, detailPageToUse, null,
                null);
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix,
            final Date sysPublishDate, final HTMLPageAsset detailPageToUse, final String urlTitle) {
        return createURLMapperContentType(newsPatternPrefix, sysPublishDate, detailPageToUse,
                urlTitle, null);
    }

    private static Contentlet createURLMapperContentType(final String newsPatternPrefix,
            final Date sysPublishDate, final HTMLPageAsset detailPageToUse, final String urlTitle,
            final String urlMapPattern) {

        HTMLPageAsset detailPage = detailPage1;
        if (null != detailPageToUse) {
            detailPage = detailPageToUse;
        }

        String urlMapPatternToUse = newsPatternPrefix + "{urlTitle}";
        if (null != urlMapPattern) {
            urlMapPatternToUse = urlMapPattern;
        }

        final ContentType newsContentType = getNewsLikeContentType(
                "News" + System.currentTimeMillis(),
                host,
                detailPage.getIdentifier(),
                urlMapPatternToUse);

        return TestDataUtils
                .getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        newsContentType.id(), host, sysPublishDate, urlTitle);

    }

    private UrlMapContext getUrlMapContext(final User systemUser, final Host host, final String uri, final PageMode pageMode) {
        return UrlMapContextBuilder.builder()
                .setHost(host)
                .setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setMode(pageMode)
                .setUri(uri)
                .setUser(systemUser)
                .build();
    }

    private UrlMapContext getUrlMapContext(final User systemUser, final Host host, final String uri) {
        return getUrlMapContext(systemUser, host, uri, PageMode.PREVIEW_MODE);
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: The Content Type's detail page is configured on a different (global) host
     *   using a custom Page content type. The current request host does NOT have any page at the
     *   same path. This reproduces the runtime 404 from issue #35268: URL mapping failed silently
     *   when the configured detail page lived on a different host.
     * ExpectedResult: processURLMap must fall back to the configured detail-page identifier
     *   (from the other host) rather than returning empty, so the URL map resolves successfully.
     */
    @Test
    public void processURLMap_detailPageOnDifferentHost_shouldFallBackToConfiguredIdentifier()
            throws DotDataException, DotSecurityException {

        // A separate "global" host that owns the shared detail page
        final Host globalHost = new SiteDataGen().nextPersisted();
        final Folder globalFolder = new FolderDataGen()
                .name("global-detail-pages-" + System.currentTimeMillis())
                .site(globalHost)
                .nextPersisted();

        // Custom Page content type on the global host (simulates "Landing Page")
        final ContentType customPageType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.HTMLPAGE)
                .host(globalHost)
                .name("LandingPage" + System.currentTimeMillis())
                .nextPersisted();

        final Template globalTemplate = new TemplateDataGen().site(globalHost).nextPersisted();

        // Detail page using the custom Page content type — lives ONLY on globalHost
        final Contentlet detailPageContentlet = new ContentletDataGen(customPageType)
                .host(globalHost)
                .folder(globalFolder)
                .setProperty(HTMLPageAssetAPI.URL_FIELD, "global-detail-" + System.currentTimeMillis())
                .setProperty(HTMLPageAssetAPI.TITLE_FIELD, "Global Detail Page")
                .setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, globalTemplate.getIdentifier())
                .setProperty(HTMLPageAssetAPI.FRIENDLY_NAME_FIELD, "Global Detail Page")
                .setProperty(HTMLPageAssetAPI.CACHE_TTL_FIELD, "0")
                .nextPersisted();

        final String detailPageIdentifierId = detailPageContentlet.getIdentifier();

        // Content Type on the regular host, pointing to the global host's custom-type detail page
        final String urlPattern = "/global-test-" + System.currentTimeMillis() + "/{urlTitle}";
        final ContentType urlMappedType = getNewsLikeContentType(
                "GlobalNews" + System.currentTimeMillis(),
                host,
                detailPageIdentifierId,
                urlPattern);

        // Content item on the regular host
        final Contentlet newsContent = TestDataUtils.getNewsContent(
                true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                urlMappedType.id(),
                host,
                null,
                null);

        // Request the URL-mapped URL from the regular host
        // (the regular host has no page at the same path as globalHost's detail page)
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                urlPattern.replace("{urlTitle}", newsContent.getStringProperty("urlTitle")));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertTrue("processURLMap should fall back to the configured detail page on globalHost",
                urlMapInfoOptional.isPresent());
        assertEquals("URLMapInfo identifier should be the configured detail page on globalHost",
                detailPageIdentifierId,
                urlMapInfoOptional.get().getIdentifier().getId());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Content Type with URL pattern is registered on SYSTEM_HOST (global), but
     *   its configured detail page lives on siteA. A content item matching the URL pattern was
     *   created on siteB (a different site). The request comes in on siteA.
     *   This is the cross-site URL-map scenario from issue #35268: a list page on siteA shows
     *   content from multiple sites, and clicking a siteB item navigates to siteA's detail page.
     * ExpectedResult: processURLMap must resolve the siteB content and return the siteA detail page.
     *   The initial ES query (restricted to siteA + SYSTEM_HOST) finds nothing because the content
     *   is on siteB; the fallback site-agnostic query locates it and the siteA detail page is used.
     */
    @Test
    public void processURLMap_contentOnDifferentSite_shouldResolveViaFallback()
            throws DotDataException, DotSecurityException {

        // siteB: the site where the content lives (different from the request site / host)
        final Host siteB = new SiteDataGen().nextPersisted();

        // Content type on SYSTEM_HOST so content can be created on any site.
        // The detail page is on siteA (host), which is where the request originates.
        // Path must not begin with a prefix that matches BACKEND_FILTERED_COLLECTION using
        // startsWith (e.g. "/c" matches "/cross-site/..."), or findMatch() skips URL-map logic entirely.
        final String urlPattern = "/urlmap-cross-site-" + System.currentTimeMillis() + "/{urlTitle}";
        final ContentType urlMappedType = getNewsLikeContentType(
                "CrossSiteNews" + System.currentTimeMillis(),
                APILocator.systemHost(),
                detailPage1.getIdentifier(),
                urlPattern);

        // Content item lives on siteB — NOT on siteA (host) or SYSTEM_HOST,
        // so the host-restricted query will miss it and the fallback is required.
        // Publish (uses IndexPolicy.WAIT_FOR) so ES commits the document before we query.
        final Contentlet contentOnSiteB = ContentletDataGen.publish(
                TestDataUtils.getNewsContent(
                        true,
                        APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        urlMappedType.id(),
                        siteB,
                        null,
                        null));

        // Request the URL-mapped URL from siteA (LIVE mode to match published content).
        final UrlMapContext context = getUrlMapContext(systemUser, host,
                urlPattern.replace("{urlTitle}", contentOnSiteB.getStringProperty("urlTitle")),
                PageMode.LIVE);

        final Optional<URLMapInfo> result = urlMapAPI.processURLMap(context);

        assertTrue("processURLMap should find cross-site content via fallback query", result.isPresent());
        assertEquals("Resolved contentlet should be the one from siteB",
                contentOnSiteB.getIdentifier(),
                result.get().getContentlet().getIdentifier());
        assertEquals("Detail page should be siteA's configured detail page",
                "/news-events/news/news-detail",
                result.get().getIdentifier().getURI());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists but the field to Map is a
     * {@link com.dotcms.contenttype.model.field.DataTypes#INTEGER}
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content ans detail page
     */
    @Test
    public void shouldReturnContentletWhenTheContentExistsAndUseAIntegerFIeld()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                "integer" + TEST_PATTERN + System.currentTimeMillis() + "/";


        final Field field = new FieldDataGen().dataType(DataTypes.INTEGER).next();
        final String urlMapper = newsPatternPrefix  + "{" + field.variable() + "}";
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(detailPage1.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        final Contentlet newsTestContent = new ContentletDataGen(contentType.id())
                .setProperty(field.variable(), 2)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .host(host)
                .nextPersisted();

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty(field.variable()));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertTrue(urlMapInfoOptional.isPresent());
        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getLongProperty(field.variable()),
                urlMapInfo.getContentlet().getLongProperty(field.variable()));
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists but the field to Map is a
     * {@link com.dotcms.contenttype.model.field.DataTypes#FLOAT}
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content ans detail page
     */
    @Test
    public void shouldReturnContentletWhenTheContentExistsAndUseAFloatField()
            throws DotDataException, DotSecurityException {
        final String newsPatternPrefix =
                "float" + TEST_PATTERN + System.currentTimeMillis() + "/";


        final Field field = new FieldDataGen().dataType(DataTypes.FLOAT).next();
        final String urlMapper = newsPatternPrefix  + "{" + field.variable() + "}";
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(detailPage1.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();
        final Contentlet newsTestContent = new ContentletDataGen(contentType.id())
                .setProperty(field.variable(), 2f)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .host(host)
                .nextPersisted();

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getFloatProperty(field.variable()));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertTrue(urlMapInfoOptional.isPresent());
        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getLongProperty(field.variable()),
                urlMapInfo.getContentlet().getLongProperty(field.variable()));
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }
}
