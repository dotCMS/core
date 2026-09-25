package com.dotcms.ai.viewtool;

import com.dotcms.ai.AiKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import org.owasp.encoder.Encode;

import java.util.Collection;
import java.util.Map;

/**
 * Produces the HTML-escaped copy of a dotAI viewtool payload that the {@code $ai} tool hands to
 * Velocity by default (#37153).
 *
 * <p>Two entry points, chosen by the calling tool according to what the payload contains:
 * <ul>
 *   <li>{@link #deepEscape(Object)}: every string leaf at any depth is encoded. Used for payloads
 *       that are provider output in full ({@code raw}, {@code generateText}, {@code generateImage})
 *       and for every handled error payload.</li>
 *   <li>{@link #escapeSearchShaped(Object)}: for the {@code summarize} / {@code search.*} shape.
 *       Encodes {@code query}, {@code error}, the whole {@code openAiResponse} subtree and
 *       {@code dotCMSResults[].matches[].extractedText}; every other value, including
 *       {@code title} and the contentlet fields under {@code dotCMSResults}, is carried through
 *       by reference.</li>
 * </ul>
 *
 * <p>The input is never mutated: new {@link JSONObject} / {@link JSONArray} containers are built
 * along the escaped paths and untouched values are shared. Plain {@link Map} and {@link Collection}
 * inputs (including immutable {@code Map.of(...)}) come back as {@code JSONObject} /
 * {@code JSONArray}. Java {@code null} values become {@link JSONObject#NULL}, exactly as
 * {@code new JSONObject(Map)} does. Numbers, booleans, enums and {@code NULL} are returned as is;
 * any other leaf object is stringified and encoded (the JSON carrier would stringify it on output).
 *
 * <p>Encoding is OWASP {@link Encode#forHtml(String)} ({@code & < > " '}), safe for HTML body and
 * quoted-attribute context. It is not idempotent: text that already contains entities is encoded
 * again.
 *
 * <p>Package-private on purpose: templates reach the escaped or the unescaped payload only through
 * {@code $ai} and {@code $ai.unsafe}, never through this class.
 *
 * @author hassandotcms
 */
final class AIViewToolOutputEscaper {

    private AIViewToolOutputEscaper() {
        // static utility
    }

    /**
     * Returns a copy of {@code value} in which every string at any depth is HTML-encoded.
     *
     * @param value a {@link String}, {@link Map} (including {@link JSONObject}), {@link Collection}
     *              (including {@link JSONArray}), scalar, {@link JSONObject#NULL} or {@code null}
     * @return the encoded copy; {@code Map} inputs become {@link JSONObject}, {@code Collection}
     *         inputs become {@link JSONArray}, scalars and {@code null} are returned unchanged
     */
    static Object deepEscape(final Object value) {
        if (value instanceof String) {
            return Encode.forHtml((String) value);
        }
        if (value instanceof Map) {
            final JSONObject copy = new JSONObject();
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                copy.put(String.valueOf(entry.getKey()), deepEscape(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof Collection) {
            final JSONArray copy = new JSONArray();
            for (final Object element : (Collection<?>) value) {
                copy.put(deepEscape(element));
            }
            return copy;
        }
        if (value == null || JSONObject.NULL.equals(value) || value instanceof Number
                || value instanceof Boolean || value.getClass().isEnum()) {
            return value;
        }
        // any other leaf object would be stringified by the JSON carrier on output; encode that
        // string now so no path renders raw markup
        return Encode.forHtml(String.valueOf(value));
    }

    /**
     * Typed convenience overload of {@link #deepEscape(Object)} for callers that must keep returning
     * {@link JSONObject}.
     *
     * @param value the payload
     * @return the encoded copy, or {@code null} when {@code value} is {@code null}
     */
    static JSONObject deepEscape(final JSONObject value) {
        return (JSONObject) deepEscape((Object) value);
    }

    /**
     * Escapes the {@code summarize} / {@code search.*} payload by source: {@code query} and
     * {@code error} (echoed or fixed text), the {@code openAiResponse} subtree (provider output) and
     * {@code dotCMSResults[].matches[].extractedText} (stored search excerpt, which the {@code cache}
     * index fills with other visitors' query text). Everything else is shared by reference.
     *
     * @param payload the API result; a non-{@link Map} value is delegated to {@link #deepEscape(Object)}
     * @return the escaped copy
     */
    static Object escapeSearchShaped(final Object payload) {
        if (!(payload instanceof Map)) {
            return deepEscape(payload);
        }
        final JSONObject copy = new JSONObject();
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) payload).entrySet()) {
            final String key = String.valueOf(entry.getKey());
            final Object value = entry.getValue();
            switch (key) {
                case AiKeys.QUERY:
                case AiKeys.ERROR:
                    copy.put(key, escapeIfString(value));
                    break;
                case AiKeys.OPEN_AI_RESPONSE:
                    copy.put(key, deepEscape(value));
                    break;
                case AiKeys.DOT_CMS_RESULTS:
                    copy.put(key, escapeResults(value));
                    break;
                default:
                    copy.put(key, value);
            }
        }
        return copy;
    }

    private static Object escapeResults(final Object results) {
        if (!(results instanceof Collection)) {
            return results;
        }
        final JSONArray copy = new JSONArray();
        for (final Object result : (Collection<?>) results) {
            copy.put(escapeResult(result));
        }
        return copy;
    }

    private static Object escapeResult(final Object result) {
        if (!(result instanceof Map)) {
            return result;
        }
        final JSONObject copy = new JSONObject();
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) result).entrySet()) {
            final String key = String.valueOf(entry.getKey());
            final Object value = entry.getValue();
            copy.put(key, AiKeys.MATCHES.equals(key) ? escapeMatches(value) : value);
        }
        return copy;
    }

    private static Object escapeMatches(final Object matches) {
        if (!(matches instanceof Collection)) {
            return matches;
        }
        final JSONArray copy = new JSONArray();
        for (final Object match : (Collection<?>) matches) {
            copy.put(escapeMatch(match));
        }
        return copy;
    }

    private static Object escapeMatch(final Object match) {
        if (!(match instanceof Map)) {
            return match;
        }
        final JSONObject copy = new JSONObject();
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) match).entrySet()) {
            final String key = String.valueOf(entry.getKey());
            final Object value = entry.getValue();
            copy.put(key, AiKeys.EXTRACTED_TEXT.equals(key) ? escapeIfString(value) : value);
        }
        return copy;
    }

    private static Object escapeIfString(final Object value) {
        return value instanceof String ? Encode.forHtml((String) value) : value;
    }

}
