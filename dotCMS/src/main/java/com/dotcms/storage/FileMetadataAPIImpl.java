package com.dotcms.storage;

import static com.dotcms.storage.FileStorageAPI.BASIC_METADATA_FIELDS;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation
 * @author jsanca
 */
public class FileMetadataAPIImpl implements FileMetadataAPI {

    private static final String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";
    private static final String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    private static final String DOT_METADATA = "dotmetadata";
    private static final String DEFAULT_METADATA_GROUP_NAME = DOT_METADATA;
    private final FileStorageAPI fileStorageAPI;
    private final ContentletCache contentletCache;

    public FileMetadataAPIImpl() {
        this(APILocator.getFileStorageAPI(), CacheLocator.getContentletCache());
    }

    @VisibleForTesting
    FileMetadataAPIImpl(final FileStorageAPI fileStorageAPI, final ContentletCache contentletCache) {
        this.fileStorageAPI = fileStorageAPI;
        this.contentletCache = contentletCache;
    }

    /**
     * {@inheritDoc}
     * @param contentlet Contentlet
     * @param basicBinaryFieldNameSet {@link Set} fields to generate basic metadata
     * @param fullBinaryFieldNameSet  {@link Set} fields to generate full metadata
     * @return
     * @throws IOException
     */
    @Override
    public ContentletMetadata generateContentletMetadata(final Contentlet contentlet,
                                                         final Set<String> basicBinaryFieldNameSet,
                                                         final Set<String> fullBinaryFieldNameSet)
            throws IOException, DotDataException {

        final ImmutableMap.Builder<String, Map<String, Serializable>> fullMetadataMap  = new ImmutableMap.Builder<>();
        final ImmutableMap.Builder<String, Map<String, Serializable>> basicMetadataMap = new ImmutableMap.Builder<>();
        final  Map<String, Field>  fieldMap   = contentlet.getContentType().fieldMap();
        /*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
        final boolean alwaysRegenerateMetadata = Config
                .getBooleanProperty("always.regenerate.metadata.on.reindex", false);

        Logger.debug(this, ()-> "Generating the metadata for contentlet, id = " + contentlet.getIdentifier());

        // Full MD is stored in disc (FS or DB)
        this.generateFullMetadata (contentlet, fullBinaryFieldNameSet, fullMetadataMap, fieldMap, alwaysRegenerateMetadata);
        //Basic MD lives only in cache
        this.generateBasicMetadata(contentlet, basicBinaryFieldNameSet, fullBinaryFieldNameSet, basicMetadataMap, fieldMap, alwaysRegenerateMetadata);

        return new ContentletMetadata(fullMetadataMap.build(), basicMetadataMap.build());
    }

    /**
     * Basic metadata generation entry point.
     * @param contentlet
     * @param basicBinaryFieldNameSet
     * @param fullBinaryFieldNameSet
     * @param basicMetadataMap
     * @param fieldMap
     * @param alwaysRegenerateMetadata
     * @throws IOException
     */
    private void generateBasicMetadata(final Contentlet contentlet,
                                       final Set<String> basicBinaryFieldNameSet,
                                       final Set<String> fullBinaryFieldNameSet,
                                       final ImmutableMap.Builder<String, Map<String, Serializable>> basicMetadataMap,
                                       final Map<String, Field> fieldMap,
                                       final boolean alwaysRegenerateMetadata)
            throws IOException, DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        Map<String, Serializable> metadataMap;
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
                            metadataKey -> metadataFields.isEmpty() || metadataFields
                                    .contains(metadataKey));
                    contentletCache.addMetadataMap(contentlet.getInode() + StringPool.COLON + basicBinaryFieldName, metadataMap);
                } else {


                    metadataMap = this.fileStorageAPI.generateMetaData(file,
                            new GenerateMetadataConfig.Builder()
                                    .full(false)
                                    .override(alwaysRegenerateMetadata)
                                    .store(true)
                                    .cache(()-> contentlet.getInode() + StringPool.COLON + basicBinaryFieldName)
                                    .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty()
                                            || metadataFields.contains(metadataKey))
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                                    .build()
                    );
                }

                basicMetadataMap.put(basicBinaryFieldName, metadataMap);
            } else {
               //We're dealing with a  non required neither set binary field. No need to throw an exception. Just continue processing.
               Logger.warn(FileMetadataAPIImpl.class, String.format("The binary field : `%s`, is null, does not exists or can not be accessed",basicBinaryFieldName));
            }
        }
    }

    /**
     * Metadata file generator.
     * @param contentlet
     * @param fieldVariableName
     * @return
     */
    private String getFileName (final Contentlet contentlet, final String fieldVariableName) {

        final String inode        = contentlet.getInode();
        final String fileName     = fieldVariableName + "-metadata.json";
        return StringUtils.builder(File.separator,
                inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                fileName).toString();
    }

    private void generateFullMetadata(final Contentlet contentlet,
                                      final Set<String> fullBinaryFieldNameSet,
                                      final ImmutableMap.Builder<String, Map<String, Serializable>> fullMetadataMap,
                                      final Map<String, Field> fieldMap,
                                      final boolean alwaysRegenerateMetadata)
            throws IOException, DotDataException {


        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME,
                DOT_METADATA);
        for (final String fullBinaryFieldName : fullBinaryFieldNameSet) {
            final File file           = contentlet.getBinary(fullBinaryFieldName);
            final String metadataPath = this.getFileName(contentlet, fullBinaryFieldName);
            if (null != file && file.exists() && file.canRead()) {
                final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(fullBinaryFieldName).id());
                final Map<String, Serializable> metadataMap = this.fileStorageAPI.generateMetaData(file,
                        new GenerateMetadataConfig.Builder()
                            .full(true)
                            .override(alwaysRegenerateMetadata)
                            .cache(false)  // do not want cache on full meta
                            .store(true)
                            .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty()
                                    || metadataFields.contains(metadataKey))
                            .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                            .build()
                        );

                fullMetadataMap.put(fullBinaryFieldName, metadataMap);
            } else {

                throw new IOException("The file: " + file + ", is null, does not exists or can not access");
            }
        }
    }

    /**
     * based on the identifier this will give you a set of fields for the metadata generation
     * fields are specific to the CT or preconfigured
     * @param fieldIdentifier
     * @return
     */
    @VisibleForTesting
    Set<String> getMetadataFields (final String fieldIdentifier) {

        final Optional<FieldVariable> customIndexMetaDataFieldsOpt =
                Try.of(()->FactoryLocator.getFieldFactory().byFieldVariableKey(fieldIdentifier, BinaryField.INDEX_METADATA_FIELDS)).getOrElse(Optional.empty());

        final Set<String> metadataFields = customIndexMetaDataFieldsOpt.isPresent()?
                new HashSet<>(Arrays.asList(customIndexMetaDataFieldsOpt.get().value().split(StringPool.COMMA))):
                this.getConfiguredMetadataFields();

        Logger.info(FileMetadataAPIImpl.class,
                () -> String.format(" `%s` has these fields: `%s` ", fieldIdentifier, String
                        .join(",", metadataFields)));
        return metadataFields;
    }

    /**
     * Reads INDEX_METADATA_FIELDS for  pre-configured metadata fields
     * @return
     */
    private Set<String> getConfiguredMetadataFields(){

        final String configFields = Config.getStringProperty("INDEX_METADATA_FIELDS", null);

        return UtilMethods.isSet(configFields)?
            new HashSet<>(Arrays.asList( configFields.split(StringPool.COMMA))):
            Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     * @param contentlet {@link Contentlet}
     * @return
     * @throws IOException
     */
    @Override
    public ContentletMetadata generateContentletMetadata(final Contentlet contentlet)
            throws IOException, DotDataException {

        final Tuple2<Set<String>, Set<String>> binaryFields = this.findBinaryFields(contentlet);
        return this.generateContentletMetadata(contentlet, binaryFields._1(), binaryFields._2());
    }

    /**
     * {@inheritDoc}
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @return
     */
    @Override
    public Map<String, Serializable> getMetadata(final Contentlet contentlet, final Field field)
            throws DotDataException {

        return this.getMetadata(contentlet, field.variable());
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Map<String, Serializable> getMetadata(final Contentlet contentlet,final  String fieldVariableName)
            throws DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final String metadataPath       = this.getFileName(contentlet, fieldVariableName);

        return this.fileStorageAPI.retrieveMetaData(
                new RequestMetadata.Builder()
                        .wrapMetadataMapForCache(this::filterNonCacheableMetadataFields)
                        .cache(()-> contentlet.getInode() + StringPool.COLON + fieldVariableName)
                        .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                        .build()
        );
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Map<String, Serializable> getMetadataNoCache(final Contentlet contentlet,
            final String fieldVariableName) throws DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final String metadataPath = this.getFileName(contentlet, fieldVariableName);

        return this.fileStorageAPI.retrieveMetaData(
                new RequestMetadata.Builder()
                        .cache(false)
                        .wrapMetadataMapForCache(this::filterNonCacheableMetadataFields) // why is it needed for the non-cached version??
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName).path(metadataPath)
                                        .storage(storageType).build())
                        .build()
        );
    }

    /**
     * filters exclude non-basic metadata fields
     * @param originalMap
     * @return
     */
    private Map<String, Serializable> filterNonCacheableMetadataFields(final Map<String, Serializable> originalMap) {

        return originalMap.entrySet().stream().filter(entry -> BASIC_METADATA_FIELDS
                .contains(entry.getKey())).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * This separates binaries in two sets candidates for the full meta and regular basic metadata
     * @param contentlet
     * @return
     */
    @VisibleForTesting
    Tuple2<Set<String>, Set<String>> findBinaryFields(final Contentlet contentlet) {

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
