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

            if (metaDataKeyFilter.test(TITLE_META_KEY)) {
                mapBuilder.put(TITLE_META_KEY, binary.getName());
            }

            if (metaDataKeyFilter.test(PATH_META_KEY)) {
                mapBuilder.put(PATH_META_KEY, binary.getAbsolutePath());
            }

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

    @Override
    public Map<String, Object> generateMetaData(final File binary,
            final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

        Map<String, Object> metadataMap = Collections.emptyMap();
        final StorageKey storageKey = generateMetaDataConfiguration.getStorageKey();
        final StorageType storageType = this.getStorageType(storageKey);
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageType);

        this.checkBucket(storageKey, storage);  // todo: see if you want to remove this
        this.checkOverride(storage, generateMetaDataConfiguration);

        if (!storage.existsObject(storageKey.getGroup(), storageKey.getKey())) {

            if (this.validBinary(binary)) {

                final long maxLength = generateMetaDataConfiguration.getMaxLength();
                metadataMap = generateMetaDataConfiguration.isFull() ?
                        this.generateFullMetaData(binary,
                                generateMetaDataConfiguration.getMetaDataKeyFilter(), maxLength) :
                        this.generateBasicMetaData(binary,
                                generateMetaDataConfiguration.getMetaDataKeyFilter());

                if (generateMetaDataConfiguration.isStore()) {

                    this.storeMetadata(storageKey, storage, metadataMap, binary);
                }
            }
        } else {

            metadataMap = this.retrieveMetadata(storageKey, storage);
        }

        if (generateMetaDataConfiguration.isCache()) {

            this.putIntoCache(generateMetaDataConfiguration.getCacheKeySupplier().get(),
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

            storage.pushObject(storageKey.getGroup(), storageKey.getKey(),
                    this.objectWriterDelegate, (Serializable) metadataMap,
                    this.generateRawBasicMetaData(binary));
            Logger.info(this, "Metadata wrote on: " + storageKey.getKey());
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private boolean validBinary(final File binary) {

        return null != binary && binary.exists() && binary.canRead();
    }

    private void checkOverride(final StoragePersistenceAPI storage,
            final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

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
    public Map<String, Object> retrieveMetaData(final RequestMetaData requestMetaData) {

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
