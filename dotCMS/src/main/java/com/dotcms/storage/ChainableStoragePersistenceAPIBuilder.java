package com.dotcms.storage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Builder used to create an instance of the {@link ChainableStoragePersistenceAPI} class.
 */
public class ChainableStoragePersistenceAPIBuilder implements Supplier<StoragePersistenceAPI> {

    private final List<StoragePersistenceAPI> storagePersistenceAPIs =  new CopyOnWriteArrayList<>();

    /**
     * Adds a storages to the builder (the order is important)
     * @param storagePersistenceAPI
     * @return CompositeStoragePersistenceAPIBuilder
     */
    public ChainableStoragePersistenceAPIBuilder add(final StoragePersistenceAPI storagePersistenceAPI) {
        storagePersistenceAPIs.add(storagePersistenceAPI);
        return this;
    }

    /**
     * Adds a storage to the builder at the begining of the chain (the order is important)
     * @param storagePersistenceAPI
     * @return CompositeStoragePersistenceAPIBuilder
     */
    public ChainableStoragePersistenceAPIBuilder addFirst(final StoragePersistenceAPI storagePersistenceAPI) {
        storagePersistenceAPIs.add(0, storagePersistenceAPI);
        return this;
    }

    /**
     * Returns the list of Storage Persistence APIs that have been added to the builder so far.
     *
     * @return The list of the current {@link StoragePersistenceAPI} objects.
     */
    public List<StoragePersistenceAPI> list() {
        return this.storagePersistenceAPIs;
    }

    /**
     * Builds a new {@link ChainableStoragePersistenceAPI}
     * @return CompositeStoragePersistenceAPI
     */
    @Override
    public StoragePersistenceAPI get() {
        return new ChainableStoragePersistenceAPI(storagePersistenceAPIs);
    }

}
