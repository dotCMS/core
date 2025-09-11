package com.dotcms.ai.v2.api.embeddings;

/**
 * General exception for embeddings
 * @author jsanca
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
