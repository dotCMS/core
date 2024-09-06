package com.dotcms.ema;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@link EMAWebInterceptor} class works as expected.
 *
 * @author Erick Gonzalez
 * @since Aug 3rd, 2020
 */
public class EMAWebInterceptorTest {

    private static final HttpServletRequest request = mock(HttpServletRequest.class);
    private static final HttpServletResponse response = mock(HttpServletResponse.class);
    private static final HttpSession session = mock(HttpSession.class);
    private static EMAWebInterceptor emaWebInterceptor;
    private static User admin;
    private static AppsAPI appsAPI;

    private static final String DUMMY_PROXY_URL = "http://dummy.proxy.url/ema";
    private static final String DUMMY_OVERRIDDEN_PROXY_URL = "http://dummy.overridden.proxy.url/ema";
    private static final String API_CALL = "/api/v1/page/render";
    private static final String INDEX_PAGE = API_CALL + "/index";
    private static final String DEMO_SINGLE_EMA_CONFIG = "["
                                                             + "   {"
                                                             + "      \"pattern\":\".*\" ,"
                                                             + "      \"urlEndpoint\": \"%s\","
                                                             + "      \"includeRendered\": false,"
                                                             + "      \"headers\":{"
                                                             + "         \"authenticationToken\": \"\","
                                                             + "         \"X-CONTENT-APP\": \"dotCMS\""
                                                             + "      }"
                                                             + "   }"
                                                         + "]";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        APILocator.getContentletIndexAPI().checkAndInitialiazeIndex();

        appsAPI = APILocator.getAppsAPI();
        emaWebInterceptor = new EMAWebInterceptor();
        admin = TestUserUtils.getAdminUser();

        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.EDIT_MODE.name());
        when(session.getAttribute(com.liferay.portal.util.WebKeys.USER_ID)).thenReturn(admin.getUserId());
        when(request.getRequestURI()).thenReturn(INDEX_PAGE);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Intercept a request made to a page in a Site where the EMA App is not
     *     configured at all.</li>
     *     <li><b>Expected Result:</b> The returned value is the default {@link Result#NEXT} since there is no EMA
     *     configuration.</li>
     * </ul>
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretDoesNotExist_ReturnNext() {
        this.createAndSetTestSite("EmaSecretDoesNotExist");
        final Result result = emaWebInterceptor.intercept(request,response);
        assertNotNull("The filter Result must be generated", result);
        assertEquals("The HTTP Response object in 'result' MUST be null", Result.NEXT, result);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Intercept a request made to a page in a Site where the EMA App is configured
     *     correctly.</li>
     *     <li><b>Expected Result:</b> The generated {@link Result#getResponse()} is not null and is an instance of
     *     {@link MockHttpCaptureResponse}.</li>
     * </ul>
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretExistAtHost_ReturnResult() throws DotDataException, DotSecurityException {
        final Host site = this.createAndSetTestSite("EmaSecretAtHost");
        // Create app secret
        final String emaConfig = String.format(DEMO_SINGLE_EMA_CONFIG, DUMMY_PROXY_URL);
        final AppSecrets emaAppSecrets = new AppSecrets.Builder()
                .withKey(EMAWebInterceptor.EMA_APP_CONFIG_KEY)
                .withSecret(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR, emaConfig)
                .build();
        appsAPI.saveSecrets(emaAppSecrets, site, admin);

        final Result result = emaWebInterceptor.intercept(request,response);
        assertNotNull("The filter Result must be generated", result);
        assertTrue("The generated Response MUST BE an instance of 'MockHttpCaptureResponse'",
                result.getResponse() instanceof MockHttpCaptureResponse);
        assertNotEquals("The HTTP Response object in 'result' CANNOT be null", Result.NEXT, result);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link EMAWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}</li>
     *     <li><b>Given Scenario:</b> Intercept a request made to a page in a Site where the EMA App is not configured
     *     at all, but the System Host does have a valid configuration.</li>
     *     <li><b>Expected Result:</b> The generated {@link Result#getResponse()} is not null and is an instance of
     *     {@link MockHttpCaptureResponse}.</li>
     * </ul>
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretExistsAtSystemHost_ReturnResult() throws DotDataException, DotSecurityException {
        this.createAndSetTestSite("EmaSecretAtSystemHost");
        // Create app secret
        final String emaConfig = String.format(DEMO_SINGLE_EMA_CONFIG, DUMMY_PROXY_URL);
        final AppSecrets emaAppSecrets = new AppSecrets.Builder()
                .withKey(EMAWebInterceptor.EMA_APP_CONFIG_KEY)
                .withSecret(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR, emaConfig)
                .build();
        appsAPI.saveSecrets(emaAppSecrets, APILocator.systemHost(), admin);

        final Result result = emaWebInterceptor.intercept(request,response);
        assertNotNull("The filter Result must be generated", result);
        assertTrue("The generated Response MUST BE an instance of 'MockHttpCaptureResponse'",
                result.getResponse() instanceof MockHttpCaptureResponse);
        assertNotEquals("The HTTP Response object in 'result' CANNOT be null", Result.NEXT, result);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link EMAWebInterceptor#proxyUrl(Host, HttpServletRequest)}</li>
     *     <li><b>Given Scenario:</b> Overrides the EMA Server URL by setting it via a Request Parameter using the
     *     {@link EMAWebInterceptor#PROXY_EDIT_MODE_URL_VAR}</li>
     *     <li><b>Expected Result:</b> The generated {@link Result#getResponse()} is not null and is an instance of
     *     @link MockHttpCaptureResponse}.</li>
     * </ul>
     */
    @Test
    public void test_EMAInterceptor_intercept_SecretExistsAtHost_ReturnResult_Query_String() throws DotDataException, DotSecurityException {
        final Host site = this.createAndSetTestSite("EmaSecretAtHostQueryString");
        when(request.getParameter(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR)).thenReturn(DUMMY_OVERRIDDEN_PROXY_URL);
        // Create app secret
        final String emaConfig = String.format(DEMO_SINGLE_EMA_CONFIG, DUMMY_PROXY_URL);
        final AppSecrets emaAppSecrets = new AppSecrets.Builder()
                .withKey(EMAWebInterceptor.EMA_APP_CONFIG_KEY)
                .withSecret(EMAWebInterceptor.PROXY_EDIT_MODE_URL_VAR, emaConfig)
                .build();
        appsAPI.saveSecrets(emaAppSecrets, site, admin);

        final Optional<String> proxyUrlOpt = emaWebInterceptor.proxyUrl(site, request);
        assertNotNull("Optional variable CANNOT be null", proxyUrlOpt);
        assertTrue("Optional proxy MUST be present", proxyUrlOpt.isPresent());
        assertEquals("Proxy URL does not equal overridden value", DUMMY_OVERRIDDEN_PROXY_URL, proxyUrlOpt.get());
    }

    /**
     * Utility method that creates a Site and loads its Identifier into the HTTP Request object. For easier tracking
     * purposes, its name will be composed of this test class' name, the given name, and the current time in millis.
     *
     * @param name The seed Site name.
     *
     * @return The new {@link Host} object.
     */
    private Host createAndSetTestSite(final String name) {
        final String testSiteName =
                EMAWebInterceptorTest.class.getSimpleName() + "-" + name + "-" + System.currentTimeMillis();
        final Host site = new SiteDataGen().name(testSiteName).nextPersisted();
        when(request.getParameter("host_id")).thenReturn(site.getIdentifier());
        return site;
    }

}
