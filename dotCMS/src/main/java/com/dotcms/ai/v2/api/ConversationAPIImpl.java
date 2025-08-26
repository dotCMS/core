package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.aiservices.AiChatService;
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

    @Inject
    public ConversationAPIImpl(final ChatMemoryFactory chatMemoryFactory,
                               final ModelProviderFactory modelProviderFactory) {
        this.chatMemoryFactory = chatMemoryFactory;
        this.modelProviderFactory = modelProviderFactory;
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
                .build();

        return new ChatResponse(conversationId, aiChatService.chat(prompt));
    }
}
