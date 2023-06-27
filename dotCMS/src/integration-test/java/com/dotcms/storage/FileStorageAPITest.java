package com.dotcms.storage;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.quartz.job.ReplicateStoragesJob;
import com.dotmarketing.util.DateUtil;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Test for the {@link FileStorageAPI} implementations
 * @author jsanca
 */
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
           StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.MEMORY, ()-> new MemoryMockTestStoragePersistanceAPIImpl());

       }
    }

    /**
     * Method to test: This test tries the {@link MemoryMockTestStoragePersistanceAPIImpl}
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
     * Method to test: This test tries the {@link ChainableStoragePersistenceAPI#existsObject(String, String)} (String, String, ObjectReaderDelegate)}
     * Given Scenario: Creates a group and stores some objects on it
     * ExpectedResult: The objects should be in each storage configurated on the chainable storage, after the delete the objects should be not on the storages
     *
     * @throws Exception
     */
    @Test
    public void Test_Chainable_Storage_check_exist_object() throws Exception {
        prepareIfNecessary();

        // Creates a chain storage with file, memory, and db in that order
        final JsonReaderDelegate jsonReaderDelegate = new JsonReaderDelegate(String.class);
        final JsonWriterDelegate jsonWriterDelegate = new JsonWriterDelegate();
        final ChainableStoragePersistenceAPIBuilder chainStorageBuilder = new ChainableStoragePersistenceAPIBuilder();

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI memStorage  = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.MEMORY);
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);

        // file -> mem -
        chainStorageBuilder.add(fileStorage);
        chainStorageBuilder.add(memStorage);

        StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.CHAIN3, chainStorageBuilder);
        final StoragePersistenceAPI chainStorage   = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.CHAIN3);

        // creates the group on all storages
        final String groupName = "bucket-test2-stores";

        try {

            // this creates all groups on all storages on the chain
            Try.run(() -> chainStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));

            Assert.assertTrue("The group should exists for the storage: " + chainStorage, Try.of(()->chainStorage.existsGroup(groupName)).getOrElse(false));

            Stream.of(fileStorage, memStorage).forEach(storage -> {
                Assert.assertTrue("The group should exists for the storage: " + storage, Try.of(()->storage.existsGroup(groupName)).getOrElse(false));
            });

            // Now create a few objects on the mem storage
            final String[] paths = {"/path1.txt", "/path2.txt", "/path3.txt"};
            final String[] objects = {"Object1", "Object2", "Object3"};
            for (int i = 0; i < paths.length; i++) {
                chainStorage.pushObject(groupName, paths[i], jsonWriterDelegate, objects[i], null);
            }

            Assert.assertTrue("Path1 should exist on mem storage", memStorage.existsObject(groupName, "/path1.txt"));
            Assert.assertTrue("Path1 should exist on file storage", fileStorage.existsObject(groupName, "/path1.txt"));
            Assert.assertTrue("Path1 should exist on file storage", chainStorage.existsObject(groupName, "/path1.txt"));

            Assert.assertFalse("Path1 should exist on mem storage", memStorage.existsObject(groupName, "/noexist.txt"));
            Assert.assertFalse("Path1 should exist on file storage", fileStorage.existsObject(groupName, "/noexist.txt"));
            Assert.assertFalse("Path1 should exist on file storage", chainStorage.existsObject(groupName, "/noexist.txt"));

            Assert.assertTrue("Path1 should exist on file storage", chainStorage.deleteObjectAndReferences(groupName, "/path1.txt"));

            Assert.assertFalse("Path1 should exist on mem storage", memStorage.existsObject(groupName, "/path1.txt"));
            Assert.assertFalse("Path1 should exist on file storage", fileStorage.existsObject(groupName, "/path1.txt"));
            Assert.assertFalse("Path1 should exist on file storage", chainStorage.existsObject(groupName, "/path1.txt"));

        } finally {

            Stream.of(fileStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    }

    /**
     * Method to test: This test tries the {@link ChainableStoragePersistenceAPI#pullObject(String, String, ObjectReaderDelegate)}
     * Given Scenario: To start will set a chain storage including as a first layer the file storage, then memory storage and finally db storage
     * - then will add a few elements to the memory storage
     * ExpectedResult: The cascate propagation you should ok right, will populate the objects from memory storage to the upper layer (file storage)
     *
     * @throws Exception
     */
    @Test
    public void Test_Chainable_Storage_Cascate_propagate_stores() throws Exception {
        prepareIfNecessary();

        // Creates a chain storage with file, memory, and db in that order
        final JsonReaderDelegate jsonReaderDelegate = new JsonReaderDelegate(String.class);
        final ChainableStoragePersistenceAPIBuilder chainStorageBuilder = new ChainableStoragePersistenceAPIBuilder();

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
        final String groupName = "bucket-test-stores";

        try {

            // this creates all groups on all storages on the chain
            Try.run(() -> chainStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));

            Assert.assertTrue("The group should exists for the storage: " + chainStorage, Try.of(()->chainStorage.existsGroup(groupName)).getOrElse(false));

            Stream.of(fileStorage, memStorage, dbStorage).forEach(storage -> {
                Assert.assertTrue("The group should exists for the storage: " + storage, Try.of(()->storage.existsGroup(groupName)).getOrElse(false));
            });

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



    /**
     * Method to test: This test tries the {@link ChainableStoragePersistenceAPI#pullObject(String, String, ObjectReaderDelegate)}
     * Given Scenario: To start will set a chain storage including as a first layer the file storage, then memory storage
     * - Then we will pull for some non existing objects
     * ExpectedResult: the result should be, the objects does not existe neither file nor memory, but should exist on cache
     *
     * @throws Exception
     */
    @Test
    public void Test_Chainable_Storage_Cache_404_pulling() throws Exception {
        prepareIfNecessary();

        // Creates a chain storage with file, memory, and db in that order
        final JsonReaderDelegate jsonReaderDelegate = new JsonReaderDelegate(String.class);
        final JsonWriterDelegate writerDelegate     = new JsonWriterDelegate();
        final ChainableStoragePersistenceAPIBuilder chainStorageBuilder = new ChainableStoragePersistenceAPIBuilder();

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI memStorage  = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.MEMORY);
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);

        // file -> mem -> db
        chainStorageBuilder.add(fileStorage);
        chainStorageBuilder.add(memStorage);

        StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.CHAIN2, chainStorageBuilder);
        final StoragePersistenceAPI chainStorage   = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.CHAIN2);

        // creates the group on all storages
        final String groupName = "bucket-test-404";

        try {

            // this creates all groups on all storages on the chain
            Try.run(() -> chainStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            final String path1 = "/path1.txt";
            final String path2 = "/path2.txt";

            // Now run the chain cascada to populate file but not db
            final Object object1 = Try.of(()->chainStorage.pullObject(groupName, path1, jsonReaderDelegate)).getOrNull();
            Assert.assertNull("The object on group bucket-test, /path1 should be null", object1);

            final Object object2 = Try.of(()->chainStorage.pullObject(groupName, path2, jsonReaderDelegate)).getOrNull();
            Assert.assertNull("The object on group bucket-test, /path1 should be null", object2);

            final Chainable404StorageCache cache = ChainableStoragePersistenceAPI.class.cast(chainStorage).getCache();
            Assert.assertTrue("The object on group bucket-test, /path1 should be 404", cache.is404(groupName, path1));
            Assert.assertTrue("The object on group bucket-test, /path2 should be 404", cache.is404(groupName, path2));

            // Now create a few objects on the chainStorage
            final String[] paths = {"/path1.txt", "/path2.txt", "/path3.txt"};
            final String[] objects = {"Object1", "Object2", "Object3"};
            for (int i = 0; i < paths.length; i++) {
                chainStorage.pushObject(groupName, paths[i], writerDelegate, objects[i], null);
            }

            // after that the cache should be clean, since we have pushed objects to the stores that previously were 404 on the cache
            Assert.assertFalse("The object on group bucket-test, /path1 should be NOT 404", cache.is404(groupName, path1));
            Assert.assertFalse("The object on group bucket-test, /path2 should be NOT 404", cache.is404(groupName, path2));
        } finally {

            Stream.of(fileStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    }


    /**
     * Method to test: This test tries the {@link ChainableStoragePersistenceAPI#pullObject(String, String, ObjectReaderDelegate)}
     * Given Scenario: To start will set a chain storage including as a first layer the file storage, then memory storage
     * - Then we will push for some objects to mem storage and pull the same on chainable storage
     * ExpectedResult: since the objects already exist on a storage layer, the cache should be clean
     *
     * @throws Exception
     */
    @Test
    public void Test_Chainable_Storage_Cache_404_pulling_existing_objects() throws Exception {
        prepareIfNecessary();

        // Creates a chain storage with file, memory, and db in that order
        final JsonReaderDelegate jsonReaderDelegate = new JsonReaderDelegate(String.class);
        final ChainableStoragePersistenceAPIBuilder chainStorageBuilder = new ChainableStoragePersistenceAPIBuilder();

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI memStorage  = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.MEMORY);
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);

        // file -> mem -> db
        chainStorageBuilder.add(fileStorage);
        chainStorageBuilder.add(memStorage);

        StoragePersistenceProvider.INSTANCE.get().addStorageInitializer(StorageType.CHAIN2, chainStorageBuilder);
        final StoragePersistenceAPI chainStorage   = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.CHAIN2);

        // creates the group on all storages
        final String groupName = "bucket-test-404-2";

        try {

            // this creates all groups on all storages on the chain
            Try.run(() -> chainStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            final String path1 = "/path1.txt";
            final String path2 = "/path2.txt";

            // Now create a few objects on the chainStorage
            final String[] paths = {"/path1.txt", "/path2.txt", "/path3.txt"};
            final String[] objects = {"Object1", "Object2", "Object3"};
            for (int i = 0; i < paths.length; i++) {
                memStorage.pushObject(groupName, paths[i], null, objects[i], null);
            }

            final Object object1 = Try.of(()->chainStorage.pullObject(groupName, path1, jsonReaderDelegate)).getOrNull();
            Assert.assertNotNull("The object on group bucket-test, /path1 should be NOT null", object1);

            final Object object2 = Try.of(()->chainStorage.pullObject(groupName, path2, jsonReaderDelegate)).getOrNull();
            Assert.assertNotNull("The object on group bucket-test, /path1 should be NOT null", object2);

            // since the objects were resolved on the chain, the cache 404 should be clean
            final Chainable404StorageCache cache = ChainableStoragePersistenceAPI.class.cast(chainStorage).getCache();
            Assert.assertFalse("The object on group bucket-test, /path1 should be 404", cache.is404(groupName, path1));
            Assert.assertFalse("The object on group bucket-test, /path2 should be 404", cache.is404(groupName, path2));

        } finally {

            Stream.of(fileStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    }

    /**
     * Method to test: This test tries the {@link FileSystemStoragePersistenceAPIImpl#toIterable(String)}
     * Given Scenario: Initially will create a few objects in a bucket, after that will iterate over it
     * ExpectedResult: The iteration should return the files created successfully
     *
     * @throws Exception
     */
    @Test
    public void Test_File_Iterable() throws Exception {

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);

        // group for testing on the storage
        final String groupName = "bucket-test-iterable";

        try {

            // this creates the group on the storage
            Try.run(() -> fileStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));

            // Now create a few objects on the fileStorage
            final String[] paths = {"path1.txt", "path2.txt", "path3.txt"};
            final Serializable[] objects = { new HashMap<>(Map.of("key1","Object1")),
                    new HashMap<>(Map.of("key2","Object2")),
                    new HashMap<>(Map.of("key3","Object3"))};

            for (int i = 0; i < paths.length; i++) {
                fileStorage.pushObject(groupName, paths[i], FileStorageAPI.DEFAULT_OBJECT_WRITER_DELEGATE, objects[i], null);
            }

            final Map<String, Map> resultMap = new HashMap<>();
            for (final ObjectPath objectPath : fileStorage.toIterable(groupName)) {
                final String path = objectPath.getPath();
                final Object object = fileStorage.pullObject(groupName, path, FileStorageAPI.DEFAULT_OBJECT_READER_DELEGATE);
                Assert.assertNotNull("The object on group bucket-test, /path1 should be NOT null", object);
                resultMap.put(path, (Map)objectPath.getObject());
            }

            Assert.assertEquals("Should retrieve 3 elements", 3, resultMap.size());
            for (int i = 0; i < paths.length; i++) {
                Assert.assertTrue("Should retrieve 3 elements", resultMap.containsKey(paths[i]));
                Assert.assertEquals("Should retrieve 3 elements", objects[i], resultMap.get(paths[i]));
            }
        } finally {

            Stream.of(fileStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    } // Test_File_Iterable.

    /**
     * Method to test: This test tries the {@link com.dotmarketing.quartz.job.ReplicateStoragesJob#replicate(StorageType, List)}
     * Given Scenario: Initially will create a few objects in a bucket, and replicate it to a db storage
     * ExpectedResult: The bucket has to be replicated successfully to the db
     *
     * @throws Exception
     */
    @Test
    public void Test_File_Replication() throws Exception {

        // we need to clean any previous storage configuration to proceed on the test
        StoragePersistenceProvider.INSTANCE.get().forceInitialize();
        final StoragePersistenceAPI fileStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM);
        final StoragePersistenceAPI dbStorage = StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.DB);

        // group for testing on the storage
        final String groupName = "bucket-test-replication";

        try {

            // this creates the group on the storage
            Try.run(() -> fileStorage.createGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));

            // Now create a few objects on the fileStorage
            final String[] paths = {"path1.txt", "path2.txt", "path3.txt"};
            final Serializable[] objects = { new HashMap<>(Map.of("key1","Object1")),
                    new HashMap<>(Map.of("key2","Object2")),
                    new HashMap<>(Map.of("key3","Object3"))};

            for (int i = 0; i < paths.length; i++) {
                fileStorage.pushObject(groupName, paths[i], FileStorageAPI.DEFAULT_OBJECT_WRITER_DELEGATE, objects[i], null);
            }

            // fires replication from file system to db
            new ReplicateStoragesJob().replicate(StorageType.FILE_SYSTEM, List.of(StorageType.DB));
            for (int i = 0; i < paths.length; i++) {

                Assert.assertTrue("The object path: " + paths[i] + " should be exist", dbStorage.existsObject(groupName, paths[i]));
                final Object object = dbStorage.pullObject(groupName, paths[i], FileStorageAPI.DEFAULT_OBJECT_READER_DELEGATE);
                Assert.assertEquals("The object path: " + paths[i] + " should be equals", objects[i], object);
            }
        } finally {

            Stream.of(fileStorage, dbStorage).forEach(storage -> {
                Try.run(() -> storage.deleteGroup(groupName)).getOrElseThrow((e) -> new RuntimeException(e));
            });
        }
    } // Test_File_Iterable.
}
