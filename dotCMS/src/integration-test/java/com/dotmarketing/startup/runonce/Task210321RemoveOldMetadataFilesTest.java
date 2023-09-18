package com.dotmarketing.startup.runonce;

import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import io.vavr.Tuple2;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static org.junit.Assert.assertTrue;

public class Task210321RemoveOldMetadataFilesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link Task210321RemoveOldMetadataFiles#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Recreate the asset + metadata structure under the {@code
     *     ASSET_REAL_PATH} folder.</li>
     *     <li><b>Expected Result: </b>Since we're passing a file asset, we expect at least one
     *     occurrence of the {@code fileasset-metadata.json}.</li>
     * </ul>
     *
     * @throws DotDataException     An error occurred when generating the file's metadata.
     * @throws IOException          An error occurred when generating the file's metadata.
     * @throws ExecutionException   An error occurred when retrieving the result of the Future
     *                              object.
     * @throws InterruptedException An error occurred when retrieving the result of the Future
     *                              object.
     */
    @Test
    public void Test_Upgrade_Task()
            throws DotDataException, IOException, ExecutionException, InterruptedException {
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE, null);
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            //Recreate the metadata structure
            final Contentlet contentlet = getFileAssetContent(true, langId, TestFile.PDF);
            //And the metaData/content structure too
            APILocator.getFileMetadataAPI().generateContentletMetadata(contentlet);

            final Task210321RemoveOldMetadataFiles task = new Task210321RemoveOldMetadataFiles();
            assertTrue(task.forceRun());
            task.executeUpgrade();
            final Tuple2<Integer, Integer> tuple = task.getFuture().get();
            // At least 1 fileasset-metadata.json
            assertTrue("The Task210321RemoveOldMetadataFiles should've deleted at least one file"
                    , tuple._1 >= 1);
            // Zero metaData/content
            assertTrue("There should NOT be any 'metaData/content' folders anymore", 0 == tuple._2);
        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        }
    }

}
