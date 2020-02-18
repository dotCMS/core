package com.dotcms.contenttype.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation
 * @author jsanca
 */
public class DotAssetAPIImpl implements DotAssetAPI {

    @Override
    public Optional<ContentType> tryMatch(final File file,final  Host currentHost,final  User user) throws DotDataException, DotSecurityException {

        return this.tryMatch(MimeTypeUtils.getMimeType(file), currentHost, user);
    }

    @Override
    public Optional<ContentType> tryMatch(final String mimeType, final Host currentHost, final User user) throws DotSecurityException, DotDataException {

        final List<ContentType> dotAssetContentTypes = APILocator.getContentTypeAPI(user).findByType(BaseContentType.DOTASSET);

        if (UtilMethods.isSet(dotAssetContentTypes)) {

            // Stores the content type indexed by mimetypes, on each index stores the subsets
            // 0:Exact/on Site, 1:Exact/SYSTEM_HOST, 2:Partial Wildcard/on Site, 3:Partial Wildcard/SYSTEM_HOST,
            // 4:Total Wildcard (or null)/on Site, 5:Total Wildcard (or null)/SYSTEM_HOST
            final Map<String, ContentType>[] mimeTypeMappingArray = new Map [6];

            dotAssetContentTypes.stream()
                    .filter(contentType -> isSystemHostOrCurrentHost(contentType, currentHost)) // remove the ones that are not system host or current host
                    .map(this::mapToContentTypeMimeType).forEach(contentTypeMimeType -> {

                for (final String mimeTypeItem : contentTypeMimeType.mimeTypeFieldVariables) {

                    if (ALL_MIME_TYPE.equals(mimeTypeItem)) {

                        this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost?5:4).put(mimeTypeItem, contentTypeMimeType.contentType);
                    } else if (mimeTypeItem.endsWith(PARTIAL_MIME_TYPE)) {

                        this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost?3:2).put(mimeTypeItem, contentTypeMimeType.contentType);
                    } else {

                        this.getMap(mimeTypeMappingArray, contentTypeMimeType.systemHost?1:0).put(mimeTypeItem, contentTypeMimeType.contentType);
                    }
                }
            });

            return findDotAssetContentType (mimeType, mimeTypeMappingArray);
        }

        return Optional.empty();
    }

    private Optional<ContentType> findDotAssetContentType(final String mimeType,
                                                          final Map<String, ContentType>[] mimeTypeMappingArray) {

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

    private ContentTypeMimeType mapToContentTypeMimeType(final ContentType contentType) {

        final String systemHostId          = APILocator.systemHost().getIdentifier();
        final String contentTypeHostId     = contentType.host();
        final boolean isSystemHost         = null == contentTypeHostId || contentTypeHostId.equals(systemHostId);
        final Map<String, Field>  fieldMap = contentType.fieldMap();
        String [] mimeTypeFieldVariables   = new String [] { ALL_MIME_TYPE };
        final List<FieldVariable> fieldVariables = fieldMap.get(DotAssetContentType.ASSET_FIELD_VAR).fieldVariables();
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

    private class ContentTypeMimeType {

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
