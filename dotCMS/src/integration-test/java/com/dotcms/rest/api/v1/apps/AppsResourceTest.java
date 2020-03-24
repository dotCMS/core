package com.dotcms.rest.api.v1.apps;

import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.security.apps.Param;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppsResourceTest extends IntegrationTestBase {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";

    private static AppsResource appsResource;

    private static final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            //.enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
            .findAndRegisterModules();

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);
        when(user.getLocale()).thenReturn(Locale.getDefault());
        when(user.isBackendUser()).thenReturn(true);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);

        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(dataObject);

        final AppsHelper appsHelper = new AppsHelper();

        appsResource = new AppsResource(webResource,
                appsHelper);
    }

    private InputStream createAppDescriptorFile(final String fileName, final String key,
            final String appName, final String description, final boolean allowExtraParameters,
            final Map<String, Param> paramMap) throws IOException {
        final AppDescriptor appDescriptor = new AppDescriptor(key, appName,
                description, "/black.png", allowExtraParameters, new HashMap<>());

        for (final Entry<String, Param> entry : paramMap.entrySet()) {
            final Param param = entry.getValue();
            appDescriptor
                    .addParam(entry.getKey(), param.getValue(), param.isHidden(), param.getType(),
                            param.getLabel(), param.getHint(), param.isRequired());
        }

        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        ymlMapper.writeValue(file, appDescriptor);
        //System.out.println(file);
        return Files.newInputStream(Paths.get(file.getPath()));
    }

    private FormDataMultiPart createFormDataMultiPart(final String fileName,
            final InputStream inputStream) {
        final FormDataBodyPart filePart1 = mock(FormDataBodyPart.class);
        when(filePart1.getEntityAs(any(Class.class))).thenReturn(inputStream);
        final ContentDisposition contentDisposition = mock(ContentDisposition.class);
        when(contentDisposition.getFileName()).thenReturn(fileName);
        when(filePart1.getContentDisposition()).thenReturn(contentDisposition);
        final FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
        when(formDataMultiPart.getFields("file")).thenReturn(Collections.singletonList(filePart1));
        return formDataMultiPart;
    }

    @Test
    public void Test_Create_app_descriptor_Then_Create_App_Integration_Then_Delete_The_Whole_App() {

        final Host host = new SiteDataGen().nextPersisted();
        final Map<String, Param> paramMap = ImmutableMap.of(
                "p1", Param.newParam("v1", false, Type.STRING, "label", "hint", true),
                "p2", Param.newParam("v2", false, Type.STRING, "label", "hint", true),
                "p3", Param.newParam("v3", false, Type.STRING, "label", "hint", true)
        );
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", appKey);
        try(final InputStream inputStream = createAppDescriptorFile(fileName, appKey, "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {
            final Response appIntegrationResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));

            Assert.assertNotNull(appIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appIntegrationResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> "lola"
                                    .equals(appView.getName())));

            final SecretForm secretForm = new SecretForm(appKey,host.getIdentifier(), paramMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response hostIntegrationsResponse = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView2 = (ResponseEntityView) hostIntegrationsResponse
                    .getEntity();
            final AppView appWithSites = (AppView) responseEntityView2
                    .getEntity();
            Assert.assertEquals(1, appWithSites.getConfigurationsCount());
            Assert.assertEquals("lola", appWithSites.getName());
            final List<SiteView> sites = appWithSites.getSites();
            Assert.assertNotNull(sites);
            Assert.assertTrue(sites.size() >= 2);
            Assert.assertEquals(sites.get(0).getId(), Host.SYSTEM_HOST); //system host is always the first element to come.
            Assert.assertEquals(sites.get(1).getId(), host.getIdentifier());
            Assert.assertTrue(
                    sites.stream()
                            .anyMatch(hostView -> host.getIdentifier().equals(hostView.getId()))
            );

            final Response detailedIntegrationResponse = appsResource
                    .getAppDetail(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                    .getEntity();
            final AppView appDetailedView = (AppView) responseEntityView3
                    .getEntity();
            Assert.assertNotNull(appDetailedView.getSites());
            Assert.assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());

            final Response deleteIntegrationsResponse = appsResource
                    .deleteAllAppSecrets(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, deleteIntegrationsResponse.getStatus());

            final Response detailedIntegrationResponseAfterDelete = appsResource
                    .getAppDetail(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND,
                    detailedIntegrationResponseAfterDelete.getStatus());

            //Now test the entry has been removed from the list of available configurations.
            final Response hostIntegrationsResponseAfterDelete = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponseAfterDelete.getStatus());
            final ResponseEntityView responseEntityViewAfterDelete = (ResponseEntityView) hostIntegrationsResponseAfterDelete
                    .getEntity();
            final AppView appHostViewAfterDelete = (AppView) responseEntityViewAfterDelete
                    .getEntity();

            Assert.assertEquals(0, appHostViewAfterDelete.getConfigurationsCount());
            Assert.assertEquals("lola", appHostViewAfterDelete.getName());
            final List<SiteView> expectedEmptyHosts = appHostViewAfterDelete.getSites();
            Assert.assertNotNull(expectedEmptyHosts);
            // Previously this test wasn't expecting any entry here
            // But the pagination will now return only items marked to have no configurations.
            Assert.assertEquals("None of the returned item should have configuration", 0,
                    expectedEmptyHosts.stream().filter(SiteView::isConfigured).count());
        }catch (Exception e){
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_Create_App_descriptor_Then_Create_App_Integration_Then_Delete_One_Single_Secret() {

        final Map<String, Param> paramMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("val-2", false, Type.STRING, "label", "hint", true),
                "param3", Param.newParam("val-3", false, Type.STRING, "label", "hint", true)
        );
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String appKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", appKey);
        try(final InputStream inputStream = createAppDescriptorFile(fileName, appKey, "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {

            // Create App integration Descriptor
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> "lola"
                                    .equals(appView.getName())));

            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(appKey,host.getIdentifier(), paramMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            //fetch and verify the secrets by app-key
            final Response appByKey = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, appByKey.getStatus());
            final ResponseEntityView responseEntityView2 = (ResponseEntityView) appByKey
                    .getEntity();
            final AppView appByKeyView = (AppView) responseEntityView2
                    .getEntity();

            Assert.assertEquals(1, appByKeyView.getConfigurationsCount());
            Assert.assertEquals("lola", appByKeyView.getName());
            final List<SiteView> hosts = appByKeyView.getSites();
            Assert.assertNotNull(hosts);
            Assert.assertTrue(hosts.size() >= 2);
            Assert.assertEquals(hosts.get(0).getId(), Host.SYSTEM_HOST); //system host is always the first element to come.
            Assert.assertEquals(hosts.get(1).getId(), host.getIdentifier());
            Assert.assertTrue(
                    hosts.stream()
                            .anyMatch(hostView -> host.getIdentifier().equals(hostView.getId()))
            );

            final Response detailedIntegrationResponse = appsResource
                    .getAppDetail(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                    .getEntity();
            final AppView appDetailedView = (AppView) responseEntityView3
                    .getEntity();

            Assert.assertNotNull(appDetailedView.getSites());
            Assert.assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());

            //Delete individual secrets
            final Set<String> paramsToDelete = ImmutableSet.of(
                    "param1",
                    "param3"
            );

            final DeleteSecretForm deleteSecretForm = new DeleteSecretForm(appKey,host.getIdentifier(),paramsToDelete);
            final Response deleteIndividualSecretResponse = appsResource
                    .deleteIndividualAppSecret(request, response, deleteSecretForm);
            Assert.assertEquals(HttpStatus.SC_OK, deleteIndividualSecretResponse.getStatus());

            //The app integration should still be there but the individual params/secrets should be gone
            final Response detailedIntegrationResponseAfterDelete = appsResource
                    .getAppDetail(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK,
                    detailedIntegrationResponseAfterDelete.getStatus());

            final ResponseEntityView responseAfterDeleteEntityView = (ResponseEntityView) detailedIntegrationResponseAfterDelete
                    .getEntity();
            final AppView appViewAfterDelete = (AppView) responseAfterDeleteEntityView
                    .getEntity();

            Assert.assertNotNull(appViewAfterDelete.getSites());
            Assert.assertFalse(appViewAfterDelete.getSites().isEmpty());
            final Map<String, Secret> secretsAfterDelete = appViewAfterDelete
                    .getSites()
                    .get(0).getSecrets();
            Assert.assertTrue(secretsAfterDelete.containsKey("param2"));
            //The ones we removed must not be present.. right?
            Assert.assertFalse(secretsAfterDelete.containsKey("param1"));
            Assert.assertFalse(secretsAfterDelete.containsKey("param3"));
        }catch (Exception e){
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_Create_App_Descriptor_Then_Create_App_Integration_Then_Delete_App_Descriptor() {

        final Map<String, Param> paramMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("val-2", false, Type.STRING, "label", "hint", true),
                "param3", Param.newParam("val-3", false, Type.STRING, "label", "hint", true)
        );

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", appKey);
        try (final InputStream inputStream = createAppDescriptorFile(fileName, appKey,
                appKey,
                "This should go away.", false, paramMap)) {
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final List<String> sites = new ArrayList<>();
            final int max = 10;
            for (int i = 1; i <= max; i++) {
                final Host host = createAppIntegrationParams(paramMap, appKey, request,
                        response);
                sites.add(host.getIdentifier());
            }

            //The App does exist and so it does the secrets.
            final Response hostIntegrationsResponse = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView = (ResponseEntityView) hostIntegrationsResponse
                    .getEntity();
            final AppView appIntegrationView = (AppView) responseEntityView
                    .getEntity();
            Assert.assertEquals(max, appIntegrationView.getConfigurationsCount());

            //Now lets get rid of the app Descriptor and verify.
            final Response deleteAppResponse = appsResource
                    .deleteApp(request, response, appKey, true);
            Assert.assertEquals(HttpStatus.SC_OK, deleteAppResponse.getStatus());

            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            //App is gone.
            Assert.assertTrue(
                    integrationViewList.stream()
                            .noneMatch(view -> appKey.equals(view.getName())));
            final Response responseAfterDelete = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseAfterDelete.getStatus());

            for (final String siteId : sites) {
                final Response responseNotFound = appsResource
                        .getAppDetail(request, response, appKey, siteId);
                Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseNotFound.getStatus());
            }
        } catch (Exception e) {
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }

    @Test
    public void Test_Protected_Hidden_Secret() {

        final Map<String, Param> initialParamsMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("true", false, Type.BOOL, "label", "hint", true),
                "param3", Param.newParam("val-2", true, Type.STRING, "label", "hint", true),
                "param4", Param.newParam("true", true, Type.BOOL, "label", "hint", true)
        );

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", appKey);
        try(final InputStream inputStream = createAppDescriptorFile(fileName, appKey,
                appKey,
                "Test-hidden-secret-protection", false, initialParamsMap)) {

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            final List<String> sites = new ArrayList<>();

            final int max = 10;
            for (int i = 1; i <= max; i++) {
                sites.add(
                        createAppIntegrationParams(initialParamsMap, appKey, request,
                                response)
                                .getIdentifier()
                );
            }

            //The App does exist and so it does the secrets.
            final Response siteIntegrationsResponse = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, siteIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView = (ResponseEntityView) siteIntegrationsResponse
                    .getEntity();
            final AppView appView = (AppView) responseEntityView
                    .getEntity();

            Assert.assertEquals(max, appView.getConfigurationsCount());

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = appsResource
                        .getAppDetail(request, response, appKey, siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
                final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                        .getEntity();
                final AppView appDetailedView = (AppView) responseEntityView3
                        .getEntity();
                Assert.assertNotNull(appDetailedView.getSites());
                Assert.assertFalse(appDetailedView.getSites().isEmpty());
                final Map<String, Secret> secrets = appDetailedView.getSites().get(0)
                        .getSecrets();
                for (Entry<String, Secret> secretEntry : secrets.entrySet()) {
                    final String key = secretEntry.getKey();
                    final Secret secret = secretEntry.getValue();
                    final Param originalParam = initialParamsMap.get(key);
                    if (secret.isHidden()) {
                        Assert.assertEquals(AppsHelper.PROTECTED_HIDDEN_SECRET,
                                new String(secret.getValue()));
                    } else {
                        Assert.assertEquals(originalParam.getString(),
                                new String(secret.getValue()));
                    }
                }
            }
        }catch (Exception e){
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_App_Key_Casing() throws IOException, DotDataException, DotSecurityException {

        final Map<String, Param> initialParamsMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        long time = System.currentTimeMillis();

        final String appKey1 = String.format("all_lower_case_not_too_short_prefix_%d", time);
        final String fileName = String.format("%s.yml", appKey1);
        try(final InputStream inputStream = createAppDescriptorFile(fileName, appKey1,
                appKey1,
                "Test-service-casing", false, initialParamsMap)) {

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            final String appKeyCasingVariant1 = upperCaseRandom(appKey1, 30);

            final Response appByKey = appsResource
                    .getAppByKey(request, response, appKeyCasingVariant1, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, appByKey.getStatus());

            final List<String> sites = new ArrayList<>();
            final int max = 5;
            for (int i = 1; i <= max; i++) {
                sites.add(
                        createAppIntegrationParams(initialParamsMap, appKey1, request,
                                response)
                                .getIdentifier()
                );
            }

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = appsResource
                        .getAppDetail(request, response,
                                upperCaseRandom(appKey1, 30), siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            }
        }catch (Exception e){
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }

    @Test
    public void Test_Required_Params() throws IOException, DotDataException, DotSecurityException {

        final Map<String, Param> initialParamsMap = ImmutableMap.of(
             "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true)
        );

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        long time = System.currentTimeMillis();

        final String key = String.format("all_lower_case_not_too_short_prefix_%d", time);
        final String fileName = String.format("%s.yml", key);

        //We're indicating that extra params are allowed to test required params are still required
        try(final InputStream inputStream = createAppDescriptorFile(fileName, key,
                key,
                "Test-required-params",
                true, initialParamsMap)){

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Here's a secret with an empty param that is marked as required.
            final Map<String, Param> secretParam = ImmutableMap.of(
                    "param1", Param.newParam("", false, Type.STRING, null, null, true)
            );

            final Host host = new SiteDataGen().nextPersisted();
            final SecretForm secretForm = new SecretForm(key, host.getIdentifier(), secretParam);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse.getStatus());
        }catch (Exception e){
            Logger.error(AppsResourceTest.class, e);
            fail();
        }
    }

    @Test
    public void Test_Pagination_And_Sort_Then_Request_Filter_Expect_Empty_Results() {

        final Map<String, Param> paramMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("val-2", false, Type.STRING, "label", "hint", true),
                "param3", Param.newParam("val-3", false, Type.STRING, "label", "hint", true)
        );
        final long timeMark = System.currentTimeMillis();
        final List<Host> hosts= new ArrayList<>();
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        for(final char chr :alphabet) {
           hosts.add(new SiteDataGen().name( String.format("%s,%d",chr, timeMark )).nextPersisted());
        }
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String key = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", key);
        try (final InputStream inputStream = createAppDescriptorFile(fileName, key,
                "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {

            // Create App integration Descriptor
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> "lola"
                                    .equals(appView.getName())));

            // Add secrets to it.
            for(final Host host:hosts) {
                createSecret(request, response, key, host, paramMap);
            }

            //fetch and test pagination
            final int pageSize = 4;
            final int numberOfPages = hosts.size() / pageSize;

            for(int currentPage = 1; currentPage <= numberOfPages; currentPage++) {

                final Response paginationResponse1 = appsResource
                        .getAppByKey(request, response, key,
                                new PaginationContext(null, currentPage, pageSize, "", ""));
                Assert.assertEquals(HttpStatus.SC_OK, paginationResponse1.getStatus());
                final ResponseEntityView paginationEntity1 = (ResponseEntityView) paginationResponse1
                        .getEntity();
                final AppView paginationView1 = (AppView) paginationEntity1
                        .getEntity();
                Assert.assertEquals(hosts.size(), paginationView1.getConfigurationsCount());
                final List<SiteView> hostsBatch1 = paginationView1.getSites();
                Assert.assertNotNull(hostsBatch1);

                if(currentPage < numberOfPages) {
                    Assert.assertEquals(pageSize, hostsBatch1.size());
                } else {
                    Assert.assertTrue(pageSize <= hostsBatch1.size());
                }

                List<SiteView> itemsPage;
                if(currentPage == 1){
                    assertEquals(Host.SYSTEM_HOST, hostsBatch1.get(0).getId());
                    itemsPage = hostsBatch1.subList(1,hostsBatch1.size());
                } else {
                    itemsPage = hostsBatch1;
                }

                final List<String> pageNames = itemsPage.stream().map(SiteView::getName).collect(Collectors.toList());
                Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNames));
            }

            final Response paginationFilterResponse = appsResource
                    .getAppByKey(request, response, key,
                            new PaginationContext("lol", 0, numberOfPages, "", ""));
            Assert.assertEquals(HttpStatus.SC_OK, paginationFilterResponse.getStatus());
            final ResponseEntityView paginationFilterEntity = (ResponseEntityView) paginationFilterResponse.getEntity();
            final AppView paginationFilterView = (AppView) paginationFilterEntity.getEntity();
            assertTrue(paginationFilterView.getSites().isEmpty());

        } catch (Exception e) {
            Logger.error(AppsResourceTest.class, e);
            fail();
        }

    }

    private void createSecret(final HttpServletRequest request, final HttpServletResponse response,
            final String appKey, final Host host, final Map<String, Param> paramMap){
        final SecretForm secretForm = new SecretForm(appKey, host.getIdentifier(), paramMap);
        final Response createSecretResponse = appsResource
                .createAppSecrets(request, response, secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
    }

    private Host createAppIntegrationParams(final Map<String, Param> paramMap,
            final String key, final HttpServletRequest request,
            final HttpServletResponse response) {
        final Host host = new SiteDataGen().nextPersisted();
        final SecretForm secretForm = new SecretForm(key, host.getIdentifier(), paramMap);
        final Response createSecretResponse = appsResource
                .createAppSecrets(request, response, secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
        return host;
    }

    private PaginationContext paginationContext(){
        //OrderBy and direction are ignored by design.
       return new PaginationContext(null, 1, 100, "", "");
    }

}
