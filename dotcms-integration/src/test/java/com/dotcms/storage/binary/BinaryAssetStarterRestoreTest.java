package com.dotcms.storage.binary;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.starter.ExportStarterUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Run alone in two fresh disposable integration harnesses, first with s3.starter.phase=export,
 * then with s3.starter.phase=restore and starter.run.path pointing at s3.starter.archive.
 * The S3 bucket and exported archive survive between runs; the database and local cache do not.
 */
@EnabledIfSystemProperty(named = "s3.starter.restore.enabled", matches = "true")
public class BinaryAssetStarterRestoreTest {

    @Test
    @EnabledIfSystemProperty(named = "s3.starter.phase", matches = "export")
    void exportColdStarterAndRemoveSourceObjects(
            @TempDir final Path temporary) throws Exception {
        IntegrationTestInitService.getInstance().init();
        assertTrue(AssetStorageFeature.isEnabled());
        final var client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ENDPOINT"), "us-east-1"))
                .withPathStyleAccessEnabled(true)
                .withCredentials(com.dotmarketing.util.UtilMethods.isSet(System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ACCESS_KEY"))
                        ? new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_ACCESS_KEY"),
                        System.getProperty("DOT_STORAGE_FILE_METADATA_S3_SECRET_ACCESS_KEY")))
                        : new com.amazonaws.auth.DefaultAWSCredentialsProviderChain())
                .build();
        try {
            final String bucket = System.getProperty("DOT_STORAGE_FILE_METADATA_S3_BUCKET_NAME");
            final String field = FileAssetAPI.BINARY_FIELD;
            final var metadata = APILocator.getFileMetadataAPI();
            final var binaries = APILocator.getBinaryAssetStorageAPI();
            final var configuration = createConfiguration("Starter", true);
            final var source = Files.writeString(temporary.resolve("Starter-MixedCase.Txt"), "starter restore original bytes");
            final var content = new FileAssetDataGen(new FolderDataGen().nextPersisted(), source.toFile()).nextPersisted();
            metadata.putCustomMetadataAttributes(content, Map.of(field, Map.of("credit", "Starter author")));
            final String inode = content.getInode();
            final var binary = content.getBinary(field);
            final String binaryKey = BinaryAssetReference.keyOf(binary);
            final String metadataPath = metadata.getFileName(content, field);
            final String remoteBinary = BinaryAssetStorageAPI.BINARY_ASSETS_GROUP + "/" + binaryKey;
            final var localMetadata = Path.of(ConfigUtils.getAssetPath())
                    .resolve(metadataPath.substring(1).toLowerCase(Locale.ROOT));
            assertNotNull(binaryKey);
            assertTrue(binaries.evictLocalFile(binary));
            Files.delete(localMetadata);
            CacheLocator.getMetadataCache().removeMetadata(metadata.getMetadataCacheKey(content, field));

            final Path archive = Path.of(System.getProperty("s3.starter.archive"));
            Files.createDirectories(archive.getParent());
            try (final var output = Files.newOutputStream(archive)) {
                new ExportStarterUtil().streamCompressedStarter(output, true, true, -1);
            }
            assertTrue(Files.size(archive) > 0);

            // Remove only this test asset's durable objects and cache after the archive is complete.
            // A successful post-import read must therefore come from the import, not the source bucket.
            binaries.deleteAllBinaries(inode);
            com.dotcms.storage.StoragePersistenceProvider.INSTANCE.get().getStorage()
                    .deleteObjectAndReferences(com.dotcms.storage.FileMetadataAPI.DOT_METADATA, metadataPath);
            Files.deleteIfExists(localMetadata);
            assertFalse(client.doesObjectExist(bucket, remoteBinary));
            assertFalse(binary.exists());
            assertFalse(com.dotcms.storage.StoragePersistenceProvider.INSTANCE.get()
                    .getStorage(com.dotcms.storage.StorageType.S3)
                    .existsObject(com.dotcms.storage.FileMetadataAPI.DOT_METADATA, metadataPath));
            new ObjectMapper().writeValue(archive.resolveSibling("fixture.json").toFile(),
                    Map.of("inode", inode, "binaryKey", binaryKey, "metadataPath", metadataPath,
                            "configuration", configuration));
        } finally {
            client.shutdown();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "s3.starter.phase", matches = "restore")
    void freshStartupRestoresOriginalAndMetadataFromExportedStarter() throws Exception {
        final Path archive = Path.of(System.getProperty("s3.starter.archive"));
        assertEquals(archive.toAbsolutePath().toString(), System.getProperty("DOT_STARTER_DATA_LOAD"),
                "The fresh CMS must actually start from the exported archive");
        IntegrationTestInitService.getInstance().init();
        assertRestoredFromArchive(archive);
    }

    @Test
    @EnabledIfSystemProperty(named = "s3.starter.phase", matches = "populated")
    void populatedDatabaseRestoresOriginalAndMetadataFromExportedStarter() throws Exception {
        final Path archive = Path.of(System.getProperty("s3.starter.archive"));
        assertNotEquals(archive.toAbsolutePath().toString(), System.getProperty("DOT_STARTER_DATA_LOAD"),
                "Populate the disposable harness with its normal starter before importing the archive");
        IntegrationTestInitService.getInstance().init();
        assertTrue(AssetStorageFeature.isEnabled());
        assertTrue(new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from workflow_action").getInt("total") > 0,
                "This case must exercise an existing workflow graph");
        final var oldTemplate = new com.dotcms.datagen.TemplateDataGen().nextPersisted();
        final var oldCategory = new com.dotcms.datagen.CategoryDataGen().nextPersisted();
        assertNotNull(APILocator.getTemplateAPI().find(oldTemplate.getInode(), APILocator.systemUser(), false));
        assertNotNull(APILocator.getCategoryAPI().find(oldCategory.getInode(), APILocator.systemUser(), false));
        final var fixture = new ObjectMapper().readTree(archive.resolveSibling("fixture.json").toFile());
        final var oldConfiguration = createConfiguration("Destination", false);
        new com.dotcms.datagen.VariantDataGen()
                .name(fixture.path("configuration").path("variant").asText())
                .description("Conflicting destination variant").nextPersisted();
        assertFalse(com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl.withPlainPaths()
                .existsObject(BinaryAssetStorageAPI.BINARY_ASSETS_GROUP, fixture.path("binaryKey").asText()),
                "Run the export phase first: the fixture must be absent from S3 before import");
        assertFalse(com.dotcms.storage.StoragePersistenceProvider.INSTANCE.get()
                .getStorage(com.dotcms.storage.StorageType.S3)
                .existsObject(com.dotcms.storage.FileMetadataAPI.DOT_METADATA, fixture.path("metadataPath").asText()),
                "The fixture's metadata must also be absent before import");
        final var importer = new com.dotmarketing.util.starter.ImportStarterUtil(archive.toFile());
        final int existingContent = new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from contentlet").getInt("total");
        final var cleanup = importer.getClass().getDeclaredMethod("deleteDotCMS");
        cleanup.setAccessible(true);
        com.dotmarketing.db.HibernateUtil.startTransaction();
        try {
            new com.dotmarketing.common.db.DotConnect().setSQL(
                    "create table starter_cleanup_guard (inode varchar(36) references contentlet(inode))")
                    .getResult();
            new com.dotmarketing.common.db.DotConnect().setSQL(
                    "insert into starter_cleanup_guard select inode from contentlet limit 1").getResult();
            final var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> cleanup.invoke(importer));
            assertTrue(failure.getCause().getMessage().contains("starter_cleanup_guard"),
                    "Foreign keys must remain enforced during full replacement");
        } finally {
            com.dotmarketing.db.HibernateUtil.rollbackTransaction();
        }
        assertDeletionTriggersEnabled();
        assertEquals(existingContent, new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from contentlet").getInt("total"));
        com.dotmarketing.db.HibernateUtil.startTransaction();
        try {
            cleanup.invoke(importer);
            assertEquals(0, new com.dotmarketing.common.db.DotConnect()
                    .setSQL("select count(*) as total from contentlet").getInt("total"));
            assertDeletionTriggersEnabled();
        } finally {
            com.dotmarketing.db.HibernateUtil.rollbackTransaction();
        }
        assertEquals(existingContent, new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from contentlet").getInt("total"),
                "Rolling back cleanup must retain the populated site");
        assertDeletionTriggersEnabled();
        importer.doImport();
        assertDeletionTriggersEnabled();
        assertRestoredFromArchive(archive);
        assertTrue(com.dotmarketing.business.FactoryLocator.getVariantFactory()
                .get(oldConfiguration.get("variant")).isEmpty());
        assertTrue(com.dotmarketing.business.FactoryLocator.getExperimentsFactory()
                .find(oldConfiguration.get("experiment")).isEmpty());
        assertNull(APILocator.getRulesAPI().getRuleById(oldConfiguration.get("rule"), APILocator.systemUser(), false));
        assertNull(APILocator.getTemplateAPI().find(oldTemplate.getInode(), APILocator.systemUser(), false),
                "Restored sites must not serve cached templates from the replaced site");
        assertNull(APILocator.getCategoryAPI().find(oldCategory.getInode(), APILocator.systemUser(), false));
        assertEquals(0, new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from template where inode = ?")
                .addParam(oldTemplate.getInode()).getInt("total"));
        assertEquals(0, new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from category where inode = ?")
                .addParam(oldCategory.getInode()).getInt("total"));
        assertTrue(new com.dotmarketing.common.db.DotConnect()
                .setSQL("select count(*) as total from workflow_action").getInt("total") > 0,
                "The archive must restore its workflows after cleanup");
    }

    private static void assertDeletionTriggersEnabled() {
        assertEquals(2, new com.dotmarketing.common.db.DotConnect().setSQL(
                "select count(*) as total from pg_trigger where tgenabled = 'O' and "
                        + "((tgrelid = 'contentlet'::regclass and tgname = 'content_versions_check_trigger') "
                        + "or (tgrelid = 'identifier'::regclass and tgname = 'check_child_assets_trigger'))")
                .getInt("total"), "Starter cleanup must restore the deletion hooks");
    }

    private static Map<String, String> createConfiguration(String prefix, boolean archived) throws Exception {
        final var variant = new com.dotcms.datagen.VariantDataGen().name(prefix + "_Variant")
                .description(prefix + " variant").archived(archived).nextPersisted();
        final var experiment = new com.dotcms.datagen.ExperimentDataGen().name(prefix + " experiment").nextPersisted();
        final var rule = new com.dotmarketing.portlets.rules.RuleDataGen().name(prefix + " rule").nextPersisted();
        final var group = new com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen()
                .rule(rule).nextPersisted();
        final var condition = new com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen().group(group).next();
        condition.addValue("comparison", "is");
        condition.addValue("country", "CA");
        APILocator.getRulesAPI().saveCondition(condition, APILocator.systemUser(), false);
        final var action = new com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen().rule(rule).next();
        action.addParameter(new com.dotmarketing.portlets.rules.model.ParameterModel("sessionKey", "starter"));
        action.addParameter(new com.dotmarketing.portlets.rules.model.ParameterModel("sessionValue", prefix));
        APILocator.getRulesAPI().saveRuleAction(action, APILocator.systemUser(), false);
        // Warm the caches too; cleanup must remove persisted records and their cached representations.
        assertTrue(com.dotmarketing.business.FactoryLocator.getVariantFactory().get(variant.name()).isPresent());
        assertTrue(com.dotmarketing.business.FactoryLocator.getExperimentsFactory().find(experiment.id().orElseThrow()).isPresent());
        assertNotNull(APILocator.getRulesAPI().getRuleById(rule.getId(), APILocator.systemUser(), false));
        return Map.of("variant", variant.name(), "experiment", experiment.id().orElseThrow(),
                "rule", rule.getId(), "condition", condition.getId(), "action", action.getId());
    }

    private void assertRestoredFromArchive(final Path archive) throws Exception {
        final var fixture = new ObjectMapper().readTree(archive.resolveSibling("fixture.json").toFile());
        assertTrue(AssetStorageFeature.isEnabled());
        final String inode = fixture.path("inode").asText();
        final String field = FileAssetAPI.BINARY_FIELD;
        final String binaryKey = fixture.path("binaryKey").asText();
        final String metadataPath = fixture.path("metadataPath").asText();
        final var configuration = fixture.path("configuration");
        final var variant = com.dotmarketing.business.FactoryLocator.getVariantFactory()
                .get(configuration.path("variant").asText()).orElseThrow();
        assertTrue(variant.archived(), "Restore must replace a conflicting active destination variant");
        assertEquals("Starter variant", variant.description().orElseThrow());
        assertTrue(com.dotmarketing.business.FactoryLocator.getVariantFactory().get("DEFAULT").isPresent());
        assertEquals("Starter experiment", com.dotmarketing.business.FactoryLocator.getExperimentsFactory()
                .find(configuration.path("experiment").asText()).orElseThrow().name());
        final var rule = APILocator.getRulesAPI().getRuleById(configuration.path("rule").asText(), APILocator.systemUser(), false);
        assertNotNull(rule);
        assertEquals("Starter rule", rule.getName());
        assertEquals(1, rule.getGroups().size());
        assertEquals(1, rule.getGroups().get(0).getConditions().size());
        assertEquals("CA", rule.getGroups().get(0).getConditions().get(0).getParameters().get("country").getValue());
        assertEquals(1, rule.getRuleActions().size());
        assertEquals("Starter", rule.getRuleActions().get(0).getParameters().get("sessionValue").getValue());
        final var metadata = APILocator.getFileMetadataAPI();
        final var binaries = APILocator.getBinaryAssetStorageAPI();
        assertFalse(Files.exists(Path.of(ConfigUtils.getBackupPath(), "temp")),
                "Successful import must clean up its extracted input");
        final var restored = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
        assertEquals(binaryKey, BinaryAssetReference.find(inode, field));
        assertEquals(metadataPath, metadata.getFileName(restored, field));
        assertTrue(binaries.evictLocalFile(restored.getBinary(field)), "Import must publish a verified S3 copy");
        Files.delete(Path.of(ConfigUtils.getAssetPath()).resolve(metadataPath.substring(1).toLowerCase(Locale.ROOT)));
        CacheLocator.getMetadataCache().removeMetadata(metadata.getMetadataCacheKey(restored, field));
        assertEquals("Starter-MixedCase.Txt", restored.getBinary(field).getName());
        assertEquals("starter restore original bytes", Files.readString(restored.getBinary(field).toPath()));
        assertEquals("Starter author", metadata.getMetadata(restored, field).getCustomMeta().get("credit"));
    }
}
