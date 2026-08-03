package com.dotmarketing.business;

import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_1;
import static com.dotcms.datagen.TestDataUtils.FILE_ASSET_2;
import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;
import static com.dotmarketing.business.DeterministicIdentifierAPIImpl.GENERATE_DETERMINISTIC_IDENTIFIERS;
import static com.dotmarketing.business.DeterministicIdentifierAPIImpl.NON_DETERMINISTIC_IDENTIFIER;
import static com.dotmarketing.quartz.DotStatefulJob.EXECUTION_DATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
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
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJob;
import com.dotmarketing.quartz.job.TestJobExecutor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Expected deterministic id seed for a field: {@code variable:typeName:dataType}.
     */
    private static String expectedFieldSeed(final Field field) {
        return String.format("%s:%s:%s", field.variable(), field.typeName(), field.dataType().value);
    }


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

            // RowField and ColumnField are layout-only fields that may already exist in the DB
            // with pre-assigned deterministic IDs (e.g. from system initialization). Including them
            // causes isFieldInode() to find the hash taken and fall back to a random UUID, breaking
            // idempotency. Since they carry no content semantics, they are excluded from this check.
            for(final Field field : testCase.contentType.fields().stream()
                    .filter(f -> !(f instanceof RowField) && !(f instanceof ColumnField)).collect(Collectors.toList())){

                final String fieldIdentifier1 = defaultGenerator.generateDeterministicIdBestEffort(field, field::variable);
                assertTrue(UUIDUtil.isUUID(fieldIdentifier1));
                final String fieldIdentifier2 = defaultGenerator.generateDeterministicIdBestEffort(field, field::variable);
                //Test it is idempotent
                assertEquals(fieldIdentifier1, fieldIdentifier2);
                final String expected = expectedFieldSeed(field);
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
                 final String expected = expectedFieldSeed(field);
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

    /**
     * Method to test: {@link DeterministicIdentifierAPIImpl#generateDeterministicIdBestEffort(Field, Supplier)}
     * Given Scenario: A Text field is created with data type TEXT, then deleted, then re-created with the
     * same variable name but data type INTEGER (the scenario behind issue #36636).
     * ExpectedResult: The re-created field must get a DIFFERENT deterministic id (the data type is part of
     * the seed) and its data type must be INTEGER.
     */
    @Test
    public void Test_Delete_And_Recreate_Field_With_Different_DataType_Generates_New_Id() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            final User systemUser = APILocator.systemUser();
            final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
            final String fieldVarName = "myField" + System.currentTimeMillis();

            final Field textField = new FieldDataGen()
                    .type(TextField.class)
                    .name(fieldVarName)
                    .velocityVarName(fieldVarName)
                    .dataType(DataTypes.TEXT)
                    .next();

            final ContentType contentType = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(textField)
                    .nextPersisted();
            try {
                final Field originalField = fieldAPI
                        .byContentTypeIdAndVar(contentType.id(), fieldVarName);
                assertEquals(DataTypes.TEXT, originalField.dataType());

                fieldAPI.delete(originalField);

                final Field recreatedField = fieldAPI.save(FieldBuilder.builder(TextField.class)
                        .name(fieldVarName)
                        .variable(fieldVarName)
                        .contentTypeId(contentType.id())
                        .dataType(DataTypes.INTEGER)
                        .build(), systemUser);

                assertEquals(DataTypes.INTEGER, recreatedField.dataType());
                assertNotEquals(
                        "A field re-created with the same variable but a different data type must get a different deterministic id",
                        originalField.id(), recreatedField.id());
            } finally {
                ContentTypeDataGen.remove(contentType);
            }
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    /**
     * Method to test: {@link com.dotcms.contenttype.business.ContentTypeAPI#save(ContentType, List)}
     * Given Scenario: Replicates the push-publish receiver flow for issue #36636.
     * <p>
     * The receiver holds a TextField with dataType=TEXT whose deterministic id was seeded as
     * {@code contentType:variable:TextField:text}. The sender deleted that field and re-created it
     * with dataType=INTEGER, producing a different deterministic id seeded as
     * {@code contentType:variable:TextField:integer}. When the sender pushes the content type, the
     * receiver gets a field list where the same variable maps to a different id.
     * <p>
     * This also covers the mixed-version case: a sender running old code (no dataType in the
     * seed) generates {@code contentType:variable:TextField} as the seed, which again differs
     * from the receiver's stored id. In both cases the contract is the same: the receiver must
     * recognise the variable match, drop the stale field, and persist the incoming one.
     * <p>
     * ExpectedResult: The old field is removed, and the incoming field (new id, new dataType) is
     * persisted — the dataType change is never silently dropped.
     */
    @Test
    public void Test_ContentType_Save_Applies_DataType_Change_When_Field_Id_Differs() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            final User systemUser = APILocator.systemUser();
            final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
            final String fieldVarName = "myField" + System.currentTimeMillis();

            final Field textField = new FieldDataGen()
                    .type(TextField.class)
                    .name(fieldVarName)
                    .velocityVarName(fieldVarName)
                    .dataType(DataTypes.TEXT)
                    .next();

            final ContentType contentType = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .field(textField)
                    .nextPersisted();
            try {
                // Receiver's stored field: id seeded from contentType:variable:TextField:text
                final Field originalField = fieldAPI
                        .byContentTypeIdAndVar(contentType.id(), fieldVarName);
                assertEquals(DataTypes.TEXT, originalField.dataType());

                // Build the incoming (sender-side) field without a pre-set id so the
                // deterministic API can compute it from the seed contentType:variable:TextField:integer.
                // That seed differs from the receiver's stored seed (:text vs :integer), so the
                // resulting id will differ — exactly the push-publish mismatch we want to test.
                final Field incomingFieldTemplate = FieldBuilder.builder(TextField.class)
                        .name(fieldVarName)
                        .variable(fieldVarName)
                        .contentTypeId(contentType.id())
                        .dataType(DataTypes.INTEGER)
                        .build();
                final String senderSideId = APILocator.getDeterministicIdentifierAPI()
                        .generateDeterministicIdBestEffort(incomingFieldTemplate, () -> fieldVarName);
                final Field recreatedField = FieldBuilder.builder(incomingFieldTemplate)
                        .id(senderSideId)
                        .build();

                assertNotEquals(
                        "Sender id (seeded with :integer) must differ from receiver id (seeded with :text)",
                        originalField.id(), recreatedField.id());

                // The content type has exactly one field (CONTENT base type has no required fields),
                // so we pass only the incoming field — the old one is replaced entirely.
                contentTypeAPI.save(contentType, List.of(recreatedField));

                final Field savedField = fieldAPI
                        .byContentTypeIdAndVar(contentType.id(), fieldVarName);
                assertEquals(DataTypes.INTEGER, savedField.dataType());
                assertEquals(recreatedField.id(), savedField.id());
                assertNotEquals(originalField.id(), savedField.id());
            } finally {
                ContentTypeDataGen.remove(contentType);
            }
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    /**
     * Method to test: {@link DeterministicIdentifierAPIImpl#resolveName(Field, Supplier)}
     * Given Scenario: Two fields share the same variable and field type but differ on data type.
     * ExpectedResult: The seed always includes the data type ({@code variable:typeName:dataType}),
     * so fields with the same variable but different data types produce different seeds.
     */
    @Test
    public void Test_ResolveName_Seed_Includes_DataType_When_Present() {
        final String fieldVarName = "seedField";

        final Field textDataTypeField = FieldBuilder.builder(TextField.class)
                .name(fieldVarName)
                .variable(fieldVarName)
                .contentTypeId("fakeContentTypeId")
                .dataType(DataTypes.TEXT)
                .build();

        final Field integerDataTypeField = FieldBuilder.builder(TextField.class)
                .name(fieldVarName)
                .variable(fieldVarName)
                .contentTypeId("fakeContentTypeId")
                .dataType(DataTypes.INTEGER)
                .build();

        final String textSeed = defaultGenerator
                .resolveName(textDataTypeField, textDataTypeField::variable);
        final String integerSeed = defaultGenerator
                .resolveName(integerDataTypeField, integerDataTypeField::variable);

        assertEquals(String.format("%s:%s:%s", fieldVarName, textDataTypeField.typeName(),
                DataTypes.TEXT.value), textSeed);
        assertEquals(String.format("%s:%s:%s", fieldVarName, integerDataTypeField.typeName(),
                DataTypes.INTEGER.value), integerSeed);
        assertNotEquals(
                "Fields with the same variable but different data types must produce different seeds",
                textSeed, integerSeed);
    }

    /**
     * Method to test: {@link com.dotcms.contenttype.business.FieldFactory#save(Field)}
     * Given Scenario: A TagField (whose acceptedDataTypes is exclusively SYSTEM) is saved twice via
     * the API — once with an explicit {@code dataType=TEXT} in the payload (e.g. a direct REST call
     * or an import file), and once with no dataType (defaulting to SYSTEM). Both payloads produce
     * the same persisted row after {@code normalizeData} forces the dataType to SYSTEM.
     * Expected Result: Both saves must produce the same deterministic id, because the id is seeded
     * from the normalised field (dataType=SYSTEM) rather than the raw incoming payload.
     */
    @Test
    public void Test_TagField_DeterministicId_Is_Independent_Of_Incoming_DataType() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);

            final User systemUser = APILocator.systemUser();
            final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
            final String fieldVarName = "tags" + System.currentTimeMillis();

            final ContentType contentType = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .nextPersisted();
            try {
                // Save a TagField with an explicit dataType=TEXT (as a REST API caller might send)
                final Field savedWithText = fieldAPI.save(
                        FieldBuilder.builder(TagField.class)
                                .name(fieldVarName)
                                .variable(fieldVarName)
                                .contentTypeId(contentType.id())
                                .dataType(DataTypes.TEXT)
                                .build(),
                        systemUser);

                assertEquals("normalizeData must force dataType to SYSTEM regardless of payload",
                        DataTypes.SYSTEM, savedWithText.dataType());

                fieldAPI.delete(savedWithText);

                // Save the same TagField without specifying dataType (defaults to SYSTEM)
                final Field savedWithDefault = fieldAPI.save(
                        FieldBuilder.builder(TagField.class)
                                .name(fieldVarName)
                                .variable(fieldVarName)
                                .contentTypeId(contentType.id())
                                .build(),
                        systemUser);

                assertEquals("normalizeData must force dataType to SYSTEM",
                        DataTypes.SYSTEM, savedWithDefault.dataType());

                assertEquals(
                        "TagField id must be the same regardless of whether the payload carries dataType=TEXT or omits it",
                        savedWithText.id(), savedWithDefault.id());
            } finally {
                ContentTypeDataGen.remove(contentType);
            }
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

    /**
     * Method to test: {@link CleanUpFieldReferencesJob#run}
     * Given Scenario: The receiver holds two TEXT fields — {@code myField} with a legacy id
     * (generated before dataType was added to the seed, simulated by disabling deterministic
     * generation) and {@code genuineField} with its own legacy id. A push-publish bundle
     * arrives that replaces {@code myField} with a new-recipe id (same variable, same dataType)
     * but carries no entry for {@code genuineField}, making it a genuine delete. Both fields
     * originally mapped to the same db column set (text1, text2). After the save,
     * the incoming {@code myField} is assigned text1 (recycled from the deleted legacy field).
     * CleanUpFieldReferencesJob is then run manually for each deleted field.
     * <p>
     * Two assertions together prove correctness:
     * <ol>
     *   <li>The job skips text1 for {@code myField} because the same-variable guard fires
     *       — column recycled by the same variable means the replacement field's content must
     *       be preserved.</li>
     *   <li>The job clears text2 for {@code genuineField} — the guard does NOT fire for a
     *       different variable, proving the job actually ran (no vacuous pass).</li>
     * </ol>
     */
    @Test
    public void Test_CleanUpFieldJob_SkipsCleanup_WhenColumnRecycledBySameVariable() throws Exception {
        prepareIfNecessary();
        final boolean generateConsistentIdentifiers = Config
                .getBooleanProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
        try {
            final User systemUser = APILocator.systemUser();
            final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
            final String myFieldVar     = "myField"      + System.currentTimeMillis();
            final String genuineFieldVar = "genuineField" + System.currentTimeMillis();
            final String myFieldContent      = "hello world "    + System.currentTimeMillis();
            final String genuineFieldContent = "genuine content " + System.currentTimeMillis();

            // Create both fields with deterministic id OFF to get legacy (random) ids,
            // simulating fields created before dataType was added to the seed.
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, false);
            final ContentType contentType = new ContentTypeDataGen()
                    .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                    .baseContentType(BaseContentType.CONTENT)
                    .nextPersisted();
            try {
                // myField lands on text1 (first TEXT column), genuineField on text2.
                final Field legacyField = fieldAPI.save(
                        FieldBuilder.builder(TextField.class)
                                .name(myFieldVar).variable(myFieldVar)
                                .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build(),
                        systemUser);

                final Field genuineField = fieldAPI.save(
                        FieldBuilder.builder(TextField.class)
                                .name(genuineFieldVar).variable(genuineFieldVar)
                                .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build(),
                        systemUser);

                // Create a contentlet with data in both fields.
                final Contentlet contentlet = new ContentletDataGen(contentType.id())
                        .setProperty(myFieldVar, myFieldContent)
                        .setProperty(genuineFieldVar, genuineFieldContent)
                        .nextPersisted();
                assertEquals(myFieldContent,      contentlet.get(myFieldVar));
                assertEquals(genuineFieldContent, contentlet.get(genuineFieldVar));

                // Switch to new-recipe generation and compute the sender-side id for myField.
                Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, true);
                final Field incomingTemplate = FieldBuilder.builder(TextField.class)
                        .name(myFieldVar).variable(myFieldVar)
                        .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();
                final String newRecipeId = APILocator.getDeterministicIdentifierAPI()
                        .generateDeterministicIdBestEffort(incomingTemplate, () -> myFieldVar);
                final Field incomingField = FieldBuilder.builder(incomingTemplate)
                        .id(newRecipeId).build();

                assertNotEquals("Legacy and new-recipe ids must differ to trigger the replace flow",
                        legacyField.id(), incomingField.id());

                // Simulate the push: only incomingField arrives.
                // legacyField (text1) and genuineField (text2) are both deleted; incomingField
                // is inserted and nextAvailableColumn recycles text1.
                contentTypeAPI.save(contentType, List.of(incomingField));

                // Confirm text1 was recycled by incomingField — key precondition.
                final Field savedIncoming = fieldAPI.byContentTypeIdAndVar(contentType.id(), myFieldVar);
                assertEquals(
                        "incomingField must land on the same column as the deleted legacyField",
                        legacyField.dbColumn(), savedIncoming.dbColumn());

                final Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
                final CleanUpFieldReferencesJob job = new CleanUpFieldReferencesJob();

                // Run job for legacyField: same-variable guard must fire and skip text1.
                Map<String, Object> props = new HashMap<>();
                props.put(EXECUTION_DATA, ImmutableMap.of(
                        "field", legacyField, "deletionDate", futureDate, "user", systemUser));
                TestJobExecutor.execute(job, props);

                // Run job for genuineField: different variable, no guard — text2 must be cleared.
                props = new HashMap<>();
                props.put(EXECUTION_DATA, ImmutableMap.of(
                        "field", genuineField, "deletionDate", futureDate, "user", systemUser));
                TestJobExecutor.execute(job, props);

                // text1 (myField / incomingField) must be preserved — same-variable guard fired.
                final Contentlet refreshed = APILocator.getContentletAPI()
                        .find(contentlet.getInode(), systemUser, false);
                assertEquals(
                        "Column recycled by the same variable must not be cleared",
                        myFieldContent, refreshed.get(myFieldVar));

                // text2 (genuineField) must be cleared — proves the job ran AND that the guard
                // correctly left the genuine delete path unprotected.
                final DotConnect dc = new DotConnect();
                dc.setSQL("SELECT " + genuineField.dbColumn() + " FROM contentlet WHERE inode = ?");
                dc.addParam(contentlet.getInode());
                final Object genuineColumnValue = dc.loadObjectResults()
                        .getFirst().get(genuineField.dbColumn());
                assertNull(
                        "Job must have cleared the genuine field column, confirming it ran",
                        genuineColumnValue);

            } finally {
                ContentTypeDataGen.remove(contentType);
            }
        } finally {
            Config.setProperty(GENERATE_DETERMINISTIC_IDENTIFIERS, generateConsistentIdentifiers);
        }
    }

}
