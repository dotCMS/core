package com.dotmarketing.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Pure unit tests verifying that {@link IdentifierAPIImpl#getTopLevelHostId(Host)} throws
 * {@link DotRuntimeException} when the ancestor chain contains a cycle (AC 20).
 *
 * <p>All external dependencies ({@link APILocator}, {@link FactoryLocator}) are mocked via
 * Mockito's {@code mockStatic} facility so that no running dotCMS container is required.
 *
 * <p>The test matrix covers:
 * <ul>
 *   <li>Direct two-host cycle: A → B → A</li>
 *   <li>Three-host transitive cycle: A → B → C → A</li>
 *   <li>Self-referential cycle: A → A</li>
 *   <li>Normal top-level host (SYSTEM_HOST parent): returns own identifier</li>
 *   <li>Normal two-level chain (nested host): returns root identifier</li>
 *   <li>Normal three-level chain: returns root identifier</li>
 * </ul>
 */
class IdentifierAPIImplCycleDetectionTest {

    private MockedStatic<APILocator>     mockedAPILocator;
    private MockedStatic<FactoryLocator> mockedFactoryLocator;

    private IdentifierFactory identifierFactory;
    private IdentifierAPIImpl identifierAPI;

    @BeforeEach
    void setUp() {
        identifierFactory = mock(IdentifierFactory.class);

        mockedAPILocator     = mockStatic(APILocator.class);
        mockedFactoryLocator = mockStatic(FactoryLocator.class);

        mockedAPILocator.when(APILocator::getContentletAPI).thenReturn(mock(ContentletAPI.class));
        mockedFactoryLocator.when(FactoryLocator::getIdentifierFactory)
                .thenReturn(identifierFactory);

        identifierAPI = new IdentifierAPIImpl();
    }

    @AfterEach
    void tearDown() {
        mockedAPILocator.close();
        mockedFactoryLocator.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates a minimal {@link Host} backed by a {@link Contentlet} to avoid the
     *  {@link Host#Host()} no-arg constructor which requires the dotCMS container. */
    private static Host createHost(final String identifier, final String hostname) {
        final Contentlet c = new Contentlet();
        final Host h = new Host(c);
        h.setIdentifier(identifier);
        h.setHostname(hostname);
        return h;
    }

    /** Creates a minimal {@link Identifier} with the given id and hostId. */
    private static Identifier makeIdentifier(final String id, final String hostId) {
        final Identifier ident = new Identifier(id);
        ident.setHostId(hostId);
        return ident;
    }

    // -----------------------------------------------------------------------
    // Cycle detection tests — must throw DotRuntimeException
    // -----------------------------------------------------------------------

    /**
     * Direct two-host cycle: A's hostId points to B, and B's hostId points back to A.
     * The traversal must detect the repeated identifier and throw {@link DotRuntimeException}.
     */
    @Test
    void directTwoHostCycle_throwsDotRuntimeException() throws Exception {
        final String aId = "host-a-uuid";
        final String bId = "host-b-uuid";

        final Identifier aIdent = makeIdentifier(aId, bId); // A → B
        final Identifier bIdent = makeIdentifier(bId, aId); // B → A  (cycle!)

        when(identifierFactory.find(aId)).thenReturn(aIdent);
        when(identifierFactory.find(bId)).thenReturn(bIdent);

        final Host hostA = createHost(aId, "host-a.example.com");

        final DotRuntimeException thrown = assertThrows(
                DotRuntimeException.class,
                () -> identifierAPI.getTopLevelHostId(hostA),
                "A two-host cycle must throw DotRuntimeException");

        // The exception message must mention the word "Cycle" (case-insensitive) and the ID.
        final String message = thrown.getMessage().toLowerCase();
        assert message.contains("cycle") : "Exception message must mention 'cycle'";
    }

    /**
     * Three-host transitive cycle: A → B → C → A.
     * The method must detect that it has revisited identifier A and throw.
     */
    @Test
    void transitiveThreeHostCycle_throwsDotRuntimeException() throws Exception {
        final String aId = "cycle3-a";
        final String bId = "cycle3-b";
        final String cId = "cycle3-c";

        when(identifierFactory.find(aId)).thenReturn(makeIdentifier(aId, bId)); // A → B
        when(identifierFactory.find(bId)).thenReturn(makeIdentifier(bId, cId)); // B → C
        when(identifierFactory.find(cId)).thenReturn(makeIdentifier(cId, aId)); // C → A (cycle!)

        final Host hostA = createHost(aId, "a.example.com");

        assertThrows(
                DotRuntimeException.class,
                () -> identifierAPI.getTopLevelHostId(hostA),
                "A three-host transitive cycle must throw DotRuntimeException");
    }

    /**
     * Self-referential cycle: A's hostId points to itself.
     */
    @Test
    void selfReferentialCycle_throwsDotRuntimeException() throws Exception {
        final String aId = "self-cycle-uuid";

        // A's parent is itself
        when(identifierFactory.find(aId)).thenReturn(makeIdentifier(aId, aId));

        final Host hostA = createHost(aId, "self.example.com");

        assertThrows(
                DotRuntimeException.class,
                () -> identifierAPI.getTopLevelHostId(hostA),
                "A self-referential cycle must throw DotRuntimeException");
    }

    // -----------------------------------------------------------------------
    // Normal traversal tests — must return correct top-level ID
    // -----------------------------------------------------------------------

    /**
     * A top-level host whose parent is SYSTEM_HOST — the method must return its own identifier.
     */
    @Test
    void topLevelHost_returnsOwnIdentifier() throws Exception {
        final String topId = "top-level-uuid";

        when(identifierFactory.find(topId))
                .thenReturn(makeIdentifier(topId, Host.SYSTEM_HOST));

        final Host topHost = createHost(topId, "dotcms.com");

        assertEquals(topId, identifierAPI.getTopLevelHostId(topHost),
                "A top-level host must return its own identifier");
    }

    /**
     * A top-level host whose {@code hostId} is null — treated the same as SYSTEM_HOST.
     */
    @Test
    void topLevelHostWithNullParent_returnsOwnIdentifier() throws Exception {
        final String topId = "top-null-parent-uuid";

        when(identifierFactory.find(topId))
                .thenReturn(makeIdentifier(topId, null));

        final Host topHost = createHost(topId, "example.org");

        assertEquals(topId, identifierAPI.getTopLevelHostId(topHost),
                "A host with null hostId must be treated as top-level");
    }

    /**
     * Two-level chain: nested host → top-level host → SYSTEM_HOST.
     * The method must return the top-level host's identifier.
     */
    @Test
    void twoLevelChain_returnsTopLevelHostId() throws Exception {
        final String topId    = "top-2level-uuid";
        final String nestedId = "nested-2level-uuid";

        when(identifierFactory.find(nestedId))
                .thenReturn(makeIdentifier(nestedId, topId));
        when(identifierFactory.find(topId))
                .thenReturn(makeIdentifier(topId, Host.SYSTEM_HOST));

        final Host nestedHost = createHost(nestedId, "nested.example.com");

        assertEquals(topId, identifierAPI.getTopLevelHostId(nestedHost),
                "A two-level chain must return the top-level host's identifier");
    }

    /**
     * Three-level chain: grandchild → child → top-level → SYSTEM_HOST.
     * The method must return the top-level host's identifier, skipping intermediate nodes.
     */
    @Test
    void threeLevelChain_returnsTopLevelHostId() throws Exception {
        final String topId        = "top-3level-uuid";
        final String nestedId     = "nested-3level-uuid";
        final String grandNestedId = "grand-3level-uuid";

        when(identifierFactory.find(grandNestedId))
                .thenReturn(makeIdentifier(grandNestedId, nestedId));
        when(identifierFactory.find(nestedId))
                .thenReturn(makeIdentifier(nestedId, topId));
        when(identifierFactory.find(topId))
                .thenReturn(makeIdentifier(topId, Host.SYSTEM_HOST));

        final Host grandNestedHost = createHost(grandNestedId, "grand.example.com");

        assertEquals(topId, identifierAPI.getTopLevelHostId(grandNestedHost),
                "A three-level chain must return the top-level host's identifier");
    }

    // -----------------------------------------------------------------------
    // Null / blank host test
    // -----------------------------------------------------------------------

    /**
     * Passing {@code null} as the host must throw {@link DotStateException}.
     */
    @Test
    void nullHost_throwsDotStateException() {
        assertThrows(
                DotStateException.class,
                () -> identifierAPI.getTopLevelHostId(null),
                "A null host must throw DotStateException");
    }
}
