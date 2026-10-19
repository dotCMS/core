package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the {@code $cmsuser} Velocity tool ({@link CMSUsersWebAPI}).
 *
 * <p>The user lookups ({@code getUserByEmail}, {@code getUserByUserId},
 * {@code getAllUsersInGroup}) give templates a read-only profile of the account: the display
 * fields templates print (id, names, email, ...) without the credential fields kept on the user
 * record, and without a way back to the underlying {@link User}. The tool's own role helpers
 * accept those profiles. Installations whose templates still need full {@link User} objects can
 * turn on {@code velocity.cmsuser.legacy.user.objects}, which returns a detached copy with the
 * credential fields cleared.
 */
public class CMSUsersWebAPITest {

    private static final String LEGACY_USER_OBJECTS_PROP = "velocity.cmsuser.legacy.user.objects";

    /** Accessors for fields that must never reach a template through a looked-up account. */
    private static final Set<String> CREDENTIAL_ACCESSORS = Set.of(
            "getPassword", "getPasswordEncrypted", "getIcqId", "getRememberMeToken", "getSkinId",
            "getAdditionalInfo", "getLoginIP", "getLastLoginIP");

    private static User profiledUser;
    private static User adminUser;
    private static User requester;
    private static Role profiledUserRole;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        final String suffix = UUIDGenerator.shorty();
        profiledUserRole = new RoleDataGen().name("cmsuser-profile-role-" + suffix).nextPersisted();

        profiledUser = new UserDataGen()
                .firstName("Profile")
                .lastName("Target" + suffix)
                .emailAddress("cmsuser.profile." + suffix + "@dotcms.com")
                .password("Pr0file-Passw0rd-" + suffix)
                .additionalInfo("department", "editorial")
                .roles(profiledUserRole)
                .nextPersisted();
        // a pending password-reset token is stored on the user record
        profiledUser.setIcqId("reset-" + UUIDGenerator.generateUuid());
        APILocator.getUserAPI().save(profiledUser, APILocator.systemUser(), false);

        adminUser = new UserDataGen()
                .emailAddress("cmsuser.admin." + suffix + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();

        requester = new UserDataGen()
                .emailAddress("cmsuser.requester." + suffix + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    /**
     * Builds the tool the way the toolbox does for a request made by {@code user}.
     */
    private static CMSUsersWebAPI cmsUserTool(final User user) {
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(viewContext.getRequest()).thenReturn(request);
        when(viewContext.getVelocityContext()).thenReturn(VelocityUtil.getBasicContext());
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(user);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final CMSUsersWebAPI tool = new CMSUsersWebAPI();
        tool.init(viewContext);
        return tool;
    }

    private static String render(final String template) throws Exception {
        final Context context = VelocityUtil.getBasicContext();
        context.put("cmsuser", cmsUserTool(requester));
        context.put("email", profiledUser.getEmailAddress());
        context.put("userId", profiledUser.getUserId());
        context.put("adminEmail", adminUser.getEmailAddress());
        context.put("roleId", profiledUserRole.getId());
        context.put("roleName", profiledUserRole.getName());
        return VelocityUtil.eval(template, context).trim();
    }

    /**
     * A looked-up account is a read-only profile: not a {@link User}, no credential accessors,
     * and no method that hands back a {@link User}.
     */
    private static void assertReadOnlyProfile(final Object lookedUp) {
        assertNotNull(lookedUp);
        assertFalse("Looked-up accounts must not be User objects: " + lookedUp.getClass(),
                lookedUp instanceof User);
        for (final Method method : lookedUp.getClass().getMethods()) {
            assertFalse("Profile must not expose " + method.getName(),
                    CREDENTIAL_ACCESSORS.contains(method.getName()));
            assertFalse("Profile must not hand back a User through " + method.getName(),
                    User.class.isAssignableFrom(method.getReturnType()));
        }
    }

    /**
     * A user copy returned in legacy mode keeps the identity and display fields but carries none
     * of the credential fields, and is detached from the cached user record.
     */
    private static void assertDetachedCopyWithoutCredentials(final Object lookedUp, final User source)
            throws Exception {
        assertTrue("Legacy mode returns User objects", lookedUp instanceof User);
        final User copy = (User) lookedUp;
        assertEquals(source.getUserId(), copy.getUserId());
        assertEquals(source.getEmailAddress(), copy.getEmailAddress());
        assertEquals(source.getFullName(), copy.getFullName());
        assertFalse("password must be cleared", UtilMethods.isSet(copy.getPassword()));
        assertFalse("reset token must be cleared", UtilMethods.isSet(copy.getIcqId()));
        // the remember-me token is kept in the skin id field
        assertFalse("remember-me token must be cleared", UtilMethods.isSet(copy.getRememberMeToken()));
        assertFalse("remember-me token must be cleared", UtilMethods.isSet(copy.getSkinId()));
        assertTrue("additional info must be cleared",
                copy.getAdditionalInfo() == null || copy.getAdditionalInfo().isEmpty());
        assertNotSame("Legacy copies must be detached from the cached user record",
                APILocator.getUserAPI().loadUserById(source.getUserId()), copy);
    }

    @Test
    public void getUserByEmail_returns_a_read_only_profile() {
        assertReadOnlyProfile(cmsUserTool(requester).getUserByEmail(profiledUser.getEmailAddress()));
    }

    @Test
    public void getUserByUserId_returns_a_read_only_profile() {
        assertReadOnlyProfile(cmsUserTool(requester).getUserByUserId(profiledUser.getUserId()));
    }

    @Test
    public void getAllUsersInGroup_returns_read_only_profiles() {
        final List<?> members = cmsUserTool(requester).getAllUsersInGroup(profiledUserRole.getId());
        assertEquals(1, members.size());
        members.forEach(CMSUsersWebAPITest::assertReadOnlyProfile);
    }

    /**
     * Templates keep printing the display fields of a looked-up account, using both property and
     * getter syntax, while the credential fields render as nothing.
     */
    @Test
    public void templates_render_profile_display_fields_but_no_credential_fields() throws Exception {
        final String expected = profiledUser.getUserId() + "|" + profiledUser.getFullName() + "|"
                + profiledUser.getEmailAddress() + "|Profile|" + profiledUser.getLastName() + "|true||||||";

        assertEquals(expected, render("#set($u = $cmsuser.getUserByEmail($email))"
                + "$u.userId|$u.fullName|$u.emailAddress|$u.getFirstName()|$u.lastName|$u.active"
                + "|$!u.password|$!u.icqId|$!u.getRememberMeToken()|$!u.skinId|$!u.additionalInfo|$!u.passwordEncrypted"));

        assertEquals(expected, render("#set($u = $cmsuser.getUserByUserId($userId))"
                + "$u.userId|$u.fullName|$u.emailAddress|$u.getFirstName()|$u.lastName|$u.active"
                + "|$!u.password|$!u.icqId|$!u.getRememberMeToken()|$!u.skinId|$!u.additionalInfo|$!u.passwordEncrypted"));

        assertEquals(profiledUser.getUserId() + "|",
                render("#foreach($m in $cmsuser.getAllUsersInGroup($roleId))$m.userId|$!m.password#end"));
    }

    /**
     * The tool's role helpers accept the profiles returned by its lookups.
     */
    @Test
    public void role_helpers_accept_looked_up_profiles() throws Exception {
        assertEquals("true|false|found|found", render("#set($u = $cmsuser.getUserByEmail($email))"
                + "$cmsuser.isUserRole($u, $roleName)|$cmsuser.isCMSAdmin($u)"
                + "|#foreach($r in $cmsuser.getUserRoles($u))#if($r.id == $roleId)found#end#end"
                + "|#foreach($r in $cmsuser.getUserGroups($u))#if($r.id == $roleId)found#end#end"));

        assertEquals("true", render("$cmsuser.isCMSAdmin($cmsuser.getUserByEmail($adminEmail))"));
    }

    /**
     * With {@code velocity.cmsuser.legacy.user.objects} on, lookups return {@link User} objects
     * again for templates that need them, as detached copies without credential fields.
     */
    @Test
    public void legacy_mode_returns_user_copies_without_credential_fields() throws Exception {
        Config.setProperty(LEGACY_USER_OBJECTS_PROP, true);
        try {
            final CMSUsersWebAPI tool = cmsUserTool(requester);

            assertDetachedCopyWithoutCredentials(tool.getUserByEmail(profiledUser.getEmailAddress()), profiledUser);
            assertDetachedCopyWithoutCredentials(tool.getUserByUserId(profiledUser.getUserId()), profiledUser);

            final List<?> members = tool.getAllUsersInGroup(profiledUserRole.getId());
            assertEquals(1, members.size());
            assertDetachedCopyWithoutCredentials(members.get(0), profiledUser);

            assertEquals("true", render("$cmsuser.isUserRole($cmsuser.getUserByEmail($email), $roleName)"));
        } finally {
            Config.setProperty(LEGACY_USER_OBJECTS_PROP, false);
        }
    }
}
