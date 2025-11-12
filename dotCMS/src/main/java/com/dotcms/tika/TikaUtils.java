package com.dotcms.tika;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.osgi.OSGIConstants;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.storage.model.ExtendedMetadataFields;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.framework.OSGISystem;

public class TikaUtils {

    public static final int SIZE = 1024;
    public static final int DEFAULT_META_DATA_MAX_SIZE = 5;
    private final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private TikaProxyService tikaService;
    private boolean osgiInitialized;

    public TikaUtils() throws DotDataException {

        OSGISystem.getInstance().initializeFramework();
        osgiInitialized = true;

        //Search for the TikaServiceBuilder service instance expose through OSGI
        TikaServiceBuilder tikaServiceBuilder = null;
        try {
            tikaServiceBuilder = OSGISystem.getInstance()
                    .getService(TikaServiceBuilder.class,
                            OSGIConstants.BUNDLE_NAME_DOTCMS_TIKA);

            if (null == tikaServiceBuilder) {

                Logger.error(this.getClass(),
                        String.format("OSGI Service [%s] not found for bundle [%s]",
                                TikaServiceBuilder.class,
                                OSGIConstants.BUNDLE_NAME_DOTCMS_TIKA));
            }
        } catch (Exception e) {
            Logger.error(this.getClass(),
                    String.format("Failure retrieving OSGI Service [%s] in bundle [%s]",
                            TikaServiceBuilder.class,
                            OSGIConstants.BUNDLE_NAME_DOTCMS_TIKA),
                    e);
        }

        if (null == tikaServiceBuilder) {
            osgiInitialized = false;
            return;
        }

        /*
        Creating a new instance of the TikaProxyService in order to use the Tika services exposed in OSGI,
        when the createTikaService method is called a new instance of Tika is also created
        by the TikaProxyService implementation.
         */
        this.tikaService = tikaServiceBuilder.createTikaService();

    }

    /**
     * This method takes a file and uses tika to parse the metadata from it. It
     * returns a Map of the metadata <strong>BUT this method won't try to create any metadata file
     * if does not exist or to override the existing metadata file for the given Contentlet and
     * beside that will put in memory the given file content before to parse it</strong>.
     * @deprecated
     *   This method is no longer acceptable to compute metadata.
     *   <p> Use {@link Contentlet#getBinaryMetadata(Field)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#generateContentletMetadata(Contentlet)} instead.
     *
     * @param inode Contentlet owner of the file to parse
     * @param binFile File to parse the metadata from it
     */
    @Deprecated
    public Map<String, String> getMetaDataMapForceMemory(final String inode, final File binFile) {
        return getMetaDataMap(inode, binFile, true);
    }

    /**
     * This method takes a file and uses tika to parse the metadata from it. It
     * returns a Map of the metadata and creates a metadata file for the given
     * Contentlet if does not already exist, if already exist only the metadata is returned and no
     * file is override.
     * @deprecated
     *      * This method is no longer acceptable to compute metadata.
     *      * <p> Use {@link Contentlet#getBinaryMetadata(Field)} instead.
     *
     * @param inode Contentlet owner of the file to parse
     * @param binFile File to parse the metadata from it
     */
    @Deprecated
    public Map<String, String> getMetaDataMap(final String inode, final File binFile) {
        return getMetaDataMap(inode, binFile, false);
    }

    /**
     * Verifies if the Contentlet is a File asset in order to identify if it
     * is missing a metadata file, if the metadata does not exist this method
     * parses the file asset and generates it, <strong>this operation also implies a save
     * operation to the Contentlet in order to save the parsed metadata info</strong>.
     * @deprecated
     *   This method is no longer acceptable to compute metadata.
     *   <p> Use {@link Contentlet#getBinaryMetadata(Field)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#generateContentletMetadata(Contentlet)} instead.
     *
     * @param contentlet Content parse in order to extract the metadata info
     * @return True if a metadata file was generated.
     */
    @Deprecated
    public boolean generateMetaData(Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        return generateMetaData(contentlet, false);
    }

    /**
     * Verifies if the Contentlet is a File asset in order to parse it and generate a metadata
     * file for it, <strong>this operation also implies a save operation to the Contentlet
     * in order to save the parsed metadata info</strong>.
     * @deprecated
     *   This method is no longer acceptable to compute metadata.
     *   <p> Use {@link Contentlet#getBinaryMetadata(Field)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#generateContentletMetadata(Contentlet)} instead.
     * @param contentlet Content parse in order to extract the metadata info
     * @return True if a metadata file was generated.
     */
    @Deprecated
    public Map<String, Object> generateMetaDataForce(final Contentlet contentlet, final File binaryField,
                                                     final String fieldVariableName, final Set<String> metadataFields) {

        return this.generateMetaData(contentlet, binaryField, fieldVariableName, metadataFields, true);
    }

    /**
     * Verifies if the Contentlet is a File asset in order to parse it and generate a metadata
     * file for it, <strong>this operation also implies a save operation to the Contentlet
     * in order to save the parsed metadata info</strong>.
     *
     * @deprecated
     *   This method is no longer acceptable to compute metadata.
     *   <p> Use {@link Contentlet#getBinaryMetadata(Field)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#generateContentletMetadata(Contentlet)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#getFullMetadataNoCache(File, Supplier)} instead.
     * @param contentlet Content parse in order to extract the metadata info
     * @return True if a metadata file was generated.
     */
    @Deprecated
    public Map<String, Object> generateMetaData(final Contentlet contentlet, final File binaryField,
                                                final String fieldVariableName, final Set<String> metadataFields) {

        return this.generateMetaData(contentlet, binaryField, fieldVariableName, metadataFields, false);
    }

    @CloseDBIfOpened
    private Map<String, Object> generateMetaData(final Contentlet contentlet, final File binaryField, final String fieldVariableName,
                                                 final Set<String> metadataFields, final boolean force) {

        //See if we have content metadata file
        Map<String, Object> metaDataMap = Collections.emptyMap();
        final String fileName           = fieldVariableName + "-metadata.json";
        
        // creates something like /1/2/12421124-15652532-235325-12312/fileAsset-metadata.json
        final File contentMetaFile =
                        APILocator.getFileAssetAPI().getContentMetadataFile(contentlet.getInode(), fileName);

        /*
        If we want to force the parse of the file and the generation of the metadata file
        we need to delete the existing one first.
         */
        if (force) {
            try {
                contentMetaFile.delete();
            } catch (Exception e) {
                Logger.error(this.getClass(),
                        String.format("Unable to delete existing metadata file [%s] [%s]",
                                contentMetaFile.getAbsolutePath(), e.getMessage()), e);
            }
        }

        //If the metadata file does not exist we need to parse and get the metadata for the file
        if (!contentMetaFile.exists()) {

            if (binaryField != null) {

                final int maxLength = Config.getIntProperty("META_DATA_MAX_SIZE",
                        DEFAULT_META_DATA_MAX_SIZE) * SIZE;
                //Parse the metadata from this file
                metaDataMap =
                        this.getForcedMetaDataMap(binaryField, metadataFields, maxLength);

                this.writeCompressJsonMetadataFile (contentMetaFile,
                        UtilMethods.isSet(metaDataMap)?metaDataMap:Collections.emptyMap());
            }

        } else {

            metaDataMap = this.readCompressedJsonMetadataFile (contentMetaFile);
        }

        return metaDataMap;
    }


    private Map<String, Object> readCompressedJsonMetadataFile(final File contentMetaFile) {

        Map<String, Object> objectMap = Collections.emptyMap();
        // compressor config
        final String compressor = Config
                .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");

        try (InputStream inputStream = FileUtil.createInputStream(contentMetaFile.toPath(), compressor)) {

            objectMap   = objectMapper.readValue(inputStream, Map.class);
            Logger.debug(this, "Metadata read from: " + contentMetaFile);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }

        return objectMap;
    }

    private void writeCompressJsonMetadataFile(final File contentMetaFile, final Map<?, Object> objectMap) {

        // compressor config
        final String compressor = Config
                .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");

        if (!contentMetaFile.getParentFile().exists()) {

            contentMetaFile.getParentFile().mkdirs();
        }

        try (OutputStream out = FileUtil.createOutputStream(contentMetaFile.toPath(), compressor)){

            objectMapper.writeValue(out, objectMap);

            out.flush();
            Logger.info(this, "Metadata wrote on: " + contentMetaFile);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }
    }

    /**
     * Verifies if the Contentlet is a File asset in order to parse it and generate a metadata
     * file for it, <strong>this operation also implies a save operation to the Contentlet
     * in order to save the parsed metadata info</strong>.
     * @deprecated
     *   This method is no longer acceptable to compute metadata.
     *   <p> Use {@link Contentlet#getBinaryMetadata(Field)}
     *   or {@link com.dotcms.storage.FileMetadataAPI#generateContentletMetadata(Contentlet)} instead.
     *
     * @param contentlet Content parse in order to extract the metadata info
     * @param force If <strong>false</strong> we will try to parse and generate the metadata file
     * only if a metadata file does NOT already exist. If <strong>true</strong> we delete the
     * existing metadata file in order to force a parse and generation of the metadata file.
     * @return True if a metadata file was generated.
     */
    @Deprecated
    @CloseDBIfOpened
    public boolean generateMetaData(Contentlet contentlet, boolean force)
            throws DotSecurityException, DotDataException {

        if (BaseContentType.FILEASSET.equals(contentlet.getContentType().baseType())) {

            //See if we have content metadata file
            final File contentMeta = APILocator.getFileAssetAPI()
                    .getContentMetadataFile(contentlet.getInode());

            /*
            If we want to force the parse of the file and the generation of the metadata file
            we need to delete the existing one first.
             */
            if (force && contentMeta.exists()) {
                try {
                    contentMeta.delete();
                } catch (Exception e) {
                    Logger.error(this.getClass(),
                            String.format("Unable to delete existing metadata file [%s] [%s]",
                                    contentMeta.getAbsolutePath(), e.getMessage()), e);
                }
            }

            //If the metadata file does not exist we need to parse and get the metadata for the file
            if (!contentMeta.exists()) {

                final File binFile = APILocator.getContentletAPI()
                        .getBinaryFile(contentlet.getInode(), FileAssetAPI.BINARY_FIELD,
                                APILocator.getUserAPI().getSystemUser());
                if (binFile != null) {

                    //Parse the metadata from this file
                    final Map<String, String> metaData = getMetaDataMap(contentlet.getInode(), binFile);
                    if (null != metaData) {

                        //Testing indicates that jackson does not escape html by default
                        final String json = Try.of(()->objectMapper.writeValueAsString(metaData)).getOrElseThrow(DotDataException::new);
                        contentlet.setProperty(FileAssetAPI.META_DATA_FIELD, json);

                        //Save the parsed metadata to the contentlet
                        FactoryLocator.getContentletFactory().save(contentlet);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Similar as {@link #getMetaDataMap(String, File, boolean)} but includes the metadata fields to filter from the tika collection
     * and the max length of the binary file to parse.
     * Also, it is not storing anything on the file system as the reference method {@link #getMetaDataMap(String, File, boolean)}
     * This one does everything on memory, means forceMemory is always true and the file system cache has to be performed on upper layers
     */
    private Map<String, Object> getForcedMetaDataMap(final File binFile,
                                               final Set<String> metadataFields,
                                               final int maxLength) {

        final Map<String, Object> metaMap = this.getForcedMetaDataMap(binFile, maxLength);

        this.filterMetadataFields(metaMap, metadataFields);

        return metaMap;
    }

    /**
     * Similar as {@link #getMetaDataMap(String, File, boolean)} but includes tika collection
     * and the max length of the binary file to parse.
     * Also, it is not storing anything on the file system as the reference method {@link #getMetaDataMap(String, File, boolean)}
     * This one does everything on memory, means forceMemory is always true and the file system cache has to be performed on upper layers
     */
    public Map<String, Object> getForcedMetaDataMap(final File binFile,
                                                    final int maxLength) {

        if (!osgiInitialized) {
            Logger.error(this.getClass(),
                    "Unable to get file Meta Data, OSGI Framework not initialized");
            return Collections.emptyMap();
        }

        final Map<String, Object> metaMap = new TreeMap<>();
        this.tikaService.setMaxStringLength(maxLength);

        try (InputStream stream = Files.newInputStream(binFile.toPath())) {
            // no worry about the limit and less time to process.
            final String content = this.tikaService.parseToString(stream);

            //Creating the meta data map to use by our content
            metaMap.putAll(this.buildMetaDataMap());
            metaMap.put(FileAssetAPI.CONTENT_FIELD, content);

            //Adding missing keys that were excluded in Tika 2.0
            includeMissingKeys(metaMap);
        } catch (IOException ioExc) {
            if (this.isZeroByteFileException(ioExc.getCause())) {
                logWarning(binFile, ioExc.getCause());
            } else {

                this.parseFallbackAsPlainText(binFile, metaMap, ioExc);
            }
        } catch (Throwable e) {

            logWarnDebug(binFile, e);

        } finally {
            metaMap.put(FileAssetAPI.SIZE_FIELD, binFile.length());
            metaMap.put("length", binFile.length());
        }

        return metaMap;
    }

    /**
     * This method adds missing keys from Tika 1.x that were excluded in Tika 2.0. For example: keywords and title
     * For further details, please visit https://cwiki.apache.org/confluence/display/TIKA/Migrating+to+Tika+2.0.0
     * @param metaMap
     */
    private static void includeMissingKeys(Map<String, Object> metaMap) {
        ExtendedMetadataFields.keyMap().forEach((key, value) -> {
            if(metaMap.containsKey(key)){
                value.forEach(v -> metaMap.putIfAbsent(v, metaMap.get(key)));
            }
        });
    }

    private void parseFallbackAsPlainText(final File binFile, final Map<String, Object> metaMap, final IOException ioExc) {
        try {
            //On error lets try a fallback operation
            final String errorMessage = String
                    .format("Error Reading Tika parsed Stream for file [%s] [%s] ",
                            binFile.getAbsolutePath(),
                            UtilMethods.isSet(ioExc.getMessage()) ? ioExc.getMessage()
                                    : ioExc.getCause().getMessage());
            Logger.warnAndDebug(this.getClass(), errorMessage, ioExc);

            try (InputStream stream = Files.newInputStream(binFile.toPath())) {
                //Parse the content as plain text in order to avoid validation errors
                final String content = this.tikaService.parseToStringAsPlainText(stream);

                //Creating the meta data map to use by our content
                metaMap.putAll(this.buildMetaDataMap());
                metaMap.put(FileAssetAPI.CONTENT_FIELD, content);
            }
        } catch (Exception e) {
            logWarnDebug(binFile, e);
        }
    }

    /**
     * Right now the method use the Tika facade directly for parse the document without any kind of restriction about the parser because the
     * new Tika().parse method use the AutoDetectParser by default.
     *
     * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
     *
     * May 31, 2013 - 12:27:19 PM
     */
    private Map<String, String> getMetaDataMap(final String inode, final File binFile,
            boolean forceMemory) {

        if (!osgiInitialized) {
            Logger.error(this.getClass(),
                    "Unable to get file Meta Data, OSGI Framework not initialized");
            return new HashMap<>();
        }

        this.tikaService.setMaxStringLength(-1);

        Map<String, String> metaMap = new HashMap<>();

        // Search for the stored content metadata on disk
        File contentMetadataFile = APILocator.getFileAssetAPI().getContentMetadataFile(inode);

        // if the limit is not "unlimited"
        // I can use the faster parseToString
        try {

            if (forceMemory) {

                try (InputStream stream = Files.newInputStream(binFile.toPath())) {
                    // no worry about the limit and less time to process.
                    final String content = this.tikaService.parseToString(stream);

                    //Creating the meta data map to use by our content
                    metaMap = buildMetaDataMap();
                    metaMap.put(FileAssetAPI.CONTENT_FIELD, content);
                }

            } else {

                try (InputStream is = this.tikaService.tikaInputStreamGet(binFile);
                        Reader fulltext = this.tikaService.parse(is)) {

                    //Write the parsed info into the metadata file
                    metaMap = writeMetadata(fulltext, contentMetadataFile);
                }

            }
        } catch (IOException ioExc) {
            if (isZeroByteFileException(ioExc.getCause())) {
                logWarning(binFile, ioExc.getCause());
            } else {
                try {
                    //On error lets try a fallback operation
                    metaMap = fallbackParse(binFile, contentMetadataFile, ioExc);
                } catch (Exception e) {
                    logWarnDebug(binFile, e);
                }
            }
        } catch (Throwable e) {

            if (isZeroByteFileException(e)) {
                logWarning(binFile, e);
            } else {
                logWarnDebug(binFile, e);
            }
        } finally {
            metaMap.put(FileAssetAPI.SIZE_FIELD, String.valueOf(binFile.length()));
        }

        filterMetadataFields(metaMap, getConfiguredMetadataFields());

        return metaMap;
    }

    /**
     * Reads INDEX_METADATA_FIELDS from configuration
     * @return
     */
    public static Set<String> getConfiguredMetadataFields(){
        final String configFields=Config.getStringProperty("INDEX_METADATA_FIELDS", null);

        if (UtilMethods.isSet(configFields)) {

            return new HashSet<>(Arrays.asList( configFields.split(",")));

        }
        return Collections.emptySet();
    }

    /**
     * Filters fields from a map given a set of fields to be kept
     * @param metaMap
     * @param configFieldsSet
     */
    public static void filterMetadataFields(final Map<String, ?> metaMap, final Set<String> configFieldsSet){

        if (UtilMethods.isSet(metaMap) && UtilMethods.isSet(configFieldsSet)) {
            metaMap.entrySet()
                    .removeIf(entry -> !configFieldsSet.contains("*")
                            && !checkIfFieldMatches(entry.getKey(), configFieldsSet));
        }
    }

    /**
     * Verifies if a string matches in a set of regex/strings
     * @param key
     * @param configFieldsSet
     * @return
     */
    private static boolean checkIfFieldMatches(final String key, final Set<String> configFieldsSet){
        final Predicate<String> condition = e -> key.matches(e);
        return configFieldsSet.stream().anyMatch(condition);
    }

    /**
     * Writes the content of a given Reader into the Contentlet metadata file
     * TODO: Probably this won't be needed any longer consider removing it and all code using it.
     */
    private Map<String, String> writeMetadata(Reader fullText, File contentMetadataFile)
            throws IOException {

        final char[] buf = new char[SIZE];
        int count = fullText.read(buf);

        if (count > 0 && !contentMetadataFile.exists()) {

            //Create the new content metadata file
            prepareMetaDataFile(contentMetadataFile);

            OutputStream out = Files.newOutputStream(contentMetadataFile.toPath());

            // compressor config
            final String compressor = Config
                    .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
            if ("gzip".equals(compressor)) {
                out = new GZIPOutputStream(out);
            } else if ("bzip2".equals(compressor)) {
                out = new BZip2CompressorOutputStream(out);
            }

            try {

                byte[] bytes;
                final int metadataLimit =
                        Config.getIntProperty("META_DATA_MAX_SIZE",
                                DEFAULT_META_DATA_MAX_SIZE) * SIZE * SIZE;
                int numOfChunks = metadataLimit / SIZE;

                do {
                    String lowered = new String(buf);
                    lowered = lowered.toLowerCase();
                    bytes = lowered.getBytes(StandardCharsets.UTF_8);
                    out.write(bytes, 0, bytes.length);
                    numOfChunks--;
                } while ((count = fullText.read(buf)) > 0 && numOfChunks > 0);
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            /*
            Create an empty file if count == 0, there is no content but it is a record
            that we already try to process this file. If the file already exist do nothing
             */
            if (!contentMetadataFile.exists()) {
                prepareMetaDataFile(contentMetadataFile);
                FileUtils.writeStringToFile(contentMetadataFile, "NO_METADATA");
            }
        }

        //Creating the meta data map to use by our content
        return buildMetaDataMap();
    }

    /**
     * Fallback method used in cases when a file can not be parsed properly like for example
     * malformed xml files, a malformed xml file will throw a parsing exception.
     * </br>
     * This fallback operation will read the given file as a plain text file in order to avoid
     * validation errors.
     */
    private Map<String, String> fallbackParse(final File binFile, File contentMetadataFile,
            final Exception ioExc)
            throws Exception {

        final String errorMessage = String
                .format("Error Reading Tika parsed Stream for file [%s] [%s] - "
                                + "Executing fallback in order to parse the file "
                                + "as a plain text file.",
                        binFile.getAbsolutePath(),
                        UtilMethods.isSet(ioExc.getMessage()) ? ioExc.getMessage()
                                : ioExc.getCause().getMessage());
        Logger.warn(this.getClass(), errorMessage);
        Logger.debug(this.getClass(), errorMessage, ioExc);

        try (InputStream stream = Files.newInputStream(binFile.toPath())) {
            //Parse the content as plain text in order to avoid validation errors
            final String content = this.tikaService.parseToStringAsPlainText(stream);

            try (Reader contentReader = new StringReader(content)) {
                //Write the parsed info into the metadata file
                return writeMetadata(contentReader, contentMetadataFile);
            }
        }
    }

    /**
     * Detects the media type of the given file. The type detection is
     * based on the document content and a potential known file extension.
     *
     * @param file the file
     * @return detected media type
     * @throws IOException if the file can not be read
     */
    public String detect(File file) throws IOException {
        if (!osgiInitialized) {
            Logger.error(this.getClass(),
                    "Unable to get file media type, OSGI Framework not initialized");
            return "";
        }

        return this.tikaService.detect(file);
    }

    private Map<String, String> buildMetaDataMap() {

        Map<String, String> metaMap = new HashMap<>();
        for (int i = 0; i < this.tikaService.metadataNames().length; i++) {
            String name = this.tikaService.metadataNames()[i];
            if (UtilMethods.isSet(name) && this.tikaService.metadataGetName(name) != null) {
                // we will want to normalize our metadata for searching
                String[] x = translateKey(name);
                for (String y : x) {
                    metaMap.put(y, this.tikaService.metadataGetName(name));
                }
            }
        }

        return metaMap;
    }

    /**
     * Creates the metadata file where the parsed info will be stored
     */
    private void prepareMetaDataFile(final File contentMetadataFile) throws IOException {

        if (!contentMetadataFile.exists()) {
            //Create the file if does not exist
            contentMetadataFile.getParentFile().mkdirs();
            contentMetadataFile.createNewFile();
        }
    }

    /**
     * normalize metadata from various filetypes this method will return an
     * array of metadata keys that we can use to normalize the values in our
     * fileAsset metadata For example, tiff:ImageLength = "height" for image
     * files, so we return {"tiff:ImageLength", "height"} and both metadata are
     * written to our metadata field
     */
    private String[] translateKey(String key) {
        String[] x = getTranslationMap().get(key);
        if (x == null) {
            x = new String[]{StringUtils.camelCaseLower(key)};
        }
        return x;
    }

    private Map<String, String[]> translateMeta = null;

    private Map<String, String[]> getTranslationMap() {
        if (translateMeta == null) {
            synchronized ("translateMeta".intern()) {
                if (translateMeta == null) {
                    translateMeta = new HashMap<>();
                    translateMeta.put("tiff:ImageWidth", new String[]{"tiff:ImageWidth", "width"});
                    translateMeta
                            .put("tiff:ImageLength", new String[]{"tiff:ImageLength", "height"});
                }
            }
        }
        return translateMeta;
    }

    private boolean isZeroByteFileException(Throwable exception) {
        return null != exception && exception.getClass().getCanonicalName()
                .equals(TikaProxyService.EXCEPTION_ZERO_BYTE_FILE_EXCEPTION);
    }

    private void logWarning(final File binFile, Throwable exception) {
        Logger.warn(this.getClass(),
                String.format("Could not parse file metadata for file [%s] [%s]",
                        binFile.getAbsolutePath(), exception.getMessage()));
    }

    private void logError(final File binFile, Throwable exception) {
        Logger.error(this.getClass(),
                String.format("Could not parse file metadata for file [%s] [%s]",
                        binFile.getAbsolutePath(), exception.getMessage()), exception);
    }

    private void logWarnDebug(final File binFile, Throwable exception) {
        Logger.warnAndDebug(this.getClass(),
                String.format("Could not parse file metadata for file [%s] [%s]",
                        binFile.getAbsolutePath(), exception.getMessage()), exception);
    }

}
