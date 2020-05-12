package com.dotcms.enterprise;

import static com.dotcms.rendering.velocity.directive.ParseContainer.getDotParserContainerUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.repackage.org.jsoup.nodes.Document;
import com.dotcms.repackage.org.jsoup.nodes.Element;
import com.dotcms.repackage.org.jsoup.select.Elements;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class HTMLDiffUtilTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static MultiTreeAPI multiTreeAPI;
    private static String contentGenericId;
    private static Host site;
    private static Template template;
    private static Folder folder;
    private static User systemUser;
    private static Container container;
    private static Language defaultLang;
    private static String uuid;

    private static final String NOTHING_CHANGED = "Nothing Changed";

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        contentletAPI = APILocator.getContentletAPI();
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        multiTreeAPI = APILocator.getMultiTreeAPI();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        defaultLang = languageAPI.getDefaultLanguage();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
        contentGenericId = contentGenericType.id();

        final String nameTitle = "anyTestContainer" + System.currentTimeMillis();
        uuid = UUIDGenerator.generateUuid();
        container = new ContainerDataGen()
                .withContentType(contentGenericType, "$!{body}")
                .friendlyName(nameTitle)
                .title(nameTitle)
                .nextPersisted();

        PublishFactory.publishAsset(container, systemUser, false, false);
        template = new TemplateDataGen().withContainer(container.getIdentifier(), uuid)
                .nextPersisted();

        folder = new FolderDataGen().site(site).nextPersisted();

        PublishFactory.publishAsset(template, systemUser, false, false);
    }

    /**
     * Given scenario: We have a page created out of a layout and a container. The Container holds a List of items.
     * Expected Result:  We create a working copy and modify the list of items. The new items must replace the old ones.
     * @throws Exception
     */
    @Test
    public void Test_Page_With_Changes_Expect_Difference() throws Exception {

        final String pageName = "seekers-expect-difference-page";

        final Set<String> seekers = ImmutableSet.of("Skywarp", "Starscream", "Thundercracker");

        final Set<String> coneheads = ImmutableSet.of("Thrust", "Ramjet", "Dirge");

        final Contentlet contentlet = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang.getId())
                .folder(folder)
                .host(site)
                .setProperty("title", "seekers")
                .setProperty("body", String.join(",", seekers))
                .nextPersisted();

        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet, systemUser, false);

        final HTMLPageAsset pageLive = new HTMLPageDataGen(folder, template).languageId(defaultLang.getId())
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();

        final MultiTree multiTreeV1 = new MultiTree(pageLive.getIdentifier(),
                container.getIdentifier(), contentlet.getIdentifier(),
                getDotParserContainerUUID(uuid), 0);
        multiTreeAPI.saveMultiTree(multiTreeV1);
        HTMLPageDataGen.publish(pageLive);

        final Contentlet workingPage = contentletAPI
                .checkout(pageLive.getInode(), systemUser, false);
        final Contentlet checkedOut = contentletAPI
                .checkout(contentlet.getInode(), systemUser, false);
        checkedOut.setProperty("body", String.join(",", coneheads));
        contentletAPI.checkin(checkedOut, systemUser, false);
        contentletAPI.checkin(workingPage, systemUser, false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/"+pageName);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
                .thenReturn(systemUser.getUserId());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        when(request.getParameter("host_id")).thenReturn(site.getIdentifier());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(defaultLang.getId()+"");
        when(session.getAttribute(WebKeys.PAGE_MODE_SESSION)).thenReturn(PageMode.PREVIEW_MODE);

        // Collection to store attributes keys/values
        final Map<String, Object> attributes = new ConcurrentHashMap<>();

        // Mock setAttribute
        Mockito.doAnswer((Answer<Void>) invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            final Object value = invocation.getArgumentAt(1, Object.class);
            attributes.put(key, value);
            return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.anyObject());

        // Mock getAttribute
        Mockito.doAnswer((Answer<Object>) invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            return attributes.get(key);
        }).when(request).getAttribute(Mockito.anyString());

        final String diff = HTMLDiffUtil.htmlDiffPage(pageLive, systemUser, request, response);
        Assert.assertNotEquals(NOTHING_CHANGED,diff);
        final Document document = Jsoup.parse(diff);
        final Elements removedElements = document.select("span.diff-html-removed");
        for (final Element removedElement : removedElements) {
           Assert.assertTrue(seekers.contains(removedElement.text()));
        }

        final Elements addedElements = document.select("span.diff-html-added");
        for (final Element addedElement : addedElements) {
            Assert.assertTrue(coneheads.contains(addedElement.text()));
        }
    }


    /**
     *Given scenario: We have a page created out of a layout and a container. The Container holds a List of items.
     * Expected Result:  We create a working copy od the page but this one isn't modified. So whn compared
     * @throws Exception
     */
    @Test
    public void Test_Page_No_Changes_Expect_Nothing_Changed_Message() throws Exception {

        final String pageName = "seekers-expect-No-difference-page";

        final Contentlet contentlet = new ContentletDataGen(contentGenericId)
                .languageId(defaultLang.getId())
                .folder(folder)
                .host(site)
                .setProperty("title", "seekers")
                .setProperty("body", "Skywarp, Starscream, Thundercracker")
                .nextPersisted();

        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet, systemUser, false);

       final HTMLPageAsset pageLive = new HTMLPageDataGen(folder, template).languageId(defaultLang.getId())
                .pageURL(pageName)
                .friendlyName(pageName)
                .title(pageName).nextPersisted();

        final MultiTree multiTreeV1 = new MultiTree(pageLive.getIdentifier(),
                container.getIdentifier(), contentlet.getIdentifier(),
                getDotParserContainerUUID(uuid), 0);
        multiTreeAPI.saveMultiTree(multiTreeV1);
        HTMLPageDataGen.publish(pageLive);

        //Force a working copy identical to the original
        final Contentlet workingPage = contentletAPI.checkout(pageLive.getInode(), systemUser, false);
        contentletAPI.checkin(workingPage, systemUser, false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);

        when(request.getRequestURI()).thenReturn("/"+pageName);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER_ID))
                .thenReturn(systemUser.getUserId());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(systemUser);

        when(request.getParameter("host_id")).thenReturn(site.getIdentifier());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(defaultLang.getId()+"");
        when(session.getAttribute(WebKeys.PAGE_MODE_SESSION)).thenReturn(PageMode.PREVIEW_MODE);

        // Collection to store attributes keys/values
        final Map<String, Object> attributes = new ConcurrentHashMap<>();

        // Mock setAttribute
        Mockito.doAnswer((Answer<Void>) invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            final Object value = invocation.getArgumentAt(1, Object.class);
            attributes.put(key, value);
            return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.anyObject());

        // Mock getAttribute
        Mockito.doAnswer((Answer<Object>) invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            return attributes.get(key);
        }).when(request).getAttribute(Mockito.anyString());

        final String diff = HTMLDiffUtil.htmlDiffPage(pageLive, systemUser, request, response);
        Assert.assertEquals(NOTHING_CHANGED, diff);
    }

}
