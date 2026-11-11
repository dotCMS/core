package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Reduces the page URL fields of Content Analytics events to an allowlist of query parameters
 * before the events are forwarded to the analytics backend.
 *
 * <p>For every event in the ingest body, {@code data.page.url}, {@code data.page.doc_search},
 * {@code data.page.doc_hash} and the clicked link's {@code data.element.href} keep only the query
 * parameters whose name is allowed; params are separated by {@code &}, {@code ;} or a further
 * {@code ?}. Values of allowed params are kept as sent. The standard campaign parameters and ad
 * click ids ({@link #DEFAULT_ALLOWED_PARAMS}) are always allowed; more names can be added for the
 * whole instance ({@value #ALLOWED_PARAMS_PROPERTY}) or per site (the
 * {@value ContentAnalyticsUtil#ALLOWED_URL_PARAMS_KEY} field of the Content Analytics App). A site
 * can turn filtering off with the {@value ContentAnalyticsUtil#FILTER_URL_PARAMS_KEY} App field.
 *
 * <p>Fragments are handled so that hash-routed single-page applications keep their route: a
 * fragment such as {@code #/products/42?tab=2} keeps {@code #/products/42} and filters the
 * parameters after the {@code ?}, while a fragment made only of parameters
 * ({@code #a=1&b=2}) is filtered like a query string. A route starts with {@code /} or
 * {@code !/}; it ends at its first {@code &} or {@code ;}, and a segment containing {@code =} is
 * removed with everything after it ({@code #/catalog/color=red} keeps {@code #/catalog}).
 *
 * <p>All methods work on plain strings and never throw on unexpected input: a value that cannot be
 * interpreted is filtered as far as possible. Field names are matched regardless of case; an array
 * or object in a URL field is replaced with {@code ""}, and an event whose {@code data} or
 * {@code page} is not an object is removed from the batch.
 */
public final class UrlParamFilter {

    /**
     * Config property with extra parameter names to keep, comma-separated, added for every site
     * (environment variable {@code DOT_ANALYTICS_URL_ALLOWED_PARAMS}).
     */
    public static final String ALLOWED_PARAMS_PROPERTY = "analytics.url.allowed.params";

    /**
     * Parameter names that are always kept: the standard campaign parameters (including the ones
     * added by Google Analytics 4) and ad click ids.
     */
    public static final Set<String> DEFAULT_ALLOWED_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
            "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
            "gclid", "fbclid", "dclid");

    private static final String EVENTS_KEY = "events";
    private static final String DATA_KEY = "data";
    private static final String PAGE_KEY = "page";
    private static final String URL_KEY = "url";
    private static final String DOC_SEARCH_KEY = "doc_search";
    private static final String DOC_HASH_KEY = "doc_hash";
    private static final String DOC_PATH_KEY = "doc_path";
    private static final String ELEMENT_KEY = "element";
    private static final String HREF_KEY = "href";

    /** A {@code ;jsessionid=...} path segment, up to the next {@code /} or {@code ;}. */
    private static final Pattern SESSION_ID_SEGMENT = Pattern.compile("(?i);jsessionid=[^/;]*");

    private UrlParamFilter() {
        // static utility
    }

    /**
     * Tells whether page URL fields should be filtered for a site before its events are forwarded.
     * Filtering applies unless the site's {@value ContentAnalyticsUtil#FILTER_URL_PARAMS_KEY} App
     * field is explicitly {@code false}; a missing or blank value keeps it on.
     *
     * @param siteSecrets the site's Content Analytics App secrets; may be {@code null} or empty
     * @return {@code false} only when the site has turned filtering off
     */
    public static boolean isEnabled(final Map<String, Secret> siteSecrets) {
        if (siteSecrets == null) {
            return true;
        }
        final Secret filterSetting = siteSecrets.get(ContentAnalyticsUtil.FILTER_URL_PARAMS_KEY);
        return filterSetting == null || !"false".equalsIgnoreCase(filterSetting.getString().trim());
    }

    /**
     * Builds the set of query parameter names to keep for a site: the defaults, plus the names in
     * {@value #ALLOWED_PARAMS_PROPERTY}, plus the names in the site's
     * {@value ContentAnalyticsUtil#ALLOWED_URL_PARAMS_KEY} App field. Names are trimmed and lower-cased; blank entries are ignored.
     *
     * @param siteSecrets the site's Content Analytics App secrets; may be {@code null} or empty
     * @return an unmodifiable set of lower-case parameter names
     */
    public static Set<String> resolveAllowedParams(final Map<String, Secret> siteSecrets) {
        final Set<String> allowed = new HashSet<>(DEFAULT_ALLOWED_PARAMS);
        for (final String names : Config.getStringArrayProperty(ALLOWED_PARAMS_PROPERTY, new String[0])) {
            addNames(allowed, names);
        }
        if (siteSecrets != null) {
            final Secret siteNames = siteSecrets.get(ContentAnalyticsUtil.ALLOWED_URL_PARAMS_KEY);
            if (siteNames != null) {
                addNames(allowed, siteNames.getString());
            }
        }
        return Collections.unmodifiableSet(allowed);
    }

    /**
     * Filters the page URL fields of every event in a parsed ingest body, in place. Keys are
     * matched regardless of case at every level ({@code events}, {@code data}, {@code page} and
     * the fields below). For each {@code events[].data.page} object:
     * <ul>
     *     <li>{@code url} goes through {@link #filterUrl}, {@code doc_search} through
     *     {@link #filterQuery}, {@code doc_hash} through {@link #filterFragment} and
     *     {@code doc_path} through {@link #filterPath};</li>
     *     <li>one of those fields holding an array or an object is replaced with {@code ""};
     *     numbers, booleans and {@code null} are left as they are;</li>
     *     <li>every other field is left untouched.</li>
     * </ul>
     * The {@code href} of an {@code events[].data.element} object goes through {@link #filterUrl}
     * as well (an array or object becomes {@code ""}).
     * An event whose {@code data} or {@code page} is present but is not an object (an array or a
     * string, for example) is removed from the batch. Other unexpected shapes are skipped.
     *
     * @param body    the parsed ingest body; may be {@code null}
     * @param allowed lower-case parameter names to keep, see {@link #resolveAllowedParams}
     */
    public static void filterEventPages(final Map<String, Object> body, final Set<String> allowed) {
        if (body == null) {
            return;
        }
        int removed = 0;
        for (final Object events : valuesIgnoreCase(body, EVENTS_KEY)) {
            if (!(events instanceof List)) {
                continue;
            }
            final Iterator<?> iterator = ((List<?>) events).iterator();
            while (iterator.hasNext()) {
                if (!filterEvent(iterator.next(), allowed)) {
                    iterator.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            final int removedCount = removed;
            Logger.debug(UrlParamFilter.class, () -> "Removed " + removedCount
                    + " analytics event(s) whose data or page is not an object");
        }
    }

    /**
     * Removes user info ({@code user:password@} before the host) and {@code ;jsessionid=...} path
     * segments from the page URL fields of every event, in place, keeping query parameters and
     * fragments as they are. Used for sites that turned query-parameter filtering off: these two
     * parts are removed either way. Keys are matched regardless of case; {@code page.url},
     * {@code page.doc_path} and {@code element.href} strings are handled, and nothing else is
     * changed or removed.
     *
     * @param body the parsed ingest body; may be {@code null}
     */
    public static void removeUserInfoAndSessionIds(final Map<String, Object> body) {
        if (body == null) {
            return;
        }
        for (final Object events : valuesIgnoreCase(body, EVENTS_KEY)) {
            if (!(events instanceof List)) {
                continue;
            }
            for (final Object event : (List<?>) events) {
                if (!(event instanceof Map)) {
                    continue;
                }
                for (final Object data : valuesIgnoreCase((Map<?, ?>) event, DATA_KEY)) {
                    if (!(data instanceof Map)) {
                        continue;
                    }
                    for (final Object page : valuesIgnoreCase((Map<?, ?>) data, PAGE_KEY)) {
                        if (page instanceof Map) {
                            @SuppressWarnings("unchecked")
                            final Map<Object, Object> pageMap = (Map<Object, Object>) page;
                            removePageUserInfoAndSessionIds(pageMap);
                        }
                    }
                    for (final Object element : valuesIgnoreCase((Map<?, ?>) data, ELEMENT_KEY)) {
                        if (element instanceof Map) {
                            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) element).entrySet()) {
                                if (entry.getKey() instanceof String && entry.getValue() instanceof String
                                        && HREF_KEY.equalsIgnoreCase((String) entry.getKey())) {
                                    @SuppressWarnings("unchecked")
                                    final Map.Entry<Object, Object> hrefEntry = (Map.Entry<Object, Object>) entry;
                                    hrefEntry.setValue(removeUserInfoAndSessionId((String) entry.getValue()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Applies {@link #removeUserInfoAndSessionIds} to the {@code url} and {@code doc_path} strings of one page. */
    private static void removePageUserInfoAndSessionIds(final Map<Object, Object> page) {
        for (final Map.Entry<Object, Object> entry : page.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                continue;
            }
            final String key = ((String) entry.getKey()).toLowerCase(Locale.ROOT);
            final String value = (String) entry.getValue();
            if (URL_KEY.equals(key)) {
                entry.setValue(removeUserInfoAndSessionId(value));
            } else if (DOC_PATH_KEY.equals(key)) {
                entry.setValue(filterPath(value));
            }
        }
    }

    /**
     * Removes user info and {@code ;jsessionid} segments from the part of a URL before its query
     * or fragment, keeping the query and fragment as they are.
     */
    private static String removeUserInfoAndSessionId(final String url) {
        final int end = firstIndexOf(url, '?', '#');
        return filterPath(removeUserInfo(url.substring(0, end))) + url.substring(end);
    }

    /** Returns the index of the first of the two characters in {@code value}, or its length. */
    private static int firstIndexOf(final String value, final char first, final char second) {
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character == first || character == second) {
                return index;
            }
        }
        return value.length();
    }

    /**
     * Filters the page objects of one event.
     *
     * @return {@code false} when the event has a {@code data} or a {@code page} that is present
     *         but not an object, and must be removed from the batch; {@code true} otherwise
     */
    private static boolean filterEvent(final Object event, final Set<String> allowed) {
        if (!(event instanceof Map)) {
            return true;
        }
        for (final Object data : valuesIgnoreCase((Map<?, ?>) event, DATA_KEY)) {
            if (data == null) {
                continue;
            }
            if (!(data instanceof Map)) {
                return false;
            }
            for (final Object page : valuesIgnoreCase((Map<?, ?>) data, PAGE_KEY)) {
                if (page == null) {
                    continue;
                }
                if (!(page instanceof Map)) {
                    return false;
                }
                @SuppressWarnings("unchecked")
                final Map<Object, Object> pageMap = (Map<Object, Object>) page;
                filterPage(pageMap, allowed);
            }
            for (final Object element : valuesIgnoreCase((Map<?, ?>) data, ELEMENT_KEY)) {
                if (element instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<Object, Object> elementMap = (Map<Object, Object>) element;
                    filterElementHref(elementMap, allowed);
                }
            }
        }
        return true;
    }

    /**
     * Filters the {@code href} of a clicked element like a page {@code url}, matching the key
     * regardless of case. An array or object becomes {@code ""}; other values are left as they are.
     */
    private static void filterElementHref(final Map<Object, Object> element, final Set<String> allowed) {
        for (final Map.Entry<Object, Object> entry : element.entrySet()) {
            if (!(entry.getKey() instanceof String) || !HREF_KEY.equalsIgnoreCase((String) entry.getKey())) {
                continue;
            }
            final Object value = entry.getValue();
            if (value instanceof String) {
                entry.setValue(filterUrl((String) value, allowed));
            } else if (value instanceof Map || value instanceof List) {
                entry.setValue("");
            }
        }
    }

    /**
     * Filters the {@code url}, {@code doc_search}, {@code doc_hash} and {@code doc_path} fields of a page object,
     * matching their names regardless of case. Strings are filtered; arrays and objects are
     * replaced with {@code ""}; other values are left as they are.
     */
    private static void filterPage(final Map<Object, Object> page, final Set<String> allowed) {
        for (final Map.Entry<Object, Object> entry : page.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                continue;
            }
            final UnaryOperator<String> filter;
            final String key = ((String) entry.getKey()).toLowerCase(Locale.ROOT);
            if (URL_KEY.equals(key)) {
                filter = value -> filterUrl(value, allowed);
            } else if (DOC_SEARCH_KEY.equals(key)) {
                filter = value -> filterQuery(value, allowed);
            } else if (DOC_HASH_KEY.equals(key)) {
                filter = value -> filterFragment(value, allowed);
            } else if (DOC_PATH_KEY.equals(key)) {
                filter = UrlParamFilter::filterPath;
            } else {
                continue;
            }
            final Object value = entry.getValue();
            if (value instanceof String) {
                entry.setValue(filter.apply((String) value));
            } else if (value instanceof Map || value instanceof List) {
                entry.setValue("");
            }
        }
    }

    /** Returns the values of every key in {@code map} that equals {@code name} ignoring case. */
    private static List<Object> valuesIgnoreCase(final Map<?, ?> map, final String name) {
        final List<Object> values = new ArrayList<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String && name.equalsIgnoreCase((String) entry.getKey())) {
                values.add(entry.getValue());
            }
        }
        return values;
    }

    /**
     * Keeps only the allowed query parameters of a URL. The URL can be absolute, relative or not
     * parseable at all: it is split on the first {@code #} and then on the first {@code ?}, without
     * any further parsing. The fragment follows {@link #filterFragment}. When no query parameter is
     * left, the {@code ?} is removed too. A {@code user:password@} part before the host is removed,
     * and the path goes through {@link #filterPath}.
     *
     * @param url     the URL; may be {@code null}
     * @param allowed lower-case parameter names to keep
     * @return the filtered URL, or {@code null} when {@code url} is {@code null}
     */
    public static String filterUrl(final String url, final Set<String> allowed) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        final int hashIndex = url.indexOf('#');
        final String beforeHash = hashIndex >= 0 ? url.substring(0, hashIndex) : url;
        final String fragment = hashIndex >= 0 ? url.substring(hashIndex) : "";

        final int queryIndex = beforeHash.indexOf('?');
        final String path = filterPath(removeUserInfo(
                queryIndex >= 0 ? beforeHash.substring(0, queryIndex) : beforeHash));
        final String query = queryIndex >= 0 ? filterPairs(beforeHash.substring(queryIndex + 1), allowed) : "";

        return path + (query.isEmpty() ? "" : "?" + query) + filterFragment(fragment, allowed);
    }

    /**
     * Removes {@code ;jsessionid=...} segments, matched regardless of case, from a URL path such as
     * {@code location.pathname}. The rest of the path is kept as it is.
     *
     * @param path the path; may be {@code null}
     * @return the path without session id segments, or {@code null} when {@code path} is {@code null}
     */
    public static String filterPath(final String path) {
        return path == null ? null : SESSION_ID_SEGMENT.matcher(path).replaceAll("");
    }

    /**
     * Removes the {@code user:password@} part that can precede the host of a URL such as
     * {@code https://u:p@host/path} or {@code //u@host}. An {@code @} after the host is left alone.
     */
    private static String removeUserInfo(final String beforeQuery) {
        final int slashes = beforeQuery.indexOf("//");
        if (slashes < 0 || (slashes > 0 && beforeQuery.charAt(slashes - 1) != ':')) {
            return beforeQuery;
        }
        final int hostStart = slashes + 2;
        final int slash = beforeQuery.indexOf('/', hostStart);
        final int hostEnd = slash >= 0 ? slash : beforeQuery.length();
        final int at = beforeQuery.lastIndexOf('@', hostEnd - 1);
        if (at < hostStart) {
            return beforeQuery;
        }
        return beforeQuery.substring(0, hostStart) + beforeQuery.substring(at + 1);
    }

    /**
     * Keeps only the allowed parameters of a query string such as {@code location.search}. A
     * leading {@code ?} is kept when at least one parameter remains.
     *
     * @param search  the query string, with or without a leading {@code ?}; may be {@code null}
     * @param allowed lower-case parameter names to keep
     * @return the filtered query string, {@code ""} when nothing is left, or {@code null} when
     *         {@code search} is {@code null}
     */
    public static String filterQuery(final String search, final Set<String> allowed) {
        if (search == null) {
            return null;
        }
        final boolean hasPrefix = search.startsWith("?");
        final String kept = filterPairs(hasPrefix ? search.substring(1) : search, allowed);
        return kept.isEmpty() ? "" : (hasPrefix ? "?" : "") + kept;
    }

    /**
     * Filters a URL fragment such as {@code location.hash}. The fragment is split at its first
     * {@code ?} into a route and a query:
     * <ul>
     *     <li>when the route contains {@code =} and does not start with {@code /} or {@code !/},
     *     the whole fragment is a list of parameters and is filtered like a query string;</li>
     *     <li>otherwise the route is kept (plain anchors as they are; routes starting with
     *     {@code /} or {@code !/} up to their first {@code &} or {@code ;} and without the first
     *     segment containing {@code =} and what follows it) and the query after it keeps only the
     *     allowed parameters.</li>
     * </ul>
     * A leading {@code #} is kept when something remains.
     *
     * @param hash    the fragment, with or without a leading {@code #}; may be {@code null}
     * @param allowed lower-case parameter names to keep
     * @return the filtered fragment, {@code ""} when nothing is left, or {@code null} when
     *         {@code hash} is {@code null}
     */
    public static String filterFragment(final String hash, final Set<String> allowed) {
        if (hash == null) {
            return null;
        }
        final boolean hasPrefix = hash.startsWith("#");
        final String body = hasPrefix ? hash.substring(1) : hash;

        final int queryIndex = body.indexOf('?');
        final String route = queryIndex >= 0 ? body.substring(0, queryIndex) : body;

        final String kept;
        if (route.contains("=") && !route.startsWith("/") && !route.startsWith("!/")) {
            kept = filterPairs(body, allowed);
        } else {
            final String keptRoute = route.startsWith("/") || route.startsWith("!/") ? trimRoute(route) : route;
            final String query = queryIndex >= 0 ? filterPairs(body.substring(queryIndex + 1), allowed) : "";
            kept = keptRoute + (query.isEmpty() ? "" : "?" + query);
        }
        return kept.isEmpty() ? "" : (hasPrefix ? "#" : "") + kept;
    }

    /**
     * Shortens a hash route that starts with {@code /} or {@code !/}: it ends at its first
     * {@code &} or {@code ;}, and the first {@code /}-segment containing {@code =} is removed with
     * everything after it. {@code /catalog/color=red} becomes {@code /catalog}, {@code /a=1&b=2}
     * becomes {@code /}, and {@code !/a=1} becomes {@code !/}.
     */
    private static String trimRoute(final String route) {
        final int pairEnd = firstIndexOf(route, '&', ';');
        String trimmed = route.substring(0, pairEnd);
        final int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex >= 0) {
            trimmed = trimmed.substring(0, trimmed.lastIndexOf('/', equalsIndex));
            if (trimmed.isEmpty() || "!".equals(trimmed)) {
                trimmed += "/";
            }
        }
        return trimmed;
    }

    /**
     * Keeps the {@code name=value} pairs of a parameter list whose name is allowed. Pairs are
     * separated by {@code &}, {@code ;} or a further {@code ?}, so text appended after an allowed
     * value with one of those characters is treated as separate params and filtered by name. A
     * literal {@code ?} or {@code ;} inside a value therefore ends that value. Names are compared as
     * sent (not URL-decoded) and regardless of case; kept pairs are copied unchanged, in their
     * original order, and joined with {@code &}. Empty pairs are dropped.
     *
     * <p>The list is scanned once in place: only kept pairs are copied, and a name longer than the
     * longest allowed name is skipped without being copied, so the extra memory used is about the
     * size of the result, whatever the length of the input.
     */
    private static String filterPairs(final String pairs, final Set<String> allowed) {
        final int maxNameLength = allowed.stream().mapToInt(String::length).max().orElse(0);
        final StringBuilder kept = new StringBuilder();
        final int length = pairs.length();
        int index = 0;
        while (index < length) {
            final int start = index;
            int nameEnd = -1;
            while (index < length && !isPairSeparator(pairs.charAt(index))) {
                if (nameEnd < 0 && pairs.charAt(index) == '=') {
                    nameEnd = index;
                }
                index++;
            }
            final int end = index;
            index++;
            if (end == start) {
                continue;
            }
            if (nameEnd < 0) {
                nameEnd = end;
            }
            if (nameEnd - start <= maxNameLength
                    && allowed.contains(pairs.substring(start, nameEnd).toLowerCase(Locale.ROOT))) {
                if (kept.length() > 0) {
                    kept.append('&');
                }
                kept.append(pairs, start, end);
            }
        }
        return kept.toString();
    }

    /** Tells whether a character separates query params: {@code &}, {@code ;} or a further {@code ?}. */
    private static boolean isPairSeparator(final char character) {
        return character == '&' || character == ';' || character == '?';
    }

    /** Adds the trimmed, lower-cased, non-blank names of a comma-separated list to {@code target}. */
    private static void addNames(final Set<String> target, final String commaSeparatedNames) {
        if (!UtilMethods.isSet(commaSeparatedNames)) {
            return;
        }
        for (final String name : commaSeparatedNames.split(",")) {
            final String normalized = name.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }
}
