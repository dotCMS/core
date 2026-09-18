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
import com.dotcms.storage.binary.BinaryAssetReference;
import com.dotcms.storage.model.BasicMetadataFields;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    @RequestCost(Price.FILE_METADATA_FROM_DB)
    private ContentletMetadata internalGenerateContentletMetadata(final Contentlet contentlet,
                                                          final SortedSet<String> basicBinaryFieldNameSet,
                                                          final SortedSet<String> fullBinaryFieldNameSet,
                                                          final boolean overrideMetadata)
            throws IOException, DotDataException {
        if (AssetStorageFeature.isEnabled()) {
            return generateImmutableMetadata(contentlet, basicBinaryFieldNameSet,
                    fullBinaryFieldNameSet, overrideMetadata);
        }
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

    private ContentletMetadata generateImmutableMetadata(final Contentlet contentlet,
            final Set<String> basicFields, final Set<String> fullFields,
            final boolean override) throws DotDataException {
        final Contentlet requested = new Contentlet(contentlet);
        final Map<String, Metadata> full = new HashMap<>();
        final Map<String, Metadata> basic = new HashMap<>();
        final Set<String> fields = new TreeSet<>(basicFields);
        fields.addAll(fullFields);
        for (final String field : fields) {
            if (requested.get(field) == null) {
                continue;
            }
            final StorageKey storageKey = new StorageKey.Builder()
                    .group(Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA))
                    .path(getFileName(requested, field))
                    .storage(StoragePersistenceProvider.getStorageType()).build();
            final Map<String, Serializable> previous = fileStorageAPI.retrieveRawMetaData(storageKey);
            Map<String, Serializable> metadata = previous == null ? Map.of() : previous;
            final boolean generate = override || metadata.isEmpty()
                    || metadata.keySet().stream().allMatch(key -> key.startsWith(Metadata.CUSTOM_PROP_PREFIX)
                            || key.equals(BasicMetadataFields.EDITABLE_AS_TEXT.key()));
            if (generate) {
                final Set<String> indexedKeys = getMetadataFields(contentlet.getContentType().fieldMap().get(field).id());
                try {
                    metadata = new HashMap<>(fileStorageAPI.generateMetaData(
                            () -> Try.of(() -> requested.getBinary(field)).get(),
                            new GenerateMetadataConfig.Builder().full(fullFields.contains(field))
                                    .override(true).store(false).cache(false)
                                    .metaDataKeyFilter(key -> indexedKeys.isEmpty() || indexedKeys.contains(key))
                                    .storageKey(storageKey)
                                    .build()));
                } catch (final IllegalArgumentException missingBinary) {
                    Logger.debug(this, () -> "Cannot generate metadata for missing binary: " + field);
                    continue;
                }
                if (previous != null) {
                    metadata.putAll(filterNonCustomMetadataFields(previous));
                }
                final Map<String, Serializable> generated = metadata;
                // Publish only against the snapshot we read. Reindexing a historical snapshot
                // must not replace newer metadata, even when the binary bytes are unchanged.
                publishMetadata(contentlet, Set.of(field), (snapshot, name) -> generated, requested);
            } else {
                metadataCache.addMetadataMap(getMetadataCacheKey(requested, field),
                        filterNonBasicMetadataFields(metadata));
            }
            if (fullFields.contains(field)) {
                full.put(field, new Metadata(field, metadata));
            }
            if (basicFields.contains(field)) {
                basic.put(field, new Metadata(field, fullFields.contains(field)
                        ? filterNonBasicMetadataFields(metadata) : metadata));
            }
        }
        return new ContentletMetadata(full, basic);
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

            final String metadataPath = getFileName(contentlet, binaryFieldName);

            // A map-level check only — no filesystem stat. The binary itself is resolved
            // lazily, and only when the metadata actually has to be regenerated: stats on
            // network-backed storage are expensive and can hang (issue #36498).
            if (null == contentlet.get(binaryFieldName)) {
                //We're dealing with a  non required neither set binary field. No need to throw an exception. Just continue processing.
                Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet with id `%s` references a binary field: `%s` that is null.", contentlet.getIdentifier(), binaryFieldName));
                continue;
            }

            // if already included on the full, the file was already generated, just need to add the basic to the cache.
            final Set<String> metadataFields = this.getMetadataFields(fieldMap.get(binaryFieldName).id());
            final Predicate<String> filterBasicMetadataKey = metadataKey -> metadataFields.isEmpty() || metadataFields.contains(metadataKey);

            if (fullMetadata.containsKey(binaryFieldName)) {

                final Metadata metadata = fullMetadata.get(binaryFieldName);

                // if it is included on the full keys, we only have to store the meta in the cache.
                metadataMap = filterNonBasicMetadataFields(metadata.getMap());
                metadataCache.addMetadataMap(getMetadataCacheKey(contentlet, binaryFieldName), metadataMap);

            } else {

                //get Old metadata from cache so we don't loose any custom attributes
                final Metadata mergeWithMetadata = internalGetGenerateMetadata(contentlet, binaryFieldName,false, false);
                final String cacheKey = getMetadataCacheKey(contentlet, binaryFieldName);

                try {
                    metadataMap = this.fileStorageAPI.generateMetaData(
                            () -> Try.of(() -> contentlet.getBinary(binaryFieldName)).getOrNull(),
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
                } catch (final IllegalArgumentException missingBinary) {
                    // the metadata had to be regenerated but the binary is missing/unreadable —
                    // same skip-and-continue as the old upfront file checks, minus the stats.
                    Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet with id `%s` references a binary field: `%s` that does not exists or can not be access.", contentlet.getIdentifier(), binaryFieldName));
                    continue;
                }
            }

            builder.put(binaryFieldName, new Metadata( binaryFieldName, metadataMap));
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
            final String metadataPath = getFileName(contentlet, binaryFieldName);

            // A map-level check only — no filesystem stat. The binary itself is resolved
            // lazily, and only when the metadata actually has to be regenerated (issue #36498).
            if (null == contentlet.get(binaryFieldName)) {
                Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet with id `%s` references a binary field: `%s` that is null.", contentlet.getIdentifier(), binaryFieldName));
                continue;
            }

            final Metadata mergeWithMetadata = internalGetGenerateMetadata(contentlet, binaryFieldName, false, false);

            final Set<String> metadataFields = getMetadataFields(fieldMap.get(binaryFieldName).id());
            final Map<String, Serializable> metadataMap;
            try {
                metadataMap = fileStorageAPI.generateMetaData(
                        () -> Try.of(() -> contentlet.getBinary(binaryFieldName)).getOrNull(),
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
            } catch (final IllegalArgumentException missingBinary) {
                // the metadata had to be regenerated but the binary is missing/unreadable —
                // same skip-and-continue as the old upfront file checks, minus the stats.
                Logger.debug(FileMetadataAPIImpl.class,String.format("The Contentlet with id `%s` references a binary field: `%s` that does not exists or can not be access.", contentlet.getIdentifier(), binaryFieldName));
                continue;
            }

            builder.put(binaryFieldName, new Metadata(binaryFieldName, metadataMap));
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
    @RequestCost(Price.FILE_METADATA_FROM_CACHE)
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
                            .cache(() -> getMetadataCacheKey(contentlet, fieldVariableName))
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
                // Generate metadata and check both full and basic metadata maps
                // For indexed fields, metadata will be in fullMetadataMap
                // For non-indexed fields, metadata will be in basicMetadataMap
                final ContentletMetadata contentletMetadata = Try.of(() -> generateContentletMetadata(contentlet))
                        .getOrElseThrow(DotDataException::new);
                final Metadata result = get(contentletMetadata, fieldVariableName);

                if (result == null) {
                    Logger.error(this, String.format(
                            "Unable to generate metadata for field '%s' on contentlet '%s'",
                            fieldVariableName, contentlet.getIdentifier()));
                }

                return result;
            }
        }
        return null;

    }

    /**
     * Given that at this point we don't know exactly if the fieldVariableName corresponds to the first indexed binary (Which would make it part of the FullMetadata)
     * So we check both maps to make sure we're returning the proper entry.
     * @param contentletMetadata
     * @param fieldVariableName
     * @return
     */
    private Metadata get(final ContentletMetadata contentletMetadata, final String fieldVariableName) {
        Metadata metadata = null;
        if (contentletMetadata.getFullMetadataMap().get(fieldVariableName) != null) {
            metadata = contentletMetadata.getFullMetadataMap().get(fieldVariableName);
        }
        if (contentletMetadata.getBasicMetadataMap().get(fieldVariableName) != null) {
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

    /** Removes legacy and revision metadata while source keys are still available for retries. */
    @Override
    public void removeMetadataForInode(final String inode, final List<String> binaryPaths) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return;
        }
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid metadata inode");
        }
        final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/";
        final String group = Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
        final StoragePersistenceAPI storage = StoragePersistenceProvider.INSTANCE.get()
                .getStorage(StoragePersistenceProvider.getStorageType());
        final Set<String> paths = new HashSet<>();
        // Legacy metadata can exist even when its field/source no longer exists.
        for (final String path : storage.listObjectPaths(group, "/" + prefix)) {
            if (path.endsWith(METADATA_JSON)) {
                paths.add(path.startsWith("/") ? path : "/" + path);
            }
        }
        for (final String path : binaryPaths) {
            if (!path.startsWith(prefix) || !Path.of(path).normalize().toString().equals(path)) {
                throw new DotDataException("Binary path escapes metadata cleanup inode: " + path);
            }
            final String relative = path.substring(prefix.length());
            final int slash = relative.indexOf('/');
            if (slash >= 0) {
                paths.add("/" + prefix + relative.substring(0, slash) + METADATA_JSON);
                if (relative.contains("/.revisions/")) {
                    paths.add("/" + path + METADATA_JSON);
                    final String metadataParent = "/" + path.substring(0, path.lastIndexOf('/') + 1);
                    for (final String metadataPath : storage.listObjectPaths(group, metadataParent)) {
                        if (metadataPath.endsWith(METADATA_JSON)) {
                            paths.add(metadataPath.startsWith("/") ? metadataPath : "/" + metadataPath);
                        }
                    }
                }
            }
        }
        for (final String path : paths) {
            if (!path.startsWith("/" + prefix) || !Path.of(path).normalize().toString().equals(path)) {
                throw new DotDataException("Metadata path escapes cleanup inode: " + path);
            }
            storage.deleteObjectAndReferences(group, path);
            if (storage.existsObject(group, path)) {
                throw new DotDataException("Metadata remains after deletion: " + path);
            }
            metadataCache.removeMetadata(path);
        }
    }

    /** Removes metadata associated with a contentlet's binary fields and local metadata paths. */
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

        if (AssetStorageFeature.isEnabled()) {
            publishMetadata(contentlet, customAttributesByField.keySet(), (snapshot, field) -> {
                final Metadata previous = getFullMetadataNoCache(snapshot, field);
                final Map<String, Serializable> metadata = previous == null ? new HashMap<>()
                        : new HashMap<>(previous.getMap());
                final Map<String, Serializable> attributes = customAttributesByField.get(field);
                if (attributes.isEmpty()) {
                    metadata.keySet().removeIf(key -> key.startsWith(Metadata.CUSTOM_PROP_PREFIX));
                } else {
                    attributes.forEach((key, value) -> metadata.put(Metadata.CUSTOM_PROP_PREFIX + key, value));
                }
                return metadata;
            });
            return;
        }
        putCustomMetadataAttributesForCheckin(contentlet, customAttributesByField);
    }

    @Override
    public void putCustomMetadataAttributesForCheckin(final Contentlet contentlet,
            final Map<String, Map<String, Serializable>> customAttributesByField) throws DotDataException {

        final StorageType storageType = StoragePersistenceProvider.getStorageType();
        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);
       for (final var entry : customAttributesByField.entrySet()) {
           final String fieldName = entry.getKey();
           final Map<String, Serializable> customAttributes = entry.getValue();

           final String metadataPath = getFileName(contentlet, fieldName);
           try {
                fileStorageAPI.putCustomMetadataAttributes((new FetchMetadataParams.Builder()
                        .cache(() -> getMetadataCacheKey(contentlet, fieldName))
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .forceInsert(true)
                        .storageKey(
                                new StorageKey.Builder().group(metadataBucketName)
                                        .path(metadataPath)
                                        .storage(storageType).build())
                        .build()), customAttributes);

           }catch (Exception e){
               if (AssetStorageFeature.isEnabled()) {
                   throw new DotDataException("Unable to save binary metadata for " + fieldName, e);
               }
               Logger.error(FileMetadataAPIImpl.class, "Error saving custom attributes", e);
           }
       }

    }

    /**
     * Only the database reference is mutable. Upload failures and rollbacks leave committed
     * metadata intact; the row lock makes concurrent custom-attribute merges read the latest edit.
     */
    @CloseDBIfOpened
    private void publishMetadata(final Contentlet contentlet,
            final Set<String> updatedFields,
            final io.vavr.CheckedFunction2<Contentlet, String, Map<String, Serializable>> update) throws DotDataException {
        publishMetadata(contentlet, updatedFields, update, null);
    }

    private void publishMetadata(final Contentlet contentlet,
            final Set<String> updatedFields,
            final io.vavr.CheckedFunction2<Contentlet, String, Map<String, Serializable>> update,
            final Contentlet expectedSnapshot) throws DotDataException {
        final boolean skipChangedSnapshot = expectedSnapshot != null;
        if (updatedFields.isEmpty()) {
            return;
        }
        final boolean localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
        try {
            final String inode = contentlet.getInode();
            final String previousJson = new DotConnect()
                    .setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                    .addParam(inode).getString("contentlet_as_json");
            final boolean missingJson = previousJson == null || previousJson.isBlank();
            if (missingJson && !skipChangedSnapshot) {
                throw new DotDataException("Cannot edit metadata for missing content: " + inode);
            }
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode json = (ObjectNode) mapper.readTree(missingJson ? "{\"fields\":{}}" : previousJson);
            final ObjectNode fields = (ObjectNode) json.path("fields");
            final Contentlet snapshot = new Contentlet(contentlet);
            final Map<String, File> references = new HashMap<>();
            final Set<String> binaryFields = contentlet.getContentType().fields(BinaryField.class).stream()
                    .map(Field::variable).collect(Collectors.toSet());
            for (final String field : updatedFields) {
                if (!binaryFields.contains(field)
                        || !((skipChangedSnapshot ? expectedSnapshot : contentlet).get(field) instanceof File)) {
                    throw new DotDataException("Cannot edit metadata for an absent binary field: " + field);
                }
                final BinaryAssetReference.StoredBinary stored = BinaryAssetReference.fromJson(fields.path(field), inode, field);
                final File current = stored == null ? null : stored.localFile(inode, field);
                final File requested = (File) (skipChangedSnapshot ? expectedSnapshot : contentlet).get(field);
                if (current == null || !current.toPath().toAbsolutePath().normalize()
                        .equals(requested.toPath().toAbsolutePath().normalize())) {
                    if (skipChangedSnapshot) {
                        continue;
                    }
                    throw new DotDataException("Binary changed before metadata edit: " + field);
                }
                snapshot.getMap().put(field, current);
                if (skipChangedSnapshot && !getFileName(snapshot, field).equals(getFileName(expectedSnapshot, field))) {
                    continue;
                }
                final Map<String, Serializable> metadata = Try.of(() -> update.apply(snapshot, field))
                        .getOrElseThrow(DotDataException::new);
                if (metadata == null) {
                    continue;
                }
                final String key = BinaryAssetReference.newMetadataKey(current, inode, field);
                final boolean storedMetadata = fileStorageAPI.setMetadata(new FetchMetadataParams.Builder()
                        .cache(() -> key)
                        .projectionMapForCache(this::filterNonBasicMetadataFields)
                        .storageKey(new StorageKey.Builder()
                                .group(Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA))
                                .path(key).storage(StoragePersistenceProvider.getStorageType()).build())
                        .build(), metadata);
                if (!storedMetadata) {
                    throw new DotDataException("Metadata was not stored: " + field);
                }
                ((ObjectNode) fields.path(field)).put("metadataStorageKey", key);
                references.put(field, BinaryAssetReference.withMetadata(current, inode, field, key));
            }
            if (!references.isEmpty()) {
                final String updatedJson = mapper.writeValueAsString(json);
                new DotConnect().setSQL("update contentlet set contentlet_as_json = "
                                + (DbConnectionFactory.isPostgres() ? "?::jsonb" : "?") + " where inode = ?")
                        .addParam(updatedJson).addParam(inode).loadResult();
                // ContentletCache may share the supplied object with other requests. Do not mutate
                // it before commit. A writer can find the content again to read its pending reference.
                HibernateUtil.addSyncCommitListener(() -> {
                    CacheLocator.getContentletCache().remove(inode);
                    references.forEach((field, file) -> contentlet.getMap().put(field, file));
                    contentlet.getMap().put(Contentlet.CONTENTLET_AS_JSON, updatedJson);
                });
            }
            if (localTransaction) {
                HibernateUtil.commitTransaction();
            }
        } catch (Exception e) {
            if (localTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            throw new DotDataException("Unable to publish binary metadata", e);
        }
    }

   /**
    * Build a tmp resource path so that temp files will get created under a more suitable location
    */
    private String tempResourcePath(final String tempResourceId){
        return ConfigUtils.getAssetTempPath() + File.separator + tempResourceId + File.separator +  tempResourceId + META_TMP;
    }

    private StorageKey temporaryMetadataKey(final String id, final boolean legacy) {
        if (!TemporaryAssetStorage.validId(id)) {
            throw new IllegalArgumentException("Invalid temporary resource id");
        }
        return new StorageKey.Builder().group(Config.getStringProperty(METADATA_GROUP_NAME, DOT_METADATA))
                .path(legacy ? tempResourcePath(id) : TemporaryAssetStorage.metadataPath(id))
                .storage(legacy ? StorageType.FILE_SYSTEM : StorageType.S3).build();
    }

    private Map<String, Serializable> temporaryMetadata(final String id) throws DotDataException {
        // Temporary metadata is mutable: read S3 directly so another node's edits are visible.
        // Only a genuine absence falls back to pre-feature local metadata, never a storage failure.
        final Map<String, Serializable> shared = fileStorageAPI.retrieveRawMetaData(temporaryMetadataKey(id, false));
        return shared != null ? shared : fileStorageAPI.retrieveRawMetaData(temporaryMetadataKey(id, true));
    }

    /**
     * {@inheritDoc}
     * @param tempResourceId
     * @param customAttributesByField
     * @throws DotDataException
     */
    public void putCustomMetadataAttributes(final String tempResourceId,
            final Map<String, Map<String,Serializable>> customAttributesByField) throws DotDataException {

        if (AssetStorageFeature.isEnabled()) {
            if (customAttributesByField.isEmpty()) {
                return;
            }
            final Map<String, Serializable> previous = temporaryMetadata(tempResourceId);
            final Map<String, Serializable> updated = new HashMap<>(previous == null ? Map.of() : previous);
            for (final Map<String, Serializable> attributes : customAttributesByField.values()) {
                if (attributes.isEmpty()) {
                    updated.keySet().removeIf(key -> key.startsWith(Metadata.CUSTOM_PROP_PREFIX));
                } else {
                    attributes.forEach((key, value) -> updated.put(Metadata.CUSTOM_PROP_PREFIX + key, value));
                }
            }
            // Retain a nonempty record after clearing custom attributes, so a legacy local copy
            // cannot resurrect an earlier focal point on the next read.
            updated.put("tempResourceId", tempResourceId);
            if (!fileStorageAPI.setMetadata(new FetchMetadataParams.Builder().cache(false)
                    .storageKey(temporaryMetadataKey(tempResourceId, false)).build(), updated)) {
                throw new DotDataException("Unable to save temporary binary metadata for " + tempResourceId);
            }
            return;
        }

        final String metadataBucketName = Config
                .getStringProperty(METADATA_GROUP_NAME, DOT_METADATA);

        for (final var entry : customAttributesByField.entrySet()) {
            final Map<String, Serializable> customAttributes = entry.getValue();

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
                if (AssetStorageFeature.isEnabled()) {
                    throw new DotDataException("Unable to save temporary binary metadata for " + tempResourceId, e);
                }
                Logger.error(FileMetadataAPIImpl.class, "Error saving custom attributes", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param tempResourceId
     * @return
     * @throws DotDataException
     */
    public Optional<Metadata> getMetadata(final String tempResourceId)
            throws DotDataException {

            if (AssetStorageFeature.isEnabled()) {
                return Optional.ofNullable(temporaryMetadata(tempResourceId))
                        .map(values -> new Metadata(tempResourceId, values));
            }

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
        if (!AssetStorageFeature.isEnabled()) {
            copyCustomMetadataForCheckin(source, destination);
            return;
        }
        if (!source.getContentType().baseType().equals(destination.getContentType().baseType())) {
            throw new DotDataException("Source and destination contentlet are not the same type.");
        }
        final Map<String, Map<String, Serializable>> copiedAttributes = new HashMap<>();
        for (final Field field : source.getContentType().fields(BinaryField.class)) {
            final String name = field.variable();
            if (source.get(name) == null || destination.get(name) == null
                    || getFileName(source, name).equals(getFileName(destination, name))) {
                continue;
            }
            final Metadata metadata = getFullMetadataNoCache(source, name);
            if (metadata != null) {
                copiedAttributes.put(name, metadata.getCustomMetaWithPrefix());
            }
        }
        publishMetadata(destination, copiedAttributes.keySet(), (snapshot, field) -> {
            final Metadata previous = getFullMetadataNoCache(snapshot, field);
            final Map<String, Serializable> metadata = previous == null ? new HashMap<>()
                    : new HashMap<>(previous.getMap());
            metadata.keySet().removeIf(key -> key.startsWith(Metadata.CUSTOM_PROP_PREFIX));
            metadata.putAll(copiedAttributes.get(field));
            return previous == null && metadata.isEmpty()
                    || previous != null && metadata.equals(previous.getMap()) ? null : metadata;
        });
    }

    @Override
    public void copyCustomMetadataForCheckin(final Contentlet source, final Contentlet destination)
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
            if (AssetStorageFeature.isEnabled() && (destination.get(binaryFieldName) == null
                    || sourceMetadataPath.equals(getFileName(destination, binaryFieldName)))) {
                continue;
            }
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
                            .cache(() -> getMetadataCacheKey(destination, binaryFieldName))
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
          if (AssetStorageFeature.isEnabled()) {
              final Map<String, Map<String, Serializable>> updates = new HashMap<>();
              for (final Field field : contentlet.getContentType().fields(BinaryField.class)) {
                  final Metadata metadata = binariesMetadata.get(field.variable());
                  if (contentlet.get(field.variable()) != null && metadata != null) {
                      updates.put(field.variable(), metadata.getMap());
                  }
              }
              publishMetadata(contentlet, updates.keySet(), (snapshot, field) -> updates.get(field));
              return;
          }
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
                        .cache(() -> getMetadataCacheKey(contentlet, validField.variable()))
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
