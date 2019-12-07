package com.dotcms.security.secret;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class ServiceIntegrationAPIImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    @Test
    public void Test_Store_Json_Then_Recover_Decrypted_Then_Delete_Then_Verify_Its_Gone()
            throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
        final String serviceKey = "anyServiceKey";
        final ServiceSecrets bean = builder.withServiceKey(serviceKey)
                .withHiddenSecret("mySecret1", "Once I saw a UFO")
                .withHiddenSecret("mySecret2", "Football soccer sucks!")
                .withSecret("boolSecret1", true)
                .withSecret("boolSecret2", false)
                .build();

        final Host host = new SiteDataGen().nextPersisted();
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        api.saveServiceSecrets(bean, host, admin);
        final Optional<ServiceSecrets> optionalBean = api
                .getSecretForService(serviceKey, host, admin);

        assertTrue(optionalBean.isPresent());

        final ServiceSecrets recoveredBean = optionalBean.get();
        assertEquals(serviceKey, recoveredBean.getServiceKey());
        assertTrue(recoveredBean.getSecrets().containsKey("mySecret1"));
        assertTrue(recoveredBean.getSecrets().containsKey("mySecret2"));
        assertTrue(recoveredBean.getSecrets().containsKey("boolSecret1"));
        assertTrue(recoveredBean.getSecrets().containsKey("boolSecret2"));

        final Secret secret1 = recoveredBean.getSecrets().get("mySecret1");
        assertEquals("Once I saw a UFO", secret1.getString());
        assertTrue(secret1.isHidden());

        final Secret secret2 = recoveredBean.getSecrets().get("mySecret2");
        assertEquals("Football soccer sucks!", secret2.getString());
        assertTrue(secret2.isHidden());

        final Secret boolSecret1 = recoveredBean.getSecrets().get("boolSecret1");
        assertTrue(boolSecret1.getBoolean());
        assertFalse(boolSecret1.isHidden());

        final Secret boolSecret2 = recoveredBean.getSecrets().get("boolSecret2");
        assertFalse(boolSecret2.getBoolean());
        assertFalse(boolSecret2.isHidden());
    }

    @Test
    public void Test_Store_Json_On_Different_Sites_Then_Pull_Sites_By_Host_Verify_Match()
            throws DotDataException, DotSecurityException {

        final User admin = TestUserUtils.getAdminUser();
        final Host host1 = new SiteDataGen().nextPersisted();
        final Host host2 = new SiteDataGen().nextPersisted();

        final ServiceSecrets.Builder builder1 = new ServiceSecrets.Builder();

        final ServiceSecrets bean1Host1 = builder1.withServiceKey("serviceKey-1-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        final ServiceSecrets bean2Host1 = builder1.withServiceKey("serviceKey-2-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        final ServiceSecrets bean3Host1 = builder1.withServiceKey("serviceKey-3-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        ServiceSecrets.Builder builder2 = new ServiceSecrets.Builder();

        final ServiceSecrets bean1Host2 = builder2.withServiceKey("serviceKey-1-Host-2")
                .withHiddenSecret("h2:secret1", "sec1")
                .withHiddenSecret("h2:secret2", "sec2")
                .withSecret("h2:bool1", true)
                .withSecret("h2:bool2", false)
                .build();

        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();

        api.saveServiceSecrets(bean1Host1, host1, admin);
        api.saveServiceSecrets(bean2Host1, host1, admin);
        api.saveServiceSecrets(bean3Host1, host1, admin);
        api.saveServiceSecrets(bean1Host2, host2, admin);

        final Map<String,List<String>> serviceKeysByHost = api.serviceKeysByHost();
        assertNotNull(serviceKeysByHost.get(host1.getIdentifier()));
        assertNotNull(serviceKeysByHost.get(host2.getIdentifier()));

        assertEquals(3, api.listServiceKeys(host1).size());
        assertEquals(1, api.listServiceKeys(host2).size());

        assertEquals(3, serviceKeysByHost.get(host1.getIdentifier()).size());
        assertEquals(1, serviceKeysByHost.get(host2.getIdentifier()).size());

        assertTrue(serviceKeysByHost.get(host1.getIdentifier()).contains("serviceKey-1-Host-1".toLowerCase()));
        assertTrue(serviceKeysByHost.get(host1.getIdentifier()).contains("serviceKey-2-Host-1".toLowerCase()));
        assertTrue(serviceKeysByHost.get(host1.getIdentifier()).contains("serviceKey-3-Host-1".toLowerCase()));
        assertTrue(serviceKeysByHost.get(host2.getIdentifier()).contains("serviceKey-1-Host-2".toLowerCase()));

    }

    @Test
    public void Test_Lookup_Fallback() throws DotDataException, DotSecurityException {
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host systemHost = APILocator.systemHost();
        final Host host1 = new SiteDataGen().nextPersisted();

        final ServiceSecrets.Builder builder1 = new ServiceSecrets.Builder();

        final ServiceSecrets beanSystemHost1 = builder1.withServiceKey("serviceKey-1-Any-Host")
                .withHiddenSecret("fallback:test:secret1", "sec1")
                .withSecret("fallback:test", true)
                .build();

        api.saveServiceSecrets(beanSystemHost1, systemHost, admin);

        final Optional<ServiceSecrets> serviceSecretsOptional = api.getSecretForService("serviceKey-1-Any-Host", host1, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final ServiceSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("serviceKey-1-Any-Host", recoveredBean.getServiceKey());
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test"));
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test:secret1"));
    }

    @Test(expected = DotSecurityException.class)
    public void Test_Non_Admin_User_Read_Attempt() throws DotDataException, DotSecurityException {
        final User nonAdmin = TestUserUtils.getChrisPublisherUser();
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
        api.saveServiceSecrets(builder.build(), APILocator.systemHost() , nonAdmin);
    }


}
