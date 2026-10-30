package com.dotcms.rest.api.v1.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the READ requirement when {@link ContainerResource} renders a contentlet
 * into a container: the contentlet is served only to a caller who can read the version they would
 * be served, whether it is addressed by identifier or by inode.
 * <p>
 * The permission decision runs against the real content, permission and container APIs. The
 * calling user is supplied through a mocked {@link WebResource}, and the Velocity merge is replaced,
 * so a rendered response is recognized by the merge being called.
 */
public class ContainerResourceContentReadAccessTest {

    private static final String RENDERED = "rendered";

    private static PermissionAPI permissionAPI;
    private static Host site;
    private static Container systemContainer;
    private static Contentlet restrictedContent;
    private static Contentlet anonymousReadableContent;
    private static Contentlet anonymousReadableContentWithDraft;
    private static Contentlet unpublishedContent;
    private static Contentlet unpublishedAnonymousReadableContent;
    private static Contentlet readableContentWithDraft;
    private static Contentlet archivedReadableContent;
    private static User backendUserWithoutAccess;
    private static User frontendUserWithoutAccess;
    private static User reader;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Creates a site with three published contentlets: one readable only by a dedicated reader,
     * one the anonymous role can read, and one the anonymous role can read that also has a newer
     * unpublished version. Adds two contentlets that were never published: one readable by the
     * reader and one the anonymous role can read, one readable by the reader with a newer unpublished
     * version, and an archived one readable by the reader. Also creates a back-end user and a
     * front-end user with no permission on the first one.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemContainer = APILocator.getContainerAPI().systemContainer();

        site = new SiteDataGen().nextPersisted();
        final ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        restrictedContent = ContentletDataGen.publish(newContent(contentType));
        anonymousReadableContent = ContentletDataGen.publish(newContent(contentType));
        anonymousReadableContentWithDraft = ContentletDataGen.publish(newContent(contentType));
        unpublishedContent = newContent(contentType);
        unpublishedAnonymousReadableContent = newContent(contentType);
        readableContentWithDraft = ContentletDataGen.publish(newContent(contentType));
        archivedReadableContent = newContent(contentType);

        backendUserWithoutAccess = newUser(TestUserUtils.getBackendRole());
        frontendUserWithoutAccess = newUser(TestUserUtils.getFrontendRole());
        reader = newUser(TestUserUtils.getBackendRole());

        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        grantRead(restrictedContent, APILocator.getRoleAPI().getUserRole(reader).getId());
        grantRead(anonymousReadableContent, anonymousRole.getId());
        grantRead(anonymousReadableContentWithDraft, anonymousRole.getId());
        grantRead(unpublishedContent, APILocator.getRoleAPI().getUserRole(reader).getId());
        grantRead(unpublishedAnonymousReadableContent, anonymousRole.getId());
        grantRead(readableContentWithDraft, APILocator.getRoleAPI().getUserRole(reader).getId());
        grantRead(archivedReadableContent, APILocator.getRoleAPI().getUserRole(reader).getId());
        ContentletDataGen.createNewVersion(readableContentWithDraft,
                VariantAPI.DEFAULT_VARIANT, Map.of("title", "draft-" + System.currentTimeMillis()));
        ContentletDataGen.archive(archivedReadableContent);
        // A newer working version that is not published
        ContentletDataGen.createNewVersion(anonymousReadableContentWithDraft,
                VariantAPI.DEFAULT_VARIANT, Map.of("title", "draft-" + System.currentTimeMillis()));

        assertFalse("The fixture needs a back-end user who cannot read the content",
                permissionAPI.doesUserHavePermission(restrictedContent,
                        PermissionAPI.PERMISSION_READ, backendUserWithoutAccess, true));
        assertFalse("The fixture needs a front-end user who cannot read the content",
                permissionAPI.doesUserHavePermission(restrictedContent,
                        PermissionAPI.PERMISSION_READ, frontendUserWithoutAccess, true));
        assertTrue("The fixture needs a user who can read the content",
                permissionAPI.doesUserHavePermission(restrictedContent,
                        PermissionAPI.PERMISSION_READ, reader, false));
        assertTrue("The fixture needs content the anonymous role can read",
                permissionAPI.doesUserHavePermission(anonymousReadableContent,
                        PermissionAPI.PERMISSION_READ, frontendUserWithoutAccess, true));
    }

    /**
     * Removes the users and the site created for these tests.
     */
    @AfterClass
    public static void cleanup() {
        try {
            createdUsers.forEach(user -> UserDataGen.remove(user, true));
        } finally {
            if (site != null) {
                try {
                    APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                    APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
                } catch (final Exception e) {
                    Logger.warn(ContainerResourceContentReadAccessTest.class,
                            "Could not remove test site: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user renders it into the system container by identifier,
     * Then the request is forbidden and nothing is rendered.
     */
    @Test
    public void containerContent_byIdentifier_backendUserWithoutRead_isForbidden() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(backendUserWithoutAccess, velocityUtil)
                .containerContent(requestFor(backendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, restrictedContent.getIdentifier(), null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user renders it by identifier with the container passed as a query parameter,
     * Then the request is forbidden and nothing is rendered.
     */
    @Test
    public void containerContentByQueryParam_byIdentifier_backendUserWithoutRead_isForbidden() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(backendUserWithoutAccess, velocityUtil)
                .containerContentByQueryParam(requestFor(backendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, null, restrictedContent.getIdentifier()));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a front-end user with no permission on the contentlet,
     * When the user renders it into the system container by identifier,
     * Then the request is forbidden and nothing is rendered.
     */
    @Test
    public void containerContent_byIdentifier_frontendUserWithoutRead_isForbidden() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(frontendUserWithoutAccess, velocityUtil)
                .containerContent(requestFor(frontendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, restrictedContent.getIdentifier(), null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the user renders it by its inode,
     * Then the request is forbidden and nothing is rendered.
     */
    @Test
    public void containerContent_byInode_backendUserWithoutRead_isForbidden() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(backendUserWithoutAccess, velocityUtil)
                .containerContent(requestFor(backendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, restrictedContent.getInode(), null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a back-end user with no permission on the contentlet,
     * When the request is forbidden,
     * Then the response does not include the contentlet's title, identifier or inode.
     */
    @Test
    public void containerContent_forbidden_responseDoesNotIncludeContentDetails() {
        final ForbiddenException forbidden = assertThrows(ForbiddenException.class,
                () -> resourceFor(backendUserWithoutAccess, velocityUtil())
                        .containerContent(requestFor(backendUserWithoutAccess), response(),
                                Container.SYSTEM_CONTAINER, restrictedContent.getIdentifier(), null));

        final String message = forbidden.getResponse().getEntity() + " "
                + forbidden.getResponse().getHeaderString("error-message");
        assertFalse(message, message.contains(restrictedContent.getTitle()));
        assertFalse(message, message.contains(restrictedContent.getIdentifier()));
        assertFalse(message, message.contains(restrictedContent.getInode()));
    }

    /**
     * Given a back-end user granted READ on the contentlet,
     * When the user renders it into the system container by identifier,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byIdentifier_userWithRead_isRendered() throws Exception {
        assertRendered(reader, restrictedContent);
    }

    /**
     * Given a front-end user and a contentlet the anonymous role can read,
     * When the user renders it into the system container by identifier,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byIdentifier_frontendUser_anonymousReadable_isRendered() throws Exception {
        assertRendered(frontendUserWithoutAccess, anonymousReadableContent);
    }

    /**
     * Given a back-end user and a contentlet the anonymous role can read,
     * When the user renders it into the system container by identifier,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byIdentifier_backendUser_anonymousReadable_isRendered() throws Exception {
        assertRendered(backendUserWithoutAccess, anonymousReadableContent);
    }

    /**
     * Given a front-end user and a published contentlet the anonymous role can read, which also
     * has a newer unpublished version,
     * When the user renders it into the system container by identifier,
     * Then the contentlet is rendered (a front-end user is served the published version).
     */
    @Test
    public void containerContent_byIdentifier_frontendUser_anonymousReadableWithNewerDraft_isRendered()
            throws Exception {
        assertRendered(frontendUserWithoutAccess, anonymousReadableContentWithDraft);
    }

    /**
     * Given a back-end user granted READ on a contentlet that was never published,
     * When the user renders it into the system container by identifier,
     * Then the contentlet is rendered (a back-end user is served the working version).
     */
    @Test
    public void containerContent_byIdentifier_userWithRead_unpublished_isRendered() throws Exception {
        assertRendered(reader, unpublishedContent);
    }

    /**
     * Given a front-end user and a contentlet the anonymous role can read that was never published,
     * When the user renders it into the system container by identifier,
     * Then the request answers not found and nothing is rendered.
     */
    @Test
    public void containerContent_byIdentifier_frontendUser_unpublished_isNotFound() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ResourceNotFoundException.class, () -> resourceFor(frontendUserWithoutAccess, velocityUtil)
                .containerContent(requestFor(frontendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, unpublishedAnonymousReadableContent.getIdentifier(), null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a back-end user whose READ comes only from the anonymous role, and a published contentlet
     * with a newer unpublished version,
     * When the user renders it by identifier,
     * Then the request is forbidden: a back-end user is served the working version, which the
     * anonymous role does not open.
     */
    @Test
    public void containerContent_byIdentifier_backendUser_anonymousReadableWithNewerDraft_isForbidden() {
        assertForbidden(backendUserWithoutAccess, requestFor(backendUserWithoutAccess),
                anonymousReadableContentWithDraft.getIdentifier());
    }

    /**
     * Given a back-end user whose READ comes only from the anonymous role, and a contentlet that was
     * never published,
     * When the user renders it by identifier,
     * Then the request is forbidden.
     */
    @Test
    public void containerContent_byIdentifier_backendUser_anonymousReadableUnpublished_isForbidden() {
        assertForbidden(backendUserWithoutAccess, requestFor(backendUserWithoutAccess),
                unpublishedAnonymousReadableContent.getIdentifier());
    }

    /**
     * Given a front-end user with no READ on the contentlet,
     * When the user renders it with the container passed as a query parameter,
     * Then the request is forbidden and nothing is rendered.
     */
    @Test
    public void containerContentByQueryParam_byIdentifier_frontendUserWithoutRead_isForbidden() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(frontendUserWithoutAccess, velocityUtil)
                .containerContentByQueryParam(requestFor(frontendUserWithoutAccess), response(),
                        Container.SYSTEM_CONTAINER, null, restrictedContent.getIdentifier()));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Given a front-end user, a Time Machine date on the request, and a published contentlet the
     * anonymous role can read that has a newer unpublished version,
     * When the user renders it by identifier,
     * Then the request is forbidden, since the Time Machine can render the working version.
     */
    @Test
    public void containerContent_byIdentifier_frontendUser_timeMachine_anonymousReadableWithNewerDraft_isForbidden() {
        final HttpServletRequest request = requestFor(frontendUserWithoutAccess);
        when(request.getAttribute(PageResource.TM_DATE)).thenReturn("4102444800000");

        assertForbidden(frontendUserWithoutAccess, request,
                anonymousReadableContentWithDraft.getIdentifier());
    }

    /**
     * Given a back-end user granted READ on a published contentlet with a newer unpublished version,
     * When the user renders it by identifier,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byIdentifier_userWithRead_newerDraft_isRendered() throws Exception {
        assertRendered(reader, readableContentWithDraft);
    }

    /**
     * Given an administrator,
     * When rendering a contentlet only a dedicated reader can read, by identifier,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byIdentifier_admin_isRendered() throws Exception {
        assertRendered(TestUserUtils.getAdminUser(), restrictedContent);
    }

    /**
     * Given a back-end user granted READ on the contentlet,
     * When the user renders it by its inode,
     * Then the contentlet is rendered.
     */
    @Test
    public void containerContent_byInode_userWithRead_isRendered() throws Exception {
        final ChainedContext context = mock(ChainedContext.class);
        final VelocityUtil velocityUtil = velocityUtil(context);

        final Response response = resourceFor(reader, velocityUtil).containerContent(requestFor(reader),
                response(), Container.SYSTEM_CONTAINER, restrictedContent.getInode(), null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(velocityUtil).merge(anyString(), any());
    }

    /**
     * Given a back-end user granted READ on an archived contentlet,
     * When the user renders it by identifier,
     * Then the request answers not found and nothing is rendered.
     */
    @Test
    public void containerContent_byIdentifier_archived_isNotFound() {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ResourceNotFoundException.class, () -> resourceFor(reader, velocityUtil)
                .containerContent(requestFor(reader), response(),
                        Container.SYSTEM_CONTAINER, archivedReadableContent.getIdentifier(), null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Renders the contentlet by identifier as the given user, with the given request, and checks
     * that the request is forbidden and nothing is rendered.
     */
    private static void assertForbidden(final User user, final HttpServletRequest request,
            final String identifier) {
        final VelocityUtil velocityUtil = velocityUtil();

        assertThrows(ForbiddenException.class, () -> resourceFor(user, velocityUtil)
                .containerContent(request, response(), Container.SYSTEM_CONTAINER, identifier, null));

        verify(velocityUtil, never()).merge(anyString(), any());
    }

    /**
     * Renders the contentlet by identifier as the given user and checks that the merge ran with
     * that contentlet in the container's list.
     */
    private static void assertRendered(final User user, final Contentlet contentlet) throws Exception {
        final ChainedContext context = mock(ChainedContext.class);
        final VelocityUtil velocityUtil = velocityUtil(context);

        final Response response = resourceFor(user, velocityUtil).containerContent(requestFor(user),
                response(), Container.SYSTEM_CONTAINER, contentlet.getIdentifier(), null);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(velocityUtil).merge(anyString(), any());
        verify(context).put("contentletList" + systemContainer.getIdentifier()
                + Container.LEGACY_RELATION_TYPE, List.of(contentlet.getIdentifier()));
    }

    /**
     * Builds the resource with the real content, container and shorty APIs, acting as the given
     * user and merging through the given Velocity utility.
     */
    private static ContainerResource resourceFor(final User user, final VelocityUtil velocityUtil) {
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Boolean.class))).thenReturn(initDataObject);

        return new ContainerResource(webResource, mock(PaginationUtil.class),
                APILocator.getFormAPI(), APILocator.getContainerAPI(),
                APILocator.getVersionableAPI(), velocityUtil, APILocator.getShortyAPI(),
                APILocator.getContentletAPI(), mock(ContainerDependenciesHelper.class));
    }

    private static VelocityUtil velocityUtil() {
        return velocityUtil(mock(ChainedContext.class));
    }

    private static VelocityUtil velocityUtil(final ChainedContext context) {
        final VelocityUtil velocityUtil = mock(VelocityUtil.class);
        when(velocityUtil.getContext(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(context);
        when(velocityUtil.merge(anyString(), any())).thenReturn(RENDERED);
        return velocityUtil;
    }

    /**
     * A request carrying the given user on the test site, so the page mode resolves as it does for
     * that logged-in caller.
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST)).thenReturn(site);
        when(request.getRequestURI()).thenReturn("/api/v1/containers");
        return request;
    }

    private static HttpServletResponse response() {
        return new EmptyHttpResponse();
    }

    private static Contentlet newContent(final ContentType contentType) {
        return new ContentletDataGen(contentType.id())
                .host(site)
                .setProperty("title", "content-" + System.nanoTime())
                .setProperty("body", "body")
                .nextPersisted();
    }

    private static void grantRead(final Contentlet contentlet, final String roleId) throws Exception {
        permissionAPI.save(new Permission(contentlet.getPermissionId(), roleId,
                PermissionAPI.PERMISSION_READ, true), contentlet, APILocator.systemUser(), false);
    }

    private static User newUser(final Role role) {
        final User user = new UserDataGen().roles(role).nextPersisted();
        createdUsers.add(user);
        return user;
    }
}
