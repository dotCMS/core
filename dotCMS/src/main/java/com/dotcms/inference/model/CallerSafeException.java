package com.dotcms.inference.model;

/**
 * An invalid-request failure whose message dotCMS wrote and is therefore safe to return.
 *
 * <p>Exists because "is this text safe to show a caller?" is not a question that can be answered
 * by looking at an exception's type. Most of what reaches the error translator is an
 * {@link IllegalArgumentException}, and they arrive from two very different places: some carry a
 * sentence dotCMS composed about the site's own configuration, which is exactly what a caller
 * needs to fix the problem; others carry a provider client's message, which can hold the
 * provider's endpoint, its account identifiers, or a fragment of the prompt. Passing the message
 * through whenever the type matched meant the second kind reached the caller too.</p>
 *
 * <p>So the decision is made where the text is written rather than where it is rendered. Throwing
 * this type is a statement that the message was authored here and may be returned; everything
 * else is answered with a generic sentence and logged in full. Extending
 * {@link IllegalArgumentException} keeps existing callers that catch the broader type working
 * unchanged, which matters because the throw sites are shared with the older dotAI endpoints,
 * whose behaviour must not shift.</p>
 */
public class CallerSafeException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message a message written by dotCMS, safe to return to a caller
     */
    public CallerSafeException(final String message) {
        super(message);
    }
}
