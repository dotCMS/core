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
import com.dotmarketing.portlets.languagesmanager.business.LanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

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
        "es-es",37
    );

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        seedLanguages();
    }

    /**
     * Given scenario: we copy the messages directory to the expected location and run the upgrade task
     * Expected result: the upgrade task should run without errors and the expected results should be ingested into the system
     * @throws DotDataException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testExecuteUpgrade() throws DotDataException, IOException, URISyntaxException {
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

            summary.success().forEach((language, inodes) -> {
                final String isoCode = language.getIsoCode();
                assertTrue("Missing isoCode: " + isoCode, expectedResults.containsKey(isoCode));
                assertEquals("Number of ingested lines is not the same as the expected", expectedResults.get(isoCode).intValue(), inodes.size());
            });
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
          assertEquals("There must be 4 successfully processed Locales", 4, summary.success().size());
          assertEquals("There must be no errors", 0, summary.fails().size());
        } finally {
          final Optional<ImmutableMigrationSummary> migrationSummary = dataTask.getMigrationSummary();
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
            new LanguageDataGen().persist(new Language(new Random().nextLong(), languageCode, countryCode, languageName, countryName));
        }
    }

}
