package com.dotcms.rest.api.v1.maintenance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.cluster.bean.Server;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link ClusterLogCollector}. Exercises ZIP assembly, file naming,
 * scheme selection (auto/https/http), and error handling when peers are
 * unreachable.
 */
public class ClusterLogCollectorTest {

    private static final String LOG_FILE_NAME = "test.log";
    private static final String LOCAL_LOG_CONTENT = "local log line 1\nlocal log line 2\n";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * When there are no peer servers, the ZIP should contain only the local
     * server's log file with the correct label-based name.
     */
    @Test
    public void test_collect_noPeers_returnsZipWithLocalLogOnly() throws Exception {

        final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
        final Server localServer = buildServer("aaa-bbb-ccc", "node-local", "10.0.0.1");

        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(), LOG_FILE_NAME, "fake-jwt", 8080, localLog, localServer);

        final StreamingOutput output = collector.collect();
        final List<ZipEntryData> entries = extractZip(output);

        assertEquals("Should have exactly 1 ZIP entry", 1, entries.size());

        final ZipEntryData localEntry = entries.get(0);
        assertTrue("Entry name should end with log file name",
                localEntry.name.endsWith("_" + LOG_FILE_NAME));
        assertTrue("Entry name should contain the hostname",
                localEntry.name.contains("node-local"));
        assertEquals("Entry content should match local log",
                LOCAL_LOG_CONTENT, localEntry.content);
    }

    /**
     * When peer servers are unreachable, the ZIP should contain the local log
     * plus an _errors.txt file documenting the failures.
     */
    @Test
    public void test_collect_unreachablePeer_includesErrorFile() throws Exception {

        // Force HTTP only to avoid HTTPS fallback delay
        Config.setProperty("CLUSTER_LOG_DOWNLOAD_SCHEME", "http");
        Config.setProperty("CLUSTER_LOG_DOWNLOAD_CONNECT_TIMEOUT_SECONDS", 2);

        try {
            final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
            final Server localServer = buildServer("local-id", "node-local", "10.0.0.1");
            final Server unreachablePeer = buildServer("peer-id", "node-peer", "192.0.2.1");

            final ClusterLogCollector collector = new ClusterLogCollector(
                    List.of(unreachablePeer), LOG_FILE_NAME, "fake-jwt", 59999,
                    localLog, localServer);

            final StreamingOutput output = collector.collect();
            final List<ZipEntryData> entries = extractZip(output);

            final Set<String> entryNames = new HashSet<>();
            for (final ZipEntryData e : entries) {
                entryNames.add(e.name);
            }

            assertTrue("Should contain _errors.txt", entryNames.contains("_errors.txt"));
            assertTrue("Should still contain local log",
                    entryNames.stream().anyMatch(n -> n.endsWith("_" + LOG_FILE_NAME)));

            final ZipEntryData errorsEntry = entries.stream()
                    .filter(e -> "_errors.txt".equals(e.name))
                    .findFirst().orElse(null);
            assertNotNull("Errors entry should exist", errorsEntry);
            assertTrue("Errors should mention the peer",
                    errorsEntry.content.contains("node-peer")
                            || errorsEntry.content.contains("peer-id"));
        } finally {
            Config.setProperty("CLUSTER_LOG_DOWNLOAD_SCHEME", "auto");
            Config.setProperty("CLUSTER_LOG_DOWNLOAD_CONNECT_TIMEOUT_SECONDS", 10);
        }
    }

    /**
     * When a peer has no ipAddress, host, or name, it should be skipped
     * entirely — no error entry, just the local log.
     */
    @Test
    public void test_collect_peerWithNoHost_isSkipped() throws Exception {

        final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
        final Server localServer = buildServer("local-id", "node-local", "10.0.0.1");
        final Server noHostPeer = Server.builder()
                .withServerId("ghost-peer")
                .build();

        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(noHostPeer), LOG_FILE_NAME, "fake-jwt", 8080,
                localLog, localServer);

        final StreamingOutput output = collector.collect();
        final List<ZipEntryData> entries = extractZip(output);

        assertEquals("Should have only the local log entry", 1, entries.size());
        assertTrue("Entry should be the local log",
                entries.get(0).name.endsWith("_" + LOG_FILE_NAME));
    }

    /**
     * The label format should be shortId_hostname when name is set.
     */
    @Test
    public void test_collect_labelFormat_usesShortIdAndHostname() throws Exception {

        final String serverId = "56ebea93-4b99-4d08-9aa0-8b8185fe2ecd";
        final String hostname = "my-docker-host";
        final String expectedShortId = APILocator.getShortyAPI().shortify(serverId);

        final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
        final Server localServer = buildServer(serverId, hostname, "10.0.0.1");

        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(), LOG_FILE_NAME, "fake-jwt", 8080, localLog, localServer);

        final StreamingOutput output = collector.collect();
        final List<ZipEntryData> entries = extractZip(output);

        assertEquals(1, entries.size());

        final String expectedPrefix = expectedShortId + "_" + hostname + "_";
        assertTrue("Entry name should start with shortId_hostname: " + entries.get(0).name,
                entries.get(0).name.startsWith(expectedPrefix));
    }

    /**
     * When the server has no name but does have a host, the label should
     * fall back to shortId_host.
     */
    @Test
    public void test_collect_labelFormat_fallsBackToHost() throws Exception {

        final String serverId = "aabbccdd-1122-3344-5566-778899001122";
        final String expectedShortId = APILocator.getShortyAPI().shortify(serverId);

        final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
        final Server localServer = Server.builder()
                .withServerId(serverId)
                .withHost("fallback-host")
                .withIpAddress("10.0.0.1")
                .build();

        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(), LOG_FILE_NAME, "fake-jwt", 8080, localLog, localServer);

        final StreamingOutput output = collector.collect();
        final List<ZipEntryData> entries = extractZip(output);

        assertEquals(1, entries.size());

        final String expectedPrefix = expectedShortId + "_fallback-host_";
        assertTrue("Entry name should use host when name is null: " + entries.get(0).name,
                entries.get(0).name.startsWith(expectedPrefix));
    }

    /**
     * When the server has only a serverId (no name, no host), the label
     * should be the full serverId.
     */
    @Test
    public void test_collect_labelFormat_fallsBackToServerId() throws Exception {

        final String serverId = "only-server-id";

        final File localLog = createTempLogFile(LOCAL_LOG_CONTENT);
        final Server localServer = Server.builder()
                .withServerId(serverId)
                .withIpAddress("10.0.0.1")
                .build();

        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(), LOG_FILE_NAME, "fake-jwt", 8080, localLog, localServer);

        final StreamingOutput output = collector.collect();
        final List<ZipEntryData> entries = extractZip(output);

        assertEquals(1, entries.size());
        assertTrue("Entry name should start with full serverId: " + entries.get(0).name,
                entries.get(0).name.startsWith(serverId + "_" + LOG_FILE_NAME));
    }

    // ---- helpers ----

    private Server buildServer(final String serverId, final String name, final String ip) {
        return Server.builder()
                .withServerId(serverId)
                .withName(name)
                .withIpAddress(ip)
                .build();
    }

    private File createTempLogFile(final String content) throws IOException {
        final File file = Files.createTempFile("cluster-log-test-", ".log").toFile();
        file.deleteOnExit();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private List<ZipEntryData> extractZip(final StreamingOutput output) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        output.write(baos);

        final List<ZipEntryData> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final byte[] data = zis.readAllBytes();
                entries.add(new ZipEntryData(
                        entry.getName(),
                        new String(data, StandardCharsets.UTF_8)));
                zis.closeEntry();
            }
        }
        return entries;
    }

    private static class ZipEntryData {
        final String name;
        final String content;

        ZipEntryData(final String name, final String content) {
            this.name = name;
            this.content = content;
        }
    }
}
