package com.dotcms.rest.api.v1.user;

import static org.junit.Assert.assertEquals;
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
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.job.EncryptPlainPasswordsJob;
import com.dotmarketing.quartz.job.TestJobExecutor;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers how {@link UserResource} handles the password on {@code POST /api/v1/users} and
 * {@code PUT /api/v1/users}: the configured password policy is applied, and the password is stored
 * hashed with {@code user_.passwordEncrypted} set, so {@link EncryptPlainPasswordsJob} leaves it as
 * it is.
 */
public class UserResourcePasswordIntegrationTest {

    /** Meets the default policy in portal.properties: 15 to 64 characters with all four classes. */
    private static final String VALID_PASSWORD = "Changed-Pass-2026!";
    /** Too short for the default policy, which requires at least 15 characters. */
    private static final String SHORT_PASSWORD = "abcdef";

    private static UserResource resource;
    private static HttpServletResponse response;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new UserResource(new WebResource(new ApiProvider()), UserResourceHelper.getInstance(),
                new PaginationUtil(new UserPaginator()), new DotRestInstanceProvider()
                        .setUserAPI(APILocator.getUserAPI())
                        .setHostAPI(APILocator.getHostAPI())
                        .setRoleAPI(APILocator.getRoleAPI())
                        .setErrorHelper(ErrorResponseHelper.INSTANCE));
        response = new MockHttpResponse();
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
     * Method to test: {@link UserResource#update(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: An admin sets a new password that meets the password policy.
     * Expected Result: 200; the stored password is a hash of the new one, not the value sent, and
     * the row is flagged as encrypted.
     */
    @Test
    public void test_update_validPassword_storedHashed() throws Exception {
        final User target = newUser();

        final Response resourceResponse = resource.update(adminRequest(), response,
                updateFormFor(target).password(VALID_PASSWORD.toCharArray()).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        assertStoredHashOf(target.getUserId(), VALID_PASSWORD);
    }

    /**
     * Method to test: {@link UserResource#update(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: An admin sets a new password that does not meet the password policy.
     * Expected Result: the update is rejected with the password policy error and the stored
     * password does not change.
     */
    @Test
    public void test_update_passwordNotMeetingPolicy_rejectedAndPasswordUnchanged() throws Exception {
        final User target = newUser();
        final Map<String, Object> before = loadRow(target.getUserId());

        final DotDataException exception = assertThrows(DotDataException.class,
                () -> resource.update(adminRequest(), response,
                        updateFormFor(target).password(SHORT_PASSWORD.toCharArray()).build()));
        assertEquals("User-Info-Save-Password-Failed", exception.getMessageKey());

        final Map<String, Object> after = loadRow(target.getUserId());
        assertEquals("the stored password must not change", before.get("password_"), after.get("password_"));
    }

    /**
     * Method to test: {@link UserResource#update(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: An admin updates a user without sending a password.
     * Expected Result: 200; the stored password is left as it was.
     */
    @Test
    public void test_update_withoutPassword_passwordUnchanged() throws Exception {
        final User target = newUser();
        final Map<String, Object> before = loadRow(target.getUserId());

        final Response resourceResponse = resource.update(adminRequest(), response,
                updateFormFor(target).firstName("Renamed").build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        final Map<String, Object> after = loadRow(target.getUserId());
        assertEquals("the stored password must not change", before.get("password_"), after.get("password_"));
    }

    /**
     * Method to test: {@link UserResource#create(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: An admin creates a user with a password that meets the password policy, and
     * then {@link EncryptPlainPasswordsJob} runs.
     * Expected Result: 200; the password is stored hashed and flagged as encrypted right away, and
     * the job leaves that hash as it is, so the user can still log in with the password.
     */
    @Test
    public void test_create_validPassword_storedHashedAndKeptByEncryptJob() throws Exception {
        final String userId = "pwdcreate-" + uniq();

        final Response resourceResponse = resource.create(adminRequest(), response,
                createFormFor(userId).password(VALID_PASSWORD.toCharArray()).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());
        usersToClean.add(APILocator.getUserAPI().loadUserById(userId));

        assertStoredHashOf(userId, VALID_PASSWORD);
        final Object storedHash = loadRow(userId).get("password_");

        TestJobExecutor.execute(new EncryptPlainPasswordsJob(), new HashMap<>());

        assertEquals("the job must not change a password that is already hashed",
                storedHash, loadRow(userId).get("password_"));
        assertStoredHashOf(userId, VALID_PASSWORD);
    }

    /**
     * Method to test: {@link UserResource#create(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: An admin creates a user with a password that does not meet the password policy.
     * Expected Result: the create is rejected with the password policy error and no user is saved.
     */
    @Test
    public void test_create_passwordNotMeetingPolicy_rejectedAndUserNotCreated() throws Exception {
        final String userId = "pwdcreateshort-" + uniq();

        final DotDataException exception = assertThrows(DotDataException.class,
                () -> resource.create(adminRequest(), response,
                        createFormFor(userId).password(SHORT_PASSWORD.toCharArray()).build()));
        assertEquals("User-Info-Save-Password-Failed", exception.getMessageKey());

        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select userId from user_ where userId = ?")
                .addParam(userId)
                .loadObjectResults();
        assertTrue("no user must be saved when the password is rejected", rows.isEmpty());
    }

    /**
     * Checks that the stored password for the given user is a hash of {@code plainPassword}, not
     * the password itself, and that the row is flagged as encrypted.
     */
    private static void assertStoredHashOf(final String userId, final String plainPassword)
            throws Exception {
        final Map<String, Object> row = loadRow(userId);
        final String stored = (String) row.get("password_");
        assertNotEquals("the password must not be stored as sent", plainPassword, stored);
        assertEquals("the stored hash must match the password",
                AuthenticationStatus.AUTHENTICATED, PasswordFactoryProxy.authPassword(plainPassword, stored));
        assertTrue("the row must be flagged as encrypted", toBool(row.get("passwordencrypted")));
    }

    /** Creates a user with a password that meets the policy, removed once after the class. */
    private static User newUser() {
        final User user = new UserDataGen().password("Initial-Pass-2026!").nextPersisted();
        usersToClean.add(user);
        return user;
    }

    /** Builds an update form that keeps the user's names and email; no password, no roles. */
    private static UserForm.Builder updateFormFor(final User target) {
        return new UserForm.Builder()
                .userId(target.getUserId())
                .firstName(target.getFirstName())
                .lastName(target.getLastName())
                .email(target.getEmailAddress());
    }

    /** Builds a create form with an explicit userId so the test can read the row back. */
    private static UserForm.Builder createFormFor(final String userId) {
        return new UserForm.Builder()
                .userId(userId)
                .firstName("Password")
                .lastName("Create" + uniq())
                .email("pwdcreate-" + uniq() + "@dotcms.com")
                .active(true);
    }

    private static Map<String, Object> loadRow(final String userId) throws DotDataException {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select password_, passwordEncrypted from user_ where userId = ?")
                .addParam(userId)
                .loadObjectResults();
        assertEquals("Expected exactly one row for userId=" + userId, 1, rows.size());
        return rows.get(0);
    }

    private static boolean toBool(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
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
        // the password policy reads the request from this thread-local to localize its error,
        // as the REST filters do on a real call
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        return request;
    }

    private static String uniq() {
        return Long.toString(System.nanoTime());
    }
}