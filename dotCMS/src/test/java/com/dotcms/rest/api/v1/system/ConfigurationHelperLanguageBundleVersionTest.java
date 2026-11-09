package com.dotcms.rest.api.v1.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.rest.api.v2.languages.ResponseEntityLanguageMessagesView;
import com.dotcms.util.LanguageBundleVersion;
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

/** Unit coverage of the language bundle identity advertised by app configuration. */
public class ConfigurationHelperLanguageBundleVersionTest {

    /** Configuration and the language response expose the same token outside translations. */
    @Test
    public void configAndLanguageResponseShareBuildVersion() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class, RETURNS_DEEP_STUBS);
        when(companyAPI.getDefaultCompany().getCity()).thenReturn("NA");
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
            licenseUtil.when(LicenseUtil::getDisplayServerId).thenReturn("test-server");
            licenseUtil.when(LicenseUtil::getLevelName).thenReturn("Community");
            messages.when(() -> LanguageUtil.get(any(Locale.class), anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(1));

            final Map<String, Object> english = helper.getConfigProperties(request, Locale.US);
            final Map<String, Object> french = helper.getConfigProperties(request, Locale.FRANCE);
            final ResponseEntityLanguageMessagesView response =
                    new ResponseEntityLanguageMessagesView(Collections.emptyMap());

            assertEquals(LanguageBundleVersion.get(), english.get("languageBundleVersion"));
            assertEquals(english.get("languageBundleVersion"), french.get("languageBundleVersion"));
            assertEquals(english.get("languageBundleVersion"), response.getLanguageBundleVersion());
            assertTrue(response.getLanguageBundleVersion().matches("[0-9a-f]{64}"));
            assertFalse(((Map<?, ?>) english.get(ConfigurationHelper.I18N_MESSAGES_MAP))
                    .containsKey("languageBundleVersion"));
            assertFalse(response.getEntity().containsKey("languageBundleVersion"));
        }
    }
}
