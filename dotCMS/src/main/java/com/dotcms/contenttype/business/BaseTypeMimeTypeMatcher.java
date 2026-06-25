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
 * stricter matches.</p>
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

            // Stores the content type indexed by mimetypes, on each index stores the subsets
            // 0:Exact/on Site, 1:Exact/SYSTEM_HOST, 2:Partial Wildcard/on Site, 3:Partial Wildcard/SYSTEM_HOST,
            // 4:Total Wildcard (or null)/on Site, 5:Total Wildcard (or null)/SYSTEM_HOST
            final Map<String, ContentType>[] mimeTypeMappingArray = new Map[6];

            contentTypes.stream()
                    .filter(contentType -> isSystemHostOrCurrentHost(contentType, currentHost))
                    .map(contentType -> mapToContentTypeMimeType(contentType, binaryFieldVar))
                    .forEach(contentTypeMimeType -> {

                        for (final String mimeTypeItem : contentTypeMimeType.mimeTypeFieldVariables) {

                            if (DotAssetAPI.ALL_MIME_TYPE.equals(mimeTypeItem)) {

                                this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost ? 5 : 4).put(mimeTypeItem, contentTypeMimeType.contentType);
                            } else if (mimeTypeItem.endsWith(DotAssetAPI.PARTIAL_MIME_TYPE)) {

                                this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost ? 3 : 2).put(mimeTypeItem, contentTypeMimeType.contentType);
                            } else {

                                this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost ? 1 : 0).put(mimeTypeItem, contentTypeMimeType.contentType);
                            }
                        }
                    });

            return findContentType(mimeType, mimeTypeMappingArray);
        }

        return Optional.empty();
    }

    private Optional<ContentType> findContentType(final String mimeType,
                                                  final Map<String, ContentType>... mimeTypeMappingArray) {

        for (final Map<String, ContentType> mimeTypeContentTypeMap : mimeTypeMappingArray) {

            if (null != mimeTypeContentTypeMap) {
                for (final Map.Entry<String, ContentType> entry : mimeTypeContentTypeMap.entrySet()) {

                    if (MimeTypeUtils.match(entry.getKey(), mimeType)) {

                        return Optional.ofNullable(entry.getValue());
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Map<String, ContentType> getMap(final Map<String, ContentType>[] mimeTypeMappingArray, final int index) {

        if (null == mimeTypeMappingArray[index]) {

            mimeTypeMappingArray[index] = new HashMap<>();
        }

        return mimeTypeMappingArray[index];
    }

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

    private boolean isSystemHostOrCurrentHost (final ContentType contentType, final Host currentHost) {

        final String contentTypeHostId = contentType.host();
        final String systemHostId      = APILocator.systemHost().getIdentifier();
        return null == contentTypeHostId ||
                contentTypeHostId.equals(currentHost.getIdentifier()) || contentTypeHostId.equals(systemHostId);
    }

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
