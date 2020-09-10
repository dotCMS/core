package com.dotcms.storage;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.dotcms.repackage.org.nfunk.jep.function.Str;
import com.dotcms.storage.StoragePersistenceProvider.INSTANCE;
import com.dotcms.util.IntegrationTestInitService;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class StoragePersistenceAPITest {

    private static final String TEMP_FILE = "temp";
    private static final String TXT = "txt";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Get_Default_Provider(){
        final StoragePersistenceProvider persistenceProvider = INSTANCE.get();
       assertTrue (persistenceProvider.getStorage() instanceof FileSystemStoragePersistenceAPIImpl);
    }

    @Test
    public void Test_Get_Provider_By_StorageType(){
        final StoragePersistenceProvider persistenceProvider = INSTANCE.get();
        assertTrue (persistenceProvider.getStorage(StorageType.FILE_SYSTEM) instanceof FileSystemStoragePersistenceAPIImpl);
        assertTrue (persistenceProvider.getStorage(StorageType.DB) instanceof DataBaseStoragePersistenceAPIImpl);
    }


    @Test
    public void Test_DBStorage_Push_File_Then_Recover() throws IOException {
        final String fileName1 = TEMP_FILE + System.currentTimeMillis();
        final File pushFile = File.createTempFile(fileName1, TXT);
        final StoragePersistenceProvider persistenceProvider = INSTANCE.get();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(StorageType.DB);
        final List<Object> objects = storage.listGroups();
        System.out.println(objects);
        final String groupName = RandomStringUtils.randomAlphanumeric(10);
        assertFalse(storage.existsGroup(groupName));
        assertTrue(storage.createGroup(groupName));
        final String path = "path-to-my-file";
        final Object resultObject = storage.pushFile(groupName, path, pushFile, ImmutableMap.of());
        assertNotNull(resultObject);
        assertTrue(storage.existsGroup(groupName));
        final File pullFile = storage.pullFile(groupName, path);
        assertTrue(pullFile.exists());
        assertTrue(pullFile.canRead());
        assertTrue(pullFile.canWrite());
        assertNotSame(pushFile,pullFile);
        assertEquals(pullFile.length(),pushFile.length());
        final int count = storage.deleteGroup(groupName);
        assertEquals(1, count);
        assertFalse(storage.existsGroup(groupName));
        assertFalse(storage.existsObject(groupName,path));


    }

    public static class TestCase {
        private StorageType storageType;
        private File file;

    }

    @DataProvider
    public static Object[] getTestCases() throws Exception {
        return new Object[]{};
        }

}
