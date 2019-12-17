package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap.Builder;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * Json Serializer of {@link HTMLPageAssetInfoSerializer}
 */
public class HTMLPageAssetInfoSerializer extends JsonSerializer<HTMLPageAssetInfo> {
    @Override
    public void serialize(final HTMLPageAssetInfo htmlPageAssetInfo, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        final ContentletToMapTransformer transformer = new ContentletToMapTransformer(htmlPageAssetInfo.getPage());
        final Map<String, Object> pageContentletMap  = transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);

        final Builder<Object, Object> pageMapBuilder =
                new Builder<>(Comparator.comparing(Object::toString))
                .putAll(pageContentletMap)
                .put("workingInode",  htmlPageAssetInfo.getWorkingInode())
                .put("shortyWorking", htmlPageAssetInfo.getShortyWorking())
                .put("canEdit",       htmlPageAssetInfo.isCanEdit())
                .put("canRead",       htmlPageAssetInfo.isCanRead())
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