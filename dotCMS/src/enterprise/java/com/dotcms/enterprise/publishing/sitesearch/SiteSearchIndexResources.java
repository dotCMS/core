/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.content.index.domain.DotSearchException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Loads the bundled site-search index settings/mapping JSON and applies the optional analyzer
 * override.
 *
 * <p>The {@value #ANALYZER_PROPERTY} {@link Config} property (env:
 * {@code DOT_SITE_SEARCH_ANALYZER}) names any analyzer known to the search cluster — a built-in
 * language analyzer ({@code cjk}, {@code arabic}, {@code thai}, ...) or a plugin-provided one
 * ({@code kuromoji}, {@code nori}, {@code smartcn} — the plugin must be installed on every node).
 * When set, it replaces the default English-tuned {@code standard_content} analyzer on the text
 * fields of the site-search mapping; unset keeps the bundled defaults untouched. The override
 * takes effect on index creation, so existing indices need a rebuild to pick it up. An analyzer
 * name unknown to the cluster fails index creation loudly with the engine's error.</p>
 */
final class SiteSearchIndexResources {

    static final String ANALYZER_PROPERTY = "SITE_SEARCH_ANALYZER";

    private static final List<String> TEXT_FIELDS = List.of("content", "title", "description", "author");

    private SiteSearchIndexResources() {
    }

    /** The bundled index settings, verbatim. */
    static String settings(final String resource) {
        return readResource(resource);
    }

    /** The bundled index mapping, with the {@value #ANALYZER_PROPERTY} override applied when set. */
    static String mapping(final String resource) {
        final String mapping = readResource(resource);
        final String analyzer = Config.getStringProperty(ANALYZER_PROPERTY, null);
        if (!UtilMethods.isSet(analyzer)) {
            return mapping;
        }
        try {
            final JSONObject root = new JSONObject(mapping);
            final JSONObject properties = root.getJSONObject("properties");
            for (final String field : TEXT_FIELDS) {
                properties.getJSONObject(field).put("analyzer", analyzer);
            }
            // the ngram subfield still indexes with the edge-ngram analyzer, but must search with
            // the same analyzer as the main content field to tokenize queries consistently
            properties.getJSONObject("content").getJSONObject("fields").getJSONObject("ngram")
                    .put("search_analyzer", analyzer);
            return root.toString();
        } catch (final JSONException e) {
            throw new DotSearchException("Error applying " + ANALYZER_PROPERTY + "=" + analyzer
                    + " to site search mapping " + resource + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads a UTF-8 classpath resource fully into a String via {@code getResourceAsStream}, so it
     * resolves whether the resource sits on the filesystem or inside a packaged JAR. Throws a clear
     * {@link DotSearchException} when the resource is absent rather than NPE-ing on a null URL.
     */
    private static String readResource(final String resource) {
        try (final InputStream in =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new DotSearchException(
                        "Site search index resource not found on the classpath: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new DotSearchException(
                    "Error reading site search index resource " + resource + ": " + e.getMessage(), e);
        }
    }
}
