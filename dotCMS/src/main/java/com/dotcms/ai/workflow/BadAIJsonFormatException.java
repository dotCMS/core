package com.dotcms.ai.workflow;

public class BadAIJsonFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadAIJsonFormatException(String message) {
        super(message);
    }

    public BadAIJsonFormatException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public BadAIJsonFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
