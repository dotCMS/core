package com.dotcms.content.model.hydration;

import static com.dotcms.content.model.hydration.HydrationUtils.findLinkedBinary;
import static com.dotcms.storage.FileMetadataAPI.DEFAULT_METADATA_GROUP_NAME;
import static com.dotcms.storage.StoragePersistenceProvider.METADATA_GROUP_NAME;
import static com.dotcms.util.ReflectionUtils.setValue;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.FileStorageAPI;
import com.dotcms.storage.GenerateMetadataConfig;
import com.dotcms.storage.StorageKey;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import io.vavr.control.Try;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Little reusable component meant to populate a field in the FieldValue Object through the FieldValue's Builder
 */
public class MetadataDelegate implements HydrationDelegate {

    public static final String SHA_256 = "sha256";
    public static final String NAME = "name";
    public static final String CONTENT_TYPE = "contentType";
    public static final String IS_IMAGE = "isImage";

    static final Set<String> fieldNames = Set.of(SHA_256, NAME, CONTENT_TYPE, IS_IMAGE);

    @Override
    public FieldValueBuilder hydrate(final FieldValueBuilder builder, final Field field,
            final Contentlet contentlet, String propertyName)
            throws DotDataException, DotSecurityException {

        Map<String, Object> metadataMap = null;
        try{
            metadataMap = getMetadataMap(field, contentlet);
        } finally {
            //We know it is safe to set null as a fallback cuz we marked the attribute nullable
            setValue(builder, propertyName, metadataMap);
        }
        return builder;
    }

    /**
     * This must be solid we dont want a NPE here since it could break the entire start-up process
     * @param field
     * @param contentlet
     * @return
     */
    private Map<String, Object> getMetadataMap(final Field field, final Contentlet contentlet) {
        final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();
        Map<String, Serializable> metadataMap = null;
        try {
            if (field instanceof BinaryField) {
                final File file = (File) contentlet.get(field.variable());
                final String path = Try.of(()-> fileMetadataAPI.getFileName(contentlet, field.variable())).getOrElse("unk");
                metadataMap = getMetadataMap(file, path);
            } else {
                if (field instanceof ImageField) {
                    final Optional<Contentlet> fileAsContentOptional = findLinkedBinary(contentlet, (ImageField) field);
                    if (fileAsContentOptional.isPresent()) {
                        final Contentlet fileAsset = fileAsContentOptional.get();
                        final String path = Try.of(()-> fileMetadataAPI.getFileName(contentlet, FileAssetAPI.BINARY_FIELD)).getOrElse("unk");
                        final File file = (File) fileAsset.get(FileAssetAPI.BINARY_FIELD);
                        metadataMap = getMetadataMap(file, path);
                    }
                }
            }
        } catch (Throwable e) {
            Logger.warnAndDebug(MetadataDelegate.class, "error calculating metadata ", e);
        }
        return (metadataMap == null || metadataMap.isEmpty() ? null : filterMetadataFields(metadataMap));
    }

    /**
     *
     * @param file
     * @return
     * @throws DotDataException
     */
    private Map<String, Serializable> getMetadataMap(final File file, final String path) throws DotDataException {
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DEFAULT_METADATA_GROUP_NAME);
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final FileStorageAPI fileStorageAPI = APILocator.getFileStorageAPI();
        final Predicate<String> filterBasicMetadataKey = metadataKey -> true;
         return fileStorageAPI.generateMetaData(file,
                new GenerateMetadataConfig.Builder()
                        //if there's metadata already generated this should find it for us (all that if we have an inode)
                        //otherwise it'll give us the basic md. Nothing gets stored/saved here.
                        .storageKey(new StorageKey.Builder().group(metadataBucketName).path(path).storage(storageType).build())
                        .full(false)
                        .override(false)
                        .store(false)
                        .cache(false)
                        .metaDataKeyFilter(filterBasicMetadataKey)
                        .build());
    }

    /**
     * Remove unwanted extra attributes
     * @param originalMap
     * @return
     */
    private Map<String, Object> filterMetadataFields(
            final Map<String, Serializable> originalMap) {
        if (null == originalMap) {
            return null;
        }
        return originalMap.entrySet().stream().filter(entry -> fieldNames
                .contains(entry.getKey())).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue));
    }

}
