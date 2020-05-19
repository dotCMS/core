package com.dotcms.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageProvider {

    private final Map<String, Storage> fileSystemStorageMap = new ConcurrentHashMap<>();

    public FileSystemStorage getFileSystemStorage() {

        return (FileSystemStorage) this.fileSystemStorageMap.get(StorageType.FILE_SYSTEM.name());
    }

    public DataBaseStorage getDbStorage() {

        return (DataBaseStorage) this.fileSystemStorageMap.get(StorageType.DB.name());
    }

    public Storage getStorage (final StorageType storageType) {

        return this.getStorage(storageType.name());
    }

    public Storage getStorage (final String storageType) {

        return this.fileSystemStorageMap.get(storageType);
    }

    public void addStorage (final String storageType, final Storage storage) {

        this.fileSystemStorageMap.put(storageType, storage);
    }

    public void addStorage (final StorageType storageType, final Storage storage) {

        this.addStorage(storageType.name(), storage);
    }
}
