package com.dotcms.ai.util;

import com.dotcms.ai.AiKeys;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The OpenAiRequestUtil class provides utility methods for handling OpenAI requests.
 * It includes methods for truncating prompts to a maximum length.
 *
 * @author vico
 */
public class OpenAiRequestUtil {

    private static final int PROMPT_LENGTH_MAX = 400;
    private static final Lazy<OpenAiRequestUtil> OPEN_AI_REQUEST_UTIL_LAZY = Lazy.of(OpenAiRequestUtil::new);

    /**
     * Retrieves the singleton instance of the OpenAiRequestUtil class.
     *
     * @return the singleton instance of the OpenAiRequestUtil class
     */
    public static OpenAiRequestUtil get() {
        return OPEN_AI_REQUEST_UTIL_LAZY.get();
    }

    private OpenAiRequestUtil() {
    }

    /**
     * Truncates a given prompt to a maximum length.
     *
     * @param prompt the prompt to truncate
     * @return the truncated prompt
     */
    public Pair<Boolean, String> truncatePrompt(final String prompt) {
        if (prompt.length() <= PROMPT_LENGTH_MAX) {
            return Pair.of(false, prompt);
        }

        final StringBuilder builder = new StringBuilder();
        for (final String token : prompt.split("\\s+")) {
            builder.append(token).append(" ");
            if (builder.length() + token.length() + 5 > PROMPT_LENGTH_MAX) {
                break;
            }
        }

        return Pair.of(true, builder.toString());
    }

    /**
     * Handles a large prompt by truncating it to a maximum length.
     *
     * This method takes a JSONObject that contains a prompt, truncates the prompt if it exceeds the maximum length,
     * and replaces the original prompt in the JSONObject with the truncated version.
     *
     * @param prompt the JSONObject that contains the prompt to handle
     */
    public void handleLargePrompt(final JSONObject prompt) {
        final Pair<Boolean, String> promptPair = truncatePrompt(prompt.getString(AiKeys.PROMPT));
        if (Boolean.TRUE.equals(promptPair.getLeft())) {
            prompt.put(AiKeys.PROMPT, promptPair.getRight());
        }
    }

}
