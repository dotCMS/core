package com.dotcms.storage;

import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
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

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @return
     */
    @Override
    public Map<String, Object> generateRawBasicMetaData(final File binary) {

        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    /**
     * {@inheritDoc}
     * @param binary  {@link File} file to get the information
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return
     */
    @Override
    public Map<String, Object> generateRawFullMetaData(final File binary, long maxLength) {

        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return
     */
    @Override
    public Map<String, Object> generateBasicMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter) {

        final ImmutableSortedMap.Builder<String, Object> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (this.validBinary(binary)) {


            mapBuilder.put(TITLE_META_KEY, binary.getName());

            final Optional<String> optional = FileUtil.getRealAssetsPathRelativePiece(binary);
            if (optional.isPresent()) {
                mapBuilder.put(PATH_META_KEY, optional.get());
            } else {
                Logger.warn(FileStorageAPIImpl.class, String.format(
                        "Unable to determine relative path for asset `%s`. Using absolute path.",
                        binary));
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

    /**
     * {@inheritDoc}
     * @param binary  {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter for the map result generation
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return
     */
    @Override
    public Map<String, Object> generateFullMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter,
            final long maxLength) {

        final TreeMap<String, Object> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {
            final Map<String, Object> fullMetaDataMap = this.metadataGenerator.generate(binary, maxLength);
            if (UtilMethods.isSet(fullMetaDataMap)) {
                for (final Map.Entry<String, Object> entry : fullMetaDataMap.entrySet()) {
                    if (metaDataKeyFilter.test(entry.getKey())) {
                        metadataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            //basic meta data should override any previous value that might have exist already.
            metadataMap.putAll(this.generateBasicMetaData(binary, metaDataKeyFilter));
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            return Collections.emptyMap();
        }

        return new ImmutableSortedMap.Builder<String, Object>(Comparator.naturalOrder())
                .putAll(metadataMap).build();
    }

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @param configuration {@link GenerateMetadataConfig}
     * @return
     */
    @Override
    public Map<String, Object> generateMetaData(final File binary,
            final GenerateMetadataConfig configuration) {

        final Map<String, Object> metadataMap;
        final StorageKey storageKey = configuration.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);  //if the group/bucket doesn't exist create it.
        this.checkOverride(storage, configuration); //if config states we need to remove and force regen
        //if the entry isn't already there skip and simply store in cache.
        if (!storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
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
                    this.storeMetadata(storageKey, storage, metadataMap);
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

    /**
     * Group existence verifier
     * @param storageKey
     * @param storage
     */
    private void checkBucket(final StorageKey storageKey, final StoragePersistenceAPI storage) {
        if (!storage.existsGroup(storageKey.getGroup())) {
            storage.createGroup(storageKey.getGroup());
        }
    }

    /**
     * save into cache
     * @param cacheKey
     * @param metadataMap
     */
    private void putIntoCache(final String cacheKey, final Map<String, Object> metadataMap) {

        final DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
        if (null != cacheAdmin) {

            cacheAdmin.put(cacheKey,
                    metadataMap, CACHE_GROUP);
        }
    }

    /**
     * meta-data retriever
     * @param storageKey
     * @param storage
     * @return
     */
    private Map<String, Object> retrieveMetadata(final StorageKey storageKey,
            final StoragePersistenceAPI storage) {

        Map<String, Object> objectMap = Collections.emptyMap();

        try {
            objectMap = (Map<String, Object>) storage.pullObject(storageKey.getGroup(), storageKey.getPath(), this.objectReaderDelegate);
            Logger.info(this, "Metadata read from: " + storageKey.getPath());
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }

        return objectMap;
    }

    /**
     * sends metadata to the respective configured storage
     * @param storageKey
     * @param storage
     * @param metadataMap
     */
    private void storeMetadata(final StorageKey storageKey, final StoragePersistenceAPI storage,
            final Map<String, Object> metadataMap) {

        try {
            storage.pushObject(storageKey.getGroup(), storageKey.getPath(),
                    this.objectWriterDelegate, (Serializable) metadataMap, metadataMap);
            Logger.info(this, "Metadata wrote on: " + storageKey.getPath());
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
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
                .existsObject(storageKey.getGroup(), storageKey.getPath())) {

            try {

                storage.deleteObject(storageKey.getGroup(), storageKey.getPath());
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                storageKey.getPath(), e.getMessage()), e);
            }
        }
    } // checkOverride.

    /**
     * {@inheritDoc}
     * @param requestMetaData {@link RequestMetadata}
     * @return
     */
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
            final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

            this.checkBucket(storageKey, storage);
            if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {

                metadataMap = this.retrieveMetadata(storageKey, storage);
                Logger.info(this,
                        "Retrieve the meta data from storage, path: " + storageKey.getPath());
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
