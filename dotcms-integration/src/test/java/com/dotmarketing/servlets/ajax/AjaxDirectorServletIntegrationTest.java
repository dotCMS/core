package com.dotmarketing.servlets.ajax;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.LoginLockout;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.dotcms.DefaultBackEndLoginRequiredWebInterceptor;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AjaxDirectorServletIntegrationTest {

    private static final List<User> urlLoginUsers = new ArrayList<>();

    /** Removes only the disposable URL-login accounts and their lockout state. */
    @AfterClass
    public static void cleanUpUrlLoginUsers() throws Exception {
        for (final User user : urlLoginUsers) {
            try {
                LoginLockout.getInstance().clear(user.getUserId());
            } finally {
                UserDataGen.remove(user);
            }
        }
        urlLoginUsers.clear();
    }

    /** Exercises real password verification and Ajax dispatch, not a mocked WebResource. */
    @Test
    public void legacyUrlLoginWorksOnlyWithBothCompatibilitySettings() throws Exception {
        final String previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        final String previousUrlCredentials = Config.getStringProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", null);
        final String previousForceSsl = Config.getStringProperty("FORCE_SSL_ON_RESP_API", null);
        final String previousPrincipal = PrincipalThreadLocal.getName();
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final String password = "Ajax-" + UUID.randomUUID() + "A1!";
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadCMSAdminRole(),
                        APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        urlLoginUsers.add(user);
        APILocator.getUserAPI().updatePassword(user, password, APILocator.systemUser(), false);
        assertTrue("Fixture must be a real back-end user", user.isBackendUser());
        assertTrue("Fixture password must be stored through the password API",
                APILocator.getLoginServiceAPI().passwordMatch(password,
                        APILocator.getUserAPI().loadUserById(user.getUserId())));
        final String username = Company.AUTH_TYPE_EA.equals(CompanyUtils.getDefaultCompany().getAuthType())
                ? user.getEmailAddress() : user.getUserId();
        try {
            Config.setProperty("FORCE_SSL_ON_RESP_API", false);
            for (final String[] keys : new String[][]{{"u", "p"}, {"user", "passwd"}, {"user", "password"}}) {
                for (final String transport : new String[]{"path", "query"}) {
                    // user/password query fields were not a legacy Ajax login spelling.
                    if ("query".equals(transport) && "password".equals(keys[1])) {
                        continue;
                    }
                    Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
                    Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", true);
                    assertUrlLogin(user, username, password, keys, transport, true);
                    assertUrlLogin(user, username, "wrong-" + password, keys, transport, false);
                    Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", false);
                    assertUrlLogin(user, username, password, keys, transport, false);
                    Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", true);
                    for (final String mode : new String[]{"NONE", "JWT_TOKEN"}) {
                        Config.setProperty("ALLOW_BASIC_AUTH", mode);
                        assertUrlLogin(user, username, password, keys, transport, false);
                    }
                }
            }
        } finally {
            Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
            Config.setProperty("ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS", previousUrlCredentials);
            Config.setProperty("FORCE_SSL_ON_RESP_API", previousForceSsl);
            PrincipalThreadLocal.setName(previousPrincipal);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    /** Runs a fresh, cookie-free request through the unchanged path filter and real Ajax servlet. */
    private void assertUrlLogin(final User user, final String username, final String password,
            final String[] keys, final String transport, final boolean allowed) throws Exception {
        final String endpoint = "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/action";
        final String path = endpoint + ("path".equals(transport)
                ? "/" + keys[0] + "/" + username + "/" + keys[1] + "/" + password : "");
        final Map<String, String> query = "query".equals(transport)
                ? Map.of(keys[0], username, keys[1], password) : Map.of();
        final HttpServletRequest request = spy(new MockParameterRequest(
                new MockHttpRequestIntegrationTest("localhost", path).request(), query).request());
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn("/DotAjaxDirector");
        when(request.getPathInfo()).thenReturn(path.substring("/DotAjaxDirector".length()));
        final HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final DefaultBackEndLoginRequiredWebInterceptor interceptor =
                new DefaultBackEndLoginRequiredWebInterceptor();
        interceptor.init();
        try {
            assertEquals(Result.NEXT, interceptor.intercept(request, response));
            new AjaxDirectorServlet(new WebResource()).service(request, response);
            if (allowed) {
                assertEquals(user.getUserId(), request.getAttribute(WebKeys.USER_ID));
                verify(response, never()).sendError(anyInt());
                verify(response, never()).sendError(anyInt(), anyString());
            } else {
                verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                assertNull(request.getAttribute(WebKeys.USER));
            }
        } finally {
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] testCases(){

        // first test case RemotePublishAjaxAction.addToBundle
        final AjaxDirectorServletTestCase remotePublishAjaxActionAddToBundle = new AjaxDirectorServletTestCase();

        final String assetIdentifier = "71bf1747-56b9-41ca-a3fa-1fc7b8471dba";
        final String bundleName = "testingBundle";

        remotePublishAjaxActionAddToBundle.setRequestJSON("{\n"
                + "    \"assetIdentifier\": \""+assetIdentifier+"\" ,\n"
                + "    \"bundleName\": \""+bundleName+"\",\n"
                + "}");

        remotePublishAjaxActionAddToBundle.setRequestURI("/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle");

        remotePublishAjaxActionAddToBundle.setAssertion(() -> {
            try {
                Bundle bundle = APILocator.getBundleAPI().getBundleByName("testingBundle");

                PublisherAPI publisherAPI = PublisherAPI.getInstance();
                List<PublishQueueElement> foundElements =
                        publisherAPI.getQueueElementsByBundleId(bundle.getId());

                final boolean anyMatch = foundElements.stream().anyMatch(
                        (element) -> element.getAsset()
                                .equals("71bf1747-56b9-41ca-a3fa-1fc7b8471dba"));

                assertTrue("Asset added to the Bundle", anyMatch);
            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }

        });

        remotePublishAjaxActionAddToBundle.setDisposer(() -> Sneaky.sneaked(()-> {
            Bundle bundle = APILocator.getBundleAPI().getBundleByName("testingBundle");

            if (bundle != null) {
                APILocator.getBundleAPI().deleteBundle(bundle.getId());
            }
        }));

        // second test case = RemotePublishAjaxAction.pushBundle
        final AjaxDirectorServletTestCase remotePublishAjaxActionPushBundle = new AjaxDirectorServletTestCase();

        final String bundleId = UUID.randomUUID().toString();

        final Date publishDate = Date.from(LocalDate.of(2001, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        final String publishDateString = DateUtil.format(publishDate, "yyyy-MM-dd");

        final Date expireDate = Date.from(LocalDate.of(2051, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        final String expireDateString = DateUtil.format(expireDate, "yyyy-MM-dd");

        final List<String> environmentsIds = Arrays.asList(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final String environmentIdsString = String.join(",", environmentsIds);

        remotePublishAjaxActionPushBundle.setInitializer(() -> {
            try {
                environmentsIds.forEach((environmentId) -> {
                    try {
                        final Environment environment = new Environment();
                        environment.setId(environmentId);
                        environment.setName(environmentId);
                        environment.setPushToAll(true);
                        APILocator.getEnvironmentAPI().saveEnvironment(environment, null);
                    } catch(DotDataException | DotSecurityException e) {
                        throw new RuntimeException(e);
                    }
                });

                final Bundle bundle = new Bundle();
                bundle.setId(bundleId);
                bundle.setPublishDate(publishDate);
                bundle.setExpireDate(expireDate);
                bundle.setOwner(APILocator.systemUser().getUserId());

                APILocator.getBundleAPI().saveBundle(bundle);

                // add an asset to the bundle
                PublisherAPI.getInstance()
                        .saveBundleAssets(List.of(assetIdentifier),
                                bundle.getId(), APILocator.systemUser());
            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }
        });


        remotePublishAjaxActionPushBundle.setRequestJSON("{\n"
                + "    \"assetIdentifier\": \""+bundleId+"\",\n"
                + "    \"remotePublishDate\":\""+publishDateString+"\",\n"
                + "    \"remotePublishTime\":\"10-00\",\n"
                + "    \"remotePublishExpireDate\":\""+expireDateString+"\",\n"
                + "    \"remotePublishExpireTime\":\"10-00\",\n"
                + "    \"iWantTo\":\"publish\",\n"
                + "    \"whoToSend\":\""+environmentIdsString+"\"\n"
                + "}");

        remotePublishAjaxActionPushBundle.setRequestURI("/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle");

        remotePublishAjaxActionPushBundle.setAssertion(() -> {
            try {
                List<String> foundEnvironmentsIdsByBundle = APILocator.getEnvironmentAPI()
                        .findEnvironmentsByBundleId(bundleId).stream().map(Environment::getId)
                        .collect(Collectors.toList());

                assertTrue("Bundle ready to be pushed to all environments",
                    foundEnvironmentsIdsByBundle.containsAll(environmentsIds));

                assertTrue(PublisherAPI.getInstance()
                        .getQueueElementsByBundleId(bundleId).stream()
                        .allMatch((queueElement)->
                                queueElement.getOperation()==PublisherAPI.ADD_OR_UPDATE_ELEMENT));

            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }

        });

        remotePublishAjaxActionPushBundle.setDisposer(() -> Sneaky.sneaked(()-> {
            Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);

            if(bundle != null) {
                APILocator.getBundleAPI().deleteBundle(bundle.getId());
            }

            environmentsIds.forEach((environmentId) -> Sneaky.sneaked(()-> {
                APILocator.getEnvironmentAPI().deleteEnvironment(environmentId);
            }));
        }));



        return new AjaxDirectorServletTestCase[] {
                remotePublishAjaxActionAddToBundle,
                remotePublishAjaxActionPushBundle
        };

    }

    @Test
    @UseDataProvider("testCases")
    public void testService(final AjaxDirectorServletTestCase testCase)
            throws IOException, ServletException {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(APILocator.systemUser());
        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class),
                anyBoolean(), any()))
                .thenReturn(dataObject);

        final AjaxDirectorServlet ajaxDirectorServlet = new AjaxDirectorServlet(webResource);

        final Reader inputString = new StringReader(testCase.getRequestJSON());

        final BufferedReader reader = new BufferedReader(inputString);
        when(request.getReader()).thenReturn(reader);
        when(request.getRequestURI()).thenReturn(testCase.getRequestURI());
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        try {
            testCase.getInitializer().initialize();

            ajaxDirectorServlet.service(request, response);

            testCase.getAssertion().executeAssertion();
        } finally {
            testCase.getDisposer().dispose();
        }
    }
}
