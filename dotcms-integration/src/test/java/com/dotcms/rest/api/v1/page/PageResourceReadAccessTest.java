package com.dotcms.rest.api.v1.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the page READ requirement on the page content tree and page personas
 * endpoints of {@link PageResource}.
 */
public class PageResourceReadAccessTest {

    private static Host site;
    private static HTMLPageAsset publishedPage;
    private static HTMLPageAsset unpublishedPage;
    private static HTMLPageAsset archivedPage;
    private static Contentlet content;
    private static User userWithoutAccess;
    private static User reader;
    private static final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        site = new SiteDataGen().nextPersisted();
        final ContentType webPageContent = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");
        content = new ContentletDataGen(webPageContent.id())
                .languageId(1)
                .host(site)
                .setProperty("title", "content")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        userWithoutAccess = newBackendUser();
        reader = newBackendUser();
        PageRenderTestUtil.grantPageReadOnSite(site, reader);

        final PageRenderTestUtil.PageRenderTest published = PageRenderTestUtil.createPage(1, site, true);
        publishedPage = published.getPage();
        addTreeRow(publishedPage, published.getFirstContainer().getIdentifier());

        final PageRenderTestUtil.PageRenderTest unpublished = PageRenderTestUtil.createPage(1, site, false);
        unpublishedPage = unpublished.getPage();
        addTreeRow(unpublishedPage, unpublished.getFirstContainer().getIdentifier());

        final PageRenderTestUtil.PageRenderTest archived = PageRenderTestUtil.createPage(1, site, false);
        archivedPage = archived.getPage();
        addTreeRow(archivedPage, archived.getFirstContainer().getIdentifier());
        APILocator.getContentletAPI().archive(archivedPage, APILocator.systemUser(), false);
    }

    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(PageResourceReadAccessTest.class, "Could not remove test site: " + e.getMessage());
            }
        }
    }

    /**
     * Given a back-end user with no permission on the page's site or the page,
     * When the user requests the page content tree,
     * Then the request answers not found, the same as for an identifier that matches no page.
     */
    @Test
    public void contentTree_withoutReadOnPage_returnsNotFound() {
        final PageResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.getContentTree(
                mockRequest(), new EmptyHttpResponse(), publishedPage.getIdentifier()));
    }

    /**
     * Given a non-admin back-end user granted READ on the page,
     * When the user requests the page content tree,
     * Then the page's tree rows are returned.
     */
    @Test
    public void contentTree_withReadOnPage_returnsTree() throws Exception {
        final ResponseEntityView<List<MulitreeView>> tree = resourceFor(reader).getContentTree(
                mockRequest(), new EmptyHttpResponse(), publishedPage.getIdentifier());

        assertContainsContentRow(tree.getEntity(), publishedPage);
    }

    /**
     * Given an identifier that does not belong to any page,
     * When the content tree is requested,
     * Then the request answers not found.
     */
    @Test
    public void contentTree_unknownPage_returnsNotFound() {
        final PageResource resource = resourceFor(reader);

        assertThrows(DoesNotExistException.class, () -> resource.getContentTree(
                mockRequest(), new EmptyHttpResponse(), UUIDGenerator.generateUuid()));
    }

    /**
     * Given a page that has never been published and a user with READ on it,
     * When the user requests the page content tree,
     * Then the page's tree rows are returned.
     */
    @Test
    public void contentTree_unpublishedPage_withRead_returnsTree() throws Exception {
        final ResponseEntityView<List<MulitreeView>> tree = resourceFor(reader).getContentTree(
                mockRequest(), new EmptyHttpResponse(), unpublishedPage.getIdentifier());

        assertContainsContentRow(tree.getEntity(), unpublishedPage);
    }

    /**
     * Given an archived page and a user with READ on it,
     * When the user requests the page content tree,
     * Then the page's tree rows are returned.
     */
    @Test
    public void contentTree_archivedPage_withRead_returnsTree() throws Exception {
        final ResponseEntityView<List<MulitreeView>> tree = resourceFor(reader).getContentTree(
                mockRequest(), new EmptyHttpResponse(), archivedPage.getIdentifier());

        assertContainsContentRow(tree.getEntity(), archivedPage);
    }

    /**
     * Given a back-end user with no permission on the page's site or the page,
     * When the user requests the page personas,
     * Then the request answers not found, the same as for an identifier that matches no page.
     */
    @Test
    public void personasOnPage_withoutReadOnPage_returnsNotFound() {
        final PageResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> requestPersonas(resource, publishedPage.getIdentifier()));
    }

    /**
     * Given an identifier that does not belong to any page,
     * When the page personas are requested,
     * Then the request answers not found.
     */
    @Test
    public void personasOnPage_unknownPage_returnsNotFound() {
        final PageResource resource = resourceFor(reader);

        assertThrows(DoesNotExistException.class, () -> requestPersonas(resource, UUIDGenerator.generateUuid()));
    }

    private static Response requestPersonas(final PageResource resource, final String pageId) throws Exception {
        return resource.getPersonalizedPersonasOnPage(mockRequest(), new EmptyHttpResponse(),
                null, 0, 10, "title", OrderDirection.ASC.name(), site.getIdentifier(), pageId, false);
    }

    private static void assertContainsContentRow(final List<MulitreeView> rows, final HTMLPageAsset page) {
        assertFalse("The page should have tree rows", rows.isEmpty());
        assertTrue("The tree should hold the content placed on the page",
                rows.stream().anyMatch(row -> content.getIdentifier().equals(row.getContentId())));
        rows.forEach(row -> assertEquals(page.getIdentifier(), row.getPageId()));
    }

    private static PageResource resourceFor(final User user) {
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);

        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        return new PageResource(PageResourceHelper.getInstance(), webResource,
                new HTMLPageAssetRenderedAPIImpl(), APILocator.getContentletAPI(),
                WebAPILocator.getHostWebAPI());
    }

    private static HttpServletRequest mockRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/page");
        return request;
    }

    private static void addTreeRow(final HTMLPageAsset page, final String containerId) throws Exception {
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(page.getIdentifier(), containerId,
                content.getIdentifier(), UUIDGenerator.generateUuid(), 1));
    }

    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        createdUsers.add(user);
        return user;
    }
}
