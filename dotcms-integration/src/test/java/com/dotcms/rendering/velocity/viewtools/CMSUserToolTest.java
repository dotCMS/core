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
import com.dotcms.featureflag.FeatureFlagName;
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
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for the {@code $cmsuser} Velocity tool ({@link CMSUserTool}).
 *
 * <p>The user lookups ({@code getUserByEmail}, {@code getUserByUserId},
 * {@code getAllUsersInGroup}) give templates a read-only profile of the account: the display
 * fields templates print (id, names, email, ...) without the credential fields kept on the user
 * record, and without a way back to the underlying {@link User}. The tool's own role helpers
 * accept those profiles. This is controlled by the {@code FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES}
 * feature flag, on by default; installations whose templates still need full {@link User} objects
 * can turn it off, which makes lookups return a detached copy with the credential fields cleared.
 *
 * <p>{@code getLoggedInUser} keeps returning a {@link User}, because templates pass the requester to
 * methods that require one, but as a detached copy without the credential fields.
 */
public class CMSUserToolTest {

    private static final String READ_ONLY_PROFILES_FLAG = FeatureFlagName.FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES;

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
        profiledUser.setLastLoginDate(new Date());
        APILocator.getUserAPI().save(profiledUser, APILocator.systemUser(), false);

        adminUser = new UserDataGen()
                .emailAddress("cmsuser.admin." + suffix + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();

        requester = new UserDataGen()
                .emailAddress("cmsuser.requester." + suffix + "@dotcms.com")
                .password("Requ3ster-Passw0rd-" + suffix)
                .additionalInfo("team", "qa")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        requester.setIcqId("reset-" + UUIDGenerator.generateUuid());
        APILocator.getUserAPI().save(requester, APILocator.systemUser(), false);
    }

    /**
     * Removes the users and the role created for these tests.
     */
    @AfterClass
    public static void cleanupTestData() {
        UserDataGen.remove(requester);
        UserDataGen.remove(adminUser);
        UserDataGen.remove(profiledUser);
        RoleDataGen.remove(profiledUserRole);
    }

    /**
     * Builds a request made by {@code user}, logged in through the session.
     */
    private static HttpServletRequest requestFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(user);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        return request;
    }

    /**
     * Builds the tool the way the toolbox does for the given request.
     */
    private static CMSUserTool cmsUserTool(final HttpServletRequest request) {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(request);
        when(viewContext.getVelocityContext()).thenReturn(VelocityUtil.getBasicContext());

        final CMSUserTool tool = new CMSUserTool();
        tool.init(viewContext);
        return tool;
    }

    /**
     * Builds the tool the way the toolbox does for a request made by {@code user}.
     */
    private static CMSUserTool cmsUserTool(final User user) {
        return cmsUserTool(requestFor(user));
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
     * A user copy handed to a template keeps the identity, display fields, additional info and
     * modification date of the stored record, carries none of the credential fields, and is
     * detached from the cached user record.
     */
    private static void assertDetachedCopyWithoutCredentials(final Object handedOut, final User source)
            throws Exception {
        assertTrue("Expected a User copy: " + handedOut, handedOut instanceof User);
        final User copy = (User) handedOut;
        final User stored = APILocator.getUserAPI().loadUserById(source.getUserId());
        assertNotSame("Copies must be detached from the cached user record", stored, copy);

        assertEquals(stored.getUserId(), copy.getUserId());
        assertEquals(stored.getEmailAddress(), copy.getEmailAddress());
        assertEquals(stored.getFullName(), copy.getFullName());
        assertNotNull("premise: the stored record has a modification date", stored.getModificationDate());
        assertEquals("modification date must be kept", stored.getModificationDate(), copy.getModificationDate());
        assertEquals("additional info must be kept", stored.getAdditionalInfo(), copy.getAdditionalInfo());
        assertNotSame("additional info must be a copy", stored.getAdditionalInfo(), copy.getAdditionalInfo());

        assertFalse("password must be cleared", UtilMethods.isSet(copy.getPassword()));
        assertFalse("reset token must be cleared", UtilMethods.isSet(copy.getIcqId()));
        // the remember-me token is kept in the skin id field
        assertFalse("remember-me token must be cleared", UtilMethods.isSet(copy.getRememberMeToken()));
        assertFalse("remember-me token must be cleared", UtilMethods.isSet(copy.getSkinId()));
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
        members.forEach(CMSUserToolTest::assertReadOnlyProfile);
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
     * The read-only profiles are on by default: with the feature flag not set at all, lookups
     * still return profiles.
     */
    @Test
    public void lookups_return_profiles_when_the_flag_is_not_set() {
        final String flagValue = Config.getStringProperty(READ_ONLY_PROFILES_FLAG, null);
        Config.setProperty(READ_ONLY_PROFILES_FLAG, null);
        try {
            assertReadOnlyProfile(cmsUserTool(requester).getUserByEmail(profiledUser.getEmailAddress()));
        } finally {
            Config.setProperty(READ_ONLY_PROFILES_FLAG, flagValue);
        }
    }

    /**
     * With the {@code FEATURE_FLAG_CMSUSER_READ_ONLY_PROFILES} flag off, lookups return
     * {@link User} objects again for templates that need them, as detached copies without
     * credential fields.
     */
    @Test
    public void flag_off_returns_user_copies_without_credential_fields() throws Exception {
        // restore the raw value afterwards (null clears it) so other tests see the shipped default
        final String flagValue = Config.getStringProperty(READ_ONLY_PROFILES_FLAG, null);
        Config.setProperty(READ_ONLY_PROFILES_FLAG, false);
        try {
            final CMSUserTool tool = cmsUserTool(requester);

            assertDetachedCopyWithoutCredentials(tool.getUserByEmail(profiledUser.getEmailAddress()), profiledUser);
            assertDetachedCopyWithoutCredentials(tool.getUserByUserId(profiledUser.getUserId()), profiledUser);

            final List<?> members = tool.getAllUsersInGroup(profiledUserRole.getId());
            assertEquals(1, members.size());
            assertDetachedCopyWithoutCredentials(members.get(0), profiledUser);

            assertEquals("true", render("$cmsuser.isUserRole($cmsuser.getUserByEmail($email), $roleName)"));
        } finally {
            Config.setProperty(READ_ONLY_PROFILES_FLAG, flagValue);
        }
    }

    /**
     * {@code getLoggedInUser} returns a {@link User} for the requester, since templates pass it to
     * methods that require one, but as a detached copy without the credential fields. The object
     * held by the session is left untouched.
     */
    @Test
    public void getLoggedInUser_returns_a_detached_copy_without_credential_fields() throws Exception {
        final User sessionUser = APILocator.getUserAPI().loadUserById(requester.getUserId());
        final String sessionPassword = sessionUser.getPassword();
        final String sessionResetToken = sessionUser.getIcqId();
        final String sessionRememberMeToken = sessionUser.getRememberMeToken();
        // premise: the session's object carries all three credential fields
        assertTrue(UtilMethods.isSet(sessionPassword));
        assertTrue(UtilMethods.isSet(sessionResetToken));
        assertTrue(UtilMethods.isSet(sessionRememberMeToken));

        final HttpServletRequest request = requestFor(sessionUser);
        final Object loggedIn = cmsUserTool(request).getLoggedInUser(request);

        assertNotSame("Templates must not get the session's own user object", sessionUser, loggedIn);
        assertDetachedCopyWithoutCredentials(loggedIn, requester);

        assertEquals(sessionPassword, sessionUser.getPassword());
        assertEquals(sessionResetToken, sessionUser.getIcqId());
        assertEquals(sessionRememberMeToken, sessionUser.getRememberMeToken());
    }

    /**
     * Profiles expose the last login date, which templates print as the last access time.
     */
    @Test
    public void profiles_expose_the_last_login_date() throws Exception {
        final User stored = APILocator.getUserAPI().loadUserById(profiledUser.getUserId());
        assertNotNull("premise: the stored record has a last login date", stored.getLastLoginDate());

        final Object profile = cmsUserTool(requester).getUserByEmail(profiledUser.getEmailAddress());
        final Method lastLoginDate = profile.getClass().getMethod("getLastLoginDate");
        assertEquals(stored.getLastLoginDate(), lastLoginDate.invoke(profile));
    }
}
