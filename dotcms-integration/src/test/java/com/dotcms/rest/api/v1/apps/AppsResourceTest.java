package com.dotcms.rest.api.v1.apps;

import static com.dotcms.rest.api.v1.apps.Input.newInputParam;
import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.AppDescriptorDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.apps.view.SecretView;
import com.dotcms.rest.api.v1.apps.view.SecretView.SecretViewSerializer;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.rest.api.v1.apps.view.ViewUtil;
import com.dotcms.security.apps.AppDescriptorHelper;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.SecretsStore;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.liferay.util.StringPool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppsResourceTest extends IntegrationTestBase {

    private static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    private static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    private static final String ADMIN_NAME = "User Admin";

    private static AppsResource appsResource;

    private static AppsResource appsResourceNonLicense;

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
        when(user.isAdmin()).thenReturn(true);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);

        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(dataObject);

        final AppsHelper appsHelper = new AppsHelper();

        appsResource = new AppsResource(webResource,
                appsHelper);

        final AppsAPI appsAPI = new AppsAPIImpl(APILocator.getLayoutAPI(),
                APILocator.getHostAPI(),
                SecretsStore.INSTANCE.get(), CacheLocator.getAppsCache(),
                APILocator.getLocalSystemEventsAPI(),
                new AppDescriptorHelper(),
                new LicenseValiditySupplier() {
                    @Override
                    public boolean hasValidLicense() {
                        return false;
                    }
                }
        );

        final AppsHelper appsHelperNonLicense = new AppsHelper(appsAPI, APILocator.getHostAPI(), APILocator.getPermissionAPI(), APILocator.getSecurityLogger());
        appsResourceNonLicense = new AppsResource(webResource,
                appsHelperNonLicense);

    }

    /**
     * mock a multi-part with a file
     * @param fileName
     * @param inputStream
     * @return
     */
    private FormDataMultiPart createFormDataMultiPart(final String fileName, final InputStream inputStream) {
       return createFormDataMultiPart(fileName, inputStream, null);
    }

    /**
     * mock a multipart with file and json
     * @param fileName
     * @param inputStream
     * @param json
     * @return
     */
    private FormDataMultiPart createFormDataMultiPart(final String fileName, final InputStream inputStream, final String json) {
        final FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
        final FormDataBodyPart filePart1 = mock(FormDataBodyPart.class);
        when(filePart1.getEntityAs(any(Class.class))).thenReturn(inputStream);
        final ContentDisposition contentDisposition1 = mock(ContentDisposition.class);
        when(contentDisposition1.getFileName()).thenReturn(fileName);
        when(filePart1.getContentDisposition()).thenReturn(contentDisposition1);
        when(formDataMultiPart.getFields("file")).thenReturn(List.of(filePart1));
        if(null != json) {
            final FormDataBodyPart filePart2 = mock(FormDataBodyPart.class);
            when(filePart2.getEntityAs(any(Class.class))).thenReturn(IOUtils.toInputStream(json));
            final ContentDisposition contentDisposition2 = mock(ContentDisposition.class);
            when(contentDisposition2.getParameters()).thenReturn(ImmutableMap.of("name", "json"));
            when(filePart2.getContentDisposition()).thenReturn(contentDisposition2);
            when(formDataMultiPart.getBodyParts()).thenReturn(List.of(filePart2));
        }
        return formDataMultiPart;
    }

    /**
     * This method tests quite a few things on the resource. First we create a yml file that exist physically on disc.
     * Then we upload the file with an app definition. Then we Create an App Then list the available apps.
     * Then we Verify the New app is listed. under the right Host.
     * Then We delete the app and verify the pagination results make sense.
     * Given scenario: Test we can create an app then delete it.
     * Expected Result: Pagination shows all available sites with no configurations
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Create_app_descriptor_Then_Create_App_Integration_Then_Delete_The_Whole_App()
            throws IOException {

        final Host host = new SiteDataGen().nextPersisted();

        // Ensure the host cache is properly warmed with the newly created host
        // This prevents race conditions where the cache might not have the host yet
        try {
            CacheLocator.getHostCache().clearCache();
            APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), com.dotmarketing.portlets.contentlet.business.HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
        } catch (Exception e) {
            Logger.error(AppsResourceTest.class, "Failed to warm host cache", e);
        }

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .stringParam("p3", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(false);

        final Map<String, Input> inputParamMap = ImmutableMap.of(
                "p1", newInputParam("v1".toCharArray(),false),
                "p2", newInputParam("v1".toCharArray(),false),
                "p3", newInputParam("v1".toCharArray(),false)
        );
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey =  dataGen.getKey();
        final String fileName =  dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {
            final Response appIntegrationResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));

            Assert.assertNotNull(appIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appIntegrationResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response, null);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> dataGen.getName()
                                    .equals(appView.getName())));

            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response hostIntegrationsResponse = appsResource
                    .getAppByKey(request, response, appKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView2 = (ResponseEntityView) hostIntegrationsResponse
                    .getEntity();
            final AppView appWithSites = (AppView) responseEntityView2
                    .getEntity();
            Assert.assertEquals(1, appWithSites.getConfigurationsCount());
            Assert.assertEquals(dataGen.getName(), appWithSites.getName());
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
            assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());

            final Response deleteIntegrationsResponse = appsResource
                    .deleteAllAppSecrets(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, deleteIntegrationsResponse.getStatus());

            final Response detailedIntegrationResponseAfterDelete = appsResource
                    .getAppDetail(request, response, appKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK,
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
            Assert.assertEquals(dataGen.getName(), appHostViewAfterDelete.getName());
            final List<SiteView> expectedEmptyHosts = appHostViewAfterDelete.getSites();
            Assert.assertNotNull(expectedEmptyHosts);
            // Previously this test wasn't expecting any entry here
            // But the pagination will now return only items marked to have no configurations.
            Assert.assertEquals("None of the returned item should have configuration", 0,
                    expectedEmptyHosts.stream().filter(SiteView::isConfigured).count());
        }
    }

    /**
     * In this method we create a yml file that exist physically on disc.
     * Then we upload the file with an app definition. Then we Create an App Then list the available apps.
     * Then we verify for app input parameters overriding through the use of environment variables (check dotcms-integration/pom.xml file for used env-vars definitions).
     * Given scenario: Test we can create an app with overridden app parameters through environment variables.
     * Expected Result: app params values para overridden with what is present in the env-vars
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Create_app_descriptor_overriding_with_envvars_Then_Create_App_Integration_Then_Delete_The_Whole_App()
            throws IOException {

        final Host host = new SiteDataGen().nextPersisted();
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .param(
                        "p1",
                        ParamDescriptor.builder()
                                .withValue(StringPool.BLANK)
                                .withHidden(false)
                                .withType(Type.STRING)
                                .withRequired(true)
                                .withLabel("label")
                                .withHint("hint")
                                .withEnvVar("ENV_VAR_1")
                                .withEnvShow(true)
                                .build())
                .param(
                        "p2",
                        ParamDescriptor.builder()
                                .withValue(StringPool.BLANK)
                                .withHidden(false)
                                .withType(Type.STRING)
                                .withRequired(true)
                                .withLabel("label")
                                .withHint("hint")
                                .withEnvVar("ENV_VAR_2")
                                .withEnvShow(false)
                                .build())
                .stringParam("p3", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(false);

        final Map<String, Input> inputParamMap = ImmutableMap.of(
                "p1", newInputParam("v1".toCharArray(),false),
                "p2", newInputParam("v1".toCharArray(),false),
                "p3", newInputParam("v1".toCharArray(),false)
        );
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey =  dataGen.getKey();
        final String fileName =  dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {
            final Response appIntegrationResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));

            Assert.assertNotNull(appIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appIntegrationResponse.getStatus());

            final SecretForm secretForm = new SecretForm(inputParamMap);
            appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);

            final Response detailedIntegrationResponse = appsResource
                    .getAppDetail(
                            request,
                            response,
                            appKey,
                            host.getIdentifier());
            final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse.getEntity();
            final AppView appDetailedView = (AppView) responseEntityView3.getEntity();

            final SiteView siteView = appDetailedView.getSites().get(0);
            int i = 0;
            SecretView secretView = siteView.getSecrets().get(i);
            Assert.assertTrue(secretView.getParamDescriptor().hasEnvVar());
            Assert.assertTrue(secretView.getSecret().hasEnvVar());
            Assert.assertEquals("ENV_VAR_" + (i + 1), secretView.getParamDescriptor().getEnvVar());
            Assert.assertEquals("ENV_VAR_" + (i + 1), secretView.getSecret().getEnvVar());
            Assert.assertTrue(secretView.getParamDescriptor().isEnvShow());
            Assert.assertTrue(secretView.getSecret().isEnvShow());

            secretView = siteView.getSecrets().get(++i);
            Assert.assertTrue(secretView.getParamDescriptor().hasEnvVar());
            Assert.assertTrue(secretView.getSecret().hasEnvVar());
            Assert.assertEquals("ENV_VAR_" + (i + 1), secretView.getParamDescriptor().getEnvVar());
            Assert.assertEquals("ENV_VAR_" + (i + 1), secretView.getSecret().getEnvVar());
            Assert.assertFalse(secretView.getParamDescriptor().isEnvShow());
            Assert.assertFalse(secretView.getSecret().isEnvShow());

            secretView = siteView.getSecrets().get(++i);
            Assert.assertFalse(secretView.getParamDescriptor().hasEnvVar());
            Assert.assertFalse(secretView.getSecret().hasEnvVar());
            Assert.assertNull(secretView.getParamDescriptor().getEnvVar());
            Assert.assertNull(secretView.getSecret().getEnvVar());
            Assert.assertTrue(secretView.getParamDescriptor().isEnvShow());
            Assert.assertTrue(secretView.getSecret().isEnvShow());

            appsResource.deleteAllAppSecrets(request, response, appKey, host.getIdentifier());
        }
    }

    /**
     * This method tests quite a few things on the resource. First we create a yml file that exist physically on disc.
     * The we upload the file with an app definition. Then we Create an App Then list the available apps.
     * Then we Verify the New app is listed. under the right Host.
     * Then We delete one single entry of the secret.
     * Given scenario: Test we can create an app then delete individual properties/secrets from it. Not the whole secret
     * Expected Result: Pagination shows all available sites with no configurations and the default values from the yml are loaded instead.
     */
    @Test
    public void Test_Create_App_descriptor_Then_Create_App_Integration_Then_Delete_One_Single_Secret()
            throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .stringParam("param2", false,  true)
                .stringParam("param3", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final Host host = new SiteDataGen().nextPersisted();

        // Ensure the host cache is properly warmed with the newly created host
        // This prevents race conditions where the cache might not have the host yet
        try {
            CacheLocator.getHostCache().clearCache();
            APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), com.dotmarketing.portlets.contentlet.business.HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
        } catch (Exception e) {
            Logger.error(AppsResourceTest.class, "Failed to warm host cache", e);
        }

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try( InputStream inputStream = Files.newInputStream(file.toPath())) {

            // Create App integration Descriptor
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response, null);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> dataGen.getName()
                                    .equals(appView.getName())));

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("v1".toCharArray(),false),
                    "param2", newInputParam("v1".toCharArray(),false),
                    "param3", newInputParam("v1".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
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
            Assert.assertEquals(dataGen.getName(), appByKeyView.getName());
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
            assertFalse(appDetailedView.getSites().isEmpty());
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
            assertFalse(appViewAfterDelete.getSites().isEmpty());
            final Map<String, SecretView> secretsAfterDelete = appViewAfterDelete
                    .getSites()
                    .get(0).getSecrets().stream().collect(Collectors.toMap(SecretView::getName,
                    Function.identity()));
            Assert.assertEquals(secretsAfterDelete.get("param2").getSecret().getString(),"v1");
            //The deleted secrets should have returned to show their default vales defined in the yml descriptor.
            assertNull(secretsAfterDelete.get("param1").getSecret());
            assertNull(secretsAfterDelete.get("param3").getSecret());

        }
    }

    /**
     * Test we save a secret that is described as 1 public value and 2 hidden ones
     * Given scenario: We create a secret then we update the non-hidden (public) values and send a string of asters like the UI would
     * Expected Result: Only the public value  was supposed to be updated. The other 2 hidden fields should remain the same since we sent a bunch of *****
     * @throws IOException
     */
    @Test
    public void Test_Create_App_descriptor_Then_Create_App_Integration_Then_Update_Secrets()
            throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .stringParam("param2", true,  true)
                .stringParam("param3", true,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {

            // Create App integration Descriptor
            final Response appResponse = appsResource.createApp(request, response, createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("public-value1".toCharArray(),false),
                    "param2", newInputParam("hidden-value1".toCharArray(),false),
                    "param3", newInputParam("hidden-value2".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response detailedIntegrationResponse = appsResource.getAppDetail(request, response, appKey, host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityViewSave = (ResponseEntityView) detailedIntegrationResponse.getEntity();
            final AppView appDetailedView = (AppView) responseEntityViewSave.getEntity();

            Assert.assertNotNull(appDetailedView.getSites());
            assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(), host.getIdentifier());
            //We can the secrets here because this happens before the a serializer will exchange the values by asters making them look like `*****`
            final List<SecretView> secretViews = appDetailedView.getSites().get(0).getSecrets();
            final SecretView param1 = secretViews.get(0);
            Assert.assertEquals(new String(param1.getSecret().getValue()),"public-value1");
            final SecretView param2 = secretViews.get(1);
            Assert.assertEquals(new String(param2.getSecret().getValue()),"hidden-value1");
            final SecretView param3 = secretViews.get(2);
            Assert.assertEquals(new String(param3.getSecret().getValue()),"hidden-value2");

            final Map<String, Input> inputParamMapFromUI = ImmutableMap.of(
                    "param1", newInputParam("new-public-value".toCharArray(),false),
                    "param2", newInputParam("*******".toCharArray(),false),
                    "param3", newInputParam("****".toCharArray(),false));

            final SecretForm secretFormUpdate = new SecretForm(inputParamMapFromUI);
            final Response createSecretResponseAfterUpdate = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretFormUpdate);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponseAfterUpdate.getStatus());

            final Response detailedIntegrationResponseAfterUpdate = appsResource.getAppDetail(request, response, appKey, host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponseAfterUpdate.getStatus());
            final ResponseEntityView responseEntityViewUpdate = (ResponseEntityView) detailedIntegrationResponseAfterUpdate.getEntity();
            final AppView appDetailedViewAfterUpdate = (AppView) responseEntityViewUpdate.getEntity();

            Assert.assertNotNull(appDetailedViewAfterUpdate.getSites());
            assertFalse(appDetailedViewAfterUpdate.getSites().isEmpty());
            Assert.assertEquals(appDetailedViewAfterUpdate.getSites().get(0).getId(), host.getIdentifier());

            final List<SecretView> secretViewsAfterUpdate = appDetailedViewAfterUpdate.getSites().get(0).getSecrets();
            final SecretView param1AfterUpdate = secretViewsAfterUpdate.get(0);
            Assert.assertEquals(new String(param1AfterUpdate.getSecret().getValue()),"new-public-value");
            final SecretView param2AfterUpdate = secretViewsAfterUpdate.get(1);
            Assert.assertEquals(new String(param2AfterUpdate.getSecret().getValue()),"hidden-value1");
            final SecretView param3AfterUpdate = secretViewsAfterUpdate.get(2);
            Assert.assertEquals(new String(param3AfterUpdate.getSecret().getValue()),"hidden-value2");

        }
    }

    /**
     * This test ensures that secrets from archived sites are not shown.
     * Given scenario: We create a configuration for a site then later we archive the site
     * Expected Result: The site or any secrets it might have must not be included
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_Create_App_descriptor_Then_Create_App_Integration_Then_Archive_Site_Expect_Secrets_No_Longer_Available()
            throws IOException, DotSecurityException, DotDataException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {

            // Create App integration Descriptor
            final Response appResponse = appsResource.createApp(request, response, createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            final Map<String, Input> inputParamMap = ImmutableMap.of("param1", newInputParam("public-value1".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response detailedIntegrationResponse = appsResource.getAppDetail(request, response, appKey, host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityViewSave = (ResponseEntityView) detailedIntegrationResponse.getEntity();
            final AppView appDetailedView = (AppView) responseEntityViewSave.getEntity();

            Assert.assertNotNull(appDetailedView.getSites());
            assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(), host.getIdentifier());
            final List<String> collected = appDetailedView.getSites().stream().map(SiteView::getId)
                    .collect(Collectors.toList());
            Assert.assertTrue(collected.contains(host.getIdentifier()));
        }
        //Now lets archive the site and test secrets are gone from the results.
        APILocator.getHostAPI().archive(host, APILocator.systemUser(), false);

        final Response detailResponse = appsResource.getAppDetail(request, response, appKey, host.getIdentifier());
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, detailResponse.getStatus());

        final Response appByKeyResponse = appsResource
                .getAppByKey(request, response, appKey, paginationContext());

        Assert.assertEquals(HttpStatus.SC_OK, appByKeyResponse.getStatus());
        final ResponseEntityView responseEntityView = (ResponseEntityView) appByKeyResponse
                .getEntity();
        final AppView appIntegrationView = (AppView) responseEntityView
                .getEntity();

        Assert.assertNotNull(appIntegrationView.getSites());
        assertFalse(appIntegrationView.getSites().isEmpty());
        final Set<String> siteIds = appIntegrationView.getSites().stream().map(SiteView::getId).collect(Collectors.toSet());
        assertFalse(siteIds.contains(host.getIdentifier()));
    }


    /**
     * Test we can delete an app together with all it definition and associated secrets.
     * Given scenario: Test we can create an app descriptor and an app then delete the descriptor
     * Expected Result: App and secrets are gone (404).
     */
    @Test
    public void Test_Create_App_Descriptor_Then_Create_App_Integration_Then_Delete_App_Descriptor()
            throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .stringParam("param2", false,  true)
                .stringParam("param3", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try( InputStream inputStream = Files.newInputStream(file.toPath())) {
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final List<String> sites = new ArrayList<>();
            final int max = 10;
            for (int i = 1; i <= max; i++) {

                //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
                final Map<String, Input> inputParamMap = ImmutableMap.of(
                        "param1", newInputParam("val-1".toCharArray()),
                        "param2", newInputParam("val-2".toCharArray()),
                        "param3", newInputParam("val-3".toCharArray()));

                final Host host = createAppSecret(inputParamMap, appKey, request, response);
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
                    .listAvailableApps(request, response, null);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> appViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            if(!appViewList.isEmpty()) { //it is possible to get an empty list here.
                //App is gone.
                Assert.assertTrue(
                        appViewList.stream()
                                .noneMatch(view -> appKey.equals(view.getName())));
                final Response responseAfterDelete = appsResource
                        .getAppByKey(request, response, appKey, paginationContext());
                Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseAfterDelete.getStatus());
            }
            for (final String siteId : sites) {
                final Response responseNotFound = appsResource
                        .getAppDetail(request, response, appKey, siteId);
                Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseNotFound.getStatus());
            }
        }
    }

    /**
     * Test hidden secrets come back protected.
     * Given scenario: We have a descriptor with hidden and non-hidden stuff
     * Expected Result: Whatever was marked as hidden in the app-descriptor must come back protected Also we expect the response to have the params in natural order.
     */
    @Test
    public void Test_Secret_Serializer_Returned_Values_Match_Descriptor_Verify_Hidden_Secrets_Are_Protected_Verify_Params_Are_Ordered() throws Exception{

        final List<String> orderedParamNames = ImmutableList.of("param1","param2","param3","param4");

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam(orderedParamNames.get(0), true,  true)
                .boolParam(orderedParamNames.get(1), false,  true)
                .boolParam(orderedParamNames.get(2), false,  true)
                .stringParam(orderedParamNames.get(3), true,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try( InputStream inputStream = Files.newInputStream(file.toPath())) {

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            final List<String> sites = new ArrayList<>();

            final int max = 5;
            for (int i = 1; i <= max; i++) {

                //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
                //Also note we're sending hidden value as true. Trying to override the value on the descriptor.
                final Map<String, Input> inputParamMap = ImmutableMap.of(
                        orderedParamNames.get(0), newInputParam("hidden".toCharArray()),
                        orderedParamNames.get(1), newInputParam("true".toCharArray()),
                        orderedParamNames.get(2), newInputParam("true".toCharArray()),
                        orderedParamNames.get(3), newInputParam("non-hidden".toCharArray())
                );

                sites.add(
                   createAppSecret(inputParamMap, appKey, request, response).getIdentifier()
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

            final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
                    .getDefaultObjectMapper();

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = appsResource
                        .getAppDetail(request, response, appKey, siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
                final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                        .getEntity();
                final AppView appDetailedView = (AppView) responseEntityView3
                        .getEntity();
                Assert.assertNotNull(appDetailedView.getSites());
                assertFalse(appDetailedView.getSites().isEmpty());
                final Map<String, SecretView> secrets = appDetailedView.getSites().get(0)
                        .getSecrets().stream().collect(Collectors.toMap(SecretView::getName,
                                Function.identity(),(v1, v2) -> v1, LinkedHashMap::new));
                //Using a LinkedHashMap we guarantee we keep the original order on which the elements were sent.
                int index = 0;
                for (Entry<String, SecretView> secretEntry : secrets.entrySet()) {
                    try (StringWriter writer = new StringWriter()) {

                        try(JsonGenerator jsonGenerator = createJsonGenerator(writer)){
                        final String key = secretEntry.getKey();
                        final SecretView view = secretEntry.getValue();
                        ViewUtil.newStackContext(appDetailedView);
                        ViewUtil.currentSite(siteId);
                        final SecretViewSerializer secretViewSerializer = new SecretViewSerializer();
                        secretViewSerializer.serialize(view, jsonGenerator, null);
                        jsonGenerator.flush();
                        final String asJson = writer.toString();
                        ViewUtil.disposeStackContext();
                        Logger.debug(AppsResourceTest.class, () -> String.format(" `%s` ", asJson));

                        final ParamDescriptor descriptorParam = dataGen.paramMap().get(key);

                        final Map<String, Object> deserializedView = mapper.readValue(asJson, new TypeReference<Map<String, Object>>() {});
                        final Type type = Type.valueOf(deserializedView.get("type").toString());
                        final boolean hidden = Boolean.parseBoolean(deserializedView.get("hidden").toString());
                        final String value = deserializedView.get("value").toString();
                        assertNotNull(deserializedView.get("required"));
                        assertNull(deserializedView.get("warnings"));

                        Assert.assertEquals(
                                "If it comes back as hidden it's because the descriptor also says it is hidden ",
                                descriptorParam.isHidden(), hidden);

                        if(Type.STRING.equals(type)){
                            if (hidden) {
                                Assert.assertEquals(SecretViewSerializer.HIDDEN_SECRET_MASK, value);
                            } else {
                                Assert.assertNotEquals(SecretViewSerializer.HIDDEN_SECRET_MASK, value);
                            }
                        } else {
                               if(hidden){
                                  fail("boolean type can not be marked as hidden");
                               } else {
                                   Assert.assertTrue("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value));
                               }
                        }

                        //These should always match whatever was specified on the app-descriptor.
                        Assert.assertEquals(descriptorParam.isHidden(), view.getSecret().isHidden());
                        Assert.assertEquals(descriptorParam.getType(), view.getSecret().getType());
                        //Test the order on which the secrets were sent back. They must whatever order was specified when building the app-descriptor.
                        Assert.assertEquals(view.getName(), orderedParamNames.get(index++));
                        }
                    }
                }
            }
        }
    }

    /**
     * Test we save a secret that is described as a Select List
     * Given scenario: We create a secret then we update the select value sending one of the values
     * Expected Result: Only the public value  was supposed to be updated. The other 2 hidden fields should remain the same since we sent a bunch of *****
     * @throws IOException
     */
    @Test
    public void Test_Select_List_Param()
            throws IOException {
       //Select list definition
       final List<Map<String,String>> list = ImmutableList.of(
           ImmutableMap.of("label","-","value",""),
           ImmutableMap.of("label","uno","value","1"),
           ImmutableMap.of("label","dos","value","2","selected","true"),
           ImmutableMap.of("label","tres","value","3")
       );

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .selectParam("selectParam", true, list)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String appKey = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {

            // Create App integration Descriptor
            final Response appResponse = appsResource.createApp(request, response, createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("public-value1".toCharArray(),false),
                    "selectParam", newInputParam("1".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response detailedIntegrationResponse = appsResource.getAppDetail(request, response, appKey, host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityViewSave = (ResponseEntityView) detailedIntegrationResponse.getEntity();
            final AppView appDetailedView = (AppView) responseEntityViewSave.getEntity();

            Assert.assertNotNull(appDetailedView.getSites());
            assertFalse(appDetailedView.getSites().isEmpty());
            Assert.assertEquals(appDetailedView.getSites().get(0).getId(), host.getIdentifier());
            //We can the secrets here because this happens before the a serializer will exchange the values by asters making them look like `*****`
            final List<SecretView> secretViews = appDetailedView.getSites().get(0).getSecrets();
            final SecretView param1 = secretViews.get(0);
            Assert.assertEquals(new String(param1.getSecret().getValue()),"public-value1");
            final SecretView param2 = secretViews.get(1);
            Assert.assertEquals(new String(param2.getSecret().getValue()),"1");

            //Now testing setting a value that isn't in the list.

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            final Map<String, Input> inputParamMap2 = ImmutableMap.of(
                    "param1", newInputParam("public-value1".toCharArray(),false),
                    "selectParam", newInputParam("5".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm2 = new SecretForm(inputParamMap2);
            final Response createSecretResponse2 = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm2);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse2.getStatus());

            //Now testing setting an empty value. Even though there's an empty value in the list the param is marked as required.
            final Map<String, Input> inputParamMap3 = ImmutableMap.of(
                    "param1", newInputParam("public-value1".toCharArray(),false),
                    "selectParam", newInputParam("".toCharArray(),false));
            // Add secrets to it.
            final SecretForm secretForm3 = new SecretForm(inputParamMap3);
            final Response createSecretResponse3 = appsResource.createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm3);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse3.getStatus());

        }
    }


    private JsonGenerator createJsonGenerator(final StringWriter writer) throws IOException{
        final JsonFactory factory = new JsonFactory();
        final JsonGenerator generator = factory.createGenerator(writer);
        generator.useDefaultPrettyPrinter(); // pretty print JSON
        return generator;
    }


    /**
     * App keys must be case insensitive since they are part of the endpoint URL
     * Given scenario: Test we generate an app and try to access the resource using a randomly cased app-key
     * Expected Result: The App should be available when accessed with the randomly cased url.
     */
    @Test
    public void Test_App_Key_Casing() throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        long time = System.currentTimeMillis();

        final String descriptorFileName = String.format("all_lower_case_not_too_short_prefix_%d.yml", time);
        final String appKey = removeExtension(descriptorFileName);
        dataGen.withFileName(descriptorFileName);
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())) {

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            final String appKeyCasingVariant1 = upperCaseRandom(appKey, 30);

            final Response appByKey = appsResource
                    .getAppByKey(request, response, appKeyCasingVariant1, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, appByKey.getStatus());

            final List<String> sites = new ArrayList<>();
            final int max = 5;
            for (int i = 1; i <= max; i++) {

                //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
                final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("val-1".toCharArray(),false)
                );

                sites.add(
                   createAppSecret(inputParamMap, appKey, request, response).getIdentifier()
                );
            }

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = appsResource
                        .getAppDetail(request, response,
                                upperCaseRandom(appKey, 30), siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            }
        }
    }

    /**
     * Test an addSecret operation with missing required params fails.
     * Given scenario: Test we create an app descriptor that states required field we send the param but empty.
     * Expected Result: We expect a BAD_REQUEST (400) when the request is sent missing a required value.
     */
    @Test
    public void Test_Required_Params_Sent_Empty_Value_Expect_Bad_Request() throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String key = dataGen.getKey();
        final String fileName = dataGen.getFileName();

        //We're indicating that extra params are allowed to test required params are still required
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())){

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            //Here's a secret with an empty param that is marked as required.
            final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("".toCharArray(),false));

            final Host host = new SiteDataGen().nextPersisted();
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, key, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse.getStatus());
        }
    }

    /**
     * Test a combination of extra params and required params
     * Given scenario: Test we create an app descriptor that states required fields and extra params are allowed params too.
     * Then we Send an incomplete request
     * Expected Result: We expect a BAD_REQUEST (400)
     */
    @Test
    public void Test_Required_Params_Send_Missing_Required_Param_Expect_Bad_Request()
            throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .stringParam("param2", false,  true)
                .withName("any")
                .withDescription("demo")
                //We're indicating that extra params are allowed to test required params are still required.
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String key = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())){

            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
            //We're sending only one parameter when the descriptor says there's another one mandatory.
            final Map<String, Input> inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("any-value".toCharArray(),false)
                    //Please Notice We're NOT sending param2!!!
            );

            final Host host = new SiteDataGen().nextPersisted();
            final SecretForm secretForm = new SecretForm(inputParamMap);
            final Response createSecretResponse = appsResource
                    .createAppSecrets(request, response, key, host.getIdentifier(), secretForm);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse.getStatus());
        }
    }

    /**
     * Test
     * Scenario: We attempt to upload the exact same file twice.
     * Expected results: Bad-Request
     * @throws IOException
     */
    @Test
    public void Test_Already_Exists_Exception_Expect_Bad_Request() throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false,  true)
                .stringParam("param2", false,  true)
                .withName("any")
                .withDescription("demo")
                //We're indicating that extra params are allowed to test required params are still required.
                .withExtraParameters(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())){
            final FormDataMultiPart formDataMultiPart = createFormDataMultiPart(fileName, inputStream);
            final Response appResponseOk = appsResource.createApp(request, response,formDataMultiPart);
            Assert.assertNotNull(appResponseOk);
            Assert.assertEquals(HttpStatus.SC_OK, appResponseOk.getStatus());
        }

        try(InputStream inputStream = Files.newInputStream(Paths.get(file.getPath()))){
            final FormDataMultiPart formDataMultiPart = createFormDataMultiPart(fileName, inputStream);
            final Response appResponseBadRequest = appsResource
                    .createApp(request, response, formDataMultiPart);
            Assert.assertNotNull(appResponseBadRequest);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, appResponseBadRequest.getStatus());
        }
    }

    /**
     * Test Extra params support
     * Given scenario: Test we create an app descriptor that states extra params are allowed.
     * Expected Result: We should be able to add extra params. And they are shown at the end on the site view detail.
     */
    @Test
    public void Test_Create_Descriptor_Then_Add_Dynamic_Prop() throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", true,  true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(true);

        final String key = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())){

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            // Create App integration Descriptor
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());

            final Host host = new SiteDataGen().nextPersisted();
            final String siteId = host.getIdentifier();
            Map<String, Input> inputParamMap = ImmutableMap.of(
               "param1", newInputParam("any-value".toCharArray())
            );
            final SecretForm secretForm1 = new SecretForm(inputParamMap);
            final Response createSecretResponse1 = appsResource.createAppSecrets(request, response, key, siteId, secretForm1);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse1.getStatus());

            //Should we support adding individual dynamic props once all required pros are all set.
            inputParamMap = ImmutableMap.of(
                    "param1", newInputParam("any-value".toCharArray()),
                    "dynamicParam1", newInputParam("any-value".toCharArray())
            );
            final SecretForm secretForm2 = new SecretForm(inputParamMap);
            final Response createSecretResponse2 = appsResource.createAppSecrets(request, response, key, host.getIdentifier(), secretForm2);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse2.getStatus());

            final Response detailedIntegrationResponse = appsResource.getAppDetail(request, response, key, siteId);
            final ResponseEntityView responseEntityView = (ResponseEntityView) detailedIntegrationResponse.getEntity();
            final AppView appDetailedView = (AppView) responseEntityView.getEntity();

            final SiteView siteView = appDetailedView.getSites().get(0);
            Assert.assertTrue(siteView.isConfigured());
            //Once a custom comparator is applied to a treemap it becomes unnavigable.
            final Map<String,SecretView> secrets = siteView.getSecrets().stream().collect(Collectors.toMap(SecretView::getName,
                    Function.identity()));

            final SecretView param1 = secrets.get("param1");
            Assert.assertNotNull(param1);
            assertFalse(param1.isDynamic());

            final SecretView dynamicParam1 = secrets.get("dynamicParam1");
            Assert.assertNotNull(dynamicParam1);
            Assert.assertTrue(dynamicParam1.isDynamic());
            Assert.assertEquals("any-value", String.valueOf(dynamicParam1.getSecret().getValue()));

        }
    }

    /**
     * This basically test pagination and filtering together.
     * Given scenario: Test we create an app descriptor that states extra params are allowed.
     * Expected Result: We should be able to add extra params. And they are shown at the end on the site view detail
     */
    @Test
    public void Test_Pagination_And_Sort_Then_Request_Filter_Expect_Empty_Results()
            throws IOException {

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false, true)
                .stringParam("param2", false, true)
                .stringParam("param3", false, true)
                .withName("any")
                .withDescription("demo")
                .withExtraParameters(false);

        final long timeMark = System.currentTimeMillis();
        final List<Host> hosts = new ArrayList<>();
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        for(final char chr :alphabet) {
           hosts.add(new SiteDataGen().name( String.format("%s-%d",chr, timeMark )).nextPersisted());
        }

        // Ensure the host cache is properly warmed with all newly created hosts
        // This prevents race conditions where the cache might not have all hosts yet
        try {
            CacheLocator.getHostCache().clearCache();
            APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), com.dotmarketing.portlets.contentlet.business.HostAPI.SearchType.INCLUDE_SYSTEM_HOST);
        } catch (Exception e) {
            Logger.error(AppsResourceTest.class, "Failed to warm host cache", e);
        }

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");

        final String key = dataGen.getKey();
        final String fileName = dataGen.getFileName();
        final File file = dataGen.nextPersistedDescriptor();
        try(InputStream inputStream = Files.newInputStream(file.toPath())){

            // Create App integration Descriptor
            final Response appResponse = appsResource
                    .createApp(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(appResponse);
            Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
            final Response availableAppsResponse = appsResource
                    .listAvailableApps(request, response, null);
            Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                    .getEntity();
            final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                    .getEntity();
            assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            appView -> dataGen.getName()
                                    .equals(appView.getName())));

            // Add secrets to it.
            for(final Host host:hosts) {

                //Secrets are destroyed for security every time. Making the form useless. They need to be re-generated every time.
                final Map<String, Input> inputParamMap = ImmutableMap.of(
                        "param1", newInputParam("val-1".toCharArray(),false),
                        "param2", newInputParam("val-2".toCharArray(),false),
                        "param3", newInputParam("val-2".toCharArray(),false)
                );

                createSecret(request, response, key, host, inputParamMap);
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

        }

    }

    /**
     * Given Scenario: We create secrets then export that particular one then the generated file is used as the input for the import method.
     * Expected Results: The secrets are imported correctly and replace the same old exiting secrets. No additional secrets are created.
     * @throws IOException
     */
     @Test
     public void Test_Export_Import() throws IOException {
         final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                 .stringParam("param1", false,  true)
                 .stringParam("param2", false,  true)
                 .withName("import-export-test")
                 .withDescription("import-export-test")
                 //We're indicating that extra params are allowed to test required params are still required.
                 .withExtraParameters(true);
         final String key = dataGen.getKey();
         final HttpServletRequest request = mock(HttpServletRequest.class);
         final HttpServletResponse response = mock(HttpServletResponse.class);

         when(request.getRequestURI()).thenReturn("/baseURL");
         final String fileName = dataGen.getFileName();
         final File file = dataGen.nextPersistedDescriptor();
         try(InputStream inputStream = Files.newInputStream(file.toPath())){
             final FormDataMultiPart formDataMultiPart = createFormDataMultiPart(fileName, inputStream);
             final Response appResponseOk = appsResource.createApp(request, response,formDataMultiPart);
             Assert.assertNotNull(appResponseOk);
             Assert.assertEquals(HttpStatus.SC_OK, appResponseOk.getStatus());

             final Response availableAppsResponse = appsResource
                     .listAvailableApps(request, response, null);
             Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
             final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                     .getEntity();
             final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                     .getEntity();
             assertFalse(integrationViewList.isEmpty());

             final Host site = new SiteDataGen().nextPersisted();

             final String siteId = site.getIdentifier();
             final Map<String, Input> inputParamMap = ImmutableMap.of(
              "param1", newInputParam("value".toCharArray()),
              "param2", newInputParam("value".toCharArray())
             );
             final SecretForm secretForm1 = new SecretForm(inputParamMap);
             final Response createSecretResponse1 = appsResource.createAppSecrets(request, response, key, siteId, secretForm1);
             Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse1.getStatus());

             final String password = "12345678901234";
             ExportSecretForm form = new ExportSecretForm(password,false, ImmutableMap.of(siteId, ImmutableSet.of(key)));
             final OutboundJaxrsResponse exportResponse = (OutboundJaxrsResponse)appsResource.exportSecrets(request, response, form);
             final String json = String.format("{ password: \"%s\" }",password);
             final InputStream entity = (InputStream) exportResponse.getEntity();
             final File targetFile = File.createTempFile("secrets", ".export");
             FileUtils.copyInputStreamToFile(entity, targetFile);

            try(InputStream exportInputStream = Files.newInputStream(targetFile.toPath())){
                 final Response appResponse = appsResource.importSecrets(request, response,
                         createFormDataMultiPart(targetFile.getName(), exportInputStream, json));
                 Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatus());
             }
         }
     }

    /**
     * Given Scenario: We create secrets with whitespaces at the beginning/end of keys/values
     * Expected Results: Secrets' keys/values should be saved without whitespaces at the beginning/end
     * @throws IOException
     */
     @Test
     public void Test_Secret_Form_Keys_And_Values_Are_Trimmed() throws IOException {
         final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                 .stringParam("param1", false,  true)
                 .stringParam("param2", false,  true)
                 .withName("secret-form-trim-test")
                 .withDescription("secret-form-trim-test")
                 //We're indicating that extra params are allowed to test required params are still required.
                 .withExtraParameters(true);
         final String key = dataGen.getKey();
         final HttpServletRequest request = mock(HttpServletRequest.class);
         final HttpServletResponse response = mock(HttpServletResponse.class);

         when(request.getRequestURI()).thenReturn("/baseURL");
         final String fileName = dataGen.getFileName();
         final File file = dataGen.nextPersistedDescriptor();
         try(InputStream inputStream = Files.newInputStream(file.toPath())) {
             final FormDataMultiPart formDataMultiPart = createFormDataMultiPart(fileName,
                     inputStream);
             final Response appResponseOk = appsResource
                     .createApp(request, response, formDataMultiPart);
             Assert.assertNotNull(appResponseOk);
             Assert.assertEquals(HttpStatus.SC_OK, appResponseOk.getStatus());

             final Response availableAppsResponse = appsResource
                     .listAvailableApps(request, response, null);
             Assert.assertEquals(HttpStatus.SC_OK, availableAppsResponse.getStatus());
             final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableAppsResponse
                     .getEntity();
             final List<AppView> integrationViewList = (List<AppView>) responseEntityView1
                     .getEntity();
             assertFalse(integrationViewList.isEmpty());

             final Host site = new SiteDataGen().nextPersisted();

             final String siteId = site.getIdentifier();
             final Map<String, Input> inputParamMap = ImmutableMap.of(
                     "    param1", newInputParam("   value1".toCharArray()),
                     "param2    ", newInputParam("value2    ".toCharArray())
             );
             final SecretForm secretForm1 = new SecretForm(inputParamMap);
             final Response createSecretResponse1 = appsResource
                     .createAppSecrets(request, response, key, siteId, secretForm1);
             Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse1.getStatus());

             final Response appDetail = appsResource.getAppDetail(request, response, key,siteId);
             Assert.assertEquals(HttpStatus.SC_OK, appDetail.getStatus());

             final ResponseEntityView entityView = (ResponseEntityView) appDetail.getEntity();
             final AppView appView = (AppView) entityView.getEntity();

             Assert.assertNotNull(appView.getSites());
             assertFalse(appView.getSites().isEmpty());
             final Map<String, SecretView> storedSecrets = appView
                     .getSites()
                     .get(0).getSecrets().stream().collect(Collectors.toMap(SecretView::getName,
                             Function.identity()));

             Assert.assertEquals(storedSecrets.get("param1").getSecret().getString(),"value1");
             Assert.assertEquals(storedSecrets.get("param2").getSecret().getString(),"value2");
         }
     }

    /**
     * This basically tests that we can not use the  list endpoint that list available apps
     * Given scenario: We have a non-valid-license type of situation then we call the list available apps endpoint
     * Expected Result: We should be getting a 403
     */
    @Test
    public void Test_List_Available_Apps_Invalid_License() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Response listAvailableAppsResponse = appsResourceNonLicense.listAvailableApps(request, response, null);
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, listAvailableAppsResponse.getStatus());
    }

    /**
     * This basically tests that we can not use the app by key endpoint
     * Given scenario: We have a non-valid-license type of situation then we call the get app by key endpoint
     * Expected Result: We should be getting a 403
     */
    @Test
    public void Test_Get_App_By_Key_Invalid_License() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Response appByKeyResponse = appsResourceNonLicense.getAppByKey(request, response, "anyKey", paginationContext());
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, appByKeyResponse.getStatus());
    }

    /**
     * This basically tests that we can not use the get app detail endpoint.
     * Given scenario: We have a non-valid-license type of situation then we call the  get app detail endpoint.
     * Expected Result: We should be getting a 403
     */
    @Test
    public void Test_Get_App_Detail_Invalid_License() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Response appDetailResponse = appsResourceNonLicense.getAppDetail(request, response, "anyKey", "any-site-id");
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, appDetailResponse.getStatus());
    }

    private void createSecret(final HttpServletRequest request, final HttpServletResponse response,
            final String appKey, final Host host, final Map<String, Input> paramMap){
        final SecretForm secretForm = new SecretForm(paramMap);
        final Response createSecretResponse = appsResource
                .createAppSecrets(request, response, appKey, host.getIdentifier(), secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
    }

    private Host createAppSecret(final Map<String, Input> paramMap,
            final String key, final HttpServletRequest request,
            final HttpServletResponse response) {
        final Host host = new SiteDataGen().nextPersisted();
        final SecretForm secretForm = new SecretForm(paramMap);
        final Response createSecretResponse = appsResource.createAppSecrets(request, response, key, host.getIdentifier(), secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
        return host;
    }

    private PaginationContext paginationContext(){
        //OrderBy and direction are ignored by design.
       return new PaginationContext(null, 1, 100, "", "");
    }

}
