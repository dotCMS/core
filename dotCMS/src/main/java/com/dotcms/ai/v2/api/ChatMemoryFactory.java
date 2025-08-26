package com.dotcms.ai.v2.api;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatMemoryFactory {

    private static final String CHAT_MEMORY_PREFIX = "cm:v1";

    private Map<String, ChatMemory> chatMemoryMap = new ConcurrentHashMap<>();

    public ChatMemory create(final ChatMemoryBean chatMemoryBean) {

        this.isValidChatMemoryBean(chatMemoryBean);
        final String memoryId = createMemoryId(chatMemoryBean);
        final int maxWindowMessages = this.getMaxWindowMessages();

        // todo: eventually we have to see if make sense to cache this objects or not
        return chatMemoryMap.computeIfAbsent(memoryId, k -> {
                    final ChatMemory memory = MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(maxWindowMessages)
                            //.chatMemoryStore(store) // todo by now the default single mem, in the future may have redis + db
                            .build();

                    return memory;
                });
    }

    private static String createMemoryId(final ChatMemoryBean chatMemoryBean) {
        return String.format(CHAT_MEMORY_PREFIX + ":%s:%s:%s",
                chatMemoryBean.getTenantId(), chatMemoryBean.getUserId(), chatMemoryBean.getConversationId());
    }

    private int getMaxWindowMessages() {
        return 40; // todo: this should be handle by model may be or global as a fallback
    }

    private void isValidChatMemoryBean(ChatMemoryBean chatMemoryBean) {
        // todo: all good by now
    }
}
