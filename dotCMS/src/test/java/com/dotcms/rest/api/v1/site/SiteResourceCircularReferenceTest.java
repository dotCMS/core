package com.dotcms.rest.api.v1.site;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.util.PaginationUtil;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.contentlet.business.HostFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

/**
 * Unit tests for the circular-reference validation logic in {@link SiteResource}.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>New sites (no existing identifier) always pass validation (no descendants yet)</li>
 *   <li>A direct self-reference as an ancestor is rejected</li>
 *   <li>A transitive cycle (proposed parent is a descendant of the current site) is rejected</li>
 *   <li>A valid, non-circular parent is accepted</li>
 *   <li>A {@link FactoryLocator} failure defaults to graceful pass (no false positives)</li>
 * </ul>
 *
 * <p>The private {@code validateNoCircularReference} method is accessed via reflection so that
 * the test can verify its behavior without exposing it in the public API.  A {@link Host} mock
 * is used to avoid the CDI / DB dependencies of the real {@link Host} constructor.
 */
public class SiteResourceCircularReferenceTest {

    @Mock
    private HostFactory mockHostFactory;

    private SiteResource siteResource;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use the @VisibleForTesting constructor to avoid SiteHelper.getInstance() side-effects.
        siteResource = new SiteResource(
                mock(WebResource.class),
                mock(SiteHelper.class),
                mock(PaginationUtil.class));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Invokes {@code SiteResource.validateNoCircularReference} via reflection.
     *
     * @param proposedParentHostId the identifier of the proposed parent host
     * @param proposedParentLabel  human-readable label for error messages
     * @param site                 the site being created/updated (may be a mock)
     * @throws Throwable the underlying exception thrown by the method (unwrapped from
     *                   {@link java.lang.reflect.InvocationTargetException})
     */
    private void invokeValidateNoCircularReference(final String proposedParentHostId,
            final String proposedParentLabel, final Host site) throws Throwable {
        final Method method = SiteResource.class.getDeclaredMethod(
                "validateNoCircularReference", String.class, String.class, Host.class);
        method.setAccessible(true);
        try {
            method.invoke(siteResource, proposedParentHostId, proposedParentLabel, site);
        } catch (final java.lang.reflect.InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    /** Creates a mock {@link Host} with the given identifier and hostname. */
    private static Host mockSite(final String identifier, final String hostname) {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(identifier);
        when(site.getHostname()).thenReturn(hostname);
        return site;
    }

    // -----------------------------------------------------------------------
    // Test: new site (no identifier) always passes
    // -----------------------------------------------------------------------

    /**
     * For a brand-new site whose identifier is blank, no cycle can exist yet.
     * The method must return without throwing.
     */
    @Test
    public void testNewSite_noIdentifier_alwaysPasses() throws Throwable {
        // identifier not set → getIdentifier() returns null
        final Host newSite = mockSite(null, "new.example.com");

        // No database calls should be needed; the method should return early.
        invokeValidateNoCircularReference("some-parent-id", "parentLabel", newSite);
        // Reaching here means validation correctly skipped the check.
    }

    /**
     * A blank identifier string is also treated as a brand-new site — validation passes.
     */
    @Test
    public void testNewSite_emptyIdentifier_alwaysPasses() throws Throwable {
        final Host newSite = mockSite("", "new.example.com");

        invokeValidateNoCircularReference("some-parent-id", "parentLabel", newSite);
    }

    // -----------------------------------------------------------------------
    // Test: direct self-reference as ancestor is rejected
    // -----------------------------------------------------------------------

    /**
     * When the proposed parent host ID equals the site's own identifier, the method must
     * throw {@link IllegalArgumentException}.
     */
    @Test
    public void testSelfReferenceAncestor_throwsIllegalArgument() throws Throwable {
        final Host site = mockSite("site-uuid-123", "example.com");

        try {
            invokeValidateNoCircularReference("site-uuid-123", "example.com", site);
            fail("Expected IllegalArgumentException for self-reference ancestor");
        } catch (final IllegalArgumentException e) {
            assertTrue("Error message must mention circular host reference",
                    e.getMessage().contains("circular host reference"));
        }
    }

    // -----------------------------------------------------------------------
    // Test: transitive cycle – proposed parent is descendant of site
    // -----------------------------------------------------------------------

    /**
     * When the proposed parent is already a descendant of the current site (transitive cycle),
     * the method must throw {@link IllegalArgumentException}.
     *
     * <p>Example: A → B → C → A would be detected when validating whether C can be reparented
     * under A, because C is already a descendant of A.
     */
    @Test
    public void testTransitiveCycle_proposedParentIsDescendant_throwsIllegalArgument()
            throws Throwable {
        final String siteId  = "site-a-uuid";
        final String childId = "site-c-uuid";

        final Host site = mockSite(siteId, "site-a.example.com");

        try (MockedStatic<FactoryLocator> factoryLocatorMock = mockStatic(FactoryLocator.class)) {
            factoryLocatorMock.when(FactoryLocator::getHostFactory).thenReturn(mockHostFactory);
            // site-c is already a descendant of site-a → cycle detected
            when(mockHostFactory.isDescendantHost(childId, siteId)).thenReturn(true);

            try {
                invokeValidateNoCircularReference(childId, "site-c.example.com", site);
                fail("Expected IllegalArgumentException for transitive circular reference");
            } catch (final IllegalArgumentException e) {
                assertTrue("Error message must mention circular host reference",
                        e.getMessage().contains("circular host reference"));
                assertTrue("Error message must mention descendant",
                        e.getMessage().contains("descendant"));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test: valid non-circular parent is accepted
    // -----------------------------------------------------------------------

    /**
     * When the proposed parent is NOT a descendant of the current site, the validation must
     * pass without throwing.
     */
    @Test
    public void testValidParent_notDescendant_passes() throws Throwable {
        final String siteId   = "site-a-uuid";
        final String parentId = "site-p-uuid";

        final Host site = mockSite(siteId, "site-a.example.com");

        try (MockedStatic<FactoryLocator> factoryLocatorMock = mockStatic(FactoryLocator.class)) {
            factoryLocatorMock.when(FactoryLocator::getHostFactory).thenReturn(mockHostFactory);
            // parent is NOT a descendant of site → no cycle
            when(mockHostFactory.isDescendantHost(parentId, siteId)).thenReturn(false);

            // Should not throw
            invokeValidateNoCircularReference(parentId, "parent.example.com", site);
        }
    }

    // -----------------------------------------------------------------------
    // Test: FactoryLocator failure defaults to safe pass (no false positives)
    // -----------------------------------------------------------------------

    /**
     * When {@link FactoryLocator#getHostFactory()} throws an unexpected exception, the method
     * must not propagate that exception (graceful degradation).  The {@code Try.getOrElse(false)}
     * pattern means the validation silently allows the operation rather than giving a
     * false-positive rejection.
     */
    @Test
    public void testFactoryLocatorFailure_gracefullyAllows() throws Throwable {
        final String siteId   = "site-a-uuid";
        final String parentId = "site-p-uuid";

        final Host site = mockSite(siteId, "site-a.example.com");

        try (MockedStatic<FactoryLocator> factoryLocatorMock = mockStatic(FactoryLocator.class)) {
            factoryLocatorMock.when(FactoryLocator::getHostFactory)
                    .thenThrow(new RuntimeException("DB unavailable"));

            // Should not throw — degrades gracefully
            invokeValidateNoCircularReference(parentId, "parent.example.com", site);
        }
    }
}
