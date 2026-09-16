package com.dotcms.storage.binary;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Phase 2 BinaryAssetStorageAPI migration.
 * Exercises migrated code paths (Contentlet.getBinary, handleBinaries,
 * content versioning, FileAsset flow) end-to-end with real storage.
 */
@Tag("BinaryStorage")
public class BinaryAssetStorageIntegrationTest {

    private static User user;
    private static ContentletAPI contentletAPI;
    private static BinaryAssetStorageAPI binaryAssetStorageAPI;

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();
        assertEquals(Boolean.getBoolean("s3.cms.enabled"), com.dotcms.storage.AssetStorageFeature.isEnabled(),
                "Integration configuration must exercise the requested storage mode");
        user = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        binaryAssetStorageAPI = APILocator.getBinaryAssetStorageAPI();
    }

    @Test
    void s3ReplacementRollbackPreservesCommittedBinaryThroughCmsCheckin(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        assertTrue(com.dotcms.storage.AssetStorageFeature.isEnabled(), "CMS must actually opt in to S3");
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            final Field title = new FieldDataGen().velocityVarName("title").contentTypeId(type.id())
                    .type(TextField.class).nextPersisted();
            ContentTypeDataGen.addField(title);
            final Field binary = new FieldDataGen().velocityVarName("heroImage").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(binary);
            final Field attachment = new FieldDataGen().velocityVarName("attachment").contentTypeId(type.id())
                    .type(BinaryField.class).nextPersisted();
            ContentTypeDataGen.addField(attachment);
            final java.nio.file.Path before = uploads.resolve("before/Friday.Txt");
            final java.nio.file.Path after = uploads.resolve("after/Friday.Txt");
            Files.createDirectories(before.getParent());
            Files.createDirectories(after.getParent());
            Files.writeString(before, "last committed bytes");
            Files.writeString(after, "replacement bytes");
            final ContentletDataGen generator = new ContentletDataGen(type);
            generator.setProperty("title", "S3 transaction test");
            generator.setProperty("heroImage", before.toFile());
            generator.setProperty("attachment", before.toFile());
            final Contentlet original = generator.nextPersisted();
            final File originalFile = original.getBinary("heroImage");
            final String originalKey = BinaryAssetReference.find(original.getInode(), "heroImage");
            assertNotNull(originalKey);
            assertTrue(originalKey.contains("/.revisions/"));
            assertEquals("Friday.Txt", originalFile.getName());
            assertBinaryMetadataLength(original, Files.size(before));
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(original,
                    java.util.Map.of("heroImage", java.util.Map.of("credit", "Original author"),
                            "attachment", java.util.Map.of("credit", "Original attachment author")));

            File rolledBackFile;
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                final Contentlet edit = new Contentlet(original);
                edit.setBinary("heroImage", after.toFile());
                final Contentlet changed = contentletAPI.checkinWithoutVersioning(edit,
                        (com.dotmarketing.portlets.structure.model.ContentletRelationships) null,
                        null, null, user, false);
                assertEquals(original.getInode(), changed.getInode());
                rolledBackFile = changed.getBinary("heroImage");
                final String replacementKey = BinaryAssetReference.find(original.getInode(), "heroImage");
                assertNotNull(replacementKey, "Check-in must publish a stored reference, not just the upload filename");
                assertNotEquals(originalKey, replacementKey);
                assertEquals(replacementKey,
                        BinaryAssetReference.keyOf(rolledBackFile), "Returned binary must reference stored bytes: " + rolledBackFile);
                assertEquals("replacement bytes", Files.readString(rolledBackFile.toPath()));
                assertBinaryMetadataLength(changed, Files.size(after));
                assertEquals("Original author", APILocator.getFileMetadataAPI()
                        .getOrGenerateMetadata(changed, "heroImage").getCustomMeta().get("credit"));
                final java.util.Map<String, java.io.Serializable> receivedMetadata = new java.util.HashMap<>(
                        APILocator.getFileMetadataAPI().getFullMetadataNoCache(changed, "heroImage").getMap());
                receivedMetadata.put("dot:credit", "Received author");
                APILocator.getFileMetadataAPI().setMetadata(changed, java.util.Map.of("heroImage",
                        new com.dotcms.storage.model.Metadata("heroImage", receivedMetadata)));
                assertEquals("Received author", APILocator.getFileMetadataAPI().getMetadata(
                        contentletAPI.find(changed.getInode(), user, false), "heroImage")
                        .getCustomMeta().get("credit"));
                assertMetadataRestoredFromS3(original, Files.size(before));
                assertEquals("Original attachment author", APILocator.getFileMetadataAPI()
                        .getFullMetadataNoCache(original, "attachment").getCustomMeta().get("credit"),
                        "A publishing metadata update must preserve omitted fields");
                APILocator.getFileMetadataAPI().setMetadata(changed, java.util.Map.of());
                assertEquals("Received author", APILocator.getFileMetadataAPI().getMetadata(
                        contentletAPI.find(changed.getInode(), user, false), "heroImage")
                        .getCustomMeta().get("credit"), "An empty metadata bundle must not delete existing snapshots");
            } finally {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
            }
            assertEquals(originalKey, BinaryAssetReference.find(original.getInode(), "heroImage"));
            assertMetadataRestoredFromS3(contentletAPI.find(original.getInode(), user, false), Files.size(before));
            assertTrue(binaryAssetStorageAPI.evictLocalFile(originalFile));
            assertTrue(binaryAssetStorageAPI.evictLocalFile(rolledBackFile), "Unable to evict replacement cache file " + rolledBackFile);
            assertEquals("last committed bytes", Files.readString(contentletAPI.find(original.getInode(), user, false)
                    .getBinary("heroImage").toPath()));

            final Contentlet edit = new Contentlet(original);
            edit.setBinary("heroImage", after.toFile());
            final Contentlet committed = contentletAPI.checkinWithoutVersioning(edit,
                    (com.dotmarketing.portlets.structure.model.ContentletRelationships) null,
                    null, null, user, false);
            final File committedFile = committed.getBinary("heroImage");
            assertNotEquals(originalKey, BinaryAssetReference.find(committed.getInode(), "heroImage"));
            assertTrue(binaryAssetStorageAPI.evictLocalFile(committedFile));
            assertMetadataRestoredFromS3(committed, Files.size(after));
            assertEquals("replacement bytes", Files.readString(contentletAPI.find(committed.getInode(), user, false)
                    .getBinary("heroImage").toPath()));
            assertEquals("Original author", APILocator.getFileMetadataAPI()
                    .getOrGenerateMetadata(original, "heroImage").getCustomMeta().get("credit"));
            assertBinaryMetadataLength(original, Files.size(before));

            final Contentlet multiEdit = new Contentlet(committed);
            for (final String field : java.util.List.of("heroImage", "attachment")) {
                final com.dotcms.rest.api.v1.temp.DotTempFile upload = APILocator.getTempFileAPI()
                        .createTempFile(field + ".Txt",
                                com.dotcms.rest.api.v1.temp.TempFileAPITest.mockHttpServletRequest(),
                                new java.io.ByteArrayInputStream(("updated " + field).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                APILocator.getFileMetadataAPI().putCustomMetadataAttributes(upload.id,
                        java.util.Map.of(field, java.util.Map.of("credit", "Uploaded " + field)));
                multiEdit.setBinary(field, upload.file);
            }
            final Contentlet multiSaved = contentletAPI.checkinWithoutVersioning(multiEdit,
                    (com.dotmarketing.portlets.structure.model.ContentletRelationships) null,
                    null, null, user, false);
            for (final String field : java.util.List.of("heroImage", "attachment")) {
                assertEquals("Uploaded " + field, APILocator.getFileMetadataAPI()
                        .getOrGenerateMetadata(multiSaved, field).getCustomMeta().get("credit"),
                        "Copying another field must not overwrite this upload's custom metadata");
            }

            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                contentletAPI.destroy(multiSaved, user, false);
                assertMetadataRestoredFromS3(multiSaved, "updated heroImage".length(), "Uploaded heroImage");
            } finally {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
            }
            assertNotNull(BinaryAssetReference.find(multiSaved.getInode(), "heroImage"));
            assertEquals("updated heroImage", Files.readString(contentletAPI.find(multiSaved.getInode(), user, false)
                    .getBinary("heroImage").toPath()));

            final String inodePrefix = "/" + multiSaved.getInode().charAt(0) + "/"
                    + multiSaved.getInode().charAt(1) + "/" + multiSaved.getInode();
            final com.dotcms.storage.FetchMetadataParams orphanMetadata = metadataRequest(
                    inodePrefix + "/removedField-metadata.json");
            final com.dotcms.storage.FetchMetadataParams neighborMetadata = metadataRequest(
                    inodePrefix + "-neighbor/otherField-metadata.json");
            APILocator.getFileStorageAPI().setMetadata(orphanMetadata, java.util.Map.of("dot:credit", "Removed field"));
            APILocator.getFileStorageAPI().setMetadata(neighborMetadata, java.util.Map.of("dot:credit", "Neighbor"));
            Files.delete(java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                    .resolve(orphanMetadata.getStorageKey().getPath().substring(1).toLowerCase(java.util.Locale.ROOT)));

            contentletAPI.destroy(multiSaved, user, false);
            final java.util.List<java.util.Map<String, Object>> jobs = new com.dotmarketing.common.db.DotConnect()
                    .setSQL("select id from job where queue_name = ? and parameters ->> 'inode' = ?")
                    .addParam(BinaryAssetCleanupProcessor.QUEUE).addParam(multiSaved.getInode()).loadObjectResults();
            assertFalse(jobs.isEmpty(), "CMS deletion must commit a durable cleanup job");
            final com.dotcms.jobs.business.job.Job job = APILocator.getJobQueueManagerAPI()
                    .getJob(jobs.getFirst().get("id").toString());
            new BinaryAssetCleanupProcessor().process(job);
            assertTrue(binaryAssetStorageAPI.listBinaryPaths(multiSaved.getInode()).isEmpty());
            for (final Contentlet snapshot : java.util.List.of(original, committed, multiSaved)) {
                for (final String field : java.util.List.of("heroImage", "attachment")) {
                    assertNull(APILocator.getFileMetadataAPI().getMetadata(snapshot, field),
                            "Cleanup must remove metadata for current and historical revisions");
                }
            }
            new BinaryAssetCleanupProcessor().process(job); // Durable retries after success are harmless.
            assertNull(APILocator.getFileStorageAPI().retrieveMetaData(orphanMetadata),
                    "Remote-only metadata must be deleted even when its field and source are gone");
            assertNotNull(APILocator.getFileStorageAPI().retrieveMetaData(neighborMetadata),
                    "Inode cleanup must not delete a neighboring inode's metadata");
            APILocator.getFileStorageAPI().removeMetaData(neighborMetadata);
        } finally {
            ContentTypeDataGen.remove(type);
        }
    }

    @Test
    void s3HttpResponseProtectsTheResolvedFileUntilItIsOpened(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        try {
            final File source = Files.writeString(uploads.resolve("Leased-Response.Txt"), "response bytes from S3").toFile();
            final Contentlet content = new FileAssetDataGen(folder, source).nextPersisted();
            ContentletDataGen.publish(content);
            final File cached = content.getBinary(FileAssetAPI.BINARY_FIELD);
            assertTrue(binaryAssetStorageAPI.evictLocalFile(cached));
            final String uri = "/contentAsset/raw-data/" + content.getInode() + "/" + FileAssetAPI.BINARY_FIELD + "/byInode/true";
            for (final String range : java.util.List.of("", "bytes=1-4")) {
                final var request = new com.dotcms.mock.request.MockHeaderRequest(new com.dotcms.mock.request.MockSessionRequest(new com.dotcms.mock.request.MockServletPathRequest(
                        new com.dotcms.mock.request.MockHttpRequestIntegrationTest("localhost", uri).request(), "/contentAsset")));
                request.setHeader("range", range);
                request.setAttribute(com.liferay.portal.util.WebKeys.USER, user);
                final var output = new java.io.ByteArrayOutputStream();
                final var opened = new java.util.concurrent.atomic.AtomicBoolean();
                final var capture = new com.dotcms.mock.response.MockHttpStatusResponse(new com.dotcms.mock.response.MockHttpCaptureResponse(
                        org.mockito.Mockito.mock(javax.servlet.http.HttpServletResponse.class), output));
                final var response = new javax.servlet.http.HttpServletResponseWrapper(capture) {
                    @Override
                    public javax.servlet.ServletOutputStream getOutputStream() throws java.io.IOException {
                        opened.set(true);
                        try {
                            assertFalse(binaryAssetStorageAPI.evictLocalFile(cached), "Eviction must defer through response streaming, including the range branch before source open");
                            assertTrue(cached.isFile());
                        } catch (com.dotmarketing.exception.DotDataException e) {
                            throw new java.io.IOException(e);
                        }
                        return super.getOutputStream();
                    }
                };
                final var servlet = new com.dotmarketing.servlets.BinaryExporterServlet();
                servlet.init();
                servlet.doGet(request, response);
                assertEquals(range.isEmpty() ? 200 : 206, response.getStatus());
                assertTrue(opened.get(), "The test must reach actual response streaming");
                final byte[] expected = Files.readAllBytes(source.toPath());
                assertArrayEquals(range.isEmpty() ? expected : java.util.Arrays.copyOfRange(expected, 1, 5), output.toByteArray());
                assertTrue(binaryAssetStorageAPI.evictLocalFile(cached), "Response completion must release the lease");
            }
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void s3StandaloneMetadataEditsAreIsolatedAndRollbackSafe(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        final var metadataAPI = APILocator.getFileMetadataAPI();
        final String field = FileAssetAPI.BINARY_FIELD;
        try {
            final File upload = Files.writeString(uploads.resolve("MetadataHistory.Txt"), "unchanged binary bytes").toFile();
            final Contentlet asset = new FileAssetDataGen(folder, upload).nextPersisted();
            metadataAPI.putCustomMetadataAttributes(asset, java.util.Map.of(field, java.util.Map.of("credit", "Original")));
            final Contentlet before = new Contentlet(asset);
            final String originalMetadataPath = metadataAPI.getFileName(before, field);
            com.dotmarketing.business.CacheLocator.getContentletCache().remove(asset.getInode());
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                metadataAPI.putCustomMetadataAttributes(asset, java.util.Map.of(field, java.util.Map.of("credit", "Pending")));
                assertEquals("Pending", metadataAPI.getMetadata(contentletAPI.find(asset.getInode(), user, false), field)
                        .getCustomMeta().get("credit"), "The writer must read its metadata update inside the transaction");
                final String observed = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return (String) metadataAPI.getMetadata(contentletAPI.find(asset.getInode(), user, false), field)
                                .getCustomMeta().get("credit");
                    } catch (Exception e) {
                        throw new java.util.concurrent.CompletionException(e);
                    }
                }).get(30, java.util.concurrent.TimeUnit.SECONDS);
                assertEquals("Original", observed, "Another request must not observe an uncommitted metadata edit");
            } finally {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
            }
            final Contentlet rolledBack = contentletAPI.find(asset.getInode(), user, false);
            assertEquals("Original", metadataAPI.getMetadata(rolledBack, field).getCustomMeta().get("credit"));
            assertEquals(originalMetadataPath, metadataAPI.getFileName(rolledBack, field));

            metadataAPI.putCustomMetadataAttributes(rolledBack,
                    java.util.Map.of(field, java.util.Map.of("credit", "Committed")));
            final Contentlet committed = contentletAPI.find(asset.getInode(), user, false);
            assertNotEquals(originalMetadataPath, metadataAPI.getFileName(committed, field));
            assertEquals("Committed", metadataAPI.getMetadata(committed, field).getCustomMeta().get("credit"));
            assertEquals("Original", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"),
                    "A prior content snapshot must retain its prior metadata");
            for (final Contentlet snapshot : java.util.List.of(before, committed)) {
                final String path = metadataAPI.getFileName(snapshot, field);
                Files.delete(java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                        .resolve(path.substring(1).toLowerCase(java.util.Locale.ROOT)));
                com.dotmarketing.business.CacheLocator.getMetadataCache()
                        .removeMetadata(metadataAPI.getMetadataCacheKey(snapshot, field));
            }
            assertEquals("Original", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"));
            assertEquals("Committed", metadataAPI.getMetadata(committed, field).getCustomMeta().get("credit"));
            final String committedMetadataKey = metadataAPI.getFileName(committed, field);
            assertTrue(binaryAssetStorageAPI.evictLocalFile(committed.getBinary(field)));
            assertEquals(committedMetadataKey, BinaryAssetReference.metadataKeyOf(committed.getBinary(field)),
                    "Cold Contentlet binary restoration must preserve its metadata snapshot");
            assertEquals(committedMetadataKey, BinaryAssetReference.metadataKeyOf(
                    binaryAssetStorageAPI.getBinaryFile(asset.getInode(), field)));
            assertEquals(committedMetadataKey, BinaryAssetReference.metadataKeyOf(
                    binaryAssetStorageAPI.getBinaryFile(asset.getInode(), field, "MetadataHistory.Txt")));

            final Contentlet concurrentSnapshot = new Contentlet(committed);
            final java.util.concurrent.CountDownLatch attemptingEdit = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CompletableFuture<Void> concurrentEdit = null;
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                metadataAPI.putCustomMetadataAttributes(committed,
                        java.util.Map.of(field, java.util.Map.of("credit", "First concurrent edit")));
                concurrentEdit = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        attemptingEdit.countDown();
                        metadataAPI.putCustomMetadataAttributes(concurrentSnapshot,
                                java.util.Map.of(field, java.util.Map.of("license", "Second concurrent edit")));
                    } catch (Exception e) {
                        throw new java.util.concurrent.CompletionException(e);
                    } finally {
                        com.dotmarketing.db.DbConnectionFactory.closeConnection();
                    }
                });
                assertTrue(attemptingEdit.await(30, java.util.concurrent.TimeUnit.SECONDS));
                final var pending = concurrentEdit;
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> pending.get(250, java.util.concurrent.TimeUnit.MILLISECONDS),
                        "Concurrent metadata edits must wait for the content transaction");
                com.dotmarketing.db.HibernateUtil.commitTransaction();
            } finally {
                if (com.dotmarketing.db.DbConnectionFactory.inTransaction()) {
                    com.dotmarketing.db.HibernateUtil.rollbackTransaction();
                }
                if (concurrentEdit != null) {
                    concurrentEdit.get(30, java.util.concurrent.TimeUnit.SECONDS);
                }
            }
            final var merged = metadataAPI.getMetadata(contentletAPI.find(asset.getInode(), user, false), field).getCustomMeta();
            assertEquals("First concurrent edit", merged.get("credit"));
            assertEquals("Second concurrent edit", merged.get("license"),
                    "The second edit must merge against the newly committed metadata, not its stale snapshot");
            assertEquals("Original", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"));
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void s3MetadataCopyPreservesDestinationBytesAndHistoricalSnapshots(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        final var metadataAPI = APILocator.getFileMetadataAPI();
        final String field = FileAssetAPI.BINARY_FIELD;
        try {
            final File sourceFile = Files.writeString(uploads.resolve("CopySource.Txt"), "source").toFile();
            final File destinationFile = Files.writeString(uploads.resolve("CopyDestination.Txt"),
                    "Destination binary must retain its own metadata.").toFile();
            final Contentlet source = new FileAssetDataGen(folder, sourceFile).nextPersisted();
            final Contentlet destination = new FileAssetDataGen(folder, destinationFile).nextPersisted();
            metadataAPI.putCustomMetadataAttributes(source,
                    java.util.Map.of(field, java.util.Map.of("credit", "Source author", "sourceOnly", "copied")));
            metadataAPI.putCustomMetadataAttributes(destination,
                    java.util.Map.of(field, java.util.Map.of("credit", "Destination author", "destinationOnly", "removed")));
            final Contentlet before = new Contentlet(destination);
            final String beforeKey = metadataAPI.getFileName(before, field);
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                metadataAPI.copyCustomMetadata(source, destination);
                final var pending = metadataAPI.getMetadata(contentletAPI.find(destination.getInode(), user, false), field);
                assertEquals("Source author", pending.getCustomMeta().get("credit"));
                assertEquals("copied", pending.getCustomMeta().get("sourceOnly"));
                assertFalse(pending.getCustomMeta().containsKey("destinationOnly"));
                assertEquals(Long.toString(destinationFile.length()), String.valueOf(pending.getFieldsMeta().get("length")),
                        "Copying attributes must retain the destination binary's generated metadata");
                assertEquals("Destination author", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"));
            } finally {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
            }
            final Contentlet rolledBack = contentletAPI.find(destination.getInode(), user, false);
            assertEquals(beforeKey, metadataAPI.getFileName(rolledBack, field));
            assertEquals("Destination author", metadataAPI.getMetadata(rolledBack, field).getCustomMeta().get("credit"));
            metadataAPI.copyCustomMetadata(source, rolledBack);
            final Contentlet committed = contentletAPI.find(destination.getInode(), user, false);
            assertNotEquals(beforeKey, metadataAPI.getFileName(committed, field));
            for (final Contentlet snapshot : java.util.List.of(before, committed)) {
                final String path = metadataAPI.getFileName(snapshot, field);
                Files.delete(java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                        .resolve(path.substring(1).toLowerCase(java.util.Locale.ROOT)));
                com.dotmarketing.business.CacheLocator.getMetadataCache().removeMetadata(
                        metadataAPI.getMetadataCacheKey(snapshot, field));
            }
            assertEquals("Destination author", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"));
            assertEquals("Source author", metadataAPI.getMetadata(committed, field).getCustomMeta().get("credit"));
            assertArrayEquals(Files.readAllBytes(destinationFile.toPath()), Files.readAllBytes(committed.getBinary(field).toPath()));

            metadataAPI.putCustomMetadataAttributes(source, java.util.Map.of(field, java.util.Map.of()));
            metadataAPI.copyCustomMetadata(source, committed);
            final var cleared = metadataAPI.getMetadata(contentletAPI.find(destination.getInode(), user, false), field);
            assertTrue(cleared.getCustomMeta().isEmpty(), "An empty source removes only destination custom attributes");
            assertEquals(Long.toString(destinationFile.length()), String.valueOf(cleared.getFieldsMeta().get("length")));
            assertEquals("Destination author", metadataAPI.getMetadata(before, field).getCustomMeta().get("credit"));
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void s3RegenerationPreservesSnapshotsAndRollback(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        final var metadataAPI = APILocator.getFileMetadataAPI();
        final String field = FileAssetAPI.BINARY_FIELD;
        final String overrideProperty = com.dotcms.storage.FileMetadataAPI.ALWAYS_REGENERATE_METADATA_ON_REINDEX;
        final boolean previousOverride = com.dotmarketing.util.Config.getBooleanProperty(overrideProperty, false);
        try {
            final File upload = Files.writeString(uploads.resolve("RegenerateMetadata.Txt"), "metadata source bytes").toFile();
            final Contentlet asset = new FileAssetDataGen(folder, upload).nextPersisted();
            metadataAPI.setMetadata(asset, java.util.Map.of(field,
                    new com.dotcms.storage.model.Metadata(field,
                            java.util.Map.of("length", -1L, "dot:credit", "Original author"))));
            final Contentlet before = new Contentlet(asset);
            final String beforeKey = metadataAPI.getFileName(before, field);
            com.dotmarketing.util.Config.setProperty(overrideProperty, true);
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                metadataAPI.generateContentletMetadata(asset);
                final Contentlet pending = contentletAPI.find(asset.getInode(), user, false);
                assertNotEquals(beforeKey, metadataAPI.getFileName(pending, field));
                assertEquals(Long.toString(upload.length()),
                        String.valueOf(metadataAPI.getMetadata(pending, field).getFieldsMeta().get("length")));
                assertEquals("-1", String.valueOf(metadataAPI.getMetadata(before, field).getFieldsMeta().get("length")),
                        "Regeneration must not overwrite the metadata object held by an older snapshot");
            } finally {
                com.dotmarketing.db.HibernateUtil.rollbackTransaction();
            }
            final Contentlet rolledBack = contentletAPI.find(asset.getInode(), user, false);
            assertEquals(beforeKey, metadataAPI.getFileName(rolledBack, field));
            metadataAPI.generateContentletMetadata(rolledBack);
            final Contentlet regenerated = contentletAPI.find(asset.getInode(), user, false);
            assertNotEquals(beforeKey, metadataAPI.getFileName(regenerated, field));
            assertEquals("Original author", metadataAPI.getMetadata(regenerated, field).getCustomMeta().get("credit"));

            metadataAPI.putCustomMetadataAttributes(regenerated,
                    java.util.Map.of(field, java.util.Map.of("credit", "Current author")));
            final Contentlet current = contentletAPI.find(asset.getInode(), user, false);
            final String currentKey = metadataAPI.getFileName(current, field);
            metadataAPI.generateContentletMetadata(before);
            assertEquals(currentKey, metadataAPI.getFileName(contentletAPI.find(asset.getInode(), user, false), field),
                    "Reindexing an old metadata snapshot must not publish over the current reference");
            for (final Contentlet snapshot : java.util.List.of(before, current)) {
                final String path = metadataAPI.getFileName(snapshot, field);
                Files.delete(java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                        .resolve(path.substring(1).toLowerCase(java.util.Locale.ROOT)));
                com.dotmarketing.business.CacheLocator.getMetadataCache()
                        .removeMetadata(metadataAPI.getMetadataCacheKey(snapshot, field));
            }
            assertEquals("-1", String.valueOf(metadataAPI.getMetadata(before, field).getFieldsMeta().get("length")));
            assertEquals("Current author", metadataAPI.getMetadata(current, field).getCustomMeta().get("credit"));
            assertEquals(Long.toString(upload.length()),
                    String.valueOf(metadataAPI.getMetadata(current, field).getFieldsMeta().get("length")));

            com.dotmarketing.util.Config.setProperty(overrideProperty, false);
            metadataAPI.setMetadata(current, java.util.Map.of(field,
                    new com.dotcms.storage.model.Metadata(field, java.util.Map.of("dot:credit", "Custom only"))));
            // A custom edit reads the metadata through the UI projection before publishing it.
            // Its derived editableAsText value must not make this a complete generated record.
            metadataAPI.putCustomMetadataAttributes(current,
                    java.util.Map.of(field, java.util.Map.of("license", "Retained during generation")));
            final Contentlet customOnly = new Contentlet(current);
            metadataAPI.generateContentletMetadata(current);
            final Contentlet lazyGenerated = contentletAPI.find(asset.getInode(), user, false);
            assertNotEquals(metadataAPI.getFileName(customOnly, field), metadataAPI.getFileName(lazyGenerated, field));
            assertEquals("Custom only", metadataAPI.getMetadata(lazyGenerated, field).getCustomMeta().get("credit"));
            assertEquals("Retained during generation", metadataAPI.getMetadata(lazyGenerated, field).getCustomMeta().get("license"));
            assertEquals(Long.toString(upload.length()),
                    String.valueOf(metadataAPI.getMetadata(lazyGenerated, field).getFieldsMeta().get("length")));
            assertFalse(metadataAPI.getMetadata(customOnly, field).getFieldsMeta().containsKey("length"));
            final String warmKey = metadataAPI.getFileName(lazyGenerated, field);
            metadataAPI.generateContentletMetadata(lazyGenerated);
            assertEquals(warmKey, metadataAPI.getFileName(lazyGenerated, field),
                    "Generated metadata with custom attributes must be reused on a normal reindex");
        } finally {
            com.dotmarketing.util.Config.setProperty(overrideProperty, previousOverride);
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void assetExportIncludesBinaryAndMetadataInBothStorageModes(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        final boolean s3Enabled = Boolean.getBoolean("s3.cms.enabled");
        final Folder folder = new FolderDataGen().nextPersisted();
        final String field = FileAssetAPI.BINARY_FIELD;
        final var metadataAPI = APILocator.getFileMetadataAPI();
        try {
            final File upload = Files.writeString(uploads.resolve("ColdExport-MixedCase.Txt"), "cold export original bytes").toFile();
            final Contentlet asset = new FileAssetDataGen(folder, upload).nextPersisted();
            metadataAPI.putCustomMetadataAttributes(asset,
                    java.util.Map.of(field, java.util.Map.of("credit", "Preserved in starter")));
            final File binary = asset.getBinary(field);
            final String inode = asset.getInode();
            final String binaryPath = s3Enabled ? BinaryAssetReference.keyOf(binary)
                    : inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/" + field + "/" + binary.getName();
            final String metadataPath = metadataAPI.getFileName(asset, field);
            final java.nio.file.Path localMetadata = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                    .resolve(metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT));
            if (s3Enabled) {
                assertTrue(binaryAssetStorageAPI.evictLocalFile(binary));
                Files.delete(localMetadata);
                com.dotmarketing.business.CacheLocator.getMetadataCache().removeMetadata(
                        metadataAPI.getMetadataCacheKey(asset, field));
                assertFalse(binary.exists());
                assertFalse(Files.exists(localMetadata));
            }

            final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            new com.dotmarketing.util.starter.ExportStarterUtil().streamCompressedAssets(output, true, -1);
            final java.util.Map<String, byte[]> entries = readZipEntries(output.toByteArray());
            assertArrayEquals(Files.readAllBytes(upload.toPath()), entries.get("assets/" + binaryPath));
            final byte[] metadataBytes = entries.get("assets/" + metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT));
            assertNotNull(metadataBytes, "The archive must include metadata restored from S3, including custom attributes");
            assertEquals("Preserved in starter", new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(metadataBytes).path("dot:credit").asText());
            assertTrue(binary.exists());
            if (s3Enabled) {
                assertEquals(1, new com.dotmarketing.common.db.DotConnect()
                        .setSQL("select count(*) as draft_count from contentlet_version_info where working_inode = ? and live_inode is null")
                        .addParam(inode).getInt("draft_count"), "Exercise an unpublished working asset");
                assertTrue(binaryAssetStorageAPI.evictLocalFile(binary));
                final java.io.ByteArrayOutputStream workingOnly = new java.io.ByteArrayOutputStream();
                new com.dotmarketing.util.starter.ExportStarterUtil().streamCompressedAssets(workingOnly, false, -1);
                assertArrayEquals(Files.readAllBytes(upload.toPath()),
                        readZipEntries(workingOnly.toByteArray()).get("assets/" + binaryPath),
                        "Working-only exports must include drafts with no live version");
                binaryAssetStorageAPI.deleteAllBinaries(inode);
                assertThrows(com.dotmarketing.exception.DotRuntimeException.class,
                        () -> new com.dotmarketing.util.starter.ExportStarterUtil()
                                .streamCompressedAssets(new java.io.ByteArrayOutputStream(), true, -1),
                        "A referenced but missing S3 binary must fail the export instead of silently omitting it");
            }
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void s3LegacyBackfillResumesWithoutChangingReferencesOrLosingLocalSources(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        final var client = com.amazonaws.services.s3.AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration(
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ENDPOINT"), "us-east-1"))
                .withPathStyleAccessEnabled(true)
                .withCredentials(com.dotmarketing.util.UtilMethods.isSet(System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ACCESS_KEY"))
                        ? new com.amazonaws.auth.AWSStaticCredentialsProvider(new com.amazonaws.auth.BasicAWSCredentials(
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ACCESS_KEY"),
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_SECRET_ACCESS_KEY")))
                        : new com.amazonaws.auth.DefaultAWSCredentialsProviderChain())
                .build();
        final String bucket = System.getProperty("DOT_STORAGE_FILE_METADATA_S3_BUCKET_NAME");
        try {
            final String field = FileAssetAPI.BINARY_FIELD;
            final var metadataAPI = APILocator.getFileMetadataAPI();
            final File upload = Files.writeString(uploads.resolve("Legacy-MixedCase.Txt"), "legacy imported bytes").toFile();
            final Contentlet created = new FileAssetDataGen(folder, upload).nextPersisted();
            metadataAPI.putCustomMetadataAttributes(created,
                    java.util.Map.of(field, java.util.Map.of("credit", "Legacy author")));
            final String inode = created.getInode();
            final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode;
            final String key = prefix + "/" + field + "/" + upload.getName();
            final var local = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath()).resolve(key);
            Files.createDirectories(local.getParent());
            Files.copy(upload.toPath(), local);
            final String metadataPath = "/" + prefix + "/" + field + "-metadata.json";
            final var localMetadata = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                    .resolve(metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT));
            final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Files.writeString(localMetadata, mapper.writeValueAsString(metadataAPI.getFullMetadataNoCache(created, field).getMap()));
            final var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
                    new com.dotmarketing.common.db.DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ?")
                            .addParam(inode).getString("contentlet_as_json"));
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields").path(field)).remove("storageKey");
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields").path(field)).remove("metadataStorageKey");
            new com.dotmarketing.common.db.DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(mapper.writeValueAsString(json)).addParam(inode).loadResult();
            com.dotmarketing.business.CacheLocator.getContentletCache().remove(inode);
            final Contentlet legacy = contentletAPI.find(inode, user, false);
            assertNull(BinaryAssetReference.find(inode, field));
            assertFalse(binaryAssetStorageAPI.evictLocalFile(local.toFile()));

            client.putObject(bucket, binaryObjectKey(key), "conflicting destination bytes");
            assertThrows(com.dotmarketing.exception.DotDataException.class, BinaryAssetBackfill::runAll);
            assertEquals("legacy imported bytes", Files.readString(local));
            assertTrue(Files.exists(localMetadata));
            assertEquals("conflicting destination bytes", client.getObjectAsString(bucket,
                    binaryObjectKey(key)));
            client.deleteObject(bucket, binaryObjectKey(key));

            // Existing remote raw bytes must migrate too, including retired field definitions.
            client.putObject(bucket, binaryObjectKey(key), upload);
            assertTrue(binaryAssetStorageAPI.evictLocalFile(local.toFile()));
            final String retiredKey = prefix + "/retiredBinary/Retired-Mixed.Txt";
            client.putObject(bucket, binaryObjectKey(retiredKey), "retired field bytes");
            ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields")).putObject("retiredBinary")
                    .put("type", "Binary").put("value", "Retired-Mixed.Txt");
            new com.dotmarketing.common.db.DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(mapper.writeValueAsString(json)).addParam(inode).loadResult();

            var progress = BinaryAssetBackfill.runBatch("", 1);
            int copied = progress.binaries();
            while (!progress.complete()) {
                final String cursor = progress.afterInode();
                progress = BinaryAssetBackfill.runBatch(cursor, 1);
                assertTrue(progress.complete() || !cursor.equals(progress.afterInode()));
                copied += progress.binaries();
            }
            assertTrue(copied > 0);
            BinaryAssetBackfill.runAll();
            assertEquals(org.apache.commons.codec.digest.DigestUtils.sha256Hex("legacy imported bytes"),
                    client.getObjectMetadata(bucket, binaryObjectKey(key))
                            .getUserMetaDataOf("dotcms-blob-sha256"));
            assertEquals(org.apache.commons.codec.digest.DigestUtils.sha256Hex("retired field bytes"),
                    client.getObjectMetadata(bucket, binaryObjectKey(retiredKey))
                            .getUserMetaDataOf("dotcms-blob-sha256"));
            assertNull(BinaryAssetReference.find(inode, field), "Backfill must preserve legacy content references");
            assertTrue(binaryAssetStorageAPI.evictLocalFile(local.toFile()));
            Files.delete(localMetadata);
            com.dotmarketing.business.CacheLocator.getMetadataCache().removeMetadata(metadataAPI.getMetadataCacheKey(legacy, field));
            assertEquals("legacy imported bytes", Files.readString(contentletAPI.find(inode, user, false).getBinary(field).toPath()));
            assertEquals("Legacy author", metadataAPI.getMetadata(legacy, field).getCustomMeta().get("credit"));
        } finally {
            client.shutdown();
            FolderDataGen.remove(folder);
        }
    }

    @Test
    void legacyImageAndFileBytesSurviveMigrationExportAndRecoveryWithoutChangingFieldTypes(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        String archiveKey = null;
        final var backups = ContentletBackupStorage.getInstance();
        try {
            for (final var field : java.util.Map.<String, Class<? extends Field>>of(
                    "title", TextField.class,
                    "legacyImage", com.dotcms.contenttype.model.field.ImageField.class,
                    "legacyFile", com.dotcms.contenttype.model.field.FileField.class).entrySet()) {
                ContentTypeDataGen.addField(new FieldDataGen().velocityVarName(field.getKey())
                        .contentTypeId(type.id()).type(field.getValue()).nextPersisted());
            }
            final Contentlet content = new ContentletDataGen(type).setProperty("title", "Legacy file inventory").nextPersisted();
            final String inode = content.getInode();
            final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode;
            final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
                    new com.dotmarketing.common.db.DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ?")
                            .addParam(inode).getString("contentlet_as_json"));
            final var fields = (com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields");
            fields.putObject("legacyImage").put("type", "Image").put("value", "Legacy-Mixed.PNG");
            fields.putObject("legacyFile").put("type", "File").put("value", "Document-Mixed.Txt");
            fields.putObject("retiredBinary").put("type", "Binary").put("value", "Retired-Mixed.Txt");
            fields.putObject("linkedImage").put("type", "Image").put("value", "other-asset-identifier");
            fields.putObject("externalFile").put("type", "File").put("value", "https://example.com/file.txt");
            final var expected = new java.util.HashMap<String, byte[]>();
            final var localFiles = new java.util.ArrayList<java.nio.file.Path>();
            for (String field : java.util.List.of("legacyImage", "legacyFile", "retiredBinary")) {
                final String key = prefix + "/" + field + "/" + fields.path(field).path("value").asText();
                final var local = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath(), key);
                Files.createDirectories(local.getParent());
                if (field.equals("legacyImage")) {
                    javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 10,
                            java.awt.image.BufferedImage.TYPE_INT_RGB), "png", local.toFile());
                } else {
                    Files.writeString(local, "original " + field + " bytes");
                }
                expected.put("assets/" + key, Files.readAllBytes(local));
                localFiles.add(local);
                assertFalse(binaryAssetStorageAPI.evictLocalFile(local.toFile()), "Unmigrated legacy bytes must stay local");
            }
            final String metadataPath = prefix + "/legacyimage-metadata.json";
            final var localMetadata = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath(), metadataPath);
            Files.writeString(localMetadata, "{\"dot:credit\":\"Legacy image author\"}");
            final String snapshot = mapper.writeValueAsString(json);
            new com.dotmarketing.common.db.DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                    .addParam(snapshot).addParam(inode).loadResult();
            com.dotmarketing.business.CacheLocator.getContentletCache().remove(inode);
            BinaryAssetBackfill.runAll();
            assertEquals(json, mapper.readTree(new com.dotmarketing.common.db.DotConnect()
                    .setSQL("select contentlet_as_json from contentlet where inode = ?").addParam(inode)
                    .getString("contentlet_as_json")), "Migration must not turn linked-field strings into binary values");
            assertEquals("Legacy-Mixed.PNG", contentletAPI.find(inode, user, false).get("legacyImage"));
            for (var local : localFiles) assertTrue(binaryAssetStorageAPI.evictLocalFile(local.toFile()));
            Files.delete(localMetadata);
            final var output = new java.io.ByteArrayOutputStream();
            new com.dotmarketing.util.starter.ExportStarterUtil().streamCompressedAssets(output, true, -1);
            final var exported = readZipEntries(output.toByteArray());
            assertEquals("Legacy-Mixed.PNG", contentletAPI.find(inode, user, false).get("legacyImage"),
                    "Export must not replace a cached Image field's string with a File");
            for (var entry : expected.entrySet()) assertArrayEquals(entry.getValue(), exported.get(entry.getKey()), entry.getKey());
            assertEquals("Legacy image author", mapper.readTree(exported.get("assets/" + metadataPath)).path("dot:credit").asText());
            for (var local : localFiles) assertTrue(binaryAssetStorageAPI.evictLocalFile(local.toFile()));
            Files.deleteIfExists(localMetadata);
            com.dotmarketing.db.HibernateUtil.startTransaction();
            try {
                archiveKey = backups.store(content);
                com.dotmarketing.db.HibernateUtil.commitTransaction();
            } finally { com.dotmarketing.db.HibernateUtil.rollbackTransaction(); }
            try (var archive = backups.open(archiveKey)) {
                final var recovered = readZipEntries(archive.readAllBytes());
                for (var entry : expected.entrySet()) assertArrayEquals(entry.getValue(), recovered.get(entry.getKey()), entry.getKey());
                assertEquals(json, mapper.readTree(recovered.get("contentlet.json")));
                assertEquals("Legacy image author", mapper.readTree(recovered.get("assets/" + metadataPath)).path("dot:credit").asText());
            }
        } finally {
            if (archiveKey != null) com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl.withPlainPaths()
                    .deleteObjectAndReferences(ContentletBackupStorage.GROUP, archiveKey);
            ContentTypeDataGen.remove(type);
        }
    }

    private String binaryObjectKey(String path) {
        final String namespace = System.getProperty("DOT_STORAGE_FILE_METADATA_S3_NAMESPACE", "");
        return (namespace.isEmpty() ? "" : "asset-namespaces/" + namespace + "/")
                + BinaryAssetStorageAPI.BINARY_ASSETS_GROUP + "/" + path;
    }

    private java.util.Map<String, byte[]> readZipEntries(final byte[] bytes) throws Exception {
        final java.util.Map<String, byte[]> entries = new java.util.HashMap<>();
        try (final var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                assertNull(entries.put(entry.getName(), zip.readAllBytes()), "ZIP entries must not be duplicated");
            }
        }
        return entries;
    }

    private com.dotcms.storage.FetchMetadataParams metadataRequest(final String path) {
        return new com.dotcms.storage.FetchMetadataParams.Builder().cache(false)
                .storageKey(new com.dotcms.storage.StorageKey.Builder().group(com.dotcms.storage.FileMetadataAPI.DOT_METADATA)
                        .path(path).storage(com.dotcms.storage.StorageType.DEFAULT_CHAIN).build()).build();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void s3CmsImageUsesItsOwnRenditionDirectoryAndRestoresAfterEviction(final boolean legacy,
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path uploads) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("s3.cms.enabled"));
        final Folder folder = new FolderDataGen().nextPersisted();
        try {
            final File upload = uploads.resolve("dotGenerated_UserUpload.PNG").toFile();
            final java.awt.image.BufferedImage pixels = new java.awt.image.BufferedImage(64, 48,
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 48; y++) {
                    pixels.setRGB(x, y, (x < 32 ? java.awt.Color.RED : java.awt.Color.BLUE).getRGB());
                }
            }
            javax.imageio.ImageIO.write(pixels, "PNG", upload);
            Contentlet asset = new FileAssetDataGen(folder, upload).nextPersisted();
            final String inode = asset.getInode();
            if (legacy) {
                // Model a pre-feature binary without changing the runtime feature flag.
                final String field = FileAssetAPI.BINARY_FIELD;
                final File local = new BinaryAssetReference.StoredBinary(null, null, upload.getName())
                        .localFile(inode, field);
                Files.createDirectories(local.toPath().getParent());
                Files.copy(upload.toPath(), local.toPath());
                final var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                final var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(
                        new com.dotmarketing.common.db.DotConnect()
                                .setSQL("select contentlet_as_json from contentlet where inode = ?")
                                .addParam(inode).getString("contentlet_as_json"));
                final var binary = (com.fasterxml.jackson.databind.node.ObjectNode) json.path("fields").path(field);
                binary.remove("storageKey");
                binary.remove("metadataStorageKey");
                new com.dotmarketing.common.db.DotConnect()
                        .setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                        .addParam(mapper.writeValueAsString(json)).addParam(inode).loadResult();
                com.dotmarketing.business.CacheLocator.getContentletCache().remove(inode);
                asset = contentletAPI.find(inode, user, false);
                assertNull(BinaryAssetReference.find(inode, field));
                assertTrue(binaryAssetStorageAPI.backfillBinary(inode, field, local));
            }
            final java.util.Map<String, String[]> parameters = new java.util.HashMap<>();
            parameters.put("filter", new String[]{"resize"});
            parameters.put("resize_w", new String[]{"24"});
            parameters.put("fieldVarName", new String[]{FileAssetAPI.BINARY_FIELD});
            parameters.put("assetInodeOrIdentifier", new String[]{inode});
            final var exporter = new com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter();
            final File original = asset.getBinary(FileAssetAPI.BINARY_FIELD);
            final File rendition = exporter.exportContent(original, parameters).getDataFile();
            assertEquals(24, javax.imageio.ImageIO.read(rendition).getWidth());
            assertEquals(java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getDotGeneratedPath(),
                    inode.substring(0, 1), inode.substring(1, 2), inode).toFile().getCanonicalFile(),
                    rendition.getParentFile());
            final byte[] expected = Files.readAllBytes(rendition.toPath());
            assertTrue(binaryAssetStorageAPI.evictLocalFile(original));
            assertTrue(binaryAssetStorageAPI.evictLocalFile(rendition));
            final File restored = exporter.exportContent(contentletAPI.find(inode, user, false)
                    .getBinary(FileAssetAPI.BINARY_FIELD), parameters).getDataFile();
            assertArrayEquals(expected, Files.readAllBytes(restored.toPath()));
            parameters.put("filter", new String[]{"resize", "crop"});
            parameters.put("crop_w", new String[]{"6"});
            parameters.put("crop_h", new String[]{"6"});
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(asset,
                    java.util.Map.of(FileAssetAPI.BINARY_FIELD, java.util.Map.of("focalPoint", "0.25,0.5")));
            final File redSource = asset.getBinary(FileAssetAPI.BINARY_FIELD);
            final File red = exporter.exportContent(redSource, parameters).getDataFile();
            assertEquals(java.awt.Color.RED.getRGB(), javax.imageio.ImageIO.read(red).getRGB(3, 3));
            APILocator.getFileMetadataAPI().putCustomMetadataAttributes(asset,
                    java.util.Map.of(FileAssetAPI.BINARY_FIELD, java.util.Map.of("focalPoint", "0.75,0.5")));
            final File blueSource = contentletAPI.find(inode, user, false).getBinary(FileAssetAPI.BINARY_FIELD);
            final File blue = exporter.exportContent(blueSource, parameters).getDataFile();
            assertNotEquals(red, blue, "A focal edit must change the chained crop cache key");
            assertEquals(java.awt.Color.BLUE.getRGB(), javax.imageio.ImageIO.read(blue).getRGB(3, 3));
            final byte[] cropBytes = Files.readAllBytes(blue.toPath());
            assertTrue(binaryAssetStorageAPI.evictLocalFile(blue));
            assertArrayEquals(cropBytes, Files.readAllBytes(exporter.exportContent(blueSource, parameters).getDataFile().toPath()));
            assertTrue(binaryAssetStorageAPI.evictLocalFile(red));
            assertEquals(red, exporter.exportContent(redSource, parameters).getDataFile(),
                    "An older metadata snapshot must retain its focal crop after a later edit");
            assertEquals(java.awt.Color.RED.getRGB(), javax.imageio.ImageIO.read(red).getRGB(3, 3));
            assertFalse(parameters.containsKey("fp"), "An export must not pin metadata in the caller's reusable parameters");
            APILocator.getFileAssetAPI().cleanThumbnailsFromFileAsset(APILocator.getFileAssetAPI().fromContentlet(asset));
            assertFalse(restored.exists());
            assertNull(binaryAssetStorageAPI.getGeneratedFile(restored), "CMS invalidation must delete the S3 rendition");
        } finally {
            FolderDataGen.remove(folder);
        }
    }

    private void assertBinaryMetadataLength(final Contentlet contentlet, final long expected) throws Exception {
        final com.dotcms.storage.model.Metadata metadata = APILocator.getFileMetadataAPI()
                .getOrGenerateMetadata(contentlet, "heroImage");
        assertNotNull(metadata);
        assertEquals(Long.toString(expected), String.valueOf(metadata.getFieldsMeta().get("length")),
                "Metadata must describe the binary referenced by this content snapshot");
    }

    private void assertMetadataRestoredFromS3(final Contentlet contentlet, final long expected) throws Exception {
        assertMetadataRestoredFromS3(contentlet, expected, "Original author");
    }

    private void assertMetadataRestoredFromS3(final Contentlet contentlet, final long expected,
                                             final String credit) throws Exception {
        final com.dotcms.storage.FileMetadataAPI metadataAPI = APILocator.getFileMetadataAPI();
        final String key = metadataAPI.getFileName(contentlet, "heroImage");
        // The existing metadata filesystem provider normalizes its paths to lowercase.
        final java.nio.file.Path local = java.nio.file.Path.of(com.dotmarketing.util.ConfigUtils.getAssetPath())
                .resolve(key.substring(1).toLowerCase(java.util.Locale.ROOT));
        Files.delete(local);
        com.dotmarketing.business.CacheLocator.getMetadataCache()
                .removeMetadata(metadataAPI.getMetadataCacheKey(contentlet, "heroImage"));
        // getMetadata cannot regenerate from the binary: only the stored S3 object can satisfy this read.
        final com.dotcms.storage.model.Metadata restored = metadataAPI.getMetadata(contentlet, "heroImage");
        assertNotNull(restored, "Metadata must survive loss of both local caches");
        assertEquals(Long.toString(expected), String.valueOf(restored.getFieldsMeta().get("length")));
        assertEquals(credit, restored.getCustomMeta().get("credit"));
        assertTrue(Files.isRegularFile(local), "S3 retrieval must repopulate the local metadata cache");
    }

    /**
     * AC-1: Contentlet binary checkin + retrieval works through abstraction.
     * Creates a content type with a binary field, checks in a contentlet with a test file,
     * and verifies retrieval through both Contentlet.getBinary() and BinaryAssetStorageAPI.
     */
    @Test
    void test_checkin_contentlet_with_binary_stores_and_retrieves_file() throws Exception {

        final String testContent = "integration-test-content-" + System.currentTimeMillis();
        ContentType contentType = null;

        try {
            // Create content type with title + binary field
            contentType = new ContentTypeDataGen().nextPersisted();

            final Field titleField = new FieldDataGen()
                    .velocityVarName("title")
                    .contentTypeId(contentType.id())
                    .type(TextField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(titleField);

            final Field binaryField = new FieldDataGen()
                    .velocityVarName("testBinary")
                    .contentTypeId(contentType.id())
                    .type(BinaryField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(binaryField);

            // Create temp file with known content
            final File tempFile = File.createTempFile("binary-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            // Create and checkin contentlet with binary
            final ContentletDataGen dataGen = new ContentletDataGen(contentType);
            dataGen.setProperty("title", "Binary Test");
            dataGen.setProperty("testBinary", tempFile);
            final Contentlet checkedIn = dataGen.nextPersisted();

            // Verify via Contentlet.getBinary (migrated in 02-01)
            final File retrievedFile = checkedIn.getBinary("testBinary");
            assertNotNull(retrievedFile, "getBinary should return a file");
            assertTrue(retrievedFile.exists(), "Retrieved file should exist on disk");
            assertEquals(testContent, Files.readString(retrievedFile.toPath()),
                    "File content should match what was uploaded");

            if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                final String inode = checkedIn.getInode();
                final java.nio.file.Path legacyPath = java.nio.file.Path.of(
                        com.dotmarketing.util.ConfigUtils.getAssetPath(),
                        inode.substring(0, 1), inode.substring(1, 2), inode, "testBinary", tempFile.getName());
                assertTrue(Files.isSameFile(legacyPath, retrievedFile.toPath()),
                        "Disabled mode must retain the legacy filesystem/NFS layout");
                assertNull(BinaryAssetReference.find(inode, "testBinary"),
                        "Disabled check-in must not persist an S3 revision reference");
                assertFalse(binaryAssetStorageAPI.evictLocalFile(retrievedFile));
                assertTrue(retrievedFile.exists(), "Disabled eviction must leave the original in place");
            }

            // Verify via BinaryAssetStorageAPI directly (2-arg, filename-less)
            final File apiFile = binaryAssetStorageAPI.getBinaryFile(
                    checkedIn.getInode(), "testBinary");
            assertNotNull(apiFile, "BinaryAssetStorageAPI should find the file");
            assertEquals(retrievedFile.getName(), apiFile.getName(),
                    "API file name should match getBinary file name");

        } finally {
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
        }
    }

    /**
     * AC-2: Content versioning preserves binaries through abstraction.
     * Creates a contentlet with binary, checks out, modifies non-binary field,
     * checks back in, and verifies binary survives the version.
     */
    @Test
    void test_content_version_preserves_binary() throws Exception {

        final String testContent = "version-test-content-" + System.currentTimeMillis();
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen().nextPersisted();

            final Field titleField = new FieldDataGen()
                    .velocityVarName("title")
                    .contentTypeId(contentType.id())
                    .type(TextField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(titleField);

            final Field binaryField = new FieldDataGen()
                    .velocityVarName("testBinary")
                    .contentTypeId(contentType.id())
                    .type(BinaryField.class)
                    .nextPersisted();
            ContentTypeDataGen.addField(binaryField);

            // Create temp file and checkin
            final File tempFile = File.createTempFile("version-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            final ContentletDataGen dataGen = new ContentletDataGen(contentType);
            dataGen.setProperty("title", "Version Test v1");
            dataGen.setProperty("testBinary", tempFile);
            final Contentlet v1 = dataGen.nextPersisted();

            // Checkout, modify title only, checkin again (new version)
            final Contentlet checkout = contentletAPI.checkout(v1.getInode(), user, false);
            checkout.setStringProperty("title", "Version Test v2");
            final Contentlet v2 = contentletAPI.checkin(checkout, user, false);

            // Verify binary survives on the new version
            assertNotEquals(v1.getInode(), v2.getInode(),
                    "New version should have a different inode");

            final File v2File = v2.getBinary("testBinary");
            assertNotNull(v2File, "Binary should exist on new version");
            assertTrue(v2File.exists(), "Binary file should exist on disk");
            assertEquals(testContent, Files.readString(v2File.toPath()),
                    "Binary content should match original after versioning");

        } finally {
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
        }
    }

    /**
     * AC-3: FileAsset flow works end-to-end.
     * Creates a FileAsset via FileAssetDataGen and verifies binary retrieval
     * through both Contentlet.getBinary and BinaryAssetStorageAPI.
     */
    @Test
    void test_fileAsset_flow_end_to_end() throws Exception {

        final String testContent = "fileasset-test-content-" + System.currentTimeMillis();
        Folder folder = null;

        try {
            folder = new FolderDataGen().nextPersisted();

            // Create temp file with known content
            final File tempFile = File.createTempFile("fileasset-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            // Create FileAsset via data generator
            final Contentlet fileAsset = new FileAssetDataGen(folder, tempFile).nextPersisted();

            // Verify via Contentlet.getBinary (exercises migrated read path)
            final File retrievedFile = fileAsset.getBinary(FileAssetAPI.BINARY_FIELD);
            assertNotNull(retrievedFile, "FileAsset binary should be retrievable");
            assertTrue(retrievedFile.exists(), "FileAsset binary should exist on disk");
            assertEquals(testContent, Files.readString(retrievedFile.toPath()),
                    "FileAsset content should match what was uploaded");

            // Verify via BinaryAssetStorageAPI directly (2-arg, filename-less)
            final File apiFile = binaryAssetStorageAPI.getBinaryFile(
                    fileAsset.getInode(), FileAssetAPI.BINARY_FIELD);
            assertNotNull(apiFile, "BinaryAssetStorageAPI should find the FileAsset binary");
            assertTrue(apiFile.exists(), "API-retrieved file should exist");

        } finally {
            if (folder != null) {
                FolderDataGen.remove(folder);
            }
        }
    }

    /**
     * AC-1 (backward compat): Deprecated getRealAssetPath still returns valid paths.
     * Ensures the deprecated methods continue to work for callers that haven't migrated.
     */
    @SuppressWarnings("deprecation")
    @Test
    void test_deprecated_getRealAssetPath_still_works() throws Exception {

        final String testContent = "deprecated-test-content-" + System.currentTimeMillis();
        Folder folder = null;

        try {
            folder = new FolderDataGen().nextPersisted();

            final File tempFile = File.createTempFile("deprecated-test-", ".txt");
            tempFile.deleteOnExit();
            Files.writeString(tempFile.toPath(), testContent);

            final Contentlet fileAsset = new FileAssetDataGen(folder, tempFile).nextPersisted();

            // Call deprecated method — should still return a valid path
            if (com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                assertTrue(binaryAssetStorageAPI.evictLocalFile(fileAsset.getBinary(FileAssetAPI.BINARY_FIELD)));
            }
            final String realPath = APILocator.getFileAssetAPI()
                    .getRealAssetPath(fileAsset.getInode(),
                            APILocator.getFileAssetAPI().fromContentlet(fileAsset).getUnderlyingFileName());

            assertNotNull(realPath, "Deprecated getRealAssetPath should return a path");

            final File fileFromPath = new File(realPath);
            assertTrue(fileFromPath.exists(),
                    "Path from deprecated method should point to an existing file");
            assertEquals(testContent, Files.readString(fileFromPath.toPath()),
                    "Content from deprecated path should match uploaded content");
            assertEquals(realPath, APILocator.getFileAssetAPI().getRealAssetPathIgnoreExtensionCase(
                    fileAsset.getInode(), fileFromPath.getName()));
            final String differentName = APILocator.getFileAssetAPI().getRealAssetPath(fileAsset.getInode(), "DifferentName.txt");
            assertNotEquals(realPath, differentName);
            assertTrue(differentName.endsWith("DifferentName.txt"));

        } finally {
            if (folder != null) {
                FolderDataGen.remove(folder);
            }
        }
    }

}
