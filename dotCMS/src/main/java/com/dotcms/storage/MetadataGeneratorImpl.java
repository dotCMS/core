package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.image.filter.ImageFilterApiImpl;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.dotcms.storage.model.BasicMetadataFields.CONTENT_TYPE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.EDITABLE_AS_TEXT;
import static com.dotcms.storage.model.BasicMetadataFields.HEIGHT_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.IS_IMAGE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.LENGTH_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.MOD_DATE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.NAME_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.SHA256_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.SIZE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.TITLE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.WIDTH_META_KEY;

/**
 * This implementation of {@link MetadataGenerator} provides two ways of retrieving file metadata:
 * <ul>
 *     <li>Through Tika.</li>
 *     <li>By retrieving the file's properties provided by the {@link File} class itself.</li>
 * </ul>
 *
 * @author Fabrizzio Araya
 * @since Jan 14th, 2022
 */
class MetadataGeneratorImpl implements MetadataGenerator {

    /**
     * {@inheritDoc}
     * @param binary     {@link File} binary to generate the metadata
     * @param maxLength  {@link Long} max length to parse the content
     * @return
     */
    @Override
    public Map<String, Serializable> tikaBasedMetadata(final File binary, final long maxLength) {
        try {
            final TikaUtils tikaUtils = new TikaUtils();

            final Map<String, Object> metaDataMap = tikaUtils
                    .getForcedMetaDataMap(binary, (int) maxLength);
            return metaDataMap.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Serializable).
                            collect(Collectors.toMap(Entry::getKey,
                                    e -> (Serializable) e.getValue()));
        } catch (Exception e) {
            Logger.warnAndDebug(MetadataGeneratorImpl.class, e.getMessage(), e);
        }
        return ImmutableMap.of();
    }

    @Override
    public TreeMap<String, Serializable> standAloneMetadata(final File binary){
        final TreeMap<String, Serializable> metadataMap = new TreeMap<>(Comparator.naturalOrder());
        final String binaryName = binary.getName();
        final String mimeType = Try.of(()->MimeTypeUtils.getMimeType(binary)).getOrElse(MimeTypeUtils.MIME_TYPE_APP_OCTET_STREAM);
        metadataMap.put(NAME_META_KEY.key(), binaryName);
        // Title gets replaced by the loaded metadata. Otherwise, we set a default
        metadataMap.put(TITLE_META_KEY.key(), binaryName);
        final String relativePath = binary.getAbsolutePath()
                .replace(ConfigUtils.getAssetPath(),
                        StringPool.BLANK);
        metadataMap.put(PATH_META_KEY.key(), relativePath);
        final long length = binary.length();
        metadataMap.put(LENGTH_META_KEY.key(), length);
        metadataMap.put(SIZE_META_KEY.key(), length);
        metadataMap.put(CONTENT_TYPE_META_KEY.key(), mimeType);
        metadataMap.put(MOD_DATE_META_KEY.key(), binary.lastModified());
        metadataMap.put(SHA256_META_KEY.key(),
                Try.of(() -> FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));

        final boolean isImage = UtilMethods.isImage(relativePath);
        metadataMap.put(IS_IMAGE_META_KEY.key(), isImage);
        if (isImage) {
            // These are added here to even things when comparing. Typically, these values are added by Tika except for
            // SVG files, so that creates some sort of inconsistency. We add them for image types with a default value
            // of zero that gets replaced by the values provided by Tika
            final Dimension dimension = calculateDimensions(binary);
            metadataMap.put(WIDTH_META_KEY.key(), dimension.width);
            metadataMap.put(HEIGHT_META_KEY.key(), dimension.height);
        } else {
            metadataMap.put(EDITABLE_AS_TEXT.key(), FileUtil.isFileEditableAsText(mimeType));
        }
        return metadataMap;
    }

    static ImageFilterApiImpl imageApi = ImageFilterAPI.apiInstance.apply();

    /**
     * Computes the dimensions -- height and width -- of a given image binary through our
     * {@link ImageFilterAPI}.
     *
     * @param binary The {@link File} whose dimensions wil be computed.
     *
     * @return The {@link Dimension} of the given binary, or a zero-width zer-height Dimension if an
     * error occurred.
     */
    private Dimension calculateDimensions(final File binary) {
        try {
            return imageApi.getWidthHeight(binary);
        } catch (final Throwable throwable) {
            Logger.warnAndDebug(MetadataGeneratorImpl.class,
                    String.format("Unable to calculate dimensions for the given binary %s.",
                            binary),
                    throwable);
        }
        return new Dimension(0, 0);
    }

}
