package com.dotcms.rest.api.v1.authentication;

import static com.dotmarketing.util.Constants.CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.user.UserService;
import com.dotcms.company.CompanyAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.repackage.org.apache.struts.Globals;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.SendPasswordException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ForgotPasswordResourceTest extends UnitTestBase {


    public ForgotPasswordResourceTest() {

    }

    @Before
    public void initTest(){
        RestUtilTest.initMockContext();
    }

    @After
    public void cleanupTest(){
        Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);
        RestUtilTest.cleanupContext();
    }

    @Test
    public void testEmptyParameter() throws JSONException{

        try {
            final ForgotPasswordForm forgotPasswordForm =
                    new ForgotPasswordForm.Builder().build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testNoSuchUserExceptionExposingNoData() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final UserService userService = mock(UserService.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession(false)).thenReturn(session); //
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
            when(companyAPI.getCompany(request)).thenReturn(company);
            when(userLocalManager.getUserById(anyString()))
                    .thenAnswer(new Answer<User>() { // if this method is called, should fail

                        @Override
                        public User answer(InvocationOnMock invocation) throws Throwable {

                            throw new NoSuchUserException();
                        }
                    });

            /*
            Set the CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD property to FALSE in order to
            hide from the user that the email is not an existing user email. For security reasons.
             */
            boolean displayNotSuchUserError = Config
                    .getBooleanProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);
            if (displayNotSuchUserError) {
                Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);
            }

            final ForgotPasswordResource authenticationResource =
                    new ForgotPasswordResource(userLocalManager, userService,
                            companyAPI, responseUtil, Runnable::run);


            final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

            System.out.println(response1);
            assertNotNull(response1);
            assertEquals(response1.getStatus(), Response.Status.OK.getStatusCode());
            assertNotNull(response1.getEntity());
            System.out.println(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().isEmpty());
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testNoSuchUserExceptionExposingData() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final UserService userService = mock(UserService.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession(false)).thenReturn(session); //
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
            when(companyAPI.getCompany(request)).thenReturn(company);
            when(userLocalManager.getUserById(anyString()))
                    .thenAnswer(new Answer<User>() { // if this method is called, should fail

                        @Override
                        public User answer(InvocationOnMock invocation) throws Throwable {

                            throw new NoSuchUserException();
                        }
                    });

            /*
            Set the CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD property to TRUE in order to
            send a response informing the user the email is an non existing user email.
             */
            boolean displayNotSuchUserError = Config
                    .getBooleanProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);
            if (!displayNotSuchUserError) {
                Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, true);
            }

            final ForgotPasswordResource authenticationResource =
                    new ForgotPasswordResource(userLocalManager, userService,
                            companyAPI, responseUtil);


            final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

            System.out.println(response1);
            assertNotNull(response1);
            assertEquals(response1.getStatus(), Status.BAD_REQUEST.getStatusCode());
            assertNotNull(response1.getEntity());
            System.out.println(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals
                    ("the-email-address-you-requested-is-not-registered-in-our-database"));
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testSendPasswordException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil authenticationHelper = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final UserService userService = mock(UserService.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession(false)).thenReturn(session); //
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
            when(companyAPI.getCompany(request)).thenReturn(company);
            when(userLocalManager.getUserById(anyString()))
                    .thenAnswer(new Answer<User>() { // if this method is called, should fail

                        @Override
                        public User answer(InvocationOnMock invocation) throws Throwable {

                            throw new SendPasswordException();
                        }
                    });



            // the user is looked up on the request thread only when unknown accounts are reported
            Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, true);

            final ForgotPasswordResource authenticationResource =
                    new ForgotPasswordResource(userLocalManager, userService,
                            companyAPI, authenticationHelper, Runnable::run);


            final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

            System.out.println(response1);
            assertNotNull(response1);
            assertEquals(response1.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
            assertNotNull(response1.getEntity());
            System.out.println(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals
                    ("a-new-password-can-only-be-sent-to-an-external-email-address"));
        } finally {
            Config.CONTEXT = null;
        }
    }

    /**
     * Given login by user id and an existing account,
     * when a reset is requested,
     * then the response is 200 and carries the submitted value.
     */
    @Test
    public void userIdLogin_existingAccount_returnsSubmittedValue() throws Exception {

        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final UserService userService = mock(UserService.class);
        final User account = mock(User.class);
        when(account.getEmailAddress()).thenReturn("owner@example.com");
        when(userLocalManager.getUserById("owner.id")).thenReturn(account);

        final Response response = requestReset(Company.AUTH_TYPE_ID, "owner.id", userLocalManager, userService);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("owner.id", entityOf(response));
    }

    /**
     * Given login by user id and no account for the submitted id (default configuration),
     * when a reset is requested,
     * then the response is the same as for an existing account: 200 with the submitted value.
     */
    @Test
    public void userIdLogin_unknownAccount_returnsSubmittedValue() throws Exception {

        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final UserService userService = mock(UserService.class);
        when(userLocalManager.getUserById("missing.id")).thenThrow(new NoSuchUserException());

        final Response response = requestReset(Company.AUTH_TYPE_ID, "missing.id", userLocalManager, userService);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("missing.id", entityOf(response));
    }

    /**
     * Given login by email and a value that is not an email address,
     * when a reset is requested,
     * then the request is rejected with 400.
     */
    @Test
    public void emailLogin_malformedAddress_isRejected() throws Exception {

        final UserService userService = mock(UserService.class);

        final Response response = requestReset(Company.AUTH_TYPE_EA, "1",
                mock(UserLocalManager.class), userService);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("please-enter-a-valid-email-address", firstErrorCode(response));
    }

    /**
     * Given login by email and a well-formed address whose reset cannot be completed,
     * when a reset is requested,
     * then the response is still 200 with the submitted address.
     */
    @Test
    public void emailLogin_resetFailure_returnsSubmittedValue() throws Exception {

        final UserService userService = mock(UserService.class);
        doThrow(new UserEmailAddressException()).when(userService)
                .sendResetPassword(any(), eq("owner@example.com"), any(Locale.class));

        final Response response = requestReset(Company.AUTH_TYPE_EA, "owner@example.com",
                mock(UserLocalManager.class), userService);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("owner@example.com", entityOf(response));
    }

    /**
     * Given login by email and any address (default configuration),
     * when a reset is requested,
     * then nothing that depends on the account runs before the response is returned: the lookup,
     * token and email are handed to the reset pool, and run there.
     */
    @Test
    public void emailLogin_accountWork_runsOnTheResetPool() throws Exception {

        final UserService userService = mock(UserService.class);
        final List<Runnable> queued = new ArrayList<>();

        final Response response = requestReset(Company.AUTH_TYPE_EA,
                new ForgotPasswordForm.Builder().userId("owner@example.com").build(),
                mock(UserLocalManager.class), userService, queued::add);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("owner@example.com", entityOf(response));
        assertEquals(1, queued.size());
        verifyNoInteractions(userService);

        queued.get(0).run();

        verify(userService).sendResetPassword(any(), eq("owner@example.com"), any(Locale.class));
    }

    /**
     * Given login by user id,
     * when a reset is requested,
     * then the user is looked up on the reset pool, not on the request thread.
     */
    @Test
    public void userIdLogin_lookup_runsOnTheResetPool() throws Exception {

        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final UserService userService = mock(UserService.class);
        final User account = mock(User.class);
        when(account.getEmailAddress()).thenReturn("owner@example.com");
        when(userLocalManager.getUserById("owner.id")).thenReturn(account);
        final List<Runnable> queued = new ArrayList<>();

        requestReset(Company.AUTH_TYPE_ID, new ForgotPasswordForm.Builder().userId("owner.id").build(),
                userLocalManager, userService, queued::add);

        verifyNoInteractions(userLocalManager, userService);

        queued.get(0).run();

        verify(userService).sendResetPassword(any(), eq("owner@example.com"), any(Locale.class));
    }

    /**
     * Given the reset pool is the server's kind of bounded submitter and it is full,
     * when a reset is requested,
     * then the response is the same 200 as for an accepted request, nothing is thrown, the
     * account work is not run, and the value is not left marked as pending.
     */
    @Test
    public void resetPool_full_answersTheSame200() throws Exception {

        final String poolName = "forgotPasswordTestPool-" + UUID.randomUUID();
        final DotSubmitter pool = DotConcurrentFactory.getInstance().getSubmitter(poolName,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(1).maxPoolSize(1).queueCapacity(1)
                        .rejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy())
                        .build());
        final CountDownLatch release = new CountDownLatch(1);
        try {

            // occupy the only thread and the only queue slot
            pool.execute(() -> awaitQuietly(release));
            pool.execute(() -> awaitQuietly(release));

            final UserService userService = mock(UserService.class);
            final String email = "owner-" + UUID.randomUUID() + "@example.com";
            final Response response = requestReset(Company.AUTH_TYPE_EA,
                    new ForgotPasswordForm.Builder().userId(email).build(),
                    mock(UserLocalManager.class), userService, pool);

            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            assertEquals(email, entityOf(response));
            assertTrue(((ResponseEntityView<?>) response.getEntity()).getErrors().isEmpty());
            verifyNoInteractions(userService);
            assertFalse(ForgotPasswordResource.isResetPending(
                    ForgotPasswordResource.pendingKey(RestUtilTest.DEFAULT_COMPANY, email, false)));
        } finally {

            release.countDown();
            DotConcurrentFactory.getInstance().shutdown(poolName);
        }
    }

    /**
     * Given a reset is already queued for an address,
     * when another reset is requested for the same address (in a different case),
     * then both requests get the same 200 but only one reset is queued, and once it has run the
     * address can be queued again.
     */
    @Test
    public void duplicatePendingValue_isQueuedOnce() throws Exception {

        final UserService userService = mock(UserService.class);
        final String email = "owner-" + UUID.randomUUID() + "@example.com";
        final List<Runnable> queued = new ArrayList<>();

        final Response first = requestReset(Company.AUTH_TYPE_EA,
                new ForgotPasswordForm.Builder().userId(email).build(),
                mock(UserLocalManager.class), userService, queued::add);
        final Response second = requestReset(Company.AUTH_TYPE_EA,
                new ForgotPasswordForm.Builder().userId(email.toUpperCase()).build(),
                mock(UserLocalManager.class), userService, queued::add);

        assertEquals(Status.OK.getStatusCode(), first.getStatus());
        assertEquals(Status.OK.getStatusCode(), second.getStatus());
        assertEquals(email.toUpperCase(), entityOf(second));
        assertEquals(1, queued.size());

        queued.get(0).run();
        requestReset(Company.AUTH_TYPE_EA, new ForgotPasswordForm.Builder().userId(email).build(),
                mock(UserLocalManager.class), userService, queued::add);
        assertEquals(2, queued.size());
        queued.get(1).run();

        verify(userService, times(2)).sendResetPassword(any(), eq(email), any(Locale.class));
    }

    /**
     * Given the queued reset fails, either with a known reset error or an unexpected one,
     * when it has run,
     * then the value is no longer marked as pending.
     */
    @Test
    public void queuedReset_alwaysReleasesItsPendingEntry() throws Exception {

        for (final Exception failure : List.of(new NoSuchUserException(), new UserEmailAddressException(),
                new IllegalStateException("mail relay down"))) {

            final UserService userService = mock(UserService.class);
            final String email = "owner-" + UUID.randomUUID() + "@example.com";
            doThrow(failure).when(userService).sendResetPassword(any(), eq(email), any(Locale.class));
            final String key = ForgotPasswordResource.pendingKey(RestUtilTest.DEFAULT_COMPANY, email, false);
            final List<Runnable> queued = new ArrayList<>();

            requestReset(Company.AUTH_TYPE_EA, new ForgotPasswordForm.Builder().userId(email).build(),
                    mock(UserLocalManager.class), userService, queued::add);
            assertTrue(ForgotPasswordResource.isResetPending(key));

            queued.get(0).run();

            assertFalse(failure.getClass().getSimpleName(), ForgotPasswordResource.isResetPending(key));
        }
    }

    private static void awaitQuietly(final CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Given a request with no body,
     * when a reset is requested,
     * then the request is rejected with 400.
     */
    @Test
    public void emptyBody_isRejected() throws Exception {

        final Response response = requestReset(Company.AUTH_TYPE_EA, (ForgotPasswordForm) null,
                mock(UserLocalManager.class), mock(UserService.class));

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Calls the resource for the given login type and submitted value, with
     * {@code CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD} set to its default (false) for the
     * duration of the call and restored afterwards.
     */
    private Response requestReset(final String authType, final String submittedValue,
                                  final UserLocalManager userLocalManager,
                                  final UserService userService) throws Exception {

        return requestReset(authType, new ForgotPasswordForm.Builder().userId(submittedValue).build(),
                userLocalManager, userService);
    }

    /**
     * Calls the resource for the given login type and form (may be {@code null}), with
     * {@code CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD} set to its default (false) for the
     * duration of the call and restored afterwards.
     */
    private Response requestReset(final String authType, final ForgotPasswordForm form,
                                  final UserLocalManager userLocalManager,
                                  final UserService userService) throws Exception {

        return requestReset(authType, form, userLocalManager, userService, Runnable::run);
    }

    /**
     * Same as above, with the given executor standing in for the reset pool.
     */
    private Response requestReset(final String authType, final ForgotPasswordForm form,
                                  final UserLocalManager userLocalManager,
                                  final UserService userService,
                                  final Executor executor) throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final ServletContext context = mock(ServletContext.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return authType;
            }
        };

        final boolean previousDisplayFlag =
                Config.getBooleanProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);
        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault());
            when(companyAPI.getCompany(request)).thenReturn(company);
            when(companyAPI.getCompanyId(request)).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, false);

            return new ForgotPasswordResource(userLocalManager, userService, companyAPI, ResponseUtil.INSTANCE,
                    executor).forgotPassword(request, mock(HttpServletResponse.class), form);
        } finally {
            Config.setProperty(CONFIG_DISPLAY_NOT_EXISTING_USER_AT_RECOVER_PASSWORD, previousDisplayFlag);
            Config.CONTEXT = null;
        }
    }

    /**
     * Returns the {@code entity} value of a {@link ResponseEntityView} response.
     */
    private static Object entityOf(final Response response) {

        assertTrue(response.getEntity() instanceof ResponseEntityView);
        return ((ResponseEntityView<?>) response.getEntity()).getEntity();
    }

    /**
     * Returns the error code of the first error of a {@link ResponseEntityView} response.
     */
    private static String firstErrorCode(final Response response) {

        assertTrue(response.getEntity() instanceof ResponseEntityView);
        final ResponseEntityView<?> view = (ResponseEntityView<?>) response.getEntity();
        assertTrue(view.getErrors().size() > 0);
        return view.getErrors().get(0).getErrorCode();
    }

}
