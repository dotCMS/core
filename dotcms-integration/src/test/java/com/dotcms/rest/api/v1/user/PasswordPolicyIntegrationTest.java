package com.dotcms.rest.api.v1.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.DotRestInstanceProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

/**
 * Covers the default password requirements (15 to 64 printable ASCII characters with an uppercase
 * letter, a lowercase letter, a digit and a special character, spaces allowed) on the paths that
 * validate a password: the validating save, {@link UserAPI#updatePassword}, and the users REST
 * resource.
 */
public class PasswordPolicyIntegrationTest {

    private static final String PATTERN_KEY = "passwords.regexptoolkit.pattern";
    private static final String ERROR_KEY = "User-Info-Save-Password-Failed";

    /** 14 characters, all four classes. */
    private static final String FOURTEEN = "Abcdefgh12!xyz";
    /** 15 characters, all four classes. */
    private static final String FIFTEEN = "Abcdefgh12!xyzk";

    private static UserAPI userAPI;
    private static UserResource resource;
    private static HttpServletResponse response;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
        resource = new UserResource(new WebResource(new ApiProvider()), UserResourceHelper.getInstance(),
                new PaginationUtil(new UserPaginator()), new DotRestInstanceProvider()
                        .setUserAPI(userAPI)
                        .setHostAPI(APILocator.getHostAPI())
                        .setRoleAPI(APILocator.getRoleAPI())
                        .setErrorHelper(ErrorResponseHelper.INSTANCE));
        response = new MockHttpResponse();
    }

    @Before
    public void setRequest() throws Exception {
        // a rejected password localizes its error from the request in this thread-local, as the
        // REST filters set it on a real call
        adminRequest();
    }

    @After
    public void clearRequest() throws Exception {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        // a rejected save rolls back its transaction, which clears the role cache; resolve the
        // back-end role again so the next test's REST auth check doesn't start against a cold cache
        APILocator.getRoleAPI().loadBackEndUserRole();
    }

    @AfterClass
    public static void cleanUp() {
        usersToClean.forEach(UserDataGen::remove);
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: A validating save with a 14-character password and then a 15-character one,
     * both with all four classes.
     * Expected Result: the first is rejected with the password error key and leaves the stored
     * password unchanged; the second is accepted and stored hashed.
     */
    @Test
    public void test_validatingSave_lengthBounds() throws Exception {
        final User target = newUser();
        final Object before = storedPassword(target.getUserId());

        target.setPassword(FOURTEEN);
        final DotDataException rejected = assertThrows(DotDataException.class,
                () -> userAPI.save(target, APILocator.systemUser(), true, false));
        assertEquals(ERROR_KEY, rejected.getMessageKey());
        assertEquals("the stored password must not change", before, storedPassword(target.getUserId()));

        target.setPassword(FIFTEEN);
        userAPI.save(target, APILocator.systemUser(), true, false);
        assertStoredHashOf(target.getUserId(), FIFTEEN);
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: A validating save with a password that has spaces inside, at the start and at
     * the end, and otherwise meets the requirements.
     * Expected Result: accepted, and the stored hash matches the password exactly as typed, without
     * trimming.
     */
    @Test
    public void test_validatingSave_spacesKeptAsTyped() throws Exception {
        final User target = newUser();
        final String withSpaces = " Abcd 12! xyz  k ";

        target.setPassword(withSpaces);
        userAPI.save(target, APILocator.systemUser(), true, false);

        assertStoredHashOf(target.getUserId(), withSpaces);
        assertNotEquals("the trimmed value must not match",
                AuthenticationStatus.AUTHENTICATED,
                PasswordFactoryProxy.authPassword(withSpaces.trim(), (String) storedPassword(target.getUserId())));
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: A valid password followed by a line feed.
     * Expected Result: rejected.
     */
    @Test
    public void test_validatingSave_trailingLineFeed_rejected() throws Exception {
        final User target = newUser();

        target.setPassword(FIFTEEN + "\n");
        final DotDataException rejected = assertThrows(DotDataException.class,
                () -> userAPI.save(target, APILocator.systemUser(), true, false));
        assertEquals(ERROR_KEY, rejected.getMessageKey());
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: A 15-character password that lacks one of the classes, and a 65-character one.
     * Expected Result: every one is rejected.
     */
    @Test
    public void test_validatingSave_missingClassAndTooLong_rejected() throws Exception {
        final User target = newUser();
        final String[] rejectedPasswords = {
                "abcdefgh12!xyzk", // no uppercase
                "ABCDEFGH12!XYZK", // no lowercase
                "Abcdefghij!xyzk", // no digit
                "Abcdefgh12xyzkk", // no special
                FIFTEEN + "x".repeat(50) // 65
        };
        for (final String password : rejectedPasswords) {
            target.setPassword(password);
            final DotDataException rejected = assertThrows(DotDataException.class,
                    () -> userAPI.save(target, APILocator.systemUser(), true, false));
            assertEquals(ERROR_KEY, rejected.getMessageKey());
        }
    }

    /**
     * Method to test: {@link UserAPI#updatePassword(User, String, User, boolean)}
     * Given Scenario: Setting a new password of 14 and then of 15 characters with all four classes.
     * Expected Result: the first throws {@link DotInvalidPasswordException}; the second is stored
     * hashed.
     */
    @Test
    public void test_updatePassword_lengthBounds() throws Exception {
        final User target = newUser();

        assertThrows(DotInvalidPasswordException.class,
                () -> userAPI.updatePassword(target, FOURTEEN, APILocator.systemUser(), false));

        userAPI.updatePassword(target, FIFTEEN, APILocator.systemUser(), false);
        assertStoredHashOf(target.getUserId(), FIFTEEN);
    }

    /**
     * Method to test: {@link UserResource#create} and {@link UserResource#update}
     * Given Scenario: An admin sets a 14-character and then a 15-character password with all four
     * classes, through the create and the update of the users resource.
     * Expected Result: the 14-character one is rejected with the password error key on both; the
     * 15-character one is accepted on both and stored hashed.
     */
    @Test
    public void test_restCreateAndUpdate_lengthBounds() throws Exception {
        final String userId = "pwdpolicy-" + uniq();
        final DotDataException rejectedCreate = assertThrows(DotDataException.class,
                () -> resource.create(adminRequest(), response,
                        createFormFor(userId).password(FOURTEEN.toCharArray()).build()));
        assertEquals(ERROR_KEY, rejectedCreate.getMessageKey());

        assertEquals(Status.OK.getStatusCode(), resource.create(adminRequest(), response,
                createFormFor(userId).password(FIFTEEN.toCharArray()).build()).getStatus());
        final User created = userAPI.loadUserById(userId);
        usersToClean.add(created);
        assertStoredHashOf(userId, FIFTEEN);

        final DotDataException rejectedUpdate = assertThrows(DotDataException.class,
                () -> resource.update(adminRequest(), response,
                        updateFormFor(created).password(FOURTEEN.toCharArray()).build()));
        assertEquals(ERROR_KEY, rejectedUpdate.getMessageKey());

        final String other = "Zyxwvuts98#abcd";
        assertEquals(Status.OK.getStatusCode(), resource.update(adminRequest(), response,
                updateFormFor(created).password(other.toCharArray()).build()).getStatus());
        assertStoredHashOf(userId, other);
    }

    /**
     * Method to test: login with a password stored before the new default existed.
     * Given Scenario: A user whose stored hash is of an 8-character password that does not meet the
     * new requirements.
     * Expected Result: the password still matches; requirements apply only when a password is set.
     */
    @Test
    public void test_existingShortPassword_stillMatches() throws Exception {
        final User target = newUser();
        final String oldPassword = "oldpass1";
        target.setPassword(PasswordFactoryProxy.generateHash(oldPassword));
        target.setPasswordEncrypted(true);
        userAPI.save(target, APILocator.systemUser(), false);

        assertTrue(APILocator.getLoginServiceAPI().passwordMatch(oldPassword, userAPI.loadUserById(target.getUserId())));
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: {@code passwords.regexptoolkit.pattern} is overridden with a simple
     * 8-character pattern.
     * Expected Result: the override decides: a 10-character password is accepted, and the default
     * is back once the override is removed.
     */
    @Test
    public void test_patternOverride_replacesDefault() throws Exception {
        final String original = PropsUtil.get(PATTERN_KEY);
        final User target = newUser();
        try {
            Config.setProperty(PATTERN_KEY, "/^[a-z0-9]{8,}$/");
            target.setPassword("abcdefg123");
            userAPI.save(target, APILocator.systemUser(), true, false);
            assertStoredHashOf(target.getUserId(), "abcdefg123");
        } finally {
            Config.setProperty(PATTERN_KEY, original);
        }

        target.setPassword("abcdefg456");
        final DotDataException rejected = assertThrows(DotDataException.class,
                () -> userAPI.save(target, APILocator.systemUser(), true, false));
        assertEquals(ERROR_KEY, rejected.getMessageKey());
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: A validating save with a 14-character password.
     * Expected Result: exactly one security log entry, with the target user, the acting user, the
     * entry point of the request, the rule, and the client address that the logger appends; it never
     * contains the password or the start of it.
     */
    @Test
    public void test_rejectionLog_validatingSave_oneEntryWithoutThePassword() throws Exception {
        final HttpServletRequest request = adminRequest();
        final User target = newUser();
        target.setPassword(FOURTEEN);

        final List<String> entries = captureRejectionEntries(() -> assertThrows(DotDataException.class,
                () -> userAPI.save(target, APILocator.systemUser(), true, false)));

        assertEquals("one entry per rejection: " + entries, 1, entries.size());
        final String entry = entries.get(0);
        assertTrue(entry, entry.contains(target.getUserId()));
        assertTrue(entry, entry.contains(APILocator.systemUser().getUserId()));
        assertTrue(entry, entry.contains(request.getMethod() + " " + request.getRequestURI()));
        assertTrue(entry, entry.contains("TOO_SHORT"));
        assertTrue("the logger appends the client address: " + entry, entry.contains("ip:"));
        assertFalse(entry, entry.contains(FOURTEEN));
        assertFalse(entry, entry.contains("Abcdef"));
    }

    /**
     * Method to test: {@link UserAPI#updatePassword(User, String, User, boolean)}
     * Given Scenario: Passwords rejected for different reasons: too short, too long, and a missing
     * uppercase letter.
     * Expected Result: one entry each, with the rule that failed.
     */
    @Test
    public void test_rejectionLog_updatePassword_ruleIsReported() throws Exception {
        final User target = newUser();
        final String[][] cases = {
                {FOURTEEN, "TOO_SHORT"},
                {FIFTEEN + "x".repeat(50), "TOO_LONG"},
                {"abcdefgh12!xyzk", "MISSING_CLASS"}};
        for (final String[] testCase : cases) {
            final List<String> entries = captureRejectionEntries(() -> assertThrows(DotInvalidPasswordException.class,
                    () -> userAPI.updatePassword(target, testCase[0], APILocator.systemUser(), false)));
            assertEquals("one entry for " + testCase[1] + ": " + entries, 1, entries.size());
            assertTrue(entries.get(0), entries.get(0).contains(testCase[1]));
            assertTrue(entries.get(0), entries.get(0).contains(target.getUserId()));
        }
    }

    /**
     * Method to test: {@link UserResource#create}
     * Given Scenario: An admin creates a user with a 14-character password.
     * Expected Result: exactly one entry for the rejection, not one per layer it passes through.
     */
    @Test
    public void test_rejectionLog_restCreate_oneEntry() throws Exception {
        final String userId = "pwdlog-" + uniq();

        final List<String> entries = captureRejectionEntries(() -> assertThrows(DotDataException.class,
                () -> resource.create(adminRequest(), response,
                        createFormFor(userId).password(FOURTEEN.toCharArray()).build())));

        assertEquals("one entry per rejection: " + entries, 1, entries.size());
        assertTrue(entries.get(0), entries.get(0).contains(userId));
    }

    /**
     * Method to test: {@link UserAPI#updatePassword(User, String, User, boolean)}
     * Given Scenario: A rejection with no request in the thread-local (a call that is not an HTTP
     * request).
     * Expected Result: one entry whose entry point is {@code internal}.
     */
    @Test
    public void test_rejectionLog_withoutRequest_entryPointIsInternal() throws Exception {
        final User target = newUser();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);

        final List<String> entries = captureRejectionEntries(() -> assertThrows(DotInvalidPasswordException.class,
                () -> userAPI.updatePassword(target, FOURTEEN, APILocator.systemUser(), false)));

        assertEquals("one entry per rejection: " + entries, 1, entries.size());
        assertTrue(entries.get(0), entries.get(0).contains("internal"));
    }

    /**
     * Method to test: {@link UserAPI#save(User, User, boolean, boolean)}
     * Given Scenario: The pattern is overridden and a password is rejected by it.
     * Expected Result: the rule is {@code NOT_MET}: the classifier does not describe a customer's
     * own pattern.
     */
    @Test
    public void test_rejectionLog_patternOverride_ruleIsNotMet() throws Exception {
        final String original = PropsUtil.get(PATTERN_KEY);
        final User target = newUser();
        try {
            Config.setProperty(PATTERN_KEY, "/^[a-z]{20,}$/");
            target.setPassword(FIFTEEN);
            final List<String> entries = captureRejectionEntries(() -> assertThrows(DotDataException.class,
                    () -> userAPI.save(target, APILocator.systemUser(), true, false)));
            assertEquals("one entry per rejection: " + entries, 1, entries.size());
            assertTrue(entries.get(0), entries.get(0).contains("NOT_MET"));
        } finally {
            Config.setProperty(PATTERN_KEY, original);
        }
    }

    /**
     * Runs the action and returns the security log entries about rejected passwords written while
     * it ran.
     */
    private static List<String> captureRejectionEntries(final ThrowingRunnable action) throws Exception {
        final List<String> entries = new ArrayList<>();
        final AbstractAppender appender = new AbstractAppender("PasswordRejectionCapture", null, null, true, null) {
            @Override
            public void append(final LogEvent event) {
                final String message = event.getMessage().getFormattedMessage();
                if (message.contains("Password rejected")) {
                    entries.add(message);
                }
            }
        };
        appender.start();
        final Logger logger = (Logger) LogManager.getLogger(SecurityLogger.class);
        logger.addAppender(appender);
        try {
            action.run();
        } catch (Exception | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return entries;
    }

    /**
     * Checks that the stored password is a hash of {@code plainPassword}, not the password itself.
     */
    private static void assertStoredHashOf(final String userId, final String plainPassword) throws Exception {
        final String stored = (String) storedPassword(userId);
        assertNotEquals("the password must not be stored as sent", plainPassword, stored);
        assertEquals("the stored hash must match the password",
                AuthenticationStatus.AUTHENTICATED, PasswordFactoryProxy.authPassword(plainPassword, stored));
    }

    private static Object storedPassword(final String userId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select password_ from user_ where userId = ?")
                .addParam(userId)
                .loadObjectResults();
        assertEquals("Expected exactly one row for userId=" + userId, 1, rows.size());
        return rows.get(0).get("password_");
    }

    /** Creates a user with a password that meets the requirements, removed once after the class. */
    private static User newUser() {
        final User user = new UserDataGen().password("Initial-Pass-2026!").nextPersisted();
        usersToClean.add(user);
        return user;
    }

    private static UserForm.Builder updateFormFor(final User target) {
        return new UserForm.Builder()
                .userId(target.getUserId())
                .firstName(target.getFirstName())
                .lastName(target.getLastName())
                .email(target.getEmailAddress());
    }

    private static UserForm.Builder createFormFor(final String userId) {
        return new UserForm.Builder()
                .userId(userId)
                .firstName("Policy")
                .lastName("Create" + uniq())
                .email("pwdpolicy-" + uniq() + "@dotcms.com")
                .active(true);
    }

    /** Builds a request authenticated (Basic) as the default admin. */
    private static HttpServletRequest adminRequest() throws Exception {
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(host.getHostname(), "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        request.getSession().setAttribute(WebKeys.CURRENT_HOST, host);
        request.getSession().setAttribute(WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        return request;
    }

    private static String uniq() {
        return Long.toString(System.nanoTime());
    }
}
