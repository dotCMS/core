package com.dotcms.content.elasticsearch.business;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ESContentletAPIHelper}
 * @author jsanca
 */
public class ESContentletAPIHelperTest extends UnitTestBase {

    private static final String PERMISSION_ID = "contentlet-permission-id";
    private static final String ROLE_A = "role-a";
    private static final String ROLE_B = "role-b";
    private static final String USER_ID = "editor-user-id";
    private static final int READ = PermissionAPI.PERMISSION_READ;
    private static final int WRITE = PermissionAPI.PERMISSION_WRITE;
    private static final int PUBLISH = PermissionAPI.PERMISSION_PUBLISH;
    private static final int EDIT_PERMISSIONS = PermissionAPI.PERMISSION_EDIT_PERMISSIONS;

    private final ESContentletAPIHelper helper = ESContentletAPIHelper.INSTANCE;

    private boolean testGenerateNotificationStartDeleting = false;

    @Test(expected = DotStateException.class)
    public void testGenerateNotificationStartDeleting() throws Exception {

        final ESContentletAPIHelper esContentletAPIHelper =
                ESContentletAPIHelper.INSTANCE;
        final NotificationAPI notificationAPI = mock(NotificationAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("US").build();

        this.initMessages();
        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);

            doAnswer(new Answer<Void>() { // if this method is called, should fail

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {

                    testGenerateNotificationStartDeleting = true;
                    return null;
                }
            }).when(notificationAPI).generateNotification(
                    new I18NMessage("notification.escontentelet.cannotdelete.info.title"), //"Contentlet Notification"),
                    new I18NMessage("notification.escontentelet.cannotdelete.info.message.", "iFieldNode1"),
                    null,
                    NotificationLevel.ERROR,
                    NotificationType.GENERIC,
                    "admin@dotcms.com",
                    locale
            );

            esContentletAPIHelper.generateNotificationCanNotDelete
                    (notificationAPI, locale,
                            "admin@dotcms.com", "iFieldNode1");


            assertTrue(this.testGenerateNotificationStartDeleting);
        } finally {
            Config.CONTEXT = null;
        }
    }

    /**
     * Given the same roles with the same bits, in the same order
     * When the two lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_whenRolesAndBitsMatch_isTrue() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ | WRITE), permission(ROLE_B, READ));
        final List<Permission> current = Arrays.asList(
                permission(ROLE_A, READ | WRITE), permission(ROLE_B, READ));

        assertTrue(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given the same entries in a different order
     * When the two lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_whenOnlyOrderDiffers_isTrue() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_B, READ), permission(ROLE_A, READ | WRITE));
        final List<Permission> current = Arrays.asList(
                permission(ROLE_A, READ | WRITE), permission(ROLE_B, READ));

        assertTrue(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given one role as separate READ and WRITE entries on one side and as one combined entry on the other
     * When the two lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_whenBitsAreSplitDifferently_isTrue() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_A, WRITE));
        final List<Permission> current = Collections.singletonList(
                permission(ROLE_A, READ | WRITE));

        assertTrue(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given a requested list with one role more than the current one
     * When the two lists are compared
     * Then they differ
     */
    @Test
    public void samePermissions_whenARoleIsAdded_isFalse() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_B, READ));
        final List<Permission> current = Collections.singletonList(
                permission(ROLE_A, READ));

        assertFalse(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given a requested list without one of the current roles
     * When the two lists are compared
     * Then they differ
     */
    @Test
    public void samePermissions_whenARoleIsMissing_isFalse() {

        final List<Permission> requested = Collections.singletonList(
                permission(ROLE_A, READ));
        final List<Permission> current = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_B, READ));

        assertFalse(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given the same roles, one of them with different bits
     * When the two lists are compared
     * Then they differ
     */
    @Test
    public void samePermissions_whenBitsDiffer_isFalse() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ | WRITE | PUBLISH | EDIT_PERMISSIONS), permission(ROLE_B, READ));
        final List<Permission> current = Arrays.asList(
                permission(ROLE_A, READ | WRITE), permission(ROLE_B, READ));

        assertFalse(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given a non-empty requested list and no current permissions
     * When the two lists are compared
     * Then they differ
     */
    @Test
    public void samePermissions_whenCurrentIsEmpty_isFalse() {

        final List<Permission> requested = Collections.singletonList(
                permission(ROLE_A, READ));

        assertFalse(helper.hasSamePermissions(requested, Collections.emptyList()));
    }

    /**
     * Given a requested list that repeats an entry of the current one
     * When the two lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_whenEntriesRepeat_isTrue() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_A, READ), permission(ROLE_B, WRITE));
        final List<Permission> current = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_B, WRITE));

        assertTrue(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given a requested list that adds a role with no bits to the current one
     * When the two lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_ignoresRolesWithoutBits() {

        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_B, 0));
        final List<Permission> current = Collections.singletonList(permission(ROLE_A, READ));

        assertTrue(helper.hasSamePermissions(requested, current));
    }

    /**
     * Given a null list against an empty one, and a list with a null entry against the same list
     * without it
     * When the lists are compared
     * Then they are the same
     */
    @Test
    public void samePermissions_treatsNullListsAndEntriesAsEmpty() {

        assertTrue(helper.hasSamePermissions(null, Collections.emptyList()));
        assertTrue(helper.hasSamePermissions(Arrays.asList(null, permission(ROLE_A, READ)),
                Collections.singletonList(permission(ROLE_A, READ))));
    }

    /**
     * Given a contentlet flagged as being copied and a user without EDIT_PERMISSIONS anywhere
     * When the user's right to change its permissions is checked
     * Then it is allowed
     */
    @Test
    public void canEditPermissions_whenContentletIsBeingCopied_isTrue() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        when(contentlet.getBoolProperty(Contentlet.IS_COPY)).thenReturn(true);

        assertTrue(helper.canEditPermissions(permissionAPI, contentlet, user));
    }

    /**
     * Given a user with EDIT_PERMISSIONS on the contentlet, front-end roles included
     * When the user's right to change its permissions is checked
     * Then it is allowed, front-end roles counting as in PermissionAPI#save
     */
    @Test
    public void canEditPermissions_withEditPermissionsOnContentlet_isTrue() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        // only the 3-argument check is stubbed: it counts front-end roles, the 4-argument one with false does not
        when(permissionAPI.doesUserHavePermission(contentlet, EDIT_PERMISSIONS, user)).thenReturn(true);

        assertTrue(helper.canEditPermissions(permissionAPI, contentlet, user));
    }

    /**
     * Given a user without EDIT_PERMISSIONS on the contentlet but with it on the contentlet's content type
     * When the user's right to change its permissions is checked
     * Then it is allowed
     */
    @Test
    public void canEditPermissions_withEditPermissionsOnContentTypeOnly_isTrue() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        final ContentType contentType = contentlet.getContentType();
        when(permissionAPI.doesUserHavePermission(contentType, EDIT_PERMISSIONS, user)).thenReturn(true);

        assertTrue(helper.canEditPermissions(permissionAPI, contentlet, user));
    }

    /**
     * Given a user without EDIT_PERMISSIONS on the contentlet or its content type, and a contentlet that is not being copied
     * When the user's right to change its permissions is checked
     * Then it is refused
     */
    @Test
    public void canEditPermissions_withoutEditPermissions_isFalse() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();

        assertFalse(helper.canEditPermissions(permissionAPI, contentlet, user));
    }

    /**
     * Given a contentlet flagged as being copied and no user
     * When the right to change its permissions is checked
     * Then it is allowed
     */
    @Test
    public void canEditPermissions_copyFlagWithNullUser_isTrue() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        when(contentlet.getBoolProperty(Contentlet.IS_COPY)).thenReturn(true);

        assertTrue(helper.canEditPermissions(permissionAPI, contentlet, null));
    }

    /**
     * Given no user and a list equal to the contentlet's current permissions
     * When the replacement is prepared
     * Then there is nothing to do: no task and no removal
     */
    @Test
    public void permissionReplacement_nullUserWithUnchangedList_doesNothing() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        when(permissionAPI.getPermissions(contentlet)).thenReturn(Collections.singletonList(
                permission(ROLE_A, READ)));
        final List<Permission> requested = Collections.singletonList(permission(ROLE_A, READ));

        final Optional<Runnable> replacement = helper.permissionReplacement(
                permissionAPI, contentlet, requested, null, systemUser());

        assertFalse(replacement.isPresent());
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Given a user without EDIT_PERMISSIONS and a list equal to the contentlet's current permissions
     * When the replacement is prepared
     * Then there is nothing to do: no removal, no save, no exception
     */
    @Test
    public void permissionReplacement_unchangedListWithoutEditPermissions_doesNothing() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        when(permissionAPI.getPermissions(contentlet)).thenReturn(Arrays.asList(
                permission(ROLE_A, READ | WRITE), permission(ROLE_B, READ)));
        final List<Permission> requested = Arrays.asList(
                permission(ROLE_B, READ), permission(ROLE_A, READ | WRITE));

        final Optional<Runnable> replacement = helper.permissionReplacement(
                permissionAPI, contentlet, requested, user, systemUser());

        assertFalse(replacement.isPresent());
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
        verify(permissionAPI, never()).save(anyCollection(), any(Permissionable.class),
                any(User.class), anyBoolean());
    }

    /**
     * Given a user without EDIT_PERMISSIONS and a list that differs from the current permissions
     * When the replacement is prepared
     * Then a DotSecurityException is thrown and PermissionAPI#removePermissions is never called
     */
    @Test
    public void permissionReplacement_changedListWithoutEditPermissions_isRefusedBeforeRemoval() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        when(permissionAPI.getPermissions(contentlet)).thenReturn(Collections.singletonList(
                permission(ROLE_A, READ)));
        final List<Permission> requested = Arrays.asList(
                permission(ROLE_A, READ), permission(ROLE_B, READ | WRITE | EDIT_PERMISSIONS));

        try {
            helper.permissionReplacement(permissionAPI, contentlet, requested, user, systemUser());
            fail("Expected a DotSecurityException when the list differs and the user may not edit permissions");
        } catch (final DotSecurityException e) {
            assertEquals("User id: " + USER_ID
                    + " does not have permission to alter permissions on asset " + PERMISSION_ID,
                    e.getMessage());
        }
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
        verify(permissionAPI, never()).save(anyCollection(), any(Permissionable.class),
                any(User.class), anyBoolean());
    }

    /**
     * Given a user allowed to change the contentlet's permissions
     * When the prepared task runs
     * Then the individual permissions are removed, then the requested ones are saved with the system user
     */
    @Test
    public void permissionReplacement_allowed_removesThenSavesWithoutRecheck() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        final User systemUser = systemUser();
        when(permissionAPI.doesUserHavePermission(contentlet, EDIT_PERMISSIONS, user)).thenReturn(true);
        final List<Permission> requested = Collections.singletonList(permission(ROLE_B, READ));

        final Optional<Runnable> replacement = helper.permissionReplacement(
                permissionAPI, contentlet, requested, user, systemUser);

        assertTrue("An allowed user must get a replacement task", replacement.isPresent());
        replacement.get().run();

        final InOrder inOrder = inOrder(permissionAPI);
        inOrder.verify(permissionAPI).removePermissions(contentlet);
        inOrder.verify(permissionAPI).save(anyCollection(), eq(contentlet), same(systemUser), eq(false));
    }

    /**
     * Given an allowed user and a save that fails after the removal (e.g. a role locked for
     * editing permissions)
     * When the prepared task runs
     * Then the failure is logged and does not escape the task, which may run as a commit listener
     */
    @Test
    public void permissionReplacement_taskFailure_doesNotEscapeTheTask() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        final User systemUser = systemUser();
        when(permissionAPI.doesUserHavePermission(contentlet, EDIT_PERMISSIONS, user)).thenReturn(true);
        doThrow(new DotSecurityException("locked role")).when(permissionAPI)
                .save(anyCollection(), eq(contentlet), same(systemUser), eq(false));
        final List<Permission> requested = Collections.singletonList(permission(ROLE_B, READ));

        final Optional<Runnable> replacement = helper.permissionReplacement(
                permissionAPI, contentlet, requested, user, systemUser);

        assertTrue("An allowed user must get a replacement task", replacement.isPresent());
        replacement.get().run();

        verify(permissionAPI).removePermissions(contentlet);
    }

    /**
     * Given an allowed user and requested entries without the contentlet's permission id
     * When the prepared task runs
     * Then every saved entry carries the contentlet's permission id with the requested role and bits
     */
    @Test
    public void permissionReplacement_savesEntriesAgainstContentletPermissionId() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        final User user = user();
        final User systemUser = systemUser();
        when(permissionAPI.doesUserHavePermission(contentlet, EDIT_PERMISSIONS, user)).thenReturn(true);
        final List<Permission> requested = Arrays.asList(
                new Permission("", ROLE_A, READ | WRITE),
                new Permission("another-permission-id", ROLE_B, READ));

        final Optional<Runnable> replacement = helper.permissionReplacement(
                permissionAPI, contentlet, requested, user, systemUser);

        assertTrue("An allowed user must get a replacement task", replacement.isPresent());
        replacement.get().run();

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Collection<Permission>> saved =
                ArgumentCaptor.forClass((Class<Collection<Permission>>) (Class<?>) Collection.class);
        verify(permissionAPI).save(saved.capture(), eq(contentlet), same(systemUser), anyBoolean());
        final List<Permission> savedEntries = new ArrayList<>(saved.getValue());
        assertEquals(requested.size(), savedEntries.size());
        for (int i = 0; i < requested.size(); i++) {
            assertEquals(PERMISSION_ID, savedEntries.get(i).getInode());
            assertEquals(requested.get(i).getRoleId(), savedEntries.get(i).getRoleId());
            assertEquals(requested.get(i).getPermission(), savedEntries.get(i).getPermission());
        }
    }

    /**
     * Given no user and a list that differs from the current permissions
     * When the replacement is prepared
     * Then it is refused with a DotSecurityException, without a NullPointerException
     */
    @Test
    public void permissionReplacement_nullUser_isRefusedWithoutError() throws Exception {

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        final Contentlet contentlet = contentlet();
        when(permissionAPI.getPermissions(contentlet)).thenReturn(Collections.singletonList(
                permission(ROLE_A, READ)));
        final List<Permission> requested = Collections.singletonList(permission(ROLE_B, READ));

        try {
            helper.permissionReplacement(permissionAPI, contentlet, requested, null, systemUser());
            fail("Expected a DotSecurityException when there is no user and the list differs");
        } catch (final DotSecurityException e) {
            assertNotNull(e.getMessage());
        }
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Builds a permission entry for the test contentlet.
     */
    private static Permission permission(final String roleId, final int bits) {
        return new Permission(PERMISSION_ID, roleId, bits);
    }

    /**
     * Builds a contentlet mock with a permission id and a content type, not flagged as being copied.
     */
    private static Contentlet contentlet() {
        final Contentlet contentlet = mock(Contentlet.class);
        final ContentType contentType = mock(ContentType.class);
        when(contentlet.getPermissionId()).thenReturn(PERMISSION_ID);
        when(contentlet.getInode()).thenReturn("contentlet-inode");
        when(contentlet.getIdentifier()).thenReturn("contentlet-identifier");
        when(contentlet.getContentType()).thenReturn(contentType);
        return contentlet;
    }

    /**
     * Builds the user asking for the change.
     */
    private static User user() {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        return user;
    }

    /**
     * Builds the user the replacement task saves the permissions with.
     */
    private static User systemUser() {
        final User systemUser = mock(User.class);
        when(systemUser.getUserId()).thenReturn("system-user-id");
        return systemUser;
    }
}
