package com.dotcms.storage;

import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;

import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;
import io.vavr.control.Try;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Default implementation
 *
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    private static final String CACHE_GROUP = "Contentlet";

    // width,height,contentType,author,keywords,fileSize,content,length,title

    private volatile ObjectReaderDelegate objectReaderDelegate;
    private volatile ObjectWriterDelegate objectWriterDelegate;
    private volatile MetadataGenerator metadataGenerator;
    private final StoragePersistenceProvider persistenceProvider;

    /**
     * Testing constructor
     * @param objectReaderDelegate
     * @param objectWriterDelegate
     * @param metadataGenerator
     * @param persistenceProvider
     */
    @VisibleForTesting
    FileStorageAPIImpl(final ObjectReaderDelegate objectReaderDelegate,
            final ObjectWriterDelegate objectWriterDelegate,
            final MetadataGenerator metadataGenerator,
            final StoragePersistenceProvider persistenceProvider) {
        this.objectReaderDelegate = objectReaderDelegate;
        this.objectWriterDelegate = objectWriterDelegate;
        this.metadataGenerator = metadataGenerator;
        this.persistenceProvider = persistenceProvider;
    }

    /**
     * Default constructor
     */
    public FileStorageAPIImpl() {
        this(new JsonReaderDelegate<>(Map.class), new JsonWriterDelegate(),
                new TikaMetadataGenerator(), StoragePersistenceProvider.INSTANCE.get());
    }

    @Override
    public Map<String, Object> generateRawBasicMetaData(final File binary) {

        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    @Override
    public Map<String, Object> generateRawFullMetaData(final File binary, long maxLength) {

        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    @Override
    public Map<String, Object> generateBasicMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter) {

        final ImmutableSortedMap.Builder<String, Object> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (this.validBinary(binary)) {

            //if (metaDataKeyFilter.test(TITLE_META_KEY)) {
                mapBuilder.put(TITLE_META_KEY, binary.getName());
            //}

            //if (metaDataKeyFilter.test(PATH_META_KEY)) {

                final Optional<String> optional = FileUtil.getRealAssetsPathRelativePiece(binary);
                if (optional.isPresent()) {
                    mapBuilder.put(PATH_META_KEY, optional.get());
                } else {
                    mapBuilder.put(PATH_META_KEY, binary.getAbsolutePath());
                }
            //}

            if (metaDataKeyFilter.test(LENGTH_META_KEY)) {
                mapBuilder.put(LENGTH_META_KEY, binary.length());
            }

            if (metaDataKeyFilter.test(CONTENT_TYPE_META_KEY)) {
                mapBuilder.put(CONTENT_TYPE_META_KEY, MimeTypeUtils.getMimeType(binary));
            }

            mapBuilder.put(MOD_DATE_META_KEY, System.currentTimeMillis());
            mapBuilder.put(SHA226_META_KEY,
                    Try.of(() -> FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));
        }

        return mapBuilder.build();
    }


    @Override
    public Map<String, Object> generateFullMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter,
            final long maxLength) {

        final TreeMap<String, Object> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {

            metadataMap.putAll(this.generateBasicMetaData(binary, metaDataKeyFilter));
            final Map<String, Object> fullMetaDataMap = this.metadataGenerator
                    .generate(binary, maxLength);
            if (UtilMethods.isSet(fullMetaDataMap)) {
                for (final Map.Entry<String, Object> entry : fullMetaDataMap.entrySet()) {

                    if (metaDataKeyFilter.test(entry.getKey())) {

                        metadataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            return Collections.emptyMap();
        }

        return new ImmutableSortedMap.Builder<String, Object>(Comparator.naturalOrder())
                .putAll(metadataMap).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> generateMetaData(final File binary,
            final GenerateMetadataConfig configuration) {

        final Map<String, Object> metadataMap;
        final StorageKey storageKey = configuration.getStorageKey();
        final StorageType storageType = this.getStorageType(storageKey);
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageType);

        this.checkBucket(storageKey, storage);  //if the group/bucket doesn't exist create it.
        this.checkOverride(storage, configuration); //if config states we need to remove and force regen
        //if the entry isn't already there skip and simply store in cache.
        if (!storage.existsObject(storageKey.getGroup(), storageKey.getKey())) {
            if (this.validBinary(binary)) {
                Logger.warn(FileStorageAPIImpl.class, String.format(
                        "Object identified by `/%s/%s` didn't exist in storage %s will be generated.",
                        storageKey.getGroup(), storageKey,
                        configuration.isFull() ? "full-metadata" : "basic-metadata"));
                final long maxLength = configuration.getMaxLength();
                metadataMap = configuration.isFull() ?
                        this.generateFullMetaData(binary,
                                configuration.getMetaDataKeyFilter(), maxLength) :
                        this.generateBasicMetaData(binary,
                                configuration.getMetaDataKeyFilter());

                if (configuration.isStore()) {
                    this.storeMetadata(storageKey, storage, metadataMap, binary);
                }

            } else {
               throw new IllegalArgumentException(String.format("the binary `%s` isn't accessible ", binary != null ? binary.getName() : "unknown"));
            }
        } else {
            metadataMap = this.retrieveMetadata(storageKey, storage);
        }

        if (configuration.isCache()) {

            this.putIntoCache(configuration.getCacheKeySupplier().get(),
                    metadataMap);
        }

        return metadataMap;
    }

    private StorageType getStorageType(final StorageKey storageKey) {
        final String val = UtilMethods.isSet(storageKey.getStorage()) ? storageKey.getStorage()
                : Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        return StorageType.valueOf(val);
    }

    private void checkBucket(final StorageKey storageKey, final StoragePersistenceAPI storage) {

        if (!storage.existsGroup(storageKey.getGroup())) {

            storage.createGroup(storageKey.getGroup());
        }
    }

    private void putIntoCache(final String cacheKey, final Map<String, Object> metadataMap) {

        final DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
        if (null != cacheAdmin) {

            cacheAdmin.put(cacheKey,
                    metadataMap, CACHE_GROUP);
        }
    }

    private Map<String, Object> retrieveMetadata(final StorageKey storageKey,
            final StoragePersistenceAPI storage) {

        Map<String, Object> objectMap = Collections.emptyMap();

        try {
            objectMap = (Map<String, Object>) storage.pullObject(storageKey.getGroup(), storageKey.getKey(), this.objectReaderDelegate);
            Logger.info(this, "Metadata read from: " + storageKey.getKey());
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }

        return objectMap;
    }

    private void storeMetadata(final StorageKey storageKey, final StoragePersistenceAPI storage,
            final Map<String, Object> metadataMap, final File binary) {

        try {
            //Commenting this rawBasic Metadata that basically brings everythig
            //final Map<String, Object> extraMeta = generateRawBasicMetaData(binary);
            //printInfo(metadataMap, extraMeta);
            storage.pushObject(storageKey.getGroup(), storageKey.getKey(),
                    this.objectWriterDelegate, (Serializable) metadataMap, metadataMap);
            Logger.info(this, "Metadata wrote on: " + storageKey.getKey());
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
     * debug utility
     * @param meta
     * @param extraMeta
     */
    private void printInfo( final Map<String, Object> meta, final Map<String, Object> extraMeta ){
        //if (!Logger.isDebugEnabled(FileStorageAPIImpl.class)){ return;  }
        Logger.warn(FileStorageAPIImpl.class," basic meta:  ");
        for (Entry<String, Object> entry : meta.entrySet()) {
            Logger.info(FileStorageAPIImpl.class,entry.getKey() + " " + entry.getValue());
        }
        Logger.warn(FileStorageAPIImpl.class," extra meta:  ");
        for (Entry<String, Object> entry : extraMeta.entrySet()) {
            Logger.info(FileStorageAPIImpl.class,entry.getKey() + " " + entry.getValue());
        }
    }

    /**
     * Check's file is readable and valid
     * @param binary
     * @return
     */
    private boolean validBinary(final File binary) {

        return null != binary && binary.exists() && binary.canRead();
    }

    /**
     * if the given configuration states we must override the previously existing file will be deleted before re-generating.
     * @param storage
     * @param generateMetaDataConfiguration
     */
    private void checkOverride(final StoragePersistenceAPI storage,
            final GenerateMetadataConfig generateMetaDataConfiguration) {

        final StorageKey storageKey = generateMetaDataConfiguration.getStorageKey();
        if (generateMetaDataConfiguration.isOverride() && storage
                .existsObject(storageKey.getGroup(), storageKey.getKey())) {

            try {

                storage.deleteObject(storageKey.getGroup(), storageKey.getKey());
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                storageKey.getKey(), e.getMessage()), e);
            }
        }
    } // checkOverride.

    @Override
    public Map<String, Object> retrieveMetaData(final RequestMetadata requestMetaData) {

        Map<String, Object> metadataMap = Collections.emptyMap();

        if (requestMetaData.isCache()) {

            final DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (null != cacheAdmin) {

                metadataMap = Try.of(() -> (Map<String, Object>) cacheAdmin
                        .get(requestMetaData.getCacheKeySupplier().get(),
                                CACHE_GROUP)).getOrElse(Collections.emptyMap());
            }
        }

        if (!UtilMethods.isSet(metadataMap)) {

            final StorageKey storageKey = requestMetaData.getStorageKey();
            final StorageType storageType = this.getStorageType(storageKey);
            final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageType);

            this.checkBucket(storageKey, storage);
            if (storage.existsObject(storageKey.getGroup(), storageKey.getKey())) {

                metadataMap = this.retrieveMetadata(storageKey, storage);
                Logger.info(this,
                        "Retrieve the meta data from storage, path: " + storageKey.getKey());
                if (null != requestMetaData.getCacheKeySupplier()) {
                    this.putIntoCache(requestMetaData.getCacheKeySupplier().get(),
                            requestMetaData.getWrapMetadataMapForCache().apply(metadataMap));
                }
            }
        } else {

            Logger.info(this, "Retrieve the meta data from cache, key: " + requestMetaData
                    .getCacheKeySupplier().get());
        }

        return metadataMap;
    }

}
