package com.dotcms.ai.api;

import com.dotcms.ai.listener.OpenAIImageTaggingContentListener;
import com.dotcms.ai.util.AIUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.exporter.ImageFilterExporter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.velocity.context.Context;

public class OpenAIVisionAPIImpl implements AIVisionAPI {

    static final String AI_VISION_ALT_TEXT_VARIABLE = "altText";
    static final String AI_VISION_TAG_FIELD = DotAssetContentType.TAGS_FIELD_VAR;

    static final String TAGGED_BY_DOTAI = "dot:taggedbydotai";

    static final ImageFilterExporter IMAGE_FILTER_EXPORTER = new ImageFilterExporter();

    static final Cache<String, Tuple2<String, List<String>>> promptCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    final Map<String, String[]> imageResizeParameters = Map.of(
            "resize_maxw", new String[]{"500"},
            "resize_maxh", new String[]{"500"},
            "webp_q", new String[]{"85"}
    );


   /**
    * Checks if the contentlet should be processed by the AI
    *
    * @param contentlet
    * @param binaryField
    * @return
    */
    boolean shouldProcessTags(Contentlet contentlet, Field binaryField) {

        Optional<Field> tagFieldOpt = contentlet.getContentType().fields(TagField.class).stream().findFirst();

        Optional<File> fileToProcess = getFileToProcess(contentlet, binaryField);

        if (tagFieldOpt.isEmpty()) {
            return false;
        }

        boolean alreadyTagged = Try.of(() -> contentlet.getStringProperty(AI_VISION_TAG_FIELD))
                .map(tags -> tags.contains(TAGGED_BY_DOTAI))
                .getOrElse(false);

        //If the contentlet is already tagged by this AI, then we should not process it again
        if (alreadyTagged) {
            return false;
        }

        //If there is no image to process, then we should not process it
        if (fileToProcess.isEmpty() || fileToProcess.get().length() < 100 || !UtilMethods.isImage(
                fileToProcess.get().getName())) {
            return false;
        }

        return !AIUtil.getSecrets(contentlet).isEmpty();
    }

   /**
    * Checks if the contentlet should be processed by the AI
    * @param contentlet
    * @param binaryField
    * @param altTextField
    * @return
    */
    boolean shouldProcessAltText(Contentlet contentlet, Field binaryField, Field altTextField) {

        if (UtilMethods.isSet(contentlet.getStringProperty(altTextField.variable()))) {
            return false;
        }

        Optional<File> fileToProcess = getFileToProcess(contentlet, binaryField);
        //If there is no image to process, then we should not process it
        if (fileToProcess.isEmpty() || fileToProcess.get().length() < 100 || !UtilMethods.isImage(
                fileToProcess.get().getName())) {
            return false;
        }

        return !AIUtil.getSecrets(contentlet).isEmpty();
    }

   /**
    * Checks if the contentlet should be processed by the AI, if so to tag it
    * @param contentlet
    * @return
    */
    @Override
    public boolean tagImageIfNeeded(Contentlet contentlet) {

        Optional<Field> tagField = contentlet.getContentType().fields().stream()
                .filter(f -> f.fieldVariablesMap().containsKey(AIVisionAPI.AI_VISION_TAG_FIELD_VAR)).findFirst();
        if (tagField.isEmpty()) {
            return false;
        }

        String binaryFieldVariable = tagField.get().fieldVariablesMap().get(AIVisionAPI.AI_VISION_TAG_FIELD_VAR)
                .value();
        Optional<Field> binaryField = contentlet.getContentType().fields().stream()
                .filter(f -> f.variable().equalsIgnoreCase(binaryFieldVariable)).findFirst();
        if (binaryField.isEmpty()) {
            return false;
        }
        return binaryField.filter(field -> tagImageIfNeeded(contentlet, binaryField.get())).isPresent();
    }

   /**
    * Checks if the contentlet should be processed by the AI, if so to tag it
    * @param contentlet
    * @param  binaryField
    * @return
    */
    public boolean tagImageIfNeeded(Contentlet contentlet, Field binaryField) {
        if (!shouldProcessTags(contentlet, binaryField)) {
            return false;
        }

        Optional<Tuple2<String, List<String>>> altAndTags = readImageTagsAndDescription(contentlet, binaryField);

        if (altAndTags.isEmpty()) {
            return false;
        }

        saveTags(contentlet, altAndTags.get()._2);
        return true;

    }

   /**
    * Checks if the contentlet should be processed by the AI, if so to add the alt tags to it
    * @param contentlet
    * @return
    */
    @Override
    public boolean addAltTextIfNeeded(Contentlet contentlet) {

        List<Field> altTextFields = contentlet.getContentType().fields().stream()
                .filter(f -> f.fieldVariablesMap().containsKey(AIVisionAPI.AI_VISION_ALT_FIELD_VAR)).collect(
                        Collectors.toList());

        boolean valToReturn = false;
        for (Field field : altTextFields) {
            String binaryFieldVariable = field.fieldVariablesMap().get(AIVisionAPI.AI_VISION_ALT_FIELD_VAR).value();
            Optional<Field> binaryField = contentlet.getContentType().fields().stream()
                    .filter(f -> f.variable().equalsIgnoreCase(binaryFieldVariable)).findFirst();
            if (binaryField.isEmpty()) {
                continue;
            }
            if (addAltTextIfNeeded(contentlet, binaryField.get(), field)) {
                valToReturn = true;
            }
        }
        return valToReturn;
    }

   /**
    * Checks if the contentlet should be processed by the AI, if so to add the alt tags to it
    * @param contentlet
    * @param binaryField
    * @param altTextField
    * @return
    */
    public boolean addAltTextIfNeeded(Contentlet contentlet, Field binaryField, Field altTextField) {

       if (!shouldProcessAltText(contentlet, binaryField, altTextField)) {
          return false;
       }
        Optional<Tuple2<String, List<String>>> altAndTags = readImageTagsAndDescription(contentlet, binaryField);

        if (altAndTags.isEmpty()) {
            return false;
        }

        Optional<Contentlet> contentToSave = setAltText(contentlet, altTextField, altAndTags.get()._1);
        return contentToSave.isPresent();
    }



   /**
    * This method takes a file and returns a Tuple2 with the first element being the alt
    * description and the second element being the list of tags
    * @param imageFile
    * @return
    */
    @Override
    public Optional<Tuple2<String, List<String>>> readImageTagsAndDescription(File imageFile) {

        String parsedPrompt = Try.of(() -> {
            final Context ctx = VelocityContextFactory.getMockContext();
            ctx.put("visionModel", getAiVisionModel(Host.SYSTEM_HOST));
            ctx.put("maxTokens", getAiVisionMaxTokens(Host.SYSTEM_HOST));
            ctx.put("base64Image", base64EncodeImage(imageFile));
            return VelocityUtil.eval(getAiVisionPrompt(Host.SYSTEM_HOST), ctx);
        }).getOrNull();
        if (parsedPrompt == null) {
            return Optional.empty();
        }

        return readImageTagsAndDescription(parsedPrompt);
    }


   /**
    * This method takes a prompt and returns a Tuple2 with the first element being the alt
    * description and the second element being the list of tags
    * @param parsedPrompt
    * @return
    */
    private Optional<Tuple2<String, List<String>>> readImageTagsAndDescription(String parsedPrompt) {

       String promptHash = StringUtils.hashText(parsedPrompt);
        if (UtilMethods.isEmpty(promptHash) || UtilMethods.isEmpty(parsedPrompt)) {
            return Optional.empty();
        }
        return Optional.ofNullable(promptCache.get(promptHash, k -> {
            try {
                JSONObject parsedPromptJson = new JSONObject(parsedPrompt);
                Logger.debug(this.getClass(), "parsedPromptJson: " + parsedPromptJson.toString());

                final JSONObject openAIResponse = APILocator.getDotAIAPI()
                        .getCompletionsAPI()
                        .raw(parsedPromptJson, APILocator.systemUser().getUserId());

                Logger.debug(OpenAIImageTaggingContentListener.class.getName(),
                        "OpenAI Response: " + openAIResponse.toString());

                final JSONObject parsedResponse = parseAIResponse(openAIResponse);
                Logger.debug(OpenAIImageTaggingContentListener.class.getName(),
                        "parsedResponse: " + parsedResponse.toString());

                return Tuple.of(parsedResponse.getString(AI_VISION_ALT_TEXT_VARIABLE),
                        parsedResponse.getJSONArray(AI_VISION_TAG_FIELD));
            } catch (Exception e) {
                Logger.warnAndDebug(OpenAIImageTaggingContentListener.class.getCanonicalName(), e.getMessage(), e);
                return null;
            }

        }));


    }

   /**
    * This method takes a contentlet and a binary field and returns a Tuple2 with the first element being the alt description
    * and the second element being the list of tags
    * @param contentlet
    * @param imageOrBinaryField
    * @return
    */
    @Override
    public Optional<Tuple2<String, List<String>>> readImageTagsAndDescription(Contentlet contentlet,
            Field imageOrBinaryField) {




        Optional<File> fileToProcess = getFileToProcess(contentlet, imageOrBinaryField);
        if (fileToProcess.isEmpty()) {
            return Optional.empty();
        }

        final Context ctx = VelocityContextFactory.getMockContext(contentlet, APILocator.systemUser());
        ctx.put("visionModel", getAiVisionModel(contentlet.getHost()));
        ctx.put("maxTokens", getAiVisionMaxTokens(contentlet.getHost()));
        ctx.put("base64Image", base64EncodeImage(fileToProcess.get()));

        final String prompt = Try.of(() -> VelocityUtil.eval(getAiVisionPrompt(contentlet.getHost()), ctx))
                .onFailure(e -> Logger.warnAndDebug(OpenAIVisionAPIImpl.class, e)).getOrNull();
        if (prompt == null) {
            return Optional.empty();
        }

        return readImageTagsAndDescription(prompt);

    }


   /**
    * Retrieves a file to process based on the provided contentlet and field.
    * The method attempts to resolve the file using binary or asset field information.
    *
    * @param contentlet the content object from which the file is to be retrieved
    * @param field the field that specifies the binary or asset data to process
    * @return an {@link Optional} containing the resolved file, or an empty {@link Optional} if no file could be determined
    */
    Optional<File> getFileToProcess(Contentlet contentlet, Field field) {

        return Try.of(() ->{
            if(field instanceof BinaryField) {
                return contentlet.getBinary(field.variable());
            }

            String id = contentlet.getStringProperty(field.variable());
            Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id, contentlet.getLanguageId());
                   if (cvi.isEmpty() && contentlet.getLanguageId() != APILocator.getLanguageAPI().getDefaultLanguage()
                           .getId()) {
                cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id, APILocator.getLanguageAPI().getDefaultLanguage().getId());
            }
            if(cvi.isEmpty()) {
                return null;
            }
            Contentlet fileOrDotAsset= APILocator.getContentletAPI().find(cvi.get().getWorkingInode(), APILocator.systemUser(), true);
            return fileOrDotAsset.isFileAsset() ? fileOrDotAsset.getBinary(FileAssetAPI.BINARY_FIELD) : fileOrDotAsset.getBinary(DotAssetContentType.ASSET_FIELD_VAR);

        }




        ).toJavaOptional();
    }


   /**
    * Parses the AI response JSON object to extract and convert the AI-generated content
    * into a new JSON object representing the processed data.
    *
    * @param response the JSON object containing the AI response, which is expected
    *                 to include a "choices" field with nested "message" and "content" data
    * @return a JSON object created from the extracted and processed content of the AI response
    */
    JSONObject parseAIResponse(JSONObject response) {

        String aiJson = response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        // gets at the first json object
        while (!aiJson.isEmpty() && !aiJson.startsWith("{")) {
            aiJson = aiJson.substring(1);
        }
        while (!aiJson.isEmpty() && !aiJson.endsWith("}")) {
            aiJson = aiJson.substring(0, aiJson.length() - 1);
        }

        return new JSONObject(aiJson);

    }


    String base64EncodeImage(File imageFile) {
        File transformedFile = Try.of(
                () -> IMAGE_FILTER_EXPORTER.exportContent(imageFile, new HashMap<>(this.imageResizeParameters))
                        .getDataFile()).getOrElseThrow(DotRuntimeException::new);

        Logger.debug(OpenAIImageTaggingContentListener.class.getCanonicalName(),
                "Transformed file: " + transformedFile.getAbsolutePath());
        try {
            return java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(transformedFile.toPath()));
        } catch (Exception e) {
            Logger.error(this, "Error encoding image", e);
            throw new DotRuntimeException(e);
        }
    }


    String getAiVisionModel(String hostId) {

        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_VISION_MODEL).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_VISION_MODEL).getString();
        }
        return "gpt-4o";
    }

   /**
    * Retrieves the AI vision max tokens for the given host ID. If a max tokens value is configured for the specified host,
    * @param hostId
    * @return
    */
    String getAiVisionMaxTokens(String hostId) {
        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_VISION_MAX_TOKENS).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_VISION_MAX_TOKENS).getString();
        }
        return "500";
    }

   /**
    * Retrieves the AI vision prompt for the given host ID. If a prompt is configured for the specified host,
    * it is returned; otherwise, a default prompt is loaded from a JSON file in the classpath.
    *
    * @param hostId the identifier of the host for which the AI vision*/
    String getAiVisionPrompt(String hostId) {
        if (UtilMethods.isSet(() -> AIUtil.getSecrets(hostId).get(AI_VISION_PROMPT).getString())) {
            return AIUtil.getSecrets(hostId).get(AI_VISION_PROMPT).getString();
        }


        return Try.of(()->{
            try (InputStream in = OpenAIVisionAPIImpl.class.getResourceAsStream("/com/dotcms/ai/prompts/default-vision-prompt.json")) {
                return new String(in.readAllBytes());
            }
        }).getOrElseThrow(e->new DotRuntimeException("Unable to find default prompt template in classpath:/com/dotcms/ai/prompts/default-vision-prompt.json " ));

    }


   /**
    * Saves the tags to the contentlet
    *
    * @param contentlet
    * @param tags
    */
   private void saveTags(Contentlet contentlet, List<String> tags) {
      Optional<Field> tagFieldOpt = contentlet.getContentType().fields(TagField.class).stream().findFirst();
      if (tagFieldOpt.isEmpty()) {
         return;
      }
      Try.run(() -> APILocator.getTagAPI()
              .addContentletTagInode(TAGGED_BY_DOTAI, contentlet.getInode(), contentlet.getHost(),
                      tagFieldOpt.get().variable())).getOrElseThrow(
              DotRuntimeException::new);

      for (final String tag : tags) {
         Try.run(() -> APILocator.getTagAPI().addContentletTagInode(tag, contentlet.getInode(), contentlet.getHost(),
                 tagFieldOpt.get().variable())).getOrElseThrow(
                 DotRuntimeException::new);
      }
   }

   /**
    * Sets the alt text to the contentlet
    *
    * @param contentlet
    * @param altTextField
    * @param altText
    * @return
    */
   private Optional<Contentlet> setAltText(Contentlet contentlet, Field altTextField, String altText) {
      if (UtilMethods.isEmpty(altText)) {
         return Optional.empty();
      }

      if (UtilMethods.isSet(() -> contentlet.getStringProperty(altTextField.variable()))) {
         return Optional.empty();
      }

      contentlet.setStringProperty(altTextField.variable(), altText);
      return Optional.of(contentlet);


   }


}
