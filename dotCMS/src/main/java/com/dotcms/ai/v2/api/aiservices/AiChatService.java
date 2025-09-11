package com.dotcms.ai.v2.api.aiservices;

import dev.langchain4j.service.UserMessage;

public interface AiChatService {

    String chat(@UserMessage String userMessage);
}
