package com.dotcms.auth.providers.saml.v1;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.company.CompanyAPI;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SAMLHelper}
 * @author jsanca
 */
public class SAMLHelperTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    private class MockSamlAuthenticationService implements SamlAuthenticationService {


        @Override
        public void initService(Map<String, Object> context) {

        }

        @Override
        public boolean isValidSamlRequest(HttpServletRequest request, HttpServletResponse response, IdentityProviderConfiguration identityProviderConfiguration) {
            return false;
        }

        @Override
        public void authentication(HttpServletRequest request, HttpServletResponse response, IdentityProviderConfiguration identityProviderConfiguration, String relayState) {

        }

        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response, Object nameID, String sessionIndexValue, IdentityProviderConfiguration identityProviderConfiguration) {

        }

        @Override
        public Attributes resolveAttributes(HttpServletRequest request, HttpServletResponse response, IdentityProviderConfiguration identityProviderConfiguration) {
            return null;
        }

        @Override
        public Map<String, String> resolveAllAttributes(final HttpServletRequest request,
                                                        final HttpServletResponse response,
                                                        final IdentityProviderConfiguration identityProviderConfiguration) {
            return null;
        }

            @Override
        public void renderMetadataXML(Writer writer, IdentityProviderConfiguration identityProviderConfiguration) {

        }

        @Override
        public String getValue(Object samlObject) {
            return samlObject.toString();
        }

        @Override
        public List<String> getValues(Object samlObject) {

            return Stream.of((Object[])samlObject).map(Object::toString).collect(Collectors.toList());
        }
    }

    /**
     * Method to test: testing the {@link SAMLHelper#doLogin(HttpServletRequest, HttpServletResponse, IdentityProviderConfiguration, User, LoginServiceAPI)}
     * Given Scenario: tries to log in the admin user
     * ExpectedResult: User must be log in
     *
     */
    @Test()
    public void testLoginUser() throws  SystemException, PortalException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final LoginServiceAPI loginServiceAPI                             = mock(LoginServiceAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final User user = new UserDataGen().nextPersisted();
        when(loginServiceAPI.doCookieLogin(EncryptorFactory.getInstance().getEncryptor().encryptString(user.getUserId()), request, response)).thenReturn(true);
        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestURI()).thenReturn("/dotCMS/login");

        samlHelper.doLogin(request, response, identityProviderConfiguration, user, loginServiceAPI);
        final String userId = PrincipalThreadLocal.getName();
        Assert.assertEquals("the User id on the Principal thread should be the same", userId, user.getUserId());
    }

    /**
     * Method to test: testing the resolveUser of the {@link SAMLHelper}
     * Given Scenario: the test creates two users:
     *                      - one by hand with normal id
     *                      - one from the Helper (which is gonna be with hashed id)
     *                      - them gonna ask for the both
     * ExpectedResult: Must return the users
     *
     */
    @Test()
    public void testResolveUser() throws DotDataException, DotSecurityException, IOException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);
        final String nativeLastName     = "For SAML";
        final String nativeFirstName    = "Native User";
        final String nativeEmailAddress = "native.user" + UUID.randomUUID() +  "@dotcms.com";

        final String samlNameId         = "-" + UUID.randomUUID(); // invalid id
        final String samlLastName       = "For SAML";
        final String samlFirstName      = "Native User";
        final String samlEmailAddress   = "saml.user" + UUID.randomUUID() +  "@dotcms.com";


        //// create necessary mocks

        // wants login by id
        company.setAuthType(Company.AUTH_TYPE_ID);
        when(companyAPI.getDefaultCompany()).thenReturn(company);

        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);

        final SamlConfigurationService samlConfigurationService  = mock(SamlConfigurationService.class);
        SAMLHelper.setThirdPartySamlConfigurationService(samlConfigurationService);

        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);
        
        // creates a new native user
        final User nativeUser = new UserDataGen().active(true).lastName(nativeLastName).firstName(nativeFirstName)
                .emailAddress(nativeEmailAddress).nextPersisted();

        final Attributes nativeUserAttributes = new Attributes.Builder().firstName(nativeFirstName + "Updated")
                .lastName(nativeLastName).nameID(nativeUser.getUserId()).email(nativeEmailAddress).build();

        // recover with SAML the native user
        final User recoveredNativeUser = samlHelper.resolveUser(nativeUserAttributes, identityProviderConfiguration);

        Assert.assertNotNull(recoveredNativeUser);
        Assert.assertEquals (nativeUser.getUserId(),       recoveredNativeUser.getUserId());
        Assert.assertEquals (nativeUser.getEmailAddress(), recoveredNativeUser.getEmailAddress());
        Assert.assertNotEquals(nativeUser.getFirstName(),  recoveredNativeUser.getFirstName());
        Assert.assertEquals (nativeUser.getLastName(),     recoveredNativeUser.getLastName());

        // creates an user from saml
        final Attributes samlUserAttributes = new Attributes.Builder().firstName(samlFirstName)
                .lastName(samlLastName).nameID(samlNameId).email(samlEmailAddress).build();

        // recover with SAML the new saml user
        final User samlUser = samlHelper.resolveUser(samlUserAttributes, identityProviderConfiguration);

        Assert.assertNotNull(samlUser);
        Assert.assertNotEquals (samlNameId,    samlUser.getUserId());  // id should be hashed, so should be diff
        Assert.assertEquals (samlEmailAddress, samlUser.getEmailAddress());
        Assert.assertEquals (samlFirstName,    samlUser.getFirstName());
        Assert.assertEquals (samlLastName,     samlUser.getLastName());

    }

    /**
     * Method to test: testing the resolveUser of the {@link SAMLHelper}
     * Given Scenario: the test creates two users:
     *                      - one by hand
     *                      - one from the Helper, this one will have the same email of the previous one added by hand, but diff user id
     * ExpectedResult: Since the user id is diff, the second user should be saved with a diff id
     *
     */
    @Test()
    public void testResolveUserEmailRepeated() throws DotDataException, DotSecurityException, IOException, NoSuchAlgorithmException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);
        final String nativeLastName     = "For SAML";
        final String nativeFirstName    = "Native User";
        final String email              = "native.user" + UUID.randomUUID() +  "@dotcms.com";
        final String nativeNameId       = "1" + UUID.randomUUID(); // invalid id

        //// create necessary mocks

        // wants login by id
        company.setAuthType(Company.AUTH_TYPE_ID);
        when(companyAPI.getDefaultCompany()).thenReturn(company);

        when(identityProviderConfiguration.containsOptionalProperty(SAMLHelper.ALLOW_USERS_DIFF_ID_REPEATED_EMAIL_KEY)).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);
        when(identityProviderConfiguration.getOptionalProperty(SAMLHelper.ALLOW_USERS_DIFF_ID_REPEATED_EMAIL_KEY)).thenReturn(true);

        final SamlConfigurationService samlConfigurationService  = mock(SamlConfigurationService.class);
        SAMLHelper.setThirdPartySamlConfigurationService(samlConfigurationService);

        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);

        // creates a new native user
        final User nativeUser = new UserDataGen().id(samlHelper.hashIt(nativeNameId)).active(true).lastName(nativeLastName).firstName(nativeFirstName)
                .emailAddress(email).nextPersisted();

        // creates an user from saml
        final Attributes samlUserAttributes = new Attributes.Builder().firstName(nativeFirstName)
                .lastName(nativeLastName).nameID("123" + nativeNameId).email(email).build(); // diff id, same email

        // recover with SAML the new user
        final User recoveredNewUser = samlHelper.resolveUser(samlUserAttributes, identityProviderConfiguration);

        Assert.assertNotNull(recoveredNewUser);
        Assert.assertNotEquals (nativeUser.getUserId(),       recoveredNewUser.getUserId());
        Assert.assertNotEquals (nativeUser.getEmailAddress(), recoveredNewUser.getEmailAddress());
        Assert.assertEquals(nativeUser.getFirstName(),        recoveredNewUser.getFirstName());
        Assert.assertEquals (nativeUser.getLastName(),        recoveredNewUser.getLastName());
        Assert.assertTrue(recoveredNewUser.getEmailAddress().contains(email));
    }


    /**
     * Method to test: testing the resolveUser of the {@link SAMLHelper}
     * Given Scenario: the test creates one user, the login is by email and the name id is an id (not email) so will fallbacks to email
     * ExpectedResult: Must return the user by email
     *
     */
    @Test()
    public void testResolveUser_byEmailAddress() throws DotDataException, DotSecurityException, IOException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);
        final String nativeLastName     = "For SAML";
        final String nativeFirstName    = "Native User";
        final String nativeEmailAddress = "native.user" + UUID.randomUUID() +  "@dotcms.com";

        // wants login by id
        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);

        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);

        final SamlConfigurationService samlConfigurationService  = mock(SamlConfigurationService.class);
        SAMLHelper.setThirdPartySamlConfigurationService(samlConfigurationService);

        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOT_SAML_ALLOW_USER_SYNCHRONIZATION)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsBoolean(identityProviderConfiguration, SamlName.DOTCMS_SAML_LOGIN_UPDATE_EMAIL)).thenReturn(true);
        when(samlConfigurationService.getConfigAsString(identityProviderConfiguration, SamlName.DOTCMS_SAML_BUILD_ROLES)).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);
        // creates a new native user
        final User nativeUser = new UserDataGen().active(true).lastName(nativeLastName).firstName(nativeFirstName)
                .emailAddress(nativeEmailAddress).nextPersisted();

        final Attributes nativeUserAttributes = new Attributes.Builder().firstName(nativeFirstName + "Updated")
                .lastName(nativeLastName).nameID("xxxxxxxxxxx").email(nativeEmailAddress).build();

        // recover with SAML the native user
        final User recoveredNativeUser = samlHelper.resolveUser(nativeUserAttributes, identityProviderConfiguration);

        Assert.assertNotNull(recoveredNativeUser);
        Assert.assertEquals (nativeUser.getEmailAddress(), recoveredNativeUser.getEmailAddress());
        Assert.assertNotEquals(nativeUser.getFirstName(),  recoveredNativeUser.getFirstName());
        Assert.assertEquals (nativeUser.getLastName(),     recoveredNativeUser.getLastName());
    }

    /**
     * Method to test: {@link SAMLHelper#getRoleKeySubstitution(String)}  {@link SAMLHelper#processReplacement(String, Optional)}
     * Given Scenario: sending an null pattern should be doing nothing
     * ExpectedResult: The role does not change
     *
     */
    @Test()
    public void test_getRoleKeySubstitution_processReplacement_doing_null_substitution() throws DotDataException, DotSecurityException, IOException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);

        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);

        final String pattern      = null;
        final Optional<Tuple2<String, String>> substitutionTokenOpt = samlHelper.getRoleKeySubstitution(pattern);

        Assert.assertFalse(substitutionTokenOpt.isPresent());
        final String expected     = "CMS Administrator";

        final String roleResult   = samlHelper.processReplacement(expected, substitutionTokenOpt);

        Assert.assertEquals(expected, roleResult);
    }

    /**
     * Method to test: {@link SAMLHelper#getRoleKeySubstitution(String)}  {@link SAMLHelper#processReplacement(String, Optional)}
     * Given Scenario: sending an substitution pattern should be doing the replacement
     * ExpectedResult: The role will be clean up
     *
     */
    @Test()
    public void test_getRoleKeySubstitution_processReplacement_doing_substitution() throws DotDataException, DotSecurityException, IOException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);

        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);

        final String pattern      = "/_sepsep_/ /";
        final Optional<Tuple2<String, String>> substitutionTokenOpt = samlHelper.getRoleKeySubstitution(pattern);

        Assert.assertTrue(substitutionTokenOpt.isPresent());
        final String tokenRole    = "CMS_sepsep_Administrator";
        final String expected     = "CMS Administrator";

        final String roleResult   = samlHelper.processReplacement(tokenRole, substitutionTokenOpt);

        Assert.assertEquals(expected, roleResult);
    }

    /**
     * Method to test: {@link SAMLHelper#createNewUser(User, Attributes, IdentityProviderConfiguration)}
     * Given Scenario: creates an user only with name id
     * ExpectedResult: The user is created successfully with random values
     *
     */
    @Test()
    public void test_createNewUser_non_Attrs() throws NoSuchAlgorithmException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);

        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final Attributes attrs = new Attributes.Builder().nameID(UUID.randomUUID()).build();

        final User user = samlHelper.createNewUser(APILocator.systemUser(), attrs, identityProviderConfiguration);

        Assert.assertNotNull(user);
        Assert.assertEquals(samlHelper.hashIt(attrs.getNameID().toString()), user.getUserId());
    }

    /**
     * Method to test: {@link SAMLHelper#createNewUser(User, Attributes, IdentityProviderConfiguration)}
     * Given Scenario: creates an user with all properties
     * ExpectedResult: The user is created successfully with random values
     *
     */
    @Test()
    public void test_createNewUser_known_Attrs() throws NoSuchAlgorithmException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);

        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final IdentityProviderConfiguration identityProviderConfiguration = mock(IdentityProviderConfiguration.class);
        final String uuid = UUID.randomUUID().toString();
        final Attributes attrs = new Attributes.Builder().nameID(uuid).email(uuid+"@dotcms.com.cr").firstName("John").lastName("Sn").build();

        final User user = samlHelper.createNewUser(APILocator.systemUser(), attrs, identityProviderConfiguration);

        Assert.assertNotNull(user);
        Assert.assertEquals(samlHelper.hashIt(attrs.getNameID().toString()), user.getUserId());
        Assert.assertEquals(uuid+"@dotcms.com.cr", attrs.getEmail());
        Assert.assertEquals("John", attrs.getFirstName());
        Assert.assertEquals("Sn", attrs.getLastName());
    }

    /**
     * Method to test: {@link SAMLHelper#hashIt(String, boolean)}
     * Given Scenario: creates an uuid, does the hash for one call, and does not the hash for another call
     * ExpectedResult: The hashed call will be diff to the original uuid, the non hashed uuid will be the same of uuid
     *
     */
    @Test()
    public void test_hash_user_id() throws NoSuchAlgorithmException {

        final SamlAuthenticationService samlAuthenticationService         = new MockSamlAuthenticationService();
        final CompanyAPI companyAPI                                       = mock(CompanyAPI.class);
        final Company    company                                          = new Company();
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService, companyAPI);

        company.setAuthType(Company.AUTH_TYPE_EA);
        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final String uuid = UUID.randomUUID().toString();

        final String hashUuid   = samlHelper.hashIt(uuid, true);
        final String nohashUuid = samlHelper.hashIt(uuid, false);

        Assert.assertNotNull(hashUuid);
        Assert.assertNotNull(nohashUuid);
        Assert.assertNotEquals(hashUuid, uuid);
        Assert.assertEquals(nohashUuid, uuid);
    }


}
