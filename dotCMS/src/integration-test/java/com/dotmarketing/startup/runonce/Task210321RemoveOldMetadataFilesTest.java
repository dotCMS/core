package com.dotmarketing.startup.runonce;

import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210321RemoveOldMetadataFilesTest {


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given scenario: Recreate the asset + metadata structure under a temp ASSET_REAL_PATH
     * Expected scenario: since we're passing a file asset we expect just one occurrence of the fileasset-metadata.json
     * and one occurrence of /metaData/content
     * @throws DotDataException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Upgrade_Task()
            throws DotDataException, IOException, ExecutionException, InterruptedException, DotSecurityException {
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", null);
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        try {
            final Path temp = Files.createTempDirectory(null);
            Config.setProperty("ASSET_REAL_PATH", temp.toString());
            Config.setProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());

            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            //Recreate the metadata structure
            final Contentlet contentlet = getFileAssetContent(true, langId, TestFile.PDF);
            //And the metaData/content structure too
            APILocator.getFileMetadataAPI().generateContentletMetadata(contentlet);
            new TikaUtils().generateMetaData(contentlet, true);

            final Task210321RemoveOldMetadataFiles task = new Task210321RemoveOldMetadataFiles();
            assertTrue(task.forceRun());
            task.executeUpgrade();
            final Tuple2<Integer, Integer> tuple = task.getFuture().get();
            //1 fileasset-metadata.json
            assertEquals(1, tuple._1.intValue());
            //1 metaData/content
            assertEquals(1, tuple._2.intValue());

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty("ASSET_REAL_PATH", assetRealPath);
            StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        }


    }


}
