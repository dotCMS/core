package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.api.system.user.UserServiceFactory;
import com.dotcms.company.CompanyAPI;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
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
