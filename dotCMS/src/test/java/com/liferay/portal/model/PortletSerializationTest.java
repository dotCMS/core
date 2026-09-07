package com.liferay.portal.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Regression tests for issue #34435: when the Portlet cache is backed by a serializing provider
 * (e.g. Redis), a cached {@link Portlet} is round-tripped through Java serialization. The
 * {@code initParams} field used to be {@code transient}, so it came back {@code null} after
 * deserialization and later NPE'd in {@code PortletConfigImpl.getInitParameterNames()}.
 */
public class PortletSerializationTest {

    private static Portlet roundTrip(final Portlet portlet) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(portlet);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Portlet) in.readObject();
        }
    }

    /**
     * A Portlet serialized and deserialized (as it is when stored in a Redis-backed cache) must
     * preserve its init parameters rather than returning a null map.
     */
    @Test
    public void test_initParams_survive_serialization_roundtrip() throws Exception {
        final Map<String, String> initParams = new HashMap<>();
        initParams.put("view-action", "/ext/contentlet/view_contentlets");
        initParams.put("name", "content");

        final Portlet deserialized = roundTrip(new Portlet("content", "com.liferay.portlet.StrutsPortlet", initParams));

        assertNotNull("initParams must not be null after deserialization", deserialized.getInitParams());
        assertEquals(initParams, deserialized.getInitParams());
    }

    /**
     * Simulates an object serialized by an older version where {@code initParams} was transient (so
     * it is absent from the stream). The {@code readObject} guard must default it to an empty map so
     * downstream look-ups never NPE.
     */
    @Test
    public void test_null_initParams_defaults_to_empty_map_after_deserialization() throws Exception {
        final Portlet portlet = new Portlet("content", "com.liferay.portlet.StrutsPortlet", new HashMap<>());
        // Force the pre-fix state: a Portlet whose initParams was dropped during serialization.
        portlet.initParams = null;

        final Portlet deserialized = roundTrip(portlet);

        assertNotNull("Guard must replace a null initParams with an empty map", deserialized.getInitParams());
        assertTrue("Recovered init params should be empty", deserialized.getInitParams().isEmpty());
    }
}
