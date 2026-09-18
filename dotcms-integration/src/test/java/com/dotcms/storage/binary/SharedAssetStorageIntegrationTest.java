package com.dotcms.storage.binary;

import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
import com.dotcms.storage.JsonReaderDelegate;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "s3.cms.enabled", matches = "true")
public class SharedAssetStorageIntegrationTest {
    @BeforeAll static void initialize() throws Exception { IntegrationTestInitService.getInstance().init(); }
    @TempDir Path root;

    @Test void realTikaExtractionIsSharedWhileFilenameAndPathRemainPerUse() throws Exception {
        final String text = "Shared extracted document " + UUID.randomUUID();
        final var first = Files.writeString(root.resolve("First-Name.Txt"), text).toFile();
        final var second = Files.writeString(root.resolve("Another-Name.Txt"), text).toFile();
        final String hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(text);
        final var remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        final String group = "extracted-metadata";
        try {
            assertNotNull(new TikaUtils().extractorVersion(), "The actual loaded parser must identify its version");
            final var api = APILocator.getFileStorageAPI();
            final var firstMetadata = api.generateRawFullMetaData(first, 8192);
            final var secondMetadata = api.generateRawFullMetaData(second, 8192);
            assertEquals(first.getName(), firstMetadata.get("name"));
            assertEquals(second.getName(), secondMetadata.get("name"));
            assertNotEquals(firstMetadata.get("path"), secondMetadata.get("path"));
            assertEquals(firstMetadata.get("content"), secondMetadata.get("content"));
            assertTrue(secondMetadata.get("content").toString().contains(text));
            final var paths = remote.listObjectPaths(group, hash + "/");
            assertEquals(1, paths.size(), "Equal bytes with different filenames share one extraction");
            final var shared = (Map<?, ?>) remote.pullObject(group, paths.getFirst(), new JsonReaderDelegate<>(Map.class));
            assertFalse(shared.containsKey("name"));
            assertFalse(shared.containsKey("path"));
            assertEquals(firstMetadata.get("content"), shared.get("content"));
            api.generateRawFullMetaData(second, 4096);
            assertEquals(2, remote.listObjectPaths(group, hash + "/").size());
        } finally {
            for (String key : remote.listObjectPaths(group, hash + "/")) remote.deleteObjectAndReferences(group, key);
        }
    }
    @Test void coldRevisionMetadataAndLinkedImageUseTheExactOwner() throws Exception {
        final var assets = APILocator.getBinaryAssetStorageAPI();
        final var files = APILocator.getFileStorageAPI();
        final var metadata = APILocator.getFileMetadataAPI();
        final String inode = UUID.randomUUID().toString();
        final String field = "HeroImage";
        final String text = "Original metadata bytes " + UUID.randomUUID();
        final var source = Files.writeString(root.resolve("Mixed-Name.Txt"), text).toFile();
        final String hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(text);
        final var first = assets.storeRevision(inode, field, source.getName(), source);
        Files.writeString(source.toPath(), "Later revision bytes");
        final var replacement = assets.storeRevision(inode, field, source.getName(), source);
        final var config = new com.dotcms.storage.GenerateMetadataConfig.Builder()
                .storageKey(new com.dotcms.storage.StorageKey.Builder().group("dotmetadata")
                        .path("/metadata-cold-" + inode).storage(com.dotcms.storage.StorageType.FILE_SYSTEM).build())
                .override(true).store(false).cache(false).full(false).build();
        try {
            for (int mode = 0; mode < 3; mode++) {
                assertTrue(assets.evictLocalFile(first));
                assertFalse(first.exists());
                final var result = switch (mode) {
                    case 0 -> files.generateRawBasicMetaData(first);
                    case 1 -> files.generateRawFullMetaData(first, 8192);
                    default -> files.generateMetaData(first, config);
                };
                assertEquals(hash, result.get("sha256"));
                assertEquals("Mixed-Name.Txt", result.get("name"));
                if (mode == 1) assertTrue(result.get("content").toString().contains(text));
                assertEquals(text, Files.readString(first.toPath()), "Historical revision must not resolve latest bytes");
            }
        } finally {
            assets.deleteBinary(inode, field);
        }

        final var type = new com.dotcms.datagen.ContentTypeDataGen().nextPersisted();
        try {
            final var binaryField = new com.dotcms.datagen.FieldDataGen().contentTypeId(type.id())
                    .velocityVarName("fileAsset").type(com.dotcms.contenttype.model.field.BinaryField.class).nextPersisted();
            com.dotcms.datagen.ContentTypeDataGen.addField(binaryField);
            final var imageField = new com.dotcms.datagen.FieldDataGen().contentTypeId(type.id())
                    .velocityVarName("linkedImage").type(com.dotcms.contenttype.model.field.ImageField.class).nextPersisted();
            com.dotcms.datagen.ContentTypeDataGen.addField(imageField);
            final var linked = new com.dotcms.datagen.ContentletDataGen(type)
                    .setProperty("fileAsset", Files.writeString(root.resolve("Linked-Name.Txt"), text).toFile())
                    .nextPersisted();
            final var parent = new com.dotcms.datagen.ContentletDataGen(type)
                    .setProperty("fileAsset", source).setProperty("linkedImage", linked.getIdentifier()).nextPersisted();
            final var linkedFile = linked.getBinary("fileAsset");
            final var delegate = new com.dotcms.content.model.hydration.MetadataDelegate();
            assertTrue(assets.evictLocalFile(linkedFile));
            final var imageBuilder = com.dotcms.content.model.type.ImageFieldType.builder().value(linked.getIdentifier());
            delegate.hydrate(imageBuilder, imageField, parent, "metadata");
            assertEquals(hash, imageBuilder.build().metadata().get("sha256"), "Image metadata belongs to its linked binary");
            assertEquals("Linked-Name.Txt", imageBuilder.build().metadata().get("name"));
            assertFalse(linkedFile.exists(), "Complete stored metadata must not download the original");

            // No metadata exists at this snapshot key: hydration must restore the evicted revision.
            final var cold = new com.dotmarketing.portlets.contentlet.model.Contentlet(linked);
            final String missingMetadata = BinaryAssetReference.newMetadataKey(linkedFile, linked.getInode(), "fileAsset");
            cold.setProperty("fileAsset", BinaryAssetReference.withMetadata(linkedFile, linked.getInode(), "fileAsset", missingMetadata));
            final var binaryBuilder = com.dotcms.content.model.type.system.BinaryFieldType.builder().value(linkedFile.getName());
            delegate.hydrate(binaryBuilder, binaryField, cold, "metadata");
            assertEquals(hash, binaryBuilder.build().metadata().get("sha256"));
            assertTrue(linkedFile.exists());
            assertEquals(missingMetadata, metadata.getFileName(cold, "fileAsset"));
        } finally {
            com.dotcms.datagen.ContentTypeDataGen.remove(type);
        }
    }

}
