package org.apache.velocity.runtime.parser.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.introspection.ClassMap;
import org.apache.velocity.util.introspection.Introspector;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RecordComponentExecutor} and its wiring into
 * {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}.
 *
 * <p>The suite is deliberately split in two halves:</p>
 *
 * <ul>
 *   <li><strong>What the change adds</strong> — {@code $rec.component} now resolves against a record's
 *       canonical accessor, which previously resolved to nothing and rendered as literal text.</li>
 *   <li><strong>What the change must not touch</strong> — every other resolution strategy in the
 *       chain (bean getter, {@code Map}, {@code get("x")}, {@code isFoo()}) and, most importantly,
 *       the guarantee that a plain class exposing a no-argument {@code foo()} still does
 *       <em>not</em> resolve as {@code $obj.foo}. Widening resolution to arbitrary no-argument
 *       methods would silently change the meaning of existing templates across the product.</li>
 * </ul>
 *
 * @author Fabrizio Araya
 */
public class RecordComponentExecutorTest {

    /** A record with idiomatic (canonical) accessors: {@code id()}, {@code title()}. */
    public record CanonicalRecord(String id, String title, int hits, boolean draft) {}

    /** A record whose components are bean-named, the shape {@code SearchHit} uses today. */
    public record BeanNamedRecord(String getId, String getTitle) {}

    /** Not a record, but exposes a no-arg method named like a record component would be. */
    public static final class NotARecord {

        public String id() {
            return "should-not-resolve";
        }

        public String getTitle() {
            return "bean-getter";
        }

        public boolean isPublished() {
            return true;
        }
    }

    /** Exercises the {@code get("key")} branch of the chain. */
    public static final class HasGenericGet {

        public String get(final String key) {
            return "generic:" + key;
        }
    }

    /** A public record carrying a collection component, walked the way a template would. */
    public record PageRecord(String title, List<String> tags) {}

    /** Package-private on purpose: Velocity cannot introspect a non-public class. */
    record PackagePrivateRecord(String id) {}

    private UberspectImpl uberspect;

    @Before
    public void setUp() {
        uberspect = new UberspectImpl();
        uberspect.init();
    }

    private Object resolve(final Object target, final String identifier) throws Exception {
        final VelPropertyGet getter = uberspect.getPropertyGet(target, identifier, null);
        return getter == null ? null : getter.invoke(target);
    }

    // ---------------------------------------------------------------------
    // What the change adds
    // ---------------------------------------------------------------------

    /**
     * Method to test: {@link RecordComponentExecutor#discover(Class, String)}
     * Given scenario: a record with canonical accessors is referenced as {@code $rec.component}.
     * Expected result: every component resolves to its accessor's value. Before this change the
     * getter was {@code null} and Velocity rendered the reference as literal text.
     */
    @Test
    public void test_canonicalRecordComponents_resolve() throws Exception {
        final CanonicalRecord record = new CanonicalRecord("abc-123", "Hello", 42, true);

        assertEquals("abc-123", resolve(record, "id"));
        assertEquals("Hello", resolve(record, "title"));
        assertEquals(42, resolve(record, "hits"));
        assertEquals(true, resolve(record, "draft"));
    }

    /**
     * Method to test: {@link RecordComponentExecutor#discover(Class, String)}
     * Given scenario: the identifier does not name any component of the record.
     * Expected result: nothing resolves, so Velocity keeps its existing behaviour for unknown
     * references instead of failing.
     */
    @Test
    public void test_unknownComponent_doesNotResolve() throws Exception {
        final CanonicalRecord record = new CanonicalRecord("abc-123", "Hello", 42, true);

        assertNull(uberspect.getPropertyGet(record, "nope", null));
    }

    /**
     * Method to test: {@link RecordComponentExecutor#discover(Class, String)}
     * Given scenario: the reference capitalises the first character ({@code $rec.Title}).
     * Expected result: it resolves, matching the case-flip convenience {@link PropertyExecutor}
     * already offers for bean getters.
     */
    @Test
    public void test_firstCharacterCaseFlip_resolves() throws Exception {
        final CanonicalRecord record = new CanonicalRecord("abc-123", "Hello", 42, true);

        assertEquals("Hello", resolve(record, "Title"));
    }

    /**
     * Method to test: {@link RecordComponentExecutor#discover(Class, String)}
     * Given scenario: a record component whose accessor legitimately returns {@code null}.
     * Expected result: the getter resolves (it is alive) and yields {@code null}, which is different
     * from the reference not resolving at all.
     */
    @Test
    public void test_componentReturningNull_stillResolves() throws Exception {
        final CanonicalRecord record = new CanonicalRecord(null, "Hello", 0, false);

        assertNotNull("the getter itself must resolve", uberspect.getPropertyGet(record, "id", null));
        assertNull(resolve(record, "id"));
    }

    /**
     * Method to test: {@link RecordComponentExecutor#discover(Class, String)}
     * Given scenario: resolution goes through {@link SecureUberspector}, which is the uberspect
     * dotCMS actually configures (see {@code system.properties}).
     * Expected result: it resolves there too, because {@code SecureUberspector} inherits
     * {@code getPropertyGet} from {@link UberspectImpl}.
     */
    @Test
    public void test_secureUberspector_resolvesRecordComponents() throws Exception {
        final RuntimeServices runtimeServices = mock(RuntimeServices.class);
        when(runtimeServices.getConfiguration()).thenReturn(new ExtendedProperties());

        final SecureUberspector secure = new SecureUberspector();
        secure.setRuntimeServices(runtimeServices);
        secure.init();

        final CanonicalRecord record = new CanonicalRecord("abc-123", "Hello", 42, true);
        final VelPropertyGet getter = secure.getPropertyGet(record, "id", null);

        assertNotNull(getter);
        assertEquals("abc-123", getter.invoke(record));
    }

    // ---------------------------------------------------------------------
    // What the change must not touch
    // ---------------------------------------------------------------------

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a plain class (not a record) exposing a no-argument method {@code id()}.
     * Expected result: {@code $obj.id} still does NOT resolve. This is the guardrail of the whole
     * change — resolving arbitrary no-argument methods would silently alter existing templates.
     */
    @Test
    public void test_nonRecordNoArgMethod_stillDoesNotResolve() throws Exception {
        assertNull(uberspect.getPropertyGet(new NotARecord(), "id", null));
    }

    /**
     * Method to test: {@link RecordComponentExecutor#RecordComponentExecutor(Introspector, Class, String)}
     * Given scenario: the executor is handed a class that is not a record.
     * Expected result: it never becomes alive, so the chain falls through to the next strategy.
     */
    @Test
    public void test_executorIsInertForNonRecords() {
        final Introspector introspector = new Introspector();

        assertFalse(new RecordComponentExecutor(introspector, NotARecord.class, "id").isAlive());
        assertTrue(new RecordComponentExecutor(introspector, CanonicalRecord.class, "id").isAlive());
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a bean getter on a plain class.
     * Expected result: unchanged — still resolved by {@link PropertyExecutor}.
     */
    @Test
    public void test_beanGetter_unchanged() throws Exception {
        assertEquals("bean-getter", resolve(new NotARecord(), "title"));
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a boolean {@code isFoo()} accessor.
     * Expected result: unchanged — still resolved by {@link BooleanPropertyExecutor}.
     */
    @Test
    public void test_booleanIsGetter_unchanged() throws Exception {
        assertEquals(true, resolve(new NotARecord(), "published"));
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a {@code Map}, which is how most dotCMS content reaches templates.
     * Expected result: unchanged — still resolved by {@link MapGetExecutor} as a key lookup.
     */
    @Test
    public void test_mapKeyLookup_unchanged() throws Exception {
        assertEquals("mapped", resolve(Map.of("title", "mapped"), "title"));
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a class exposing {@code get(String)}.
     * Expected result: unchanged — still resolved by {@link GetExecutor}.
     */
    @Test
    public void test_genericGet_unchanged() throws Exception {
        assertEquals("generic:title", resolve(new HasGenericGet(), "title"));
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a record whose components are bean-named, the shape {@code SearchHit} uses today.
     * Expected result: {@code $rec.id} keeps resolving through {@link PropertyExecutor}, because the
     * record executor is tried only after it. The already-shipped records are therefore untouched.
     */
    @Test
    public void test_beanNamedRecord_stillResolvesThroughPropertyExecutor() throws Exception {
        final BeanNamedRecord record = new BeanNamedRecord("abc-123", "Hello");

        assertEquals("abc-123", resolve(record, "id"));
        assertEquals("Hello", resolve(record, "title"));

        // The component name itself also resolves now, which is additive: it used to render literally.
        assertEquals("abc-123", resolve(record, "getId"));
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: a record component holding a collection, walked the way a template would.
     * Expected result: the component resolves and the collection is usable downstream.
     */
    @Test
    public void test_recordComponentHoldingCollection_resolves() throws Exception {
        final Object resolved = resolve(new PageRecord("Home", List.of("a", "b")), "tags");

        assertEquals(List.of("a", "b"), resolved);
    }

    /**
     * Method to test: {@link UberspectImpl#getPropertyGet(Object, String, org.apache.velocity.util.introspection.Info)}
     * Given scenario: the record is not {@code public} (package-private, or declared local to a
     * method).
     * Expected result: it still does not resolve, and that is not a shortcoming of this change —
     * {@link ClassMap} only reflects over publicly accessible classes (it checks
     * {@code Modifier.isPublic} on the class before collecting its methods), so no Velocity
     * resolution strategy has ever reached a non-public type.
     *
     * <p>Pinned as a test because it is the trap of using records from templates: a small record is
     * naturally declared package-private or local next to its use, and doing so makes it invisible to
     * VTL with no error — only literal text in the rendered page. A record that must be readable
     * from a template has to be {@code public}, or nested inside a public type.</p>
     */
    @Test
    public void test_nonPublicRecord_doesNotResolve() throws Exception {
        assertNull(uberspect.getPropertyGet(new PackagePrivateRecord("abc-123"), "id", null));
    }
}
