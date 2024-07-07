package com.dotcms.languagevariable.business;

import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import io.vavr.control.Try;
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
import org.apache.logging.log4j.util.Strings;

/**
 * Helper class to migrate legacy language variables to the new language variable content type
 */
public class LegacyLangVarMigrationHelper {

    static final Language plainEnglish = new Language(1, "en", null, "English", null);

    final ContentletAPI contentletAPI;

    final String langVarContentTypeInode;

    /**
     * Constructor
     * @param langVarContentTypeInode the language variable content type inode
     */
    public LegacyLangVarMigrationHelper(final String langVarContentTypeInode) {
        this.langVarContentTypeInode = langVarContentTypeInode;
        this.contentletAPI = APILocator.getContentletAPI();
    }

    /**
     * Returns the messages directory
     * @return the messages directory
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
            Logger.info(this," No messages directory found. Skipping language variable migration.");
            return summary.build();
        }

        Map<Language,List<String>> successSummary = new HashMap<>();
        Map<Language,List<String>> failsSummary = new HashMap<>();
        final Map<String, List<Language>> langVariants = mapLanguageVariants();
        try (Stream<Path> files = Files.list(messagesDir).filter(path -> path.toString().endsWith(".properties")).sorted(
                Comparator.comparing(o -> o.getFileName().toString()))){
            files.forEach(path -> {
                final String fileName = path.getFileName().toString();
                Logger.debug(this,"Processing file " + fileName);
                final String cmsLanguage = fileName.replace("cms_language_", "").replace(".properties", "").replace("_", "-");
                Logger.info(this," Extracted language code: " + cmsLanguage);
                final Optional<Locale> locale = localeFromTag(cmsLanguage);
                if(locale.isEmpty()){
                    Logger.error(this,"Invalid locale found for " + cmsLanguage + ". Skipping file " + path);
                    summary.addInvalidFiles(path);
                    return;
                }
                Logger.debug(this," Locale: " + locale);

                final Optional<Language> matchingLanguage = matchWithExistingLanguage(locale.get(), langVariants);
                if (matchingLanguage.isPresent()) {
                    try {
                        Logger.info(this, String.format(" Matching language: %s found for locale %s", matchingLanguage.get(), locale));
                        final List<String> success = new ArrayList<>();
                        final List<String> fails = new ArrayList<>();
                        loadFileContents(path, matchingLanguage.get(), success, fails);
                        //build the summary
                        //We always update the summary with the success (regardless of they are empty or not)
                        //Because sometimes file are zero length, and we still want to know that we processed them
                        successSummary.computeIfAbsent(matchingLanguage.get(),
                                    k -> new ArrayList<>()).addAll(success);

                        //We only add the fails if they are not empty
                        if(!fails.isEmpty()) {
                            failsSummary.computeIfAbsent(matchingLanguage.get(),
                                    k -> new ArrayList<>()).addAll(fails);
                        }
                    } catch (IOException e) {
                        Logger.error(this,"Error reading file " + path, e);
                    }
                } else {
                     Logger.warn(this,"No matching language found for " + cmsLanguage + ". Skipping file " + path);
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
     * Load the file contents and save the language variables
     * @param path the file path
     * @param language the language
     * @throws IOException if an error occurs reading the file
     */
    private void loadFileContents(final Path path, final Language language, final List<String> success,  final List<String> fails) throws IOException {

        Logger.info(this," Found language: " + language.getLanguageCode() + " " + language.getCountryCode());
        final List<String> lines = Try.of(() -> Files.readAllLines(path, StandardCharsets.ISO_8859_1))
                .getOrElseThrow(e -> new IOException("Error reading file " + path, e));
        if (lines.isEmpty()) {
            Logger.warn(this,"Empty file " + path);
            return;
        }
        lines.forEach(line -> {
            final String[] parts = line.split("=",2);
            if (parts.length == 2) {
                final String key = parts[0];
                final String value = parts[1];
                final Contentlet langVar = saveLanguageVariableContent(key, value, language.getId());
                try {
                    final Contentlet checkin = contentletAPI.checkin(langVar, APILocator.systemUser(), false);
                    Logger.debug(this,"Saved language variable " + checkin.getIdentifier() + " for language " + language.getLanguageCode() + " " + language.getCountryCode());
                    success.add(checkin.getInode());
                } catch (Exception  e) {
                    Logger.warn(this,String.format("Error saving language variable  key= %s, val= %s, lang= %s ",key, value,language.getId()), e);
                    fails.add(key);
                }
            } else {
                Logger.warn(this,"Invalid line " + line);
            }
        });
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
        //We're adding the root language english (no country)
        //to force a match with a file ending in _en.properties
        //So we're simply adding it here to force a match with that file
        final List<Language> mutable = new ArrayList<>(languages);
        mutable.add(plainEnglish);
        return mutable.stream().collect(
                Collectors.groupingBy(lang -> lang.getLanguageCode().toLowerCase(), Collectors.toList()));
    }

    /**
     * Matches the given locale with the existing languages
     * @param locale the locale
     * @param languagesByLocale the languages grouped by language code
     * @return an Optional with the matching language if found, empty otherwise
     */
    Optional<Language> matchWithExistingLanguage(final Locale locale, final Map<String, List<Language>> languagesByLocale){
        final String langCode = locale.getLanguage().toLowerCase();
        final List<Language> languages = languagesByLocale.get(langCode);
        if (languages == null || languages.isEmpty()) {
            return Optional.empty();
        }
        //All these languages share the same Root Language but not necessarily the same country
        //e.k. en_US, en_GB, en_AU, en_NZ, en_CA all share the same root language en
        //So now we need to find the best match for the given locale
        final String countryCode = locale.getCountry().toLowerCase();
        if(Strings.isEmpty(countryCode)){
            //if we don't have a country code we try to find an occurrence of the root language
            return languages.stream().filter(lang -> Strings.isEmpty(lang.getCountryCode())).findFirst();
        }

        //if we find an exact match we return it
        return languages.stream()
                .filter(lang -> Strings.isNotEmpty(lang.getCountryCode()) && lang.getCountryCode()
                        .equalsIgnoreCase(countryCode)).findFirst();
    }

    /**
     * Saves a language variable content
     * @param key the key
     * @param value the value
     * @param languageId the language id
     * @return the saved contentlet
     */
    public Contentlet saveLanguageVariableContent(final String key, final String value, final long languageId) {
        final Contentlet langVar = new Contentlet();
        langVar.setContentTypeId(langVarContentTypeInode);
        langVar.setLanguageId(languageId);
        langVar.setIndexPolicy(IndexPolicy.FORCE);
        langVar.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
        langVar.setStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);
        return langVar;
    }

}
