package com.dotcms.storage;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_3;
import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;
import static com.dotcms.datagen.TestDataUtils.removeAnyMetadata;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static com.dotcms.storage.model.BasicMetadataFields.HEIGHT_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.WIDTH_META_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.storage.model.BasicMetadataFields;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class FileMetadataAPITest {

    private static final String WRITE_METADATA_ON_REINDEX = ESMappingAPIImpl.WRITE_METADATA_ON_REINDEX;
    private static final String FILE_ASSET = FileAssetAPI.BINARY_FIELD;
    private static FileMetadataAPI fileMetadataAPI;

    /**
     * if we have a code that requires some environment initialization required to run prior to our dataProvider Methods the @BeforeClass annotation won't do
     * See https://github.com/TNG/junit-dataprovider/issues/114
     * That's why I'm making this a static method and calling it from every data provider we have here.
     * I know.. it Sucks.
     * @throws Exception
     */
    public static void prepareIfNecessary() throws Exception {
       if(fileMetadataAPI == null){
         IntegrationTestInitService.getInstance().init();
         fileMetadataAPI = APILocator.getFileMetadataAPI();
       }
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

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(testCase.fileAssetContent);

            assertNotNull(metadata);

            assertNotNull(metadata.getBasicMetadataMap());
            assertNotNull(metadata.getFullMetadataMap());

            validateBasicStrict(metadata.getBasicMetadataMap().get(FILE_ASSET));
            validateFull(metadata.getFullMetadataMap().get(FILE_ASSET), testCase.testFile);

            final Map<String, Set<String>> metadataInfo = fileMetadataAPI.removeMetadata(testCase.fileAssetContent);
            assertFalse(metadataInfo.isEmpty());
            //Verify the metadata got removed successfully
            final StoragePersistenceAPI storage = StoragePersistenceProvider.INSTANCE.get().getStorage(testCase.storageType);
            metadataInfo.forEach((metadataGroup, metadataPaths) -> {
                 for(final String path: metadataPaths){
                     try {
                         Assert.assertFalse(storage.existsObject(metadataGroup, path));
                     } catch (DotDataException e) {
                         fail("Failure verifying object existence :" + e.getMessage());
                     }
                 }
            });

        }finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE,stringProperty);
        }

    }

    @DataProvider
    public static Object[] getFileAssetMetadataTestCases() throws Exception {
        prepareIfNecessary();
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
    private void validateFull(final Metadata metaData, final TestFile testFile){
        final Map<String, Serializable> meta = metaData.getFieldsMeta();
        assertTrue(meta.containsKey("content"));
        assertTrue(meta.containsKey("contentType"));
        assertTrue(meta.containsKey("fileSize"));
        if(isSupportedImage(testFile.getFilePath())){ //svg files don't have dimensions. or Tika fails to read them.
          assertTrue(meta.containsKey("height"));
          assertTrue(meta.containsKey("width"));
        }
        //basic meta is a sub set of the full-md
        validateBasic(metaData);
    }

    /**
     * validate basic layout expected in the basic md for File-Asset
     * @param metaData
     */
    private void validateBasic(final Metadata metaData){
        final Map<String, Serializable> meta = metaData.getFieldsMeta();
        basicMetadataFields.forEach(key -> {
            assertTrue("metadata fields isn't present in: " + key, meta.containsKey(key));
        });
    }

    private static Set<String> basicMetadataFields = BasicMetadataFields.keySet().stream()
            .filter(s -> !WIDTH_META_KEY.key().equals(s) && !HEIGHT_META_KEY.key().equals(s))
            .collect(Collectors.toSet()); //Remove width and height since they are present only on images

    /**
     * validate basic layout expected in the basic md for File-Asset
     * But nothing else if there are additional values we fail!!
     * @param metaData
     */
    private void validateBasicStrict(final Metadata metaData){
        final int expectedFields = basicMetadataFields.size();
        validateBasic(metaData);
        assertEquals(String.format("we're expecting exactly `%d` entries.",expectedFields),expectedFields, metaData.getFieldsMeta().size());
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
            final Contentlet multipleBinariesContent = getMultipleImageBinariesContent(true, langId, null);

            //Multiple binary fields
            //since the index operation performs this very same operation here the method generateContentletMetadata should retrieve the existing metadata.
            final ContentletMetadata multiBinaryMetadata = fileMetadataAPI
                    .generateContentletMetadata(multipleBinariesContent);
            assertNotNull(multiBinaryMetadata);

            final Map<String, Metadata> fullMetadataMap = multiBinaryMetadata
                    .getFullMetadataMap();
            assertNotNull(fullMetadataMap);

            final Map<String, Metadata> basicMetadataMap = multiBinaryMetadata
                    .getBasicMetadataMap();
            assertNotNull(basicMetadataMap);

            //the filed is set as the first one according to the sortOrder prop. This is the only that has to have full metadata
            final Metadata fileAsset2FullMeta = fullMetadataMap.get(FILE_ASSET_2);
            assertNotNull(fileAsset2FullMeta);

            //These are all the non-null binaries
            final Metadata fileAsset1BasicMeta = basicMetadataMap.get(FILE_ASSET_1);
            assertNotNull(fileAsset1BasicMeta);

            final Metadata fileAsset2BasicMeta = basicMetadataMap.get(FILE_ASSET_2);
            assertNotNull(fileAsset2BasicMeta);

            //the filed does exist but it was not set
            final Metadata fileAsset3BasicMeta = basicMetadataMap.get(FILE_ASSET_3);
            assertNull(fileAsset3BasicMeta);

            final Map<String, Set<String>> metadataInfo = fileMetadataAPI.removeMetadata(multipleBinariesContent);
            assertFalse(metadataInfo.isEmpty());
            //Verify the metadata got removed successfully
            final StoragePersistenceAPI storage = StoragePersistenceProvider.INSTANCE.get().getStorage(storageType);
            metadataInfo.forEach((metadataGroup, metadataPaths) -> {
                for(final String path: metadataPaths){
                    try {
                        Assert.assertFalse(storage.existsObject(metadataGroup, path));
                    } catch (DotDataException e) {
                        fail("Failure verifying object existence :" + e.getMessage());
                    }
                }
            });

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
        final Contentlet multipleBinariesContent = getMultipleImageBinariesContent(true, langId, null);

        final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) fileMetadataAPI;
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
     *  Method to test: {@link FileMetadataAPIImpl#getFullMetadataNoCache(Contentlet, String)}
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

            Metadata fileAssetMeta = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, fileAssetField);
            //Expect no metadata it hasn't been generated
            assertNull(fileAssetMeta);

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(fileAssetContent);
            assertNotNull(metadata);

            fileAssetMeta = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, fileAssetField);
            assertFalse(fileAssetMeta.getFieldsMeta().isEmpty());

            //This might seem a little unnecessary but by doing this we verify the fields in the resulting map are the ones allowed to be preset in the metadata generation
            final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) fileMetadataAPI;

            final Map<String, Field> fieldMap = fileAssetContent.getContentType().fieldMap();

            final Set<String> metadataFields = impl
                    .getMetadataFields(fieldMap.get(fileAssetField).id());

            fileAssetMeta.getFieldsMeta().forEach((key, value) -> {
                assertTrue(metadataFields.contains(key) || basicMetadataFields.contains(key));
            });

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    /**
     *  Method to test: {@link FileMetadataAPIImpl#getFullMetadataNoCacheForceGenerate(Contentlet, String)}
     *  Given scenario: We create a new piece of content then we call getMetadataNoCache. The new piece of content isn't expected to have any previously generated metadata
     *  Expected Result: Calling again the method with the force param set to true must take care of the MD generation
     * @param storageType
     * @throws IOException
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Get_Metadata_No_Cache_Force_Generate(final StorageType storageType) throws IOException, DotDataException {
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

            Metadata fileAssetMD = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, fileAssetField);
            //Expect no metadata it hasn't been generated
            assertNull(fileAssetMD);

            fileAssetMD = fileMetadataAPI
                    .getFullMetadataNoCacheForceGenerate(fileAssetContent, fileAssetField);
            assertFalse(fileAssetMD.getFieldsMeta().isEmpty());

            //This might seem a little unnecessary but by doing this we verify the fields in the resulting map are the ones allowed to be preset in the metadata generation
            final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) fileMetadataAPI;

            final Map<String, Field> fieldMap = fileAssetContent.getContentType().fieldMap();

            final Set<String> metadataFields = impl
                    .getMetadataFields(fieldMap.get(fileAssetField).id());

            fileAssetMD.getFieldsMeta().forEach((key, value) -> {
                assertTrue(metadataFields.contains(key) || basicMetadataFields.contains(key));
            });

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }


    /**
     * Method to test: {@link FileMetadataAPIImpl#getMetadata(Contentlet, String)}
     * Given scenario: We create a new piece of content then we call getMetadata. Then we call it again after calling generateContentletMetadata
     * Expected Result: Until generateContentletMetadata gets called no metadata should be returned
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

            Metadata fileAssetMD = fileMetadataAPI
                    .getMetadata(fileAssetContent, fileAssetField);
            //Expect no metadata it has not been generated
            assertNull(fileAssetMD);

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(fileAssetContent);
            assertNotNull(metadata);

            final Metadata meta = fileMetadataAPI
                    .getMetadata(fileAssetContent,fileAssetField);
            assertNotNull(meta);

            final Map<String, Serializable> metadataMap = meta.getFieldsMeta();
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

    /**
     * Method to test: {@link FileMetadataAPIImpl#getMetadataForceGenerate(Contentlet, String)}
     * Given scenario: We create a new piece of content then we call getMetadata. Which isn't expected to have any previously generated metadata
     * Expected Result: Calling again the method with the force param set to true must take care of the MD generation
     * @param storageType
     * @throws IOException
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_GetMetadata_ForceGenerate(final StorageType storageType) throws IOException, DotDataException {
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

            Metadata fileAssetMD = fileMetadataAPI
                    .getMetadata(fileAssetContent, fileAssetField);
            //Expect no metadata it has not been generated
            assertNull(fileAssetMD);

            final Metadata meta = fileMetadataAPI
                    .getMetadataForceGenerate(fileAssetContent, fileAssetField);
            assertNotNull(meta);

            final Map<String, Serializable> metadataMap = meta.getFieldsMeta();
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


    @Test
    @UseDataProvider("getStorageType")
    public void Test_Add_Custom_Attributes_FileAssets(final StorageType storageType)
            throws IOException, DotDataException {
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet multipleBinariesContent = getMultipleBinariesContent(true, langId,
                    null);
            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(multipleBinariesContent);

            assertFalse(metadata.getFullMetadataMap().isEmpty());
            assertFalse(metadata.getBasicMetadataMap().isEmpty());


            final Tuple2 <String,String> t1 = Tuple.of("focalPoint","67.4,6.77");
            final Tuple2 <String,String> t2 = Tuple.of("title","lol");
            final Tuple2 <String,String> t3 = Tuple.of("foo","bar");

            for (final String fieldName : metadata.getBasicMetadataMap().keySet()) {
                final Map<String, Map<String, Serializable>> customAttributes = ImmutableMap
                        .of(fieldName, ImmutableMap.of(t1._1, t1._2, t2._1, t2._2, t3._1, t3._2));
                fileMetadataAPI
                        .putCustomMetadataAttributes(multipleBinariesContent, customAttributes);
                Logger.info(FileMetadataAPITest.class, "setting up attribute: " + fieldName);
            }

            for (final String fieldName : metadata.getBasicMetadataMap().keySet()) {

                final Metadata meta = fileMetadataAPI
                      .getMetadata(multipleBinariesContent, fieldName);

                assertEquals(meta.getCustomMeta().get(t1._1),t1._2);
                assertEquals(meta.getCustomMeta().get(t2._1),t2._2);
                assertEquals(meta.getCustomMeta().get(t3._1),t3._2);

                final Metadata metaNoCache = fileMetadataAPI
                        .getFullMetadataNoCache(multipleBinariesContent, fieldName);

                assertEquals(metaNoCache.getCustomMeta().get(t1._1),t1._2);
                assertEquals(metaNoCache.getCustomMeta().get(t2._1),t2._2);
                assertEquals(metaNoCache.getCustomMeta().get(t3._1),t3._2);

            }

            
        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }


    @DataProvider
    public static Object[] getStorageType() throws Exception {
        prepareIfNecessary();
        return new Object[]{
         StorageType.FILE_SYSTEM,
         StorageType.DB
        };
    }

}
