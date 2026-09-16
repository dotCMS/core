package com.dotcms.storage;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileSystemStoragePersistenceAPIImpl;
import com.dotcms.storage.binary.BinaryAssetStorageAPIImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.mockito.Mockito;

/** Standalone acceptance against the current compiled classes on two independently mounted NFS clients. */
class NfsAcceptance {
    public static void main(String[] args) {
        try {
            run(args);
            System.exit(0); // Existing WebDAV timers otherwise keep this standalone test JVM alive.
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
    private static void run(String[] args) throws Exception {
        final Path root = Path.of("/mnt/nfs/assets");
        Files.createDirectories(root);
        final String type = Files.getFileStore(root).type();
        if (!type.startsWith("nfs")) throw new AssertionError("Expected a real NFS mount, found " + type);
        if (args[0].equals("unit")) {
            final var listener = new SummaryGeneratingListener();
            LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(
                    DiscoverySelectors.selectClass("com.dotcms.storage.BinaryFileSystemStorageTest"),
                    DiscoverySelectors.selectClass("com.dotcms.storage.AssetStorageFeatureTest"),
                    DiscoverySelectors.selectClass("com.dotmarketing.webdav.WebdavAssetStorageTest")).build(), listener);
            listener.getSummary().printTo(new PrintWriter(System.out, true));
            listener.getSummary().printFailuresTo(new PrintWriter(System.out, true));
            if (listener.getSummary().getTestsSucceededCount() != 30 || listener.getSummary().getTotalFailureCount() != 0) {
                throw new AssertionError("Expected all 30 focused checks to pass on NFS");
            }
            return;
        }
        Config.setProperty(AssetStorageFeature.FLAG, false);
        Config.setProperty("BINARY_ASSET_STORAGE_TYPE", "BINARY_CHAIN");
        Config.setProperty("storage.file-metadata.s3.endpoint", "http://127.0.0.1:1");
        Config.setProperty("ROOT_GROUP_FOLDER_PATH", root.toString());
        try (var config = Mockito.mockStatic(ConfigUtils.class)) {
            config.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            final var binaries = new BinaryAssetStorageAPIImpl(new FileSystemStoragePersistenceAPIImpl());
            final Path source = Files.createTempFile("nfs-source-", ".txt");
            try {
                switch (args[0]) {
                    case "seed" -> {
                        Files.writeString(source, "from node A");
                        binaries.storeBinary("abc123", "asset", "report.txt", source.toFile(), false);
                        check(binaries, "abc123", "from node A");
                    }
                    case "replace" -> {
                        check(binaries, "abc123", "from node A");
                        binaries.storeBinary("def456", "asset", "report.txt",
                                binaries.getBinaryFile("abc123", "asset", "report.txt"), false);
                        Files.writeString(source, "from node B");
                        binaries.storeBinary("abc123", "asset", "report.txt", source.toFile(), false);
                    }
                    case "verify" -> {
                        check(binaries, "abc123", "from node B");
                        check(binaries, "def456", "from node A");
                        if (binaries.evictLocalFile(binaries.getBinaryFile("abc123", "asset", "report.txt"))) {
                            throw new AssertionError("Feature-disabled NFS must not evict its authoritative file");
                        }
                    }
                    case "delete" -> binaries.deleteBinary("abc123", "asset");
                    case "deleted" -> {
                        if (binaries.existsBinary("abc123", "asset")) throw new AssertionError("Other node still sees deleted binary");
                        check(binaries, "def456", "from node A");
                        binaries.deleteBinary("def456", "asset");
                    }
                    default -> throw new IllegalArgumentException(args[0]);
                }
            } finally {
                Files.deleteIfExists(source);
            }
        }
        System.out.println("PASS " + args[0] + " on " + type + " with S3 feature disabled");
    }
    private static void check(BinaryAssetStorageAPIImpl storage, String inode, String expected) throws Exception {
        final var file = storage.getBinaryFile(inode, "asset", "report.txt");
        if (file == null || !expected.equals(Files.readString(file.toPath()))) throw new AssertionError("Wrong shared bytes for " + inode);
    }
}
