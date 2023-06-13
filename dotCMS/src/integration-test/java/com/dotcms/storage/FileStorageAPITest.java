package com.dotcms.storage;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.storage.model.BasicMetadataFields;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.FilenameUtils;
import org.apache.felix.framework.OSGIUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_3;
import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;
import static com.dotcms.datagen.TestDataUtils.removeAnyMetadata;
import static com.dotcms.rest.api.v1.temp.TempFileAPITest.mockHttpServletRequest;
import static com.dotcms.storage.FileMetadataAPI.BINARY_METADATA_VERSION;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static com.dotcms.storage.model.Metadata.CUSTOM_PROP_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileStorageAPITest {

    private static final String WRITE_METADATA_ON_REINDEX = ESMappingAPIImpl.WRITE_METADATA_ON_REINDEX;
    private static final String FILE_ASSET = FileAssetAPI.BINARY_FIELD;
    private static FileStorageAPI fileStorageAPI;

    /**
     * if we have a code that requires some environment initialization required to run prior to our dataProvider Methods the @BeforeClass annotation won't do
     * See https://github.com/TNG/junit-dataprovider/issues/114
     * That's why I'm making this a static method and calling it from every data provider we have here.
     * I know.. it Sucks.
     * @throws Exception
     */
    private static void prepareIfNecessary() throws Exception {
       if(null == fileStorageAPI){
           IntegrationTestInitService.getInstance().init();
           fileStorageAPI = new FileStorageAPIImpl();
           StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.MEMORY, ()-> new MemoryStoragePersistanceAPIImpl());

       }
    }

    /**
     * Method to test: This test tries the {@link MemoryStoragePersistanceAPIImpl}
     * Given Scenario: Will create a bucket, put a few objects, remover them and non existing ones, remove them
     * ExpectedResult: The bucket has to be created right, the objects must be there and the non existing ones must not
     *
     * @throws Exception
     */
    @Test
    public void Test_Mem_Storage() throws Exception {
        prepareIfNecessary();

        final String groupName = "mem-test";
        final StoragePersistenceAPI memStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.MEMORY);
        Assert.assertFalse("The bucket mem-test should not exist by default", memStorage.existsGroup(groupName));
        Assert.assertTrue("The bucket mem-test should be created", memStorage.createGroup(groupName));
        memStorage.pushObject(groupName, "/path1", null,"Object1", null);
        memStorage.pushObject(groupName, "/path2", null,"Object2", null);

        Assert.assertTrue("The object on group mem-test and path2 should exist", null != memStorage.pullObject(groupName, "/path2", null));
        Assert.assertTrue("The object on group mem-test and path1 should exist", null != memStorage.pullObject(groupName, "/path1", null));
        Assert.assertFalse("The object on group mem-xxx and path2 should not exist ", null != memStorage.pullObject("mem-xxx", "/path1", null));
        Assert.assertFalse("The object on group mem-test and pathxxxx should not exist ", null != memStorage.pullObject(groupName, "/pathxxxx", null));

        Assert.assertTrue("The delete object on group mem-test and path1 should be fine", memStorage.deleteObjectReference(groupName, "/path1"));
        Assert.assertFalse("The delete object on group mem-test and pathxxxx should be false", memStorage.deleteObjectReference(groupName, "/pathxxxx"));

        Assert.assertFalse("The delete group mem-xxxx should be false", memStorage.deleteGroup("mem-xxxx")<=0);
        Assert.assertFalse("The object on group mem-test and path1 should not exist", null != memStorage.pullObject(groupName, "/path1", null));
        Assert.assertTrue("The delete group mem-test should be right", memStorage.deleteGroup(groupName)>0);
        Assert.assertFalse("The object on group mem-test and path2 should not exist", null != memStorage.pullObject(groupName, "/path2", null));
        Assert.assertFalse("The object on group mem-test should not exist", memStorage.existsGroup(groupName));
    }

    /**
     * Method to test: This test tries the {@link CompositeStoragePersistenceAPI#pullObject(String, String, ObjectReaderDelegate)}
     * Given Scenario: To start will set a chain storage including as a first layer the file storage, then memory storage and finally db storage
     * - then will add a few elements to the memory storage
     * ExpectedResult: The cascate propagation you should ok right, will populate the objects from memory storage to the upper layer (file storage)
     *
     * @throws Exception
     */
    @Test
    public void Test_Composite_Storage_Chain_Cascate_propagate_stores() throws Exception {
        prepareIfNecessary();

        // Creates a chain storage with file, memory, and db in that order
        final JsonReaderDelegate jsonReaderDelegate = new JsonReaderDelegate(String.class);
        final CompositeStoragePersistenceAPIBuilder chainStorageBuilder = new CompositeStoragePersistenceAPIBuilder();

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI memStorage  = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.MEMORY);
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);
        final StoragePersistenceAPI dbStorage   = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.DB);

        // file -> mem -> db
        chainStorageBuilder.add(fileStorage);
        chainStorageBuilder.add(memStorage);
        chainStorageBuilder.add(dbStorage);

        StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.CHAIN1, chainStorageBuilder);
        final StoragePersistenceAPI chainStorage   = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.CHAIN1);

        // creates the group on all storages
        final String groupName = "bucket-test";

        try {

            // this creates all groups on all storages on the chain
            Try.run(() -> chainStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));

            // Now create a few objects on the mem storage
            final String[] paths = {"/path1.txt", "/path2.txt", "/path3.txt"};
            final String[] objects = {"Object1", "Object2", "Object3"};
            for (int i = 0; i < paths.length; i++) {
                memStorage.pushObject(groupName, paths[i], null, objects[i], null);
            }

            // Now run the chain cascada to populate file but not db
            final Object object1 = chainStorage.pullObject(groupName, "/path1.txt", jsonReaderDelegate);
            Assert.assertNotNull("The object on group bucket-test, /path1 should be not null", object1);
            Assert.assertEquals("The object on group bucket-test, /path1 should be Object1", objects[0], object1);

            // the push to file storage is async so wait a bit
            DateUtil.sleep(2000);

            // Now the file storage should have the objects by propagation
            final Object fileObject1 = fileStorage.pullObject(groupName, "/path1.txt", jsonReaderDelegate);
            Assert.assertNotNull("The object on group bucket-test, /path1 should be not null", fileObject1);
            Assert.assertEquals("The object on group bucket-test, /path1 should be Object1", objects[0], fileObject1);

            // but the db should not, since it is a lower layer
            final Object dbObject1 = Try.of(()->dbStorage.pullObject(groupName, "/path1.txt", jsonReaderDelegate)).getOrNull();
            Assert.assertNull("The object on group bucket-test, /path1 should be null", dbObject1);
        } finally {

            Stream.of(fileStorage, dbStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    }


}
