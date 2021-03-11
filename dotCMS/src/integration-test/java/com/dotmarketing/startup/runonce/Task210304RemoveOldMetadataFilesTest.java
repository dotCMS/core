package com.dotmarketing.startup.runonce;

import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import io.vavr.Tuple2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210304RemoveOldMetadataFilesTest {


    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Upgrade_Task()
            throws DotDataException, IOException, ExecutionException, InterruptedException {
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", null);
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        try {
            final Path temp = Files.createTempDirectory(null);
            Config.setProperty("ASSET_REAL_PATH", temp.toString());
            Config.setProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());

            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet contentlet = getMultipleBinariesContent(true, langId, null);

            APILocator.getFileMetadataAPI().generateContentletMetadata(contentlet);

            final Task210304RemoveOldMetadataFiles task = new Task210304RemoveOldMetadataFiles();
            assertTrue(task.forceRun());
            task.executeUpgrade();
            final Tuple2<Integer, Integer> tuple = task.getFuture().get();
            final long count = contentlet.getContentType().fields(BinaryField.class).stream().filter(field -> null != contentlet.get(field.variable())).count();
            assertTrue(tuple._1 >= count);
            assertEquals(0, tuple._2.intValue());

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.getStringProperty("ASSET_REAL_PATH", assetRealPath);
            StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        }

    }


}
