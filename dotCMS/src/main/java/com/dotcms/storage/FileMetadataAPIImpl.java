package com.dotcms.storage;


import static com.dotmarketing.util.FileUtil.binaryPath;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
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
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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

    //This property is a comma-separated list that contains all the metadata keys that will be stored as basic in addition to the ones defined in {@link BasicMetadataFields}
    public static final String BASIC_METADATA_EXTENDED_KEYS = "BASIC_METADATA_EXTENDED_KEYS";
    private final FileStorageAPI fileStorageAPI;
    private final MetadataCache metadataCache;

    private Lazy<Set<String>> basicMetadataKeySet;

    public FileMetadataAPIImpl() {
        this(APILocator.getFileStorageAPI(), CacheLocator.getMetadataCache());
    }

    private FileMetadataAPIImpl(final FileStorageAPI fileStorageAPI, final MetadataCache metadataCache) {
        this(fileStorageAPI, metadataCache, () -> {
            //we are including additional keys to the basic metadata if `BASIC_METADATA_EXTENDED_KEYS` exists
            String extendedKeys = Config.getStringProperty(BASIC_METADATA_EXTENDED_KEYS, null);
            Set<String> basicMetadataKeys = new HashSet<>(BasicMetadataFields.keyMap().keySet());
            if (UtilMethods.isSet(extendedKeys)){
                basicMetadataKeys.addAll(Arrays.stream(extendedKeys.split(",")).map(String::trim).collect(Collectors.toSet()));
            }
            return basicMetadataKeys;
        }
        );
    }

    private FileMetadataAPIImpl(final FileStorageAPI fileStorageAPI, final MetadataCache metadataCache,
            Supplier<? extends Set<String>> basicMetadataKeySetSupplier) {
        this.fileStorageAPI = fileStorageAPI;
        this.metadataCache = metadataCache;
        this.basicMetadataKeySet = Lazy.of(basicMetadataKeySetSupplier);
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
    private ContentletMetadata internalGenerateContentletMetadata(final Contentlet contentlet,
                                                          final SortedSet<String> basicBinaryFieldNameSet,
                                                          final SortedSet<String> fullBinaryFieldNameSet,
                                                          final boolean overrideMetadata)
            throws IOException, DotDataException {
        final  Map<String, Field> fieldMap = contentlet.getContentType().fieldMap();

        Logger.debug(this, ()-> "Generating the metadata for contentlet, id = " + contentlet.getIdentifier());

        // Full MD is stored in disc (FS or DB)
        final Map<String, Metadata> fullMetadata = generateFullMetadata(contentlet,
                fullBinaryFieldNameSet, fieldMap, overrideMetadata);
        //Basic MD is also stored in disc but it also lives in cache
        final Map<String, Metadata> basicMetadata = generateBasicMetadata(contentlet,
                basicBinaryFieldNameSet, fullMetadata, fieldMap, overrideMetadata);

        return new ContentletMetadata(fullMetadata, basicMetadata);
    }

    /**
     * Basic metadata generation entry point.
     * @param contentlet
     * @param basicBinaryFieldNameSet
     * @param fullMetadata
     * @param fieldMap
     * @param overrideMetadata
     * @throws IOException
     */
    private Map<String, Metadata> generateBasicMetadata(final Contentlet contentlet,
                                       final Set<String> basicBinaryFieldNameSet,
                                       final Map<String, Metadata> fullMetadata,
                                       final Map<String, Field> fieldMap,
                                       final boolean overrideMetadata)
            throws IOException, DotDataException {


        final ImmutableMap.Builder<String, Metadata> builder = new ImmutableMap.Builder<>();
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        Map<String, Serializable> metadataMap;
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DEFAULT_METADATA_GROUP_NAME);
        for (final String binaryFieldName : basicBinaryFieldNameSet) {

            final File file           = contentlet.getBinary(binaryFieldName);
            final String metadataPath = getFileName(contentlet, binaryFieldName);

            if (null != file && file.exists() && file.canRead()) {

                // if already included on the full, the file was already generated, just need to add the basic to the cache.
                final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(binaryFieldName).id());
                final Predicate<String> filterBasicMetadataKey = metadataKey -> metadataFields.isEmpty() || metadataFields.contains(metadataKey);

                if (fullMetadata.containsKey(binaryFieldName)) {

                    final Metadata metadata = fullMetadata.get(binaryFieldName);

                    // if it is included on the full keys, we only have to store the meta in the cache.
                    metadataMap = filterNonBasicMetadataFields(metadata.getMap());
                    metadataCache.addMetadataMap(contentlet.getInode() + StringPool.COLON + binaryFieldName, metadataMap);

                } else {

                    //get Old metadata from cache so we don't loose any custom attributes
                    final Metadata mergeWithMetadata = internalGetGenerateMetadata(contentlet, binaryFieldName,false, false);
                    final String cacheKey = contentlet.getInode() + StringPool.COLON + binaryFieldName;

                    metadataMap = this.fileStorageAPI.generateMetaData(file,
                            new GenerateMetadataConfig.Builder()
                                    .full(false)
                                    .override(overrideMetadata)
                                    .store(true)
                                    .cache(true)
                                    .cache(()-> cacheKey)
                                    .metaDataKeyFilter(filterBasicMetadataKey)
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                                    .mergeWithMetadata(mergeWithMetadata)
                                    .getIfOnlyHasCustomMetadata(this::getIfOnlyHasCustomMetadata)
                                    .build()
                    );
                }

                builder.put(binaryFieldName, new Metadata( binaryFieldName, metadataMap));
            } else {
               //We're dealing with a  non required neither set binary field. No need to throw an exception. Just continue processing.
               Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet named `%s` references a binary field: `%s` that is null, does not exists or can not be access.", contentlet.getTitle(), binaryFieldName));
            }
        }
        return builder.build();
    }

    /**
     * Full metadata generation entry point.
     * @param contentlet
     * @param fullBinaryFieldNameSet
     * @param fieldMap
     * @param overrideMetadata
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private Map<String, Metadata> generateFullMetadata(final Contentlet contentlet,
                                      final Set<String> fullBinaryFieldNameSet,
                                      final Map<String, Field> fieldMap,
                                      final boolean overrideMetadata)
            throws IOException, DotDataException {

        final ImmutableMap.Builder<String, Metadata> builder  = new ImmutableMap.Builder<>();

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        for (final String binaryFieldName : fullBinaryFieldNameSet) {
            final File file = contentlet.getBinary(binaryFieldName);
            final String metadataPath = getFileName(contentlet, binaryFieldName);
            if (null != file && file.exists() && file.canRead()) {

                final Metadata mergeWithMetadata = internalGetGenerateMetadata(contentlet, binaryFieldName, false, false);

                final Set<String> metadataFields = getMetadataFields(fieldMap.get(binaryFieldName).id());
                final Map<String, Serializable> metadataMap = fileStorageAPI.generateMetaData(file,
                        new GenerateMetadataConfig.Builder()
                            .full(true)
                            .override(overrideMetadata)
                            .cache(false)  // do not want cache on full meta
                            .store(true)
                            .metaDataKeyFilter(metadataKey -> metadataFields.isEmpty()
                                    || metadataFields.contains(metadataKey))
                            .storageKey(new StorageKey.Builder().group(metadataBucketName).path(metadataPath).storage(storageType).build())
                            .mergeWithMetadata(mergeWithMetadata)
                            .getIfOnlyHasCustomMetadata(this::getIfOnlyHasCustomMetadata)
                            .build()
                        );

                builder.put(binaryFieldName, new Metadata(binaryFieldName, metadataMap));
            } else {
                Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet named `%s` references a binary field: `%s` that is null, does not exists or can not be access.", contentlet.getTitle(), binaryFieldName));
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
    @CloseDBIfOpened
    Set<String> getMetadataFields (final String fieldIdentifier) {

        final Optional<FieldVariable> customIndexMetaDataFieldsOpt =
                Try.of(()->FactoryLocator.getFieldFactory().byFieldVariableKey(fieldIdentifier, BinaryField.INDEX_METADATA_FIELDS)).getOrElse(Optional.empty());

        final Set<String> metadataFields = customIndexMetaDataFieldsOpt.isPresent()?
                new HashSet<>(Arrays.asList(customIndexMetaDataFieldsOpt.get().value().split(StringPool.COMMA))):
                getConfiguredMetadataFields();

        Logger.debug(FileMetadataAPIImpl.class,
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
        /*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
        final boolean alwaysRegenerateMetadata = Config
                .getBooleanProperty(ALWAYS_REGENERATE_METADATA_ON_REINDEX, false);

        return generateContentletMetadata(contentlet, alwaysRegenerateMetadata);
    }


    /**
     * This version is for internal use and makes sure we can force overriding the generated md
     * @param contentlet
     * @param overrideMetadata
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private ContentletMetadata generateContentletMetadata(final Contentlet contentlet, final boolean overrideMetadata)
            throws IOException, DotDataException {
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(contentlet);
        return internalGenerateContentletMetadata(contentlet, binaryFields._1(), binaryFields._2(), overrideMetadata);
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

        return internalGetGenerateMetadata(contentlet, fieldVariableName, false, false);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return
     */
    @RequestCost(Price.FILE_METADATA_FROM_CACHE)
    @Override
    public Metadata getOrGenerateMetadata(final Contentlet contentlet, final String fieldVariableName)
            throws DotDataException {
        return internalGetGenerateMetadata(contentlet, fieldVariableName, true, true);
    }

    /**
     * {@inheritDoc}
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @param generateIfAbsent  @boolean
     * @return
     */
        private Metadata internalGetGenerateMetadata(final Contentlet contentlet, final String fieldVariableName, final boolean generateIfAbsent, final boolean checkVersion)
            throws DotDataException {

        if(null != contentlet.get(fieldVariableName) && UtilMethods.isSet(contentlet.getInode())) {
            final StorageType storageType = StoragePersistenceProvider.getStorageType();
            final String metadataBucketName = Config
                    .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
            final String metadataPath = getFileName(contentlet, fieldVariableName);

            final Map<String, Serializable> metadataMap = fileStorageAPI.retrieveMetaData(
                    new FetchMetadataParams.Builder()
                            .projectionMapForCache(this::filterNonBasicMetadataFields)
                            .cache(() -> contentlet.getInode() + StringPool.COLON
                                    + fieldVariableName)
                            .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                    .path(metadataPath).storage(storageType).build())
                            .build()
            );

            if (null != metadataMap) {
                //if check version and the stored ver is lower than current version then re-generate
                if (checkVersion) {
                    if (!metadataMap.isEmpty()) {
                        //Now verify versions
                        final Number storedVersionNumber = (Number) metadataMap
                                .getOrDefault(BasicMetadataFields.VERSION_KEY.key(), 0);
                        if (getBinaryMetadataVersion() > storedVersionNumber.intValue()) {
                            //If we find there's a higher version we re-generate the md for all binaries on this contentlet
                            final ContentletMetadata contentletMetadata = Try
                                    .of(() -> generateContentletMetadata(contentlet, true))
                                    .getOrElseThrow(DotDataException::new);
                            return get(contentletMetadata, fieldVariableName);
                        }
                    }
                }
                //version is fine return whatever we got from storage/cache
                return new Metadata(fieldVariableName, metadataMap);
            }

            if (generateIfAbsent) {
                return Try.of (()->generateContentletMetadata(contentlet).getFullMetadataMap().get(fieldVariableName)).getOrElseThrow(
                        DotDataException::new);
            }
        }
        return null;

    }

    /**
     * Given that at this point we dont know exactly if the fieldVariableName corresponds to the first indexed binary (Which would make it part of the FullMetadata)
     * So we check both maps to make sure we're returning the proper entry.
     * @param contentletMetadata
     * @param fieldVariableName
     * @return
     */
    private Metadata get(final ContentletMetadata contentletMetadata, final String fieldVariableName){
        Metadata metadata = null;
        if(contentletMetadata.getFullMetadataMap().get(fieldVariableName)!=null){
           metadata = contentletMetadata.getFullMetadataMap().get(fieldVariableName);
        }
        if(contentletMetadata.getBasicMetadataMap().get(fieldVariableName)!=null){
            metadata = contentletMetadata.getBasicMetadataMap().get(fieldVariableName);
        }
        return metadata;
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
    public Metadata getOrGenerateFullMetadataNoCache(final Contentlet contentlet,
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
        final String metadataPath = getFileName(contentlet, fieldVariableName);

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
    private Map<String, Serializable> filterNonBasicMetadataFields(final Map<String, Serializable> originalMap) {
        return null != originalMap ?
                originalMap.entrySet().stream().filter(entry -> basicMetadataKeySet.get()
                .contains(entry.getKey()) || entry.getKey().startsWith(Metadata.CUSTOM_PROP_PREFIX) ).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue))
                : Map.of();
    }

    /**
     * filters exclude non-custom metadata fields
     * @param originalMap
     * @return
     */
    private Map<String, Serializable> filterNonCustomMetadataFields(final Map<String, Serializable> originalMap) {
        return null != originalMap ?
                originalMap.entrySet().stream().filter(entry -> entry.getKey().startsWith(Metadata.CUSTOM_PROP_PREFIX) ).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue))
                : Map.of();
    }

    /**
     * This method gets you all the custom metadata but only the originalMap only has custom metadata
     * @param originalMap
     * @return
     */
    private Map<String, Serializable> getIfOnlyHasCustomMetadata(final Map<String, Serializable> originalMap) {
        //Filter all non custom metadata
        if(filterNonCustomMetadataFields(originalMap).isEmpty()){
           return ImmutableMap.of();
        }
        //If after having filtered all non-custom metadata we still have something it means we only have custom metadata.
        return originalMap.entrySet().stream().filter(entry -> entry.getKey().startsWith(Metadata.CUSTOM_PROP_PREFIX) ).collect(
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
     * {@inheritDoc}
     * @param contentlet
     * @return
     */
    public Optional<Metadata> getDefaultMetadata(final Contentlet contentlet) {
        return getDefaultMetadata(contentlet, false);
    }

    /**
     * {@inheritDoc}
     * @param contentlet
     * @return
     */
    public Optional<Metadata> getOrGenerateDefaultMetadata(final Contentlet contentlet) {
       return getDefaultMetadata(contentlet, true);
    }

    /**
     * Finds the first indexed binary and returns the metadata
     * @param contentlet
     * @param generateIfAbsent if true the md will be generated in case it is still missing
     * @return
     */
    private Optional<Metadata> getDefaultMetadata(final Contentlet contentlet, final boolean generateIfAbsent) {

        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(
                contentlet);
        final String first = binaryFields._1().first();

        try {
            return Optional.ofNullable(
                    internalGetGenerateMetadata(contentlet, first, generateIfAbsent, false));
        } catch (DotDataException e) {
            Logger.error(FileMetadataAPIImpl.class, e);
        }

        return Optional.empty();
    }

    /**
     * Given a contentlet this will iterate over all the binary fields it has and remove the associated metadata
     * @param contentlet
     * @return
     */
    public Map<String, Set<String>> removeMetadata(final Contentlet contentlet) {
        final Map<String,Set<String>> removedMetaPaths = new HashMap<>();
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        removedMetaPaths.putAll(removeMetadataFromFields(contentlet, storageType, metadataBucketName));
        removedMetaPaths.putAll(removeMetadataFromPaths(contentlet, storageType, metadataBucketName));
        return removedMetaPaths;
    }

    /**
     * Given that depending on the strategy used to remove the CT where the contentlet belongs to
     * we might end up missing the fields' info. Therefore, we need to iterate over the binary fields themselves to compute metadata keys
     * @param contentlet
     * @param storageType
     * @param metadataBucketName
     * @return
     */
    private Map<String, Set<String>> removeMetadataFromPaths(final Contentlet contentlet,
            final StorageType storageType, final String metadataBucketName) {
        final String prefix = APILocator.getFileAssetAPI().getRealAssetsRootPath();
        final String suffix = FileMetadataAPI.METADATA_JSON;
        final Map<String, Set<String>> removedMetaPaths = new HashMap<>();
        final Optional<Path> rootPath = binaryPath(contentlet);
        if (rootPath.isPresent()) {
            try (Stream<Path> walk = Files.walk(rootPath.get())) {
                final Set<String> paths = walk.sorted(Comparator.reverseOrder())
                        .filter(path -> path.toString().endsWith(suffix))
                        .map(path -> path.toString().replace(prefix, File.separator))
                        .collect(Collectors.toSet());

                for (final String path : paths) {
                    if (fileStorageAPI.removeMetaData(
                            new FetchMetadataParams.Builder()
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                            .path(path).storage(storageType).build()).build()
                    )) {
                        removedMetaPaths.computeIfAbsent(metadataBucketName, k -> new HashSet<>())
                                .add(path);
                    }
                }

            } catch (IOException | DotDataException e) {
                Logger.error(ESContentletAPIImpl.class, e.getMessage(), e);
            }
        }
        return removedMetaPaths;
    }

    private Map<String, Set<String>> removeMetadataFromFields(final Contentlet contentlet, final StorageType storageType, final String metadataBucketName) {
        final Map<String,Set<String>> removedMetaPaths = new HashMap<>();
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = findBinaryFields(
                contentlet);
        final Set<String> fields = Stream
                .concat(binaryFields._1.stream(), binaryFields._2.stream())
                .collect(Collectors.toSet());
        try {
            for (final String basicMetaFieldName : fields) {
                final String metadataPath = getFileName(contentlet, basicMetaFieldName);
                if (fileStorageAPI.removeMetaData(
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .forceInsert(true)
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
    * Build a tmp resource path so that temp files will get created under a more suitable location
    */
    private String tempResourcePath(final String tempResourceId){
        return ConfigUtils.getAssetTempPath() + File.separator + tempResourceId + File.separator +  tempResourceId + META_TMP;
    }

    /**
     * {@inheritDoc}
     * @param tempResourceId
     * @param customAttributesByField
     * @throws DotDataException
     */
    public void putCustomMetadataAttributes(final String tempResourceId,
            final Map<String, Map<String,Serializable>> customAttributesByField) throws DotDataException {

        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);

        customAttributesByField.forEach((fieldName, customAttributes) -> {

            try {
                final String tempResourcePath = tempResourcePath(tempResourceId);
                fileStorageAPI.putCustomMetadataAttributes((new FetchMetadataParams.Builder()
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .cache(() -> tempResourcePath)
                        .forceInsert(true)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(tempResourcePath)
                                        .storage(StorageType.FILE_SYSTEM).build())
                        .build()), customAttributes);

            }catch (Exception e){
                Logger.error(FileMetadataAPIImpl.class, "Error saving custom attributes", e);
            }
        });
    }

    /**
     * {@inheritDoc}
     * @param tempResourceId
     * @return
     * @throws DotDataException
     */
    public Optional<Metadata> getMetadata(final String tempResourceId)
            throws DotDataException {

            final StorageType storageType = StoragePersistenceProvider.getStorageType();
            final String metadataBucketName = Config
                    .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
            final String resourcePath = tempResourcePath(tempResourceId);
            Map<String, Serializable> metadataMap = fileStorageAPI.retrieveMetaData(
                    new FetchMetadataParams.Builder()
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .cache(() -> resourcePath)
                        .storageKey(
                            new StorageKey.Builder().group(metadataBucketName)
                                .path(resourcePath)
                                .storage(storageType).build())
                        .build()
            );

            if (null != metadataMap) {
                return  Optional.of(new Metadata(tempResourceId, metadataMap));
            }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * @param source
     * @param destination
     * @throws DotDataException
     */
    public void copyCustomMetadata(final Contentlet source, final Contentlet destination)
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
                // We're copying only custom metadata attributes
                final Map<String, Serializable> filteredMap = filterNonCustomMetadataFields(metadataMap);
                if(filteredMap.isEmpty()) {
                    fileStorageAPI.removeMetaData(
                            new FetchMetadataParams.Builder()
                                    .storageKey(new StorageKey.Builder().group(metadataBucketName)
                                            .path(destMetadataPath).storage(storageType).build()).build()
                    );
                } else {
                    fileStorageAPI.setMetadata(new FetchMetadataParams.Builder()
                            .cache(() -> destination.getInode() + StringPool.COLON
                                    + binaryFieldName)
                            .projectionMapForCache(this::filterNonBasicMetadataFields)
                            .storageKey(
                                    new StorageKey.Builder().group(metadataBucketName)
                                            .path(destMetadataPath)
                                            .storage(storageType).build())
                            .build(), filteredMap);
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     * @param contentlet
     * @param binariesMetadata
     * @throws DotDataException
     */
    @Override
    public void setMetadata(final Contentlet contentlet, final Map<String, Metadata> binariesMetadata) throws DotDataException {
          removeMetadata(contentlet);
          final Set<Field> validFields = contentlet.getContentType().fields(BinaryField.class).stream()
                .filter(field -> contentlet.get(field.variable()) != null)
                .collect(Collectors.toSet());
        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        for (final Field validField : validFields) {
            final Metadata metadata = binariesMetadata.get(validField.variable());
            if(null != metadata){
                final String destMetadataPath = getFileName(contentlet, validField.variable());
                fileStorageAPI.setMetadata(new FetchMetadataParams.Builder()
                        .cache(() -> contentlet.getInode() + StringPool.COLON + validField.variable())
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(destMetadataPath)
                                        .storage(storageType).build())
                        .build(), metadata.getMap());
            }
        }
    }

}
