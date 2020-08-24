package com.dotcms.ema;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class EMAWebInterceptorTest {

    private static HttpServletRequest request = mock(HttpServletRequest.class);
    private static HttpServletResponse response = mock(HttpServletResponse.class);
    private static HttpSession session = mock(HttpSession.class);
    private static EMAWebInterceptor emaWebInterceptor;
    private static User admin;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        emaWebInterceptor = new EMAWebInterceptor();
        admin = TestUserUtils.getAdminUser();

        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.EDIT_MODE.name());
        when(session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID)).thenReturn(admin.getUserId());
    }

    /**
     * Method to test: {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Intercept a request but there is no secret set for the EMA App
     * ExpectedResult: Result.Next since there is no EMA
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretNotExist_ReturnNext() throws IOException {
        final Host host = new SiteDataGen().name("EmaSecretDoesNotExist").nextPersisted();

        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());

        final Result result = emaWebInterceptor.intercept(request,response);
        Assert.assertNotNull(result);
        Assert.assertEquals(Result.NEXT,result);
    }

    /**
     * Method to test: {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Intercept a request and there is a secret set for the host
     * ExpectedResult: Result.response is an instance of {@link MockHttpCaptureResponse}
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretExistAtHost_ReturnResult() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().name("EmaSecretAtHost").nextPersisted();
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());

        //Create app secret
        final AppSecrets emaAppSecrets = new AppSecrets.Builder()
                .withKey(EMAWebInterceptor.EMA_APP_CONFIG_KEY)
                .withSecret(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR,"testValue")
                .build();
        APILocator.getAppsAPI().saveSecrets(emaAppSecrets,host,admin);

        final Result result = emaWebInterceptor.intercept(request,response);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getResponse() instanceof MockHttpCaptureResponse);
        Assert.assertNotEquals(Result.NEXT,result);
    }

    /**
     * Method to test: {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Intercept a request and there is a secret set for the SYSTEM_HOST
     * ExpectedResult: Result.response is an instance of {@link MockHttpCaptureResponse}
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretExistAtSystemHost_ReturnResult() throws IOException, DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().name("EmaSecretAtSystemHost").nextPersisted();
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());

        //Create app secret
        final AppSecrets emaAppSecrets = new AppSecrets.Builder()
                .withKey(EMAWebInterceptor.EMA_APP_CONFIG_KEY)
                .withSecret(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR,"testValue")
                .build();
        APILocator.getAppsAPI().saveSecrets(emaAppSecrets,APILocator.systemHost(),admin);

        final Result result = emaWebInterceptor.intercept(request,response);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getResponse() instanceof MockHttpCaptureResponse);
        Assert.assertNotEquals(Result.NEXT,result);
    }

}
