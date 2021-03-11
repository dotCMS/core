package com.dotcms.storage;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Singleton that serves as entry point to the storage api
 */
public final class StoragePersistenceProvider {

    public static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    public static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";

    private final Map<StorageType, StoragePersistenceAPI> storagePersistenceInstances = new ConcurrentHashMap<>();
    
    private final Map<StorageType, Supplier<StoragePersistenceAPI>> initializers = ImmutableMap.of(
            StorageType.FILE_SYSTEM, () -> {
                final FileSystemStoragePersistenceAPIImpl fileSystemStorage = new FileSystemStoragePersistenceAPIImpl();
                final String metadataGroupName = Config
                        .getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
                final File assetsDir = new File(ConfigUtils.getAbsoluteAssetsRootPath());
                if (!assetsDir.exists()) {
                    assetsDir.mkdirs();
                }
                fileSystemStorage.addGroupMapping(metadataGroupName, assetsDir);
                return fileSystemStorage;
            },
            StorageType.DB, DataBaseStoragePersistenceAPIImpl::new
    );

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

        final StoragePersistenceAPI api = storagePersistenceInstances.putIfAbsent(storageType, initializers.get(storageType).get());
        if(null != api){
           return api;
        }
        return storagePersistenceInstances.get(storageType);
    }

    /**
     * default storage getter
     * @return
     */
    public StoragePersistenceAPI getStorage(){
        return getStorage(null);
    }

    /**
     *
     */
    public void forceInitialize(){
       storagePersistenceInstances.clear();
    }

    public enum INSTANCE {
        INSTANCE;
        private final StoragePersistenceProvider provider = new StoragePersistenceProvider();

        public static StoragePersistenceProvider get() {
            return INSTANCE.provider;
        }
    }

}
