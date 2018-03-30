package com.dotcms.tika;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.osgi.OSGIConstants;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.OSGIUtil;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

public class TikaUtils {

    private static final int SIZE = 1024;
    private static final int DEFAULT_META_DATA_MAX_SIZE = 5;

    private TikaProxyService tikaService;
    private Boolean osgiInitialized;
    private User systemUser;

    public TikaUtils() throws DotDataException {

        osgiInitialized = OSGIUtil.getInstance().isInitialized();
        if (osgiInitialized) {

            //Search for the TikaServiceBuilder service instance expose through OSGI
            TikaServiceBuilder tikaServiceBuilder = OSGIUtil.getInstance()
                    .getService(TikaServiceBuilder.class,
                            OSGIConstants.BUNDLE_NAME_DOTCMS_TIKA);
            if (null == tikaServiceBuilder) {
                throw new IllegalStateException(
                        String.format("OSGI Service [%s] not found for bundle [%s]",
                                TikaServiceBuilder.class,
                                OSGIConstants.BUNDLE_NAME_DOTCMS_TIKA));
            }

            this.systemUser = APILocator.getUserAPI().getSystemUser();

            /*
            Creating a new instance of the TikaProxyService in order to use the Tika services exposed in OSGI,
            when the createTikaService method is called a new instance of Tika is also created
            by the TikaProxyService implementation.
             */
            this.tikaService = tikaServiceBuilder.createTikaService();
        } else {
            Logger.error(this.getClass(), "OSGI Framework not initialized");
        }
    }

    /**
     * This method takes a file and uses tika to parse the metadata from it. It
     * returns a Map of the metadata <strong>BUT this method won't try to create any metadata file
     * if does not exist or to override the existing metadata file for the given Contentlet and
     * beside that will put in memory the given file content before to parse it</strong>.
     *
     * @param inode Contentlet owner of the file to parse
     * @param binFile File to parse the metadata from it
     */
    public Map<String, String> getMetaDataMapForceMemory(String inode, File binFile) {
        return getMetaDataMap(inode, binFile, true);
    }

    /**
     * This method takes a file and uses tika to parse the metadata from it. It
     * returns a Map of the metadata and creates a metadata file for the given
     * Contentlet if does not already exist, if already exist only the metadata is returned and no
     * file is override.
     *
     * @param inode Contentlet owner of the file to parse
     * @param binFile File to parse the metadata from it
     */
    public Map<String, String> getMetaDataMap(String inode, File binFile) {
        return getMetaDataMap(inode, binFile, false);
    }

    /**
     * Verifies if the Contentlet is a File asset in order to identify if it
     * is missing a metadata file, if the metadata does not exist this method
     * parses the file asset and generates it, this operation also implies a save
     * operation to the Contentlet in order to save the parsed metadata info.
     *
     * @param contentlet Content to validate if have or not a metadata file
     */
    @CloseDBIfOpened
    public Boolean generateMetaDataIfRequired(Contentlet contentlet)
            throws DotSecurityException, DotDataException {

        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {

            //See if we have content metadata file
            File contentMeta = APILocator.getFileAssetAPI()
                    .getContentMetadataFile(contentlet.getInode());

            //If the metadata file does not exist we need to parse and get the metadata for the file
            if (!contentMeta.exists()) {

                File binFile = APILocator.getContentletAPI()
                        .getBinaryFile(contentlet.getInode(), FileAssetAPI.BINARY_FIELD,
                                APILocator.getUserAPI().getSystemUser());
                if (binFile != null) {

                    //Parse the metadata from this file
                    Map<String, String> metaData = getMetaDataMap(contentlet.getInode(), binFile);
                    if (null != metaData) {
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        contentlet.setProperty(FileAssetAPI.META_DATA_FIELD, gson.toJson(metaData));
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
     * Right now the method use the Tika facade directly for parse the document without any kind of restriction about the parser because the
     * new Tika().parse method use the AutoDetectParser by default.
     *
     * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
     *
     * May 31, 2013 - 12:27:19 PM
     */
    private Map<String, String> getMetaDataMap(String inode, File binFile, boolean forceMemory) {

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

            } else if (!contentMetadataFile
                    .exists()) { //If a metadata file exist we should parse nothing

                try (InputStream is = this.tikaService.tikaInputStreamGet(binFile);
                        Reader fulltext = this.tikaService.parse(is)) {

                    //Write the parsed info into the metadata file
                    metaMap = writeMetadata(fulltext, contentMetadataFile);
                }
            }
        } catch (IOException ioExc) {
            try {
                //On error lets try a fallback operation
                metaMap = fallbackParse(binFile, contentMetadataFile, ioExc);
            } catch (Exception e) {
                logError(binFile, e);
            }
        } catch (Exception e) {
            logError(binFile, e);
        } finally {
            metaMap.put(FileAssetAPI.SIZE_FIELD, String.valueOf(binFile.length()));
        }

        //Getting the original content's map
        Map<String, Object> additionProps = getAdditionalProperties(inode);
        for(Map.Entry<String, Object> entry : additionProps.entrySet()){
            metaMap.put(entry.getKey().toLowerCase(), entry.getValue().toString().toLowerCase());
        }

        return metaMap;
    }

    /**
     * Writes the content of a given Reader into the Contentlet metadata file
     */
    private Map<String, String> writeMetadata(Reader fullText, File contentMetadataFile)
            throws IOException {

        char[] buf = new char[SIZE];
        int count = fullText.read(buf);

        if (count > 0) {

            //Create the new content metadata file
            prepareMetaDataFile(contentMetadataFile);

            OutputStream out = Files.newOutputStream(contentMetadataFile.toPath());

            // compressor config
            String compressor = Config
                    .getStringProperty("CONTENT_METADATA_COMPRESSOR", "none");
            if ("gzip".equals(compressor)) {
                out = new GZIPOutputStream(out);
            } else if ("bzip2".equals(compressor)) {
                out = new BZip2CompressorOutputStream(out);
            }

            try {

                byte[] bytes;
                int metadataLimit =
                        Config.getIntProperty("META_DATA_MAX_SIZE",
                                DEFAULT_META_DATA_MAX_SIZE) * SIZE * SIZE;
                int numOfChunks = metadataLimit / SIZE;

                do {
                    String lowered = new String(buf);
                    lowered = lowered.toLowerCase();
                    bytes = lowered.getBytes(StandardCharsets.UTF_8);
                    out.write(bytes, 0, count);
                    numOfChunks--;
                } while ((count = fullText.read(buf)) > 0 && numOfChunks > 0);
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            /*
            Create an empty file as we have nothing to put here but it is a record
            that we already try to process this file.
             */
            prepareMetaDataFile(contentMetadataFile);
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
    private Map<String, String> fallbackParse(File binFile, File contentMetadataFile,
            Exception ioExc)
            throws Exception {

        String errorMessage = String
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
            String content = this.tikaService.parseToStringAsPlainText(stream);

            try (Reader contentReader = new StringReader(content)) {
                //Write the parsed info into the metadata file
                return writeMetadata(contentReader, contentMetadataFile);
            }
        }
    }

    /**
     * Returns a {@link Map} that includes original content's map entries and also special entries for string
     * representation of the values of binary, category fields and also tags
     */
    private Map<String, Object> getAdditionalProperties(String inode) {
        Map<String, Object> additionProps = new HashMap<>();
        try {
            additionProps = ContentletUtil.getContentPrintableMap(this.systemUser,
                    APILocator.getContentletAPI().find(inode, this.systemUser, true));
        } catch (Exception e) {
            Logger.error(this, "Unable to add additional metadata to map", e);
        }
        return additionProps;
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
    private void prepareMetaDataFile(File contentMetadataFile) throws IOException {
        //Create the file if does not exist
        contentMetadataFile.getParentFile().mkdirs();
        contentMetadataFile.createNewFile();
    }

    private void logError(File binFile, Exception e) {
        Logger.error(this.getClass(),
                String.format("Could not parse file metadata for file [%s] [%s]",
                        binFile.getAbsolutePath(), e.getMessage()), e);
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

}