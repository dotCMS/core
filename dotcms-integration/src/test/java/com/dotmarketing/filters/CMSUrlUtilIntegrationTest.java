package com.dotmarketing.filters;

import static com.dotcms.datagen.TestDataUtils.getDotAssetLikeContentlet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.LicenseTestUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.filters.CMSFilter.IAmSubType;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CMSUrlUtilIntegrationTest {
    private static final String TEST_PATTERN = "/testpattern";
    private static ContentletAPI contentletAPI;
    private static Host site;
    private static User systemUser;
    private static long defaultLanguageId;
    private CMSUrlUtil cmsUrlUtil;
    private static HTMLPageAsset detailPage1;
    private static HTMLPageAsset detailPage2;
    private static HttpSession session;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        /* APIs initialization */
        contentletAPI = APILocator.getContentletAPI();

        /* Default user */
        systemUser = APILocator.getUserAPI().getSystemUser();

        /* Default variables */
        site = new SiteDataGen().nextPersisted();
        defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        final String parent1Name = "news-events";
        final Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(site)
                .nextPersisted();
        final String parent2Name = "news";
        final Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name).parent(parent1)
                .nextPersisted();

        final String parent3Name = "news-events2";
        final Folder parent3 = new FolderDataGen().name(parent3Name).title(parent3Name).site(site)
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
                .pageURL("index")
                .title("index")
                .nextPersisted();


        final HttpServletRequest request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
    }

    @Before
    public void prepareTest() {
        cmsUrlUtil = CMSUrlUtil.getInstance();
    }

    /**
     * methodToTest {@link CMSUrlUtil#resolvePageAssetSubtype(String, Host, Long)}
     * Given Scenario: A Content Type with a URLMap pattern and a detail page exists
     * ExpectedResult: Should return a {@link Tuple2} object with true and IAmSubType.PAGE_URL_MAP
     */
    @Test
    public void shouldReturnATupleWithTrueAndPageUrlMap() {

        // Given
        // Create a Content Type with a URLMap pattern and a detail page
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Field field = new FieldDataGen().dataType(DataTypes.INTEGER).next();
        final String urlMapper = newsPatternPrefix  + "{" + field.variable() + "}";

        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(detailPage1.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        final Contentlet newsTestContent = new ContentletDataGen(contentType.id())
                .setProperty(field.variable(), 2)
                .languageId(1)
                .host(site)
                .nextPersisted();

        //When
        final Tuple2<Boolean, IAmSubType> pageAssetSubtype =
                cmsUrlUtil.resolvePageAssetSubtype(
                        newsPatternPrefix + newsTestContent.getStringProperty(field.variable()),
                        site, defaultLanguageId);

        assertTrue(pageAssetSubtype._1());
        assertEquals(IAmSubType.PAGE_URL_MAP, pageAssetSubtype._2());

    }

    /**
     * methodToTest {@link CMSUrlUtil#resolvePageAssetSubtype(String, Host, Long)}
     * Given Scenario: A Content Type without a URLMap pattern
     * ExpectedResult: Should return a {@link Tuple2} object with false and IAmSubType.NONE
     */
    @Test
    public void shouldReturnATupleWithFalseAndNone() {
        // Given
        // Create a Content Type as a Contentlet Type
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Field field = new FieldDataGen().dataType(DataTypes.INTEGER).next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .nextPersisted();

        final Contentlet newsTestContent = new ContentletDataGen(contentType.id())
                .setProperty(field.variable(), 2)
                .languageId(1)
                .host(site)
                .nextPersisted();

        // When
        final Tuple2<Boolean, IAmSubType> pageAssetSubtype =
                cmsUrlUtil.resolvePageAssetSubtype(
                        newsPatternPrefix,
                        site, defaultLanguageId);

        assertFalse(pageAssetSubtype._1());
        assertEquals(IAmSubType.NONE, pageAssetSubtype._2());
    }

    /**
     * methodToTest {@link CMSUrlUtil#resolvePageAssetSubtype(String, Host, Long)}
     * Given Scenario: A HtmlPage with a detail page and without a URLMap pattern
     * ExpectedResult: Should return a {@link Tuple2} object with true and IAmSubType.NONE
     */
    @Test
    public void shouldReturnATupleWithTrueAndNone() {
        // Given
        final String newsPatternPrefix = "/news-events/news/news-detail";
        final Contentlet newsTestContent = getDotAssetLikeContentlet();

        // When
        final Tuple2<Boolean, IAmSubType> pageAssetSubtype =
                cmsUrlUtil.resolvePageAssetSubtype(
                        newsPatternPrefix,
                        site, defaultLanguageId);

        assertTrue(pageAssetSubtype._1());
        assertEquals(IAmSubType.NONE, pageAssetSubtype._2());
    }

    /**
     * methodToTest {@link CMSUrlUtil#resolveResourceType(IAm, String, Host, long)}
     * Given Scenario: A Content Type with a URLMap pattern and a detail page exists
     * ExpectedResult: Should return a {@link Tuple2} object with IAm.PAGE and IAmSubType.PAGE_URL_MAP
     */
    @Test
    public void whenResolveResourceType_shouldReturnATupleWithPageAndPageUrlMap() {
        // Given
        // Create a Content Type with a URLMap pattern and a detail page
        final String newsPatternPrefix =
                TEST_PATTERN + System.currentTimeMillis() + "/";

        final Field field = new FieldDataGen().dataType(DataTypes.INTEGER).next();
        final String urlMapper = newsPatternPrefix  + "{" + field.variable() + "}";

        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(detailPage1.getIdentifier())
                .urlMapPattern(urlMapper)
                .nextPersisted();

        final Contentlet newsTestContent = new ContentletDataGen(contentType.id())
                .setProperty(field.variable(), 2)
                .languageId(1)
                .host(site)
                .nextPersisted();

        final Tuple2<IAm, IAmSubType> type = cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS,
                newsPatternPrefix + newsTestContent.getStringProperty(field.variable()),
                site, defaultLanguageId);

        assertEquals(IAm.PAGE, type._1());
        assertEquals(IAmSubType.PAGE_URL_MAP, type._2());

    }

    /**
     * methodToTest {@link CMSUrlUtil#resolveResourceType(IAm, String, Host, long)}
     * Given Scenario: A HtmlPage with a detail page and without a URLMap pattern associated
     * ExpectedResult: Should return a {@link Tuple2} object with IAm.PAGE and IAmSubType.NONE
     */
    @Test
    public void whenResolveResourceType_shouldReturnATupleWithPageAndNone() {
        // Given
        final String newsPatternPrefix = "/news-events/news/news-detail";
        final Contentlet newsTestContent = getDotAssetLikeContentlet();

        // When
        final Tuple2<IAm, IAmSubType> type = cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS,
                newsPatternPrefix,
                site, defaultLanguageId);

        assertEquals(IAm.PAGE, type._1());
        assertEquals(IAmSubType.NONE, type._2());
    }

    /**
     * methodToTest {@link CMSUrlUtil#resolveResourceType(IAm, String, Host, long)}
     * Given Scenario: A site with folder structure exists
     * ExpectedResult: Should return a {@link Tuple2} object with IAm.FOLDER and IAmSubType.NONE
     */
    @Test
    public void whenResolveResourceType_shouldReturnATupleWithFolderAndNone() {
        // Given
        final String newsPatternPrefix = "/news-events/news/";

        // When
        final Tuple2<IAm, IAmSubType> type = cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS,
                newsPatternPrefix,
                site, defaultLanguageId);

        assertEquals(IAm.FOLDER, type._1());
        assertEquals(IAmSubType.NONE, type._2());
    }

    /**
     * methodToTest {@link CMSUrlUtil#resolveResourceType(IAm, String, Host, long)}
     * Given Scenario: A site with folder structure exists and an index page is inside the folder
     * ExpectedResult: Should return a {@link Tuple2} object with IAm.PAGE and IAmSubType.PAGE_INDEX
     */
    @Test
    public void whenResolveResourceType_shouldReturnATupleWithPageAndPageIndex() {
        // Given
        final String newsPatternPrefix = "/news-events2/news2/";

        // When
        final Tuple2<IAm, IAmSubType> type = cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS,
                newsPatternPrefix,
                site, defaultLanguageId);

        assertEquals(IAm.PAGE, type._1());
        assertEquals(IAmSubType.PAGE_INDEX, type._2());
    }

}
