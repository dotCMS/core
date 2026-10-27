package com.dotcms.security.apps;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for reading an Apps export file back through {@link AppsUtil#importSecrets(Path, Key)}.
 * Files written by {@link AppsUtil#exportSecret(AppsSecretsImportExport, Key)}, and files written by
 * earlier releases, must keep importing; a file that holds any other type must be refused without
 * that type being loaded into use.
 */
public class AppsUtilImportFileTest {

    private static final String PASSWORD = "importFileTest1234";

    /** Set by {@link UnlistedType}'s static initializer, so a test can tell whether it ever ran. */
    static final AtomicBoolean UNLISTED_TYPE_INITIALIZED = new AtomicBoolean(false);

    private final List<Path> createdFiles = new ArrayList<>();

    @After
    public void deleteCreatedFiles() throws IOException {
        for (final Path file : createdFiles) {
            Files.deleteIfExists(file);
        }
        createdFiles.clear();
    }

    /**
     * Given: an export written by the current release, holding apps with no secrets, one secret and
     * several secrets (each a different map shape on the wire), across two sites, including a
     * hidden secret and a secret carrying environment-variable details
     * Expected: every site, app and secret value is read back unchanged
     */
    @Test
    public void test_importSecrets_current_export_reads_back() throws Exception {
        final Key key = AppsUtil.generateKey(PASSWORD);
        final AppSecrets several = AppSecrets.builder().withKey("app-several")
                .withSecret("plain", "plain-value")
                .withHiddenSecret("hidden", "hidden-value")
                .withHiddenSecret("flag", true)
                .withSecret("env", Secret.builder().withValue("env-value").withHidden(false)
                        .withType(Type.STRING).withEnvVar("SOME_VAR").withEnvShow(true)
                        .withEnvValue("env-var-value").build())
                .build();
        final AppSecrets single = AppSecrets.builder().withKey("app-single")
                .withSecret("only", "only-value").build();
        final AppSecrets none = AppSecrets.builder().withKey("app-none").build();
        final Map<String, List<AppSecrets>> bySite = new HashMap<>();
        bySite.computeIfAbsent("SYSTEM_HOST", site -> new LinkedList<>()).add(several);
        bySite.get("SYSTEM_HOST").add(single);
        bySite.computeIfAbsent("site-2", site -> new LinkedList<>()).add(none);

        final Path exported = track(AppsUtil.exportSecret(new AppsSecretsImportExport(bySite), key));
        final Map<String, List<AppSecrets>> imported = AppsUtil.importSecrets(exported, key);

        assertEquals(2, imported.size());
        final List<AppSecrets> systemHost = imported.get("SYSTEM_HOST");
        assertEquals(2, systemHost.size());
        final AppSecrets importedSeveral = systemHost.get(0);
        assertEquals("app-several", importedSeveral.getKey());
        assertEquals(4, importedSeveral.getSecrets().size());
        assertEquals("plain-value", new String(importedSeveral.getSecrets().get("plain").getValue()));
        assertEquals("hidden-value", new String(importedSeveral.getSecrets().get("hidden").getValue()));
        assertTrue(importedSeveral.getSecrets().get("hidden").isHidden());
        assertTrue(importedSeveral.getSecrets().get("flag").getBoolean());
        final Secret env = importedSeveral.getSecrets().get("env");
        assertEquals("SOME_VAR", env.getEnvVar());
        assertTrue(env.isEnvShow());
        assertArrayEquals("env-var-value".toCharArray(), env.getEnvVarValue());
        assertEquals(Type.STRING, env.getType());
        assertEquals("only-value", new String(systemHost.get(1).getSecrets().get("only").getValue()));
        assertEquals("app-none", imported.get("site-2").get(0).getKey());
        assertTrue(imported.get("site-2").get(0).getSecrets().isEmpty());
    }

    /**
     * Given: an export file written by an earlier release, whose classes carry older serial
     * version ids and plain JDK maps
     * Expected: it still imports, with its single secret intact
     */
    @Test
    public void test_importSecrets_earlier_release_export_still_imports() throws Exception {
        final URL resource = Thread.currentThread().getContextClassLoader()
                .getResource("apps/googleTranslationApp.export");
        assertNotNull(resource);
        final Key key = AppsUtil.generateKey("testdotcms1234");

        final Map<String, List<AppSecrets>> imported = AppsUtil.importSecrets(pathOf(resource), key);

        final AppSecrets appSecrets = imported.get("SYSTEM_HOST").get(0);
        assertEquals("dotGoogleTranslate-config", appSecrets.getKey());
        assertEquals("testgoogleAPIAPP", new String(appSecrets.getSecrets().get("apiKey").getValue()));
    }

    /**
     * Given: an export whose site map and app lists are the JDK's immutable collections
     * ({@code Map.of}, {@code List.of} and {@code Stream.toList}), as callers of
     * {@link AppsUtil#exportSecret(AppsSecretsImportExport, Key)} may build them
     * Expected: it reads back with every site and app intact
     */
    @Test
    public void test_importSecrets_export_built_from_jdk_immutable_collections_reads_back()
            throws Exception {
        final Key key = AppsUtil.generateKey(PASSWORD);
        final AppSecrets first = AppSecrets.builder().withKey("app-first")
                .withSecret("one", "one-value").build();
        final AppSecrets second = AppSecrets.builder().withKey("app-second")
                .withSecret("two", "two-value").build();
        final Map<String, List<AppSecrets>> bySite = Map.of(
                "SYSTEM_HOST", List.of(first),
                "site-2", Stream.of(first, second).toList());

        final Path exported = track(AppsUtil.exportSecret(new AppsSecretsImportExport(bySite), key));
        final Map<String, List<AppSecrets>> imported = AppsUtil.importSecrets(exported, key);

        assertEquals(2, imported.size());
        assertEquals("one-value",
                new String(imported.get("SYSTEM_HOST").get(0).getSecrets().get("one").getValue()));
        assertEquals(2, imported.get("site-2").size());
        assertEquals("app-second", imported.get("site-2").get(1).getKey());
    }

    /**
     * Given: an import file that names a class which is not part of an Apps export
     * Expected: the file is refused as an invalid export, and the named class is never
     * initialized (its static initializer does not run)
     */
    @Test
    public void test_importSecrets_unlisted_class_is_refused_before_initialization() throws Exception {
        final Key key = AppsUtil.generateKey(PASSWORD);
        // Write a StandInClass, then point the stream at UnlistedType (same name length), so this
        // test never touches UnlistedType itself and only the import can initialize it.
        final byte[] stream = serialize(new StandInClass());
        final byte[] renamed = replaceAscii(stream, StandInClass.class.getName(),
                StandInClass.class.getName().replace("StandInClass", "UnlistedType"));
        final Path file = writeImportFile(renamed, key);

        assertThrows(IllegalArgumentException.class, () -> AppsUtil.importSecrets(file, key));
        assertFalse(UNLISTED_TYPE_INITIALIZED.get());
    }

    /**
     * Given: an import file whose top-level object is an ordinary JDK type instead of an export
     * Expected: the file is refused as an invalid export rather than failing on a cast
     */
    @Test
    public void test_importSecrets_unexpected_top_level_type_is_refused() throws Exception {
        final Key key = AppsUtil.generateKey(PASSWORD);
        final Path file = writeImportFile(serialize(new Date()), key);

        assertThrows(IllegalArgumentException.class, () -> AppsUtil.importSecrets(file, key));
    }

    /**
     * Given: an import file whose top-level object is a map, a type an export uses, but not an
     * export itself
     * Expected: the file is refused as an invalid export rather than failing on a cast
     */
    @Test
    public void test_importSecrets_listed_type_at_top_level_is_refused() throws Exception {
        final Key key = AppsUtil.generateKey(PASSWORD);
        final Path file = writeImportFile(serialize(new HashMap<String, String>()), key);

        assertThrows(IllegalArgumentException.class, () -> AppsUtil.importSecrets(file, key));
    }

    /**
     * Serializes an object with plain Java serialization, as the export does.
     *
     * @param object the object to write
     * @return the serialized bytes
     * @throws IOException if writing fails
     */
    private static byte[] serialize(final Object object) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        return bytes.toByteArray();
    }

    /**
     * Replaces one ASCII string with another of the same length inside a byte array, which keeps
     * the length prefixes of a serialized stream valid.
     *
     * @param source the bytes to search
     * @param from the text to replace
     * @param to the replacement, which must be as long as {@code from}
     * @return a copy of {@code source} with every occurrence replaced
     */
    private static byte[] replaceAscii(final byte[] source, final String from, final String to) {
        final byte[] needle = from.getBytes(StandardCharsets.US_ASCII);
        final byte[] replacement = to.getBytes(StandardCharsets.US_ASCII);
        assertEquals(needle.length, replacement.length);
        final byte[] result = source.clone();
        int replaced = 0;
        for (int i = 0; i <= result.length - needle.length; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length && match; j++) {
                match = result[i + j] == needle[j];
            }
            if (match) {
                System.arraycopy(replacement, 0, result, i, replacement.length);
                replaced++;
            }
        }
        assertTrue(replaced > 0);
        return result;
    }

    /**
     * Encrypts serialized bytes the way an export file is encrypted and writes them to a temporary
     * file that is deleted after the test.
     *
     * @param serialized the plain serialized bytes
     * @param key the key the import will decrypt with
     * @return the path of the written file
     * @throws Exception if encryption or writing fails
     */
    private Path writeImportFile(final byte[] serialized, final Key key) throws Exception {
        final Path file = track(Files.createTempFile("apps-import-test", ".export"));
        Files.write(file, AppsUtil.encrypt(key, serialized));
        return file;
    }

    /**
     * Registers a file for deletion after the test.
     *
     * @param file the file to delete later
     * @return the same file
     */
    private Path track(final Path file) {
        createdFiles.add(file);
        return file;
    }

    /**
     * Converts a classpath resource URL to a path.
     *
     * @param resource the resource URL
     * @return the resource's path
     * @throws URISyntaxException if the URL is not a valid URI
     */
    private static Path pathOf(final URL resource) throws URISyntaxException {
        return Path.of(resource.toURI());
    }

    /** A serializable type the test can write; its name is then swapped for {@link UnlistedType}. */
    static class StandInClass implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /** A serializable type that is not part of an export and records when it is initialized. */
    static class UnlistedType implements Serializable {
        private static final long serialVersionUID = 1L;

        static {
            UNLISTED_TYPE_INITIALIZED.set(true);
        }
    }
}
