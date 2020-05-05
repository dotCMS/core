package com.dotcms.storage;

import com.dotcms.tika.TikaUtils;
import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import io.vavr.control.Try;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Default implementation
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    // width,height,contentType,author,keywords,fileSize,content,length,title


    @Override
    public Map<String, Object> generateBasicMetaData(final File binary) {

        final ImmutableSortedMap.Builder<String, Object> mapBuilder =
                new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());

        if (binary.exists() && binary.canRead()) {
            mapBuilder.put("title", binary.getName());
            mapBuilder.put("path", binary.getAbsolutePath());
            mapBuilder.put("length", binary.length());
            mapBuilder.put("contentType", MimeTypeUtils.getMimeType(binary));
            mapBuilder.put("modDate", System.currentTimeMillis());
            mapBuilder.put("sha256", Try.of(()->FileUtil.sha256(binary)).getOrElse("unknown"));
        }

        return mapBuilder.build();
    }

    @Override
    public Map<String, Object> generateFullMetaData(final  File binary) {

        final TreeMap<String, Object> metadataMap = new TreeMap<>(Comparator.naturalOrder());

        try {

            final TikaUtils tikaUtils = new TikaUtils();
            final int maxLength       = Config.getIntProperty("META_DATA_MAX_SIZE",
                    TikaUtils.DEFAULT_META_DATA_MAX_SIZE) * TikaUtils.SIZE;

            metadataMap.putAll(this.generateBasicMetaData(binary));
            metadataMap.putAll(tikaUtils.getForcedMetaDataMap(binary, maxLength));
        } catch (DotDataException e) {

            return Collections.emptyMap();
        }

        return new ImmutableSortedMap.Builder<String, Object>(Comparator.naturalOrder()).putAll(metadataMap).build();
    }
}
