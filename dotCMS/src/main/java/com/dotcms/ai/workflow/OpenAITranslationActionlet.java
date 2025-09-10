package com.dotcms.ai.workflow;


import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.translate.TranslationException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.actionlet.TranslationActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Actionlet(onlyBatch = true)
public class OpenAITranslationActionlet extends TranslationActionlet {


    static final public String TRANSLATION_KEY_PREFIX = "translationkeyPrefix";
    static final String TRANSLATE_TO = "translateTo";
    static final String FIELD_TYPES = "fieldTypes";
    static final String TRANSLATE_FIELDS = "translateFields";
    static final String IGNORE_FIELDS = "ignoreFields";
    static final String COMMA_SPLITER = "[,\\s]+";
    static int MAX_LANGUAGE_VARIABLE_CONTEXT = 1000;

    @Override
    public String getName() {
       return "AI - Translate Content";
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();
        params.add(new WorkflowActionletParameter(TRANSLATE_TO,
                "Translation to these languages (comma separated lang or lang-country codes or `*` for all )", "*",
                false));
        params.add(new WorkflowActionletParameter(FIELD_TYPES,
                "Always Translate these Field types (optional, comma separated)", "text,wysiwyg,textarea,storyblock",
                true));
        params.add(new WorkflowActionletParameter(TRANSLATE_FIELDS,
                "Then also always translate these Fields (optional, comma separated var names)", "", false));
        params.add(new WorkflowActionletParameter(IGNORE_FIELDS,
                "Finally, ignore these fields (optional, comma separated var names)", "", false));
        params.add(new WorkflowActionletParameter(TRANSLATION_KEY_PREFIX,
                "Language variable prefix to include as glossary - this is the prefix of language variables that you want to include as glossary for the translation. Leave empty for none.  Set to `*` for all (up to "
                        + MAX_LANGUAGE_VARIABLE_CONTEXT + ").", "", false));

        return params;
    }


    @Override
    public String getHowTo() {
        return "This actionlet will attempt to translate the content of a field using OpenAI's model. "
                + "The translate to field should be a comma separated list of languages you want to translate "
                + "the content into - you can leave it blank to translate to all languages. The fields to translate are built from the (Always Field Types + Always Field Variable) - Always Ignore Fields. "
                + "dotCMS will add the language variables as a context for translations.  You can specify a prefix of the keys you wish to include to get the context variables. ";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet sourceContentlet = processor.getContentlet();
        final User user = processor.getUser();
        final Optional<String> translationKeyPrefix = Try.of(() -> params.get(TRANSLATION_KEY_PREFIX).getValue().trim())
                .toJavaOptional();
        final List<Language> languages = new ArrayList<>(languagesToTranslate(params.get(TRANSLATE_TO).getValue()));
        languages.removeIf(lang -> lang.getId() == sourceContentlet.getLanguageId());
        Set<Field> fields = getIncludedFields(sourceContentlet,
                params.get(FIELD_TYPES).getValue(),
                params.get(IGNORE_FIELDS).getValue(),
                params.get(TRANSLATE_FIELDS).getValue());

       // our Translation Service interface does not have a way to pass custom parameters, so we add this value
        // to the underlying content map
        translationKeyPrefix.ifPresent(s -> sourceContentlet.getMap().put(TRANSLATION_KEY_PREFIX, s));

        List<com.dotmarketing.portlets.structure.model.Field> oldFields = new LegacyFieldTransformer(
                new ArrayList<>(fields)).asOldFieldList();

        try {
            List<Contentlet> translatedContents = com.dotcms.ai.api.OpenAITranslationService.INSTANCE.get()
                    .translateContent(sourceContentlet, languages, oldFields, APILocator.systemUser());

            final boolean live = sourceContentlet.isLive();

            for (final Contentlet translatedContent : translatedContents) {
                sourceContentlet.setTags();
                copyBinariesAndTags(processor.getUser(), sourceContentlet, translatedContent);
                translatedContent.setProperty(Contentlet.DISABLE_WORKFLOW, true);
                translatedContent.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                Contentlet persisted = APILocator.getContentletAPI().checkin(translatedContent, user, false);
                if (live) {
                    APILocator.getContentletAPI().publish(translatedContent, user, false);
                }
                APILocator.getContentletAPI().unlock(translatedContent, user, false);
            }


        } catch (Exception e) {
            Logger.error(this.getClass(), "Error translating contentlet:" + e.getMessage(), e);
            throw new WorkflowActionFailureException("Error translating contentlet", e);
        }


    }


    List<Language> languagesToTranslate(String translateToIn) {
        String translateTo = Try.of(
                () -> "all".equalsIgnoreCase(translateToIn.trim()) || "*".equalsIgnoreCase(translateToIn.trim()) ? ""
                        : translateToIn).getOrNull();

        if (UtilMethods.isEmpty(translateTo)) {
            return APILocator.getLanguageAPI().getLanguages();
        }

        List<Language> languages = new ArrayList<>();
        String[] langCodes = translateTo.split(COMMA_SPLITER);
        for (String langCode : langCodes) {
            String[] langCountry = langCode.split("[_|-]");
            Language lang = APILocator.getLanguageAPI()
                    .getLanguage(langCountry[0], langCountry.length > 1 ? langCountry[1] : null);
            if (lang != null) {
                languages.add(lang);
            }
        }
        return languages;


    }


    Set<Field> getIncludedFields(Contentlet contentlet, String fieldTypesStr, String ignoreFieldsStr,
            String translateFieldsStr) {

        final List<String> fieldTypes = Try.of(() -> Arrays.asList(fieldTypesStr.trim().split(COMMA_SPLITER)))
                .getOrElse(List.of());

        final List<String> ignoreFields = Try.of(() -> Arrays.asList(ignoreFieldsStr.trim().split(COMMA_SPLITER)))
                .getOrElse(List.of());

        final List<String> translateFields = Try.of(() -> Arrays.asList(translateFieldsStr.trim().split(COMMA_SPLITER)))
                .getOrElse(List.of());

        Set<Field> fields = new HashSet<>();

        for (Field f : contentlet.getContentType().fields()) {
            for (String type : fieldTypes) {
                if (f.getClass().getSimpleName().toLowerCase().contains(type + "field")) {
                    fields.add(f);
                }
            }
            for (String translate : translateFields) {
                if (f.variable().equalsIgnoreCase(translate)) {
                    fields.add(f);
                }
            }
            for (String ignore : ignoreFields) {
                if (f.variable().equalsIgnoreCase(ignore)) {
                    fields.remove(f);
                }
            }
        }

        return fields;
    }

   void copyBinariesAndTags(final User user, final Contentlet sourceContentlet, final Contentlet translatedContent)
           throws DotDataException, DotSecurityException, TranslationException {

      final ContentType structure = translatedContent.getContentType();

      for (Field field : structure.fields(BinaryField.class)) {

         final java.io.File inputFile = APILocator
                 .getContentletAPI()
                 .getBinaryFile(sourceContentlet.getInode(), field.variable(), user);
         if (inputFile != null) {

            final java.io.File acopyFolder = new java.io.File(
                    ConfigUtils.getAssetTempPath()
                            + java.io.File.separator + UUIDGenerator.generateUuid());

            if (!acopyFolder.exists()) {
               acopyFolder.mkdirs();
            }

            final String shortFileName = FileUtil.getShortFileName(inputFile.getAbsolutePath());
            final java.io.File binaryFile = new java.io.File(acopyFolder, shortFileName.trim());

            try {

               FileUtil.copyFile(inputFile, binaryFile);
               translatedContent.setBinary(field.variable(), binaryFile);
            } catch (IOException e) {
               throw new TranslationException(e);
            }
         }
      }
      for (Field field : structure.fields(TagField.class)) {
         translatedContent.setStringProperty(field.variable(),
                 sourceContentlet.getStringProperty(field.variable()));
      }
   }
}
