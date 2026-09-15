package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiKeys;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;

/**
 * Single place where the dotAI viewtools turn a caught exception into the object a Velocity
 * template receives.
 *
 * <p>Security intent (issue #37154, epic #37255): anything a viewtool returns can be printed
 * into a public page. The payload built here therefore carries exactly one key,
 * {@link AiKeys#ERROR}, holding a fixed sentence, and never anything derived from the exception:
 * no stack trace, no message, no class name, no provider response. The full exception, with its
 * stack trace, goes to the server log at ERROR under the logger of the tool class that caught it,
 * so operators keep the detail that templates no longer receive.</p>
 *
 * <p>Do not add the prompt, the request body, the query text or the user to the log message; the
 * exception object is the only diagnostic detail passed on, as the throwable argument.</p>
 *
 * @author hassandotcms
 */
final class AIViewToolErrorHandler {

    /** The only text a template sees when a dotAI viewtool call fails. */
    public static final String GENERIC_ERROR_MESSAGE = "AI request failed. Check the dotCMS log for details.";

    /** Fixed log message; the exception travels as the throwable argument, never in this text. */
    static final String LOG_MESSAGE = "AI viewtool call failed";

    private AIViewToolErrorHandler() {
        // static utility
    }

    /**
     * Logs {@code cause} at ERROR under {@code source}'s logger and returns the template-safe
     * failure payload.
     *
     * @param source the viewtool class that caught the exception; pass the literal class
     *               (for example {@code CompletionsTool.class}), not {@code getClass()}, so
     *               anonymous test subclasses do not change the logger name
     * @param cause  the caught exception
     * @return a {@link JSONObject} with the single key {@code error} and a fixed message
     */
    static JSONObject handle(final Class<?> source, final Throwable cause) {
        Logger.error(source, LOG_MESSAGE, cause);
        return new JSONObject().put(AiKeys.ERROR, GENERIC_ERROR_MESSAGE);
    }

}
