package com.dotmarketing.servlets.ajax;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.EnvironmentDataGen;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.PermissionAPI;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
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
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.ConfigUtils;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AjaxDirectorServletIntegrationTest {

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

    /**
     * Given: a bundle owned by one back-end user
     * When: a different back-end user, who is not a CMS Administrator, adds an asset to it through
     * the add-to-bundle action, selecting the bundle by id
     * Then: 403, and nothing is added to the bundle
     */
    @Test
    public void test_addToBundle_nonOwnerBundleId_returns403AndAddsNothing() throws Exception {
        final User owner = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final User otherUser = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final Bundle bundle = new Bundle("add-to-bundle-non-owner-" + System.currentTimeMillis(),
                null, null, owner.getUserId());
        APILocator.getBundleAPI().saveBundle(bundle);
        final String assetIdentifier = UUID.randomUUID().toString();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(otherUser);
        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class),
                anyBoolean(), any()))
                .thenReturn(dataObject);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\n"
                + "    \"assetIdentifier\": \"" + assetIdentifier + "\",\n"
                + "    \"bundleSelect\": \"" + bundle.getId() + "\"\n"
                + "}")));
        when(request.getRequestURI()).thenReturn(
                "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle");
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        try {
            new AjaxDirectorServlet(webResource).service(request, response);

            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
            assertTrue("Nothing should be added to the bundle", PublisherAPI.getInstance()
                    .getQueueElementsByBundleId(bundle.getId()).isEmpty());
        } finally {
            APILocator.getBundleAPI().deleteBundle(bundle.getId());
        }
    }

    /**
     * Given: a generated archive directly in the bundles directory under a plain id
     * When: a CMS Administrator asks the legacy download action for it, the way the audit-detail
     * page used to before it moved to /api/bundle/_download
     * Then: nothing is streamed, because the action no longer offers a download
     */
    @Test
    public void test_downloadBundle_legacyActionNoLongerStreams() throws Exception {
        final String bundleId = "download-legacy-" + System.currentTimeMillis();
        final File archive = new File(ConfigUtils.getBundlePath(), bundleId + ".tar.gz");
        Files.write(archive.toPath(), "archive".getBytes(StandardCharsets.UTF_8));
        final HttpServletResponse response = mock(HttpServletResponse.class);

        try {
            new AjaxDirectorServlet(downloadWebResource()).service(downloadRequest(
                    "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadBundle/bid/"
                            + bundleId, null), response);

            verify(response, never()).getOutputStream();
            verify(response, never()).setContentType("application/x-tgz");
        } finally {
            Files.deleteIfExists(archive.toPath());
        }
    }

    /**
     * A {@link WebResource} that resolves every request to the admin user, who holds the
     * publishing tool.
     */
    private static WebResource downloadWebResource() throws DotDataException {
        final User admin = TestUserUtils.getAdminUser();
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(admin);
        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class),
                anyBoolean(), any()))
                .thenReturn(dataObject);
        return webResource;
    }

    /**
     * A request for the given URI; when {@code bid} is set it is also sent as a request parameter.
     */
    private static HttpServletRequest downloadRequest(final String uri, final String bid)
            throws IOException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        if (bid == null) {
            when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        } else {
            when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("bid")));
            when(request.getParameter("bid")).thenReturn(bid);
        }
        return request;
    }

    /**
     * Given: a bundle owned by one back-end user, and an environment a second back-end user may use
     * When: the second user, who is not a CMS Administrator, pushes the bundle through the
     * push-bundle action
     * Then: 403, and no environment is saved on the bundle
     */
    @Test
    public void test_pushBundle_nonOwner_returns403AndSavesNothing() throws Exception {
        final User owner = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final User otherUser = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final Bundle bundle = new Bundle("push-bundle-non-owner-" + System.currentTimeMillis(),
                null, null, owner.getUserId());
        APILocator.getBundleAPI().saveBundle(bundle);
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        APILocator.getPermissionAPI().save(
                new Permission(environment.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(otherUser).getId(),
                        PermissionAPI.PERMISSION_USE, true),
                environment, APILocator.systemUser(), false);

        final HttpServletRequest request = pushBundleRequest(bundle.getId(), environment.getId());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        try {
            new AjaxDirectorServlet(webResourceFor(otherUser)).service(request, response);

            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
            assertTrue("No environment should be saved on the bundle", APILocator.getEnvironmentAPI()
                    .findEnvironmentsByBundleId(bundle.getId()).isEmpty());
        } finally {
            APILocator.getBundleAPI().deleteBundleAndDependencies(bundle.getId(), APILocator.systemUser());
            APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
            UserDataGen.remove(owner);
            UserDataGen.remove(otherUser);
        }
    }

    /**
     * Given: a bundle owned by a back-end user who is not a CMS Administrator, and an environment
     * that user may use
     * When: the owner pushes the bundle through the push-bundle action
     * Then: no error, and the environment is saved on the bundle
     */
    @Test
    public void test_pushBundle_owner_savesEnvironment() throws Exception {
        final User owner = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final Bundle bundle = new Bundle("push-bundle-owner-" + System.currentTimeMillis(),
                null, null, owner.getUserId());
        APILocator.getBundleAPI().saveBundle(bundle);
        final Environment environment = new EnvironmentDataGen().nextPersisted();
        APILocator.getPermissionAPI().save(
                new Permission(environment.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(owner).getId(),
                        PermissionAPI.PERMISSION_USE, true),
                environment, APILocator.systemUser(), false);

        final HttpServletRequest request = pushBundleRequest(bundle.getId(), environment.getId());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        try {
            new AjaxDirectorServlet(webResourceFor(owner)).service(request, response);

            verify(response, never()).sendError(anyInt());
            verify(response, never()).sendError(anyInt(), any());
            assertEquals(List.of(environment.getId()), APILocator.getEnvironmentAPI()
                    .findEnvironmentsByBundleId(bundle.getId()).stream()
                    .map(Environment::getId).collect(Collectors.toList()));
        } finally {
            APILocator.getBundleAPI().deleteBundleAndDependencies(bundle.getId(), APILocator.systemUser());
            APILocator.getEnvironmentAPI().deleteEnvironment(environment.getId());
            UserDataGen.remove(owner);
        }
    }

    /**
     * A {@link WebResource} that resolves every request to the given user.
     */
    private static WebResource webResourceFor(final User user) {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class),
                anyBoolean(), any()))
                .thenReturn(dataObject);
        return webResource;
    }

    /**
     * A push-bundle request for the given bundle and environment, publishing in 2030.
     */
    private static HttpServletRequest pushBundleRequest(final String bundleId,
            final String environmentId) throws IOException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\n"
                + "    \"assetIdentifier\": \"" + bundleId + "\",\n"
                + "    \"remotePublishDate\": \"2030-01-01\",\n"
                + "    \"remotePublishTime\": \"10-00\",\n"
                + "    \"iWantTo\": \"publish\",\n"
                + "    \"whoToSend\": \"" + environmentId + "\"\n"
                + "}")));
        when(request.getRequestURI()).thenReturn(
                "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle");
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        return request;
    }
}
