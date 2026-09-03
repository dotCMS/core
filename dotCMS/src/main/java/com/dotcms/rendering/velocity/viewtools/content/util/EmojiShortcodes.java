package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a Story Block {@code emoji} node's shortcode to the character it stands for (#37340).
 * <p>
 * A stored {@code emoji} node carries only a name — {@code {"type":"emoji","attrs":{"name":
 * "copyright"}}} — never the literal character and never a codepoint. Without a lookup table the
 * VTL renderer had no branch for the node at all and silently dropped it, so a {@code ©} in a
 * footer simply vanished from the published page.
 * <p>
 * The table is generated from {@code @tiptap/extension-emoji}'s own list by
 * {@code core-web/tools/scripts/generate-emoji-map.mjs}, so it cannot describe a character the
 * editor could not have produced. CI regenerates it and fails on any drift.
 *
 * @author dotCMS
 */
public final class EmojiShortcodes {

    private static final String RESOURCE = "/emoji/emoji-shortcodes.json";

    /** Loaded once; the table is a few tens of KB and never changes at runtime. */
    private static final Map<String, String> SHORTCODES = load();

    private EmojiShortcodes() {
        // utility class
    }

    private static Map<String, String> load() {
        final Map<String, String> map = new HashMap<>();

        try (InputStream stream = EmojiShortcodes.class.getResourceAsStream(RESOURCE)) {
            if (null == stream) {
                Logger.warn(EmojiShortcodes.class,
                        "Emoji shortcode map not found on the classpath: " + RESOURCE
                                + ". Stored emoji nodes will render as :shortcode:.");
                return Collections.emptyMap();
            }

            final JSONObject json = new JSONObject(
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8));

            // JSONObject#keySet is raw, so iterate as Object and narrow.
            for (final Object key : json.keySet()) {
                final String name = String.valueOf(key);
                map.put(name, json.getString(name));
            }
        } catch (final IOException | JSONException e) {
            // Never fail a page render over this: an unreadable table degrades to the
            // :shortcode: fallback, which is visible but harmless.
            Logger.error(EmojiShortcodes.class,
                    "Could not read the emoji shortcode map: " + e.getMessage(), e);
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Resolves an emoji shortcode.
     *
     * @param name the shortcode stored in {@code attrs.name}
     * @return the character, or {@code null} when the name is unknown
     */
    public static String resolve(final String name) {
        return null == name ? null : SHORTCODES.get(name);
    }

    /** Number of entries loaded; exposed for the map-integrity test. */
    public static int size() {
        return SHORTCODES.size();
    }
}
