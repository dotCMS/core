package com.dotcms.auth.providers.saml.v1;

import com.dotcms.IntegrationTestBase;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DotSamlResourceTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

    }

    /**
     * Method to test: testing the metadata of the DotSamlResource
     * Given Scenario: the test creates a limited user and call the saml metadata
     * ExpectedResult: Must throw a security exception
     *
     */
    @Test(expected = SecurityException.class)
    public void testMetadata() throws DotDataException, DotSecurityException, IOException {

        final User limitedUser = mock(User.class);
        final SamlConfigurationService  samlConfigurationService  = mock(SamlConfigurationService.class);
        final SAMLHelper           		samlHelper                = mock(SAMLHelper.class);
        final SamlAuthenticationService samlAuthenticationService = mock(SamlAuthenticationService.class);
        final IdentityProviderConfigurationFactory identityProviderConfigurationFactory = mock(IdentityProviderConfigurationFactory.class);
        final WebResource webResource = new WebResource();
        final HttpServletRequest httpServletRequest   = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        when(httpServletRequest.getAttribute(WebKeys.USER)).thenReturn(limitedUser);

        new DotSamlResource(samlConfigurationService, samlHelper, samlAuthenticationService, identityProviderConfigurationFactory, webResource)
                .metadata("123456", httpServletRequest, httpServletResponse);
    }
}
