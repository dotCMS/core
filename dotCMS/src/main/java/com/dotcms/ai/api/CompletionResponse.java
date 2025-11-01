package com.dotcms.ai.api;

public class CompletionResponse {

    private final String messageText;
    private final Object aiMessage;
    private final Object metatada;

    public CompletionResponse(String messageText, Object aiMessage, Object metatada) {
        this.messageText = messageText;
        this.aiMessage = aiMessage;
        this.metatada = metatada;
    }

    public String getMessageText() {
        return messageText;
    }

    public Object getAiMessage() {
        return aiMessage;
    }

    public Object getMetatada() {
        return metatada;
    }
}
