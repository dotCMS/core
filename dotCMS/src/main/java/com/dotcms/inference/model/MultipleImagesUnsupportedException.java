package com.dotcms.inference.model;

/**
 * Raised when a caller asks for several images from a provider whose implementation only ever
 * produces one.
 *
 * <p>The capability is declared on the provider abstraction but not honoured by every
 * implementation behind it, and the unsupported case announces itself by throwing from deep
 * inside the client library. Left as it comes, that reaches the caller as an upstream failure —
 * a retryable status for a request that cannot succeed no matter how many times it is sent. This
 * type exists so the endpoint can tell the two apart and answer with a refusal naming
 * {@code n} instead.</p>
 *
 * <p>This mirrors the adopted format's own behaviour, where {@code n} is accepted by the
 * operation but rejected for models that cannot honour it.</p>
 */
public class MultipleImagesUnsupportedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param modelName the model that cannot produce more than one image per request
     */
    public MultipleImagesUnsupportedException(final String modelName) {
        super("The image model '" + modelName + "' can only produce one image per request");
    }
}
