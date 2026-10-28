package com.dotcms.util;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Either;
import io.vavr.control.Try;


public class RedirectUtil {

    final static String redirectTemplate =
                        new StringWriter()
                        .append("<html>")
                        .append("<head>")
                        .append("<meta http-equiv=\"refresh\" content=\"0;URL='REDIRECT_ME'\"/>")
                        .append("<style>p {font-family: Arial;font-size: 16px;color: #666;margin: 50px;text-align:center;opacity: 1;animation: fadeIn ease 5s;animation-iteration-count: 0;-webkit-animation: fadeIn ease 5s;}@keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-moz-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-webkit-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}@-o-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}@-ms-keyframes fadeIn {0% {opacity:0;}100% {opacity:1;}}</style>")
                        .append("</head>")
                        .append("<body><p>If your browser does not refresh, click <a href=\"REDIRECT_ME\">Here</a>.</p></body>")
                        .append("</html>")
                        .toString();

    /**
     * Writes a small HTML page that sends the browser to {@code redirectUrl}, through a meta refresh and a
     * fallback link. The URL is HTML-attribute-encoded before it is written, so a value containing quotes or
     * angle brackets cannot close the attribute and inject markup. Browsers decode the entities back, so a
     * valid URL such as {@code /a?x=1&y=2} still redirects to exactly that URL.
     *
     * @param response    the response to write the page to
     * @param redirectUrl where to send the browser
     */
    public static void sendRedirectHTML(final HttpServletResponse response, final String redirectUrl) {

        final String finalTemplate = UtilMethods.replace(redirectTemplate, "REDIRECT_ME",
                encodeHtmlAttribute(redirectUrl));

        response.setContentType("text/html");
        Try.run(() -> {
            response.getWriter().write(finalTemplate);
            response.getWriter().flush();
        }).onFailure(e->Logger.warn(RedirectUtil.class,"Unable to redirect to :" + redirectUrl+ " cause:"+e.getMessage()));
    }

    /**
     * Checks whether a post-login destination is safe to redirect to, and returns it as a normalized
     * relative path when it is.
     *
     * A destination is accepted when it is either a path starting with a single {@code /}, or an
     * {@code http}/{@code https} URL without user-info whose host is {@code requestHost} (case-insensitive;
     * the port is ignored). Before deciding, the path is percent-decoded once and its {@code .} and
     * {@code ..} segments are resolved. A path that climbs above the root, that contains control or
     * whitespace characters, a backslash, quotes, angle brackets or a backtick (raw, or once-decoded except
     * for an encoded space), that
     * has malformed percent-encoding, or that would start with {@code //} is rejected.
     *
     * The result is always relative ({@code /path?query#fragment}), so the browser stays on the current
     * scheme and host, and any path check made on the result sees exactly the path the browser will load.
     * Path segments other than dot segments keep their original encoding.
     *
     * Callers must not log a rejected candidate: it can be attacker-controlled.
     *
     * @param candidate   the destination to check, e.g. a SAML RelayState
     * @param requestHost the host of the current request ({@code request.getServerName()})
     * @return the normalized relative destination, or empty when the candidate is unsafe. Never throws.
     */
    public static Optional<String> toSameHostRedirect(final String candidate, final String requestHost) {

        return toSameHostRedirect(candidate, singleHost(requestHost));
    }

    /**
     * Same check as {@link #toSameHostRedirect(String, String)}, except that an absolute URL is accepted when
     * its host is any of {@code allowedHosts} (case-insensitive). The SAML paths pass the request host plus the
     * hostname and aliases of the site being logged in to, so a destination on another alias of the same site
     * is followed while other sites are not.
     *
     * @param candidate    the destination to check, e.g. a SAML RelayState
     * @param allowedHosts the hosts an absolute URL may point to; null or empty rejects every absolute URL
     * @return the normalized relative destination, or empty when the candidate is unsafe. Never throws.
     */
    public static Optional<String> toSameHostRedirect(final String candidate, final Collection<String> allowedHosts) {

        return toAllowedRedirect(candidate, allowedHosts, Collections.emptyList());
    }

    /**
     * Same check as {@link #toSameHostRedirect(String, Collection)}, plus a list of explicitly allowed external
     * hosts (the SAML app's {@code auth.relaystate.allowed.hosts}). A destination on one of {@code siteHosts}
     * becomes a normalized relative path, as before. A destination on one of {@code externalHosts} keeps its
     * absolute form ({@code scheme://host[:port]/path?query#fragment}, host lower-cased, path normalized), so the
     * browser actually goes to that host. Every other rule still applies to both.
     *
     * @param candidate     the destination to check, e.g. a SAML RelayState
     * @param siteHosts     the request host plus the hostname and aliases of the current site
     * @param externalHosts other hosts an admin explicitly allowed; null or empty allows none
     * @return the normalized destination, or empty when the candidate is unsafe. Never throws.
     */
    public static Optional<String> toAllowedRedirect(final String candidate, final Collection<String> siteHosts,
                                                     final Collection<String> externalHosts) {

        return evaluate(candidate, siteHosts, externalHosts).toJavaOptional();
    }

    /**
     * Explains why {@link #toSameHostRedirect(String, String)} rejects a destination, so a log line can say
     * why without logging the destination itself.
     *
     * @param candidate   the destination to check
     * @param requestHost the host of the current request ({@code request.getServerName()})
     * @return the reason the candidate is rejected, or empty when it is accepted. Never throws.
     */
    public static Optional<RedirectRejection> rejectionReason(final String candidate, final String requestHost) {

        return rejectionReason(candidate, singleHost(requestHost));
    }

    /**
     * Explains why {@link #toSameHostRedirect(String, Collection)} rejects a destination, so a log line can say
     * why without logging the destination itself.
     *
     * @param candidate    the destination to check
     * @param allowedHosts the hosts an absolute URL may point to
     * @return the reason the candidate is rejected, or empty when it is accepted. Never throws.
     */
    public static Optional<RedirectRejection> rejectionReason(final String candidate,
                                                              final Collection<String> allowedHosts) {

        return rejectionReason(candidate, allowedHosts, Collections.emptyList());
    }

    /**
     * Explains why {@link #toAllowedRedirect(String, Collection, Collection)} rejects a destination.
     *
     * @param candidate     the destination to check
     * @param siteHosts     the request host plus the hostname and aliases of the current site
     * @param externalHosts other hosts an admin explicitly allowed
     * @return the reason the candidate is rejected, or empty when it is accepted. Never throws.
     */
    public static Optional<RedirectRejection> rejectionReason(final String candidate,
                                                              final Collection<String> siteHosts,
                                                              final Collection<String> externalHosts) {

        final Either<RedirectRejection, String> result = evaluate(candidate, siteHosts, externalHosts);
        return result.isLeft() ? Optional.of(result.getLeft()) : Optional.empty();
    }

    /**
     * True when the value still contains an encoded slash, backslash or dot ({@code %2F}, {@code %5C},
     * {@code %2E}, any case): after one decode, that means the original path was double-encoded.
     */
    private static boolean hasEncodedSeparator(final String value) {
        final String lowerCase = value.toLowerCase(Locale.ROOT);
        return lowerCase.contains("%2f") || lowerCase.contains("%5c") || lowerCase.contains("%2e");
    }

    /**
     * Wraps a single request host as the allowed-hosts collection; a null host allows no absolute URL.
     */
    private static Collection<String> singleHost(final String requestHost) {
        return null == requestHost ? Collections.emptyList() : Collections.singletonList(requestHost);
    }

    /**
     * Why a post-login destination was rejected by {@link #toSameHostRedirect(String, String)}.
     */
    public enum RedirectRejection {
        /** Null, empty or only whitespace. */
        BLANK,
        /** An absolute URL for another host, or with user-info. */
        OTHER_HOST,
        /** Neither a path starting with {@code /} nor an http(s) URL. */
        NOT_A_PATH,
        /** Starts, or would start after decoding or resolving dot segments, with {@code //}. */
        PROTOCOL_RELATIVE,
        /**
         * Contains a control or whitespace character, backslash, quote, angle bracket or backtick, an encoded
         * slash ({@code %2F}) in its path, or a double-encoded slash, backslash or dot in its path.
         */
        FORBIDDEN_CHARACTER,
        /** Malformed percent-encoding or URL syntax. */
        MALFORMED_ENCODING,
        /** Its {@code ..} segments climb above the root. */
        ABOVE_ROOT
    }

    /**
     * Single entry point for every redirect check in this class. Runs {@link #sameHostRedirect} and turns any
     * parsing exception into a {@link RedirectRejection#MALFORMED_ENCODING} rejection, so callers never see
     * an exception.
     *
     * @param candidate     the redirect value to check, may be null
     * @param siteHosts     the hosts of the current site; a URL on one of them is reduced to its relative path
     * @param externalHosts other hosts that are explicitly allowed; a URL on one of them is kept absolute. May
     *                      be null or empty
     * @return on the right, the safe redirect target; on the left, why the value was rejected
     */
    private static Either<RedirectRejection, String> evaluate(final String candidate,
                                                              final Collection<String> siteHosts,
                                                              final Collection<String> externalHosts) {

        return Try.of(() -> sameHostRedirect(candidate, siteHosts, externalHosts))
                .getOrElse(Either.left(RedirectRejection.MALFORMED_ENCODING));
    }

    /**
     * Decides whether a redirect value is safe to send the browser to. The value must be a single-slash path
     * ({@code /path?query#fragment}) or an {@code http}/{@code https} URL without user info on a site host or
     * an allowed external host. Its path is decoded once and must be free of forbidden characters, encoded
     * slashes and double-encoded separators. Its dot segments must not climb above the root. Only the path is
     * normalized; the query and fragment are passed through unchanged.
     *
     * @param candidate     the redirect value to check, may be null
     * @param siteHosts     the hosts of the current site, compared ignoring case
     * @param externalHosts other allowed hosts, compared ignoring case; may be null or empty
     * @return on the right, the normalized relative path (or the absolute URL for an allowed external host);
     * on the left, the first rule the value broke
     * @throws URISyntaxException when an absolute URL cannot be parsed
     */
    private static Either<RedirectRejection, String> sameHostRedirect(final String candidate,
                                                                      final Collection<String> siteHosts,
                                                                      final Collection<String> externalHosts)
            throws URISyntaxException {

        if (!UtilMethods.isSet(candidate) || candidate.isBlank()) {
            return Either.left(RedirectRejection.BLANK);
        }
        if (hasForbiddenCharacter(candidate)) {
            return Either.left(RedirectRejection.FORBIDDEN_CHARACTER);
        }

        final String relative;
        String externalOrigin = null;
        final String lowerCase = candidate.toLowerCase(Locale.ROOT);
        if (lowerCase.startsWith("http://") || lowerCase.startsWith("https://")) {

            final URI uri = new URI(candidate);
            if (null != uri.getRawUserInfo() || null == uri.getHost()) {
                return Either.left(RedirectRejection.OTHER_HOST);
            }
            if (!matchesAny(uri.getHost(), siteHosts)) {
                if (!matchesAny(uri.getHost(), externalHosts)) {
                    return Either.left(RedirectRejection.OTHER_HOST);
                }
                // an explicitly allowed external host: keep the absolute form so the browser goes there
                externalOrigin = uri.getScheme().toLowerCase(Locale.ROOT) + "://"
                        + uri.getHost().toLowerCase(Locale.ROOT) + (uri.getPort() > -1 ? ":" + uri.getPort() : "");
            }
            relative = (UtilMethods.isSet(uri.getRawPath()) ? uri.getRawPath() : "/")
                    + (null != uri.getRawQuery()    ? "?" + uri.getRawQuery()    : "")
                    + (null != uri.getRawFragment() ? "#" + uri.getRawFragment() : "");
        } else {
            relative = candidate;
        }

        if (!relative.startsWith("/")) {
            return Either.left(RedirectRejection.NOT_A_PATH);
        }
        if (relative.startsWith("//")) {
            return Either.left(RedirectRejection.PROTOCOL_RELATIVE);
        }

        int pathEnd = relative.length();
        for (int i = 0; i < relative.length(); i++) {
            final char c = relative.charAt(i);
            if (c == '?' || c == '#') {
                pathEnd = i;
                break;
            }
        }
        final String rawPath = relative.substring(0, pathEnd);
        final String suffix  = relative.substring(pathEnd);

        final String decodedPath = percentDecode(rawPath);
        if (null == decodedPath) {
            return Either.left(RedirectRejection.MALFORMED_ENCODING);
        }
        if (decodedPath.startsWith("//")) {
            return Either.left(RedirectRejection.PROTOCOL_RELATIVE);
        }
        if (hasForbiddenDecodedCharacter(decodedPath)) {
            return Either.left(RedirectRejection.FORBIDDEN_CHARACTER);
        }
        // An encoded slash can hide a ".." segment from the dot-segment resolution below
        // ("/members/..%2f..%2fdotAdmin/" decodes to "/members/../../dotAdmin/"), and servlet containers refuse
        // such paths anyway, so it is never accepted in the path. It stays allowed in the query string.
        if (rawPath.toLowerCase(Locale.ROOT).contains("%2f")) {
            return Either.left(RedirectRejection.FORBIDDEN_CHARACTER);
        }
        // The same trick double-encoded ("%252f", "%252e", "%255c") survives the single decode above, and a
        // second decode by a proxy or the container would turn it into a traversal, so it is never accepted.
        if (hasEncodedSeparator(decodedPath)) {
            return Either.left(RedirectRejection.FORBIDDEN_CHARACTER);
        }

        final String normalizedPath = removeDotSegments(rawPath);
        if (null == normalizedPath) {
            return Either.left(RedirectRejection.ABOVE_ROOT);
        }
        if (normalizedPath.startsWith("//")) {
            return Either.left(RedirectRejection.PROTOCOL_RELATIVE);
        }

        return Either.right((null != externalOrigin ? externalOrigin : "") + normalizedPath + suffix);
    }

    /**
     * True when the host equals, ignoring case, one of the given hosts; false for a null collection.
     */
    private static boolean matchesAny(final String host, final Collection<String> hosts) {
        return null != hosts && hosts.stream().anyMatch(host::equalsIgnoreCase);
    }

    /**
     * Resolves {@code .} and {@code ..} segments of an absolute path, comparing each segment's decoded value
     * so {@code %2e%2e} counts as {@code ..}, while keeping every other segment's original encoding.
     * Returns null when a {@code ..} would climb above the root.
     */
    private static String removeDotSegments(final String rawPath) {

        final String[] segments = rawPath.substring(1).split("/", -1);
        final Deque<String> kept = new ArrayDeque<>();
        boolean trailingSlash = false;

        for (int i = 0; i < segments.length; i++) {

            final String decoded = percentDecode(segments[i]);
            final boolean last   = i == segments.length - 1;
            if (".".equals(decoded)) {
                trailingSlash = last;
            } else if ("..".equals(decoded)) {
                if (kept.isEmpty()) {
                    return null;
                }
                kept.removeLast();
                trailingSlash = last;
            } else {
                kept.addLast(segments[i]);
                trailingSlash = false;
            }
        }

        final String path = "/" + String.join("/", kept);
        return trailingSlash && !kept.isEmpty() ? path + "/" : path;
    }

    /**
     * Percent-decodes a URL path once, as UTF-8. Unlike form decoding, {@code +} is left as is. Strict: a
     * {@code %} not followed by two hex digits makes the whole value invalid instead of being kept as is.
     *
     * @param value the raw path, not null
     * @return the decoded path, or null when an escape is malformed
     */
    public static String percentDecode(final String value) {

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {

            final char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    return null;
                }
                final int high = Character.digit(value.charAt(i + 1), 16);
                final int low  = Character.digit(value.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return null;
                }
                bytes.write((high << 4) + low);
                i += 2;
            } else {
                final byte[] encoded = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                bytes.write(encoded, 0, encoded.length);
            }
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * True for characters that never appear unencoded in a valid URL and that browsers either strip
     * (control and whitespace characters) or that could break out of an HTML attribute.
     */
    private static boolean hasForbiddenCharacter(final String value) {

        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c <= 0x20 || c == 0x7f || c == '\\' || c == '"' || c == '\'' || c == '<' || c == '>' || c == '`') {
                return true;
            }
        }
        return false;
    }

    /**
     * The same screen applied to the once-decoded path, except that a plain space is allowed: an encoded
     * space ({@code %20}) is common in asset and page URLs and is harmless, because browsers never decode it
     * before deciding where to go. Encoded control characters and backslashes stay rejected.
     */
    private static boolean hasForbiddenDecodedCharacter(final String value) {

        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c != ' ' && hasForbiddenCharacter(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Escapes {@code &}, {@code "}, {@code '}, {@code <} and {@code >} so the value can be safely placed inside
     * an HTML attribute.
     */
    private static String encodeHtmlAttribute(final String value) {

        if (null == value) {
            return null;
        }
        final StringBuilder encoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            encoded.append(switch (c) {
                case '&'  -> "&amp;";
                case '"'  -> "&quot;";
                case '\'' -> "&#39;";
                case '<'  -> "&lt;";
                case '>'  -> "&gt;";
                default   -> String.valueOf(c);
            });
        }
        return encoded.toString();
    }
}
