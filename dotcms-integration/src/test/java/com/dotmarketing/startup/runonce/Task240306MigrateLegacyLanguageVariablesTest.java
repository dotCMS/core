package com.dotmarketing.startup.runonce;

import static com.liferay.util.StringPool.BLANK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.languagevariable.business.ImmutableMigrationSummary;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.languagevariable.business.LegacyLangVarMigrationHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the {@link Task240306MigrateLegacyLanguageVariables} data task runs as expected.
 *
 * @author Fabrizzio Araya, Jose Castro
 * @since Mar 15th, 2024
 */
public class Task240306MigrateLegacyLanguageVariablesTest {

    final Map<String,Integer> expectedResults = Map.of(
        "en", 38,
        "en-us",0,
        "fr-fr",23,
        "es-es",34
    );

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        seedLanguages();
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task240306MigrateLegacyLanguageVariables#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Run the Data Task as it would when dotCMS is being updated
     *     .</li>
     *     <li><b>Expected Result: </b>Running the Data Task should not cause any errors.</li>
     * </ul>
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @Test
    public void testExecuteUpgrade() throws DotDataException {
        final Task240306MigrateLegacyLanguageVariables dataTask = new Task240306MigrateLegacyLanguageVariables();
        assertTrue("This Data Task must always run", dataTask.forceRun());
        assertTrue("The migration summary object should not exist before running the task",
                dataTask.getMigrationSummary().isEmpty());
        try {
            dataTask.executeUpgrade();
            final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
            assertTrue("There must be a migration summary after the task execution",
                    dataTask.getMigrationSummary().isPresent());
            final ImmutableMigrationSummary summary = migrationSummary.get();
            assertFalse("There must be migrated Language Variables", summary.success().isEmpty());

            final ImmutableList<Locale> locales = summary.nonExistingLanguages();
            // Verify the languages we were not able to find
            assertTrue(locales.contains(Locale.SIMPLIFIED_CHINESE));

            // There are other ITs that create random languages. So let's check ONLY for our
            // expected languages
            final Set<String> expectedLanguages = new HashSet<>();
            summary.success().forEach((language, addedKeys) -> {
                final String isoCode = language.getIsoCode();
                if (expectedResults.containsKey(isoCode)) {
                    expectedLanguages.add(isoCode);
                }
            });
            assertEquals("The expected languages must be present", expectedResults.size(), expectedLanguages.size());
        } finally {
            final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
            migrationSummary.ifPresent(this::cleanup);
        }
    }

    /**
     * Given scenario: We simulate the case where the language variable content type is dropped
     * Expected result: the upgrade task should run without errors and recreate the language variable content type when missing. We run a basic check to verify the task ran successfully
     * @throws DotDataException   if an error occurs
     * @throws DotSecurityException if a security violation occurs
     */
    @Test
    public void testDropThenRecreateLanguageVariableContentType() throws DotDataException, DotSecurityException {
        final Task240306MigrateLegacyLanguageVariables dataTask = new Task240306MigrateLegacyLanguageVariables();
        assertTrue("This Data Task must always run", dataTask.forceRun());
        try {
          removeLanguageVariableContentType();
          final Optional<String> optional = dataTask.checkContentType();
          assertTrue("The Language Variable Content Type must always be present", optional.isPresent());
          assertTrue("The migration summary object should not exist before running the task",
                  dataTask.getMigrationSummary().isEmpty());
          dataTask.executeUpgrade();
          assertTrue("There must be a migration summary after the task execution",
                  dataTask.getMigrationSummary().isPresent());
          final ImmutableMigrationSummary summary = dataTask.getMigrationSummary().get();
            assertTrue("There must be at least 4 successfully processed Locales", summary.success().size() >= 4);
          assertEquals("There must be no errors", 0, summary.fails().size());
        } finally {
          final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
          migrationSummary.ifPresent(this::cleanup);
      }
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task240306MigrateLegacyLanguageVariables#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Run the Data Task twice.</li>
     *     <li><b>Expected Result: </b>Running the Data Task more than once should not cause any
     *     issue.</li>
     * </ul>
     *
     * @throws DotDataException An error occurred when interacting with the database
     */
    @Test
    public void testDataTaskIdempotency() throws DotDataException {
        final Task240306MigrateLegacyLanguageVariables dataTaskFirstInstance = new Task240306MigrateLegacyLanguageVariables();
        final Task240306MigrateLegacyLanguageVariables dataTaskSecondInstance = new Task240306MigrateLegacyLanguageVariables();
        try {
            assertTrue("The first migration summary object should not exist before running the task",
                    dataTaskFirstInstance.getMigrationSummary().isEmpty());
            dataTaskFirstInstance.executeUpgrade();
            assertTrue("There must be a first migration summary after the task execution",
                    dataTaskFirstInstance.getMigrationSummary().isPresent());
            final ImmutableMigrationSummary firstTaskSummary = dataTaskFirstInstance.getMigrationSummary().get();
            assertTrue("There must be at least 4 successfully processed Locales in the first run", firstTaskSummary.success().size() >= 4);
            assertEquals("There must be no errors in the first run", 0, firstTaskSummary.fails().size());

            assertTrue("The second migration summary object should not exist before running the task",
                    dataTaskSecondInstance.getMigrationSummary().isEmpty());
            dataTaskSecondInstance.executeUpgrade();
            assertTrue("There must be a second migration summary after the task execution",
                    dataTaskSecondInstance.getMigrationSummary().isPresent());
            final ImmutableMigrationSummary secondTaskSummary = dataTaskSecondInstance.getMigrationSummary().get();
            assertTrue("There must be at least 4 successfully processed Locales in the second run",
                    secondTaskSummary.success().size() >= 4);
            assertEquals("There must be no errors in the second run", 0, secondTaskSummary.fails().size());

            // There are other ITs that create random languages. So let's check ONLY for our
            // expected languages
            final Set<String> expectedLanguages = new HashSet<>();
            secondTaskSummary.success().forEach((language, addedKeys) -> {
                final String isoCode = language.getIsoCode();
                if (expectedResults.containsKey(isoCode)) {
                    expectedLanguages.add(isoCode);
                }
                assertEquals("No entries should've been updated", 0, addedKeys.size());
            });
            assertEquals("The expected languages must be present", expectedResults.size(), expectedLanguages.size());
        } finally {
            final Optional<ImmutableMigrationSummary> migrationSummary = dataTaskFirstInstance.getMigrationSummary();
            migrationSummary.ifPresent(this::cleanup);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link Task240306MigrateLegacyLanguageVariables#executeUpgrade()}</li>
     *     <li><b>Given Scenario: </b>Two properties files exist:
     *     cms_language_en.properties (33 entries - 32 shared + 1 unique) and cms_language_en_US.properties (37 entries - 32 shared + 5 unique).
     *     The root 'en' language (no country) does NOT exist in the system (removed before test).
     *     Only the 'en_US' language exists. According to migration rules, when root language doesn't exist,
     *     both files map to the most specific available language (en_US in this case).</li>
     *     <li><b>Expected Result: </b>Both files map to en_US language. Files are processed in reverse alphabetical order,
     *     so cms_language_en_US.properties is processed first, creating 37 language variables.
     *     Then cms_language_en.properties is processed: 32 keys already exist (skipped), but 1 unique key is added.
     *     EN_US values take priority for duplicates. Total unique variables = 38 (37 from en_US + 1 unique from en).
     *     Root EN language is restored after test completion.
     *     No errors should occur.</li>
     * </ul>
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException An error occurred due to security constraints.
     */
    @Test
    public void testBothFilesMapToSameLanguageWithPriorityHandling() throws DotDataException, DotSecurityException, IOException {
        final Task240306MigrateLegacyLanguageVariables dataTask = new Task240306MigrateLegacyLanguageVariables();
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

        // Get the root 'en' language (created in seedLanguages with no country code)
        final Language rootEnLanguage = languageAPI.getLanguage("en", BLANK);
        assertNotNull("Root English language must exist before test for removal", rootEnLanguage);

        // Get the 'en_US' language
        final Language enUSLanguage = languageAPI.getLanguage("en", "US");
        assertNotNull("English (United States) language must exist for this test", enUSLanguage);

        // Backup existing messages directory files before modifying
        final Map<String, byte[]> backupFiles = backupMessagesDirectory();

        // Track if root EN language needs restoration
        boolean rootEnLanguageRemoved = false;

        try {
            // Remove root EN language and its associated content
            rootEnLanguageRemoved = removeLanguageAndContent(rootEnLanguage);

            // Verify root EN language no longer exists
            final Language verifyRemoved = languageAPI.getLanguage("en", BLANK);
            assertNull("Root EN language should not exist for this test", verifyRemoved);

            // Copy test language variable files from test resources to the messages directory
            // where the migration task will read them
            copyTestLanguageFiles();

            // Remove any existing language variable contentlets to ensure clean state
            removeExistingLanguageVariables();

            // Calculate expected count: total unique keys from both files
            // Both files will map to en_US since root en doesn't exist
            // en_US has 37 entries, en has 33 entries (32 duplicates + 1 unique)
            // Expected: 38 unique keys (en_US values take priority for duplicates)
            final String enUSFilePath = "lang-vars/cms_language_en_US.properties";
            final String enFilePath = "lang-vars/cms_language_en.properties";
            final long expectedUniqueKeysCount = getExpectedUniqueKeysCount(enUSFilePath, enFilePath);

            assertTrue("This Data Task must always run", dataTask.forceRun());
            assertTrue("The migration summary object should not exist before running the task",
                    dataTask.getMigrationSummary().isEmpty());

            dataTask.executeUpgrade();

            final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
            assertTrue("There must be a migration summary after the task execution",
                    migrationSummary.isPresent());

            final ImmutableMigrationSummary summary = migrationSummary.get();

            // Verify no failures occurred
            assertEquals("There should be no failures during migration", 0, summary.fails().size());

            // Verify that en_US language has all the unique entries
            final List<String> enUSSuccessInodes = summary.success().get(enUSLanguage);
            assertTrue("There must be migrated variables for 'en_US' language",
                    enUSSuccessInodes != null && !enUSSuccessInodes.isEmpty());

            // Both files map to en_US, so total should be unique keys count (38: 37 from en_US + 1 from en)
            assertEquals("Should have " + expectedUniqueKeysCount + " unique language variables from both files",
                    expectedUniqueKeysCount, enUSSuccessInodes.size());

            // Verify specific key has value from en_US file (priority over en file)
            final String pagesValue = languageVariableAPI.getLanguageVariable(
                    "com.dotcms.javax.portlet.title.c-Pages", enUSLanguage.getId(), APILocator.systemUser());
            assertEquals("Language variable should have value from en_US file (priority)", "Pages", pagesValue);

            // Verify keys that only exist in en_US file
            final String destinationsValue = languageVariableAPI.getLanguageVariable(
                    "com.dotcms.javax.portlet.title.c_Destinations", enUSLanguage.getId(), APILocator.systemUser());
            assertEquals("Language variable from en_US exclusive entry should exist", "Destinations", destinationsValue);

            final String newsValue = languageVariableAPI.getLanguageVariable(
                    "com.dotcms.javax.portlet.title.c_News", enUSLanguage.getId(), APILocator.systemUser());
            assertEquals("Language variable from en_US exclusive entry should exist", "News", newsValue);

            // Verify key that only exists in en file (should still be created for en_US language)
            final String dashboardValue = languageVariableAPI.getLanguageVariable(
                    "com.dotcms.javax.portlet.title.c_Dashboard", enUSLanguage.getId(), APILocator.systemUser());
            assertEquals("Language variable from en exclusive entry should exist and be mapped to en_US", "Dashboard", dashboardValue);

            Logger.info(this, String.format("Successfully migrated %d unique language variables for en_US without errors. " +
                    "cms_language_en_US.properties (37 entries) was processed first, cms_language_en.properties (33 entries with 32 duplicates + 1 unique) added 1 more.",
                    enUSSuccessInodes.size()));
        } finally {
            final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
            migrationSummary.ifPresent(this::cleanup);

            // Restore root EN language if it was removed
            if (rootEnLanguageRemoved) {
                try {
                    createLangVariantInNotExists("en", BLANK, "English", BLANK);
                    Logger.info(this, "Restored root EN language after test");
                } catch (Exception e) {
                    Logger.error(this, "Failed to restore root EN language: " + e.getMessage(), e);
                }
            }

            // Always restore the messages directory to its original state
            restoreMessagesDirectory(backupFiles);
        }
    }

    /**
     * Given scenario: We simulate the case where the language variable content type is dropped
     * @throws DotSecurityException if a security violation occurs
     * @throws DotDataException if an error occurs
     */
    private void removeLanguageVariableContentType() throws DotSecurityException, DotDataException {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(
                APILocator.systemUser());
        final ContentType languageVariableCt = contentTypeAPI.find(
                LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        final boolean asyncDelete = Config.getBooleanProperty(
                ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, true);
        Config.setProperty(ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, false);
        contentTypeAPI.delete(languageVariableCt);
        Config.setProperty(ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, asyncDelete);
    }

    /**
     * Backs up existing files in the messages directory before modifying them.
     * @return Map of filename to file contents for restoration
     * @throws IOException if an error occurs reading files
     */
    private Map<String, byte[]> backupMessagesDirectory() throws IOException {
        final Path messagesDir = LegacyLangVarMigrationHelper.messagesDir();
        final Map<String, byte[]> backup = new HashMap<>();

        if (Files.exists(messagesDir)) {
            try (final var files = Files.list(messagesDir)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".properties"))
                        .forEach(p -> {
                            try {
                                backup.put(p.getFileName().toString(), Files.readAllBytes(p));
                            } catch (IOException e) {
                                Logger.warn(this, "Failed to backup file: " + p, e);
                            }
                        });
            }
        }

        Logger.info(this, String.format("Backed up %d files from messages directory", backup.size()));
        return backup;
    }

    /**
     * Restores the messages directory to its original state from the backup.
     * @param backupFiles Map of filename to file contents
     * @throws IOException if an error occurs restoring files
     */
    private void restoreMessagesDirectory(final Map<String, byte[]> backupFiles) throws IOException {
        final Path messagesDir = LegacyLangVarMigrationHelper.messagesDir();

        // First, remove all current .properties files
        if (Files.exists(messagesDir)) {
            try (final var files = Files.list(messagesDir)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".properties"))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                Logger.warn(this, "Failed to delete file during restore: " + p, e);
                            }
                        });
            }
        }

        // Then restore backed up files
        for (Map.Entry<String, byte[]> entry : backupFiles.entrySet()) {
            final Path targetPath = messagesDir.resolve(entry.getKey());
            Files.write(targetPath, entry.getValue());
        }

        Logger.info(this, String.format("Restored %d files to messages directory", backupFiles.size()));
    }

    /**
     * Copies test language variable files from test resources to the messages directory
     * where the migration task will read them.
     * @throws IOException if an error occurs copying files
     */
    private void copyTestLanguageFiles() throws IOException {
        final Path messagesDir = LegacyLangVarMigrationHelper.messagesDir();

        // Clear the messages directory to ensure clean state
        if (Files.exists(messagesDir)) {
            try (Stream<Path> messagesDirFiles = Files.walk(messagesDir)) {
                messagesDirFiles
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".properties"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            Logger.warn(this, "Failed to delete file: " + p);
                        }
                    });
            }
        } else {
            Files.createDirectories(messagesDir);
        }

        // List of test language files to copy
        final String[] testFiles = {
                "cms_language_en.properties",
                "cms_language_en_US.properties",
                "cms_language_en_CA.properties",
                "cms_language_es.properties",
                "cms_language_es_ES.properties",
                "cms_language_fr.properties",
                "cms_language_fr_FR.properties",
                "cms_language_eo.properties",
                "cms_language_ja.properties"
        };

        // Copy each file from test resources to messages directory
        for (final String fileName : testFiles) {
            final String resourcePath = "lang-vars/" + fileName;
            try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    final Path targetPath = messagesDir.resolve(fileName);
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    Logger.info(this, String.format("Copied test file: %s to %s", fileName, targetPath));
                }
            }
        }
    }
    
    /**
     * Reads both specified properties files and counts the total unique keys across both.
     * @param enUSFilePath Path to cms_language_en_US.properties in test resources
     * @param enFilePath Path to cms_language_en.properties in test resources
     * @return Count of unique keys across both files
     * @throws IOException if an error occurs reading the files
     */
    private long getExpectedUniqueKeysCount(String enUSFilePath, String enFilePath) throws IOException{
        final Set<String> allKeys = new HashSet<>();

        // Read keys from en_US file
        final var enUSResourceUrl = getClass().getClassLoader().getResource(enUSFilePath);
        assertNotNull("The en_US language variables file must exist in classpath", enUSResourceUrl);
        final Path enUSPath = Paths.get(enUSResourceUrl.getPath());
        try (final var lines = Files.lines(enUSPath)) {
            lines.filter(line -> !line.trim().isEmpty() && line.contains("="))
                    .forEach(line -> allKeys.add(line.substring(0, line.indexOf('='))));
        }

        // Read keys from en file (will add any unique keys, but there are none)
        final var enResourceUrl = getClass().getClassLoader().getResource(enFilePath);
        assertNotNull("The en language variables file must exist in classpath", enResourceUrl);
        final Path enPath = Paths.get(enResourceUrl.getPath());
        try (final var lines = Files.lines(enPath)) {
            lines.filter(line -> !line.trim().isEmpty() && line.contains("="))
                    .forEach(line -> allKeys.add(line.substring(0, line.indexOf('='))));
        }

        return allKeys.size();
    }

    /**
     * Removes all existing language variable contentlets to ensure clean state for testing.
     * @throws DotDataException if an error occurs querying or deleting contentlets
     * @throws DotSecurityException if a security violation occurs
     */
    private void removeExistingLanguageVariables() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        try {
            final ContentType languageVariableContentType = contentTypeAPI.find(
                    LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);

            if (languageVariableContentType != null) {
                final List<Contentlet> existingVariables = contentletAPI.search(
                        "+contentType:" + languageVariableContentType.variable(),
                        0, 0, null, APILocator.systemUser(), false);

                for (Contentlet contentlet : existingVariables) {
                    try {
                        contentletAPI.destroy(contentlet, APILocator.systemUser(), false);
                    } catch (Exception e) {
                        Logger.warn(this, "Failed to delete existing language variable: " +
                                contentlet.getIdentifier(), e);
                    }
                }

                Logger.info(this, String.format("Removed %d existing language variable contentlets",
                        existingVariables.size()));
            }
        } catch (Exception e) {
            Logger.warn(this, "Error removing existing language variables: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a language and all its associated content.
     * This is required before deleting a language, as languages with existing content cannot be removed.
     * @param language the language to remove
     * @throws DotDataException if an error occurs querying or deleting content
     * @throws DotSecurityException if a security violation occurs
     * @return true if the language was removed, false otherwise
     */
    private boolean removeLanguageAndContent(final Language language) throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();

        // Remove all content associated with the language
        final List<Contentlet> languageContent = contentletAPI.search(
                "+languageId:" + language.getId(),
                0, 0, null, APILocator.systemUser(), false);

        for (Contentlet contentlet : languageContent) {
            try {
                contentletAPI.destroy(contentlet, APILocator.systemUser(), false);
                return false;
            } catch (Exception e) {
                Logger.warn(this, "Failed to delete content for language " + language.getIsoCode() + ": " +
                        contentlet.getIdentifier(), e);
            }
        }

        // Now remove the language itself
        languageAPI.deleteLanguage(language);
        Logger.info(this, String.format("Removed language '%s' and %d associated content items",
                language.getIsoCode(), languageContent.size()));
        return true;
    }

    /**
     * Cleanup the contentlets created during the test
     * @param summary the migration summary
     */
    private void cleanup(final ImmutableMigrationSummary summary) {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        //Clean up required to avoid side effects
        summary.success().forEach((language, inodes) -> {
            inodes.forEach(inode -> {
                try {
                    final Contentlet contentlet = contentletAPI.find(inode, APILocator.systemUser(), false);
                    contentletAPI.destroy(contentlet, APILocator.systemUser(), false);
                } catch (DotDataException | DotSecurityException e) {
                    Logger.debug(this, e.getMessage(), e);
                }
            });
        });
    }

    /**
     * Seed the languages required for the test
     * @throws DotDataException if an error occurs
     */
    static void seedLanguages() throws DotDataException {
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        //Making sure Esperanto is not present
        final Optional<Language> eo = Optional.ofNullable(languageAPI.getLanguage("eo", BLANK));
        eo.ifPresent(languageAPI::deleteLanguage);
        // Create English variants
        createLangVariantInNotExists("en", BLANK, "English", BLANK);
        createLangVariantInNotExists("en", "us", "English", "United States");
        createLangVariantInNotExists("en", "ca", "English", "Canada");
        // Create French variant
        createLangVariantInNotExists("fr", "fr", "French", "French");
        // Create Spanish variant (required for test expectations)
        createLangVariantInNotExists("es", "es", "Spanish", "Spain");
    }

    /**
     * Create a language variant if it does not exist
     * @param languageCode the language code
     * @param countryCode the country code
     * @param languageName the language name
     * @param countryName the country name
     * @throws DotDataException if an error occurs
     */
    static void createLangVariantInNotExists(final String languageCode, final String countryCode, final String languageName, final String countryName) throws DotDataException {
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final List<Language> languages = languageAPI.getLanguages();
        final boolean exists = languages.stream().anyMatch(l -> l.getLanguageCode().equalsIgnoreCase(languageCode) && l.getCountryCode().equalsIgnoreCase(countryCode));
        if (!exists) {
            new UniqueLanguageDataGen().persist(new Language(new Random().nextLong(), languageCode, countryCode, languageName, countryName));
        }
    }

}
