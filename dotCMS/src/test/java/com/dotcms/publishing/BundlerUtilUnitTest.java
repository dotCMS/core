package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.thoughtworks.xstream.XStream;
import com.dotcms.util.xstream.XStreamHandler;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Gamba 2019-01-08
 */
public class BundlerUtilUnitTest {

    @Test
    public void sanitize_bundle_name_no_changes() throws Exception {

        final String[] testBundleNames = {"bundle.zip", "bundle.tar.gz", "bundle", "bunDLE.zip",
                "Bundle.tar.gz", "bundle.bundle.bundle", "bundle.bundle.bundle.bundle"};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, testBundleName);
        }
    }

    @Test
    public void sanitize_bundle_name_with_changes() throws Exception {

        final String bundleName = "bundle.zip";
        final String[] testBundleNames = {
                "/some/path/" + bundleName,
                "/another/random/path/" + bundleName,
                "another/random/path/" + bundleName};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, bundleName);
        }
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_null_name() throws Exception {
        BundlerUtil.sanitizeBundleName(null);
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_empty_name() throws Exception {
        BundlerUtil.sanitizeBundleName("");
    }

    /**
     * Verifies that dotStyleProperties inside the multiTree list survive the
     * XStream XML round-trip (serialize → deserialize) that happens between the
     * push-publish sender (ContentBundler) and receiver (ContentHandler).
     * The sender writes a .content.xml with BundlerUtil.objectToXML and the
     * receiver reads it back with XStream.fromXML.  A nested HashMap used as
     * the dotStyleProperties value must survive both legs.
     */
    @Test
    public void multiTree_dotStyleProperties_survives_xstream_round_trip() throws Exception {

        // --- BUILD a PushContentWrapper with one multiTree entry that has style properties ---
        final Map<String, Object> styleProps = new HashMap<>();
        styleProps.put("title-size", "medium");
        styleProps.put("button-color", "blue");

        final Map<String, Object> multiTreeEntry = new HashMap<>();
        multiTreeEntry.put("parent1", "page-identifier-123");
        multiTreeEntry.put("parent2", "container-identifier-456");
        multiTreeEntry.put("child", "contentlet-identifier-789");
        multiTreeEntry.put("relation_type", "uuid-abc");
        multiTreeEntry.put("tree_order", 0);
        multiTreeEntry.put("personalization", "dot:default");
        multiTreeEntry.put("variantId", "DEFAULT");
        multiTreeEntry.put("dotStyleProperties", new HashMap<>(styleProps));  // mimic ContentBundler fix

        final List<Map<String, Object>> multiTreeList = new ArrayList<>();
        multiTreeList.add(multiTreeEntry);

        final PushContentWrapper wrapper = new PushContentWrapper();
        wrapper.setMultiTree(multiTreeList);
        wrapper.setOperation(Operation.PUBLISH);

        // --- SERIALIZE to XML (what ContentBundler does on the sender) ---
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        BundlerUtil.objectToXML(wrapper, out);
        final String xml = out.toString(StandardCharsets.UTF_8);

        // --- DESERIALIZE from XML (what ContentHandler does on the receiver) ---
        final XStream xstream = XStreamHandler.newXStreamInstance();
        final PushContentWrapper restored = (PushContentWrapper)
                xstream.fromXML(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        // --- ASSERT the multiTree list is intact ---
        assertNotNull("multiTree list must not be null after deserialization", restored.getMultiTree());
        assertEquals("multiTree list must have exactly one entry", 1, restored.getMultiTree().size());

        final Map<String, Object> restoredEntry = restored.getMultiTree().get(0);

        // Core fields
        assertEquals("page-identifier-123", restoredEntry.get("parent1"));
        assertEquals("container-identifier-456", restoredEntry.get("parent2"));
        assertEquals("contentlet-identifier-789", restoredEntry.get("child"));

        // dotStyleProperties key must survive
        assertTrue("dotStyleProperties key must be present after deserialization",
                restoredEntry.containsKey("dotStyleProperties"));
        assertNotNull("dotStyleProperties value must not be null after deserialization",
                restoredEntry.get("dotStyleProperties"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> restoredStyleProps = (Map<String, Object>) restoredEntry.get("dotStyleProperties");

        assertEquals("title-size property must match", "medium", restoredStyleProps.get("title-size"));
        assertEquals("button-color property must match", "blue", restoredStyleProps.get("button-color"));
    }

}