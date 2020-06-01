package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
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
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    private static final String CACHE_GROUP = "Contentlet";

    // width,height,contentType,author,keywords,fileSize,content,length,title

    private volatile ObjectReaderDelegate objectReaderDelegate = new JsonReaderDelegate<>(Map.class);
    private volatile ObjectWriterDelegate objectWriterDelegate = new JsonWriterDelegate();
    private volatile MetadataGenerator    metadataGenerator    = new TikaMetadataGenerator();
    private final    StorageProvider      storageProvider      = new StorageProvider();

    @Override
    public void setObjectReaderDelegate(final ObjectReaderDelegate objectReaderDelegate) {
        this.objectReaderDelegate = objectReaderDelegate;
    }

    @Override
    public void setObjectWriterDelegate(final ObjectWriterDelegate objectWriterDelegate) {
        this.objectWriterDelegate = objectWriterDelegate;
    }

    @Override
    public void setMetadataGenerator(final MetadataGenerator metadataGenerator) {
        this.metadataGenerator = metadataGenerator;
    }

    public StorageProvider getStorageProvider() {
        return this.storageProvider;
    }

    @Override
    public Map<String, Object> generateRawBasicMetaData(final File binary) {

        return this.generateBasicMetaData(binary, s -> true); // raw = no filter
    }

    @Override
    public Map<String, Object> generateRawFullMetaData(final  File binary, long maxLength) {

        return this.generateFullMetaData(binary, s -> true, maxLength); // raw = no filter
    }

    @Override
    public Map<String, Object> generateBasicMetaData(final File binary, final Predicate<String> metaDataKeyFilter) {

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
            mapBuilder.put(SHA226_META_KEY,   Try.of(()->FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));
        }

        return mapBuilder.build();
    }

    @Override
    public Map<String, Object> generateFullMetaData(final File binary, final Predicate<String> metaDataKeyFilter,
                                                    final long maxLength) {

        final TreeMap<String, Object> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {

            metadataMap.putAll(this.generateBasicMetaData(binary, metaDataKeyFilter));
            final Map<String, Object> fullMetaDataMap = this.metadataGenerator.generate(binary, maxLength);
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

        return new ImmutableSortedMap.Builder<String, Object>(Comparator.naturalOrder()).putAll(metadataMap).build();
    }

    @Override
    public Map<String, Object> generateMetaData(final File binary,
                                                final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

        Map<String, Object> metadataMap    = Collections.emptyMap();
        final StorageKey    storageKey     = generateMetaDataConfiguration.getStorageKey();
        final String        storageType    = this.getStorageType (storageKey);
        final Storage       storage        = this.getStorageProvider().getStorage(storageType);

        this.checkBucket  (storageKey, storage);  // todo: see if you want to remove this
        this.checkOverride(storage, generateMetaDataConfiguration);

        if (!storage.existsObject(storageKey.getBucket(), storageKey.getPath())) {

            if (this.validBinary(binary)) {

                final long maxLength = generateMetaDataConfiguration.getMaxLength();
                metadataMap          = generateMetaDataConfiguration.isFull()?
                                        this.generateFullMetaData (binary, generateMetaDataConfiguration.getMetaDataKeyFilter(), maxLength):
                                        this.generateBasicMetaData(binary, generateMetaDataConfiguration.getMetaDataKeyFilter());

                if (generateMetaDataConfiguration.isStore()) {

                    this.storeMetadata(storageKey, storage, metadataMap, binary);
                }
            }
        } else {

            metadataMap =  this.retrieveMetadata(storageKey, storage);
        }

        if (generateMetaDataConfiguration.isCache()) {

            this.putIntoCache(generateMetaDataConfiguration.getCacheKeySupplier().get(), metadataMap);
        }

        return metadataMap;
    }

    private String getStorageType(final StorageKey storageKey) {

        return UtilMethods.isSet(storageKey.getStorage())? storageKey.getStorage():
                Config.getStringProperty("DEFAULT_STORAGE_TYPE", StorageType.FILE_SYSTEM.name());
    }

    private void checkBucket(final StorageKey storageKey, final Storage storage) {

        if (!storage.existsGroup(storageKey.getBucket())) {

            storage.createGroup(storageKey.getBucket());
        }
    }

    private void putIntoCache (final String cacheKey, final Map<String, Object> metadataMap) {

        final DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
        if (null != cacheAdmin) {

            cacheAdmin.put(cacheKey,
                    metadataMap, CACHE_GROUP);
        }
    }

    private Map<String, Object> retrieveMetadata(final StorageKey storageKey, final Storage storage) {

        Map<String, Object> objectMap = Collections.emptyMap();

        try {

            objectMap   = (Map<String, Object>) storage.pullObject(storageKey.getBucket(), storageKey.getPath(), this.objectReaderDelegate);
            Logger.info(this, "Metadata read from: " + storageKey.getPath());
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }

        return objectMap;
    }

    private void storeMetadata(final StorageKey storageKey, final Storage storage,
                               final Map<String, Object> metadataMap, final File binary) {

        try {

            storage.pushObject(storageKey.getBucket(), storageKey.getPath(),
                    this.objectWriterDelegate, (Serializable)metadataMap,
                    this.generateRawBasicMetaData(binary));
            Logger.info(this, "Metadata wrote on: " + storageKey.getPath());
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    private boolean validBinary (final File binary) {

        return null != binary && binary.exists() && binary.canRead();
    }

    private boolean exists (final Optional<File> metadataFile) {

        return metadataFile.isPresent() && metadataFile.get().exists();
    }

    private void checkOverride (final Storage storage,
                                final GenerateMetaDataConfiguration generateMetaDataConfiguration) {

        final StorageKey storageKey = generateMetaDataConfiguration.getStorageKey();
        if (generateMetaDataConfiguration.isOverride() && storage.existsObject(storageKey.getBucket(), storageKey.getPath())) {

            try {

                storage.deleteObject(storageKey.getBucket(), storageKey.getPath());
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                storageKey.getPath(), e.getMessage()), e);
            }
        }
    } // checkOverride.

    @Override
    public Map<String, Object> retrieveMetaData(final RequestMetaData requestMetaData) {

        Map<String, Object> metadataMap = Collections.emptyMap();

        if (requestMetaData.isCache()) {

            final DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (null != cacheAdmin) {

                metadataMap = Try.of(()->(Map<String, Object>)cacheAdmin.get(requestMetaData.getCacheKeySupplier().get(),
                        CACHE_GROUP)).getOrElse(Collections.emptyMap());
            }
        }

        if (!UtilMethods.isSet(metadataMap)) {

            final StorageKey    storageKey     = requestMetaData.getStorageKey();
            final String        storageType    = this.getStorageType (storageKey);
            final Storage       storage        = this.getStorageProvider().getStorage(storageType);

            this.checkBucket(storageKey, storage);
            if (storage.existsObject(storageKey.getBucket(), storageKey.getPath())) {

                metadataMap =  this.retrieveMetadata(storageKey, storage);
                Logger.info(this, "Retrieve the meta data from storage, path: " + storageKey.getPath());
                if (null != requestMetaData.getCacheKeySupplier()) {
                    this.putIntoCache(requestMetaData.getCacheKeySupplier().get(),
                            requestMetaData.getWrapMetadataMapForCache().apply(metadataMap));
                }
            }
        } else {

            Logger.info(this, "Retrieve the meta data from cache, key: " + requestMetaData.getCacheKeySupplier().get());
        }

        return metadataMap;
    }

    private class TikaMetadataGenerator implements MetadataGenerator {

        @Override
        public Map<String, Object> generate(final File binary, final long maxLength) {

            try {

                final TikaUtils tikaUtils = new TikaUtils();
                final Map<String, Object> tikaMetaDataMap = tikaUtils.getForcedMetaDataMap(binary, new Long(maxLength).intValue());
                return tikaMetaDataMap;
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage(), e);
            }

            return Collections.emptyMap();
        }
    }
}
