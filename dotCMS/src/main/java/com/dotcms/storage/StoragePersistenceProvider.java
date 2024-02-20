package com.dotcms.storage;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Singleton that serves as entry point to the Storage API. This class initializes and provides all
 * the different Storage Providers and the different provider chains that can be used in a dotCMS
 * instance.
 * <p>It's very important to note that the {@link RedisStoragePersistenceAPI} and the
 * {@link AmazonS3StoragePersistenceAPIImpl} providers are only available with an Enterprise
 * License.</p>
 */
public final class StoragePersistenceProvider {

    public static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    public static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";
    public static final String DEFAULT_CHAIN_PROVIDERS = "storage.file-metadata.default-chain";
    public static final String CHAIN1_PROVIDERS = "storage.file-metadata.chain1";
    public static final String CHAIN2_PROVIDERS = "storage.file-metadata.chain2";
    public static final String CHAIN3_PROVIDERS = "storage.file-metadata.chain3";

    final static Lazy<StorageType> storageType = Lazy.of(()->{
        String storageType= Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.DEFAULT_CHAIN.name());
        return Try.of(()->StorageType.valueOf(storageType)).getOrElse(StorageType.DEFAULT_CHAIN);
    });

    private final Map<StorageType, StoragePersistenceAPI> storagePersistenceInstances = new ConcurrentHashMap<>();
    
    private final Map<StorageType, Supplier<StoragePersistenceAPI>> initializers = new ConcurrentHashMap<>(Map.of(
            StorageType.FILE_SYSTEM, () -> {
                final FileSystemStoragePersistenceAPIImpl fileSystemStorage = new FileSystemStoragePersistenceAPIImpl();
                final String metadataGroupName = Config
                        .getStringProperty(METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA);
                final File assetsDir = new File(ConfigUtils.getAssetPath());
                if (!assetsDir.exists()) {
                    final boolean directoryCreated = assetsDir.mkdirs();
                    if (!directoryCreated) {
                        Logger.error(this, String.format("Assets directory '%s' could not be created", assetsDir));
                    }
                }
                fileSystemStorage.addGroupMapping(metadataGroupName, assetsDir);
                return fileSystemStorage;
            },
            StorageType.DB, DataBaseStoragePersistenceAPIImpl::new,
            StorageType.MEMORY, RedisStoragePersistenceAPI::new,
            StorageType.S3, AmazonS3StoragePersistenceAPIImpl::new
    ));

    /**
     * default constructor
     */
    private StoragePersistenceProvider() {
        initializeStorageChain();
    }

    /**
     * Initializes the Storage Chain used by the current dotCMS instance. It can be composed of one
     * or more Storage Providers and, for flexibility purposes, System Administrators can use the
     * following properties to set it up:
     * <ul>
     *     <li>{@link #DEFAULT_CHAIN_PROVIDERS}: Contains the default Storage Providers. You can
     *     update this property with the providers you want. If not specified, the File System
     *     provider will be used by default.</li>
     *     <li>{@link #CHAIN1_PROVIDERS}, {@link #CHAIN2_PROVIDERS}, and
     *     {@link #CHAIN3_PROVIDERS}: These are simply three Storage Provider chains that can be
     *     personalized by customers in case different combinations need to be available.</li>
     *     <li>{@link #DEFAULT_STORAGE_TYPE}: Allows you to specify what chain is going to be active
     *     for the current dotCMS instance: The {@link #DEFAULT_CHAIN_PROVIDERS} chain, or any of
     *     the previously specified three Chains.</li>
     * </ul>
     * It's worth noting that, in case the configuration property for the selected storage chain is
     * not present, the File System Provider will be the default value as it has always been the
     * original behavior.
     */
    private void initializeStorageChain() {
        final StorageType storageType = getStorageType();
        final String[] defaultProvider = new String[]{StorageType.FILE_SYSTEM.name()};
        switch (storageType) {
            case DEFAULT_CHAIN:
                initializeStorageChain(DEFAULT_CHAIN_PROVIDERS, defaultProvider);
                break;
            case CHAIN1:
                initializeStorageChain(CHAIN1_PROVIDERS, defaultProvider);
                break;
            case CHAIN2:
                initializeStorageChain(CHAIN2_PROVIDERS, defaultProvider);
                break;
            case CHAIN3:
                initializeStorageChain(CHAIN3_PROVIDERS, defaultProvider);
                break;
            default:
                throw new IllegalArgumentException(String.format("Storage Type '%s' is not supported", storageType));
        }
    }

    /**
     * Adds the specified chain of Storage Providers to the initializers map.
     *
     * @param chainName           The configuration property containing the chain of Storage
     *                            Providers.
     * @param defaultStorageTypes The default chain of Storage Providers in case the specified
     *                            property has not been set.
     */
    private void initializeStorageChain(final String chainName, final String[] defaultStorageTypes) {
        final String[] storageTypes = Config.getStringArrayProperty(chainName, defaultStorageTypes);
        final ChainableStoragePersistenceAPIBuilder builder = new ChainableStoragePersistenceAPIBuilder();
        Arrays.stream(storageTypes).iterator().forEachRemaining(storageTypeName -> {
            final StorageType storageType = StorageType.valueOf(storageTypeName);
                builder.add(this.getStorage(storageType));
        });
        if (builder.list().isEmpty()) {
            builder.add(this.getStorage(StorageType.FILE_SYSTEM));
        }
        Logger.info(this, String.format("Initializing Metadata Storage Chain with: '%s'", builder.list()));
        addStorageInitializer(StorageType.DEFAULT_CHAIN, builder);
    }

    /**
     * Adds the specified Storage Type to the initializers map. Each Storage Type provides either a
     * concrete {@link StoragePersistenceAPI} or a {@link ChainableStoragePersistenceAPI} which
     * contains one or more persistence APIs.
     *
     * @param storageType The {@link StorageType} that is being initialized.
     * @param initializer The {@link Supplier} that provides the {@link StoragePersistenceAPI}.
     */
    public void addStorageInitializer(final StorageType storageType, final Supplier<StoragePersistenceAPI> initializer){
        initializers.put(storageType, initializer);
    }
    /**
     * Returns the default Storage Type set in the current dotCMS instance.
     *
     * @return The specified {@link StorageType}.
     */
    public static StorageType getStorageType(){
        return storageType.get();
    }

    /**
     * Returns the appropriate Storage API based on the specified type.
     *
     * @param storageType The {@link StorageType} containing the expected API instance.
     * @return The {@link StoragePersistenceAPI} instance for the specified type.
     */
    public StoragePersistenceAPI getStorage (StorageType storageType) {
        if(null == storageType){
            storageType = getStorageType();
        }
        final StorageType finalStorageType = storageType;
        Logger.debug(this, ()-> "Retrieving from storage: " + finalStorageType);
        if (!initializers.containsKey(storageType)) {
            final String errorMsg = String.format("Storage type '%s' is not part of the initializers map", storageType);
            Logger.error(this, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        final StoragePersistenceAPI api = storagePersistenceInstances.putIfAbsent(storageType, initializers.get(storageType).get());
        if(null != api){
           return api;
        }
        return storagePersistenceInstances.get(storageType);
    }

    /**
     * Returns the Storage API for the default Storage Type.
     *
     * @return The {@link StoragePersistenceAPI} instance.
     */
    public StoragePersistenceAPI getStorage(){
        return getStorage(null);
    }

    /**
     * Flushes the map containing the Storage API instances that are groped by Storage Type.
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
