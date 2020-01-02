package com.dotcms.rest.api.v1.secret;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.secret.view.HostView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationDetailedView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationHostView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
import com.dotcms.security.secret.Param;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
            .findAndRegisterModules()
            ;

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

        //ServiceIntegrationAPI serviceIntegrationAPI = new ServiceIntegrationAPIImpl();

        ServiceIntegrationHelper serviceIntegrationHelper = new ServiceIntegrationHelper();

        serviceIntegrationResource = new ServiceIntegrationResource(webResource, serviceIntegrationHelper);
    }

    private InputStream createServiceDescriptorFile(final String fileName, final String serviceKey,
            String serviceName, final String description,
            final Map<String, Param> paramMap) throws IOException {
        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor(serviceKey, serviceName, description, "/black.png");

        for (final Entry<String, Param> entry : paramMap.entrySet()) {
            final Param param = entry.getValue();
            serviceDescriptor.addParam(entry.getKey(),param.getValue(), param.isHidden(), param.getType(), param.getLabel(), param.getHint());
        }

        String basePath = System.getProperty("java.io.tmpdir");
        basePath = Paths.get(basePath).normalize().toString();
        final File file = new File(basePath,fileName);
        ymlMapper.writeValue(file, serviceDescriptor);
        System.out.println(file);
        return new FileInputStream(file);
    }

    private FormDataMultiPart createFormDataMultiPart(final String fileName, final InputStream inputStream) {
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
    public void  Test_Create_service_descriptor_Then_Create_Service_Integration() throws IOException {
        final Map<String, Param> paramMap = ImmutableMap.of(
                "p1", Param.newParam("v1", false, Type.STRING, "label", "hint"),
                "p2", Param.newParam("v2", false, Type.STRING, "label", "hint"),
                "p3", Param.newParam("v3", false, Type.STRING, "label", "hint")
        );
        final Host host = new SiteDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String serviceKey = String.format("lol_%d",System.currentTimeMillis());
        final String fileName = String.format("%s.yml",serviceKey);
        final InputStream inputStream = createServiceDescriptorFile(fileName, serviceKey, "lola",
                "A bunch of string params to demo the mechanism.", paramMap);
        final Response serviceIntegrationResponse = serviceIntegrationResource.createServiceIntegration(request, response, createFormDataMultiPart(fileName, inputStream));
        Assert.assertNotNull(serviceIntegrationResponse);
        Assert.assertEquals(HttpStatus.SC_OK, serviceIntegrationResponse.getStatus());
        final Response availableServicesResponse = serviceIntegrationResource.listAvailableServices(request, response);
        Assert.assertEquals(HttpStatus.SC_OK, availableServicesResponse.getStatus());
        final ResponseEntityView responseEntityView1 = (ResponseEntityView)availableServicesResponse.getEntity();
        final List<ServiceIntegrationView> integrationViewList = (List<ServiceIntegrationView>)responseEntityView1.getEntity();
        Assert.assertFalse(integrationViewList.isEmpty());
        Assert.assertTrue(
                integrationViewList.stream().anyMatch(serviceIntegrationView ->  "lola".equals(serviceIntegrationView.getName())));

        final SecretForm secretForm = new SecretForm();
        secretForm.setServiceKey(serviceKey);
        secretForm.setHostId(host.getIdentifier());
        secretForm.setParams(paramMap);
        final Response createSecretResponse = serviceIntegrationResource.createSecret(request, response, secretForm);
        Assert.assertEquals(HttpStatus.SC_OK, createSecretResponse.getStatus());

        final Response hostIntegrationsResponse = serviceIntegrationResource.getServiceIntegrationByKey(request, response, serviceKey);
        Assert.assertEquals(HttpStatus.SC_OK, hostIntegrationsResponse.getStatus());
        final ResponseEntityView responseEntityView2 = (ResponseEntityView) hostIntegrationsResponse.getEntity();
        final ServiceIntegrationHostView serviceIntegrationHostView2 = (ServiceIntegrationHostView) responseEntityView2.getEntity();
        final ServiceIntegrationView serviceIntegrationView2 = serviceIntegrationHostView2.getService();
        Assert.assertEquals(1, serviceIntegrationView2.getConfigurationsCount());
        Assert.assertEquals("lola", serviceIntegrationView2.getName());
        final List<HostView> hosts = serviceIntegrationHostView2.getHosts();
        Assert.assertNotNull(hosts);
        Assert.assertFalse(hosts.isEmpty());
        Assert.assertEquals(hosts.get(0).getHostId(),host.getIdentifier());
        Assert.assertTrue(
                hosts.stream().anyMatch(hostView -> host.getIdentifier().equals(hostView.getHostId()))
                );

        final Response detailedIntegrationResponse = serviceIntegrationResource.getDetailedServiceIntegration(request, response, serviceKey, host.getIdentifier());
        Assert.assertEquals(HttpStatus.SC_OK, detailedIntegrationResponse.getStatus());
        final ResponseEntityView responseEntityView3 = (ResponseEntityView) detailedIntegrationResponse.getEntity();
        final ServiceIntegrationDetailedView serviceIntegrationDetailedView = (ServiceIntegrationDetailedView) responseEntityView3.getEntity();
        Assert.assertEquals(serviceIntegrationDetailedView.getHost().getHostId(),host.getIdentifier());
    }

}
