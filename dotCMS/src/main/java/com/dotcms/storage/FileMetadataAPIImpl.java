package com.dotcms.storage;

import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.storage.model.BasicMetadataFields;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.MetadataCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.Ordering;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final MetadataCache metadataCache;

    public FileMetadataAPIImpl() {
        this(APILocator.getFileStorageAPI(), CacheLocator.getMetadataCache());
    }

    @VisibleForTesting
    FileMetadataAPIImpl(final FileStorageAPI fileStorageAPI, final MetadataCache metadataCache) {
        this.fileStorageAPI = fileStorageAPI;
        this.metadataCache = metadataCache;
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

    /**
     * Returns the full and basic metadata for the binaries passed in the parameters.
     * Keep in mind that the full metadata won't be stored on the cache, but it will be stored in the selected persistence media
     * so full metadata for the fullBinaryFieldNameSet will stores on the cache the basic metadata instead of the full one (but the file system will keep the full one)
     *
     * Note: if the basicBinaryFieldNameSet has field which is also include on the fullBinaryFieldNameSet, it will be skipped.
     * @param contentlet Contentlet
     * @param basicBinaryFieldNameSet {@link SortedSet} fields to generate basic metadata
     * @param fullBinaryFieldNameSet  {@link SortedSet} fields to generate full metadata
     * @return ContentletMetadata
     */
    private ContentletMetadata generateContentletMetadata(final Contentlet contentlet,
                                                         final SortedSet<String> basicBinaryFieldNameSet,
                                                         final SortedSet<String> fullBinaryFieldNameSet)
            throws IOException, DotDataException {

        final  Map<String, Field> fieldMap = contentlet.getContentType().fieldMap();
        /*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
        final boolean alwaysRegenerateMetadata = Config
                .getBooleanProperty("always.regenerate.metadata.on.reindex", false);

        Logger.debug(this, ()-> "Generating the metadata for contentlet, id = " + contentlet.getIdentifier());

        // Full MD is stored in disc (FS or DB)
        final Map<String, Metadata> fullMetadata = generateFullMetadata(contentlet,
                fullBinaryFieldNameSet, fieldMap, alwaysRegenerateMetadata);
        //Basic MD is also stored in disc but it also lives in cache
        final Map<String, Metadata> basicMetadata = generateBasicMetadata(contentlet,
                basicBinaryFieldNameSet, fullMetadata, fieldMap, alwaysRegenerateMetadata);

        return new ContentletMetadata(fullMetadata, basicMetadata);
    }

    /**
     * Basic metadata generation entry point.
     * @param contentlet
     * @param basicBinaryFieldNameSet
     * @param fullMetadata
     * @param fieldMap
     * @param alwaysRegenerateMetadata
     * @throws IOException
     */
    private Map<String, Metadata> generateBasicMetadata(final Contentlet contentlet,
                                       final Set<String> basicBinaryFieldNameSet,
                                       final Map<String, Metadata> fullMetadata,
                                       final Map<String, Field> fieldMap,
                                       final boolean alwaysRegenerateMetadata)
            throws IOException, DotDataException {


        final ImmutableMap.Builder<String, Metadata> builder = new ImmutableMap.Builder<>();
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        Map<String, Serializable> metadataMap;
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DEFAULT_METADATA_GROUP_NAME);
        for (final String binaryFieldName : basicBinaryFieldNameSet) {

            final File file           = contentlet.getBinary(binaryFieldName);
            final String metadataPath = this.getFileName(contentlet, binaryFieldName);

            if (null != file && file.exists() && file.canRead()) {

                // if already included on the full, the file was already generated, just need to add the basic to the cache.
                final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(binaryFieldName).id());
                final Predicate<String> filterBasicMetadataKey = metadataKey -> metadataFields.isEmpty() || metadataFields.contains(metadataKey);

                if (fullMetadata.containsKey(binaryFieldName)) {

                    final Metadata metadata = fullMetadata.get(binaryFieldName);

                    // if it is included on the full keys, we only have to store the meta in the cache.
                    //metadataMap = this.fileStorageAPI.generateBasicMetaData(file, filterBasicMetadataKey);

                    metadataMap = filterNonCacheableMetadataFields(metadata.toMap());
                    metadataCache.addMetadataMap(contentlet.getInode() + StringPool.COLON + binaryFieldName, metadataMap);

                } else {

                    metadataMap = this.fileStorageAPI.generateMetaData(file,
                            new GenerateMetadataConfig.Builder()
                                    .full(false)
                                    .override(alwaysRegenerateMetadata)
                                    .store(true)
                                    .cache(true)
                                    .cache(()-> contentlet.getInode() + StringPool.COLON + binaryFieldName)
                                    .metaDataKeyFilter(filterBasicMetadataKey)
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                                    .build()
                    );
                }

                builder.put(binaryFieldName, new Metadata( binaryFieldName, metadataMap));
            } else {
               //We're dealing with a  non required neither set binary field. No need to throw an exception. Just continue processing.
               Logger.warn(FileMetadataAPIImpl.class,String.format("The Contentlet named `%s` references a binary field: `%s` that is null, does not exists or can not be access.", contentlet.getTitle(), binaryFieldName));
            }
        }
        return builder.build();
    }

    /**
     * Full metadata generation entry point.
     * @param contentlet
     * @param fullBinaryFieldNameSet
     * @param fieldMap
     * @param alwaysRegenerateMetadata
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private Map<String, Metadata> generateFullMetadata(final Contentlet contentlet,
                                      final Set<String> fullBinaryFieldNameSet,
                                      final Map<String, Field> fieldMap,
                                      final boolean alwaysRegenerateMetadata)
            throws IOException, DotDataException {

        final ImmutableMap.Builder<String, Metadata> builder  = new ImmutableMap.Builder<>();
        final Optional<Map<String, Metadata>> lazyMetadata = contentlet.getLazyMetadata();

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        for (final String binaryFieldName : fullBinaryFieldNameSet) {
            final File file = contentlet.getBinary(binaryFieldName);
            final String metadataPath = getFileName(contentlet, binaryFieldName);
            if (null != file && file.exists() && file.canRead()) {

                final Metadata mergeWithMetadata = lazyMetadata
                        .map(stringMetadataMap -> stringMetadataMap.get(binaryFieldName))
                        .orElse(null);

                final Set<String> metadataFields = getMetadataFields(fieldMap.get(binaryFieldName).id());
                final Map<String, Serializable> metadataMap = fileStorageAPI.generateMetaData(file,
                        new GenerateMetadataConfig.Builder()
                            .full(true)
                            .override(alwaysRegenerateMetadata)
                            .cache(false)  // do not want cache on full meta
                            .store(true)
                            .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty()
                                    || metadataFields.contains(metadataKey))
                            .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                            .mergeWithMetadata(mergeWithMetadata)
                            .build()
                        );

                builder.put(binaryFieldName, new Metadata(binaryFieldName, metadataMap));
            } else {
                Logger.warn(FileMetadataAPIImpl.class,String.format("The Contentlet named `%s` references a binary field: `%s` that is null, does not exists or can not be access.", contentlet.getTitle(), binaryFieldName));
            }
        }
        return builder.build();
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
                getConfiguredMetadataFields();

        Logger.info(FileMetadataAPIImpl.class,
                () -> String.format(" `%s` has these fields: `%s` ", fieldIdentifier, String
                        .join(",", metadataFields)));

        return metadataFields;
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

        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = this.findBinaryFields(contentlet);
        return this.generateContentletMetadata(contentlet, binaryFields._1(), binaryFields._2());
    }

    /**
     * {@inheritDoc}
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @return
     */
    @Override
    public Metadata getMetadata(final Contentlet contentlet, final Field field)
            throws DotDataException {

        return this.getMetadata(contentlet, field.variable(), false);
    }

    /**
     * {@inheritDoc}
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @param forceGenerate @boolean
     * @return
     */
    @Override
    public Metadata getMetadata(final Contentlet contentlet, final Field field, final boolean forceGenerate)
            throws DotDataException {

        return this.getMetadata(contentlet, field.variable(), forceGenerate);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Metadata getMetadata(final Contentlet contentlet,final  String fieldVariableName)
            throws DotDataException {

        return getMetadata(contentlet, fieldVariableName, false);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Metadata getMetadataForceGenerate(final Contentlet contentlet, final String fieldVariableName)
            throws DotDataException {
        return getMetadata(contentlet, fieldVariableName, true);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @param forceGenerate  @boolean
     * @return
     */
    private Metadata getMetadata(final Contentlet contentlet, final String fieldVariableName, final boolean forceGenerate)
            throws DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final String metadataPath       = this.getFileName(contentlet, fieldVariableName);

        Map<String, Serializable> metadataMap = fileStorageAPI.retrieveMetaData(
                new FetchMetadataParams.Builder()
                        .projectionMapForCache(this::filterNonCacheableMetadataFields)
                        .cache(() -> contentlet.getInode() + StringPool.COLON + fieldVariableName)
                        .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                .path(metadataPath).storage(storageType).build())
                        .build()
        );

        if(null != metadataMap){
            return new Metadata(fieldVariableName, metadataMap);
        }

        if(forceGenerate){
            try {
                return generateContentletMetadata(contentlet).getFullMetadataMap().get(fieldVariableName);
            } catch (IOException e) {
                throw new DotDataException(e);
            }
        }

        return null;

    }


    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Metadata getFullMetadataNoCache(final Contentlet contentlet,
            final String fieldVariableName) throws DotDataException {

        return getFullMetadataNoCache(contentlet, fieldVariableName, false);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @Override
    public Metadata getFullMetadataNoCacheForceGenerate(final Contentlet contentlet,
            final String fieldVariableName) throws DotDataException{
        return getFullMetadataNoCache(contentlet, fieldVariableName, true);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @param forceGenerate  @boolean
     * @return
     */
    private Metadata getFullMetadataNoCache(final Contentlet contentlet,
            final String fieldVariableName, final boolean forceGenerate) throws DotDataException {
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final String metadataPath = this.getFileName(contentlet, fieldVariableName);

        Map<String, Serializable> metadataMap = fileStorageAPI.retrieveMetaData(
                new FetchMetadataParams.Builder()
                        .cache(false)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(metadataPath)
                                        .storage(storageType).build())
                        .build()
        );

        if(null != metadataMap){
           return new Metadata(fieldVariableName, metadataMap);
        }

        if(forceGenerate){
            try {
                return generateContentletMetadata(contentlet).getFullMetadataMap().get(fieldVariableName);
            } catch (IOException e) {
                throw new DotDataException(e);
            }
        }
        return null;
    }

    /**
     * filters exclude non-basic metadata fields
     * @param originalMap
     * @return
     */
    private Map<String, Serializable> filterNonCacheableMetadataFields(final Map<String, Serializable> originalMap) {
        final Set<String> basicMetadataFieldsSet = BasicMetadataFields.keySet();
        return originalMap.entrySet().stream().filter(entry -> basicMetadataFieldsSet
                .contains(entry.getKey()) || entry.getKey().startsWith(Metadata.CUSTOM_PROP_PREFIX) ).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * This separates binaries in two sets candidates for the full meta and regular basic metadata
     * @param contentlet
     * @return
     */
    Tuple2<SortedSet<String>, SortedSet<String>> findBinaryFields(final Contentlet contentlet) {

        final List<Field> binaryFields = contentlet.getContentType().fields(BinaryField.class);

        final SortedSet<String> basicBinaryFieldNameSet = new TreeSet<>();
        final SortedSet<String> fullBinaryFieldNameSet  = new TreeSet<>();

        if (isSet(binaryFields)) {
            for (final Field binaryField : binaryFields) {
                if (binaryField.indexed() && fullBinaryFieldNameSet.isEmpty()) {
                    fullBinaryFieldNameSet.add(binaryField.variable());
                }
                basicBinaryFieldNameSet.add(binaryField.variable());
            }
        }

        return Tuple.of(basicBinaryFieldNameSet, fullBinaryFieldNameSet);
    }


    /**
     * This builds a View compiling all basic md
     * @param contentlet
     * @return
     */
    public Optional<Map<String, Metadata>> collectFieldsMetadata(final Contentlet contentlet) {

        final Builder<String, Metadata> builder = new Builder<>(Ordering.natural());
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(contentlet);

        try {
            for (final String basicMetaFieldName : binaryFields._1()) {
                final Metadata metadata = getMetadata(contentlet, basicMetaFieldName);
                if(null != metadata){
                    builder.put(basicMetaFieldName, metadata);
                }
            }

        } catch (DotDataException e) {
            Logger.error(FileMetadataAPIImpl.class, e);
        }

        Optional<Map<String, Metadata>> collected = Optional.empty();
        final SortedMap<String, Metadata> sortedMap = builder.build();
        if(!sortedMap.isEmpty()){
           collected = Optional.of(sortedMap);
        }
        return collected;
    }

    /**
     * Given a contentlet this will iterate over all the binary fields it has and remove the associated metadata
     * @param contentlet
     * @return
     */
    public Map<String, Set<String>> removeMetadata(final Contentlet contentlet) {
        final Map<String,Set<String>> removedMetaPaths = new HashMap<>();
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(
                contentlet);
        final Set<String> fields = Stream
                .concat(binaryFields._1.stream(), binaryFields._2.stream())
                .collect(Collectors.toSet());
        try {
            for (final String basicMetaFieldName : fields) {
                final String metadataPath = getFileName(contentlet, basicMetaFieldName);
                if (this.fileStorageAPI.removeMetaData(
                        new FetchMetadataParams.Builder()
                                .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                        .path(metadataPath).storage(storageType).build()).build()
                )) {
                    removedMetaPaths.computeIfAbsent(metadataBucketName, k -> new HashSet<>()).add(metadataPath);
                }
            }
        } catch (DotDataException e) {
            Logger.error(FileMetadataAPIImpl.class, e);
        }
        return removedMetaPaths;
    }

    /**
     * Given a contentlet this will iterate over all the binary fields it has and remove the associated metadata per the current version (inode)
     * Meaning all other versions of the conentlet will get to keep their own metadata
     * @param contentlet
     * @return
     */
    public Map<String, Set<String>> removeVersionMetadata(final Contentlet contentlet){
        final Map<String,Set<String>> removedMetaPaths = new HashMap<>();
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(
                contentlet);
        final Set<String> fields = Stream
                .concat(binaryFields._1.stream(), binaryFields._2.stream())
                .collect(Collectors.toSet());
        try {
            for (final String basicMetaFieldName : fields) {
                final String metadataPath = getFileName(contentlet, basicMetaFieldName);
                if (this.fileStorageAPI.removeVersionMetaData(
                        new FetchMetadataParams.Builder()
                                .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                        .path(metadataPath).storage(storageType).build()).build()
                )) {
                    removedMetaPaths.computeIfAbsent(metadataBucketName, k -> new HashSet<>()).add(metadataPath);
                }
            }
        } catch (DotDataException e) {
            Logger.error(FileMetadataAPIImpl.class, e);
        }
        return removedMetaPaths;
    }

    /**
     *
     * @param binary
     * @param fallbackContentlet
     * @return
     */
    public Metadata getFullMetadataNoCache(final File binary, final Supplier<Contentlet> fallbackContentlet)
            throws DotDataException {
        final Map<String, Serializable> metaData = fileStorageAPI
                .generateRawFullMetaData(binary, -1);

        if(isSet(metaData)){
            return new Metadata(null, metaData);
        }

        if(null != fallbackContentlet){
              final String firstIndexedBinary = findBinaryFields(fallbackContentlet.get())._2().first();
              return getFullMetadataNoCache(fallbackContentlet.get(), firstIndexedBinary);
        }
        return null;
    }


    /**
     *
     * @param contentlet the contentlet we want to associate the md with
     * @param customAttributesByField
     * @throws DotDataException
     */
    public void putCustomMetadataAttributes(final Contentlet contentlet,
            final Map<String, Map<String,Serializable>> customAttributesByField) throws DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);

       customAttributesByField.forEach((fieldName, customAttributes) -> {

           final String metadataPath = getFileName(contentlet, fieldName);
           try {
                fileStorageAPI.putCustomMetadataAttributes((new FetchMetadataParams.Builder()
                        .cache(() -> contentlet.getInode() + StringPool.COLON + fieldName)
                        .projectionMapForCache(this::filterNonCacheableMetadataFields)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(metadataPath)
                                        .storage(storageType).build())
                        .build()), customAttributes);

           }catch (Exception e){
               Logger.error(FileMetadataAPIImpl.class, "Error saving custom attributes", e);
           }
       });

    }


    /**
     *
     * @param source
     * @param destination
     * @throws DotDataException
     */
    public void copyMetadata(final Contentlet source, final Contentlet destination)
            throws DotDataException {
        if (!source.getContentType().baseType().equals(destination.getContentType().baseType())) {
            throw new DotDataException("Source and destination contentlet are not the same type.");
        }
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);

        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(
                source);

        final Set<String> binaryFieldNames = Stream
                .concat(binaryFields._1.stream(), binaryFields._2.stream())
                .collect(Collectors.toSet());

        for (final String binaryFieldName : binaryFieldNames) {

            final String sourceMetadataPath = getFileName(source, binaryFieldName);
            final Map<String, Serializable> metadataMap = fileStorageAPI.retrieveMetaData(
                    new FetchMetadataParams.Builder()
                            .cache(false)
                            .storageKey(
                                    new StorageKey.Builder().group(metadataBucketName)
                                            .path(sourceMetadataPath)
                                            .storage(storageType).build())
                            .build()
            );

            if (null != metadataMap) {

                final String destMetadataPath = getFileName(destination, binaryFieldName);

                fileStorageAPI.setMetadata(new FetchMetadataParams.Builder()
                        .cache(() -> destination.getInode() + StringPool.COLON + binaryFieldName)
                        .projectionMapForCache(this::filterNonCacheableMetadataFields)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(destMetadataPath)
                                        .storage(storageType).build())
                        .build(), metadataMap);

            }
        }

    }

}
