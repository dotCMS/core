package com.dotcms.ai.v2.api.aiservices;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Defines Ai services for completions + RAG
 * @author jsanca
 */
public interface RAGCompletionsService {

    @SystemMessage("${systemMessage}")
    @UserMessage("Context:\n" +
            "        ${retrievedContext}\n" +
            "        ---\n" +
            "        Question: ${userQuestion}"
        )
    AiMessage complete(@V("systemMessage") String systemMessage,
                       @V("retrievedContext") String retrievedContext,
                       @V("userQuestion") String userQuestion);
}
