package com.dotcms.auth.providers.saml.v1;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.saml.Attributes;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlName;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
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
        public void authentication(HttpServletRequest request, HttpServletResponse response, IdentityProviderConfiguration identityProviderConfiguration) {

        }

        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response, Object nameID, String sessionIndexValue, IdentityProviderConfiguration identityProviderConfiguration) {

        }

        @Override
        public Attributes resolveAttributes(HttpServletRequest request, HttpServletResponse response, IdentityProviderConfiguration identityProviderConfiguration) {
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
        final SAMLHelper           		samlHelper                        = new SAMLHelper(samlAuthenticationService);
        final String nativeLastName     = "For SAML";
        final String nativeFirstName    = "Native User";
        final String nativeEmailAddress = "native.user" + UUID.randomUUID() +  "@dotcms.com";

        final String samlNameId         = "-" + UUID.randomUUID(); // invalid id
        final String samlLastName       = "For SAML";
        final String samlFirstName      = "Native User";
        final String samlEmailAddress   = "saml.user" + UUID.randomUUID() +  "@dotcms.com";

        //// create necessary mocks
        // no want to sync roles
        when(identityProviderConfiguration.containsOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(true);
        when(identityProviderConfiguration.getOptionalProperty(SamlName.DOTCMS_SAML_BUILD_ROLES.getPropertyName())).thenReturn(DotSamlConstants.DOTCMS_SAML_BUILD_ROLES_NONE_VALUE);

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
}
