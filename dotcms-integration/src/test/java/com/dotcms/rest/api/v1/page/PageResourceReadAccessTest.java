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
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.variant.VariantAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the page READ requirement on the page content tree and page personas
 * endpoints of {@link PageResource}, and for the personalized flag of the page view-as status.
 */
public class PageResourceReadAccessTest {

    private static Host site;
    private static HTMLPageAsset publishedPage;
    private static HTMLPageAsset unpublishedPage;
    private static HTMLPageAsset archivedPage;
    private static HTMLPageAsset anonymousReadablePage;
    private static Contentlet content;
    private static HTMLPageAsset mixedPage;
    private static Contentlet unreadableContent;
    private static Contentlet anonymousOnlyContent;
    private static Contentlet archivedContent;
    private static String missingContentId;
    private static HTMLPageAsset personaPage;
    private static Persona personaWithReadableContent;
    private static Persona personaWithoutReadableContent;
    private static Persona personaWithPublicContent;
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
        grantRead(content, reader);

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

        prepareMixedPage(webPageContent);
        preparePersonaPage();
    }

    /**
     * Builds a page personalized for three personas: one holds content the reader can read, one
     * only content the reader has no READ on, and one only content readable through the anonymous
     * role.
     */
    private static void preparePersonaPage() throws Exception {
        final PageRenderTestUtil.PageRenderTest personalized = PageRenderTestUtil.createPage(1, site, true);
        personaPage = personalized.getPage();
        final String containerId = personalized.getFirstContainer().getIdentifier();

        personaWithReadableContent = newPersona("readable");
        personaWithoutReadableContent = newPersona("unreadable");
        personaWithPublicContent = newPersona("public");

        saveRow(personaPage, containerId, content.getIdentifier(), 0, personaTag(personaWithReadableContent),
                VariantAPI.DEFAULT_VARIANT.name());
        saveRow(personaPage, containerId, unreadableContent.getIdentifier(), 1,
                personaTag(personaWithoutReadableContent), VariantAPI.DEFAULT_VARIANT.name());
        saveRow(personaPage, containerId, anonymousOnlyContent.getIdentifier(), 2,
                personaTag(personaWithPublicContent), VariantAPI.DEFAULT_VARIANT.name());
    }

    /**
     * Builds a page whose tree holds content the reader can read and content the reader cannot
     * read, each in its own row.
     */
    private static void prepareMixedPage(final ContentType webPageContent) throws Exception {
        final PageRenderTestUtil.PageRenderTest mixed = PageRenderTestUtil.createPage(1, site, true);
        mixedPage = mixed.getPage();
        final String containerId = mixed.getFirstContainer().getIdentifier();

        unreadableContent = newContent(webPageContent);
        anonymousOnlyContent = newContent(webPageContent);
        breakInheritance(anonymousOnlyContent);
        PermissionUtilTest.addAnonymousUser(anonymousOnlyContent);
        ContentletDataGen.publish(anonymousOnlyContent);

        archivedContent = newContent(webPageContent);
        grantRead(archivedContent, reader);
        APILocator.getContentletAPI().archive(archivedContent, APILocator.systemUser(), false);

        missingContentId = UUIDGenerator.generateUuid();

        int order = 0;
        saveRow(mixedPage, containerId, content.getIdentifier(), order++, MultiTree.DOT_PERSONALIZATION_DEFAULT,
                VariantAPI.DEFAULT_VARIANT.name());
        saveRow(mixedPage, containerId, unreadableContent.getIdentifier(), order++,
                MultiTree.DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());
        saveRow(mixedPage, containerId, unreadableContent.getIdentifier(), order++, "dot:somepersona",
                VariantAPI.DEFAULT_VARIANT.name());
        saveRow(mixedPage, containerId, anonymousOnlyContent.getIdentifier(), order++,
                MultiTree.DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());
        saveRow(mixedPage, containerId, archivedContent.getIdentifier(), order++,
                MultiTree.DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());
        saveRow(mixedPage, containerId, missingContentId, order,
                MultiTree.DOT_PERSONALIZATION_DEFAULT, VariantAPI.DEFAULT_VARIANT.name());
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

    /**
     * Given a page holding content the reader has READ on and content the reader has no READ on,
     * the latter placed twice (default and persona-specific rows),
     * When the reader requests the page content tree,
     * Then only rows for content the reader can read are returned.
     */
    @Test
    public void contentTree_omitsRowsForContentWithoutRead() throws Exception {
        final List<String> contentIds = mixedTreeContentIds(reader);

        assertTrue("Readable content should be listed", contentIds.contains(content.getIdentifier()));
        assertFalse("Content without READ for the user should not be listed",
                contentIds.contains(unreadableContent.getIdentifier()));
    }

    /**
     * Given a tree row whose content identifier matches no content,
     * When a non-admin reader requests the page content tree,
     * Then that row is not returned.
     */
    @Test
    public void contentTree_omitsRowsWithoutLoadableContent_forNonAdmin() throws Exception {
        assertFalse("A row whose content cannot be loaded should not be listed",
                mixedTreeContentIds(reader).contains(missingContentId));
    }

    /**
     * Given the same page,
     * When a CMS administrator requests the page content tree,
     * Then every row is returned, including rows whose content cannot be loaded.
     */
    @Test
    public void contentTree_returnsEveryRow_forAdmin() throws Exception {
        final List<String> contentIds = mixedTreeContentIds(TestUserUtils.getAdminUser());

        assertEquals(APILocator.getMultiTreeAPI().getMultiTrees(mixedPage.getIdentifier()).size(),
                contentIds.size());
        assertTrue(contentIds.contains(unreadableContent.getIdentifier()));
        assertTrue(contentIds.contains(missingContentId));
    }

    /**
     * Given content readable only through the anonymous role,
     * When a back-end reader without permission of their own requests the page content tree,
     * Then that row is not returned: front-end roles do not count, as for the page itself.
     */
    @Test
    public void contentTree_omitsRowsReadableOnlyThroughAnonymousRole() throws Exception {
        assertTrue("Precondition: the anonymous role can read the content",
                APILocator.getPermissionAPI().doesUserHavePermission(anonymousOnlyContent,
                        PermissionAPI.PERMISSION_READ, APILocator.getUserAPI().getAnonymousUser(), true));

        assertFalse(mixedTreeContentIds(reader).contains(anonymousOnlyContent.getIdentifier()));
    }

    /**
     * Given archived content the reader can read,
     * When the reader requests the page content tree,
     * Then its row is returned.
     */
    @Test
    public void contentTree_keepsRowsForArchivedContentWithRead() throws Exception {
        assertTrue(mixedTreeContentIds(reader).contains(archivedContent.getIdentifier()));
    }


    /**
     * Given a page personalized for one persona with content the reader can read, and for another
     * persona with content the reader has no READ on,
     * When the reader requests the page personas,
     * Then only the first persona is flagged as personalized.
     */
    @Test
    public void personasOnPage_flagsOnlyPersonasWithReadableContent() throws Exception {
        final Response response = requestPersonas(resourceFor(reader), personaPage.getIdentifier());

        assertEquals(Boolean.TRUE, personalizedFlag(response, personaWithReadableContent));
        assertEquals(Boolean.FALSE, personalizedFlag(response, personaWithoutReadableContent));
    }

    /**
     * Given the same page,
     * When a CMS administrator requests the page personas,
     * Then both personas are flagged as personalized.
     */
    @Test
    public void personasOnPage_flagsEveryPersonalizedPersona_forAdmin() throws Exception {
        final Response response = requestPersonas(resourceFor(TestUserUtils.getAdminUser()),
                personaPage.getIdentifier());

        assertEquals(Boolean.TRUE, personalizedFlag(response, personaWithReadableContent));
        assertEquals(Boolean.TRUE, personalizedFlag(response, personaWithoutReadableContent));
    }

    /**
     * Given the same page,
     * When the reader renders it as each persona,
     * Then the view-as status is personalized only for the persona with content the reader can read.
     */
    @Test
    public void viewAsStatus_personalizedOnlyForPersonaWithReadableContent() throws Exception {
        assertTrue(viewAsPersonalized(reader, personaWithReadableContent, PageMode.PREVIEW_MODE));
        assertFalse("Content without READ for the user should not mark the persona as personalized",
                viewAsPersonalized(reader, personaWithoutReadableContent, PageMode.PREVIEW_MODE));
    }

    /**
     * Given the same page,
     * When a CMS administrator renders it as each persona,
     * Then the view-as status is personalized for both.
     */
    @Test
    public void viewAsStatus_personalizedForEveryPersona_forAdmin() throws Exception {
        final User admin = TestUserUtils.getAdminUser();

        assertTrue(viewAsPersonalized(admin, personaWithReadableContent, PageMode.PREVIEW_MODE));
        assertTrue(viewAsPersonalized(admin, personaWithoutReadableContent, PageMode.PREVIEW_MODE));
    }

    /**
     * Given the same page, also personalized for a persona whose content only the anonymous role
     * can read,
     * When it is rendered in LIVE mode for the anonymous user as that persona,
     * Then the view-as status is personalized, since front-end roles count in that mode.
     */
    @Test
    public void viewAsStatus_countsFrontEndRolesInLiveMode() throws Exception {
        assertTrue(viewAsPersonalized(APILocator.getUserAPI().getAnonymousUser(), personaWithPublicContent,
                PageMode.LIVE));
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

    private static List<String> mixedTreeContentIds(final User user) throws Exception {
        return resourceFor(user).getContentTree(mockRequest(), new EmptyHttpResponse(), mixedPage.getIdentifier())
                .getEntity().stream().map(MulitreeView::getContentId).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static Object personalizedFlag(final Response response, final Persona persona) {
        final List<PersonalizationPersonaPageView> personas =
                (List<PersonalizationPersonaPageView>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
        return personas.stream()
                .filter(view -> persona.getIdentifier().equals(view.getPersona().get("identifier")))
                .findFirst()
                .map(view -> view.getPersona().get("personalized"))
                .orElseThrow(() -> new AssertionError("The persona should be listed"));
    }

    private static boolean viewAsPersonalized(final User user, final Persona persona, final PageMode mode)
            throws Exception {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        final Visitor visitor = new Visitor();
        visitor.setPersona(persona);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.VISITOR, visitor);
        return APILocator.getHTMLPageAssetRenderedAPI()
                .getViewAsStatus(request, mode, personaPage, user).isPersonalized();
    }

    private static Persona newPersona(final String name) throws Exception {
        final Persona persona = new PersonaDataGen()
                .host(site)
                .keyTag(name + System.currentTimeMillis())
                .nextPersisted();
        grantRead(persona, reader);
        ContentletDataGen.publish(persona);
        return persona;
    }

    private static String personaTag(final Persona persona) {
        return Persona.DOT_PERSONA_PREFIX_SCHEME + ":" + persona.getKeyTag();
    }

    private static Contentlet newContent(final ContentType contentType) {
        return new ContentletDataGen(contentType.id())
                .languageId(1)
                .host(site)
                .setProperty("title", "content")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();
    }

    private static void grantRead(final Contentlet contentlet, final User user) throws Exception {
        breakInheritance(contentlet);
        PermissionUtilTest.addPermission(contentlet, user, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                PermissionAPI.PERMISSION_READ);
    }

    private static void breakInheritance(final Contentlet contentlet) throws Exception {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        if (permissionAPI.isInheritingPermissions(contentlet)) {
            permissionAPI.permissionIndividually(permissionAPI.findParentPermissionable(contentlet), contentlet,
                    APILocator.systemUser());
        }
    }

    private static void saveRow(final HTMLPageAsset page, final String containerId, final String contentId,
            final int order, final String personalization, final String variantName) throws Exception {
        APILocator.getMultiTreeAPI().saveMultiTree(new MultiTree(page.getIdentifier(), containerId, contentId,
                UUIDGenerator.generateUuid(), order, personalization, variantName, null));
    }

    private static User newBackendUser() throws Exception {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        createdUsers.add(user);
        return user;
    }
}
