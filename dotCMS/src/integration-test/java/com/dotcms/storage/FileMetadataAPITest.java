package com.dotcms.storage;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_3;
import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleBinariesContent;
import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;
import static com.dotcms.datagen.TestDataUtils.removeAnyMetadata;
import static com.dotcms.rest.api.v1.temp.TempFileAPITest.mockHttpServletRequest;
import static com.dotcms.storage.StoragePersistenceProvider.DEFAULT_STORAGE_TYPE;
import static com.dotcms.storage.model.Metadata.CUSTOM_PROP_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.storage.model.BasicMetadataFields;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class FileMetadataAPITest {

    private static final String WRITE_METADATA_ON_REINDEX = ESMappingAPIImpl.WRITE_METADATA_ON_REINDEX;
    private static final String FILE_ASSET = FileAssetAPI.BINARY_FIELD;
    private static FileMetadataAPI fileMetadataAPI;
    private static TempFileAPI tempFileAPI;

    /**
     * if we have a code that requires some environment initialization required to run prior to our dataProvider Methods the @BeforeClass annotation won't do
     * See https://github.com/TNG/junit-dataprovider/issues/114
     * That's why I'm making this a static method and calling it from every data provider we have here.
     * I know.. it Sucks.
     * @throws Exception
     */
    private static void prepareIfNecessary() throws Exception {
       if(null == fileMetadataAPI){
          IntegrationTestInitService.getInstance().init();
          fileMetadataAPI = APILocator.getFileMetadataAPI();
       }
       if(null == tempFileAPI){
          tempFileAPI = APILocator.getTempFileAPI();
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
    public void Test_Generate_Metadata_From_FileAssets(final TestCase testCase) throws Exception {
        prepareIfNecessary();
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
            validateFull(metadata.getFullMetadataMap().get(FILE_ASSET));

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

    /**
     * Given scenario: We simply force pushing any metadata into an empty contentlet
     * Expected result: The Contentlet must have the attributes we forced in it.
     * @param testCase
     * @throws Exception
     */
    
    @Test
    @UseDataProvider("getFileAssetMetadataTestCases")
    public void Test_Force_Set_Metadata(final TestCase testCase) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, testCase.storageType.name());
            final Contentlet fileAsset1 = testCase.fileAssetContent;
            final Metadata fileAsset1Metadata = fileAsset1.getBinaryMetadata(FILE_ASSET);
            assertNotNull(fileAsset1Metadata);
            final boolean isImageNegatedValue = !fileAsset1Metadata.isImage();

            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAsset2 = getFileAssetContent(true, langId);

            final HashMap <String,Serializable> editedMap = new HashMap<>(fileAsset1Metadata.getMap());

            editedMap.put("isImage", isImageNegatedValue);
            editedMap.put("content", "lol");//Expect this within the regular attributes
            editedMap.put("foo", "bar");
            editedMap.put(Metadata.CUSTOM_PROP_PREFIX + "custom-attribute", "custom");//Expect one custom attribute
            final Metadata fileAsset2Metadata = new Metadata(FILE_ASSET, editedMap);
            fileMetadataAPI.setMetadata(fileAsset2, ImmutableMap.of(FILE_ASSET, fileAsset2Metadata));

            final Metadata metadataAfter = fileAsset2.getBinaryMetadata(FILE_ASSET);
            //For the sake of demonstrating we can manipulate metadata with through this method
            assertEquals(metadataAfter.isImage(), isImageNegatedValue);

            final Map<String, Serializable> metadataAfterMap = metadataAfter.getMap();
            assertNull(metadataAfterMap.get("content"));
            assertNull(metadataAfterMap.get("foo"));

            //Verify we have the custom attributes
            final Serializable serializable = metadataAfter.getCustomMeta().get("custom-attribute");
            assertEquals("custom", serializable.toString());

            final Metadata fullMetadataNoCache = fileMetadataAPI
                    .getFullMetadataNoCache(fileAsset2, FILE_ASSET);

            final Map<String, Serializable> fullMetadataNoCacheMap = fullMetadataNoCache.getMap();
            assertEquals("lol", fullMetadataNoCacheMap.get("content"));
            assertEquals("bar", fullMetadataNoCacheMap.get("foo"));


        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
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
     */
    private void validateFull(final Metadata metaData){
        final Map<String, Serializable> meta = metaData.getFieldsMeta();
        assertTrue(meta.containsKey("content"));
        assertTrue(meta.containsKey("contentType"));
        assertTrue(meta.containsKey("fileSize"));
        if((Boolean) meta.get("isImage")){
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

            if (!(Boolean)meta.get("isImage") && (key.equals("width") || key.equals("height"))) {
                // we don't expect
                Logger.info(FileMetadataAPI.class,"We're not supposed to have width or height in a non-image type of file");
            } else {
                assertTrue(String.format("expected metadata key `%s` isn't present in fields:: %s", key, meta) , meta.containsKey(key));
            }

        });
    }

    private static Set<String> basicMetadataFields = new HashSet<>(BasicMetadataFields.keyMap().keySet());

    /**
     * validate basic layout expected in the basic md for File-Asset
     * But nothing else if there are additional values we fail!!
     * @param meta
     */
    private void validateBasicStrict(final Metadata meta) {
        final Boolean isImage = (Boolean) meta.getFieldsMeta().get("isImage");
        //We must account that width and height are only available for images so minus two
        final int expectedFieldsNumber = !isImage ? basicMetadataFields.size() -2 : basicMetadataFields.size();
        validateBasic(meta);

        assertEquals(
                String.format("we're expecting exactly `%d` entries in `%s`", expectedFieldsNumber,
                        meta), expectedFieldsNumber, meta.getFieldsMeta().size());
    }

    /**
     * Given scenario: We have an instance of a content-type that has different fields of type binary
     * this time we test that the first field gets the generated full-MD generated while the rest only get the basic one.
     * Expected Results:
     * @throws IOException
     */
    
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Generate_Metadata_From_ContentType_With_Multiple_Binary_Fields(final StorageType storageType) throws Exception {
        prepareIfNecessary();
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

            //the field is set as the first one according to the sortOrder prop. This is the only that has to have full metadata
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
    public void Test_Get_First_Indexed_Binary_Field() throws Exception {
        prepareIfNecessary();
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
    public void Test_Get_Metadata_No_Cache(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PNG);

            Metadata fileAssetMeta = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, FILE_ASSET);
            //Expect No metadata it's generation until generateContentletMetadata is called
            assertNull(fileAssetMeta);

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(fileAssetContent);
            assertNotNull(metadata);

            fileAssetMeta = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, FILE_ASSET);
            assertFalse(fileAssetMeta.getFieldsMeta().isEmpty());

            //This might seem a little unnecessary but by doing this we verify the fields in the resulting map are the ones allowed to be preset in the metadata generation
            final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) fileMetadataAPI;

            final Map<String, Field> fieldMap = fileAssetContent.getContentType().fieldMap();

            final Set<String> metadataFields = impl
                    .getMetadataFields(fieldMap.get(FILE_ASSET).id());

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
    public void Test_Get_Metadata_No_Cache_Force_Generate(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PDF);

            Metadata fileAssetMD = fileMetadataAPI
                    .getFullMetadataNoCache(fileAssetContent, FILE_ASSET);
            //Expect No metadata it hasn't been generated right out of the box
            assertNull(fileAssetMD);

            fileAssetMD = fileMetadataAPI
                    .getFullMetadataNoCacheForceGenerate(fileAssetContent, FILE_ASSET);
            assertFalse(fileAssetMD.getFieldsMeta().isEmpty());

            //This might seem a little unnecessary but by doing this we verify the fields in the resulting map are the ones allowed to be preset in the metadata generation
            final FileMetadataAPIImpl impl = (FileMetadataAPIImpl) fileMetadataAPI;

            final Map<String, Field> fieldMap = fileAssetContent.getContentType().fieldMap();

            final Set<String> metadataFields = impl
                    .getMetadataFields(fieldMap.get(FILE_ASSET).id());

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
    public void Test_GetMetadata(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PDF);

            //Remove any metadata generated by the checkin
            final File file = (File) fileAssetContent.get(FILE_ASSET);
            removeAnyMetadata(file);

            //So this returns the metadata only if it has been generated already.
            final Metadata fileAssetMD = fileMetadataAPI
                    .getMetadata(fileAssetContent, FILE_ASSET);
            assertNull(fileAssetMD);

             //While this forces the generation if it hasn't occurred yet
            final Metadata meta = fileAssetContent.getBinaryMetadata(FILE_ASSET);

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
    public void Test_GetMetadata_ForceGenerate(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet fileAssetContent = getFileAssetContent(true, langId, TestFile.PDF);

            //Remove any metadata generated by the checkin
            final File file = (File) fileAssetContent.get(FILE_ASSET);
            removeAnyMetadata(file);

            //Once again this method does not force it's generation. it returns the md if it's already available
            Metadata fileAssetMD = fileMetadataAPI.getMetadata(fileAssetContent, FILE_ASSET);
            assertNull(fileAssetMD);
            ///Now we should be getting the generated md
            final Metadata meta = fileMetadataAPI.getMetadataForceGenerate(fileAssetContent, FILE_ASSET);
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
     * Method to test: {@link FileMetadataAPIImpl#putCustomMetadataAttributes(Contentlet, Map)}
     * Given scenario: We create a new piece of content then we generate MD the we add custom attributes and then
     * Expected Result: The customs metadata attributes must be available through calling getMetadata
     * or getFullMetadataNoCache (if it was added to the first indexed binary)
     * @param storageType
     * @throws IOException
     */
    
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Add_Custom_Attributes_FileAssets(final StorageType storageType)
            throws Exception {
        prepareIfNecessary();
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

    /**
     * Method to test: {@link FileMetadataAPIImpl#copyCustomMetadata(Contentlet, Contentlet)}
     * Given scenario: We have 2 contentlets we copy the md into the destination contentlet
     * Expected Result: expect the destination contentlet to have the same custom-metadata as the source contentlet where we took it from.
     * @param storageType
     * @throws Exception
     */
    
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Copy_Metadata(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet source = getMultipleBinariesContent(true, langId, null);

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(source);
            assertNotNull("must have metadata", metadata);

            final Contentlet dest = getMultipleBinariesContent(true, langId, null);

            //MD hasn't been generated on the destination contentlet
            assertNull("Expect no metadata ",fileMetadataAPI.getMetadata(dest,FILE_ASSET_1));
            assertNull("Expect no metadata ",fileMetadataAPI.getMetadata(dest,FILE_ASSET_2));

            fileMetadataAPI.copyCustomMetadata(source, dest);

            //Verify through getMetadata since it does return null if it doesn't exist and doesnt force it's generation.
            //Both still lack meta
            assertNull(fileMetadataAPI.getMetadata(dest,FILE_ASSET_1));
            assertNull(fileMetadataAPI.getMetadata(dest,FILE_ASSET_2));
            //Put on some custom metadata attributes
            fileMetadataAPI.putCustomMetadataAttributes(source,ImmutableMap.of(FILE_ASSET_1, ImmutableMap.of("foo","bar", "bar","foo")));
            //Copy them
            fileMetadataAPI.copyCustomMetadata(source, dest);
            //Verify the metadata gets copied
            assertNotNull(dest.getBinaryMetadata(FILE_ASSET_1));
            validateCustomMetadata(dest.getBinaryMetadata(FILE_ASSET_1).getCustomMeta());
            //Now lets try adding some more custom attributes and verify they get there.
            fileMetadataAPI.putCustomMetadataAttributes(dest,ImmutableMap.of(FILE_ASSET_1, ImmutableMap.of("foo","bar", "bar","foo","lol", "kek")));
            validateCustomMetadata(dest.getBinaryMetadata(FILE_ASSET_1).getCustomMeta());
            assertEquals(dest.getBinaryMetadata(FILE_ASSET_1).getCustomMeta().get("lol"),"kek");
            //Now lets try setting an empty map and verify the're gone.
            fileMetadataAPI.putCustomMetadataAttributes(dest,ImmutableMap.of(FILE_ASSET_1, ImmutableMap.of()));
            assertTrue(dest.getBinaryMetadata(FILE_ASSET_1).getMap().isEmpty());
            //And last
            fileMetadataAPI.putCustomMetadataAttributes(source,ImmutableMap.of(FILE_ASSET_1, ImmutableMap.of("foo","bar", "bar","foo")));
            fileMetadataAPI.copyCustomMetadata(source, dest);
            fileMetadataAPI.generateContentletMetadata(dest);
            validateCustomMetadata(dest.getBinaryMetadata(FILE_ASSET_1).getCustomMeta());


        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    /**
     * Method to test: {@link FileMetadataAPIImpl#generateContentletMetadata(Contentlet)}
     * Given scenario: We have a source contentlet that we have fed with custom metadata attributes
     * Expected Result: Then we call generate metadata methods then we look for the custom attributes we initially set.. They still must be there.
     * @param storageType
     * @throws Exception
     */
    
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Generate_Metadata_Should_Not_Override_Custom_Attributes(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet source = getMultipleBinariesContent(true, langId, null);
            assertNotNull(source.get(FILE_ASSET_1));
            assertNotNull(source.get(FILE_ASSET_2));

            final ContentletMetadata metadata = fileMetadataAPI
                    .generateContentletMetadata(source);
            assertNotNull("must have metadata", metadata);

            fileMetadataAPI.putCustomMetadataAttributes(source, ImmutableMap
                    .of(
                            FILE_ASSET_1, ImmutableMap.of("foo", "bar", "bar", "foo"),
                            FILE_ASSET_2, ImmutableMap.of("foo", "bar", "bar", "foo")
                    )
            );

            CacheLocator.getMetadataCache().clearCache();

            final Metadata fileAsset1Meta = source.getBinaryMetadata(FILE_ASSET_1);
            validateCustomMetadata(fileAsset1Meta.getCustomMeta());

            final Metadata fileAsset2Meta = source.getBinaryMetadata(FILE_ASSET_2);
            validateCustomMetadata(fileAsset2Meta.getCustomMeta());

            final ContentletMetadata regeneratedMetadata = fileMetadataAPI
                    .generateContentletMetadata(source);

            //fileAsset2 is the first binary indexed in our contentlet so we only expect that one on the full-md
            validateCustomMetadata(regeneratedMetadata.getFullMetadataMap().get(FILE_ASSET_2).getCustomMeta());
            //basic metadata is expected to be generated for the two binaries
            validateCustomMetadata(regeneratedMetadata.getBasicMetadataMap().get(FILE_ASSET_1).getCustomMeta());
            validateCustomMetadata(regeneratedMetadata.getBasicMetadataMap().get(FILE_ASSET_2).getCustomMeta());

        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }

    }

    /**
     * Method to test: {@link FileMetadataAPIImpl#putCustomMetadataAttributes(Contentlet, Map)}
     * combined with {@link FileMetadataAPIImpl#generateContentletMetadata(Contentlet)}
     * Given scenario: We have a file asset then we set custom attributes then we generate the file Metadata by calling {@link FileMetadataAPIImpl#generateContentletMetadata(Contentlet)}
     * Expected Result: We must have the combined metadata straight from disk no cache.. this to ensure that both were saved.
     * @param storageType
     * @throws Exception
     */
    @Test
    @UseDataProvider("getStorageType")
    public void Test_Write_Custom_Metadata_Then_Generate_Metadata_Expect_All_Metadata(final StorageType storageType) throws Exception {
        prepareIfNecessary();
        final String stringProperty = Config.getStringProperty(DEFAULT_STORAGE_TYPE);
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            Config.setProperty(DEFAULT_STORAGE_TYPE, storageType.name());
            final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Contentlet source = getFileAssetContent(true, langId, TestFile.GIF);

            assertNull(" Must not have metadata",
                 fileMetadataAPI.getMetadata(source, FILE_ASSET)
            );

            fileMetadataAPI.putCustomMetadataAttributes(source, ImmutableMap
                    .of(
                       FILE_ASSET, ImmutableMap.of("foo", "bar", "bar", "foo")
                    )
            );

            final ContentletMetadata contentletMetadata = fileMetadataAPI
                    .generateContentletMetadata(source);
            assertNotNull(contentletMetadata);

            //Now verify that the MD has been really merged and stored in disk
            final Metadata fullMetadataNoCache = fileMetadataAPI
                    .getFullMetadataNoCache(source, FILE_ASSET);

            assertNotNull(fullMetadataNoCache);

            validateCustomMetadata(fullMetadataNoCache.getCustomMeta());

            validateFull(fullMetadataNoCache);
            validateBasic(fullMetadataNoCache);


        } finally {
            Config.setProperty(DEFAULT_STORAGE_TYPE, stringProperty);
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }

    }

    /**
     * This Method uses the file system to store metadata linked to a temp file.
     * Method to test: {@link FileMetadataAPIImpl#putCustomMetadataAttributes(String, Map)}
     * Given scenario: We create a temp file to get a valid temp-resource-id we attach some random meta
     * Expected result: When we request such info using the same id the results we get must match the originals
     * @throws Exception
     */
    
    @Test
    public void Test_Add_Then_Recover_Temp_Resource_Metadata() throws Exception {
        prepareIfNecessary();
        //disconnect the MD generation on indexing so we can test directly here.
        final boolean defaultValue = Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
        try {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, false);
            final HttpServletRequest request = mockHttpServletRequest();
            final DotTempFile dotTempFile = tempFileAPI.createEmptyTempFile("temp", request);
            fileMetadataAPI.putCustomMetadataAttributes(dotTempFile.id,
                    ImmutableMap.of("fieldXYZ", ImmutableMap.of("foo", "bar", "bar", "foo")));
            final Optional<Metadata> metadataOptional = fileMetadataAPI.getMetadata(dotTempFile.id);
            assertTrue(metadataOptional.isPresent());
            final Metadata metadata = metadataOptional.get();
            assertEquals(dotTempFile.id, metadata.getFieldName());
            final Map<String, Serializable> customMeta = metadata.getCustomMeta();
            validateCustomMetadata(customMeta);
        }finally {
            Config.setProperty(WRITE_METADATA_ON_REINDEX, defaultValue);
        }
    }

    private void validateCustomMetadata(final Map<String, Serializable> customMeta){
        assertEquals("bar", customMeta.get("foo"));
        assertEquals("foo", customMeta.get("bar"));
    }

    @DataProvider
    public static Object[] getStorageType() throws Exception {
        return new Object[]{
         StorageType.FILE_SYSTEM,
         StorageType.DB
        };
    }
    
    @Test
    public void TestMetadataModel() {

        final ImmutableMap<String, Serializable> inputMap = ImmutableMap.of(
                "foo", "bar",
                "bar", "foo",
                CUSTOM_PROP_PREFIX + "foo", "foo",
                CUSTOM_PROP_PREFIX + "bar", "foo",
                "lol:lol", "lol"
        );

        final Metadata metadata = new Metadata("lol", inputMap);

        //Both custom metadata are the same. The second variant only includes the prefix. The first one does not.
        assertEquals(metadata.getCustomMeta().size(),metadata.getCustomMetaWithPrefix().size());

        //The view object returned by this method removes the prefix
        metadata.getCustomMeta().forEach((key, serializable) -> {
            assertFalse(key.startsWith(CUSTOM_PROP_PREFIX));
        });
        //The view object returned by this method keeps the prefix
        metadata.getCustomMetaWithPrefix().forEach((key, serializable) -> {
            assertTrue(key.startsWith(CUSTOM_PROP_PREFIX));
        });

        //None of the main properties should contain the prefix either
        final Map<String, Serializable> fieldsMeta = metadata.getFieldsMeta();

        fieldsMeta.forEach((key, serializable) -> {
            assertFalse(key.startsWith(CUSTOM_PROP_PREFIX));
        });

        final Map<String, Serializable> toMapView = metadata.getMap();

        inputMap.forEach((key, serializable) -> {
               final Serializable object = toMapView.get(key);
               assertEquals(object, serializable);
           }
        );

        final Metadata emptyMapMetadata = new Metadata("lol", ImmutableMap.of());
        assertEquals(emptyMapMetadata.getName(),"unknown");
        assertEquals(emptyMapMetadata.getTitle(),"unknown");
        assertEquals(emptyMapMetadata.getSha256(),"unknown");
        assertEquals(emptyMapMetadata.getLength(),0);
        assertEquals(emptyMapMetadata.getSize(),0);
        assertEquals(emptyMapMetadata.getHeight(),0);
        assertEquals(emptyMapMetadata.getWidth(),0);
        assertFalse(emptyMapMetadata.isImage());

    }
}
