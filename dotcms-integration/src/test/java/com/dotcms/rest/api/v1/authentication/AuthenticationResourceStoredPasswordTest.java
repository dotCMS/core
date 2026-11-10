package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portal.struts.DotCustomLoginPostAction;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link AuthenticationResource#authentication} that set the stored
 * password of a user directly and check which typed passwords the login accepts.
 */
public class AuthenticationResourceStoredPasswordTest {

    private static final String PASSWORD = "Str0ng-Test-Passw0rd!";

    private final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void cleanup() {
        createdUsers.forEach(UserDataGen::remove);
        createdUsers.clear();
    }

    /**
     * Given: a user whose stored password is the reserved directory-account value.
     * When:  that value is sent as the password.
     * Then:  the login is rejected.
     */
    @Test
    public void test_authentication_reservedDirectoryValue_isRejected() throws Exception {
        final String stored = DotCustomLoginPostAction.FAKE_PASSWORD;
        final User user = userWithStoredPassword(stored);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), login(user, stored));
    }

    /**
     * Given: a user whose stored password is in the older digest format.
     * When:  the stored digest is sent as the password.
     * Then:  the login is rejected.
     */
    @Test
    public void test_authentication_storedDigest_isRejected() throws Exception {
        final String stored = PublicEncryptionFactory.digestString(PASSWORD);
        final User user = userWithStoredPassword(stored);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), login(user, stored));
    }

    /**
     * Given: a user whose stored password is plain text.
     * When:  that same text is sent as the password.
     * Then:  the login is rejected.
     */
    @Test
    public void test_authentication_plainTextValue_isRejected() throws Exception {
        final User user = userWithStoredPassword(PASSWORD);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), login(user, PASSWORD));
    }

    /**
     * Given: a user whose password is stored as a secure hash.
     * When:  the stored hash is sent as the password, and then the real password.
     * Then:  the stored hash is rejected and the real password logs in.
     */
    @Test
    public void test_authentication_secureHash_onlyRealPasswordLogsIn() throws Exception {
        final String stored = PasswordFactoryProxy.generateHash(PASSWORD);
        final User user = userWithStoredPassword(stored);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), login(user, stored));
        assertEquals(Response.Status.OK.getStatusCode(), login(user, PASSWORD));
    }

    /**
     * Sends a login for the user through {@link AuthenticationResource} with a fresh request
     * and session.
     *
     * @param user     the {@link User} to log in as
     * @param password the password to send
     * @return the HTTP status of the response
     */
    private int login(final User user, final String password) {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        final String login = Company.AUTH_TYPE_ID.equals(
                PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
        final AuthenticationForm form = new AuthenticationForm.Builder()
                .userId(login)
                .password(password)
                .build();

        return new AuthenticationResource()
                .authentication(request, new MockHttpResponse().response(), form)
                .getStatus();
    }

    /**
     * Creates an active user, overwrites its stored password directly in the database, and
     * clears it from the cache so the login reads the new value.
     *
     * @param storedPassword the value to place in {@code user_.password_}
     * @return the created {@link User}
     */
    private User userWithStoredPassword(final String storedPassword) throws Exception {
        final User user = new UserDataGen()
                .emailAddress("auth-resource-" + UUIDGenerator.generateUuid() + "@dotcms.test")
                .roles(APILocator.getRoleAPI().loadFrontEndUserRole())
                .nextPersisted();
        createdUsers.add(user);

        new DotConnect()
                .setSQL("update user_ set password_ = ? where userId = ?")
                .addParam(storedPassword)
                .addParam(user.getUserId())
                .loadResult();
        CacheLocator.getUserCache().remove(user.getUserId());

        return user;
    }
}
