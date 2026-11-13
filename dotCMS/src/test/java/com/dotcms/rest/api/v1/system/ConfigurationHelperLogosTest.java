package com.dotcms.rest.api.v1.system;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.company.CompanyAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.util.PortalUtil;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the logos returned by {@link ConfigurationHelper#getConfigProperties}.
 */
public class ConfigurationHelperLogosTest {

    private static final String ID = "e9f32fc04a4d7a0e7e6a025ee65fcf30";

    /** Stored logos that are not dotAsset paths are returned as "NA". */
    @Test
    public void storedLogosNotDotAssetPaths_areReturnedAsNA() throws Exception {
        final Map<?, ?> logos = logos("/dA/../html/logo.png",
                "/dA/" + ID + "/%2e%2e/%2e%2e/html/logo.png#%zz");

        assertEquals("NA", logos.get(ConfigurationHelper.LOGIN_SCREEN_LOGO));
        assertEquals("NA", logos.get(ConfigurationHelper.NAV_BAR_LOGO));
    }

    /** Stored dotAsset logos are returned with each path segment encoded. */
    @Test
    public void storedDotAssetLogos_areReturnedEncoded() throws Exception {
        final Map<?, ?> logos = logos("/dA/" + ID + "/asset/my logo (1).png",
                "/dA/" + ID + "/asset/50%.png");

        assertEquals("/dA/" + ID + "/asset/my%20logo%20(1).png",
                logos.get(ConfigurationHelper.LOGIN_SCREEN_LOGO));
        assertEquals("/dA/" + ID + "/asset/50%25.png",
                logos.get(ConfigurationHelper.NAV_BAR_LOGO));
    }

    /** On a Community license the navigation bar logo is "NA" even when one is stored. */
    @Test
    public void navBarLogo_onCommunityLicense_isReturnedAsNA() throws Exception {
        final Map<?, ?> logos = logos("/dA/" + ID + "/asset/logo.png",
                "/dA/" + ID + "/asset/nav.png", LicenseLevel.COMMUNITY.level);

        assertEquals("/dA/" + ID + "/asset/logo.png",
                logos.get(ConfigurationHelper.LOGIN_SCREEN_LOGO));
        assertEquals("NA", logos.get(ConfigurationHelper.NAV_BAR_LOGO));
    }

    /**
     * Calls {@link ConfigurationHelper#getConfigProperties} with the given stored logos and a
     * license that shows the navigation bar logo, and returns its logos.
     */
    private static Map<?, ?> logos(final String loginLogo, final String navLogo) throws Exception {
        return logos(loginLogo, navLogo, LicenseLevel.PLATFORM.level);
    }

    /**
     * Same as {@link #logos(String, String)}, with the given license level.
     */
    private static Map<?, ?> logos(final String loginLogo, final String navLogo,
            final int licenseLevel) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class, RETURNS_DEEP_STUBS);
        when(companyAPI.getDefaultCompany().getCity()).thenReturn(loginLogo);
        when(companyAPI.getDefaultCompany().getState()).thenReturn(navLogo);
        when(companyAPI.getDefaultCompany().getSize()).thenReturn("NA");
        when(companyAPI.getDefaultCompany().getType()).thenReturn("NA");
        when(companyAPI.getDefaultCompany().getStreet()).thenReturn("NA");
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        when(languageAPI.getLanguages()).thenReturn(Collections.emptyList());
        final ConfigurationHelper helper = spy(ConfigurationHelper.INSTANCE);
        doReturn(Collections.emptyMap()).when(helper).clusterMap(isNull());

        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
             MockedStatic<PortalUtil> portal = mockStatic(PortalUtil.class);
             MockedStatic<LanguageUtil> messages = mockStatic(LanguageUtil.class);
             MockedStatic<LicenseManager> licenses = mockStatic(LicenseManager.class);
             MockedStatic<LicenseUtil> licenseUtil = mockStatic(LicenseUtil.class);
             MockedStatic<Config> config = mockStatic(Config.class)) {
            apis.when(APILocator::getCompanyAPI).thenReturn(companyAPI);
            apis.when(APILocator::getLanguageAPI).thenReturn(languageAPI);
            licenses.when(LicenseManager::getInstance).thenReturn(mock(LicenseManager.class));
            licenseUtil.when(LicenseUtil::getLevel).thenReturn(licenseLevel);
            licenseUtil.when(LicenseUtil::getDisplayServerId).thenReturn("test-server");
            licenseUtil.when(LicenseUtil::getLevelName).thenReturn("Platform");
            messages.when(() -> LanguageUtil.get(any(Locale.class), anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(1));

            return (Map<?, ?>) helper.getConfigProperties(request, Locale.US)
                    .get(ConfigurationHelper.LOGOS);
        }
    }
}
