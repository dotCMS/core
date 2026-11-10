package com.dotmarketing.sitesearch.ajax;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.liferay.portal.model.User;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Request login credentials must not be retained in scheduled or immediately executed job data. */
public class SiteSearchScheduledCredentialsTest extends UnitTestBase {

    /** Preserves legitimate scalar/array job settings while dropping every supported login alias. */
    @Test
    public void authenticationParametersNeverReachJobConfiguration() throws Exception {
        for (final boolean runNow : new boolean[]{false, true}) {
            final Map<String, String[]> params = new LinkedHashMap<>();
            params.put("QUARTZ_JOB_NAME", new String[]{"test-site-search"});
            params.put("INDEX_NAME", new String[]{"test-index"});
            params.put("RUN_NOW", new String[]{Boolean.toString(runNow)});
            params.put("langToIndex", new String[]{"en"});
            final Set<String> authenticationKeys = Set.of("u", "p", "user", "passwd", "password");
            authenticationKeys.forEach(key -> params.put(key, new String[]{"credential", "another-value"}));
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getParameterMap()).thenReturn(params);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            final SiteSearchAPI api = mock(SiteSearchAPI.class);
            final LanguageAPI languages = mock(LanguageAPI.class);
            final Language language = mock(Language.class);
            when(language.getId()).thenReturn(1L);
            when(languages.getDefaultLanguage()).thenReturn(language);
            final SiteSearchAjaxAction action = mock(SiteSearchAjaxAction.class, CALLS_REAL_METHODS);
            final User user = mock(User.class);
            when(user.getUserId()).thenReturn("job-owner");
            when(action.getUser()).thenReturn(user);
            try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                    MockedStatic<ActivityLogger> activity = mockStatic(ActivityLogger.class);
                    MockedStatic<AdminLogger> admin = mockStatic(AdminLogger.class)) {
                apis.when(APILocator::getSiteSearchAPI).thenReturn(api);
                apis.when(APILocator::getLanguageAPI).thenReturn(languages);

                action.scheduleJob(request, response);

                final ArgumentCaptor<SiteSearchConfig> config = ArgumentCaptor.forClass(SiteSearchConfig.class);
                if (runNow) {
                    verify(api).executeTaskNow(config.capture());
                } else {
                    verify(api).scheduleTask(config.capture());
                }
                assertEquals("test-site-search", config.getValue().getJobName());
                assertEquals("test-index", config.getValue().getIndexName());
                assertArrayEquals(new String[]{"en"}, (String[]) config.getValue().get("langToIndex"));
                for (final String key : authenticationKeys) {
                    assertFalse("Job data must not retain " + key, config.getValue().containsKey(key));
                }
            }
        }
    }
}
