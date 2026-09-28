package com.dotcms.storage.binary;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
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

    @ParameterizedTest
    @ValueSource(strings = {"destroy", "allVersions", "version"})
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
            final List<Contentlet> deleted = deletion.equals("version") ? List.of(first) : List.of(first, second);
            final var expected = new HashMap<String, Map<String, byte[]>>();
            for (Contentlet content : deleted) {
                final var entries = new HashMap<String, byte[]>();
                for (String field : List.of("heroImage", "attachment")) {
                    final var binary = content.getBinary(field);
                    entries.put("assets/" + BinaryAssetReference.keyOf(binary), Files.readAllBytes(binary.toPath()));
                    org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5))
                            .until(() -> APILocator.getBinaryAssetStorageAPI().evictLocalFile(binary));
                    final String metadataPath = APILocator.getFileMetadataAPI().getFileName(content, field);
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
                default -> throw new AssertionError(deletion);
            }
            for (Contentlet content : deleted) {
                assertTrue(new DotConnect().setSQL("select inode from contentlet where inode = ?")
                        .addParam(content.getInode()).loadObjectResults().isEmpty());
                final var jobs = new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> 'inode' = ?")
                        .addParam(BinaryAssetCleanupProcessor.QUEUE).addParam(content.getInode()).loadObjectResults();
                assertFalse(jobs.isEmpty());
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
                    if (entry.getKey().endsWith("-metadata.json")) {
                        final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        assertEquals(mapper.readTree(entry.getValue()), mapper.readTree(entries.get(entry.getKey())));
                    } else assertArrayEquals(entry.getValue(), entries.get(entry.getKey()));
                }
            }
            if (deletion.equals("version")) {
                assertEquals("new image version", Files.readString(api.find(second.getInode(), user, false).getBinary("heroImage").toPath()));
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

    private List<com.dotcms.jobs.business.job.Job> fieldJobs(String owner, String id) throws Exception {
        final var rows = new DotConnect().setSQL("select id from job where queue_name = ? and parameters ->> ? = ?")
                .addParam(BinaryFieldCleanupProcessor.QUEUE).addParam(owner).addParam(id).loadObjectResults();
        final var jobs = new java.util.ArrayList<com.dotcms.jobs.business.job.Job>();
        for (var row : rows) jobs.add(APILocator.getJobQueueManagerAPI().getJob(row.get("id").toString()));
        return jobs;
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
