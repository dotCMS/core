package com.dotcms.storage;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_3;
import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.datagen.TestDataUtils.removeAnyMetadata;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import org.apache.commons.io.FilenameUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class FileMetadataAPITest {

    private static final String WRITE_METADATA_ON_REINDEX = ESMappingAPIImpl.WRITE_METADATA_ON_REINDEX;
    private static final String FILE_ASSET = FileAssetAPI.BINARY_FIELD;
    private static FileMetadataAPI contentletMetadataAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        contentletMetadataAPI = APILocator.getFileMetadataAPI();
    }

    /**
     * This test evaluates both basic vs full MD
     * Given scenarios: We're testing metadata api against different types of asset-files
     * Expected Results: we should get full and basic md for every type. Basic metadata must be included within the fm
     * @throws IOException
     */
    @Test
    @UseDataProvider("getFileAssetMetadataTestCases")
    public void Test_Generate_Metadata_From_FileAssets(final TestCase testCase) throws IOException, DotDataException {

        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, testCase.storageType.name());

            //Remove any previously generated metadata by the checkin process
            final File file = (File) testCase.fileAssetContent.get(FILE_ASSET);
            removeAnyMetadata(file);

            final ContentletMetadata metadata = contentletMetadataAPI
                    .generateContentletMetadata(testCase.fileAssetContent);

            assertNotNull(metadata);

            assertNotNull(metadata.getBasicMetadataMap());
            assertNotNull(metadata.getFullMetadataMap());

            validateBasicStrict(metadata.getBasicMetadataMap().get(FILE_ASSET));
            validateFull(metadata.getFullMetadataMap().get(FILE_ASSET), testCase.testFile);
        }finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE,stringProperty);
        }

    }

    @DataProvider
    public static Object[] getFileAssetMetadataTestCases() {
        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        //disconnect the MD generation on indexing so we can test the generation directly using the API.
        Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
        try {
            return new Object[]{
                    new TestCase(langId, StorageType.FILE_SYSTEM, TestFile.JPG,
                            getFileAssetContent(true, langId, TestFile.JPG)),
                    new TestCase(langId, StorageType.FILE_SYSTEM, TestFile.GIF,
                            getFileAssetContent(true, langId, TestFile.GIF)),
                    new TestCase(langId, StorageType.FILE_SYSTEM, TestFile.PNG,
                            getFileAssetContent(true, langId, TestFile.PNG)),
                    new TestCase(langId, StorageType.FILE_SYSTEM, TestFile.SVG,
                            getFileAssetContent(true, langId, TestFile.SVG)),
                    new TestCase(langId, StorageType.FILE_SYSTEM, TestFile.PDF,
                            getFileAssetContent(true, langId, TestFile.PDF)),

                    new TestCase(langId, StorageType.DB, TestFile.JPG,

                            getFileAssetContent(true, langId, TestFile.JPG)),
                    new TestCase(langId, StorageType.DB, TestFile.GIF,
                            getFileAssetContent(true, langId, TestFile.GIF)),
                    new TestCase(langId, StorageType.DB, TestFile.PNG,
                            getFileAssetContent(true, langId, TestFile.PNG)),
                    new TestCase(langId, StorageType.DB, TestFile.SVG,
                            getFileAssetContent(true, langId, TestFile.SVG)),
                    new TestCase(langId, StorageType.DB, TestFile.PDF,
                            getFileAssetContent(true, langId, TestFile.PDF)),
            };
        } finally {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    static class TestCase{
        final long langId;
        final StorageType storageType;
        final TestFile testFile;
        final Contentlet fileAssetContent;

        TestCase(final long langId,final StorageType storageType,
                final TestFile testFile,final Contentlet fileAssetContent) {
            this.langId = langId;
            this.storageType = storageType;
            this.testFile = testFile;
            this.fileAssetContent = fileAssetContent;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "langId=" + langId +
                    ", storageType=" + storageType +
                    ", testFile=" + FilenameUtils.getName(testFile.getFilePath()) +
                    ", fileAssetContent=" + fileAssetContent +
                    '}';
        }
    }

    /**
     * validate basic layout expected in the full md for File-Asset
     * @param metaData
     * @param testFile
     */
    private void validateFull(final Map<String, Serializable> metaData, final TestFile testFile){
        assertTrue(metaData.containsKey("content"));
        assertTrue(metaData.containsKey("contentType"));
        assertTrue(metaData.containsKey("fileSize"));
        if(isSupportedImage(testFile.getFilePath())){ //svg files don't have dimensions. or Tika fails to read them.
          assertTrue(metaData.containsKey("height"));
          assertTrue(metaData.containsKey("width"));
        }
        //basic meta is a sub set of the full-md
        validateBasic(metaData);
    }

    /**
     * validate basic layout expected in the basic md for File-Asset
     * @param metaData
     */
    private void validateBasic(final Map<String, Serializable> metaData){
        basicMetadataFields.forEach(key -> {
            assertTrue(metaData.containsKey(key));
        });
    }

    private static Set<String> basicMetadataFields = ImmutableSet.of("sha256", "path","title","modDate","contentType", "isImage");

    /**
     * validate basic layout expected in the basic md for File-Asset
     * But nothing else if there are additional values we fail!!
     * @param metaData
     */
    private void validateBasicStrict(final Map<String, Serializable> metaData){
        final int expectedFields = 6;
        validateBasic(metaData);
        assertEquals(String.format("we're expecting exactly `%d` entries.",expectedFields),expectedFields, metaData.size());
    }

    //SVG  do not have dimensions, but that's a known issue we're willing to forgive.
    private static Set<String> imageExt = ImmutableSet.of("jpg", "png", "gif");

    /**
     * Custom `isImage` method. this custom version skips svg files. So we can forgive them from getting us dimensions
     * @param fileName
     * @return
     */
    private boolean isSupportedImage(final String fileName) {
        final String assetNameExt = UtilMethods.getFileExtension(fileName).toLowerCase();
        return imageExt.contains(assetNameExt);
    }

    /**
     * Given scenario: We have an instance of a content-type that has different fields of type binary
     * this time we test that the first field gets the generated full-MD generated while the rest only get the basic one.
     * Expected Results:
     * @throws IOException
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Generate_Metadata_From_ContentType_With_Multiple_Binary_Fields(final StorageType storageType) throws IOException, DotDataException {
        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());

            //Multiple binary fields
            final Contentlet multipleBinariesContent = getMultipleBinariesContent(true, langId, null);

            //Multiple binary fields
            final ContentletMetadata multiBinaryMetadata = contentletMetadataAPI
                    .generateContentletMetadata(multipleBinariesContent);
            assertNotNull(multiBinaryMetadata);

            final Map<String, Map<String, Serializable>> fullMetadataMap = multiBinaryMetadata
                    .getFullMetadataMap();
            assertNotNull(fullMetadataMap);

            final Map<String, Map<String, Serializable>> basicMetadataMap = multiBinaryMetadata
                    .getBasicMetadataMap();
            assertNotNull(basicMetadataMap);

            //the filed is set as the first one according to the sortOrder prop. This is the only that has to have full metadata
            final Map<String, Serializable> fileAsset2FullMeta = fullMetadataMap.get(FILE_ASSET_2);
            assertNotNull(fileAsset2FullMeta);

            //These are all the non-null binaries
            final Map<String, Serializable> fileAsset1BasicMeta = basicMetadataMap.get(FILE_ASSET_1);
            assertNotNull(fileAsset1BasicMeta);

            final Map<String, Serializable> fileAsset2BasicMeta = basicMetadataMap.get(FILE_ASSET_2);
            assertNotNull(fileAsset2BasicMeta);

            //the filed does exist but it was not set
            final Map<String, Serializable> fileAsset3BasicMeta = basicMetadataMap.get(FILE_ASSET_3);
            assertNull(fileAsset3BasicMeta);
        }finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
        }
    }

    /**
     * Method to test: {@link FileMetadataAPIImpl#findBinaryFields(Contentlet)}
     * Given scenario: We have an instance of a content-type that has different fields of type bin
     * Expected Results: After calling findBinaryFields I should get a tuple with one file
     * candidate for the full MD generation and the rest in the second component of the tuple
     */
    @Test
    public void Test_Get_First_Indexed_Binary_Field() {
        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet multipleBinariesContent = getMultipleBinariesContent(true, langId, null);

        final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) contentletMetadataAPI;
        final Tuple2<SortedSet<String>, SortedSet<String>> binaryFields = impl
                .findBinaryFields(multipleBinariesContent);

        final Set<String> allBinaryFields = binaryFields._1;

        assertEquals(allBinaryFields.size(), 3);

        assertTrue(allBinaryFields.contains(FILE_ASSET_1));
        assertTrue(allBinaryFields.contains(FILE_ASSET_2));
        assertTrue(allBinaryFields.contains(FILE_ASSET_3));

        final Set<String> binaryFieldCandidateForFullMetadata = binaryFields._2;
        assertEquals(binaryFieldCandidateForFullMetadata.size(), 1);
        assertTrue(binaryFieldCandidateForFullMetadata.contains(FILE_ASSET_2));
    }

    /**
     *  Method to test: {@link FileMetadataAPIImpl#getMetadataNoCache(Contentlet, String)}
     *  Given scenario: We create a new piece of content then we call getMetadataNoCache. Then we call it again after calling generateContentletMetadata
     *  Expected Result: Until generateContentletMetadata gets called no metadata should be returned
     * @param storageType
     * @throws IOException
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Get_Metadata_No_Cache(final StorageType storageType) throws IOException, DotDataException {
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PDF);

            //Remove any metadata generated by the checkin
            final String fileAssetField = FILE_ASSET;

            final File file = (File) fileAssetContent.get(fileAssetField);
            removeAnyMetadata(file);

            Map<String, Serializable> fileAssetMD = contentletMetadataAPI
                    .getMetadataNoCache(fileAssetContent, fileAssetField);
            //Expect no metadata it hasn't been generated
            assertNull(fileAssetMD);

            final ContentletMetadata metadata = contentletMetadataAPI
                    .generateContentletMetadata(fileAssetContent);
            assertNotNull(metadata);

            fileAssetMD = contentletMetadataAPI
                    .getMetadataNoCache(fileAssetContent, fileAssetField);
            assertFalse(fileAssetMD.isEmpty());

            //This might seem a little unnecessary but by doing this we verify the fields in the resulting map are the ones allowed to be preset in the metadata generation
            final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) contentletMetadataAPI;

            final Map<String, Field> fieldMap = fileAssetContent.getContentType().fieldMap();

            final Set<String> metadataFields = impl
                    .getMetadataFields(fieldMap.get(fileAssetField).id());

            fileAssetMD.forEach((key, value) -> {
                assertTrue(metadataFields.contains(key) || basicMetadataFields.contains(key));
            });

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    /**
     *  Method to test: {@link FileMetadataAPIImpl#getMetadata(Contentlet, String)}
     *  Given scenario: We create a new piece of content then we call getMetadata. Then we call it again after calling generateContentletMetadata
     *  Expected Result: Until generateContentletMetadata gets called no metadata should be returned
     * @param storageType
     * @throws IOException
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_GetMetadata(final StorageType storageType) throws IOException, DotDataException {
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PDF);

            //Remove any metadata generated by the checkin
            final String fileAssetField = FILE_ASSET;

            final File file = (File) fileAssetContent.get(fileAssetField);
            removeAnyMetadata(file);

            Map<String, Serializable> fileAssetMD = contentletMetadataAPI
                    .getMetadata(fileAssetContent, fileAssetField);
            //Expect no metadata it has not been generated
            assertNull(fileAssetMD);

            final ContentletMetadata metadata = contentletMetadataAPI
                    .generateContentletMetadata(fileAssetContent);
            assertNotNull(metadata);

            final Map<String,Serializable> metadataMap = contentletMetadataAPI
                    .getMetadata(fileAssetContent,fileAssetField);
            assertNotNull(metadataMap);

            assertNotNull(metadataMap.get("contentType"));
            assertNotNull(metadataMap.get("modDate"));
            assertNotNull(metadataMap.get("path"));
            assertNotNull(metadataMap.get("sha256"));
            assertNotNull(metadataMap.get("title"));

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    @DataProvider
    public static Object[] getStorageType() {
        return new Object[]{StorageType.FILE_SYSTEM, StorageType.DB};
    }

}
