package com.dotmarketing.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for {@link ImageUriPathExpander} — the shorthand-token -> filter-param expansion used by
 * {@link ShortyServlet}. Focused on the libvips additions: the {@code /avif} format token and the
 * {@code /smart} modifier that turns a crop into a content-aware smartcrop.
 */
public class ShortyServletImagePathTest {

    private String build(final boolean avif, final boolean smart, final int quality,
            final int cropW, final int cropH, final int width, final String... tokens) {
        final StringBuilder sb = new StringBuilder();
        ImageUriPathExpander.expand(
                width, 0, 0, 0, 0, 0, quality,
                false, false, false, avif, smart,
                sb, Optional.empty(), cropW, cropH, 0, tokens);
        return sb.toString();
    }

    @Test
    public void smart_turns_crop_into_smartcrop() {
        final String path = build(false, true, 0, 300, 400, 0, "300cw", "400ch");
        assertTrue("smartcrop_w expected: " + path, path.contains("/smartcrop_w/300"));
        assertTrue("smartcrop_h expected: " + path, path.contains("/smartcrop_h/400"));
        assertFalse("plain crop_w must not be emitted: " + path, path.contains("/crop_w/"));
        assertFalse("plain crop_h must not be emitted: " + path, path.contains("/crop_h/"));
    }

    @Test
    public void crop_without_smart_stays_regular_crop() {
        final String path = build(false, false, 0, 300, 400, 0, "300cw", "400ch");
        assertTrue("crop_w expected: " + path, path.contains("/crop_w/300"));
        assertTrue("crop_h expected: " + path, path.contains("/crop_h/400"));
        assertFalse("smartcrop must not leak in: " + path, path.contains("/smartcrop_"));
    }

    @Test
    public void avif_token_emits_default_quality() {
        final String path = build(true, false, 0, 0, 0, 200, "200w", "avif");
        assertTrue("resize kept: " + path, path.contains("/resize_w/200"));
        assertTrue("avif_q default 75 expected: " + path, path.contains("/avif_q/75"));
    }

    @Test
    public void avif_token_honours_explicit_quality() {
        final String path = build(true, false, 40, 0, 0, 200, "200w", "40q", "avif");
        assertTrue("avif_q should use explicit quality: " + path, path.contains("/avif_q/40"));
    }
}
