package com.dotcms.storage;

import com.dotmarketing.exception.DotDataException;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** Only byte-derived extraction goes here; filenames and editorial metadata are merged afterwards. */
final class SharedExtractedMetadata {
    static final String GROUP = "extracted-metadata";
    private final StoragePersistenceAPI storage;

    private static class Holder {
        private static final SharedExtractedMetadata INSTANCE = new SharedExtractedMetadata(
                StoragePersistenceProvider.remoteObjectStorage());
    }

    static SharedExtractedMetadata getInstance() { return Holder.INSTANCE; }
    SharedExtractedMetadata(StoragePersistenceAPI storage) { this.storage = storage; }

    /**
     * Returns the byte-derived metadata for a file, sharing one extraction across every copy of the
     * same bytes when S3 asset storage is on. Extracts directly, without the shared cache, when the
     * feature is off or the extractor version is unknown, because unknown parser bundles must not
     * share one cached result.
     *
     * @param source           the file to extract from
     * @param extractorVersion the Tika bundle version, or {@code null} when it cannot be determined
     * @param schemaVersion    the metadata schema version, part of the cache key
     * @param textLimit        the maximum extracted text length, part of the cache key
     * @param extract          the extraction to run on a cache miss
     * @return the extracted metadata, as a mutable map
     * @throws Exception when copying, hashing, or publishing the extraction fails
     */
    @SuppressWarnings("unchecked")
    Map<String, Serializable> get(File source, String extractorVersion, int schemaVersion, int textLimit,
            Function<File, Map<String, Serializable>> extract) throws Exception {
        if (!AssetStorageFeature.isEnabled()) return extract.apply(source);
        if (extractorVersion == null || extractorVersion.isBlank()) return extract.apply(source);
        final var snapshot = Files.createTempFile("asset-extraction-", ".tmp");
        try {
            Files.copy(source.toPath(), snapshot, StandardCopyOption.REPLACE_EXISTING);
            final String hash = S3ContentAddressedStorage.hash(snapshot.toFile());
            final String configuration = org.apache.commons.codec.digest.DigestUtils.sha256Hex(
                    "tika:" + extractorVersion + ":schema:" + schemaVersion + ":text-limit:" + textLimit);
            final String key = hash + "/" + configuration + ".json";
            final var reader = new JsonReaderDelegate<>(Map.class);
            final Map<String, Serializable> existing = (Map<String, Serializable>) storage.pullObject(GROUP, key, reader);
            if (existing != null) return new HashMap<>(existing);
            final Map<String, Serializable> extracted = extract.apply(snapshot.toFile());
            // Tika adds size on failures too; only a completed extraction includes its content field.
            if (!extracted.containsKey(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.CONTENT_FIELD)) return extracted;
            storage.createGroup(GROUP);
            storage.writeObjectIfMatch(GROUP, key, new TreeMap<>(extracted), null);
            // Concurrent extraction may differ in incidental parser diagnostics. Use the first published result.
            final Map<String, Serializable> published = (Map<String, Serializable>) storage.pullObject(GROUP, key, reader);
            if (published == null) throw new DotDataException("Extracted metadata publication was not verified");
            return new HashMap<>(published);
        } finally { Files.deleteIfExists(snapshot); }
    }
}
