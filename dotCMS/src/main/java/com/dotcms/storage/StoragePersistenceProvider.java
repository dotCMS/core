package com.dotcms.storage;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that serves as entry point to the storage api
 */
public final class StoragePersistenceProvider {

    static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";

    private final Map<StorageType, StoragePersistenceAPI> storageMap = new ConcurrentHashMap<>();

    /**
     * default constructor
     */
    private StoragePersistenceProvider() {
    }

    /**
     * default storage type
     * @return
     */
    public static StorageType getStorageType(){
        final String storageType = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        return StorageType.valueOf(storageType);
    }

    /**
     * file system storage type
     * @return
     */
    public FileSystemStoragePersistenceAPIImpl getFileSystemStorage() {

        return (FileSystemStoragePersistenceAPIImpl) this.storageMap.get(StorageType.FILE_SYSTEM);
    }

    /**
     * db storage type
     * @return
     */
    public DataBaseStoragePersistenceAPIImpl getDbStorage() {

        return (DataBaseStoragePersistenceAPIImpl) this.storageMap.get(StorageType.DB);
    }

    /**
     * param based storage type getter
     * @param storageType
     * @return
     */
    public StoragePersistenceAPI getStorage (StorageType storageType) {
        if(null == storageType){
            storageType = getStorageType();
        }
        final StorageType finalStorageType = storageType;
        Logger.info(this, ()-> "Retrieving from storage: " + finalStorageType);
        return this.storageMap.get(storageType);
    }

    /**
     * default storage getter
     * @return
     */
    public StoragePersistenceAPI getStorage(){
        return getStorage(null);
    }

    /**
     * DB Storage initializer
     * @param storageMap
     */
    private static void initDbStorage(final Map<StorageType, StoragePersistenceAPI> storageMap) {
        final DataBaseStoragePersistenceAPIImpl dataBaseStorage = new DataBaseStoragePersistenceAPIImpl();
        storageMap.put(StorageType.DB, dataBaseStorage);
    }

    /**
     * File System Storage initializer
     * @param storageMap
     */
    private static void initFileSystemStorage(final Map<StorageType, StoragePersistenceAPI> storageMap) {
        final FileSystemStoragePersistenceAPIImpl fileSystemStorage = new FileSystemStoragePersistenceAPIImpl();
        final String metadataGroupName = Config.getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
        fileSystemStorage.addGroupMapping(metadataGroupName, new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()));
        storageMap.put(StorageType.FILE_SYSTEM, fileSystemStorage);
    }

    public enum INSTANCE {
        INSTANCE;
        private final StoragePersistenceProvider provider = initProviders();

        /**
         * singleton instance getter
         * @return
         */
        public static StoragePersistenceProvider get() {
            return INSTANCE.provider;
        }

        /**
         * providers init method
         * @return
         */
        private static StoragePersistenceProvider initProviders() {
            final StoragePersistenceProvider storagePersistenceProvider = new StoragePersistenceProvider();
            initDbStorage(storagePersistenceProvider.storageMap);
            initFileSystemStorage(storagePersistenceProvider.storageMap);
            return storagePersistenceProvider;
        }
    }
}
