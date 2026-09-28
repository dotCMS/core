package com.dotcms.storage;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** WebDAV names point to completed immutable payloads; local files are disposable copies. */
public final class WebdavTemporaryStorage {
    public static final String GROUP = "webdav-temporary";
    private static final JsonReaderDelegate<Map> READER = new JsonReaderDelegate<>(Map.class);
    private final Path configuredRoot;
    private AmazonS3StoragePersistenceAPIImpl remote;
    private boolean groupReady;

    private static class Holder {
        private static final WebdavTemporaryStorage INSTANCE = new WebdavTemporaryStorage(null, null);
    }

    public static WebdavTemporaryStorage getInstance() { return Holder.INSTANCE; }

    public WebdavTemporaryStorage(AmazonS3StoragePersistenceAPIImpl remote, Path root) {
        this.remote = remote;
        this.configuredRoot = root;
    }

    private Path root() throws IOException {
        return (configuredRoot == null ? Path.of(ConfigUtils.getAssetTempPath()) : configuredRoot)
                .toFile().getCanonicalFile().toPath();
    }

    public String key(File file) throws IOException {
        final Path root = root();
        final Path supplied = file.toPath().toAbsolutePath().normalize();
        final Path configured = (configuredRoot == null ? Path.of(ConfigUtils.getAssetTempPath()) : configuredRoot)
                .toAbsolutePath().normalize();
        final Path boundary = supplied.startsWith(configured) ? configured : root;
        if (!supplied.startsWith(boundary)) throw new IOException("Invalid WebDAV staging path");
        for (Path part = supplied; !part.equals(boundary); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("WebDAV staging path contains a symbolic link");
        }
        final Path canonical = file.getCanonicalFile().toPath();
        if (!canonical.startsWith(root) || canonical.equals(root)) throw new IOException("Invalid WebDAV staging path");
        final String key = root.relativize(canonical).toString().replace(File.separatorChar, '/');
        if (key.startsWith(".webdav-cache/") || key.equals(".webdav-cache")) throw new IOException("Reserved staging path");
        return key;
    }

    public File file(String key) throws IOException {
        final Path path = Path.of(key);
        if (path.isAbsolute() || key.contains("\\") || !path.normalize().equals(path)) throw new IOException("Invalid staging key");
        final File file = root().resolve(path).toFile();
        if (!key(file).equals(key)) throw new IOException("Invalid staging key");
        return file;
    }

    private synchronized AmazonS3StoragePersistenceAPIImpl remote() throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) throw new DotDataException("S3 asset storage is disabled");
        if (remote == null) remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        if (!groupReady) {
            remote.createGroup(GROUP);
            groupReady = true;
        }
        return remote;
    }

    public record Entry(String path, boolean directory, long size, long modified, String dataKey) {
        public boolean expired() {
            return modified <= System.currentTimeMillis()
                    - Config.getIntProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 3) * 3_600_000L;
        }
    }

    // A record is a leaf beside its descendants, including on S3-compatible filesystem backends.
    private String recordPrefix(String path) {
        return "entries/" + path.replace("%", "%25").replace(".", "%2E") + "/";
    }

    private String recordPath(String object) throws IOException {
        if (!object.startsWith("entries/") || !object.endsWith("/.entry")) {
            throw new IOException("Invalid WebDAV entry key");
        }
        final String path = object.substring("entries/".length(), object.length() - "/.entry".length())
                .replace("%2E", ".").replace("%25", "%");
        file(path);
        if (!(recordPrefix(path) + ".entry").equals(object)) throw new IOException("Invalid WebDAV entry key");
        return path;
    }

    private record State(AmazonS3StoragePersistenceAPIImpl.ObjectSnapshot snapshot, Entry entry, String pending) { }

    private State state(String key) throws DotDataException, IOException {
        file(key);
        final var object = remote().readObjectSnapshot(GROUP, recordPrefix(key) + ".entry", READER);
        if (object == null) return null;
        try {
            final Map<?, ?> data = (Map<?, ?>) object.value();
            final String pending = (String) data.get("pending");
            if (pending != null) validateDataKey(key, pending);
            Entry entry = null;
            if (data.containsKey("directory")) {
                entry = new Entry(key, (Boolean) data.get("directory"), ((Number) data.get("size")).longValue(),
                        ((Number) data.get("modified")).longValue(), (String) data.get("dataKey"));
                if (entry.size() < 0 || entry.modified() <= 0 || entry.dataKey() == null) {
                    throw new IOException("Invalid WebDAV entry: " + key);
                }
                if (!entry.directory()) validateDataKey(key, entry.dataKey());
            }
            return new State(object, entry, pending);
        } catch (RuntimeException failure) {
            throw new IOException("Invalid WebDAV entry: " + key, failure);
        }
    }

    private void validateDataKey(String path, String dataKey) throws IOException {
        if (!dataKey.matches("data/[0-9]+/[0-9a-f-]{36}/[^/]+")
                || !Path.of(dataKey).getFileName().equals(Path.of(path).getFileName())
                || !Path.of(dataKey).normalize().toString().equals(dataKey)) {
            throw new IOException("Invalid WebDAV payload reference: " + path);
        }
    }

    private Entry read(String key) throws DotDataException, IOException {
        final State state = state(key);
        return state == null ? null : state.entry();
    }

    public Entry stat(File file) {
        try {
            final String key = key(file);
            final Entry entry = read(key);
            if (entry != null && !entry.expired()) return entry;
            // Parents can be implicit, including after a concurrent child write and folder deletion.
            final var children = children(file);
            return children.isEmpty() ? null : new Entry(key, true, 0,
                    children.stream().mapToLong(Entry::modified).max().orElseThrow(), "");
        } catch (IOException | DotDataException failure) {
            throw new DotRuntimeException("Unable to inspect WebDAV staging file", failure);
        }
    }

    public List<Entry> children(File directory) throws IOException, DotDataException {
        final String prefix = key(directory) + "/";
        final Map<String, Entry> children = new java.util.TreeMap<>();
        for (String object : remote().listObjectPaths(GROUP, recordPrefix(key(directory)))) {
            final String path = recordPath(object);
            if (path.equals(key(directory))) continue;
            if (!path.startsWith(prefix)) throw new IOException("Invalid staging listing");
            final Entry entry = read(path);
            if (entry == null || entry.expired()) continue;
            final String suffix = path.substring(prefix.length());
            final int slash = suffix.indexOf('/');
            if (slash < 0) children.put(path, entry);
            else {
                final String parent = prefix + suffix.substring(0, slash);
                children.putIfAbsent(parent, new Entry(parent, true, 0, entry.modified(), ""));
            }
        }
        return new ArrayList<>(children.values());
    }

    private String publish(String path, Entry entry, String pending, String expected) throws DotDataException {
        final var data = new HashMap<String, Serializable>();
        // Every mutation changes the ETag, including rollback to the same completed entry.
        data.put("mutation", UUID.randomUUID().toString());
        if (entry != null) {
            data.put("directory", entry.directory());
            data.put("size", entry.size());
            data.put("modified", entry.modified());
            data.put("dataKey", entry.dataKey());
        }
        if (pending != null) data.put("pending", pending);
        final String version = remote().writeObjectIfMatch(GROUP, recordPrefix(path) + ".entry", data, expected);
        if (version == null) throw new DotDataException("WebDAV path changed concurrently: " + path);
        return version;
    }

    public void mkdir(File directory) throws IOException, DotDataException {
        final String path = key(directory);
        final State current = state(path);
        final Entry existing = current == null ? null : current.entry();
        if (existing != null && !existing.directory() && !existing.expired()) throw new IOException("A file exists at " + path);
        publish(path, new Entry(path, true, 0, System.currentTimeMillis(), ""), null,
                current == null ? null : current.snapshot().version());
    }

    /** Reserve a reference before uploading; cleanup or a competing writer fences late publication. */
    public void store(File destination, File completed) throws IOException, DotDataException {
        final String path = key(destination);
        final State before = state(path);
        final Entry existing = before == null ? null : before.entry();
        if ((existing != null && existing.directory() && !existing.expired()) || !children(destination).isEmpty()) {
            throw new IOException("A directory exists at " + path);
        }
        final String dataKey = "data/" + System.currentTimeMillis() + "/" + UUID.randomUUID() + "/" + destination.getName();
        final String reservation = publish(path, existing, dataKey, before == null ? null : before.snapshot().version());
        try {
            if (!remote().backfillFile(GROUP, dataKey, completed)) throw new IOException("WebDAV payload was not verified in S3");
            publish(path, new Entry(path, false, Files.size(completed.toPath()), System.currentTimeMillis(), dataKey), null, reservation);
        } catch (IOException | DotDataException | RuntimeException failure) {
            try {
                // Restore only our own reservation; never replace a newer upload or resurrect a deletion.
                publish(path, existing, null, reservation);
            } catch (Exception rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    /** Immutable cache names prevent another upload from changing an in-flight response. */
    public File materialize(Entry entry) throws IOException, DotDataException {
        if (entry == null || entry.directory() || entry.expired()) throw new IOException("Missing WebDAV staging file");
        file(entry.path());
        final Path data = Path.of(entry.dataKey());
        if (data.isAbsolute() || !data.normalize().equals(data) || !entry.dataKey().startsWith("data/")) {
            throw new IOException("Invalid WebDAV payload key");
        }
        final Path cache = root().resolve(".webdav-cache").resolve(data);
        if (!cache.toFile().getCanonicalFile().toPath().equals(cache)) throw new IOException("Invalid WebDAV cache path");
        if (!Files.isRegularFile(cache)) {
            final File download = remote().pullFile(GROUP, entry.dataKey());
            if (download == null) throw new IOException("Missing WebDAV payload: " + entry.path());
            try {
                Files.createDirectories(cache.getParent());
                final Path stage = Files.createTempFile(cache.getParent(), ".download-", ".tmp");
                try {
                    Files.copy(download.toPath(), stage, StandardCopyOption.REPLACE_EXISTING);
                    if (Files.size(stage) != entry.size()) throw new IOException("Incomplete WebDAV payload");
                    Files.move(stage, cache, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(stage);
                }
            } finally {
                remote().releaseRetrievedFile(download);
            }
        }
        return cache.toFile();
    }

    public void delete(File target) throws IOException, DotDataException {
        final String key = key(target);
        final var objects = new ArrayList<>(remote().listObjectPaths(GROUP, recordPrefix(key)));
        objects.sort(Comparator.reverseOrder());

        for (String object : objects) {
            final String path = recordPath(object);
            if (!path.equals(key) && !path.startsWith(key + "/")) throw new IOException("Invalid staging deletion path");
            final State current = state(path);
            if (current == null) continue;
            publish(path, null, null, current.snapshot().version());
            final Entry entry = current.entry();
            if (entry != null && !entry.directory()) remove(entry.dataKey());
            // A pending upload may still be running. Its reservation is gone, so it cannot publish;
            // its completed payload, if any, is reclaimed by the reference-aware expiry pass.
        }
        com.liferay.util.FileUtil.deltree(target, true);
    }

    /** Uses a server timestamp so node clock skew cannot age a newly created reservation. */
    private long cleanupCutoff() throws DotDataException {
        final int hours = Config.getIntProperty("CLEANUP_TMP_FILES_OLDER_THAN_HOURS", 3);
        if (hours < 0) throw new DotDataException("WebDAV cleanup age must not be negative");
        final String path = "maintenance/" + UUID.randomUUID();
        final String version = remote().writeObjectIfMatch(GROUP, path, new HashMap<String, Serializable>(), null);
        if (version == null) throw new DotDataException("Unable to establish S3 cleanup time");
        try {
            final var clock = remote().readObjectSnapshot(GROUP, path, READER);
            if (clock == null || !version.equals(clock.version())) throw new DotDataException("S3 cleanup clock changed");
            return clock.modified() - hours * 3_600_000L;
        } finally {
            remote().deleteObjectAndReferences(GROUP, path);
        }
    }

    /** Fence expired reservations with conditional tombstones, then collect aged, unreferenced payloads. */
    public void cleanupExpired() throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return;
        final long cutoff = cleanupCutoff();
        final var references = new java.util.HashSet<String>();
        try {
            for (String object : remote().listObjectPaths(GROUP, "entries/")) {
                final State current = state(recordPath(object));
                if (current == null) continue;
                if (current.entry() == null && current.pending() == null) continue;
                if (current.snapshot().modified() < cutoff) {
                    // Never DELETE a mutable record: some S3-compatible stores ignore If-Match on DELETE.
                    // A unique tombstone fences late writers. A conflicting publication aborts this pass.
                    publish(recordPath(object), null, null, current.snapshot().version());
                } else {
                    if (current.entry() != null && !current.entry().directory()) references.add(current.entry().dataKey());
                    if (current.pending() != null) references.add(current.pending());
                }
            }
            // New reservations are written before payload creation. Payloads published after this
            // scan are either in references, younger than cutoff, or fenced by a tombstoned reservation.
            for (var object : remote().listObjectSnapshots(GROUP, "data/")) {
                if (!object.path().matches("data/[0-9]+/[0-9a-f-]{36}/[^/]+")) {
                    throw new IOException("Invalid WebDAV payload inventory");
                }
                if (object.modified() < cutoff && !references.contains(object.path())) {
                    // Payload keys are unique and immutable. Every publisher first reserves its reference.
                    remove(object.path());
                }
            }
            for (var object : remote().listObjectSnapshots(GROUP, "maintenance/")) {
                if (object.modified() < cutoff) remove(object.path());
            }
        } catch (IOException failure) {
            throw new DotDataException("Unable to clean WebDAV staging objects", failure);
        }
    }

    private void remove(String key) throws DotDataException {
        if (!remote().deleteObjectAndReferences(GROUP, key)) throw new DotDataException("Unable to delete WebDAV object " + key);
    }

    public void copy(File source, File destination) throws IOException, DotDataException {
        final String from = key(source), to = key(destination);
        if (from.equals(to)) return;
        if (to.startsWith(from + "/")) throw new IOException("Cannot copy a staging directory into itself");
        final Entry entry = stat(source);
        if (entry == null) throw new IOException("Missing WebDAV copy source");
        if (entry.directory()) {
            mkdir(destination);
            for (Entry child : children(source)) copy(file(child.path()), new File(destination, Path.of(child.path()).getFileName().toString()));
        } else store(destination, materialize(entry));
    }
}
