package com.dotcms.storage;

import static com.dotcms.storage.StoragePersistenceAPI.HASH_OBJECT;
import static com.dotcms.storage.model.BasicMetadataFields.*;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.storage.model.BasicMetadataFields;
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
import java.util.Iterator;
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
     * Gets the basic metadata from the binary, this method does not any stores but could do a filter anything
     * Stand alone Tika-independent metadata
     * We can get it without having to call tika
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return
     */
    private Map<String, Serializable> generateBasicMetaData(final File binary,
            final Predicate<String> metaDataKeyFilter) {

        final ImmutableSortedMap.Builder<String, Serializable> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (this.validBinary(binary)) {
            final String binaryName = binary.getName();
            mapBuilder.put(NAME_META_KEY.key(), binaryName);
            mapBuilder.put(TITLE_META_KEY.key(), binaryName); //Title gets replaced by the loaded metadata. Otherwise iwe set a default
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

            final boolean isImage = UtilMethods.isImage(relativePath);
            mapBuilder.put(IS_IMAGE_META_KEY.key(), isImage);
            //These are added here to even things when comparing
            //typically these values are added by tika except for svg file so that creates some sort of inconsistency
            //we add them for image types with a default value of zero that gets replaced by the values provided by tika
            //if tika fails to tell us the dimension we are keeping a default 0
            if(isImage){
               mapBuilder.put(WIDTH_META_KEY.key(), 0);
               mapBuilder.put(HEIGHT_META_KEY.key(), 0);
            }

        }

        return mapBuilder.build();
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
            final Map<String, Serializable> fullMetaDataMap = this.metadataGenerator.generate(binary, maxLength);
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

    /**
     * {@inheritDoc}
     * @param binary {@link File} file to get the information
     * @param configuration {@link GenerateMetadataConfig}
     * @return
     */
    @Override
    public Map<String, Serializable> generateMetaData(final File binary,
            final GenerateMetadataConfig configuration) throws DotDataException {

         Map<String, Serializable> metadataMap;
        final StorageKey storageKey = configuration.getStorageKey();
        final StoragePersistenceAPI storage = persistenceProvider.getStorage(storageKey.getStorage());

        this.checkBucket(storageKey, storage);  //if the group/bucket doesn't exist create it.
        this.checkOverride(storage, configuration); //if config states we need to remove and force regen
        //if the entry isn't already there skip and simply store in cache.
        if (!storage.existsObject(storageKey.getGroup(), storageKey.getPath())) {
            if (this.validBinary(binary)) {
                Logger.debug(FileStorageAPIImpl.class, ()->String.format(
                        "Object identified by `/%s/%s` didn't exist in storage %s will be generated.",
                        storageKey.getGroup(), storageKey,
                        configuration.isFull() ? "full-metadata" : "basic-metadata"));
                final long maxLength = configuration.getMaxLength();
                metadataMap = configuration.isFull() ?
                        generateFullMetaData(binary,
                                configuration.getMetaDataKeyFilter(), maxLength) :
                        generateBasicMetaData(binary,
                                configuration.getMetaDataKeyFilter());

                if (configuration.isStore()) {

                    if(null != configuration.getMergeWithMetadata()){

                        final Map<String, Serializable> patchMap = configuration.getMergeWithMetadata().getCustomMeta();
                        if(!patchMap.isEmpty()){
                            //This is necessary since metadataMap is immutable.
                            metadataMap = new HashMap<>(metadataMap);
                            patchMap.forEach(metadataMap::putIfAbsent);
                        }
                    }

                    storeMetadata(storageKey, storage, metadataMap);
                }

            } else {
               throw new IllegalArgumentException(String.format("the binary `%s` isn't accessible ", binary != null ? binary.getName() : "unknown"));
            }
        } else {
            metadataMap = retrieveMetadata(storageKey, storage);
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
     * if the given configuration states we must override the previously existing file will be deleted before re-generating.
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

        if (override && storage
                .existsObject(storageKey.getGroup(), storageKey.getPath())) {

            try {

                storage.deleteObjectAndReferences(storageKey.getGroup(), storageKey.getPath());
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                storageKey.getPath(), e.getMessage()), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param requestMetaData {@link FetchMetadataParams}
     * @return
     */
    @Override
    public Map<String, Serializable> retrieveMetaData(final FetchMetadataParams requestMetaData)
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
            Logger.debug(FileStorageAPIImpl.class,
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
     * @param requestMetaData {@link FetchMetadataParams}
     * @return
     * @throws DotDataException
     */
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

    /***
     * {@inheritDoc}
     * @param requestMetaData {@link FetchMetadataParams}
     * @return
     * @throws DotDataException
     */
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

    /**
     * {@inheritDoc}
     * @param fetchMetadataParams
     * @param customAttributes
     * @throws DotDataException
     */
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
                newMetadataMap.putAll(prefixedCustomAttributes);

                checkOverride(storage, fetchMetadataParams.getStorageKey(), true);
                storeMetadata(storageKey, storage, newMetadataMap);

                if(null != fetchMetadataParams.getCacheKeySupplier()){
                    final String cacheKey = fetchMetadataParams.getCacheKeySupplier().get();
                    putIntoCache(cacheKey, newMetadataMap);
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

    /**
     * {@inheritDoc}
     * @param requestMetadata
     * @param metadata
     * @throws DotDataException
     */
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
