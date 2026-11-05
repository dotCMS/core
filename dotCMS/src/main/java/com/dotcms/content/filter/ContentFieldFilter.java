package com.dotcms.content.filter;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Masks or removes configurable contentlet properties from the content returned to callers that are
 * neither CMS administrators nor back-end users, such as anonymous visitors of a headless site or
 * logged-in front-end users.
 * <p>
 * <strong>Scope:</strong> responses are filtered only by the legacy Content API ({@code /api/content}).
 * Other endpoints that return contentlets, such as {@code /api/v1/content}, the Page API and GraphQL, still
 * return these properties unchanged; they are meant to reuse this class. The {@code catchall} exclusion
 * described below applies at index time, so it covers every search endpoint.
 * <p>
 * Two configuration properties drive it. Both are comma-separated lists of property names, matched
 * without regard to case, and both are empty by default, in which case no response is changed:
 * <ul>
 *     <li>{@value #FIELDS_TO_MASK_PROPERTY} (environment variable
 *     {@code DOT_CONTENT_API_FIELDS_TO_MASK}): the values of these properties are partially masked,
 *     so {@code john@example.com} is returned as {@code j*****n.e*****m}. A value given as a nested
 *     object or a list, such as a user, keeps its shape and has every value inside it masked. Masking
 *     does not protect values from a small or predictable set; see {@link #mask(String)}.</li>
 *     <li>{@value #FIELDS_TO_HIDE_PROPERTY} (environment variable
 *     {@code DOT_CONTENT_API_FIELDS_TO_HIDE}): these properties are removed from the response. A
 *     property listed in both is removed.</li>
 * </ul>
 * For example, {@code DOT_CONTENT_API_FIELDS_TO_MASK=modUser,owner,publishUser} and
 * {@code DOT_CONTENT_API_FIELDS_TO_HIDE=modUserName,ownerUserName,publishUserName}.
 * <p>
 * The properties in {@link #NEVER_FILTERED_FIELDS} ({@code identifier}, {@code inode},
 * {@code contentType} and {@code languageId}) are never masked nor removed, because REST clients
 * need them to address and route content. Listing them only logs a warning.
 * <p>
 * Masking a value in the response does not stop a caller from searching on it, so a Lucene query or
 * sort clause from a filtered caller that names a configured property is refused; see
 * {@link #allowsSearch(String, String)}. A value can also be searched without naming its field, because
 * every indexed value is copied into the {@code catchall} field, which is also the field searched by a
 * query term without a field name. Wildcard queries on it would let a caller rebuild a value one
 * character at a time, so the configured properties are left out of {@code catchall} when content is
 * indexed; see {@link #catchallExclusion()}. Two consequences follow:
 * <ul>
 *     <li>It only applies to content indexed after the properties are set, so content must be
 *     reindexed after setting or changing them.</li>
 *     <li>It applies to every user: CMS administrators and back-end users can still search these
 *     properties by naming them, e.g. {@code +modUser:dotcms.org.1}, but not without a field name.</li>
 * </ul>
 * Other index fields may hold the same values, such as the user IDs in the workflow fields
 * {@code wfCreatedBy} and {@code wfAssign}; list them too to keep them out of searches.
 */
public final class ContentFieldFilter {

    /** Comma-separated contentlet properties whose values are partially masked. Empty by default. */
    public static final String FIELDS_TO_MASK_PROPERTY = "CONTENT_API_FIELDS_TO_MASK";

    /** Comma-separated contentlet properties removed from the response. Empty by default. */
    public static final String FIELDS_TO_HIDE_PROPERTY = "CONTENT_API_FIELDS_TO_HIDE";

    /**
     * {@code Language.properties} key of the message returned to a caller whose query or sort names a masked or
     * hidden property.
     */
    public static final String SEARCH_REJECTED_MESSAGE_KEY = "content.api.error.search.restricted.field";

    /** The message used when {@link #SEARCH_REJECTED_MESSAGE_KEY} cannot be resolved. */
    static final String SEARCH_REJECTED_DEFAULT_MESSAGE =
            "The query or sort references a content property that is not available to this user";

    /**
     * Properties that are never masked nor removed, whatever the configuration says, because clients
     * need them to address and route content. Lower-cased.
     */
    public static final Set<String> NEVER_FILTERED_FIELDS = Set.of(
            Contentlet.IDENTIFIER_KEY.toLowerCase(Locale.ROOT),
            Contentlet.INODE_KEY.toLowerCase(Locale.ROOT),
            Contentlet.CONTENT_TYPE_KEY.toLowerCase(Locale.ROOT),
            Contentlet.LANGUAGEID_KEY.toLowerCase(Locale.ROOT));

    static final String MASK = "*****";

    /** Shorter values are masked completely: showing their first and last characters would give most of them away. */
    private static final int MIN_PARTIAL_MASK_LENGTH = 4;
    /** The match-all clause {@code *:*}, which names no field and must stay allowed. */
    private static final Pattern MATCH_ALL = Pattern.compile("(?<![\\w.*?\\\\])\\*\\s*:\\s*\\*(?![\\w.*?])");
    /** A field name in a Lucene query: the token right before a colon. */
    private static final Pattern QUERY_FIELD = Pattern.compile("([\\w.*?\\\\]+)\\s*:");
    private static final Pattern SORT_SEPARATOR = Pattern.compile("[\\s,]+");
    private static final Set<String> SORT_DIRECTIONS = Set.of("asc", "desc");
    /** Prefix of index fields that mirror a contentlet property, e.g. {@code publishUser} is indexed as {@code sysPublishUser}. */
    private static final String SEARCH_FIELD_PREFIX = "sys";
    /** Suffixes of index fields derived from a contentlet property, e.g. {@code moduser_dotraw}. */
    private static final List<String> SEARCH_FIELD_SUFFIXES = List.of("_dotraw", "_text", "_sha256");

    private static final ContentFieldFilter NO_OP = new ContentFieldFilter(Set.of(), Set.of(), null);
    /** The ignored names last warned about, per configuration property, so the warning is not repeated on every request. */
    private static final Map<String, Set<String>> LAST_IGNORED_WARNING = new ConcurrentHashMap<>();

    private final Set<String> fieldsToMask;
    private final Set<String> fieldsToHide;
    private final User user;

    private ContentFieldFilter(final Set<String> fieldsToMask, final Set<String> fieldsToHide, final User user) {
        this.fieldsToMask = fieldsToMask;
        this.fieldsToHide = fieldsToHide;
        this.user = user;
    }

    /**
     * Returns the filter that applies to the given user. CMS administrators and back-end users get a
     * filter that changes nothing, and so does everyone when neither property is set. Anonymous
     * callers, front-end users and a {@code null} user get the configured filter.
     *
     * @param user the {@link User} the content is being returned to
     * @return the filter to apply to that user's content
     */
    public static ContentFieldFilter forUser(final User user) {
        final Set<String> toMask = readFields(FIELDS_TO_MASK_PROPERTY);
        final Set<String> toHide = readFields(FIELDS_TO_HIDE_PROPERTY);
        if ((toMask.isEmpty() && toHide.isEmpty()) || isTrusted(user)) {
            return NO_OP;
        }
        return new ContentFieldFilter(toMask, toHide, user);
    }

    /**
     * Tells whether this filter changes anything.
     *
     * @return {@code true} when there is at least one property to mask or hide
     */
    public boolean isActive() {
        return !fieldsToMask.isEmpty() || !fieldsToHide.isEmpty();
    }

    /**
     * Removes the hidden properties from the given content map and masks the masked ones, in place.
     * Works on any map whose keys are property names, including a {@link JSONObject}.
     *
     * @param contentMap the contentlet map that is about to be returned; may be {@code null}
     * @param <M> the map type, returned as is so the call can be chained
     * @return the same map, filtered
     */
    @SuppressWarnings("unchecked")
    public <M extends Map<?, ?>> M apply(final M contentMap) {
        if (!isActive() || null == contentMap || contentMap.isEmpty()) {
            return contentMap;
        }
        final Map<Object, Object> map = (Map<Object, Object>) contentMap;
        for (final Object key : new ArrayList<>(map.keySet())) {
            final String name = String.valueOf(key).toLowerCase(Locale.ROOT);
            if (fieldsToHide.contains(name)) {
                map.remove(key);
            } else if (fieldsToMask.contains(name)) {
                map.put(key, maskValue(map.get(key)));
            }
        }
        return contentMap;
    }

    /**
     * Tells whether the caller may run the given Lucene query and sort. Searching on a masked or hidden
     * property would let a caller recover its value one guess at a time, so a query or sort that names
     * one is refused, and the attempt is written to the security log.
     *
     * @param luceneQuery the Lucene query sent by the caller; may be {@code null}
     * @param sortBy the sort clause sent by the caller; may be {@code null}
     * @return {@code false} when the query or sort names a masked or hidden property
     */
    public boolean allowsSearch(final String luceneQuery, final String sortBy) {
        if (!isActive()) {
            return true;
        }
        final Set<String> restricted = new LinkedHashSet<>(fieldsToMask);
        restricted.addAll(fieldsToHide);
        if (queryReferences(luceneQuery, restricted) || sortReferences(sortBy, restricted)) {
            SecurityLogger.logInfo(ContentFieldFilter.class,
                    () -> "Search on a masked or hidden content property refused for user: "
                            + (null == user ? null : user.getUserId()));
            return false;
        }
        return true;
    }

    /**
     * Returns a test telling which index fields must be left out of the {@code catchall} field: the ones
     * that stand for a masked or hidden property, including their {@code Type.field}, {@code sys} prefix,
     * {@code _dotraw}, {@code _text} and {@code _sha256} variants. Used when content is indexed, whoever
     * will search it.
     *
     * @return a test on lower-cased index field names; always {@code false} when nothing is configured
     */
    public static Predicate<String> catchallExclusion() {
        final Set<String> restricted = new LinkedHashSet<>(readFields(FIELDS_TO_MASK_PROPERTY));
        restricted.addAll(readFields(FIELDS_TO_HIDE_PROPERTY));
        return catchallExclusion(restricted);
    }

    /**
     * Returns a test telling whether an index field stands for one of the given properties.
     *
     * @param fields lower-cased property names
     * @return a test on index field names; always {@code false} when there are no properties
     */
    static Predicate<String> catchallExclusion(final Set<String> fields) {
        return fields.isEmpty() ? indexField -> false : indexField -> refersTo(indexField, fields);
    }

    /**
     * Returns the message to send to a caller whose search was refused by {@link #allowsSearch(String, String)},
     * in the language of that caller's profile. Anonymous callers get the default language. When the message
     * cannot be resolved, the English text is returned.
     *
     * @return the message explaining why the search was refused
     */
    public String searchRejectedMessage() {
        return localizedOrDefault(Try.of(() -> LanguageUtil.get(user, SEARCH_REJECTED_MESSAGE_KEY)).getOrNull());
    }

    /**
     * Picks the localized search-refused message, or the English text when the lookup failed or only gave the
     * key back, which is what {@link LanguageUtil} does for a key it does not know.
     *
     * @param localized the result of the message lookup; may be {@code null}
     * @return the message to send
     */
    static String localizedOrDefault(final String localized) {
        return UtilMethods.isSet(localized) && !SEARCH_REJECTED_MESSAGE_KEY.equals(localized)
                ? localized
                : SEARCH_REJECTED_DEFAULT_MESSAGE;
    }

    /**
     * Tells whether a user gets content unfiltered: CMS administrators and back-end users do, since
     * the back-office relies on these properties and content is still limited by their permissions.
     *
     * @param user the {@link User} to check; may be {@code null}
     * @return {@code true} when the user's content is not filtered
     */
    static boolean isTrusted(final User user) {
        return null != user && !user.isAnonymousUser() && (user.isAdmin() || user.isBackendUser());
    }

    /**
     * Masks a property value. A text value is masked with {@link #mask(String)}; a map or list keeps
     * its shape and type and has every value inside it masked; any other value is masked as text.
     *
     * @param value the value to mask; may be {@code null}
     * @return the masked value, or {@code null} when the value was {@code null}
     */
    static Object maskValue(final Object value) {
        if (null == value || value == JSONObject.NULL) {
            return value;
        }
        if (value instanceof Map) {
            final Map<Object, Object> masked = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((key, entry) -> masked.put(key, maskValue(entry)));
            return value instanceof JSONObject ? new JSONObject(masked) : masked;
        }
        if (value instanceof Collection) {
            final List<Object> masked = ((Collection<?>) value).stream()
                    .map(ContentFieldFilter::maskValue).collect(Collectors.toList());
            return value instanceof JSONArray ? new JSONArray(masked) : masked;
        }
        return mask(value.toString());
    }

    /**
     * Masks a text value, keeping the first and last characters of each part: {@code john@example.com}
     * becomes {@code j*****n.e*****m} and {@code dotcms.org.1} becomes {@code d*****1}. A part shorter
     * than four characters is masked completely. Masking an already masked value never reveals more of it.
     * <p>
     * This hides a value from casual viewing, not from enumeration: anyone can mask a guessed value and
     * compare, so a value from a small or predictable set, such as {@code dotcms.org.1} or
     * {@code dotcms.org.2}, can still be told apart. Hide such properties instead of masking them.
     *
     * @param value the text to mask; may be {@code null}
     * @return the masked text
     */
    static String mask(final String value) {
        if (null == value || value.isEmpty()) {
            return value;
        }
        final String[] parts = value.split("@", 2);
        final String masked = maskPart(parts[0]);
        return parts.length > 1 ? masked + "." + maskPart(parts[1]) : masked;
    }

    /**
     * Tells whether a Lucene query names any of the given properties as a field, including through the
     * index fields derived from them ({@code Type.field}, {@code sys} prefix, {@code _dotraw},
     * {@code _text} and {@code _sha256} suffixes) and through wildcards in the field name. The
     * match-all clause {@code *:*} is allowed.
     *
     * @param luceneQuery the query to inspect; may be {@code null}
     * @param fields lower-cased property names
     * @return {@code true} when the query names one of them
     */
    static boolean queryReferences(final String luceneQuery, final Set<String> fields) {
        if (!UtilMethods.isSet(luceneQuery) || fields.isEmpty()) {
            return false;
        }
        final Matcher matcher = QUERY_FIELD.matcher(MATCH_ALL.matcher(luceneQuery).replaceAll(" "));
        while (matcher.find()) {
            if (refersTo(matcher.group(1), fields)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether a sort clause such as {@code "modUser desc, title"} sorts by any of the given
     * properties.
     *
     * @param sortBy the sort clause to inspect; may be {@code null}
     * @param fields lower-cased property names
     * @return {@code true} when the sort uses one of them
     */
    static boolean sortReferences(final String sortBy, final Set<String> fields) {
        if (!UtilMethods.isSet(sortBy) || fields.isEmpty()) {
            return false;
        }
        return Arrays.stream(SORT_SEPARATOR.split(sortBy.trim()))
                .filter(token -> !token.isEmpty())
                .filter(token -> !SORT_DIRECTIONS.contains(token.toLowerCase(Locale.ROOT)))
                .anyMatch(token -> refersTo(token, fields));
    }

    /**
     * Tells whether one search or index field name stands for one of the given properties.
     *
     * @param searchField a field name taken from a query, a sort or an index document
     * @param fields lower-cased property names
     * @return {@code true} when it does
     */
    private static boolean refersTo(final String searchField, final Set<String> fields) {
        String name = searchField.replace("\\", "").toLowerCase(Locale.ROOT);
        name = name.substring(name.lastIndexOf('.') + 1);
        for (final String suffix : SEARCH_FIELD_SUFFIXES) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
                break;
            }
        }
        if (name.isEmpty()) {
            return false;
        }
        if (name.indexOf('*') >= 0 || name.indexOf('?') >= 0) {
            final Pattern wildcard = Pattern.compile(Pattern.quote(name)
                    .replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q"));
            return fields.stream().anyMatch(field -> wildcard.matcher(field).matches()
                    || wildcard.matcher(SEARCH_FIELD_PREFIX + field).matches());
        }
        return fields.contains(name) || (name.startsWith(SEARCH_FIELD_PREFIX)
                && fields.contains(name.substring(SEARCH_FIELD_PREFIX.length())));
    }

    /**
     * Masks one part of a value, keeping its first and last characters.
     *
     * @param part the text to mask
     * @return the masked text
     */
    private static String maskPart(final String part) {
        return part.length() < MIN_PARTIAL_MASK_LENGTH
                ? MASK
                : part.charAt(0) + MASK + part.charAt(part.length() - 1);
    }

    /**
     * Reads a comma-separated list of property names from the configuration, lower-cased, without
     * blanks, duplicates or the {@link #NEVER_FILTERED_FIELDS}.
     *
     * @param property the configuration property to read
     * @return the property names; empty when it is not set
     */
    private static Set<String> readFields(final String property) {
        final String[] values = Config.getStringArrayProperty(property, new String[0]);
        if (null == values || values.length == 0) {
            return Set.of();
        }
        final Set<String> fields = Arrays.stream(values)
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<String> ignored = fields.stream().filter(NEVER_FILTERED_FIELDS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!ignored.isEmpty()) {
            fields.removeAll(ignored);
            warnIgnored(property, ignored);
        }
        return fields.isEmpty() ? Set.of() : Collections.unmodifiableSet(fields);
    }

    /**
     * Warns that some configured properties are ignored, once per distinct configuration.
     *
     * @param property the configuration property they were listed in
     * @param ignored the ignored property names
     */
    private static void warnIgnored(final String property, final Set<String> ignored) {
        if (!ignored.equals(LAST_IGNORED_WARNING.put(property, ignored))) {
            Logger.warn(ContentFieldFilter.class, String.format(
                    "%s lists %s, which are never masked nor hidden because clients need them; ignoring them",
                    property, ignored));
        }
    }

}
