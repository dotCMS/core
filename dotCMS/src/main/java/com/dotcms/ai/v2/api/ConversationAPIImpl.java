package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.aiservices.AiChatService;
import com.dotcms.ai.v2.api.aitools.AiContentTools;
import com.dotcms.ai.v2.api.aitools.AiContentTypeTools;
import com.dotcms.ai.v2.api.provider.ModelProviderFactory;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ConversationAPIImpl implements ConversationAPI {

    private final ChatMemoryFactory chatMemoryFactory;
    private final ModelProviderFactory modelProviderFactory;
    private final AiContentTools contentTools;
    private final AiContentTypeTools contentTypeTools;

    @Inject
    public ConversationAPIImpl(final ChatMemoryFactory chatMemoryFactory,
                               final ModelProviderFactory modelProviderFactory,
                               final AiContentTools contentTool,
                               final AiContentTypeTools contentTypeTools) {
        this.chatMemoryFactory = chatMemoryFactory;
        this.modelProviderFactory = modelProviderFactory;
        this.contentTools = contentTool;
        this.contentTypeTools = contentTypeTools;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {

        final String providerKey = chatRequest.getModelProviderKey();
        final String conversationId = chatRequest.getConversationId();
        final String prompt = chatRequest.getPrompt();
        final ChatMemoryBean chatMemoryBean = new ChatMemoryBean(); // todo by now it is just static
        final ChatMemory chatMemory = this.chatMemoryFactory.create(chatMemoryBean);
        final ChatModel chatModel = this.modelProviderFactory.get(providerKey, chatRequest.getModelConfig());

        final AiChatService aiChatService = AiServices.builder(AiChatService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(this.contentTools, this.contentTypeTools)
                // .contentRetriever() // todo: we already have a content retriever but not sure if we need a one here, may be for freshdesk
                .build();

        return new ChatResponse(conversationId, aiChatService.chat(prompt));
    }
}
