package com.dotcms.ai.api;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import io.vavr.Tuple2;
import java.io.File;
import java.util.List;
import java.util.Optional;

public interface AIVisionAPI {

   static final String AI_VISION_AUTOTAG_CONTENTTYPES_KEY = "AI_VISION_AUTOTAG_CONTENTTYPES";

   static final String AI_VISION_MODEL = "AI_VISION_MODEL";

   static final String AI_VISION_MAX_TOKENS = "AI_VISION_MAX_TOKENS";

   static final String AI_VISION_PROMPT = "AI_VISION_PROMPT";

   static final String AI_VISION_ALT_FIELD_VAR = "dotAIDescriptionSrc";

   static final String AI_VISION_TAG_FIELD_VAR = "dotAITagSrc";

   static final String AI_VISITON_TAG_AND_ALT_PROMPT_TEMPLATE = "AI_VISITON_TAG_AND_ALT_PROMPT_TEMPLATE";



   /**
    * This method will tag the image if it is missing.  It will use the dotAITagSrc field variable to find the image
    * field to use when tagging.  If the image field is not found, it will not tag the image.
    *
    * Returns true if the image was tagged, false otherwise.
    *
    * @param contentlet
    * @return
    */
   boolean tagImageIfNeeded(Contentlet contentlet);


   /**
    * this method will add the alt text to the contentlet if it is missing.  It will use the dotAIDescriptionSrc field
    * variable to find the image field to use when generating the alt text.
    *
    * Returns true if the alt text was added, false otherwise.
    *
    * @param contentlet
    * @return
    */
   boolean addAltTextIfNeeded(Contentlet contentlet);

   /**
    * This method takes a file and returns a Tuple2 with the first element being the description and the second element
    * being the tags
    *
    * @param imageFile
    * @return
    */
   Optional<Tuple2<String, List<String>>> readImageTagsAndDescription(File imageFile);

   /**
    * This method takes a contentlet and a binary field and returns a Tuple2 with the first element being the
    * description and the second element being the tags.  The contentlet can
    *
    * @param contentlet
    * @param binaryField
    * @return
    */
   Optional<Tuple2<String, List<String>>> readImageTagsAndDescription(Contentlet contentlet,
           Field binaryField);
}
