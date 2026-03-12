package com.dotmarketing.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests for {@link Host#getAbsoluteBaseUrl()}.
 *
 * <p>These tests do not require a running dotCMS instance.  All API dependencies are mocked via
 * Mockito's {@code mockStatic} facility.  Host instances are created using the
 * {@link Host#Host(Contentlet)} constructor to avoid the {@code CacheLocator} call present in
 * the no-arg constructor.</p>
 */
class HostAbsoluteBaseUrlTest {

    private MockedStatic<APILocator> mockedAPILocator;
    private IdentifierAPI identifierAPI;
    private HostAPI hostAPI;
    private User systemUser;

    @BeforeEach
    void setUp() {
        identifierAPI = mock(IdentifierAPI.class);
        hostAPI = mock(HostAPI.class);
        systemUser = mock(User.class);

        mockedAPILocator = mockStatic(APILocator.class);
        mockedAPILocator.when(APILocator::getIdentifierAPI).thenReturn(identifierAPI);
        mockedAPILocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        mockedAPILocator.when(APILocator::systemUser).thenReturn(systemUser);
    }

    @AfterEach
    void tearDown() {
        mockedAPILocator.close();
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link Host} using the {@link Host#Host(Contentlet)} copy constructor so that the
     * {@code CacheLocator} dependency in the no-arg constructor is bypassed.
     */
    private static Host createHost(final String identifier, final String hostname) {
        final Contentlet contentlet = new Contentlet();
        final Host host = new Host(contentlet);
        if (identifier != null) {
            host.setIdentifier(identifier);
        }
        host.setHostname(hostname);
        return host;
    }

    /** Creates a minimal {@link Identifier} populated with the given values. */
    private static Identifier makeIdentifier(final String id,
                                             final String hostId,
                                             final String parentPath,
                                             final String assetName) {
        final Identifier ident = new Identifier(id);
        ident.setHostId(hostId);
        ident.setParentPath(parentPath);
        ident.setAssetName(assetName);
        return ident;
    }

    // -----------------------------------------------------------------------
    // Tests: edge cases
    // -----------------------------------------------------------------------

    @Test
    void systemHostReturnsEmptyString() {
        final Host sysHost = createHost("SYSTEM_HOST", "System Host");
        sysHost.setSystemHost(true);

        assertEquals("", sysHost.getAbsoluteBaseUrl(),
                "System Host should return empty string");
    }

    @Test
    void noIdentifierFallsBackToHttpsHostname() {
        // Host with a null/blank identifier (not yet persisted)
        final Host host = createHost(null, "example.com");

        assertEquals("https://example.com", host.getAbsoluteBaseUrl(),
                "Unpersisted host should fall back to simple https URL");
    }

    // -----------------------------------------------------------------------
    // Tests: top-level host
    // -----------------------------------------------------------------------

    @Test
    void topLevelHostReturnsSimpleHttpsUrl() throws Exception {
        final String id = "top-level-uuid";
        final Host host = createHost(id, "dotcms.com");
        final Identifier ident = makeIdentifier(id, Host.SYSTEM_HOST, "/", "dotcms.com");

        when(identifierAPI.find(id)).thenReturn(ident);

        assertEquals("https://dotcms.com", host.getAbsoluteBaseUrl(),
                "Top-level host must return https:// + hostname only");
    }

    @Test
    void topLevelHostWithNullParentIdFallsBackToSimpleUrl() throws Exception {
        final String id = "top-id-null-parent";
        final Host host = createHost(id, "example.org");
        final Identifier ident = makeIdentifier(id, null, "/", "example.org");

        when(identifierAPI.find(id)).thenReturn(ident);

        assertEquals("https://example.org", host.getAbsoluteBaseUrl(),
                "Top-level host with null parentId must return simple https URL");
    }

    // -----------------------------------------------------------------------
    // Tests: single-level nesting
    // -----------------------------------------------------------------------

    @Test
    void nestedHostDirectlyUnderTopLevelWithRootParentPath() throws Exception {
        // nestedHost.identifier.hostId = topLevelHost, parentPath = /
        final String topId = "top-id";
        final String nestedId = "nested-id";

        final Host topHost = createHost(topId, "dotcms.com");
        final Host nestedHost = createHost(nestedId, "shop");

        final Identifier topIdent = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");
        final Identifier nestedIdent = makeIdentifier(nestedId, topId, "/", "shop");

        when(identifierAPI.find(nestedId)).thenReturn(nestedIdent);
        when(identifierAPI.find(topId)).thenReturn(topIdent);
        when(hostAPI.find(topId, systemUser, false)).thenReturn(topHost);

        assertEquals("https://dotcms.com/shop", nestedHost.getAbsoluteBaseUrl(),
                "Host nested directly under top-level (parentPath=/) must produce /hostname");
    }

    @Test
    void nestedHostUnderFolderInTopLevel() throws Exception {
        // nestedHost.identifier.hostId = topLevelHost, parentPath = /en/
        final String topId = "top-id";
        final String nestedId = "nested-id";

        final Host topHost = createHost(topId, "dotcms.com");
        final Host nestedHost = createHost(nestedId, "blog");

        final Identifier topIdent = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");
        final Identifier nestedIdent = makeIdentifier(nestedId, topId, "/en/", "blog");

        when(identifierAPI.find(nestedId)).thenReturn(nestedIdent);
        when(identifierAPI.find(topId)).thenReturn(topIdent);
        when(hostAPI.find(topId, systemUser, false)).thenReturn(topHost);

        assertEquals("https://dotcms.com/en/blog", nestedHost.getAbsoluteBaseUrl(),
                "Host nested under /en/ folder must produce /en/hostname URL");
    }

    // -----------------------------------------------------------------------
    // Tests: spec example
    // -----------------------------------------------------------------------

    @Test
    void specExampleParentPathEnNestedHost1() throws Exception {
        // From spec §4.2:
        //   parentPath = /en/nestedHost1/
        //   hostname   = nestedHost2
        //   top domain = dotcms.com
        //   result     = https://dotcms.com/en/nestedHost1/nestedHost2
        final String topId = "top-id";
        final String nestedId = "nested2-id";

        final Host topHost = createHost(topId, "dotcms.com");
        final Host nestedHost2 = createHost(nestedId, "nestedHost2");

        final Identifier topIdent = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");
        final Identifier nestedIdent = makeIdentifier(nestedId, topId, "/en/nestedHost1/", "nestedHost2");

        when(identifierAPI.find(nestedId)).thenReturn(nestedIdent);
        when(identifierAPI.find(topId)).thenReturn(topIdent);
        when(hostAPI.find(topId, systemUser, false)).thenReturn(topHost);

        assertEquals("https://dotcms.com/en/nestedHost1/nestedHost2",
                nestedHost2.getAbsoluteBaseUrl(),
                "Must match the spec §4.2 example verbatim");
    }

    // -----------------------------------------------------------------------
    // Tests: multi-level host-under-host nesting
    // -----------------------------------------------------------------------

    @Test
    void threeLevelHostNesting() throws Exception {
        // dotcms.com → nestedHost1 → nestedHost2
        // Each parentPath = "/"
        final String topId = "top-id";
        final String nested1Id = "nested1-id";
        final String nested2Id = "nested2-id";

        final Host topHost = createHost(topId, "dotcms.com");
        final Host nestedHost1 = createHost(nested1Id, "nestedHost1");
        final Host nestedHost2 = createHost(nested2Id, "nestedHost2");

        final Identifier topIdent = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");
        final Identifier nested1Ident = makeIdentifier(nested1Id, topId, "/", "nestedHost1");
        final Identifier nested2Ident = makeIdentifier(nested2Id, nested1Id, "/", "nestedHost2");

        when(identifierAPI.find(nested2Id)).thenReturn(nested2Ident);
        when(identifierAPI.find(nested1Id)).thenReturn(nested1Ident);
        when(identifierAPI.find(topId)).thenReturn(topIdent);
        when(hostAPI.find(nested1Id, systemUser, false)).thenReturn(nestedHost1);
        when(hostAPI.find(topId, systemUser, false)).thenReturn(topHost);

        assertEquals("https://dotcms.com/nestedHost1/nestedHost2",
                nestedHost2.getAbsoluteBaseUrl(),
                "Three-level host-under-host nesting must build full path");
    }

    @Test
    void fourLevelMixedNesting() throws Exception {
        // dotcms.com → nestedHost1 (parentPath=/) → nestedHost2 (parentPath=/en/) → nestedHost3
        // nestedHost3.hostId = nestedHost2, parentPath = /en/
        final String topId = "top-id";
        final String nested1Id = "n1-id";
        final String nested2Id = "n2-id";
        final String nested3Id = "n3-id";

        final Host topHost = createHost(topId, "dotcms.com");
        final Host nestedHost1 = createHost(nested1Id, "n1");
        final Host nestedHost2 = createHost(nested2Id, "n2");
        final Host nestedHost3 = createHost(nested3Id, "n3");

        final Identifier topIdent = makeIdentifier(topId, Host.SYSTEM_HOST, "/", "dotcms.com");
        // n1 is under dotcms.com at /
        final Identifier n1Ident = makeIdentifier(nested1Id, topId, "/", "n1");
        // n2 is under n1 at /
        final Identifier n2Ident = makeIdentifier(nested2Id, nested1Id, "/", "n2");
        // n3 is under n2 inside folder /en/
        final Identifier n3Ident = makeIdentifier(nested3Id, nested2Id, "/en/", "n3");

        when(identifierAPI.find(nested3Id)).thenReturn(n3Ident);
        when(identifierAPI.find(nested2Id)).thenReturn(n2Ident);
        when(identifierAPI.find(nested1Id)).thenReturn(n1Ident);
        when(identifierAPI.find(topId)).thenReturn(topIdent);
        when(hostAPI.find(nested2Id, systemUser, false)).thenReturn(nestedHost2);
        when(hostAPI.find(nested1Id, systemUser, false)).thenReturn(nestedHost1);
        when(hostAPI.find(topId, systemUser, false)).thenReturn(topHost);

        // Expected: dotcms.com / n1 / n2 / en/ n3
        assertEquals("https://dotcms.com/n1/n2/en/n3",
                nestedHost3.getAbsoluteBaseUrl(),
                "Four-level mixed nesting must produce correct full path");
    }

    // -----------------------------------------------------------------------
    // Tests: cycle detection
    // -----------------------------------------------------------------------

    @Test
    void cycleInAncestorChainThrowsDotRuntimeException() throws Exception {
        // nestedHost1.hostId → nestedHost2, nestedHost2.hostId → nestedHost1 (cycle)
        final String n1Id = "cycle-n1-id";
        final String n2Id = "cycle-n2-id";

        final Host nestedHost1 = createHost(n1Id, "cycleHost1");
        final Host nestedHost2 = createHost(n2Id, "cycleHost2");

        final Identifier n1Ident = makeIdentifier(n1Id, n2Id, "/", "cycleHost1");
        final Identifier n2Ident = makeIdentifier(n2Id, n1Id, "/", "cycleHost2");

        when(identifierAPI.find(n1Id)).thenReturn(n1Ident);
        when(identifierAPI.find(n2Id)).thenReturn(n2Ident);
        when(hostAPI.find(n2Id, systemUser, false)).thenReturn(nestedHost2);
        when(hostAPI.find(n1Id, systemUser, false)).thenReturn(nestedHost1);

        assertThrows(DotRuntimeException.class,
                nestedHost1::getAbsoluteBaseUrl,
                "Cycle in ancestor chain must throw DotRuntimeException");
    }
}
