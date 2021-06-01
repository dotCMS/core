package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Map<String, Serializable> generate(final File binary, final long maxLength) {
        try {
            final TikaUtils tikaUtils = new TikaUtils();

            final Map<String, Object> metaDataMap = tikaUtils
                    .getForcedMetaDataMap(binary, (int) maxLength);
            return metaDataMap.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof Serializable).
                            collect(Collectors.toMap(Entry::getKey,
                                    e -> (Serializable) e.getValue()));
        } catch (Exception e) {
            Logger.warnAndDebug(TikaMetadataGenerator.class, e.getMessage(), e);
        }
        return ImmutableMap.of();
    }
}
