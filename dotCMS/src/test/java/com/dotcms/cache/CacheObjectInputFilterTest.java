package com.dotcms.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.cache.lettuce.DecodeException;
import com.dotcms.cache.lettuce.DotObjectCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CacheObjectInputFilter}, the class filter applied when cache values are read back
 * from Redis.
 */
class CacheObjectInputFilterTest {

    /** A cache value class from a dotCMS package. */
    static class SampleValue implements Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final Map<String, Object> map = new HashMap<>();

        SampleValue(final String name) {
            this.name = name;
        }
    }

    private static byte[] serialize(final Object value) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
            out.flush();
            return bytes.toByteArray();
        }
    }

    private static Object read(final CacheObjectInputFilter filter, final Object value)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialize(value)))) {
            in.setObjectInputFilter(filter);
            return in.readObject();
        }
    }

    /**
     * Given: values made of JDK types and dotCMS classes, as the caches store them
     * Expected: they are read back unchanged
     */
    @Test
    void readsCacheValueTypes() throws Exception {
        final CacheObjectInputFilter filter = new CacheObjectInputFilter();
        final SampleValue sample = new SampleValue("sample");
        sample.map.put("created", Instant.ofEpochSecond(100));
        sample.map.put("modDate", new Timestamp(1000));
        sample.map.put("amount", new BigDecimal("12.50"));
        sample.map.put("tags", new ArrayList<>(List.of("a", "b")));
        sample.map.put("binary", new byte[]{1, 2, 3});
        sample.map.put("matrix", new int[][]{{1}, {2}});
        sample.map.put("timeZone", TimeZone.getTimeZone("America/New_York"));
        sample.map.put("key", new SecretKeySpec(new byte[]{1, 2, 3, 4}, "AES"));

        final SampleValue result = (SampleValue) read(filter, sample);

        assertEquals("sample", result.name);
        assertEquals(Instant.ofEpochSecond(100), result.map.get("created"));
        assertEquals(new BigDecimal("12.50"), result.map.get("amount"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) result.map.get("binary"));
        assertEquals(TimeZone.getTimeZone("America/New_York"), result.map.get("timeZone"));
    }

    /**
     * Given: a value whose class is not in the list, alone or nested in a map
     * Expected: the read fails with an InvalidClassException
     */
    @Test
    void rejectsClassesOutsideTheList() {
        final CacheObjectInputFilter filter = new CacheObjectInputFilter();
        final Map<String, Object> nested = new HashMap<>(Map.of("uri", URI.create("http://example.com")));

        assertThrows(InvalidClassException.class, () -> read(filter, URI.create("http://example.com")));
        assertThrows(InvalidClassException.class, () -> read(filter, nested));
    }

    /**
     * Given: a class added through the constructor (what the config property feeds)
     * Expected: it is read back
     */
    @Test
    void readsClassesAddedToTheList() throws Exception {
        final CacheObjectInputFilter filter = new CacheObjectInputFilter("java.net.URI");

        assertEquals(URI.create("http://example.com"), read(filter, URI.create("http://example.com")));
    }

    /**
     * Given: class names matched against the default list
     * Expected: dotCMS and JDK value packages pass; repackaged libraries and the JDK internals that
     * only matter to object graphs (lambdas, reflection) do not
     */
    @Test
    void matchesPackagesByPattern() {
        final CacheObjectInputFilter filter = new CacheObjectInputFilter();

        assertTrue(filter.isAllowed("com.dotcms.contenttype.model.type.ImmutableSimpleContentType"));
        assertTrue(filter.isAllowed("com.dotmarketing.portlets.contentlet.model.Contentlet"));
        assertTrue(filter.isAllowed("com.liferay.portal.model.User"));
        assertTrue(filter.isAllowed("java.lang.String"));
        assertTrue(filter.isAllowed("java.util.concurrent.ConcurrentHashMap"));
        assertTrue(filter.isAllowed("com.google.common.collect.ImmutableList$SerializedForm"));
        assertTrue(filter.isAllowed("sun.util.calendar.ZoneInfo"));
        assertTrue(filter.isAllowed("javax.crypto.spec.SecretKeySpec"));

        assertFalse(filter.isAllowed("com.dotcms.repackage.org.apache.commons.beanutils.BeanComparator"));
        assertFalse(filter.isAllowed("java.lang.invoke.SerializedLambda"));
        assertFalse(filter.isAllowed("java.lang.reflect.Proxy"));
        assertFalse(filter.isAllowed("org.apache.commons.collections.functors.InvokerTransformer"));
        assertFalse(filter.isAllowed("java.net.URL"));
        assertFalse(filter.isAllowed("com.dotcmsx.Sample"));
        assertFalse(filter.isAllowed("sun.util.calendar.ZoneInfoFile"));
        assertFalse(filter.isAllowed("javax.crypto.SealedObject"));
    }

    /**
     * Given: the Redis codec decoding a value whose class is not in the list
     * Expected: a DecodeException caused by an InvalidClassException, which RedisCache treats as a miss
     */
    @Test
    void codecRejectsClassesOutsideTheList() throws Exception {
        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();
        final ByteBuffer planted = ByteBuffer.wrap(serialize(URI.create("http://example.com")));

        final DecodeException exception = assertThrows(DecodeException.class, () -> codec.decodeValue(planted));
        assertInstanceOf(InvalidClassException.class, exception.getCause());
    }

    /**
     * Given: the Redis codec encoding and decoding a dotCMS value
     * Expected: the value comes back
     */
    @Test
    void codecReadsCacheValues() {
        final DotObjectCodec<String, Object> codec = new DotObjectCodec<>();

        final SampleValue result = (SampleValue) codec.decodeValue(codec.encodeValue(new SampleValue("x")));

        assertEquals("x", result.name);
    }
}
