package com.dotcms.security.apps;

import static com.dotcms.security.apps.ParamDescriptor.newParam;
import static com.dotcms.security.apps.Secret.newSecret;
import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.AppDescriptorDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.PortletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AppsAPIImplTest {

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
        final Secret secret = newSecret("secret-3".toCharArray(), Type.STRING, false);
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
        final Secret secret = newSecret("lol".toCharArray(), Type.STRING, false);

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
        final Map<String, ParamDescriptor> params = of(
        "requiredNoDefault",newParam(null, false, Type.STRING, "any", "hint", true), // This is the rule we intend to break
        "requiredDefault", newParam("default", false, Type.STRING, "any", "hint", true),
        "nonRequiredNoDefault", newParam(null, false, Type.STRING, "any", "hint", false)
         );
        when(descriptor.getParams()).thenReturn(params);
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
        final Map<String, ParamDescriptor> params = of(
                "requiredNoDefault",newParam(null, false, Type.STRING, "any", "hint", true),
                "requiredDefault", newParam("default", false, Type.STRING, "any", "hint", true),
                "nonRequiredNoDefault", newParam(null, false, Type.STRING, "any", "hint", false)
        );
        when(descriptor.getParams()).thenReturn(params);
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
            throws IOException, DotDataException, DotSecurityException {
        Logger.info(AppsAPIImplTest.class, () -> "Evaluating  " + testCase.toString());
        final AppDescriptorDataGen descriptorDataGen = new AppDescriptorDataGen();
        descriptorDataGen.withName(testCase.name).withKey(testCase.key)
                .withExtraParameters(testCase.allowExtraParameters)
                .withDescription(testCase.description).withIconUrl(testCase.iconUrl);
        for (final Map.Entry<String, ParamDescriptor> entry : testCase.params.entrySet()) {
            descriptorDataGen.param(entry.getKey(), entry.getValue());
        }
        try (final InputStream inputStream = descriptorDataGen.nextPersistedDescriptor()) {
            final AppsAPI api = APILocator.getAppsAPI();
            final User admin = TestUserUtils.getAdminUser();
            return api.createAppDescriptor(inputStream, admin);
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
            throws IOException, DotDataException, DotSecurityException {
        assertNotNull(evaluateAppTestCase(testCase));
    }

    @DataProvider
    public static Object[] getExpectedExceptionTestCases() throws Exception {
        final Map<String, ParamDescriptor> emptyParams = ImmutableMap.of();
        return new Object[]{
                //The following test that the general required fields are mandatory.
                new AppTestCase("", "", "", "", false, emptyParams),
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
                       ImmutableSortedMap.of("p1", newParam("", true, null, "", "", true)
                )),
                //Hidden bool param is not allowed.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of("p1", newParam("", true, Type.BOOL, "label", "hint", true)
                )),
                //emptyLabel.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of("p1", newParam("v1", true, Type.STRING, "", "", true)
                )),
                //emptyHint.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of("p1", newParam("v1", true, Type.STRING, "label", "", true)
                )),
                //Required param with null default.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                       ImmutableSortedMap.of("p1", newParam("null", false, Type.STRING, "label", "hint", true)
                )),
                //non parsable to bool string.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of("p1", newParam("lol", false, Type.BOOL, "label", "hint", true)
                )),
                //Null hidden to emulate missing hidden field.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1", newParam("false", null, Type.BOOL, "label", "hint", true)
                        )),
                //Null required to emulate missing hidden field.
                new AppTestCase("any-key", "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1", newParam("false", false, Type.BOOL, "label", "hint", null)
                        ))
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
            throws IOException, DotDataException, DotSecurityException {
        assertNotNull(evaluateAppTestCase(testCase));
    }


    @DataProvider
    public static Object[] getValidExceptionFreeTestCases() throws Exception {
        final long postfix = System.currentTimeMillis();
        return new Object[]{
                new AppTestCase("key1_" + postfix, "any-name", "desc", "icon", true,
                        ImmutableSortedMap.of(
                                "p1", newParam("", true, Type.STRING, "label", "hint", true)
                        )),
                new AppTestCase("key2_" + postfix, "any-name", "desc", "icon", true,
                        ImmutableSortedMap.of(
                                "p1", newParam("", true, Type.STRING, "label", "hint", false)
                        )),
                new AppTestCase("key3_" + postfix, "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1", newParam("true", false, Type.BOOL, "label", "hint", true)
                        )),
                new AppTestCase("key4_" + postfix, "any-name", "desc", "icon", false,
                        ImmutableSortedMap.of(
                                "p1", newParam("false", false, Type.BOOL, "label", "hint", true)
                        ))
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

}
