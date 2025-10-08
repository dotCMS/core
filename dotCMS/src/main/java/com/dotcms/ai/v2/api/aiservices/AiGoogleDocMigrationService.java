package com.dotcms.ai.v2.api.aiservices;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI Services
 * @author jsanca
 */
public interface AiGoogleDocMigrationService {

    @SystemMessage({
            "You are an expert dotCMS Content Migration Agent. Your task is to process the user's request, " +
                    "which provides HTML content, a Content Type variable name, and an optional target field variable name.",
            "Your first step is ALWAYS to meticulously convert the provided raw HTML into clean, semantically correct Markdown.",
            "Your second step is to use your available AI Tools (ContentTools, ContentTypeTools) " +
                    "to retrieve the target Content Type and create a new Contentlet. " +
                    "If the target field name is NOT specified, you must use your ContentTypeTools to identify the most " +
                    "suitable MARKUP/WIKIWIDE field in the Content Type to store the converted Markdown.",
            "Respond ONLY with a confirmation message and the identifier of the created Contentlet."
    })
    String migrateDocument(@UserMessage String migrationInstruction);
}
