package com.dotcms.storage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Builder to create a {@link CompositeStoragePersistenceAPI}
 */
public class CompositeStoragePersistenceAPIBuilder implements Supplier<StoragePersistenceAPI> {

    private final List<StoragePersistenceAPI> storagePersistenceAPIs =  new CopyOnWriteArrayList<>();

    /**
     * Adds a storages to the builder (the order is important)
     * @param storagePersistenceAPI
     * @return CompositeStoragePersistenceAPIBuilder
     */
    public CompositeStoragePersistenceAPIBuilder add(final StoragePersistenceAPI storagePersistenceAPI) {
        storagePersistenceAPIs.add(storagePersistenceAPI);
        return this;
    }

    public CompositeStoragePersistenceAPIBuilder addFirst(final StoragePersistenceAPI storagePersistenceAPI) {
        storagePersistenceAPIs.add(0, storagePersistenceAPI);
        return this;
    }

    /**
     * Creates a new builder based on an existing one
     * @param builder CompositeStoragePersistenceAPIBuilder
     * @return CompositeStoragePersistenceAPIBuilder
     */
    public static CompositeStoragePersistenceAPIBuilder copyFrom(final CompositeStoragePersistenceAPIBuilder builder) {
        final CompositeStoragePersistenceAPIBuilder newBuilder = new CompositeStoragePersistenceAPIBuilder();
        newBuilder.storagePersistenceAPIs.addAll(builder.storagePersistenceAPIs);
        return newBuilder;
    }

    /**
     * Builds a new {@link CompositeStoragePersistenceAPI}
     * @return CompositeStoragePersistenceAPI
     */
    @Override
    public StoragePersistenceAPI get() {
        return new CompositeStoragePersistenceAPI(storagePersistenceAPIs);
    }
}
