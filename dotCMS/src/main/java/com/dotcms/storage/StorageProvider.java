package com.dotcms.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageProvider {

    private final Map<String, Storage> storageMap = new ConcurrentHashMap<>();

    public FileSystemStorage getFileSystemStorage() {

        return (FileSystemStorage) this.storageMap.get(StorageType.FILE_SYSTEM.name());
    }

    public DataBaseStorage getDbStorage() {

        return (DataBaseStorage) this.storageMap.get(StorageType.DB.name());
    }

    public Storage getStorage (final StorageType storageType) {

        return this.getStorage(storageType.name());
    }

    public Storage getStorage (final String storageType) {

        return this.storageMap.get(storageType);
    }

    public void addStorage (final String storageType, final Storage storage) {

        this.storageMap.put(storageType, storage);
    }

    public void addStorage (final StorageType storageType, final Storage storage) {

        this.addStorage(storageType.name(), storage);
    }
}
