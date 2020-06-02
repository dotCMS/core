package com.dotcms.storage;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation
 * @author jsanca
 */
public class ContentletMetadataAPIImpl implements ContentletMetadataAPI {

    private static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";
    private static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    private static final String DEFAULT_METADATA_GROUP_NAME = "dotmetadata";
    private final FileStorageAPI fileStorageAPI;

    public ContentletMetadataAPIImpl() {
        this(APILocator.getFileStorageAPI());
    }

    @VisibleForTesting
    public ContentletMetadataAPIImpl(final FileStorageAPI fileStorageAPI) {
        this.fileStorageAPI = fileStorageAPI;
    }

    @Override
    public ContentletMetadata generateContentletMetadata(final Contentlet contentlet,
                                                         final Set<String> basicBinaryFieldNameSet,
                                                         final Set<String> fullBinaryFieldNameSet) throws IOException {

        final ImmutableMap.Builder<String, Map<String, Object>> fullMetadataMap  = new ImmutableMap.Builder();
        final ImmutableMap.Builder<String, Map<String, Object>> basicMetadataMap = new ImmutableMap.Builder();
        final  Map<String, Field>  fieldMap   = contentlet.getContentType().fieldMap();
        /*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
        final boolean alwaysRegenerateMetadata = Config
                .getBooleanProperty("always.regenerate.metadata.on.reindex", false);

        Logger.debug(this, ()-> "Generating the metadata for contentlet, id = " + contentlet.getIdentifier());

        this.generateFullMetadata (contentlet, fullBinaryFieldNameSet, fullMetadataMap, fieldMap, alwaysRegenerateMetadata);
        this.generateBasicMetadata(contentlet, basicBinaryFieldNameSet, fullBinaryFieldNameSet, basicMetadataMap, fieldMap, alwaysRegenerateMetadata);

        return new ContentletMetadata(fullMetadataMap.build(), basicMetadataMap.build());
    }

    private void generateBasicMetadata(final Contentlet contentlet,
                                       final Set<String> basicBinaryFieldNameSet,
                                       final Set<String> fullBinaryFieldNameSet,
                                       final ImmutableMap.Builder<String, Map<String, Object>> basicMetadataMap,
                                       final Map<String, Field> fieldMap,
                                       final boolean alwaysRegenerateMetadata) throws IOException {

        final String storageType        = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        Map<String, Object> metadataMap = Collections.emptyMap();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DEFAULT_METADATA_GROUP_NAME);
        for (final String basicBinaryFieldName : basicBinaryFieldNameSet) {

            final File file           = contentlet.getBinary(basicBinaryFieldName);
            final String metadataPath = this.getFileName(contentlet, basicBinaryFieldName);

            if (null != file && file.exists() && file.canRead()) {

                // if already included on the full, the file was already generated, just need to add the basic to the cache.
                final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(basicBinaryFieldName).id());

                if (fullBinaryFieldNameSet.contains(basicBinaryFieldName)) {

                    // if it is included on the full keys, we only have to store the meta in the cache.
                    metadataMap = this.fileStorageAPI.generateBasicMetaData(file,
                            metadataKey -> metadataFields.isEmpty() ? true : metadataFields.contains(metadataKey));
                    CacheLocator.getContentletCache()
                            .addMetadataMap(contentlet.getInode() + basicBinaryFieldName, metadataMap);
                } else {


                    metadataMap = this.fileStorageAPI.generateMetaData(file,
                            new GenerateMetaDataConfiguration.Builder()
                                    .full(false)
                                    .override(alwaysRegenerateMetadata)
                                    .store(true)
                                    .cache(()-> ContentletCache.META_DATA_MAP_KEY + contentlet.getInode() + basicBinaryFieldName)
                                    .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty() ? true : metadataFields.contains(metadataKey))
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName).key(metadataPath).storage(storageType).build())
                                    .build()
                    );
                }

                basicMetadataMap.put(basicBinaryFieldName, metadataMap);
            } else {

                throw new IOException("The file: " + file + ", is null, does not exists or can not access");
            }
        }
    }

    private String getFileName (final Contentlet contentlet, final String fieldVariableName) {

        final String inode        = contentlet.getInode();
        final String fileName     = fieldVariableName + "-metadata.json";
        return StringUtils.builder(File.separator,
                inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                fileName).toString();
    }

    private void generateFullMetadata(final Contentlet contentlet,
                                      final Set<String> fullBinaryFieldNameSet,
                                      final ImmutableMap.Builder<String, Map<String, Object>> fullMetadataMap,
                                      final Map<String, Field> fieldMap,
                                      final boolean alwaysRegenerateMetadata) throws IOException {

        final String storageType        = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        Map<String, Object> metadataMap = Collections.emptyMap();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
        for (final String fullBinaryFieldName : fullBinaryFieldNameSet) {

            final File file           = contentlet.getBinary(fullBinaryFieldName);
            final String metadataPath = this.getFileName(contentlet, fullBinaryFieldName);
            if (null != file && file.exists() && file.canRead()) {

                final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(fullBinaryFieldName).id());
                metadataMap = this.fileStorageAPI.generateMetaData(file,
                        new GenerateMetaDataConfiguration.Builder()
                            .full(true)
                            .override(alwaysRegenerateMetadata)
                            .cache(false)  // do not want cache on full meta
                            .store(true)
                            .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty()? true: metadataFields.contains(metadataKey))
                            .storageKey(new StorageKey.Builder().group(metadataBucketName).key(metadataPath).storage(storageType).build())
                            .build()
                        );

                fullMetadataMap.put(fullBinaryFieldName, metadataMap);
            } else {

                throw new IOException("The file: " + file + ", is null, does not exists or can not access");
            }
        }
    }

    private Set<String> getMetadataFields (final String fieldIdentifier) {

        final Optional<FieldVariable> customIndexMetaDataFieldsOpt =
                Try.of(()->FactoryLocator.getFieldFactory().byFieldVariableKey(fieldIdentifier, BinaryField.INDEX_METADATA_FIELDS)).getOrElse(Optional.empty());

        final Set<java.lang.String> metadataFields = customIndexMetaDataFieldsOpt.isPresent()?
                new HashSet<>(Arrays.asList(customIndexMetaDataFieldsOpt.get().value().split(StringPool.COMMA))):
                this.getConfiguredMetadataFields();

        return metadataFields;
    }

    /**
     * Reads INDEX_METADATA_FIELDS from configuration
     * @return
     */
    private Set<java.lang.String> getConfiguredMetadataFields(){

        final java.lang.String configFields = Config.getStringProperty("INDEX_METADATA_FIELDS", null);

        return UtilMethods.isSet(configFields)?
            new HashSet<>(Arrays.asList( configFields.split(StringPool.COMMA))):
            Collections.emptySet();
    }

    @Override
    public ContentletMetadata generateContentletMetadata(final Contentlet contentlet) throws IOException {

        final Tuple2<Set<String>, Set<String>> binaryFields = this.findBinaryFields(contentlet);
        return this.generateContentletMetadata(contentlet, binaryFields._1(), binaryFields._2());
    }

    @Override
    public Map<String, Object> getMetadata(final Contentlet contentlet, final Field field) {

        return this.getMetadata(contentlet, field.variable());
    }

    @Override
    public Map<String, Object> getMetadata(final Contentlet contentlet,final  String fieldVariableName) {

        final String storageType        = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
        final String metadataPath       = this.getFileName(contentlet, fieldVariableName);

        return this.fileStorageAPI.retrieveMetaData(
                new RequestMetaData.Builder()
                        .wrapMetadataMapForCache(this::wrapMetadataMapForCache)
                        .cache(()-> ContentletCache.META_DATA_MAP_KEY + contentlet.getInode() + fieldVariableName)
                        .storageKey(new StorageKey.Builder().group(metadataBucketName).key(metadataPath).storage(storageType).build())
                        .build()
        );
    }

    @Override
    public Map<String, Object> getMetadataNoCache(final Contentlet contentlet,final  String fieldVariableName) {

        final String storageType        = Config.getStringProperty(DEFAULT_STORAGE_TYPE, StorageType.FILE_SYSTEM.name());
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, "dotmetadata");
        final String metadataPath       = this.getFileName(contentlet, fieldVariableName);

        return this.fileStorageAPI.retrieveMetaData(
                new RequestMetaData.Builder()
                        .cache(false)
                        .wrapMetadataMapForCache(this::wrapMetadataMapForCache)
                        .storageKey(new StorageKey.Builder().group(metadataBucketName).key(metadataPath).storage(storageType).build())
                        .build()
        );
    }

    private Map<String, Object> wrapMetadataMapForCache (final Map<String, Object> originalMap) {

        final ImmutableMap.Builder<String, Object> reduceMap = new ImmutableMap.Builder<>();

        for (final String key : originalMap.keySet()) {

            if (FileStorageAPI.TITLE_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.TITLE_META_KEY, originalMap.get(key));
            }

            if (FileStorageAPI.PATH_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.PATH_META_KEY, originalMap.get(key));
            }

            if (FileStorageAPI.LENGTH_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.LENGTH_META_KEY, originalMap.get(key));
            }

            if (FileStorageAPI.CONTENT_TYPE_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.CONTENT_TYPE_META_KEY, originalMap.get(key));
            }

            if (FileStorageAPI.MOD_DATE_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.MOD_DATE_META_KEY, originalMap.get(key));
            }

            if (FileStorageAPI.SHA226_META_KEY.equals(key)) {

                reduceMap.put(FileStorageAPI.SHA226_META_KEY, originalMap.get(key));
            }
        }

        return reduceMap.build();
    }

    private Tuple2<Set<String>, Set<String>> findBinaryFields(final Contentlet contentlet) {

        final List<Field> binaryFields = contentlet.getContentType().fields(BinaryField.class);
        final Set<String> basicBinaryFieldNameSet = new HashSet<>();
        final Set<String> fullBinaryFieldNameSet  = new HashSet<>();

        if (UtilMethods.isSet(binaryFields)) {

            for (final Field binaryField : binaryFields) {

                if (binaryField.indexed() && fullBinaryFieldNameSet.isEmpty()) {

                    fullBinaryFieldNameSet.add(binaryField.variable());
                }

                basicBinaryFieldNameSet.add(binaryField.variable());
            }
        }

        return Tuple.of(basicBinaryFieldNameSet, fullBinaryFieldNameSet);
    }

}
