package com.dotmarketing.startup.runonce;

import static com.dotmarketing.util.FileUtil.copyDir;
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
import com.dotmarketing.portlets.languagesmanager.business.LanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task240306MigrateLegacyLanguageVariablesTest {

    final Map<String,Integer> expectedResults = Map.of(
        "en", 32,
        "en-us",37,
        "fr-fr",50,
        "es-es",23,
        "en-ca", 37
    );

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        seedLanguages();
        copyMessageBundle(LegacyLangVarMigrationHelper.messagesDir());
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
        final Task240306MigrateLegacyLanguageVariables upgradeTask = new Task240306MigrateLegacyLanguageVariables();
        try {
            assertTrue(upgradeTask.forceRun());
            Assert.assertTrue(upgradeTask.getMigrationSummary().isEmpty());
            upgradeTask.executeUpgrade();
            Assert.assertTrue(upgradeTask.getMigrationSummary().isPresent());
            final Optional<ImmutableMigrationSummary> migrationSummary = upgradeTask.getMigrationSummary();
            Assert.assertTrue(migrationSummary.isPresent());
            ImmutableMigrationSummary summary = migrationSummary.get();
            Assert.assertFalse(summary.success().isEmpty());

            final ImmutableList<Locale> locales = summary.nonExistingLanguages();
            //Verify the languages we were not able to find
            Assert.assertTrue(locales.contains(Locale.JAPANESE));
            Assert.assertTrue(locales.contains(new Locale("eo", "")));
            Assert.assertTrue(locales.contains(new Locale("fr", "")));
            Assert.assertTrue(locales.contains(new Locale("es", "")));
            //Now verify the languages we were able to find
            Assert.assertTrue(summary.success().size() > 0);
            summary.success().forEach((language, inodes) -> {
                final String isoCode = language.getIsoCode();
                Assert.assertTrue("missing isoCode:"+isoCode, expectedResults.containsKey(isoCode));
                Assert.assertEquals("Number of ingested lines is not the same as the expected", expectedResults.get(isoCode).intValue(), inodes.size());
            });

        } finally {
            final Optional<ImmutableMigrationSummary> migrationSummary = upgradeTask.getMigrationSummary();
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
        final Task240306MigrateLegacyLanguageVariables upgradeTask = new Task240306MigrateLegacyLanguageVariables();
        try {
          assertTrue(upgradeTask.forceRun());
          removeLanguageVariableContentType();
          final Optional<String> optional = upgradeTask.checkContentType();
          Assert.assertTrue(optional.isPresent());
          Assert.assertTrue(upgradeTask.getMigrationSummary().isEmpty());
          upgradeTask.executeUpgrade();
          Assert.assertTrue(upgradeTask.getMigrationSummary().isPresent());
          final ImmutableMigrationSummary summary = upgradeTask.getMigrationSummary().get();
          Assert.assertEquals(5, summary.success().size());
          Assert.assertEquals(0, summary.fails().size());
        } finally {
          final Optional<ImmutableMigrationSummary> migrationSummary = upgradeTask.getMigrationSummary();
          migrationSummary.ifPresent(this::cleanup);
      }
    }

    /**
     * Given scenario: We simulate the case where the language variable content type is dropped
     * @throws DotSecurityException if a security violation occurs
     * @throws DotDataException if an error occurs
     */
    private static void removeLanguageVariableContentType() throws DotSecurityException, DotDataException {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(
                APILocator.systemUser());
        final ContentType contentType = contentTypeAPI.find(
                LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        final boolean asyncDelete = Config.getBooleanProperty(
                ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, true);
        Config.setProperty(ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, false);
        contentTypeAPI.delete(contentType);
        Config.setProperty(ContentTypeAPIImpl.DELETE_CONTENT_TYPE_ASYNC, asyncDelete);
    }

    /**
     * Cleanup the contentlets created during the test
     * @param summary the migration summary
     */
    private void cleanup(ImmutableMigrationSummary summary) {
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
        final Optional<Language> eo = Optional.ofNullable(languageAPI.getLanguage("eo", ""));
        eo.ifPresent(languageAPI::deleteLanguage);

        // Create languages English variants
        createLangVariantInNotExists("en", null, "English", null);
        createLangVariantInNotExists("en", "ca", "English", "Canada");
        // Create languages French and Spanish variants
        createLangVariantInNotExists("fr", "fr", "French", "French");
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
        final boolean exists = languages.stream().anyMatch(l -> l.getLanguageCode().equals(languageCode) && (StringUtils.isNotEmpty(l.getCountryCode()) && l.getCountryCode().equals(countryCode)));
        if (!exists) {
            new LanguageDataGen().persist(new Language(new Random().nextLong(), languageCode, countryCode, languageName, countryName));
        }
    }

    /**
     * Copy the message bundle to the expected location
     * @param messagesDir
     * @throws URISyntaxException
     * @throws IOException
     */
    private static void copyMessageBundle(final Path messagesDir)
            throws URISyntaxException, IOException {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource("lang-vars");
        if(resource == null){
            throw new RuntimeException("lang-vars directory not found");
        }
        final Path resourcesPath = Path.of(resource.toURI());
        if (!Files.exists(messagesDir)){
            Files.createDirectories(messagesDir);
        }
        copyDir(resourcesPath, messagesDir);
    }


}
