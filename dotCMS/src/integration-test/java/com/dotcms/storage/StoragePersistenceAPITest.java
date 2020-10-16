package com.dotcms.storage;

import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.storage.StoragePersistenceProvider.INSTANCE;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.Encryptor;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class StoragePersistenceAPITest {

    private static final String textFilePath = "textFilePath";
    private static final String imageFilePath = "imageFilePath";

    private static final String TEST_IMAGE_JPG = TestFile.JPG.getFilePath();
    private static final String TEST_IMAGE_PNG = TestFile.PNG.getFilePath();
    private static final String TEST_TEXT_PATH = TestFile.TEXT.getFilePath();

    private static final int LARGE_FILE_SIZE = 1024 * 1024 * 4;

    private static final StoragePersistenceProvider persistenceProvider = INSTANCE.get();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given Scenario: We call the singleton method that retrieves the default storage
     * Expected Result: We expect the default storage to be an instance of FileSystemStoragePersistenceAPIImpl
     */
    @Test
    public void Test_Get_Default_Provider() {
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        try {
        //if there's a property already set we need to clean it so we can test that by default we will get FileSystem
            Config.setProperty(DEFAULT_STORAGE_TYPE, null);
            final StoragePersistenceProvider persistenceProvider = INSTANCE.get();
            assertTrue(persistenceProvider
                    .getStorage() instanceof FileSystemStoragePersistenceAPIImpl);
        }finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
        }
    }

    /**
     * Given Scenario:
     * Expected Result:
     */
    @Test
    public void Test_Get_Provider_By_StorageType() {
        final StoragePersistenceProvider persistenceProvider = INSTANCE.get();
        assertTrue(persistenceProvider.getStorage(
                StorageType.FILE_SYSTEM) instanceof FileSystemStoragePersistenceAPIImpl);
        assertTrue(persistenceProvider
                .getStorage(StorageType.DB) instanceof DataBaseStoragePersistenceAPIImpl);
    }

    /**
     * Given scenario: For the same group we send two files of different type.
     * Expected Result: The Group should be created then the file should be stored and then removed together with all of its elements.
     * Same group is always send to test delete is effective.
     */
    @Test
    @UseDataProvider("getMixedStorageTestCases")
    public void Test_Storage_Push_File_Then_Recover_Then_Remove_Group(final TestCase testCase)
            throws DotDataException {
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(testCase.storageType);
        final String groupName = testCase.groupName;
        final String path = testCase.path;

        try {
            assertFalse(storage.existsGroup(groupName));
            assertTrue(storage.createGroup(groupName));
            assertTrue(storage.listGroups().contains(groupName));

            final File pushFile = testCase.file;
            final Object resultObject = storage
                    .pushFile(groupName, path, pushFile, ImmutableMap.of());
            assertNotNull(resultObject);
            assertTrue(storage.existsGroup(groupName));
            final File pullFile = storage.pullFile(groupName, path);
            assertTrue(pullFile.exists());
            assertTrue(pullFile.canRead());
            assertTrue(pullFile.canWrite());
            assertNotSame(pushFile, pullFile);
            assertEquals(pullFile.length(), pushFile.length());
        } finally {
            final int count = storage.deleteGroup(groupName);
            assertEquals(1, count);
            assertFalse(storage.existsGroup(groupName));
            assertFalse(storage.existsObject(groupName, path));
        }

    }


    @DataProvider
    public static Object[] getMixedStorageTestCases() throws Exception {
        final String groupName = "fixed-group-name";
        final File tempFile = new File(
                ConfigTestHelper.getUrlToTestResource(TEST_TEXT_PATH).toURI());
        final File imageFile = new File(
                ConfigTestHelper.getUrlToTestResource(TEST_IMAGE_JPG).toURI());

        return new Object[]{

                new TestCase(StorageType.FILE_SYSTEM,
                        groupName, textFilePath, tempFile
                ),
                new TestCase(StorageType.FILE_SYSTEM,
                        groupName, imageFilePath, imageFile
                ),

                new TestCase(StorageType.DB,
                        groupName, textFilePath, tempFile
                ),
                new TestCase(StorageType.DB,
                        groupName, imageFilePath, imageFile
                )

        };
    }

    /**
     * Test Scenario: We simply do two consecutive calls to createGroup with the same arguments
     * Expected Results: Nothing should break. Second call should tell the group  already exist.
     * @param testCase
     */
    @Test
    @UseDataProvider("getGroupNameTestCases")
    public void Test_Attempt_Create_Dupe_Group(final TestCase testCase) throws DotDataException {
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(testCase.storageType);
        final String groupName = testCase.groupName;
        assertFalse(storage.existsGroup(groupName));
        assertTrue(storage.createGroup(groupName));
        assertFalse(storage.createGroup(groupName));
    }

    @DataProvider
    public static Object[] getGroupNameTestCases() {
        final String groupName = RandomStringUtils.randomAlphanumeric(10);

        return new Object[]{
                new TestCase(StorageType.FILE_SYSTEM, groupName),
                new TestCase(StorageType.DB, groupName)
        };
    }

    /**
     * Test Scenario: Regardless of group we attempt saving a binary
     * Expected Results: We're basically testing we no longer have an exception where the binary would be inserted as a new file generating an constrain violation
     * @param testCase
     */
    @Test
    @UseDataProvider("getDupeBinaryTestCases")
    public void Test_Attempt_Push_Same_Binary_Twice(final TestCase testCase)
            throws DotDataException {
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(testCase.storageType);
        storage.deleteObject(testCase.groupName, testCase.path);
        assertTrue(storage.createGroup(testCase.groupName));
        final Object object = storage
                .pushFile(testCase.groupName, testCase.path, testCase.file, ImmutableMap.of());
        assertNotNull(object);

        assertTrue(storage.existsObject(testCase.groupName, testCase.path));

        final Object object2 = storage
                .pushFile(testCase.groupName, testCase.path, testCase.file, ImmutableMap.of());
        assertNotNull(object2);
    }

    @DataProvider
    public static Object[] getDupeBinaryTestCases() throws Exception{
        final String groupName = RandomStringUtils.randomAlphanumeric(10);

        final File imageFile = new File(
                ConfigTestHelper.getUrlToTestResource(TEST_IMAGE_PNG).toURI());

        return new Object[]{

                new TestCase(StorageType.FILE_SYSTEM,
                        groupName, imageFilePath, imageFile
                ),

                new TestCase(StorageType.DB,
                        groupName, imageFilePath, imageFile
                )

        };
    }

    /**
     * Given scenario: We want corroborate that we're able to recover files and verify the existence of a group regardless of casing.
     * Expected Results: Regardless of casing we should be able to retrieve and find objects
     * @param testCase
     */
    @Test
    @UseDataProvider("getRandomTestCases")
    public void Test_Pull_File_Different_Casing(final TestCase testCase) throws DotDataException {
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(testCase.storageType);
        if(!storage.existsGroup(testCase.groupName)){
           assertTrue(storage.createGroup(testCase.groupName));
        }
        final Object object = storage
                .pushFile(testCase.groupName, testCase.path, testCase.file, ImmutableMap.of());
        assertNotNull(object);

        final String randomCaseGroupName = upperCaseRandom(testCase.groupName, testCase.groupName.length());
        assertTrue(storage.existsGroup(randomCaseGroupName));
        final String randomCasePath = upperCaseRandom(testCase.path, testCase.path.length());
        final File file = storage.pullFile(randomCaseGroupName, randomCasePath);
        assertNotNull(file);

    }

    @DataProvider
    public static Object[] getRandomTestCases() throws Exception{
        final String path = "any-path";
        final String groupName = RandomStringUtils.randomAlphanumeric(10);
        final File temp = File.createTempFile("tempfile", ".txt");
        return new Object[]{
                new TestCase(StorageType.FILE_SYSTEM, groupName, path, temp),
                new TestCase(StorageType.DB, groupName, path, temp)
        };
    }

    /**
     * Given scenario:
     * Expected Results:
     * @param testCase
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @UseDataProvider("getLargeFileTestCases")
    public void Test_Push_Large_File(final TestCase testCase)
            throws IOException, NoSuchAlgorithmException, DotDataException {

        final StoragePersistenceAPI storage = persistenceProvider.getStorage(testCase.storageType);
        if (!storage.existsGroup(testCase.groupName)) {
            assertTrue(storage.createGroup(testCase.groupName));
        }
        storage.pushFile(testCase.groupName, testCase.path, testCase.file, ImmutableMap.of());

        final File pullFile = storage.pullFile(testCase.groupName, testCase.path);
        assertEquals(testCase.file.length(), pullFile.length());

        assertEquals(testCase.file.length(), LARGE_FILE_SIZE);

        assertEquals(Encryptor.Hashing.sha256().append(Files.readAllBytes(testCase.file.toPath())).buildUnixHash(),
                     Encryptor.Hashing.sha256().append(Files.readAllBytes(pullFile.toPath())).buildUnixHash());

    }

    private static final Random random = new Random();

    @DataProvider
    public static Object[] getLargeFileTestCases() throws Exception{
        final String path = "fixed-large-file-path";
        final String groupName = RandomStringUtils.randomAlphanumeric(10);

        final File temp = File.createTempFile("largeFile", ".bin");

        try (FileOutputStream output = new FileOutputStream(temp, true)) {
            final byte[] data = new byte[LARGE_FILE_SIZE];
            random.nextBytes(data);
            output.write(data);
        }

        return new Object[]{
                new TestCase(StorageType.FILE_SYSTEM, groupName, path, temp),
                new TestCase(StorageType.DB, groupName, path, temp)
        };
    }


    /**
     * Test Data DTO
     */
    public static class TestCase {

        final StorageType storageType;
        final File file;
        final String groupName;
        final String path;

        /**
         * full test-case
         * @param storageType
         * @param groupName
         * @param path
         * @param file
         */
        public TestCase(final StorageType storageType, final String groupName, final String path,
                final File file) {
            this.storageType = storageType;
            this.file = file;
            this.groupName = groupName;
            this.path = path;
        }

        /**
         * partial test-case
         * @param storageType
         * @param groupName
         */
        public TestCase(StorageType storageType, String groupName) {
            this(storageType, groupName, null, null);
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "storageType=" + storageType +
                    ", groupName='" + groupName + '\'' +
                    ", path='" + path + '\'' +
                    ", file=" + (file != null ? file.getName() : "n/a" )   +
                    '}';
        }
    }

}
