package com.dotcms.storage.binary;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "s3.cms.enabled", matches = "true")
public class ContentletBackupStorageTest {
    @BeforeAll static void initialize() throws Exception { IntegrationTestInitService.getInstance().init(); }
    @TempDir Path uploads;

    /**
     * Deletes content whose binaries and metadata have been evicted from local disk, so the backup
     * must restore them from S3, and checks that each deleted version gets its own recovery ZIP with
     * the exact bytes and metadata the database row points to. Runs once per deletion mode: full
     * destroy, all versions, a single version and one language. Versions the deletion keeps must
     * still serve their binaries afterwards.
     *
     * @param deletion the deletion mode to exercise
     * @throws Exception if the content setup, the deletion or reading the archives fails
     */
    @ParameterizedTest
    @ValueSource(strings = {"destroy", "allVersions", "version", "language"})
    void coldBackupsSurviveCommittedBinaryAndMetadataCleanup(String deletion) throws Exception {
        final String previous = Config.getStringProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", null);
        final var type = new ContentTypeDataGen().nextPersisted();
        final var backups = ContentletBackupStorage.getInstance();
        String identifier = null;
        try {
            for (String name : List.of("heroImage", "attachment", "optionalBinary")) {
                ContentTypeDataGen.addField(new FieldDataGen().velocityVarName(name).contentTypeId(type.id())
                        .type(BinaryField.class).nextPersisted());
            }
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("first", "original image"));
            generator.setProperty("attachment", source("attachment", "separate attachment"));
            final var api = APILocator.getContentletAPI();
            final var user = APILocator.systemUser();
            Contentlet first = generator.nextPersisted();
            identifier = first.getIdentifier();
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(first,
                    Map.of("heroImage", Map.of("credit", "Original author")));
            first = api.find(first.getInode(), user, false);
            final Contentlet edit = api.checkout(first.getInode(), user, false);
            edit.setBinary("heroImage", source("second", "new image version"));
            final Contentlet second = api.checkin(edit, user, false);
            assertNotEquals(first.getInode(), second.getInode());
            // Deleting one language of multilingual content takes the per-language path (#9146).
            final Contentlet otherLanguage = deletion.equals("language") ? ContentletDataGen.createNewVersion(
                    second, VariantAPI.DEFAULT_VARIANT, new LanguageDataGen().nextPersisted(), null) : null;
            final List<Contentlet> deleted = deletion.equals("version") ? List.of(first) : List.of(first, second);
            final var expected = new HashMap<String, Map<String, byte[]>>();
            for (Contentlet content : deleted) {
                final var entries = new HashMap<String, byte[]>();
                for (String field : List.of("heroImage", "attachment")) {
                    final var binary = content.getBinary(field);
                    entries.put("assets/" + BinaryAssetReference.keyOf(binary), Files.readAllBytes(binary.toPath()));
                    org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
                            .until(() -> APILocator.getBinaryAssetStorageAPI().evictLocalFile(binary));
                    final String metadataPath = storedMetadataPath(content, field);
                    final var metadataKey = new com.dotcms.storage.StorageKey.Builder()
                            .group(com.dotcms.storage.FileMetadataAPI.DOT_METADATA).path(metadataPath)
                            .storage(com.dotcms.storage.StoragePersistenceProvider.getStorageType()).build();
                    final var raw = APILocator.getFileStorageAPI().retrieveRawMetaData(metadataKey);
                    if (raw != null) {
                        assertTrue(APILocator.getFileStorageAPI().backfillMetadata(metadataKey));
                        entries.put("assets/" + metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT),
                                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(raw));
                        Files.deleteIfExists(Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath(),
                                metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT)));
                    }
                }
                expected.put(content.getInode(), entries);
            }
            final var retired = APILocator.getBinaryAssetStorageAPI().storeRevision(first.getInode(),
                    "retiredBinary", "Shared-Mixed.Txt", source("retired", "retired field bytes"));
            final var persistedMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final var raw = (com.fasterxml.jackson.databind.node.ObjectNode) persistedMapper.readTree(new DotConnect()
                    .setSQL("select contentlet_as_json from contentlet where inode = ?").addParam(first.getInode())
                    .getString("contentlet_as_json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) raw.get("fields")).set("retiredBinary", persistedMapper.valueToTree(
                    com.dotcms.content.model.type.system.BinaryFieldType.builder().value(retired.getName())
                            .storageKey(BinaryAssetReference.keyOf(retired)).build()));
            new DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(persistedMapper.writeValueAsString(raw)).addParam(first.getInode()).loadResult();
            expected.get(first.getInode()).put("assets/" + BinaryAssetReference.keyOf(retired), Files.readAllBytes(retired.toPath()));
            assertTrue(APILocator.getBinaryAssetStorageAPI().evictLocalFile(retired));
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", true);
            switch (deletion) {
                case "destroy" -> api.destroy(second, user, false);
                case "allVersions" -> api.deleteAllVersionsandBackup(List.of(second), user, false);
                case "version" -> api.deleteVersion(first, user, false);
                case "language" -> {
                    api.archive(api.find(second.getInode(), user, false), user, false);
                    api.delete(api.find(second.getInode(), user, false), user, false);
                }
                default -> throw new AssertionError(deletion);
            }
            for (Contentlet content : deleted) {
                assertTrue(new DotConnect().setSQL("select inode from contentlet where inode = ?")
                        .addParam(content.getInode()).loadObjectResults().isEmpty());
                final var jobs = new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> 'inode' = ?")
                        .addParam(BinaryAssetCleanupProcessor.QUEUE).addParam(content.getInode()).loadObjectResults();
                assertEquals(1, jobs.size(), "Each deleted inode needs exactly one cleanup job, even when listed twice");
                for (var job : jobs) new BinaryAssetCleanupProcessor().process(
                        APILocator.getJobQueueManagerAPI().getJob(job.get("id").toString()));
                assertTrue(APILocator.getBinaryAssetStorageAPI().listBinaryPaths(content.getInode()).isEmpty());
                assertNull(APILocator.getFileMetadataAPI().getMetadata(content, "heroImage"));
            }
            final var keys = backups.list(identifier);
            assertEquals(deleted.size(), keys.size(), "Each deleted inode needs its own complete backup");
            for (String key : keys) {
                final var entries = new HashMap<String, byte[]>();
                try (var zip = new ZipInputStream(backups.open(key))) {
                    for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        assertNull(entries.put(entry.getName(), zip.readAllBytes()));
                    }
                }
                final String inode = key.split("/")[1];
                assertTrue(new String(entries.get("contentlet.xml"), java.nio.charset.StandardCharsets.UTF_8).contains(inode));
                assertNotNull(new com.fasterxml.jackson.databind.ObjectMapper().readTree(entries.get("contentlet.json")).get("fields"));
                for (var entry : expected.get(inode).entrySet()) {
                    assertTrue(entries.containsKey(entry.getKey()), "Backup " + key + " is missing " + entry.getKey());
                    if (entry.getKey().endsWith("-metadata.json")) {
                        final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        assertEquals(mapper.readTree(entry.getValue()), mapper.readTree(entries.get(entry.getKey())));
                    } else assertArrayEquals(entry.getValue(), entries.get(entry.getKey()));
                }
            }
            if (otherLanguage != null) {
                assertEquals("new image version", Files.readString(api.find(otherLanguage.getInode(), user, false)
                        .getBinary("heroImage").toPath()), "Deleting one language must keep the other's binaries");
            }
            if (deletion.equals("version")) {
                final Contentlet kept = api.find(second.getInode(), user, false);
                assertEquals("new image version", Files.readString(kept.getBinary("heroImage").toPath()));
                // The unchanged field would lose its bytes if versions ever shared a revision.
                assertEquals("separate attachment", Files.readString(kept.getBinary("attachment").toPath()));
            }
        } finally {
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", false);
            ContentTypeDataGen.remove(type);
            if (identifier != null) {
                final var remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
                for (String key : backups.list(identifier)) remote.deleteObjectAndReferences(ContentletBackupStorage.GROUP, key);
            }
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", previous);
        }
    }

    /**
     * A binary and its metadata that are already gone everywhere (no local copy and no S3 object)
     * must not block deleting the content. The archive still holds every other field and lists the
     * missing files in {@code missing.json}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"destroy", "version"})
    void alreadyMissingSourcesAreRecordedWithoutBlockingDeletion(String deletion) throws Exception {
        final String previous = Config.getStringProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", null);
        final var type = new ContentTypeDataGen().nextPersisted();
        final var backups = ContentletBackupStorage.getInstance();
        String identifier = null;
        try {
            for (String name : List.of("heroImage", "attachment")) {
                ContentTypeDataGen.addField(new FieldDataGen().velocityVarName(name).contentTypeId(type.id())
                        .type(BinaryField.class).nextPersisted());
            }
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("lost", "lost image"));
            generator.setProperty("attachment", source("kept", "kept attachment"));
            final var api = APILocator.getContentletAPI();
            final var user = APILocator.systemUser();
            Contentlet first = generator.nextPersisted();
            identifier = first.getIdentifier();
            // Custom attributes give heroImage a metadata revision that the content row references.
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(first,
                    Map.of("heroImage", Map.of("credit", "Lost author")));
            first = api.find(first.getInode(), user, false);
            final Contentlet target = first;
            if (deletion.equals("version")) {
                // deleteVersion only removes a version that is not the working one.
                final Contentlet edit = api.checkout(first.getInode(), user, false);
                edit.setBinary("heroImage", source("current", "current image"));
                api.checkin(edit, user, false);
            }

            final var binaries = APILocator.getBinaryAssetStorageAPI();
            final var lost = target.getBinary("heroImage");
            final String lostKey = BinaryAssetReference.keyOf(lost);
            final String lostMetadataKey = BinaryAssetReference.metadataKeyOf(lost);
            assertNotNull(lostMetadataKey, "The test needs a referenced metadata revision");
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
                    .until(() -> binaries.evictLocalFile(lost));
            binaries.deleteBinaryPaths(target.getInode(), "heroImage", List.of(lostKey));
            final String metadataPath = APILocator.getFileMetadataAPI().getFileName(target, "heroImage");
            com.dotcms.storage.StoragePersistenceProvider.INSTANCE.get()
                    .getStorage(com.dotcms.storage.StoragePersistenceProvider.getStorageType())
                    .deleteObjectAndReferences(com.dotcms.storage.FileMetadataAPI.DOT_METADATA, metadataPath);
            Files.deleteIfExists(Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath(),
                    metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT)));
            assertThrows(java.nio.file.NoSuchFileException.class, () -> binaries.openLocalFile(lost).close(),
                    "Precondition: the binary must be gone locally and in S3");
            assertNull(APILocator.getFileStorageAPI().retrieveRawMetaData(new com.dotcms.storage.StorageKey.Builder()
                    .group(com.dotcms.storage.FileMetadataAPI.DOT_METADATA).path(metadataPath)
                    .storage(com.dotcms.storage.StoragePersistenceProvider.getStorageType()).build()),
                    "Precondition: the metadata must be gone locally and in S3");
            final var attachment = target.getBinary("attachment");
            final byte[] attachmentBytes = Files.readAllBytes(attachment.toPath());

            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", true);
            switch (deletion) {
                case "destroy" -> api.destroy(target, user, false);
                case "version" -> api.deleteVersion(target, user, false);
                default -> throw new AssertionError(deletion);
            }
            assertTrue(new DotConnect().setSQL("select inode from contentlet where inode = ?")
                    .addParam(target.getInode()).loadObjectResults().isEmpty(), "Deletion must go through");

            final var keys = backups.list(identifier).stream()
                    .filter(key -> key.split("/")[1].equals(target.getInode())).toList();
            assertEquals(1, keys.size(), "The deleted version still needs its recovery archive");
            final var entries = new HashMap<String, byte[]>();
            try (var zip = new ZipInputStream(backups.open(keys.getFirst()))) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    assertNull(entries.put(entry.getName(), zip.readAllBytes()));
                }
            }
            assertNotNull(entries.get("contentlet.json"));
            assertArrayEquals(attachmentBytes, entries.get("assets/" + BinaryAssetReference.keyOf(attachment)),
                    "Fields that still have their bytes must be archived");
            assertFalse(entries.containsKey("assets/" + lostKey), "A missing binary must not leave an empty entry");
            final var manifest = new com.fasterxml.jackson.databind.ObjectMapper().readTree(entries.get("missing.json"));
            assertNotNull(manifest, "The archive must list what it could not include");
            final var missing = new HashMap<String, String>();
            for (var item : manifest) {
                assertEquals("heroImage", item.get("field").asText());
                missing.put(item.get("kind").asText(), item.get("path").asText());
            }
            assertEquals(Map.of("binary", "assets/" + lostKey,
                    "metadata", "assets/" + metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT)), missing);
        } finally {
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", false);
            ContentTypeDataGen.remove(type);
            if (identifier != null) removeBackups(identifier);
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", previous);
        }
    }

    /**
     * A deleted inode can come back before its cleanup job runs (push publishing keeps the sender's
     * inodes). The job must then finish: delete the recorded revisions the returned row no longer
     * references, with their metadata, and keep everything the row does reference.
     */
    @Test
    void cleanupOfAReturnedInodeDeletesOnlyUnreferencedRevisions() throws Exception {
        final var type = new ContentTypeDataGen().nextPersisted();
        try {
            for (String name : List.of("heroImage", "attachment")) {
                ContentTypeDataGen.addField(new FieldDataGen().velocityVarName(name).contentTypeId(type.id())
                        .type(BinaryField.class).nextPersisted());
            }
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("returned-old", "old hero"));
            generator.setProperty("attachment", source("returned-kept", "kept attachment"));
            Contentlet content = generator.nextPersisted();
            final String inode = content.getInode();
            // Custom attributes give the old hero revision a metadata file of its own.
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(content,
                    Map.of("heroImage", Map.of("credit", "Old author")));
            content = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            final var binaries = APILocator.getBinaryAssetStorageAPI();
            final String oldHero = BinaryAssetReference.keyOf(content.getBinary("heroImage"));
            final String oldHeroMetadata = BinaryAssetReference.metadataKeyOf(content.getBinary("heroImage"));
            final String keptAttachment = BinaryAssetReference.keyOf(content.getBinary("attachment"));
            assertNotNull(oldHero);
            assertNotNull(oldHeroMetadata);
            assertNotNull(keptAttachment);

            // The deletion records the inode's inventory...
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                BinaryAssetCleanupProcessor.enqueue(inode);
                com.dotmarketing.db.HibernateUtil.commitTransaction();
            } catch (Exception failure) {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
                throw failure;
            }
            final var jobs = new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> 'inode' = ?")
                    .addParam(BinaryAssetCleanupProcessor.QUEUE).addParam(inode).loadObjectResults();
            assertEquals(1, jobs.size());
            final var recorded = APILocator.getJobQueueManagerAPI().getJob(jobs.getFirst().get("id").toString());
            assertTrue(((List<?>) recorded.parameters().get("binaries")).containsAll(List.of(oldHero, keptAttachment)));
            assertTrue(((List<?>) recorded.parameters().get("metadata")).contains(oldHeroMetadata));

            // ...and the inode comes back referencing the recorded attachment plus a new hero revision.
            final var newHero = binaries.storeRevision(inode, "heroImage", "Shared-Mixed.Txt", source("returned-new", "new hero"));
            final String newHeroKey = BinaryAssetReference.keyOf(newHero);
            final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(new DotConnect()
                    .setSQL("select contentlet_as_json from contentlet where inode = ?").addParam(inode)
                    .getString("contentlet_as_json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields")).set("heroImage", mapper.valueToTree(
                    com.dotcms.content.model.type.system.BinaryFieldType.builder().value(newHero.getName())
                            .storageKey(newHeroKey).build()));
            new DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(mapper.writeValueAsString(json)).addParam(inode).loadResult();
            com.dotmarketing.business.CacheLocator.getContentletCache().remove(inode);

            new BinaryAssetCleanupProcessor().process(recorded);

            final var remaining = binaries.listBinaryPaths(inode);
            assertFalse(remaining.contains(oldHero), "The unreferenced old revision is deleted");
            assertTrue(remaining.contains(keptAttachment), "A recorded key the returned row references is kept");
            assertTrue(remaining.contains(newHeroKey), "A revision uploaded after the deletion is kept");
            final var metadataKey = new com.dotcms.storage.StorageKey.Builder()
                    .group(com.dotcms.storage.FileMetadataAPI.DOT_METADATA)
                    .storage(com.dotcms.storage.StoragePersistenceProvider.getStorageType());
            assertNull(APILocator.getFileStorageAPI().retrieveRawMetaData(metadataKey.path(oldHeroMetadata).build()),
                    "The old revision's metadata goes with it");
            final var returned = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
            assertEquals("kept attachment", Files.readString(returned.getBinary("attachment").toPath()));
            assertEquals("new hero", Files.readString(returned.getBinary("heroImage").toPath()));
            new BinaryAssetCleanupProcessor().process(recorded); // A retry after success is harmless.
            assertTrue(binaries.listBinaryPaths(inode).containsAll(List.of(keptAttachment, newHeroKey)));
        } finally {
            ContentTypeDataGen.remove(type);
        }
    }

    @Test
    void failedBackupAbortsDeletionAndPreservesSources() throws Exception {
        final String previous = Config.getStringProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", null);
        final var type = new ContentTypeDataGen().nextPersisted();
        try {
            ContentTypeDataGen.addField(new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted());
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("failure", "recoverable bytes"));
            final Contentlet content = generator.nextPersisted();
            final var binary = content.getBinary("heroImage");
            final var remote = spy(AmazonS3StoragePersistenceAPIImpl.withPlainPaths());
            doThrow(new DotDataException("injected backup outage")).when(remote)
                    .backfillFile(eq(ContentletBackupStorage.GROUP), anyString(), any());
            final var backups = new ContentletBackupStorage(remote);
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", true);
            try (var factory = mockStatic(ContentletBackupStorage.class)) {
                factory.when(ContentletBackupStorage::getInstance).thenReturn(backups);
                assertThrows(DotDataException.class, () -> APILocator.getContentletAPI().destroy(content, APILocator.systemUser(), false));
            }
            assertFalse(new DotConnect().setSQL("select inode from contentlet where inode = ?")
                    .addParam(content.getInode()).loadObjectResults().isEmpty());
            assertFalse(APILocator.getVersionableAPI().isDeleted(APILocator.getContentletAPI()
                    .find(content.getInode(), APILocator.systemUser(), false)), "Failed backup must roll back the archive state too");
            assertTrue(new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> 'inode' = ?")
                    .addParam(BinaryAssetCleanupProcessor.QUEUE).addParam(content.getInode()).loadObjectResults().isEmpty());
            assertTrue(binary.isFile(), "Backup must not move the caller's local source");
            assertTrue(APILocator.getBinaryAssetStorageAPI().evictLocalFile(binary));
            assertEquals("recoverable bytes", Files.readString(APILocator.getContentletAPI()
                    .find(content.getInode(), APILocator.systemUser(), false).getBinary("heroImage").toPath()));
            assertTrue(backups.list(content.getIdentifier()).isEmpty());
        } finally {
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", false);
            ContentTypeDataGen.remove(type);
            Config.setProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", previous);
        }
    }

    @Test
    void removedFieldArchivesHistoricalAndUnreferencedRevisionsWithoutDeletingNewUploads() throws Exception {
        final var type = new ContentTypeDataGen().nextPersisted();
        String identifier = null;
        try {
            final var field = new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(field);
            ContentTypeDataGen.addField(new FieldDataGen().velocityVarName("attachment").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted());
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("field-first", "first field bytes"));
            generator.setProperty("attachment", source("field-sibling", "keep sibling"));
            final var api = APILocator.getContentletAPI();
            final var user = APILocator.systemUser();
            final Contentlet first = generator.nextPersisted();
            identifier = first.getIdentifier();
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(first, Map.of("heroImage", Map.of("credit", "Keep this credit")));
            final var edit = api.checkout(first.getInode(), user, false);
            edit.setBinary("heroImage", source("field-second", "second field bytes"));
            final Contentlet second = api.checkin(edit, user, false);
            final var binaries = APILocator.getBinaryAssetStorageAPI();
            final var orphan = binaries.storeRevision(first.getInode(), "heroImage", "Extra-Mixed-metadata.json", source("orphan", "unreferenced revision"));
            final Map<String, byte[]> expected = new HashMap<>();
            for (Contentlet content : List.of(first, second)) {
                for (String path : binaries.listBinaryPaths(content.getInode())) {
                    if (!path.contains("/heroImage/") || !(path.endsWith("/Shared-Mixed.Txt") || path.endsWith("/Extra-Mixed-metadata.json"))) continue;
                    final var file = new java.io.File(com.dotmarketing.util.ConfigUtils.getAssetPath(), path);
                    try (var input = binaries.openLocalFile(file)) { expected.put("binary-assets/" + path, input.readAllBytes()); }
                    org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() -> binaries.evictLocalFile(file));
                }
            }
            APILocator.getContentTypeFieldAPI().delete(field, user);
            final var requests = fieldJobs("type", type.id());
            assertEquals(1, requests.size(), "Field deletion must persist its cleanup request");
            new BinaryFieldCleanupProcessor().process(requests.getFirst());
            final Map<String, byte[]> archived = new HashMap<>();
            final var backups = ContentletBackupStorage.getInstance();
            assertEquals(2, backups.list(identifier).size(), "Historical and working versions both need archives");
            for (String key : backups.list(identifier)) {
                try (var zip = new ZipInputStream(backups.open(key))) {
                    for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        if (!entry.getName().equals("contentlet.json")) archived.put(entry.getName(), zip.readAllBytes());
                    }
                }
            }
            for (var entry : expected.entrySet()) assertArrayEquals(entry.getValue(), archived.get(entry.getKey()));
            assertTrue(archived.values().stream().anyMatch(bytes -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8).contains("Keep this credit")));

            // A later incarnation of the same field must survive an old cleanup job and its retries.
            ContentTypeDataGen.addField(new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted());
            final var replacement = binaries.storeRevision(second.getInode(), "heroImage", "New-Mixed.Txt", source("recreated", "new field incarnation"));
            final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(new DotConnect()
                    .setSQL("select contentlet_as_json from contentlet where inode = ?").addParam(second.getInode()).getString("contentlet_as_json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields")).putObject("heroImage")
                    .put("type", "Binary").put("value", replacement.getName()).put("storageKey", BinaryAssetReference.keyOf(replacement));
            new DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(mapper.writeValueAsString(json)).addParam(second.getInode()).loadResult();
            for (Contentlet content : List.of(first, second)) {
                final var jobs = fieldJobs("inode", content.getInode());
                assertEquals(1, jobs.size());
                new BinaryFieldCleanupProcessor().process(jobs.getFirst());
                new BinaryFieldCleanupProcessor().process(jobs.getFirst());
                com.dotmarketing.business.CacheLocator.getContentletCache().remove(content.getInode());
                assertEquals("keep sibling", Files.readString(api.find(content.getInode(), user, false).getBinary("attachment").toPath()));
                assertTrue(binaries.listBinaryPaths(content.getInode()).stream()
                        .noneMatch(path -> expected.containsKey("binary-assets/" + path)));
            }
            assertEquals("new field incarnation", Files.readString(api.find(second.getInode(), user, false).getBinary("heroImage").toPath()));
            assertFalse(orphan.exists());
        } finally {
            ContentTypeDataGen.remove(type);
            if (identifier != null) removeBackups(identifier);
        }
    }

    /**
     * One row whose removed field points at a binary and metadata that are already gone everywhere
     * must not stall the field cleanup: that row is archived with a {@code missing.json} listing,
     * and every later row of the type is still archived and cleaned.
     */
    @Test
    void alreadyMissingFieldSourcesDoNotStallFieldCleanup() throws Exception {
        final var type = new ContentTypeDataGen().nextPersisted();
        final List<String> identifiers = new java.util.ArrayList<>();
        try {
            final var field = new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(field);
            ContentTypeDataGen.addField(new FieldDataGen().velocityVarName("attachment").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted());
            final var api = APILocator.getContentletAPI();
            final var user = APILocator.systemUser();
            final var binaries = APILocator.getBinaryAssetStorageAPI();
            final List<Contentlet> contents = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                final var generator = new ContentletDataGen(type);
                generator.setProperty("heroImage", source("stall-hero-" + i, "hero bytes " + i));
                generator.setProperty("attachment", source("stall-attachment-" + i, "attachment bytes " + i));
                final Contentlet content = generator.nextPersisted();
                identifiers.add(content.getIdentifier());
                contents.add(content);
            }
            // Rows are cleaned in inode order; break the first so the others come after it.
            contents.sort(java.util.Comparator.comparing(Contentlet::getInode));
            final Contentlet broken = contents.getFirst();
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(broken,
                    Map.of("heroImage", Map.of("credit", "Lost author")));
            final var lost = api.find(broken.getInode(), user, false).getBinary("heroImage");
            final String lostKey = BinaryAssetReference.keyOf(lost);
            final String lostMetadataKey = BinaryAssetReference.metadataKeyOf(lost);
            assertNotNull(lostMetadataKey, "The test needs a referenced metadata revision");
            org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
                    .until(() -> binaries.evictLocalFile(lost));
            binaries.deleteBinaryPaths(broken.getInode(), "heroImage", List.of(lostKey));
            com.dotcms.storage.StoragePersistenceProvider.INSTANCE.get()
                    .getStorage(com.dotcms.storage.StoragePersistenceProvider.getStorageType())
                    .deleteObjectAndReferences(com.dotcms.storage.FileMetadataAPI.DOT_METADATA, lostMetadataKey);
            Files.deleteIfExists(Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath(),
                    lostMetadataKey.substring(1).toLowerCase(java.util.Locale.ROOT)));
            assertThrows(java.nio.file.NoSuchFileException.class, () -> binaries.openLocalFile(lost).close(),
                    "Precondition: the binary must be gone locally and in S3");
            assertNull(APILocator.getFileStorageAPI().retrieveRawMetaData(new com.dotcms.storage.StorageKey.Builder()
                    .group(com.dotcms.storage.FileMetadataAPI.DOT_METADATA).path(lostMetadataKey)
                    .storage(com.dotcms.storage.StoragePersistenceProvider.getStorageType()).build()),
                    "Precondition: the metadata must be gone locally and in S3");

            APILocator.getContentTypeFieldAPI().delete(field, user);
            final var requests = fieldJobs("type", type.id());
            assertEquals(1, requests.size());
            new BinaryFieldCleanupProcessor().process(requests.getFirst());

            final var backups = ContentletBackupStorage.getInstance();
            final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            for (Contentlet content : contents) {
                assertTrue(mapper.readTree(new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ?")
                                .addParam(content.getInode()).getString("contentlet_as_json")).path("fields").path("heroImage").isMissingNode(),
                        "Every row, including those after the broken one, must have the field removed");
                final var keys = backups.list(content.getIdentifier());
                assertEquals(1, keys.size(), "Each row needs its field archive");
                final var entries = new HashMap<String, byte[]>();
                try (var zip = new ZipInputStream(backups.open(keys.getFirst()))) {
                    for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        assertNull(entries.put(entry.getName(), zip.readAllBytes()));
                    }
                }
                if (content == broken) {
                    assertFalse(entries.containsKey("binary-assets/" + lostKey), "A missing binary must not leave an empty entry");
                    final var missing = new HashMap<String, String>();
                    for (var item : mapper.readTree(entries.get(ContentletBackupStorage.MISSING_MANIFEST))) {
                        assertEquals("heroImage", item.get("field").asText());
                        missing.put(item.get("kind").asText(), item.get("path").asText());
                    }
                    assertEquals(Map.of("binary", "binary-assets/" + lostKey,
                            "metadata", "dotmetadata/" + lostMetadataKey.substring(1)), missing);
                } else {
                    assertFalse(entries.containsKey(ContentletBackupStorage.MISSING_MANIFEST),
                            "Complete archives must not carry a missing-files manifest");
                    assertTrue(entries.keySet().stream().anyMatch(name -> name.startsWith("binary-assets/")
                            && name.contains("/heroImage/")), "Intact rows still archive their field bytes");
                }
                final var cleanups = fieldJobs("inode", content.getInode());
                assertEquals(1, cleanups.size(), "Each archived row records its cleanup");
                new BinaryFieldCleanupProcessor().process(cleanups.getFirst());
                assertTrue(binaries.listBinaryPaths(content.getInode()).stream().noneMatch(path -> path.contains("/heroImage/")));
                com.dotmarketing.business.CacheLocator.getContentletCache().remove(content.getInode());
                assertTrue(Files.readString(api.find(content.getInode(), user, false).getBinary("attachment").toPath())
                        .startsWith("attachment bytes "), "The other field must be untouched");
            }
        } finally {
            ContentTypeDataGen.remove(type);
            for (String identifier : identifiers) removeBackups(identifier);
        }
    }

    @Test
    void fieldBackupFailureRollsBackReferenceRemovalAndCleanupFailureCanRetry() throws Exception {
        final var type = new ContentTypeDataGen().nextPersisted();
        String identifier = null;
        try {
            final var field = new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(field);
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("field-failure", "preserved field bytes"));
            final Contentlet content = generator.nextPersisted();
            identifier = content.getIdentifier();
            final String originalKey = BinaryAssetReference.find(content.getInode(), "heroImage");
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                APILocator.getContentTypeFieldAPI().delete(field, APILocator.systemUser());
                assertEquals(1, fieldJobs("type", type.id()).size());
            } finally { com.dotmarketing.db.HibernateUtil.rollbackTransaction(); }
            assertTrue(fieldJobs("type", type.id()).isEmpty(), "Rolled-back field deletion must not leave a cleanup request");
            assertEquals(originalKey, BinaryAssetReference.find(content.getInode(), "heroImage"));
            APILocator.getContentTypeFieldAPI().delete(field, APILocator.systemUser());
            final var request = fieldJobs("type", type.id()).getFirst();
            final var remote = spy(AmazonS3StoragePersistenceAPIImpl.withPlainPaths());
            doThrow(new DotDataException("injected field backup outage")).when(remote)
                    .backfillFile(eq(ContentletBackupStorage.GROUP), anyString(), any());
            try (var factory = mockStatic(ContentletBackupStorage.class)) {
                factory.when(ContentletBackupStorage::getInstance).thenReturn(new ContentletBackupStorage(remote));
                assertThrows(com.dotcms.jobs.business.error.JobProcessingException.class,
                        () -> new BinaryFieldCleanupProcessor().process(request));
            }
            assertEquals(originalKey, BinaryAssetReference.find(content.getInode(), "heroImage"));
            assertTrue(fieldJobs("inode", content.getInode()).isEmpty());
            new BinaryFieldCleanupProcessor().process(request);
            final var cleanup = fieldJobs("inode", content.getInode()).getFirst();
            final var binaries = spy(APILocator.getBinaryAssetStorageAPI());
            doThrow(new DotDataException("injected binary delete outage")).when(binaries).deleteBinaryPaths(anyString(), anyString(), anyList());
            try (var locator = mockStatic(APILocator.class, CALLS_REAL_METHODS)) {
                locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
                assertThrows(com.dotcms.jobs.business.error.JobProcessingException.class,
                        () -> new BinaryFieldCleanupProcessor().process(cleanup));
            }
            assertTrue(APILocator.getBinaryAssetStorageAPI().listBinaryPaths(content.getInode()).contains(originalKey));
            new BinaryFieldCleanupProcessor().process(cleanup);
            assertTrue(APILocator.getBinaryAssetStorageAPI().listBinaryPaths(content.getInode()).isEmpty());
            assertEquals(1, ContentletBackupStorage.getInstance().list(identifier).size());
        } finally {
            ContentTypeDataGen.remove(type);
            if (identifier != null) removeBackups(identifier);
        }
    }

    @Test
    void directCleanFieldQueuesArchivingInsteadOfUploadingInline() throws Exception {
        final var type = new ContentTypeDataGen().nextPersisted();
        String identifier = null;
        try {
            final var field = new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(field);
            final var generator = new ContentletDataGen(type);
            generator.setProperty("heroImage", source("field-direct", "direct clean bytes"));
            final Contentlet content = generator.nextPersisted();
            identifier = content.getIdentifier();
            final String originalKey = BinaryAssetReference.find(content.getInode(), "heroImage");
            final var backups = spy(ContentletBackupStorage.getInstance());
            try (var factory = mockStatic(ContentletBackupStorage.class, CALLS_REAL_METHODS)) {
                factory.when(ContentletBackupStorage::getInstance).thenReturn(backups);
                APILocator.getContentletAPI().cleanField(
                        new com.dotcms.contenttype.transform.contenttype.StructureTransformer(type).asStructure(),
                        new java.util.Date(),
                        new com.dotcms.contenttype.transform.field.LegacyFieldTransformer(field).asOldField(),
                        APILocator.systemUser(), false);
            }
            // The call must not upload recovery archives while its caller waits and holds row locks.
            verify(backups, never()).storeField(anyString(), anyString(), anyString(), anyList(), anyList(), anyList());
            assertEquals(originalKey, BinaryAssetReference.find(content.getInode(), "heroImage"),
                    "Archiving and clearing happen in the queued job, not inline");
            assertEquals(1, fieldJobs("type", type.id()).size(),
                    "A direct call queues the same cleanup request as field deletion");
        } finally {
            ContentTypeDataGen.remove(type);
            if (identifier != null) removeBackups(identifier);
        }
    }

    private List<com.dotcms.jobs.business.job.Job> fieldJobs(String owner, String id) throws Exception {
        final var rows = new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> ? = ?")
                .addParam(BinaryFieldCleanupProcessor.QUEUE).addParam(owner).addParam(id).loadObjectResults();
        final var jobs = new java.util.ArrayList<com.dotcms.jobs.business.job.Job>();
        for (var row : rows) jobs.add(APILocator.getJobQueueManagerAPI().getJob(row.get("id").toString()));
        return jobs;
    }

    /**
     * Returns the metadata path that the content row records for a field, which is the file the
     * backup archives. The Contentlet a test holds can be older than the row: indexing may publish
     * a new metadata key from another copy of the same version, and then this copy still points
     * at the earlier file.
     *
     * @param content the content version
     * @param field   the binary field variable name
     * @return the metadata path, starting with {@code /}
     * @throws Exception if the row cannot be read or names no binary for the field
     */
    private static String storedMetadataPath(Contentlet content, String field) throws Exception {
        final var stored = new Contentlet(content);
        stored.getMap().put(field, BinaryAssetReference.fromContentJson(BinaryAssetReference.contentJson(
                content.getInode()), content.getInode()).get(field).localFile(content.getInode(), field));
        return APILocator.getFileMetadataAPI().getFileName(stored, field);
    }

    private void removeBackups(String identifier) throws Exception {
        final var remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        for (String key : ContentletBackupStorage.getInstance().list(identifier)) remote.deleteObjectAndReferences(ContentletBackupStorage.GROUP, key);
    }

    private java.io.File source(String directory, String bytes) throws Exception {
        final Path path = uploads.resolve(directory).resolve("Shared-Mixed.Txt");
        Files.createDirectories(path.getParent());
        return Files.writeString(path, bytes).toFile();
    }
}
