package com.dotcms.rest.api.v1.linkchecker;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link LinkCheckerResource}, the {@code /v1/link-checker} endpoints of the
 * Link Checker tool.
 * <p>
 * The resource is invoked directly with mock requests. Callers exercised: a CMS Administrator, a
 * back-end user with access to the Link Checker tool who can read only one of two contentlets, a
 * back-end user without the tool, a front-end user and a request with no user. When an allowed
 * caller asks for an on-demand run, the resource is built with a mock helper, so the real
 * link-check job is not started.
 */
public class LinkCheckerResourceIntegrationTest {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    private static final String JOBS_GROUP = "dotcms_jobs";
    private static final String LINK_CHECK_JOB_PREFIX = "linkCheckerJob-";
    private static final String SAMPLE_URL = "https://example.com/missing";

    private static LayoutAPI layoutAPI;
    private static RoleAPI roleAPI;
    private static PermissionAPI permissionAPI;
    private static Host testHost;

    private static ContentType linkedType;
    private static ContentType emptyType;
    private static Field textField;

    /** Contentlet the tool holder is granted READ on. */
    private static Contentlet readableContent;

    /** Contentlet the tool holder has no grant on. */
    private static Contentlet hiddenContent;

    private static Layout toolLayout;
    private static Layout contentOnlyLayout;
    private static Role toolRole;
    private static Role contentOnlyRole;

    /** Back-end user with access to the Link Checker tool. */
    private static User toolHolder;

    /** Back-end user whose sections do not contain the Link Checker tool. */
    private static User noToolUser;

    /** Front-end user without the back-end role. */
    private static User frontEndUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        layoutAPI = APILocator.getLayoutAPI();
        roleAPI = APILocator.getRoleAPI();
        permissionAPI = APILocator.getPermissionAPI();
        testHost = new SiteDataGen().nextPersisted();

        toolLayout = new LayoutDataGen()
                .name("Link Checker " + uniq())
                .portletIds("link-checker")
                .tabOrder(800000)
                .nextPersisted();
        toolRole = new RoleDataGen().layout(toolLayout).nextPersisted();
        toolHolder = backendUser(toolRole);

        contentOnlyLayout = new LayoutDataGen()
                .name("Content Only " + uniq())
                .portletIds("content")
                .tabOrder(800001)
                .nextPersisted();
        contentOnlyRole = new RoleDataGen().layout(contentOnlyLayout).nextPersisted();
        noToolUser = backendUser(contentOnlyRole);

        frontEndUser = new UserDataGen().roles(roleAPI.loadFrontEndUserRole()).nextPersisted();

        linkedType = new ContentTypeDataGen()
                .name("Sample Linked " + uniq())
                .velocityVarName("sampleLinked" + uniq())
                .nextPersisted();
        final String fieldVariable = "sampleBody" + uniq();
        textField = new FieldDataGen()
                .name("Sample Body")
                .velocityVarName(fieldVariable)
                .contentTypeId(linkedType.id())
                .nextPersisted();
        emptyType = new ContentTypeDataGen()
                .name("Sample Empty " + uniq())
                .velocityVarName("sampleEmpty" + uniq())
                .nextPersisted();

        readableContent = new ContentletDataGen(linkedType.id())
                .host(testHost)
                .setProperty(fieldVariable, "Sample text A")
                .nextPersisted();
        hiddenContent = new ContentletDataGen(linkedType.id())
                .host(testHost)
                .setProperty(fieldVariable, "Sample text B")
                .nextPersisted();

        final Permission read = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                readableContent.getPermissionId(), toolRole.getId(), PermissionAPI.PERMISSION_READ, true);
        permissionAPI.save(read, readableContent, APILocator.getUserAPI().getSystemUser(), false);

        APILocator.getLinkCheckerAPI().saveInvalidLinks(readableContent, List.of(sampleLink()));
        APILocator.getLinkCheckerAPI().saveInvalidLinks(hiddenContent, List.of(sampleLink()));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (final Contentlet contentlet : new Contentlet[]{hiddenContent, readableContent}) {
            if (contentlet != null) {
                quietly(() -> APILocator.getLinkCheckerAPI().deleteInvalidLinks(contentlet));
                quietly(() -> ContentletDataGen.destroy(contentlet, true));
            }
        }
        for (final ContentType type : new ContentType[]{emptyType, linkedType}) {
            if (type != null) {
                quietly(() -> ContentTypeDataGen.remove(type, true));
            }
        }
        for (final User user : new User[]{frontEndUser, noToolUser, toolHolder}) {
            if (user != null) {
                quietly(() -> UserDataGen.remove(user, true));
            }
        }
        for (final Role role : new Role[]{contentOnlyRole, toolRole}) {
            if (role != null) {
                quietly(() -> RoleDataGen.remove(role, true));
            }
        }
        for (final Layout layout : new Layout[]{contentOnlyLayout, toolLayout}) {
            if (layout != null) {
                quietly(() -> removeLayout(layout.getId()));
            }
        }
        if (testHost != null) {
            quietly(() -> APILocator.getHostAPI().archive(testHost, APILocator.systemUser(), false));
            quietly(() -> APILocator.getHostAPI().delete(testHost, APILocator.systemUser(), false));
        }
    }

    // ==================== Fixtures ====================

    /** A cleanup step that may fail because the object is already gone. */
    @FunctionalInterface
    private interface CleanupStep {
        void run() throws Exception;
    }

    private static void quietly(final CleanupStep step) {
        try {
            step.run();
        } catch (final Exception e) {
            Logger.warn(LinkCheckerResourceIntegrationTest.class, "Cleanup step failed: " + e.getMessage());
        }
    }

    private static void removeLayout(final String layoutId) throws Exception {
        final Layout layout = layoutAPI.findLayout(layoutId);
        if (layout != null && UtilMethods.isSet(layout.getId())) {
            layoutAPI.removeLayout(layout);
        }
    }

    private static InvalidLink sampleLink() {
        final InvalidLink link = new InvalidLink();
        link.setField(textField.id());
        link.setUrl(SAMPLE_URL);
        link.setTitle("Sample link");
        link.setStatusCode(404);
        return link;
    }

    /** Creates a user holding the given role plus the back-end user role. */
    private static User backendUser(final Role role) throws Exception {
        final User user = new UserDataGen().roles(role).nextPersisted();
        final Role backendRole = roleAPI.loadBackEndUserRole();
        if (!roleAPI.doesUserHaveRole(user, backendRole)) {
            roleAPI.addRoleToUser(backendRole, user);
        }
        return user;
    }

    private static String uniq() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /** Counts the scheduled link-check jobs, ignoring other jobs of the same group. */
    private static long scheduledJobCount() throws Exception {
        return Arrays.stream(QuartzUtils.getScheduler().getJobNames(JOBS_GROUP))
                .filter(name -> name != null && name.startsWith(LINK_CHECK_JOB_PREFIX))
                .count();
    }

    // ==================== Request builders ====================

    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = baseRequest();
        request.setHeader("Authorization", TOKEN_UTILS.adminBearerHeader());
        return request;
    }

    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = baseRequest();
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER, user);
        return request;
    }

    private static HttpServletRequest anonymousRequest() {
        return baseRequest();
    }

    private static MockHeaderRequest baseRequest() {
        return new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(testHost.getHostname(), "/").request())
                                .request())
                        .request());
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse();
    }

    private static BrokenLinksView listAs(final HttpServletRequest request, final String contentType)
            throws Exception {
        return listAs(request, contentType, "0", "25");
    }

    private static BrokenLinksView listAs(final HttpServletRequest request, final String contentType,
                                          final String offset, final String limit) throws Exception {
        final Response response = new LinkCheckerResource().list(request, response(), contentType, offset, limit);
        assertEquals(200, response.getStatus());
        final ResponseEntityBrokenLinksView view = (ResponseEntityBrokenLinksView) response.getEntity();
        assertNotNull(view);
        return view.getEntity();
    }

    // ==================== Tests ====================

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator and a content type with one broken link on each of two contentlets
     * Expected: both rows are returned and total is 2
     */
    @Test
    public void list_admin_seesEveryRow() throws Exception {
        final BrokenLinksView view = listAs(adminRequest(), linkedType.id());

        assertEquals(2, view.total());
        assertEquals(2, view.list().size());
        assertTrue(view.list().stream().anyMatch(row -> readableContent.getInode().equals(row.inode())));
        assertTrue(view.list().stream().anyMatch(row -> hiddenContent.getInode().equals(row.inode())));
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a tool holder who can read one contentlet but not the other
     * Expected: only the readable row is returned, total is 1, and the editor appears by full name
     */
    @Test
    public void list_holder_seesOnlyReadableRows() throws Exception {
        assertTrue(permissionAPI.doesUserHavePermission(readableContent, PermissionAPI.PERMISSION_READ,
                toolHolder, false));
        assertFalse(permissionAPI.doesUserHavePermission(hiddenContent, PermissionAPI.PERMISSION_READ,
                toolHolder, false));

        final BrokenLinksView view = listAs(requestFor(toolHolder), linkedType.id());

        assertEquals(1, view.total());
        assertEquals(1, view.list().size());
        final BrokenLinkView row = view.list().get(0);
        assertEquals(readableContent.getInode(), row.inode());

        final User editor = APILocator.getUserAPI().loadUserById(readableContent.getModUser());
        assertTrue(UtilMethods.isSet(editor.getFullName()));
        assertEquals(editor.getFullName(), row.modUserName());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator who sends no offset and limit, then blank ones
     * Expected: both answer the same rows as offset "0" and limit "25"
     */
    @Test
    public void list_withoutPagingParameters_usesDefaults() throws Exception {
        final BrokenLinksView expected = listAs(adminRequest(), linkedType.id(), "0", "25");
        final BrokenLinksView withNulls = listAs(adminRequest(), linkedType.id(), null, null);
        final BrokenLinksView withBlanks = listAs(adminRequest(), linkedType.id(), "", "");

        assertEquals(expected.total(), withNulls.total());
        assertEquals(inodesOf(expected), inodesOf(withNulls));
        assertEquals(expected.total(), withBlanks.total());
        assertEquals(inodesOf(expected), inodesOf(withBlanks));
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator who sends no offset and limit, and a resource whose helper is a mock
     * Expected: the helper is asked for the content type with offset 0 and limit 25
     */
    @Test
    public void list_withoutPagingParameters_passesDefaultOffsetAndLimit() throws Exception {
        final LinkCheckerHelper helper = mock(LinkCheckerHelper.class);
        when(helper.findBrokenLinks(anyString(), anyInt(), anyInt(), any(User.class)))
                .thenReturn(new BrokenLinksView(List.of(), 0));
        final LinkCheckerResource resource =
                new LinkCheckerResource(new WebResource(), APILocator.getLayoutAPI(), helper);
        final String typeId = linkedType.id();

        final Response response = resource.list(adminRequest(), response(), typeId, null, null);

        assertEquals(200, response.getStatus());
        verify(helper, times(1)).findBrokenLinks(eq(typeId), eq(0), eq(25), any(User.class));
    }

    /**
     * Method to test: {@link LinkCheckerResource#run}
     * Given: a CMS Administrator and a resource whose helper is a mock
     * Expected: the answer is "queued" and the helper is asked to queue the check exactly once
     */
    @Test
    public void run_allowedUser_queuesOnce() throws Exception {
        final LinkCheckerHelper helper = mock(LinkCheckerHelper.class);
        final LinkCheckerResource resource =
                new LinkCheckerResource(new WebResource(), APILocator.getLayoutAPI(), helper);

        final Response response = resource.run(adminRequest(), response());

        assertEquals(200, response.getStatus());
        final ResponseEntityStringView view = (ResponseEntityStringView) response.getEntity();
        assertNotNull(view);
        assertEquals("queued", view.getEntity());
        verify(helper, times(1)).runCheckNow();
    }

    /**
     * Method to test: {@link LinkCheckerResource#run}
     * Given: a back-end user who holds the Link Checker tool, and a resource whose helper is a mock
     * Expected: the answer is "queued" and the helper is asked to queue the check exactly once
     */
    @Test
    public void run_toolHolder_queuesOnce() throws Exception {
        final LinkCheckerHelper helper = mock(LinkCheckerHelper.class);
        final LinkCheckerResource resource =
                new LinkCheckerResource(new WebResource(), APILocator.getLayoutAPI(), helper);

        final Response response = resource.run(requestFor(toolHolder), response());

        assertEquals(200, response.getStatus());
        final ResponseEntityStringView view = (ResponseEntityStringView) response.getEntity();
        assertNotNull(view);
        assertEquals("queued", view.getEntity());
        verify(helper, times(1)).runCheckNow();
    }

    private static List<String> inodesOf(final BrokenLinksView view) {
        return view.list().stream().map(BrokenLinkView::inode).collect(Collectors.toList());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list} and {@link LinkCheckerResource#run}
     * Given: a back-end user without access to the Link Checker tool
     * Expected: both answer forbidden and no job is scheduled
     */
    @Test
    public void listAndRun_userWithoutTool_isForbidden() throws Exception {
        assertThrows(DotSecurityException.class,
                () -> new LinkCheckerResource().list(requestFor(noToolUser), response(), linkedType.id(), "0", "25"));

        final long before = scheduledJobCount();
        assertThrows(DotSecurityException.class,
                () -> new LinkCheckerResource().run(requestFor(noToolUser), response()));
        assertEquals(before, scheduledJobCount());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list} and {@link LinkCheckerResource#run}
     * Given: a front-end user and a request with no user
     * Expected: both answer 401 and no job is scheduled
     */
    @Test
    public void listAndRun_frontEndOrAnonymous_isUnauthorized() throws Exception {
        for (final HttpServletRequest request : List.of(requestFor(frontEndUser), anonymousRequest())) {
            final SecurityException listError = assertThrows(SecurityException.class,
                    () -> new LinkCheckerResource().list(request, response(), linkedType.id(), "0", "25"));
            assertEquals(401, listError.getResponse().getStatus());
        }

        final long before = scheduledJobCount();
        for (final HttpServletRequest request : List.of(requestFor(frontEndUser), anonymousRequest())) {
            final SecurityException runError = assertThrows(SecurityException.class,
                    () -> new LinkCheckerResource().run(request, response()));
            assertEquals(401, runError.getResponse().getStatus());
        }
        assertEquals(before, scheduledJobCount());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a whitespace-only content type, a negative offset or a limit outside 1..100
     * Expected: each answers a bad request
     */
    @Test
    public void list_invalidParameters_isBadRequest() {
        final String typeId = linkedType.id();
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), " ", "0", "25"));
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), typeId, "-1", "25"));
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), typeId, "0", "0"));
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), typeId, "0", "101"));
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator who sends a null content type, then an empty one
     * Expected: each answers a bad request
     */
    @Test
    public void list_missingContentType_isBadRequest() {
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), null, "0", "25"));
        assertThrows(BadRequestException.class,
                () -> new LinkCheckerResource().list(adminRequest(), response(), "", "0", "25"));
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator who sends an offset that is a decimal, larger than an int, or not a number
     * Expected: each answers a bad request
     */
    @Test
    public void list_offsetNotAWholeInt_isBadRequest() {
        final String typeId = linkedType.id();
        for (final String offset : List.of("1.5", "99999999999", "abc")) {
            assertThrows("offset " + offset, BadRequestException.class,
                    () -> new LinkCheckerResource().list(adminRequest(), response(), typeId, offset, "25"));
        }
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator who sends a limit that is a decimal, larger than an int, or not a number
     * Expected: each answers a bad request
     */
    @Test
    public void list_limitNotAWholeInt_isBadRequest() {
        final String typeId = linkedType.id();
        for (final String limit : List.of("1.5", "99999999999", "abc")) {
            assertThrows("limit " + limit, BadRequestException.class,
                    () -> new LinkCheckerResource().list(adminRequest(), response(), typeId, "0", limit));
        }
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a CMS Administrator and a content type value with quote, percent, semicolon and angle brackets
     * Expected: 200 with an empty list and total 0
     */
    @Test
    public void list_contentTypeWithSpecialCharacters_isEmpty() throws Exception {
        final BrokenLinksView view = listAs(adminRequest(), "x'%;<b>");

        assertEquals(0, view.total());
        assertTrue(view.list().isEmpty());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: an unknown content type id
     * Expected: an empty list with total 0
     */
    @Test
    public void list_unknownContentType_isEmpty() throws Exception {
        final BrokenLinksView view = listAs(adminRequest(), UUID.randomUUID().toString());

        assertEquals(0, view.total());
        assertTrue(view.list().isEmpty());
    }

    /**
     * Method to test: {@link LinkCheckerResource#list}
     * Given: a content type with no broken links
     * Expected: an empty list with total 0
     */
    @Test
    public void list_contentTypeWithoutLinks_isEmpty() throws Exception {
        final BrokenLinksView view = listAs(adminRequest(), emptyType.id());

        assertEquals(0, view.total());
        assertTrue(view.list().isEmpty());
    }
}
