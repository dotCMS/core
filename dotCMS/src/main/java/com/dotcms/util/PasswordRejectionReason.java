package com.dotcms.util;

/**
 * Tells why a password was rejected by the default password requirements, so the rejection can be
 * logged without writing the password, its length or anything derived from it.
 *
 * <p>The reasons describe the default pattern shipped in {@code portal.properties}: 15 to 64
 * printable ASCII characters with an uppercase letter, a lowercase letter, a digit and a special
 * character. When the pattern has been overridden, the rules known here would not describe the
 * customer's own, so the reason is {@link #NOT_MET}.</p>
 */
public enum PasswordRejectionReason {

    /** Fewer characters than the minimum, or no password at all. */
    TOO_SHORT,
    /** More characters than the maximum. */
    TOO_LONG,
    /** A character outside printable ASCII, such as a line feed, a tab or an accented letter. */
    DISALLOWED_CHARACTER,
    /** One of the uppercase, lowercase, digit or special character requirements is not met. */
    MISSING_CLASS,
    /** The configured pattern is not the default, or no default rule explains the rejection. */
    NOT_MET;

    /** The default {@code passwords.regexptoolkit.pattern}, as loaded from {@code portal.properties}. */
    public static final String DEFAULT_PATTERN =
            "/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9 ])(?!.*\\n)[ -~]{15,64}$/";

    private static final int MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 64;

    /**
     * Tells which rule of the default requirements a rejected password breaks.
     *
     * @param password the rejected password, may be {@code null}
     * @param configuredPattern the pattern in force when the password was rejected
     * @return the reason; {@link #NOT_MET} when the pattern is not the default or the password
     * breaks none of the default rules
     */
    public static PasswordRejectionReason classify(final String password, final String configuredPattern) {
        if (!DEFAULT_PATTERN.equals(configuredPattern)) {
            return NOT_MET;
        }
        if (null == password || password.isEmpty()) {
            return TOO_SHORT;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            final char c = password.charAt(i);
            if (c < ' ' || c > '~') {
                return DISALLOWED_CHARACTER;
            }
            if (c >= 'A' && c <= 'Z') {
                hasUpper = true;
            } else if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else if (c != ' ') {
                hasSpecial = true;
            }
        }
        if (password.length() < MIN_LENGTH) {
            return TOO_SHORT;
        }
        if (password.length() > MAX_LENGTH) {
            return TOO_LONG;
        }
        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            return MISSING_CLASS;
        }
        return NOT_MET;
    }

    /**
     * Removes control characters (carriage return, line feed and the rest below the space, plus
     * the delete character) from a value before it is written to a log, so the value cannot add
     * lines or fake entries.
     *
     * @param value the value to clean, may be {@code null}
     * @return the value without control characters; an empty string for {@code null}
     */
    public static String sanitizeForLog(final String value) {
        if (null == value) {
            return "";
        }
        final StringBuilder clean = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c >= ' ' && c != 0x7F) {
                clean.append(c);
            }
        }
        return clean.toString();
    }
}
