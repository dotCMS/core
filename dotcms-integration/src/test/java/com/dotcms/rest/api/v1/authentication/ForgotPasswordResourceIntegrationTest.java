package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.repackage.net.sf.hibernate.Session;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.util.EmailUtils;
import com.dotmarketing.util.MailerWrapper;
import com.dotmarketing.util.MailerWrapperFactoryImpl;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Checks that {@link ForgotPasswordResource} answers every accepted request the same way and does the
 * account-dependent work (token and email) after the response is built, still producing the reset
 * email for an existing account.
 */
public class ForgotPasswordResourceIntegrationTest {

    private static final List<SentMail> SENT = Collections.synchronizedList(new ArrayList<>());
    private static final List<User> CREATED = new ArrayList<>();

    private static final class SentMail {
        private final String to;
        private final String body;

        private SentMail(final String to, final String body) {
            this.to = to;
            this.body = body;
        }
    }

    /** A mailer that records the message instead of sending it. */
    private static final class RecordingMailer implements MailerWrapper {
        private String to;
        private String body;

        @Override public void setToEmail(final String email) { this.to = email; }
        @Override public void setToName(final String name) { }
        @Override public void setSubject(final String subject) { }
        @Override public void setHTMLBody(final String htmlBody) { this.body = htmlBody; }
        @Override public void setFromEmail(final String email) { }
        @Override public void setFromName(final String name) { }
        @Override public void setHTMLAndTextBody(final String emailText) { }
        @Override public void setTextBody(final String emailText) { }

        @Override
        public void sendMessage() {
            SENT.add(new SentMail(this.to, this.body));
            synchronized (SENT) {
                SENT.notifyAll();
            }
        }
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        EmailUtils.setMailerWrapperFactory(RecordingMailer::new);
    }

    @AfterClass
    public static void cleanup() {
        EmailUtils.setMailerWrapperFactory(new MailerWrapperFactoryImpl());
        CREATED.forEach(user -> UserDataGen.remove(user, true));
    }

    @Before
    public void clearSent() {
        SENT.clear();
    }

    /** A request like a browser's: the mock has no locale of its own, a real container always supplies one. */
    private static HttpServletRequest request() {
        return new HttpServletRequestWrapper(new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request())) {
            @Override
            public Locale getLocale() {
                return Locale.US;
            }
        };
    }

    private static Response requestReset(final ForgotPasswordResource resource, final String submittedValue) {
        return resource.forgotPassword(request(), Mockito.mock(HttpServletResponse.class),
                new ForgotPasswordForm.Builder().userId(submittedValue).build());
    }

    private static ForgotPasswordResource resourceWith(final CompanyAPI companyAPI, final List<Runnable> queued) {
        return new ForgotPasswordResource(UserLocalManagerFactory.getManager(),
                UserServiceFactory.getInstance().getUserService(),
                companyAPI, ResponseUtil.INSTANCE, queued::add);
    }

    private static User newUser() throws Exception {
        final String email = "reset-" + UUID.randomUUID() + "@example.com";
        final User user = new UserDataGen().emailAddress(email)
                .roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        CREATED.add(user);
        return user;
    }

    /** A company API for the default company, as seen with the given login type (the real company is left alone). */
    private static CompanyAPI loginBy(final String authType) throws Exception {
        final String companyId = APILocator.getCompanyAPI().getCompanyId(request());
        final Company company = new Company() {
            @Override
            public String getAuthType() {
                return authType;
            }
        };
        company.setCompanyId(companyId);
        final CompanyAPI companyAPI = Mockito.mock(CompanyAPI.class);
        Mockito.when(companyAPI.getCompany(Mockito.any(HttpServletRequest.class))).thenReturn(company);
        Mockito.when(companyAPI.getCompanyId(Mockito.any(HttpServletRequest.class)))
                .thenReturn(company.getCompanyId());
        return companyAPI;
    }

    private static String entityOf(final Response response) {
        return String.valueOf(ResponseEntityView.class.cast(response.getEntity()).getEntity());
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: login by email address, one request for an existing account and one for an
     *                 address with no account.
     * ExpectedResult: both get 200 with the submitted address and no errors, before any account work
     *                 has run; running the queued work then creates the token and sends the email for
     *                 the existing account only.
     */
    @Test
    public void emailLogin_sameResponse_andResetProducedForExistingAccount() throws Exception {

        final User user = newUser();
        final String unknown = "nobody-" + UUID.randomUUID() + "@example.com";
        final List<Runnable> queued = new ArrayList<>();
        final ForgotPasswordResource resource = resourceWith(loginBy(Company.AUTH_TYPE_EA), queued);

        final Response forExisting = requestReset(resource, user.getEmailAddress());
        final Response forUnknown = requestReset(resource, unknown);

        assertEquals(Response.Status.OK.getStatusCode(), forExisting.getStatus());
        assertEquals(forExisting.getStatus(), forUnknown.getStatus());
        assertEquals(user.getEmailAddress(), entityOf(forExisting));
        assertEquals(unknown, entityOf(forUnknown));
        assertTrue(ResponseEntityView.class.cast(forExisting.getEntity()).getErrors().isEmpty());
        assertTrue(ResponseEntityView.class.cast(forUnknown.getEntity()).getErrors().isEmpty());

        // nothing that depends on the account has happened yet
        assertEquals(2, queued.size());
        assertTrue(SENT.isEmpty());
        assertNull(APILocator.getUserAPI().loadUserById(user.getUserId()).getIcqId());

        queued.forEach(Runnable::run);

        final String token = APILocator.getUserAPI().loadUserById(user.getUserId()).getIcqId();
        assertNotNull(token);
        assertTrue(token.matches(ResetPasswordTokenUtil.VALIDATE_TOKEN_REGEX));
        assertEquals(1, SENT.size());
        assertEquals(user.getEmailAddress(), SENT.get(0).to);
        assertTrue(SENT.get(0).body.contains(token.substring(0, token.indexOf(':'))));
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: login by user id, one request for an existing user id and one for an unknown one.
     * ExpectedResult: both get 200 with the submitted user id before any account work has run; the
     *                 queued work then sends the email for the existing user only.
     */
    @Test
    public void userIdLogin_sameResponse_andResetProducedForExistingAccount() throws Exception {

        final User user = newUser();
        final String unknown = "missing." + UUID.randomUUID();
        final List<Runnable> queued = new ArrayList<>();
        final ForgotPasswordResource resource = resourceWith(loginBy(Company.AUTH_TYPE_ID), queued);

        final Response forExisting = requestReset(resource, user.getUserId());
        final Response forUnknown = requestReset(resource, unknown);

        assertEquals(Response.Status.OK.getStatusCode(), forExisting.getStatus());
        assertEquals(forExisting.getStatus(), forUnknown.getStatus());
        assertEquals(user.getUserId(), entityOf(forExisting));
        assertEquals(unknown, entityOf(forUnknown));
        assertTrue(SENT.isEmpty());

        queued.forEach(Runnable::run);

        assertEquals(1, SENT.size());
        assertEquals(user.getEmailAddress(), SENT.get(0).to);
        assertNotNull(APILocator.getUserAPI().loadUserById(user.getUserId()).getIcqId());
    }

    /** A one-thread pool built the same way as the server's reset pool, so every task shares its thread. */
    private static DotSubmitter singleThreadPool(final String name) {
        return DotConcurrentFactory.getInstance().getSubmitter(name,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(1).maxPoolSize(1).queueCapacity(10).build());
    }

    /** Runs the callable on the pool's thread after everything queued before it, and returns its result. */
    private static <T> T onPool(final DotSubmitter pool, final Callable<T> callable) throws Exception {
        return pool.submit(callable).get(30, TimeUnit.SECONDS);
    }

    private static String column(final String column, final String userId) {
        return new DotConnect().setSQL("select " + column + " from user_ where userid = ?")
                .addParam(userId).getString(column);
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: the reset for an existing account runs on a real pool thread, which starts with
     *                 no connection of its own.
     * ExpectedResult: the reset token is committed (read back through the test thread's own
     *                 connection), and afterwards the pool thread holds no database connection and
     *                 no Hibernate session.
     */
    @Test
    public void resetOnPoolThread_commitsToken_andReleasesConnectionAndSession() throws Exception {

        final User user = newUser();
        final String poolName = "forgotPasswordTestPool-" + UUID.randomUUID();
        final DotSubmitter pool = singleThreadPool(poolName);
        try {

            final ForgotPasswordResource resource = new ForgotPasswordResource(UserLocalManagerFactory.getManager(),
                    UserServiceFactory.getInstance().getUserService(), loginBy(Company.AUTH_TYPE_EA),
                    ResponseUtil.INSTANCE, pool);

            assertEquals(Response.Status.OK.getStatusCode(),
                    requestReset(resource, user.getEmailAddress()).getStatus());

            assertNothingHeldOnPoolThread(pool);

            final String token = column("icqid", user.getUserId());
            assertNotNull(token);
            assertTrue(token.matches(ResetPasswordTokenUtil.VALIDATE_TOKEN_REGEX));
            assertEquals(1, SENT.size());
        } finally {
            DotConcurrentFactory.getInstance().shutdown(poolName);
        }
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: the reset for an address with no account runs on a real pool thread, so the
     *                 lookup fails after it has opened a connection.
     * ExpectedResult: afterwards the pool thread holds no database connection and no Hibernate session.
     */
    @Test
    public void unknownAccountResetOnPoolThread_releasesConnectionAndSession() throws Exception {

        final String poolName = "forgotPasswordTestPool-" + UUID.randomUUID();
        final DotSubmitter pool = singleThreadPool(poolName);
        try {

            final ForgotPasswordResource resource = new ForgotPasswordResource(UserLocalManagerFactory.getManager(),
                    UserServiceFactory.getInstance().getUserService(), loginBy(Company.AUTH_TYPE_EA),
                    ResponseUtil.INSTANCE, pool);

            assertEquals(Response.Status.OK.getStatusCode(),
                    requestReset(resource, "nobody-" + UUID.randomUUID() + "@example.com").getStatus());

            assertNothingHeldOnPoolThread(pool);
            assertTrue(SENT.isEmpty());
        } finally {
            DotConcurrentFactory.getInstance().shutdown(poolName);
        }
    }

    /** Checks, from a task queued after everything else, that the pool's thread holds no connection or open session. */
    private static void assertNothingHeldOnPoolThread(final DotSubmitter pool) throws Exception {
        final boolean[] held = onPool(pool, () -> new boolean[] {
                DbConnectionFactory.connectionExists(),
                // a closed session stays in the holder; the next getSession() replaces it
                HibernateUtil.getSessionIfOpened().map(Session::isOpen).orElse(false)});
        assertFalse("connection left open on the pool thread", held[0]);
        assertFalse("Hibernate session left open on the pool thread", held[1]);
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: a reset runs on a pool thread, the password is then changed elsewhere, and a
     *                 second reset for the same account runs on the same pool thread.
     * ExpectedResult: the second reset keeps the new password: it does not write back the account
     *                 as it was loaded by the first reset.
     */
    @Test
    public void secondResetOnSamePoolThread_keepsPasswordChangedInBetween() throws Exception {

        final User user = newUser();
        final String poolName = "forgotPasswordTestPool-" + UUID.randomUUID();
        final DotSubmitter pool = singleThreadPool(poolName);
        try {

            final ForgotPasswordResource resource = new ForgotPasswordResource(UserLocalManagerFactory.getManager(),
                    UserServiceFactory.getInstance().getUserService(), loginBy(Company.AUTH_TYPE_EA),
                    ResponseUtil.INSTANCE, pool);

            requestReset(resource, user.getEmailAddress());
            onPool(pool, () -> null);
            final String firstToken = column("icqid", user.getUserId());
            assertNotNull(firstToken);

            final String changedPassword = "changed-" + UUID.randomUUID();
            LocalTransaction.wrap(() -> new DotConnect().executeUpdate(
                    "update user_ set password_ = ? where userid = ?", changedPassword, user.getUserId()));

            requestReset(resource, user.getEmailAddress());
            onPool(pool, () -> null);

            assertEquals(changedPassword, column("password_", user.getUserId()));
            assertFalse(firstToken.equals(column("icqid", user.getUserId())));
            assertEquals(2, SENT.size());
        } finally {
            DotConcurrentFactory.getInstance().shutdown(poolName);
        }
    }

    /**
     * Method to test: {@link ForgotPasswordResource#forgotPassword}
     * Given Scenario: the resource as the server builds it (its own thread pool), existing account.
     * ExpectedResult: 200, and the reset email still arrives, sent from the pool.
     */
    @Test
    public void defaultPool_sendsResetEmailForExistingAccount() throws Exception {

        final User user = newUser();

        final Response response = requestReset(new ForgotPasswordResource(), user.getEmailAddress());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        synchronized (SENT) {
            while (SENT.isEmpty() && System.currentTimeMillis() < deadline) {
                SENT.wait(500);
            }
        }
        assertEquals(1, SENT.size());
        assertEquals(user.getEmailAddress(), SENT.get(0).to);
    }
}
