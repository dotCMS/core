package com.dotcms.ai.api;

import com.dotcms.ai.util.AIUtil;
import com.dotcms.ai.workflow.OpenAITranslationActionlet;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.translate.AbstractTranslationService;
import com.dotcms.translate.ServiceParameter;
import com.dotcms.translate.TranslationException;
import com.dotcms.translate.TranslationService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.velocity.context.Context;

public class OpenAITranslationService extends AbstractTranslationService {

    static final String AI_TRANSLATION_SYSTEM_PROMPT = "AI_TRANSLATION_SYSTEM_PROMPT";
    static final String AI_TRANSLATION_USER_PROMPT = "AI_TRANSLATION_USER_PROMPT";
    static final String AI_TRANSLATION_MODEL_KEY = "AI_TRANSLATION_MODEL";
    static final String AI_TRANSLATIONS_MAX_TOKENS = "AI_TRANSLATIONS_MAX_TOKENS";
    static final String AI_TRANSLATION_TEMPERATURE ="AI_TRANSLATION_TEMPERATURE";
    static final String AI_TRANSLATION_RESPONSE_FORMAT ="AI_TRANSLATION_RESPONSE_FORMAT";

    public static final Lazy<TranslationService> INSTANCE = Lazy.of(()-> new OpenAITranslationService());

   /**
    * Translate a string from one language to another.
    *
    * @param toTranslate
    * @param from
    * @param to
    * @return
    * @throws TranslationException
    */
    @Override
    public String translateString(String toTranslate, Language from, Language to) throws TranslationException {
        throw new DotRuntimeException("Not implemented");
    }

   /**
    * Translate a list of strings from one language to another.
    *
    * @param toTranslate
    * @param from
    * @param to
    * @return
    * @throws TranslationException
    */
   @Override
    public List<String> translateStrings(List<String> toTranslate, Language from, Language to)
            throws TranslationException {
        throw new DotRuntimeException("Not implemented");
    }

    @Override
    public List<ServiceParameter> getServiceParameters() {
        throw new DotRuntimeException("Not implemented");
    }

    @Override
    public void setServiceParameters(List<ServiceParameter> params, String hostId) {
        throw new DotRuntimeException("Not implemented");
    }

   /**
    * Translate a contentlet to a list of languages.
    *
    * @param contentlet content to translate
    * @param langs      list of language to translate the content to
    * @param oldFields  list of fields to translate
    * @param user
    * @return
    * @throws TranslationException
    */
   @Override
    public List<Contentlet> translateContent(Contentlet contentlet, List<Language> langs,
            List<com.dotmarketing.portlets.structure.model.Field> oldFields,
            User user) throws TranslationException {

        return langs.stream().map(lang -> {
                    try {
                        return translateContent(contentlet, lang, oldFields, user);
                    } catch (TranslationException e) {
                        Logger.warnAndDebug(OpenAITranslationService.class, e.getMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Translate a contentlet's fields to a language.
     * @param contentlet               content to translate
     * @param targetLanguage       language to translate te content to
     * @param oldFields list of fields to translate
     * @param user
     * @return
     * @throws TranslationException
     */
    @Override
    public Contentlet translateContent(Contentlet contentlet, Language targetLanguage, List<com.dotmarketing.portlets.structure.model.Field> oldFields, User user)
            throws TranslationException {
        Language sourceLang = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
        List<Field> fields = new LegacyFieldTransformer(oldFields).asList();

        // Build source JSON from contentlet fields
       JSONObject sourceJson = buildSourceJson(contentlet, fields);

       // Setup translation context
       TranslationContext context = setupTranslationContext(contentlet, sourceLang, targetLanguage);

       // Prepare prompts
       PromptData promptData = preparePrompts(contentlet, sourceJson, context);

       // Create AI request
       JSONObject promptJson = buildAIRequest(contentlet, promptData);

       // Execute AI call and process response
       JSONObject aiResponse = executeTranslation(promptJson);

       if (aiResponse.isEmpty()) {
          return null;
       }

       // Create translated contentlet
       return createTranslatedContentlet(contentlet, targetLanguage, fields, aiResponse, user);
    }

   /**
    * Builds the source JSON object from contentlet fields.
    */
   private JSONObject buildSourceJson(Contentlet contentlet, List<Field> fields) {
      JSONObject sourceJson = new JSONObject();
        fields.forEach(f -> {
            String value = contentlet.getStringProperty(f.variable());
            if (UtilMethods.isSet(value)) {
                if (StringUtils.isJson(value)) {
                   sourceJson.put(f.variable(), new JSONObject(value));
                } else {
                   sourceJson.put(f.variable(), value);
                }
            }
        });
        return sourceJson;
   }

   /**
    * Sets up the translation context including translation keys and language information.
    */
   private TranslationContext setupTranslationContext(Contentlet contentlet, Language sourceLang,
           Language targetLanguage) {
      Optional<String> translationKeyPrefix = Optional.ofNullable((String) contentlet.getMap().get(
                OpenAITranslationActionlet.TRANSLATION_KEY_PREFIX));

        JSONObject translationKeysJSON = new JSONObject(getTranslationKeys(translationKeyPrefix,
                contentlet.getLanguageId(), targetLanguage.getId()));

      return new TranslationContext(sourceLang, targetLanguage, translationKeysJSON);
   }

   /**
    * Prepares system and user prompts for the AI translation request.
    */
   private PromptData preparePrompts(Contentlet contentlet, JSONObject sourceJson, TranslationContext context) {
      String systemPromptTemplate = getAISystemTranslationPrompt(contentlet.getHost());
        String userPromptTemplate = getAIUserTranslationPrompt(contentlet.getHost());

        // Build system prompt
      Context systemContext = VelocityUtil.getBasicContext();
        systemContext.put("sourceLanguage",
                context.getSourceLanguage().getLanguage() + "(" + context.getSourceLanguage().getCountry() + ")");
      systemContext.put("targetLanguage",
              context.getTargetLanguage().getLanguage() + "(" + context.getTargetLanguage().getCountry() + ")");

      if (!context.getTranslationKeysJSON().isEmpty()) {
         systemContext.put("translationKeys", context.getTranslationKeysJSON().toString());
      }

      String systemPrompt = Try.of(() -> VelocityUtil.eval(systemPromptTemplate, systemContext))
                .getOrElseThrow(DotRuntimeException::new);

        // Build user prompt
      Context userContext = VelocityUtil.getBasicContext();
        userContext.put("sourceJson", sourceJson.toString());
        userContext.put("sourceLanguage",
                context.getSourceLanguage().getLanguage() + "(" + context.getSourceLanguage().getCountry() + ")");
      userContext.put("targetLanguage",
              context.getTargetLanguage().getLanguage() + "(" + context.getTargetLanguage().getCountry() + ")");

      String userPrompt = Try.of(() -> VelocityUtil.eval(userPromptTemplate, userContext))
                .getOrElseThrow(DotRuntimeException::new);

        return new PromptData(systemPrompt, userPrompt);
   }

   /**
    * Builds the AI request JSON payload.
    */
   private JSONObject buildAIRequest(Contentlet contentlet, PromptData promptData) {
      int maxTokens = getMaxTokens(contentlet.getHost());
        String model = getTranslationModel(contentlet.getHost());

        JSONObject promptJson = new JSONObject();
        promptJson.put("model", model);

        Optional<String> responseFormat = getResponseFormat(contentlet.getHost());
        if (responseFormat.isPresent()) {
            promptJson.putAll(Map.of("response_format", Map.of("type", responseFormat.get())));
        }

        if (maxTokens > 0) {
            promptJson.put("max_tokens", maxTokens);
        }

        promptJson.put("messages", List.of(
                Map.of("role", "system", "content", promptData.getSystemPrompt()),
                Map.of("role", "user", "content", promptData.getUserPrompt())
        ));

      return promptJson;
   }

   /**
    * Executes the translation request to the AI API.
    */
   private JSONObject executeTranslation(JSONObject promptJson) {
      Logger.info(this.getClass(), "promptJson: " + promptJson.toString(2) + "\n\n");

      final JSONObject openAIResponse = APILocator.getDotAIAPI()
                .getCompletionsAPI()
                .raw(promptJson, APILocator.systemUser().getUserId());

        Logger.info(this.getClass(), "openAIResponse: " + openAIResponse.toString(2) + "\n\n");

      return parseAIResponse(openAIResponse);
   }

   /**
    * Creates the translated contentlet from the AI response.
    */
   private Contentlet createTranslatedContentlet(Contentlet contentlet, Language targetLanguage,
           List<Field> fields, JSONObject aiResponse, User user) {

      Contentlet translated = Try.of(() -> APILocator.getContentletAPI()
              .checkout(contentlet.getInode(), user, false)).getOrElseThrow(DotRuntimeException::new);

        translated.setLanguageId(targetLanguage.getId());

        boolean hasChanges = false;

        for (Field field : fields) {
            String value = aiResponse.optString(field.variable());
            if (UtilMethods.isSet(value)) {
               translated.setStringProperty(field.variable(), value.replaceAll("<\\\\/", "</"));
               hasChanges = true;
            }
        }

        return hasChanges ? translated : null;
    }

    /**
     * Inner class to hold translation context data.
     */
    private static class TranslationContext {

       private final Language sourceLanguage;
       private final Language targetLanguage;
       private final JSONObject translationKeysJSON;

       public TranslationContext(Language sourceLanguage, Language targetLanguage, JSONObject translationKeysJSON) {
          this.sourceLanguage = sourceLanguage;
          this.targetLanguage = targetLanguage;
          this.translationKeysJSON = translationKeysJSON;
       }

       public Language getSourceLanguage() {
          return sourceLanguage;
       }

       public Language getTargetLanguage() {
          return targetLanguage;
       }

       public JSONObject getTranslationKeysJSON() {
          return translationKeysJSON;
       }
    }

   /**
    * Inner class to hold prompt data.
    */
   private static class PromptData {

      private final String systemPrompt;
      private final String userPrompt;

      public PromptData(String systemPrompt, String userPrompt) {
         this.systemPrompt = systemPrompt;
         this.userPrompt = userPrompt;
      }

      public String getSystemPrompt() {
         return systemPrompt;
      }

      public String getUserPrompt() {
         return userPrompt;
      }
   }

   JSONObject parseAIResponse(JSONObject response) {
      try {
         String aiJson = response
                 .getJSONArray("choices")
                 .getJSONObject(0)
                 .getJSONObject("message")
                 .getString("content");

         if (aiJson == null) {
            return new JSONObject();
         }
         aiJson = aiJson.trim();
         if (UtilMethods.isEmpty(aiJson)) {
            return new JSONObject();
         }
         // gets at the first json object
         while (!aiJson.startsWith("{")) {
            aiJson = aiJson.substring(1);
         }
         while (!aiJson.endsWith("}")) {
            aiJson = aiJson.substring(0, aiJson.length() - 1);
         }

         return new JSONObject(aiJson);
      } catch (Exception e) {
         // Handle JSON parsing errors
         Logger.error(this.getClass(), "Error parsing AI response: " + e.getMessage(), e);
         return new JSONObject(); // Return an empty JSONObject or handle the error as needed
      }
   }

   Map<String, String> getTranslationKeys(Optional<String> prefixIn, long originalLang, long langToTranslate) {

      if (prefixIn.isEmpty() || UtilMethods.isEmpty(prefixIn.get())) {
         return Map.of();
      }

        if (originalLang == langToTranslate) {
            throw new DotRuntimeException("Cannot translate contentlet to the same language: " + langToTranslate);
        }

        final StringBuilder query = new StringBuilder()
                .append("+baseType:")
                .append(BaseContentType.KEY_VALUE.getType());
        if (!"*".equals(prefixIn.get())) {
            query.append(" +key_dotraw:")
                    .append(prefixIn.get())
                    .append("*");
        }
        query.append(" +languageId:(")
                .append(originalLang)
                .append(" OR ")
                .append(langToTranslate)
                .append(") +deleted:false");

        String queryStr = query.toString();

        Map<String, String> context = new HashMap<>();
        int limit = 1000;
        int page = 0;
        int maxContextSize = Config.getIntProperty("MAX_LANGUAGE_VARIABLE_CONTEXT", 1000);
        while (context.size() < maxContextSize) {
            int myPage = page;
            List<Contentlet> contentResults = Try.of(() -> APILocator.getContentletAPI()
                    .search(queryStr, limit, myPage * limit, "identifier,languageid", APILocator.systemUser(),
                            false)).getOrElse(List.of());
            if (contentResults.isEmpty()) {
                break;
            }
            for (int i = 0; i < contentResults.size() - 1; i++) {
                Contentlet workingCon = contentResults.get(i);
                Contentlet nextCon = contentResults.get(i + 1);

                if (UtilMethods.isEmpty(() -> workingCon.getIdentifier()) || UtilMethods.isEmpty(
                        () -> nextCon.getIdentifier())) {
                    continue;
                }
                if (workingCon.getIdentifier().equals(nextCon.getIdentifier())) {
                    if (workingCon.getLanguageId() == originalLang) {
                        context.put(workingCon.getStringProperty("value"), nextCon.getStringProperty("value"));
                    } else {
                        context.put(nextCon.getStringProperty("value"), workingCon.getStringProperty("value"));
                    }
                    i++;
                }
            }

            page++;
        }

        return context;
    }

    String getAISystemTranslationPrompt(String hostId) {
        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_TRANSLATION_SYSTEM_PROMPT).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_TRANSLATION_SYSTEM_PROMPT).getString();
        }
        return AIUtil.getProperty(AI_TRANSLATION_SYSTEM_PROMPT, null);
    }

    String getAIUserTranslationPrompt(String hostId) {
        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_TRANSLATION_USER_PROMPT).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_TRANSLATION_USER_PROMPT).getString();
        }
        return AIUtil.getProperty(AI_TRANSLATION_USER_PROMPT, null);
    }

    int getMaxTokens(String hostId) {

        return Try.of(() -> Integer.parseInt(AIUtil.getSecrets(hostId).get(AI_TRANSLATIONS_MAX_TOKENS).getString()))
                .getOrElse(0);
    }

    String getTranslationModel(String hostId) {

        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_TRANSLATION_MODEL_KEY).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_TRANSLATION_MODEL_KEY).getString();
        }
        return "gpt-4o";
    }

    Optional<String> getResponseFormat(String hostId) {

        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_TRANSLATION_RESPONSE_FORMAT).getString())) {
            return Optional.ofNullable(AIUtil.getSecrets(hostId).get(AI_TRANSLATION_RESPONSE_FORMAT).getString());
        }
        return "gpt-4o".equals(getTranslationModel(hostId)) ? Optional.of("json_object") : Optional.empty();
    }

    float getTemperature(String hostId) {
        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_TRANSLATION_TEMPERATURE).getString())) {
            return Float.parseFloat(AIUtil.getSecrets(hostId).get(AI_TRANSLATION_TEMPERATURE).getString());
        }
        return 0.1f;
    }
}
