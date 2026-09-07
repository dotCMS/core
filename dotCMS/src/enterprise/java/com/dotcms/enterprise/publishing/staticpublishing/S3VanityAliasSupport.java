package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.http.HttpStatus;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Normalizes and filters vanity aliases supported by S3 publishing.
 */
public class S3VanityAliasSupport {

    private static final String UNSUPPORTED_CHARS = "*?[](){}|^$\\+";

    /**
     * Converts current Vanity URLs into persistable mappings.
     *
     * @param context operational mapping context
     * @param vanityUrls current Vanity URLs found for the content
     * @return normalized and deduplicated mappings
     */
    public List<S3VanityAlias> toAliasMappings(final S3VanityAliasContext context,
                                               final List<CachedVanityUrl> vanityUrls) {
        final Map<String, S3VanityAlias> aliasesByPath = new LinkedHashMap<>();

        if (vanityUrls == null) {
            return new ArrayList<>();
        }

        for (final CachedVanityUrl vanityUrl : vanityUrls) {
            normalizeMaterializedVanityPath(vanityUrl).ifPresent(vanityPath ->
                    aliasesByPath.putIfAbsent(vanityPath,
                            new S3VanityAlias(context.lookup.endpointId, context.lookup.hostId,
                                    context.lookup.languageId, context.lookup.canonicalPath, vanityPath,
                                    vanityUrl.vanityUrlId, context.bucketName, context.bucketRegion,
                                    context.bucketPrefix)));
        }

        return new ArrayList<>(aliasesByPath.values());
    }

    /**
     * Normalizes a Vanity URL into the concrete S3 key that will be published.
     *
     * @param vanityUrl source Vanity URL
     * @return normalized path when supported
     */
    private Optional<String> normalizeMaterializedVanityPath(final CachedVanityUrl vanityUrl) {
        return vanityUrl == null ? Optional.empty() : materializeVanityPath(vanityUrl.url, DotAsset.PAGE);
    }

    /**
     * Converts a Vanity URL path into the S3 key used for the clone.
     * The resolved source type only decides what dotCMS renders; the Vanity URL
     * path remains the literal S3 key to materialize.
     *
     * @param vanityPath source vanity path
     * @param targetType resolved dotCMS target type
     * @return materialized S3 key when supported
     */
    public Optional<String> materializeVanityPath(final String vanityPath, final DotAsset targetType) {
        return normalizeVanityPath(vanityPath);
    }

    /**
     * Normalizes a vanity path into a publishable S3 key.
     *
     * @param vanityPath source vanity path
     * @return normalized path when supported
     */
    public Optional<String> normalizeVanityPath(final String vanityPath) {
        if (!UtilMethods.isSet(vanityPath)) {
            return Optional.empty();
        }

        final String normalized = vanityPath.trim().replace('\\', '/');
        if (!normalized.startsWith(StringPool.FORWARD_SLASH)
                || containsUnsupportedChars(normalized)
                || containsTraversalOrUnsafePath(normalized)) {
            return Optional.empty();
        }

        return Optional.of(normalized.replaceAll("/{2,}", "/"));
    }

    /**
     * Checks whether the Vanity URL contentlet can be materialized as a static clone.
     *
     * @param vanityContentlet Vanity URL contentlet
     * @return true when the contentlet is a supported 200-forward Vanity URL
     */
    public boolean isSupportedVanityUrl(final Contentlet vanityContentlet) {
        return vanityContentlet != null
                && vanityContentlet.isVanityUrl()
                && isForwardAction(vanityContentlet)
                && normalizeVanityPath(getUri(vanityContentlet)).isPresent()
                && normalizeCanonicalPath(getForwardTo(vanityContentlet)).isPresent();
    }

    /**
     * Normalizes a Vanity URL forward target into a renderable canonical path.
     *
     * @param forwardTo Vanity URL forward target
     * @return normalized path when the target is internal and supported
     */
    public Optional<String> normalizeCanonicalPath(final String forwardTo) {
        if (!UtilMethods.isSet(forwardTo)) {
            return Optional.empty();
        }

        final String path = forwardTo.trim().replace('\\', '/');
        if (!path.startsWith(StringPool.FORWARD_SLASH) || path.startsWith("//") || path.contains("://")
                || containsTraversalOrUnsafePath(path)) {
            return Optional.empty();
        }

        return Optional.of(path.replaceAll("/{2,}", "/"));
    }

    /**
     * Checks whether the Vanity URL is a 200-forward.
     *
     * @param vanityContentlet Vanity URL contentlet
     * @return true when the action is 200
     */
    public boolean isForwardAction(final Contentlet vanityContentlet) {
        return vanityContentlet != null
                && vanityContentlet.getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR) == HttpStatus.SC_OK;
    }

    /**
     * Reads the Vanity URL URI field.
     *
     * @param vanityContentlet Vanity URL contentlet
     * @return URI value
     */
    public String getUri(final Contentlet vanityContentlet) {
        return vanityContentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR);
    }

    /**
     * Reads the Vanity URL forward target field.
     *
     * @param vanityContentlet Vanity URL contentlet
     * @return forward target value
     */
    public String getForwardTo(final Contentlet vanityContentlet) {
        return vanityContentlet.getStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR);
    }

    /**
     * Checks whether the path contains traversal sequences or unsafe characters.
     *
     * URL-decodes the path once to detect encoded traversal attempts
     * (%2e%2e, %2f, %00, etc.), then rejects:
     * <ul>
     *   <li>control characters (code points below 0x20 or 0x7F)</li>
     *   <li>{@code .} and {@code ..} path segments</li>
     *   <li>residual {@code %} after decoding (double-encoded sequences)</li>
     * </ul>
     *
     * @param path path to evaluate
     * @return true when the path contains traversal or unsafe content
     */
    private boolean containsTraversalOrUnsafePath(final String path) {
        for (int i = 0; i < path.length(); i++) {
            final char c = path.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        final String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        for (int i = 0; i < decoded.length(); i++) {
            final char c = decoded.charAt(i);
            if (c < 0x20 || c == 0x7F || c == '%') {
                return true;
            }
        }
        for (final String segment : decoded.split("/", -1)) {
            if ("..".equals(segment) || ".".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the path contains unsupported regular expression
     * metacharacters.
     *
     * @param vanityPath path to evaluate
     * @return true when the path contains regex metacharacters
     */
    private boolean containsUnsupportedChars(final String vanityPath) {
        for (final char character : vanityPath.toCharArray()) {
            if (UNSUPPORTED_CHARS.indexOf(character) >= 0) {
                return true;
            }
        }
        return false;
    }

}
