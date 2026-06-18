/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Helper for static-publishing un-publish bundles.
 *
 * <p>When content is being removed from a static endpoint there is no live version left to render,
 * so the static bundlers cannot place the usual rendered artifact under {@code /live/...}. Instead
 * they write a zero-byte <b>marker</b> at the content's canonical {@code /live/} path. The static
 * publishers ({@code StaticPublisher} for the filesystem mirror and {@code AWSS3Publisher} for S3)
 * later use that path — not its contents — to delete the matching artifact from the endpoint.</p>
 *
 * <p>The behavior is gated on {@link PublisherConfig#isStatic()} so it can only ever apply to static
 * publishing. Site Search bundles ({@code isStatic == false}) and dynamic Push Publishing are
 * therefore excluded, and a marker can never reach a Site Search index bundle.</p>
 *
 * @see <a href="https://github.com/dotCMS/core/issues/35365">issue #35365</a>
 */
public final class StaticUnpublishMarker {

    /** The bundle-relative prefix every marker must stay within. */
    private static final String LIVE_PREFIX = File.separator + "live" + File.separator;

    private StaticUnpublishMarker() {
    }

    /**
     * Whether the given bundle is a static un-publish bundle, i.e. it targets a static endpoint and
     * its operation is {@link Operation#UNPUBLISH}. A {@code null} config or operation is treated as
     * not-a-static-unpublish (the legacy publish behavior).
     *
     * @param config the bundle configuration
     * @return {@code true} only for static {@code UNPUBLISH} bundles
     */
    public static boolean isStaticUnpublish(final PublisherConfig config) {
        return config != null
                && config.isStatic()
                && Operation.UNPUBLISH.equals(config.getOperation());
    }

    /**
     * Writes a zero-byte {@code /live/} path marker for the content being un-published, unless the
     * marker is already present. Does nothing for non static-unpublish bundles.
     *
     * @param config            the bundle configuration
     * @param output            the bundle output
     * @param liveUnpublishPath the canonical {@code /live/<host>/<langId>/<uri>} path of the content
     * @return {@code true} when this is a static un-publish bundle (the marker was handled), so the
     *         caller can skip the regular render/copy logic; {@code false} otherwise
     * @throws IOException if the marker file cannot be created
     */
    public static boolean writeMarkerIfNeeded(final PublisherConfig config, final BundleOutput output,
            final String liveUnpublishPath) throws IOException {
        if (!isStaticUnpublish(config)) {
            return false;
        }
        // Defend against path traversal / tar-slip: the host and asset path segments are not
        // validated against ".." at save time, so normalize and reject anything that escapes the
        // bundle /live/ subtree before it can reach the bundle output (filesystem or tar entry).
        // This is the second line of defence — callers that compose the path (writeContentMarkers)
        // also reject traversal up front, but this guard protects any direct caller of this method.
        final String safePath = safeLivePath(liveUnpublishPath);
        if (safePath == null) {
            Logger.warn(StaticUnpublishMarker.class, "Skipping un-publish marker whose path escapes "
                    + "the bundle /live/ root (possible traversal): " + sanitizeForLog(liveUnpublishPath));
            return true;
        }
        // Record the canonical /live/ path (contents irrelevant) so the static publisher knows which
        // rendered artifact to delete from the endpoint. The publisher deletes by path, not content.
        if (!output.exists(safePath)) {
            try (final OutputStream marker = output.addFile(safePath)) {
                // marker only; no content needed
            }
        }
        return true;
    }

    /**
     * Normalizes a bundle-relative marker path and returns it only when it still resolves within the
     * {@code /live/} subtree. Returns {@code null} when traversal segments ({@code ..}) would place
     * the marker outside {@code /live/}, so callers skip writing it.
     *
     * @param liveUnpublishPath the composed {@code /live/<host>/<langId>/<uri>} path
     * @return the normalized safe path, or {@code null} if it escapes {@code /live/}
     */
    private static String safeLivePath(final String liveUnpublishPath) {
        final String normalized = Paths.get(liveUnpublishPath).normalize().toString();
        return normalized.startsWith(LIVE_PREFIX) ? normalized : null;
    }

    /**
     * Sanitizes an attacker-influenced value before logging: strips CR/LF (prevents log-forging via
     * injected line breaks) and caps the length.
     *
     * @param value the value to log
     * @return a single-line, length-capped representation safe to log
     */
    private static String sanitizeForLog(final String value) {
        if (value == null) {
            return "null";
        }
        final String oneLine = value.replaceAll("[\\r\\n]", "_");
        return oneLine.length() > 128 ? oneLine.substring(0, 128) + "..." : oneLine;
    }

    /**
     * Writes {@code /live/} path markers for a single un-published content asset, one per language,
     * so the static publisher can delete the matching artifacts from the endpoint. Does nothing for
     * non static-unpublish bundles or when the asset cannot be located.
     *
     * @param config      the bundle configuration
     * @param output      the bundle output
     * @param hostname    the hostname of the site the content belongs to
     * @param languageIds the language ids (as strings) to remove the content for
     * @param assetPath   the content path/URI relative to host and language (e.g. {@code /about-us/index})
     * @throws IOException if a marker file cannot be created
     */
    public static void writeContentMarkers(final PublisherConfig config, final BundleOutput output,
            final String hostname, final Collection<String> languageIds, final String assetPath)
            throws IOException {
        if (!isStaticUnpublish(config) || hostname == null || languageIds == null
                || assetPath == null || assetPath.isBlank()) {
            return;
        }
        // Defense-in-depth: reject a hostname or asset path that could escape the bundle root before
        // composing the path. safeLivePath (in writeMarkerIfNeeded) is the downstream backstop; these
        // up-front checks give an explicit, symmetric rejection at the composition point.
        if (hostname.contains(File.separator) || hostname.contains("/") || hostname.contains("..")) {
            Logger.warn(StaticUnpublishMarker.class, "Skipping un-publish markers for a hostname that "
                    + "contains path separators or traversal sequences: " + sanitizeForLog(hostname));
            return;
        }
        if (assetPath.contains("..")) {
            Logger.warn(StaticUnpublishMarker.class, "Skipping un-publish markers for an asset path that "
                    + "contains traversal sequences: " + sanitizeForLog(assetPath));
            return;
        }
        for (final String languageId : languageIds) {
            final String liveUnpublishPath = File.separator + "live" + File.separator
                    + hostname + File.separator + languageId
                    + assetPath.replace("/", File.separator);
            writeMarkerIfNeeded(config, output, liveUnpublishPath);
        }
    }
}
