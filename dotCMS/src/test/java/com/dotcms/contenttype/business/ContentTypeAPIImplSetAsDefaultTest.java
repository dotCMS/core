package com.dotcms.contenttype.business;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContentTypeAPIImpl#setAsDefault(ContentType)}: making a content type the
 * default one requires EDIT_PERMISSIONS on it, the permission that saving the type requires.
 */
class ContentTypeAPIImplSetAsDefaultTest {

    private final ContentTypeFactory contentTypeFactory = mock(ContentTypeFactory.class);
    private final PermissionAPI permissionAPI = mock(PermissionAPI.class);
    private final User user = mock(User.class);
    private final ContentType type = mock(ContentType.class);
    private final ContentTypeAPIImpl contentTypeAPI = new ContentTypeAPIImpl(user, false,
            contentTypeFactory, mock(FieldFactory.class), permissionAPI, mock(FieldAPI.class),
            mock(LocalSystemEventsAPI.class));

    /**
     * Method to test: {@link ContentTypeAPIImpl#setAsDefault(ContentType)}
     * Given: a user who can read the content type but has no EDIT_PERMISSIONS on it
     * Expected: a {@link DotSecurityException} and the default content type is not changed
     */
    @Test
    void setAsDefault_requiresEditPermissions() throws Exception {
        givenUserHoldsUpTo(PermissionAPI.PERMISSION_READ);

        assertThrows(DotSecurityException.class, () -> contentTypeAPI.setAsDefault(type));
        verify(contentTypeFactory, never()).setAsDefault(any());
    }

    /**
     * Method to test: {@link ContentTypeAPIImpl#setAsDefault(ContentType)}
     * Given: a user with EDIT_PERMISSIONS on the content type
     * Expected: the content type is made the default one
     */
    @Test
    void setAsDefault_withEditPermissions_updatesDefault() throws Exception {
        givenUserHoldsUpTo(PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        contentTypeAPI.setAsDefault(type);

        verify(contentTypeFactory).setAsDefault(type);
    }

    /**
     * Method to test: {@link ContentTypeAPIImpl#setAsDefault(ContentType)}
     * Given: an API that does not count front-end roles, as for back-end users, and a user whose
     * EDIT_PERMISSIONS on the content type only come from a front-end role
     * Expected: a {@link DotSecurityException} and the default content type is not changed, as
     * when the content type is saved
     */
    @Test
    void setAsDefault_editPermissionsOnlyThroughFrontEndRole_isRefused() throws Exception {
        givenUserHoldsUpTo(PermissionAPI.PERMISSION_EDIT_PERMISSIONS);
        when(permissionAPI.doesUserHavePermission((Permissionable) type,
                PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, false)).thenReturn(false);

        assertThrows(DotSecurityException.class, () -> contentTypeAPI.setAsDefault(type));
        verify(contentTypeFactory, never()).setAsDefault(any());
    }

    /**
     * Method to test: {@link ContentTypeAPIImpl#setAsDefault(ContentType)}
     * Given: a user whose stored permissions on the content type stop at READ, while the content
     * type permission check answers yes
     * Expected: a {@link DotSecurityException} and the default content type is not changed: only the
     * permissions stored for the content type count
     */
    @Test
    void setAsDefault_onlyStoredPermissionsCount() throws Exception {
        givenUserHoldsUpTo(PermissionAPI.PERMISSION_READ);
        when(permissionAPI.doesUserHavePermission(eq(type), anyInt(), eq(user), anyBoolean()))
                .thenReturn(true);

        assertThrows(DotSecurityException.class, () -> contentTypeAPI.setAsDefault(type));
        verify(contentTypeFactory, never()).setAsDefault(any());
    }

    /**
     * Grants the user every permission on the content type up to the given level, through every
     * way the permission API can be asked: the content type checks and the checks on the stored
     * permissions.
     */
    private void givenUserHoldsUpTo(final int highestPermission) throws Exception {
        doAnswer(invocation -> {
            final PermissionLevel level = invocation.getArgument(1);
            if (level.getType() > highestPermission) {
                throw new DotSecurityException("not allowed");
            }
            return null;
        }).when(permissionAPI).checkPermission(eq(type), any(PermissionLevel.class), eq(user));
        when(permissionAPI.doesUserHavePermission(eq(type), anyInt(), eq(user)))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(1) <= highestPermission);
        when(permissionAPI.doesUserHavePermission(eq(type), anyInt(), eq(user), anyBoolean()))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(1) <= highestPermission);
        when(permissionAPI.doesUserHavePermission(eq((Permissionable) type), anyInt(), eq(user)))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(1) <= highestPermission);
        when(permissionAPI.doesUserHavePermission(eq((Permissionable) type), anyInt(), eq(user),
                anyBoolean()))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(1) <= highestPermission);
    }
}
