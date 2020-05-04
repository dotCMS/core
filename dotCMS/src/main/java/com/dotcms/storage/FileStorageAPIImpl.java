package com.dotcms.storage;

import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.util.FileUtil;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;

import java.io.File;
import java.util.Map;

/**
 * Default implementation
 * @author jsanca
 */
public class FileStorageAPIImpl implements FileStorageAPI {

    // width,height,contentType,author,keywords,fileSize,content,length,title


    @Override
    public Map<String, Object> generateBasicMetaData(final File binary) {

        final ImmutableMap.Builder<String, Object> mapBuilder = new ImmutableMap.Builder<>();

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
        return null;
    }
}
