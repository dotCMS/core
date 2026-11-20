package com.dotcms.util;

import com.dotcms.config.SystemTableConfigSource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts sensitive configuration values in the JVM information endpoint and audit log.
 * The public configuration endpoint uses only the deployment-specific name pattern.
 */
public final class ObfuscationUtil {

    public static final String DEFAULT_OBFUSCATE_PATTERN = "passw|pass|passwd|secret|key|token";

    /**
     * Words that hide a value only as a whole word of the name: bounded by the start or end of the name, an
     * underscore, a dot or a dash ({@code SMTP_AUTH_USER}, {@code dotcms.cluster.salt}, {@code SENTRY_DSN}), so that
     * {@code AUTHOR_NAME} keeps its value. {@code pwd} also needs a separator before it ({@code TRUSTSTORE_PWD}), so
     * that the shell's working directory ({@code PWD}) stays visible.
     */
    private static final String WHOLE_WORD_OBFUSCATE_PATTERN =
            "(^|[_.-])(salt|credentials?|auth|authorization|jdbc|dsn|bearer)($|[_.-])|[_.-]pwd($|[_.-])";

    private static final String MASK = "*****";

    /** Always-on name rules, independent of the deployment-specific pattern. */
    public static final Pattern BASE_PATTERN = Pattern.compile(
            DEFAULT_OBFUSCATE_PATTERN + "|" + WHOLE_WORD_OBFUSCATE_PATTERN, Pattern.CASE_INSENSITIVE);

    /**
     * The same words as {@link #WHOLE_WORD_OBFUSCATE_PATTERN} written as a word of a camel-case name
     * ({@code jdbcUrl}, {@code spring.datasource.jdbcUrl}, {@code dbPwd}, {@code smtpAuth}); case-sensitive, so that
     * {@code authorName} keeps its value.
     */
    private static final Pattern CAMEL_CASE_OBFUSCATE_PATTERN = Pattern.compile(
            "(^|[a-z0-9_.-])(Salt|Pwd|Credentials?|Auth|Authorization|Jdbc|Dsn|Bearer)($|[A-Z0-9_.-])"
                    + "|(^|[_.-])(salt|pwd|credentials?|auth|authorization|jdbc|dsn|bearer)[A-Z]");

    /** Deployment-specific name rules, defaulting to the original substring pattern. */
    public static final Pattern CUSTOM_PATTERN = Pattern.compile(
            Config.getStringProperty("OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES",
                    DEFAULT_OBFUSCATE_PATTERN),
            Pattern.CASE_INSENSITIVE);

    /**
     * The user information of a URL inside a value, {@code scheme://user:password@host}, up to the last {@code @}
     * before the path, so that a password holding an {@code @} is hidden whole.
     */
    private static final Pattern URL_USER_INFO = Pattern.compile("://[^/\\s]*@");

    /**
     * The name of a {@code name=value} pair inside a value, such as a JVM option or a URL query parameter. The name
     * starts after a character that cannot be part of it and is read possessively, so a long run of name characters
     * is scanned once.
     */
    private static final Pattern PAIR_NAME = Pattern.compile("(?<![A-Za-z0-9_.-])([A-Za-z0-9_.-]++)=");

    /**
     * The value of a pair: an authorization scheme and its token ({@code Basic abc}, {@code Bearer abc}), a double- or
     * single-quoted string, or the text up to a space, {@code &}, {@code ;} or {@code ,}.
     */
    private static final Pattern PAIR_VALUE = Pattern.compile(
            "(?i:basic|bearer|digest|token)\\s+[^\\s&;,]+|\"[^\"]*\"|'[^']*'|[^\\s&;,]+");

    /** The {@code -} or {@code --} and the {@code D} that start a JVM option such as {@code -Dname=value}. */
    private static final Pattern OPTION_PREFIX = Pattern.compile("^-+(D(?=[A-Za-z]))?");

    private ObfuscationUtil() {
    }

    /**
     * @return true when the name matches the built-in rules, a custom pattern, or the system table blacklist
     */
    public static boolean shouldObfuscate(final String key) {
        return isMaskedName(key, SystemTableConfigSource.refusedKeys());
    }

    /**
     * @return true when the name matches the configured custom pattern
     */
    public static boolean matchesCustomPattern(final String key) {
        return CUSTOM_PATTERN.matcher(key).find();
    }

    /**
     * Returns the value to show for a configuration, environment or system property entry. The whole value is
     * replaced by {@code *****} when the name is a masked name ({@link #isMaskedName(String, Predicate)}). Otherwise
     * the value is shown with the value of any {@code name=value} pair whose name is a masked name, and the user
     * information of any URL, replaced by {@code *****}. The scan inside values covers {@code name=value} pairs (also
     * nested in another pair's value, with a quoted value, or with an authorization scheme such as
     * {@code Basic <token>}) and URL user information; other forms, such as JSON or YAML text, a quote that is not
     * closed or is preceded by a backslash, or a password holding a {@code /}, {@code ,} or {@code ;} that is not
     * percent-encoded, are not recognised.
     *
     * @param key         the name of the entry
     * @param valueObject the value of the entry, converted to text when present
     * @return an empty string for a missing value, otherwise the value to show
     */
    public static String obfuscateIfNeeded(final String key, final Object valueObject) {
        final String value = valueObject == null ? null : String.valueOf(valueObject);
        if (UtilMethods.isEmpty(value)) {
            return "";
        }
        final Predicate<String> refusedKeys = SystemTableConfigSource.refusedKeys();
        return isMaskedName(key, refusedKeys) ? MASK : obfuscateInsideValue(value, refusedKeys);
    }

    /**
     * Tells whether the value of a name is hidden: the name matches the fixed pattern, its camel-case form, the
     * configurable {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES} pattern, or is a key the system table refuses,
     * read as is, in the form of an environment variable (upper case, {@code .} and {@code -} as {@code _}, without
     * the {@code DOT_} prefix that environment variables use for configuration properties) or in the form of a
     * property (lower case, {@code _} as {@code .}), so that {@code DOT_MY_PRIVATE_VALUE} matches a refused
     * {@code my.private.value}.
     */
    private static boolean isMaskedName(final String name, final Predicate<String> refusedKeys) {
        if (BASE_PATTERN.matcher(name).find()
                || CAMEL_CASE_OBFUSCATE_PATTERN.matcher(name).find()
                || matchesCustomPattern(name)) {
            return true;
        }
        final String upperCase = name.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        final String environmentForm = upperCase.startsWith("DOT_") ? upperCase.substring(4) : upperCase;
        final String propertyForm = environmentForm.toLowerCase(Locale.ROOT).replace('_', '.');
        return refusedKeys.test(name) || refusedKeys.test(environmentForm) || refusedKeys.test(propertyForm);
    }

    /**
     * Tells whether the value of a {@code name=value} pair found inside a value is hidden. The dashes and the
     * {@code D} of a JVM option ({@code -Dname}, {@code --name}) are not part of the name; the dash counts as the
     * separator before its first word.
     */
    private static boolean isMaskedPairName(final String pairName, final Predicate<String> refusedKeys) {
        final String name = OPTION_PREFIX.matcher(pairName).replaceFirst("");
        return isMaskedName(name, refusedKeys) || (!name.equals(pairName) && isMaskedName("-" + name, refusedKeys));
    }

    /**
     * Replaces the values of {@code name=value} pairs whose name is a masked name, then the user information of
     * URLs, and keeps the rest of the value. After a pair whose name is not masked the scan goes on inside its value,
     * so a pair nested in it ({@code -Ddb.url=jdbc:postgresql://h/db?password=x}) is found too.
     */
    private static String obfuscateInsideValue(final String value, final Predicate<String> refusedKeys) {
        final Matcher name = PAIR_NAME.matcher(value);
        final Matcher pairValue = PAIR_VALUE.matcher(value);
        final StringBuilder result = new StringBuilder();
        int copied = 0;
        int from = 0;
        while (from < value.length() && name.find(from)) {
            from = name.end();
            if (isMaskedPairName(name.group(1), refusedKeys)
                    && pairValue.region(from, value.length()).lookingAt()) {
                result.append(value, copied, from).append(MASK);
                copied = pairValue.end();
                from = copied;
            }
        }
        result.append(value, copied, value.length());
        return URL_USER_INFO.matcher(result).replaceAll("://" + MASK + "@");
    }
}
