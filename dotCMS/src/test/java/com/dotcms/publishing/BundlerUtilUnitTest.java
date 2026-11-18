package com.dotcms.publishing;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.STYLE_PROPERTIES_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.pusher.wrapper.ContentTypeWrapper;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.dotcms.util.xstream.XStreamHandler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

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
        multiTreeEntry.put(STYLE_PROPERTIES_KEY, new HashMap<>(styleProps));  // mimic ContentBundler fix

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
                restoredEntry.containsKey(STYLE_PROPERTIES_KEY));
        assertNotNull("dotStyleProperties value must not be null after deserialization",
                restoredEntry.get(STYLE_PROPERTIES_KEY));

        @SuppressWarnings("unchecked")
        final Map<String, Object> restoredStyleProps = (Map<String, Object>) restoredEntry.get(STYLE_PROPERTIES_KEY);

        assertEquals("title-size property must match", "medium", restoredStyleProps.get("title-size"));
        assertEquals("button-color property must match", "blue", restoredStyleProps.get("button-color"));
    }

    /**
     * Verifies that DOT_STYLE_EDITOR_SCHEMA stored in ContentType.metadata() survives the Jackson
     * JSON round-trip that ContentTypeBundler performs when building the bundle. ContentTypeBundler
     * serializes via BundlerUtil.objectToJSON(wrapper, outputStream) and ContentTypeHandler
     * deserializes it back — metadata must arrive intact on the receiver.
     */
    @Test
    public void contentTypeWrapper_metadata_survives_json_round_trip() throws Exception {

        // --- BUILD a ContentType with DOT_STYLE_EDITOR_SCHEMA in metadata ---
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("DOT_STYLE_EDITOR_SCHEMA",
                "[{\"name\":\"title-size\",\"type\":\"dropdown\"}]");

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .name("Test Style Editor Type")
                .variable("testStyleEditorType")
                .id("test-ct-id-001")
                .metadata(metadata)
                .build();

        final ContentTypeWrapper wrapper = new ContentTypeWrapper();
        wrapper.setContentType(contentType);
        wrapper.setOperation(Operation.PUBLISH);
        wrapper.setWorkflowSchemaIds(java.util.Collections.emptyList());
        wrapper.setWorkflowSchemaNames(java.util.Collections.emptyList());

        // --- SERIALIZE to JSON (what ContentTypeBundler does on the sender) ---
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        BundlerUtil.objectToJSON(wrapper, out);
        final byte[] json = out.toByteArray();

        // --- DESERIALIZE from JSON (what ContentTypeHandler does on the receiver) ---
        final ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(new GuavaModule());
        final ContentTypeWrapper restored = mapper.readValue(json, ContentTypeWrapper.class);

        // --- ASSERT metadata is intact ---
        assertNotNull("ContentType must not be null after deserialization",
                restored.getContentType());
        assertNotNull("metadata must not be null after deserialization",
                restored.getContentType().metadata());
        assertTrue("DOT_STYLE_EDITOR_SCHEMA key must be present after deserialization",
                Objects.requireNonNull(restored.getContentType().metadata())
                        .containsKey("DOT_STYLE_EDITOR_SCHEMA"));
        assertEquals("DOT_STYLE_EDITOR_SCHEMA value must match",
                "[{\"name\":\"title-size\",\"type\":\"dropdown\"}]",
                Objects.requireNonNull(restored.getContentType().metadata())
                        .get("DOT_STYLE_EDITOR_SCHEMA"));
    }

    /**
     * A bundle file naming a type the trusted list does not cover must come back as that
     * refusal, with the file left exactly as it arrived.
     *
     * <p>The fallback below the first read prepends an XML 1.1 prologue and reads again, which
     * rewrites the received file on disk. For a refusal that is pointless — no prologue changes
     * whether a type is trusted — and it costs the message that names the offending type,
     * replacing it with a generic parse failure.</p>
     */
    @Test
    public void testXmlToObject_whenTypeOutsideTrustedList_shouldRefuseWithoutRewritingFile()
            throws IOException {
        final File file = File.createTempFile("trusted-list-probe", ".xml");
        file.deleteOnExit();
        final byte[] asReceived = "<java.beans.EventHandler/>".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), asReceived);

        try {
            BundlerUtil.xmlToObject(file);
            fail("Expected java.beans.EventHandler to be refused by the trusted list");
        } catch (final ForbiddenClassException e) {
            assertTrue("The refusal should name the refused type, was: " + e.getMessage(),
                    e.getMessage().contains("java.beans.EventHandler"));
        }

        assertArrayEquals("A refused bundle file must be left as it arrived",
                asReceived, Files.readAllBytes(file.toPath()));
    }

    /**
     * Writes a small archive file at the given location, creating its parent directories.
     */
    private static File writeArchive(final File archive) throws IOException {
        archive.getParentFile().mkdirs();
        Files.write(archive.toPath(), "archive".getBytes(StandardCharsets.UTF_8));
        return archive;
    }

    @Test
    public void findBundleArchive_existingArchive_isReturned() throws IOException {
        // Given a bundles directory holding <id>.tar.gz
        final File bundlesDir = tempFolder.newFolder("bundles");
        final File archive = writeArchive(new File(bundlesDir, "01JBUNDLE0001.tar.gz"));

        // When the archive is looked up by that plain id
        final Optional<File> found = BundlerUtil.findBundleArchive(bundlesDir, "01JBUNDLE0001");

        // Then the lookup returns bundlesDir/<id>.tar.gz
        assertTrue("An existing archive must be found", found.isPresent());
        assertEquals(archive.getCanonicalFile(), found.get().getCanonicalFile());
    }

    @Test
    public void findBundleArchive_missingArchive_isEmpty() throws IOException {
        // Given a bundles directory without <id>.tar.gz
        final File bundlesDir = tempFolder.newFolder("bundles");

        // When the archive is looked up by that plain id
        // Then the lookup is empty
        assertFalse(BundlerUtil.findBundleArchive(bundlesDir, "01JBUNDLE0002").isPresent());
    }

    @Test
    public void findBundleArchive_idWithParentSegments_isEmpty() throws IOException {
        // Given an archive in a directory next to the bundles directory
        final File bundlesDir = tempFolder.newFolder("bundles");
        writeArchive(new File(tempFolder.getRoot(), "other" + File.separator + "kept.tar.gz"));

        // When the id names it through parent segments
        final Optional<File> found = BundlerUtil.findBundleArchive(bundlesDir,
                ".." + File.separator + "other" + File.separator + "kept");

        // Then the lookup is empty
        assertFalse("Only archives directly in the bundles directory may be found", found.isPresent());
    }

    @Test
    public void findBundleArchive_idWithSeparator_isEmpty() throws IOException {
        // Given an archive in a subdirectory of the bundles directory
        final File bundlesDir = tempFolder.newFolder("bundles");
        writeArchive(new File(bundlesDir, "sub" + File.separator + "kept.tar.gz"));

        // When the id is "<subdirectory>/<name>"
        final Optional<File> found = BundlerUtil.findBundleArchive(bundlesDir, "sub/kept");

        // Then the lookup is empty: an id must be a single path segment
        assertFalse("An id must be a single path segment", found.isPresent());
    }

    @Test
    public void findBundleArchive_absoluteId_isEmpty() throws IOException {
        // Given an archive somewhere else on disk
        final File bundlesDir = tempFolder.newFolder("bundles");
        final File other = writeArchive(new File(tempFolder.getRoot(), "other" + File.separator + "kept.tar.gz"));

        // When the id is its absolute path without the extension
        final String absoluteId = new File(other.getParentFile(), "kept").getAbsolutePath();

        // Then the lookup is empty
        assertFalse(BundlerUtil.findBundleArchive(bundlesDir, absoluteId).isPresent());
    }

    @Test
    public void findBundleArchive_blankId_isEmpty() throws IOException {
        // Given a bundles directory
        final File bundlesDir = tempFolder.newFolder("bundles");

        writeArchive(new File(bundlesDir, ".tar.gz"));

        // When the id is null or empty
        // Then the lookup is empty and nothing is thrown
        assertFalse(BundlerUtil.findBundleArchive(bundlesDir, null).isPresent());
        assertFalse(BundlerUtil.findBundleArchive(bundlesDir, "").isPresent());
    }

    @Test
    public void findBundleArchive_archiveNameIsDirectory_isEmpty() throws IOException {
        // Given bundlesDir/<id>.tar.gz is a directory, not a file
        final File bundlesDir = tempFolder.newFolder("bundles");
        assertTrue(new File(bundlesDir, "folder.tar.gz").mkdir());

        // When the archive is looked up by <id>
        // Then the lookup is empty: only regular files are archives
        assertFalse(BundlerUtil.findBundleArchive(bundlesDir, "folder").isPresent());
    }

    @Test
    public void findBundleArchive_linkResolvingOutside_isEmpty() throws IOException {
        // Given bundlesDir/<id>.tar.gz is a symbolic link to an archive in another directory
        final File bundlesDir = tempFolder.newFolder("bundles");
        final File other = writeArchive(new File(tempFolder.getRoot(), "other" + File.separator + "kept.tar.gz"));
        try {
            Files.createSymbolicLink(new File(bundlesDir, "linked.tar.gz").toPath(), other.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            Assume.assumeNoException("Symbolic links cannot be created here", e);
        }

        // When the archive is looked up by <id>
        // Then the lookup is empty
        assertFalse("An archive that resolves to another directory must not be found",
                BundlerUtil.findBundleArchive(bundlesDir, "linked").isPresent());
    }

    @Test
    public void findBundleArchive_bundlesDirIsLink_archiveIsReturned() throws IOException {
        // Given the bundles directory is reached through a symbolic link and holds <id>.tar.gz
        final File realDir = tempFolder.newFolder("real-bundles");
        final File archive = writeArchive(new File(realDir, "01JBUNDLE0004.tar.gz"));
        final File linkedDir = new File(tempFolder.getRoot(), "bundles");
        try {
            Files.createSymbolicLink(linkedDir.toPath(), realDir.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            Assume.assumeNoException("Symbolic links cannot be created here", e);
        }

        // When the archive is looked up through the linked directory
        final Optional<File> found = BundlerUtil.findBundleArchive(linkedDir, "01JBUNDLE0004");

        // Then the archive is found
        assertTrue("An archive in a linked bundles directory must be found", found.isPresent());
        assertEquals(archive.getCanonicalFile(), found.get().getCanonicalFile());
    }

    @Test
    public void findBundleArchive_neverCreatesDirectories() throws IOException {
        // Given a bundles directory and a sibling directory name that does not exist yet
        final File bundlesDir = tempFolder.newFolder("bundles");
        final File sibling = new File(tempFolder.getRoot(), "fresh");

        // When ids naming a missing archive, and a missing directory through parent segments, are looked up
        BundlerUtil.findBundleArchive(bundlesDir, "01JBUNDLE0003");
        BundlerUtil.findBundleArchive(bundlesDir, ".." + File.separator + "fresh" + File.separator + "x");

        // Then no directory has been created in either place
        assertArrayEquals("A lookup must leave the bundles directory unchanged",
                new String[0], bundlesDir.list());
        assertFalse("A lookup must not create directories", sibling.exists());
    }
}
