package com.dotcms.ai.v2.api.util;

import com.dotcms.ai.v2.api.embeddings.RetrievedChunk;
import com.liferay.util.StringPool;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility for composing system/context/user prompt blocks.
 * Keeps the prompt policy centralized and testable.
 * @author jsanca
 */
public final class PromptComposer {

    private PromptComposer() {}

    /**
     * Build a system prompt according to language and optional style.
     * Keep it concise to save tokens.
     *
     * @param language Optional language id/locale. Null means default locale.
     * @param style Optional style (e.g., "brief", "bulleted").
     * @return System instruction block.
     */
    public static String systemForLanguage(final Long language, final String style) {

        final String lang = (language == null) ? "es" : String.valueOf(language); // todo: this should be in our config
        final String styleToken = (style == null) ? "" : " Style: " + style + ".";
        return "You are a precise assistant. Respond in language \"" + lang + "\"."
                + " Be concise, factual, and avoid speculation." + styleToken + "\n";
    }

    /**
     * Render retrieved chunks as a compact context block.
     * Truncates text per-chunk to avoid bloating tokens.
     */
    public static String contextBlock(final List<RetrievedChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            return StringPool.BLANK;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("\n[CONTEXT]\n");
        int n = 1;
        for (final RetrievedChunk retrievedChunk : chunks) {
            String text = truncate(retrievedChunk.getText(), 900); // guardrail
            sb.append("#").append(n++).append(" (score=").append(String.format("%.3f", retrievedChunk.getScore())).append(")\n");
            if (retrievedChunk.getTitle() != null) sb.append("Title: ").append(retrievedChunk.getTitle()).append("\n");
            if (retrievedChunk.getUrl() != null) sb.append("URL: ").append(retrievedChunk.getUrl()).append("\n");
            sb.append(text).append("\n\n");
        }
        sb.append("[/CONTEXT]\n");
        return sb.toString();
    }

    /** Build a standard user block for general completions. */
    public static String userBlock(final String prompt, final Map<String,Object> responseFormat) {

        final StringBuilder sb = new StringBuilder();
        sb.append("\n[USER]\n");
        if (responseFormat != null && !responseFormat.isEmpty()) {
            sb.append("If a response format is specified, return ONLY valid output for it.\n");
        }
        sb.append(prompt).append("\n[/USER]\n");
        return sb.toString();
    }

    /** Build a user block tailored for summarization. */
    public static String summarizeUserBlock(final String prompt,
                                            final String style,
                                            final Integer maxChars,
                                            final Map<String,Object> responseFormat) {

        final StringJoiner stringJoiner = new StringJoiner(" ");
        stringJoiner.add("Summarize the provided context and the following user prompt.");
        if (style != null && !style.isEmpty()) {
            stringJoiner.add("Style:").add(style + ".");
        }
        if (maxChars != null && maxChars > 0) {
            stringJoiner.add("Hard limit:").add(String.valueOf(maxChars)).add("characters.");
        }
        if (responseFormat != null && !responseFormat.isEmpty()) {
            stringJoiner.add("If response format is present, output ONLY valid content for it.");
        }
        String instr = stringJoiner.toString();

        return "\n[USER]\n" + instr + "\nUser prompt: " + prompt + "\n[/USER]\n";
    }

    /** Concatenate blocks into the final prompt. */
    public static String combine(final String systemMessage,
                                 final String contextMesage,
                                 final String userMessage) {

        final StringBuilder sb = new StringBuilder();

        if (systemMessage != null) {
            sb.append(systemMessage.trim()).append("\n");
        }
        if (contextMesage != null) {
            sb.append(contextMesage.trim()).append("\n");
        }
        if (userMessage != null) {
            sb.append(userMessage.trim()).append("\n");
        }

        return sb.toString();
    }

    private static String truncate(final String text, final int max) {
        if (text == null) {
            return StringPool.BLANK;
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}
