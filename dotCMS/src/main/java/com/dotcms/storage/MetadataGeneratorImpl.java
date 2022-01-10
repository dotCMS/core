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
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

/**
 * Tika based metadata generator.
 */
class MetadataGeneratorImpl implements MetadataGenerator {

    private static final String IMAGE_DIMENSIONS_CALCULATOR_IMPL = "IMAGE_DIMENSIONS_CALCULATOR_IMPL";

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
     * Compute the dimensions of a given image binary
     * @param binary
     * @return
     */
    private Dimension calculateDimensions(final File binary) {
        return dimensionsCalculators.get().calculateDimensions(binary);
    }

    /**
     * Lazy loaded DimensionCalculator instance
     * The calc implementation can be changed based on a property name.
     */
    Lazy<StandAloneDimensionCalculator> dimensionsCalculators = Lazy.of(()->{
        final String className = Config.getStringProperty(IMAGE_DIMENSIONS_CALCULATOR_IMPL, SuffixBasedDimensionCalcImpl.class.getName());
        final StandAloneDimensionCalculator instance = (StandAloneDimensionCalculator) (UtilMethods.isSet(className)
                ? Try.of(() -> Class.forName(className).newInstance()).getOrElseThrow(
                DotRuntimeException::new) : new SuffixBasedDimensionCalcImpl());
        Logger.info(MetadataGeneratorImpl.class, "Image dimensions stand-alone calculator instance is "+instance.getClass());
        return instance;
    });

    /**
     * This leaves open the possibility to change the implementation on how we calculate the dimension for a given image
     */
    @FunctionalInterface
    interface StandAloneDimensionCalculator {

        /**
         * Implement to load the Dimensions of the given image
         * @param image {@link File} binary to generate the metadata
         * @return dimensions {@link Dimension} width and height
         */
        Dimension calculateDimensions(File image);
    }

    /**
     * This implementation is reliable as long as the file has an extension
     * It does not read the whole file into memory for the purpose of getting the dimensions
     * An alternative would be to use  ImageIO.read like this
     * final BufferedImage bufferedImage = ImageIO.read(image);
     *   width = bufferedImage.getWidth();
     *   height = bufferedImage.getHeight();
     * But this has the implication that loads the whole file into memory
     * see https://stackoverflow.com/questions/672916/how-to-get-image-height-and-width-using-java#9083914
     */
    static class SuffixBasedDimensionCalcImpl implements StandAloneDimensionCalculator {

        /**
         * This method still uses ImageIO to get the readers based on the given file name
         * It relies on the file extension to get the proper reader
         * The advantage here is that we do not read the whole file into memory
         * @param image
         * @return
         */
        @Override
        public Dimension calculateDimensions(final File image) {
            final int pos = image.getName().lastIndexOf(".");
            if (pos > 0) {
                final String suffix = image.getName().substring(pos + 1);
                final Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
                while (iter.hasNext()) {
                    final ImageReader reader = iter.next();
                    try {
                        final ImageInputStream stream = new FileImageInputStream(image);
                        reader.setInput(stream);
                        final int width = reader.getWidth(reader.getMinIndex());
                        final int height = reader.getHeight(reader.getMinIndex());
                        return new Dimension(width, height);
                    } catch (IOException e) {
                        Logger.warn(MetadataGeneratorImpl.class,
                                "Error reading: " + image.getAbsolutePath(), e);
                    } finally {
                        reader.dispose();
                    }
                }
            }
            Logger.warn(MetadataGeneratorImpl.class, String.format(
                    "Can't calculate dimensions for the given image '%s' with unknown file extension.",
                    image.getName()));
            return new Dimension(0, 0);
        }
    }

}
