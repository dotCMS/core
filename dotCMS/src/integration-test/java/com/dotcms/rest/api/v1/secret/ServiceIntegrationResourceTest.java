package com.dotcms.rest.api.v1.secret;

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
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
import com.dotcms.rest.api.v1.secret.view.SiteView;
import com.dotcms.security.secret.Param;
import com.dotcms.security.secret.Secret;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.Type;
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

public class ServiceIntegrationResourceTest extends IntegrationTestBase {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";

    private static ServiceIntegrationResource serviceIntegrationResource;

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

        final ServiceIntegrationHelper serviceIntegrationHelper = new ServiceIntegrationHelper();

        serviceIntegrationResource = new ServiceIntegrationResource(webResource,
                serviceIntegrationHelper);
    }

    private InputStream createServiceDescriptorFile(final String fileName, final String serviceKey,
            final String serviceName, final String description, final boolean allowExtraParameters,
            final Map<String, Param> paramMap) throws IOException {
        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(serviceKey, serviceName,
                description, "/black.png", allowExtraParameters, new HashMap<>());

        for (final Entry<String, Param> entry : paramMap.entrySet()) {
            final Param param = entry.getValue();
            serviceDescriptor
                    .addParam(entry.getKey(), param.getValue(), param.isHidden(), param.getType(),
                            param.getLabel(), param.getHint(), param.isRequired());
        }

        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath, fileName);
        ymlMapper.writeValue(file, serviceDescriptor);
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
    public void Test_Create_service_descriptor_Then_Create_Service_Integration_Then_Delete_The_Whole_Integration() {

        final Host host = new SiteDataGen().nextPersisted();
        final Map<String, Param> paramMap = ImmutableMap.of(
                "p1", Param.newParam("v1", false, Type.STRING, "label", "hint", true),
                "p2", Param.newParam("v2", false, Type.STRING, "label", "hint", true),
                "p3", Param.newParam("v3", false, Type.STRING, "label", "hint", true)
        );
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String serviceKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", serviceKey);
        try(final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey, "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {
            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));

            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());
            final Response availableServicesResponse = serviceIntegrationResource
                    .listAvailableServices(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableServicesResponse
                    .getEntity();
            final List<ServiceIntegrationView> integrationViewList = (List<ServiceIntegrationView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            serviceIntegrationView -> "lola"
                                    .equals(serviceIntegrationView.getName())));

            final SecretForm secretForm = new SecretForm(serviceKey,host.getIdentifier(), paramMap);
            final Response createSecretResponse = serviceIntegrationResource
                    .createServiceIntegrationSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            final Response hostIntegrationsResponse = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView2 = (ResponseEntityView) hostIntegrationsResponse
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationWithSites = (ServiceIntegrationView) responseEntityView2
                    .getEntity();
            Assert.assertEquals(1, serviceIntegrationWithSites.getConfigurationsCount());
            Assert.assertEquals("lola", serviceIntegrationWithSites.getName());
            final List<SiteView> sites = serviceIntegrationWithSites.getSites();
            Assert.assertNotNull(sites);
            Assert.assertTrue(sites.size() >= 2);
            Assert.assertEquals(sites.get(0).getId(), Host.SYSTEM_HOST); //system host is always the first element to come.
            Assert.assertEquals(sites.get(1).getId(), host.getIdentifier());
            Assert.assertTrue(
                    sites.stream()
                            .anyMatch(hostView -> host.getIdentifier().equals(hostView.getId()))
            );

            final Response detailedIntegrationResponse = serviceIntegrationResource
                    .getDetailedServiceIntegration(request, response, serviceKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationDetailedView = (ServiceIntegrationView) responseEntityView3
                    .getEntity();
            Assert.assertNotNull(serviceIntegrationDetailedView.getSites());
            Assert.assertFalse(serviceIntegrationDetailedView.getSites().isEmpty());
            Assert.assertEquals(serviceIntegrationDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());

            final Response deleteIntegrationsResponse = serviceIntegrationResource
                    .deleteAllServiceIntegrationSecrets(request, response, serviceKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, deleteIntegrationsResponse.getStatus());

            final Response detailedIntegrationResponseAfterDelete = serviceIntegrationResource
                    .getDetailedServiceIntegration(request, response, serviceKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND,
                    detailedIntegrationResponseAfterDelete.getStatus());

            //Now test the entry has been removed from the list of available configurations.
            final Response hostIntegrationsResponseAfterDelete = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponseAfterDelete.getStatus());
            final ResponseEntityView responseEntityViewAfterDelete = (ResponseEntityView) hostIntegrationsResponseAfterDelete
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationHostViewAfterDelete = (ServiceIntegrationView) responseEntityViewAfterDelete
                    .getEntity();

            Assert.assertEquals(0, serviceIntegrationHostViewAfterDelete.getConfigurationsCount());
            Assert.assertEquals("lola", serviceIntegrationHostViewAfterDelete.getName());
            final List<SiteView> expectedEmptyHosts = serviceIntegrationHostViewAfterDelete.getSites();
            Assert.assertNotNull(expectedEmptyHosts);
            // Previously this test wasn't expecting any entry here
            // But the pagination will now return only items marked to have no configurations.
            Assert.assertEquals("None of the returned item should have configuration", 0,
                    expectedEmptyHosts.stream().filter(SiteView::isConfigured).count());
        }catch (Exception e){
            Logger.error(ServiceIntegrationResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_Create_service_descriptor_Then_Create_Service_Integration_Then_Delete_One_Single_Secret() {

        final Map<String, Param> paramMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("val-2", false, Type.STRING, "label", "hint", true),
                "param3", Param.newParam("val-3", false, Type.STRING, "label", "hint", true)
        );
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/baseURL");
        final String serviceKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", serviceKey);
        try(final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey, "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {

            // Create Service integration Descriptor
            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());
            final Response availableServicesResponse = serviceIntegrationResource
                    .listAvailableServices(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableServicesResponse
                    .getEntity();
            final List<ServiceIntegrationView> integrationViewList = (List<ServiceIntegrationView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            serviceIntegrationView -> "lola"
                                    .equals(serviceIntegrationView.getName())));

            // Add secrets to it.
            final SecretForm secretForm = new SecretForm(serviceKey,host.getIdentifier(), paramMap);
            final Response createSecretResponse = serviceIntegrationResource
                    .createServiceIntegrationSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

            //fetch and verify the secrets by service-key
            final Response serviceIntegrationByKey = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationByKey.getStatus());
            final ResponseEntityView responseEntityView2 = (ResponseEntityView) serviceIntegrationByKey
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationByKeyView = (ServiceIntegrationView) responseEntityView2
                    .getEntity();

            Assert.assertEquals(1, serviceIntegrationByKeyView.getConfigurationsCount());
            Assert.assertEquals("lola", serviceIntegrationByKeyView.getName());
            final List<SiteView> hosts = serviceIntegrationByKeyView.getSites();
            Assert.assertNotNull(hosts);
            Assert.assertTrue(hosts.size() >= 2);
            Assert.assertEquals(hosts.get(0).getId(), Host.SYSTEM_HOST); //system host is always the first element to come.
            Assert.assertEquals(hosts.get(1).getId(), host.getIdentifier());
            Assert.assertTrue(
                    hosts.stream()
                            .anyMatch(hostView -> host.getIdentifier().equals(hostView.getId()))
            );

            final Response detailedIntegrationResponse = serviceIntegrationResource
                    .getDetailedServiceIntegration(request, response, serviceKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationDetailedView = (ServiceIntegrationView) responseEntityView3
                    .getEntity();

            Assert.assertNotNull(serviceIntegrationDetailedView.getSites());
            Assert.assertFalse(serviceIntegrationDetailedView.getSites().isEmpty());
            Assert.assertEquals(serviceIntegrationDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());
            Assert.assertEquals(serviceIntegrationDetailedView.getSites().get(0).getId(),
                    host.getIdentifier());

            //Delete individual secrets
            final Set<String> paramsToDelete = ImmutableSet.of(
                    "param1",
                    "param3"
            );

            final DeleteSecretForm deleteSecretForm = new DeleteSecretForm(serviceKey,host.getIdentifier(),paramsToDelete);
            final Response deleteIndividualSecretResponse = serviceIntegrationResource
                    .deleteIndividualServiceIntegrationSecret(request, response, deleteSecretForm);
            Assert.assertEquals(HttpStatus.SC_OK, deleteIndividualSecretResponse.getStatus());

            //The service integration should still be there but the individual params/secrets should be gone
            final Response detailedIntegrationResponseAfterDelete = serviceIntegrationResource
                    .getDetailedServiceIntegration(request, response, serviceKey,
                            host.getIdentifier());
            Assert.assertEquals(HttpStatus.SC_OK,
                    detailedIntegrationResponseAfterDelete.getStatus());

            final ResponseEntityView responseAfterDeleteEntityView = (ResponseEntityView) detailedIntegrationResponseAfterDelete
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationViewAfterDelete = (ServiceIntegrationView) responseAfterDeleteEntityView
                    .getEntity();

            Assert.assertNotNull(serviceIntegrationViewAfterDelete.getSites());
            Assert.assertFalse(serviceIntegrationViewAfterDelete.getSites().isEmpty());
            final Map<String, Secret> secretsAfterDelete = serviceIntegrationViewAfterDelete
                    .getSites()
                    .get(0).getSecrets();
            Assert.assertTrue(secretsAfterDelete.containsKey("param2"));
            //The ones we removed must not be present.. right?
            Assert.assertFalse(secretsAfterDelete.containsKey("param1"));
            Assert.assertFalse(secretsAfterDelete.containsKey("param3"));
        }catch (Exception e){
            Logger.error(ServiceIntegrationResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_Create_service_descriptor_Then_Create_Service_Integration_Then_Delete_Service_Descriptor() {

        final Map<String, Param> paramMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true),
                "param2", Param.newParam("val-2", false, Type.STRING, "label", "hint", true),
                "param3", Param.newParam("val-3", false, Type.STRING, "label", "hint", true)
        );

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        final String serviceKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", serviceKey);
        try (final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey,
                serviceKey,
                "This should go away.", false, paramMap)) {
            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());
            final List<String> sites = new ArrayList<>();
            final int max = 10;
            for (int i = 1; i <= max; i++) {
                final Host host = createServiceIntegrationParams(paramMap, serviceKey, request,
                        response);
                sites.add(host.getIdentifier());
            }

            //The Service does exist and so it does the secrets.
            final Response hostIntegrationsResponse = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView = (ResponseEntityView) hostIntegrationsResponse
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationView = (ServiceIntegrationView) responseEntityView
                    .getEntity();
            Assert.assertEquals(max, serviceIntegrationView.getConfigurationsCount());

            //Now lets get rid of the service Descriptor and verify.
            final Response deleteServiceIntegrationResponse = serviceIntegrationResource
                    .deleteServiceIntegration(request, response, serviceKey, true);
            Assert.assertEquals(HttpStatus.SC_OK, deleteServiceIntegrationResponse.getStatus());

            final Response availableServicesResponse = serviceIntegrationResource
                    .listAvailableServices(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableServicesResponse
                    .getEntity();
            final List<ServiceIntegrationView> integrationViewList = (List<ServiceIntegrationView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            //Service is gone.
            Assert.assertTrue(
                    integrationViewList.stream()
                            .noneMatch(view -> serviceKey.equals(view.getName())));
            final Response responseAfterDelete = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseAfterDelete.getStatus());

            for (final String siteId : sites) {
                final Response responseNotFound = serviceIntegrationResource
                        .getDetailedServiceIntegration(request, response, serviceKey, siteId);
                Assert.assertEquals(HttpStatus.SC_NOT_FOUND, responseNotFound.getStatus());
            }
        } catch (Exception e) {
            Logger.error(ServiceIntegrationResourceTest.class, e);
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

        final String serviceKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", serviceKey);
        try(final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey,
                serviceKey,
                "Test-hidden-secret-protection", false, initialParamsMap)) {

            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());

            final List<String> sites = new ArrayList<>();

            final int max = 10;
            for (int i = 1; i <= max; i++) {
                sites.add(
                        createServiceIntegrationParams(initialParamsMap, serviceKey, request,
                                response)
                                .getIdentifier()
                );
            }

            //The Service does exist and so it does the secrets.
            final Response siteIntegrationsResponse = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, siteIntegrationsResponse.getStatus());
            final ResponseEntityView responseEntityView = (ResponseEntityView) siteIntegrationsResponse
                    .getEntity();
            final ServiceIntegrationView serviceIntegrationView = (ServiceIntegrationView) responseEntityView
                    .getEntity();

            Assert.assertEquals(max, serviceIntegrationView.getConfigurationsCount());

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = serviceIntegrationResource
                        .getDetailedServiceIntegration(request, response, serviceKey, siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
                final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse
                        .getEntity();
                final ServiceIntegrationView serviceIntegrationDetailedView = (ServiceIntegrationView) responseEntityView3
                        .getEntity();
                Assert.assertNotNull(serviceIntegrationDetailedView.getSites());
                Assert.assertFalse(serviceIntegrationDetailedView.getSites().isEmpty());
                final Map<String, Secret> secrets = serviceIntegrationDetailedView.getSites().get(0)
                        .getSecrets();
                for (Entry<String, Secret> secretEntry : secrets.entrySet()) {
                    final String key = secretEntry.getKey();
                    final Secret secret = secretEntry.getValue();
                    final Param originalParam = initialParamsMap.get(key);
                    if (secret.isHidden()) {
                        Assert.assertEquals(ServiceIntegrationHelper.PROTECTED_HIDDEN_SECRET,
                                new String(secret.getValue()));
                    } else {
                        Assert.assertEquals(originalParam.getString(),
                                new String(secret.getValue()));
                    }
                }
            }
        }catch (Exception e){
            Logger.error(ServiceIntegrationResourceTest.class, e);
            fail();
        }
    }


    @Test
    public void Test_Service_Key_Casing() throws IOException, DotDataException, DotSecurityException {

        final Map<String, Param> initialParamsMap = ImmutableMap.of(
                "param1", Param.newParam("val-1", false, Type.STRING, "label", "hint", true));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/baseURL");

        long time = System.currentTimeMillis();

        final String serviceKey1 = String.format("all_lower_case_not_too_short_prefix_%d", time);
        final String fileName = String.format("%s.yml", serviceKey1);
        try(final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey1,
                serviceKey1,
                "Test-service-casing", false, initialParamsMap)) {

            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());

            final String serviceKeyCasingVariant1 = upperCaseRandom(serviceKey1, 30);

            final Response serviceIntegrationByKey = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKeyCasingVariant1, paginationContext());
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationByKey.getStatus());

            final List<String> sites = new ArrayList<>();
            final int max = 5;
            for (int i = 1; i <= max; i++) {
                sites.add(
                        createServiceIntegrationParams(initialParamsMap, serviceKey1, request,
                                response)
                                .getIdentifier()
                );
            }

            for (final String siteId : sites) {
                final Response detailedIntegrationResponse = serviceIntegrationResource
                        .getDetailedServiceIntegration(request, response,
                                upperCaseRandom(serviceKey1, 30), siteId);
                Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
            }
        }catch (Exception e){
            Logger.error(ServiceIntegrationResourceTest.class, e);
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

        final String serviceKey = String.format("all_lower_case_not_too_short_prefix_%d", time);
        final String fileName = String.format("%s.yml", serviceKey);

        //We're indicating that extra params are allowed to test required params are still required
        try(final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey,
                serviceKey,
                "Test-required-params",
                true, initialParamsMap)){

            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());

            //Here's a secret with an empty param that is marked as required.
            final Map<String, Param> secretParam = ImmutableMap.of(
                    "param1", Param.newParam("", false, Type.STRING, null, null, true)
            );

            final Host host = new SiteDataGen().nextPersisted();
            final SecretForm secretForm = new SecretForm(serviceKey, host.getIdentifier(), secretParam);
            final Response createSecretResponse = serviceIntegrationResource
                    .createServiceIntegrationSecrets(request, response, secretForm);
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, createSecretResponse.getStatus());
        }catch (Exception e){
            Logger.error(ServiceIntegrationResourceTest.class, e);
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
        final String serviceKey = String.format("lol_%d", System.currentTimeMillis());
        final String fileName = String.format("%s.yml", serviceKey);
        try (final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey,
                "lola",
                "A bunch of string params to demo the mechanism.", false, paramMap)) {

            // Create Service integration Descriptor
            final Response serviceIntegrationResponse = serviceIntegrationResource
                    .createServiceIntegration(request, response,
                            createFormDataMultiPart(fileName, inputStream));
            Assert.assertNotNull(serviceIntegrationResponse);
            Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());
            final Response availableServicesResponse = serviceIntegrationResource
                    .listAvailableServices(request, response);
            Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatus());
            final ResponseEntityView responseEntityView1 = (ResponseEntityView) availableServicesResponse
                    .getEntity();
            final List<ServiceIntegrationView> integrationViewList = (List<ServiceIntegrationView>) responseEntityView1
                    .getEntity();
            Assert.assertFalse(integrationViewList.isEmpty());
            Assert.assertTrue(
                    integrationViewList.stream().anyMatch(
                            serviceIntegrationView -> "lola"
                                    .equals(serviceIntegrationView.getName())));

            // Add secrets to it.
            for(final Host host:hosts) {
                createSecret(request, response, serviceKey, host, paramMap);
            }

            //fetch and test pagination
            final int pageSize = 4;
            final int numberOfPages = hosts.size() / pageSize;

            for(int currentPage = 1; currentPage <= numberOfPages; currentPage++) {

                final Response paginationResponse1 = serviceIntegrationResource
                        .getServiceIntegrationByKey(request, response, serviceKey,
                                new PaginationContext(null, currentPage, pageSize, "", ""));
                Assert.assertEquals(HttpStatus.SC_OK, paginationResponse1.getStatus());
                final ResponseEntityView paginationEntity1 = (ResponseEntityView) paginationResponse1
                        .getEntity();
                final ServiceIntegrationView paginationView1 = (ServiceIntegrationView) paginationEntity1
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

            final Response paginationFilterResponse = serviceIntegrationResource
                    .getServiceIntegrationByKey(request, response, serviceKey,
                            new PaginationContext("lol", 0, numberOfPages, "", ""));
            Assert.assertEquals(HttpStatus.SC_OK, paginationFilterResponse.getStatus());
            final ResponseEntityView paginationFilterEntity = (ResponseEntityView) paginationFilterResponse.getEntity();
            final ServiceIntegrationView paginationFilterView = (ServiceIntegrationView) paginationFilterEntity.getEntity();
            assertTrue(paginationFilterView.getSites().isEmpty());

        } catch (Exception e) {
            Logger.error(ServiceIntegrationResourceTest.class, e);
            fail();
        }

    }

    private void createSecret(final HttpServletRequest request, final HttpServletResponse response,
            final String serviceKey, final Host host, final Map<String, Param> paramMap){
        final SecretForm secretForm = new SecretForm(serviceKey, host.getIdentifier(), paramMap);
        final Response createSecretResponse = serviceIntegrationResource.createServiceIntegrationSecrets(request, response, secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
    }

    private Host createServiceIntegrationParams(final Map<String, Param> paramMap,
            final String serviceKey, final HttpServletRequest request,
            final HttpServletResponse response) {
        final Host host = new SiteDataGen().nextPersisted();
        final SecretForm secretForm = new SecretForm(serviceKey, host.getIdentifier(), paramMap);
        final Response createSecretResponse = serviceIntegrationResource
                .createServiceIntegrationSecrets(request, response, secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());
        return host;
    }

    private PaginationContext paginationContext(){
        //OrderBy and direction are ignored by design.
       return new PaginationContext(null, 1, 100, "", "");
    }

}
