package com.dotcms.rest.api.v1.contenttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@code GET /api/v1/contenttype/_writable} (#37896, PR #38054 review): one
 * page of the content types the current user can write to, searchable and with an optional list
 * of variables to leave out. Content Drive's CSV import uses it, leaving out Host, to list the
 * types a user can import into.
 */
public class ContentTypeResourceWritableIntegrationTest {

    private static ContentTypeResource resource;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ContentTypeResource();
    }

    /**
     * Given scenario: a limited backend user has READ and EDIT on one new content type and only READ
     * on another, and searches for both.
     * Expected result: only the first is listed, sorted by name, with id, variable, name and base
     * type.
     */
    @Test
    public void test_only_lists_types_the_user_can_write() throws Exception {
        final String stamp = "WritablePerm" + System.currentTimeMillis();
        final ContentType writable = new ContentTypeDataGen().name(stamp + "Writable").nextPersisted();
        final ContentType readOnly = new ContentTypeDataGen().name(stamp + "ReadOnly").nextPersisted();
        try {
            final User limitedUser = new UserDataGen()
                    .roles(TestUserUtils.getBackendRole()).nextPersisted();
            addPermission(writable, limitedUser, PermissionAPI.PERMISSION_READ,
                    PermissionAPI.PERMISSION_EDIT);
            addPermission(readOnly, limitedUser, PermissionAPI.PERMISSION_READ);
            final HttpServletRequest limitedRequest = JobUtil.generateMockRequest(limitedUser,
                    APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)
                            .getHostname());

            final List<ContentTypeSummaryView> types = resource.getWritableContentTypes(
                    limitedRequest, new EmptyHttpResponse(), stamp, null, null, null).getEntity();

            assertNotNull(types);
            final List<String> variables = variables(types);
            assertTrue("A type the user can edit should be listed",
                    variables.contains(writable.variable()));
            assertFalse("A type the user can only read should not be listed",
                    variables.contains(readOnly.variable()));

            final ContentTypeSummaryView item = types.stream()
                    .filter(type -> writable.variable().equals(type.variable()))
                    .findFirst().orElseThrow();
            assertEquals(writable.id(), item.id());
            assertEquals(writable.name(), item.name());
            assertEquals(writable.baseType().name(), item.baseType());
        } finally {
            ContentTypeDataGen.remove(writable);
            ContentTypeDataGen.remove(readOnly);
        }
    }

    /**
     * Given scenario: an admin searches for "host", with and without {@code exclude=Host}.
     * Expected result: Host is listed without the exclusion, and left out with it, so a caller such
     * as the CSV import decides which types it won't offer.
     */
    @Test
    public void test_exclude_leaves_out_the_named_types() throws Exception {
        final List<String> all = variables(resource.getWritableContentTypes(adminRequest(),
                new EmptyHttpResponse(), "host", null, null, null).getEntity());
        assertTrue("An admin can write to Host, so it is listed",
                all.stream().anyMatch(Host.HOST_VELOCITY_VAR_NAME::equalsIgnoreCase));

        final List<String> excluded = variables(resource.getWritableContentTypes(adminRequest(),
                new EmptyHttpResponse(), "host", null, null, Host.HOST_VELOCITY_VAR_NAME)
                .getEntity());
        assertFalse("Host should be left out when excluded",
                excluded.stream().anyMatch(Host.HOST_VELOCITY_VAR_NAME::equalsIgnoreCase));
    }

    /**
     * Given scenario: three writable content types share a name prefix; the admin pages through
     * them two at a time, searching by that prefix.
     * Expected result: page 1 holds the first two by name and page 2 the third, and both carry the
     * page, the page size and the total of 3.
     */
    @Test
    public void test_writable_types_are_paged() throws Exception {
        final String stamp = "WritablePaged" + System.currentTimeMillis();
        final ContentType first = new ContentTypeDataGen().name(stamp + "A").nextPersisted();
        final ContentType second = new ContentTypeDataGen().name(stamp + "B").nextPersisted();
        final ContentType third = new ContentTypeDataGen().name(stamp + "C").nextPersisted();
        try {
            final ResponseEntityContentTypeSummaryListView pageOne = resource
                    .getWritableContentTypes(adminRequest(), new EmptyHttpResponse(), stamp, 1, 2,
                            null);
            assertEquals(List.of(first.variable(), second.variable()),
                    variables(pageOne.getEntity()));
            assertEquals(1, pageOne.getPagination().getCurrentPage());
            assertEquals(2, pageOne.getPagination().getPerPage());
            assertEquals(3L, pageOne.getPagination().getTotalEntries());

            final ResponseEntityContentTypeSummaryListView pageTwo = resource
                    .getWritableContentTypes(adminRequest(), new EmptyHttpResponse(), stamp, 2, 2,
                            null);
            assertEquals(List.of(third.variable()), variables(pageTwo.getEntity()));
            assertEquals(2, pageTwo.getPagination().getCurrentPage());
            assertEquals(3L, pageTwo.getPagination().getTotalEntries());
        } finally {
            ContentTypeDataGen.remove(first);
            ContentTypeDataGen.remove(second);
            ContentTypeDataGen.remove(third);
        }
    }

    /**
     * Given scenario: a writable content type whose name and variable differ, searched for in a
     * different letter case by part of its name, and then by part of its variable.
     * Expected result: both searches find it.
     */
    @Test
    public void test_search_by_name_or_variable() throws Exception {
        final String stamp = String.valueOf(System.currentTimeMillis());
        final ContentType type = new ContentTypeDataGen()
                .name("Writable Search Name " + stamp)
                .velocityVarName("writableSearchVar" + stamp)
                .nextPersisted();
        try {
            assertEquals(List.of(type.variable()), variables(resource.getWritableContentTypes(
                    adminRequest(), new EmptyHttpResponse(), "SEARCH NAME " + stamp, null, null,
                    null).getEntity()));
            assertEquals(List.of(type.variable()), variables(resource.getWritableContentTypes(
                    adminRequest(), new EmptyHttpResponse(), "searchvar" + stamp, null, null,
                    null).getEntity()));
        } finally {
            ContentTypeDataGen.remove(type);
        }
    }

    /**
     * Given scenario: the content types are asked for without paging parameters.
     * Expected result: the first page of 40, as the general content type listing.
     */
    @Test
    public void test_defaults_to_the_first_page_of_40() throws Exception {
        final ResponseEntityContentTypeSummaryListView types = resource.getWritableContentTypes(
                adminRequest(), new EmptyHttpResponse(), null, null, null, null);

        assertEquals(1, types.getPagination().getCurrentPage());
        assertEquals(40, types.getPagination().getPerPage());
        assertTrue(types.getEntity().size() <= 40);
    }

    /**
     * Given scenario: a page below 1, or a page size below 1, is asked for.
     * Expected result: a bad request.
     */
    @Test
    public void test_refuses_invalid_paging() {
        assertThrows(BadRequestException.class, () -> resource.getWritableContentTypes(
                adminRequest(), new EmptyHttpResponse(), null, 0, 10, null));
        assertThrows(BadRequestException.class, () -> resource.getWritableContentTypes(
                adminRequest(), new EmptyHttpResponse(), null, 1, 0, null));
    }

    private static List<String> variables(final List<ContentTypeSummaryView> types) {
        return types.stream().map(ContentTypeSummaryView::variable).collect(Collectors.toList());
    }

    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    /**
     * Grants a limited user's individual role the given permissions on a permissionable.
     */
    private static void addPermission(final Permissionable permissionable, final User limitedUser,
            final int... permissions) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                Arrays.stream(permissions).sum(), true);
        APILocator.getPermissionAPI().save(permission, permissionable, APILocator.systemUser(),
                false);
    }
}
