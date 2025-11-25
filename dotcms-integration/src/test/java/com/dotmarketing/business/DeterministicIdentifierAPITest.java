package com.dotmarketing.business;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;
import static com.dotmarketing.business.DeterministicIdentifierAPIImpl.GENERATE_DETERMINISTIC_IDENTIFIERS;
import static com.dotmarketing.business.DeterministicIdentifierAPIImpl.NON_DETERMINISTIC_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class DeterministicIdentifierAPITest {

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * if we have a code that requires some environment initialization required to run prior to our dataProvider Methods the @BeforeClass annotation won't do
     * See https://github.com/TNG/junit-dataprovider/issues/114
     * That's why I'm making this a static method and calling it from every data provider we have here.
     * I know.. it Sucks.
     * @throws Exception
     */
    private static void prepareIfNecessary() throws Exception {
        if(!initialized.getAndSet(true)){
            IntegrationTestInitService.getInstance().init();
        }
    }

    private final DeterministicIdentifierAPIImpl defaultGenerator = new DeterministicIdentifierAPIImpl();


    /**
     * Given Scenario: We have a fileAsset with missing physical file
     * Expected behavior: The method must return an empty optional and the generation of an identifier still must be possible
     * The whole purpose of this test is to ensure that the method is robust enough to handle missing files
     * and demonstrate that we won't get a NPE
     * But the returned id must be a valid UUID and not a deterministic one
     * @throws IOException if the file cannot be created
     * @throws DotDataException if the data cannot be persisted
     * @throws DotSecurityException if the data cannot be persisted
     */
    @Test
    public void TestNullBinary() throws IOException, DotDataException, DotSecurityException {

        final int english = 1;
        final String hostName = String.format("my.host%s.com", System.currentTimeMillis());
        final Host site = new SiteDataGen().name(hostName).nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();

        java.io.File file = java.io.File.createTempFile("file", ".txt");
        FileUtil.write(file, "helloworld");

        //Now let's create a fileAsset with a missing binary
        final Contentlet fileAsset = new FileAssetDataGen(folder, file).languageId(english).nextPersisted();
        fileAsset.setIdentifier(null);
        fileAsset.setInode(null);
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, null);

        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            //We just introduced a failure by removing the binary
            final Optional<String> resolved = defaultGenerator.resolveAssetName(fileAsset);
            assertTrue(resolved.isEmpty());

            //But we should still be able to get
            final String generatedId = defaultGenerator.generateDeterministicIdBestEffort(fileAsset, folder);
            assertTrue(UUIDUtil.isUUID(generatedId));

        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }

    }

    /**
     * Given scenario: We have created contentlets having turned off the deterministic id generation therefore it all comes with random ids
     * Meaning that any deterministic id request does not exist in the database
     * Expected behavior: The best effort must give us deterministic ids until they're inserted on te database.
     * @param testCase
     * @throws Exception
     */

    @Test
    @UseDataProvider("getAssetsTestCases")
    public void Test_Asset_Generate_Deterministic_Id_Best_Effort(final AssetTestCase testCase)
            throws Exception {

        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
            //First the basic two checks
            //We check the asset type is what we expect
            assertEquals(testCase.expectedType,
                    defaultGenerator.resolveAssetType(testCase.versionable));

            final Optional<String> resolved = defaultGenerator.resolveAssetName(
                    testCase.versionable);

            assertTrue(resolved.isPresent());

            //We also check the asset name is what we expect too
            assertEquals(testCase.expectedName,
                    resolved.get());
            //While the identifier isnt in the database we should continue to get the same (That's why we call it consistent)
            final String generatedId1 = defaultGenerator
                    .generateDeterministicIdBestEffort(testCase.versionable, testCase.parent);
            assertFalse(isIdentifier(generatedId1));
            assertTrue(defaultGenerator.isDeterministicId(generatedId1));
            final String generatedId2 = defaultGenerator
                    .generateDeterministicIdBestEffort(testCase.versionable, testCase.parent);
            //And they should be compatible with our definition of UUID
            assertTrue(UUIDUtil.isUUID(generatedId1));
            //They must be the same until it gets inserted into the identifier table then afterwards a random uuid will be generated. That's why it is called bestEffort
            assertEquals(generatedId1, generatedId2);
            //Now simulate a situation on which the identifier already lives in the db
            insertIdentifier(generatedId1, testCase.expectedName, testCase.expectedType,
                    testCase.site.getIdentifier());
            //The expected this time would be a non-deterministic identifier
            final String generatedId3 = defaultGenerator
                    .generateDeterministicIdBestEffort(testCase.versionable, testCase.parent);
            assertNotEquals(generatedId2, generatedId3);
            //They always must pass this function correctly regardless of the nature
            assertTrue(UUIDUtil.isUUID(generatedId3));
            //And finally we test we're looking at the old format
            assertTrue(generatedId3.matches(NON_DETERMINISTIC_IDENTIFIER));

            assertFalse(defaultGenerator.isDeterministicId(generatedId3));
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }

    }

    private boolean isIdentifier(final String hash){
        return (new DotConnect()
                .setSQL("select count(id) as test from identifier where id=?")
                .addParam(hash)
                .getInt("test")>0);
    }

    private void insertIdentifier(final String hash, final String assetName, String assetType, final String siteId)
            throws DotDataException {

        new DotConnect()
        .setSQL("INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,owner,create_date,asset_subtype,id) values (?,?,?,?,?,?,?,?,?,?)")
        .addParam("/")
        .addParam(assetName + " : "  + System.nanoTime())
        .addParam(siteId)
        .addParam(assetType)
        .addParam(new Date())
        .addParam(new Date())
        .addParam(APILocator.systemUser().getUserId())
        .addParam(new Date())
        .addParam((String)null)
        .addParam(hash).loadResult();

    }

    @DataProvider
    public static Object[] getAssetsTestCases() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            //Disconnect the consistent identifier generation so we can test the generator and no identifier will be already stored in the db
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, false);

            final int english = 1;

            final Host systemHost = APILocator.getHostAPI().findSystemHost();

            final String hostName = String.format("my.host%s.com", System.currentTimeMillis());

            final Host site = new SiteDataGen().name(hostName).nextPersisted();

            final Folder folder = new FolderDataGen().site(site).nextPersisted();

            java.io.File file = java.io.File.createTempFile("file", ".txt");
            FileUtil.write(file, "helloworld");

            final Contentlet fileAsset = new FileAssetDataGen(folder, file)
                    .languageId(english).nextPersisted();

            final Template template = new TemplateDataGen().site(site).nextPersisted();

            final HTMLPageAsset pageAsset = new HTMLPageDataGen(folder, template)
                    .languageId(english).nextPersisted();

            final Contentlet multiBinary = getMultipleImageBinariesContent(true,
                    english, null);

            final Persona persona = new PersonaDataGen().hostFolder(site.getIdentifier())
                    .nextPersisted();

            return new Object[]{

                new AssetTestCase(site, systemHost, site.getName(), "Host", site),
                new AssetTestCase(fileAsset, folder, fileAsset.getName(), "FileAsset", site),
                new AssetTestCase(template, site, template.getName(), "Template", site),
                new AssetTestCase(pageAsset, folder, pageAsset.getPageUrl(), "htmlpageasset", site),

                    new AssetTestCase(multiBinary,
                            (Treeable) multiBinary.getParentPermissionable(),
                            multiBinary.getBinary(FILE_ASSET_1).getName() + ":" + multiBinary
                                    .getBinary(FILE_ASSET_2).getName(),
                            multiBinary.getContentType().variable(),
                            site),

                new AssetTestCase(persona, site, persona.getKeyTag(), "persona", site)

            };
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    static class AssetTestCase {
        final Versionable versionable;
        final Treeable parent;
        final String expectedName;
        final String expectedType;
        final Host site;

        public AssetTestCase(Versionable versionable, final Treeable parent, final String expectedName, final String expectedType, final Host site) {
            this.versionable = versionable;
            this.parent = parent;
            this.expectedName = expectedName;
            this.expectedType = expectedType;
            this.site = site;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "versionable=" + versionable +
                    ", parent=" + parent +
                    ", expectedName='" + expectedName + '\'' +
                    ", expectedType='" + expectedType + '\'' +
                    '}';
        }

    }

    /**
     * Given Scenario: We get a bunch of Content-types then we revise the generation of the name based on the info provided on the test-case
     * methodToTest {@link DeterministicIdentifierAPIImpl#generateDeterministicIdBestEffort(ContentType, Supplier)}
     * methodToTest {@link DeterministicIdentifierAPIImpl#generateDeterministicIdBestEffort(Field, Supplier)}
     * Expected Results: Both tested methods must be idempotent for a given set of inputs the outcome should always remain the same
     * @param testCase
     */
    @Test
    @UseDataProvider("getContentTypeTestCases")
    public void Test_Generate_Content_Type_Identifier(final ContentTypeTestCase testCase) {
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
            assertEquals(testCase.expectedType,
                    defaultGenerator.resolveAssetType(testCase.contentType));
            assertEquals(testCase.expectedName, defaultGenerator.resolveName(testCase.contentType,
                    testCase.contentType::variable));
            final String generatedId1 = defaultGenerator
                    .generateDeterministicIdBestEffort(testCase.contentType,
                            testCase.contentType::variable);
            assertTrue(UUIDUtil.isUUID(generatedId1));
            final String generatedId2 = defaultGenerator
                    .generateDeterministicIdBestEffort(testCase.contentType,
                            testCase.contentType::variable);
            //Test it is idempotent
            assertEquals(generatedId1, generatedId2);

            for(final Field field : testCase.contentType.fields()){

                final String fieldIdentifier1 = defaultGenerator.generateDeterministicIdBestEffort(field, field::variable);
                assertTrue(UUIDUtil.isUUID(fieldIdentifier1));
                final String fieldIdentifier2 = defaultGenerator.generateDeterministicIdBestEffort(field, field::variable);
                //Test it is idempotent
                assertEquals(fieldIdentifier1, fieldIdentifier2);
                final String expected = String.format("%s:%s", field.variable(), field.typeName());
                assertEquals(expected, defaultGenerator.resolveName(field, field::variable));
            }

        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    @DataProvider
    public static Object[] getContentTypeTestCases() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            //Disconnect the consistent identifier generation so we can test the generator and no identifier will be already stored in the db
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, false);
            final Host systemHost = APILocator.systemHost();

            final ContentType languageVariableContentType = ContentTypeDataGen
                    .createLanguageVariableContentType();

            final ContentType contentGenericType = new ContentTypeDataGen().workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(new FieldDataGen().name("title").velocityVarName("title").next())
                    .field(new FieldDataGen().name("body").velocityVarName("body").next()).nextPersisted();

            return new Object[]{
               new ContentTypeTestCase(languageVariableContentType, languageVariableContentType.variable(), "KeyValue", systemHost),
               new ContentTypeTestCase(contentGenericType, contentGenericType.variable(), "CONTENT", systemHost)
            };
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    static class ContentTypeTestCase {

        final ContentType contentType;
        final String expectedName;
        final String expectedType;
        final Host site;

         ContentTypeTestCase(final ContentType contentType, final String expectedName,
                final String expectedType,final Host site) {
            this.contentType = contentType;
            this.expectedName = expectedName;
            this.expectedType = expectedType;
            this.site = site;
        }

        @Override
        public String toString() {
            return "ContentTypeTestCase{" +
                    "contentType=" + contentType +
                    ", expectedName='" + expectedName + '\'' +
                    ", expectedType='" + expectedType + '\'' +
                    '}';
        }
    }

    //There is constant in javascript MAX_SAFE_INTEGER which is the proposed limit
    private static final long JS_MAX_SAFE_INTEGER = 9007199254740991L;

    @Test
    @UseDataProvider("getLanguageTestCases")
    public void Test_Language_Deterministic_Id(final LanguageTestCase testCase){

        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
            final Language lang =  new Language(0, testCase.langCode, testCase.countryCode, "", testCase.country);
            assertEquals(testCase.expectedSeed, defaultGenerator.deterministicIdSeed(lang));
            final long id = defaultGenerator.generateDeterministicIdBestEffort(lang);
            //Longs above this number are not correctly rendered in javascript
            assertTrue(id < JS_MAX_SAFE_INTEGER);
            assertEquals(testCase.expectedHash, id);
        }finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    @DataProvider
    public static Object[] getLanguageTestCases() throws Exception {

        prepareIfNecessary();
        //Propose a set of test languages

        return Stream
                .of(new LanguageTestCase("es", "US", "United States", "Language:es:US", 4913155),
                        new LanguageTestCase("ep", "", "", "Language:ep:", 5292269),
                        new LanguageTestCase("sg", "SAG", "", "Language:sg:SAG", 4713118),
                        new LanguageTestCase("en", "NZ", "New Zealand", "Language:en:NZ", 5382528))
                .toArray();
    }

    static class LanguageTestCase {
         final String expectedSeed;
         final long expectedHash;
         final String langCode;
         final String countryCode;
         final String country;

         LanguageTestCase(final String langCode, final String countryCode, String country,final String expectedSeed, final long expectedHash) {
            this.langCode = langCode;
            this.countryCode = countryCode;
            this.country = country;
            this.expectedSeed = expectedSeed;
            this.expectedHash = expectedHash;
        }

        @Override
        public String toString() {
            return "LanguageTestCase{" +
                    "expectedSeed='" + expectedSeed + '\'' +
                    ", expectedHash=" + expectedHash +
                    ", langCode='" + langCode + '\'' +
                    ", countryCode='" + countryCode + '\'' +
                    ", country='" + country + '\'' +
                    '}';
        }
    }

    /**
     * Test Two separate Content-Types sharing a pretty much identical structure dont generate a conflict identifier wise
     */
    @Test
    public void Test_Similar_Content_Type_Wont_Clash() {
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
            final ContentType contentGenericType1 = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(new FieldDataGen().name("title").velocityVarName("title").next())
                    .field(new FieldDataGen().name("body").velocityVarName("body").next())
                    .field(new FieldDataGen().name("bin1").velocityVarName("bin1").next())
                    .field(new FieldDataGen().name("bin2").velocityVarName("bin2").next())
                    .nextPersisted();

            final ContentType contentGenericType2 = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(new FieldDataGen().name("title").velocityVarName("title").next())
                    .field(new FieldDataGen().name("body").velocityVarName("body").next())
                    .field(new FieldDataGen().name("bin1").velocityVarName("bin1").next())
                    .field(new FieldDataGen().name("bin2").velocityVarName("bin2").next())
                    .nextPersisted();

            assertTrue(defaultGenerator.isDeterministicId(contentGenericType1.id()));
            assertTrue(defaultGenerator.isDeterministicId(contentGenericType2.id()));

            contentGenericType1.fields().forEach(field -> {
                assertTrue(defaultGenerator.isDeterministicId(field.id()));
            });

            contentGenericType2.fields().forEach(field -> {
                assertTrue(defaultGenerator.isDeterministicId(field.id()));
            });

        }finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }

    }

    /**
     * This is small test to verify the seed used to generate categories looks ok
     * @throws Exception
     */
    @Test
    public void Test_Category_Path_Seed_And_Id() throws Exception{
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
            final CategoryAPI api = APILocator.getCategoryAPI();
            final String parentName = "Parent:" + System.currentTimeMillis();
            //Create First Child Category.
            final Category parent = new Category();
            parent.setCategoryName(parentName);
            parent.setKey("key");
            parent.setCategoryVelocityVarName(parentName);
            parent.setSortOrder(1);
            parent.setKeywords(null);

            final String child1Name = "Child1:" + System.currentTimeMillis();

            final Category child1 = new Category();
            child1.setCategoryName(child1Name);
            child1.setKey("key");
            child1.setCategoryVelocityVarName(child1Name);
            child1.setSortOrder(1);
            child1.setKeywords(null);

            final String child2Name = "Child2:" + System.currentTimeMillis();

            final Category child2 = new Category();
            child2.setCategoryName(child2Name);
            child2.setKey("key");
            child2.setCategoryVelocityVarName(child2Name);
            child2.setSortOrder(1);
            child2.setKeywords(null);

            api.save(null, parent, APILocator.systemUser(), false);
            api.save(parent, child1, APILocator.systemUser(), false);
            api.save(child1, child2, APILocator.systemUser(), false);

            String out = defaultGenerator.deterministicIdSeed(parent, null);
            assertEquals(String.format("Category:{%s}", parentName), out);
            out = defaultGenerator.deterministicIdSeed(child1, parent);
            assertEquals(String.format("Category:{%s > %s}", parentName, child1Name), out);

            out = defaultGenerator.deterministicIdSeed(child2, child1);
            assertEquals(String.format("Category:{%s > %s > %s}", parentName, child1Name, child2Name), out);

        }finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    /**
     * Method to test: {@link DeterministicIdentifierAPIImpl#resolveName(Field, Supplier)}
     * Given Scenario: We should take into consideration the field type when generating the deterministic Id.
     * ExpectedResult: The seed should contain the field type
     *
     */
    @Test
    public void test_resolveName_seedShouldContainFieldType(){
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            final Field relationshipField = new FieldDataGen().name("test").velocityVarName("test").type(RelationshipField.class).defaultValue(null)
                    .type(RelationshipField.class)
                    .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                    .relationType("Comments")
                    .next();

            final ContentType contentType = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(relationshipField)
                    .nextPersisted();

            //verify is deterministic
            assertTrue(defaultGenerator.isDeterministicId(contentType.id()));


            //verify the seed contains the field type
            for(final Field field : contentType.fields()){
                 final String expected = String.format("%s:%s", field.variable(), field.typeName());
                assertEquals(expected, defaultGenerator.resolveName(field, field::variable));
            }

        }finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void Test_Category_None_Persisted_Category_Should_Return_Deterministic_Id() throws Exception{

            final String name = "Root:" + System.currentTimeMillis();
            //Create First Child Category.
            final Category category = new Category();
            category.setCategoryName(name);
            category.setKey("key");
            category.setCategoryVelocityVarName(name);
            category.setSortOrder(1);
            category.setKeywords(null);

            String out = defaultGenerator.deterministicIdSeed(category,null);
            assertEquals(String.format("Category:{%s}", name), out);

            final String identifier1 = defaultGenerator.generateDeterministicIdBestEffort(category,(Category) null);
            assertTrue(defaultGenerator.isDeterministicId(identifier1));
    }


    /**
     * Given Scenario: Generate id for folders having {@link Host} or another {@link Folder} as parent
     * methodToTest {@link DeterministicIdentifierAPIImpl#generateDeterministicIdBestEffort(Folder, Treeable)}
     * Expected Results: The method must be idempotent for a given set of inputs the outcome should always remain the same and the id returned must be valid
     */
    @Test
    public void Test_Generate_Folder_Identifier() throws DotDataException {
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            final Host systemHost = APILocator.getHostAPI().findSystemHost();

            final String hostName = String.format("my.host%s.com", System.currentTimeMillis());

            final Host site = new SiteDataGen().name(hostName).nextPersisted();

            Folder folder = new FolderDataGen().site(site).next();

            final String generatedId1 = defaultGenerator
                    .generateDeterministicIdBestEffort(folder, site);
            assertTrue(UUIDUtil.isUUID(generatedId1));
            final String generatedId2 = defaultGenerator
                    .generateDeterministicIdBestEffort(folder,
                            site);
            //Test it is idempotent
            assertEquals(generatedId1, generatedId2);

            final Folder parentFolder = new FolderDataGen().site(site).nextPersisted();
            //Get Id for folder having a parent folder
            assertTrue(UUIDUtil.isUUID(defaultGenerator
                    .generateDeterministicIdBestEffort(folder, parentFolder)));

            folder = new FolderDataGen().site(systemHost).next();
            //Get Id for folder with SYSTEM_HOST as parent
            assertTrue(UUIDUtil.isUUID(defaultGenerator
                    .generateDeterministicIdBestEffort(folder, systemHost)));



        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

}
