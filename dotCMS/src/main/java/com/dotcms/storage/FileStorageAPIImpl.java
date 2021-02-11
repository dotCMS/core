package com.dotcms.storage;

import static com.dotcms.storage.model.BasicMetadataFields.*;

import com.dotcms.storage.model.Metadata;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.MetadataCache;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Default implementation
 *
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    private volatile ObjectReaderDelegate objectReaderDelegate;
    private volatile ObjectWriterDelegate objectWriterDelegate;
    private volatile MetadataGenerator metadataGenerator;
    private final StoragePersistenceProvider persistenceProvider;
    private final MetadataCache metadataCache;

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
            final StoragePersistenceProvider persistenceProvider, final MetadataCache metadataCache) {
        this.objectReaderDelegate = objectReaderDelegate;
        this.objectWriterDelegate = objectWriterDelegate;
        this.metadataGenerator = metadataGenerator;
        this.persistenceProvider = persistenceProvider;
        this.metadataCache = metadataCache;
    }

    /**
     * Default constructor
     */
    public FileStorageAPIImpl() {
        this(new JsonReaderDelegate<>(Map.class), new JsonWriterDelegate(),
                new TikaMetadataGenerator(), StoragePersistenceProvider.INSTANCE.get(), CacheLocator.getMetadataCache());
    }

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @return
     */
    @Override
    public Map<String, Serializable> generateRawBasicMetaData(final File binary) {

        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    /**
     * {@inheritDoc}
     * @param binary  {@link File} file to get the information
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return
     */
    @Override
    public Map<String, Serializable> generateRawFullMetaData(final File binary, long maxLength) {

        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    /**
     * {@inheritDoc}
     * Stand alone Tika-independent metadata
     * We can get it without having to call tika
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return
     */
    @Override
    public Map<String, Serializable> generateBasicMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter) {

        final ImmutableSortedMap.Builder<String, Serializable> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (this.validBinary(binary)) {

            mapBuilder.put(TITLE_META_KEY.key(), binary.getName());
            final String relativePath = binary.getAbsolutePath()
                    .replace(ConfigUtils.getAbsoluteAssetsRootPath(),
                            StringPool.BLANK);

            mapBuilder.put(PATH_META_KEY.key(), relativePath);

            if (metaDataKeyFilter.test(LENGTH_META_KEY.key())) {
                mapBuilder.put(LENGTH_META_KEY.key(), binary.length());
            }

            if (metaDataKeyFilter.test(SIZE_META_KEY.key())) {
                mapBuilder.put(SIZE_META_KEY.key(), binary.length());
            }

            if (metaDataKeyFilter.test(CONTENT_TYPE_META_KEY.key())) {
                mapBuilder.put(CONTENT_TYPE_META_KEY.key(), MimeTypeUtils.getMimeType(binary));
            }

            mapBuilder.put(MOD_DATE_META_KEY.key(), binary.lastModified());
            mapBuilder.put(SHA256_META_KEY.key(),
                    Try.of(() -> FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));

            mapBuilder.put(IS_IMAGE_META_KEY.key(), UtilMethods.isImage(relativePath));

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
    public Map<String, Serializable> generateFullMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter,
            final long maxLength) {

        final TreeMap<String, Serializable> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {
            final Map<String, Serializable> fullMetaDataMap = this.metadataGenerator.generate(binary, maxLength);
            if (UtilMethods.isSet(fullMetaDataMap)) {
                for (final Map.Entry<String, Serializable> entry : fullMetaDataMap.entrySet()) {
                    if (metaDataKeyFilter.test(entry.getKey())) {
                        metadataMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            //Add the additional metadata that only exists on basic Metadata
            //do not replace any existing value already calculated by tika
            final Map<String, Serializable> patchMap = generateBasicMetaData(binary, metaDataKeyFilter);
            patchMap.forEach(metadataMap::putIfAbsent);

        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            return Collections.emptyMap();
        }

        return ImmutableSortedMap.copyOf(metadataMap);
    }

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @param configuration {@link GenerateMetadataConfig}
     * @return
     */
    @Override
    public Map<String, Serializable> generateMetaData(final File binary,
            final GenerateMetadataConfig configuration) throws DotDataException {

        final Map<String, Serializable> metadataMap;
        final StorageKey storageKey = configuration.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);  //if the group/bucket doesn't exist create it.
        this.checkOverride(storage, configuration); //if config states we need to remove and force regen
        //if the entry isn't already there skip and simply store in cache.
        if (!storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            if (this.validBinary(binary)) {
                Logger.info(FileStorageAPIImpl.class, ()->String.format(
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
    private void checkBucket(final StorageKey storageKey, final StoragePersistenceAPI storage)
            throws DotDataException {
        if (!storage.existsGroup(storageKey.getGroup())) {
            storage.createGroup(storageKey.getGroup());
        }
    }

    /**
     * save into cache
     * @param cacheKey
     * @param metadataMap
     */
    private void putIntoCache(final String cacheKey, final Map<String, Serializable> metadataMap) {
        metadataCache.addMetadataMap(cacheKey, metadataMap);
    }

    /**
     * meta-data retriever
     * @param storageKey
     * @param storage
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, Serializable> retrieveMetadata(final StorageKey storageKey,
            final StoragePersistenceAPI storage) throws DotDataException {

        final Map<String, Serializable> objectMap = (Map<String, Serializable>) storage
                .pullObject(storageKey.getGroup(), storageKey.getPath(), this.objectReaderDelegate);
        Logger.info(this, "Metadata read from path: " + storageKey.getPath());

        return objectMap;
    }

    /**
     * sends metadata to the respective configured storage
     * @param storageKey
     * @param storage
     * @param metadataMap
     */
    private void storeMetadata(final StorageKey storageKey, final StoragePersistenceAPI storage,
            final Map<String, Serializable> metadataMap) throws DotDataException {

            final Map <String, Serializable> paramsMap = new HashMap<>(metadataMap);
            paramsMap.put("hashObject", true);
        storage.pushObject(storageKey.getGroup(), storageKey.getPath(),
                    this.objectWriterDelegate, (Serializable) metadataMap, paramsMap);
            Logger.info(this, "Metadata wrote on: " + storageKey.getPath());

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
            final GenerateMetadataConfig generateMetaDataConfiguration) throws DotDataException {

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
    public Map<String, Serializable> retrieveMetaData(final RequestMetadata requestMetaData)
            throws DotDataException {

        if (requestMetaData.isCache()) {
            final Map<String, Serializable> metadataMap = metadataCache
                    .getMetadataMap(requestMetaData.getCacheKeySupplier().get());
            if (null != metadataMap) {
                return metadataMap;
            }
        }

        Map<String, Serializable> metadataMap = null;
        final StorageKey storageKey = requestMetaData.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider
                .getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {

            metadataMap = retrieveMetadata(storageKey, storage);
            Logger.info(FileStorageAPIImpl.class,
                    "Retrieve the meta data from storage, path: " + storageKey.getPath());
            if (null != requestMetaData.getCacheKeySupplier()) {
                final Map<String, Serializable> projection = requestMetaData.getProjectionMapForCache().apply(metadataMap);
                putIntoCache(requestMetaData.getCacheKeySupplier().get(), projection);
                return projection;
            }

        }

        return metadataMap;
    }

    /***
     * {@inheritDoc}
     * @param requestMetaData {@link RequestMetadata}
     * @return
     * @throws DotDataException
     */
    public boolean removeMetaData(final RequestMetadata requestMetaData) throws DotDataException{
        boolean deleteSucceeded = false;
        final StorageKey storageKey = requestMetaData.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider
                .getStorage(storageKey.getStorage());
        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
           deleteSucceeded = storage.deleteObject(storageKey.getGroup(), storageKey.getPath());
        }
        return deleteSucceeded;
    }


    /**
     * {@inheritDoc}
     * @param configuration
     * @param customAttributes
     * @throws DotDataException
     */
    @Override
    public void putCustomMetadataAttributes(
            final GenerateMetadataConfig configuration,
            final Map<String, Serializable> customAttributes) throws DotDataException {

        final Map<String, Serializable> prefixedCustomAttributes = customAttributes.entrySet().stream()
                .collect(Collectors.toMap(o -> Metadata.CUSTOM_PROP_PREFIX + o.getKey(), Entry::getValue));

        final StorageKey storageKey = configuration.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {

            final Map<String, Serializable> retrievedMetadata = retrieveMetadata(storageKey, storage);
            if(null != retrievedMetadata){
                final Map<String,Serializable> newMetadataMap = new HashMap<>(retrievedMetadata);
                newMetadataMap.putAll(prefixedCustomAttributes);

                checkOverride(storage, configuration);
                if(configuration.isStore()) {
                   storeMetadata(storageKey, storage, newMetadataMap);
                }
                if(null != configuration.getCacheKeySupplier()){
                    final String cacheKey = configuration.getCacheKeySupplier().get();
                    putIntoCache(cacheKey, newMetadataMap);
                }
            }

        } else {
            Logger.info(FileStorageAPIImpl.class, String.format(
                    "Unable to set custom attribute for the given group: `%s` and path: `%s` ",
                    storageKey.getGroup(), storageKey.getPath()));
        }

    }

}
