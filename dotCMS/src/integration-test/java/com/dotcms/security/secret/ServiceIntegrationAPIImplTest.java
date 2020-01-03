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
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
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
        api.saveSecrets(bean, host, admin);
        final Optional<ServiceSecrets> optionalBean = api
                .getSecretsForService(serviceKey, host, admin);

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

        api.saveSecrets(bean1Host1, host1, admin);
        api.saveSecrets(bean2Host1, host1, admin);
        api.saveSecrets(bean3Host1, host1, admin);
        api.saveSecrets(bean1Host2, host2, admin);

        final Map<String,List<String>> serviceKeysByHost = api.serviceKeysByHost();
        assertNotNull(serviceKeysByHost.get(host1.getIdentifier()));
        assertNotNull(serviceKeysByHost.get(host2.getIdentifier()));

        assertEquals(3, api.listServiceKeys(host1, admin).size());
        assertEquals(1, api.listServiceKeys(host2, admin).size());

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

        api.saveSecrets(beanSystemHost1, systemHost, admin);

        final Optional<ServiceSecrets> serviceSecretsOptional = api.getSecretsForService("serviceKey-1-Any-Host", host1, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final ServiceSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("serviceKey-1-Any-Host", recoveredBean.getServiceKey());
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test"));
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test:secret1"));
    }

    @Test
    public void Test_Save_Secrets_Then_Replace_Secret() throws DotDataException, DotSecurityException {
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        final ServiceSecrets.Builder builder1 = new ServiceSecrets.Builder();
        final ServiceSecrets secrets1 = builder1.withServiceKey("serviceKey-1-Host-1")
                .withHiddenSecret("test:secret1", "sec1")
                .withHiddenSecret("test:secret2", "sec2")
                .build();
        api.saveSecrets(secrets1, host, admin);

        final ServiceSecrets.Builder builder2 = new ServiceSecrets.Builder();
        final ServiceSecrets secrets2 = builder2.withServiceKey("serviceKey-1-Host-1")
                .withHiddenSecret("test:secret1", "secret1")
                .build();
        api.saveSecrets(secrets2, host, admin);
        final Optional<ServiceSecrets> serviceSecretsOptional = api.getSecretsForService("serviceKey-1-Host-1", host, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final ServiceSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("serviceKey-1-Host-1", recoveredBean.getServiceKey());
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret1"));
        assertFalse(recoveredBean.getSecrets().containsKey("test:secret2"));
        assertEquals("secret1", recoveredBean.getSecrets().get("test:secret1").getString());
    }

    @Test
    public void Test_Save_Secrets_Then_Update_Single_Secret_Verify_Updated_Value() throws DotDataException, DotSecurityException {
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        //Let's create a set of secrets for a service
        final ServiceSecrets.Builder builder1 = new ServiceSecrets.Builder();
        final ServiceSecrets secrets1 = builder1.withServiceKey("serviceKey-1-Host-1")
                .withHiddenSecret("test:secret1", "secret-1")
                .withHiddenSecret("test:secret2", "secret-2")
                .withHiddenSecret("test:secret3", "secret3")
                .withHiddenSecret("test:secret4", "secret-4")
                .build();
        //Save it
        api.saveSecrets(secrets1, host, admin);

        //Now we want to update one of the values within the secret.
        //We want to change the value from `secret3` to `secret-3` for the secret named "test:secret3"
        final Secret secret = Secret.newSecret("secret-3".toCharArray(), Type.STRING, false);
        //Update the individual secret
        api.saveSecret("serviceKey-1-Host-1", new Tuple2<>("test:secret3",secret), host, admin);
        //The other properties of the object should remind the same so lets verify so.
        final Optional<ServiceSecrets> serviceSecretsOptional = api.getSecretsForService("serviceKey-1-Host-1", host, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final ServiceSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("serviceKey-1-Host-1", recoveredBean.getServiceKey());

        //We didn't modify the keys just the value associated with `test:secret3`
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret1"));
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret2"));
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret3"));
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret4"));

        //now lets verify the values returned
        assertEquals("secret-1", recoveredBean.getSecrets().get("test:secret1").getString());
        assertEquals("secret-2", recoveredBean.getSecrets().get("test:secret2").getString());
        assertEquals("secret-3", recoveredBean.getSecrets().get("test:secret3").getString());
        assertEquals("secret-4", recoveredBean.getSecrets().get("test:secret4").getString());

    }

    @Test
    public void Test_Save_Secrets_Then_Delete_Single_Secret_Entry_Then_Add_It_Again_With_New_Value_Then_Verify() throws DotDataException, DotSecurityException {
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        //Let's create a set of secrets for a service
        final ServiceSecrets.Builder builder1 = new ServiceSecrets.Builder();
        final ServiceSecrets secrets1 = builder1.withServiceKey("serviceKeyHost-1")
                .withHiddenSecret("test:secret1", "secret-1")
                .withHiddenSecret("test:secret2", "secret-2")
                .withHiddenSecret("test:secret3", "secret-3")
                .withHiddenSecret("test:secret4", "secret-4")
                .build();
        //Save it
        api.saveSecrets(secrets1, host, admin);

        api.deleteSecret("serviceKeyHost-1",new ImmutableSet.Builder<String>().add("test:secret3").build(), host, admin);

        //The other properties of the object should remind the same so lets verify so.
        final Optional<ServiceSecrets> serviceSecretsOptional1 = api.getSecretsForService("serviceKeyHost-1", host, admin);
        assertTrue(serviceSecretsOptional1.isPresent());
        final ServiceSecrets recoveredBean1 = serviceSecretsOptional1.get();
        assertEquals("serviceKeyHost-1", recoveredBean1.getServiceKey());

        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret1"));
        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret2"));
        assertFalse(recoveredBean1.getSecrets().containsKey("test:secret3"));
        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret4"));

        //now lets verify the values returned
        assertEquals("secret-1", recoveredBean1.getSecrets().get("test:secret1").getString());
        assertEquals("secret-2", recoveredBean1.getSecrets().get("test:secret2").getString());
        assertEquals("secret-4", recoveredBean1.getSecrets().get("test:secret4").getString());

        //Now lets re-introduce again the property we just deleted
        final Secret secret = Secret.newSecret("lol".toCharArray(), Type.STRING, false);

        //This should create again the entry we just removed.
        api.saveSecret("serviceKeyHost-1", new Tuple2<>("test:secret3",secret), host, admin);

        final Optional<ServiceSecrets> serviceSecretsOptional2 = api.getSecretsForService("serviceKeyHost-1", host, admin);
        assertTrue(serviceSecretsOptional2.isPresent());
        final ServiceSecrets recoveredBean2 = serviceSecretsOptional2.get();
        assertEquals("serviceKeyHost-1", recoveredBean2.getServiceKey());

        assertTrue(recoveredBean2.getSecrets().containsKey("test:secret1"));
        assertTrue(recoveredBean2.getSecrets().containsKey("test:secret2"));
        assertTrue(recoveredBean2.getSecrets().containsKey("test:secret3"));
        assertTrue(recoveredBean2.getSecrets().containsKey("test:secret4"));

        //now lets verify the values returned
        assertEquals("secret-1", recoveredBean2.getSecrets().get("test:secret1").getString());
        assertEquals("secret-2", recoveredBean2.getSecrets().get("test:secret2").getString());
        assertEquals("lol", recoveredBean2.getSecrets().get("test:secret3").getString()); //<-- Here the updated value.
        assertEquals("secret-4", recoveredBean2.getSecrets().get("test:secret4").getString());
    }


    @Test(expected = DotSecurityException.class)
    public void Test_Non_Admin_User_Read_Attempt() throws DotDataException, DotSecurityException {
        final User nonAdmin = TestUserUtils.getChrisPublisherUser();
        final ServiceIntegrationAPI api = APILocator.getServiceIntegrationAPI();
        final ServiceSecrets.Builder builder = new ServiceSecrets.Builder();
        api.saveSecrets(builder.build(), APILocator.systemHost() , nonAdmin);
    }


}
