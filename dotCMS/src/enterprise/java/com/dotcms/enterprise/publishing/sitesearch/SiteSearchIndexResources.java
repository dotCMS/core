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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Loads Site Search settings and mappings, applying analyzer and source-storage configuration.
 * Source is retained by default; disabling it stores result fields individually so metadata and
 * highlight snippets remain available. Both settings are read again for each new index.
 *
 * <p>The {@value #ANALYZER_PROPERTY} {@link Config} property (env:
 * {@code DOT_SITE_SEARCH_ANALYZER}) names any analyzer known to the search cluster — a built-in
 * language analyzer ({@code cjk}, {@code arabic}, {@code thai}, ...) or a plugin-provided one
 * ({@code kuromoji}, {@code nori}, {@code smartcn} — the plugin must be installed on every node).
 * When set, it is applied to every text field of the site-search mapping (replacing the
 * English-stemming {@code standard_content} on {@code content}, and the plain {@code standard} on
 * {@code content_raw}/{@code title}/{@code description}/{@code author}); unset keeps the bundled
 * defaults untouched. The override takes effect on index creation, so existing indices
 * need a rebuild to pick it up. An analyzer name unknown to the cluster fails index creation
 * loudly with the engine's error.
 *
 * <p>Operational notes: during ES→OS migration dual-write phases the analyzer must be resolvable
 * by <b>every</b> active engine — a shadow-engine mapping rejection is only logged by the phase
 * router, leaving the shadow index on dynamic mapping until reads flip. And when changing the
 * analyzer, run a fresh site-search job rather than "clear index": clearing deletes and recreates
 * the active index in place, so a rejected mapping would leave the still-default index
 * mappingless.</p></p>
 */
final class SiteSearchIndexResources {

    static final String ANALYZER_PROPERTY = "SITE_SEARCH_ANALYZER";

    /** Get requests do not expand stored-field wildcards, so name the supported result fields. */
    static final List<String> RESULT_FIELDS = List.of("content", "content_raw", "host", "contentLength",
            "uri", "url", "mimeType", "title", "description", "modified", "keywords", "language",
            "author", "id", "fileName", "seokeywords");

    // content_raw is not written by the publishers' mapping-visible DTO fields but IS added to every
    // text/* document (see ESSiteSearchAPI/OSSiteSearchAPI "content_raw"); it must be covered here or
    // query_string(default_field:*) matches it with default analysis, reintroducing CJK unigram
    // false positives.
    private static final List<String> TEXT_FIELDS =
            List.of("content", "content_raw", "title", "description", "author");

    private SiteSearchIndexResources() {
    }

    /** The bundled index settings, verbatim. */
    static String settings(final String resource) {
        return readResource(resource);
    }

    /** Applies the current analyzer override and source-storage setting (on by default). */
    static String mapping(final String resource) {
        final String mapping = readResource(resource);
        final String configured = Config.getStringProperty(ANALYZER_PROPERTY, null);
        final boolean sourceEnabled = Config.getBooleanProperty("index.sitesearch.source.enabled", true);
        if (!UtilMethods.isSet(configured) && sourceEnabled) {
            return mapping;
        }
        final String analyzer = configured == null ? null : configured.trim();
        try {
            final JSONObject root = new JSONObject(mapping);
            final JSONObject properties = root.getJSONObject("properties");
            if (!sourceEnabled) {
                root.put("_source", new JSONObject().put("enabled", false));
                for (final Object field : properties.keySet()) {
                    properties.getJSONObject(String.valueOf(field)).put("store", true);
                }
                // Keep extra crawler strings (id, fileName, SEO metadata) retrievable as well.
                root.put("dynamic_templates", List.of(Map.of("stored_strings", Map.of(
                        "match_mapping_type", "string", "mapping", Map.of(
                                "type", "text", "store", true, "fields", Map.of(
                                        "keyword", Map.of("type", "keyword", "ignore_above", 256)))))));
            }
            if (UtilMethods.isSet(configured)) {
                for (final String field : TEXT_FIELDS) {
                    properties.getJSONObject(field).put("analyzer", analyzer);
                }
                // Edge ngrams keep their index analyzer but follow the configured search analyzer.
                properties.getJSONObject("content").getJSONObject("fields").getJSONObject("ngram")
                        .put("search_analyzer", analyzer);
            }
            final String result = root.toString();
            if (result == null) {
                // this JSONObject fork returns null instead of throwing on serialization failure
                throw new DotSearchException("Error serializing site search mapping " + resource
                        + " after applying its analyzer and source-storage settings");
            }
            Logger.info(SiteSearchIndexResources.class,
                    "Site search mapping " + resource + ": source=" + sourceEnabled
                            + ", analyzer=" + analyzer);
            return result;
        } catch (final JSONException e) {
            throw new DotSearchException("Error configuring site search mapping " + resource
                    + ": " + e.getMessage(), e);
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
