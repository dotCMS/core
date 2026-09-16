package com.dotcms.storage;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.dotcms.enterprise.publishing.storage.Storage;
import com.dotmarketing.exception.DotDataException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.apache.commons.io.IOUtils;

/** Immutable shared bytes, addressed by SHA-256, behind ordinary content-owned S3 references. */
final class S3ContentAddressedStorage {
    static final String BLOB_PREFIX = "asset-blobs/sha256/";
    static final String HASH_HEADER = "dotcms-blob-sha256";
    private final Storage storage;
    private final String bucket;

    S3ContentAddressedStorage(Storage storage, String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    static String blobKey(String hash) {
        if (hash == null || !hash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid asset SHA-256");
        return BLOB_PREFIX + hash.substring(0, 2) + "/" + hash.substring(2, 4) + "/"
                + hash.substring(4, 6) + "/" + hash.substring(6, 8) + "/" + hash;
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static String hash(File file) throws IOException {
        final var digest = digest();
        try (var input = new DigestInputStream(Files.newInputStream(file.toPath()), digest)) {
            input.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    String store(String ownerKey, File source, boolean backfill) throws DotDataException {
        Path snapshot = null;
        Path reference = null;
        try {
            // Hash and upload the same private snapshot, even if a caller subsequently edits its file.
            snapshot = Files.createTempFile("s3-blob-", ".tmp");
            final var digest = digest();
            try (var input = new DigestInputStream(Files.newInputStream(source.toPath()), digest);
                 var output = Files.newOutputStream(snapshot)) { input.transferTo(output); }
            final String hash = HexFormat.of().formatHex(digest.digest());
            final String blob = blobKey(hash);
            String previousVersion = null;
            if (backfill) {
                try (var owner = storage.getObject(bucket, ownerKey)) {
                    if (owner != null) {
                        previousVersion = owner.getObjectMetadata().getETag();
                        final String storedHash = owner.getObjectMetadata().getUserMetaDataOf(HASH_HEADER);
                        if (storedHash != null) {
                            consumeReference(owner);
                            if (!hash.equals(storedHash)) throw new DotDataException("Backfill conflicts with an existing asset reference");
                            if (blobMatches(blob, snapshot.toFile())) return hash;
                        } else {
                            try (var local = Files.newInputStream(snapshot)) {
                                if (!IOUtils.contentEquals(local, owner.getObjectContent())) {
                                    throw new DotDataException("Backfill conflicts with an existing raw asset");
                                }
                            }
                        }
                    }
                }
            }
            if (!blobMatches(blob, snapshot.toFile())) {
                try { storage.uploadFileIfAbsent(bucket, blob, snapshot.toFile()); }
                catch (RuntimeException conflict) {
                    if (!preconditionFailure(conflict)) throw conflict;
                }
                if (!blobMatches(blob, snapshot.toFile())) {
                    throw new DotDataException("Shared asset bytes do not match their SHA-256 key");
                }
            }
            reference = Files.createTempFile("s3-asset-reference-", ".json");
            Files.writeString(reference, "{\"sha256\":\"" + hash + "\",\"length\":" + Files.size(snapshot) + "}");
            final var metadata = new ObjectMetadata();
            metadata.setContentType("application/vnd.dotcms.asset-reference+json");
            metadata.addUserMetadata(HASH_HEADER, hash);
            final var request = new PutObjectRequest(bucket, ownerKey, reference.toFile()).withMetadata(metadata);
            if (backfill) {
                request.putCustomRequestHeader(previousVersion == null ? "If-None-Match" : "If-Match",
                        previousVersion == null ? "*" : "\"" + previousVersion + "\"");
            }
            try { storage.uploadFile(request).waitForCompletion(); }
            catch (Exception conflict) {
                if (!backfill || !preconditionFailure(conflict)) throw conflict;
            }
            try (var published = storage.getObject(bucket, ownerKey)) {
                if (published == null || !hash.equals(published.getObjectMetadata().getUserMetaDataOf(HASH_HEADER))) {
                    throw new DotDataException("Asset reference publication was not verified");
                }
                consumeReference(published);
            }
            if (!matches(ownerKey, snapshot.toFile())) throw new DotDataException("Shared asset bytes were not verified after publication");
            return hash;
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new DotDataException("Unable to store shared asset " + ownerKey, failure);
        } finally {
            remove(snapshot);
            remove(reference);
        }
    }

    private static boolean preconditionFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.amazonaws.services.s3.model.AmazonS3Exception s3
                    && (s3.getStatusCode() == 412 || s3.getStatusCode() == 409)) return true;
        }
        return false;
    }

    /** Read old raw objects too; only an explicit S3 metadata marker denotes a reference. */
    boolean retrieve(String ownerKey, File destination) throws IOException, DotDataException {
        try (var owner = storage.getObject(bucket, ownerKey)) {
            if (owner == null) return false;
            final String hash = owner.getObjectMetadata().getUserMetaDataOf(HASH_HEADER);
            if (hash == null) {
                try (var output = Files.newOutputStream(destination.toPath())) { owner.getObjectContent().transferTo(output); }
                return true;
            }
            consumeReference(owner);
            try (var blob = openBlob(hash)) {
                if (blob == null) throw new DotDataException("Asset reference points to a missing shared blob");
                final var digest = digest();
                try (var input = new DigestInputStream(blob.getObjectContent(), digest);
                     var output = Files.newOutputStream(destination.toPath())) { input.transferTo(output); }
                if (!hash.equals(HexFormat.of().formatHex(digest.digest()))) {
                    throw new DotDataException("Downloaded asset does not match its SHA-256 reference");
                }
            }
            return true;
        }
    }

    boolean matches(String ownerKey, File file) throws IOException {
        try (var owner = storage.getObject(bucket, ownerKey)) {
            if (owner == null) return false;
            final String hash = owner.getObjectMetadata().getUserMetaDataOf(HASH_HEADER);
            if (hash == null) {
                try (InputStream local = Files.newInputStream(file.toPath())) {
                    return IOUtils.contentEquals(local, owner.getObjectContent());
                }
            }
            consumeReference(owner);
            if (!hash.equals(hash(file))) return false;
            try (var blob = openBlob(hash); var local = Files.newInputStream(file.toPath())) {
                return blob != null && IOUtils.contentEquals(local, blob.getObjectContent());
            }
        }
    }

    private com.amazonaws.services.s3.model.S3Object openBlob(String hash) {
        final var blob = storage.getObject(bucket, blobKey(hash));
        // Existing one-level blobs remain readable; only absence permits fallback.
        return blob != null ? blob : storage.getObject(bucket, BLOB_PREFIX + hash.substring(0, 2) + "/" + hash);
    }

    private boolean blobMatches(String key, File file) throws IOException {
        try (var object = storage.getObject(bucket, key)) {
            if (object == null) return false;
            try (var input = Files.newInputStream(file.toPath())) {
                return object.getObjectMetadata().getContentLength() == file.length()
                        && IOUtils.contentEquals(input, object.getObjectContent());
            }
        }
    }

    private static void consumeReference(com.amazonaws.services.s3.model.S3Object owner) throws IOException {
        if (owner.getObjectMetadata().getContentLength() > 256) throw new IOException("Invalid asset reference size");
        owner.getObjectContent().transferTo(java.io.OutputStream.nullOutputStream());
    }

    private static void remove(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException failure) { com.dotmarketing.util.Logger.warn(S3ContentAddressedStorage.class,
                "Unable to remove shared asset staging file", failure); }
    }
}
