package com.dotcms.rendering.js.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.js.proxy.JsUser;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the JavaScript engine's {@code cmsuser} tool ({@link UserJsViewTool}).
 *
 * <p>Looked-up accounts are wrapped in a {@link JsUser} around a detached copy of the user record:
 * the copy keeps the profile fields, additional info and modification date, and has the password,
 * password-reset token and remember-me token cleared. What scripts can read through the
 * {@link JsUser} stays the same.
 */
public class UserJsViewToolTest {

    private static User lookedUpUser;
    private static Role lookedUpUserRole;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        final String suffix = UUIDGenerator.shorty();
        lookedUpUserRole = new RoleDataGen().name("cmsuser-js-role-" + suffix).nextPersisted();
        lookedUpUser = new UserDataGen()
                .firstName("Script")
                .lastName("Target" + suffix)
                .emailAddress("cmsuser.js." + suffix + "@dotcms.com")
                .password("Scr1pt-Passw0rd-" + suffix)
                .additionalInfo("department", "editorial")
                .roles(lookedUpUserRole)
                .nextPersisted();
        // a pending password-reset token is stored on the user record
        lookedUpUser.setIcqId("reset-" + UUIDGenerator.generateUuid());
        APILocator.getUserAPI().save(lookedUpUser, APILocator.systemUser(), false);
    }

    /**
     * Removes the user and the role created for these tests.
     */
    @AfterClass
    public static void cleanupTestData() {
        UserDataGen.remove(lookedUpUser);
        RoleDataGen.remove(lookedUpUserRole);
    }

    /**
     * The {@link User} behind a looked-up {@link JsUser} is a detached copy of the stored record
     * without the credential fields.
     */
    private static void assertWrapsDetachedCopyWithoutCredentials(final JsUser jsUser) throws Exception {
        assertNotNull(jsUser);
        final User wrapped = jsUser.getUser();
        final User stored = APILocator.getUserAPI().loadUserById(lookedUpUser.getUserId());
        // premise: the stored record carries all three credential fields
        assertTrue(UtilMethods.isSet(stored.getPassword()));
        assertTrue(UtilMethods.isSet(stored.getIcqId()));
        assertTrue(UtilMethods.isSet(stored.getRememberMeToken()));

        assertNotSame("Scripts must not get the cached user record", stored, wrapped);
        assertEquals(stored.getUserId(), wrapped.getUserId());
        assertEquals(stored.getEmailAddress(), wrapped.getEmailAddress());
        assertEquals(stored.getFullName(), wrapped.getFullName());
        assertEquals("additional info must be kept", stored.getAdditionalInfo(), wrapped.getAdditionalInfo());
        assertEquals("modification date must be kept", stored.getModificationDate(), wrapped.getModificationDate());

        assertFalse("password must be cleared", UtilMethods.isSet(wrapped.getPassword()));
        assertFalse("reset token must be cleared", UtilMethods.isSet(wrapped.getIcqId()));
        assertFalse("remember-me token must be cleared", UtilMethods.isSet(wrapped.getRememberMeToken()));

        // what scripts can read through the JsUser is unchanged
        assertEquals(stored.getFullName(), jsUser.getFullName());
        assertEquals(stored.getEmailAddress(), jsUser.getEmailAddress());
        assertEquals(stored.isAdmin(), jsUser.isAdmin());
    }

    @Test
    public void getUserByEmail_wraps_a_detached_copy_without_credential_fields() throws Exception {
        assertWrapsDetachedCopyWithoutCredentials(
                new UserJsViewTool().getUserByEmail(lookedUpUser.getEmailAddress()));
    }

    @Test
    public void getUserByUserId_wraps_a_detached_copy_without_credential_fields() throws Exception {
        assertWrapsDetachedCopyWithoutCredentials(
                new UserJsViewTool().getUserByUserId(lookedUpUser.getUserId()));
    }

    @Test
    public void getAllUsersInGroup_wraps_detached_copies_without_credential_fields() throws Exception {
        final List<JsUser> members = new UserJsViewTool().getAllUsersInGroup(lookedUpUserRole.getId());
        assertEquals(1, members.size());
        assertWrapsDetachedCopyWithoutCredentials(members.get(0));
    }
}
