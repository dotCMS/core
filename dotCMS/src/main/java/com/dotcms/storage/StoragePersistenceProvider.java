package com.dotcms.storage;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StoragePersistenceProvider {

    static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";

    private final Map<StorageType, StoragePersistenceAPI> storageMap = new ConcurrentHashMap<>();

    private StoragePersistenceProvider() {
    }

    public static StorageType getStorageType(){
        final String storageType = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        return StorageType.valueOf(storageType);
    }

    public FileSystemStoragePersistenceAPIImpl getFileSystemStorage() {

        return (FileSystemStoragePersistenceAPIImpl) this.storageMap.get(StorageType.FILE_SYSTEM);
    }

    public DataBaseStoragePersistenceAPIImpl getDbStorage() {

        return (DataBaseStoragePersistenceAPIImpl) this.storageMap.get(StorageType.DB);
    }

    public StoragePersistenceAPI getStorage (StorageType storageType) {
        if(null == storageType){
            storageType = getStorageType();
        }
        final StorageType finalStorageType = storageType;
        Logger.info(this, ()-> "Retrieving from storage: " + finalStorageType);
        return this.storageMap.get(storageType);
    }

    public StoragePersistenceAPI getStorage(){
        return getStorage(null);
    }

    private static void initDbStorage(final Map<StorageType, StoragePersistenceAPI> storageMap) {
        final DataBaseStoragePersistenceAPIImpl dataBaseStorage = new DataBaseStoragePersistenceAPIImpl();
        storageMap.put(StorageType.DB, dataBaseStorage);
    }

    private static void initFileSystemStorage(final Map<StorageType, StoragePersistenceAPI> storageMap) {
        final FileSystemStoragePersistenceAPIImpl fileSystemStorage = new FileSystemStoragePersistenceAPIImpl();
        final String metadataGroupName = Config.getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
        fileSystemStorage.addGroupMapping(metadataGroupName, new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()));
        storageMap.put(StorageType.FILE_SYSTEM, fileSystemStorage);
    }

    public enum INSTANCE {
        INSTANCE;
        private final StoragePersistenceProvider provider = initProviders();

        public static StoragePersistenceProvider get() {
            return INSTANCE.provider;
        }

        private static StoragePersistenceProvider initProviders() {
            final StoragePersistenceProvider storagePersistenceProvider = new StoragePersistenceProvider();
            initDbStorage(storagePersistenceProvider.storageMap);
            initFileSystemStorage(storagePersistenceProvider.storageMap);
            return storagePersistenceProvider;
        }
    }
}
