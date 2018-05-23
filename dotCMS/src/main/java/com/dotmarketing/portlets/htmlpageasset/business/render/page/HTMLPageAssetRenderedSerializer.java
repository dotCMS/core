package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Json serializer of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedSerializer extends PageViewSerializer {

    @Override
    protected ImmutableMap<String, Object> getObjectMap(PageView pageView) {
        final HTMLPageAssetRendered htmlPageAssetRendered = (HTMLPageAssetRendered) pageView;

        final Map<String, Object> objectMap = super.getObjectMap(pageView);

        final Map<String, Object> pageMap = (Map<String, Object>) objectMap.get("page");
        pageMap.put("rendered", htmlPageAssetRendered.getHtml());

        final Map<String, Object> templateMap = (Map<String, Object>) objectMap.get("template");
        templateMap.put("canEdit", htmlPageAssetRendered.isCanEditTemplate());

        final ImmutableMap.Builder<String, Object> responseMapBuilder = ImmutableMap.<String, Object> builder()
                .putAll(objectMap)
                .put("viewAs", htmlPageAssetRendered.getViewAs())
                .put("canCreateTemplate", htmlPageAssetRendered.isCanCreateTemplate());

        return responseMapBuilder.build();
    }

}
