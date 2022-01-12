package com.dotcms.storage;

import static com.dotcms.storage.model.BasicMetadataFields.CONTENT_TYPE_META_KEY;
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

import com.dotcms.tika.TikaUtils;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.image.filter.ImageFilterAPI;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tika based metadata generator.
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
        metadataMap.put(NAME_META_KEY.key(), binaryName);
        metadataMap.put(TITLE_META_KEY.key(), binaryName); //Title gets replaced by the loaded metadata. Otherwise iwe set a default
        final String relativePath = binary.getAbsolutePath()
                .replace(ConfigUtils.getAbsoluteAssetsRootPath(),
                        StringPool.BLANK);
        metadataMap.put(PATH_META_KEY.key(), relativePath);
        final long length = binary.length();
        metadataMap.put(LENGTH_META_KEY.key(), length);
        metadataMap.put(SIZE_META_KEY.key(), length);
        metadataMap.put(CONTENT_TYPE_META_KEY.key(), MimeTypeUtils.getMimeType(binary));
        metadataMap.put(MOD_DATE_META_KEY.key(), binary.lastModified());
        metadataMap.put(SHA256_META_KEY.key(),
                Try.of(() -> FileUtil.sha256toUnixHash(binary)).getOrElse("unknown"));

        final boolean isImage = UtilMethods.isImage(relativePath);
        metadataMap.put(IS_IMAGE_META_KEY.key(), isImage);
        //These are added here to even things when comparing
        //typically these values are added by tika except for svg file so that creates some sort of inconsistency
        //we add them for image types with a default value of zero that gets replaced by the values provided by tika
        if (isImage) {
            final Dimension dimension = calculateDimensions(binary);
            metadataMap.put(WIDTH_META_KEY.key(), dimension.width);
            metadataMap.put(HEIGHT_META_KEY.key(), dimension.height);
        }
        return metadataMap;
    }

    /**
     * Compute the dimensions of a given image binary through our image api
     * @param binary
     * @return
     */
    private Dimension calculateDimensions(final File binary) {
       return ImageFilterAPI.apiInstance.get().getWidthHeight(binary);
    }

}
