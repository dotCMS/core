package com.dotcms.storage;

import com.dotcms.storage.model.BasicMetadataFields;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.MetadataCache;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dotcms.storage.StoragePersistenceAPI.HASH_OBJECT;
import static com.dotcms.storage.model.BasicMetadataFields.CONTENT_TYPE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.EDITABLE_AS_TEXT;
import static com.dotcms.storage.model.BasicMetadataFields.LENGTH_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.SIZE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.VERSION_KEY;
import static com.dotmarketing.util.UtilMethods.isSet;

/**
 * This is the default implementation of the {@link FileStorageAPI} class.
 *
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    private final ObjectReaderDelegate objectReaderDelegate;
    private final ObjectWriterDelegate objectWriterDelegate;
    private final MetadataGenerator metadataGenerator;
    private final StoragePersistenceProvider persistenceProvider;
    private final MetadataCache metadataCache;

    /**
     * Constructor used by Integration Tests.
     *
     * @param objectReaderDelegate The {@link ObjectReaderDelegate} that reads an object.
     * @param objectWriterDelegate The {@link ObjectWriterDelegate} that writes an object.
     * @param metadataGenerator    The {@link MetadataGenerator} that generates the metadata.
     * @param persistenceProvider  The {@link StoragePersistenceProvider} that persists the file's
     *                             metadata in a specific destination: File System, Redis, AWS S3,
     *                             etc.
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
        this(DEFAULT_OBJECT_READER_DELEGATE, DEFAULT_OBJECT_WRITER_DELEGATE,
                DEFAULT_METADATA_GENERATOR, StoragePersistenceProvider.INSTANCE.get(),
                CacheLocator.getMetadataCache());
    }

    @Override
    public Map<String, Serializable> generateRawBasicMetaData(final File binary) {
        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    @Override
    public Map<String, Serializable> generateRawFullMetaData(final File binary, long maxLength) {
        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    /**
     * Gets the basic metadata from the binary, this method does not any stores but could do a filter anything
     * Stand alone Tika-independent metadata
     * We can get it without having to call tika
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return
     */
    private Map<String, Serializable> generateBasicMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter) {
        if (this.validBinary(binary)) {

            final TreeMap<String, Serializable> standAloneMetadata = metadataGenerator.standAloneMetadata(binary);

            if (!metaDataKeyFilter.test(LENGTH_META_KEY.key())) {
                standAloneMetadata.remove(LENGTH_META_KEY.key());
            }

            if (!metaDataKeyFilter.test(SIZE_META_KEY.key())) {
                standAloneMetadata.remove(SIZE_META_KEY.key());
            }

            if (!metaDataKeyFilter.test(CONTENT_TYPE_META_KEY.key())) {
                standAloneMetadata.remove(CONTENT_TYPE_META_KEY.key());
            }

            standAloneMetadata.put(VERSION_KEY.key(), APILocator.getFileMetadataAPI().getBinaryMetadataVersion());

            return ensureTypes(standAloneMetadata);
        }

        return ImmutableMap.of();
    }

    /**
     * Gets the full metadata from the binary, this could involved a more expensive process such as Tika, this method does not any stores but could do a filter anything
     * @param binary  {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter for the map result generation
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return Map with the metadata
     */
    private Map<String, Serializable> generateFullMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter,
            final long maxLength) {
        TreeMap<String, Serializable> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {
            final Map<String, Serializable> fullMetaDataMap = this.metadataGenerator.tikaBasedMetadata(binary, maxLength);
            if (isSet(fullMetaDataMap)) {
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
            Logger.error(FileStorageAPIImpl.class, "Exception generating full metadata", e);
            return Collections.emptyMap();
        }

        metadataMap = ensureTypes(metadataMap);

        return ImmutableSortedMap.copyOf(metadataMap);
    }

    /**
     *This bit makes sure that the expected properties are rendered accordingly to the registered type
     * @param metadataMap
     * @return
     */
    private TreeMap<String, Serializable> ensureTypes(TreeMap<String, Serializable> metadataMap){
        final Map<String, BasicMetadataFields> metadataFieldsMap = BasicMetadataFields.keyMap();
        final Iterator<Entry<String, Serializable>> iterator = metadataMap.entrySet().iterator();
        while(iterator.hasNext()){
            final Entry<String, Serializable> entry = iterator.next();
            final Serializable value = entry.getValue();
            if (isSet(value)) {
                final BasicMetadataFields field = metadataFieldsMap.get(entry.getKey());
                if(null == field) {
                   continue;
                }
                if (field.isNumericType()) {
                     Try.of(()-> entry.setValue(Integer.parseInt(value.toString())));
                }

                if (field.isBooleanType()) {
                     Try.of(()-> entry.setValue(Boolean.parseBoolean(value.toString())));
                }
            }
        }
        return metadataMap;
    }

    @Override
    public Map<String, Serializable> generateMetaData(final File binary,
            final GenerateMetadataConfig configuration) throws DotDataException {
        final StorageKey storageKey = configuration.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());
        if(configuration.isStore()) {
            //if the group/bucket doesn't exist create it.
            this.checkBucket(storageKey, storage);
            //if config states we need to remove and force regeneration of the metadata
            this.checkOverride(storage, configuration);
        }
        final boolean objectExists = storage.existsObject(storageKey.getGroup(), storageKey.getPath());

        Map<String, Serializable> metadataMap = objectExists ? retrieveMetadata(storageKey, storage) : Map.of();
        final Map<String, Serializable> onlyHasCustomMetadataMap = configuration.getIfOnlyHasCustomMetadata().apply(metadataMap);

        // here we test quite a few things:
        // if the metadata did not exist at all we need to generate it for sure .
        // But if there was any metadata already, and it only contained custom metadata attributes, we need to generate it.
        if (!objectExists || (null != onlyHasCustomMetadataMap && !onlyHasCustomMetadataMap.isEmpty())) {
            metadataMap = generateMetadataFromFile(binary, configuration, storageKey, onlyHasCustomMetadataMap, storage);
        }

        if (configuration.isCache()) {
            this.putIntoCache(configuration.getCacheKeySupplier().get(), metadataMap);
        }

        return metadataMap;
    }

    /**
     * Generates the metadata for the specified binary file.
     *
     * @param binary                   The binary {@link File} whose metadata is being generated.
     * @param configuration            The {@link GenerateMetadataConfig} that specifies the
     *                                 constraints and how the metadata will be generated
     * @param storageKey               The {@link StorageKey} that identifies the binary file and
     *                                 how it will be stored.
     * @param onlyHasCustomMetadataMap The {@link Map} that contains the custom metadata
     *                                 attributes.
     * @param storage                  The {@link StoragePersistenceAPI} that will be used to
     *                                 persist the metadata.
     *
     * @return The {@link Map} that contains the generated metadata.
     *
     * @throws DotDataException An error occurred when persisting the generated metadata.
     */
    private Map<String, Serializable> generateMetadataFromFile(final File binary,
                                                               final GenerateMetadataConfig configuration, final StorageKey storageKey, final Map<String, Serializable> onlyHasCustomMetadataMap, final StoragePersistenceAPI storage) throws DotDataException {
        Map<String, Serializable> metadataMap;
        if (!validBinary(binary)) {
            throw new IllegalArgumentException(String.format("the binary `%s` isn't accessible ",
                    binary != null ? binary : "unknown"));
        }

        Logger.debug(FileStorageAPIImpl.class, () -> String.format(
                "Object identified by `/%s/%s` didn't exist in storage %s. It will be generated",
                storageKey.getGroup(), storageKey,
                configuration.isFull() ? "full-metadata" : "basic-metadata"));
        final long maxLength = configuration.getMaxLength();
        metadataMap = configuration.isFull() ?
                generateFullMetaData(binary,
                        configuration.getMetaDataKeyFilter(), maxLength) :
                generateBasicMetaData(binary,
                        configuration.getMetaDataKeyFilter());

        if (configuration.isStore()) {
            if (null != configuration.getMergeWithMetadata()) {
                final Map<String, Serializable> patchMap =
                        configuration.getMergeWithMetadata().getCustomMetaWithPrefix();
                //we need to include the prefix since we're saving it directly into persistence
                if (!patchMap.isEmpty()) {
                    //This is necessary since metadataMap is immutable.
                    metadataMap = new HashMap<>(metadataMap);
                    patchMap.forEach(metadataMap::putIfAbsent);
                }
            } else {
                //Carry the custom metadata
                if (!onlyHasCustomMetadataMap.isEmpty()) {
                    //This metadata is expected to have prefix that's fine.
                    //This is necessary since metadataMap is immutable.
                    metadataMap = new HashMap<>(metadataMap);
                    onlyHasCustomMetadataMap.forEach(metadataMap::putIfAbsent);
                }
            }
            storeMetadata(storageKey, storage, metadataMap);
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
        if(metadataMap.isEmpty()){
          metadataCache.removeMetadata(cacheKey);
        } else {
            metadataCache.addMetadataMap(cacheKey, metadataMap);
        }
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
        Logger.debug(this, "Metadata read from path: " + storageKey.getPath());

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
        final Map<String, Serializable> paramsMap = new HashMap<>(metadataMap);
        paramsMap.put(HASH_OBJECT, true);

        storage.pushObject(storageKey.getGroup(), storageKey.getPath(),
                this.objectWriterDelegate, (Serializable) metadataMap, paramsMap);
        Logger.debug(this, "Metadata written to: " + storageKey.getPath());

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
     * if the given configuration states we must override the previously existing file. It will be deleted before re-generating.
     * @param storage
     * @param generateMetaDataConfiguration
     */
    private void checkOverride(final StoragePersistenceAPI storage,
            final GenerateMetadataConfig generateMetaDataConfiguration) throws DotDataException {
        checkOverride(storage, generateMetaDataConfiguration.getStorageKey(), generateMetaDataConfiguration.isOverride());
    }

    /**
     * if the given configuration states we must override the previously existing file will be deleted before re-generating.
     * @param storage
     * @param storageKey
     * @param override
     */
    private void checkOverride(final StoragePersistenceAPI storage, final StorageKey storageKey, final boolean override) throws DotDataException {
        if (override && storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            try {
                storage.deleteObjectAndReferences(storageKey.getGroup(), storageKey.getPath());
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file '%s': %s",
                                storageKey.getPath(), e.getMessage()), e);
            }
        }
    }

    @Override
    public Map<String, Serializable> retrieveMetaData(final FetchMetadataParams requestMetaData)
            throws DotDataException {
        if (requestMetaData.isCache()) {
            final Map<String, Serializable> metadataMap = this.metadataCache
                    .getMetadataMap(requestMetaData.getCacheKeySupplier().get());
            if (null != metadataMap) {
                checkEditableAsText(metadataMap);
                putIntoCache(requestMetaData.getCacheKeySupplier().get(), metadataMap);
                return metadataMap;
            }
        }

        Map<String, Serializable> metadataMap = null;
        final StorageKey storageKey = requestMetaData.getStorageKey();
        final StoragePersistenceAPI storage = this.persistenceProvider
                .getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            metadataMap = retrieveMetadata(storageKey, storage);
            Logger.debug(FileStorageAPIImpl.class,
                    () -> "Retrieve the meta data from storage path: " + storageKey.getPath());
            checkEditableAsText(metadataMap);
            if (null != requestMetaData.getCacheKeySupplier()) {
                final Map<String, Serializable> projection = requestMetaData.getProjectionMapForCache().apply(metadataMap);
                putIntoCache(requestMetaData.getCacheKeySupplier().get(), projection);
                return projection;
            }
        }
        return metadataMap;
    }

    /**
     * Performs a simple check that verifies whether the File that the metadata belongs to can be
     * editable as a text file. If it can, the {@link BasicMetadataFields#EDITABLE_AS_TEXT} property
     * will be added.
     * <p>This is particularly useful in the File's edit mode in the back-end for dotCMS to allow
     * content authors to edit the contents directly in the Code Editor field. If, for any reason,
     * the MIME Type cannot be detected, the value of the
     * the {@link BasicMetadataFields#EDITABLE_AS_TEXT} property will be set to {@code false}.</p>
     *
     * @param metadataMap The File's metadata Map.
     */
    private void checkEditableAsText(final Map<String, Serializable> metadataMap) {
        if (UtilMethods.isSet(metadataMap)) {
            metadataMap.computeIfAbsent(EDITABLE_AS_TEXT.key(), key -> {

                final String mimeType =
                        Try.of(() -> metadataMap.get(CONTENT_TYPE_META_KEY.key()).toString()).getOrElse(StringPool.BLANK);
                return FileUtil.isFileEditableAsText(mimeType);

            });
        }
    }

    @Override
    public boolean removeMetaData(final FetchMetadataParams requestMetaData) throws DotDataException{
        boolean deleteSucceeded = false;
        final StorageKey storageKey = requestMetaData.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider
                .getStorage(storageKey.getStorage());
        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
           deleteSucceeded = storage.deleteObjectAndReferences(storageKey.getGroup(), storageKey.getPath());
        }
        return deleteSucceeded;
    }

    @Override
    public boolean removeVersionMetaData(final FetchMetadataParams requestMetaData) throws DotDataException{
        boolean deleteSucceeded = false;
        final StorageKey storageKey = requestMetaData.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider
                .getStorage(storageKey.getStorage());
        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            deleteSucceeded = storage.deleteObjectReference(storageKey.getGroup(), storageKey.getPath());
        }
        return deleteSucceeded;
    }

    @Override
    public void putCustomMetadataAttributes(
            final FetchMetadataParams fetchMetadataParams,
            final Map<String, Serializable> customAttributes) throws DotDataException {

        final Map<String, Serializable> prefixedCustomAttributes = customAttributes.entrySet().stream()
                .collect(Collectors.toMap(o -> Metadata.CUSTOM_PROP_PREFIX + o.getKey(), Entry::getValue));

        final StorageKey storageKey = fetchMetadataParams.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);
        if (storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            final Map<String, Serializable> retrievedMetadata = retrieveMetadata(storageKey, storage);
            if(null != retrievedMetadata){
                final Map<String,Serializable> newMetadataMap = new HashMap<>(retrievedMetadata);

                if(prefixedCustomAttributes.isEmpty()){
                   //Delete all custom attributes
                    newMetadataMap.keySet().removeAll(newMetadataMap.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(Metadata.CUSTOM_PROP_PREFIX))
                            .map(Entry::getKey).collect(Collectors.toSet()));
                } else {
                    //merge maps
                    prefixedCustomAttributes.forEach((key, serializable) -> {
                        if (!newMetadataMap.containsKey(key)) {
                            newMetadataMap.remove(key);
                        }
                    });
                    newMetadataMap.putAll(prefixedCustomAttributes);
                }

                checkOverride(storage, fetchMetadataParams.getStorageKey(), true);
                storeMetadata(storageKey, storage, newMetadataMap);

                if(null != fetchMetadataParams.getCacheKeySupplier()){
                    final String cacheKey = fetchMetadataParams.getCacheKeySupplier().get();
                    putIntoCache(cacheKey, newMetadataMap);
                } else {
                    Logger.warn(FileStorageAPIImpl.class, "No Cache Key has been provided for stored object "+storageKey);
                }
            } else {
                Logger.warn(FileStorageAPIImpl.class, String.format("Unable to locate object: `%s` ",storageKey));
            }
        } else {
           if(fetchMetadataParams.isForceInsertion()){
              storeMetadata(storageKey, storage, prefixedCustomAttributes);
               if(null != fetchMetadataParams.getCacheKeySupplier()){
                   final String cacheKey = fetchMetadataParams.getCacheKeySupplier().get();
                   putIntoCache(cacheKey, prefixedCustomAttributes);
               }
           } else {
               Logger.warn(FileStorageAPIImpl.class, String.format(
                    "Unable to set custom attribute for the given group: `%s` and path: `%s` ",
                    storageKey.getGroup(), storageKey.getPath()));
            }
        }

    }

    @Override
    public boolean setMetadata(final FetchMetadataParams requestMetadata,
            final Map<String, Serializable> metadata) throws DotDataException {
        final StorageKey storageKey = requestMetadata.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider
                .getStorage(storageKey.getStorage());

        checkBucket(storageKey, storage);
        checkOverride(storage, requestMetadata.getStorageKey(), true);
        storeMetadata(storageKey, storage, metadata);

        if (null != requestMetadata.getCacheKeySupplier()) {
            final String cacheKey = requestMetadata.getCacheKeySupplier().get();
            //need to apply filter here before we store into cache?
            if (null != requestMetadata.getProjectionMapForCache()) {
                final Map<String, Serializable> projection = requestMetadata
                        .getProjectionMapForCache().apply(metadata);
                putIntoCache(cacheKey, projection);
            } else {
                putIntoCache(cacheKey, metadata);
            }
        }
        return storage.existsObject(storageKey.getGroup(), storageKey.getPath());

    }

}
