package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.languagevariable.business.ImmutableMigrationSummary;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
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
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static com.liferay.util.StringPool.BLANK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
          assertTrue("There must be at least 5 successfully processed Locales", summary.success().size() >= 5);
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
            assertTrue("There must be at least 5 successfully processed Locales in the second run",
                    secondTaskSummary.success().size() >= 5);
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
        createLangVariantInNotExists("en", "ca", "English", "Canada");
        // Create French variant
        createLangVariantInNotExists("fr", "fr", "French", "French");
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
