package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

public interface ConversationAPI {

    ChatResponse chat (ChatRequest request);

    String migration(final String typeVar,
                     final String fieldVar,
                     final String docHtmlContent,
                     final String providerKey,
                     final ModelConfig modelConfig);
}
