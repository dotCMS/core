package com.dotcms.company;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Rules for the stored company logo values (the login screen logo and the navigation bar logo):
 * whether a value is a dotAsset path, and the path the logo is served from.
 *
 * <p>A value is a dotAsset path when it starts with {@code /dA/}, followed by an identifier made
 * of letters, digits and dashes, and then any number of segments. The value is percent-decoded
 * once (a broken percent sequence is read literally). After decoding it must not hold a control
 * character, a UTF-16 surrogate that is not part of a pair, or any of {@code \ " < > ?}, and no
 * segment may be {@code .} or {@code ..}, alone or
 * before a {@code ;}; the same holds for the value before decoding, where {@code %2e} counts as
 * a dot. Every other character a file name may keep, such as {@code ' # ; % & +}, is allowed.</p>
 *
 * <p>The logo is served, and shown to clients, from {@link #forwardPath(String)}: the decoded
 * segments, each one percent-encoded again, so whoever reads the path gets exactly the segments
 * that were checked.</p>
 */
public final class CompanyLogoPath {

    private static final String PREFIX = "/dA/";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9-]+");
    private static final Pattern ENCODED_SLASH = Pattern.compile("/|%2[fF]");
    private static final Pattern ENCODED_DOT = Pattern.compile("%2[eE]");
    private static final Pattern PARAMETERS = Pattern.compile(";|%3[bB]");
    private static final String NOT_ALLOWED = "\\\"<>?";
    private static final String KEPT_WHEN_ENCODING = "-._~!$&'()*+,=:@";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private CompanyLogoPath() {
    }

    /**
     * Tells whether the given value is a dotAsset path that may be used as a company logo.
     *
     * @param path the stored or submitted logo value, may be {@code null}
     * @return {@code true} if the value is a dotAsset path as described in the class
     *         documentation; {@code false} for {@code null}, empty or any other value
     */
    public static boolean isValid(final String path) {
        if (path == null || !path.startsWith(PREFIX)) {
            return false;
        }
        final String decoded = decodeOnce(path);
        for (int i = 0; i < decoded.length(); i++) {
            final char c = decoded.charAt(i);
            if (c < 0x20 || c == 0x7f || NOT_ALLOWED.indexOf(c) >= 0) {
                return false;
            }
        }
        if (decoded.codePoints().anyMatch(cp -> cp >= Character.MIN_SURROGATE
                && cp <= Character.MAX_SURROGATE)) {
            return false;
        }
        final String[] segments = decoded.substring(PREFIX.length()).split("/", -1);
        if (!IDENTIFIER.matcher(segments[0]).matches()) {
            return false;
        }
        for (final String segment : segments) {
            if (isDotSegment(segment)) {
                return false;
            }
        }
        // The value as stored is also what a browser reads, and a browser resolves percent-encoded
        // dots; check the undecoded segments too, whatever the rest of the value holds.
        for (final String segment : ENCODED_SLASH.split(path.substring(PREFIX.length()), -1)) {
            if (isDotSegment(ENCODED_DOT.matcher(segment).replaceAll("."))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a segment is {@code .} or {@code ..}, ignoring any {@code ;} part (literal or
     * percent-encoded) after it.
     */
    private static boolean isDotSegment(final String segment) {
        final String name = PARAMETERS.split(segment, 2)[0];
        return ".".equals(name) || "..".equals(name);
    }

    /**
     * Returns the path to serve a stored logo from: its decoded segments, each one
     * percent-encoded again.
     *
     * @param path the stored logo value, may be {@code null}
     * @return the path under {@code /dA/}, or empty when the value is not valid
     *         (see {@link #isValid(String)})
     */
    public static Optional<String> forwardPath(final String path) {
        if (!isValid(path)) {
            return Optional.empty();
        }
        final String decoded = decodeOnce(path);
        final StringJoiner joiner = new StringJoiner("/", PREFIX, "");
        for (final String segment : decoded.substring(PREFIX.length()).split("/", -1)) {
            joiner.add(encodeSegment(segment));
        }
        return Optional.of(joiner.toString());
    }

    /**
     * Decodes percent sequences once, as UTF-8. A value with a broken sequence is returned as
     * it is, so a literal percent sign in a file name is kept.
     */
    private static String decodeOnce(final String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    return value;
                }
                final int high = Character.digit(value.charAt(i + 1), 16);
                final int low = Character.digit(value.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return value;
                }
                bytes.write((high << 4) + low);
                i += 2;
            } else {
                final int end = Character.isHighSurrogate(c) && i + 1 < value.length() ? i + 2 : i + 1;
                final byte[] encoded = value.substring(i, end).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
                i = end - 1;
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * Percent-encodes one path segment, keeping letters, digits and {@code -._~!$&'()*+,=:@}.
     */
    private static String encodeSegment(final String segment) {
        final StringBuilder encoded = new StringBuilder(segment.length());
        for (final byte b : segment.getBytes(StandardCharsets.UTF_8)) {
            final int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || KEPT_WHEN_ENCODING.indexOf(c) >= 0) {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(HEX[c >> 4]).append(HEX[c & 0x0f]);
            }
        }
        return encoded.toString();
    }
}
