package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Tika based metadata generator.
 */
class TikaMetadataGenerator implements MetadataGenerator {

    /**
     * {@inheritDoc}
     * @param binary     {@link File} binary to generate the metadata
     * @param maxLength  {@link Long} max length to parse the content
     * @return
     */
    @Override
    public Map<String, Object> generate(final File binary, final long maxLength) {
        try {
            final TikaUtils tikaUtils = new TikaUtils();
            return tikaUtils.getForcedMetaDataMap(binary,(int) maxLength);
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
