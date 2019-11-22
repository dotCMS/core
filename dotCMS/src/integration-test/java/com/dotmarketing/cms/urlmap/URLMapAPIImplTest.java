package com.dotmarketing.cms.urlmap;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertFalse;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
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
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLMapAPIImplTest {
    public static final String TEST_PATTERN = "/testpattern";
    private static User systemUser;
    private static Host host;

    private URLMapAPIImpl urlMapAPI;
    private static HTMLPageAsset detailPage;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        host = new SiteDataGen().nextPersisted();

        final String parent1Name = "news-events";
        final Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(host)
                .nextPersisted();
        final String parent2Name = "news";
        final Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name).parent(parent1)
                .nextPersisted();

        final Template template = new TemplateDataGen().nextPersisted();
        detailPage = new HTMLPageDataGen(parent2, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();
    }

    @Before
    public void prepareTest() {
        urlMapAPI = new URLMapAPIImpl();
    }

    /**
     * methodToTest {@link URLMapAPIImpl#processURLMap(UrlMapContext)}
     * Given Scenario: Process a URL Map url when both the Content Type and Content exists
     * ExpectedResult: Should return a {@link URLMapInfo} wit the right content ans detail page
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
            throws DotDataException {
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
    public void isUrlPatternWithoutUrlMap() throws DotDataException {
        deleteAllUrlMapperContentType();
        final UrlMapContext context = getUrlMapContext(systemUser, host, "/dotAdmin");
        assertFalse(urlMapAPI.isUrlPattern(context));
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

        final ContentType newsContentType = getNewsLikeContentType(
                "News" + System.currentTimeMillis(),
                host,
                detailPage.getIdentifier(),
                newsPatternPrefix + "{urlTitle}");

        return TestDataUtils
                .getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        newsContentType.id(), host);
    }

    private UrlMapContext getUrlMapContext(final User systemUser, final Host host, final String uri) {
        return UrlMapContextBuilder.builder()
                .setHost(host)
                .setLanguageId(1L)
                .setMode(PageMode.PREVIEW_MODE)
                .setUri(uri)
                .setUser(systemUser)
                .build();
    }
}
