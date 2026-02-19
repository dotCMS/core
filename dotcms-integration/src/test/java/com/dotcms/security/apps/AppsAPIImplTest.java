package com.dotcms.security.apps;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.content.elasticsearch.business.IndiciesAPI;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.datagen.AppDescriptorDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PortletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rest.api.v1.apps.ExportSecretForm;
import com.dotcms.rest.api.v1.apps.view.SecretView.SecretViewSerializer;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.util.EncryptorException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AppsAPIImplTest {

    private static final Map<String, ParamDescriptor> PARAMS = of(
            "requiredNoDefault",
            ParamDescriptor.builder()
                    .withHidden(false)
                    .withType(Type.STRING)
                    .withLabel("any")
                    .withHint("hint")
                    .withRequired(true)
                    .build(),
            "requiredDefault",
            ParamDescriptor.builder()
                    .withValue("default")
                    .withHidden(false)
                    .withType(Type.STRING)
                    .withLabel("any")
                    .withHint("hint")
                    .withRequired(true)
                    .build(),
            "nonRequiredNoDefault",
            ParamDescriptor.builder()
                    .withHidden(false)
                    .withType(Type.STRING)
                    .withLabel("any")
                    .withHint("hint")
                    .withRequired(false)
                    .build()
    );

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    /**
     * This method test basis scenarios like adding a secret and recovering it by the finder method.
     * Given scenario: Happy path testing a save and find then a Delete
     * Expected Result: The bean once saved is returned by the API once removed it should be no longer returned
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Store_Json_Then_Recover_Decrypted_Then_Delete_Then_Verify_Its_Gone()
            throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        AppSecrets.Builder builder = new AppSecrets.Builder();
        final String appKey = "anyAppKey";
        final AppSecrets bean = builder.withKey(appKey)
                .withHiddenSecret("mySecret1", "Once I saw a UFO")
                .withHiddenSecret("mySecret2", "Football soccer sucks!")
                .withSecret("boolSecret1", true)
                .withSecret("boolSecret2", false)
                .build();

        final Host host = new SiteDataGen().nextPersisted();
        final AppsAPI api = APILocator.getAppsAPI();
        api.saveSecrets(bean, host, admin);
        final Optional<AppSecrets> optionalBean = api
                .getSecrets(appKey, host, admin);

        assertTrue(optionalBean.isPresent());

        final AppSecrets recoveredBean = optionalBean.get();
        assertEquals(appKey, recoveredBean.getKey());
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

        api.deleteSecrets(appKey, host, admin);

        final Optional<AppSecrets> optionalBeanAfterDelete = api
                .getSecrets(appKey, host, admin);

        assertFalse("it's gone.", optionalBeanAfterDelete.isPresent());
    }

    /**
     * This method test AppsAPI#appKeysByHost() which organizes app-Keys by host id
     * Given scenario: save a bunch of secrets under different sites
     * Expected Result: call method in question and verify secrets came back showing what secrets belong into what hosts.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Store_Json_On_Different_Sites_Then_Pull_Sites_By_Host_Verify_Match()
            throws DotDataException, DotSecurityException {

        final User admin = TestUserUtils.getAdminUser();
        final Host host1 = new SiteDataGen().nextPersisted();
        final Host host2 = new SiteDataGen().nextPersisted();

        final AppSecrets.Builder builder1 = new AppSecrets.Builder();

        final AppSecrets bean1Host1 = builder1.withKey("appKey-1-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        final AppSecrets bean2Host1 = builder1.withKey("appKey-2-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        final AppSecrets bean3Host1 = builder1.withKey("appKey-3-Host-1")
                .withHiddenSecret("h1:secret1", "sec1")
                .withHiddenSecret("h1:secret2", "sec2")
                .withSecret("h1:bool1", true)
                .withSecret("h1:bool2", false)
                .build();

        final AppSecrets.Builder builder2 = new AppSecrets.Builder();

        final AppSecrets bean1Host2 = builder2.withKey("appKey-1-Host-2")
                .withHiddenSecret("h2:secret1", "sec1")
                .withHiddenSecret("h2:secret2", "sec2")
                .withSecret("h2:bool1", true)
                .withSecret("h2:bool2", false)
                .build();

        final AppsAPI api = APILocator.getAppsAPI();

        api.saveSecrets(bean1Host1, host1, admin);
        api.saveSecrets(bean2Host1, host1, admin);
        api.saveSecrets(bean3Host1, host1, admin);
        api.saveSecrets(bean1Host2, host2, admin);

        final Map<String, Set<String>> appKeysByHost = api.appKeysByHost();
        assertNotNull(appKeysByHost.get(host1.getIdentifier()));
        assertNotNull(appKeysByHost.get(host2.getIdentifier()));

        assertEquals(3, api.listAppKeys(host1, admin).size());
        assertEquals(1, api.listAppKeys(host2, admin).size());

        assertEquals(3, appKeysByHost.get(host1.getIdentifier()).size());
        assertEquals(1, appKeysByHost.get(host2.getIdentifier()).size());

        assertTrue(appKeysByHost.get(host1.getIdentifier()).contains("appKey-1-Host-1".toLowerCase()));
        assertTrue(appKeysByHost.get(host1.getIdentifier()).contains("appKey-2-Host-1".toLowerCase()));
        assertTrue(appKeysByHost.get(host1.getIdentifier()).contains("appKey-3-Host-1".toLowerCase()));
        assertTrue(appKeysByHost.get(host2.getIdentifier()).contains("appKey-1-Host-2".toLowerCase()));

    }

    /**
     * This method test AppsAPI#getSecrets
     * Given scenario: I create a secret under System_Host Then the method in question is called passing the fallBackOnSystemHost flag in true.
     * Expected Result: Then the method in question is called passing the fallBackOnSystemHost flag in true For a random Id using the same key I should get the value under system host.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Lookup_Fallback() throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host systemHost = APILocator.systemHost();
        final Host host1 = new SiteDataGen().nextPersisted();

        final AppSecrets.Builder builder1 = new AppSecrets.Builder();

        final AppSecrets beanSystemHost1 = builder1.withKey("appKey-1-Any-Host")
                .withHiddenSecret("fallback:test:secret1", "sec1")
                .withSecret("fallback:test", true)
                .build();

        api.saveSecrets(beanSystemHost1, systemHost, admin);

        final Optional<AppSecrets> serviceSecretsOptional = api.getSecrets("appKey-1-Any-Host", true, host1, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final AppSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("appKey-1-Any-Host", recoveredBean.getKey());
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test"));
        assertTrue(recoveredBean.getSecrets().containsKey("fallback:test:secret1"));
    }

    /**
     * This method test AppsAPI#saveSecrets
     * Given scenario: I create a secret for a given key and host-id then I call again the save method and replace the secret width one that has a complete different structure,
     * Expected Result: For the key and host the results coming back must match the new structure used to replace the original secret.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Save_Secrets_Then_Replace_Secret() throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets1 = builder1.withKey("appKey-1-Host-1")
                .withHiddenSecret("test:secret1", "sec1")
                .withHiddenSecret("test:secret2", "sec2")
                .build();
        api.saveSecrets(secrets1, host, admin);

        final AppSecrets.Builder builder2 = new AppSecrets.Builder();
        final AppSecrets secrets2 = builder2.withKey("appKey-1-Host-1")
                .withHiddenSecret("test:secret1", "secret1")
                .build();
        api.saveSecrets(secrets2, host, admin);
        final Optional<AppSecrets> serviceSecretsOptional = api.getSecrets("appKey-1-Host-1", host, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final AppSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("appKey-1-Host-1", recoveredBean.getKey());
        assertTrue(recoveredBean.getSecrets().containsKey("test:secret1"));
        assertFalse(recoveredBean.getSecrets().containsKey("test:secret2"));
        assertEquals("secret1", recoveredBean.getSecrets().get("test:secret1").getString());
    }

    /**
     * This method test AppsAPI#saveSecret that takes a Tuple of (key,secret) and replaces one single property at the time
     * Given scenario: I create a secret for a given key and host-id then I call again the saveSecret method and replace one single prop
     * Expected Result: For the key and host the results coming back must match the old existing structure but the replaced property must match the new value.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Save_Secrets_Then_Update_Single_Secret_Verify_Updated_Value() throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        //Let's create a set of secrets for a service
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets1 = builder1.withKey("appKey-1-Host-1")
                .withHiddenSecret("test:secret1", "secret-1")
                .withHiddenSecret("test:secret2", "secret-2")
                .withHiddenSecret("test:secret3", "secret3")
                .withHiddenSecret("test:secret4", "secret-4")
                .build();
        //Save it
        api.saveSecrets(secrets1, host, admin);

        //Now we want to update one of the values within the secret.
        //We want to change the value from `secret3` to `secret-3` for the secret named "test:secret3"
        final Secret secret = Secret.builder().withValue("secret-3").withHidden(false).withType(Type.STRING).build();
        //Update the individual secret
        api.saveSecret("appKey-1-Host-1", Tuple.of("test:secret3",secret), host, admin);
        //The other properties of the object should remind the same so lets verify so.
        final Optional<AppSecrets> serviceSecretsOptional = api.getSecrets("appKey-1-Host-1", host, admin);
        assertTrue(serviceSecretsOptional.isPresent());
        final AppSecrets recoveredBean = serviceSecretsOptional.get();
        assertEquals("appKey-1-Host-1", recoveredBean.getKey());

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

    /**
     * This method test AppsAPI#saveSecret that takes a Tuple of (key,secret) followed by AppsAPI#saveSecret
     * Given scenario: I create a secret for a given key and host-id then I perform a single delete Prop operation through `deleteSecret`
     * then again I call the saveSecret method and replace one single prop.
     * Expected Result: For the key and host the results coming back must match the old existing structure but the replaced property must match the new value.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Save_Secrets_Then_Delete_Single_Secret_Entry_Then_Add_It_Again_With_New_Value_Then_Verify() throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final User admin = TestUserUtils.getAdminUser();
        final Host host = new SiteDataGen().nextPersisted();

        //Let's create a set of secrets for a service
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets1 = builder1.withKey("appKeyHost-1")
                .withHiddenSecret("test:secret1", "secret-1")
                .withHiddenSecret("test:secret2", "secret-2")
                .withHiddenSecret("test:secret3", "secret-3")
                .withHiddenSecret("test:secret4", "secret-4")
                .build();
        //Save it
        api.saveSecrets(secrets1, host, admin);

        api.deleteSecret("appKeyHost-1",new ImmutableSet.Builder<String>().add("test:secret3").build(), host, admin);

        //The other properties of the object should remind the same so lets verify so.
        final Optional<AppSecrets> serviceSecretsOptional1 = api.getSecrets("appKeyHost-1", host, admin);
        assertTrue(serviceSecretsOptional1.isPresent());
        final AppSecrets recoveredBean1 = serviceSecretsOptional1.get();
        assertEquals("appKeyHost-1", recoveredBean1.getKey());

        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret1"));
        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret2"));
        assertFalse(recoveredBean1.getSecrets().containsKey("test:secret3"));
        assertTrue(recoveredBean1.getSecrets().containsKey("test:secret4"));

        //now lets verify the values returned
        assertEquals("secret-1", recoveredBean1.getSecrets().get("test:secret1").getString());
        assertEquals("secret-2", recoveredBean1.getSecrets().get("test:secret2").getString());
        assertEquals("secret-4", recoveredBean1.getSecrets().get("test:secret4").getString());

        //Now lets re-introduce again the property we just deleted
        final Secret secret = Secret.builder().withValue("lol").withHidden(false).withType(Type.STRING).build();

        //This should create again the entry we just removed.
        api.saveSecret("appKeyHost-1", Tuple.of("test:secret3",secret), host, admin);

        final Optional<AppSecrets> serviceSecretsOptional2 = api.getSecrets("appKeyHost-1", host, admin);
        assertTrue(serviceSecretsOptional2.isPresent());
        final AppSecrets recoveredBean2 = serviceSecretsOptional2.get();
        assertEquals("appKeyHost-1", recoveredBean2.getKey());

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

    /**
     * This method test a save secret operation using a non-admin random user.
     * Given scenario: A non-admin user without portlet apps access tries to save.
     * Expected Result: DotDataException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = DotSecurityException.class)
    public void Test_Non_Admin_User_Read_Attempt() throws DotDataException, DotSecurityException {
        final User nonAdmin = TestUserUtils.getChrisPublisherUser();
        final AppsAPI api = APILocator.getAppsAPI();
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        api.saveSecrets(builder.build(), APILocator.systemHost() , nonAdmin);
    }

    /**
     * This method test a read secret operation using a non-admin random user with portlet access.
     * Given scenario: A non-admin user with granted portlet apps access tries to read.
     * Expected Result: Read operation should succeed
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Non_Admin_User_With_Portlet_Read_Attempt() throws DotDataException, DotSecurityException {
        final String integrationsPortletId = AppsAPIImpl.APPS_PORTLET_ID;
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        final Role backEndUserRole = TestUserUtils.getBackendRole();
        final Portlet portlet = getOrCreateServiceIntegrationPortlet();

        assertNotNull(portlet);
        final LayoutDataGen layoutDataGen = new LayoutDataGen();
        final Layout layout = layoutDataGen
                .portletIds(integrationsPortletId).nextPersisted();

        final RoleDataGen roleDataGen1 = new RoleDataGen();
        final Role role1 = roleDataGen1.layout(layout).nextPersisted();
        final User nonAdminUserWithAccessToPortlet = new UserDataGen().roles(role1,backEndUserRole).nextPersisted();

        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(integrationsPortletId, nonAdminUserWithAccessToPortlet));

        final AppsAPI api = APILocator.getAppsAPI();
        final AppSecrets.Builder builder = new AppSecrets.Builder();
        api.saveSecrets(builder.build(), APILocator.systemHost() , nonAdminUserWithAccessToPortlet);
    }

    /**
     * Test secrets.destroy. Destroying a secret consists in setting all chars within the array to null char.
     * Given scenario: You create a secret with any random value then call destroy
     * Expected Result: Original secret has been replaced by null chars.
     */
    @Test
    public void Test_Secret_Destroy_Method() {
        final AppSecrets secrets = new AppSecrets.Builder()
                .withKey("TheKey")
                .withHiddenSecret("hidden", RandomStringUtils.randomAlphanumeric(60))
                .withSecret("non-hidden1", RandomStringUtils.randomAlphanumeric(27))
                .withSecret("non-hidden5", RandomStringUtils.randomAlphanumeric(100))
                .withSecret("bool1", true)
                .build();
        secrets.destroy();
        for (final Secret secret : secrets.getSecrets().values()) {
            for (final char chr : secret.getValue()) {
                 assertEquals(chr,(char)0);
            }
        }
    }

    /**
     * Test computeSecretWarnings.
     * Given scenario: You create a secret with no value the secret is expected to be required according to the descriptor.
     * Expected Result: Such situation should generate a warning
     */
    @Test
    public void Test_Secret_Integrity_Check_Expect_Warning()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();

        final String appKey = "any-key";

        final AppDescriptor descriptor = mock(AppDescriptor.class);
        when(descriptor.isAllowExtraParameters()).thenReturn(false);
        when(descriptor.getParams()).thenReturn(PARAMS);
        when(descriptor.getName()).thenReturn("any-name");
        when(descriptor.getKey()).thenReturn(appKey);

        final Host site = new SiteDataGen().nextPersisted();
        final User admin = TestUserUtils.getAdminUser();

        //Let's create a set of secrets for a service
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets = builder1.withKey(appKey)
                .withHiddenSecret("requiredNoDefault", "") //Here's the offense.
                .withHiddenSecret("requiredDefault", "secret-2")
                .build();

        //Save it
        api.saveSecrets(secrets, site, admin);
        final Map<String, List<String>> optionalAppWarning = api.computeSecretWarnings(descriptor, site, admin);
        assertFalse(optionalAppWarning.isEmpty());

        final Map<String, Map<String, List<String>>> warningsBySite = api.computeWarningsBySite(descriptor, ImmutableSet.of(site.getIdentifier()), admin);
        assertFalse(warningsBySite.get(site.getIdentifier()).isEmpty());
    }

    /**
     * Test computeSecretWarnings.
     * Given scenario: You create a descriptor that states a param is required. Then you set a Param that has a value on the expected required param.
     * Expected Result: Such situation should NOT generate a warning.
     */
    @Test
    public void Test_Secret_Integrity_Check_Expect_No_Warning()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();

        final String appKey = "any-key";

        final AppDescriptor descriptor = mock(AppDescriptor.class);
        when(descriptor.isAllowExtraParameters()).thenReturn(false);
        when(descriptor.getParams()).thenReturn(PARAMS);
        when(descriptor.getName()).thenReturn("any-name");
        when(descriptor.getKey()).thenReturn(appKey);

        final Host site = new SiteDataGen().nextPersisted();
        final User admin = TestUserUtils.getAdminUser();

        //Let's create a set of secrets for a service
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets = builder1.withKey(appKey)
                .withHiddenSecret("requiredNoDefault", "value") //We're providing the expected value
                .withHiddenSecret("requiredDefault", "secret-2")
                .build();
        //Save it
        api.saveSecrets(secrets, site, admin);

        final Map<String, List<String>> optionalAppWarning = api.computeSecretWarnings(descriptor, site, admin);
        assertTrue(optionalAppWarning.isEmpty());

        final Map<String, Map<String, List<String>>> warningsBySite = api.computeWarningsBySite(descriptor, ImmutableSet.of(site.getIdentifier()), admin);
        assertTrue(warningsBySite.get(site.getIdentifier()).isEmpty());
    }

    private AppDescriptor evaluateAppTestCase(final AppTestCase testCase)
            throws IOException, DotDataException, AlreadyExistException, DotSecurityException {
        Logger.info(AppsAPIImplTest.class, () -> "Evaluating  " + testCase.toString());
        final AppDescriptorDataGen descriptorDataGen = new AppDescriptorDataGen();
        descriptorDataGen.withName(testCase.name).withFileName(testCase.key)
                .withExtraParameters(testCase.allowExtraParameters)
                .withDescription(testCase.description).withIconUrl(testCase.iconUrl);
        for (final Map.Entry<String, ParamDescriptor> entry : testCase.params.entrySet()) {
            descriptorDataGen.param(entry.getKey(), entry.getValue());
        }
        final File file = descriptorDataGen.nextPersistedDescriptor();
        try {
            final AppsAPI api = APILocator.getAppsAPI();
            final User admin = TestUserUtils.getAdminUser();
            return api.createAppDescriptor(file, admin);
        } finally {
            file.delete();
        }

    }

    /**
     * Test AppsAPI#createAppDescriptor input validation.
     * Given scenario: Passing Params that expect to break the validation rules
     * Expected Result: DotDataValidationException is raised.
     */
    @Test(expected = DotDataValidationException.class)
    @UseDataProvider("getExpectedExceptionTestCases")
    public void Test_App_Descriptor_Validation_Expect_Validation_Exceptions(final AppTestCase testCase)
            throws IOException, DotDataException, DotSecurityException, AlreadyExistException {
        assertNotNull(evaluateAppTestCase(testCase));
    }

    @DataProvider
    public static Object[] getExpectedExceptionTestCases() {
        Try.run(() -> IntegrationTestInitService.getInstance().init());
        final Map<String, ParamDescriptor> emptyParams = ImmutableMap.of();
        return new Object[]{
                //The following test that the general required fields are mandatory.
                new AppTestCase("any-key", "", "", "", false, emptyParams),
                new AppTestCase("any-key", "any-name", "", "", false, emptyParams),
                new AppTestCase("any-key", "any-name", "desc", "", false, emptyParams),
                new AppTestCase("any-key", "any-name", "desc", "icon", false, emptyParams),
                //Name too large.
                new AppTestCase(RandomStringUtils
                        .randomAlphanumeric(AppsAPIImpl.DESCRIPTOR_KEY_MAX_LENGTH + 1), "any-name",
                        "desc", "icon", false,
                        emptyParams),
                //Key-too large.
                new AppTestCase("any-key", RandomStringUtils
                        .randomAlphanumeric(AppsAPIImpl.DESCRIPTOR_NAME_MAX_LENGTH + 1), "desc",
                        "icon", false,
                        emptyParams),
                //The following test paramDefinition.
                //Null type  is not allowed.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                               "p1",
                               ParamDescriptor.builder()
                                       .withValue("")
                                       .withHidden(true)
                                       .withType(null)
                                       .withLabel("")
                                       .withHint("")
                                       .withRequired(true)
                                       .build())),
                //Hidden bool param is not allowed.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of(
                               "p2",
                               ParamDescriptor.builder()
                                       .withValue("")
                                       .withHidden(true)
                                       .withType(Type.BOOL)
                                       .withLabel("label")
                                       .withHint("hint")
                                       .withRequired(true)
                                       .build())),
                //emptyLabel.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of(
                               "p3",
                               ParamDescriptor.builder()
                                       .withValue("v1")
                                       .withHidden(true)
                                       .withType(Type.STRING)
                                       .withLabel("")
                                       .withHint("")
                                       .withRequired(true)
                                       .build())),
                //emptyHint.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of(
                               "p4",
                               ParamDescriptor.builder()
                                       .withValue("v1")
                                       .withHidden(true)
                                       .withType(Type.STRING)
                                       .withLabel("label")
                                       .withHint("")
                                       .withRequired(true)
                                       .build())),
                //Required param with null default.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of(
                               "p5",
                               ParamDescriptor.builder()
                                       .withValue(null)
                                       .withHidden(false)
                                       .withType(Type.STRING)
                                       .withLabel("label")
                                       .withHint("hint")
                                       .withRequired(true)
                                       .build())),
                //non parsable to bool string.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p6",
                                ParamDescriptor.builder()
                                        .withValue("lol")
                                        .withHidden(false)
                                        .withType(Type.BOOL)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(true)
                                        .build())),
                //Null hidden to emulate missing hidden field.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p7",
                                ParamDescriptor.builder()
                                        .withValue("false")
                                        .withHidden(null)
                                        .withType(Type.BOOL)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(true)
                                        .build())),
                //Null required to emulate missing hidden field.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p8",
                                ParamDescriptor.builder()
                                        .withValue("false")
                                        .withHidden(false)
                                        .withType(Type.BOOL)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(null)
                                        .build()))
        };
    }

    /**
     * Test AppsAPI#createAppDescriptor input validation.
     * Given scenario: Passing Params that normally wouldn't break the validation rules.
     * Expected Result: No DotDataValidationException is raised.
     */
    @Test
    @UseDataProvider("getValidExceptionFreeTestCases")
    public void Test_App_Descriptor_Validation_Exception_Free(final AppTestCase testCase)
            throws IOException, DotDataException, DotSecurityException, AlreadyExistException {
        assertNotNull(evaluateAppTestCase(testCase));
    }


    @DataProvider
    public static Object[] getValidExceptionFreeTestCases() throws Exception {
        final long postfix = System.currentTimeMillis();
        return new Object[]{
                new AppTestCase("key1_" + postfix, "any-name", "desc", "icon", true,
                        ImmutableSortedMap.of(
                                "p1",
                                ParamDescriptor.builder()
                                        .withValue("")
                                        .withHidden(true)
                                        .withType(Type.STRING)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(true)
                                        .build())),
                new AppTestCase("key2_" + postfix, "any-name", "desc", "icon", true,
                        ImmutableSortedMap.of(
                                "p1",
                                ParamDescriptor.builder()
                                        .withValue("")
                                        .withHidden(true)
                                        .withType(Type.STRING)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(false)
                                        .build())),
                new AppTestCase("key3_" + postfix, "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1",
                                ParamDescriptor.builder()
                                        .withValue("true")
                                        .withHidden(false)
                                        .withType(Type.BOOL)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(true)
                                        .build())),
                new AppTestCase("key4_" + postfix, "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1",
                                ParamDescriptor.builder()
                                        .withValue("false")
                                        .withHidden(false)
                                        .withType(Type.BOOL)
                                        .withLabel("label")
                                        .withHint("hint")
                                        .withRequired(true)
                                        .build()))
        };
    }


    static class AppTestCase {

        private final String key;
        private final String name;
        private final String description;
        private final String iconUrl;
        private final Boolean allowExtraParameters;
        private final Map<String,ParamDescriptor> params;

        AppTestCase(
                final String key,
                final String name,
                final String description,
                final String iconUrl,
                final Boolean allowExtraParameters,
                final Map<String, ParamDescriptor> params) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.iconUrl = iconUrl;
            this.allowExtraParameters = allowExtraParameters;
            this.params = params;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
        }
    }

    private Portlet getOrCreateServiceIntegrationPortlet(){
        final String integrationsPortletId = AppsAPIImpl.APPS_PORTLET_ID;
        final PortletAPI portletAPI = APILocator.getPortletAPI();
        Portlet portlet = portletAPI.findPortlet(integrationsPortletId);
        if(null == portlet) {
            final PortletDataGen portletDataGen = new PortletDataGen();
            portlet = portletDataGen.portletId(integrationsPortletId).nextPersisted();
        }
        return portlet;
    }

    @Test
    public void Test_Delete_Secrets_On_Site_Delete()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final User admin = TestUserUtils.getAdminUser();

        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final String appKey ="appKeyHost";
        //Let's create a set of secrets for a service
        final AppSecrets secrets1 = builder1.withKey(appKey)
                .withHiddenSecret("test:secret1", "secret-1")
                .withHiddenSecret("test:secret2", "secret-2")
                .build();
        final Host newSite = new SiteDataGen().nextPersisted();
        //Save it
        api.saveSecrets(secrets1, newSite, admin);

        final Optional<AppSecrets> secrets = api.getSecrets(appKey, newSite, admin);
        assertTrue(secrets.isPresent());

        //Now delete the site
        final HostAPI hostAPI = APILocator.getHostAPI();
        hostAPI.archive(newSite, admin, false);
        hostAPI.delete(newSite, admin, false);

        final Optional<AppSecrets> secretsAfterSiteDelete = api.getSecrets(appKey, newSite, admin);
        assertFalse(secretsAfterSiteDelete.isPresent());

    }

    private final AppsAPI nonValidLicenseAppsAPI = new AppsAPIImpl(
            APILocator.getLayoutAPI(),
            APILocator.getHostAPI(), SecretsStore.INSTANCE.get(),
            CacheLocator.getAppsCache(), APILocator.getLocalSystemEventsAPI(), new AppDescriptorHelper(),
            new LicenseValiditySupplier() {
                public boolean hasValidLicense() {
                    return false;
                }
            });

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#getAppDescriptors
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Get_Descriptor_With_Non_Valid_License()
            throws DotDataException, DotSecurityException {

        final User admin = TestUserUtils.getAdminUser();
        nonValidLicenseAppsAPI.getAppDescriptors(admin);
    }

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#getAppDescriptor
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Get_Apps_With_Non_Valid_License()
            throws DotDataException, DotSecurityException {

        final User admin = TestUserUtils.getAdminUser();
        nonValidLicenseAppsAPI.getAppDescriptor("anyKey",admin);
    }

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#getSecrets
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Get_Secret_With_Non_Valid_License()
            throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        nonValidLicenseAppsAPI.getSecrets("anyKey",systemHost, admin);
    }

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#getSecrets
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Get_Secret_fallbackOnSystemHost_With_Non_Valid_License()
            throws DotDataException, DotSecurityException {
        final User admin = TestUserUtils.getAdminUser();
        final Host systemHost = APILocator.getHostAPI().findSystemHost();
        nonValidLicenseAppsAPI.getSecrets("anyKey", true, systemHost, admin);
    }

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#exportSecrets
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Export_With_Non_Valid_License()
            throws DotDataException, DotSecurityException, IOException {

        final User admin = TestUserUtils.getAdminUser();
        //AES only supports key sizes of 16, 24 or 32 bytes.
        final String password = RandomStringUtils.randomAlphanumeric(32);
        final Key key = AppsUtil.generateKey(password);
        nonValidLicenseAppsAPI.exportSecrets(key,true,ImmutableMap.of(),admin);
    }

    /**
     * Given scenario: We simulate a non valid license situation then we call  AppsAPI#importSecretsAndSave
     * Expected Results: we should get an InvalidLicenseException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = InvalidLicenseException.class)
    public void Test_Import_With_Non_Valid_License()
            throws DotDataException, DotSecurityException, IOException, EncryptorException {

        final User admin = TestUserUtils.getAdminUser();
        //AES only supports key sizes of 16, 24 or 32 bytes.
        final String password = RandomStringUtils.randomAlphanumeric(32);
        final Key key = AppsUtil.generateKey(password);
        //any file would do we're just testing license
        final Path path = File.createTempFile("fileName1", "txt").toPath();
        nonValidLicenseAppsAPI.importSecretsAndSave(path, key, admin);
    }

    /**
     * Given scenario: We subscribe an event listener and save an event
     * Expected Results: We expect that an event is fired and that after firing the event the AppsSecret that was initially passed to the save method is now destroyed
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Save_Secret_Expect_Event_Notification() throws DotDataException, DotSecurityException{
        final AtomicInteger callsCount = new AtomicInteger(0);
        final AppsAPI api = APILocator.getAppsAPI();
        final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        localSystemEventsAPI.subscribe(AppSecretSavedEvent.class, new AppsSecretEventSubscriber(){
            @Override
            public void notify(AppSecretSavedEvent event) {
                final AppSecrets appSecrets = event.getAppSecrets();
                final Map<String, Secret> secrets = appSecrets.getSecrets();
                secrets.forEach((s, secret) -> {
                    assertFalse(isSecretDestroyed(secret.getValue()));
                });
                callsCount.incrementAndGet();
            }
        });

        final String appKey = AppsSecretEventSubscriber.appKey;

        final AppDescriptor descriptor = mock(AppDescriptor.class);
        when(descriptor.isAllowExtraParameters()).thenReturn(false);
        when(descriptor.getParams()).thenReturn(PARAMS);
        when(descriptor.getName()).thenReturn("any-name");
        when(descriptor.getKey()).thenReturn(appKey);

        final Host site = new SiteDataGen().nextPersisted();
        final User admin = TestUserUtils.getAdminUser();

        //Let's create a set of secrets for a service
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppSecrets secrets = builder1.withKey(appKey)
                .withHiddenSecret("requiredNoDefault", "value") //We're providing the expected value
                .withHiddenSecret("requiredDefault", "secret-2")
                .build();
        //Save it
        api.saveSecrets(secrets, site, admin);
        DateUtil.sleep(2000);
        assertEquals(callsCount.get(), 1);

        // Now Test Secret has been destroyed.
        final Map<String, Secret> secretsPostSave = secrets.getSecrets();
        for(final String key: secretsPostSave.keySet()){
            final char[] value = secretsPostSave.get(key).getValue();
            assertTrue(isSecretDestroyed(value));
        }
    }

    /**
     * for internal use validate a secret has been destroyed
     * @param chars
     * @return
     */
    private boolean isSecretDestroyed(final char [] chars){
        final char nullChar = (char) 0;
        for(final char chr: chars){
            if(chr != nullChar){
                return false;
            }
        }
        return true;
    }

    /***
     * Given scenario: We create a file then move it into the system folder we clear cache and the the request app-descriptors again
     * Expected: The Key must appear marked as System-app. If we attempt a delete
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test(expected = DotSecurityException.class)
    public void Test_Add_System_File_Retrieve_Descriptors_Verify()
            throws DotDataException, DotSecurityException, IOException, URISyntaxException {
            //Generate a yml file
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .stringParam("p3", false,  true)
                .withName("system-app-example")
                .withDescription("system-app-demo")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            //Move the file to the system folder
            final Path systemAppsDescriptorDirectory = AppDescriptorHelper
                    .getSystemAppsDescriptorDirectory();
            final boolean result = file.renameTo(new File(
                    systemAppsDescriptorDirectory.toString() + File.separator + file.getName()));
            assertTrue(result);

            final User admin = TestUserUtils.getAdminUser();
            final AppsAPI api = APILocator.getAppsAPI();
            final AppsCache appsCache = CacheLocator.getAppsCache();

            //Invalidate cache so the new descriptors get picked
            appsCache.invalidateDescriptorsCache();
            final List<AppDescriptor> appDescriptors = api.getAppDescriptors(admin);

            //Verify the file we just submitted is recognized as a system-app-file
            final Optional<AppDescriptor> optional = appDescriptors.stream()
                    .filter(appDescriptor -> dataGen.getKey().equals(appDescriptor.getKey()))
                    .findFirst();
            assertTrue(optional.isPresent());
            final AppDescriptor descriptor = optional.get();
            final AppDescriptorImpl impl = (AppDescriptorImpl) descriptor;
            assertTrue(impl.isSystemApp());
            //Now attempt a delete and instruct the api to remove the system app
            api.removeApp(descriptor.getKey(), admin, true);
        } finally {
            file.delete();
        }
    }

    /**
     * Given scenario: We have two files almost identical. one under user-apps-folder and another under system-app-folder
     * Expected: The file placed under system-app-folder must take precedence.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws URISyntaxException
     * @throws AlreadyExistException
     */
    @Test
    public void Test_System_File_Has_Precedence()
            throws DotDataException, DotSecurityException, IOException, URISyntaxException, AlreadyExistException {

        final User admin = TestUserUtils.getAdminUser();
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsCache appsCache = CacheLocator.getAppsCache();

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .withName("system-app-example")
                .withDescription("system-app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            //Move the file to the system folder
            final Path systemAppsDescriptorDirectory = AppDescriptorHelper
                    .getSystemAppsDescriptorDirectory();
            final boolean result = file.renameTo(new File(
                    systemAppsDescriptorDirectory.toString() + File.separator + file.getName()));
            assertTrue(result);
            //Even though we just moved the file under apps-system-folder this should recreate the file again.
            //But before that.. lets make a small change so we can tell the difference between the tow files.
            dataGen.withDescription("user-app");
            final File newFile = dataGen.nextPersistedDescriptor();
            try{
                api.createAppDescriptor(newFile, admin);

                //Invalidate cache so the new descriptors get picked
                appsCache.invalidateDescriptorsCache();
                final List<AppDescriptor> appDescriptors = api.getAppDescriptors(admin);

                //Verify the file we just submitted is recognized as a system-app-file
                final Optional<AppDescriptor> optional = appDescriptors.stream()
                        .filter(appDescriptor -> dataGen.getKey().equals(appDescriptor.getKey()))
                        .findFirst();
                assertTrue(optional.isPresent());
                //
                final AppDescriptor descriptor = optional.get();
                final AppDescriptorImpl impl = (AppDescriptorImpl) descriptor;
                assertTrue(impl.isSystemApp());
                //This proves that even though we had two files named the same. 1 in the user apps folder and another 1 in the system-apps folder.
                //The one from the system-folder takes precedence.
                assertEquals("system-app", impl.getDescription());
            } finally {
                newFile.delete();
            }
        } finally {
            file.delete();
        }
    }

    /**
     * Given scenario: We have two files almost identical. one under user-apps-folder and another under system-app-folder,
     * with the same file name but one in lower case and the other in upper case.
     * Expected: The file name case must be ignored and the file placed under system-app-folder must take precedence.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws URISyntaxException
     * @throws AlreadyExistException
     */
    @Test
    public void Test_File_Comparison_Is_Case_Sensitive()
            throws DotDataException, DotSecurityException, IOException, URISyntaxException, AlreadyExistException {

        final User admin = TestUserUtils.getAdminUser();
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsCache appsCache = CacheLocator.getAppsCache();

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .withName("system-app-example")
                .withDescription("system-app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            //Move the file to the system folder and save it in upper case
            final Path systemAppsDescriptorDirectory = AppDescriptorHelper
                    .getSystemAppsDescriptorDirectory();
            final boolean result = file.renameTo(new File(
                    systemAppsDescriptorDirectory.toString() + File.separator + file.getName()
                            .toUpperCase().replace("YML", "yml")));
            assertTrue(result);
            //Even though we just moved the file under apps-system-folder this should recreate the file again.
            //But before that.. lets make a small change so we can tell the difference between the two files.
            dataGen.withDescription("user-app");
            final File newFile = dataGen.nextPersistedDescriptor();
            try{
                api.createAppDescriptor(newFile, admin);

                //Invalidate cache so the new descriptors get picked
                appsCache.invalidateDescriptorsCache();
                final List<AppDescriptor> appDescriptors = api.getAppDescriptors(admin);

                //Verify the file we just submitted is recognized as a system-app-file
                assertEquals(1, appDescriptors.stream()
                        .filter(appDescriptor -> dataGen.getKey()
                                .equalsIgnoreCase(appDescriptor.getKey())).count());
                final Optional<AppDescriptor> optional = appDescriptors.stream()
                        .filter(appDescriptor -> dataGen.getKey()
                                .equalsIgnoreCase(appDescriptor.getKey())).findFirst();
                assertTrue(optional.isPresent());
                //
                final AppDescriptor descriptor = optional.get();
                final AppDescriptorImpl impl = (AppDescriptorImpl) descriptor;
                assertTrue(impl.isSystemApp());
                //This proves that even though we had two files named the same. 1 in the user apps folder and another 1 in the system-apps folder.
                //The one from the system-folder takes precedence.
                assertEquals("system-app", impl.getDescription());
            }finally {
                newFile.delete();
            }
        } finally {
            file.delete();
        }
    }

    /**
     * Method to test {@link AppsAPIImpl#exportSecrets(Key, boolean, Map, User)} and {@link AppsAPIImpl#importSecretsAndSave(Path, Key, User)}
     * Given Scenario: This test creates secrets then exports them Then re-imports the file with the generated secrets for different host.
     * Expected Result: The test should be Able to recreate the secrets from the generated file regardless of the host.
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws EncryptorException
     * @throws ClassNotFoundException
     */
    @Test
    @UseDataProvider("getTargetSitesTestCases")
    public void Test_Create_Secrets_Then_Export_Them_Then_Import_Then_Save(final Host site)
            throws DotDataException, DotSecurityException, IOException, EncryptorException, AlreadyExistException {
        final User admin = TestUserUtils.getAdminUser();
        final AppsAPI api = APILocator.getAppsAPI();
        //generate a descriptor
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false, true)
                .stringParam("p2", false, true)
                .withName("system-app-example")
                .withDescription("system-app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            api.createAppDescriptor(file, admin);

            final String appKey = dataGen.getKey();
            //generate secrets
            AppSecrets secrets = builder1.withKey(appKey)
                    .withHiddenSecret("p1", "secret-1")
                    .withHiddenSecret("p2", "secret-2")
                    .build();
            //Save it
            api.saveSecrets(secrets, site, admin);
            final Optional<AppSecrets> secretsOptional = api.getSecrets(appKey, site, admin);
            Assert.assertTrue(secretsOptional.isPresent());
            secrets = secretsOptional.get();

            //AES only supports security Key of sizes of 16, 24 or 32 bytes.
            final String password = RandomStringUtils.randomAlphanumeric(32);
            final Key securityKey = AppsUtil.generateKey(password);

            //Now that we have a valid key lets dump our selection of secrets
            final Map<String, Set<String>> appKeysBySite = ImmutableMap
                    .of(site.getIdentifier(), ImmutableSet.of(appKey));
            final Path exportSecretsFile = api
                    .exportSecrets(securityKey, false, appKeysBySite, admin);
            assertTrue(exportSecretsFile.toFile().exists());

            //Remove the secret we dumped we can re import it.
            api.deleteSecrets(appKey, site, admin);

            //import it
            api.importSecretsAndSave(exportSecretsFile, securityKey, admin);

            //verify
            final Optional<AppSecrets> secretsOptionalPostImport = api
                    .getSecrets(appKey, site, admin);
            assertTrue(secretsOptionalPostImport.isPresent());
            final AppSecrets restoredSecrets = secretsOptionalPostImport.get();

            assertEquals(restoredSecrets.getKey(), secrets.getKey());
            assertEquals(restoredSecrets.getSecrets().size(), secrets.getSecrets().size());
            for (final Entry<String, Secret> entry : secrets.getSecrets().entrySet()) {
                final Secret originalSecret = entry.getValue();
                final Secret restoredSecret = restoredSecrets.getSecrets().get(entry.getKey());
                assertTrue(originalSecret.equals(restoredSecret));
            }
        } finally {
            file.delete();
        }
    }

    @DataProvider
    public static Object[] getTargetSitesTestCases() throws Exception {
        Try.run(() -> IntegrationTestInitService.getInstance().init());
        return new Object[]{
                new SiteDataGen().nextPersisted(),
                APILocator.getHostAPI().findSystemHost()
        };
    }

    /**
     * Method to test {@link AppsAPIImpl#exportSecrets(Key, boolean, Map, User)} and {@link AppsAPIImpl#importSecretsAndSave(Path, Key, User)}
     * Given Scenario: This test creates secrets then exports them Then Deletes the appDescriptor used to generate the import Then re-imports the file with the generated secrets.
     * Expected Result: The test should be Able to recreate the secrets from the generated file but due to the inconsistency we get an Exception.
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws EncryptorException
     * @throws AlreadyExistException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Create_Offending_Secrets_Missing_Descriptor_Then_Export_Them_Then_Import_Then_Save()
            throws DotDataException, DotSecurityException, IOException, EncryptorException, AlreadyExistException {

        final User admin = TestUserUtils.getAdminUser();
        final Host site = new SiteDataGen().nextPersisted();
        final AppsAPI api = APILocator.getAppsAPI();
        //generate a descriptor
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .withName("system-app-example")
                .withDescription("system-app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            api.createAppDescriptor(file, admin);

            final String appKey = dataGen.getKey();
            //generate secrets
            final AppSecrets secrets = builder1.withKey(appKey)
                    .withHiddenSecret("p1", "secret-1")
                    .withHiddenSecret("p2", "secret-2")
                    .build();
            //Save it
            api.saveSecrets(secrets, site, admin);
            final Optional<AppSecrets> secretsOptional = api.getSecrets(appKey, site, admin);
            Assert.assertTrue(secretsOptional.isPresent());

            //AES only supports security Key of sizes of 16, 24 or 32 bytes.
            final String password = RandomStringUtils.randomAlphanumeric(32);
            final Key securityKey = AppsUtil.generateKey(password);

            //Now that we have a valid key lets dump our selection of secrets
            final Map<String, Set<String>> appKeysBySite = ImmutableMap
                    .of(site.getIdentifier(), ImmutableSet.of(appKey));
            final Path exportSecretsFile = api
                    .exportSecrets(securityKey, false, appKeysBySite, admin);
            assertTrue(exportSecretsFile.toFile().exists());

            //Remove the secret we dumped we can re import it.
            api.deleteSecrets(appKey, site, admin);

            //Now we're gonna create an inconsistency removing the app descriptor and then try to import the secret.
            api.removeApp(appKey, admin, true);

            //and finally import it
            api.importSecretsAndSave(exportSecretsFile, securityKey, admin);
        }finally {
            file.delete();
        }
    }

    /**
     * Method to test {@link AppsAPIImpl#exportSecrets(Key, boolean, Map, User)} and {@link AppsAPIImpl#importSecretsAndSave(Path, Key, User)}
     * Given Scenario: This test creates secrets then exports them Then Deletes the site used to generate the import Then re-imports the file with the generated secrets.
     * Expected Result: The test should be Able to recreate the secrets from the generated file but due to the inconsistency we get an Exception.
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws EncryptorException
     * @throws AlreadyExistException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Create_Offending_Secrets_Missing_Site_Then_Export_Them_Then_Import_Then_Save()
            throws DotDataException, DotSecurityException, IOException, EncryptorException, AlreadyExistException {

        final User admin = TestUserUtils.getAdminUser();
        final Host site = new SiteDataGen().nextPersisted();
        final AppsAPI api = APILocator.getAppsAPI();
        //generate a descriptor
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .withName("system-app-example")
                .withDescription("system-app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            api.createAppDescriptor(file, admin);

            final String appKey = dataGen.getKey();
            //generate secrets
            final AppSecrets secrets = builder1.withKey(appKey)
                    .withHiddenSecret("p1", "secret-1")
                    .withHiddenSecret("p2", "secret-2")
                    .build();
            //Save it
            api.saveSecrets(secrets, site, admin);
            final Optional<AppSecrets> secretsOptional = api.getSecrets(appKey, site, admin);
            Assert.assertTrue(secretsOptional.isPresent());

            //AES only supports security Key of sizes of 16, 24 or 32 bytes.
            final String password = RandomStringUtils.randomAlphanumeric(32);
            final Key securityKey = AppsUtil.generateKey(password);

            //Now that we have a valid key lets dump our selection of secrets
            final Map<String, Set<String>> appKeysBySite = ImmutableMap
                    .of(site.getIdentifier(), ImmutableSet.of(appKey));
            final Path exportSecretsFile = api
                    .exportSecrets(securityKey, false, appKeysBySite, admin);
            assertTrue(exportSecretsFile.toFile().exists());

            //Remove the secret we dumped we can re import it.
            api.deleteSecrets(appKey, site, admin);

            //Now we're gonna create an inconsistency removing the app descriptor and then try to import the secret.
            final HostAPI hostAPI = APILocator.getHostAPI();
            hostAPI.archive(site, admin, false);
            hostAPI.delete(site, admin, false);

            //and finally import it
            api.importSecretsAndSave(exportSecretsFile, securityKey, admin);
        }finally {
            file.delete();
        }
    }

    /**
     * Method to test {@link AppsAPIImpl#exportSecrets(Key, boolean, Map, User)} and {@link AppsAPIImpl#collectSecretsForExport(Map, User)}
     * Given scenario: We're testing exporting secrets generated for different sites (one being SYSTEM_HOST)
     * Expected results:  We should be able to recover everything that initially got exported using the "exportAll" param regardless of site.
     * @throws DotDataException
     * @throws IOException
     * @throws AlreadyExistException
     * @throws DotSecurityException
     * @throws EncryptorException
     */
    @Test
    public void Test_Export_All_Secrets_For_System_Non_System_Sites()
            throws DotDataException, IOException, AlreadyExistException, DotSecurityException, EncryptorException {
        final User admin = TestUserUtils.getAdminUser();
        final AppsAPI api = APILocator.getAppsAPI();
        api.resetSecrets(admin);

        final Host site = new SiteDataGen().nextPersisted();
        final Host systemHost = APILocator.getHostAPI().findSystemHost();

        //generate a descriptor
        final AppSecrets.Builder builder1 = new AppSecrets.Builder();
        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("p1", false,  true)
                .stringParam("p2", false,  true)
                .withName("app-example")
                .withDescription("app")
                .withExtraParameters(false);
        final File file = dataGen.nextPersistedDescriptor();
        try {
            api.createAppDescriptor(file, admin);

            final String appKey = dataGen.getKey();
            //generate secrets
            final AppSecrets secrets = builder1.withKey(appKey)
                    .withHiddenSecret("p1", "secret-1")
                    .withHiddenSecret("p2", "secret-2")
                    .build();

            //Save it
            api.saveSecrets(secrets, site, admin);
            final Optional<AppSecrets> siteSecretsOptional = api.getSecrets(appKey, site, admin);

            api.saveSecrets(secrets, systemHost, admin);
            final Optional<AppSecrets> systemHostSecretsOptional = api
                    .getSecrets(appKey, systemHost, admin);

            //AES only supports security Key of sizes of 16, 24 or 32 bytes.
            final String password = RandomStringUtils.randomAlphanumeric(32);
            final Key securityKey = AppsUtil.generateKey(password);

            //Now that we have a valid key lets dump all our secrets.
            final Path exportSecretsFile = api.exportSecrets(securityKey, true, null, admin);
            assertTrue(exportSecretsFile.toFile().exists());

            assertTrue(exportSecretsFile.toFile().exists());

            //Remove the secret we dumped we can re import it.
            api.deleteSecrets(appKey, site, admin);
            api.deleteSecrets(appKey, systemHost, admin);

            //and finally import it
            api.importSecretsAndSave(exportSecretsFile, securityKey, admin);

            //verify
            final Optional<AppSecrets> secretsOptionalPostImport1 = api.getSecrets(appKey, site, admin);
            assertTrue(secretsOptionalPostImport1.isPresent());
            final AppSecrets restoredSecrets1 = secretsOptionalPostImport1.get();

            final Optional<AppSecrets> secretsOptionalPostImport2 = api.getSecrets(appKey, systemHost, admin);
            assertTrue(secretsOptionalPostImport2.isPresent());
            final AppSecrets restoredSecrets2 = secretsOptionalPostImport2.get();

            assertEquals(restoredSecrets1.getKey(),appKey);
            assertEquals(restoredSecrets2.getKey(),appKey);

            assertTrue(restoredSecrets1.getSecrets().containsKey("p1"));
            assertTrue(restoredSecrets2.getSecrets().containsKey("p2"));

        }finally {
           file.delete();
        }

    }

    /**
     * Method to test {@link ExportSecretForm#toString()}
     * Given scenario: We feed the form with null and non null values
     * Expected results: We expect the form to behave NPE Free when calling toString
     */
    @Test
    public void Test_NPE_Free_ToString(){
        final ExportSecretForm formAllNull = new ExportSecretForm(null, false, null);
        assertNotNull(formAllNull.toString());

        final ExportSecretForm formNotAllNull = new ExportSecretForm(null, false, ImmutableMap.of("1", ImmutableSet.of("1","2")));
        assertNotNull(formNotAllNull.toString());
    }

    /**
     * Method to test {@link AppsAPI#appKeysByHost()}}
     * Given scenario: Here we deactivate both indices and then call the method in question that was causing an NPE
     * Expected results: We should get back all the sites even when there are no active indices
     * @throws DotDataException
     * @throws IOException
     * @throws DotSecurityException
     */
    @Test
    public void Test_AppKeyByHost_On_Index_Deactivation()
            throws DotDataException, IOException, DotSecurityException {
        final ContentletIndexAPI contentletIndexAPI = APILocator.getContentletIndexAPI();
        final IndiciesAPI indiciesAPI = APILocator.getIndiciesAPI();
        final IndiciesInfo indiciesInfo = indiciesAPI.loadIndicies();
        try {

            AppSecrets.Builder builder = new AppSecrets.Builder();
            final String appKey = "anyAppKey";
            final AppSecrets bean = builder.withKey(appKey)
                    .withHiddenSecret("mySecret1", "lol")
                    .withHiddenSecret("mySecret2", "lol")
                    .withSecret("boolSecret1", true)
                    .withSecret("boolSecret2", false)
                    .build();

            final Host host = new SiteDataGen().nextPersisted();

            contentletIndexAPI.deactivateIndex(indiciesInfo.getWorking());
            contentletIndexAPI.deactivateIndex(indiciesInfo.getLive());

            final AppsAPI api = APILocator.getAppsAPI();
            api.saveSecrets(bean,host,APILocator.systemUser());
            final Map<String, Set<String>> keysByHost = api.appKeysByHost();

            assertTrue(keysByHost.get(host.getIdentifier()).contains(appKey.toLowerCase()));

        } finally {
            contentletIndexAPI.activateIndex(indiciesInfo.getWorking());
            contentletIndexAPI.activateIndex(indiciesInfo.getLive());
        }
    }

    /**
     * Test maintainHiddenValues method when no existing secrets are present.
     * Given scenario: No existing secrets, new secrets are provided.
     * Expected Result: The new secrets should be returned as-is without modification.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_MaintainHiddenValues_No_Existing_Secrets_Returns_New_Secrets()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsAPIImpl apiImpl = (AppsAPIImpl) api;

        final AppSecrets newSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("secret1", "newValue1")
                .withHiddenSecret("secret2", "newValue2")
                .withSecret("nonHiddenSecret", "value3")
                .build();

        final AppSecrets result = apiImpl.maintainHiddenValues(newSecrets, Optional.empty());

        assertNotNull(result);
        assertEquals("test-app", result.getKey());
        assertEquals(3, result.getSecrets().size());
        assertEquals("newValue1", result.getSecrets().get("secret1").getString());
        assertEquals("newValue2", result.getSecrets().get("secret2").getString());
        assertEquals("value3", result.getSecrets().get("nonHiddenSecret").getString());
    }

    /**
     * Test maintainHiddenValues method when hidden secret has mask value.
     * Given scenario: Existing hidden secret exists, new secret has mask value (******).
     * Expected Result: The existing secret value should be maintained.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_MaintainHiddenValues_Hidden_Secret_With_Mask_Maintains_Existing_Value()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsAPIImpl apiImpl = (AppsAPIImpl) api;

        final AppSecrets existingSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("secret1", "originalValue")
                .withHiddenSecret("secret2", "originalValue2")
                .build();

        final AppSecrets newSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("secret1", SecretViewSerializer.HIDDEN_SECRET_MASK)
                .withHiddenSecret("secret2", "newValue2")
                .build();

        final AppSecrets result = apiImpl.maintainHiddenValues(newSecrets, Optional.of(existingSecrets));

        assertNotNull(result);
        assertEquals("test-app", result.getKey());
        assertEquals(2, result.getSecrets().size());
        // secret1 should maintain original value because it came in as masked
        assertEquals("originalValue", result.getSecrets().get("secret1").getString());
        assertTrue(result.getSecrets().get("secret1").getHidden());
        // secret2 should have new value because it came in with actual value
        assertEquals("newValue2", result.getSecrets().get("secret2").getString());
        assertTrue(result.getSecrets().get("secret2").getHidden());
    }

    /**
     * Test maintainHiddenValues method with mixed scenarios.
     * Given scenario: Multiple secrets with different states (masked, new values, non-hidden).
     * Expected Result: Each secret should be handled according to its state.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_MaintainHiddenValues_Mixed_Scenarios()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsAPIImpl apiImpl = (AppsAPIImpl) api;

        final AppSecrets existingSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("hiddenSecret1", "existingValue1")
                .withHiddenSecret("hiddenSecret2", "existingValue2")
                .withSecret("nonHiddenSecret", "existingValue3")
                .build();

        final AppSecrets newSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("hiddenSecret1", SecretViewSerializer.HIDDEN_SECRET_MASK)  // Should maintain existing
                .withHiddenSecret("hiddenSecret2", "updatedValue2")  // Should use new value
                .withSecret("nonHiddenSecret", "updatedValue3")  // Should use new value
                .withHiddenSecret("newSecret", "newValue4")  // Should use new value (no existing)
                .build();

        final AppSecrets result = apiImpl.maintainHiddenValues(newSecrets, Optional.of(existingSecrets));

        assertNotNull(result);
        assertEquals("test-app", result.getKey());
        assertEquals(4, result.getSecrets().size());

        // hiddenSecret1 should maintain existing value (came in as masked)
        assertEquals("existingValue1", result.getSecrets().get("hiddenSecret1").getString());
        assertTrue(result.getSecrets().get("hiddenSecret1").getHidden());

        // hiddenSecret2 should have new value (came in with actual value)
        assertEquals("updatedValue2", result.getSecrets().get("hiddenSecret2").getString());
        assertTrue(result.getSecrets().get("hiddenSecret2").getHidden());

        // nonHiddenSecret should have new value
        assertEquals("updatedValue3", result.getSecrets().get("nonHiddenSecret").getString());
        assertFalse(result.getSecrets().get("nonHiddenSecret").getHidden());

        // newSecret should have new value (no existing secret with this key)
        assertEquals("newValue4", result.getSecrets().get("newSecret").getString());
        assertTrue(result.getSecrets().get("newSecret").getHidden());
    }

    /**
     * Test maintainHiddenValues method when hidden secret has mask but no existing secret.
     * Given scenario: New hidden secret with mask value but no existing secret with that key.
     * Expected Result: The masked value should be used (no existing value to fall back to).
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_MaintainHiddenValues_Masked_Secret_Without_Existing_Uses_Mask()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsAPIImpl apiImpl = (AppsAPIImpl) api;

        final AppSecrets existingSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("existingSecret", "existingValue")
                .build();

        final AppSecrets newSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("newSecret", SecretViewSerializer.HIDDEN_SECRET_MASK)
                .build();

        final AppSecrets result = apiImpl.maintainHiddenValues(newSecrets, Optional.of(existingSecrets));

        assertNotNull(result);
        assertEquals("test-app", result.getKey());
        assertEquals(1, result.getSecrets().size());
        // newSecret should have mask value because no existing secret with that key exists
        assertEquals(SecretViewSerializer.HIDDEN_SECRET_MASK, result.getSecrets().get("newSecret").getString());
        assertTrue(result.getSecrets().get("newSecret").getHidden());
    }

    /**
     * Test maintainHiddenValues method with all secrets being maintained.
     * Given scenario: All secrets are hidden and masked, existing values exist for all.
     * Expected Result: All existing secret values should be maintained.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_MaintainHiddenValues_All_Secrets_Maintained()
            throws DotDataException, DotSecurityException {
        final AppsAPI api = APILocator.getAppsAPI();
        final AppsAPIImpl apiImpl = (AppsAPIImpl) api;

        final AppSecrets existingSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("secret1", "originalValue1")
                .withHiddenSecret("secret2", "originalValue2")
                .withHiddenSecret("secret3", "originalValue3")
                .build();

        final AppSecrets newSecrets = new AppSecrets.Builder()
                .withKey("test-app")
                .withHiddenSecret("secret1", SecretViewSerializer.HIDDEN_SECRET_MASK)
                .withHiddenSecret("secret2", SecretViewSerializer.HIDDEN_SECRET_MASK)
                .withHiddenSecret("secret3", SecretViewSerializer.HIDDEN_SECRET_MASK)
                .build();

        final AppSecrets result = apiImpl.maintainHiddenValues(newSecrets, Optional.of(existingSecrets));

        assertNotNull(result);
        assertEquals("test-app", result.getKey());
        assertEquals(3, result.getSecrets().size());
        assertEquals("originalValue1", result.getSecrets().get("secret1").getString());
        assertEquals("originalValue2", result.getSecrets().get("secret2").getString());
        assertEquals("originalValue3", result.getSecrets().get("secret3").getString());
        assertTrue(result.getSecrets().get("secret1").getHidden());
        assertTrue(result.getSecrets().get("secret2").getHidden());
        assertTrue(result.getSecrets().get("secret3").getHidden());
    }


}
