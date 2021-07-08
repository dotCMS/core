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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
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
     * Given scenario: We have created contentlets having turned off the deterministic id generation therefore it all comes with random ids
     * Meaning that any deterministic id request does not exist in the database
     * Expected behavior: The best effort must give us deterministic ids unitl they're inserted on te database.
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
            //We also check the asset name is what we expect too
            assertEquals(testCase.expectedName,
                    defaultGenerator.resolveAssetName(testCase.versionable));
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
                new AssetTestCase(folder, site, folder.getName(), "Folder", site),
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

    @Test
    @UseDataProvider("getContentTypeTestCases")
    public void Test_create_Content_Type(final ContentTypeTestCase testCase) {
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
        final List<LanguageTestCase> testCases = Stream
                .of(new LanguageTestCase("es", "US", "United States", "Language:es:US", 4913155),
                    new LanguageTestCase("ep", "", "", "Language:ep:", 5292269),
                    new LanguageTestCase("ru", "RUS", "", "Language:ru:RUS", 5066818),
                    new LanguageTestCase("en", "NZ", "New Zealand", "Language:en:NZ", 5382528))
                .collect(Collectors.toList());

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final Language defaultLanguage = languageAPI.getDefaultLanguage();
        final Iterator<LanguageTestCase> iterator = testCases.iterator();
        while (iterator.hasNext()) {
            final LanguageTestCase testCase = iterator.next();
            final Language language = languageAPI
                    .getLanguage(testCase.langCode, testCase.countryCode);
            if (null != language && language.getId() == defaultLanguage.getId()) {
                //Exclude the default language from our test data set (is not included just in case)
                iterator.remove();
            } else {
                if (null != language) {
                    try {
                        //If the language already exists remove it
                        languageAPI.deleteLanguage(language);
                    } catch (Exception e) {
                        Logger.error(DeterministicIdentifierAPIImpl.class,String.format("Failed to remove language `%s-%s` prior to execute test ",language.getLanguageCode(), language.getCountryCode()), e);
                        //if we fail to remove it from the db. then Exclude it from the test data set.
                        iterator.remove();
                    }
                }
            }
        }

        return testCases.toArray();
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

}
