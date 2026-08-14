package com.dotcms.jitsu.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link SiteAuthValidator}'s exception handling, added for issue #36724.
 *
 * Since that issue the App secrets store raises a {@link SecretsStoreUnreadableException} when it
 * cannot be read, instead of silently wiping itself and returning an empty store. This validator sits on the
 * analytics collection request path, so that must surface as an ordinary validation failure rather
 * than a raw runtime exception escaping into the response.
 *
 * The first attempt at that widened the catch to {@code Exception}, which also swallowed
 * {@link AnalyticsValidator.AnalyticsValidationException} -- the pipeline's own signal, raised by
 * {@code ContentAnalyticsUtil} when the site cannot be resolved from the Origin/Referer headers --
 * and re-wrapped it, changing the message callers see. That broke
 * {@code AnalyticsValidatorUtilTest} in CI. Both halves are pinned below.
 *
 * Deliberately a unit test with static mocks rather than an integration test: making the real
 * secrets store unreadable would mean mutating global Config and the SecretsStore singleton, which
 * is exactly the kind of cross-test pollution this PR has already had to clean up.
 */
public class SiteAuthValidatorTest {

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<ContentAnalyticsUtil> analyticsUtil;
    private AppsAPI appsAPI;

    @Before
    public void setUp() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mock(HttpServletRequest.class));

        appsAPI = mock(AppsAPI.class);
        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        apiLocator.when(APILocator::systemUser).thenReturn(null);

        analyticsUtil = mockStatic(ContentAnalyticsUtil.class);
        analyticsUtil.when(() -> ContentAnalyticsUtil.getSiteFromRequest(any()))
                .thenReturn(new Host());
    }

    @After
    public void tearDown() {
        analyticsUtil.close();
        apiLocator.close();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /**
     * Method to test: {@link SiteAuthValidator#validate(Object)}
     * Given Scenario: the App secrets store cannot be read, so getSecrets raises
     * SecretsStoreUnreadableException.
     * Expected Result: an ordinary AnalyticsValidationException carrying INVALID_SITE_AUTH -- never
     * the raw DotRuntimeException, which on this request path would escape into the response.
     */
    @Test
    public void test_unreadableSecretsStore_becomesValidationFailure() throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any()))
                .thenThrow(new SecretsStoreUnreadableException(
                        "Unable to load the App secrets store: the store could not be decrypted",
                        new java.io.IOException("Integrity check failed")));

        try {
            new SiteAuthValidator().validate("any-site-auth");
            fail("an unreadable secrets store must fail validation");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.INVALID_SITE_AUTH, e.getCode());
        } catch (DotRuntimeException e) {
            fail("the raw store failure must not escape onto the request path: " + e);
        }
    }

    /**
     * Method to test: {@link SiteAuthValidator#validate(Object)}
     * Given Scenario: the site cannot be resolved from the Origin/Referer headers, which
     * ContentAnalyticsUtil signals with its own AnalyticsValidationException.
     * Expected Result: that exception passes through untouched -- same instance, same message. The
     * broad catch must not re-wrap it as "Site Auth for Site 'null' could not be verified: ...",
     * which is the regression that failed AnalyticsValidatorUtilTest in CI.
     */
    @Test
    public void test_siteResolutionFailure_passesThroughUnwrapped() {
        final AnalyticsValidator.AnalyticsValidationException raised =
                new AnalyticsValidator.AnalyticsValidationException(
                        "Site could not be retrieved from Origin or Referer HTTP Headers",
                        ValidationErrorCode.INVALID_SITE_AUTH);

        analyticsUtil.when(() -> ContentAnalyticsUtil.getSiteFromRequest(any())).thenThrow(raised);

        try {
            new SiteAuthValidator().validate("any-site-auth");
            fail("an unresolvable site must fail validation");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertSame("the domain exception must be rethrown, not re-wrapped", raised, e);
            assertEquals("Site could not be retrieved from Origin or Referer HTTP Headers",
                    e.getMessage());
        }
    }

    /**
     * Method to test: {@link SiteAuthValidator#validate(Object)}
     * Given Scenario: an unrelated runtime failure -- a database blip or cache error, which dotCMS
     * surfaces as a plain DotRuntimeException.
     * Expected Result: it propagates. The catch names SecretsStoreUnreadableException specifically
     * so a transient infrastructure problem is not quietly converted into "invalid site auth",
     * which is what catching the general wrapper would have done.
     */
    @Test
    public void test_unrelatedRuntimeFailure_isNotMasked() throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any()))
                .thenThrow(new DotRuntimeException("the database went away"));

        try {
            new SiteAuthValidator().validate("any-site-auth");
            fail("expected the unrelated failure to propagate");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            fail("an unrelated infrastructure failure must not be reported as invalid site auth");
        } catch (DotRuntimeException expected) {
            assertEquals("the database went away", expected.getMessage());
        }
    }

    /**
     * Method to test: {@link SiteAuthValidator#validate(Object)}
     * Given Scenario: the store is readable but holds no matching siteAuth.
     * Expected Result: the ordinary "Invalid Site Auth" failure, unchanged by this PR.
     */
    @Test
    public void test_noMatchingSecret_stillFailsValidation() throws Exception {
        when(appsAPI.getSecrets(anyString(), anyBoolean(), any(Host.class), any()))
                .thenReturn(Optional.empty());

        try {
            new SiteAuthValidator().validate("any-site-auth");
            fail("a site auth with no matching secret must fail validation");
        } catch (AnalyticsValidator.AnalyticsValidationException e) {
            assertEquals(ValidationErrorCode.INVALID_SITE_AUTH, e.getCode());
        }
    }
}
