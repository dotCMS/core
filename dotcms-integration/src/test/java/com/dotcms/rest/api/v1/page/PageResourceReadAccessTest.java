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
import com.dotcms.datagen.PermissionUtilTest;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import com.dotcms.rest.exception.SecurityException;
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
import com.liferay.portal.util.WebKeys;
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
    private static HTMLPageAsset anonymousReadablePage;
    private static Contentlet content;
    private static User userWithoutAccess;
    private static User reader;
    private static User frontEndUser;
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

        anonymousReadablePage = PageRenderTestUtil.createPage(1, site, true).getPage();
        PermissionUtilTest.addAnonymousUser(anonymousReadablePage);

        frontEndUser = new UserDataGen().roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();
        createdUsers.add(frontEndUser);
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

    /**
     * Given a non-admin back-end user granted READ on the page,
     * When the user requests the page personas,
     * Then the personas are returned for that page.
     */
    @Test
    public void personasOnPage_withReadOnPage_returnsPersonas() throws Exception {
        final Response response = requestPersonas(resourceFor(reader), publishedPage.getIdentifier());

        assertPersonasForPage(response, publishedPage);
    }

    /**
     * Given an archived page and a user with READ on it,
     * When the user requests the page personas,
     * Then the personas are returned for that page.
     */
    @Test
    public void personasOnPage_archivedPage_withRead_returnsPersonas() throws Exception {
        final Response response = requestPersonas(resourceFor(reader), archivedPage.getIdentifier());

        assertPersonasForPage(response, archivedPage);
    }

    /**
     * Given the identifier of a content item that is not a page, and a user who can read it,
     * When the content tree and the personas are requested for that identifier,
     * Then both requests answer not found.
     */
    @Test
    public void identifierOfNonPageContent_returnsNotFound() {
        final PageResource resource = resourceFor(APILocator.systemUser());

        assertThrows(DoesNotExistException.class, () -> resource.getContentTree(
                mockRequest(), new EmptyHttpResponse(), content.getIdentifier()));
        assertThrows(DoesNotExistException.class, () -> requestPersonas(resource, content.getIdentifier()));
    }

    /**
     * Given a page readable only through the anonymous role, and a back-end user with no
     * permission of their own on it,
     * When the user requests the content tree, and the personas with front-end roles requested,
     * Then both requests answer not found: front-end roles do not count for the page check.
     */
    @Test
    public void pageReadableOnlyThroughAnonymousRole_returnsNotFound() {
        final PageResource resource = resourceFor(userWithoutAccess);

        assertThrows(DoesNotExistException.class, () -> resource.getContentTree(
                mockRequest(), new EmptyHttpResponse(), anonymousReadablePage.getIdentifier()));
        assertThrows(DoesNotExistException.class, () -> requestPersonas(resource,
                anonymousReadablePage.getIdentifier(), true));
    }

    /**
     * Given a user who holds only the front-end user role,
     * When the user requests the content tree or the personas of a page,
     * Then both requests are refused with 401 before any data is loaded.
     */
    @Test
    public void frontEndUser_isRejectedWithUnauthorized() {
        final PageResource resource = resourceWithRealWebResource();

        assertUnauthorized(() -> resource.getContentTree(
                requestWithUser(frontEndUser), new EmptyHttpResponse(), publishedPage.getIdentifier()));
        assertUnauthorized(() -> resource.getPersonalizedPersonasOnPage(requestWithUser(frontEndUser),
                new EmptyHttpResponse(), null, 0, 10, "title", OrderDirection.ASC.name(),
                site.getIdentifier(), publishedPage.getIdentifier(), false));
    }

    /**
     * Given a request with no logged-in user,
     * When the content tree or the personas of a page are requested,
     * Then both requests are refused with 401.
     */
    @Test
    public void noUser_isRejectedWithUnauthorized() {
        final PageResource resource = resourceWithRealWebResource();

        assertUnauthorized(() -> resource.getContentTree(
                requestWithUser(null), new EmptyHttpResponse(), publishedPage.getIdentifier()));
        assertUnauthorized(() -> resource.getPersonalizedPersonasOnPage(requestWithUser(null),
                new EmptyHttpResponse(), null, 0, 10, "title", OrderDirection.ASC.name(),
                site.getIdentifier(), publishedPage.getIdentifier(), false));
    }

    private static Response requestPersonas(final PageResource resource, final String pageId) throws Exception {
        return requestPersonas(resource, pageId, false);
    }

    private static Response requestPersonas(final PageResource resource, final String pageId,
            final boolean respectFrontEndRoles) throws Exception {
        return resource.getPersonalizedPersonasOnPage(mockRequest(), new EmptyHttpResponse(),
                null, 0, 10, "title", OrderDirection.ASC.name(), site.getIdentifier(), pageId,
                respectFrontEndRoles);
    }

    @SuppressWarnings("unchecked")
    private static void assertPersonasForPage(final Response response, final HTMLPageAsset page) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final List<PersonalizationPersonaPageView> personas =
                (List<PersonalizationPersonaPageView>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
        assertFalse("The page should list at least the default persona", personas.isEmpty());
        personas.forEach(persona -> assertEquals(page.getIdentifier(), persona.getPageId()));
    }

    private static void assertUnauthorized(final org.junit.function.ThrowingRunnable call) {
        final SecurityException exception = assertThrows(SecurityException.class, call);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
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

    private static PageResource resourceWithRealWebResource() {
        return new PageResource(PageResourceHelper.getInstance(), new WebResource(),
                new HTMLPageAssetRenderedAPIImpl(), APILocator.getContentletAPI(),
                WebAPILocator.getHostWebAPI());
    }

    private static HttpServletRequest requestWithUser(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/page").request()).request();
        if (user != null) {
            request.setAttribute(WebKeys.USER, user);
        }
        return request;
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
