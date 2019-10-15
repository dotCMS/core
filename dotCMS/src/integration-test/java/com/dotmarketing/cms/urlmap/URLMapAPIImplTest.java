package com.dotmarketing.cms.urlmap;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertFalse;

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
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLMapAPIImplTest {
    private static User systemUser;
    private static Host host;

    private URLMapAPIImpl urlMapAPI;
    private static Contentlet newsTestContent;
    private static final String newsPatternPrefix =
            "/testpattern" + System.currentTimeMillis() + "/";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.getUserAPI().getSystemUser();
        host = new SiteDataGen().nextPersisted();

        final String parent1Name = "news-events";
        Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(host)
                .nextPersisted();
        final String parent2Name = "news";
        Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name).parent(parent1)
                .nextPersisted();

        Template template = new TemplateDataGen().nextPersisted();
        HTMLPageAsset contentlet = new HTMLPageDataGen(parent2, template)
                .pageURL("news-detail")
                .title("news-detail")
                .nextPersisted();

        final ContentType newsContentType = getNewsLikeContentType(
                "News" + System.currentTimeMillis(),
                host,
                contentlet.getIdentifier(),
                newsPatternPrefix + "{urlTitle}");

        newsTestContent = TestDataUtils
                .getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        newsContentType.id(), host);
    }

    @Before
    public void prepareTest() {
        urlMapAPI = new URLMapAPIImpl();
    }

    @Test
    public void shouldReturnContentletWhenTheContentExists()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        final URLMapInfo urlMapInfo = urlMapInfoOptional.get();
        assertEquals(newsTestContent.getStringProperty("title"),
                urlMapInfo.getContentlet().getName());
        assertEquals("/news-events/news/news-detail", urlMapInfo.getIdentifier().getURI());
    }

    @Test
    public void shouldReturnNullWhenTheContentNotExists()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                newsPatternPrefix
                        + "u-s-labor-department-moves-forward-on-retirement-advice-proposal-esp"
                        + System.currentTimeMillis());

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    @Test
    public void shouldNotMatchUrlStaringWithAPI()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host,
                    "/api/v1/page/render" +
                        newsPatternPrefix
                        + "u-s-labor-department-moves-forward-on-retirement-advice-proposal-esp"
                        + System.currentTimeMillis());

        assertFalse(urlMapAPI.isUrlPattern(context));
    }

    @Test
    public void shouldReturnNullWhenThePageUriIsNotDetailPage()
            throws DotDataException, DotSecurityException {

        final UrlMapContext context = getUrlMapContext(systemUser, host, "/about-us/index");

        final Optional<URLMapInfo> urlMapInfoOptional = urlMapAPI.processURLMap(context);

        assertFalse(urlMapInfoOptional.isPresent());
    }

    @Test(expected = DotSecurityException.class)
    public void shouldThrowDotSecurityExceptionWhenUserDontHavePermission()
            throws DotDataException, DotSecurityException {

        final User newUser = new UserDataGen().nextPersisted();
        final UrlMapContext context = getUrlMapContext(newUser, host,
                newsPatternPrefix + newsTestContent.getStringProperty("urlTitle"));

        urlMapAPI.processURLMap(context);
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
