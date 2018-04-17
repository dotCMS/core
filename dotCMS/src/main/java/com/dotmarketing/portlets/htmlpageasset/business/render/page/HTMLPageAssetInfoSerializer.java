package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

/**
 * Json Serializer of {@link HTMLPageAssetInfoSerializer}
 */
public class HTMLPageAssetInfoSerializer extends JsonSerializer<HTMLPageAssetInfo> {
    @Override
    public void serialize(HTMLPageAssetInfo htmlPageAssetInfo, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        ImmutableMap.Builder<Object, Object> pageMapBuilder = ImmutableMap.builder()
                .putAll(htmlPageAssetInfo.getPage().getMap())
                .put("workingInode", htmlPageAssetInfo.getWorkingInode())
                .put("shortyWorking", htmlPageAssetInfo.getShortyWorking())
                .put("canEdit", htmlPageAssetInfo.isCanEdit())
                .putAll(getLockMap(htmlPageAssetInfo));

        if(htmlPageAssetInfo.getLiveInode() != null) {
            pageMapBuilder.put("liveInode", htmlPageAssetInfo.getLiveInode())
                    .put("shortyLive", htmlPageAssetInfo.getShortyLive());
        }

        jsonGenerator.writeObject(pageMapBuilder.build());
    }

    private Map<String, Object> getLockMap(HTMLPageAssetInfo htmlPageAssetInfo) {

        final ImmutableMap.Builder<String, Object> lockMapBuilder = ImmutableMap.builder();
        lockMapBuilder.put("canLock", htmlPageAssetInfo.isCanLock());

        String lockedBy = htmlPageAssetInfo.getLockedBy();

        if (lockedBy != null) {
            lockMapBuilder.put("lockedOn", htmlPageAssetInfo.getLockedOn())
                    .put("lockedBy", lockedBy)
                    .put("lockedByName", htmlPageAssetInfo.getLockedByName());
        }

        return lockMapBuilder.build();
    }
}