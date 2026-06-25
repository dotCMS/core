package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the {@link ContentType} for the {@link BaseContentType#FILEASSET} base type.
 *
 * <p>Mirrors {@link DotAssetBaseTypeToContentTypeStrategyImpl}: it matches the uploaded binary's
 * mime type against the "accept" field variable of the File Asset content types (via
 * {@link BaseTypeMimeTypeMatcher}). When no content type matches by mime type, it falls back to the
 * system default {@code FileAsset} content type so a File Asset upload never fails to resolve a
 * content type. In practice the default {@code FileAsset} (which has no "accept" restriction) is
 * already resolved by the matcher as the total-wildcard catch-all.</p>
 *
 * @author dotCMS
 */
public class FileAssetBaseTypeToContentTypeStrategyImpl implements BaseTypeToContentTypeStrategy {

    private final BaseTypeMimeTypeMatcher mimeTypeMatcher = new BaseTypeMimeTypeMatcher();

    @Override
    public Optional<ContentType> apply(final BaseContentType baseContentType, final Map<String, Object> contextMap) {

        final User user              = (User)contextMap.get("user");
        final Host currentHost       = (Host)contextMap.get("host");
        final List<File> binaryFiles = (List<File>) contextMap.getOrDefault("binaryFiles", Collections.emptyList());
        final Map<String, Object> contentletMap = (Map<String, Object>) contextMap.get("contentletMap");
        final List<String>        accessingList = (List<String>)contextMap.get("accessingList");

        try {

            final File file = this.getBinary(binaryFiles, contentletMap, accessingList);
            if (null != file && file.exists() && file.canRead()) {

                final String mimeType = APILocator.getFileAssetAPI().getMimeType(file);
                final Optional<ContentType> matched = this.mimeTypeMatcher.match(mimeType, currentHost, user,
                        BaseContentType.FILEASSET, FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);

                if (matched.isPresent()) {

                    return matched;
                }
            }

            // Safety fallback: the system default File Asset content type.
            return Optional.ofNullable(APILocator.getContentTypeAPI(user)
                    .find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME));
        } catch (DotDataException | DotSecurityException e) {

            Logger.debug(this, () -> "Could not resolve a FILEASSET content type: " + e.getMessage());
            return Optional.empty();
        }
    }

    private File getBinary (final List<File> binaryFiles, final Map<String, Object> contentletMap,
                            final List<String> accessingList) {

        File file = null;
        if (contentletMap.containsKey(FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR)) {

            final Object assetValue = contentletMap.get(FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR);
            if (assetValue instanceof File) {

                file = (File)assetValue;
            } else {

                final Optional<DotTempFile> tempFile = APILocator.getTempFileAPI()
                        .getTempFile(accessingList, assetValue.toString()); // lets try by temporal file id.
                if (tempFile.isPresent()) {

                    file = tempFile.get().file;
                }
            }
        } else if (!binaryFiles.isEmpty()) {

            file = binaryFiles.get(0); // upload path puts the binary here (only one file per file asset).
        }

        return file;
    }
}
