package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reusable matcher that, given a mime type, resolves the best-matching {@link ContentType} among
 * the content types of a given {@link BaseContentType}. The match is done by comparing the mime
 * type against the "accept" ({@link BinaryField#ALLOWED_FILE_TYPES}) field variable of the
 * content type's binary field.
 *
 * <p>This is the algorithm originally embedded in {@code DotAssetAPIImpl} for the
 * {@link BaseContentType#DOTASSET} base type, extracted here so it can be reused for other binary
 * base types (e.g. {@link BaseContentType#FILEASSET}) without duplicating the logic.</p>
 *
 * <p>Matches are evaluated in priority order: exact mime type wins over a partial wildcard
 * (e.g. {@code application/*}) which wins over a total wildcard ({@code *}{@code /}{@code *} or no
 * "accept" restriction); within each tier, content types on the current site win over content
 * types on the system host. A binary field with no "accept" field variable is treated as the total
 * wildcard, so a content type that accepts everything resolves as the catch-all when nothing
 * stricter matches. See {@link MimeTypeBuckets} for the explicit precedence model.</p>
 *
 * @author dotCMS
 */
public class BaseTypeMimeTypeMatcher {

    /**
     * Resolves the best-matching content type of the given base type for the supplied mime type.
     *
     * @param mimeType       the mime type to match
     * @param currentHost    the current site; content types on this site or on the system host are considered
     * @param user           the user used to look up the content types
     * @param baseContentType the base type whose content types are searched
     * @param binaryFieldVar  the variable name of the binary field that holds the "accept" field variable
     * @return Optional of the matching content type, empty if none matches
     */
    public Optional<ContentType> match(final String mimeType, final Host currentHost, final User user,
                                       final BaseContentType baseContentType, final String binaryFieldVar)
            throws DotDataException, DotSecurityException {

        final List<ContentType> contentTypes = APILocator.getContentTypeAPI(user).findByType(baseContentType);

        if (UtilMethods.isSet(contentTypes)) {

            final MimeTypeBuckets buckets = new MimeTypeBuckets();
            contentTypes.stream()
                    .filter(contentType -> isSystemHostOrCurrentHost(contentType, currentHost))
                    .map(contentType -> mapToContentTypeMimeType(contentType, binaryFieldVar))
                    .forEach(buckets::add);

            return findContentType(mimeType, buckets);
        }

        return Optional.empty();
    }

    /**
     * Returns the first content type whose accepted mime type matches {@code mimeType}, scanning the
     * candidate buckets in resolution-precedence order (see {@link MimeTypeBuckets}).
     *
     * @param mimeType the mime type to resolve
     * @param buckets  the candidate content types grouped by specificity and host
     * @return Optional of the matching content type, empty if none matches
     */
    private Optional<ContentType> findContentType(final String mimeType, final MimeTypeBuckets buckets) {

        for (final Map<String, ContentType> bucket : buckets.inPrecedenceOrder()) {
            for (final Map.Entry<String, ContentType> entry : bucket.entrySet()) {

                if (MimeTypeUtils.match(entry.getKey(), mimeType)) {

                    return Optional.ofNullable(entry.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Reads the accepted mime types from a content type's binary field "accept" field variable.
     * A binary field with no (or empty) {@link BinaryField#ALLOWED_FILE_TYPES} variable is treated
     * as the total wildcard ({@code *}{@code /}{@code *}), i.e. it accepts everything.
     *
     * @param contentType    the content type to inspect
     * @param binaryFieldVar the variable name of the binary field that holds the "accept" variable
     * @return the content type paired with its accepted mime types and whether it lives on the system host
     */
    private ContentTypeMimeType mapToContentTypeMimeType(final ContentType contentType, final String binaryFieldVar) {

        final String systemHostId          = APILocator.systemHost().getIdentifier();
        final String contentTypeHostId     = contentType.host();
        final boolean isSystemHost         = null == contentTypeHostId || contentTypeHostId.equals(systemHostId);
        final Map<String, Field>  fieldMap = contentType.fieldMap();
        String [] mimeTypeFieldVariables   = new String [] { DotAssetAPI.ALL_MIME_TYPE };
        final List<FieldVariable> fieldVariables = fieldMap.containsKey(binaryFieldVar)?
                fieldMap.get(binaryFieldVar).fieldVariables(): Collections.emptyList();

        if (UtilMethods.isSet(fieldVariables)) {

            final Optional<FieldVariable> fieldVariableOpt = fieldVariables.stream().filter(fieldVariable ->
                    BinaryField.ALLOWED_FILE_TYPES.equalsIgnoreCase(fieldVariable.key())).findFirst();

            if (fieldVariableOpt.isPresent() && UtilMethods.isSet(fieldVariableOpt.get().value())) {

                mimeTypeFieldVariables = fieldVariableOpt.get().value().split(StringPool.COMMA);
            }
        }

        return new ContentTypeMimeType(mimeTypeFieldVariables, contentType, isSystemHost);
    }

    /**
     * Keeps content types that can be resolved for the current request: those on the current site,
     * on the system host, or with no host set.
     *
     * @param contentType the content type to evaluate
     * @param currentHost the current site
     * @return {@code true} if the content type is eligible for matching
     */
    private boolean isSystemHostOrCurrentHost (final ContentType contentType, final Host currentHost) {

        final String contentTypeHostId = contentType.host();
        final String systemHostId      = APILocator.systemHost().getIdentifier();
        return null == contentTypeHostId ||
                contentTypeHostId.equals(currentHost.getIdentifier()) || contentTypeHostId.equals(systemHostId);
    }

    /**
     * Groups the candidate content types into buckets by mime specificity and host, in the order a
     * match must be resolved:
     * <ol>
     *     <li>exact accept (e.g. {@code image/png}) on the current site</li>
     *     <li>exact accept on the system host</li>
     *     <li>partial wildcard (e.g. {@code image/*}) on the current site</li>
     *     <li>partial wildcard on the system host</li>
     *     <li>total wildcard ({@code *}{@code /}{@code *} or no accept restriction) on the current site</li>
     *     <li>total wildcard on the system host</li>
     * </ol>
     * The more specific the accept, and the closer to the current site, the higher the precedence.
     */
    private static final class MimeTypeBuckets {

        private final Map<String, ContentType> exactCurrentSite    = new HashMap<>();
        private final Map<String, ContentType> exactSystemHost     = new HashMap<>();
        private final Map<String, ContentType> partialCurrentSite  = new HashMap<>();
        private final Map<String, ContentType> partialSystemHost   = new HashMap<>();
        private final Map<String, ContentType> wildcardCurrentSite = new HashMap<>();
        private final Map<String, ContentType> wildcardSystemHost  = new HashMap<>();

        /**
         * Files the given content type under the bucket matching the specificity of each of its
         * accepted mime types and whether it lives on the system host.
         */
        private void add(final ContentTypeMimeType candidate) {

            for (final String accept : candidate.mimeTypeFieldVariables) {
                this.bucketFor(accept, candidate.systemHost).put(accept, candidate.contentType);
            }
        }

        /**
         * Resolves the destination bucket for an accept value and host (see class javadoc for the order).
         */
        private Map<String, ContentType> bucketFor(final String accept, final boolean systemHost) {

            if (DotAssetAPI.ALL_MIME_TYPE.equals(accept)) {
                return systemHost ? this.wildcardSystemHost : this.wildcardCurrentSite;
            }

            if (accept.endsWith(DotAssetAPI.PARTIAL_MIME_TYPE)) {
                return systemHost ? this.partialSystemHost : this.partialCurrentSite;
            }

            return systemHost ? this.exactSystemHost : this.exactCurrentSite;
        }

        /**
         * @return the buckets in the order a match must be resolved (most specific + current site first).
         */
        private List<Map<String, ContentType>> inPrecedenceOrder() {

            return List.of(this.exactCurrentSite, this.exactSystemHost,
                    this.partialCurrentSite, this.partialSystemHost,
                    this.wildcardCurrentSite, this.wildcardSystemHost);
        }
    }

    /**
     * Holds a content type together with the mime types its binary field accepts and whether it
     * lives on the system host. Used as the intermediate value while bucketing candidates.
     */
    private static class ContentTypeMimeType {

        private final boolean systemHost;
        private final String [] mimeTypeFieldVariables;
        private final ContentType contentType;

        public ContentTypeMimeType(final String [] mimeTypeFieldVariables,
                                   final ContentType contentType, final boolean systemHost) {
            this.mimeTypeFieldVariables = mimeTypeFieldVariables;
            this.contentType = contentType;
            this.systemHost  = systemHost;
        }
    }
}
