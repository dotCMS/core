package com.dotmarketing.image.reader;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;
import com.twelvemonkeys.lang.SystemUtil;

final class SVGProviderInfo extends ReaderWriterProviderInfo {
    final static boolean SVG_READER_AVAILABLE = SystemUtil.isClassAvailable("com.dotmarketing.image.reader.SVGImageReader");

    protected SVGProviderInfo() {
        super(
                SVGProviderInfo.class,
                SVG_READER_AVAILABLE ? new String[]{"svg", "SVG"} : new String[]{""}, // Names
                SVG_READER_AVAILABLE ? new String[]{"svg"} : null, // Suffixes
                SVG_READER_AVAILABLE ? new String[]{"image/svg", "image/x-svg", "image/svg+xml", "image/svg-xml"} : null, // Mime-types
                "com.dotmarketing.image.reader.SVGImageReader", // Reader class name
                new String[] {"com.dotmarketing.image.reader.SVGImageReaderSpi"},
                null,
                null,
                false, null, null, null, null,
                true, null, null, null, null
        );
    }
}