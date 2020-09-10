package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Collections;
import java.util.Map;

class TikaMetadataGenerator implements MetadataGenerator {

    @Override
    public Map<String, Object> generate(final File binary, final long maxLength) {

        try {
            final TikaUtils tikaUtils = new TikaUtils();
            final Map<String, Object> tikaMetaDataMap = tikaUtils.getForcedMetaDataMap(binary,  Long.valueOf(maxLength).intValue());
            return tikaMetaDataMap;
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }

        return Collections.emptyMap();
    }
}
