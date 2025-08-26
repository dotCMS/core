package com.dotcms.ai.v2.api;

public class ChatResponse {

    private final String conversationId;
    private final String answer;
    public ChatResponse(final String conversationId, final String answer) {
        this.conversationId = conversationId;
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getAnswer() {
        return answer;
    }
}
