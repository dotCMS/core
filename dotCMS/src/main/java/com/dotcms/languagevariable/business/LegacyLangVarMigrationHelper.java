package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.DASH;
import static com.liferay.util.StringPool.EQUAL;
import static com.liferay.util.StringPool.UNDERLINE;

/**
 * Helper class to migrate legacy language variables to the new {@code Language Variable} Content
 * Type. This is the migration criteria:
 * <ul>
 *     <li>When reading old language vars, we need to read only the language files (e.g. just "en"),
 *     not the locale files (e.g. "en-US").</li>
 *     <li>When converting the language variables in that language file:
 *         <ul>
 *             <li>If there is already a generic language (not a locale) which matches the language
 *             of the file, create the language variables in that generic language:
 *             <ul>
 *                 <li>Do not create any local-specific language variables.</li>
 *             </ul>
 *             </li>
 *             <li>Otherwise, create one variable per locale which shares that language:
 *             <ul>
 *                 <li>So, for example, if the system has both "en-US" and "en-GB", but does NOT
 *                 have a generic "en", then when we create the content language var from the "en"
 *                 file, we need to create 2 different content language vars - one for "en-US" and
 *                 one for "en-GB".</li>
 *             </ul>
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Fabrizzio Araya, Jose Castro
 * @since Mar 15th, 2024
 */
public class LegacyLangVarMigrationHelper {

    private final WorkflowAPI workflowAPI;
    private final LanguageVariableAPI languageVariableAPI;

    final String languageVariableCtID;

    /**
     * Creates a new instance of this class.
     *
     * @param langVarContentTypeID The ID of the {@code Language Variable} Content Type.
     */
    public LegacyLangVarMigrationHelper(final String langVarContentTypeID) {
        this.languageVariableCtID = langVarContentTypeID;
        this.workflowAPI = APILocator.getWorkflowAPI();
        this.languageVariableAPI = APILocator.getLanguageVariableAPI();
    }

    /**
     * Returns the {@code /messages/} directory, which is the location where all the properties file
     * containing the legacy Language Variable live.
     *
     * @return The absolute path to the {@code /messages/} directory.
     */
    public static Path messagesDir() {
        return Paths.get(ConfigUtils.getAssetPath(),"messages");
    }

    /**
     * Migrates the legacy language variables to the new language variable content type
     * @param messagesDir the messages directory
     * @return the migration summary
     * @throws IOException if an error occurs reading the files
     */
    public ImmutableMigrationSummary migrateLegacyLanguageVariables(final Path messagesDir) throws IOException {
        final ImmutableMigrationSummary.Builder summary = ImmutableMigrationSummary.builder();
        if (!Files.exists(messagesDir)) {
            Logger.warn(this,"WARNING: No messages directory found. Skipping Language Variable Migration.");
            return summary.build();
        }

        final Map<Language,List<String>> successSummary = new HashMap<>();
        final Map<Language,List<String>> failsSummary = new HashMap<>();
        final Map<String, List<Language>> languagesByLocale = this.mapLanguageVariants();
        Logger.info(this,"===================================================");
        Logger.info(this,"    Legacy Language Variable Migration Process");
        Logger.info(this,"===================================================");
        try (final Stream<Path> files = Files.list(messagesDir).filter(path -> path.toString().endsWith(".properties")).sorted(
                Comparator.comparing(o -> o.getFileName().toString()))) {
            files.forEach(path -> {
                final String fileName = path.getFileName().toString();
                Logger.info(this,String.format("Processing file: %s", fileName));
                final String cmsLanguage = fileName.replace("cms_language_", BLANK).replace(".properties", BLANK).replace(UNDERLINE, DASH);
                Logger.info(this,String.format("Extracted language code: %s", cmsLanguage));
                final Optional<Locale> locale = this.localeFromTag(cmsLanguage);
                if (locale.isEmpty()) {
                    Logger.error(this,String.format("Locale for '%s' was not found. Skipping file: %s", cmsLanguage, path));
                    summary.addInvalidFiles(path);
                    return;
                }
                Logger.debug(this,String.format("Locale: %s", locale));

                final List<Language> matchingLanguage = this.matchWithExistingLanguage(locale.get(), languagesByLocale);
                if (UtilMethods.isSet(matchingLanguage)) {
                    matchingLanguage.forEach(language -> {
                        try {
                            Logger.info(this, String.format("-> Matching language '%s' found for locale '%s'", language, locale.get()));
                            final List<String> success = new ArrayList<>();
                            final List<String> fails = new ArrayList<>();
                            this.migrateFileContents(path, language, success, fails);
                            //build the summary
                            //We always update the summary with the success (regardless of they are empty or not)
                            //Because sometimes file are zero length, and we still want to know that we processed them
                            successSummary.computeIfAbsent(language,
                                    k -> new ArrayList<>()).addAll(success);

                            //We only add the fails if they are not empty
                            if(!fails.isEmpty()) {
                                failsSummary.computeIfAbsent(language,
                                        k -> new ArrayList<>()).addAll(fails);
                            }
                        } catch (final IOException e) {
                            Logger.error(this,String.format("Error reading file '%s': %s", path, ExceptionUtil.getErrorMessage(e)), e);
                        }
                    });
                } else {
                     Logger.warn(this,String.format("No matching language found for '%s'. Skipping file: %s", cmsLanguage, path));
                     summary.addNonExistingLanguages(locale.get());
                }
            });
        }
        summary.success(successSummary);
        summary.fails(failsSummary);
        return summary.build();
    }

    /**
     *
     * @param cmsLanguage the language tag
     * @return an Optional with the Locale if the tag is valid, empty otherwise
     */
    Optional <Locale> localeFromTag(final String cmsLanguage) {
      return Optional.ofNullable(Try.of(()->LanguageUtil.validateLanguageTag(cmsLanguage)).getOrNull());
    }

    /**
     * Reads the file contents and creates the Language Variables as Contentlets.
     *
     * @param path     The path to the .properties file containing the legacy Language Variable
     *                 entries.
     * @param language The language that will be used to create the new Language Variables.
     *
     * @throws IOException An error occurred when reading the .properties file.
     */
    private void migrateFileContents(final Path path, final Language language, final List<String> success, final List<String> fails) throws IOException {
        Logger.info(this,String.format("-> Migrating variables for language: %s_%s", language.getLanguageCode(), language.getCountryCode()));
        final List<String> lines = Try.of(() -> Files.readAllLines(path, StandardCharsets.ISO_8859_1))
                .getOrElseThrow(e -> new IOException(String.format("Could not read file: %s", path), e));
        if (lines.isEmpty()) {
            Logger.warn(this,String.format("File '%s' is empty. Moving on...", path));
            return;
        }
        lines.forEach(line -> {
            final String[] parts = line.split(EQUAL,2);
            if (parts.length == 2) {
                final String key = parts[0];
                final String value = parts[1];
                final String languageVariable = this.languageVariableAPI.getLanguageVariable(key,
                        language.getId(), APILocator.systemUser());
                if (null == languageVariable || languageVariable.equals(key)) {
                    final Contentlet langVarAsContent = this.createLanguageVariableAsContent(key, value, language.getId());
                    try {
                        final Contentlet checkedInContent = this.publish(langVarAsContent);
                        Logger.debug(this, String.format("Saved Language Variable with Inode '%s' for language " +
                                "'%s_%s'", checkedInContent.getInode(), language.getLanguageCode(), language.getCountryCode()));
                        success.add(checkedInContent.getInode());
                    } catch (final Exception e) {
                        Logger.error(this, String.format("Error saving language variable with key: " +
                                        "'%s', value: '%s', lang: '%s': %s", key, value, language.getId(),
                                ExceptionUtil.getErrorMessage(e)), e);
                        fails.add(key);
                    }
                } else {
                    Logger.warn(this, String.format("Language Variable '%s' in language '%s_%s' " +
                            " is already defined. The value from the .properties file will be ignored", key,
                            language.getLanguageCode(), language.getCountryCode()));
                }
            } else {
                Logger.warn(this,String.format("Invalid line found in file '%s': [ %s ]", path, line));
            }
        });
        Logger.info(this, String.format("-> %d of %d Language Variables for language %s_%s have been migrated", success.size(),
                lines.size(), language.getLanguageCode(), language.getCountryCode()));
    }

    /**
     * Maps the languages by language code
     * @return a map with the languages grouped by language code
     */
    private  Map<String, List<Language>> mapLanguageVariants() {
        final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        return mapLanguagesByCode(languages);
    }

    /**
     * Maps the languages by language code
     * @param languages the exising languages
     * @return a map with the languages grouped by language code
     */
    Map<String, List<Language>> mapLanguagesByCode(final List<Language> languages) {
        return languages.stream().collect(
                Collectors.groupingBy(lang -> lang.getLanguageCode().toLowerCase(), Collectors.toList()));
    }

    /**
     * Matches the given locale with the existing languages. If the Locale represent only a base
     * language -- i.e., only the language code without the country code -- then all languages
     * matching the specified language code will be returned. However, if there's an existing
     * language in dotCMS having only its language code, such a Language will be used instead.
     * <p>For instance, all these languages share the same Root Language but not necessarily the
     * same country: en_US, en_GB, en_AU, en_NZ, en_CA.</p>
     *
     * @param locale            The locale used to match one or more languages.
     * @param languagesByLocale The  available languages in dotCMS grouped by language code
     *
     * @return The list of {@link Language} objects matching the specified {@link Locale}.
     */
    private List<Language> matchWithExistingLanguage(final Locale locale, final Map<String, List<Language>> languagesByLocale) {
        final String langCode = locale.getLanguage().toLowerCase();
        final List<Language> languages = languagesByLocale.get(langCode);
        if (UtilMethods.isNotSet(languages)) {
            return List.of();
        }
        final String countryCode = locale.getCountry().toLowerCase();
        if (Strings.isEmpty(countryCode)) {
            // If we don't have a country code, we try to find an occurrence of the root
            // language; that is, a language with NO country code
            final Optional<Language> rootLanguage =
                    languages.stream().filter(lang -> Strings.isEmpty(lang.getCountryCode())).findFirst();
            if (rootLanguage.isPresent()) {
                return List.of(rootLanguage.get());
            } else {
                // If we don't have a Root Language, we return all the languages matching its
                // language code
                Logger.warn(this, String.format("There's no root Language definition for '%s'." +
                        " Returning all mapped languages for that base language", langCode));
                return languages;
            }
        }
        // If we find an exact match, we just return it
        final Optional<Language> exactLanguageOpt = languages.stream()
                .filter(lang -> Strings.isNotEmpty(lang.getCountryCode()) && lang.getCountryCode()
                        .equalsIgnoreCase(countryCode)).findFirst();
        return exactLanguageOpt.map(List::of).orElseGet(List::of);
    }

    /**
     * Creates a Contentlet object representing the Language Variable as Content using the data from
     * the legacy Language Variable.
     *
     * @param key        The variable's key.
     * @param value      The variable's value.
     * @param languageId The ID of the language that the new Language Variable content will be
     *                   created in.
     *
     * @return The {@link Contentlet} representing the new Language Variable.
     */
    private Contentlet createLanguageVariableAsContent(final String key, final String value, final long languageId) {
        final Contentlet langVar = new Contentlet();
        langVar.setContentTypeId(languageVariableCtID);
        langVar.setLanguageId(languageId);
        langVar.setIndexPolicy(IndexPolicy.FORCE);
        langVar.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);
        return langVar;
    }

    /**
     * Publishes the migrated Language Variable as a Contentlet. It's work noting that the Workflow
     * API must be used in order to have the new Contentlets in the right Workflow Step.
     *
     * @param contentlet The {@link Contentlet} representing the new Language Variable.
     *
     * @return The published Language Variable as a{@link Contentlet}.
     *
     * @throws DotSecurityException The user calling the API doesn't have the required permissions
     *                              to perform this action.
     * @throws DotDataException     An error occurred when interacting with the database.
     */
    private Contentlet publish(final Contentlet contentlet) throws DotSecurityException,
            DotDataException {
        final WorkflowAction unpublishAction = this.workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID, APILocator.systemUser());
        return this.workflowAPI.fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .indexPolicy(IndexPolicy.WAIT_FOR)
                        .workflowActionId(unpublishAction)
                        .modUser(APILocator.systemUser())
                        .build());
    }

}
